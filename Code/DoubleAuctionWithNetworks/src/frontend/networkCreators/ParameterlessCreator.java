package frontend.networkCreators;

import java.util.Map;

import backend.agents.network.AgentNetwork;
import backend.markets.Markets;

public abstract class ParameterlessCreator extends NetworkCreator {

	public boolean createInstant() {
		return true;
	}
	
	public void deferCreation( Runnable okCallback ) {
		throw new RuntimeException();
	}
	
	public boolean createImportanceSampling( AgentNetwork agents, Markets markets ) {
		return false;
	}
	
	public void setParams( Map<String, String> params ) {
		// silently ignore
	}
	
	public  Map<String, String> getParams() {
		return null;
	}
}
