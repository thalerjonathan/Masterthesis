package doubleAuction.offer;

import agents.Agent;
import agents.markets.Markets;

public class BidOfferingWithLoans extends BidOffering {

	protected double loanPrice;
	protected double finalLoanPrice;
	protected double loanPromise;
	protected double loanAmount;
	
	public BidOfferingWithLoans( double assetPrice, double assetAmount, double loanPrice, 
			                    double loanPromise, double loanAmount, Agent agent, MarketType marketType) {
		super(assetPrice, assetAmount, agent, marketType);
		this.loanPrice = loanPrice;
		this.loanPromise = loanPromise;
		this.loanAmount = loanAmount;
	}

	public double getLoanPrice() {
		return loanPrice;
	}

	public double getLoanPromise() {
		return loanPromise;
	}
	
	public double getLoanAmount() {
		return loanAmount;
	}
	
	@Override
	public boolean matches(AskOffering offer)  {
		// avoid trading with one self
		if  (agent == offer.getAgent())
			return false;
		
		// TODO: test market-type of other offer!
		
		if (Markets.TRADE_ONLY_FULL_UNITS)  {
			//asset against cash
			if ( MarketType.ASSET_CASH == this.marketType ) {
				return (offer.getPrice() <= price) ;
				
			//asset against loan		
			} else if ( MarketType.ASSET_LOAN == this.marketType ) {
				return ((offer.getPrice() <= price) && (((AskOfferingWithLoans)offer).getLoanPrice() >= loanPrice));
				// return ((AgentWithLoans)agent).matchesBid(offer.getAssetPrice(),((AskOfferingWithLoans)offer).getLoanPrice(),this);
				
			//just loan
			} else if ( MarketType.LOAN_CASH == this.marketType ) {
				return ( (((AskOfferingWithLoans)offer).getLoanPrice() >= loanPrice) 
						&& (((AskOfferingWithLoans)offer).getLoanAmount() >= loanAmount));
			}
		
			return false;  //never happens
			
		} else  {
			System.err.println("Auction with loan only for TRADE_ONLY_FULL_UNITS==true ");
			return false;
		}
	}
	
	public boolean matchesLoan(AskOfferingWithLoans askOffer)   {
		return ( (askOffer.getLoanPrice() >= loanPrice) && (askOffer.getLoanAmount() >= loanAmount));
	}
	
	@Override
	public boolean dominates(BidOffering offer)  {
		// TODO: test market-type of other offer!
		
		
		//this offer is for askers better than offer
		if (Markets.TRADE_ONLY_FULL_UNITS) {
			//asset against cash
			if ( MarketType.ASSET_CASH == this.marketType ) {
				return (offer.getPrice() <= price) ;
				
			// asset against loan
			} else if ( MarketType.ASSET_LOAN == this.marketType ) {
				return ((offer.getPrice() <= price) && (((BidOfferingWithLoans)offer).getLoanPrice() >= loanPrice));
				
			// just loan
			} else if ( MarketType.LOAN_CASH == this.marketType ) {
				return ( (((BidOfferingWithLoans)offer).getLoanPrice() >= loanPrice) 
						&& (((BidOfferingWithLoans)offer).getLoanAmount() >= loanAmount));
			}
			
			return false;  //never happens
		} else  {
			System.err.println("Auction with loan only for TRADE_ONLY_FULL_UNITS==true ");
			return false;
		}		
	}

	public void setLoanAmount(double loanAmount) {
		this.loanAmount = loanAmount;
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
}
