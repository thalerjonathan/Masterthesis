package gui.networkCreators;

import javax.swing.JOptionPane;

import agents.IAgentFactory;
import agents.network.AgentNetwork;

public class ErdosRenyiCreator implements INetworkCreator {
	private double p = 0.2;
	
	public AgentNetwork createNetwork( IAgentFactory agentFactory ) {
		return AgentNetwork.createErdosRenyiConnected( this.p, agentFactory );
	}
	
	public void deferCreation( Runnable okCallback, Runnable cancelCallback ) {
		String input = JOptionPane.showInputDialog( null, "Probability of edge-inclusion", this.p );
		if ( null != input ) {
			this.p = Double.parseDouble( input );
			okCallback.run();
			
		} else {
			cancelCallback.run();
		}
	}
	
	public boolean createInstant() {
		return false;
	}
	
	public String toString() {
		return "Erdos-Renyi";
	}
}
