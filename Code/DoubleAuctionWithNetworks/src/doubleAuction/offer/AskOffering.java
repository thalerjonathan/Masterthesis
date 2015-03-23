package doubleAuction.offer;
import doubleAuction.Auction;
import agents.Agent;

public class AskOffering extends Offering {

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
		return (offer.getAssetPrice() >= assetPrice && agent != offer.getAgent());
	}

	public boolean dominates(AskOffering offer)  {
		//this offer is for bidders better than offer
		return (offer.getAssetPrice() >= assetPrice);
	}
	

}
