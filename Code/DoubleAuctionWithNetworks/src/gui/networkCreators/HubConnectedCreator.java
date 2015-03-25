package gui.networkCreators;

import javax.swing.JOptionPane;

import agents.IAgentFactory;
import agents.network.AgentNetwork;

public class HubConnectedCreator implements INetworkCreator {
	private int n = 3;
	
	public AgentNetwork createNetwork( IAgentFactory agentFactory ) {
		return AgentNetwork.createWithHubs( this.n, agentFactory );
	}

	public void deferCreation( Runnable okCallback, Runnable cancelCallback ) {
		String input = JOptionPane.showInputDialog( null, "Nubmer of Hubs", this.n );
		
		if ( null != input ) {
			this.n = Integer.parseInt( input );
			okCallback.run();
			
		} else {
			cancelCallback.run();
		}
	}
	
	public boolean createInstant() {
		return false;
	}
	
	public String toString() {
		return "N Hubs";
	}
}