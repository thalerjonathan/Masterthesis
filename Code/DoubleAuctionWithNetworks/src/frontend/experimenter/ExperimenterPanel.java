package frontend.experimenter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import backend.replications.ReplicationsRunner;
import backend.replications.ReplicationsRunner.ReplicationsListener;
import frontend.MainWindow;
import frontend.experimenter.xml.experiment.ExperimentBean;
import frontend.experimenter.xml.experiment.ExperimentListBean;
import frontend.experimenter.xml.result.ResultBean;
import frontend.replication.ReplicationData;
import frontend.replication.ReplicationPanel;

@SuppressWarnings("serial")
public class ExperimenterPanel extends JPanel {

	private JButton openExperimentButton;
	private JButton openResultButton;
	private JButton runAllButton;
	
	private JTextField experimentPathTextField;
	
	private JFileChooser fileChooser;

	private JPanel experimentsPanel;
	
	private MainWindow mainWindow;
	
	private HashMap<ExperimentBean, ExperimentPanel> loadedExperiments;
	private HashMap<ExperimentBean, ExperimentPanel> scheduledExperiments;
	private HashMap<ExperimentBean, ExperimentPanel> finishedExperiments;
	
	private final static File EXPERIMENTS_DIRECTORY = new File( System.getProperty( "user.dir" ) + File.separator + "experiments" );
	private final static File REPLICATIONS_DIRECTORY = new File( System.getProperty( "user.dir" ) + File.separator + "replications" );
	
	public ExperimenterPanel( MainWindow mainWindow ) {
		this.mainWindow = mainWindow;
		this.loadedExperiments = new HashMap<>();
		this.scheduledExperiments = new HashMap<>();
		this.finishedExperiments = new HashMap<>();
		
		this.setLayout( new BorderLayout() );
		
		this.createControls();
		this.openExperiment(new File( EXPERIMENTS_DIRECTORY.getAbsoluteFile() + "\\experiment1\\experiment1.xml" ) ) ;
	}

	private void runAllExperiments() {
		this.scheduledExperiments.putAll( this.loadedExperiments );
		
		Iterator<Entry<ExperimentBean, ExperimentPanel>> iter = this.loadedExperiments.entrySet().iterator();
		while ( iter.hasNext() ) {
			Entry<ExperimentBean, ExperimentPanel> entry = iter.next();
			ExperimentBean experiment = entry.getKey();
			ExperimentPanel panel = entry.getValue();
			
			
		}
	}
	
	private void createControls() {
		this.openExperimentButton = new JButton( "Open Experiment" );
		this.openResultButton = new JButton( "Open Result" );
		this.runAllButton = new JButton( "Run All" );
		
		this.experimentPathTextField = new JTextField();
		this.experimentPathTextField.setEditable( false );
		
		this.fileChooser = new JFileChooser();
		this.fileChooser.setFileFilter( new FileNameExtensionFilter( "XML-Files", "xml" ) );

		this.runAllButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ExperimenterPanel.this.runAllExperiments();
			}
		});
		this.experimentsPanel = new JPanel( new GridBagLayout() );

		JScrollPane experimentsScrollPanel = new JScrollPane( this.experimentsPanel );
		experimentsScrollPanel.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
		experimentsScrollPanel.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		
		ActionListener openButtonAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if ( e.getSource() == ExperimenterPanel.this.openExperimentButton ) {
					ExperimenterPanel.this.fileChooser.setCurrentDirectory( EXPERIMENTS_DIRECTORY );
					
                } else if ( e.getSource() == ExperimenterPanel.this.openResultButton ) {
                	ExperimenterPanel.this.fileChooser.setCurrentDirectory( REPLICATIONS_DIRECTORY );
            		
                }
				
				int returnVal = ExperimenterPanel.this.fileChooser.showOpenDialog( ExperimenterPanel.this );
				
	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                File file = ExperimenterPanel.this.fileChooser.getSelectedFile();
	                
	                if ( e.getSource() == ExperimenterPanel.this.openExperimentButton ) {
	                	ExperimenterPanel.this.openExperiment( file );
	                	
	                } else if ( e.getSource() == ExperimenterPanel.this.openResultButton ) {
	                	ExperimenterPanel.this.openResult( file );
	                	
	                }
	            }	
			}
		};
			
		this.openExperimentButton.addActionListener( openButtonAction );
		this.openResultButton.addActionListener( openButtonAction );
		
		JPanel controlsPanel = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;

		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 1;
		c.gridwidth = 10;
		c.weightx = 0.9;
		controlsPanel.add( this.experimentPathTextField, c );
		c.gridx = 10;
		c.gridwidth = 1;
		c.weightx = 0.1;
		controlsPanel.add( this.openExperimentButton, c );
		
		c.gridx = 0;
		c.gridy = 1;
		c.gridheight = 1;
		c.gridwidth = 11;
		c.weightx = 1.0;
		controlsPanel.add( this.openResultButton, c );
		
		this.add( controlsPanel, BorderLayout.NORTH );
		this.add( experimentsScrollPanel, BorderLayout.CENTER );
	}

	private void openExperiment( File file ) {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance( ExperimentListBean.class );
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			ExperimentListBean experimentList = ( ExperimentListBean ) jaxbUnmarshaller.unmarshal( file );
			
			this.experimentsPanel.removeAll();
			this.loadedExperiments.clear();
			this.finishedExperiments.clear();
			this.scheduledExperiments.clear();
			
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.gridx = 0;
			c.gridy = 0;
			
			for ( ExperimentBean experimentBean : experimentList.getExperiments() ) {
				ExperimentPanel panel = new ExperimentPanel( experimentBean, new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						runExperiment( experimentBean );
					}
				});
				panel.setBorder( BorderFactory.createTitledBorder( BorderFactory.createLineBorder( Color.black ), experimentBean.getName() ) );
				
				this.experimentsPanel.add( panel, c );
				
				c.gridy++;
				
				this.loadedExperiments.put( experimentBean, panel );
			}
			
			this.experimentsPanel.add( this.runAllButton, c );
			
			this.experimentPathTextField.setText( file.getAbsolutePath() );
			this.revalidate();
			
		} catch (JAXBException e) {
			JOptionPane.showMessageDialog( this, "An Error occured parsing XML-File \"" + file.getAbsoluteFile() + "\"" );
		}
	}
	
	private void runExperiment( ExperimentBean e ) {
		// TODO: need to create agents & markets
		
		ReplicationsRunner replicationRunner = new ReplicationsRunner( this.agentNetworkTemplate, this.markets );
		replicationRunner.start( e.getReplications(), e.getTerminationMode(), e.getMaxTx(), new ReplicationsListener() {
			@Override
			public void replicationFinished(ReplicationData data, ReplicationData averageData ) {
				SwingUtilities.invokeLater( new Runnable() {
					@Override
					public void run() {
						// update progress-bars
					}
				} );
			}
			
			@Override
			public void allReplicationsFinished() {
				// TODO: do next experiment
			}
		} );
	}
	
	private void openResult( File file ) {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance( ResultBean.class );
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			ResultBean resultBean = ( ResultBean ) jaxbUnmarshaller.unmarshal( file );
			
			ExperimentResultPanel resultPanel = new ExperimentResultPanel( resultBean );
			this.mainWindow.addPanel( resultPanel, resultBean.getExperiment().getName() );

		} catch (JAXBException e) {
			JOptionPane.showMessageDialog( this, "An Error occured parsing XML-File \"" + file.getName() + "\"" );
		}
	}
}
