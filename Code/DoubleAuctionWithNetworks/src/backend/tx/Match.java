package backend.tx;

import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.math3.random.RandomDataGenerator;

import backend.agents.Agent;
import backend.markets.MarketType;
import backend.markets.Markets;
import backend.offers.AskOffering;
import backend.offers.BidOffering;

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

	private final static RandomDataGenerator PERMUTATOR = new RandomDataGenerator();
	
	public enum MatchDirection {
		SELL_MATCHES_BUY,
		BUY_MATCHES_SELL
	}
	
	public static Match matchOffers( AskOffering[] agentAskOfferings, BidOffering[] agentBidOfferings, 
			AskOffering[] bestAsks, BidOffering[] bestBids) {
		
		// generate perumtation of the markets to randomly search for matches
		int[] perm = Match.PERMUTATOR.nextPermutation( Markets.NUMMARKETS, Markets.NUMMARKETS );
		
		// check markets in random order - first match wins
		for ( int i = 0; i < Markets.NUMMARKETS; ++i ) {
			int marketIndex = perm[ i ];
			
			boolean checkAgentSellFirst = ThreadLocalRandom.current().nextDouble() >= 0.5;
			
			// check whether a sell matches or a buy matches in random order - first match wins
			for ( int j = 0; j < 2; ++j ) {
				if ( checkAgentSellFirst ) {
					AskOffering agentSellOffering = agentAskOfferings[ marketIndex ];
					BidOffering bestBuyOffering = bestBids[ marketIndex ];
					
					if ( null != agentSellOffering && null != bestBuyOffering ) {
						if ( agentSellOffering.matches( bestBuyOffering ) ) {
							return new Match( bestBuyOffering, agentSellOffering, MatchDirection.SELL_MATCHES_BUY );
						}
					}
					
					checkAgentSellFirst = false;
					
				} else {
					BidOffering agentBuyOffering = agentBidOfferings[ marketIndex ];
					AskOffering bestSellOffering = bestAsks[ marketIndex ];
		
					if ( null != agentBuyOffering && null != bestSellOffering ) {
						if ( agentBuyOffering.matches( bestSellOffering ) ) {
							return new Match( agentBuyOffering, bestSellOffering, MatchDirection.BUY_MATCHES_SELL );
						}
					}
					
					checkAgentSellFirst = true;
				}
			}
		}
		
		return null;
	}
	
	private Match( BidOffering buyOffer, AskOffering sellOffer, MatchDirection direction ) {
		if ( sellOffer.getMarketType() != buyOffer.getMarketType() ) {
			throw new RuntimeException( "ERROR: attempt of matching a buy- and sell-Offer on different markets!" );
		}
		
		this.sellOffer = sellOffer;
		this.buyOffer = buyOffer;
		this.direction = direction;
		this.market = buyOffer.getMarketType();
		
		/* WARNING: the prices of the equilibrium will differ this price-selection mechanism is used!
		// choosing a matching price depending on whether seller matched buyer or vice versa
		if ( MatchDirection.SELL_MATCHES_BUY == direction ) {
			this.price = this.buyOffer.getPrice();
			
		} else {
			this.price = this.sellOffer.getPrice();
		}
		*/
		
		// NOTE: double-auction in theory matches with halfway-price between buyer and seller
		// WARNING: the prices of the equilibrium will differ if the upper price-selection mechanism is used
		this.price = ( this.buyOffer.getPrice() + this.sellOffer.getPrice() ) / 2.0;
		
		// the amount is always the minimum of the amount offered by the buyer and the seller
		this.amount = Math.min( this.buyOffer.getAmount(), this.sellOffer.getAmount() );
		
		// calculate the normalized price: price is always for 1.0 Unit of trading-good
		// thus to get the according price for the trading amount we need to multiply both
		// to get a "normalized" price
		this.normalizedPrice = this.amount * this.price;
	}
	
	public Agent getBuyer() {
		return this.getBuyOffer().getAgent();
	}
	
	public Agent getSeller() {
		return this.getSellOffer().getAgent();
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
