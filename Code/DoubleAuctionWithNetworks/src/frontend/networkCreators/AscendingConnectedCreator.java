package frontend.networkCreators;

import backend.agents.IAgentFactory;
import backend.agents.network.AgentNetwork;
import backend.markets.Markets;

public class AscendingConnectedCreator extends ParameterlessCreator {

	public AgentNetwork createNetwork( IAgentFactory agentFactory ) {
		return AgentNetwork.createAscendingConnected( agentFactory );
	}
	
	public String name() {
		return "Ascending-Connected";
	}
	
	public boolean createImportanceSampling( AgentNetwork agents, Markets markets ) {
		int agentCount = agents.size();
		double[] ca = new double[ agentCount + 1 ];
		double[] cl = new double[ agentCount + 1 ];
		double[] cal = new double[ agentCount + 1 ];
		ca[1] = 0;
		cl[1] = 0;
		cal[1] = 0;
		
		double FV = markets.V();
		
		// NOTE: i MUST run from 1 to agentCount
		for (int i = 1; i <= agentCount; i++)  {
			int k = agentCount;
			if (i < agentCount*0.5)  {
				ca[i] = (k - i)*(i + 2)*(0.8 / (k + 1) + ca[i]) / ((k + 1 - i)*(i + 1)) - 0.8 / (k + 1);
				cl[i] = (i + 2)*(k - i)*((FV - 0.2) / (k + 1) + cl[i]) / ((i + 1)*(k + 1 - i)) - (FV - 0.2) / (k + 1);
				double ei0 = (0.2 + 0.8*i / (k + 1)) / (0.2 + (FV - 0.2)*i / (k + 1));
				double ei1 = (0.2 + 0.8*(i + 1) / (k + 1)) / (0.2 + (FV - 0.2)*(i + 1) / (k + 1));
				double ei2 = (0.2 + 0.8*(i + 2) / (k + 1)) / (0.2 + (FV - 0.2)*(i + 2) / (k + 1));
				double deltai0 = ei1 - ei0;
				double deltai1 = ei2 - ei1;
				cal[i] = deltai0*(5.0 - ei1)*(ei2 - 0.2 / FV)*(deltai0 + cal[i]) / (deltai1*(5.0 - ei0)*(ei1 - 0.2 / FV)) - deltai1;
				//		  std::cout << ca[i + 1] << ", " << cl[i + 1] << ", " << cal[i + 1] << std::endl;
			}
			else {
				ca[i] = ca[agentCount - i];
				cl[i] = cl[agentCount - i];
				cal[i] = cal[agentCount - i];
			}
		}
		
		for (int i = 1; i <= agentCount; i++)  {
			double[][] limitAssets = new double[2][2]; //first index: 0: buy, 1: sell; second index: 0: lower 1: upper limit
			double[][] limitLoans = new double[2][2];
			double[][] limitAssetLoans= new double[2][2];
			
			// selling lower- and upper-ranges
			if (i < agentCount)  {
				limitAssets[1][0] = agents.get( i - 1 ).getLimitPriceAsset(); //sell lower
				limitAssets[1][1] = agents.get(i).getLimitPriceAsset() + ca[i]; //sell upper
				limitLoans[1][0] = agents.get(i - 1).getLimitPriceLoans();
				limitLoans[1][1] = agents.get(i).getLimitPriceLoans() + cl[i];
				limitAssetLoans[1][0] = agents.get(i - 1).getLimitPriceAsset() / agents.get(i - 1).getLimitPriceLoans();
				limitAssetLoans[1][1] = agents.get(i).getLimitPriceAsset() / agents.get(i).getLimitPriceLoans() + cal[i];
			}
			else {
				limitAssets[1][0] = 1000000;
				limitAssets[1][1] = 1000000;
				limitLoans[1][0] = 1000000;
				limitLoans[1][1] = 1000000;
				limitAssetLoans[1][0] = 1000000;
				limitAssetLoans[1][1] = 1000000;
			}
			// buying lower- and upper-ranges
			if (i > 1)  {
				limitAssets[0][0] = agents.get(i - 2).getLimitPriceAsset(); //buy lower
				limitAssets[0][1] = agents.get(i - 1).getLimitPriceAsset(); //buy upper
				limitLoans[0][0] = agents.get(i - 2).getLimitPriceLoans();
				limitLoans[0][1] = agents.get(i - 1).getLimitPriceLoans();
				limitAssetLoans[0][0] = agents.get(i - 2).getLimitPriceAsset() / agents.get(i - 2).getLimitPriceLoans();
				limitAssetLoans[0][1] = agents.get(i - 1).getLimitPriceAsset() / agents.get(i - 1).getLimitPriceLoans();
	
			}
			else  {
				limitAssets[0][0] = -1000000;
				limitAssets[0][1] = -1000000;
				limitLoans[0][0] = -1000000;
				limitLoans[0][1] = -1000000;
				limitAssetLoans[0][0] = -1000000;
				limitAssetLoans[0][1] = -1000000;
			}
			
			agents.get(i - 1).setImportanceSamplingData(limitAssets, limitLoans, limitAssetLoans);
		}
		
		return true;
	}
}
