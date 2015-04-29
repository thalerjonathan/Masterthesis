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
import java.util.Date;
import java.util.Iterator;
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
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import backend.Auction;
import backend.Auction.EquilibriumStatistics;
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
import frontend.experimenter.xml.experiment.ExperimentBean;
import frontend.experimenter.xml.result.AgentBean;
import frontend.experimenter.xml.result.EquilibriumBean;
import frontend.experimenter.xml.result.ReplicationBean;
import frontend.experimenter.xml.result.ResultBean;
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
	private JCheckBox importanceSamplingCheck;
	
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
	
	private JLabel replicationsLeftLabel;
	private JLabel runningTimeLabel;
	
	private EquilibriumInfoPanel equilibriumInfoPanel;

	private ReplicationData currentStats;
	
	private ReplicationTable replicationTable;
	
	private AgentInfoFrame agentInfoFrame;
	private ReplicationInfoFrame replicationInfoFrame;
	
	private WealthVisualizer agentWealthPanel;
	private NetworkVisualisationFrame netVisFrame;
	
	private Timer spinnerChangedTimer;
	
	private ExecutorService replicationTaskExecutor;
	private List<ReplicationTask> replicationTasks;
	private Thread awaitFinishThread;
	
	private List<ReplicationData> replicationData;
	
	private Date startingTime;

	private boolean canceled;
	
	public final static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat( "dd.MM HH:mm:ss" );
	public static final DecimalFormat VALUES_FORMAT = new DecimalFormat("0.0000");
	
	private final static SimpleDateFormat FILENAME_DATE_FORMATTER = new SimpleDateFormat( "yyyyMMdd_HHmmss" );
	
	public enum TerminationMode {
		TOTAL_TX,
		FAIL_TOTAL_TX,
		FAIL_SUCCESSIVE_TX,
		TRADING_HALTED,
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
		this.equilibriumInfoPanel = new EquilibriumInfoPanel();
		
		this.abmMarketCheck = new JCheckBox( "Asset/Loan Market" );
		this.loanCashMarketCheck = new JCheckBox( "Loan/Cash Market" );
		this.bpMechanismCheck = new JCheckBox( "Bonds Pledgeability" );
		
		this.parallelEvaluationCheck = new JCheckBox( "Parallel Evaluation" );
		this.importanceSamplingCheck = new JCheckBox( "Importance-Sampling" );
		
		this.topologySelection = new JComboBox<INetworkCreator>();
		this.terminationSelection = new JComboBox<TerminationMode>( TerminationMode.values() );
		
		this.replicationButton = new JButton( "Start" );
		this.showNetworkButton = new JButton( "Show Network" );
		this.showAgentInfoButton = new JButton( "Agent-Info" );
		this.showReplicationInfoButton = new JButton( "Replication-Info" );
		
		this.agentCountSpinner = new JSpinner( new SpinnerNumberModel( 30, 10, 1000, 10 ) );
		this.faceValueSpinner = new JSpinner( new SpinnerNumberModel( 0.5, 0.1, 1.0, 0.1 ) );
		this.replicationCountSpinner = new JSpinner( new SpinnerNumberModel( 4, 1, 100, 1 ) );
		this.maxTxSpinner = new JSpinner( new SpinnerNumberModel( 10_000, 1, 10_000_000, 10_000 ) );
		
		this.runningTimeLabel = new JLabel( "Running since: -" );
		this.replicationsLeftLabel = new JLabel( "Replications left: -" );
		
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
		
		this.parallelEvaluationCheck.setSelected( false );
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
		agentsConfigPanel.add( this.importanceSamplingCheck );
		agentsConfigPanel.add( this.replicationButton );

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
	
	private void showReplicationInfo() {
		if ( 0 == this.replicationData.size() && 0 == this.replicationTasks.size() ) {
			return;
		}
		
		if ( null == this.replicationInfoFrame ) {
			this.replicationInfoFrame = new ReplicationInfoFrame( this.replicationTable );
		}
		
		this.replicationInfoFrame.setTitle( "Replication-Info (" + this.getTitleExtension() + ")" );
		this.replicationInfoFrame.setTasks( this.replicationTasks );
		this.replicationInfoFrame.setVisible( true );
	}

	private void showAgentInfoFrame() {
		if ( null == this.agentInfoFrame ) {
			this.agentInfoFrame = new AgentInfoFrame();
		}
		
		this.agentInfoFrame.setTitle( "Agent-Info (" + this.getTitleExtension() + ")" );
		this.agentInfoFrame.setAgents( this.currentStats != null ? 
				this.currentStats.getFinalAgents() : this.agentNetworkTemplate.getOrderedList() );
		this.agentInfoFrame.setVisible( true );
	}
	
	private void toggleReplication() {
		if ( null == this.awaitFinishThread ) {
			this.canceled = false;
			
			this.replicationButton.setText( "Terminate" );
			this.abmMarketCheck.setEnabled( false );
			this.loanCashMarketCheck.setEnabled( false );
			this.bpMechanismCheck.setEnabled( false );
			this.parallelEvaluationCheck.setEnabled( false );
			this.importanceSamplingCheck.setEnabled( false );
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
			
			this.replicationsLeftLabel.setText( "Replications Left: " + replicationCount.get() );
			
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
		experimentBean.setFaceValue( (double) this.faceValueSpinner.getValue() );
		experimentBean.setTopology( ( (INetworkCreator) this.topologySelection.getSelectedItem() ).toString() );
		experimentBean.setAssetLoanMarket( this.abmMarketCheck.isSelected() );
		experimentBean.setLoanCashMarket( this.loanCashMarketCheck.isSelected() );
		experimentBean.setBondsPledgeability( this.bpMechanismCheck.isSelected() );
		experimentBean.setParallelEvaluation( this.parallelEvaluationCheck.isSelected() );
		experimentBean.setImportanceSampling( this.importanceSamplingCheck.isSelected() );
		experimentBean.setTerminationMode( (TerminationMode) this.terminationSelection.getSelectedItem() );
		experimentBean.setMaxTx( (int) this.maxTxSpinner.getValue() );
		experimentBean.setReplications( (int) this.replicationCountSpinner.getValue() );
		
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
		    jaxbMarshaller.marshal( resultBean, new File( "replications/" + name + ".xml"  ) );
		    
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
	
	private void allReplicationsFinished() {
		this.updateRunningTimeLabel( System.currentTimeMillis() );
		
		this.replicationButton.setText( "Start" );
		this.abmMarketCheck.setEnabled( true );
		this.loanCashMarketCheck.setEnabled( true );
		this.bpMechanismCheck.setEnabled( true );
		this.parallelEvaluationCheck.setEnabled( true );
		this.importanceSamplingCheck.setEnabled( true );
		this.agentCountSpinner.setEnabled( true );
		this.maxTxSpinner.setEnabled( true );
		this.faceValueSpinner.setEnabled( true );
		this.topologySelection.setEnabled( true );
		this.terminationSelection.setEnabled( true );
		this.replicationCountSpinner.setEnabled( true );
		
		this.awaitFinishThread = null;
		this.replicationTasks.clear();
		
		this.replicationTaskExecutor.shutdown();
		
		this.writeResults();
	}

	private ReplicationData calculateAgentStatistics() {
		int agentCount = this.agentNetworkTemplate.size();
		
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
			Agent templateAgent = this.agentNetworkTemplate.get( i );
			
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
	
	private synchronized void replicationFinished( ReplicationData data ) {
		this.replicationData.add( data );
		this.currentStats = this.calculateAgentStatistics();
		int replicationsLeft = (int) ReplicationPanel.this.replicationCountSpinner.getValue() 
				- ReplicationPanel.this.replicationData.size();
		
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				ReplicationPanel.this.replicationTable.addReplication( data );
				ReplicationPanel.this.replicationsLeftLabel.setText( "Replications Left: " + replicationsLeft );
				
				if ( null != ReplicationPanel.this.currentStats ) {
					ReplicationPanel.this.equilibriumInfoPanel.setStats( ReplicationPanel.this.currentStats.getStats() );
					ReplicationPanel.this.agentWealthPanel.setAgents( ReplicationPanel.this.currentStats.getFinalAgents() );
					ReplicationPanel.this.updateAgentInfoFrame( ReplicationPanel.this.currentStats.getFinalAgents() );
				}
			}
		} );
	}
	
	private void handleImportanceSampling() {
		INetworkCreator creator = (INetworkCreator) this.topologySelection.getSelectedItem();
		if ( this.importanceSamplingCheck.isSelected() ) {
			creator.createImportanceSampling( this.agentNetworkTemplate, this.markets );
		} else {
			Iterator<Agent> iter = this.agentNetworkTemplate.iterator();
			while ( iter.hasNext() ) {
				iter.next().resetImportanceSamplingData();
			}
		}
	}
	
	public String getTitleExtension() {
		int agentCount = (int) this.agentCountSpinner.getValue();
		INetworkCreator creator = (INetworkCreator) this.topologySelection.getSelectedItem();
		return creator.toString() + ", " + agentCount + " Agents";
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
		
		this.handleImportanceSampling();
		
		List<Agent> agents = this.agentNetworkTemplate.getOrderedList();
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
		private int failTxTotalCount;
		private int failTxSuccessiveCount;
		
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
				
				if (  ( this.currentReplication = this.replicationCount.getAndDecrement() ) <= 0 ) {
					break;
				}
				
				// creates a deep copy of the network, need for parallel execution
				this.currentAgents = new AgentNetwork( ReplicationPanel.this.agentNetworkTemplate );
				Auction auction = new Auction( this.currentAgents );
				
				ReplicationData data = this.calculateReplication( auction );
				if ( null != data ) {
					// setting final agents here, as tx.getFinalAgents could return null
					data.setFinalAgents( this.currentAgents.getOrderedList() );
					ReplicationPanel.this.replicationFinished( data );
				}
			}
		}
		
		private ReplicationData calculateReplication( Auction auction ) {
			boolean terminated = false;
			long lastRunningTimeUpdate = 0;
			
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

				if ( TerminationMode.TOTAL_TX == this.terminationMode ) {
					terminated = this.totalTxCount >= this.maxTx;
					
				} else if ( TerminationMode.FAIL_TOTAL_TX == this.terminationMode ) {
					terminated = this.failTxTotalCount >= this.maxTx;
				
				} else if ( TerminationMode.FAIL_SUCCESSIVE_TX == this.terminationMode ) {
					terminated = this.failTxSuccessiveCount >= this.maxTx;
				
				} else if ( TerminationMode.EQUILIBRIUM == this.terminationMode && tx.isEquilibrium() ) {
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
					data.setNumber( ( int ) ReplicationPanel.this.replicationCountSpinner.getValue() - this.currentReplication + 1 );
					data.setTaskId( this.taskId );
					data.setTxCount( this.totalTxCount );
					
					this.nextTxFlag = false;
					
					return data;
				}
				
				// NOTE: if task 0 has finished, runningtime wont be updated anymore (maybe use TimerTask)
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
