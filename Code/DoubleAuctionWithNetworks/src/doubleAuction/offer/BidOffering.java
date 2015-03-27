package doubleAuction.offer;
import doubleAuction.Auction;
import agents.Agent;

public class BidOffering extends Offering {

	// BID-Offer is BUYING (bidding a price for a good) 
	public BidOffering(double assetPrice, double assetAmount, Agent agent, int market, MarketType marketType)  {
		this.assetAmount = assetAmount;
		this.assetPrice = assetPrice;
		this.agent = agent;
		this.market = market; //1+NUMLOANS asset markets (asset for cash and asset for loan type j) + NUMLOANS loan markets
		this.marketType = marketType;//0:assets against cash; 1:assets against loans; 2: loans. TODO: replace with enum
		this.NUMMARKETS = Auction.NUMMARKETS;
	}
	
	public BidOffering(double assetPrice, Agent agent, int market, MarketType marketType)  {
		this( assetPrice, Agent.UNIT, agent, market, marketType);
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
