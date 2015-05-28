package controller.replication;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.math3.stat.StatUtils;

import controller.experiment.data.ExperimentBean;
import controller.replication.data.AgentBean;
import controller.replication.data.EquilibriumBean;
import controller.replication.data.ReplicationBean;
import controller.replication.data.ReplicationData;
import controller.replication.data.ResultBean;
import utils.Utils;
import backend.Auction;
import backend.Auction.MatchingType;
import backend.EquilibriumStatistics;
import backend.agents.Agent;
import backend.agents.network.AgentNetwork;
import backend.agents.network.export.NetworkExporter;
import backend.markets.MarketType;
import backend.markets.Markets;
import backend.tx.Transaction;

public class ReplicationsRunner {

	private Date startingTime;
	
	private ExecutorService replicationTaskExecutor;
	
	private boolean running;
	private boolean canceled;
	
	private List<ReplicationTask> replicationTasks;
	private Thread awaitFinishThread;
	
	private List<ReplicationData> replicationData;
	
	private AgentNetwork template;
	private Markets markets;
	private ReplicationsListener listener;
	
	private AtomicInteger replications;
	
	private ExperimentBean experiment;
	
	private ReplicationData currentStats;
	private EquilibriumStatistics varianceStats;
	private List<double[]> medianMarkets;
	
	private final static SimpleDateFormat FILENAME_DATE_FORMATTER = new SimpleDateFormat( "yyyyMMdd_HHmmss" );
	
	public interface ReplicationsListener {
		public void replicationFinished( ReplicationData data, ReplicationData meanData, EquilibriumStatistics variance, List<double[]> medianMarkets );
		public void allReplicationsFinished();
	}
	
	public enum TerminationMode {
		TOTAL_TX,
		FAIL_TOTAL_TX,
		FAIL_SUCCESSIVE_TX,
		TRADING_HALTED
	}

	public ReplicationsRunner( AgentNetwork template, Markets markets ) {
		this.replicationTasks = new ArrayList<ReplicationTask>();
		this.replicationData = new ArrayList<ReplicationData>();
		this.medianMarkets = new ArrayList<double[]>();
		
		this.template = template;
		this.markets = markets;
		
		this.varianceStats = new EquilibriumStatistics();
	}
	
	public boolean isRunning() {
		return this.running;
	}
	
	public Date getStartingTime() {
		return this.startingTime;
	}

	public ReplicationData getCurrentStats() {
		return this.currentStats;
	}
	
	public int getReplicationsLeft() {
		return this.experiment.getReplications() - this.replicationData.size();
	}
	
	public List<ReplicationTask> getReplicationTasks() {
		return this.replicationTasks;
	}
	
	public void startAsync( ExperimentBean experiment, ReplicationsListener listener ) {
		int processorCount = Runtime.getRuntime().availableProcessors();
		// leave one for GUI-purposes, otherwise would freeze
		processorCount--;

		this.start(experiment, listener, processorCount );
		
		this.awaitFinishThread = new Thread( new Runnable() {
			@Override
			public void run() {
				for ( ReplicationTask task : replicationTasks ) {
					try {
						task.getFuture().get();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
				}
				
				if ( false == ReplicationsRunner.this.canceled ) {
					ReplicationsRunner.this.listener.allReplicationsFinished();
				}
				
				ReplicationsRunner.this.cleanUp();
			}
		} );
		
		this.awaitFinishThread.setName( "Replications finished wait-thread" );
		this.awaitFinishThread.start();
	}
	
	public void startAndWaitFinish( ExperimentBean experiment, ReplicationsListener listener, int maxThreads ) {
		this.start(experiment, listener, maxThreads );
		
		for ( ReplicationTask task : replicationTasks ) {
			try {
				task.getFuture().get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
	
		this.listener.allReplicationsFinished();
		this.cleanUp();
	}
	
	
	public void stopAsync() {
		// no replications running
		if ( false == this.isRunning() ) {
			return;
		}
		
		this.canceled = true;
		
		for ( ReplicationTask task : this.replicationTasks ) {
			task.cancel();
		}
		
		try {
			this.awaitFinishThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		this.running = false;
	}
	
	@SuppressWarnings("rawtypes")
	private void start( ExperimentBean experiment, ReplicationsListener listener, int maxThreads ) {
		// replications already running
		if ( this.isRunning() ) {
			return;
		}
		
		this.running = true;
		this.canceled = false;
		this.startingTime = new Date();
		this.experiment = experiment;
		
		this.listener = listener;
		this.replications = new AtomicInteger( experiment.getReplications() );
		
		// always do parallel-processing but at least one thread
		int threadCount = Math.max( 1, maxThreads ); 
		// if less replications than threads, then limit thread-count by replication-count
		threadCount = Math.min( threadCount, experiment.getReplications() );

		this.replicationTaskExecutor = Executors.newFixedThreadPool( threadCount, new ThreadFactory() {
			public Thread newThread( Runnable r ) {
				return new Thread( r, "Replication-Thread" );
			}
		} );
		
		for ( int i = 0; i < threadCount; ++i ) {
			ReplicationTask task = new ReplicationTask( i );
			
			Future future = this.replicationTaskExecutor.submit( task );
			task.setFuture( future );

			this.replicationTasks.add( task );
		}		
	}

	private void cleanUp() {
		this.writeResults();
		
		this.replicationData.clear();
		this.replicationTasks.clear();
		
		this.replicationTaskExecutor.shutdown();
		this.awaitFinishThread = null;
	}
	
	private synchronized void replicationFinished( ReplicationData data ) {
		this.replicationData.add( data );
		this.currentStats = this.calculateStatistics();
		
		// NOTE: need to copy markets because gui-thread will use it concurrently and may be cleared during replicationFinished 
		List<double[]> copyMarkets = new ArrayList<double[]>( this.medianMarkets );
		
		this.listener.replicationFinished( data, this.currentStats, this.varianceStats, copyMarkets );
	}
	
	private ReplicationData calculateStatistics() {
		int agentCount = this.template.size();
		
		ReplicationData currentStats = new ReplicationData();
		List<Agent> meanAgents = new ArrayList<Agent>( agentCount );
		EquilibriumStatistics meanStats = new EquilibriumStatistics();
		
		int validReplications = 0;
		double[] cashAverage = new double[ agentCount ];
		double[] assertAverage = new double[ agentCount ];
		double[] loanGivenAverage = new double[ agentCount ];
		double[] loanTakenAverage = new double[ agentCount ];
		
		double[] meanStatsAssetPriceValues = new double[ this.replicationData.size() ];
		double[] meanStatsLoanPriceValues = new double[ this.replicationData.size() ];
		double[] meanStatsAssetLoanValues = new double[ this.replicationData.size() ];
		double[] meanStatsCollateralValues = new double[ this.replicationData.size() ];
		double[] meanStatsI0Values = new double[ this.replicationData.size() ];
		double[] meanStatsI1Values = new double[ this.replicationData.size() ];
		double[] meanStatsI2Values = new double[ this.replicationData.size() ];
		double[] meanStatsPessimistValues = new double[ this.replicationData.size() ];
		double[] meanStatsMedianistValues = new double[ this.replicationData.size() ];
		double[] meanStatsOptimistValues = new double[ this.replicationData.size() ];
		
		this.medianMarkets.clear();
		
		for ( ReplicationData data : this.replicationData ) {
			if ( data.isCanceled() ) {
				continue;
			}
			
			List<MarketType> successfulMarkets = data.getSuccessfulMarkets();
			for ( int i = 0; i < successfulMarkets.size(); ++i ) {
				MarketType market = successfulMarkets.get( i );
				double[] marketCounts = null;
				
				if ( i == medianMarkets.size() ) {
					marketCounts = new double[ MarketType.values().length ];
					medianMarkets.add( marketCounts );
				} else {
					marketCounts = medianMarkets.get( i );
				}
				
				marketCounts[ market.ordinal() ]++;
			}
			
			List<Agent> finalAgents = data.getFinalAgents();
			for ( int i = 0; i < finalAgents.size(); ++i ) {
				Agent a = finalAgents.get( i );
				
				cashAverage[ i ] += a.getCash();
				assertAverage[ i ] += a.getAssets();
				loanGivenAverage[ i ] += a.getLoansGiven();
				loanTakenAverage[ i ] += a.getLoansTaken();
			}
			
			meanStats.assetPrice += data.getStats().assetPrice;
			meanStats.loanPrice += data.getStats().loanPrice;
			meanStats.assetLoanPrice += data.getStats().assetLoanPrice;
			meanStats.collateralPrice += data.getStats().collateralPrice;
			
			meanStats.i0 += data.getStats().i0;
			meanStats.i1 += data.getStats().i1;
			meanStats.i2 += data.getStats().i2;
			
			meanStats.pessimistWealth += data.getStats().pessimistWealth;
			meanStats.medianistWealth += data.getStats().medianistWealth;
			meanStats.optimistWealth += data.getStats().optimistWealth;
			
			meanStatsAssetPriceValues[ validReplications ] = data.getStats().assetPrice;
			meanStatsLoanPriceValues[ validReplications ] = data.getStats().loanPrice;
			meanStatsAssetLoanValues[ validReplications ] = data.getStats().assetLoanPrice;
			meanStatsCollateralValues[ validReplications ] = data.getStats().collateralPrice;
			
			meanStatsI0Values[ validReplications ] = data.getStats().i0;
			meanStatsI1Values[ validReplications ] = data.getStats().i1;
			meanStatsI2Values[ validReplications ] = data.getStats().i2;
			
			meanStatsPessimistValues[ validReplications ] = data.getStats().pessimistWealth;
			meanStatsMedianistValues[ validReplications ] = data.getStats().medianistWealth;
			meanStatsOptimistValues[ validReplications ] = data.getStats().optimistWealth;
			
			validReplications++;
		}
		
		// no valid replications so far: no current data
		if ( 0 == validReplications ) {
			return null;
		}
		
		for ( double[] marketCounts : this.medianMarkets ) {
			for ( int i = 0; i < MarketType.values().length; ++i ) {
				marketCounts[ i ] /= validReplications;
			}
		}
		
		meanStats.assetPrice /= validReplications;
		meanStats.loanPrice /= validReplications;
		meanStats.assetLoanPrice /= validReplications;
		meanStats.collateralPrice /= validReplications;
		
		meanStats.i0 /= validReplications;
		meanStats.i1 /= validReplications;
		meanStats.i2 /= validReplications;
		
		meanStats.pessimistWealth /= validReplications;
		meanStats.medianistWealth /= validReplications;
		meanStats.optimistWealth /= validReplications;
		
		this.varianceStats.assetPrice = Math.sqrt( StatUtils.variance( meanStatsAssetPriceValues, meanStats.assetPrice, 0, validReplications ) );
		this.varianceStats.loanPrice = Math.sqrt( StatUtils.variance( meanStatsLoanPriceValues, meanStats.loanPrice, 0, validReplications ) );
		this.varianceStats.assetLoanPrice = Math.sqrt( StatUtils.variance( meanStatsAssetLoanValues, meanStats.assetLoanPrice, 0, validReplications ) );
		this.varianceStats.collateralPrice = Math.sqrt( StatUtils.variance( meanStatsCollateralValues, meanStats.collateralPrice, 0, validReplications ) );
		
		this.varianceStats.i0 = Math.sqrt( StatUtils.variance( meanStatsI0Values, meanStats.i0, 0, validReplications ) );
		this.varianceStats.i1 = Math.sqrt( StatUtils.variance( meanStatsI1Values, meanStats.i1, 0, validReplications ) );
		this.varianceStats.i2 = Math.sqrt( StatUtils.variance( meanStatsI2Values, meanStats.i2, 0, validReplications ) );
		
		this.varianceStats.pessimistWealth = Math.sqrt( StatUtils.variance( meanStatsPessimistValues, meanStats.pessimistWealth, 0, validReplications ) );
		this.varianceStats.medianistWealth = Math.sqrt( StatUtils.variance( meanStatsMedianistValues, meanStats.medianistWealth, 0, validReplications ) );
		this.varianceStats.optimistWealth = Math.sqrt( StatUtils.variance( meanStatsOptimistValues, meanStats.optimistWealth, 0, validReplications ) );
		
		for ( int i = 0; i < agentCount; ++i ) {
			Agent templateAgent = this.template.get( i );
			
			if ( templateAgent.getH() < meanStats.i0 ) {
				meanStats.i0Index = i;
			}
			
			if ( templateAgent.getH() < meanStats.i1 ) {
				meanStats.i1Index = i;
			}

			if ( templateAgent.getH() < meanStats.i2 ) {
				meanStats.i2Index = i;
			}
			
			AgentBean medianBean = new AgentBean();
			medianBean.setH( templateAgent.getH() );
			medianBean.setAssets( assertAverage[ i ] / validReplications );
			medianBean.setCash( cashAverage[ i ] / validReplications );
			medianBean.setLoanGiven( loanGivenAverage[ i ] / validReplications );
			medianBean.setLoanTaken( loanTakenAverage[ i ] / validReplications );
			
			meanAgents.add( new Agent( medianBean, this.markets ) );
		}
		
		currentStats.setFinalAgents( meanAgents );
		currentStats.setStats( meanStats );
		currentStats.setStartingTime( this.startingTime );
		currentStats.setEndingTime( new Date() );
		
		return currentStats;
	}
	
	private void writeResults() {
		if ( null == this.currentStats || 0 == this.replicationData.size()  ) {
			return;
		}
		
		Date endingTime = new Date();
		String name = FILENAME_DATE_FORMATTER.format( endingTime );
		if ( null == this.experiment.getName() ) {
			this.experiment.setName( name );
		}
		
		ResultBean resultBean = new ResultBean();
		EquilibriumBean equilibriumMedianBean = new EquilibriumBean( this.currentStats.getStats() );
		EquilibriumBean equilibriumVarianceBean = new EquilibriumBean( this.varianceStats );
		
		List<AgentBean> resultAgents = new ArrayList<AgentBean>();
		Iterator<Agent> agentIter = this.currentStats.getFinalAgents().iterator();
		while ( agentIter.hasNext() ) {
			Agent a = agentIter.next();
			resultAgents.add( new AgentBean( a ) );
		}
		
		int validReplications = 0;
		
		double[] totalTx = new double[ this.replicationData.size() ];
		double[] successfulTx = new double[ this.replicationData.size() ];
		double[] failedTx = new double[ this.replicationData.size() ];
		double[] durations = new double[ this.replicationData.size() ];
		
		List<ReplicationBean> replications = new ArrayList<ReplicationBean>();
		for ( ReplicationData data : this.replicationData ) {
			if ( false == data.isCanceled() ) {
				totalTx[ validReplications ] = data.getTotalTxCount();
				successfulTx[ validReplications ] = data.getTotalTxCount() - data.getFailedTxCount();
				failedTx[ validReplications ] = data.getFailedTxCount();
				durations[ validReplications ] = ( data.getEndingTime().getTime() - data.getStartingTime().getTime() );
				
				validReplications++;
			}
			
			replications.add( new ReplicationBean( data ) );
		}

		resultBean.setAgents( resultAgents );
		resultBean.setEquilibriumMean( equilibriumMedianBean );
		resultBean.setEquilibriumVariance( equilibriumVarianceBean );
		resultBean.setExperiment( this.experiment );
		resultBean.setReplications( replications );
		resultBean.setDuration( (int) (( endingTime.getTime() - this.startingTime.getTime() ) / 1000) );
		resultBean.setMeanTotalTransactions( StatUtils.mean( totalTx, 0, validReplications ) );
		resultBean.setMeanSuccessfulTransactions( StatUtils.mean( successfulTx, 0, validReplications ) );
		resultBean.setMeanFailedTransactions( StatUtils.mean( failedTx, 0, validReplications ) );
		resultBean.setStdTotalTransactions( Math.sqrt( StatUtils.variance( totalTx, 0, validReplications ) ) );
		resultBean.setStdSuccessfulTransactions( Math.sqrt( StatUtils.variance( successfulTx, 0, validReplications ) ) );
		resultBean.setStdFailedTransactions( Math.sqrt( StatUtils.variance( failedTx, 0, validReplications ) ) );
		resultBean.setStartingTime( this.startingTime );
		resultBean.setEndingTime( endingTime );
		resultBean.setMeanDuration( StatUtils.mean( durations, 0, validReplications ) / 1000 );
		resultBean.setMedianMarkets( this.medianMarkets );
		resultBean.setGraph( NetworkExporter.createGraphBean( this.template ) );
		
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance( ResultBean.class );
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			 
		    jaxbMarshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );

		    String fileName = Utils.RESULTS_DIRECTORY.getAbsolutePath()
		    		+ File.separator + name + ".xml";
		    
		    jaxbMarshaller.marshal( resultBean, new File( fileName  ) );
		    
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("rawtypes")
	public class ReplicationTask implements Runnable {
		private int taskId;
		
		private int currentReplication;
		private boolean canceledFlag;
		private boolean nextTxFlag;
		
		private int totalTxCount;
		private int failTxTotalCount;
		private int failTxSuccessiveCount;
		
		private Date replicationStartingTime;
		
		private Future future;

		private AgentNetwork currentAgents;
		
		public ReplicationTask( int taskId ) {
			this.taskId = taskId;
			this.canceledFlag = false;
			this.nextTxFlag = false;
		}
		
		public int getTaskId() {
			return taskId;
		}
		
		public Future getFuture() {
			return future;
		}

		public void setFuture( Future future) {
			this.future = future;
		}

		public int getCurrentReplication() {
			return currentReplication;
		}

		public TerminationMode getTerminationMode() {
			return ReplicationsRunner.this.experiment.getTerminationMode();
		}
		
		public int getTotalTxCount() {
			return totalTxCount;
		}

		public int getFailTxTotalCount() {
			return failTxTotalCount;
		}

		public int getFailTxSuccessiveCount() {
			return failTxSuccessiveCount;
		}
		
		public int getMaxTx() {
			return ReplicationsRunner.this.experiment.getMaxTx();
		}

		public List<Agent> getAgents() {
			return this.currentAgents.getOrderedList();
		}
		
		public void cancel() {
			this.canceledFlag = true;
		}
		
		public void nextReplication() {
			this.nextTxFlag = true;
		}
		
		@Override
		public void run() {
			while ( true ) {
				if ( this.canceledFlag ) {
					break;
				}
				
				int nextReplication = ReplicationsRunner.this.replications.getAndDecrement();
				if ( nextReplication <= 0 ) {
					break;
				}
				
				this.currentReplication = experiment.getReplications() - nextReplication + 1;
				// creates a deep copy of the network, need for parallel execution
				this.currentAgents = new AgentNetwork( ReplicationsRunner.this.template );
				Auction auction = new Auction( this.currentAgents );
				
				this.replicationStartingTime = new Date();
				
				ReplicationData data = this.calculateReplication( auction );
				if ( null != data ) {
					// setting final agents here, as tx.getFinalAgents could return null
					data.setFinalAgents( this.currentAgents.getOrderedList() );
					ReplicationsRunner.this.replicationFinished( data );
				}
			}
		}
		
		private ReplicationData calculateReplication( Auction auction ) {
			boolean terminated = false;
			
			this.totalTxCount = 0;
			this.failTxTotalCount = 0;
			this.failTxSuccessiveCount = 0;
			List<MarketType> successfulMarkets = new ArrayList<MarketType>();
			
			while ( true ) {
				Transaction tx = auction.executeSingleTransaction( MatchingType.BEST_NEIGHBOUR, false );
				
				this.totalTxCount++;
				
				if ( false == tx.wasSuccessful() ) {
					this.failTxTotalCount++;
					this.failTxSuccessiveCount++;
					
				} else {
					this.failTxSuccessiveCount = 0;
					successfulMarkets.add( tx.getMatch().getMarket() );
					
				}

				if ( TerminationMode.TOTAL_TX == ReplicationsRunner.this.experiment.getTerminationMode() ) {
					terminated = this.totalTxCount >= ReplicationsRunner.this.experiment.getMaxTx();
					
				} else if ( TerminationMode.FAIL_TOTAL_TX == ReplicationsRunner.this.experiment.getTerminationMode() ) {
					terminated = this.failTxTotalCount >= ReplicationsRunner.this.experiment.getMaxTx();
				
				} else if ( TerminationMode.FAIL_SUCCESSIVE_TX == ReplicationsRunner.this.experiment.getTerminationMode() ) {
					terminated = this.failTxSuccessiveCount >= ReplicationsRunner.this.experiment.getMaxTx();
				
				}
				
				if ( this.nextTxFlag ) {
					terminated = true;
				}
				
				if ( this.canceledFlag ) {
					terminated = true;
				}
				
				// if trading has halted terminate any way, makes no more sense to continue
				if ( tx.hasTradingHalted() ) {
					terminated = true;
				}
				
				if ( terminated ) {  
					ReplicationData data = new ReplicationData();
					data.setEndingTime( new Date() );
					data.setStartingTime( this.replicationStartingTime );
					data.setStats( auction.calculateEquilibriumStats() );
					data.setTradingHalted( tx.hasTradingHalted() );
					data.setCanceled( this.canceledFlag || this.nextTxFlag );
					data.setNumber( this.currentReplication );
					data.setTaskId( this.taskId );
					data.setTotalTxCount( this.totalTxCount );
					data.setFailedTxCount( this.failTxTotalCount );
					data.setSuccessfulMarkets( successfulMarkets );
					
					this.nextTxFlag = false;
					
					return data;
				}
			}
		}
	}
}
