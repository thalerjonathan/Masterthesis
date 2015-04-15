package frontend.networkCreators;

import backend.agents.IAgentFactory;
import backend.agents.network.AgentNetwork;

public class FullyConnectedCreator extends ParameterlessCreator {

	public AgentNetwork createNetwork( IAgentFactory agentFactory ) {
		return AgentNetwork.createFullyConnected( agentFactory );
	}
	
	public String toString() {
		return "Fully Connected";
	}
}
