package doubleAuction.tx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.commons.math.random.RandomDataImpl;

import agents.Agent;
import agents.markets.Markets;
import agents.network.AgentNetwork;
import doubleAuction.offer.AskOffering;
import doubleAuction.offer.AskOfferingWithLoans;
import doubleAuction.offer.BidOffering;
import doubleAuction.offer.BidOfferingWithLoans;
import doubleAuction.offer.MarketType;
import doubleAuction.offer.Offering;

public class Transaction  {
	private boolean reachedEquilibrium;
	private double assetPrice;
	private double assetAmount = -1;
	private double finalAskAssetPrice;
	private double finalBidAssetPrice;
	private double limitPrice;
	private double finalAskH;
	private double finalBidH;
	private int transNum;
	
	private Markets markets;
	private ArrayList<ArrayList<AskOffering>> bestAskOfferings;
	private ArrayList<ArrayList<BidOffering>> bestBidOfferings;
	
	private AskOffering matchingAskOffer;
	private BidOffering matchingBidOffer;
	
	// NOTE: if a transaction was successful the agents are cloned (see agent.clone) and 
	// stored in this list
	private List<Agent> finalAgents;
	
	private int sweepCount;
	
	private double loanPrice=0;
	private double loanAmount;
	private double finalAskLoanPrice;
	private double finalBidLoanPrice;
	
	
	public Transaction( Markets markets ) {
		this.markets = markets;
		bestAskOfferings = new ArrayList<ArrayList<AskOffering>>();
		bestBidOfferings = new ArrayList<ArrayList<BidOffering>>();
		
		for (int i=0; i<Markets.NUMMARKETS;i++)  {
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
		
		for ( int i = 0; i < Markets.NUMMARKETS; ++i ) {
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
	
	/*
	// NOTE: neighborhood must be already checked
	private Offering[] matchOffers( AskOffering[] askOfferings, BidOffering[] bidOfferings, 
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
	*/
	
	public Offering[] matchOffers( AskOffering[] askOfferings, BidOffering[] bidOfferings, 
			List<List<AskOffering>> bestAsks, List<List<BidOffering>> bestBids ) {
		
		Offering[] match = new Offering[ Markets.NUMMARKETTYPES * 2 ];
		
		Iterator<BidOffering> itBestBids;
		Iterator<AskOffering> itBestAsks;
		Iterator<AskOffering> itBestAsks1;
		
		//choose a match in ask or bid offer in random order in type and market
		Random myRand = new Random();
		RandomDataImpl rdi = new RandomDataImpl();
		
		int[] perm, altPerm;
		// generate perumtation of the markets to randomly search for matches
		perm = rdi.nextPermutation( Markets.NUMMARKETS, Markets.NUMMARKETS );
		
		for (int i=0; i< Markets.NUMMARKETS; i++) {
	//loop on markets
		  int market = perm[i];
		  boolean testFirstAsk = (myRand.nextFloat()<0.5);
		
		  for (int w=0; w<2;w++) {
			if (testFirstAsk && askOfferings[market] != null) {
			  if ( MarketType.LOAN_CASH != askOfferings[market].getMarketType() ) {
				//search a matching bid transaction in same market, if the ask offer is not a pure loan offer
				itBestBids = bestBids.get(market).iterator();
				
				//iterate on all best bid offerings searching for a match
				while (itBestBids.hasNext())  {
					BidOffering bestBid = itBestBids.next();
					
					if ((askOfferings[market].getAgent() != bestBid.getAgent()) && askOfferings[market].matches(bestBid))  {
						askOfferings[market].setFinalAssetPrice(bestBid.getAssetPrice());
						bestBid.setFinalAssetPrice(bestBid.getAssetPrice());
						if ( MarketType.ASSET_LOAN == askOfferings[market].getMarketType() )  {
							((AskOfferingWithLoans)askOfferings[market]).setFinalLoanPrice(((BidOfferingWithLoans)bestBid).getLoanPrice());
							((BidOfferingWithLoans)bestBid).setFinalLoanPrice(((BidOfferingWithLoans)bestBid).getLoanPrice());
						}						
						//case 1 or 2:
						match[0] = askOfferings[market];
						match[1] = bestBid;
						return match;
					}
				}
				
	//no match found in market mkt: search in alternative markets a matching bid transaction - depending on the market type
				if ( MarketType.ASSET_CASH == askOfferings[market].getMarketType() && this.markets.isLoanMarket() )  {
					//asset against cash - try a match for a bidding agent in market 1			
					int altMkt;
					altPerm = rdi.nextPermutation(Markets.NUMLOANS, Markets.NUMLOANS);
					for (int j=0;j<Markets.NUMLOANS;j++)   {
						altMkt = altPerm[j];
						itBestBids = bestBids.get(altMkt+1).iterator();
						//iterate on all best bid offerings searching for a match
						while (itBestBids.hasNext())  {
							BidOfferingWithLoans bestBid = (BidOfferingWithLoans)itBestBids.next();	
							
							if ((askOfferings[market].getAgent() != bestBid.getAgent()) && askOfferings[market].matches(bestBid))  {
	//partial match: asset prices match - now find a loan for bestBid
								itBestAsks = bestAsks.get(1+Markets.NUMLOANS+altMkt).iterator();
								//iterate on all best ask offerings searching for a match
								while (itBestAsks.hasNext())  {
									AskOfferingWithLoans bestAsk = (AskOfferingWithLoans)itBestAsks.next();			
									if ((bestAsk.getAgent() != bestBid.getAgent()) && (askOfferings[market].getAgent() != bestAsk.getAgent()) 
											&& bestBid.matchesLoan(bestAsk))  {
	//found total match!!
										askOfferings[market].setFinalAssetPrice(bestBid.getAssetPrice());
										bestBid.setFinalAssetPrice(bestBid.getAssetPrice());
										bestBid.setFinalLoanPrice(bestAsk.getLoanPrice());
										bestAsk.setFinalLoanPrice(bestAsk.getLoanPrice());
										//case 3						
										match[0] = askOfferings[market];  //sells an asset against cash to bestBid.agent
										match[1] = bestBid;            //buys an asset against cash and takes a loan from bestAsk.agent
										match[2] = bestAsk;            //sells a loan to bestBid.agent
										return match;
									}
								}
							}
						}
					}
				}
			  }
			  else {
	//pure loan offer: find a bid offer for an asset against loan
				itBestBids = bestBids.get(1).iterator();
				//iterate on all best bid offerings searching for a match
				while (itBestBids.hasNext())  {
					BidOfferingWithLoans bestBid = (BidOfferingWithLoans)itBestBids.next();  //bids for an asset against loan			
					
					if ((askOfferings[market].getAgent() != bestBid.getAgent()) && bestBid.matchesLoan((AskOfferingWithLoans)askOfferings[market]))  {
	//partial match: loan prices match - now find an asset against cash for bestBid					
						itBestAsks = bestAsks.get(0).iterator();
						//iterate on all best ask offerings searching for a match
						while (itBestAsks.hasNext())  {
							AskOffering bestAsk = itBestAsks.next();			
							if ( (bestAsk.getAgent() != bestBid.getAgent()) && (askOfferings[market].getAgent() != bestAsk.getAgent())
									&& bestAsk.matches(bestBid) )  {
	//found total match!!
								bestAsk.setFinalAssetPrice(bestAsk.getAssetPrice());
								bestBid.setFinalAssetPrice(bestAsk.getAssetPrice());
								((AskOfferingWithLoans)askOfferings[market]).setFinalLoanPrice(bestBid.getLoanPrice());
								bestBid.setFinalLoanPrice(bestBid.getLoanPrice());
								//case 3
								match[2] = askOfferings[market];  //sells a loan to bestBid.agent
								match[1] = bestBid;            //buys an asset against cash from loan from bestAsk.agent
								match[0] = bestAsk;            //sells an asset against cash to bestBid.agent
								return match;
							}
						}
					}
				}
			  }
			}
			else if (!testFirstAsk && bidOfferings[market] != null)  {
	//search a matching ask transaction in same market; NOTE: the bid offer cannot be a pure loan offer
				itBestAsks = bestAsks.get(market).iterator();
				//iterate on all best ask offerings searching for a match
				while (itBestAsks.hasNext())  {
					AskOffering bestAsk = itBestAsks.next();

					if ((bidOfferings[market].getAgent() != bestAsk.getAgent()) && bidOfferings[market].matches(bestAsk))  {
						bidOfferings[market].setFinalAssetPrice(bestAsk.getAssetPrice());
						bestAsk.setFinalAssetPrice(bestAsk.getAssetPrice());
						if ( MarketType.ASSET_LOAN == bidOfferings[market].getMarketType() )  {
							((BidOfferingWithLoans)bidOfferings[market]).setFinalLoanPrice(((AskOfferingWithLoans)bestAsk).getLoanPrice());
							((AskOfferingWithLoans)bestAsk).setFinalLoanPrice(((AskOfferingWithLoans)bestAsk).getLoanPrice());
						}						
						//case 1 or 2:
						match[0] = bestAsk;
						match[1] = bidOfferings[market];
						return match;
					}
				}
	//no match found in market mkt: search in alternative markets a matching ask transaction - depending on the market type
				if ( MarketType.ASSET_LOAN == bidOfferings[market].getMarketType() && this.markets.isLoanMarket())  {
	//1. find an askOffer for a loan of type bidOfferings[mkt].loanType			
					itBestAsks = bestAsks.get(2).iterator();
					//iterate on all best ask offerings searching for a match
					while (itBestAsks.hasNext())  {
						AskOfferingWithLoans bestAsk = (AskOfferingWithLoans)itBestAsks.next();			
	
						
						if ((bidOfferings[market].getAgent() != bestAsk.getAgent()) && ((BidOfferingWithLoans)bidOfferings[market]).matchesLoan(bestAsk) )  {
	//partial match: loan prices match - now find an asset against cash for bestBid					
							itBestAsks1 = bestAsks.get(0).iterator();
							//iterate on all best ask offerings searching for a match
							while (itBestAsks1.hasNext())  {
								AskOffering bestAsk1 = itBestAsks1.next();			
								if ( (bidOfferings[market].getAgent() != bestAsk1.getAgent()) && (bestAsk.getAgent() != bestAsk1.getAgent()) 
										&& bestAsk1.matches((BidOfferingWithLoans)bidOfferings[market]) )  {
		//found total match!!
									((BidOfferingWithLoans)bidOfferings[market]).setFinalAssetPrice(bestAsk1.getAssetPrice());
									bestAsk1.setFinalAssetPrice(bestAsk1.getAssetPrice());
									((BidOfferingWithLoans)bidOfferings[market]).setFinalLoanPrice(bestAsk.getLoanPrice());
									bestAsk.setFinalLoanPrice(bestAsk.getLoanPrice());
									// case 3
									match[1] = bidOfferings[market];   // wants to buy an asset against a loan
									match[0] = bestAsk1;            //wants to sell an asset against cash
									match[2] = bestAsk;             //wants to give a loan
									return match;
								}
							}
						}
					}
				}		
			}
			testFirstAsk = !testFirstAsk;
		  }
		}
		
		return null;
	}
	
	public void matched( Offering[] match)  {
		//askOffer and BidOffer matched for a successful transaction: close transaction with price and amount of the asset
		this.matchingAskOffer = (AskOffering)match[0];
		this.matchingBidOffer = (BidOffering)match[1];
		
		AskOfferingWithLoans askOffLoan = (AskOfferingWithLoans)match[2];
		
		assetPrice = this.matchingAskOffer.getFinalAssetPrice();
		this.markets.getAsset().updatePrice(assetPrice);
		
		if ( MarketType.ASSET_LOAN == this.matchingBidOffer.getMarketType() )  {
			loanPrice = ((BidOfferingWithLoans)this.matchingBidOffer).getFinalLoanPrice();
			loanAmount = ((BidOfferingWithLoans)this.matchingBidOffer).getLoanAmount();
			this.markets.getLoans().updatePrice(loanPrice);
		}
		else  {
			loanPrice = 0;
			loanAmount = 0;
		}
			
//		if (loanType > 2)
//			loanType = 2;
		
		assetAmount = this.matchingAskOffer.getAssetAmount();
		
		finalAskAssetPrice = this.matchingAskOffer.getAssetPrice();
		finalBidAssetPrice = this.matchingBidOffer.getAssetPrice();
		finalAskH = this.matchingAskOffer.getAgent().getH();
		finalBidH = this.matchingBidOffer.getAgent().getH();
		if ( MarketType.ASSET_LOAN == this.matchingAskOffer.getMarketType() )  {
			finalAskLoanPrice = ((AskOfferingWithLoans)this.matchingAskOffer).getLoanPrice();
			finalBidLoanPrice = ((BidOfferingWithLoans)this.matchingBidOffer).getLoanPrice();
		}	
		else if (match[2] != null)  {
			finalAskLoanPrice = askOffLoan.getLoanPrice();
			finalBidLoanPrice = ((BidOfferingWithLoans)this.matchingBidOffer).getLoanPrice();
		}
		else  {
			finalAskLoanPrice = 0;
			finalBidLoanPrice = 0;
		}
	}

	public AskOffering getMatchingAskOffer() {
		return this.matchingAskOffer;
	}
	
	public BidOffering getMatchingBidOffer() {
		return this.matchingBidOffer;
	}
	
	/*
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
	*/
	
	public boolean wasSuccessful()  {
		return (assetAmount>0);
	}
	
	public void updateBestAskOfferings( AskOffering offer)   {
		int mkt = offer.getMarketType().ordinal();
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
	}
	
	public void updateBestBidOfferings( BidOffering offer)   {
		int mkt = offer.getMarketType().ordinal();
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
	}
	
	public void addOfferings(AskOffering[] askoff, BidOffering[] bidoff)   {
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

	public void removeAllOfferings(Agent agent) {
		for (int mkt=0; mkt<Markets.NUMMARKETS;mkt++)  {
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
		int mkt = offer.getMarketType().ordinal();
		bestAskOfferings.get(mkt).add(offer);
	}
	
	public void addToBestBidOfferings(BidOffering offer)  {
		int mkt = offer.getMarketType().ordinal();
		bestBidOfferings.get(mkt).add(offer);
	}
	
	public double getAssetPrice() {
		return assetPrice;
	}

	public double getAssetAmount() {
		return assetAmount;
	}

	public double getFinalAskAssetPrice() {
		return finalAskAssetPrice;
	}

	public double getFinalBidAssetPrice() {
		return finalBidAssetPrice;
	}

	public int getTransNum() {
		return transNum;
	}

	public void setTransNum(int transNum) {
		this.transNum = transNum;
	}

	public double getFinalAskH() {
		return finalAskH;
	}
	
	public double getFinalBidH() {
		return finalBidH;
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

	public int getSweepCount() {
		return sweepCount;
	}

	public void setSweepCount(int sweepCount) {
		this.sweepCount = sweepCount;
	}
	
	public double getLoanAmount() {
		return loanAmount;
	}
	
	public double getLoanPrice() {
		return loanPrice;
	}
	
	public double getFinalAskLoanPrice() {
		return finalAskLoanPrice;
	}
	
	public double getFinalBidLoanPrice() {
		return finalBidLoanPrice;
	}
}
