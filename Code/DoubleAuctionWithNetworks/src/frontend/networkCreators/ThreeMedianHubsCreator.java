package frontend.networkCreators;

import backend.agents.IAgentFactory;
import backend.agents.network.AgentNetwork;

public class ThreeMedianHubsCreator extends ParameterlessCreator {

	public AgentNetwork createNetwork( IAgentFactory agentFactory ) {
		return AgentNetwork.createWith3MedianHubs(agentFactory);
	}
	
	public String name() {
		return "3 Median Hubs";
	}
}
