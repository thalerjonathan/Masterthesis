package frontend.networkVisualisation;

public interface INetworkSelectionObserver {
	public void agentSeleted( AgentSelectedEvent agentSelectedEvent );
	public void connectionSeleted( ConnectionSelectedEvent connSelectedEvent );
}