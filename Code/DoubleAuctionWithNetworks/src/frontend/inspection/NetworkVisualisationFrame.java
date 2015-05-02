package frontend.inspection;

import javax.swing.JFrame;

import frontend.networkVisualisation.NetworkRenderPanel;

@SuppressWarnings("serial")
public class NetworkVisualisationFrame extends JFrame {

	private NetworkRenderPanel networkRenderPanel;
	
	public NetworkVisualisationFrame() {
		super( "Agent Network" );
		
		// TODO: add layout-selection
		
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
