package frontend.networkCreators;

import java.util.HashMap;
import java.util.Map;

import backend.agents.IAgentFactory;
import backend.agents.network.AgentNetwork;
import backend.markets.Markets;

public class WattStrogatzCreator extends NetworkCreator {
	private int k = 2;
	private double b = 0.2;
	
	public AgentNetwork createNetwork( IAgentFactory agentFactory ) {
		return AgentNetwork.createWattsStrogatzConnected( this.k, this.b, agentFactory);
	}
	
	public boolean createInstant() {
		return false;
	}
	
	@Override
	public void deferCreation( Runnable okCallback ) {
		okCallback.run();
	}
	
	public String name() {
		return "Watts-Strogatz";
	}
	
	public boolean createImportanceSampling( AgentNetwork agents, Markets markets ) {
		return false;
	}
	
	public void setParams( Map<String, String> params ) {
		this.k = Integer.parseInt( params.get( "k" ) );
		this.b = Double.parseDouble( params.get( "b" ) );
	}
	
	public Map<String, String> getParams() {
		Map<String, String> params = new HashMap<String, String>();
		params.put( "k", String.valueOf( this.k ) );
		params.put( "b", String.valueOf( this.b ) );

		return params;
	}
}
