package frontend.experimenter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import frontend.MainWindow;
import frontend.experimenter.xml.experiment.ExperimentBean;
import frontend.experimenter.xml.experiment.ExperimentListBean;
import frontend.experimenter.xml.result.ResultBean;

@SuppressWarnings("serial")
public class ExperimenterPanel extends JPanel {

	private JButton openExperimentButton;
	private JButton openResultButton;
	private JButton runButton;
	
	private JTextField experimentPathTextField;
	private JTextField experimentResultPathTextField;
	
	private JFileChooser fileChooser;
	
	private JPanel experimentsPanel;
	
	private MainWindow mainWindow;
	
	public ExperimenterPanel( MainWindow mainWindow ) {
		this.mainWindow = mainWindow;
		
		this.setLayout( new BorderLayout() );
		
		this.createControls();
		this.openExperiment(new File( "D:\\Dropbox\\Dropbox\\FH Studium\\Masterthesis\\Code\\DoubleAuctionWithNetworks\\experiments\\experiment1\\experiment1.xml") ) ;
	}

	private void createControls() {
		this.openExperimentButton = new JButton( "Open Experiment" );
		this.openResultButton = new JButton( "Open Result" );
		this.runButton = new JButton( "Run" );
		
		this.experimentPathTextField = new JTextField();
		this.experimentResultPathTextField = new JTextField();
		
		this.fileChooser = new JFileChooser();
		this.fileChooser.setFileFilter( new FileNameExtensionFilter( "XML-Files", "xml" ) );
		this.fileChooser.setCurrentDirectory( new File( System.getProperty("user.dir") ) );
		
		this.runButton.setEnabled( false );
		
		this.experimentsPanel = new JPanel( new GridBagLayout() );

		JScrollPane experimentsScrollPanel = new JScrollPane( this.experimentsPanel );
		experimentsScrollPanel.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
		experimentsScrollPanel.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		
		ActionListener openButtonAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int returnVal = ExperimenterPanel.this.fileChooser.showOpenDialog( ExperimenterPanel.this );
				 
	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                File file = ExperimenterPanel.this.fileChooser.getSelectedFile();
	                if ( e.getSource() == ExperimenterPanel.this.openExperimentButton ) {
	                	ExperimenterPanel.this.openExperiment( file );
	                	
	                } else if ( e.getSource() == ExperimenterPanel.this.openResultButton ) {
	                	ExperimenterPanel.this.openResult( file );
	                	
	                }
	            } else {
	               System.out.println("Open command cancelled by user.");
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
		c.gridwidth = 10;
		c.weightx = 0.9;
		controlsPanel.add( this.experimentResultPathTextField, c );
		c.gridx = 10;
		c.gridwidth = 1;
		c.weightx = 0.1;
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
			
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.gridx = 0;
			c.gridy = 0;
			
			for ( ExperimentBean e : experimentList.getExperiments() ) {
				ExperimentPanel panel = new ExperimentPanel( e, false );
				panel.setBorder( BorderFactory.createTitledBorder( BorderFactory.createLineBorder( Color.black ), e.getName() ) );
				
				this.experimentsPanel.add( panel, c );
				
				c.gridy++;
			}
			
			this.revalidate();
			
		} catch (JAXBException e) {
			JOptionPane.showMessageDialog( this, "An Error occured parsing XML-File \"" + file.getName() + "\"" );
		}
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
