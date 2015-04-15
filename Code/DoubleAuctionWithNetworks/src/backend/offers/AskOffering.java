package backend.offers;

import backend.agents.Agent;
import backend.markets.MarketType;

public class AskOffering extends Offering {

	// ASK-Offer is SELLING (asking for a price for a good) 
	public AskOffering(double price, double amount, Agent agent, MarketType marketType)  {
		super( price, amount, agent, marketType );
	}
	
	public boolean matches(BidOffering offer)  {
		// TODO: test market-type of other offer!
		
		// this sell-offer matches the buy-offer when the buyer bids more or equal than the seller (this ask-offer) wants for the good (asset)
		// also trading with one self is restricted by ensuring the offers are not from the same agent
		return (offer.getPrice() >= price && agent != offer.getAgent());
	}

	public boolean dominates(AskOffering offer)  {
		// TODO: test market-type of other offer!
		
		// this sell-offer is better/dominates another sell-offer when it has a higher price than the other sell-offer
		// vice versa: the other offer dominates when it has a higher price than this one
		return (offer.getPrice() >= price);
	}
}
