package doubleAuction;

import agents.Agent;
import agents.markets.Asset;
import agents.markets.Loans;
import agents.network.AgentNetwork;
import doubleAuction.tx.Transaction;
import doubleAuction.tx.TransactionWithLoans;

public class AuctionWithLoans extends Auction {
	protected double[] initialLoanPrices;
	protected double[] J;
	protected Loans loanMarket;
	protected double[][] agentsWealthMean;
	
	public AuctionWithLoans( AgentNetwork agents, Asset asset )   {
		super( agents, asset );
		agentsWealthMean = new double[this.agents.size()][5];
	}
    
	@Override
	protected Transaction getNewTransaction() {
		// TODO Auto-generated method stub
		return new TransactionWithLoans(this);
	}

	@Override
	protected void initializeMarkets()  {

		//loans
//		J = new double[] {0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.6, 0.7, 0.8, 0.9};
		J = new double[] {0.2};
//    	J = new double[] {0.1};
		//	    	initialLoanPrices = new double[] { 0.3, 0.34, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2};
		//	    	initialLoanPrices = new double[] {0.199999, 0.234499};
		//	    	initialLoanPrices = new double[] {0.2, 0.25};
//		initialLoanPrices = new double[] {0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.6, 0.7, 0.8, 0.9};
		//	    	initialLoanPrices = new double[] {0.025, 0.5, 0.075, 0.1, 0.125, 0.15, 0.175, 0.2, 0.225, 0.25, 0.3, 0.35, 0.4, 0.45};
    	initialLoanPrices = new double[] {0.2};

		//assets
		//initialAssetprice = 0.6;
		
		//1. initialize asset market
		//asset = new Asset(initialAssetprice, Agent.NUM_AGENTS*assetEndow);

		//2. initialize loan market
		loanMarket = new Loans(initialLoanPrices,J);

		//3. set NUMMARKETS: NUMLOANS loan markets and 1+NUMLOANS asset markets (asset for cash and asset for loan type j)
		NUMMARKETS = Loans.NUMLOANS + Loans.NUMLOANS + 1;
		NUMMARKETTYPES = 3; //0:assets against cash; 1:assets against loans; 2: loans
	}

	@Override	
	protected void 	initializeAgents()  {
		//assetEndow=1;
		//consumEndow=1;
		//Agent.NUM_AGENTS = NUM_AGENTS;
		Agent.TRADE_ONLY_FULL_UNITS = true;
		Agent.NUMMARKETS = NUMMARKETS;
		
//		agents = new AgentWithLoans[] {new AgentWithLoans(0.5, consumEndow, assetEndow, loanMarket, asset), 
//		    						  new AgentWithLoans(0.7, consumEndow, assetEndow, loanMarket, asset), 
//									  new AgentWithLoans(0.8, consumEndow, assetEndow, loanMarket, asset)};

		/*
		agents = new AgentWithLoans[Agent.NUM_AGENTS];
		randomlyOrderedAgents = new ArrayList<Agent>();

		for (int i=0;i<Agent.NUM_AGENTS; i++) {
			agents[i] = new  AgentWithLoans(i,(i+1)*(1.0/Agent.NUM_AGENTS), consumEndow, assetEndow, loanMarket, asset);
			randomlyOrderedAgents.add(agents[i]);
		}

		
		this.agents = AgentNetwork.createWithHubs( 3, new IAgentFactory() {
			private int i = 0;
			
			@Override
			public Agent createAgent() {
				Agent a = null;
			
				if ( i < NUM_AGENTS ) {
					a = new AgentWithLoans(i,(i+1)*(1.0/Agent.NUM_AGENTS), consumEndow, assetEndow, loanMarket, asset);
					i++;
				}
				
				return a;
			}
		});
		*/
		
		hMin = 0;
		hMax = new double[Loans.NUMLOANS];
		idMax = new int[Loans.NUMLOANS];
		for (int i=0;i<Loans.NUMLOANS;i++) {
			hMax[i] = agents.get(agents.size() - 1).getH();
			idMax[i] = this.agents.size()-1;
		}
	}
	
	public Loans getLoanMarket() {
		return loanMarket;
	}
}
