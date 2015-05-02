package frontend.networkCreators;

import backend.agents.IAgentFactory;
import backend.agents.network.AgentNetwork;
import backend.markets.Markets;

public abstract class NetworkCreator {

	public abstract AgentNetwork createNetwork( IAgentFactory agentFactory );
	
	public abstract boolean createImportanceSampling( AgentNetwork agents, Markets markets ); 
	
	// return true if createNetwork shall be called by the client instantly, otherwise false
	// when false is returned a call to deferCreation MUST follow
	public abstract boolean createInstant();
	
	// allowes the creator to defer its creation e.g. when parameters need to be retrieved
	// from user-input. a runnable is passed which the creator calls to continue where createInstant 
	// would have returne true
	public abstract void deferCreation( Runnable okCallback );
	
	public abstract String name();
	
	public String toString() {
		return this.name();
	}
}
