package frontend.replication;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
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
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import backend.agents.Agent;
import backend.agents.AgentFactoryImpl;
import backend.agents.network.AgentConnection;
import backend.agents.network.AgentNetwork;
import backend.markets.Markets;
import backend.replications.ReplicationsRunner;
import backend.replications.ReplicationsRunner.ReplicationsListener;
import backend.replications.ReplicationsRunner.TerminationMode;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import frontend.agentInfo.AgentInfoFrame;
import frontend.experimenter.ExperimenterPanel;
import frontend.experimenter.xml.experiment.ExperimentBean;
import frontend.experimenter.xml.experiment.ExperimentListBean;
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
import frontend.networkCreators.MaximumHubCreator;
import frontend.networkCreators.MedianHubCreator;
import frontend.networkCreators.NetworkCreator;
import frontend.networkCreators.ThreeMedianHubsCreator;
import frontend.networkCreators.WattStrogatzCreator;
import frontend.networkVisualisation.NetworkRenderPanel;
import frontend.replication.info.ReplicationInfoFrame;
import frontend.visualisation.WealthVisualizer;

@SuppressWarnings( value = {"serial" } )
public class ReplicationPanel extends JPanel {

	private Markets markets;
	private AgentNetwork agentNetworkTemplate;
	
	private JCheckBox abmMarketCheck;
	private JCheckBox loanCashMarketCheck;
	private JCheckBox bpMechanismCheck;
	private JCheckBox importanceSamplingCheck;
	
	private JComboBox<NetworkCreator> topologySelection;
	private JComboBox<TerminationMode> terminationSelection;
	
	private JSpinner agentCountSpinner;
	private JSpinner faceValueSpinner;
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
	
	private AgentInfoFrame agentInfoFrame;
	private ReplicationInfoFrame replicationInfoFrame;
	
	private WealthVisualizer agentWealthPanel;
	private NetworkVisualisationFrame netVisFrame;
	
	private Timer spinnerChangedTimer;
	private ReplicationsRunner replications;
	
	private String name;
	
	private Timer runningSinceUpdater;

	public final static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat( "dd.MM HH:mm:ss" );
	public static final DecimalFormat VALUES_FORMAT = new DecimalFormat("0.0000");
	
	public ReplicationPanel() {
		this.markets = new Markets();

		this.setLayout( new BorderLayout() );
		
		this.createControls();
		this.createAgents();
	}
	
	public ReplicationPanel( ExperimentBean bean ) {
		this.markets = new Markets();
		this.markets.setABM( bean.isAssetLoanMarket() );
		this.markets.setLoanMarket( bean.isLoanCashMarket() );
		this.markets.setBP( bean.isBondsPledgeability() );
		this.markets.setV( bean.getFaceValue() );
		
		this.setLayout( new BorderLayout() );
		
		this.createControls();
		
		this.name = bean.getName();
		this.agentCountSpinner.setValue( bean.getAgentCount() );
		this.faceValueSpinner.setValue( bean.getFaceValue() );
		this.maxTxSpinner.setValue( bean.getMaxTx() );
		this.replicationCountSpinner.setValue( bean.getReplications() );
		this.terminationSelection.setSelectedItem( bean.getTerminationMode() );
		this.importanceSamplingCheck.setSelected( bean.isImportanceSampling() );

		for ( int i = 0; i < this.topologySelection.getItemCount(); ++i ) {
			NetworkCreator creator = this.topologySelection.getItemAt( i );
			if ( creator.name().equals( bean.getTopology() ) ) {
				this.topologySelection.setSelectedIndex( i );
				break;
			}
		}
		
		this.createAgents();
	}
	
	private void createControls() {
		// creating controls
		this.agentWealthPanel = new WealthVisualizer();
		this.equilibriumInfoPanel = new EquilibriumInfoPanel();
		
		this.abmMarketCheck = new JCheckBox( "Asset/Loan Market" );
		this.loanCashMarketCheck = new JCheckBox( "Loan/Cash Market" );
		this.bpMechanismCheck = new JCheckBox( "Bonds Pledgeability" );
		
		this.importanceSamplingCheck = new JCheckBox( "Importance-Sampling" );
		
		this.topologySelection = new JComboBox<NetworkCreator>();
		this.terminationSelection = new JComboBox<TerminationMode>( TerminationMode.values() );
		
		this.startButton = new JButton( "Start" );
		this.saveButton = new JButton( "Save as Experiment" );
		this.showNetworkButton = new JButton( "Show Network" );
		this.showAgentInfoButton = new JButton( "Agent-Info" );
		this.showReplicationInfoButton = new JButton( "Replication-Info" );
		
		this.agentCountSpinner = new JSpinner( new SpinnerNumberModel( 30, 10, 1000, 10 ) );
		this.faceValueSpinner = new JSpinner( new SpinnerNumberModel( 0.5, 0.1, 1.0, 0.1 ) );
		this.replicationCountSpinner = new JSpinner( new SpinnerNumberModel( 4, 1, 100, 1 ) );
		this.maxTxSpinner = new JSpinner( new SpinnerNumberModel( 1_000, 1, 1_000_000, 100 ) );
		
		this.runningTimeLabel = new JLabel( "Running since: -" );
		this.replicationsLeftLabel = new JLabel( "Replications left: -" );
		
		this.fileChooser = new JFileChooser();
		this.fileChooser.setFileFilter( new FileNameExtensionFilter( "XML-Files", "xml" ) );
		this.fileChooser.setCurrentDirectory( ExperimenterPanel.EXPERIMENTS_DIRECTORY );
		
		this.replicationTable = new ReplicationTable();
		
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
		
		this.abmMarketCheck.setSelected( this.markets.isABM() );
		this.loanCashMarketCheck.setSelected( this.markets.isLoanMarket() );
		this.bpMechanismCheck.setSelected( this.markets.isBP() );
		
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
					ReplicationPanel.this.bpMechanismCheck.setSelected( false );
				}
			}
		};
		
		this.bpMechanismCheck.addActionListener( checkListener );
		this.abmMarketCheck.addActionListener( checkListener );
		this.loanCashMarketCheck.addActionListener( checkListener );
		
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
		this.faceValueSpinner.addChangeListener( spinnerChanged );
		
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
		
		this.agentWealthPanel.setSize( this.getSize() );
		
		GridBagConstraints c = new GridBagConstraints();
		
		c.gridy = 0;
		controlsPanel.add( agentsConfigPanel, c );
		c.gridy = 1;
		controlsPanel.add( replicationsConfigPanel, c );

		this.add( controlsPanel, BorderLayout.NORTH );
		this.add( this.agentWealthPanel, BorderLayout.CENTER );
		this.add( this.equilibriumInfoPanel, BorderLayout.SOUTH );
	}
	
	private void saveAsExperiment() {
		int returnVal = this.fileChooser.showSaveDialog( this );
        if (returnVal != JFileChooser.APPROVE_OPTION) {
        	return;
        }
        
        File file = this.fileChooser.getSelectedFile();
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
        	experimentList.setExperiments( new ArrayList<>() );
        }
        
        ExperimentBean experimentBean = new ExperimentBean();
        experimentBean.setAgentCount( (int) this.agentCountSpinner.getValue() );
        experimentBean.setAssetLoanMarket( this.abmMarketCheck.isSelected() );
        experimentBean.setBondsPledgeability( this.bpMechanismCheck.isSelected() );
        experimentBean.setFaceValue( (double) this.faceValueSpinner.getValue() );
        experimentBean.setImportanceSampling( this.importanceSamplingCheck.isSelected() );
        experimentBean.setLoanCashMarket( this.loanCashMarketCheck.isSelected() );
        experimentBean.setMaxTx( (int) this.maxTxSpinner.getValue() );
        experimentBean.setName( name );
        experimentBean.setReplications( (int) this.replicationCountSpinner.getValue() );
        experimentBean.setTerminationMode( (TerminationMode) this.terminationSelection.getSelectedItem() );
        experimentBean.setTopology( ( (NetworkCreator) this.topologySelection.getSelectedItem() ).name() );
        
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
			this.bpMechanismCheck.setEnabled( false );
			this.importanceSamplingCheck.setEnabled( false );
			this.agentCountSpinner.setEnabled( false );
			this.maxTxSpinner.setEnabled( false );
			this.faceValueSpinner.setEnabled( false );
			this.topologySelection.setEnabled( false );
			this.terminationSelection.setEnabled( false );
			this.replicationCountSpinner.setEnabled( false );
			this.showReplicationInfoButton.setEnabled( true );
			
			this.replicationsLeftLabel.setText( "Replications Left: " + (int) this.replicationCountSpinner.getValue() );

			// reset previously calculated data
			this.replicationTable.clearAll();
			this.agentWealthPanel.setAgents( this.agentNetworkTemplate.getOrderedList() );
			
			ExperimentBean bean = new ExperimentBean();
			bean.setName( this.name );
			bean.setAgentCount( (int) this.agentCountSpinner.getValue() );
			bean.setAssetLoanMarket( this.abmMarketCheck.isSelected() );
			bean.setBondsPledgeability( this.bpMechanismCheck.isSelected() );
			bean.setFaceValue( (double) this.faceValueSpinner.getValue() );
			bean.setImportanceSampling( this.importanceSamplingCheck.isSelected() );
			bean.setLoanCashMarket( this.loanCashMarketCheck.isSelected() );
			bean.setMaxTx( (int) this.maxTxSpinner.getValue() );
			bean.setReplications( (int) this.replicationCountSpinner.getValue() );
			bean.setTerminationMode( this.terminationSelection.getItemAt( this.terminationSelection.getSelectedIndex() ) );
			bean.setTopology( this.topologySelection.getItemAt( this.topologySelection.getSelectedIndex() ).name() );
			
			this.replications.start( bean, new ReplicationsListener() {
				@Override
				public void replicationFinished(ReplicationData data, ReplicationData currentStats ) {
					SwingUtilities.invokeLater( new Runnable() {
						@Override
						public void run() {
							ReplicationPanel.this.replicationTable.addReplication( data );
							ReplicationPanel.this.replicationsLeftLabel.setText( "Replications Left: " + ReplicationPanel.this.replications.getReplicationsLeft() );
							
							if ( null != currentStats ) {
								ReplicationPanel.this.equilibriumInfoPanel.setStats( currentStats.getStats() );
								ReplicationPanel.this.agentWealthPanel.setAgents( currentStats.getFinalAgents() );
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
			this.replications.stop();
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
		this.bpMechanismCheck.setEnabled( true );
		this.importanceSamplingCheck.setEnabled( true );
		this.agentCountSpinner.setEnabled( true );
		this.maxTxSpinner.setEnabled( true );
		this.faceValueSpinner.setEnabled( true );
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
		int agentCount = (int) this.agentCountSpinner.getValue();
		NetworkCreator creator = (NetworkCreator) this.topologySelection.getSelectedItem();
		return creator.name() + ", " + agentCount + " Agents";
	}
	
	private void createAgents() {
		int agentCount = (int) this.agentCountSpinner.getValue();
		
		this.markets = new Markets( (double) this.faceValueSpinner.getValue() );
		this.markets.setABM( ReplicationPanel.this.abmMarketCheck.isSelected() );
		this.markets.setLoanMarket( ReplicationPanel.this.loanCashMarketCheck.isSelected() );
		this.markets.setBP( ReplicationPanel.this.bpMechanismCheck.isSelected() );

		this.replicationTable.clearAll();

		NetworkCreator creator = ( NetworkCreator ) this.topologySelection.getSelectedItem();
		this.agentNetworkTemplate = creator.createNetwork( new AgentFactoryImpl( agentCount, this.markets ) );
		
		this.handleImportanceSampling();
		
		List<Agent> agents = this.agentNetworkTemplate.getOrderedList();
		
		this.replications = new ReplicationsRunner( this.agentNetworkTemplate, this.markets );
		
		this.agentWealthPanel.setAgents( this.agentNetworkTemplate.getOrderedList() );
		
		this.updateNetworkVisualisationFrame();
		this.updateAgentInfoFrame( agents );
	}
	
	private void updateAgentInfoFrame( List<Agent> agents ) {
		if ( null != this.agentInfoFrame && this.agentInfoFrame.isVisible() ) {
			this.agentInfoFrame.setAgents( agents );
			this.agentInfoFrame.setTitle( "Agent-Info (" + this.getTitleExtension() + ")" );
		}
	}
	
	@SuppressWarnings("unchecked")
	private void updateNetworkVisualisationFrame() {
		if ( null != this.netVisFrame && this.netVisFrame.isVisible() ) {
			NetworkRenderPanel networkPanel = ReplicationPanel.this.agentNetworkTemplate.getNetworkRenderingPanel( (Class<? extends Layout<Agent, AgentConnection>>) CircleLayout.class, null );
			this.netVisFrame.setNetworkRenderPanel( networkPanel );
			this.netVisFrame.setTitle( "Agent Network (" + this.getTitleExtension() + ")" );
		}
	}
	
	private void updateRunningTimeLabel() {
		long currSysMillis = System.currentTimeMillis();
		long duration = currSysMillis - this.replications.getStartingTime().getTime();
		this.runningTimeLabel.setText( "Running since " + DATE_FORMATTER.format( this.replications.getStartingTime() ) 
				+ ", " + ( duration / 1000 ) + " sec." );
	}
}
