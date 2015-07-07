package backend.offers;
import backend.agents.Agent;
import backend.markets.MarketType;

public abstract class Offering {
	
	private Agent agent;
	private MarketType marketType;
	
	private double price;
	private double amount;

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
	
	public void setAmount(double a) {
		this.amount = a;
	}
	
	public void setPrice( double p ) {
		this.price = p;
	}
}
