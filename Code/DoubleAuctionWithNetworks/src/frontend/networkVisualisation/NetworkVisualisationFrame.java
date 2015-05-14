package frontend.networkVisualisation;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;

@SuppressWarnings("serial")
public class NetworkVisualisationFrame extends JFrame {

	private JComboBox<String> layoutSelection;
	private NetworkRenderPanel networkRenderPanel;
	private JButton recreateButton;
	private JCheckBox keepSuccTXHighCheck;
	
	public NetworkVisualisationFrame() {
		super( "Agent Network" );
		
		// TODO: add layout-selection
		
		/*
		this.layoutSelection = new JComboBox<String>( new String[] { "Circle", "KK" } );
		this.recreateButton = new JButton( "Recreate" );
		
		this.layoutSelection.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if ( null == InspectionPanel.this.agentNetwork ) {
					return;
				}
				
				InspectionPanel.this.createLayout();
			}
		} );

		this.recreateButton.addActionListener( createAgentsAction );
		
		this.add( this.recreateButton );
		this.add( this.layoutSelection );
				// TODO networkVisControlsPanel.add( this.keepSuccTXHighCheck );
		    
		*/
		
		/* TODO
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
		*/
		
		
		this.setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
		this.pack();
		this.setVisible( true );
	}
	
	public void setNetworkRenderPanel( NetworkRenderPanel p ) {
		if ( null != this.networkRenderPanel ) {
			this.getContentPane().remove( this.networkRenderPanel );
		}
		
		// TODO: check if layout matches
		
		this.getContentPane().add( p );
		this.revalidate();
		this.pack();
		
		this.networkRenderPanel = p;
	}
}
