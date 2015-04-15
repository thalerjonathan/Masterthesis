package frontend.networkCreators;

import backend.agents.IAgentFactory;
import backend.agents.network.AgentNetwork;

public class MedianHubCreator extends ParameterlessCreator {

	public AgentNetwork createNetwork( IAgentFactory agentFactory ) {
		return AgentNetwork.createWithMedianHub( agentFactory );
	}
	
	public String toString() {
		return "Median Hub";
	}
}
