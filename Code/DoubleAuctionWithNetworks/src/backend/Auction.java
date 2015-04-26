package backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import backend.agents.Agent;
import backend.agents.network.AgentNetwork;
import backend.markets.Markets;
import backend.tx.Match;
import backend.tx.Transaction;

public class Auction {
	private int numTrans;
	
	private AgentNetwork agentNetwork;
	private List<Agent> tradingAgents;
	
	private final static int MAX_SWEEPS = 500;
	
	public enum MatchingType {
		BEST_NEIGHBOUR,
		BEST_GLOBAL_OFFERS,
		RANDOM_NEIGHOUR;
	}

	public Auction( AgentNetwork agentNetwork ) {
		this.agentNetwork = agentNetwork;
		this.tradingAgents = new ArrayList<>( this.agentNetwork.getOrderedList() );
		
		this.numTrans = 1;
	}
	
	public Transaction executeSingleTransaction( MatchingType type, boolean keepAgentHistory )  {
		int sweep = 1;
		Transaction transaction = new Transaction();
		
		// reset all best previous made offers
		Iterator<Agent> agIt = this.tradingAgents.iterator();
		while (agIt.hasNext())  {
			Agent a = agIt.next();
			a.clearBestOfferings();
		}
		
		while ( sweep < Auction.MAX_SWEEPS )  {
			// in each sweep shuffle order of agents
			Collections.shuffle( this.tradingAgents );
			
			transaction.setSweepCount( sweep );

			this.makeOfferings( sweep );
			
			if ( this.findMatch( transaction, type, keepAgentHistory) ) {
				return transaction;
			}
			
			sweep++;
		}
		
		if ( false == this.isTradingPossible() ) {
			transaction.setTradingHalted( true );
		}
		
		return transaction;
	}
	
	private void makeOfferings( int numRound ) {
		Iterator<Agent> agIt = this.tradingAgents.iterator();
		
		// first step: reset offerings of each agent when in first round and then calculate offerings for this round
		while (agIt.hasNext())  {
			Agent a = agIt.next();
			// let current agent calculate its new offers for this round
			a.calcNewOfferings();
			// will add the previously calculated offerings to the agents best offerings
			a.addCurrentOfferingsToBestOfferings();
		}		
	}
	
	private boolean findMatch( Transaction transaction, MatchingType type, boolean keepAgentHistory ) {
		// get same random-iterator (won't do a shuffle again)
		Iterator<Agent> agIt = this.tradingAgents.iterator();
		
		while (agIt.hasNext())  {
			Agent a = agIt.next();
			Match match = null;
			
			// find match: must be neighbours, must be same market, bid (buy) must be larger than ask (sell)
			
			if ( MatchingType.RANDOM_NEIGHOUR == type ) {
				match = transaction.findMatchesByRandomNeighborhood( a, this.agentNetwork );
			
			} else if ( MatchingType.BEST_NEIGHBOUR == type ) {
				match = transaction.findMatchesByBestNeighborhood( a, this.agentNetwork );
				
			} else if ( MatchingType.BEST_GLOBAL_OFFERS == type ) {
				match = transaction.findMatchesByGlobalOffers( a, this.agentNetwork );
			}

			// transaction found a match
			if ( match != null ) {
				// executes Transaction: sell and buy in the two agents will update new wealth
				// and will lead to a reset of the best offerings as they are invalidated because
				// of change in wealth.
				// won't calculate a new offering, this is only done once in each round
				transaction.exec( match );
				transaction.setTransNum( this.numTrans++ );
				
				List<Agent> finalAgents = null;
				
				// WARNING: keeping history sucks up huge amount of memory if many TXs
				if ( keepAgentHistory ) {
					finalAgents = this.agentNetwork.cloneAgents();
					
				} else {
					finalAgents = this.agentNetwork.getOrderedList();
				}

				transaction.setFinalAgents( finalAgents );
				
				// re-set trading agents because after a match, 
				// agents previously unable to trade could become able to trade again
				this.tradingAgents = new ArrayList<>( this.agentNetwork.getOrderedList() );
				
				return true;
			}
		}
		
		return false;
	}

	private boolean isTradingPossible() {
		// check if trading is possible within neighborhood
		Iterator<Agent> agentIter = this.tradingAgents.iterator();
		while ( agentIter.hasNext() ) {
			Agent a = agentIter.next();
			
			Iterator<Agent> neighbourIter = this.agentNetwork.getNeighbors( a );
			while ( neighbourIter.hasNext() ) {
				Agent n = neighbourIter.next();
				
				if ( this.canTrade( a, n ) ) {
					return true;
				}
			}
			
			// agent is not able to trade, remove from the trading-agents which are able to trade
			agentIter.remove();
			
			//System.out.println( "Agent " + a.getH() + " cant trade no more! " + this.tradingAgents.size() + " left." );
		}
		
		return false;
	}
	
	private boolean canTrade( Agent a1, Agent a2 ) {
		for ( int i = 0; i < Markets.NUMMARKETS; ++i ) {
			// asume neighbour has higher H => must be the buyer
			boolean sellerHasOfferings = null != a1.getBestAskOfferings()[ i ];
			boolean buyerHasOfferings = null != a2.getBestBidOfferings()[ i ];
			
			// adjust: neighbour has higher H => must be the buyer
			if ( a1.getH() > a2.getH() ) {
				buyerHasOfferings = null != a1.getBestBidOfferings()[ i ];
				sellerHasOfferings = null != a2.getBestAskOfferings()[ i ];
			}
			
			// if the buyer has still some bid offers AND the seller has still some ask offers, then they might still match
			if ( buyerHasOfferings && sellerHasOfferings ) {
				return true;
			}
		}
		
		return false;
	}
}
