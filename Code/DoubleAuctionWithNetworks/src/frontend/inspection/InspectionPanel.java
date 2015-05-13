package frontend.inspection;

import java.awt.BorderLayout;
import java.awt.Color;
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

import javax.swing.BorderFactory;
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
import backend.Auction.EquilibriumStatistics;
import backend.Auction.MatchingType;
import backend.agents.Agent;
import backend.agents.AgentFactoryImpl;
import backend.agents.network.AgentConnection;
import backend.agents.network.AgentNetwork;
import backend.markets.LoanType;
import backend.markets.Markets;
import backend.offers.AskOffering;
import backend.offers.BidOffering;
import backend.tx.Match;
import backend.tx.Transaction;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import frontend.inspection.InspectionThread.AdvanceMode;
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
import frontend.replication.EquilibriumInfoPanel;
import frontend.visualisation.MarketsVisualizer;
import frontend.visualisation.WealthVisualizer;

@SuppressWarnings("serial")
public class InspectionPanel extends JPanel implements ActionListener, ChangeListener {
	
	private AgentNetwork agentNetwork;
	private Markets markets;
	
	private JCheckBox keepSuccTXHighCheck;
	
	private JCheckBox abmMarketCheck;
	private JCheckBox loanCashMarketCheck;
	private JCheckBox collateralMarketCheck;
	private JCheckBox bpMechanismCheck;
	private JCheckBox importanceSamplingCheck;
	
	private JCheckBox keepAgentHistoryCheck;
	
	private JButton inspectionButton;
	private JButton recreateButton;
	private JButton nextTxButton;
	private JButton advance10TxButton;
	private JButton advance100TxButton;
	private JButton openOfferBookButton;
	private JToggleButton pauseButton;
	private JToggleButton toggleNetworkPanelButton;

	private JPanel visualizationPanel;
	private JPanel networkPanel;
	
	private JComboBox<NetworkCreator> topologySelection;
	private JComboBox<String> layoutSelection;
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

	private JTabbedPane visualizersTabbedPane;
	
	private WealthVisualizer agentWealthPanel;
	private MarketsVisualizer marketsVisualizer;
	private NetworkRenderPanel networkVisPanel;
	private EquilibriumInfoPanel equilibriumInfoPanel;
	
	private OfferBook offerBook;
	
	private TxHistoryTable txHistoryTable;
	
	private Timer spinnerChangedTimer;
	
	private InspectionThread simulationThread;
	
	private Agent selectedAgent;
	
	private List<Transaction> successfulTx;
	private List<Match> successfulMatches;
	
	private long lastRepaintTime;
	
	private static final DecimalFormat COMP_TIME_FORMAT = new DecimalFormat("0.00");
	public static final DecimalFormat AGENT_H_FORMAT = new DecimalFormat("0.000");
	public static final DecimalFormat TRADING_VALUES_FORMAT = new DecimalFormat("0.0000");
	
	private static final int AGENTS_COUNT_HIDE_NETWORK_PANEL = 51;
	private static final int REPAINT_WEALTH_WHENRUNNING_INTERVAL = 1000;
	
	public InspectionPanel() {
		this.markets = new Markets();
		this.successfulTx = new ArrayList<Transaction>();
		this.successfulMatches = new ArrayList<Match>();
		
        this.setLayout( new GridBagLayout() );
        
        this.createControls();
        this.createAgents();
	}
	
	public void restoreTXHistoryTable() {
		this.txHistoryTable.restore( this.successfulTx );
	}
	
	public MatchingType getSelectedMatchingType() {
		return (MatchingType) this.matchingTypeSelection.getSelectedItem();
	}
	
	public boolean isKeepAgentHistory() {
		return this.keepAgentHistoryCheck.isSelected();
	}
	
	public void simulationTerminated() {
		// simulation was running, toggle will switch all gui-stuff to "stoped"
		this.toggleInspection();
		this.agentWealthPanel.repaint();
		this.networkPanel.repaint();
		JOptionPane.showMessageDialog( this, "No Agent is able to trade with any of its neighbours - Simulation stoped.", 
				"Equilibrium Reached", JOptionPane.INFORMATION_MESSAGE );
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		this.createAgents();
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
						InspectionPanel.this.createAgents();
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

	private void createControls() {
		// instancing of components ////////////////////////////////////
		GridBagConstraints c = new GridBagConstraints();

		this.visualizersTabbedPane = new JTabbedPane();
		this.visualizationPanel = new JPanel( new GridBagLayout() );
		this.networkPanel = new JPanel( new BorderLayout() );
		this.offerBook = new OfferBook();
		this.equilibriumInfoPanel = new EquilibriumInfoPanel();
		this.marketsVisualizer = new MarketsVisualizer( this.successfulMatches );
		this.agentWealthPanel = new WealthVisualizer();

		JPanel controlsPanel = new JPanel();
		JPanel txInfoPanel = new JPanel( new GridBagLayout() );
		JPanel networkVisControlsPanel = new JPanel();
		
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
		
		this.layoutSelection = new JComboBox<String>( new String[] { "Circle", "KK" } );
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
		
		this.recreateButton = new JButton( "Recreate" );
		this.inspectionButton = new JButton( "Start Inspection" );
		this.nextTxButton = new JButton( "Next TX" );
		this.advance10TxButton = new JButton( "Advance 10 TXs" );
		this.advance100TxButton = new JButton( "Advance 100 TXs" );
		
		this.openOfferBookButton = new JButton( "Open Offer-Book" );
		this.pauseButton = new JToggleButton ( "Pause" );
		this.toggleNetworkPanelButton = new JToggleButton ( "Hide Network" );
		
		this.keepSuccTXHighCheck = new JCheckBox( "Keep TXs Highlighted" );
		this.keepSuccTXHighCheck.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				if ( null == InspectionPanel.this.simulationThread ) {
					return;
				}
				
				if ( InspectionPanel.this.networkVisPanel.isVisible() ) {
					InspectionPanel.this.networkVisPanel.setKeepTXHighlighted( InspectionPanel.this.keepSuccTXHighCheck.isSelected() );
					InspectionPanel.this.networkVisPanel.repaint();
				}
			}
		});
		
		this.abmMarketCheck = new JCheckBox( "Asset/Loan Market" );
		this.loanCashMarketCheck = new JCheckBox( "Loan/Cash Market" );
		this.collateralMarketCheck = new JCheckBox( "Collateral/Cash Market" );
		this.bpMechanismCheck = new JCheckBox( "Bonds Pledgeability" );
		this.importanceSamplingCheck = new JCheckBox( "Importance-Sampling" );
		
		this.keepAgentHistoryCheck = new JCheckBox( "Keep Agent History" );

		this.txHistoryTable = new TxHistoryTable();
		JScrollPane txHistoryScrollPane = new JScrollPane( this.txHistoryTable );
		txHistoryScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
		txHistoryScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		
		// setting properties of components ////////////////////////////////////
		this.visualizersTabbedPane.addTab( "Agents", this.agentWealthPanel );
		this.visualizersTabbedPane.addTab( "Markets", this.marketsVisualizer );
		
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
		
		this.toggleNetworkPanelButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if ( InspectionPanel.this.toggleNetworkPanelButton.isSelected() ) {
					InspectionPanel.this.toggleNetworkPanelButton.setText( "Show Network" );
					InspectionPanel.this.networkPanel.setVisible( false );
					
				} else {
					InspectionPanel.this.toggleNetworkPanelButton.setText( "Hide Network" );
					InspectionPanel.this.networkPanel.setVisible( true );
					InspectionPanel.this.createLayout();
				}
			}
		});
		
		this.layoutSelection.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if ( null == InspectionPanel.this.agentNetwork ) {
					return;
				}
				
				InspectionPanel.this.createLayout();
			}
		} );

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
		
		this.recreateButton.addActionListener( this );
		this.optimismSelection.addActionListener( this );
		this.loanTypeSelection.addActionListener( this );
		
		this.agentCountSpinner.addChangeListener( this );
		
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
		
		networkVisControlsPanel.add( this.recreateButton );
		networkVisControlsPanel.add( this.layoutSelection );
		networkVisControlsPanel.add( this.keepSuccTXHighCheck );
		
		this.networkPanel.add( networkVisControlsPanel, BorderLayout.NORTH ); 
		    
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
	    txLabelsPanel.add( this.equilibriumInfoPanel, c );
		

	    
	    c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		txControlPanel.add( this.keepAgentHistoryCheck, c );
		c.gridx = 0;
		c.gridy = 1;
		txControlPanel.add( this.openOfferBookButton, c );
	    c.gridx = 0;
		c.gridy = 2;
		txControlPanel.add( this.pauseButton, c );
	    c.gridx = 0;
		c.gridy = 3;
		txControlPanel.add( this.nextTxButton, c );
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 4;
		txControlPanel.add( this.advance10TxButton, c );
		c.gridx = 1;
		c.gridy = 4;
		txControlPanel.add( this.advance100TxButton, c );
		c.gridx = 0;
		c.gridy = 5;
		txControlPanel.add( this.advcanceModeSelection, c );
	    c.gridx = 1;
		c.gridy = 5;
		txControlPanel.add( this.matchingTypeSelection, c );
		c.gridx = 0;
		c.gridy = 6;
		c.gridwidth = 2;
		txControlPanel.add( this.toggleNetworkPanelButton, c );
	    
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
		/*
		c.gridx = 1;
		c.gridy = 0;
		this.visualizationPanel.add( this.visualizersTabbedPane, c );
		*/
		
		c.weighty = 1.0;
		c.gridheight = 10;
	    c.gridy = 1;
		this.add( this.visualizationPanel, c );
		
		c.weighty = 0.1;
		c.gridheight = 2;
	    c.gridy = 11;
		this.add( txInfoPanel, c );
	}
	
	void addSuccessfulTX( Transaction tx, boolean forceRedraw ) {
		long currMillis = System.currentTimeMillis();
		if ( InspectionPanel.REPAINT_WEALTH_WHENRUNNING_INTERVAL < currMillis - this.lastRepaintTime || forceRedraw ) {
			this.agentWealthPanel.setAgents( this.agentNetwork.getOrderedList() );
			this.marketsVisualizer.repaint();
			
			this.lastRepaintTime = currMillis;
		}

		this.successfulMatches.add( tx.getMatch() );
		
		if ( this.keepAgentHistoryCheck.isSelected() ) {
			this.successfulTx.add( tx );
			this.txHistoryTable.addTx( tx );

			this.highlightTx( tx );
		}
	}
	
	void updateEquilibriumStats( EquilibriumStatistics stats ) {
		this.equilibriumInfoPanel.setStats( stats );
	}
	
	void updateTXCounter( int succTx, int noSuccTX, int totalTX, int totalNotSuccTx, long calculationTime ) {
		this.succTxCounterLabel.setText( "" + succTx );
		this.failedTxCounterLabel.setText( "" + noSuccTX );
		this.totalTxCounterLabel.setText( "" + totalTX );
		this.totalfailedTxCounterLabel.setText( "" + totalNotSuccTx );
		this.computationTimeLabel.setText( COMP_TIME_FORMAT.format( calculationTime / 1000.0 ) + " sec." );
	}
	
	void advanceTxFinished() {
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
	
	public String getTitleExtension() {
		int agentCount = (int) this.agentCountSpinner.getValue();
		NetworkCreator creator = (NetworkCreator) this.topologySelection.getSelectedItem();
		return creator.name() + ", " + agentCount + " Agents";
	}
	
	private void createAgents() {
		int agentCount = (int) this.agentCountSpinner.getValue();
		this.markets = new Markets( this.loanTypeSelection.getItemAt( this.loanTypeSelection.getSelectedIndex() ) );
		this.setMarketMechanisms();
		
		NetworkCreator creator = (NetworkCreator) this.topologySelection.getSelectedItem();
		this.agentNetwork = creator.createNetwork( new AgentFactoryImpl( agentCount, this.markets ) );
		
		this.handleImportanceSampling();
		
		this.txHistoryTable.clearAll();
		this.successfulMatches.clear();
		
		this.agentWealthPanel.setAgents( this.agentNetwork.getOrderedList() );
		this.marketsVisualizer.repaint();
		
		// close opened offer-books because agents changed (number of agents,...)
		this.offerBook.agentsChanged( this.agentNetwork.getOrderedList(), this.getTitleExtension() );
		
		// need to create the layout too, will do the final pack-call on this frame
		this.createLayout();
	}
	
	@SuppressWarnings("unchecked")
	private void createLayout() {
		Class<? extends Layout<Agent, AgentConnection>> layout = (Class<? extends Layout<Agent, AgentConnection>>) CircleLayout.class;
		
		if ( 1 == this.layoutSelection.getSelectedIndex() ) {
			layout = (Class<? extends Layout<Agent, AgentConnection>>) KKLayout.class;
		}
		
		this.toggleNetworkPanelButton.setVisible( this.agentNetwork.size() <= InspectionPanel.AGENTS_COUNT_HIDE_NETWORK_PANEL );
		
		// don't show network-panel when too many agents
		if ( this.agentNetwork.size() > InspectionPanel.AGENTS_COUNT_HIDE_NETWORK_PANEL || 
				this.toggleNetworkPanelButton.isSelected() ) {
			this.networkPanel.setVisible( false );
			this.revalidate();
			return;

		} else {
			this.networkPanel.setVisible( true );
		}
		
		this.recreateButton.setVisible( this.agentNetwork.isRandomNetwork() );
		
		// remove network-visualization panel when already there
		if ( null != this.networkVisPanel ) {
			this.networkPanel.remove( this.networkVisPanel );
		}
		
		this.networkVisPanel = InspectionPanel.this.agentNetwork.getNetworkRenderingPanel( layout, new INetworkSelectionObserver() {
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
					
					if ( InspectionPanel.this.networkVisPanel.isVisible() ) {
						InspectionPanel.this.networkVisPanel.repaint();
					}
					
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
				
				if ( InspectionPanel.this.networkVisPanel.isVisible() ) {
					InspectionPanel.this.networkVisPanel.repaint();
				}
			}

			@Override
			public void connectionSeleted( ConnectionSelectedEvent connSelectedEvent ) {
				InspectionPanel.this.resetNetworkHighlights();
				InspectionPanel.this.txHistoryTable.clearAll();

				connSelectedEvent.getSelectedConnection().setHighlighted( true );
				InspectionPanel.this.addTXOfConnection( connSelectedEvent.getSelectedConnection() );
				
				if ( InspectionPanel.this.networkVisPanel.isVisible() ) {
					InspectionPanel.this.networkVisPanel.repaint();
				}
			}
		} );

		this.networkPanel.setBorder( BorderFactory.createTitledBorder( BorderFactory.createLineBorder( Color.black ), "" ) );
		
		this.networkPanel.add( this.networkVisPanel, BorderLayout.CENTER );
		this.networkPanel.revalidate();
		this.revalidate();
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
			this.successfulMatches.clear();
			this.txHistoryTable.clearAll();
	
			// disable controls, to prevent changes by user
			this.agentCountSpinner.setEnabled( false );
			this.loanTypeSelection.setEnabled( false );
			this.topologySelection.setEnabled( false );
			this.optimismSelection.setEnabled( false );
			this.recreateButton.setEnabled( false );
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
			
			Auction auction = new Auction( this.agentNetwork );
			
			// let simulation run in a separate thread to prevent blocking of gui
			this.simulationThread = new InspectionThread( auction, this );
			this.simulationThread.startSimulation();
			
			this.networkVisPanel.repaint();
			this.marketsVisualizer.repaint();
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
			this.recreateButton.setEnabled( true );
			this.importanceSamplingCheck.setEnabled( true );
		}
	}
	
	private void highlightTx( Transaction tx ) {
		// no need for anything highlighting-related when no network-panel available
		if ( false == this.networkPanel.isVisible() ) {
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

		this.networkVisPanel.repaint();
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
			
			if ( InspectionPanel.this.networkVisPanel.isVisible() ) {
				InspectionPanel.this.networkVisPanel.repaint();
			}
			
			InspectionPanel.this.nextTxButton.setEnabled( false );
			InspectionPanel.this.advance10TxButton.setEnabled( false );
			InspectionPanel.this.advance100TxButton.setEnabled( false );
			
			InspectionPanel.this.pauseButton.setSelected( false );
			InspectionPanel.this.pauseButton.setText( "Pause" );
			
			InspectionPanel.this.txHistoryTable.restore( InspectionPanel.this.successfulTx );

			InspectionPanel.this.simulationThread.advanceTX( ( AdvanceMode ) InspectionPanel.this.advcanceModeSelection.getSelectedItem(), this.txCount );
		}
	}
}
