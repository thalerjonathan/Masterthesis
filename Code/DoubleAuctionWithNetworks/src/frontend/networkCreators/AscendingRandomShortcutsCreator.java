package frontend.networkCreators;

import javax.swing.JOptionPane;

import backend.agents.IAgentFactory;
import backend.agents.network.AgentNetwork;
import backend.markets.Markets;

public class AscendingRandomShortcutsCreator implements INetworkCreator {
	private double p = 1.0;
	
	public AgentNetwork createNetwork( IAgentFactory agentFactory ) {
		return AgentNetwork.createAscendingConnectedWithRandomShortcuts( p, agentFactory );
	}

	public void deferCreation( Runnable okCallback ) {
		while ( true ) {
			String input  = JOptionPane.showInputDialog( null, "Probability of shortcut", p );
			
			if ( null != input ) {
				try {
					p = Double.parseDouble( input );
					okCallback.run();
					break;
				} catch ( NumberFormatException e ) {
					
				}
			}
		}
	}
	
	public boolean createInstant() {
		return false;
	}
	
	public String toString() {
		return "Ascending Random Shortcuts";
	}
	
	public boolean createTradingLimits( AgentNetwork agents, Markets markets ) {
		return false;
	}
}
