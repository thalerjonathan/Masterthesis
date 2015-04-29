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

	private int replications;
	private TerminationMode terminationMode;
	private int maxTx;
	
	private AgentNetwork template;
	private Markets markets;
	private ReplicationsListener listener;
	
	private AtomicInteger replicationsLeft;
	
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
		TRADING_HALTED,
		EQUILIBRIUM
	}
	
	public List<ReplicationTask> getReplicationTasks() {
		// TODO: sync?
		return this.replicationTasks;
	}
	
	public ReplicationsRunner( AgentNetwork template, Markets markets ) {
		this.replicationTasks = new ArrayList<>();
		this.replicationData = new ArrayList<>();
		
		this.template = template;
		this.markets = markets;
	}
	
	@SuppressWarnings("rawtypes")
	// TODO: refactoring: use ExperimentBean
	public void start( int replications, TerminationMode terminationMode, int maxTx, ReplicationsListener listener ) {
		// replications already running
		if ( null != this.awaitFinishThread ) {
			return;
		}
		
		this.canceled = false;
		this.startingTime = new Date();
		this.replications = replications;
		this.terminationMode = terminationMode;
		this.maxTx = maxTx;
		this.listener = listener;
		this.replicationsLeft = new AtomicInteger( replications );
		// always do parallel-processing
		int threadCount = Math.max( 1, Runtime.getRuntime().availableProcessors() - 1 ); // leave one for GUI-purposes, otherwise would freeze
		// if less replications than threads, then limit thread-count by replication-count
		threadCount = Math.min( threadCount, replications );
		

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
	
	private void cleanUp() {
		this.writeResults();
		
		this.replicationTaskExecutor.shutdown();
		this.awaitFinishThread = null;
	}
	
	public void stop() {
		// no replications running
		if ( null == this.awaitFinishThread ) {
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
		
		this.replicationTasks.clear();
	}

	public Date getStartingTime() {
		return this.startingTime;
	}
	
	public boolean hasFinishedReplications() {
		return this.replicationData.size() > 0;
	}
	
	public ReplicationData getCurrentStats() {
		return this.currentStats;
	}
	
	public int getReplicationsLeft() {
		return this.replications - this.replicationData.size();
	}
	
	private void replicationFinished( ReplicationData data ) {
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
		double[] medianConsumEndow = new double[ agentCount ];
		double[] medianAssetEndow = new double[ agentCount ];
		double[] medianLoanEndow = new double[ agentCount ];
		double[] medianLoanGivenEndow = new double[ agentCount ];
		double[] medianLoanTakenEndow = new double[ agentCount ];
		
		for ( ReplicationData data : this.replicationData ) {
			if ( data.isCanceled() ) {
				continue;
			}
			
			List<Agent> finalAgents = data.getFinalAgents();
			
			for ( int i = 0; i < finalAgents.size(); ++i ) {
				Agent a = finalAgents.get( i );
				
				medianConsumEndow[ i ] += a.getConumEndow();
				medianAssetEndow[ i ] += a.getAssetEndow();
				medianLoanEndow[ i ] += a.getLoan();
				medianLoanGivenEndow[ i ] += a.getLoanGiven();
				medianLoanTakenEndow[ i ] += a.getLoanTaken();
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
			
			double agentMCE = medianConsumEndow[ i ] / validReplications;
			double agentMAE = medianAssetEndow[ i ] / validReplications;
			double agentMLE = medianLoanEndow[ i ] / validReplications;
			double agentMLG = medianLoanGivenEndow[ i ] / validReplications;
			double agentMLT = medianLoanTakenEndow[ i ] / validReplications;
			
			Agent medianAgent = new Agent( templateAgent.getId(), templateAgent.getH(), this.markets ) {
				@Override
				public double getConumEndow() {
					return agentMCE;
				}
				
				@Override
				public double getAssetEndow() {
					return agentMAE;
				}
				
				@Override
				public double getLoan() {
					return agentMLE;
				}
				
				@Override
				public double getLoanGiven() {
					return agentMLG;
				}
				
				@Override
				public double getLoanTaken() {
					return agentMLT;
				}
			};
			
			meanAgents.add( medianAgent );
		}
		
		currentStats.setFinalAgents( meanAgents );
		currentStats.setStats( meanStats );

		return currentStats;
	}
	
	private void writeResults() {
		if ( null == this.currentStats || this.canceled ||  0 == this.replicationData.size()  ) {
			return;
		}
		
		String name = FILENAME_DATE_FORMATTER.format( new Date() );
		ResultBean resultBean = new ResultBean();
		ExperimentBean experimentBean = new ExperimentBean();
		EquilibriumBean equilibriumBean = new EquilibriumBean( this.currentStats.getStats() );
		
		experimentBean.setName( name );
		experimentBean.setAgentCount( this.currentStats.getFinalAgents().size() );
		experimentBean.setFaceValue( this.markets.V() );
		experimentBean.setTopology( this.template.getNetworkName() );
		experimentBean.setAssetLoanMarket( this.markets.isABM() );
		experimentBean.setLoanCashMarket( this.markets.isLoanMarket() );
		experimentBean.setBondsPledgeability( this.markets.isBP() );
		//experimentBean.setImportanceSampling( this.importanceSamplingCheck.isSelected() ); // TODO
		experimentBean.setTerminationMode( this.terminationMode );
		experimentBean.setMaxTx( this.maxTx );
		experimentBean.setReplications( this.replications );
		
		List<AgentBean> resultAgents = new ArrayList<AgentBean>();
		Iterator<Agent> agentIter = this.currentStats.getFinalAgents().iterator();
		while ( agentIter.hasNext() ) {
			Agent a = agentIter.next();
			resultAgents.add( new AgentBean( a ) );
		}
		
		List<ReplicationBean> replications = new ArrayList<ReplicationBean>();
		for ( ReplicationData data : this.replicationData ) {
			replications.add( new ReplicationBean( data ) );
		}
		
		resultBean.setAgents( resultAgents );
		resultBean.setEquilibrium( equilibriumBean );
		resultBean.setExperiment( experimentBean );
		resultBean.setReplications( replications );
		
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance( ResultBean.class );
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			 
		    jaxbMarshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );

		    //Marshal the employees list in file
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
			return terminationMode;
		}
		
		public int getTotalTxCount() {
			return totalTxCount;
		}

		public int getFailTxCount() {
			return failTxTotalCount;
		}

		public int getMaxTx() {
			return maxTx;
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
				
				if ( ( this.currentReplication = ReplicationsRunner.this.replicationsLeft.getAndDecrement() ) <= 0 ) {
					break;
				}
				
				// creates a deep copy of the network, need for parallel execution
				this.currentAgents = new AgentNetwork( ReplicationsRunner.this.template );
				Auction auction = new Auction( this.currentAgents );
				
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

				if ( TerminationMode.TOTAL_TX == ReplicationsRunner.this.terminationMode ) {
					terminated = this.totalTxCount >= ReplicationsRunner.this.maxTx;
					
				} else if ( TerminationMode.FAIL_TOTAL_TX == ReplicationsRunner.this.terminationMode ) {
					terminated = this.failTxTotalCount >= ReplicationsRunner.this.maxTx;
				
				} else if ( TerminationMode.FAIL_SUCCESSIVE_TX == ReplicationsRunner.this.terminationMode ) {
					terminated = this.failTxSuccessiveCount >= ReplicationsRunner.this.maxTx;
				
				} else if ( TerminationMode.EQUILIBRIUM == ReplicationsRunner.this.terminationMode && tx.isEquilibrium() ) {
					terminated = true;
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
					data.setStats( auction.calculateEquilibriumStats() );
					data.setTradingHalted( tx.hasTradingHalted() );
					data.setEquilibrium( tx.isEquilibrium() );
					data.setCanceled( this.canceledFlag || this.nextTxFlag );
					data.setNumber( ReplicationsRunner.this.replications - this.currentReplication + 1 );
					data.setTaskId( this.taskId );
					data.setTxCount( this.totalTxCount );
					
					this.nextTxFlag = false;
					
					return data;
				}
			}
		}
	}
}
