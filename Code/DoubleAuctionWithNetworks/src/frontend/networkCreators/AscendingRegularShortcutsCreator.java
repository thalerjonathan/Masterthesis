package frontend.networkCreators;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import backend.agents.IAgentFactory;
import backend.agents.network.AgentNetwork;
import backend.markets.Markets;

public class AscendingRegularShortcutsCreator extends NetworkCreator {
	private int n = 5;
	
	public AgentNetwork createNetwork( IAgentFactory agentFactory ) {
		return AgentNetwork.createAscendingConnectedWithRegularShortcuts( this.n, agentFactory );
	}
	
	public void deferCreation( Runnable okCallback ) {
		while ( true ) {
			String input  = JOptionPane.showInputDialog( null, "Number of Shortcuts", n );
			
			if ( null != input ) {
				try {
					n = Integer.parseInt( input );
					okCallback.run();
					break;
				} catch ( NumberFormatException e ) {
					
				}
			}
		}
	}
	
	public boolean createInstant() {
		return false;
	}
	
	public String name() {
		return "Ascending Regular Shortcuts";
	}
	
	public boolean createImportanceSampling( AgentNetwork agents, Markets markets ) {
		return false;
	}
	
	public void setParams( Map<String, String> params ) {
		this.n = Integer.parseInt( params.get( "n" ) );
	}
	
	public Map<String, String> getParams() {
		Map<String, String> params = new HashMap<String, String>();
		params.put( "n", String.valueOf( this.n ) );

		return params;
	}
}
