package doubleAuction.offer;

import doubleAuction.AuctionWithLoans;
import agents.Agent;

public class AskOfferingWithLoans extends AskOffering {
//wants either: sell an asset against cash, sell an asset against loan, or give (buy) a loan
	protected double loanPrice;
	protected double finalLoanPrice;
	protected int loanType;
	protected double loanPromise;
	protected double loanAmount;

	
	public AskOfferingWithLoans( double assetPrice, double assetAmount, double loanPrice, 
			                    int loanType, double loanPromise, double loanAmount, Agent agent, int mkt, int marketType ) {
		super(assetPrice, assetAmount, agent, mkt, marketType );
		this.loanPrice = loanPrice;
		this.loanType = loanType;
		this.loanPromise = loanPromise;
		this.loanAmount = loanAmount;
		this.NUMMARKETS = AuctionWithLoans.NUMMARKETS;
	}

	public AskOfferingWithLoans( double assetPrice, double loanPrice, 
								int loanType, double loanPromise, double loanAmount, Agent agent, int mkt, int marketType ) {
		super(assetPrice, agent, mkt, marketType);
		this.loanPrice = loanPrice;
		this.loanType = loanType;
		this.loanPromise = loanPromise;
		this.loanAmount = loanAmount;
		this.NUMMARKETS = AuctionWithLoans.NUMMARKETS;
	}

	public double getLoanPrice() {
		return loanPrice;
	}

	public double getLoanPromise() {
		return loanPromise;
	}

	public int getLoanType() {
		return loanType;
	}

	public double getLoanAmount() {
		return loanAmount;
	}

	@Override	
	public boolean matches(BidOffering offer)  {
	
	    if (agent == offer.getAgent())
	    	return false;
	    
		if (Agent.TRADE_ONLY_FULL_UNITS)  {
			if (market==0)  {  
			//asset against cash
				return (offer.getAssetPrice() >= assetPrice) ;
			} else if (market>0 && market <= NUMMARKETS) {
			//asset against loan				
				if ( ((BidOfferingWithLoans)offer).getLoanType() != loanType )
					System.err.println("error in AskOfferingWithLoan.matches: different asset markets");
				return ((offer.getAssetPrice() >= assetPrice) && (((BidOfferingWithLoans)offer).getLoanPrice() <= loanPrice));
//				return ((AgentWithLoans)agent).matchesAsk(offer.getAssetPrice(),((BidOfferingWithLoans)offer).getLoanPrice(),this);
			} else if (market>NUMMARKETS) {
				//just loan				
					if ( ((BidOfferingWithLoans)offer).getLoanType() != loanType )
						System.err.println("error in AskOfferingWithLoan.matches: different loan markets");
					return ( (((BidOfferingWithLoans)offer).getLoanPrice() <= loanPrice) 
							&& (((BidOfferingWithLoans)offer).getLoanAmount() <= loanAmount));
			}
			
			return false;  //never happens
		} else  {
			System.err.println("Auction with loan only for TRADE_ONLY_FULL_UNITS==true ");
			return false;
		}
	}

	@Override
	public boolean dominates(AskOffering offer)  {
		//this offer is for bidders better than offer
		if (Agent.TRADE_ONLY_FULL_UNITS) {
			if (market==0)  {  
				//asset against cash
					return (offer.getAssetPrice() >= assetPrice) ;
				} else if (market>0 && market <= NUMMARKETS) {
				//asset against loan				
					if ( ((AskOfferingWithLoans)offer).getLoanType() != loanType )
						System.err.println("error in AskOfferingWithLoan.dominates: different asset markets");
					return ((offer.getAssetPrice() >= assetPrice) && (((AskOfferingWithLoans)offer).getLoanPrice() <= loanPrice));
				} else if (market>NUMMARKETS) {
					//just loan				
						if ( ((AskOfferingWithLoans)offer).getLoanType() != loanType )
							System.err.println("error in AskOfferingWithLoan.dominates: different loan markets");
						return ( (((AskOfferingWithLoans)offer).getLoanPrice() <= loanPrice) 
								&& (((AskOfferingWithLoans)offer).getLoanAmount() <= loanAmount));
				}
			
				return false;  //never happens
		} else {
			System.err.println("Auction with loan only for TRADE_ONLY_FULL_UNITS==true ");
			return false;
		}
	}

	public double getFinalLoanPrice() {
		return finalLoanPrice;
	}
	
	public void setFinalLoanPrice(double finalLoanPrice) {
		this.finalLoanPrice = finalLoanPrice;
	}
	
	public void setLoanPrice(double loanPrice) {
		this.loanPrice = loanPrice;
	}
	
	public void setLoanAmount(double loanAmount) {
		this.loanAmount = loanAmount;
	}
}
