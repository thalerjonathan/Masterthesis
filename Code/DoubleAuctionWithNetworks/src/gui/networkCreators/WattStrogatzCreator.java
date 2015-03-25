package gui.networkCreators;

import agents.IAgentFactory;
import agents.network.AgentNetwork;

public class WattStrogatzCreator implements INetworkCreator {

	public AgentNetwork createNetwork( IAgentFactory agentFactory ) {
		return AgentNetwork.createWattsStrogatzConnected( 2, 0.2, agentFactory);
	}
	
	public boolean createInstant() {
		return false;
	}
	
	@Override
	public void deferCreation( Runnable okCallback, Runnable cancelCallback ) {
		okCallback.run();
	}
	
	public String toString() {
		return "Watts-Strogatz";
	}
}
