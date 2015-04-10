package doubleAuction.offer;

import agents.Agent;

public class AskOffering extends Offering {

	// ASK-Offer is SELLING (asking for a price for a good) 
	public AskOffering(double assetPrice, double assetAmount, Agent agent, MarketType marketType)  {
		this.assetAmount = assetAmount;
		this.assetPrice = assetPrice;
		this.agent = agent;
		this.marketType = marketType;
	}
	
	public AskOffering(double assetPrice, Agent agent, MarketType marketType)  {
		this( assetPrice, Agent.UNIT, agent,  marketType);
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
