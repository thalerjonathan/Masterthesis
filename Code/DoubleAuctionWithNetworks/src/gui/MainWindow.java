package gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import agents.Agent;
import agents.AgentWithLoans;
import agents.IAgentFactory;
import agents.markets.Asset;
import agents.markets.Loans;
import agents.network.AgentConnection;
import agents.network.AgentNetwork;
import agents.network.AgentNetwork.AgentSelectedEvent;
import agents.network.AgentNetwork.ConnectionSelectedEvent;
import agents.network.AgentNetwork.INetworkSelectionObserver;
import agents.network.AgentNetwork.NetworkRenderPanel;
import doubleAuction.Auction;
import doubleAuction.AuctionWithLoans;
import doubleAuction.offer.AskOffering;
import doubleAuction.offer.AskOfferingWithLoans;
import doubleAuction.offer.BidOffering;
import doubleAuction.offer.BidOfferingWithLoans;
import doubleAuction.tx.Transaction;
import doubleAuction.tx.TransactionWithLoans;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;

@SuppressWarnings("serial")
public class MainWindow extends JFrame implements ActionListener, ChangeListener {
	
	private AgentNetwork agents;
	private Asset asset;
	private Loans loans;
	
	private JCheckBox keepSuccTXHighCheck;
	
	private JButton simulateButton;
	private JButton recreateButton;
	private JButton nextTxButton;
	private JToggleButton pauseButton;
	
	private JPanel controlsPanel;
	private JPanel txInfoPanel;
	private NetworkRenderPanel networkPanel;
	private JPanel agentWealthPanel;
	
	private JComboBox<String> topologySelection;
	private JComboBox<String> layoutSelection;
	private JComboBox<String> optimismSelection;
	
	private JSpinner agentCountSpinner;
	
	private JLabel succTxCounterLabel;
	private JLabel totalTxCounterLabel;
	private JLabel noSuccTxCounterLabel;
	
	private JLabel finalAssetAskLabel;
	private JLabel finalAssetBidLabel;
	private JLabel finalLoanAskLabel;
	private JLabel finalLoanBidLabel;

	private JList<Transaction> txHistoryList;
	private DefaultListModel<Transaction> txListModel;
	
	private Timer spinnerChangedTimer;
	
	private SimulationThread simulationThread;
	
	private static final DecimalFormat df = new DecimalFormat("0.0000");
	
	private Agent selectedAgent;
	
	private List<Transaction> successfulTx;
	
	public MainWindow() {
		super("Continuous Double-Auctions");
		
		this.successfulTx = new ArrayList<Transaction>();
		
		BorderLayout layout = new BorderLayout();
		//layout.setHgap( 10 );
		layout.setVgap( 5 );
		
		this.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        this.setLayout( layout );
        
        this.createControlsPanel();
        this.createAgents();
        
        this.setVisible( true );
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		MainWindow.this.createAgents();
	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				// we are running inside a thread => need to invoke SwingUtilities.invokeLater !
				SwingUtilities.invokeLater( new Runnable() {
					@Override
					public void run() {
						MainWindow.this.createAgents();
					}
				});
			}
		};
		
		// cancel already scheduled timer
		if ( null != this.spinnerChangedTimer ) {
			this.spinnerChangedTimer.cancel();
			this.spinnerChangedTimer.purge();
		}
		
		// schedule a recreation of the agents after 500ms
		this.spinnerChangedTimer = new Timer();
		this.spinnerChangedTimer.schedule( task, 500 );
	}
	
	private void createControlsPanel() {
		// instancing of components ////////////////////////////////////
		BorderLayout layout = new BorderLayout();
		layout.setHgap( 10 );
		layout.setVgap( 10 );
		
		this.controlsPanel = new JPanel();
		this.txInfoPanel = new JPanel( layout );

		this.topologySelection = new JComboBox<String>( new String[] { "Ascending-Connected", "Ascending Shortcuts", "Hub-Connected", "Fully-Connected", "Erdos-Renyi", "Barbasi-Albert", "Watts-Strogatz" } );
		this.layoutSelection = new JComboBox<String>( new String[] { "Circle", "KK" } );
		this.optimismSelection = new JComboBox<String>( new String[] { "Linear", "Triangle"  } );
		
		this.agentCountSpinner = new JSpinner( new SpinnerNumberModel( 30, 10, 100, 1 ) );

		JLabel succTxCounterInfoLabel = new JLabel( "Successful TX: " );
		JLabel noSuccTxCounterInfoLabel = new JLabel( "No Succ. TX: " );
		JLabel totalTxCounterInfoLabel = new JLabel( "Total TX: " );
		JLabel finalAssetAskInfoLabel = new JLabel( "Asset Ask Price:" );
		JLabel finalAssetBidInfoLabel = new JLabel( "Asset Bid Price:" );
		JLabel finalLoanAskInfoLabel = new JLabel( "Loan Ask Price:" );
		JLabel finalLoanBidInfoLabel = new JLabel( "Loan Bid Price:"  );
		
		this.succTxCounterLabel = new JLabel( "-" );
		this.noSuccTxCounterLabel = new JLabel( "-" );
		this.totalTxCounterLabel = new JLabel( "-" );		
		this.finalAssetAskLabel = new JLabel( "-" );
		this.finalAssetBidLabel = new JLabel( "-" );
		this.finalLoanAskLabel = new JLabel( "-" );
		this.finalLoanBidLabel = new JLabel( "-" );
	
		this.recreateButton = new JButton( "Recreate" );
		this.simulateButton = new JButton( "Start Simulation" );
		this.nextTxButton = new JButton( "Next TX" );
		this.pauseButton = new JToggleButton ( "Run" );
		
		this.keepSuccTXHighCheck = new JCheckBox( "Keep TXs Highlighted" );
		this.keepSuccTXHighCheck.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				if ( null == MainWindow.this.simulationThread ) {
					return;
				}
				
				MainWindow.this.networkPanel.setKeepTXHighlighted( MainWindow.this.keepSuccTXHighCheck.isSelected() );
				MainWindow.this.networkPanel.repaint();
			}
		});
		
		this.txListModel = new DefaultListModel<Transaction>();
		this.txHistoryList = new JList<Transaction>( this.txListModel );
		this.txHistoryList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		
		JScrollPane txHistoryScrollPane = new JScrollPane( this.txHistoryList );
		txHistoryScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
		txHistoryScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		
		// setting properties of components ////////////////////////////////////
		this.nextTxButton.setVisible( false );
		this.pauseButton.setVisible( false );

		// setting up event-listeners of components ////////////////////////////////////
		this.txHistoryList.addListSelectionListener( new ListSelectionListener() {
			@Override
			public void valueChanged( ListSelectionEvent e ) {
				if (e.getValueIsAdjusting() == false) {
					/*
					if ( null == MainWindow.this.simulationThread || false == MainWindow.this.simulationThread.isPause() ) {
						return;
					}
					*/
					
					int index = MainWindow.this.txHistoryList.getSelectedIndex();
					if ( -1 == index ) {
						MainWindow.this.finalAssetAskLabel.setText( "-" );
						MainWindow.this.finalAssetBidLabel.setText( "-" );
						MainWindow.this.finalLoanAskLabel.setText( "-" );
						MainWindow.this.finalLoanBidLabel.setText( "-" );

						return;
					}
					
					Transaction tx = MainWindow.this.txListModel.get( index );
					MainWindow.this.highlightTx( tx );
		        }
			}
		});
		
		this.simulateButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MainWindow.this.toggleSimulation();				
			}
		});
		
		this.nextTxButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MainWindow.this.resetNetworkHighlights();
				MainWindow.this.networkPanel.repaint();

				MainWindow.this.simulationThread.nextTX();
			}
		});
		
		this.pauseButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if ( MainWindow.this.pauseButton.isSelected() ) {
					MainWindow.this.pauseButton.setText( "Paused" );
					MainWindow.this.nextTxButton.setEnabled( true );
				} else {
					MainWindow.this.pauseButton.setText( "Pause" );
					MainWindow.this.nextTxButton.setEnabled( false );
				}
				
				MainWindow.this.simulationThread.togglePause();
			}
		});
		
		this.layoutSelection.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if ( null == MainWindow.this.agents ) {
					return;
				}
				
				MainWindow.this.createLayout();
			}
		} );
		
		this.recreateButton.addActionListener( this );
		
		this.topologySelection.addActionListener( this );
		this.optimismSelection.addActionListener( this );
		
		this.agentCountSpinner.addChangeListener( this );

		// adding components ////////////////////////////////////
		this.controlsPanel.add( this.recreateButton );
		this.controlsPanel.add( this.agentCountSpinner );
		this.controlsPanel.add( this.topologySelection );
		this.controlsPanel.add( this.optimismSelection );
		this.controlsPanel.add( this.layoutSelection );
		this.controlsPanel.add( this.simulateButton );
		this.controlsPanel.add( this.pauseButton );
		this.controlsPanel.add( this.nextTxButton );
		this.controlsPanel.add( this.keepSuccTXHighCheck );
		
		JPanel txLabelsPanel = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();
		
		c.fill = GridBagConstraints.VERTICAL;
		c.weightx = 0.5;
		c.gridx = 0;
	    c.gridy = 0;
	    c.ipadx = 10;
	    txLabelsPanel.add( succTxCounterInfoLabel, c );
		c.gridx = 1;
	    c.gridy = 0;
	    txLabelsPanel.add( this.succTxCounterLabel, c );
	    c.gridx = 0;
	    c.gridy = 1;
	    txLabelsPanel.add( noSuccTxCounterInfoLabel, c );
		c.gridx = 1;
	    c.gridy = 1;
	    txLabelsPanel.add( this.noSuccTxCounterLabel, c );
		c.gridx = 0;
	    c.gridy = 2;
	    txLabelsPanel.add( totalTxCounterInfoLabel, c );
		c.gridx = 1;
	    c.gridy = 2;
	    txLabelsPanel.add( this.totalTxCounterLabel, c );
	    
		c.gridx = 0;
	    c.gridy = 3;
		txLabelsPanel.add( finalAssetAskInfoLabel, c );
		c.gridx = 1;
	    c.gridy = 3;
		txLabelsPanel.add( this.finalAssetAskLabel, c );
		
		c.gridx = 0;
	    c.gridy = 4;
		txLabelsPanel.add( finalAssetBidInfoLabel, c );
		c.gridx = 1;
	    c.gridy = 4;
		txLabelsPanel.add( this.finalAssetBidLabel, c );
		
		c.gridx = 0;
	    c.gridy = 5;
		txLabelsPanel.add( finalLoanAskInfoLabel, c );
		c.gridx = 1;
	    c.gridy = 5;
		txLabelsPanel.add( this.finalLoanAskLabel, c );
		
		c.gridx = 0;
	    c.gridy = 6;
		txLabelsPanel.add( finalLoanBidInfoLabel, c );
		c.gridx = 1;
	    c.gridy = 6;
		txLabelsPanel.add( this.finalLoanBidLabel, c );

		this.txInfoPanel.add( txHistoryScrollPane, BorderLayout.CENTER );
		this.txInfoPanel.add( txLabelsPanel, BorderLayout.EAST );
		
		this.add( this.controlsPanel, BorderLayout.NORTH );
		this.add( this.txInfoPanel, BorderLayout.SOUTH );
	}
	
	@SuppressWarnings("unchecked")
	private void createLayout() {
		Class<? extends Layout<Agent, AgentConnection>> layout = (Class<? extends Layout<Agent, AgentConnection>>) CircleLayout.class;
		
		if ( 1 == this.layoutSelection.getSelectedIndex() ) {
			layout = (Class<? extends Layout<Agent, AgentConnection>>) KKLayout.class;
		}
		
		// remove network-visualization panel when already there
		if ( null != this.networkPanel ) {
			this.remove( MainWindow.this.networkPanel );
		}
		
		this.networkPanel = MainWindow.this.agents.getNetworkRenderingPanel( layout, new INetworkSelectionObserver() {
			@Override
			public void agentSeleted( AgentSelectedEvent agentSelectedEvent ) {
				if ( null == MainWindow.this.simulationThread || false == MainWindow.this.simulationThread.isPause() ) {
					return;
				}
				
				MainWindow.this.resetNetworkHighlights();
				MainWindow.this.txListModel.clear();
				
				if ( agentSelectedEvent.isCtrlDownFlag() && null != MainWindow.this.selectedAgent ) {
					MainWindow.this.selectedAgent.setHighlighted( true );
					agentSelectedEvent.getSelectedAgent().setHighlighted( true );
					
					List<AgentConnection> path = MainWindow.this.agents.getPath( MainWindow.this.selectedAgent, agentSelectedEvent.getSelectedAgent() );
					// returns null when there is no path of successful transactions
					if ( null != path ) {
						for ( AgentConnection c : path ) {
							c.setHighlighted( true );
						}
					}
					
				} else {
					Agent a1 = agentSelectedEvent.getSelectedAgent();

					for ( int i = 0; i < MainWindow.this.successfulTx.size(); ++i ) {
						Agent a2 = null;
						Transaction tx = MainWindow.this.successfulTx.get( i );

						if ( a1 == tx.getMatchingAskOffer().getAgent() ) {
							a2 = tx.getMatchingBidOffer().getAgent();
						} else if ( a1 == tx.getMatchingBidOffer().getAgent() ) {
							a2 = tx.getMatchingAskOffer().getAgent();
						}
						
						if ( null != a2 ) {
							MainWindow.this.txListModel.addElement( tx );
							MainWindow.this.agents.getConnection( a1, a2 ).setHighlighted( true );
						}
					}
					
					MainWindow.this.selectedAgent = a1;
					MainWindow.this.selectedAgent.setHighlighted( true );
				}
				
				MainWindow.this.networkPanel.repaint();
			}

			@Override
			public void connectionSeleted( ConnectionSelectedEvent connSelectedEvent ) {
				MainWindow.this.resetNetworkHighlights();
				MainWindow.this.txListModel.clear();

				connSelectedEvent.getSelectedConnection().setHighlighted( true );
				
				for ( int i = 0; i < MainWindow.this.successfulTx.size(); ++i ) {
					Transaction tx = MainWindow.this.successfulTx.get( i );
					
					Agent a1 = tx.getMatchingAskOffer().getAgent();
					Agent a2 = tx.getMatchingBidOffer().getAgent();

					if ( connSelectedEvent.getSelectedConnection() == MainWindow.this.agents.getConnection( a1, a2 ) ) {
						a1.setHighlighted( true );
						a2.setHighlighted( true );
						
						MainWindow.this.txListModel.addElement( tx );
					}
				}
				
				MainWindow.this.networkPanel.repaint();
			}
		} );
		
		this.add( MainWindow.this.networkPanel, BorderLayout.WEST );
		this.pack();
	}

	private void createAgents() {
		// retrieve params from GUI
		int agentCount = (int) this.agentCountSpinner.getValue();
		double assetPrice = 0.6;
		double consumEndow = 1.0;
		double assetEndow = 1.0;
		int topologyIndex = this.topologySelection.getSelectedIndex();
		int optimismFunctionIndex = this.optimismSelection.getSelectedIndex();
		
		double[] J = new double[] {0.2};
		double[] initialLoanPrices = new double[] {0.2};
		
		// create asset-market
		this.asset = new Asset( assetPrice, agentCount * assetEndow );
		this.loans = new Loans( initialLoanPrices ,J);
		
		// create agent-factory
		IAgentFactory agentFactory = new IAgentFactory() {
			private int i = 1;
			
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
						double agentArea = ( ( ( halfAgentCount - i ) * ( halfAgentCount - i ) ) / 2.0 );
						
						if ( i <= halfAgentCount ) {
							agentArea = halfArea - agentArea;
							
						} else {
							agentArea = halfArea + agentArea;
						}
						
						optimism = agentArea / totalArea;
					}
					
					//a = new Agent( i, optimism, consumEndow, assetEndow, asset );
					a = new AgentWithLoans( i, optimism, consumEndow, assetEndow, loans, asset );
					
					i++;
				}
				
				return a;
			}
		};
		
		// decide which network-topology to create based on user-selection
		if ( 0 == topologyIndex ) {
			this.agents = AgentNetwork.createAscendingConnected( agentFactory );
		} else if ( 1 == topologyIndex ) {
			this.agents = AgentNetwork.createAscendingShortcutsConnected( 0.5, agentFactory );
		} else if ( 2 == topologyIndex ) {
			this.agents = AgentNetwork.createWithHubs( 3, agentFactory );
		} else if ( 3 == topologyIndex ) {
			this.agents = AgentNetwork.createFullyConnected( agentFactory );
		} else if ( 4 == topologyIndex ) {
			this.agents = AgentNetwork.createErdosRenyiConnected( 0.2, agentFactory );
		} else if ( 5 == topologyIndex ) {
			this.agents = AgentNetwork.createBarbasiAlbertConnected( 3, 1, agentFactory );
		} else if ( 6 == topologyIndex ) {
			this.agents = AgentNetwork.createWattsStrogatzConnected( 2, 0.2, agentFactory );
		}
		
		// if agent-wealth-visualisation panel is already there, remove it bevore adding a new instance
		if ( null != this.agentWealthPanel ) {
			this.remove( this.agentWealthPanel );
		}
		
		this.agentWealthPanel = this.agents.getWealthVisualizer();
		this.add( this.agentWealthPanel, BorderLayout.EAST );
		
		// need to create the layout too, will do the final pack-call on this frame
		this.createLayout();
	}
	
	private void toggleSimulation() {
		// no simulation already running...
		if ( null == this.simulationThread ) {
			// if there was a simulation-run before: reset the agents
			this.agents.reset( 1.0, 1.0 );

			this.successfulTx.clear();
			this.txListModel.clear();
			
			// disable controls, to prevent changes by user
			this.agentCountSpinner.setEnabled( false );
			this.topologySelection.setEnabled( false );
			this.optimismSelection.setEnabled( false );
			this.recreateButton.setEnabled( false );

			// reset controls
			this.finalAssetAskLabel.setText( "-" );
			this.finalAssetBidLabel.setText( "-" );
			this.finalLoanAskLabel.setText( "-" );
			this.finalLoanBidLabel.setText( "-" );
			
			this.simulateButton.setText( "Stop Simulation" );
			this.simulateButton.setEnabled( true );
			
			this.nextTxButton.setVisible( true );
			this.nextTxButton.setEnabled( true );
			
			this.pauseButton.setText( "Paused" );
			this.pauseButton.setSelected( true );
			this.pauseButton.setEnabled( true );
			this.pauseButton.setVisible( true );
			
			Auction simulation = new AuctionWithLoans( MainWindow.this.agents, MainWindow.this.asset ); // or new AuctionWithLoans
			simulation.init();
			
			// let simulation run in a separate thread to prevent blocking of gui
			this.simulationThread = new SimulationThread( simulation );
			this.simulationThread.start();
			
		// simulation is running
		} else {
			this.simulationThread.stopSimulation();
			
			try {
				// wait for thread to join
				this.simulationThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			this.simulationThread = null;

			// simulation has finished => enable controls
			this.simulateButton.setText( "Start Simulation" );
			this.nextTxButton.setVisible( false );
			this.pauseButton.setVisible( false );
			
			this.agentCountSpinner.setEnabled( true );
			this.topologySelection.setEnabled( true );
			this.optimismSelection.setEnabled( true );
			this.recreateButton.setEnabled( true );
		}
	}
	
	private void highlightTx( Transaction tx ) {
		this.resetNetworkHighlights();
		
		AskOffering askOffering = tx.getMatchingAskOffer();
		BidOffering bidOffering = tx.getMatchingBidOffer();
		
		Agent a1 = askOffering.getAgent();
		Agent a2 = bidOffering.getAgent();
		
		AgentConnection conn = this.agents.getConnection( a1, a2 );
		if ( conn.getWeight() == Double.MAX_VALUE ) {
			conn.setWeight( 1.0 );
		//} else {
		//	conn.incrementWeight( 1.0 );
		}
		
		conn.setHighlighted( true );
		a1.setHighlighted( true );
		a2.setHighlighted( true );
		
		this.networkPanel.repaint();
		
		this.finalAssetAskLabel.setText( df.format( tx.getFinalAskAssetPrice() ) );
		this.finalAssetBidLabel.setText( df.format( tx.getFinalBidAssetPrice() ) );
		
		String finalLoanAskLabelText = "-";
		String finalLoanBidLabelText = "-";
		
		if ( tx instanceof TransactionWithLoans ) {
			if ( askOffering instanceof AskOfferingWithLoans ) {
				finalLoanAskLabelText = df.format( ( (TransactionWithLoans) tx ).getFinalAskLoanPrice() );
			}
			
			if ( bidOffering instanceof BidOfferingWithLoans ) {
				finalLoanBidLabelText = df.format( ( (TransactionWithLoans) tx ).getFinalBidLoanPrice() );
			}
		}
		
		this.finalLoanAskLabel.setText( finalLoanAskLabelText );
		this.finalLoanBidLabel.setText( finalLoanBidLabelText );
	}
	
	private void resetNetworkHighlights() {
		Iterator<Agent> agentsIter = this.agents.iterator();
		while ( agentsIter.hasNext() ) {
			agentsIter.next().setHighlighted( false );
		}
		
		Iterator<AgentConnection> connIter = this.agents.connectionIterator();
		while ( connIter.hasNext() ) {
			connIter.next().setHighlighted( false );
		}
	}
	
	private void restoreTXHistoryList() {
		if ( this.txListModel.size() != this.successfulTx.size() ) {
			this.txHistoryList.clearSelection();
			
			this.txListModel.clear();
			
			for ( Transaction tx : this.successfulTx ) {
				this.txListModel.addElement( tx );
			}
		}
	}
	
	private enum SimulationState {
		EXIT,
		RUNNING,
		PAUSED,
		NEXT_TX;
	}
	
	private class SimulationThread extends Thread {
		private Lock lock = new ReentrantLock();
		private Condition condition = lock.newCondition();
		   
		private Auction simulation;

		private int succTX;
		private int totalTX;
		private int noSuccTXCounter;
		
		private Transaction lastSuccTX;
		
		private Thread nextTxThread;
		
		private SimulationState state;

		public SimulationThread( Auction simulation ) {
			this.simulation = simulation;
			
			// start in pause-mode: thread must block bevore doing first transaction
			this.state = SimulationState.PAUSED;
			
			// give nice name for debugging purposes
			this.setName( "Simulation Thread" );
			
			this.updateTXCounter();
		}

		public boolean isPause() {
			return this.state == SimulationState.PAUSED;
		}
		
		// NOTE: must be called from other thread than SimulationThread
		public void stopSimulation() {
			// simulation-thread is running and no nextTX-thread exists, just switch state to exit
			if ( SimulationState.RUNNING == this.state ) {
				this.state = SimulationState.EXIT;
				
			// simulation-thread is running but need to stop nextTX-thread
			} else if ( SimulationState.NEXT_TX == this.state ) {
				this.state = SimulationState.EXIT;
				
				this.stopNextTXThread();
				
			// simulation-thread is blocked, switch state and signal to continue
			} else if ( SimulationState.PAUSED == this.state ) {
				MainWindow.this.restoreTXHistoryList();
				
				// signal the thread to exit
				this.lock.lock();
				this.state = SimulationState.EXIT;
				this.condition.signal();
				this.lock.unlock();
			}
		}

		// NOTE: must be called from other thread than SimulationThread
		public void togglePause() {
			// simulation-thread is running, no nextTX-thread exists, just switch to pause-state
			if ( SimulationState.RUNNING == this.state ) {
				this.state = SimulationState.PAUSED;
				
				// simulation-thread is running but need to stop nextTX-thread
			} else if ( SimulationState.NEXT_TX == this.state ) {
				this.state = SimulationState.PAUSED;
				this.stopNextTXThread();
				
			// simulation-thread is blocked, switch state and signal to continue
			} else if ( SimulationState.PAUSED == this.state ) {
				// switch to running
				MainWindow.this.restoreTXHistoryList();
				
				// signal the thread to resume
				this.lock.lock();
				this.state = SimulationState.RUNNING;
				this.condition.signal();
				this.lock.unlock();
			}
		}
		
		// NOTE: must be called from other thread than SimulationThread
		public void nextTX() {
			// switch state to next-tx (will always be already in paused-mode at this point, next-tx can only be reached from paused-mode)
			this.state = SimulationState.NEXT_TX;
			
			MainWindow.this.nextTxButton.setEnabled( false );
			
			MainWindow.this.pauseButton.setSelected( false );
			MainWindow.this.pauseButton.setText( "Pause" );
			
			MainWindow.this.restoreTXHistoryList();			

			// PROBLEM: this call could block for a very long time or forever because 
			// it could take a very long time or forever for the next successful transaction to occur
			// => need another thread otherwise would block the GUI-thread!
			this.nextTxThread = new Thread( new Runnable() {
				@Override
				public void run() {
					// need to lock section bevore we can signal and to prevent concurrent modification of data
					SimulationThread.this.lock.lock();
					
					try {
						// (re-) set to null to wait for successful TX
						SimulationThread.this.lastSuccTX = null;
						
						// signal the blocking simulation-thread because nextTX can only be called from pause-state 
						// thus in pause-state simulation-thread is blocking already
						SimulationThread.this.condition.signal();

						// wait blocking till either a next successful TX has been found OR the state has changed 
						// if successful TX has been found: simulation-thread will set lastSuccTX to the given TX and switch state back to pause and give signal
						// if state-switch occured through GUI e.g. back to pause or exit, signal came from GUI-Thread and lastSuccTX will be null
						while ( null == SimulationThread.this.lastSuccTX || SimulationThread.this.state == SimulationState.NEXT_TX ) {
							SimulationThread.this.condition.await();
						}
						
					} catch (InterruptedException e) {
						e.printStackTrace();
						
					} finally {
						SimulationThread.this.lock.unlock();
						SimulationThread.this.nextTxThread = null;

						// running in thread => need to update SWING through SwingUtilities.invokeLater
						SwingUtilities.invokeLater( new Runnable() {
							@Override
							public void run() {
								MainWindow.this.nextTxButton.setEnabled( true );
								MainWindow.this.pauseButton.setSelected( true );
								MainWindow.this.pauseButton.setText( "Paused" );

							}
						});
					}
				}
			});
			
			// give nice name for debugging purposes
			this.nextTxThread.setName( "Next-TX Thread" );
			this.nextTxThread.start();
		}
		
		@Override
		public void run() {
			// run this thread until simulation-state tells to exit
			while ( SimulationState.EXIT != this.state ) {
				// need to lock section bevore we can signal and to prevent concurrent modification of data
				this.lock.lock();
				
				try {
					// wait blocking while in pause mode. GUI-Thread or NextTX-thread will change state and give signal
					while ( SimulationState.PAUSED == this.state ) {
						this.condition.await();
					}

					// count total number of TX so far
					this.totalTX++;
					
					// execute the next transaction
					Transaction tx = this.simulation.executeSingleTransaction();
					// tx was successful
					if ( tx.wasSuccessful() ) {
						// count number of successful TX so far
						this.succTX++;
						// reset counter of how many unsuccessful TX in a row occured
						this.noSuccTXCounter = 0;

						// we are in nextTX-state and found one successful TX => switch back to paused-state
						if ( SimulationState.NEXT_TX == this.state ) {
							// found successfull transaction: store in consumer-data to 
							this.lastSuccTX = tx;
							// next-tx can only happen in paused-state, switch back to paused when finished
							this.state = SimulationState.PAUSED;
						}
	
						// signal the waiting GUI/next-TX-thread (if any)
						this.condition.signal();
						
					// not successful
					} else {
						// count how many unsuccessful TX in a row occured
						this.noSuccTXCounter++;
						// need to reset to null, otherwise will stick to the last known until hit the NextTX-Button again
						//this.lastSuccTX = null;
					}
					
					// running in thread => need to update SWING through SwingUtilities.invokeLater
					SwingUtilities.invokeLater( new Runnable() {
						@Override
						public void run() {
							// update gui if TX was successful
							if ( tx.wasSuccessful() ) {
								MainWindow.this.agentWealthPanel.repaint();
								
								MainWindow.this.successfulTx.add( tx );
								MainWindow.this.txListModel.addElement( tx );
								
								MainWindow.this.highlightTx( tx );
							}
	
							SimulationThread.this.updateTXCounter();
						}
					} );
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					// need to unlock
					this.lock.unlock();
				}
			}
		}
		
		private void updateTXCounter() {
			MainWindow.this.succTxCounterLabel.setText( "" + this.succTX );
			MainWindow.this.noSuccTxCounterLabel.setText( "" + this.noSuccTXCounter );
			MainWindow.this.totalTxCounterLabel.setText( "" + this.totalTX );
		}
		
		private void stopNextTXThread() {
			// next TX-Thread is existing, needs to be terminated too, 
			// signal that condition has changed
			if ( null != this.nextTxThread ) {
				// signal the thread to resume
				this.lock.lock();
				this.condition.signal();
				this.lock.unlock();
				
				// race-condition here: next-tx thread sets itself to null when finished
				// thus if this is satisfied, we don't need to wait for join
				if ( null != this.nextTxThread ) {
					try {
						this.nextTxThread.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
