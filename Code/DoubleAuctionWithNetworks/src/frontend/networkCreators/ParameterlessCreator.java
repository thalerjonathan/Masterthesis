package frontend.networkCreators;

import backend.agents.network.AgentNetwork;
import backend.markets.Markets;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public abstract class ParameterlessCreator implements INetworkCreator {

	public boolean createInstant() {
		return true;
	}
	
	public void deferCreation( Runnable okCallback ) {
		throw new NotImplementedException();
	}
	
	public boolean createImportanceSampling( AgentNetwork agents, Markets markets ) {
		return false;
	}
}
