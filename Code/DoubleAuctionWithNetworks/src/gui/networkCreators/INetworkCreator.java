package gui.networkCreators;

import agents.IAgentFactory;
import agents.network.AgentNetwork;

public interface INetworkCreator {

	public AgentNetwork createNetwork( IAgentFactory agentFactory );
	
	// return true if createNetwork shall be called by the client instantly, otherwise false
	// when false is returned a call to deferCreation MUST follow
	public boolean createInstant();
	
	// allowes the creator to defer its creation e.g. when parameters need to be retrieved
	// from user-input. a runnable is passed which the creator calls to continue where createInstant 
	// would have returne true
	public void deferCreation( Runnable okCallback, Runnable cancelCallback );
	
	public String toString();
}
