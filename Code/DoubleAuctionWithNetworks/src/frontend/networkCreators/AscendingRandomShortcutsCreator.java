package frontend.networkCreators;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import backend.agents.IAgentFactory;
import backend.agents.network.AgentNetwork;
import backend.markets.Markets;

public class AscendingRandomShortcutsCreator extends NetworkCreator {
	private double p = 1.0;
	
	public AgentNetwork createNetwork( IAgentFactory agentFactory ) {
		return AgentNetwork.createAscendingConnectedWithRandomShortcuts( p, agentFactory );
	}

	public void deferCreation( Runnable okCallback ) {
		while ( true ) {
			String input  = JOptionPane.showInputDialog( null, "Probability of shortcut", p );
			
			if ( null != input ) {
				try {
					p = Double.parseDouble( input );
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
		return "Ascending Random Shortcuts";
	}
	
	public boolean createImportanceSampling( AgentNetwork agents, Markets markets ) {
		return false;
	}
	
	public void setParams( Map<String, String> params ) {
		this.p = Double.parseDouble( params.get( "p" ) );
	}
	
	public Map<String, String> getParams() {
		Map<String, String> params = new HashMap<String, String>();
		params.put( "p", String.valueOf( this.p ) );

		return params;
	}
}
