package doubleAuction.offer;
import agents.Agent;

public abstract class Offering {
	
	//abstract super class for both ask and bid offerings
	protected int market;
	protected int marketType; //0:assets against cash; 1:assets against loans; 2: loans
	protected int NUMMARKETS;
	protected double assetPrice;
	protected double finalAssetPrice;
	protected double assetAmount;
	protected Agent agent;
	
	public double getAssetPrice() {
		return assetPrice;
	}

	public double getAssetAmount() {
		return assetAmount;
	}

	public Agent getAgent() {
		return agent;
	}

	public int getMarket() {
		return market;
	}

	public void setMarket(int market) {
		this.market = market;
	}

	public int getMarketType() {
		return marketType;
	}

	public void setMarketType(int marketType) {
		this.marketType = marketType;
	}

	public void setAssetPrice(double assetPrice) {
		this.assetPrice = assetPrice;
	}

	public double getFinalAssetPrice() {
		return finalAssetPrice;
	}

	public void setFinalAssetPrice(double finalAssetPrice) {
		this.finalAssetPrice = finalAssetPrice;
	}

	public void setAssetAmount(double assetAmount) {
		this.assetAmount = assetAmount;
	}
}
