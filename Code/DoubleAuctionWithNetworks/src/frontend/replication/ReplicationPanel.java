package frontend.replication;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import utils.Utils;
import backend.EquilibriumStatistics;
import backend.agents.Agent;
import backend.agents.AgentFactoryImpl;
import backend.agents.network.AgentNetwork;
import backend.markets.LoanType;
import backend.markets.Markets;
import controller.experiment.data.ExperimentBean;
import controller.experiment.data.ExperimentListBean;
import controller.experiment.data.TopologyBean;
import controller.replication.ReplicationsRunner;
import controller.replication.ReplicationsRunner.ReplicationsListener;
import controller.replication.ReplicationsRunner.TerminationMode;
import controller.replication.data.ReplicationData;
import frontend.agentInfo.AgentInfoFrame;
import frontend.networkCreators.AscendingConnectedCreator;
import frontend.networkCreators.AscendingFullShortcutsCreator;
import frontend.networkCreators.AscendingRandomShortcutsCreator;
import frontend.networkCreators.AscendingRegularShortcutsCreator;
import frontend.networkCreators.BarbasiAlbertCreator;
import frontend.networkCreators.BuyersAnd2SellersCreator;
import frontend.networkCreators.BuyerHas2SellersCreator;
import frontend.networkCreators.ErdosRenyiCreator;
import frontend.networkCreators.FullyConnectedCreator;
import frontend.networkCreators.HalfFullyConnectedCreator;
import frontend.networkCreators.HubConnectedCreator;
import frontend.networkCreators.MaximumHubCreator;
import frontend.networkCreators.MedianHubCreator;
import frontend.networkCreators.NetworkCreator;
import frontend.networkCreators.SellerHas2BuyersCreator;
import frontend.networkCreators.ThreeMedianHubsCreator;
import frontend.networkCreators.WattStrogatzCreator;
import frontend.networkVisualisation.NetworkRenderPanel;
import frontend.networkVisualisation.NetworkVisualisationFrame;
import frontend.replication.info.ReplicationInfoFrame;
import frontend.visualisation.MarketsAccuOfflineVisualizer;
import frontend.visualisation.MarketsTimeOfflineVisualizer;
import frontend.visualisation.WealthVisualizer;

@SuppressWarnings( value = {"serial" } )
public class ReplicationPanel extends JPanel {

	private Markets markets;
	private AgentNetwork agentNetworkTemplate;
	
	private JCheckBox abmMarketCheck;
	private JCheckBox loanCashMarketCheck;
	private JCheckBox collateralMarketCheck;
	private JCheckBox importanceSamplingCheck;
	
	private JComboBox<NetworkCreator> topologySelection;
	private JComboBox<TerminationMode> terminationSelection;
	private JComboBox<LoanType> loanTypeSelection;
	
	private JSpinner agentCountSpinner;
	private JSpinner replicationCountSpinner;
	private JSpinner maxTxSpinner;
	
	private JButton startButton;
	private JButton saveButton;
	private JButton showNetworkButton;
	private JButton showAgentInfoButton;
	private JButton showReplicationInfoButton;
	
	private JLabel replicationsLeftLabel;
	private JLabel runningTimeLabel;
	
	private JFileChooser fileChooser;
	
	private EquilibriumInfoPanel equilibriumInfoPanel;
	private ReplicationTable replicationTable;
	
	private JTabbedPane visualizersTabbedPane;
	
	private AgentInfoFrame agentInfoFrame;
	private ReplicationInfoFrame replicationInfoFrame;
	private MarketsTimeOfflineVisualizer marketsTimeVisualizer;
	private MarketsAccuOfflineVisualizer marketsAccuVisualizer;
	private WealthVisualizer agentWealthPanel;
	private NetworkVisualisationFrame netVisFrame;
	
	private Timer spinnerChangedTimer;
	private ReplicationsRunner replications;
	
	private String name;
	
	private Timer runningSinceUpdater;
	
	public ReplicationPanel() {
		this.markets = new Markets();

		this.setLayout( new BorderLayout() );
		
		this.createControls();
		this.createAgents();
	}
	
	public ReplicationPanel( ExperimentBean bean ) {
		this.markets = new Markets();
		this.markets.setAssetBondMaret( bean.isAssetLoanMarket() );
		this.markets.setLoanMarket( bean.isLoanCashMarket() );
		this.markets.setLoanType( bean.getLoanType() );
		this.markets.setCollateralMarket( bean.isCollateralCashMarket() );
		
		this.setLayout( new BorderLayout() );
		
		this.createControls();
		
		this.name = bean.getName();
		this.agentCountSpinner.setValue( bean.getAgentCount() );
		this.loanTypeSelection.setSelectedItem( bean.getLoanType() );
		this.maxTxSpinner.setValue( bean.getMaxTx() );
		this.replicationCountSpinner.setValue( bean.getReplications() );
		this.terminationSelection.setSelectedItem( bean.getTerminationMode() );
		this.importanceSamplingCheck.setSelected( bean.isImportanceSampling() );

		NetworkCreator creator = null;
		
		for ( int i = 0; i < this.topologySelection.getItemCount(); ++i ) {
			creator = this.topologySelection.getItemAt( i );
			if ( creator.getClass().getName().equals( bean.getTopology().getClazz() ) ) {
				// need to prevent the provocation of a selection-event to prevent the input-boxes showing up
				ActionListener selectionListener = this.topologySelection.getActionListeners()[ 0 ];
				this.topologySelection.removeActionListener( selectionListener );
				this.topologySelection.setSelectedIndex( i );
				this.topologySelection.addActionListener( selectionListener );
				break;
			}
		}
		
		if ( null != creator ) {
			creator.setParams( bean.getTopology().getParams() );
		}
		
		this.createAgents();
	}
	
	private void createControls() {
		// creating controls
		this.visualizersTabbedPane = new JTabbedPane();
		
		this.agentWealthPanel = new WealthVisualizer();
		this.marketsTimeVisualizer = new MarketsTimeOfflineVisualizer();
		this.marketsAccuVisualizer = new MarketsAccuOfflineVisualizer();
		this.equilibriumInfoPanel = new EquilibriumInfoPanel();
		
		this.abmMarketCheck = new JCheckBox( "Asset/Loan Market" );
		this.loanCashMarketCheck = new JCheckBox( "Loan/Cash Market" );
		this.collateralMarketCheck = new JCheckBox( "Collateral/Cash Market" );
		
		this.importanceSamplingCheck = new JCheckBox( "Importance-Sampling" );
		
		this.topologySelection = new JComboBox<NetworkCreator>();
		this.terminationSelection = new JComboBox<TerminationMode>( TerminationMode.values() );
		this.loanTypeSelection = new JComboBox<LoanType>( LoanType.values() );
		
		this.startButton = new JButton( "Start" );
		this.saveButton = new JButton( "Save as Experiment" );
		this.showNetworkButton = new JButton( "Show Network" );
		this.showAgentInfoButton = new JButton( "Agent-Info" );
		this.showReplicationInfoButton = new JButton( "Replication-Info" );
		
		this.agentCountSpinner = new JSpinner( new SpinnerNumberModel( 100, 2, 1000, 10 ) );
		this.replicationCountSpinner = new JSpinner( new SpinnerNumberModel( 4, 1, 1000, 1 ) );
		this.maxTxSpinner = new JSpinner( new SpinnerNumberModel( 1000, 1, 1000000, 100 ) );
		
		this.runningTimeLabel = new JLabel( "Running since: -" );
		this.replicationsLeftLabel = new JLabel( "Replications left: -" );
		
		this.fileChooser = new JFileChooser();
		this.fileChooser.setFileFilter( new FileNameExtensionFilter( "XML-Files", "xml" ) );
		this.fileChooser.setCurrentDirectory( Utils.EXPERIMENTS_DIRECTORY );
		
		this.replicationTable = new ReplicationTable();
		
		this.loanTypeSelection.setSelectedItem( LoanType.LOAN_05 );
		
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
		this.topologySelection.addItem( new SellerHas2BuyersCreator() );
		this.topologySelection.addItem( new BuyerHas2SellersCreator() );
		this.topologySelection.addItem( new BuyersAnd2SellersCreator() );
		
		this.abmMarketCheck.setSelected( this.markets.isAssetBondMarket() );
		this.loanCashMarketCheck.setSelected( this.markets.isLoanMarket() );
		this.collateralMarketCheck.setSelected( this.markets.isCollateralMarket() );
		
		this.showReplicationInfoButton.setEnabled( false );
		
		this.importanceSamplingCheck.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ReplicationPanel.this.handleImportanceSampling();
			}
		});
		
		ActionListener checkListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if ( false == ReplicationPanel.this.abmMarketCheck.isSelected() ) {
					ReplicationPanel.this.loanCashMarketCheck.setSelected( false );
					ReplicationPanel.this.collateralMarketCheck.setSelected( false );
				}
				
				ReplicationPanel.this.setMarketMechanisms();
			}
		};
		
		this.abmMarketCheck.addActionListener( checkListener );
		this.loanCashMarketCheck.addActionListener( checkListener );
		this.collateralMarketCheck.addActionListener( checkListener );
		
		this.startButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ReplicationPanel.this.toggleReplication();
			}
		});
		
		this.saveButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ReplicationPanel.this.saveAsExperiment();
			}
		});
		
		this.topologySelection.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				NetworkCreator newSelected = (NetworkCreator) ReplicationPanel.this.topologySelection.getSelectedItem();

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
		this.loanTypeSelection.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ReplicationPanel.this.createAgents();
			}
		} );
		
		this.showNetworkButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if ( null == ReplicationPanel.this.netVisFrame ) {
					ReplicationPanel.this.netVisFrame = new NetworkVisualisationFrame();
				}

				ReplicationPanel.this.netVisFrame.setVisible( true );
				ReplicationPanel.this.updateNetworkVisualisationFrame();
			}
		});

		this.showAgentInfoButton.addActionListener( new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				ReplicationPanel.this.showAgentInfoFrame();
			}
		});
		
		this.showReplicationInfoButton.addActionListener( new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				ReplicationPanel.this.showReplicationInfo();
			}
		});
		
		this.visualizersTabbedPane.addTab( "Agents", this.agentWealthPanel );
		this.visualizersTabbedPane.addTab( "Markets Time", this.marketsTimeVisualizer );
		this.visualizersTabbedPane.addTab( "Markets Accum", this.marketsAccuVisualizer );
		
		// adding components ////////////////////////////////////
		JPanel controlsPanel = new JPanel( new GridBagLayout() );
		JPanel agentsConfigPanel = new JPanel();
		JPanel replicationsConfigPanel = new JPanel();
		
		agentsConfigPanel.add( this.loanTypeSelection );
		agentsConfigPanel.add( this.agentCountSpinner );
		agentsConfigPanel.add( this.topologySelection );
		agentsConfigPanel.add( this.abmMarketCheck );
		agentsConfigPanel.add( this.loanCashMarketCheck );
		agentsConfigPanel.add( this.collateralMarketCheck );
		agentsConfigPanel.add( this.importanceSamplingCheck );
		agentsConfigPanel.add( this.startButton );
		agentsConfigPanel.add( this.saveButton );
		
		replicationsConfigPanel.add( this.maxTxSpinner );
		replicationsConfigPanel.add( this.terminationSelection );
		replicationsConfigPanel.add( this.replicationCountSpinner );
		replicationsConfigPanel.add( this.showNetworkButton );
		replicationsConfigPanel.add( this.showAgentInfoButton );
		replicationsConfigPanel.add( this.showReplicationInfoButton );
		replicationsConfigPanel.add( this.runningTimeLabel );
		replicationsConfigPanel.add( this.replicationsLeftLabel );
		
		GridBagConstraints c = new GridBagConstraints();
		
		c.gridy = 0;
		controlsPanel.add( agentsConfigPanel, c );
		c.gridy = 1;
		controlsPanel.add( replicationsConfigPanel, c );

		this.add( controlsPanel, BorderLayout.NORTH );
		this.add( this.visualizersTabbedPane, BorderLayout.CENTER );
		this.add( this.equilibriumInfoPanel, BorderLayout.SOUTH );
	}
	
	private void saveAsExperiment() {
		int returnVal = this.fileChooser.showSaveDialog( this );
        if (returnVal != JFileChooser.APPROVE_OPTION) {
        	return;
        }
        
        File file = this.fileChooser.getSelectedFile();
        if ( false == file.getName().endsWith( ".xml" ) ) {
        	file = new File( this.fileChooser.getSelectedFile() + ".xml" );
        }
        
        String name = JOptionPane.showInputDialog( "Name of experiment: " );
        
        ExperimentListBean experimentList = null;
        
        if ( file.exists() ) {
			try {
				JAXBContext jaxbContext = JAXBContext.newInstance( ExperimentListBean.class );
				Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
				experimentList = ( ExperimentListBean ) jaxbUnmarshaller.unmarshal( file );
			
			} catch (JAXBException e) {
				JOptionPane.showMessageDialog( this, "An Error occured parsing XML-File \"" + file.getAbsoluteFile() + "\"" );
			}
			
        } else {
        	experimentList = new ExperimentListBean();
        	experimentList.setExperiments( new ArrayList<ExperimentBean>() );
        }
        
        NetworkCreator networkCreator = this.topologySelection.getItemAt( this.topologySelection.getSelectedIndex() );
        TopologyBean topologyBean = new TopologyBean();
        topologyBean.setClazz( networkCreator.getClass().getName() );
        topologyBean.setParams( networkCreator.getParams() );
        
        ExperimentBean experimentBean = new ExperimentBean();
        experimentBean.setAgentCount( (Integer) this.agentCountSpinner.getValue() );
        experimentBean.setAssetLoanMarket( this.abmMarketCheck.isSelected() );
        experimentBean.setLoanType( (LoanType) this.loanTypeSelection.getSelectedItem() );
        experimentBean.setImportanceSampling( this.importanceSamplingCheck.isSelected() );
        experimentBean.setLoanCashMarket( this.loanCashMarketCheck.isSelected() );
        experimentBean.setCollateralCashMarket( this.collateralMarketCheck.isSelected() );
        experimentBean.setMaxTx( (Integer) this.maxTxSpinner.getValue() );
        experimentBean.setName( name );
        experimentBean.setReplications( (Integer) this.replicationCountSpinner.getValue() );
        experimentBean.setTerminationMode( (TerminationMode) this.terminationSelection.getSelectedItem() );
        experimentBean.setTopology( topologyBean );
        
        experimentList.getExperiments().add( experimentBean );
        
        try {
			JAXBContext jaxbContext = JAXBContext.newInstance( ExperimentListBean.class );
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			 
		    jaxbMarshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );

		    jaxbMarshaller.marshal( experimentList, file );
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}

	private void showReplicationInfo() {
		if ( null == this.replicationInfoFrame ) {
			this.replicationInfoFrame = new ReplicationInfoFrame( this.replicationTable );
		}
		
		this.replicationInfoFrame.setTitle( "Replication-Info (" + this.getTitleExtension() + ")" );
		this.replicationInfoFrame.setTasks( this.replications.getReplicationTasks() );
		this.replicationInfoFrame.setVisible( true );
	}

	private void showAgentInfoFrame() {
		if ( null == this.agentInfoFrame ) {
			this.agentInfoFrame = new AgentInfoFrame();
		}
		
		List<Agent> agents = this.agentNetworkTemplate.getOrderedList();
		if ( null != this.replications && this.replications.getCurrentStats() != null ) {
			agents = this.replications.getCurrentStats().getFinalAgents();
		}
		
		this.agentInfoFrame.setTitle( "Agent-Info (" + this.getTitleExtension() + ")" );
		this.agentInfoFrame.setAgents( agents );
		this.agentInfoFrame.setVisible( true );
	}
	
	private void toggleReplication() {
		if ( false == this.replications.isRunning() ) {
			this.startButton.setText( "Terminate" );
			this.abmMarketCheck.setEnabled( false );
			this.loanCashMarketCheck.setEnabled( false );
			this.collateralMarketCheck.setEnabled( false );
			this.importanceSamplingCheck.setEnabled( false );
			this.agentCountSpinner.setEnabled( false );
			this.maxTxSpinner.setEnabled( false );
			this.loanTypeSelection.setEnabled( false );
			this.topologySelection.setEnabled( false );
			this.terminationSelection.setEnabled( false );
			this.replicationCountSpinner.setEnabled( false );
			this.showReplicationInfoButton.setEnabled( true );
			
			this.replicationsLeftLabel.setText( "Replications Left: " + (Integer) this.replicationCountSpinner.getValue() );

			// reset previously calculated data
			this.replicationTable.clearAll();
			this.agentWealthPanel.setAgents( this.agentNetworkTemplate.getOrderedList() );
			this.marketsAccuVisualizer.setMarkets( new ArrayList<double[]>() );
			this.marketsTimeVisualizer.setMarkets( new ArrayList<double[]>() );
			
			NetworkCreator networkCreator = this.topologySelection.getItemAt( this.topologySelection.getSelectedIndex() );
			TopologyBean topologyBean = new TopologyBean();
			topologyBean.setClazz( networkCreator.getClass().getName() );
			topologyBean.setParams( networkCreator.getParams() );
					        
			ExperimentBean bean = new ExperimentBean();
			bean.setName( this.name );
			bean.setAgentCount( (Integer) this.agentCountSpinner.getValue() );
			bean.setAssetLoanMarket( this.abmMarketCheck.isSelected() );
			bean.setLoanType( (LoanType) this.loanTypeSelection.getSelectedItem() );
			bean.setImportanceSampling( this.importanceSamplingCheck.isSelected() );
			bean.setLoanCashMarket( this.loanCashMarketCheck.isSelected() );
			bean.setCollateralCashMarket( this.collateralMarketCheck.isSelected() );
			bean.setMaxTx( (Integer) this.maxTxSpinner.getValue() );
			bean.setReplications( (Integer) this.replicationCountSpinner.getValue() );
			bean.setTerminationMode( this.terminationSelection.getItemAt( this.terminationSelection.getSelectedIndex() ) );
			bean.setTopology( topologyBean );
			
			this.replications.startAsync( bean, new ReplicationsListener() {
				@Override
				public void replicationFinished( final ReplicationData data, final ReplicationData currentStats, 
						final EquilibriumStatistics variance, final List<double[]> medianMarkets ) {
					
					// setting markets on ofline timevisualizere here to prevent blocking of guy
					ReplicationPanel.this.marketsTimeVisualizer.setMarkets( medianMarkets );

					SwingUtilities.invokeLater( new Runnable() {
						@Override
						public void run() {
							ReplicationPanel.this.replicationTable.addReplication( data );
							ReplicationPanel.this.replicationsLeftLabel.setText( "Replications Left: " + ReplicationPanel.this.replications.getReplicationsLeft() );
							
							if ( null != currentStats ) {
								ReplicationPanel.this.equilibriumInfoPanel.setMeanAndVariance( currentStats.getStats(), variance );
								ReplicationPanel.this.agentWealthPanel.setAgents( currentStats.getFinalAgents() );
								ReplicationPanel.this.marketsAccuVisualizer.setMarkets( medianMarkets );
								ReplicationPanel.this.marketsTimeVisualizer.repaint();
								
								ReplicationPanel.this.updateAgentInfoFrame( currentStats.getFinalAgents() );
							}
						}
					} );
				}
				
				@Override
				public void allReplicationsFinished() {
					ReplicationPanel.this.resetControls();
				}
			} );
			
			this.updateRunningTimeLabel();
			this.runningSinceUpdater = new Timer();
			this.runningSinceUpdater.schedule( new TimerTask() {
				@Override
				public void run() {
					updateRunningTimeLabel();
				}
				
			}, 1000, 1000 );
			
		} else {
			this.replications.stopAsync();
			this.resetControls();
		}
	}
	
	private void resetControls() {
		this.runningSinceUpdater.cancel();
		this.runningSinceUpdater = null;
		
		this.updateRunningTimeLabel();
		
		this.startButton.setText( "Start" );
		this.abmMarketCheck.setEnabled( true );
		this.loanCashMarketCheck.setEnabled( true );
		this.collateralMarketCheck.setEnabled( true );
		this.importanceSamplingCheck.setEnabled( true );
		this.agentCountSpinner.setEnabled( true );
		this.maxTxSpinner.setEnabled( true );
		this.loanTypeSelection.setEnabled( true );
		this.topologySelection.setEnabled( true );
		this.terminationSelection.setEnabled( true );
		this.replicationCountSpinner.setEnabled( true );
	}
	
	private void handleImportanceSampling() {
		NetworkCreator creator = (NetworkCreator) this.topologySelection.getSelectedItem();
		if ( this.importanceSamplingCheck.isSelected() ) {
			creator.createImportanceSampling( this.agentNetworkTemplate, this.markets );
			
		} else {
			Iterator<Agent> iter = this.agentNetworkTemplate.iterator();
			while ( iter.hasNext() ) {
				iter.next().resetOfferingLimits();
			}
		}
	}
	
	public String getTitleExtension() {
		int agentCount = (Integer) this.agentCountSpinner.getValue();
		NetworkCreator creator = (NetworkCreator) this.topologySelection.getSelectedItem();
		return creator.name() + ", " + agentCount + " Agents";
	}
	
	private void createAgents() {
		int agentCount = (Integer) this.agentCountSpinner.getValue();
		this.markets = new Markets( (LoanType) this.loanTypeSelection.getSelectedItem() );
		this.setMarketMechanisms();
		
		this.replicationTable.clearAll();

		NetworkCreator creator = ( NetworkCreator ) this.topologySelection.getSelectedItem();
		this.agentNetworkTemplate = creator.createNetwork( new AgentFactoryImpl( agentCount, this.markets ) );
		
		this.handleImportanceSampling();
		
		this.replications = new ReplicationsRunner( this.agentNetworkTemplate, this.markets );
		this.agentWealthPanel.setAgents( this.agentNetworkTemplate.getOrderedList() );
		this.marketsAccuVisualizer.setMarkets( new ArrayList<double[]>() );
		this.marketsTimeVisualizer.setMarkets( new ArrayList<double[]>() );
		
		this.marketsAccuVisualizer.repaint();
		this.marketsTimeVisualizer.repaint();
		
		this.updateNetworkVisualisationFrame();
		this.updateAgentInfoFrame( this.agentNetworkTemplate.getOrderedList() );
	}
	
	private void updateAgentInfoFrame( List<Agent> agents ) {
		if ( null != this.agentInfoFrame && this.agentInfoFrame.isVisible() ) {
			this.agentInfoFrame.setAgents( agents );
			this.agentInfoFrame.setTitle( "Agent-Info (" + this.getTitleExtension() + ")" );
		}
	}
	
	private void updateNetworkVisualisationFrame() {
		if ( null != this.netVisFrame && this.netVisFrame.isVisible() ) {
			NetworkRenderPanel networkPanel = this.agentNetworkTemplate.getNetworkRenderingPanel( this.netVisFrame.getSelectedLayout(), null );
			this.netVisFrame.setNetworkRenderPanel( networkPanel, this.agentNetworkTemplate );
			this.netVisFrame.setTitle( "Agent Network (" + this.getTitleExtension() + ")" );
		}
	}
	
	private void updateRunningTimeLabel() {
		long currSysMillis = System.currentTimeMillis();
		long duration = currSysMillis - this.replications.getStartingTime().getTime();
		this.runningTimeLabel.setText( "Running since " + Utils.DATE_FORMATTER.format( this.replications.getStartingTime() ) 
				+ ", " + ( duration / 1000 ) + " sec." );
	}
	
	private void setMarketMechanisms() {
		this.markets.setAssetBondMaret( this.abmMarketCheck.isSelected() );
		this.markets.setLoanMarket( this.loanCashMarketCheck.isSelected() );
		this.markets.setCollateralMarket( this.collateralMarketCheck.isSelected() );
		
		this.agentWealthPanel.setMarkets( this.markets );
		this.marketsTimeVisualizer.setMarkets( this.markets );
		this.marketsAccuVisualizer.setMarkets( this.markets );
	}
	
}
