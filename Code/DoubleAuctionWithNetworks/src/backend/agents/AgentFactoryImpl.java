package backend.agents;

import backend.markets.Markets;

public class AgentFactoryImpl implements IAgentFactory {

	private int i;
	private int agentCount;
	private Markets markets;

	private boolean triangleOptimismDistribution = false;

	public AgentFactoryImpl( int agentCount, Markets markets ) {
		this.agentCount = agentCount;
		this.markets = markets;
	}
	
	@Override
	public Agent createAgent() {
		Agent a = null;
		
		if ( i < agentCount ) {
			// linear
			double optimism = ( double ) i  / ( double ) agentCount;
			
			// triangle
			if ( triangleOptimismDistribution ) {
				double halfAgentCount = agentCount / 2.0;
				double totalArea = ( agentCount * halfAgentCount ) / 2.0;
				double halfArea = totalArea / 2.0;
				double agentArea = ( ( ( halfAgentCount - this.i ) * ( halfAgentCount - this.i ) ) / 2.0 );
				
				if ( i <= halfAgentCount ) {
					agentArea = halfArea - agentArea;
					
				} else {
					agentArea = halfArea + agentArea;
				}
				
				optimism = agentArea / totalArea;
			}
			
			a = new Agent( i, optimism, markets );

			this.i++;
		}
		
		return a;
	}
}
