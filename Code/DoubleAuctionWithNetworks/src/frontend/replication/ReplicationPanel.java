package frontend.replication;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
import backend.agents.IAgentFactory;
import backend.agents.network.AgentConnection;
import backend.agents.network.AgentNetwork;
import backend.markets.Markets;
import backend.tx.Transaction;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
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
	
	private JSpinner agentCountSpinner;
	private JSpinner faceValueSpinner;
	private JSpinner replicationCountSpinner;
	
	private JButton replicationButton;
	private JButton showNetworkButton;
	
	private Timer spinnerChangedTimer;
	
	private ReplicationTable replicationTable;
	
	private WealthVisualizer agentWealthPanel;
	private NetworkVisualisationFrame netVisFrame;
	
	private ExecutorService replicationTaskExecutor;
	private List<ReplicationTask> replicationTasks;
	private Thread awaitFinishThread;
	
	private List<ReplicationData> replicationData;
	
	private long startingTime;
	
	public ReplicationPanel() {
		this.markets = new Markets( 0.2, 1.0, 0.5 );

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
		
		this.replicationButton = new JButton( "Start Replications" );
		this.showNetworkButton = new JButton( "Show Network" );
		
		this.agentCountSpinner = new JSpinner( new SpinnerNumberModel( 30, 10, 1000, 10 ) );
		this.faceValueSpinner = new JSpinner( new SpinnerNumberModel( 0.5, 0.1, 1.0, 0.1 ) );
		this.replicationCountSpinner = new JSpinner( new SpinnerNumberModel( 4, 1, 100, 1 ) );

		this.replicationTable = new ReplicationTable();
		JScrollPane txHistoryScrollPane = new JScrollPane( this.replicationTable );
		txHistoryScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
		txHistoryScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		
		// setting properties
		this.replicationTable.getSelectionModel().addListSelectionListener( new ListSelectionListener() {
			@Override
			public void valueChanged( ListSelectionEvent e ) {
				if (e.getValueIsAdjusting() == false) {
					int rowIndex = replicationTable.getSelectedRow();
					if ( -1 == rowIndex ) {
						return;
					}
					
					int replicationNumber = (int) ReplicationPanel.this.replicationTable.getValueAt( rowIndex, 0 );
					if ( -1 == replicationNumber ) {
						replicationNumber = ReplicationPanel.this.replicationData.size();
					}
					ReplicationData data = ReplicationPanel.this.replicationData.get( replicationNumber - 1 );
					
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
				
				ReplicationPanel.this.markets.setABM( ReplicationPanel.this.abmMarketCheck.isSelected() );
				ReplicationPanel.this.markets.setLoanMarket( ReplicationPanel.this.loanCashMarketCheck.isSelected() );
				ReplicationPanel.this.markets.setBP( ReplicationPanel.this.bpMechanismCheck.isSelected() );
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
		
		this.agentCountSpinner.addChangeListener( new ChangeListener() {
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
		} );

		this.showNetworkButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if ( null == ReplicationPanel.this.netVisFrame ) {
					ReplicationPanel.this.netVisFrame = new NetworkVisualisationFrame( new WindowAdapter() {
						@Override
						public void windowClosed( WindowEvent e ) {
							ReplicationPanel.this.netVisFrame = null;
						}
					});
					
					NetworkRenderPanel networkPanel = ReplicationPanel.this.agentNetworkTemplate.getNetworkRenderingPanel( (Class<? extends Layout<Agent, AgentConnection>>) CircleLayout.class, null );
					ReplicationPanel.this.netVisFrame.setNetworkRenderPanel( networkPanel );
				}
			}
		});

		// adding components ////////////////////////////////////
		JPanel controlsPanel = new JPanel();
		
		controlsPanel.add( this.replicationCountSpinner );
		controlsPanel.add( this.faceValueSpinner );
		controlsPanel.add( this.agentCountSpinner );
		controlsPanel.add( this.topologySelection );
		controlsPanel.add( this.abmMarketCheck );
		controlsPanel.add( this.loanCashMarketCheck );
		controlsPanel.add( this.bpMechanismCheck );	
		controlsPanel.add( this.parallelEvaluationCheck );	
		controlsPanel.add( this.replicationButton );
		controlsPanel.add( this.showNetworkButton );
		
		this.agentWealthPanel.setSize( this.getSize() );
		
		this.add( controlsPanel, BorderLayout.NORTH );
		this.add( this.agentWealthPanel, BorderLayout.CENTER );
		this.add( txHistoryScrollPane, BorderLayout.SOUTH );
	}
	
	private void toggleReplication() {
		if ( null == this.awaitFinishThread ) {
			this.replicationButton.setText( "Stop Replications" );
			this.abmMarketCheck.setEnabled( false );
			this.loanCashMarketCheck.setEnabled( false );
			this.bpMechanismCheck.setEnabled( false );
			this.parallelEvaluationCheck.setEnabled( false );
			this.agentCountSpinner.setEnabled( false );
			this.faceValueSpinner.setEnabled( false );
			this.topologySelection.setEnabled( false );
			this.replicationCountSpinner.setEnabled( false );

			this.replicationTable.clearAll();
			this.replicationData.clear();
			
			int replicationThreadCount = 1;
			
			if ( this.parallelEvaluationCheck.isSelected() ) {
				replicationThreadCount = Runtime.getRuntime().availableProcessors();
			}
			
			this.replicationTaskExecutor = Executors.newFixedThreadPool( replicationThreadCount );
			
			AtomicInteger count = new AtomicInteger( (int) this.replicationCountSpinner.getValue() );
			
			this.startingTime = System.currentTimeMillis();
			
			for ( int i = 0; i < replicationThreadCount; ++i ) {
				ReplicationTask task = new ReplicationTask( i, count );
				
				Future future = this.replicationTaskExecutor.submit( task );
				task.setFuture( future );

				this.replicationTasks.add( task );
			}
			
			this.awaitFinishThread = new Thread( new Runnable() {
				@Override
				public void run() {
					ReplicationTask lastTask = null;
					
					for ( ReplicationTask task : replicationTasks ) {
						try {
							lastTask = task;
							task.getFuture().get();
						} catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}
					}
					
					// do calculations only when replications wheren't canceled
					if ( false == lastTask.isCanceled() ) {
						ReplicationPanel.this.allReplicationsFinished();
					}
					
					resetStateForStart();
				}
			} );
			
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
			
			this.resetStateForStart();
		}
	}
	
	private void resetStateForStart() {
		this.replicationButton.setText( "Start Replications" );
		this.abmMarketCheck.setEnabled( true );
		this.loanCashMarketCheck.setEnabled( true );
		this.bpMechanismCheck.setEnabled( true );
		this.parallelEvaluationCheck.setEnabled( true );
		this.agentCountSpinner.setEnabled( true );
		this.faceValueSpinner.setEnabled( true );
		this.topologySelection.setEnabled( true );
		this.replicationCountSpinner.setEnabled( true );

		this.awaitFinishThread = null;
		this.replicationTasks.clear();
		
		this.replicationTaskExecutor.shutdown();
	}

	private void allReplicationsFinished() {
		System.out.println( "All Replications finished after " + ( ( System.currentTimeMillis() -  this.startingTime ) / 1000.0 ) + " sec" );
		
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
	}
	
	private synchronized void replicationFinished( ReplicationData data ) {
		this.replicationData.add( data );
		
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				ReplicationPanel.this.agentWealthPanel.setAgents( data.getFinalAgents() );
				ReplicationPanel.this.replicationTable.addReplication( data );
			}
		});
	}
	
	private void createAgents() {
		int optimismFunctionIndex = 0;
		int agentCount = (int) this.agentCountSpinner.getValue();

		// create agent-factory
		IAgentFactory agentFactory = new IAgentFactory() {
			private int i = 0;
			
			@Override
			public Agent createAgent() {
				Agent a = null;
			
				if ( i <= agentCount ) {
					// linear
					double optimism = ( double ) i  / ( double ) agentCount;
					
					// triangle
					if ( 1 == optimismFunctionIndex ) {
						double halfAgentCount = agentCount / 2.0;
						double totalArea = ( agentCount * halfAgentCount ) / 2.0;
						double halfArea = totalArea / 2.0;
						double agentArea = ( ( ( halfAgentCount - this.i ) * ( halfAgentCount - this.i ) ) / 2.0 );
						
						if ( i <= halfAgentCount ) {
							agentArea = halfArea - agentArea;
							
						} else {
							agentArea = halfArea + agentArea;
						}
						
						optimism = agentArea / totalArea;
					}
					
					a = new Agent( i, optimism, markets );

					this.i++;
				}
				
				return a;
			}
		};
		
		this.replicationData.clear();
		this.replicationTable.clearAll();
		
		INetworkCreator creator = (INetworkCreator) this.topologySelection.getSelectedItem();
		this.agentNetworkTemplate = creator.createNetwork( agentFactory );
		this.agentWealthPanel.setAgents( this.agentNetworkTemplate.getOrderedList() );
		
		if ( null != this.netVisFrame ) {
			NetworkRenderPanel networkPanel = ReplicationPanel.this.agentNetworkTemplate.getNetworkRenderingPanel( (Class<? extends Layout<Agent, AgentConnection>>) CircleLayout.class, null );
			this.netVisFrame.setNetworkRenderPanel( networkPanel );
		}	
	}
	
	private class ReplicationTask implements Runnable {
		private int taskId;
		private int currentReplication;
		private boolean canceledFlag;
		
		private AtomicInteger replicationCount;

		private Future future;
		
		public ReplicationTask( int taskId, AtomicInteger replicationCount ) {
			this.taskId = taskId;
			this.canceledFlag = false;
			this.replicationCount = replicationCount;
		}
		
		public Future getFuture() {
			return future;
		}

		public void setFuture( Future future) {
			this.future = future;
		}

		public void cancel() {
			this.canceledFlag = false;
		}
		
		public boolean isCanceled() {
			return this.canceledFlag;
		}
		
		@Override
		public void run() {
			while ( ( this.currentReplication = this.replicationCount.getAndDecrement() ) > 0 
					&& false == this.canceledFlag ) {
				long ms = System.currentTimeMillis();
				// creates a deep copy of the network, need for parallel execution
				AgentNetwork agents = new AgentNetwork( ReplicationPanel.this.agentNetworkTemplate );
				
				System.out.println( "copying of network took " + ( System.currentTimeMillis() - ms ) + " ms" );
				
				Auction auction = new Auction( agents );
				
				ReplicationData data = this.calculateReplication( auction );
				if ( null != data ) {
					ReplicationPanel.this.replicationFinished( data );
				}
			}
		}
		
		private ReplicationData calculateReplication( Auction auction ) {
			int count = 0;
			
			while ( false == this.canceledFlag ) {
				Transaction tx = auction.executeSingleTransactionByType( MatchingType.BEST_NEIGHBOUR, false );
				count++;
				
				if ( count >= 20_000 || tx.isReachedEquilibrium() ) {
					ReplicationData data = new ReplicationData();
					data.setNumber( ( int ) ReplicationPanel.this.replicationCountSpinner.getValue() - this.currentReplication + 1 );
					data.setTaskId( this.taskId );
					data.setTxCount( count );
					data.setFinalAgents( tx.getFinalAgents() );

					return data;
				}
			}
			
			return null;
		}
	}
}
