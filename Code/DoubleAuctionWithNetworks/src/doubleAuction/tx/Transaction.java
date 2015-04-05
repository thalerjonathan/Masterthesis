package doubleAuction.tx;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import doubleAuction.Auction;
import doubleAuction.offer.AskOffering;
import doubleAuction.offer.BidOffering;
import doubleAuction.offer.Offering;
import agents.Agent;
import agents.network.AgentNetwork;

public class Transaction  {
	private boolean reachedEquilibrium;
	protected double assetPrice;
	protected double assetAmount = -1;
	protected double meanAskAssetPrice=0;
	protected double meanBidAssetPrice=0;
	protected double meanAskH=0;
	protected double meanBidH=0;
	protected double finalAskAssetPrice;
	protected double finalBidAssetPrice;
	protected double limitPrice;
	protected double totalAskAgentsAssets;
	protected double totalBidAgentsCash;
	protected double totalUtility;
	protected double totalAskUtility;
	protected double totalBidUtility;
	protected double finalAskH;
	protected double finalBidH;
	protected int transNum;
	protected int NUMMARKETS; //NUMLOANS loan markets and 1+NUMLOANS asset markets (asset for cash and asset for loan type j)
	protected int market;
	protected int length;
	protected Auction auction;
	protected ArrayList<ArrayList<AskOffering>> bestAskOfferings;
	protected ArrayList<ArrayList<BidOffering>> bestBidOfferings;
	
	protected AskOffering matchingAskOffer;
	protected BidOffering matchingBidOffer;
	
	protected static final DecimalFormat agentHFormat = new DecimalFormat("0.00");
	protected static final DecimalFormat tradingValuesFormat = new DecimalFormat("0.0000");
	
	// NOTE: if a transaction was successful the agents are cloned (see agent.clone) and 
	// stored in this list
	protected List<Agent> finalAgents;
	
	public Transaction(Auction auct) {
		NUMMARKETS = Auction.NUMMARKETS;
		auction = auct;
		bestAskOfferings = new ArrayList<ArrayList<AskOffering>>();
		bestBidOfferings = new ArrayList<ArrayList<BidOffering>>();
		
		for (int i=0; i<NUMMARKETS;i++)  {
			bestAskOfferings.add(new ArrayList<AskOffering>());
			bestBidOfferings.add(new ArrayList<BidOffering>());
		}
	}

	public Offering[] findMatchesByRandomNeighborhood( Agent a, AgentNetwork agents ) {
		// 1. process ask-offerings: need to find a bidder among the neighborhood of the agent
		Agent neighbor = agents.getRandomNeighbor( a );
		
		// agent has no neighbour => can't trade anything => no matches
		if ( null == neighbor ) {
			return null;
		}
	
		// cannot be null, calculated its offers already
		AskOffering[] askOfferings = a.getCurrentAskOfferings();
		BidOffering[] bidOfferings = a.getCurrentBidOfferings();
		
		List<List<AskOffering>> bestAsks = neighbor.getBestAskOfferings();
		List<List<BidOffering>> bestBids = neighbor.getBestBidOfferings();
		
		if ( null == bestAsks ) {
			return null;
		}
		
		return this.matchOffers(askOfferings, bidOfferings, bestAsks, bestBids );
	}
	
	public Offering[] findMatchesByBestNeighborhood( Agent a, AgentNetwork agents ) {
		Iterator<Agent> neighborhood = agents.getNeighbors( a );
		
		AskOffering[] agentAsk = a.getCurrentAskOfferings();
		BidOffering[] agentBid = a.getCurrentBidOfferings();
		
		List<List<AskOffering>> bestAsks = new ArrayList<>();
		List<List<BidOffering>> bestBids = new ArrayList<>();
		
		for ( int i = 0; i < agentAsk.length; ++i ) {
			bestAsks.add( new ArrayList<>() );
			bestBids.add( new ArrayList<>() );
		}

		// NOTE: each market can have multiple bestAsk/bestBids because dominate leads to a single bestAsk/bestBid only in case of Asset->Cash market
		// store offerings over multiple runs, findMatches keeps track of multiple offerings
		// find best ask and bid offers in neighborhood
		while ( neighborhood.hasNext() ) {
			Agent neighbor = neighborhood.next();

			List<List<AskOffering>> neighbourAsk = neighbor.getBestAskOfferings();
			if ( null != neighbourAsk ) {
				for ( int i = 0; i < neighbourAsk.size(); ++i ) {
					Iterator<AskOffering> askIter = neighbourAsk.get( i ).iterator();
					
					while( askIter.hasNext() ) {
						AskOffering ask = askIter.next();
					
						Iterator<AskOffering> bestAsksMarket = bestAsks.get( i ).iterator();
						boolean dominates = true;
						
						while ( bestAsksMarket.hasNext() ) {
							AskOffering askMarket = bestAsksMarket.next();
							
							if ( askMarket.dominates( ask ) ) {
								dominates = false;
								break;
							} else if ( ask.dominates( askMarket ) ){
								bestAsksMarket.remove();
							}
						}
						
						if ( dominates ) {
							bestAsks.get( i ).add( ask );
						}
					}
				}
			}

			List<List<BidOffering>> neighbourBid = neighbor.getBestBidOfferings();
			if ( null != neighbourBid ) {
				for ( int i = 0; i < neighbourBid.size(); ++i ) {
					Iterator<BidOffering> bidIter = neighbourBid.get( i ).iterator();
					
					while( bidIter.hasNext() ) {
						BidOffering bid = bidIter.next();
								
						Iterator<BidOffering> bestBidMarket = bestBids.get( i ).iterator();
						boolean dominates = true;
						
						while ( bestBidMarket.hasNext() ) {
							BidOffering bidMarket = bestBidMarket.next();
							
							if ( bidMarket.dominates( bid ) ) {
								dominates = false;
								break;
							} else if ( bid.dominates( bidMarket ) ){
								bestBidMarket.remove();
							}
						}
						
						if ( dominates ) {
							bestBids.get( i ).add( bid );
						}
					}
				}
			}
		}
		
		return this.matchOffers( agentAsk, agentBid, bestAsks, bestBids );
	}
	
	public Offering[] findMatchesByGlobalOffers( Agent a, AgentNetwork agents ) {
		AskOffering[] askOfferings = a.getCurrentAskOfferings();
		BidOffering[] bidOfferings = a.getCurrentBidOfferings();
		
		List<List<AskOffering>> bestAsks = new ArrayList<>();
		List<List<BidOffering>> bestBids = new ArrayList<>();
		
		for ( int i = 0; i < Auction.NUMMARKETS; ++i ) {
			List<AskOffering> marketAsks = new ArrayList<>();
			List<BidOffering> marketBids = new ArrayList<>();
			
			Iterator<AskOffering> askIter = this.bestAskOfferings.get( i ).iterator();
			while ( askIter.hasNext() ) {
				AskOffering ask = askIter.next();
				
				if ( agents.isNeighbor( a, ask.getAgent() ) ) {
					marketAsks.add( ask );
				}
			}
			
			Iterator<BidOffering> bidIter = this.bestBidOfferings.get( i ).iterator();
			while ( bidIter.hasNext() ) {
				BidOffering bid = bidIter.next();
				
				if ( agents.isNeighbor( a, bid.getAgent() ) ) {
					marketBids.add( bid );
				}
			}
			
			bestAsks.add( marketAsks );
			bestBids.add( marketBids );
		}
		
		// add offerings to the global offer-book
		this.addOfferings( askOfferings, bidOfferings );
		
		return this.matchOffers( askOfferings, bidOfferings, bestAsks, bestBids );
	}
	
	// NOTE: neighborhood must be already checked
	protected Offering[] matchOffers( AskOffering[] askOfferings, BidOffering[] bidOfferings, 
			List<List<AskOffering>> bestAsks, List<List<BidOffering>> bestBids ) {
		
		Offering[] match = new Offering[ Auction.NUMMARKETTYPES * 2 ];
		
		//returns 0: no match;  1: ask offering matched; 2: bid offering matched
		//just for one market (asset against cash) with id "0"
		Iterator<BidOffering> itBestBids;
		Iterator<AskOffering> itBestAsks;
		
		//choose a match in ask or bid offer in random order
		Random myRand = new Random();

		boolean testFirstAsk = (myRand.nextFloat()<0.5);
		
		if (testFirstAsk && askOfferings[0] != null)  {
			itBestBids = bestBids.get( 0 ).iterator();
			//iterate on all best bid offerings searching for a match
			while (itBestBids.hasNext())  {
				BidOffering bestBid = itBestBids.next();
				
				if (askOfferings[0].matches(bestBid))  {
					askOfferings[0].setFinalAssetPrice(bestBid.getAssetPrice());
					bestBid.setFinalAssetPrice(bestBid.getAssetPrice());
					match[0] = askOfferings[0];
					match[1] = bestBid;
					return match;
				}
			}
		} else if (!testFirstAsk && bidOfferings[0] != null)  {
			itBestAsks = bestAsks.get( 0 ).iterator();
			//iterate on all best ask offerings searching for a match
			while (itBestAsks.hasNext())  {
				AskOffering bestAsk = itBestAsks.next();	
				
				if (bidOfferings[0].matches(bestAsk))  {
					bidOfferings[0].setFinalAssetPrice(bestAsk.getAssetPrice());
					bestAsk.setFinalAssetPrice(bestAsk.getAssetPrice());
					match[0] = bestAsk;
					match[1] = bidOfferings[0];
					return match;
				}
			}
		}
		
		return null;
	}
	
	public AskOffering getMatchingAskOffer() {
		return this.matchingAskOffer;
	}
	
	public BidOffering getMatchingBidOffer() {
		return this.matchingBidOffer;
	}
	
	public void matched(Offering[] match)  {
		//askOffer and BidOffer matched for a successful transaction: close transaction with price and amount of the asset
		this.matchingAskOffer = (AskOffering)match[0];
		this.matchingBidOffer = (BidOffering)match[1];
		
		assetPrice = matchingAskOffer.getFinalAssetPrice(); //is equal to bidOffer.assetPrice
		auction.getAsset().updatePrice(assetPrice);
		
		finalAskAssetPrice = matchingAskOffer.getAssetPrice();
		finalBidAssetPrice = matchingBidOffer.getAssetPrice();

		finalAskH = matchingAskOffer.getAgent().getH();
		finalBidH = matchingBidOffer.getAgent().getH();
		assetAmount = Math.min(matchingAskOffer.getAssetAmount(), matchingBidOffer.getAssetAmount());	
	}
	
	public boolean wasSuccessful()  {
		return (assetAmount>0);
	}
	
	public void updateBestAskOfferings( AskOffering offer)   {
		int mkt = offer.getMarket();
		Iterator<AskOffering> askIt = bestAskOfferings.get(mkt).iterator();
		
		// removes all offers which are worse than offer
		while (askIt.hasNext())  {
			AskOffering bestAsk = askIt.next();
			if  (bestAsk.dominates(offer)) {
				return;
			}
			else if (offer.dominates(bestAsk))  {
				askIt.remove();
				while (askIt.hasNext())  
					if (offer.dominates(askIt.next()))  
						askIt.remove();
				bestAskOfferings.get(mkt).add(offer);
				return;
			}
		}
		
		bestAskOfferings.get(mkt).add(offer);
	
		/*
		if ( bestAskOfferings.get(mkt).size() > 1 ) {
			System.out.println( "bestAskOfferings.get( "+ mkt + " ).size() = " + bestAskOfferings.get( mkt ).size() );
		}
		*/
	}
	
	public void updateBestBidOfferings( BidOffering offer)   {
		int mkt = offer.getMarket();
		Iterator<BidOffering> bidIt = bestBidOfferings.get(mkt).iterator();
		
		// removes all offers which are worse than offer
		while (bidIt.hasNext())  {
			BidOffering bestBid = bidIt.next();
			if  (bestBid.dominates(offer)) {
				return;
			}
			else if (offer.dominates(bestBid))  {
				bidIt.remove();
				while (bidIt.hasNext())  
					if (offer.dominates(bidIt.next()))  
						bidIt.remove();
				bestBidOfferings.get(mkt).add(offer);
				return;
			}
		}
		
		bestBidOfferings.get(mkt).add(offer);
		
		/*
		if ( bestBidOfferings.get(mkt).size() > 1 ) {
			System.out.println( "bestBidOfferings.get( "+ mkt + " ).size() = " + bestBidOfferings.get( mkt ).size() );
		}
		*/
	}
	
	public void addOfferings(AskOffering[] askoff, BidOffering[] bidoff)   {
		//side effect: updates best offerings
		for (int i=0;i<NUMMARKETS;i++)  {
			if (askoff[i] != null)  {
				addAskOffering(askoff[i]);
				updateBestAskOfferings(askoff[i]);
			}				
			if (bidoff[i] != null)  {
				addBidOffering(bidoff[i]);
				updateBestBidOfferings(bidoff[i]);
			}				
		}
	}

	@SuppressWarnings("unused")
	public void addAskOffering(AskOffering offer)  {
		int mkt = offer.getMarket();
		meanAskAssetPrice += offer.getAssetPrice();
		meanAskH += offer.getAgent().getH();
	}

	@SuppressWarnings("unused")
	public void addBidOffering(BidOffering offer)  {
		int mkt = offer.getMarket();
		meanBidAssetPrice += offer.getAssetPrice();
		meanBidH += offer.getAgent().getH();
	}
	
	public void removeAllOfferings(Agent agent) {
		for (int mkt=0; mkt<NUMMARKETS;mkt++)  {
			Iterator<BidOffering> bidIt = bestBidOfferings.get(mkt).iterator();
			BidOffering bestBid;
		
			while (bidIt.hasNext())  {
				bestBid = bidIt.next();
				if (bestBid.getAgent() == agent) {
					bidIt.remove();
					break;
				}
			}
			
			Iterator<AskOffering> askIt = bestAskOfferings.get(mkt).iterator();
			AskOffering bestAsk;
		
			while (askIt.hasNext())  {
				bestAsk = askIt.next();
				if (bestAsk.getAgent() == agent) {
					askIt.remove();
					break;
				}
			}
		}		
	}

	public boolean removeAskOfferingsForMarket(Agent agent, int mkt) {
		Iterator<AskOffering> askIt = bestAskOfferings.get(mkt).iterator();
		AskOffering bestAsk;
		boolean result = false;
	
		while (askIt.hasNext())  {
			bestAsk = askIt.next();
			if (bestAsk.getAgent() == agent) {
				askIt.remove();
				result = true;
				break;
			}
		}
		
		return result;	
	}

	public boolean removeBidOfferingsForMarket(Agent agent, int mkt) {
		Iterator<BidOffering> bidIt = bestBidOfferings.get(mkt).iterator();
		BidOffering bestBid;
		boolean result = false;
	
		while (bidIt.hasNext())  {
			bestBid = bidIt.next();
			if (bestBid.getAgent() == agent) {
				bidIt.remove();
				result = true;
				break;
			}
		}
		
		return result;	
	}
	
	public double getLimitPrice() {
		return limitPrice;
	}

	public void setLimitPrice(double limitPrice) {
		this.limitPrice = limitPrice;
	}

	public void addToBestAskOfferings(AskOffering offer)  {
		int mkt = offer.getMarket();
		bestAskOfferings.get(mkt).add(offer);
	}
	
	public void addToBestBidOfferings(BidOffering offer)  {
		int mkt = offer.getMarket();
		bestBidOfferings.get(mkt).add(offer);
	}
	
	public double getAssetPrice() {
		return assetPrice;
	}
	public void setAssetPrice(double assetPrice) {
		this.assetPrice = assetPrice;
	}
	public double getAssetAmount() {
		return assetAmount;
	}
	public void setAssetAmount(double assetAmount) {
		this.assetAmount = assetAmount;
	}
	public double getMeanAskAssetPrice() {
		return meanAskAssetPrice;
	}

	public double getMeanBidAssetPrice() {
		return meanBidAssetPrice;
	}

	public double getMeanAskH() {
		return meanAskH;
	}

	public double getMeanBidH() {
		return meanBidH;
	}

	public int getLength() {
		return length;
	}

	public double getFinalAskAssetPrice() {
		return finalAskAssetPrice;
	}

	public double getFinalBidAssetPrice() {
		return finalBidAssetPrice;
	}

	public double getTotalAskAgentsAssets() {
		return totalAskAgentsAssets;
	}

	public void setTotalAskAgentsAssets(double totalAskAgentsAssets) {
		this.totalAskAgentsAssets = totalAskAgentsAssets;
	}

	public double getTotalBidAgentsCash() {
		return totalBidAgentsCash;
	}

	public void setTotalBidAgentsCash(double totalBidAgentsCash) {
		this.totalBidAgentsCash = totalBidAgentsCash;
	}

	public double getTotalUtility() {
		return totalUtility;
	}

	public void setTotalUtility(double totalUtility) {
		this.totalUtility = totalUtility;
	}

	public double getTotalAskUtility() {
		return totalAskUtility;
	}

	public void setTotalAskUtility(double totalAskUtility) {
		this.totalAskUtility = totalAskUtility;
	}

	public double getTotalBidUtility() {
		return totalBidUtility;
	}

	public void setTotalBidUtility(double totalBidUtility) {
		this.totalBidUtility = totalBidUtility;
	}

	public int getTransNum() {
		return transNum;
	}

	public void setTransNum(int transNum) {
		this.transNum = transNum;
	}

	public int getNUMMARKETS() {
		return NUMMARKETS;
	}
	public int getMarket() {
		return market;
	}
	public void setMarket(int market) {
		this.market = market;
	}
	public double getFinalAskH() {
		return finalAskH;
	}
	public void setFinalAskH(double finalAskH) {
		this.finalAskH = finalAskH;
	}
	public double getFinalBidH() {
		return finalBidH;
	}
	public void setFinalBidH(double finalBidH) {
		this.finalBidH = finalBidH;
	}

	public boolean isReachedEquilibrium() {
		return reachedEquilibrium;
	}

	public void setReachedEquilibrium(boolean reachedEquilibrium) {
		this.reachedEquilibrium = reachedEquilibrium;
	}

	public List<Agent> getFinalAgents() {
		return finalAgents;
	}

	public void setFinalAgents(List<Agent> finalAgents) {
		this.finalAgents = finalAgents;
	}
}
