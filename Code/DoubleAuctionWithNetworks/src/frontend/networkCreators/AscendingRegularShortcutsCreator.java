package frontend.networkCreators;

import javax.swing.JOptionPane;

import backend.agents.IAgentFactory;
import backend.agents.network.AgentNetwork;

public class AscendingRegularShortcutsCreator implements INetworkCreator {
	private int n = 5;
	
	public AgentNetwork createNetwork( IAgentFactory agentFactory ) {
		return AgentNetwork.createAscendingConnectedWithRegularShortcuts( this.n, agentFactory );
	}
	
	public void deferCreation( Runnable okCallback ) {
		while ( true ) {
			String input  = JOptionPane.showInputDialog( null, "Number of Shortcuts", n );
			
			if ( null != input ) {
				try {
					n = Integer.parseInt( input );
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
		return "Ascending Regular Shortcuts";
	}
}
