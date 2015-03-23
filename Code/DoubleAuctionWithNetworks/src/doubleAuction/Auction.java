package doubleAuction;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.math.stat.StatUtils;

import agents.Agent;
import agents.markets.Asset;
import agents.network.AgentNetwork;
import doubleAuction.offer.AskOffering;
import doubleAuction.offer.BidOffering;
import doubleAuction.offer.Offering;
import doubleAuction.stats.SingleAuctionStats;
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
	protected int rep;
	protected int zeroTrans;
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
	
	@SuppressWarnings("unused")
	public void simulate()  {
		//main method
//		ArrayList<ArrayList<Transaction>> statistics = new ArrayList<ArrayList<Transaction>>();
		ArrayList<Transaction> transactionTrace = new ArrayList<Transaction>();
		ArrayList<ArrayList<SingleAuctionStats>> statistics = new ArrayList<ArrayList<SingleAuctionStats>>();
		ArrayList<SingleAuctionStats> statisticsTrace = new ArrayList<SingleAuctionStats>();
		PrintWriter outTrace1=null, outTrace2=null, outTrace3 = null;					

		if (AGENTTRACE)  {
			try {
	  				outTrace1 = new PrintWriter(
	  						new FileWriter( "logs\\singleAgent03Wealth.txt") );
	  				outTrace2 = new PrintWriter(
	  	  					new FileWriter( "logs\\singleAgent07Wealth.txt") );
	  				outTrace3 = new PrintWriter(
	  	  					new FileWriter( "logs\\singleAgent09Wealth.txt") );
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		while (!enoughReplications(statistics) ) {
			//outer loop: repeat iid replications of the stochastic process
			//1. initialize markets and agents
			rep++;
			initializeMarkets();
			initializeAgents();
			initializeTrace();
			System.out.println("Replication: " + rep);
			
			for (int stage = 0; stage < NUMSTAGES; stage++) {
				//loop on stages	
				Transaction transaction;
				int maxTransactionLength = 0;
				
				zeroTrans = 0;  //counts failed transactions in sequence
				num_trans = 0;  //counts transactions
				numSuccTrans = 0;  //counts successful transactions
				num_trans_trace = 0;  //counts successful trace transactions
				lastSuccessfulTrans = null;
				do   {
					//loop on transactions		
					transaction = this.executeSingleTransaction();
					num_trans++;
//					if (num_trans == 20000)
//						num_trans++;
					if (transaction.wasSuccessful())  {
						numSuccTrans++;
						//System.out.println(num_trans + " , " + numSuccTrans);
					}
					
					if (transaction.getLength() > maxTransactionLength)
						maxTransactionLength = transaction.getLength();
					
					calculateSingleTransactionStatistics(transaction);
					if (transaction.wasSuccessful() && AGENTTRACE)  {
							Agent agent = agents.get(Math.round((float)0.3*agents.size())-1);
							outTrace1.println(agent.getH() + "," + agent.getCE() + "," + agent.getAE());
							agent = agents.get(Math.round((float)0.7*agents.size())-1);
							outTrace2.println(agent.getH() + "," + agent.getCE() + "," + agent.getAE());
							agent = agents.get(Math.round((float)0.9*agents.size())-1);
							outTrace3.println(agent.getH() + "," + agent.getCE() + "," + agent.getAE());
					}
					
					keepTransactionTrace(transaction);
					
				}  while (!auctionFinished(transactionTrace,transaction) );  //side effect: adds transaction to the trace

				SingleAuctionStats stats = calculateAndExportStatistics(transactionTrace, rep);
				statisticsTrace.add(stats);
				transactionTrace = new ArrayList<Transaction>();
				
				//execute events that agents hold beliefs on:
				executeEvents();
				//update h of each agent:
				resetAgentBeliefs();
			}

			if (statisticsTrace.get(0) != null)
				statistics.add(statisticsTrace);
			
			statisticsTrace = new ArrayList<SingleAuctionStats>();
		}
		
		calculateAndExportRepStatistics(statistics);
	}

	protected void keepTransactionTrace(Transaction transaction) {
		if (transaction.wasSuccessful()) {
			num_trans_trace++;
			singleTransTrace[num_trans_trace][0] = transaction.getAssetPrice();
			singleTransTrace[num_trans_trace][1] = transaction.getFinalAskH();
			singleTransTrace[num_trans_trace][2] = transaction.getFinalBidH();
//  				singleTransTrace[num_trans_trace][3+4*i] = transaction.getFinalAskH();
//  			    singleTransTrace[num_trans_trace][4+4*i] = transaction.getFinalBidH();
  			    singleTransTrace[num_trans_trace][3] = transaction.getTotalUtility();
  			    singleTransTrace[num_trans_trace][4] = transaction.getTransNum();
		}
	}
	
	@SuppressWarnings("unused")
	protected void calculateSingleTransactionStatistics(Transaction trans)  {
		//determine agents wealth for bid and ask agents group
		double totalAskAgentsAssets = 0;
		double totalBidAgentsCash = 0;
		double totalUtility=0, totalBidUtility=0, totalAskUtility=0;
		
		if ( WEALTHTRACE && (rep == 1) && (numSuccTrans%WealthInterval) == 0)  {
			try {
					PrintWriter outTraceW = null;					
  	  				outTraceW = new PrintWriter(
	  					new FileWriter( "logs\\agentsWealth" + numSuccTrans + ".txt") );

  	  				Iterator<Agent> iter = agents.iterator();
  	  				while (iter.hasNext() ) {
  	  					Agent a = iter.next();
  	  					outTraceW.println(a.getH() + "," + a.getCE() + "," + a.getAE());
  	  				}
	  			
  	  				outTraceW.flush();
  	  				outTraceW.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
				
		}

		Iterator<Agent> iter = agents.iterator();
		while (iter.hasNext() ) {
			Agent a = iter.next();
			totalUtility += a.getAccUtility();
			if (a.isAsker())  {
				totalAskAgentsAssets += a.getAE();
				totalAskUtility += a.getAccUtility();
			}
			else  {
				totalBidAgentsCash += a.getCE();
				totalBidUtility += a.getAccUtility();
			}
		}
		
		trans.setTotalAskAgentsAssets(totalAskAgentsAssets);
		trans.setTotalBidAgentsCash(totalBidAgentsCash);	
		trans.setTotalUtility(totalUtility);
		trans.setTotalAskUtility(totalAskUtility);
		trans.setTotalBidUtility(totalBidUtility);
	}
	
	@SuppressWarnings("unused")
	protected void calculateAndExportRepStatistics(ArrayList<ArrayList<SingleAuctionStats>> statistics)  {
		//calculate replication statistics
		//first version: just for stage 1
		Iterator<ArrayList<SingleAuctionStats>> statIt = statistics.iterator();
		double[] finalPrices = new double[statistics.size()];
		double[] lastTransactionLengths = new double[statistics.size()];
		double[] auctionLengths = new double[statistics.size()];
		double[] finalAskAssetPrices = new double[statistics.size()]; 
		double[] finalBidAssetPrices = new double[statistics.size()];
		double[] finalMeanAskHs = new double[statistics.size()];
		double[] finalMeanBidHs = new double[statistics.size()];
		double[] finalTotalUtilities = new double[statistics.size()];
		double[] finalTotalAskUtilities = new double[statistics.size()];
		double[] finalTotalBidUtilities = new double[statistics.size()];

		if (MAXREPS<=10)
			 return;
		
		int i=0;
		while (statIt.hasNext() )  {
			ArrayList<SingleAuctionStats> statsList = statIt.next();
			SingleAuctionStats stats = statsList.get(0);
			finalPrices[i] = stats.getFinalPrice();
			lastTransactionLengths[i] = stats.getLastTransactionLength();
			auctionLengths[i] = stats.getAuctionLength();
			finalAskAssetPrices[i] = stats.getFinalAskAssetPrice();
			finalBidAssetPrices[i] = stats.getFinalBidAssetPrice();
			finalMeanAskHs[i] = stats.getFinalMeanAskH();
			finalMeanBidHs[i] = stats.getFinalMeanBidH();
			finalTotalUtilities[i] = stats.getTotalUtility();
			finalTotalAskUtilities[i] = stats.getTotalAskUtility();
			finalTotalBidUtilities[i] = stats.getTotalBidUtility();
			i++;
		}
		
		try {
  	  		PrintWriter outTrace = new PrintWriter( new FileWriter( "logs\\replicStats.txt") );
  	  		
  	  	    outTrace.println( "final total utilities: ");
  	  	    double mean = StatUtils.mean(finalTotalUtilities);
  	  	    outTrace.println( "mean: " + mean);
  	  	    outTrace.println( "stdev: " + Math.sqrt(StatUtils.variance(finalTotalUtilities, mean)));
  	  	    outTrace.println(" " );
  	  	    
  	  	    outTrace.println( "final total ask utilities: ");
  	  	     	mean = StatUtils.mean(finalTotalAskUtilities);
  	  	    outTrace.println( "mean: " + mean);
  	  	    outTrace.println( "stdev: " + Math.sqrt(StatUtils.variance(finalTotalAskUtilities, mean)));
  	  	    outTrace.println(" " );
  	  	    
  	  	    outTrace.println( "final total bid utilities: ");
	  	     	mean = StatUtils.mean(finalTotalBidUtilities);
	  	    outTrace.println( "mean: " + mean);
	  	    outTrace.println( "stdev: " + Math.sqrt(StatUtils.variance(finalTotalBidUtilities, mean)));
	  	    outTrace.println(" " );
	  	    
  	  	    outTrace.println( "final asset prices: ");
  	  	     	mean = StatUtils.mean(finalPrices);
  	  	    outTrace.println( "mean: " + mean);
  	  	    outTrace.println( "stdev: " + Math.sqrt(StatUtils.variance(finalPrices, mean)));
  	  	    outTrace.println(" " );
  	  	    
  	  	    outTrace.println( "auction lengths: ");
  	  	     	mean = StatUtils.mean(auctionLengths);
  	  	    outTrace.println( "mean: " + mean);
  	  	    outTrace.println( "stdev: " + Math.sqrt(StatUtils.variance(auctionLengths, mean)));
  	  	    outTrace.println(" " );
  	  	    
  	  	    outTrace.println( "last transaction lengths: ");
	  	     	mean = StatUtils.mean(lastTransactionLengths);
	  	    outTrace.println( "mean: " + mean);
	  	    outTrace.println( "stdev: " + Math.sqrt(StatUtils.variance(lastTransactionLengths, mean)));
	  	    outTrace.println(" " );
	  	    
  	  	    outTrace.println( "final ask asset prices: ");
  	  	    mean = StatUtils.mean(finalAskAssetPrices);
  	  	    outTrace.println( "mean: " + mean);
  	  	    outTrace.println( "stdev: " + Math.sqrt(StatUtils.variance(finalAskAssetPrices, mean)));
  	  	    outTrace.println(" " );
  	  	    
  	  	    outTrace.println( "final bid asset prices: ");
  	  	    mean = StatUtils.mean(finalBidAssetPrices);
  	  	    outTrace.println( "mean: " + mean);
  	  	    outTrace.println( "stdev: " + Math.sqrt(StatUtils.variance(finalBidAssetPrices, mean)));
  	  	    outTrace.println(" " );
  	  	    
  	  	    outTrace.println( "final Mean Ask Hs: ");
  	  	    mean = StatUtils.mean(finalMeanAskHs);
  	  	    outTrace.println( "mean: " + mean);
  	  	    outTrace.println( "stdev: " + Math.sqrt(StatUtils.variance(finalMeanAskHs, mean)));
  	  	    outTrace.println(" " );
  	  	    
  	  	    outTrace.println( "final Mean Bid Hs: ");
  	  	    mean = StatUtils.mean(finalMeanBidHs);
  	  	    outTrace.println( "mean: " + mean);
  	  	    outTrace.println( "stdev: " + Math.sqrt(StatUtils.variance(finalMeanBidHs, mean)));
  	  	    outTrace.println(" " );  	  	    
  	  	    
    	  	outTrace.flush();
    	  	outTrace.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings({ "unused", "resource" })
	protected SingleAuctionStats calculateAndExportStatistics(ArrayList<Transaction> auctionStatistics, int repNr)  {
	//statistics for a single replication
	//first version: just for stage 1
		Iterator<Transaction> transIt = auctionStatistics.iterator();
		int nrFailure = 0;
		Transaction lastSuccessfulTrans = null;
		SingleAuctionStats stats = null;

		try {
			PrintWriter outTrace = null;
			if (MAXREPS<=10)
  	  			outTrace = new PrintWriter(
  	  					new FileWriter( "logs\\agentsTrace" + repNr + ".txt") );

  	  		while (transIt.hasNext())  {
  	  			Transaction transaction = transIt.next();
  	  			if (transaction.wasSuccessful())  {
//  	  				lastSuccessfulTrans = transaction;
  	  				if (MAXREPS<=10)
  	  				outTrace.println(transaction.getAssetPrice() + ", " + transaction.getFinalAskAssetPrice() + ", " + transaction.getFinalBidAssetPrice() 
						+ ", " + transaction.getMeanAskAssetPrice() + ", " + transaction.getMeanBidAssetPrice()
					+ ", " + transaction.getFinalAskH() + ", " + transaction.getFinalBidH() 
					+ ", " + transaction.getTotalUtility()+ ", " + transaction.getTransNum() 
					+ ", " + transaction.getTotalAskAgentsAssets() + ", "+ transaction.getTotalBidAgentsCash());
  	  			}	
  	  			else {
  	  				nrFailure++;
  	  			}
  	  		}
			if (MAXREPS<=10)  {
  	  			outTrace.println(auctionStatistics.size() + ", " + nrFailure + ", 0, 0, 0, 0, 0, 0, 0, 0, 0");
  	  			outTrace.flush();
  				if ( repNr == 1)  {					
  					outTrace = new PrintWriter(
	  				new FileWriter( "logs\\agentsWealthFinal.txt") );

  					Iterator<Agent> iter = agents.iterator();
  	  				while (iter.hasNext() ) {
  	  					Agent a = iter.next();
						outTrace.println(a.getH() + "," + a.getCE() + "," + a.getAE());
  	  				}
  	  				
  	  				outTrace.flush();
  	  				outTrace.close();
  				}

			}
		
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (lastSuccessfulTrans != null)  {
			stats = new SingleAuctionStats();
			stats.setFinalPrice(lastSuccessfulTrans.getAssetPrice());
			stats.setTotalUtility(lastSuccessfulTrans.getTotalUtility());
			stats.setTotalAskUtility(lastSuccessfulTrans.getTotalAskUtility());
			stats.setTotalBidUtility(lastSuccessfulTrans.getTotalBidUtility());
			stats.setLastTransactionLength(lastSuccessfulTrans.getLength());
			stats.setAuctionLength(auctionStatistics.size());
			stats.setFinalAskAssetPrice(lastSuccessfulTrans.getFinalAskAssetPrice());
			stats.setFinalBidAssetPrice(lastSuccessfulTrans.getFinalBidAssetPrice());
			stats.setFinalMeanAskH(lastSuccessfulTrans.getMeanAskH());
			stats.setFinalMeanBidH(lastSuccessfulTrans.getMeanBidH());
		}
		
		return stats;
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

	protected boolean enoughReplications(ArrayList<ArrayList<SingleAuctionStats>> statistics)  {
	//first simple version: fixed number of replications: better you use confidence intervals
		if (statistics.size() >= MAXREPS)
			return true;

		return false;
	}
	
	protected boolean auctionFinished( ArrayList<Transaction> transactionTrace, Transaction transaction )  {
	//first simple version: test whether last MAXZEROTRANSACTIONS without transaction being positively concluded
		int MODE = 2;
		
		if (transaction.wasSuccessful())  {
			if (TRANSACTION_TRACE)  
				transactionTrace.add(transaction);
			lastSuccessfulTrans = transaction;
		}

		switch (MODE) { 
			case 1:
				if (!transaction.wasSuccessful())  {
					zeroTrans++;
					if (zeroTrans >= MAXZEROTRANSACTIONS)
						return true;
				}
				else  {
					zeroTrans =0;
				}
				
				return false;
			
			case 2:
				return (num_trans+1 >= MAXTRANS);
		}
		
		return false;
	}
	
	protected void executeEvents()  {
		//execute events that agents hold beliefs on: eventually update wealth of agents
		//only for multistage events
		//toDo in future versions
	}

	protected void resetAgentBeliefs()  {
		//update h of each agent
		//only for multistage events
		//toDo in future versions
	}

	public enum MatchingType {
		BEST_GLOBAL_OFFERS,
		BEST_NEIGHBOUR,
		RANDOM_NEIGHOUR;
	}
	
	public Transaction executeSingleTransaction()  {
		//each agent, in random order, gets asked for its bid or ask offers for all markets in this transaction round. 
		//a single transaction repeats this in a loop until an ask and a bid offer of the same market match each other (success), 
		//or until each agent has placed its offers (failure)
		
		int MAXSWEEPS = 500;
		int numSweeps = 0;  //number of sweeps through all agents in a random order
		Transaction transaction = getNewTransaction();
		
		while (numSweeps < MAXSWEEPS)  {
			numSweeps++;
			Iterator<Agent> agIt = agents.randomIterator();
			
			boolean successfulTrans = false;
			
			while (agIt.hasNext())  {
				Agent ag = agIt.next();
				
				Offering[] match = new Offering[NUMMARKETTYPES*2]; //in general: for each market type a bid and an ask offer
	
				// agent calculates offering for each market
				ag.calcOfferings();
				AskOffering[] askOfferings = ag.getCurrentAskOfferings(); 
				BidOffering[] bidOfferings = ag.getCurrentBidOfferings();
				
				// find match: must be neighbours, must be same market, bid (buy) must be larger than ask (sell)
				int matchResult = transaction.findMatches(askOfferings, bidOfferings, match, agents); //0: no match; 1: askOffering matched; 2: bidOffering matched
				
				// add offerings of the agent ag to the offer-book (which is the transaction) which keeps track of the best offerings
				transaction.addOfferings(askOfferings, bidOfferings);
				
				// there was a match
				if  (matchResult > 0) {
					// executes Transaction: sell and buy in the two agents will update new wealth
					successfulTrans = ag.execTransaction( match, true );
				}
				
				// transaction was successul 
				if (successfulTrans)  {
					//successful transaction execution
					transaction.matched( match );
					transaction.setTransNum(num_trans++);
					
					return transaction;
				}
			}
		}
		
		return transaction;
	}

	public Transaction executeSingleTransactionByType( MatchingType type )  {
		int MAXSWEEPS = 500;
		int numSweeps = 0;  //number of sweeps through all agents in a random order
		Transaction transaction = getNewTransaction();
		
		while (numSweeps < MAXSWEEPS)  {
			numSweeps++;
			Iterator<Agent> agIt = agents.randomIterator();
			
			boolean successfulTrans = false;
		
			while (agIt.hasNext())  {
				Agent ag = agIt.next();
				Offering[] match = null;
				
				// let current agent calculate its offers
				ag.calcOfferings();
				
				// find match: must be neighbours, must be same market, bid (buy) must be larger than ask (sell)
				if ( MatchingType.RANDOM_NEIGHOUR == type ) {
					match = transaction.findMatchesByRandomNeighborhood( ag, agents ); //null: no match, else matching
				
				} else if ( MatchingType.BEST_NEIGHBOUR == type ) {
					match = transaction.findMatchesByBestNeighborhood( ag, agents ); //null: no match, else matching
					
				} else if ( MatchingType.BEST_GLOBAL_OFFERS == type ) {
					match = new Offering[NUMMARKETTYPES*2]; //in general: for each market type a bid and an ask offer
				
					AskOffering[] askOfferings = ag.getCurrentAskOfferings(); 
					BidOffering[] bidOfferings = ag.getCurrentBidOfferings();

					if ( 0 == transaction.findMatches(askOfferings, bidOfferings, match, agents) ) {
						match = null;
					}
					
					transaction.addOfferings( askOfferings, bidOfferings );
				}

				if ( match != null ) {
					// executes Transaction: sell and buy in the two agents will update new wealth
					successfulTrans = ag.execTransaction( match, true );
				}
				
				if (successfulTrans)  {
					//successful transaction execution
					transaction.matched( match );
					transaction.setTransNum(num_trans++);
					
					return transaction;
				}
			}
		}
		
		return transaction;
	}
	
	protected Transaction getNewTransaction() {
		return new Transaction(this);
	}

	public Asset getAsset() {
		return asset;
	}
}
