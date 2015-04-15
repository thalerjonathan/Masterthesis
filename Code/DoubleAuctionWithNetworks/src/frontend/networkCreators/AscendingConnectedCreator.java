package frontend.networkCreators;

import backend.agents.IAgentFactory;
import backend.agents.network.AgentNetwork;

public class AscendingConnectedCreator extends ParameterlessCreator {

	public AgentNetwork createNetwork( IAgentFactory agentFactory ) {
		return AgentNetwork.createAscendingConnected( agentFactory );
	}
	
	public String toString() {
		return "Ascending-Connected";
	}
}
