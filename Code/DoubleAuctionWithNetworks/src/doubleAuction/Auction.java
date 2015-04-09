package doubleAuction;

import java.util.Iterator;
import java.util.List;

import agents.Agent;
import agents.markets.Asset;
import agents.markets.Loans;
import agents.network.AgentNetwork;
import doubleAuction.offer.Offering;
import doubleAuction.tx.Transaction;

public class Auction {
	public static int NUMMARKETS;
	public static int NUMMARKETTYPES;
	private int num_trans; 
	
	private double[] initialLoanPrices;
	private double[] J;
	private Loans loanMarket;

	private AgentNetwork agents;
	private Asset asset;

	private final static int MAX_SWEEPS = 500;
	
	public enum MatchingType {
		BEST_NEIGHBOUR,
		BEST_GLOBAL_OFFERS,
		RANDOM_NEIGHOUR;
	}

	public Auction( AgentNetwork agents, Asset asset ) {
		this.agents = agents;
		this.asset = asset;
		
		this.num_trans = 1;
	}
	
	public void init() {
		initializeMarkets();
		initializeAgents();
	}
	
	public Transaction executeSingleTransactionByType( MatchingType type, boolean keepAgentHistory )  {
		int sweep = 1;
		Transaction transaction = new Transaction( this );
		
		while ( sweep < Auction.MAX_SWEEPS )  {
			transaction.setSweepCount( sweep );

			this.makeOfferings( sweep );
			
			if ( this.findMatch( transaction, type, keepAgentHistory) ) {
				return transaction;
			}
			
			sweep++;
		}
		
		if ( false == this.isTradingPossible() ) {
			transaction.setReachedEquilibrium( true );
		}
		
		return transaction;
	}
	
	public Asset getAsset() {
		return asset;
	}
	
	public Loans getLoanMarket() {
		return loanMarket;
	}
	
	private void makeOfferings( int numRound ) {
		Iterator<Agent> agIt = agents.randomIterator( true );
		
		// first step: reset offerings of each agent when in first round and then calculate offerings for this round
		while (agIt.hasNext())  {
			Agent a = agIt.next();
			
			// need to reset the best offerings when in first round 
			if ( 1 == numRound ) {
				a.clearBestOfferings();
			}
			
			// let current agent calculate its new offers for this round
			a.calcNewOfferings();
			// will add the previously calculated offerings to the agents best offerings
			a.addCurrentOfferingsToBestOfferings();
		}		
	}
	
	private boolean findMatch( Transaction transaction, MatchingType type, boolean keepAgentHistory ) {
		// get same random-iterator (won't do a shuffle again)
		Iterator<Agent> agIt = agents.randomIterator( false );
		
		while (agIt.hasNext())  {
			Agent a = agIt.next();
			Offering[] match = null;
			
			// find match: must be neighbours, must be same market, bid (buy) must be larger than ask (sell)
			
			if ( MatchingType.RANDOM_NEIGHOUR == type ) {
				match = transaction.findMatchesByRandomNeighborhood( a, agents );
			
			} else if ( MatchingType.BEST_NEIGHBOUR == type ) {
				match = transaction.findMatchesByBestNeighborhood( a, agents );
				
			} else if ( MatchingType.BEST_GLOBAL_OFFERS == type ) {
				match = transaction.findMatchesByGlobalOffers( a, agents );
			}

			// transaction found a match
			if ( match != null ) {
				// executes Transaction: sell and buy in the two agents will update new wealth
				// and will lead to a reset of the best offerings as they are invalidated because
				// of change in wealth.
				// won't calculate a new offering, this is only done once in each round
				if ( a.execTransaction( match, true ) )  {
					transaction.matched( match );
					transaction.setTransNum(num_trans++);
					
					List<Agent> finalAgents = null;
					
					// WARNING: sucks up huge amount of memory if many TXs
					if ( keepAgentHistory ) {
						finalAgents = agents.cloneAgents();
					} else {
						finalAgents = agents.getOrderedList();
					}

					transaction.setFinalAgents( finalAgents );
					
					return true;
				}
			}
		}
		
		return false;
	}
	
	private boolean isTradingPossible() {
		// check if trading is possible within neighborhood
		Iterator<Agent> agentIter = this.agents.iterator();
		while ( agentIter.hasNext() ) {
			Agent a = agentIter.next();
			
			Iterator<Agent> neighbourIter = this.agents.getNeighbors( a );
			while ( neighbourIter.hasNext() ) {
				Agent n = neighbourIter.next();
				
				if ( this.canTrade( a, n ) ) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	private boolean canTrade( Agent a1, Agent a2 ) {
		for ( int i = 0; i < NUMMARKETS; ++i ) {
			// asume neighbour has higher H => must be the buyer
			int sellerOfferingsCount = a1.getBestAskOfferings().get( i ).size();
			int buyerOfferingsCount = a2.getBestBidOfferings().get( i ).size();
			
			// adjust: neighbour has higher H => must be the buyer
			if ( a1.getH() > a2.getH() ) {
				buyerOfferingsCount = a1.getBestBidOfferings().get( i ).size();
				sellerOfferingsCount = a2.getBestAskOfferings().get( i ).size();
			}
			
			// if the buyer has still some bid offers AND the seller has still some ask offers, then they might still match
			if ( sellerOfferingsCount > 0 && buyerOfferingsCount > 0  ) {
				return true;
			}
		}
		
		// TODO: if bider has still cash but not enough to match neighbour asker, then its also not possible
		
		return false;
	}
	
	private void initializeMarkets()  {
		//assets
		//initialAssetprice = 0.6;
		
		//1. initialize asset market
		// TODO: Agent.NUM_AGENTS*assetEndow will always be 0 because assetEndow is 0
		//asset = new Asset(initialAssetprice, Agent.NUM_AGENTS*assetEndow);

		//2. set NUMMARKETS: one market - assets for cash
		
		NUMMARKETS = 1;
		NUMMARKETTYPES = 1;
		
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

	private void initializeAgents()  {
		//assetEndow=1;
		//consumEndow=1;
		//Agent.NUM_AGENTS = NUM_AGENTS;
		Agent.TRADE_ONLY_FULL_UNITS = true;
		Agent.NUMMARKETS = NUMMARKETS;
	}

}
