package frontend.networkVisualisation;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import controller.network.export.NetworkExporter;
import utils.Utils;
import backend.agents.Agent;
import backend.agents.network.AgentConnection;
import backend.agents.network.AgentNetwork;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;

@SuppressWarnings("serial")
public class NetworkVisualisationFrame extends JFrame {

	private JComboBox<String> layoutSelection;
	private JButton exportButton;
	private JCheckBox keepSuccTXHighCheck;
	
	private JFileChooser fileChooser;
	
	private NetworkRenderPanel networkRenderPanel;
	private AgentNetwork network;
	
	private final static String CIRCLE_LAYOUT_NAME = "Circle";
	private final static String KK_LAYOUT_NAME = "KK";
	
	public NetworkVisualisationFrame() {
		super( "Agent Network" );
	
		this.getContentPane().setLayout( new BorderLayout() );
		
		this.fileChooser = new JFileChooser();
		this.fileChooser.setFileFilter( new FileNameExtensionFilter( "Gephi-Files (.gexf)", "gexf" ) );
		
		this.layoutSelection = new JComboBox<String>( new String[] { "Circle", "KK" } );
		this.exportButton = new JButton( "Export" );
		
		this.layoutSelection.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent arg0 ) {
				NetworkVisualisationFrame.this.networkRenderPanel.changeLayout( getSelectedLayout() );
				NetworkVisualisationFrame.this.networkRenderPanel.revalidate();
			}
		} );

		this.exportButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				NetworkVisualisationFrame.this.fileChooser.setCurrentDirectory( Utils.NETWORKS_DIRECTORY );
					
				int returnVal = NetworkVisualisationFrame.this.fileChooser.showSaveDialog( NetworkVisualisationFrame.this );
				
				if (returnVal == JFileChooser.APPROVE_OPTION) {
	            	File file = NetworkVisualisationFrame.this.fileChooser.getSelectedFile();
	            	String fileName = file.getAbsolutePath();
	            	
	            	if ( false == fileName.endsWith( ".gexf" ) ) {
	            		fileName += ".gexf";
	            	}
	
	            	NetworkExporter.exportAsGEXF( NetworkVisualisationFrame.this.network, fileName );
	            }
			}
		} );
		
		this.keepSuccTXHighCheck = new JCheckBox( "Keep TXs Highlighted" );
		this.keepSuccTXHighCheck.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				NetworkVisualisationFrame.this.networkRenderPanel.setKeepTXHighlighted( NetworkVisualisationFrame.this.keepSuccTXHighCheck.isSelected() );
				NetworkVisualisationFrame.this.networkRenderPanel.repaint();
			}
		});
		
		JPanel controlsPanel = new JPanel();

		controlsPanel.add( this.exportButton, BorderLayout.NORTH );
		controlsPanel.add( this.layoutSelection, BorderLayout.NORTH );
		controlsPanel.add( this.keepSuccTXHighCheck, BorderLayout.NORTH );

		this.getContentPane().add( controlsPanel, BorderLayout.NORTH );

		this.setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
		this.pack();
		this.setVisible( true );
	}
	
	@SuppressWarnings("unchecked")
	public Class<? extends Layout<Agent, AgentConnection>> getSelectedLayout() {
		Class<? extends Layout<Agent, AgentConnection>> layoutClazz = (Class<? extends Layout<Agent, AgentConnection>>) CircleLayout.class;
		String selectedLayoutName = NetworkVisualisationFrame.this.layoutSelection.getItemAt( NetworkVisualisationFrame.this.layoutSelection.getSelectedIndex() );
		
		if ( CIRCLE_LAYOUT_NAME.equals( selectedLayoutName ) ) {
			layoutClazz = (Class<? extends Layout<Agent, AgentConnection>>) CircleLayout.class;
		} else if ( KK_LAYOUT_NAME.equals( selectedLayoutName ) ) {
			layoutClazz = (Class<? extends Layout<Agent, AgentConnection>>) KKLayout.class;
		}

		return layoutClazz;
	}
	
	public void setNetworkRenderPanel( NetworkRenderPanel p, AgentNetwork network ) {
		if ( null != this.networkRenderPanel ) {
			this.getContentPane().remove( this.networkRenderPanel );
		}
		
		this.getContentPane().add( p, BorderLayout.CENTER );
		//this.revalidate();
		this.pack();
		
		this.networkRenderPanel = p;
		this.network = network;
	}
}
