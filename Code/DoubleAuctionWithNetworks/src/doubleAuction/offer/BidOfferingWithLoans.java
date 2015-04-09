package doubleAuction.offer;

import doubleAuction.Auction;
import agents.Agent;

public class BidOfferingWithLoans extends BidOffering {

	protected double loanPrice;
	protected double finalLoanPrice;
	protected int loanType;
	protected double loanPromise;
	protected double loanAmount;
	
	public BidOfferingWithLoans( double assetPrice, double assetAmount, double loanPrice, 
			                    int loanType, double loanPromise, double loanAmount, Agent agent, int mkt, MarketType marketType) {
		super(assetPrice, assetAmount, agent, mkt, marketType);
		this.loanPrice = loanPrice;
		this.loanType = loanType;
		this.loanPromise = loanPromise;
		this.loanAmount = loanAmount;
		this.NUMMARKETS = Auction.NUMMARKETS;
	}

	public BidOfferingWithLoans( double assetPrice, double loanPrice, 
            					int loanType, double loanPromise, double loanAmount, Agent agent, int mkt, MarketType marketType) {
		super(assetPrice, agent, mkt, marketType);
		this.loanPrice = loanPrice;
		this.loanType = loanType;
		this.loanPromise = loanPromise;
		this.loanAmount = loanAmount;
		this.NUMMARKETS = Auction.NUMMARKETS;
	}

	public double getLoanPrice() {
		return loanPrice;
	}

	public int getLoanType() {
		return loanType;
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
		
		if (Agent.TRADE_ONLY_FULL_UNITS)  {
			//asset against cash
			if (market==0)  {  
				return (offer.getAssetPrice() <= assetPrice) ;
				
			//asset against loan		
			} else if (market>0 && market <= NUMMARKETS) {
						
				if ( ((AskOfferingWithLoans)offer).getLoanType() != loanType )
					System.err.println("error in BidOfferingWithLoan.matches: different asset markets");
				
				return ((offer.getAssetPrice() <= assetPrice) && (((AskOfferingWithLoans)offer).getLoanPrice() >= loanPrice));
				// return ((AgentWithLoans)agent).matchesBid(offer.getAssetPrice(),((AskOfferingWithLoans)offer).getLoanPrice(),this);
				
			//just loan
			} else if (market>NUMMARKETS) {
				if ( ((AskOfferingWithLoans)offer).getLoanType() != loanType )
					System.err.println("error in BidOfferingWithLoan.matches: different loan markets");
				
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
		if (loanType != askOffer.getLoanType()) {
			System.err.println("error in BidOfferingWithLoans.matchesLoan: loan types do not match");
			return false;
		}
		
		return ( (askOffer.getLoanPrice() >= loanPrice) && (askOffer.getLoanAmount() >= loanAmount));
	}
	
	@Override
	public boolean dominates(BidOffering offer)  {
		//this offer is for askers better than offer
		if (Agent.TRADE_ONLY_FULL_UNITS) {
			//asset against cash
			if (market==0)  {  
				return (offer.getAssetPrice() <= assetPrice) ;
				
			// asset against loan
			} else if (market>0 && market <= NUMMARKETS) {		
				if ( ((BidOfferingWithLoans)offer).getLoanType() != loanType )
					System.err.println("error in BidOfferingWithLoan.dominates: different asset markets");
				
				return ((offer.getAssetPrice() <= assetPrice) && (((BidOfferingWithLoans)offer).getLoanPrice() >= loanPrice));
				
			// just loan
			} else if (market>NUMMARKETS) {
				if ( ((BidOfferingWithLoans)offer).getLoanType() != loanType )
					System.err.println("error in BidOfferingWithLoan.dominates: different loan markets");
				
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
