package frontend.networkCreators;

import java.util.HashMap;
import java.util.Map;

import backend.agents.IAgentFactory;
import backend.agents.network.AgentNetwork;
import backend.markets.Markets;

public class BarbasiAlbertCreator extends NetworkCreator {
	private int m0 = 3;
	private int m = 1;
	
	public AgentNetwork createNetwork( IAgentFactory agentFactory ) {
		return AgentNetwork.createBarbasiAlbertConnected( this.m0, this.m, agentFactory);
	}
	
	public boolean createInstant() {
		return false;
	}
	
	@Override
	public void deferCreation( Runnable okCallback ) {
		// TODO: implement
		okCallback.run();
	}
	
	public String name() {
		return "Barbasi-Albert";
	}
	
	public boolean createImportanceSampling( AgentNetwork agents, Markets markets ) {
		return false;
	}
	
	public void setParams( Map<String, String> params ) {
		this.m0 = Integer.parseInt( params.get( "m0" ) );
		this.m = Integer.parseInt( params.get( "m" ) );
	}
	
	public Map<String, String> getParams() {
		Map<String, String> params = new HashMap<String, String>();
		params.put( "m0", String.valueOf( this.m0 ) );
		params.put( "m", String.valueOf( this.m ) );

		return params;
	}
}
