package backend.replications;

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

import backend.Auction;
import backend.Auction.EquilibriumStatistics;
import backend.Auction.MatchingType;
import backend.agents.Agent;
import backend.agents.network.AgentNetwork;
import backend.markets.Markets;
import backend.tx.Transaction;
import frontend.experimenter.xml.experiment.ExperimentBean;
import frontend.experimenter.xml.result.AgentBean;
import frontend.experimenter.xml.result.EquilibriumBean;
import frontend.experimenter.xml.result.ReplicationBean;
import frontend.experimenter.xml.result.ResultBean;
import frontend.replication.ReplicationData;

public class ReplicationsRunner {

	private Date startingTime;
	
	private ExecutorService replicationTaskExecutor;
	
	private boolean canceled;
	
	private List<ReplicationTask> replicationTasks;
	private Thread awaitFinishThread;
	
	private ReplicationData currentStats;
	
	private List<ReplicationData> replicationData;

	private AgentNetwork template;
	private Markets markets;
	private ReplicationsListener listener;
	
	private AtomicInteger replications;
	
	private ExperimentBean experiment;
	
	private final static SimpleDateFormat FILENAME_DATE_FORMATTER = new SimpleDateFormat( "yyyyMMdd_HHmmss" );
	private final static String REPLICATIONS_DIR_NAME = "replications/";
	
	public interface ReplicationsListener {
		public void replicationFinished( ReplicationData data, ReplicationData averageData );
		public void allReplicationsFinished();
	}
	
	public enum TerminationMode {
		TOTAL_TX,
		FAIL_TOTAL_TX,
		FAIL_SUCCESSIVE_TX,
		TRADING_HALTED
	}

	public ReplicationsRunner( AgentNetwork template, Markets markets ) {
		this.replicationTasks = new ArrayList<>();
		this.replicationData = new ArrayList<>();
		
		this.template = template;
		this.markets = markets;
	}
	
	public boolean isRunning() {
		return this.awaitFinishThread != null;
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
	
	@SuppressWarnings("rawtypes")
	public void start( ExperimentBean experiment, ReplicationsListener listener ) {
		// replications already running
		if ( this.isRunning() ) {
			return;
		}
		
		this.canceled = false;
		this.startingTime = new Date();
		this.experiment = experiment;
		
		this.listener = listener;
		this.replications = new AtomicInteger( experiment.getReplications() );
		// always do parallel-processing
		int threadCount = Math.max( 1, Runtime.getRuntime().availableProcessors() - 1 ); // leave one for GUI-purposes, otherwise would freeze
		// if less replications than threads, then limit thread-count by replication-count
		threadCount = Math.min( threadCount, experiment.getReplications() );

		this.replicationTaskExecutor = Executors.newFixedThreadPool( threadCount, new ThreadFactory() {
			public Thread newThread( Runnable r ) {
				return new Thread( r, "Replication-Thread" );
			}
		});
		
		for ( int i = 0; i < threadCount; ++i ) {
			ReplicationTask task = new ReplicationTask( i );
			
			Future future = this.replicationTaskExecutor.submit( task );
			task.setFuture( future );

			this.replicationTasks.add( task );
		}
		
		this.awaitFinishThread = new Thread( new Runnable() {
			@Override
			public void run() {
				for ( ReplicationTask task : replicationTasks ) {
					try {
						task.getFuture().get();
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
				}
				
				if ( false == canceled ) {
					listener.allReplicationsFinished();
				}
				
				ReplicationsRunner.this.cleanUp();
			}
		} );
		
		this.awaitFinishThread.setName( "Replications finished wait-thread" );
		this.awaitFinishThread.start();
	}
	
	public void stop() {
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
		
		this.listener.replicationFinished( data, this.currentStats );
	}
	
	private ReplicationData calculateStatistics() {
		int agentCount = this.template.size();
		
		ReplicationData currentStats = new ReplicationData();
		List<Agent> meanAgents = new ArrayList<>( agentCount );
		EquilibriumStatistics meanStats = new EquilibriumStatistics();
		
		int validReplications = 0;
		double[] cashAverage = new double[ agentCount ];
		double[] assertAverage = new double[ agentCount ];
		double[] loanGivenAverage = new double[ agentCount ];
		double[] loanTakenAverage = new double[ agentCount ];
		
		// TODO: calculate the standard deviation
		
		for ( ReplicationData data : this.replicationData ) {
			if ( data.isCanceled() ) {
				continue;
			}
			
			List<Agent> finalAgents = data.getFinalAgents();
			
			for ( int i = 0; i < finalAgents.size(); ++i ) {
				Agent a = finalAgents.get( i );
				
				cashAverage[ i ] += a.getCash();
				assertAverage[ i ] += a.getAssets();
				loanGivenAverage[ i ] += a.getLoansGiven();
				loanTakenAverage[ i ] += a.getLoansTaken();
			}
			
			meanStats.p += data.getStats().p;
			meanStats.q += data.getStats().q;
			meanStats.pq += data.getStats().pq;
			
			meanStats.i0 += data.getStats().i0;
			meanStats.i1 += data.getStats().i1;
			meanStats.i2 += data.getStats().i2;
			
			meanStats.P += data.getStats().P;
			meanStats.M += data.getStats().M;
			meanStats.O += data.getStats().O;
			
			validReplications++;
		}
		
		// no valid replications so far: no current data
		if ( 0 == validReplications ) {
			return null;
		}
		
		meanStats.p /= validReplications;
		meanStats.q /= validReplications;
		meanStats.pq /= validReplications;
		
		meanStats.i0 /= validReplications;
		meanStats.i1 /= validReplications;
		meanStats.i2 /= validReplications;
		
		meanStats.P /= validReplications;
		meanStats.M /= validReplications;
		meanStats.O /= validReplications;
		
		for ( int i = 0; i < agentCount; ++i ) {
			Agent templateAgent = this.template.get( i );
			
			if ( templateAgent.getH() < meanStats.i0 ) {
				meanStats.i0Index = i;
			}
			
			if ( templateAgent.getH() < meanStats.i1 ) {
				meanStats.i1Index = i;
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
		EquilibriumBean equilibriumBean = new EquilibriumBean( this.currentStats.getStats() );
		
		List<AgentBean> resultAgents = new ArrayList<AgentBean>();
		Iterator<Agent> agentIter = this.currentStats.getFinalAgents().iterator();
		while ( agentIter.hasNext() ) {
			Agent a = agentIter.next();
			resultAgents.add( new AgentBean( a ) );
		}
		
		double meanTotalTx = 0.0;
		double meanFailedTx = 0.0;
		double meanDuration = 0.0;
		int validReplications = 0;
		
		List<ReplicationBean> replications = new ArrayList<ReplicationBean>();
		for ( ReplicationData data : this.replicationData ) {
			if ( false == data.isCanceled() ) {
				meanTotalTx += data.getTotalTxCount();
				meanFailedTx += data.getFailedTxCount();
				meanDuration += ( data.getEndingTime().getTime() - data.getStartingTime().getTime() );
				validReplications++;
			}
			
			replications.add( new ReplicationBean( data ) );
		}
		
		meanTotalTx /= validReplications;
		meanFailedTx /= validReplications;
		meanDuration /= ( validReplications * 1000 );
		
		resultBean.setAgents( resultAgents );
		resultBean.setEquilibrium( equilibriumBean );
		resultBean.setExperiment( this.experiment );
		resultBean.setReplications( replications );
		resultBean.setDuration( (int) (( endingTime.getTime() - this.startingTime.getTime() ) / 1000) );
		resultBean.setMeanTotalTransactions( meanTotalTx );
		resultBean.setMeanFailedTransactions( meanFailedTx );
		resultBean.setStartingTime( this.startingTime );
		resultBean.setEndingTime( endingTime );
		resultBean.setMeanDuration( meanDuration );
		
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance( ResultBean.class );
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			 
		    jaxbMarshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );

		    jaxbMarshaller.marshal( resultBean, new File( REPLICATIONS_DIR_NAME + name + ".xml"  ) );
		    
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
			
			while ( true ) {
				Transaction tx = auction.executeSingleTransaction( MatchingType.BEST_NEIGHBOUR, false );
				
				this.totalTxCount++;
				
				if ( false == tx.wasSuccessful() ) {
					this.failTxTotalCount++;
					this.failTxSuccessiveCount++;
					
				} else {
					this.failTxSuccessiveCount = 0;
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
					
					this.nextTxFlag = false;
					
					return data;
				}
			}
		}
	}
}
