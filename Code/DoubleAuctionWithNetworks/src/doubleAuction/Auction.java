package doubleAuction;
import java.util.Iterator;
import java.util.List;

import agents.Agent;
import agents.markets.Asset;
import agents.network.AgentNetwork;
import doubleAuction.offer.Offering;
import doubleAuction.tx.Transaction;

public class Auction {
	//constants
	protected int MAXSTEPS;
	protected final static int MAXREPS = 1;
	protected final static int MAXTRANS = 10600;
	protected final static int NUMSTAGES = 1;
	protected final static int MAXZEROTRANSACTIONS = 500;
	protected final static double EQUILIB_PRECISION = 1E-04;
	protected final static int METHOD = 1;  //see detAgentOrderType
    protected final static int LAGSIZELIMITPRICE = 5;  //see detAgentOrderType
	protected final static int TRANSACT_TRACE_PERIOD = 1000;
    protected final static boolean AGENTTRACE = false;
    protected final static boolean RESTRICT_PRICE_SAMPLING = false;
    protected final static boolean WEALTHTRACE = false;
    protected boolean TRANSACTION_TRACE = true;
    protected final static int WealthInterval = 500;

	//attributes
	public static int NUMMARKETS;
	public static int NUMMARKETTYPES;
	protected double pMin, pMax[]; //in current free asset distribution and consume good distribution
	protected double hMin, hMax[]; // the minimum and maximum possible asset price (resp. h) 
	protected int idMin, idMax[]; //the agent ids with " " " " * * * *;
	protected int num_trans, numSuccTrans;  //counts transactions in current auction
	protected int num_trans_trace;
	protected double singleTransTrace[][];
	protected Transaction lastSuccessfulTrans;
	
	protected AgentNetwork agents;
	protected Asset asset;
	
	public Auction( AgentNetwork agents, Asset asset ) {
		this.agents = agents;
		this.asset = asset;
		
		this.num_trans = 1;
	}
	
	protected void initializeTrace()  {
		singleTransTrace = 	new double[MAXTRANS][5];
	}
	
	public void init() {
		initializeMarkets();
		initializeAgents();
	}
	
	protected void initializeMarkets()  {
		//assets
		//initialAssetprice = 0.6;
		
		//1. initialize asset market
		// TODO: Agent.NUM_AGENTS*assetEndow will always be 0 because assetEndow is 0
		//asset = new Asset(initialAssetprice, Agent.NUM_AGENTS*assetEndow);

		//2. set NUMMARKETS: one market - assets for cash
		
		NUMMARKETS = 1;
		NUMMARKETTYPES = 1;
	}

	protected void initializeAgents()  {
		//assetEndow=1;
		//consumEndow=1;
		//Agent.NUM_AGENTS = NUM_AGENTS;
		Agent.TRADE_ONLY_FULL_UNITS = true;
		Agent.NUMMARKETS = NUMMARKETS;

//		agents = new Agent[] {new Agent(1, 0.5, consumEndow, assetEndow, asset), new Agent(2, 0.7, consumEndow, assetEndow, asset), new Agent(3, 0.8, consumEndow, assetEndow, asset)};

		//agents = new Agent[Agent.NUM_AGENTS];
		//randomlyOrderedAgents = new ArrayList<Agent>();

		/*
		for (int i=0;i<Agent.NUM_AGENTS; i++) {
			agents[i] = new  Agent(i,(i+1)*(1.0/Agent.NUM_AGENTS), consumEndow, assetEndow, asset);
			randomlyOrderedAgents.add(agents[i]);
		}
		
		
		this.agents = AgentNetwork.createWithHubs( 3, new IAgentFactory() {
			private int i = 0;
			
			@Override
			public Agent createAgent() {
				Agent a = null;
			
				if ( i < NUM_AGENTS ) {
					a = new Agent(i,(i+1)*(1.0/Agent.NUM_AGENTS), consumEndow, assetEndow, asset);
					i++;
				}
				
				return a;
			}
		});*/
	}

	public enum MatchingType {
		BEST_NEIGHBOUR,
		BEST_GLOBAL_OFFERS,
		RANDOM_NEIGHOUR;
	}

	public Transaction executeSingleTransactionByType( MatchingType type, boolean keepAgentHistory )  {
		int MAX_ROUNDS = 500;
		int numRound = 0;  //number of sweeps through all agents in a random order
		Transaction transaction = getNewTransaction();
		
		while (numRound < MAX_ROUNDS)  {
			numRound++;
			Iterator<Agent> agIt = agents.randomIterator( true );
			
			// first step: reset offerings of each agent when in first round and then calculate offerings for this round
			while (agIt.hasNext())  {
				Agent a = agIt.next();
				
				// need to reset the best offerings when in first round 
				if ( 1 == numRound ) {
					a.clearBestOfferings();
				}
				
				// let current agent calculate its new offers for this round
				a.calcNewOfferings();
				// will add the previously calculated offerings to the agents best offerings
				a.addCurrentOfferingsToBestOfferings();
			}
			
			// get same random-iterator (won't do a shuffle again)
			agIt = agents.randomIterator( false );
			
			while (agIt.hasNext())  {
				Agent a = agIt.next();
				Offering[] match = null;
				
				// find match: must be neighbours, must be same market, bid (buy) must be larger than ask (sell)
				
				if ( MatchingType.RANDOM_NEIGHOUR == type ) {
					match = transaction.findMatchesByRandomNeighborhood( a, agents );
				
				} else if ( MatchingType.BEST_NEIGHBOUR == type ) {
					match = transaction.findMatchesByBestNeighborhood( a, agents );
					
				} else if ( MatchingType.BEST_GLOBAL_OFFERS == type ) {
					match = transaction.findMatchesByGlobalOffers( a, agents );
				}

				// transaction found a match
				if ( match != null ) {
					// executes Transaction: sell and buy in the two agents will update new wealth
					// and will lead to a reset of the best offerings as they are invalidated because
					// of change in wealth.
					// won't calculate a new offering, this is only done once in each round
					if ( a.execTransaction( match, true ) )  {
						transaction.matched( match );
						transaction.setTransNum(num_trans++);
						
						List<Agent> finalAgents = null;
						
						// WARNING: sucks up huge amount of memory if many TXs
						if ( keepAgentHistory ) {
							finalAgents = agents.cloneAgents();
						} else {
							finalAgents = agents.getOrderedList();
						}

						transaction.setFinalAgents( finalAgents );
						
						return transaction;
					}
				}
			}
		}
		
		// check if trading is possible within neighborhood
		Iterator<Agent> agentIter = this.agents.iterator();
		while ( agentIter.hasNext() ) {
			Agent a = agentIter.next();
			
			Iterator<Agent> neighbourIter = this.agents.getNeighbors( a );
			while ( neighbourIter.hasNext() ) {
				Agent n = neighbourIter.next();
				
				if ( this.canTrade( a, n ) ) {
					return transaction;
				}
			}
		}
		
		/*
		// create connections between two agents which can still trade
		boolean foundConnection = false;
		Iterator<Agent> agent1Iter = this.agents.iterator();
		while ( agent1Iter.hasNext() ) {
			Agent a1 = agent1Iter.next();
			
			Iterator<Agent> agent2Iter = this.agents.iterator();
			while ( agent2Iter.hasNext() ) {
				Agent a2 = agent2Iter.next();
				
				// agents must not be already connected and don't allow self-loop (self-trading)
				if ( false == this.agents.isNeighbor( a1, a2 ) && a1 != a2 ) {
					if ( this.canTrade( a1, a2 ) ) {
						this.agents.addConnection( a1, a2 );
						foundConnection = true;
						System.out.println( "Added Connection: " + a1.getH() + " <-> " + a2.getH() );
					}
				}
			}
		}
		
		if ( false == foundConnection ) {
			// NOTE: at this point we couln't add any utility-improving connection: this is the final termination of the simulation!
			transaction.setReachedEquilibrium( true );
		}
		*/
		
		// NOTE: at this point there is no more trading possible within neighborhood
		transaction.setReachedEquilibrium( true );
		
		return transaction;
	}
	
	protected Transaction getNewTransaction() {
		return new Transaction(this);
	}

	public Asset getAsset() {
		return asset;
	}
	
	private boolean canTrade( Agent a1, Agent a2 ) {
		for ( int i = 0; i < NUMMARKETS; ++i ) {
			// asume neighbour has higher H => must be the buyer
			int sellerOfferingsCount = a1.getBestAskOfferings().get( i ).size();
			int buyerOfferingsCount = a2.getBestBidOfferings().get( i ).size();
			
			// adjust: neighbour has higher H => must be the buyer
			if ( a1.getH() > a2.getH() ) {
				buyerOfferingsCount = a1.getBestBidOfferings().get( i ).size();
				sellerOfferingsCount = a2.getBestAskOfferings().get( i ).size();
			}
			
			// if the buyer has still some bid offers AND the seller has still some ask offers, then they might still match
			if ( sellerOfferingsCount > 0 && buyerOfferingsCount > 0  ) {
				return true;
			}
		}
		
		// TODO: if bider has still cash but not enough to match neighbour asker, then its also not possible
		
		return false;
	}
}
