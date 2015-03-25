package gui.networkCreators;

import javax.swing.JOptionPane;

import agents.IAgentFactory;
import agents.network.AgentNetwork;

public class AscendingRandomShortcutsCreator implements INetworkCreator {
	private double p = 1.0;
	
	public AgentNetwork createNetwork( IAgentFactory agentFactory ) {
		return AgentNetwork.createAscendingConnectedWithRandomShortcuts( p, agentFactory );
	}

	public void deferCreation( Runnable okCallback, Runnable cancelCallback ) {
		String input = JOptionPane.showInputDialog( null, "Probability of shortcut", p );
		if ( null != input ) {
			p = Double.parseDouble( input );
			okCallback.run();
			
		} else {
			cancelCallback.run();
		}
	}
	
	public boolean createInstant() {
		return false;
	}
	
	public String toString() {
		return "Ascending Random Shortcuts";
	}
}
