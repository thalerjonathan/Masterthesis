package frontend.replication;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import backend.Auction;
import backend.Auction.MatchingType;
import backend.agents.Agent;
import backend.agents.AgentFactoryImpl;
import backend.agents.network.AgentConnection;
import backend.agents.network.AgentNetwork;
import backend.markets.Markets;
import backend.tx.Transaction;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import frontend.agentInfo.AgentInfoFrame;
import frontend.inspection.NetworkVisualisationFrame;
import frontend.networkCreators.AscendingConnectedCreator;
import frontend.networkCreators.AscendingFullShortcutsCreator;
import frontend.networkCreators.AscendingRandomShortcutsCreator;
import frontend.networkCreators.AscendingRegularShortcutsCreator;
import frontend.networkCreators.BarbasiAlbertCreator;
import frontend.networkCreators.ErdosRenyiCreator;
import frontend.networkCreators.FullyConnectedCreator;
import frontend.networkCreators.HalfFullyConnectedCreator;
import frontend.networkCreators.HubConnectedCreator;
import frontend.networkCreators.INetworkCreator;
import frontend.networkCreators.MaximumHubCreator;
import frontend.networkCreators.MedianHubCreator;
import frontend.networkCreators.ThreeMedianHubsCreator;
import frontend.networkCreators.WattStrogatzCreator;
import frontend.networkVisualisation.NetworkRenderPanel;
import frontend.replication.info.ReplicationInfoFrame;
import frontend.visualisation.WealthVisualizer;

@SuppressWarnings( value = {"serial", "rawtypes" } )
public class ReplicationPanel extends JPanel {

	private Markets markets;
	private AgentNetwork agentNetworkTemplate;
	
	private JCheckBox abmMarketCheck;
	private JCheckBox loanCashMarketCheck;
	private JCheckBox bpMechanismCheck;
	
	private JCheckBox parallelEvaluationCheck;
	
	private JComboBox<INetworkCreator> topologySelection;
	private JComboBox<TerminationMode> terminationSelection;
	
	private JSpinner agentCountSpinner;
	private JSpinner faceValueSpinner;
	private JSpinner replicationCountSpinner;
	private JSpinner maxTxSpinner;
	
	private JButton replicationButton;
	private JButton showNetworkButton;
	private JButton showAgentInfoButton;
	private JButton showReplicationInfoButton;
	
	private JLabel runningTimeLabel;
	
	private AgentInfoFrame agentInfoFrame;
	private ReplicationInfoFrame replicationInfoFrame;
	
	private WealthVisualizer agentWealthPanel;
	private NetworkVisualisationFrame netVisFrame;
	
	private Timer spinnerChangedTimer;
	
	private ReplicationTable replicationTable;
	
	private ExecutorService replicationTaskExecutor;
	private List<ReplicationTask> replicationTasks;
	private Thread awaitFinishThread;
	
	private List<ReplicationData> replicationData;
	
	private Date startingTime;

	public final static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat( "dd.MM HH:mm:ss" );
	
	public enum TerminationMode {
		MAX_TOTAL_TX,
		MAX_FAIL_TX,
		EQUILIBRIUM
	}
	
	public ReplicationPanel() {
		this.markets = new Markets();

		this.replicationTasks = new ArrayList<>();
		this.replicationData = new ArrayList<>();
		
		this.setLayout( new BorderLayout() );
		
		this.createControls();
		this.createAgents();
	}
	
	private void createControls() {
		// creating controls
		this.agentWealthPanel = new WealthVisualizer();
		
		this.abmMarketCheck = new JCheckBox( "Asset/Loan Market" );
		this.loanCashMarketCheck = new JCheckBox( "Loan/Cash Market" );
		this.bpMechanismCheck = new JCheckBox( "Bonds Pledgeability" );
		
		this.parallelEvaluationCheck = new JCheckBox( "Parallel Evaluation" );
		
		this.topologySelection = new JComboBox<INetworkCreator>();
		this.terminationSelection = new JComboBox<TerminationMode>( TerminationMode.values() );
		
		this.replicationButton = new JButton( "Start Replications" );
		this.showNetworkButton = new JButton( "Show Network" );
		this.showAgentInfoButton = new JButton( "Show Agent-Info" );
		this.showReplicationInfoButton = new JButton( "Show Replication-Info" );
		
		this.agentCountSpinner = new JSpinner( new SpinnerNumberModel( 30, 10, 1000, 10 ) );
		this.faceValueSpinner = new JSpinner( new SpinnerNumberModel( 0.5, 0.1, 1.0, 0.1 ) );
		this.replicationCountSpinner = new JSpinner( new SpinnerNumberModel( 4, 1, 100, 1 ) );
		this.maxTxSpinner = new JSpinner( new SpinnerNumberModel( 1_000_000, 1, 1_000_000_000, 100_000 ) );
		
		this.runningTimeLabel = new JLabel( "Running since: -" );
		
		this.replicationTable = new ReplicationTable();
		JScrollPane txHistoryScrollPane = new JScrollPane( this.replicationTable );
		txHistoryScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
		txHistoryScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		
		// setting properties
		this.replicationTable.getSelectionModel().addListSelectionListener( new ListSelectionListener() {
			@Override
			public void valueChanged( ListSelectionEvent e ) {
				if (e.getValueIsAdjusting() == false) {
					int rowIndex = ReplicationPanel.this.replicationTable.getSelectedRow();
					if ( -1 == rowIndex ) {
						return;
					}
					
					int modelIndex = ReplicationPanel.this.replicationTable.getRowSorter().convertRowIndexToModel( rowIndex );
					
					ReplicationData data = ReplicationPanel.this.replicationData.get( modelIndex );
					ReplicationPanel.this.agentWealthPanel.setAgents( data.getFinalAgents() );
		        }
			}
		});
		
		this.topologySelection.addItem( new FullyConnectedCreator() );
		this.topologySelection.addItem( new HalfFullyConnectedCreator() );
		this.topologySelection.addItem( new AscendingConnectedCreator() );
		this.topologySelection.addItem( new AscendingFullShortcutsCreator() );
		this.topologySelection.addItem( new AscendingRegularShortcutsCreator() );
		this.topologySelection.addItem( new AscendingRandomShortcutsCreator() );
		this.topologySelection.addItem( new HubConnectedCreator() );
		this.topologySelection.addItem( new MedianHubCreator() );
		this.topologySelection.addItem( new MaximumHubCreator() );
		this.topologySelection.addItem( new ThreeMedianHubsCreator() );
		this.topologySelection.addItem( new ErdosRenyiCreator() );
		this.topologySelection.addItem( new BarbasiAlbertCreator() );
		this.topologySelection.addItem( new WattStrogatzCreator() );
		
		this.abmMarketCheck.setSelected( this.markets.isABM() );
		this.loanCashMarketCheck.setSelected( this.markets.isLoanMarket() );
		this.bpMechanismCheck.setSelected( this.markets.isBP() );
		
		this.parallelEvaluationCheck.setSelected( true );
		
		ActionListener checkListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if ( false == ReplicationPanel.this.abmMarketCheck.isSelected() ) {
					ReplicationPanel.this.loanCashMarketCheck.setSelected( false );
					ReplicationPanel.this.bpMechanismCheck.setSelected( false );
				}
			}
		};
		
		this.bpMechanismCheck.addActionListener( checkListener );
		this.abmMarketCheck.addActionListener( checkListener );
		this.loanCashMarketCheck.addActionListener( checkListener );
		
		this.replicationButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ReplicationPanel.this.toggleReplication();
			}
		});
		
		this.topologySelection.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				INetworkCreator newSelected = (INetworkCreator) ReplicationPanel.this.topologySelection.getSelectedItem();

				// creator signals to be created immediately
				if ( newSelected.createInstant() ) {
					ReplicationPanel.this.createAgents();
				
				// creator signals to defer creation for later (e.g. after user-input of parameters
				// the creator needs)
				} else {
					// defer creation and provide creator with a callback to continue creation
					newSelected.deferCreation( new Runnable() {
						@Override
						public void run() {
							ReplicationPanel.this.createAgents();
						}
					});
				}
			}
		});
		
		ChangeListener spinnerChanged = new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				TimerTask task = new TimerTask() {
					@Override
					public void run() {
						// we are running inside a thread => need to invoke SwingUtilities.invokeLater !
						SwingUtilities.invokeLater( new Runnable() {
							@Override
							public void run() {
								ReplicationPanel.this.createAgents();
							}
						});
					}
				};
				
				// cancel already scheduled timer
				if ( null != ReplicationPanel.this.spinnerChangedTimer ) {
					ReplicationPanel.this.spinnerChangedTimer.cancel();
					ReplicationPanel.this.spinnerChangedTimer.purge();
				}
				
				// schedule a recreation of the agents after 500ms
				ReplicationPanel.this.spinnerChangedTimer = new Timer();
				ReplicationPanel.this.spinnerChangedTimer.schedule( task, 500 );
			}
		};
		
		this.agentCountSpinner.addChangeListener( spinnerChanged );
		this.faceValueSpinner.addChangeListener( spinnerChanged );
		
		this.showNetworkButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if ( null == ReplicationPanel.this.netVisFrame ) {
					ReplicationPanel.this.netVisFrame = new NetworkVisualisationFrame();
					ReplicationPanel.this.updateNetworkVisualisationFrame();
				}
				
				ReplicationPanel.this.netVisFrame.setVisible( true );
			}
		});

		this.showAgentInfoButton.addActionListener( new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				ReplicationPanel.this.showAgentInfoFrame();
			}
		});
		
		showReplicationInfoButton.setEnabled( false );
		this.showReplicationInfoButton.addActionListener( new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				ReplicationPanel.this.showReplicationInfo();
			}
		});
		
		// adding components ////////////////////////////////////
		JPanel controlsPanel = new JPanel( new GridBagLayout() );
		JPanel agentsConfigPanel = new JPanel();
		JPanel replicationsConfigPanel = new JPanel();
		
		agentsConfigPanel.add( this.faceValueSpinner );
		agentsConfigPanel.add( this.agentCountSpinner );
		agentsConfigPanel.add( this.topologySelection );
		agentsConfigPanel.add( this.abmMarketCheck );
		agentsConfigPanel.add( this.loanCashMarketCheck );
		agentsConfigPanel.add( this.bpMechanismCheck );	
		agentsConfigPanel.add( this.parallelEvaluationCheck );	
		agentsConfigPanel.add( this.replicationButton );

		replicationsConfigPanel.add( this.maxTxSpinner );
		replicationsConfigPanel.add( this.terminationSelection );
		replicationsConfigPanel.add( this.replicationCountSpinner );
		replicationsConfigPanel.add( this.showNetworkButton );
		replicationsConfigPanel.add( this.showAgentInfoButton );
		replicationsConfigPanel.add( this.showReplicationInfoButton );
		replicationsConfigPanel.add( this.runningTimeLabel );
		
		this.agentWealthPanel.setSize( this.getSize() );
		
		GridBagConstraints c = new GridBagConstraints();
		
		c.gridy = 0;
		controlsPanel.add( agentsConfigPanel, c );
		c.gridy = 1;
		controlsPanel.add( replicationsConfigPanel, c );

		this.add( controlsPanel, BorderLayout.NORTH );
		this.add( this.agentWealthPanel, BorderLayout.CENTER );
		this.add( txHistoryScrollPane, BorderLayout.SOUTH );
	}
	
	private void showReplicationInfo() {
		if ( null == this.replicationInfoFrame ) {
			this.replicationInfoFrame = new ReplicationInfoFrame();
		}
		
		this.replicationInfoFrame.setTasks( this.replicationTasks );
		this.replicationInfoFrame.setVisible( true );
	}

	private void showAgentInfoFrame() {
		if ( null == this.agentInfoFrame ) {
			this.agentInfoFrame = new AgentInfoFrame();
		}
		
		List<Agent> selectedAgents = null;
		int selectedRow = this.replicationTable.getSelectedRow();
		
		if ( selectedRow >= 0 ) {
			int modelIndex = ReplicationPanel.this.replicationTable.getRowSorter().convertRowIndexToModel( selectedRow );
			selectedAgents = this.replicationData.get( modelIndex ).getFinalAgents();
		} else {
			selectedAgents = this.agentNetworkTemplate.getOrderedList();
		}
		
		this.agentInfoFrame.setAgents( selectedAgents );
		this.agentInfoFrame.setVisible( true );
	}
	
	private void toggleReplication() {
		if ( null == this.awaitFinishThread ) {
			this.replicationButton.setText( "Terminate" );
			this.abmMarketCheck.setEnabled( false );
			this.loanCashMarketCheck.setEnabled( false );
			this.bpMechanismCheck.setEnabled( false );
			this.parallelEvaluationCheck.setEnabled( false );
			this.agentCountSpinner.setEnabled( false );
			this.maxTxSpinner.setEnabled( false );
			this.faceValueSpinner.setEnabled( false );
			this.topologySelection.setEnabled( false );
			this.terminationSelection.setEnabled( false );
			this.replicationCountSpinner.setEnabled( false );
			this.showReplicationInfoButton.setEnabled( true );
			
			this.replicationTable.clearAll();
			this.replicationData.clear();
			
			this.agentWealthPanel.setAgents( this.agentNetworkTemplate.getOrderedList() );
			
			int replicationThreadCount = 1;
			AtomicInteger replicationCount = new AtomicInteger( (int) this.replicationCountSpinner.getValue() );
			
			if ( this.parallelEvaluationCheck.isSelected() ) {
				replicationThreadCount = Runtime.getRuntime().availableProcessors();
				//replicationThreadCount = Runtime.getRuntime().availableProcessors() - 1; // leave one free for other tasks
			}
			
			replicationThreadCount = Math.min( replicationCount.get(), replicationThreadCount );
			
			this.replicationTaskExecutor = Executors.newFixedThreadPool( replicationThreadCount, new ThreadFactory() {
				public Thread newThread( Runnable r ) {
					return new Thread( r, "Replication-Thread" );
				}
			});
	
			
			this.startingTime = new Date();
			
			for ( int i = 0; i < replicationThreadCount; ++i ) {
				ReplicationTask task = new ReplicationTask( i, replicationCount, 
						( TerminationMode ) this.terminationSelection.getSelectedItem(), 
						( int ) this.maxTxSpinner.getValue() );
				
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
					
					// do calculations also only when replications were canceled
					ReplicationPanel.this.allReplicationsFinished();
				}
			} );
			this.awaitFinishThread.setName( "Replications finished wait-thread" );
			this.awaitFinishThread.start();
			
		} else {
			for ( ReplicationTask task : this.replicationTasks ) {
				task.cancel();
			}
			
			try {
				this.awaitFinishThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void allReplicationsFinished() {
		this.updateRunningTimeLabel( System.currentTimeMillis() );
		
		int agentCount = this.agentNetworkTemplate.size();
		List<Agent> medianAgents = new ArrayList<>();
		
		ReplicationData finalData = new ReplicationData();
		finalData.setNumber( -1 );
		finalData.setTaskId( -1 );
		finalData.setTxCount( 0 );
		finalData.setFinalAgents( medianAgents );

		double[] medianConsumEndow = new double[ agentCount ];
		double[] medianAssetEndow = new double[ agentCount ];
		double[] medianLoanEndow = new double[ agentCount ];
		double[] medianLoanGivenEndow = new double[ agentCount ];
		double[] medianLoanTakenEndow = new double[ agentCount ];
		
		for ( ReplicationData data : this.replicationData ) {
			List<Agent> finalAgents = data.getFinalAgents();
			
			for ( int i = 0; i < finalAgents.size(); ++i ) {
				Agent a = finalAgents.get( i );
				
				medianConsumEndow[ i ] += a.getConumEndow();
				medianAssetEndow[ i ] += a.getAssetEndow();
				medianLoanEndow[ i ] += a.getLoan();
				medianLoanGivenEndow[ i ] += a.getLoanGiven();
				medianLoanTakenEndow[ i ] += a.getLoanTaken();
			}
		}
		
		for ( int i = 0; i < agentCount; ++i ) {
			Agent templateAgent = this.agentNetworkTemplate.get( i );
			
			double agentMCE = medianConsumEndow[ i ] / this.replicationData.size();
			double agentMAE = medianAssetEndow[ i ] / this.replicationData.size();
			double agentMLE = medianLoanEndow[ i ] / this.replicationData.size();
			double agentMLG = medianLoanGivenEndow[ i ] / this.replicationData.size();
			double agentMLT = medianLoanTakenEndow[ i ] / this.replicationData.size();
			
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
			
			medianAgents.add( medianAgent );
		}
		
		this.agentWealthPanel.setAgents( medianAgents );
		this.replicationData.add( finalData );
		this.replicationTable.addReplication( finalData );
		
		this.replicationButton.setText( "Start Replications" );
		this.abmMarketCheck.setEnabled( true );
		this.loanCashMarketCheck.setEnabled( true );
		this.bpMechanismCheck.setEnabled( true );
		this.parallelEvaluationCheck.setEnabled( true );
		this.agentCountSpinner.setEnabled( true );
		this.maxTxSpinner.setEnabled( true );
		this.faceValueSpinner.setEnabled( true );
		this.topologySelection.setEnabled( true );
		this.terminationSelection.setEnabled( true );
		this.replicationCountSpinner.setEnabled( true );
		this.showReplicationInfoButton.setEnabled( false );
		
		this.awaitFinishThread = null;
		this.replicationTasks.clear();
		
		this.replicationTaskExecutor.shutdown();
	}
	
	private synchronized void replicationFinished( ReplicationData data ) {
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				ReplicationPanel.this.replicationData.add( data );
				ReplicationPanel.this.replicationTable.addReplication( data );
				ReplicationPanel.this.agentWealthPanel.setAgents( data.getFinalAgents() );
				ReplicationPanel.this.updateAgentInfoFrame( data.getFinalAgents() );
			}
		} );
	}
	
	private void createAgents() {
		int agentCount = (int) this.agentCountSpinner.getValue();
		
		this.markets = new Markets( (double) this.faceValueSpinner.getValue() );
		this.markets.setABM( ReplicationPanel.this.abmMarketCheck.isSelected() );
		this.markets.setLoanMarket( ReplicationPanel.this.loanCashMarketCheck.isSelected() );
		this.markets.setBP( ReplicationPanel.this.bpMechanismCheck.isSelected() );
		
		this.replicationData.clear();
		this.replicationTable.clearAll();

		INetworkCreator creator = ( INetworkCreator ) this.topologySelection.getSelectedItem();
		this.agentNetworkTemplate = creator.createNetwork( new AgentFactoryImpl( agentCount, this.markets ) );
		
		List<Agent> agents = this.agentNetworkTemplate.getOrderedList();
		this.agentWealthPanel.setAgents( this.agentNetworkTemplate.getOrderedList() );
		
		this.updateNetworkVisualisationFrame();
		this.updateAgentInfoFrame( agents );
	}
	
	private void updateAgentInfoFrame( List<Agent> agents ) {
		if ( null != this.agentInfoFrame && this.agentInfoFrame.isVisible() ) {
			this.agentInfoFrame.setAgents( agents );
		}
	}
	
	@SuppressWarnings("unchecked")
	private void updateNetworkVisualisationFrame() {
		if ( null != this.netVisFrame && this.netVisFrame.isVisible() ) {
			NetworkRenderPanel networkPanel = ReplicationPanel.this.agentNetworkTemplate.getNetworkRenderingPanel( (Class<? extends Layout<Agent, AgentConnection>>) CircleLayout.class, null );
			this.netVisFrame.setNetworkRenderPanel( networkPanel );
		}
	}
	
	private void updateRunningTimeLabel( long currSysMillis ) {
		long duration = currSysMillis - this.startingTime.getTime();
		this.runningTimeLabel.setText( "Running since " + DATE_FORMATTER.format( this.startingTime ) + ", " + ( duration / 1000 ) + " sec." );
	}
	
	public class ReplicationTask implements Runnable {
		private int taskId;
		
		private int currentReplication;
		private boolean canceledFlag;
		private boolean nextTxFlag;
		
		private int totalTxCount;
		private int failTxCount;
		
		private AtomicInteger replicationCount;

		private Future future;
		
		private TerminationMode terminationMode;
		private int maxTx;
		
		private AgentNetwork currentAgents;
		
		public ReplicationTask( int taskId, AtomicInteger replicationCount, 
				TerminationMode terminationMode, int maxTx ) {
			this.taskId = taskId;
			this.canceledFlag = false;
			this.nextTxFlag = false;
			this.replicationCount = replicationCount;
			
			this.terminationMode = terminationMode;
			this.maxTx = maxTx;
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
			return failTxCount;
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
				
				if (  ( this.currentReplication = this.replicationCount.getAndDecrement() ) <= 0 ) {
					break;
				}
				
				// creates a deep copy of the network, need for parallel execution
				this.currentAgents = new AgentNetwork( ReplicationPanel.this.agentNetworkTemplate );
				Auction auction = new Auction( this.currentAgents );
				
				ReplicationData data = this.calculateReplication( auction );
				if ( null != data ) {
					ReplicationPanel.this.replicationFinished( data );
				}
			}
		}
		
		private ReplicationData calculateReplication( Auction auction ) {
			boolean terminated = false;
			long lastRunningTimeUpdate = 0;
			
			this.totalTxCount = 0;
			this.failTxCount = 0;
			
			while ( true ) {
				Transaction tx = auction.executeSingleTransactionByType( MatchingType.BEST_NEIGHBOUR, false );
				
				this.totalTxCount++;
				
				if ( false == tx.wasSuccessful() ) {
					this.failTxCount++;
				}

				if ( TerminationMode.MAX_TOTAL_TX == this.terminationMode ) {
					terminated = this.totalTxCount >= this.maxTx;
					
				} else if ( TerminationMode.MAX_FAIL_TX == this.terminationMode ) {
					terminated = this.failTxCount >= this.maxTx;
				}
			
				if ( this.nextTxFlag ) {
					terminated = true;
				}
				
				if ( this.canceledFlag ) {
					terminated = true;
				}
				
				if ( tx.isReachedEquilibrium() ) {
					terminated = true;
				}
				
				if ( terminated ) {
					ReplicationData data = new ReplicationData();
					data.setFinishTime( new Date() );
					data.setReachedEquilibrium( tx.isReachedEquilibrium() );
					data.setWasCanceled( this.canceledFlag || this.nextTxFlag );
					data.setNumber( ( int ) ReplicationPanel.this.replicationCountSpinner.getValue() - this.currentReplication + 1 );
					data.setTaskId( this.taskId );
					data.setTxCount( this.totalTxCount );
					data.setFinalAgents( tx.getFinalAgents() );

					this.nextTxFlag = false;
					
					return data;
				}
				
				if ( 0 == this.taskId ) {
					long currSysMillis = System.currentTimeMillis();
					if ( currSysMillis - lastRunningTimeUpdate >= 1000 ) {
						lastRunningTimeUpdate = currSysMillis;
						updateRunningTimeLabel( currSysMillis );
					}
				}
			}
		}
	}
}
