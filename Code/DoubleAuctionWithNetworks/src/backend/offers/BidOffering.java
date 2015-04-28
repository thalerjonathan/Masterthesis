package backend.offers;

import backend.agents.Agent;
import backend.markets.MarketType;

public class BidOffering extends Offering {

	// BID-Offer is BUYING (bidding a price for a good) 
	public BidOffering(double price, double amount, Agent agent, MarketType marketType)  {
		super( price, amount, agent, marketType );
	}

	public boolean matches(AskOffering offer)  {
		// TODO: test market-type of other offer!
		
		// this buy-offer (bid) matches the sell-offer (ask) when the buyer (this bid-offer) bids more or equal than the seller wants for the good (asset)
		// also trading with one self is restricted by ensuring the offers are not from the same agent
		return (offer.getPrice() <= getPrice() && getAgent() != offer.getAgent());
	}
	
	public boolean dominates(BidOffering offer)  {
		// TODO: test market-type of other offer!
		
		// this buy-offer is better/dominates another buy-offer when it has a lower price than the other buy-offer
		// vice versa: the other offer dominates when it has a lower price than this one
		return (offer.getPrice() <= getPrice() );
	}
}
