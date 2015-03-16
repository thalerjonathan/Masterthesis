package doubleAuction;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.math.stat.StatUtils;

import doubleAuction.stats.SingleAuctionStats;
import doubleAuction.stats.SingleAuctionStatsWithLoans;
import doubleAuction.tx.Transaction;
import doubleAuction.tx.TransactionWithLoans;
import agents.*;
import agents.markets.Asset;
import agents.markets.Loans;
import agents.network.AgentNetwork;

public class AuctionWithLoans extends Auction {
	protected double[] initialLoanPrices;
	protected double[] J;
	protected Loans loanMarket;
	protected double[][] agentsWealthMean;
	
	public AuctionWithLoans( AgentNetwork agents, Asset asset )   {
		super( agents, asset );
		agentsWealthMean = new double[this.agents.size()][5];
	}
    
	@Override
	protected Transaction getNewTransaction() {
		// TODO Auto-generated method stub
		return new TransactionWithLoans(this);
	}

	@Override
	protected void  initializeTrace()  {
		singleTransTrace = 	new double[MAXTRANS][7+4*Loans.NUMLOANS];
	}

	
	@Override
	protected void initializeMarkets()  {

		//loans
//		J = new double[] {0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.6, 0.7, 0.8, 0.9};
		J = new double[] {0.2};
//    	J = new double[] {0.1};
		//	    	initialLoanPrices = new double[] { 0.3, 0.34, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2};
		//	    	initialLoanPrices = new double[] {0.199999, 0.234499};
		//	    	initialLoanPrices = new double[] {0.2, 0.25};
//		initialLoanPrices = new double[] {0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.6, 0.7, 0.8, 0.9};
		//	    	initialLoanPrices = new double[] {0.025, 0.5, 0.075, 0.1, 0.125, 0.15, 0.175, 0.2, 0.225, 0.25, 0.3, 0.35, 0.4, 0.45};
    	initialLoanPrices = new double[] {0.2};

		//assets
		//initialAssetprice = 0.6;
		
		//1. initialize asset market
		//asset = new Asset(initialAssetprice, Agent.NUM_AGENTS*assetEndow);

		//2. initialize loan market
		loanMarket = new Loans(initialLoanPrices,J);

		//3. set NUMMARKETS: NUMLOANS loan markets and 1+NUMLOANS asset markets (asset for cash and asset for loan type j)
		NUMMARKETS = Loans.NUMLOANS + Loans.NUMLOANS + 1;
		NUMMARKETTYPES = 3; //0:assets against cash; 1:assets against loans; 2: loans
	}

	@Override	
	protected void 	initializeAgents()  {
		//assetEndow=1;
		//consumEndow=1;
		//Agent.NUM_AGENTS = NUM_AGENTS;
		Agent.TRADE_ONLY_FULL_UNITS = true;

//		agents = new AgentWithLoans[] {new AgentWithLoans(0.5, consumEndow, assetEndow, loanMarket, asset), 
//		    						  new AgentWithLoans(0.7, consumEndow, assetEndow, loanMarket, asset), 
//									  new AgentWithLoans(0.8, consumEndow, assetEndow, loanMarket, asset)};

		/*
		agents = new AgentWithLoans[Agent.NUM_AGENTS];
		randomlyOrderedAgents = new ArrayList<Agent>();

		for (int i=0;i<Agent.NUM_AGENTS; i++) {
			agents[i] = new  AgentWithLoans(i,(i+1)*(1.0/Agent.NUM_AGENTS), consumEndow, assetEndow, loanMarket, asset);
			randomlyOrderedAgents.add(agents[i]);
		}

		
		this.agents = AgentNetwork.createWithHubs( 3, new IAgentFactory() {
			private int i = 0;
			
			@Override
			public Agent createAgent() {
				Agent a = null;
			
				if ( i < NUM_AGENTS ) {
					a = new AgentWithLoans(i,(i+1)*(1.0/Agent.NUM_AGENTS), consumEndow, assetEndow, loanMarket, asset);
					i++;
				}
				
				return a;
			}
		});
		*/
		
		hMin = 0;
		hMax = new double[Loans.NUMLOANS];
		idMax = new int[Loans.NUMLOANS];
		for (int i=0;i<Loans.NUMLOANS;i++) {
			hMax[i] = agents.get(agents.size() - 1).getH();
			idMax[i] = this.agents.size()-1;
		}
	}
	
	@SuppressWarnings("unused")
	@Override
	protected void calculateSingleTransactionStatistics(Transaction trans)  {
		//determine agents wealth for bid and ask agents group
		double totalAskAgentsAssets = 0;
		double totalBidAgentsCash = 0;
		double totalAskAgentsFreeAssets = 0;
		double totalBidAgentsFreeAssets = 0;
		double totalUtility = 0, totalBidUtility=0, totalAskUtility=0;
		double totalBidAgentsLoans[][] = new double[Loans.NUMLOANS][2];   //0: loan taken; 1: loan given
		double totalAskAgentsLoans[][] = new double[Loans.NUMLOANS][2];  

		if ( WEALTHTRACE && (rep == 1) && (num_trans%WealthInterval) == 0)  {
			try {
				PrintWriter outTrace1 = null;					
  				outTrace1 = new PrintWriter(
  					new FileWriter( "logs\\agentsWealth" + numSuccTrans + ".txt") );

  				Iterator<Agent> iter = agents.iterator();
  				while (iter.hasNext() ) {
  					Agent a = iter.next();
					outTrace1.println(a.getH() + "," + a.getCE() + "," + a.getAE() + "," + ((AgentWithLoans)a).getFreeAssetEndow() 
							    + "," + ((AgentWithLoans)a).getLoanTaken()[0] + "," + ((AgentWithLoans)a).getLoanGiven()[0]);
  				}
  				
  				outTrace1.flush();
  				outTrace1.close();
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}

		Iterator<Agent> iter = agents.iterator();
		while (iter.hasNext() ) {
			Agent a = iter.next();
			
			totalUtility += a.getAccUtility();
			if (a.isAsker()) {
				totalAskAgentsAssets += a.getAE();
				totalAskAgentsFreeAssets += ((AgentWithLoans)a).getFreeAssetEndow();
				totalAskUtility += a.getAccUtility();
				for (int k=0; k<Loans.NUMLOANS; k++)  {
				   totalAskAgentsLoans[k][0] += ((AgentWithLoans)a).getLoanTaken()[k];
				   totalAskAgentsLoans[k][1] += ((AgentWithLoans)a).getLoanGiven()[k];
				}
			}
			else   {
				totalBidAgentsCash += a.getCE();
			    totalBidAgentsFreeAssets += ((AgentWithLoans)a).getFreeAssetEndow();
				totalBidUtility += a.getAccUtility();
				for (int k=0; k<Loans.NUMLOANS; k++)  {
					   totalBidAgentsLoans[k][0] += ((AgentWithLoans)a).getLoanTaken()[k];
					   totalBidAgentsLoans[k][1] += ((AgentWithLoans)a).getLoanGiven()[k];
				}
			}
		}
		
		trans.setTotalAskAgentsAssets(totalAskAgentsAssets);
		trans.setTotalBidAgentsCash(totalBidAgentsCash);
		trans.setTotalUtility(totalUtility);
		trans.setTotalAskUtility(totalAskUtility);
		trans.setTotalBidUtility(totalBidUtility);
		((TransactionWithLoans)trans).setTotalAskAgentsFreeAssets(totalAskAgentsFreeAssets);
		((TransactionWithLoans)trans).setTotalBidAgentsFreeAssets(totalBidAgentsFreeAssets);
		((TransactionWithLoans)trans).setTotalAskAgentsLoans(totalAskAgentsLoans);
		((TransactionWithLoans)trans).setTotalBidAgentsLoans(totalBidAgentsLoans);
	}
	
	@SuppressWarnings("unused")
	@Override
	protected SingleAuctionStats calculateAndExportStatistics(ArrayList<Transaction> auctionStatistics, int repNr)  {
		//statistics for a single replication
		//first version: just for stage 1
			Iterator<Transaction> transIt = auctionStatistics.iterator();
			int nrFailure = 0;
			SingleAuctionStatsWithLoans stats = null;
			double[] loanPrices = new double[Loans.NUMLOANS]; 
			int loanType = -1;
			double[][] agentsWealthMvAv = new double[this.agents.size()][MAXREPS];
			double y0=-1, y1=-1; 
			double meanCashPs = 0, meanLoanMs = 0, meanAssetsOs = 0 ;

			try {
				PrintWriter outTrace = null;
				if (MAXREPS<=10)  	{				
	  	  			outTrace = new PrintWriter(
	  	  					new FileWriter( "logs\\agentsTrace" + repNr + ".txt") );
				}
				

	  	  		while (transIt.hasNext())  {
	  	  			Transaction transaction = transIt.next();
	  	  			if (transaction.wasSuccessful())  {
	  	  				if (((TransactionWithLoans)transaction).getLoanPrice() > 0)  {
	  	  					loanType = ((TransactionWithLoans)transaction).getLoanType();
	  	  					loanPrices[loanType] = ((TransactionWithLoans)transaction).getLoanPrice();
	  	  				}
	  	  			}	
  	  				if (MAXREPS<=10)  {
  	  					StringBuffer traceln = new StringBuffer();
  	  					traceln.append(transaction.getAssetPrice() + ", " + ((TransactionWithLoans)transaction).getLoanPrice() + ", " + (((TransactionWithLoans)transaction).getLoanType()+1) 
  							+ ", " + transaction.getFinalAskAssetPrice() + ", " + transaction.getFinalBidAssetPrice() 
  	  						+ ", " + transaction.getFinalAskAssetPrice()+ ", " + transaction.getFinalBidAssetPrice() 
  	  						+ ", " + ((TransactionWithLoans)transaction).getTotalAskAgentsFreeAssets() + ", " + ((TransactionWithLoans)transaction).getTotalBidAgentsFreeAssets() );
  	  					for (int i=0; i< Loans.NUMLOANS; i++)  
  	  						traceln.append(", " + ((TransactionWithLoans)transaction).getTotalAskAgentsLoans()[i][0] + ", " + ((TransactionWithLoans)transaction).getTotalAskAgentsLoans()[i][1] 
						                + ", " + ((TransactionWithLoans)transaction).getTotalBidAgentsLoans() [i][0] + ", " + ((TransactionWithLoans)transaction).getTotalBidAgentsLoans() [i][1]);

  	  					traceln.append( ", " + transaction.getMeanAskH() + ", " + transaction.getMeanBidH() 
  	  						+ ", " + ((TransactionWithLoans)transaction).getFinalAskH() + ", " + ((TransactionWithLoans)transaction).getFinalBidH() + ", " + transaction.getTotalUtility()+ ", " + transaction.getTransNum());
  	  					outTrace.println(traceln);

  	  				}
	  	  			else {
	  	  				nrFailure++;
	  	  			}
	  	  		}
	  	  		
	  			if (MAXREPS<=10)  {
	  				
	  			  if (!TRANSACTION_TRACE) {
		  	  		  for (int k=0;k<num_trans_trace;k++)  {
	  	  				StringBuffer traceln = new StringBuffer();
	  	  				traceln.append(singleTransTrace[k][0] + ", " + singleTransTrace[k][1] + ", " +singleTransTrace[k][2]);
	  	  				int i;
	  	  				for (i=0; i< Loans.NUMLOANS; i++)  {  
	  	  					traceln.append(", " + singleTransTrace[k][3+4*i]);
	  	  					traceln.append(", " + singleTransTrace[k][4+4*i]);
	  	  					traceln.append(", " + singleTransTrace[k][5+4*i]);
	  	  					traceln.append(", " + singleTransTrace[k][6+4*i]);
	 	    			} 
	  	  				traceln.append(", " + singleTransTrace[k][3+4*i]);
	  	  				traceln.append(", " + singleTransTrace[k][4+4*i]);
	  	  				traceln.append(", " +singleTransTrace[k][5+4*i]);
	  	  				traceln.append(", " +singleTransTrace[k][6+4*i]);
	  		  	  		outTrace.println(traceln);
		  	  		  }  	  				
	  			  }
	  	  		
	  	  		  outTrace.flush();
	  	  		  outTrace.close();
				  outTrace = new PrintWriter( new FileWriter( "logs\\successStats"  + repNr + ".txt"));
	  			  outTrace.println(auctionStatistics.size() + ", " + nrFailure);
	  	  		  outTrace.flush();
	  	  		  outTrace.close();
//	  			  if ( repNr == 1)  {					
	  					outTrace = new PrintWriter(
		  				new FileWriter( "logs\\agentsWealthFinal" + repNr + ".txt") );

	  					Iterator<Agent> iter = agents.iterator();
	  					while (iter.hasNext() ) {
	  						Agent a = iter.next();
	  						
							outTrace.println(a.getH() + "," + a.getCE() + "," + a.getAE() + "," + ((AgentWithLoans)a).getFreeAssetEndow() + "," + ((AgentWithLoans)a).getLoanTaken()[0] 
							                              + "," + ((AgentWithLoans)a).getLoanGiven()[0]);
	  					}
	  					
	  	  				outTrace.flush();
	  	  				outTrace.close();
//	  			  }
				
	  			}
	  			else  {
  					outTrace = new PrintWriter(
  			  				new FileWriter( "logs\\agentsWealthFinal" + repNr + ".txt") );

  							int i = 0;
							Iterator<Agent> iter = agents.iterator();
							while (iter.hasNext() ) {
								Agent a = iter.next();
						
  								outTrace.println(a.getH() + "," + a.getCE() + "," + a.getAE() + "," + ((AgentWithLoans)a).getFreeAssetEndow() + "," + ((AgentWithLoans)a).getLoanTaken()[0] 
  								                                                                                                                      + "," + ((AgentWithLoans)a).getLoanGiven()[0]);
  								agentsWealthMean[i][0] += a.getCE(); 
  								agentsWealthMean[i][1] += a.getAE();
  								agentsWealthMean[i][2] += ((AgentWithLoans)a).getFreeAssetEndow();
  								agentsWealthMean[i][3] += ((AgentWithLoans)a).getLoanTaken()[0];
  								agentsWealthMean[i][4] += ((AgentWithLoans)a).getLoanGiven()[0];
  								agentsWealthMvAv[i][0] = a.getCE(); 
  								agentsWealthMvAv[i][1] = a.getAE();
  								agentsWealthMvAv[i][2] = ((AgentWithLoans)a).getFreeAssetEndow();
  								agentsWealthMvAv[i][3] = ((AgentWithLoans)a).getLoanTaken()[0];
  								agentsWealthMvAv[i][4] = ((AgentWithLoans)a).getLoanGiven()[0];
  								i++;
  							}
  			  			
  							agentsWealthMvAv = calcMovAverage(agentsWealthMvAv);
/*testStart
  		  					PrintWriter outTraceTest = new PrintWriter(  			  				
  		  							new FileWriter( "logs\\agentsWealthFinalSmoothed" + repNr + ".txt") );

  							for (int i=0;i<agents.length;i++)  
  								outTraceTest.println(agentsWealthMvAv[i][0] + "," + agentsWealthMvAv[i][4]);
  	  		  	  			
  							outTraceTest.flush();
  	  		  	  			outTraceTest.close();
  	  		  	  
endTest*/
  		  	  				outTrace.flush();
  		  	  				outTrace.close();

  		  	  				//approximate y0: indifferent agent for giving loan or keeping only cash
                            for ( i=11; i<agents.size()-11; i++ )
                            	if (agentsWealthMvAv[i][0] <= agentsWealthMvAv[i][4])  {
                            		y0 = agents.get(i).getH();
                            		break;
                            	}
  		  	  				//approximate y1: indifferent agent for giving loan or buying assets
                            if (lastSuccessfulTrans != null)  {
                            	y1 = 0.5*(lastSuccessfulTrans.getFinalAskH() + lastSuccessfulTrans.getFinalBidH());
                            }
                            if (y0>0 && y1>0)  {
                            	//calc mean cash for P-agents, mean loan given for M-agents, and mean assets for O-agents
                            	int lastI=0, iY0=0;
                            	for (i=0;i<agents.size();i++)  {
                            		if (agents.get(i).getH()<y0)  {
                            			meanCashPs += agents.get(i).getCE();
                            		}
                            		else if (agents.get(i).getH()<y1)  {
                            			if (lastI == 0)  {
                            				meanCashPs /= i;
                            				lastI=i;
                            				iY0 = i;
                            			}
                            			meanLoanMs += ((AgentWithLoans)agents.get(i)).getLoanGiven()[0];
                            		}
                            		else  {
                            			if (lastI == iY0)  {
                            				meanLoanMs /= (i-lastI);
                            				lastI=i;
                            			}
                            			meanAssetsOs += agents.get(i).getAE();
                            		}
                            	}
                            	meanAssetsOs /= (agents.size()-lastI);
                            }
                            
                            		
	  			}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (lastSuccessfulTrans != null)  {
				stats = new SingleAuctionStatsWithLoans();
				stats.setFinalPrice(lastSuccessfulTrans.getAssetPrice());
				stats.setFinalLoanPrices(loanPrices);
				stats.setFinalLoanType(loanType);
				stats.setTotalUtility(lastSuccessfulTrans.getTotalUtility());
				stats.setTotalAskUtility(lastSuccessfulTrans.getTotalAskUtility());
				stats.setTotalBidUtility(lastSuccessfulTrans.getTotalBidUtility());
				stats.setLastTransactionLength(lastSuccessfulTrans.getLength());
				stats.setAuctionLength(auctionStatistics.size());
				stats.setFinalAskAssetPrice(lastSuccessfulTrans.getFinalAskAssetPrice());
				stats.setFinalBidAssetPrice(lastSuccessfulTrans.getFinalBidAssetPrice());
				stats.setFinalMeanAskH(lastSuccessfulTrans.getFinalAskH());
				stats.setFinalMeanBidH(lastSuccessfulTrans.getFinalBidH());
				stats.setFinalMeanLoanAskers(((TransactionWithLoans)lastSuccessfulTrans).getTotalAskAgentsLoans());
				stats.setFinalMeanLoanBidders(((TransactionWithLoans)lastSuccessfulTrans).getTotalBidAgentsLoans());
				stats.setY0(y0);
				stats.setY1(y1);
				stats.setMeanCashP(meanCashPs);
				stats.setMeanLoanM(meanLoanMs);
				stats.setMeanAssetsO(meanAssetsOs);
				
			}
			return stats;
		}

	private double[][] calcMovAverage(double[][] agentsWealthMvAv) {
		//calculates a centered moving average for each column in agentsWealthMvAv
		int WINDOW = 11;
		int HALF = 5;
		double mvAvg;
		double[][] result = new double[this.agents.size()][agentsWealthMvAv[0].length]; 
		
		for (int j=0;j<agentsWealthMvAv[0].length;j++)  {
			mvAvg = 0;
			for (int i=0; i<this.agents.size()-WINDOW/2-1;i++)   {
				if (i<WINDOW)  {
					mvAvg += agentsWealthMvAv[i][j];
					result[i][j] = mvAvg/(i+1);
				}
				else  {
					mvAvg = mvAvg + agentsWealthMvAv[i][j] - agentsWealthMvAv[i-WINDOW][j];
					result[i-HALF][j] = mvAvg/WINDOW;
				}
			 
			}
		}
		return result;
	}

	@Override
	protected void keepTransactionTrace(Transaction transaction) {
		if (transaction.wasSuccessful()) {
			num_trans_trace++;
			singleTransTrace[num_trans_trace][0] = transaction.getAssetPrice();
			singleTransTrace[num_trans_trace][1] = ((TransactionWithLoans)transaction).getLoanPrice();
			singleTransTrace[num_trans_trace][2] = ((TransactionWithLoans)transaction).getMarket();
			int i;
			for (i=0; i< Loans.NUMLOANS; i++)  {  
  					singleTransTrace[num_trans_trace][3+4*i] = ((TransactionWithLoans)transaction).getTotalAskAgentsLoans()[i][0];
  					singleTransTrace[num_trans_trace][4+4*i] = ((TransactionWithLoans)transaction).getTotalAskAgentsLoans()[i][1]; 
  					singleTransTrace[num_trans_trace][5+4*i] = ((TransactionWithLoans)transaction).getTotalBidAgentsLoans()[i][0];
  					singleTransTrace[num_trans_trace][6+4*i] = ((TransactionWithLoans)transaction).getTotalBidAgentsLoans()[i][1];
  				} 
  				singleTransTrace[num_trans_trace][3+4*i] = ((TransactionWithLoans)transaction).getFinalAskH();
  			    singleTransTrace[num_trans_trace][4+4*i] = ((TransactionWithLoans)transaction).getFinalBidH();
  			    singleTransTrace[num_trans_trace][5+4*i] = transaction.getTotalUtility();
  			    singleTransTrace[num_trans_trace][6+4*i] = transaction.getTransNum();
		}

	}
	
	@SuppressWarnings("unused")
	@Override
	protected void calculateAndExportRepStatistics(ArrayList<ArrayList<SingleAuctionStats>> statistics)  {
		//calculate replication statistics
		//first version: just for stage 1
		Iterator<ArrayList<SingleAuctionStats>> statIt = statistics.iterator();
		double[] finalPrices = new double[statistics.size()];
		double[][] finalLoanPrices = new double[Loans.NUMLOANS][statistics.size()];
		double[] finalLoanTypes = new double[statistics.size()];
		double[] lastTransactionLengths = new double[statistics.size()];
		double[] auctionLengths = new double[statistics.size()];
		double[] finalAskAssetPrices = new double[statistics.size()]; 
		double[] finalBidAssetPrices = new double[statistics.size()];
		double[] finalMeanAskHs = new double[statistics.size()];
		double[] finalMeanBidHs = new double[statistics.size()];
        double[][][] finalMeanLoanAskers = new double[statistics.size()][Loans.NUMLOANS][2];
        double[][][] finalMeanLoanBidders = new double[statistics.size()][Loans.NUMLOANS][2];
		double[] finalTotalUtilities = new double[statistics.size()];
		double[] finalTotalAskUtilities = new double[statistics.size()];
		double[] finalTotalBidUtilities = new double[statistics.size()];
		double[] loanPrices, loanTypes;
		double[] y0s= new double[statistics.size()];
		double[] y1s= new double[statistics.size()];
		double[] meanCashPs= new double[statistics.size()];
		double[] meanLoanMs= new double[statistics.size()];
		double[] meanAssetsOs= new double[statistics.size()];
		
		if (MAXREPS<=10)
			 return;
		
		int i=0;
		while (statIt.hasNext() )  {
			ArrayList<SingleAuctionStats> statsList = statIt.next();
			SingleAuctionStats stats = statsList.get(0);
			finalPrices[i] = stats.getFinalPrice();
			finalLoanTypes[i] = ((SingleAuctionStatsWithLoans)stats).getFinalLoanType() + 1;			
			for (int k1=0; k1 < Loans.NUMLOANS; k1++)
				finalLoanPrices[k1][i] = ((SingleAuctionStatsWithLoans)stats).getFinalLoanPrices()[k1];			
			lastTransactionLengths[i] = stats.getLastTransactionLength();
			auctionLengths[i] = stats.getAuctionLength();
			finalAskAssetPrices[i] = stats.getFinalAskAssetPrice();
			finalBidAssetPrices[i] = stats.getFinalBidAssetPrice();
			finalMeanAskHs[i] = stats.getFinalMeanAskH();
			finalMeanBidHs[i] = stats.getFinalMeanBidH();
			finalMeanLoanAskers[i] = ((SingleAuctionStatsWithLoans)stats).getFinalMeanLoanAskers();
			finalMeanLoanBidders[i] = ((SingleAuctionStatsWithLoans)stats).getFinalMeanLoanBidders();
			finalTotalUtilities[i] = stats.getTotalUtility();
			finalTotalAskUtilities[i] = stats.getTotalAskUtility();
			finalTotalBidUtilities[i] = stats.getTotalBidUtility();
			y0s[i] = ((SingleAuctionStatsWithLoans)stats).getY0();
			y1s[i] = ((SingleAuctionStatsWithLoans)stats).getY1();
			meanCashPs[i] = ((SingleAuctionStatsWithLoans)stats).getMeanCashP();
			meanLoanMs[i] = ((SingleAuctionStatsWithLoans)stats).getMeanLoanM();
			meanAssetsOs[i] = ((SingleAuctionStatsWithLoans)stats).getMeanAssetsO();

			i++;
		}
		
		try {
  	  		PrintWriter outTrace = new PrintWriter( new FileWriter( "logs\\finalWealthMean.txt") );
			for (int i1=0;i1<agents.size();i1++)  
				outTrace.println( agentsWealthMean[i1][0]/MAXREPS + "," + agentsWealthMean[i1][1]/MAXREPS + "," + agentsWealthMean[i1][2]/MAXREPS 
				                                          + "," + agentsWealthMean[i1][3]/MAXREPS + "," + agentsWealthMean[i1][4]/MAXREPS); 
    	  	outTrace.flush();
    	  	outTrace.close();
    	  	
		} catch (IOException e) {
			e.printStackTrace();
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
  	  	    
	  	    outTrace.println( "final loan types: ");
	  	    mean = StatUtils.mean(finalLoanTypes);
	  	    outTrace.println( "mean: " + mean);
	  	    outTrace.println( "stdev: " + Math.sqrt(StatUtils.variance(finalLoanTypes, mean)));
	  	    outTrace.println(" " );

	  	    outTrace.println( "final loan prices: ");
  	  	    for (int k=0; k<Loans.NUMLOANS; k++)  {
  	  	  	    outTrace.println( "loan type: " + J[k]);  	  	        
  	  	    	mean = StatUtils.mean(finalLoanPrices[k]);
  	  	    	outTrace.println( "mean: " + mean);
  	  	    	outTrace.println( "stdev: " + Math.sqrt(StatUtils.variance(finalLoanPrices[k], mean)));
  	  	    	outTrace.println(" " );
  	  	    }
  	  	    
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
  	  	    
  	  	    outTrace.println( "y0s: ");
  	  	    mean = StatUtils.mean(y0s);
  	  	    outTrace.println( "mean: " + mean);
  	  	    outTrace.println( "stdev: " + Math.sqrt(StatUtils.variance(y0s, mean)));
  	  	    outTrace.println(" " );
  	  	    
  	  	    outTrace.println( "y1s: ");
  	  	    mean = StatUtils.mean(y1s);
  	  	    outTrace.println( "mean: " + mean);
  	  	    outTrace.println( "stdev: " + Math.sqrt(StatUtils.variance(y1s, mean)));
  	  	    outTrace.println(" " );
  	  	    
  	  	    outTrace.println( "meanCashPs: ");
  	  	    mean = StatUtils.mean(meanCashPs);
  	  	    outTrace.println( "mean: " + mean);
  	  	    outTrace.println( "stdev: " + Math.sqrt(StatUtils.variance(meanCashPs, mean)));
  	  	    outTrace.println(" " );
  	  	    
  	  	    outTrace.println( "meanLoanMs: ");
  	  	    mean = StatUtils.mean(meanLoanMs);
  	  	    outTrace.println( "mean: " + mean);
  	  	    outTrace.println( "stdev: " + Math.sqrt(StatUtils.variance(meanLoanMs, mean)));
  	  	    outTrace.println(" " );
  	  	    
  	  	    outTrace.println( "meanAssetsOs: ");
  	  	    mean = StatUtils.mean(meanAssetsOs);
  	  	    outTrace.println( "mean: " + mean);
  	  	    outTrace.println( "stdev: " + Math.sqrt(StatUtils.variance(meanAssetsOs, mean)));
  	  	    outTrace.println(" " );
  	  	    
/*  	  	    for (int k=0; k<Loans.NUMLOANS; k++)  {
  	  	    outTrace.println( "loantype: " + k);
  	  	    outTrace.println( "final askers loan taken: ");
  	  	    mean = StatUtils.mean(finalMeanLoanAskers);
  	  	    outTrace.println( "mean: " + mean);
  	  	    outTrace.println( "stdev: " + Math.sqrt(StatUtils.variance(finalMeanLoanAskers, mean)));
  	  	    outTrace.println(" " );
  	  	    outTrace.println( "final askers loan given: ");
  	  	    mean = StatUtils.mean(finalMeanLoanAskers);
  	  	    outTrace.println( "mean: " + mean);
  	  	    outTrace.println( "stdev: " + Math.sqrt(StatUtils.variance(finalMeanLoanAskers, mean)));
  	  	    outTrace.println(" " );
  	  	    
  	  	    outTrace.println( "final bidders loan: ");
  	  	    mean = StatUtils.mean(finalMeanLoanBidders);
  	  	    outTrace.println( "mean: " + mean);
  	  	    outTrace.println( "stdev: " + Math.sqrt(StatUtils.variance(finalMeanLoanBidders, mean)));
  	  	    outTrace.println(" " );
  	  	    }
*/  	  	    
  	  	    
    	  	outTrace.flush();
    	  	outTrace.close();
    	  	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Loans getLoanMarket() {
		return loanMarket;
	}
}
