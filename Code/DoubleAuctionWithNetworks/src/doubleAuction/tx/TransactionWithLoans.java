package doubleAuction.tx;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.commons.math.random.RandomDataImpl;

import agents.markets.Loans;
import doubleAuction.Auction;
import doubleAuction.AuctionWithLoans;
import doubleAuction.offer.AskOffering;
import doubleAuction.offer.AskOfferingWithLoans;
import doubleAuction.offer.BidOffering;
import doubleAuction.offer.BidOfferingWithLoans;
import doubleAuction.offer.MarketType;
import doubleAuction.offer.Offering;

public class TransactionWithLoans extends Transaction {
	protected double loanPrice=0;
	protected double loanAmount;
	protected int loanType;
	protected double finalAskLoanPrice;
	protected double finalBidLoanPrice;
	protected double finalBidH;
	protected double finalAskH;
	protected double totalAskAgentsFreeAssets;
	protected double totalBidAgentsFreeAssets;
	protected double[][] totalAskAgentsLoans;
	protected double[][] totalBidAgentsLoans;
	protected int NUMLOANS;
	
	public TransactionWithLoans(AuctionWithLoans auct)  {
		super(auct);
		NUMLOANS = Loans.NUMLOANS;
	}

	@Override
	public Offering[] matchOffers( AskOffering[] askOfferings, BidOffering[] bidOfferings, 
			List<List<AskOffering>> bestAsks, List<List<BidOffering>> bestBids ) {
		
		Offering[] match = new Offering[ Auction.NUMMARKETTYPES * 2 ];
		
		Iterator<BidOffering> itBestBids;
		Iterator<AskOffering> itBestAsks;
		Iterator<AskOffering> itBestAsks1;
		
		//choose a match in ask or bid offer in random order in type and market
		Random myRand = new Random();
		RandomDataImpl rdi = new RandomDataImpl();
		
		int[] perm, altPerm;
		// generate perumtation of the markets to randomly search for matches
		perm = rdi.nextPermutation(NUMMARKETS, NUMMARKETS);
		
		for (int i=0; i<NUMMARKETS; i++) {
	//loop on markets
		  int market = perm[i];
		  boolean testFirstAsk = (myRand.nextFloat()<0.5);
		
		  for (int w=0; w<2;w++) {
			if (testFirstAsk && askOfferings[market] != null) {
			  if ( MarketType.LOAN_AGAINST_CASH != askOfferings[market].getMarketType() ) {
				//search a matching bid transaction in same market, if the ask offer is not a pure loan offer
				itBestBids = bestBids.get(market).iterator();
				
				//iterate on all best bid offerings searching for a match
				while (itBestBids.hasNext())  {
					BidOffering bestBid = itBestBids.next();
					
					if ((askOfferings[market].getAgent() != bestBid.getAgent()) && askOfferings[market].matches(bestBid))  {
						askOfferings[market].setFinalAssetPrice(bestBid.getAssetPrice());
						bestBid.setFinalAssetPrice(bestBid.getAssetPrice());
						if ( MarketType.ASSET_AGAINST_LOAN == askOfferings[market].getMarketType() )  {
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
				if ( MarketType.ASSET_AGAINST_CASH == askOfferings[market].getMarketType() && Loans.LOANMARKET)  {
					//asset against cash - try a match for a bidding agent in market 1			
					int altMkt;
					altPerm = rdi.nextPermutation(NUMLOANS, NUMLOANS);
					for (int j=0;j<NUMLOANS;j++)   {
						altMkt = altPerm[j];
						itBestBids = bestBids.get(altMkt+1).iterator();
						//iterate on all best bid offerings searching for a match
						while (itBestBids.hasNext())  {
							BidOfferingWithLoans bestBid = (BidOfferingWithLoans)itBestBids.next();	
							
							if ((askOfferings[market].getAgent() != bestBid.getAgent()) && askOfferings[market].matches(bestBid))  {
	//partial match: asset prices match - now find a loan for bestBid
								itBestAsks = bestAsks.get(1+NUMLOANS+altMkt).iterator();
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
				itBestBids = bestBids.get(1+((AskOfferingWithLoans)askOfferings[market]).getLoanType()).iterator();
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
						if ( MarketType.ASSET_AGAINST_LOAN == bidOfferings[market].getMarketType() )  {
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
				if ( MarketType.ASSET_AGAINST_LOAN == bidOfferings[market].getMarketType() && Loans.LOANMARKET)  {
	//1. find an askOffer for a loan of type bidOfferings[mkt].loanType			
					itBestAsks = bestAsks.get(1+NUMLOANS+((BidOfferingWithLoans)bidOfferings[market]).getLoanType()).iterator();
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
	
	@Override
	public void matched( Offering[] match)  {
		//askOffer and BidOffer matched for a successful transaction: close transaction with price and amount of the asset
		this.matchingAskOffer = (AskOffering)match[0];
		this.matchingBidOffer = (BidOffering)match[1];
		
		AskOfferingWithLoans askOffLoan = (AskOfferingWithLoans)match[2];
		
		assetPrice = this.matchingAskOffer.getFinalAssetPrice();
		auction.getAsset().updatePrice(assetPrice);

		if ( MarketType.ASSET_AGAINST_LOAN == this.matchingBidOffer.getMarketType() )  {
			loanPrice = ((BidOfferingWithLoans)this.matchingBidOffer).getFinalLoanPrice();
			loanAmount = ((BidOfferingWithLoans)this.matchingBidOffer).getLoanAmount();
			loanType = ((BidOfferingWithLoans)this.matchingBidOffer).getLoanType();
			((AuctionWithLoans)auction).getLoanMarket().updatePrice(loanPrice,loanType);
		}
		else  {
			loanPrice = 0;
			loanAmount = 0;
			loanType = -1;
		}
			
//		if (loanType > 2)
//			loanType = 2;
		
		assetAmount = this.matchingAskOffer.getAssetAmount();
		
		finalAskAssetPrice = this.matchingAskOffer.getAssetPrice();
		finalBidAssetPrice = this.matchingBidOffer.getAssetPrice();
		finalAskH = this.matchingAskOffer.getAgent().getH();
		finalBidH = this.matchingBidOffer.getAgent().getH();
		if ( MarketType.ASSET_AGAINST_LOAN == this.matchingAskOffer.getMarketType() )  {
			finalAskLoanPrice = ((AskOfferingWithLoans)this.matchingAskOffer).getLoanPrice();
			finalBidLoanPrice = ((BidOfferingWithLoans)this.matchingBidOffer).getLoanPrice();
			market = ((BidOfferingWithLoans)this.matchingBidOffer).getMarket();
		}	
		else if (match[2] != null)  {
			finalAskLoanPrice = askOffLoan.getLoanPrice();
			finalBidLoanPrice = ((BidOfferingWithLoans)this.matchingBidOffer).getLoanPrice();
			market = askOffLoan.getMarket();
		}
		else  {
			market = 0;
			finalAskLoanPrice = 0;
			finalBidLoanPrice = 0;
		}
	}

	public double getLoanPrice() {
		return loanPrice;
	}

	public void setLoanPrice(double loanPrice) {
		this.loanPrice = loanPrice;
	}

	public double getLoanAmount() {
		return loanAmount;
	}

	public void setLoanAmount(double loanAmount) {
		this.loanAmount = loanAmount;
	}

	public int getLoanType() {
		return loanType;
	}

	public void setLoanType(int loanType) {
		this.loanType = loanType;
	}

	public double getFinalAskLoanPrice() {
		return finalAskLoanPrice;
	}

	public void setFinalAskLoanPrice(double finalAskLoanPrice) {
		this.finalAskLoanPrice = finalAskLoanPrice;
	}

	public double getFinalBidLoanPrice() {
		return finalBidLoanPrice;
	}

	public void setFinalBidLoanPrice(double finalBidLoanPrice) {
		this.finalBidLoanPrice = finalBidLoanPrice;
	}

	public double getTotalAskAgentsFreeAssets() {
		return totalAskAgentsFreeAssets;
	}

	public void setTotalAskAgentsFreeAssets(double totalAskAgentsFreeAssets) {
		this.totalAskAgentsFreeAssets = totalAskAgentsFreeAssets;
	}

	public double getTotalBidAgentsFreeAssets() {
		return totalBidAgentsFreeAssets;
	}

	public void setTotalBidAgentsFreeAssets(double totalBidAgentsFreeAssets) {
		this.totalBidAgentsFreeAssets = totalBidAgentsFreeAssets;
	}

	public double[][] getTotalAskAgentsLoans() {
		return totalAskAgentsLoans;
	}

	public void setTotalAskAgentsLoans(double[][] totalAskAgentsLoans) {
		this.totalAskAgentsLoans = totalAskAgentsLoans;
	}

	public double[][] getTotalBidAgentsLoans() {
		return totalBidAgentsLoans;
	}

	public void setTotalBidAgentsLoans(double[][] totalBidAgentsLoans) {
		this.totalBidAgentsLoans = totalBidAgentsLoans;
	}

	public double getFinalBidH() {
		return finalBidH;
	}

	public void setFinalBidH(double finalBidH) {
		this.finalBidH = finalBidH;
	}

	public double getFinalAskH() {
		return finalAskH;
	}

	public void setFinalAskH(double finalAskH) {
		this.finalAskH = finalAskH;
	}
}
