package gui.networkCreators;

import agents.IAgentFactory;
import agents.network.AgentNetwork;

public class BarbasiAlbertCreator implements INetworkCreator {
	private int m0 = 3;
	private int m = 1;
	
	public AgentNetwork createNetwork( IAgentFactory agentFactory ) {
		return AgentNetwork.createBarbasiAlbertConnected( this.m0, this.m, agentFactory);
	}
	
	public boolean createInstant() {
		return false;
	}
	
	@Override
	public void deferCreation( Runnable okCallback, Runnable cancelCallback) {
		// TODO: implement
		okCallback.run();
	}
	
	public String toString() {
		return "Barbasi-Albert";
	}
}
