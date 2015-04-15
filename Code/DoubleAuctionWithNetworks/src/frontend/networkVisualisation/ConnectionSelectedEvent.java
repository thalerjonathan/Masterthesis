package frontend.networkVisualisation;

import backend.agents.network.AgentConnection;

public class ConnectionSelectedEvent extends NetworkSelectionEvent {
	private AgentConnection selectedConnection;
	
	public ConnectionSelectedEvent(boolean b, AgentConnection c ) {
		super(b);
		
		this.selectedConnection = c;
	}

	public AgentConnection getSelectedConnection() {
		return selectedConnection;
	}
}