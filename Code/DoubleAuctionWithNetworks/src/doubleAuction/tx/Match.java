package doubleAuction.tx;

import doubleAuction.offer.AskOffering;
import doubleAuction.offer.BidOffering;
import doubleAuction.offer.MarketType;

public class Match {

	private AskOffering sellOffer;
	private BidOffering buyOffer;
	
	private MatchDirection direction;
	
	private double price;
	private double amount;
	
	private MarketType market;
	
	public enum MatchDirection {
		SELL_MATCHES_BUY,
		BUY_MATCHES_SELL
	}
	
	public Match( BidOffering buyOffer, AskOffering sellOffer, MatchDirection direction ) {
		if ( sellOffer.getMarketType() != buyOffer.getMarketType() ) {
			throw new RuntimeException( "ERROR: attempt of executing a buy- and Sell-Offer on different markets!" );
		}
		
		this.sellOffer = sellOffer;
		this.buyOffer = buyOffer;
		this.direction = direction;
		this.market = buyOffer.getMarketType();
		
		if ( MatchDirection.SELL_MATCHES_BUY == direction ) {
			this.price = this.buyOffer.getPrice();
			
		} else {
			this.price = this.sellOffer.getPrice();
		}
		
		this.amount = Math.min( this.buyOffer.getAmount(), this.sellOffer.getAmount() );
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
