package frontend.networkCreators;

import backend.agents.IAgentFactory;
import backend.agents.network.AgentNetwork;

public class WattStrogatzCreator implements INetworkCreator {
	private int k = 2;
	private double b = 0.2;
	
	public AgentNetwork createNetwork( IAgentFactory agentFactory ) {
		return AgentNetwork.createWattsStrogatzConnected( this.k, this.b, agentFactory);
	}
	
	public boolean createInstant() {
		return false;
	}
	
	@Override
	public void deferCreation( Runnable okCallback ) {
		okCallback.run();
	}
	
	public String toString() {
		return "Watts-Strogatz";
	}
}
