package gui;

import gui.networkCreators.AscendingConnectedCreator;
import gui.networkCreators.AscendingFullShortcutsCreator;
import gui.networkCreators.AscendingRandomShortcutsCreator;
import gui.networkCreators.AscendingRegularShortcutsCreator;
import gui.networkCreators.BarbasiAlbertCreator;
import gui.networkCreators.ErdosRenyiCreator;
import gui.networkCreators.FullyConnectedCreator;
import gui.networkCreators.HubConnectedCreator;
import gui.networkCreators.INetworkCreator;
import gui.networkCreators.MaximumHubCreator;
import gui.networkCreators.MedianHubCreator;
import gui.networkCreators.ThreeMedianHubsCreator;
import gui.networkCreators.WattStrogatzCreator;
import gui.visualisation.AgentSelectedEvent;
import gui.visualisation.ConnectionSelectedEvent;
import gui.visualisation.INetworkSelectionObserver;
import gui.visualisation.NetworkRenderPanel;

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

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import agents.Agent;
import agents.AgentWithLoans;
import agents.IAgentFactory;
import agents.markets.Asset;
import agents.markets.Loans;
import agents.network.AgentConnection;
import agents.network.AgentNetwork;
import doubleAuction.Auction;
import doubleAuction.Auction.MatchingType;
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
	private JCheckBox cashAssetOnlyCheck;
	
	private JButton simulateButton;
	private JButton recreateButton;
	private JButton nextTxButton;
	private JToggleButton pauseButton;
	private JToggleButton toggleNetworkPanelButton;
	
	private NetworkRenderPanel networkPanel;
	private JPanel agentWealthPanel;
	private JPanel visualizationPanel;
	
	private JComboBox<INetworkCreator> topologySelection;
	private JComboBox<String> layoutSelection;
	private JComboBox<String> optimismSelection;
	private JComboBox<MatchingType> matchingTypeSelection;
	
	private JSpinner agentCountSpinner;
	
	private JLabel computationTimeLabel;
	
	private JLabel succTxCounterLabel;
	private JLabel totalTxCounterLabel;
	private JLabel noSuccTxCounterLabel;
	
	private JLabel finalAssetAskLabel;
	private JLabel finalAssetBidLabel;
	private JLabel finalLoanAskLabel;
	private JLabel finalLoanBidLabel;

	private JTable txHistoryTable;
	private DefaultTableModel txTableModel;
	
	private Timer spinnerChangedTimer;
	
	private SimulationThread simulationThread;
	
	private Agent selectedAgent;
	
	private List<Transaction> successfulTx;
	
	private long lastRepaintTime;
	
	private static final DecimalFormat COMP_TIME_FORMAT = new DecimalFormat("0.00");
	private static final DecimalFormat AGENT_H_FORMAT = new DecimalFormat("0.000");
	private static final DecimalFormat TRADING_VALUES_FORMAT = new DecimalFormat("0.000000");
	
	private static final int AGENTS_COUNT_HIDE_NETWORK_PANEL = 50;
	private static final int REPAINT_WEALTH_WHENRUNNING_INTERVAL = 1000;
	
	public MainWindow() {
		super("Continuous Double-Auctions");
		
		this.successfulTx = new ArrayList<Transaction>();
		
		this.setExtendedState( JFrame.MAXIMIZED_BOTH ); 
		this.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        this.getContentPane().setLayout( new GridBagLayout() );
        
        this.createControlsPanel();
        this.createAgents();
       
        this.pack();
        this.setVisible( true );
	}
	
	public MatchingType getSelectedMatchingType() {
		return (MatchingType) this.matchingTypeSelection.getSelectedItem();
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
	
	@SuppressWarnings("rawtypes")
	private void createControlsPanel() {
		// instancing of components ////////////////////////////////////
		GridBagConstraints c = new GridBagConstraints();

		this.visualizationPanel = new JPanel( new GridBagLayout() );
		JPanel controlsPanel = new JPanel();
		JPanel txInfoPanel = new JPanel( new GridBagLayout() );

		this.topologySelection = new JComboBox<INetworkCreator>();
		this.topologySelection.addItem( new AscendingConnectedCreator() );
		this.topologySelection.addItem( new AscendingFullShortcutsCreator() );
		this.topologySelection.addItem( new AscendingRegularShortcutsCreator() );
		this.topologySelection.addItem( new AscendingRandomShortcutsCreator() );
		this.topologySelection.addItem( new FullyConnectedCreator() );
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
		
		this.agentCountSpinner = new JSpinner( new SpinnerNumberModel( 30, 10, 1000, 10 ) );

		JLabel succTxCounterInfoLabel = new JLabel( "Successful TX: " );
		JLabel noSuccTxCounterInfoLabel = new JLabel( "No Succ. TX: " );
		JLabel totalTxCounterInfoLabel = new JLabel( "Total TX: " );
		JLabel finalAssetAskInfoLabel = new JLabel( "Asset Ask Price:" );
		JLabel finalAssetBidInfoLabel = new JLabel( "Asset Bid Price:" );
		JLabel finalLoanAskInfoLabel = new JLabel( "Loan Ask Price:" );
		JLabel finalLoanBidInfoLabel = new JLabel( "Loan Bid Price:"  );
		
		this.computationTimeLabel = new JLabel( "Computation Time: -" );
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
		this.toggleNetworkPanelButton = new JToggleButton ( "Hide Network" );
		
		this.keepSuccTXHighCheck = new JCheckBox( "Keep TXs Highlighted" );
		this.keepSuccTXHighCheck.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				if ( null == MainWindow.this.simulationThread ) {
					return;
				}
				
				if ( null != MainWindow.this.networkPanel ) {
					MainWindow.this.networkPanel.setKeepTXHighlighted( MainWindow.this.keepSuccTXHighCheck.isSelected() );
					MainWindow.this.networkPanel.repaint();
				}
			}
		});
		
		this.cashAssetOnlyCheck = new JCheckBox( "Cash-Asset Market Only" );
		this.cashAssetOnlyCheck.setSelected( false );
		this.cashAssetOnlyCheck.addActionListener( this );
		
		Class[] columnClasses = new Class[]{ Integer.class, String.class, String.class, String.class,
				String.class, String.class, String.class, String.class };
		
		this.txTableModel = new DefaultTableModel(
				new Object[] { "TX", "Buyer", "Seller", "Asset Amount",
						"Asset Price", "Loan Amount", "Loan Price", "Loan Type" }, 0 ) {

		    @Override
		    public boolean isCellEditable(int row, int column) {
		       return false;
		    }

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return columnClasses[ columnIndex ];
			}
		};

		TableRowSorter<DefaultTableModel> rowSorter = new TableRowSorter<>( this.txTableModel );
		new TXColumnComparator( 5, rowSorter );
		new TXColumnComparator( 6, rowSorter );
		new TXColumnComparator( 7, rowSorter );
		
		this.txHistoryTable = new JTable( this.txTableModel );
		this.txHistoryTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		this.txHistoryTable.setAutoCreateRowSorter( true );
		this.txHistoryTable.setRowSorter( rowSorter );
		
		JScrollPane txHistoryScrollPane = new JScrollPane( this.txHistoryTable );
		txHistoryScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
		txHistoryScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		
		// setting properties of components ////////////////////////////////////
		this.nextTxButton.setVisible( false );
		this.pauseButton.setVisible( false );

		// setting up event-listeners of components ////////////////////////////////////
		this.txHistoryTable.getSelectionModel().addListSelectionListener( new ListSelectionListener() {
			@Override
			public void valueChanged( ListSelectionEvent e ) {
				if (e.getValueIsAdjusting() == false) {
					int rowIndex = MainWindow.this.txHistoryTable.getSelectedRow();
					if ( -1 == rowIndex ) {
						MainWindow.this.finalAssetAskLabel.setText( "-" );
						MainWindow.this.finalAssetBidLabel.setText( "-" );
						MainWindow.this.finalLoanAskLabel.setText( "-" );
						MainWindow.this.finalLoanBidLabel.setText( "-" );

						return;
					}
					
					int txIndex = (int) MainWindow.this.txHistoryTable.getValueAt( rowIndex, 0 );

					// starts with 1
					txIndex--;
					
					Transaction tx = MainWindow.this.successfulTx.get( txIndex );
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
				
				if ( null != MainWindow.this.networkPanel )
					MainWindow.this.networkPanel.repaint();

				MainWindow.this.nextTxButton.setEnabled( false );
				
				MainWindow.this.pauseButton.setSelected( false );
				MainWindow.this.pauseButton.setText( "Pause" );
				
				MainWindow.this.restoreTXHistoryList();			

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
		
		this.toggleNetworkPanelButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if ( MainWindow.this.toggleNetworkPanelButton.isSelected() ) {
					MainWindow.this.toggleNetworkPanelButton.setText( "Show Network" );
				} else {
					MainWindow.this.toggleNetworkPanelButton.setText( "Hide Network" );
				}
				
				MainWindow.this.createLayout();
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

		this.topologySelection.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				INetworkCreator newSelected = (INetworkCreator) MainWindow.this.topologySelection.getSelectedItem();

				// creator signals to be created immediately
				if ( newSelected.createInstant() ) {
					MainWindow.this.createAgents();
				
				// creator signals to defer creation for later (e.g. after user-input of parameters
				// the creator needs)
				} else {
					// defer creation and provide creator with a callback to continue creation
					newSelected.deferCreation( new Runnable() {
						@Override
						public void run() {
							MainWindow.this.createAgents();
						}
					});
				}
			}
		});
		
		this.recreateButton.addActionListener( this );
		this.optimismSelection.addActionListener( this );
		this.agentCountSpinner.addChangeListener( this );

		// adding components ////////////////////////////////////
		controlsPanel.add( this.toggleNetworkPanelButton );
		controlsPanel.add( this.recreateButton );
		controlsPanel.add( this.agentCountSpinner );
		//controlsPanel.add( this.optimismSelection );
		controlsPanel.add( this.topologySelection );
		controlsPanel.add( this.layoutSelection );
		controlsPanel.add( this.matchingTypeSelection );
		controlsPanel.add( this.simulateButton );
		controlsPanel.add( this.pauseButton );
		controlsPanel.add( this.nextTxButton );
		controlsPanel.add( this.keepSuccTXHighCheck );
		controlsPanel.add( this.cashAssetOnlyCheck );
		controlsPanel.add( this.computationTimeLabel );

		JPanel txLabelsPanel = new JPanel( new GridBagLayout() );
		
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.5;
		c.ipadx = 10;
		
		c.gridx = 0;
	    c.gridy = 0;
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

		
		c.weightx = 0.8;
		c.weighty = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		c.ipady = 0;
		c.gridwidth = 8;
		c.fill = GridBagConstraints.BOTH;
		txInfoPanel.add( txHistoryScrollPane, c );
		
		c.weightx = 0.1;
		c.gridx = 8;
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
		this.getContentPane().add( controlsPanel, c );

		c.weighty = 1.0;
		c.gridheight = 10;
	    c.gridy = 1;
		this.getContentPane().add( this.visualizationPanel, c );
		
		c.weighty = 0.1;
		c.gridheight = 2;
	    c.gridy = 11;
		this.getContentPane().add( txInfoPanel, c );
	}
	
	void restoreTXHistoryList() {
		if ( this.txTableModel.getRowCount() != this.successfulTx.size() ) {
			this.txHistoryTable.clearSelection();
			
			this.txTableModel.setRowCount( 0 );
			this.txHistoryTable.revalidate();
			
			for ( Transaction tx : this.successfulTx ) {
				this.addTxToTable( tx );
			}
		}
	}
	
	void addSuccessfulTX( Transaction tx, boolean forceRedraw ) {
		long currMillis = System.currentTimeMillis();
		if ( MainWindow.REPAINT_WEALTH_WHENRUNNING_INTERVAL < currMillis - this.lastRepaintTime || forceRedraw ) {
			this.agentWealthPanel.repaint();
			this.lastRepaintTime = currMillis;
		}

		this.successfulTx.add( tx );
		this.addTxToTable( tx );

		this.highlightTx( tx );
	}
	
	void updateTXCounter( int succTx, int noSuccTX, int totalTX, long calculationTime ) {
		this.succTxCounterLabel.setText( "" + succTx );
		this.noSuccTxCounterLabel.setText( "" + noSuccTX );
		this.totalTxCounterLabel.setText( "" + totalTX );
		this.computationTimeLabel.setText( "Computation Time: " + COMP_TIME_FORMAT.format( calculationTime / 1000.0 ) + " sec." );
	}
	
	void nextTXFinished() {
		this.nextTxButton.setEnabled( true );
		this.pauseButton.setSelected( true );
		this.pauseButton.setText( "Paused" );
	}

	private void createAgents() {
		double assetPrice = 0.6;
		double consumEndow = 1.0;
		double assetEndow = 1.0;
		int agentCount = (int) this.agentCountSpinner.getValue();
		int optimismFunctionIndex = this.optimismSelection.getSelectedIndex();
		
		double[] J = new double[] { 0.2 };
		double[] initialLoanPrices = new double[] { 0.2 };
		
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
						double agentArea = ( ( ( halfAgentCount - this.i ) * ( halfAgentCount - this.i ) ) / 2.0 );
						
						if ( i <= halfAgentCount ) {
							agentArea = halfArea - agentArea;
							
						} else {
							agentArea = halfArea + agentArea;
						}
						
						optimism = agentArea / totalArea;
					}
					
					if ( MainWindow.this.cashAssetOnlyCheck.isSelected() ) {
						a = new Agent( i, optimism, consumEndow, assetEndow, asset );
					} else {
						a = new AgentWithLoans( this.i, optimism, consumEndow, assetEndow, loans, asset );
					}
					
					this.i++;
				}
				
				return a;
			}
		};
		
		INetworkCreator creator = (INetworkCreator) this.topologySelection.getSelectedItem();
		this.agents = creator.createNetwork( agentFactory );
		
		// if agent-wealth-visualisation panel is already there, remove it bevore adding a new instance
		if ( null != this.agentWealthPanel ) {
			this.visualizationPanel.remove( this.agentWealthPanel );
			this.agentWealthPanel = null;
		}

		this.agentWealthPanel = this.agents.getWealthVisualizer();
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.5;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weighty = 1.0;
		c.gridx = 1;
		c.gridy = 0;
		
		if ( this.agents.size() > MainWindow.AGENTS_COUNT_HIDE_NETWORK_PANEL ) {
			c.weightx = 1.0;
			c.gridx = 0;
		}
		
		this.visualizationPanel.add( this.agentWealthPanel, c );

		// if there are still items in table-model, delete them
		if ( 0 < this.txTableModel.getRowCount() ) {
			this.txTableModel.setRowCount( 0 );
			this.txHistoryTable.revalidate();
		}
		
		// need to create the layout too, will do the final pack-call on this frame
		this.createLayout();
	}
	
	@SuppressWarnings("unchecked")
	private void createLayout() {
		Class<? extends Layout<Agent, AgentConnection>> layout = (Class<? extends Layout<Agent, AgentConnection>>) CircleLayout.class;
		
		if ( 1 == this.layoutSelection.getSelectedIndex() ) {
			layout = (Class<? extends Layout<Agent, AgentConnection>>) KKLayout.class;
		}
		
		// remove network-visualization panel when already there
		if ( null != this.networkPanel ) {
			this.visualizationPanel.remove( MainWindow.this.networkPanel );
			this.networkPanel = null;
		}
		
		this.toggleNetworkPanelButton.setVisible( this.agents.size() <= MainWindow.AGENTS_COUNT_HIDE_NETWORK_PANEL );

		// don't show network-panel when too many agents
		if ( this.agents.size() > MainWindow.AGENTS_COUNT_HIDE_NETWORK_PANEL || 
				this.toggleNetworkPanelButton.isSelected() ) {
			
			this.keepSuccTXHighCheck.setVisible( false );
			this.recreateButton.setVisible( false );
			this.layoutSelection.setVisible( false );
			
			this.revalidate();
			return;
		}

		this.recreateButton.setVisible( this.agents.isRandomNetwork() );
		this.layoutSelection.setVisible( true );
		this.keepSuccTXHighCheck.setVisible( true );
		
		this.networkPanel = MainWindow.this.agents.getNetworkRenderingPanel( layout, new INetworkSelectionObserver() {
			@Override
			public void agentSeleted( AgentSelectedEvent agentSelectedEvent ) {
				// no simulation running yet, just highlight selected agent, its neighbours and their connections
				if ( null == MainWindow.this.simulationThread ) {
					MainWindow.this.resetNetworkHighlights();
					
					Agent selectedAgent = agentSelectedEvent.getSelectedAgent();
					selectedAgent.setHighlighted( true );
					
					Iterator<Agent> neighbourIter = MainWindow.this.agents.getNeighbors( selectedAgent );
					while ( neighbourIter.hasNext() ) {
						Agent neighbour = neighbourIter.next();
						neighbour.setHighlighted( true );
						
						MainWindow.this.agents.getConnection( selectedAgent, neighbour ).setHighlighted( true );
					}
					
					if ( null != MainWindow.this.networkPanel )
						MainWindow.this.networkPanel.repaint();
					
					return;
				}
				
				// when simulation thread is not paused, don't react on selection
				if ( false == MainWindow.this.simulationThread.isPause() ) {
					return;
				}
				
				MainWindow.this.resetNetworkHighlights();
				MainWindow.this.txTableModel.setRowCount( 0 );
				MainWindow.this.txHistoryTable.revalidate();

				// find path between two selected agents
				if ( agentSelectedEvent.isCtrlDownFlag() && null != MainWindow.this.selectedAgent ) {
					MainWindow.this.selectedAgent.setHighlighted( true );
					agentSelectedEvent.getSelectedAgent().setHighlighted( true );
					
					List<AgentConnection> path = MainWindow.this.agents.getPath( MainWindow.this.selectedAgent, agentSelectedEvent.getSelectedAgent() );
					// returns null when there is no path of successful transactions
					if ( null != path ) {
						for ( AgentConnection c : path ) {
							c.setHighlighted( true );
							MainWindow.this.addTXOfConnection( c );
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
							MainWindow.this.addTxToTable( tx );
							MainWindow.this.agents.getConnection( a1, a2 ).setHighlighted( true );
						}
					}
					
					MainWindow.this.selectedAgent = a1;
					MainWindow.this.selectedAgent.setHighlighted( true );
				}
				
				if ( null != MainWindow.this.networkPanel )
					MainWindow.this.networkPanel.repaint();
			}

			@Override
			public void connectionSeleted( ConnectionSelectedEvent connSelectedEvent ) {
				MainWindow.this.resetNetworkHighlights();
				MainWindow.this.txTableModel.setRowCount( 0 );
				MainWindow.this.txHistoryTable.revalidate();

				connSelectedEvent.getSelectedConnection().setHighlighted( true );
				MainWindow.this.addTXOfConnection( connSelectedEvent.getSelectedConnection() );
				
				if ( null != MainWindow.this.networkPanel )
					MainWindow.this.networkPanel.repaint();
			}
		} );
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.5;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		
		this.visualizationPanel.add( MainWindow.this.networkPanel, c );
		this.revalidate();
	}
	
	private void toggleSimulation() {
		// no simulation already running...
		if ( null == this.simulationThread ) {
			// if there was a simulation-run before: reset the agents
			this.agents.reset( 1.0, 1.0 );
			
			// sort TX-ID descending initially to show new TXs first. one call seems not to be enough => do 2 times :D
			this.txHistoryTable.getRowSorter().toggleSortOrder( 0 );
			this.txHistoryTable.getRowSorter().toggleSortOrder( 0 );
				
			this.successfulTx.clear();
			
			this.txTableModel.setRowCount( 0 );
			this.txHistoryTable.revalidate();

			// disable controls, to prevent changes by user
			this.agentCountSpinner.setEnabled( false );
			this.topologySelection.setEnabled( false );
			this.optimismSelection.setEnabled( false );
			this.recreateButton.setEnabled( false );
			this.cashAssetOnlyCheck.setEnabled( false );
			
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
			
			Auction auction = null;
			
			if ( this.cashAssetOnlyCheck.isSelected() ) {
				auction = new Auction( this.agents, this.asset );
			} else {
				auction = new AuctionWithLoans( this.agents, this.asset );
			}

			auction.init();
			
			// let simulation run in a separate thread to prevent blocking of gui
			this.simulationThread = new SimulationThread( auction, this );
			this.simulationThread.startSimulation();
			
			if ( null != MainWindow.this.networkPanel )
				this.networkPanel.repaint();
			
			this.agentWealthPanel.repaint();
			
		// simulation is running
		} else {
			this.simulationThread.stopSimulation();
			this.simulationThread = null;

			// simulation has finished => enable controls
			this.simulateButton.setText( "Start Simulation" );
			this.nextTxButton.setVisible( false );
			this.pauseButton.setVisible( false );
			
			this.agentCountSpinner.setEnabled( true );
			this.topologySelection.setEnabled( true );
			this.optimismSelection.setEnabled( true );
			this.recreateButton.setEnabled( true );
			this.cashAssetOnlyCheck.setEnabled( true );
			
		}
	}
	
	private void highlightTx( Transaction tx ) {
		// no need for anything highlighting-related when no network-panel available
		if ( null == MainWindow.this.networkPanel ) {
			return;
		}
	
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
		
		this.finalAssetAskLabel.setText( TRADING_VALUES_FORMAT.format( tx.getFinalAskAssetPrice() ) );
		this.finalAssetBidLabel.setText( TRADING_VALUES_FORMAT.format( tx.getFinalBidAssetPrice() ) );
		
		String finalLoanAskLabelText = "-";
		String finalLoanBidLabelText = "-";
		
		if ( tx instanceof TransactionWithLoans ) {
			if ( askOffering instanceof AskOfferingWithLoans ) {
				finalLoanAskLabelText = TRADING_VALUES_FORMAT.format( ( (TransactionWithLoans) tx ).getFinalAskLoanPrice() );
			}
			
			if ( bidOffering instanceof BidOfferingWithLoans ) {
				finalLoanBidLabelText = TRADING_VALUES_FORMAT.format( ( (TransactionWithLoans) tx ).getFinalBidLoanPrice() );
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
	
	private void addTxToTable( Transaction tx ) {
		//"TX", "Buyer", "Seller", "Asset Amount", "Asset Price", "Loan Amount", "Loan Price", "Loan Type"
		
		if ( tx.getMatchingAskOffer() instanceof AskOfferingWithLoans ) {
			this.txTableModel.addRow( new Object[] {
					tx.getTransNum(),
					AGENT_H_FORMAT.format( tx.getFinalBidH() ),
					AGENT_H_FORMAT.format( tx.getFinalAskH() ),
					TRADING_VALUES_FORMAT.format( tx.getAssetAmount() ),
					TRADING_VALUES_FORMAT.format( tx.getAssetPrice() ),
					TRADING_VALUES_FORMAT.format( ( ( TransactionWithLoans ) tx ).getLoanAmount() ),
					TRADING_VALUES_FORMAT.format( ( ( TransactionWithLoans ) tx ).getLoanPrice() ),
					Integer.toString( ( ( TransactionWithLoans ) tx ).getLoanType() ) } );
			
		} else {
			this.txTableModel.addRow( new Object[] {
					tx.getTransNum(),
					AGENT_H_FORMAT.format( tx.getFinalBidH() ),
					AGENT_H_FORMAT.format( tx.getFinalAskH() ),
					TRADING_VALUES_FORMAT.format( tx.getAssetAmount() ),
					TRADING_VALUES_FORMAT.format( tx.getAssetPrice() ),
					"-", "-", "-"} );
		}
	}
	
	private void addTXOfConnection( AgentConnection c ) {
		for ( int i = 0; i < MainWindow.this.successfulTx.size(); ++i ) {
			Transaction tx = MainWindow.this.successfulTx.get( i );
			
			Agent a1 = tx.getMatchingAskOffer().getAgent();
			Agent a2 = tx.getMatchingBidOffer().getAgent();

			if ( c == MainWindow.this.agents.getConnection( a1, a2 ) ) {
				a1.setHighlighted( true );
				a2.setHighlighted( true );
				
				MainWindow.this.addTxToTable( tx );
			}
		}
	}
}
