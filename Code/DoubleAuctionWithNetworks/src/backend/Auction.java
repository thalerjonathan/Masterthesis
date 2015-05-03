package backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.stat.StatUtils;

import frontend.experimenter.xml.result.EquilibriumBean;
import backend.agents.Agent;
import backend.agents.network.AgentNetwork;
import backend.markets.MarketType;
import backend.markets.Markets;
import backend.tx.Match;
import backend.tx.Transaction;

public class Auction {
	private int numTrans;
	
	private AgentNetwork agentNetwork;
	private List<Agent> tradingAgents;
	
	private double[] lastAssetPrices;
	private double[] lastLoanPrices;
	private double[] lastAssetLoanPrices;
	private double[] lastAgents;

	private final static int LAST_PRICES = 5;
	
	private final static int MAX_SWEEPS = 500;

	public enum MatchingType {
		BEST_NEIGHBOUR,
		RANDOM_NEIGHOUR;
	}

	// used purely as a data-structure
	public static class EquilibriumStatistics {
		public double p;
		public double q;
		public double pq;
		public double i0;
		public double i1;
		public double i2;
		public double P;
		public double M;
		public double O;
		
		public EquilibriumStatistics() {
		}
		
		public EquilibriumStatistics( EquilibriumBean bean ) {
			this.p = bean.getAssetPrice();
			this.q = bean.getLoanPrice();
			this.pq = bean.getAssetLoanPrice();
			this.i0 = bean.getI0();
			this.i1 = bean.getI1();
			this.i2 = bean.getI2();
			this.P = bean.getP();
			this.M = bean.getM();
			this.O = bean.getO();
		}
	}
	
	public Auction( AgentNetwork agentNetwork ) {
		this.agentNetwork = agentNetwork;
		this.tradingAgents = new ArrayList<>( this.agentNetwork.getOrderedList() );
		
		this.numTrans = 1;
		
		this.lastAssetPrices = new double[ Auction.LAST_PRICES ];
		this.lastLoanPrices = new double[ Auction.LAST_PRICES ];
		this.lastAssetLoanPrices = new double[ Auction.LAST_PRICES ];
		this.lastAgents = new double[ Auction.LAST_PRICES ];
	}
	
	public EquilibriumStatistics calculateEquilibriumStats() {
		int nM = 0;
		int i0Index = -1;
		int i1Index = -1;

		EquilibriumStatistics stats = new EquilibriumStatistics();
		stats.p = StatUtils.mean( this.lastAssetPrices );
		stats.q = StatUtils.mean( this.lastLoanPrices );
		stats.pq = StatUtils.mean( this.lastAssetLoanPrices );
		stats.i2 = StatUtils.mean( this.lastAgents ); // assumes that the last agents which are trading are those around i2
		
		List<Agent> agents = this.agentNetwork.getOrderedList();
		
		for ( int i = 0; i < agents.size(); ++i ) {
			Agent a = agents.get( i );
			
			if ( a.getAssetEndow() > ( Math.abs( a.getLoan() ) + Math.abs( a.getConumEndow() ) ) ) {
				if ( i1Index == -1 ) {
					i1Index = i;
				}
				
				stats.O += a.getAssetEndow();
			} else {
				if ( a.getConumEndow() > ( Math.abs( a.getLoan() ) + Math.abs( a.getAssetEndow() ) ) ) {
					i0Index = i;
					
					stats.P += a.getConumEndow();
				} else {
					stats.M += a.getLoan();
					nM++;
				}
			}
		}
		
		  // pessimists
		  if(i0Index!=-1) {
			  stats.i0= agents.get( i0Index ).getH();
			  stats.P/=(i0Index+1);
		  } else {
			  stats.i0=-1;
			  stats.P=-2;
		  }
		  // optimises
		  if(i1Index!=-1) {
			  stats.i1= agents.get( i1Index ).getH();
			  stats.O/=(agents.size()-i1Index);
		  } else {
			  stats.i1=-1;
			  stats.O=-2;
		  }
		  // M
		  if(nM!=0)
			  stats.M/=nM;
		  else
			  stats.M=-2;

		return stats;
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
			transaction.clearGlobalOfferings();
			
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
	
	private boolean findMatch( Transaction tx, MatchingType type, boolean keepAgentHistory ) {
		// get same random-iterator (won't do a shuffle again)
		Iterator<Agent> agIt = this.tradingAgents.iterator();
		
		while (agIt.hasNext())  {
			Agent a = agIt.next();
			Match match = null;
			
			// find match: must be neighbours, must be same market, bid (buy) must be larger than ask (sell)
			
			if ( MatchingType.RANDOM_NEIGHOUR == type ) {
				match = tx.findMatchesByRandomNeighborhood( a, this.agentNetwork );
			
			} else if ( MatchingType.BEST_NEIGHBOUR == type ) {
				if ( this.agentNetwork.isFullyConnected() ) {
					match = tx.findMatchesByGlobalOffers( a, this.agentNetwork );
				} else {
					match = tx.findMatchesByBestNeighborhood( a, this.agentNetwork );
				}
			}

			if ( this.handleMatch( match, tx, keepAgentHistory ) ) {
				return true;
			}
		}
		
		return false;
	}

	private boolean handleMatch( Match match, Transaction tx, boolean keepAgentHistory ) {
		// transaction found a match
		if ( match == null ) {
			return false;
		}
		
		// executes Transaction: sell and buy in the two agents will update new wealth
		// and will lead to a reset of the best offerings as they are invalidated because
		// of change in wealth.
		// won't calculate a new offering, this is only done once in each round
		tx.exec( match );
		tx.setTransNum( this.numTrans++ );
		
		List<Agent> finalAgents = null;
		
		// WARNING: keeping history sucks up huge amount of memory if many TXs
		if ( keepAgentHistory ) {
			finalAgents = this.agentNetwork.cloneAgents();
			
		} else {
			finalAgents = this.agentNetwork.getOrderedList();
		}

		tx.setFinalAgents( finalAgents );
		
		// re-set trading agents because after a match, 
		// agents previously unable to trade could become able to trade again
		//this.tradingAgents = new ArrayList<>( this.agentNetwork.getOrderedList() );
		
		if ( MarketType.ASSET_CASH == match.getMarket() ) {
			this.lastAssetPrices[ this.numTrans % Auction.LAST_PRICES ] = match.getPrice();
			
		} else if ( MarketType.LOAN_CASH == match.getMarket() ) {
			this.lastLoanPrices[ this.numTrans % Auction.LAST_PRICES ] = match.getPrice();
			
		} else if ( MarketType.ASSET_LOAN == match.getMarket() ) {
			this.lastAssetLoanPrices[ this.numTrans % Auction.LAST_PRICES ] = match.getPrice();
			
		}
		
		// assumes that the last agents which are trading are those around i2
		this.lastAgents[ this.numTrans % Auction.LAST_PRICES ] = 
				( match.getBuyer().getH() + match.getSeller().getH() ) / 2.0;
		
		//this.isTradingPossible();
		
		return true;
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
			//agentIter.remove();
			
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
