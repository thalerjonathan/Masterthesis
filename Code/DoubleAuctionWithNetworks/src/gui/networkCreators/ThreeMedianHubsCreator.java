package gui.networkCreators;

import agents.IAgentFactory;
import agents.network.AgentNetwork;

public class ThreeMedianHubsCreator extends ParameterlessCreator {

	public AgentNetwork createNetwork( IAgentFactory agentFactory ) {
		return AgentNetwork.createWith3MedianHubs(agentFactory);
	}
	
	public String toString() {
		return "3 Median Hubs";
	}
}
