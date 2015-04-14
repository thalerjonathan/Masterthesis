package doubleAuction.tx;

import agents.markets.MarketType;
import doubleAuction.offer.AskOffering;
import doubleAuction.offer.BidOffering;

public class Match {

	private AskOffering sellOffer;
	private BidOffering buyOffer;
	
	// indicates whether seller matches buyer or vice versa
	private MatchDirection direction;
	
	// the price for 1.0 unit of trading-good
	private double price;
	// the amount of the trading-good 
	private double amount;
	// is the price for the given amount: price * amount
	private double normalizedPrice;
	
	// the market-type of this match
	private MarketType market;
	
	public enum MatchDirection {
		SELL_MATCHES_BUY,
		BUY_MATCHES_SELL
	}
	
	public Match( BidOffering buyOffer, AskOffering sellOffer, MatchDirection direction ) {
		if ( sellOffer.getMarketType() != buyOffer.getMarketType() ) {
			throw new RuntimeException( "ERROR: attempt of matching a buy- and sell-Offer on different markets!" );
		}
		
		this.sellOffer = sellOffer;
		this.buyOffer = buyOffer;
		this.direction = direction;
		this.market = buyOffer.getMarketType();
		
		// choosing a matching price depending on whether seller matched buyer or vice versa
		if ( MatchDirection.SELL_MATCHES_BUY == direction ) {
			this.price = this.buyOffer.getPrice();
			
		} else {
			this.price = this.sellOffer.getPrice();
		}
		
		// NOTE: double-auction in theory matches with halfway-price between buyer and seller
		// will just take longer to reach equilibrium but no fundamental difference if 
		// one of the prices of the seller/buyer is chosen
		// this.price = ( this.buyOffer.getPrice() + this.sellOffer.getPrice() ) / 2.0;
		
		// the amount is always the minimum of the amount offered by the buyer and the seller
		this.amount = Math.min( this.buyOffer.getAmount(), this.sellOffer.getAmount() );
		// calculate the normalized price: price is always for 1.0 Unit of trading-good
		// thus to get the according price for the trading amount we need to multiply both
		// to get a "normalized" price
		this.normalizedPrice = this.amount * this.price;
	}

	public AskOffering getSellOffer() {
		return sellOffer;
	}

	public BidOffering getBuyOffer() {
		return buyOffer;
	}

	public MatchDirection getDirection() {
		return direction;
	}

	public double getNormalizedPrice() {
		return this.normalizedPrice;
	}
	
	public double getPrice() {
		return price;
	}

	public double getAmount() {
		return amount;
	}

	public MarketType getMarket() {
		return market;
	}
}
