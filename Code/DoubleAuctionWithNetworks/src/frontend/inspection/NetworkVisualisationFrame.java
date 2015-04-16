package frontend.inspection;

import java.awt.event.WindowAdapter;

import javax.swing.JFrame;

import frontend.networkVisualisation.NetworkRenderPanel;

@SuppressWarnings("serial")
public class NetworkVisualisationFrame extends JFrame {

	private NetworkRenderPanel networkRenderPanel;
	
	public NetworkVisualisationFrame( WindowAdapter adapter ) {
		super( "Agent Network" );
		
		// TODO: add layout-selection
		
		this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		this.addWindowListener( adapter );
		this.pack();
		this.setVisible( true );
	}
	
	public void setNetworkRenderPanel( NetworkRenderPanel p ) {
		if ( null != this.networkRenderPanel ) {
			this.getContentPane().remove( this.networkRenderPanel );
		}
		
		// TODO: check if layout matches
		
		this.getContentPane().add( p );
		this.pack();
		
		this.networkRenderPanel = p;
	}
}
