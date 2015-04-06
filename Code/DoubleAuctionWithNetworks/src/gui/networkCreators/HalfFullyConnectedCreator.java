package gui.networkCreators;

import agents.IAgentFactory;
import agents.network.AgentNetwork;

public class HalfFullyConnectedCreator extends ParameterlessCreator {

	public AgentNetwork createNetwork( IAgentFactory agentFactory ) {
		return AgentNetwork.createHalfFullyConnected( agentFactory );
	}
	
	public String toString() {
		return "Half-Fully Connected";
	}
}
