package frontend.networkCreators;

import backend.agents.IAgentFactory;
import backend.agents.network.AgentNetwork;

public class MaximumHubCreator extends ParameterlessCreator {

	public AgentNetwork createNetwork( IAgentFactory agentFactory ) {
		return AgentNetwork.createWithMaximumHub( agentFactory );
	}
	
	public String toString() {
		return "Maximum Hub";
	}
}
