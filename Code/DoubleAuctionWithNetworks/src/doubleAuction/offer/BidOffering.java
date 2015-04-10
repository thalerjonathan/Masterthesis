package doubleAuction.offer;

import agents.Agent;

public class BidOffering extends Offering {

	// BID-Offer is BUYING (bidding a price for a good) 
	public BidOffering(double assetPrice, double assetAmount, Agent agent, MarketType marketType)  {
		this.assetAmount = assetAmount;
		this.assetPrice = assetPrice;
		this.agent = agent;
		this.marketType = marketType;//0:assets against cash; 1:assets against loans; 2: loans. TODO: replace with enum
	}
	
	public BidOffering(double assetPrice, Agent agent, MarketType marketType)  {
		this( assetPrice, Agent.UNIT, agent, marketType);
	}
	
	public boolean matches(AskOffering offer)  {
		// this buy-offer (bid) matches the sell-offer (ask) when the buyer (this bid-offer) bids more or equal than the seller wants for the good (asset)
		// also trading with one self is restricted by ensuring the offers are not from the same agent
		return (offer.getAssetPrice() <= assetPrice && agent != offer.getAgent());
	}
	
	public boolean dominates(BidOffering offer)  {
		// this buy-offer is better/dominates another buy-offer when it has a lower price than the other buy-offer
		// vice versa: the other offer dominates when it has a lower price than this one
		return (offer.getAssetPrice() <= assetPrice);
	}
}
