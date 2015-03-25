package gui.networkCreators;

import agents.IAgentFactory;
import agents.network.AgentNetwork;

public class MaximumHubCreator extends ParameterlessCreator {

	public AgentNetwork createNetwork( IAgentFactory agentFactory ) {
		return AgentNetwork.createWithMaximumHub( agentFactory );
	}
	
	public String toString() {
		return "Maximum Hub";
	}
}
