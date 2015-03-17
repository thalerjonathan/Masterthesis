package gui.visualisation;

import agents.Agent;

public class AgentSelectedEvent extends NetworkSelectionEvent {
	private Agent selectedAgent;
	
	public AgentSelectedEvent( boolean b, Agent a ) {
		super(b);

		this.selectedAgent = a;
	}

	public Agent getSelectedAgent() {
		return selectedAgent;
	}
}