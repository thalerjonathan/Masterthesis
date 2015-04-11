package doubleAuction.offer;
import agents.Agent;

public abstract class Offering {
	
	protected Agent agent;
	protected MarketType marketType;
	
	protected double finalAssetPrice;
	
	protected double price;
	protected double amount;

	public Offering( double price, double amount, Agent agent, MarketType marketType ) {
		this.price = price;
		this.amount = amount;
		this.agent = agent;
		this.marketType = marketType;
	}
	
	public double getPrice() {
		return price;
	}

	public double getAmount() {
		return amount;
	}

	public Agent getAgent() {
		return agent;
	}

	public MarketType getMarketType() {
		return marketType;
	}

	public double getFinalAssetPrice() {
		return finalAssetPrice;
	}

	public void setFinalAssetPrice(double finalAssetPrice) {
		this.finalAssetPrice = finalAssetPrice;
	}
	
	public void setAmount(double a) {
		this.amount = a;
	}
	
	public void setPrice( double p ) {
		this.price = p;
	}
}
