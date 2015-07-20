package frontend.networkCreators;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import backend.agents.IAgentFactory;
import backend.agents.network.AgentNetwork;
import backend.markets.Markets;

public class BuyersAnd2SellersCreator extends NetworkCreator {

	private int i = 0;
	
	@Override
	public AgentNetwork createNetwork(IAgentFactory agentFactory) {
		return AgentNetwork.create2BuyersAnd2Sellers( agentFactory, i );
	}

	@Override
	public String name() {
		return "2Buyers-And-2Sellers";
	}
	
	@Override
	public boolean createImportanceSampling(AgentNetwork agents, Markets markets) {
		return false;
	}

	@Override
	public boolean createInstant() {
		return false;
	}

	@Override
	public void deferCreation(Runnable okCallback) {
		while ( true ) {
			String input  = JOptionPane.showInputDialog( null, "Index", i );
			
			if ( null != input ) {
				try {
					i = Integer.parseInt( input );
					okCallback.run();
					break;
				} catch ( NumberFormatException e ) {
					
				}
			}
		}
	}

	@Override
	public void setParams(Map<String, String> params) {
		this.i = Integer.parseInt( params.get( "i" ) );
	}

	@Override
	public Map<String, String> getParams() {
		Map<String, String> params = new HashMap<String, String>();
		params.put( "i", String.valueOf( this.i ) );
		return params;
	}
}
