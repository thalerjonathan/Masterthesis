package doubleAuction.offer;
import doubleAuction.Auction;
import agents.Agent;

public class AskOffering extends Offering {

	// ASK-Offer is SELLING (asking for a price for a good) 
	public AskOffering(double assetPrice, double assetAmount, Agent agent, int market, MarketType marketType)  {
		this.assetAmount = assetAmount;
		this.assetPrice = assetPrice;
		this.agent = agent;
		this.market = market;  //1+NUMLOANS asset markets (asset for cash and asset for loan type j) + NUMLOANS loan markets
		this.marketType = marketType;//0:assets against cash; 1:assets against loans; 2: loans
		this.NUMMARKETS = Auction.NUMMARKETS;
	}
	
	public AskOffering(double assetPrice, Agent agent, int market, MarketType marketType)  {
		this( assetPrice, Agent.UNIT, agent,  market, marketType);
	}
	
	public boolean matches(BidOffering offer)  {
		// this sell-offer matches the buy-offer when the buyer bids more or equal than the seller (this ask-offer) wants for the good (asset)
		// also trading with one self is restricted by ensuring the offers are not from the same agent
		return (offer.getAssetPrice() >= assetPrice && agent != offer.getAgent());
	}

	public boolean dominates(AskOffering offer)  {
		// this sell-offer is better/dominates another sell-offer when it has a higher price than the other sell-offer
		// vice versa: the other offer dominates when it has a higher price than this one
		return (offer.getAssetPrice() >= assetPrice);
	}
}
