package frontend.inspection;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import backend.Auction;
import backend.Auction.MatchingType;
import backend.EquilibriumStatistics;
import backend.agents.Agent;
import backend.agents.AgentFactoryImpl;
import backend.agents.network.AgentConnection;
import backend.agents.network.AgentNetwork;
import backend.markets.LoanType;
import backend.markets.MarketType;
import backend.markets.Markets;
import backend.offers.AskOffering;
import backend.offers.BidOffering;
import backend.tx.Transaction;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import frontend.Utils;
import frontend.inspection.InspectionThread.AdvanceMode;
import frontend.inspection.InspectionThread.InspectionObserver;
import frontend.inspection.offerBook.OfferBook;
import frontend.inspection.txHistory.TxHistoryTable;
import frontend.networkCreators.AscendingConnectedCreator;
import frontend.networkCreators.AscendingFullShortcutsCreator;
import frontend.networkCreators.AscendingRandomShortcutsCreator;
import frontend.networkCreators.AscendingRegularShortcutsCreator;
import frontend.networkCreators.BarbasiAlbertCreator;
import frontend.networkCreators.ErdosRenyiCreator;
import frontend.networkCreators.FullyConnectedCreator;
import frontend.networkCreators.HalfFullyConnectedCreator;
import frontend.networkCreators.HubConnectedCreator;
import frontend.networkCreators.MaximumHubCreator;
import frontend.networkCreators.MedianHubCreator;
import frontend.networkCreators.NetworkCreator;
import frontend.networkCreators.ThreeMedianHubsCreator;
import frontend.networkCreators.WattStrogatzCreator;
import frontend.networkVisualisation.AgentSelectedEvent;
import frontend.networkVisualisation.ConnectionSelectedEvent;
import frontend.networkVisualisation.INetworkSelectionObserver;
import frontend.networkVisualisation.NetworkRenderPanel;
import frontend.networkVisualisation.NetworkVisualisationFrame;
import frontend.replication.EquilibriumInfoPanel;
import frontend.visualisation.MarketsAccuOnlineVisualizer;
import frontend.visualisation.MarketsTimeOnlineVisualizer;
import frontend.visualisation.WealthVisualizer;

@SuppressWarnings("serial")
public class InspectionPanel extends JPanel {
	
	private AgentNetwork agentNetwork;
	private Markets markets;
	private Auction auction;
	
	private JCheckBox abmMarketCheck;
	private JCheckBox loanCashMarketCheck;
	private JCheckBox collateralMarketCheck;
	private JCheckBox bpMechanismCheck;
	private JCheckBox importanceSamplingCheck;
	
	private JCheckBox keepAgentHistoryCheck;
	private JCheckBox forceRedrawCheck;
	
	private JButton inspectionButton;
	private JButton nextTxButton;
	private JButton advance10TxButton;
	private JButton advance100TxButton;
	private JButton openOfferBookButton;
	private JToggleButton pauseButton;
	private JButton showNetworkButton;

	private JPanel visualizationPanel;
	
	private JComboBox<NetworkCreator> topologySelection;
	private JComboBox<String> optimismSelection;
	private JComboBox<MatchingType> matchingTypeSelection;
	private JComboBox<InspectionThread.AdvanceMode> advcanceModeSelection;
	private JComboBox<LoanType> loanTypeSelection;
	
	private JSpinner agentCountSpinner;
	
	private JLabel computationTimeLabel;
	
	private JLabel succTxCounterLabel;
	private JLabel totalTxCounterLabel;
	private JLabel failedTxCounterLabel;
	private JLabel totalfailedTxCounterLabel;
	private JLabel sweepsCounterLabel;
	
	private JTabbedPane visualizersTabbedPane;
	
	private WealthVisualizer agentWealthPanel;
	private MarketsTimeOnlineVisualizer marketsTimeVisualizer;
	private MarketsAccuOnlineVisualizer marketsAccuVisualizer;
	private NetworkVisualisationFrame netVisFrame;
	private EquilibriumInfoPanel equilibriumInfoPanel;
	
	private OfferBook offerBook;
	
	private TxHistoryTable txHistoryTable;
	
	private Timer spinnerChangedTimer;
	
	private InspectionThread simulationThread;
	
	private Agent selectedAgent;
	
	private List<Transaction> successfulTx;
	private List<MarketType> successfulMarkets;
	
	private long lastRepaintTime;
	
	private static final int REPAINT_WEALTH_WHENRUNNING_INTERVAL = 1000;
	
	public InspectionPanel() {
		this.markets = new Markets();
		this.successfulTx = new ArrayList<Transaction>();
		this.successfulMarkets = new ArrayList<MarketType>();
		
        this.setLayout( new GridBagLayout() );
        
        this.createControls();
        this.createAgents();
	}

	public String getTitleExtension() {
		int agentCount = (int) this.agentCountSpinner.getValue();
		NetworkCreator creator = (NetworkCreator) this.topologySelection.getSelectedItem();
		return creator.name() + ", " + agentCount + " Agents";
	}
	
	private void simulationTerminated() {
		// simulation was running, toggle will switch all gui-stuff to "stoped"
		this.toggleInspection();
		this.agentWealthPanel.repaint();
		InspectionPanel.this.repaintNetworkVisFrame();
		JOptionPane.showMessageDialog( this, "No Agent is able to trade with any of its neighbours - Simulation stoped.", 
				"Equilibrium Reached", JOptionPane.INFORMATION_MESSAGE );
	}
	
	private void createControls() {
		// instancing of components ////////////////////////////////////
		GridBagConstraints c = new GridBagConstraints();

		this.visualizersTabbedPane = new JTabbedPane();
		this.visualizationPanel = new JPanel( new GridBagLayout() );
		this.offerBook = new OfferBook();
		this.equilibriumInfoPanel = new EquilibriumInfoPanel();
		this.marketsTimeVisualizer = new MarketsTimeOnlineVisualizer( this.successfulMarkets );
		this.marketsAccuVisualizer = new MarketsAccuOnlineVisualizer( this.successfulMarkets );
		this.agentWealthPanel = new WealthVisualizer();

		JPanel controlsPanel = new JPanel();
		JPanel txInfoPanel = new JPanel( new GridBagLayout() );

		this.topologySelection = new JComboBox<NetworkCreator>();
		this.topologySelection.addItem( new AscendingConnectedCreator() );
		this.topologySelection.addItem( new AscendingFullShortcutsCreator() );
		this.topologySelection.addItem( new AscendingRegularShortcutsCreator() );
		this.topologySelection.addItem( new AscendingRandomShortcutsCreator() );
		this.topologySelection.addItem( new FullyConnectedCreator() );
		this.topologySelection.addItem( new HalfFullyConnectedCreator() );
		this.topologySelection.addItem( new HubConnectedCreator() );
		this.topologySelection.addItem( new MedianHubCreator() );
		this.topologySelection.addItem( new MaximumHubCreator() );
		this.topologySelection.addItem( new ThreeMedianHubsCreator() );
		this.topologySelection.addItem( new ErdosRenyiCreator() );
		this.topologySelection.addItem( new BarbasiAlbertCreator() );
		this.topologySelection.addItem( new WattStrogatzCreator() );
		
		this.optimismSelection = new JComboBox<String>( new String[] { "Linear", "Triangle"  } );
		this.matchingTypeSelection = new JComboBox<MatchingType>( MatchingType.values() );
		this.advcanceModeSelection = new JComboBox<InspectionThread.AdvanceMode>( InspectionThread.AdvanceMode.values() );
		this.loanTypeSelection = new JComboBox<LoanType>( LoanType.values() );
		
		this.agentCountSpinner = new JSpinner( new SpinnerNumberModel( 30, 2, 1000, 10 ) );
		
		this.computationTimeLabel = new JLabel( "0,00 sec" );
		this.succTxCounterLabel = new JLabel( "0" );
		this.failedTxCounterLabel = new JLabel( "0" );
		this.totalfailedTxCounterLabel = new JLabel( "0" );
		this.totalTxCounterLabel = new JLabel( "0" );	
		this.sweepsCounterLabel = new JLabel( "0 (0)" );	
		
		
		this.inspectionButton = new JButton( "Start Inspection" );
		this.nextTxButton = new JButton( "Next TX" );
		this.advance10TxButton = new JButton( "Advance 10 TXs" );
		this.advance100TxButton = new JButton( "Advance 100 TXs" );
		
		this.openOfferBookButton = new JButton( "Open Offer-Book" );
		this.pauseButton = new JToggleButton ( "Pause" );
		this.showNetworkButton = new JButton ( "Show Network" );
		
		this.abmMarketCheck = new JCheckBox( "Asset/Loan Market" );
		this.loanCashMarketCheck = new JCheckBox( "Loan/Cash Market" );
		this.collateralMarketCheck = new JCheckBox( "Collateral/Cash Market" );
		this.bpMechanismCheck = new JCheckBox( "Bonds Pledgeability" );
		this.importanceSamplingCheck = new JCheckBox( "Importance-Sampling" );
		
		this.keepAgentHistoryCheck = new JCheckBox( "Keep Agent History" );
		this.forceRedrawCheck = new JCheckBox( "Force Redraw" );
		
		this.txHistoryTable = new TxHistoryTable();
		JScrollPane txHistoryScrollPane = new JScrollPane( this.txHistoryTable );
		txHistoryScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
		txHistoryScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		
		// setting properties of components ////////////////////////////////////
		this.visualizersTabbedPane.addTab( "Agents", this.agentWealthPanel );
		this.visualizersTabbedPane.addTab( "Markets Time", this.marketsTimeVisualizer );
		this.visualizersTabbedPane.addTab( "Markets Accum", this.marketsAccuVisualizer );
		
		this.loanTypeSelection.setSelectedItem( LoanType.LOAN_05 );
		
		this.abmMarketCheck.setSelected( this.markets.isABM() );
		this.loanCashMarketCheck.setSelected( this.markets.isLoanMarket() );
		this.collateralMarketCheck.setSelected( this.markets.isCollateralMarket() );
		this.bpMechanismCheck.setSelected( this.markets.isBP() );
		this.keepAgentHistoryCheck.setSelected( false );
		
		this.nextTxButton.setEnabled( false );
		this.advance10TxButton.setEnabled( false );
		this.advance100TxButton.setEnabled( false );
		this.advcanceModeSelection.setEnabled( false );
		this.matchingTypeSelection.setEnabled( false );
		this.pauseButton.setEnabled( false );

		this.importanceSamplingCheck.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				InspectionPanel.this.handleImportanceSampling();
			}
		});
		
		// setting up event-listeners of components ////////////////////////////////////
		this.txHistoryTable.getSelectionModel().addListSelectionListener( new ListSelectionListener() {
			@Override
			public void valueChanged( ListSelectionEvent e ) {
				if (e.getValueIsAdjusting() == false) {
					int rowIndex = InspectionPanel.this.txHistoryTable.getSelectedRow();
					if ( -1 == rowIndex ) {
						return;
					}

					int txIndex = InspectionPanel.this.txHistoryTable.getRowSorter().convertRowIndexToModel( rowIndex );
					
					Transaction tx = InspectionPanel.this.successfulTx.get( txIndex );
					InspectionPanel.this.offerBook.agentsUpdated( tx.getFinalAgents() );
					InspectionPanel.this.agentWealthPanel.setAgents( tx.getFinalAgents() );
					
					InspectionPanel.this.highlightTx( tx );
		        }
			}
		});
		
		this.inspectionButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				InspectionPanel.this.toggleInspection();				
			}
		});
		
		this.nextTxButton.addActionListener( new AdvanceTxButtonsActionListener( 1 ) );
		this.advance10TxButton.addActionListener( new AdvanceTxButtonsActionListener( 10 ) );
		this.advance100TxButton.addActionListener( new AdvanceTxButtonsActionListener( 100 ) );
		
		this.pauseButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if ( InspectionPanel.this.pauseButton.isSelected() ) {
					InspectionPanel.this.pauseButton.setText( "Paused" );
					InspectionPanel.this.nextTxButton.setEnabled( true );
					InspectionPanel.this.agentWealthPanel.repaint();
				} else {
					InspectionPanel.this.pauseButton.setText( "Pause" );
					InspectionPanel.this.nextTxButton.setEnabled( false );
				}
				
				InspectionPanel.this.simulationThread.togglePause();
			}
		});
		
		this.showNetworkButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if ( null == InspectionPanel.this.netVisFrame ) {
					InspectionPanel.this.netVisFrame = new NetworkVisualisationFrame();
				}

				InspectionPanel.this.netVisFrame.setVisible( true );
				InspectionPanel.this.updateNetworkVisualisationFrame();
			}
		});
		
		this.topologySelection.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				NetworkCreator newSelected = (NetworkCreator) InspectionPanel.this.topologySelection.getSelectedItem();

				// creator signals to be created immediately
				if ( newSelected.createInstant() ) {
					InspectionPanel.this.createAgents();
				
				// creator signals to defer creation for later (e.g. after user-input of parameters
				// the creator needs)
				} else {
					// defer creation and provide creator with a callback to continue creation
					newSelected.deferCreation( new Runnable() {
						@Override
						public void run() {
							InspectionPanel.this.createAgents();
						}
					});
				}
			}
		});
		
		this.openOfferBookButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				InspectionPanel.this.offerBook.showOfferBook();
			}
		});
		
		ActionListener createAgentsAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				InspectionPanel.this.createAgents();
			}
		};
		
		ChangeListener createAgentsAfterTimeoutChange = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				TimerTask task = new TimerTask() {
					@Override
					public void run() {
						// we are running inside a thread => need to invoke SwingUtilities.invokeLater !
						SwingUtilities.invokeLater( new Runnable() {
							@Override
							public void run() {
								InspectionPanel.this.createAgents();
							}
						});
					}
				};
				
				// cancel already scheduled timer
				if ( null != InspectionPanel.this.spinnerChangedTimer ) {
					InspectionPanel.this.spinnerChangedTimer.cancel();
					InspectionPanel.this.spinnerChangedTimer.purge();
				}
				
				// schedule a recreation of the agents after 500ms
				InspectionPanel.this.spinnerChangedTimer = new Timer();
				InspectionPanel.this.spinnerChangedTimer.schedule( task, 500 );
			}
		};
		
		this.optimismSelection.addActionListener( createAgentsAction );
		this.loanTypeSelection.addActionListener( createAgentsAction );
		this.agentCountSpinner.addChangeListener( createAgentsAfterTimeoutChange );
		
		ActionListener checkListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if ( false == InspectionPanel.this.abmMarketCheck.isSelected() ) {
					InspectionPanel.this.loanCashMarketCheck.setSelected( false );
					InspectionPanel.this.collateralMarketCheck.setSelected( false );
					InspectionPanel.this.bpMechanismCheck.setSelected( false );
				}
				
				InspectionPanel.this.setMarketMechanisms();
			}
		};
		
		this.bpMechanismCheck.addActionListener( checkListener );
		this.abmMarketCheck.addActionListener( checkListener );
		this.collateralMarketCheck.addActionListener( checkListener );
		this.loanCashMarketCheck.addActionListener( checkListener );

		// adding components ////////////////////////////////////

		JLabel succTxCounterInfoLabel = new JLabel( "Successful TX: " );
		JLabel failedTxCounterInfoLabel = new JLabel( "Failed TX: " );
		JLabel totalTxCounterInfoLabel = new JLabel( "Total TX: " );
		JLabel sweepsCounterInfoLabel = new JLabel( "Sweeps Count: " );
		JLabel totalfailedTxCounterInfoLabel = new JLabel( "Total failed TX: " );
		JLabel computationTimeInfoLabel = new JLabel( "Computation Time: " );

		controlsPanel.add( this.loanTypeSelection );
		controlsPanel.add( this.agentCountSpinner );
		controlsPanel.add( this.topologySelection );
		controlsPanel.add( this.abmMarketCheck );
		controlsPanel.add( this.loanCashMarketCheck );
		controlsPanel.add( this.collateralMarketCheck );
		controlsPanel.add( this.bpMechanismCheck );
		controlsPanel.add( this.importanceSamplingCheck );
		controlsPanel.add( this.inspectionButton );
		
		JPanel txLabelsPanel = new JPanel( new GridBagLayout() );
		JPanel txControlPanel = new JPanel( new GridBagLayout() );
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.ipadx = 5;
		c.ipady = 5;
		
		c.gridx = 0;
	    c.gridy = 0;
	    txLabelsPanel.add( computationTimeInfoLabel , c );
	    c.gridx = 1;
	    c.gridy = 0;
	    txLabelsPanel.add( computationTimeLabel, c );
		c.gridx = 0;
	    c.gridy = 1;
	    txLabelsPanel.add( succTxCounterInfoLabel, c );
		c.gridx = 1;
	    c.gridy = 1;
	    txLabelsPanel.add( this.succTxCounterLabel, c );
	    c.gridx = 0;
	    c.gridy = 2;
	    txLabelsPanel.add( failedTxCounterInfoLabel, c );
		c.gridx = 1;
	    c.gridy = 2;
	    txLabelsPanel.add( this.failedTxCounterLabel, c );
	    c.gridx = 0;
	    c.gridy = 3;
	    txLabelsPanel.add( totalfailedTxCounterInfoLabel, c );
		c.gridx = 1;
	    c.gridy = 3;
	    txLabelsPanel.add( this.totalfailedTxCounterLabel, c );
		c.gridx = 0;
	    c.gridy = 4;
	    txLabelsPanel.add( totalTxCounterInfoLabel, c );
		c.gridx = 1;
	    c.gridy = 4;
	    txLabelsPanel.add( this.totalTxCounterLabel, c );
	    c.gridx = 0;
	    c.gridy = 5;
	    txLabelsPanel.add( sweepsCounterInfoLabel, c );
		c.gridx = 1;
	    c.gridy = 5;
	    txLabelsPanel.add( this.sweepsCounterLabel, c );


	    c.gridx = 0;
	    c.gridy = 6;
	    txLabelsPanel.add( this.equilibriumInfoPanel, c );
		
	    
	    c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		txControlPanel.add( this.keepAgentHistoryCheck, c );
		c.gridx = 1;
		c.gridy = 0;
		txControlPanel.add( this.forceRedrawCheck, c );
		
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 1;
		txControlPanel.add( this.openOfferBookButton, c );
		c.gridx = 0;
		c.gridy = 2;
		txControlPanel.add( this.showNetworkButton, c );
	    c.gridx = 0;
		c.gridy = 3;
		txControlPanel.add( this.pauseButton, c );
	    c.gridx = 0;
		c.gridy = 4;
		txControlPanel.add( this.nextTxButton, c );
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 5;
		txControlPanel.add( this.advance10TxButton, c );
		c.gridx = 1;
		c.gridy = 5;
		txControlPanel.add( this.advance100TxButton, c );
		c.gridx = 0;
		c.gridy = 6;
		txControlPanel.add( this.advcanceModeSelection, c );
	    c.gridx = 1;
		c.gridy = 6;
		txControlPanel.add( this.matchingTypeSelection, c );
		
		c.weightx = 0.8;
		c.weighty = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		c.ipady = 0;
		c.gridwidth = 8;
		c.fill = GridBagConstraints.BOTH;
		txInfoPanel.add( txHistoryScrollPane, c );
		
		c.weightx = 0.1;
		c.weighty = 0.5;
		c.gridx = 8;
		c.gridy = 0;
		c.ipady = 0;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.BOTH;
		txInfoPanel.add( txControlPanel, c );
		
		c.weightx = 0.1;
		c.gridx = 9;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.VERTICAL;
		txInfoPanel.add( txLabelsPanel, c );
		
		c.weightx = 1.0;
		c.weighty = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		c.ipady = 0;
		c.fill = GridBagConstraints.BOTH;
		
		c.weighty = 0.0;
		c.gridheight = 1;
	    c.gridy = 0;
		this.add( controlsPanel, c );

		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.gridx = 0;
		c.gridy = 0;
		this.visualizationPanel.add( this.visualizersTabbedPane, c );

		c.weighty = 1.0;
		c.gridheight = 10;
	    c.gridy = 1;
		this.add( this.visualizationPanel, c );
		
		c.weighty = 0.1;
		c.gridheight = 2;
	    c.gridy = 11;
		this.add( txInfoPanel, c );
	}
	
	private void addSuccessfulTX( Transaction tx, boolean forceRedraw ) {
		long currMillis = System.currentTimeMillis();
		if ( InspectionPanel.REPAINT_WEALTH_WHENRUNNING_INTERVAL < currMillis - this.lastRepaintTime || 
				forceRedraw ||
				this.forceRedrawCheck.isSelected()) {
			
			this.agentWealthPanel.setAgents( this.agentNetwork.getOrderedList() );
			this.marketsTimeVisualizer.repaint();
			this.marketsAccuVisualizer.repaint();
			
			this.lastRepaintTime = currMillis;
		}

		this.successfulMarkets.add( tx.getMatch().getMarket() );
		
		if ( this.keepAgentHistoryCheck.isSelected() ) {
			this.successfulTx.add( tx );
			this.txHistoryTable.addTx( tx );

			this.highlightTx( tx );
		}
	}
	
	@SuppressWarnings("unchecked")
	private void updateNetworkVisualisationFrame() {
		if ( null != this.netVisFrame && this.netVisFrame.isVisible() ) {
			NetworkRenderPanel networkPanel = this.agentNetwork.getNetworkRenderingPanel( (Class<? extends Layout<Agent, AgentConnection>>) CircleLayout.class, 
				new INetworkSelectionObserver() {
					@Override
					public void agentSeleted( AgentSelectedEvent agentSelectedEvent ) {
						// no simulation running yet, just highlight selected agent, its neighbours and their connections
						if ( null == InspectionPanel.this.simulationThread ) {
							InspectionPanel.this.resetNetworkHighlights();
							
							Agent selectedAgent = agentSelectedEvent.getSelectedAgent();
							selectedAgent.setHighlighted( true );
							
							Iterator<Agent> neighbourIter = InspectionPanel.this.agentNetwork.getNeighbors( selectedAgent );
							while ( neighbourIter.hasNext() ) {
								Agent neighbour = neighbourIter.next();
								neighbour.setHighlighted( true );
								
								InspectionPanel.this.agentNetwork.getConnection( selectedAgent, neighbour ).setHighlighted( true );
							}
							
							InspectionPanel.this.repaintNetworkVisFrame();
							
							return;
						}
						
						// when simulation thread is not paused, don't react on selection
						if ( false == InspectionPanel.this.simulationThread.isPause() ) {
							return;
						}
						
						InspectionPanel.this.resetNetworkHighlights();
						InspectionPanel.this.txHistoryTable.clearAll();

						// find path between two selected agents
						if ( agentSelectedEvent.isCtrlDownFlag() && null != InspectionPanel.this.selectedAgent ) {
							InspectionPanel.this.selectedAgent.setHighlighted( true );
							agentSelectedEvent.getSelectedAgent().setHighlighted( true );
							
							List<AgentConnection> path = InspectionPanel.this.agentNetwork.getPath( InspectionPanel.this.selectedAgent, agentSelectedEvent.getSelectedAgent() );
							// returns null when there is no path of successful transactions
							if ( null != path ) {
								for ( AgentConnection c : path ) {
									c.setHighlighted( true );
									InspectionPanel.this.addTXOfConnection( c );
								}
							}
							
						} else {
							Agent a1 = agentSelectedEvent.getSelectedAgent();

							for ( int i = 0; i < InspectionPanel.this.successfulTx.size(); ++i ) {
								Agent a2 = null;
								Transaction tx = InspectionPanel.this.successfulTx.get( i );

								if ( a1 == tx.getMatch().getSellOffer().getAgent() ) {
									a2 = tx.getMatch().getBuyOffer().getAgent();
								} else if ( a1 == tx.getMatch().getBuyOffer().getAgent() ) {
									a2 = tx.getMatch().getSellOffer().getAgent();
								}
								
								if ( null != a2 ) {
									InspectionPanel.this.txHistoryTable.addTx( tx );
									InspectionPanel.this.agentNetwork.getConnection( a1, a2 ).setHighlighted( true );
								}
							}
							
							InspectionPanel.this.selectedAgent = a1;
							InspectionPanel.this.selectedAgent.setHighlighted( true );
						}
						
						InspectionPanel.this.repaintNetworkVisFrame();
					}

					@Override
					public void connectionSeleted( ConnectionSelectedEvent connSelectedEvent ) {
						InspectionPanel.this.resetNetworkHighlights();
						InspectionPanel.this.txHistoryTable.clearAll();

						connSelectedEvent.getSelectedConnection().setHighlighted( true );
						InspectionPanel.this.addTXOfConnection( connSelectedEvent.getSelectedConnection() );
						
						InspectionPanel.this.repaintNetworkVisFrame();
					}
				} ); 
			this.netVisFrame.setNetworkRenderPanel( networkPanel );
			this.netVisFrame.setTitle( "Agent Network (" + this.getTitleExtension() + ")" );
		}
	}
	
	private void updateStats( int succTx, int noSuccTX, int totalTX, int totalNotSuccTx, long calculationTime ) {
		this.succTxCounterLabel.setText( Utils.DECIMAL_LARGEVALUES_FORMATTER.format( succTx ) );
		this.failedTxCounterLabel.setText( Utils.DECIMAL_LARGEVALUES_FORMATTER.format( noSuccTX ) );
		this.totalTxCounterLabel.setText( Utils.DECIMAL_LARGEVALUES_FORMATTER.format( totalTX ) );
		this.totalfailedTxCounterLabel.setText( Utils.DECIMAL_LARGEVALUES_FORMATTER.format( totalNotSuccTx )  );
		this.computationTimeLabel.setText( Utils.DECIMAL_2_DIGITS_FORMATTER.format( calculationTime / 1000.0 ) + " sec." );
		this.sweepsCounterLabel.setText( Utils.DECIMAL_2_DIGITS_FORMATTER.format( this.auction.getSweepCountMean() ) + " (" + 
				Utils.DECIMAL_2_DIGITS_FORMATTER.format( this.auction.getSweepCountStd() ) + ")" );
	}
	
	private void advanceTxFinished() {
		this.nextTxButton.setEnabled( true );
		this.advance10TxButton.setEnabled( true );
		this.advance100TxButton.setEnabled( true );

		this.pauseButton.setSelected( true );
		this.pauseButton.setText( "Paused" );

		this.offerBook.offerBookChanged();
	}
	
	private void setMarketMechanisms() {
		this.markets.setABM( this.abmMarketCheck.isSelected() );
		this.markets.setLoanMarket( this.loanCashMarketCheck.isSelected() );
		this.markets.setCollateralMarket( this.collateralMarketCheck.isSelected() );
		this.markets.setBP( this.bpMechanismCheck.isSelected() );
	}
	
	private void handleImportanceSampling() {
		NetworkCreator creator = (NetworkCreator) this.topologySelection.getSelectedItem();
		if ( this.importanceSamplingCheck.isSelected() ) {
			creator.createImportanceSampling( this.agentNetwork, this.markets );
		} else {
			Iterator<Agent> iter = this.agentNetwork.iterator();
			while ( iter.hasNext() ) {
				iter.next().resetOfferingLimits();
			}
		}
	}

	private void createAgents() {
		int agentCount = (int) this.agentCountSpinner.getValue();
		this.markets = new Markets( this.loanTypeSelection.getItemAt( this.loanTypeSelection.getSelectedIndex() ) );
		this.setMarketMechanisms();
		
		NetworkCreator creator = (NetworkCreator) this.topologySelection.getSelectedItem();
		this.agentNetwork = creator.createNetwork( new AgentFactoryImpl( agentCount, this.markets ) );
		
		this.handleImportanceSampling();
		
		this.txHistoryTable.clearAll();
		this.successfulMarkets.clear();
		
		this.agentWealthPanel.setAgents( this.agentNetwork.getOrderedList() );
		this.marketsTimeVisualizer.repaint();
		this.marketsAccuVisualizer.repaint();
		
		// close opened offer-books because agents changed (number of agents,...)
		this.offerBook.agentsChanged( this.agentNetwork.getOrderedList(), this.getTitleExtension() );
		
		this.updateNetworkVisualisationFrame();
	}
	
	private void toggleInspection() {
		// no simulation already running...
		if ( null == this.simulationThread ) {
			// if there was a simulation-run before: reset the agents
			this.agentNetwork.reset();
			
			// sort TX-ID descending initially to show new TXs first. one call seems not to be enough => do 2 times :D
			this.txHistoryTable.getRowSorter().toggleSortOrder( 0 );
			this.txHistoryTable.getRowSorter().toggleSortOrder( 0 );
				
			this.successfulTx.clear();
			this.successfulMarkets.clear();
			this.txHistoryTable.clearAll();
	
			// disable controls, to prevent changes by user
			this.agentCountSpinner.setEnabled( false );
			this.loanTypeSelection.setEnabled( false );
			this.topologySelection.setEnabled( false );
			this.optimismSelection.setEnabled( false );
			// TODO this.recreateButton.setEnabled( false );
			this.importanceSamplingCheck.setEnabled( false );
			
			// reset controls
			this.inspectionButton.setText( "Stop Inspection" );
			this.inspectionButton.setEnabled( true );
			
			this.nextTxButton.setEnabled( true );
			this.advance10TxButton.setEnabled( true );
			this.advance100TxButton.setEnabled( true );
			this.advcanceModeSelection.setEnabled( true );
			this.matchingTypeSelection.setEnabled( true );
			
			this.nextTxButton.setEnabled( true );
			this.advance10TxButton.setEnabled( true );
			this.advance100TxButton.setEnabled( true );
			
			this.pauseButton.setText( "Paused" );
			this.pauseButton.setSelected( true );
			this.pauseButton.setEnabled( true );
			
			this.auction = new Auction( this.agentNetwork );
			
			// let simulation run in a separate thread to prevent blocking of gui
			this.simulationThread = new InspectionThread( this.auction, new InspectionObserver() {
				@Override
				public void advanceTxFinished() {
					// running in thread => need to update SWING through SwingUtilities.invokeLater
					SwingUtilities.invokeLater( new Runnable() {
						@Override
						public void run() {
							InspectionPanel.this.advanceTxFinished();
						}
					});
				}

				@Override
				public MatchingType getMatchingType() {
					return (MatchingType) InspectionPanel.this.matchingTypeSelection.getSelectedItem();
				}

				@Override
				public boolean isKeepAgentHistory() {
					return InspectionPanel.this.keepAgentHistoryCheck.isSelected();
				}

				@Override
				public void repaint() {
					InspectionPanel.this.repaint();
				}

				@Override
				public void addSuccessfulTX(Transaction tx, boolean forceRepaint ) {
					InspectionPanel.this.addSuccessfulTX( tx, forceRepaint );
				}

				@Override
				public void updateEquilibriumStats(EquilibriumStatistics stats) {
					InspectionPanel.this.equilibriumInfoPanel.setStats( stats );
				}

				@Override
				public void simulationTerminated() {
					InspectionPanel.this.simulationTerminated();
				}

				@Override
				public void resumeFromPause() {
					InspectionPanel.this.txHistoryTable.restore( InspectionPanel.this.successfulTx );
				}

				@Override
				public void updateStats(int succTx, int noSuccTX, int totalTX, int totalNotSuccTx, long calculationTime) {
					InspectionPanel.this.updateStats(succTx, noSuccTX, totalTX, totalNotSuccTx, calculationTime);
				}
			} );
			
			this.simulationThread.startSimulation();

			this.marketsTimeVisualizer.repaint();
			this.marketsAccuVisualizer.repaint();
			this.agentWealthPanel.repaint();
			
		// simulation is running
		} else {
			this.simulationThread.stopSimulation();
			this.simulationThread = null;

			// simulation has finished => enable controls
			this.inspectionButton.setText( "Start Inspection" );
			this.nextTxButton.setEnabled( false );
			this.advance10TxButton.setEnabled( false );
			this.advance100TxButton.setEnabled( false );
			this.advcanceModeSelection.setEnabled( false );
			this.matchingTypeSelection.setEnabled( false );
			this.pauseButton.setEnabled( false );
			
			this.agentCountSpinner.setEnabled( true );
			this.loanTypeSelection.setEnabled( true );
			this.topologySelection.setEnabled( true );
			this.optimismSelection.setEnabled( true );
			// TODO this.recreateButton.setEnabled( true );
			this.importanceSamplingCheck.setEnabled( true );
		}
	}
	
	private void highlightTx( Transaction tx ) {
		// no need for anything highlighting-related when no network-panel available
		if ( null == this.netVisFrame || false == this.netVisFrame.isVisible()  ) {
			return;
		}
	
		this.resetNetworkHighlights();
		
		AskOffering askOffering = tx.getMatch().getSellOffer();
		BidOffering bidOffering = tx.getMatch().getBuyOffer();
		
		Agent a1 = askOffering.getAgent();
		Agent a2 = bidOffering.getAgent();
		
		AgentConnection conn = this.agentNetwork.getConnection( a1, a2 );
		if ( conn.getWeight() == Double.MAX_VALUE ) {
			conn.setWeight( 1.0 );
		}
		
		conn.setHighlighted( true );
		a1.setHighlighted( true );
		a2.setHighlighted( true );

		// both iterators have same order and same length
		Iterator<Agent> agentsIter = this.agentNetwork.iterator();
		Iterator<Agent> finalAgentsIter = tx.getFinalAgents().iterator();
		while ( agentsIter.hasNext() && finalAgentsIter.hasNext() ) {
			agentsIter.next().setCantTrade( finalAgentsIter.next().isCantTrade() );
		}

		InspectionPanel.this.repaintNetworkVisFrame();
	}
	
	private void resetNetworkHighlights() {
		Iterator<Agent> agentsIter = this.agentNetwork.iterator();
		while ( agentsIter.hasNext() ) {
			agentsIter.next().setHighlighted( false );
		}
		
		Iterator<AgentConnection> connIter = this.agentNetwork.connectionIterator();
		while ( connIter.hasNext() ) {
			connIter.next().setHighlighted( false );
		}
	}
	
	private void addTXOfConnection( AgentConnection c ) {
		for ( int i = 0; i < InspectionPanel.this.successfulTx.size(); ++i ) {
			Transaction tx = InspectionPanel.this.successfulTx.get( i );
			
			Agent a1 = tx.getMatch().getSellOffer().getAgent();
			Agent a2 = tx.getMatch().getBuyOffer().getAgent();

			if ( c == InspectionPanel.this.agentNetwork.getConnection( a1, a2 ) ) {
				a1.setHighlighted( true );
				a2.setHighlighted( true );
				
				InspectionPanel.this.txHistoryTable.addTx( tx );
			}
		}
	}
	
	private class AdvanceTxButtonsActionListener implements ActionListener {
		private int txCount;
		
		public AdvanceTxButtonsActionListener(int txCount) {
			this.txCount = txCount;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			InspectionPanel.this.resetNetworkHighlights();
			InspectionPanel.this.repaintNetworkVisFrame();
			
			InspectionPanel.this.nextTxButton.setEnabled( false );
			InspectionPanel.this.advance10TxButton.setEnabled( false );
			InspectionPanel.this.advance100TxButton.setEnabled( false );
			
			InspectionPanel.this.pauseButton.setSelected( false );
			InspectionPanel.this.pauseButton.setText( "Pause" );
			
			InspectionPanel.this.txHistoryTable.restore( InspectionPanel.this.successfulTx );

			InspectionPanel.this.simulationThread.advanceTX( ( AdvanceMode ) InspectionPanel.this.advcanceModeSelection.getSelectedItem(), this.txCount );
		}
	}
	
	private void repaintNetworkVisFrame() {
		if ( null != this.netVisFrame && this.netVisFrame.isVisible() )
			this.netVisFrame.repaint();
	}
}
