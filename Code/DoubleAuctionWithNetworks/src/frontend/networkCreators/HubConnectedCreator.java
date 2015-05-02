package frontend.networkCreators;

import javax.swing.JOptionPane;

import backend.agents.IAgentFactory;
import backend.agents.network.AgentNetwork;
import backend.markets.Markets;

public class HubConnectedCreator extends NetworkCreator {
	private int n = 3;
	
	public AgentNetwork createNetwork( IAgentFactory agentFactory ) {
		return AgentNetwork.createWithHubs( this.n, agentFactory );
	}

	public void deferCreation( Runnable okCallback ) {
		while ( true ) {
			String input = JOptionPane.showInputDialog( null, "Nubmer of Hubs", this.n );
			if ( null != input ) {
				try {
					this.n = Integer.parseInt( input );
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
		return "N Hubs";
	}
	
	public boolean createImportanceSampling( AgentNetwork agents, Markets markets ) {
		return false;
	}
}