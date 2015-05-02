package frontend.networkCreators;

import backend.agents.IAgentFactory;
import backend.agents.network.AgentNetwork;

public class HalfFullyConnectedCreator extends ParameterlessCreator {

	public AgentNetwork createNetwork( IAgentFactory agentFactory ) {
		return AgentNetwork.createHalfFullyConnected( agentFactory );
	}
	
	public String name() {
		return "Half-Fully Connected";
	}
}
