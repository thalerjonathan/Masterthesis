package backend.tx;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import backend.agents.Agent;
import backend.agents.network.AgentNetwork;
import backend.markets.Markets;
import backend.offers.AskOffering;
import backend.offers.BidOffering;

public class Transaction  {
	
	private int transNum;
	private int sweepCount;
	private boolean tradingHalted;
	
	private Match match;
	
	private AskOffering[] bestGlobalAskOfferings;
	private BidOffering[] bestGlobalBidOfferings;
	
	private AskOffering[] bestLocalAskOfferings;
	private BidOffering[] bestLocalBidOfferings;
	
	// NOTE: if a transaction was successful the agents are cloned (see agent.clone) and 
	// stored in this list
	private List<Agent> finalAgents;

	public Transaction() {
		this.bestGlobalAskOfferings = new AskOffering[ Markets.NUMMARKETS ];
		this.bestGlobalBidOfferings = new BidOffering[ Markets.NUMMARKETS ];
		
		this.bestLocalAskOfferings = new AskOffering[ Markets.NUMMARKETS ];
		this.bestLocalBidOfferings = new BidOffering[ Markets.NUMMARKETS ];
	}

	public void exec( Match match ) {
		// buy-offer always at index 0, sell-offer always at index 1
		BidOffering buyOffer = match.getBuyOffer();
		AskOffering sellOffer = match.getSellOffer();
		
		Agent buyer = buyOffer.getAgent();
		Agent seller = sellOffer.getAgent();
		
		buyer.execBuyTransaction( match );
		seller.execSellTransaction( match );
		
		this.match = match;
	}
	
	public Match findMatchesByRandomNeighborhood( Agent a, AgentNetwork agents ) {
		// 1. process ask-offerings: need to find a bidder among the neighborhood of the agent
		Agent neighbor = agents.getRandomNeighbor( a );
		
		// agent has no neighbour => can't trade anything => no matches
		if ( null == neighbor ) {
			return null;
		}
	
		// cannot be null, calculated its offers already
		AskOffering[] askOfferings = a.getCurrentAskOfferings();
		BidOffering[] bidOfferings = a.getCurrentBidOfferings();
		
		AskOffering[] bestAsks = neighbor.getBestAskOfferings();
		BidOffering[] bestBids = neighbor.getBestBidOfferings();
		
		if ( null == bestAsks ) {
			return null;
		}
		
		return Match.matchOffers( askOfferings, bidOfferings, bestAsks, bestBids );
	}
	
	public Match findMatchesByBestNeighborhood( Agent a, AgentNetwork agents ) {
		Iterator<Agent> neighborhood = agents.getNeighbors( a );
		
		AskOffering[] agentAsk = a.getCurrentAskOfferings();
		BidOffering[] agentBid = a.getCurrentBidOfferings();
		
		Arrays.fill( this.bestLocalAskOfferings, null );
		Arrays.fill( this.bestLocalBidOfferings, null );
		
		// NOTE: each market can have multiple bestAsk/bestBids because dominate leads to a single bestAsk/bestBid only in case of Asset->Cash market
		// store offerings over multiple runs, findMatches keeps track of multiple offerings
		// find best ask and bid offers in neighborhood
		while ( neighborhood.hasNext() ) {
			Agent neighbor = neighborhood.next();

			AskOffering[] neighbourAsk = neighbor.getBestAskOfferings();
			for ( int i = 0; i < Markets.NUMMARKETS; ++i ) {
				AskOffering ask = neighbourAsk[ i ];
				AskOffering bestAsk = this.bestLocalAskOfferings[ i ];
						
				if ( null != ask ) {
					if ( bestAsk == null || ask.dominates( bestAsk ) ) {
						this.bestLocalAskOfferings[ i ] = ask;
					}
				}
			}

			BidOffering[] neighbourBid = neighbor.getBestBidOfferings();
			for ( int i = 0; i < Markets.NUMMARKETS; ++i ) {
				BidOffering bid = neighbourBid[ i ];
				BidOffering bestBid = this.bestLocalBidOfferings[ i ];
						
				if ( null != bid ) {
					if ( bestBid == null || bid.dominates( bestBid ) ) {
						this.bestLocalBidOfferings[ i ] = bid;
					}
				}
			}
		}
		
		return Match.matchOffers( agentAsk, agentBid, bestLocalAskOfferings, bestLocalBidOfferings );
	}
	
	// NOTE: assumes full connectivity between agents: each agent knows each other agent
	public Match findMatchesByGlobalOffers( Agent a, AgentNetwork agents ) {
		AskOffering[] askOfferings = a.getCurrentAskOfferings();
		BidOffering[] bidOfferings = a.getCurrentBidOfferings();
	
		// add offerings to the global offer-book
		this.addOfferings( askOfferings, bidOfferings );
		
		return Match.matchOffers( askOfferings, bidOfferings, this.bestGlobalAskOfferings, this.bestGlobalBidOfferings );
	}
	
	private void updateBestAskOfferings( AskOffering offer )   {
		int mkt = offer.getMarketType().ordinal();
		AskOffering bestAsk = bestGlobalAskOfferings[ mkt ];
		
		if ( null == bestAsk || offer.dominates( bestAsk ) ) {
			bestGlobalAskOfferings[ mkt ] = offer;
		}
	}
	
	private void updateBestBidOfferings( BidOffering offer )   {
		int mkt = offer.getMarketType().ordinal();
		BidOffering bestBid = bestGlobalBidOfferings[ mkt ];
		
		if ( null == bestBid || offer.dominates( bestBid ) ) {
			bestGlobalBidOfferings[ mkt ] = bestBid;
		}
	}
	
	private void addOfferings(AskOffering[] askoff, BidOffering[] bidoff)   {
		//side effect: updates best offerings
		for (int i=0;i<Markets.NUMMARKETS;i++)  {
			if (askoff[i] != null)  {
				updateBestAskOfferings(askoff[i]);
			}				
			if (bidoff[i] != null)  {
				updateBestBidOfferings(bidoff[i]);
			}				
		}
	}

	public void clearGlobalOfferings() {
		Arrays.fill( this.bestGlobalBidOfferings, null );
		Arrays.fill( this.bestGlobalAskOfferings, null );	
	}

	public Match getMatch() {
		return this.match;
	}
	
	public boolean wasSuccessful()  {
		return this.match != null;
	}

	public int getTransNum() {
		return transNum;
	}

	public void setTransNum(int transNum) {
		this.transNum = transNum;
	}
	
	public boolean hasTradingHalted() {
		return tradingHalted;
	}

	public void setTradingHalted( boolean tradingHalted ) {
		this.tradingHalted = tradingHalted;
	}

	public List<Agent> getFinalAgents() {
		return finalAgents;
	}

	public void setFinalAgents(List<Agent> finalAgents) {
		this.finalAgents = finalAgents;
	}

	public int getSweepCount() {
		return sweepCount;
	}

	public void setSweepCount(int sweepCount) {
		this.sweepCount = sweepCount;
	}
}
