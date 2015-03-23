package doubleAuction.offer;
import doubleAuction.Auction;
import agents.Agent;

public class BidOffering extends Offering {

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
		return (offer.getAssetPrice() <= assetPrice && agent != offer.getAgent());
	}
	
	public boolean dominates(BidOffering offer)  {
		//this offer is for askers better than offer
		return (offer.getAssetPrice() <= assetPrice);
	}
}
