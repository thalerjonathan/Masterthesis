package doubleAuction.tx;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.math.random.RandomDataImpl;

import agents.Agent;
import agents.markets.Markets;
import agents.network.AgentNetwork;
import doubleAuction.offer.AskOffering;
import doubleAuction.offer.BidOffering;
import doubleAuction.tx.Match.MatchDirection;

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
	private AskOffering[] bestGlobalAskOfferings;
	private BidOffering[] bestGlobalBidOfferings;
	
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
	
	private final static RandomDataImpl PERMUTATOR = new RandomDataImpl();
	
	public Transaction( Markets markets ) {
		this.markets = markets;
		bestGlobalAskOfferings = new AskOffering[ Markets.NUMMARKETS ];
		bestGlobalBidOfferings = new BidOffering[ Markets.NUMMARKETS ];
	}

	public void exec( Match match ) {
		// buy-offer always at index 0, sell-offer always at index 1
		BidOffering buyOffer = match.getBuyOffer();
		AskOffering sellOffer = match.getSellOffer();
		
		Agent buyer = buyOffer.getAgent();
		Agent seller = sellOffer.getAgent();
		
		buyer.execBuyTransaction( match );
		seller.execSellTransaction( match );
		
		this.matched( match );
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
		
		return this.matchOffers(askOfferings, bidOfferings, bestAsks, bestBids );
	}
	
	public Match findMatchesByBestNeighborhood( Agent a, AgentNetwork agents ) {
		Iterator<Agent> neighborhood = agents.getNeighbors( a );
		
		AskOffering[] agentAsk = a.getCurrentAskOfferings();
		BidOffering[] agentBid = a.getCurrentBidOfferings();
		
		AskOffering[] bestAsks = new AskOffering[ Markets.NUMMARKETS ];
		BidOffering[] bestBids = new BidOffering[ Markets.NUMMARKETS ];

		// NOTE: each market can have multiple bestAsk/bestBids because dominate leads to a single bestAsk/bestBid only in case of Asset->Cash market
		// store offerings over multiple runs, findMatches keeps track of multiple offerings
		// find best ask and bid offers in neighborhood
		while ( neighborhood.hasNext() ) {
			Agent neighbor = neighborhood.next();

			AskOffering[] neighbourAsk = neighbor.getBestAskOfferings();
			for ( int i = 0; i < Markets.NUMMARKETS; ++i ) {
				AskOffering ask = neighbourAsk[ i ];
				AskOffering bestAsk = bestAsks[ i ];
						
				if ( null != ask ) {
					if ( bestAsk == null || ask.dominates( bestAsk ) ) {
						bestAsks[ i ] = ask;
					}
				}
			}

			BidOffering[] neighbourBid = neighbor.getBestBidOfferings();
			for ( int i = 0; i < Markets.NUMMARKETS; ++i ) {
				BidOffering bid = neighbourBid[ i ];
				BidOffering bestBid = bestBids[ i ];
						
				if ( null != bid ) {
					if ( bestBid == null || bid.dominates( bestBid ) ) {
						bestBids[ i ] = bid;
					}
				}
			}
		}
		
		return this.matchOffers( agentAsk, agentBid, bestAsks, bestBids );
	}
	
	public Match findMatchesByGlobalOffers( Agent a, AgentNetwork agents ) {
		AskOffering[] askOfferings = a.getCurrentAskOfferings();
		BidOffering[] bidOfferings = a.getCurrentBidOfferings();
		
		AskOffering[] bestAsks = new AskOffering[ Markets.NUMMARKETS ];
		BidOffering[] bestBids = new BidOffering[ Markets.NUMMARKETS ];

		for ( int i = 0; i < Markets.NUMMARKETS; ++i ) {
			AskOffering ask = this.bestGlobalAskOfferings[ i ];
			BidOffering bid = this.bestGlobalBidOfferings[ i ];
			
			if ( null != ask ) {
				if ( agents.isNeighbor( a, ask.getAgent() ) ) {
					bestAsks[ i ] = ask;
				}
			}

			if ( null != bid ) {
				if ( agents.isNeighbor( a, bid.getAgent() ) ) {
					bestBids[ i ] = bid;
				}
			}
		}
		
		// add offerings to the global offer-book
		this.addOfferings( askOfferings, bidOfferings );
		
		return this.matchOffers( askOfferings, bidOfferings, bestAsks, bestBids );
	}
	
	private Match matchOffers( AskOffering[] agentAskOfferings, BidOffering[] agentBidOfferings, 
			AskOffering[] bestAsks, BidOffering[] bestBids) {
		
		// generate perumtation of the markets to randomly search for matches
		int[] perm = Transaction.PERMUTATOR.nextPermutation( Markets.NUMMARKETS, Markets.NUMMARKETS );
		
		// check markets in random order - first match wins
		for ( int i = 0; i < Markets.NUMMARKETS; ++i ) {
			int marketIndex = perm[ i ];
			
			boolean checkAgentSellFirst = Math.random() >= 0.5;
			
			// check whether a sell matches or a buy matches in random order - first match wins
			for ( int j = 0; j < 2; ++j ) {
				if ( checkAgentSellFirst ) {
					AskOffering agentSellOffering = agentAskOfferings[ marketIndex ];
					BidOffering bestBuyOffering = bestBids[ marketIndex ];
					
					if ( null != agentSellOffering && null != bestBuyOffering ) {
						if ( agentSellOffering.matches( bestBuyOffering ) ) {
							return new Match( bestBuyOffering, agentSellOffering, MatchDirection.SELL_MATCHES_BUY );
						}
					}
					
					checkAgentSellFirst = false;
					
				} else {
					BidOffering agentBuyOffering = agentBidOfferings[ marketIndex ];
					AskOffering bestSellOffering = bestAsks[ marketIndex ];
		
					if ( null != agentBuyOffering && null != bestSellOffering ) {
						if ( agentBuyOffering.matches( bestSellOffering ) ) {
							return new Match( agentBuyOffering, bestSellOffering, MatchDirection.BUY_MATCHES_SELL );
						}
					}
					
					checkAgentSellFirst = true;
				}
			}
		}
		
		return null;
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
	
	/*
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
						askOfferings[market].setFinalAssetPrice(bestBid.getPrice());
						bestBid.setFinalAssetPrice(bestBid.getPrice());
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
										askOfferings[market].setFinalAssetPrice(bestBid.getPrice());
										bestBid.setFinalAssetPrice(bestBid.getPrice());
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
								bestAsk.setFinalAssetPrice(bestAsk.getPrice());
								bestBid.setFinalAssetPrice(bestAsk.getPrice());
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
						bidOfferings[market].setFinalAssetPrice(bestAsk.getPrice());
						bestAsk.setFinalAssetPrice(bestAsk.getPrice());
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
									((BidOfferingWithLoans)bidOfferings[market]).setFinalAssetPrice(bestAsk1.getPrice());
									bestAsk1.setFinalAssetPrice(bestAsk1.getPrice());
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
	*/
	
	public void matched( Match match )  {
		//askOffer and BidOffer matched for a successful transaction: close transaction with price and amount of the asset
		this.matchingBidOffer = match.getBuyOffer();
		this.matchingAskOffer = match.getSellOffer();
		
		assetPrice = this.matchingAskOffer.getFinalAssetPrice();
		this.markets.getAsset().updatePrice(assetPrice);
		
		/*
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
		*/
		assetAmount = this.matchingAskOffer.getAmount();
		
		finalAskAssetPrice = this.matchingAskOffer.getPrice();
		finalBidAssetPrice = this.matchingBidOffer.getPrice();
		finalAskH = this.matchingAskOffer.getAgent().getH();
		finalBidH = this.matchingBidOffer.getAgent().getH();
		/*
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
		*/
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
	
	public void updateBestAskOfferings( AskOffering offer )   {
		int mkt = offer.getMarketType().ordinal();
		AskOffering bestAsk = bestGlobalAskOfferings[ mkt ];
		
		if ( null == bestAsk || offer.dominates( bestAsk ) ) {
			bestGlobalAskOfferings[ mkt ] = offer;
		}
	}
	
	public void updateBestBidOfferings( BidOffering offer )   {
		int mkt = offer.getMarketType().ordinal();
		BidOffering bestBid = bestGlobalBidOfferings[ mkt ];
		
		if ( null == bestBid || offer.dominates( bestBid ) ) {
			bestGlobalBidOfferings[ mkt ] = bestBid;
		}
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
		for (int i=0; i<Markets.NUMMARKETS; ++i )  {
			bestGlobalBidOfferings[ i ] = null;
			bestGlobalAskOfferings[ i ] = null;
		}		
	}

	public boolean removeAskOfferingsForMarket( Agent agent, int mkt ) {
		for (int i=0; i<Markets.NUMMARKETS; ++i )  {
			if ( null != bestGlobalAskOfferings[ i ] ) {
				if ( bestGlobalAskOfferings[ i ].getAgent() == agent ) {
					bestGlobalAskOfferings[ i ] = null;
					return true;
				}
			}
		}	
		
		return false;
	}

	public boolean removeBidOfferingsForMarket(Agent agent, int mkt) {
		for (int i=0; i<Markets.NUMMARKETS; ++i )  {
			if ( null != bestGlobalBidOfferings[ i ] ) {
				if ( bestGlobalBidOfferings[ i ].getAgent() == agent ) {
					bestGlobalBidOfferings[ i ] = null;
					return true;
				}
			}
		}	
		
		return false;
	}
	
	public double getLimitPrice() {
		return limitPrice;
	}

	public void setLimitPrice(double limitPrice) {
		this.limitPrice = limitPrice;
	}

	/*
	public void addToBestAskOfferings(AskOffering offer)  {
		int mkt = offer.getMarketType().ordinal();
		bestGlobalAskOfferings.get(mkt).add(offer);
	}
	
	public void addToBestBidOfferings(BidOffering offer)  {
		int mkt = offer.getMarketType().ordinal();
		bestGlobalBidOfferings.get(mkt).add(offer);
	}
	*/
	
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
