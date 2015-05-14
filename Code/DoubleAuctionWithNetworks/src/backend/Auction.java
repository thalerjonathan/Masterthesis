package backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.stat.StatUtils;

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
	private double[] lastCollateralPrices;
	private double[] lastSweepCounts;
	private double[] lastAgents;

	private final static int MOVING_AVERAGE_SIZE = 5;
	private final static int MOVING_AVERAGE_LARGE_SIZE = 10;
	
	private final static int MAX_SWEEPS = 500;

	public enum MatchingType {
		BEST_NEIGHBOUR,
		RANDOM_NEIGHOUR;
	}
	
	public Auction( AgentNetwork agentNetwork ) {
		this.agentNetwork = agentNetwork;
		this.tradingAgents = new ArrayList<>( this.agentNetwork.getOrderedList() );
		
		this.numTrans = 1;
		
		this.lastAssetPrices = new double[ Auction.MOVING_AVERAGE_SIZE ];
		this.lastLoanPrices = new double[ Auction.MOVING_AVERAGE_SIZE ];
		this.lastAssetLoanPrices = new double[ Auction.MOVING_AVERAGE_SIZE ];
		this.lastCollateralPrices = new double[ Auction.MOVING_AVERAGE_SIZE ];
		
		this.lastSweepCounts = new double[ Auction.MOVING_AVERAGE_LARGE_SIZE ];
		
		this.lastAgents = new double[ Auction.MOVING_AVERAGE_SIZE ];
	}
	
	public double getSweepCountMean() {
		return StatUtils.mean( this.lastSweepCounts );
	}
	
	public double getSweepCountStd() {
		return Math.sqrt( StatUtils.variance( this.lastSweepCounts ) );
	}
	
	public EquilibriumStatistics calculateEquilibriumStats() {
		int nM = 0;

		EquilibriumStatistics stats = new EquilibriumStatistics();
		stats.assetPrice = StatUtils.mean( this.lastAssetPrices );
		stats.loanPrice = StatUtils.mean( this.lastLoanPrices );
		stats.assetLoanPrice = StatUtils.mean( this.lastAssetLoanPrices );
		stats.collateralPrice = StatUtils.mean( this.lastCollateralPrices );
		stats.i2 = StatUtils.mean( this.lastAgents ); // assumes that the last agents which are trading are those around i2
		
		List<Agent> agents = this.agentNetwork.getOrderedList();
		
		for ( int i = 0; i < agents.size(); ++i ) {
			Agent a = agents.get( i );
			
			// i1 is where the optimists begin: hold more assets than cash and loans
			if ( a.getAssets() > ( Math.abs( a.getLoans() ) + Math.abs( a.getCash() ) ) ) {
				if ( -1 == stats.i1Index) {
					stats.i1Index = i;
				}
				
				stats.optimistWealth += a.getAssets();
				
			// left of i1 are pessimists and medium
			} else {
				// left of i0 are pessimists: have more cash than loan and assets
				if ( a.getCash() > ( Math.abs( a.getLoans() ) + Math.abs( a.getAssets() ) ) ) {
					stats.i0Index = i;
					
					stats.pessimistWealth += a.getCash();
				
				// right of i0 are medium
				} else {
					stats.medianistWealth += a.getLoans();
					nM++;
				}
			}
			
			if ( a.getH() > stats.i2 ) {
				stats.i2Index = i;
			}
		}
		
		// found marginal buyer i0
		// left of i0 are the pessimists
		if( -1 != stats.i0Index ) {
			stats.i0 = agents.get( stats.i0Index ).getH();
			// calculate mean of pessimists wealth
			stats.pessimistWealth /= ( stats.i0Index + 1 );
		}
  
		// found marginal buyer i1
		// right of i1 are the optimists
		// between i0 and i1 are the medium
		if( -1 != stats.i1Index ) {
			stats.i1 = agents.get( stats.i1Index ).getH();
			// calculate mean of optimists wealth
			stats.optimistWealth /= ( agents.size() - stats.i1Index );
		}
  
		// found medium
		if( 0 != nM ) {
			stats.medianistWealth /= nM;
		}
		
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
		
		// WARNING: keeping history sucks up huge amount of memory and CPU if many TXs
		if ( keepAgentHistory ) {
			if ( this.markUntraders() ) {
				tx.setUntraderFound( true );
			}
			finalAgents = this.agentNetwork.cloneAgents();
			
		} else {
			finalAgents = this.agentNetwork.getOrderedList();
		}

		tx.setFinalAgents( finalAgents );
		
		this.lastSweepCounts[ this.numTrans % Auction.MOVING_AVERAGE_LARGE_SIZE ] = tx.getSweepCount();
		
		if ( MarketType.ASSET_CASH == match.getMarket() ) {
			this.lastAssetPrices[ this.numTrans % Auction.MOVING_AVERAGE_SIZE ] = match.getPrice();
			
		} else if ( MarketType.LOAN_CASH == match.getMarket() ) {
			this.lastLoanPrices[ this.numTrans % Auction.MOVING_AVERAGE_SIZE ] = match.getPrice();
			
		} else if ( MarketType.ASSET_LOAN == match.getMarket() ) {
			this.lastAssetLoanPrices[ this.numTrans % Auction.MOVING_AVERAGE_SIZE ] = match.getPrice();
			
		} else if ( MarketType.COLLATERAL_CASH == match.getMarket() ) {
			this.lastCollateralPrices[ this.numTrans % Auction.MOVING_AVERAGE_SIZE ] = match.getPrice();
			
		}
		
		// assumes that the last agents which are trading are those around i2
		this.lastAgents[ this.numTrans % Auction.MOVING_AVERAGE_SIZE ] = 
				( match.getBuyer().getH() + match.getSeller().getH() ) / 2.0;
		
		return true;
	}
	
	private boolean markUntraders() {
		boolean foundUntrader = false;
		
		// check if trading is possible within neighborhood
		Iterator<Agent> agentIter = this.tradingAgents.iterator();

	outer_loop:
		while ( agentIter.hasNext() ) {
			Agent a = agentIter.next();
			
			Iterator<Agent> neighbourIter = this.agentNetwork.getNeighbors( a );
			while ( neighbourIter.hasNext() ) {
				Agent n = neighbourIter.next();
				
				if ( this.canTrade( a, n ) ) {
					a.setCantTrade( false );
					continue outer_loop;
				}
			}
			
			a.setCantTrade( true );
			foundUntrader = true;
		}

		return foundUntrader;
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
