package gui.networkCreators;

import javax.swing.JOptionPane;

import agents.IAgentFactory;
import agents.network.AgentNetwork;

public class HubConnectedCreator implements INetworkCreator {
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
	
	public String toString() {
		return "N Hubs";
	}
}