package agents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import agents.markets.Asset;
import agents.markets.Loans;
import doubleAuction.Auction;
import doubleAuction.offer.AskOffering;
import doubleAuction.offer.AskOfferingWithLoans;
import doubleAuction.offer.BidOffering;
import doubleAuction.offer.BidOfferingWithLoans;
import doubleAuction.offer.MarketType;
import doubleAuction.offer.Offering;

public class Agent {
	//public static int NUM_AGENTS; 
	public static boolean TRADE_ONLY_FULL_UNITS;
	public final static double UNIT = 0.1;
	public final static double MAXUNIT = 0.5;
	
	private final static double CONSUME_INSENSIBILITY = 1E-5;
	private final static double ASSET_INSENSIBILITY = 1E-4;
	private final static double PMARGIN = 1.0;   //asset prices are samples from U(p-PMARGIN,p+PMARGIN
    
	private Random agRand = new Random();
	
	private int id;
	// optimism factor
	private double h;
	// a reference to the asset-market
	private Asset asset;
	// amount of consumption-good endowment (cash) still available
	private double consumEndow;
	// amount of asset endowment still available
	private double assetEndow;
	// Expected Value (E, Erwartungswert) of the Asset
	private double limitPriceAsset;

	public static int NUMMARKETS;
	
	private AskOffering[] currentAskOfferings;
	private BidOffering[] currentBidOfferings;
	
	private List<List<AskOffering>> bestAskOfferings;
	private List<List<BidOffering>> bestBidOfferings;
	
	private boolean highlighted;

	// a reference to the loan-market (bonds)
	private Loans loans;
	private int NUMLOANS;
	private double[] jJ, limitPricesLoansBuy, limitPricesLoansSell;
	
	private double[] expectJ;
	// the amount of unpledged assets, this amount can be sold or pledged when selling bonds
	private double freeAssetEndow;
	// the amount of loans taken from other agents
	private double[] loanTaken;
	// the amount of loans given to other agents
	private double[] loanGiven;

	private static final double QMARGIN = 1.0; // loan prices are samples from
										// U(q-PMARGIN,q+PMARGIN

	public Agent(int id, double h, double consumEndow, double assetEndow, Asset asset, Loans loans ) {
		this.id = id;
		this.h = h;
		this.consumEndow = consumEndow; 
		this.assetEndow = assetEndow;
		this.asset = asset;
		this.highlighted = false;
		this.loans = loans;
		this.jJ = loans.getJ();
		NUMLOANS = Loans.NUMLOANS;
		
		init();
	}
	
	private void init() {
		limitPricesLoansBuy = new double[NUMLOANS];
		limitPricesLoansSell = new double[NUMLOANS];
		detLimitPricesLoansSeller();
		detLimitPriceAsset();
		
		loanTaken = new double[NUMLOANS];
		loanGiven = new double[NUMLOANS];
		expectJ = new double[NUMLOANS];
		for (int i = 0; i < NUMLOANS; i++) {
			loanTaken[i] = 0;
			loanGiven[i] = 0;
			expectJ[i] = h * (loans.getJ()[i] - asset.getPD()) + asset.getPD();
		}
		freeAssetEndow = assetEndow;
	}
	
	public double[] getLimitPricesLoansBuy() {
		return limitPricesLoansBuy;
	}

	public double[] getLimitPricesLoansSell() {
		return limitPricesLoansSell;
	}

	public double[] getLoanGiven() {
		return loanGiven;
	}

	public double getFreeAssetEndow() {
		return freeAssetEndow;
	}

	public void reset( double consumEndow, double assetEndow ) {
		this.consumEndow = consumEndow; 
		this.assetEndow = assetEndow;
		this.highlighted = false;
		detLimitPriceAsset();
		init();
	}
	
	public boolean isHighlighted() {
		return highlighted;
	}

	public void setHighlighted(boolean highlighted) {
		this.highlighted = highlighted;
	}
	
	public int getId() {
		return id;
	}

	public double getConumEndow()  {
		return consumEndow;
	}
	
	public double getAssetEndow()  {
		return assetEndow;
	}
	
	public double getH()  {
		return h;
	}
	
	public double getLimitPriceAsset() {
		return limitPriceAsset;
	}

	public void addCurrentOfferingsToBestOfferings() {
		if ( null == this.bestAskOfferings ) {
			this.bestAskOfferings = new ArrayList<>();
			this.bestBidOfferings = new ArrayList<>();
			
			for ( int i = 0; i < Auction.NUMMARKETS; ++i ) {
				this.bestAskOfferings.add( new ArrayList<>() );
				this.bestBidOfferings.add( new ArrayList<>() );
			}
		}
			
		for ( int i = 0; i < Auction.NUMMARKETS; ++i ) {
			AskOffering ask = this.currentAskOfferings[ i ];
			
			if ( null != ask ) {
				Iterator<AskOffering> bestAsksMarket = this.bestAskOfferings.get( i ).iterator();
				boolean dominates = true;
				
				while ( bestAsksMarket.hasNext() ) {
					AskOffering askMarket = bestAsksMarket.next();
					
					if ( askMarket.dominates( ask ) ) {
						dominates = false;
						break;
					} else if ( ask.dominates( askMarket ) ){
						bestAsksMarket.remove();
					}
				}
				
				if ( dominates ) {
					this.bestAskOfferings.get( i ).add( ask );
				}
			}
			
			BidOffering bid = this.currentBidOfferings [ i ];
			
			if ( null != bid ) {
				Iterator<BidOffering> bestBidMarket = this.bestBidOfferings.get( i ).iterator();
				boolean dominates = true;
				
				while ( bestBidMarket.hasNext() ) {
					BidOffering bidMarket = bestBidMarket.next();
					
					if ( bidMarket.dominates( bid ) ) {
						dominates = false;
						break;
					} else if ( bid.dominates( bidMarket ) ){
						bestBidMarket.remove();
					}
				}
				
				if ( dominates ) {
					this.bestBidOfferings.get( i ).add( bid );
				}
			}
		}
	}
	
	public void calcNewOfferings() {
		this.currentAskOfferings = calcAskOfferings();
		this.currentBidOfferings = calcBidOfferings();
	}
	
	public AskOffering[] calcAskOfferings() {
		int numTrials;
		int MAXTRIALS = 500;
		double assetAmount;

		double minP = getPMin();
		double maxP = getPMax();
		double minQ;
		double maxQ;
		double pa, qa; // assetPrice, loanPrice

		AskOffering[] askOfferings = new AskOffering[ NUMMARKETS ];
		
		// market type 0: generate an ask offer for market m=0: sell an asset
		// against cash
		if (freeAssetEndow > 0 && maxP > limitPriceAsset) {
			// draw a uniform random asset price from [limitPriceAsset,PU]
			// intersect [minP,maxP]
			double r = agRand.nextDouble();
			
			// if improvement possible
			if ( limitPriceAsset > minP ) {
				// sell asset at least for limitPriceAsset and maximum in a range up to how much the optimism-factor h allows it
				pa = limitPriceAsset + r * (maxP - limitPriceAsset);
			} else {
				// otherwise bid randomly in random range between min and max
				pa = minP + r * (maxP - minP);
			}
			
			askOfferings[0] = new AskOffering(pa, Math.min(UNIT, freeAssetEndow), this, 0, MarketType.ASSET_AGAINST_CASH );
			
		} else {
			// agent has no free assets, or cannot offer at current price level
			askOfferings[0] = null;
		}
		
		// market type 1: find an ask offer for market m=1 to NUMLOANS: sell an
		// asset against loan of type j=m-1
		// draw random asset and loan price (pa,qa) uniformly from
		// M2={(p,q)|p-limitPriceAsset+(p/q)*(expectJ[j]-q) >= 0},
		// i.e. prices that guarantee a non-negative utility for agent h
		// changed at 08/17/2011: error in distribution - bias towards uniform q
		// which is not true

		if ( Loans.ASSETFORLOANMARKET ) {
			for (int j = 0; j < NUMLOANS; j++) {
				minQ = 0.0;
				maxQ = jJ[j];
				minP = limitPriceAsset;
				maxP = 1.0;
				
				if (freeAssetEndow <= 0) {
					askOfferings[j + 1] = null;
				} else if (minQ > 0 && (limitPriceAsset + (maxP / minQ) * expectJ[j]) <= 0) {
					askOfferings[j + 1] = null;
				} else {
					numTrials = MAXTRIALS;
					//initial asset price and loan price such that we enter always the "while"
					qa = minQ + agRand.nextDouble() * (maxQ - minQ);
					pa = minP + agRand.nextDouble() * (maxP - minP);
					while ((numTrials > 0)
							&& (-limitPriceAsset + (pa / qa) * expectJ[j] < 0)) {
						// negative utility and we may try another draw from
						// random prices
						pa = minP + agRand.nextDouble() * (maxP - minP);
						qa = minQ + agRand.nextDouble() * (maxQ - minQ);
						numTrials--;
					}
					
					if (numTrials > 0) {
						// found asset and loan price with non-negative utility
						assetAmount = Math.min(UNIT, freeAssetEndow);
						askOfferings[j + 1] = new AskOfferingWithLoans(pa,
								assetAmount, qa, j, loans.getJ()[j],
								assetAmount * pa / qa, this, j + 1, MarketType.ASSET_AGAINST_LOAN );
					} else {
						System.out
								.println("in calcAskOfferings: after "
										+ MAXTRIALS
										+ " trials no (pa,qa) with positive utility found");
						askOfferings[j + 1] = null;
					}
				}
			}
		}
		
		// market type 2: find an ask offer for market m=NUMLOANS+1 to
		// 2*NUMLOANS: give (buy) a loan of type j=m-NUMLOANS-1
		// draw random loan price qa from Uniform(0,expectJ[j]),
		// i.e. prices that guarantee a non-negative utility for agent h

		if ( Loans.LOANMARKET) {
			for (int j = 0; j < NUMLOANS; j++) {
				// loop on loan types
				minQ = getQMin(j);
				maxQ = getQMax(j);

				if (consumEndow > 0 && minQ < expectJ[j]) {
					// qa = agRand.nextDouble()*expectJ[j];
					qa = minQ + agRand.nextDouble()
							* (Math.min(maxQ, expectJ[j]) - minQ);
					askOfferings[1 + NUMLOANS + j] = new AskOfferingWithLoans(
							0, 0, qa, j, loans.getJ()[j], consumEndow / qa,
							this, 1 + NUMLOANS + j, MarketType.LOAN_AGAINST_CASH );
				} else {
					// agent has nothing to borrow
					askOfferings[1 + NUMLOANS + j] = null;
				}
			}
		}
		
		return askOfferings;
	}

	public BidOffering[] calcBidOfferings() {
		int numTrials;
		int MAXTRIALS = 200;
		double assetAmount;
		double minP = getPMin();
		double maxP = getPMax();
		double minQ;
		double maxQ;
		double pb, qb;

		BidOffering[] bidOfferings = new BidOffering[ NUMMARKETS ];
		
		// market type 0: generate an bid offer for market m=0: buy an asset against cash
		if (consumEndow > 0) {
			if (minP > limitPriceAsset) { // agent cannot buy at current price level
				bidOfferings[0] = null;
				
			} else {
				double r = agRand.nextDouble();
				if (maxP > limitPriceAsset) {
					pb = minP + r * (limitPriceAsset - minP);
				} else {
					pb = minP + r * (maxP - minP);
				}
				
				assetAmount = Math.min(UNIT, consumEndow / pb);
				bidOfferings[0] = new BidOffering(pb, assetAmount, this, 0, MarketType.ASSET_AGAINST_CASH );
			}

			for (int i = 1; i < bidOfferings.length; i++)
				bidOfferings[i] = null;

		// no cash or: try to buy with loans => no bidding in asset->loan market when agent still has cash!
		// => when having asset->loan market then initially the simulation will converge towards the equilibrium
		// of the asset->cash market only
		} else {
			bidOfferings[0] = null;
			// market type 1 (nota bene: only when agent has no more cash): find
			// a bid offer for market m=1 to NUMLOANS: buy an asset against loan
			// of type j=m-1
			// draw random asset and loan price (pb,qb) uniformly from
			// M2={(p,q)|limitPriceAsset-p + (p/q)*(q-expectJ[j]) >= 0},
			// i.e. prices that guarantee a non-negative utility for agent h

			for (int j = 0; j < NUMLOANS; j++) {
				// loop an markets/loan types
				numTrials = MAXTRIALS;
				bidOfferings[j + 1] = null;
				minP = 0.2;
				maxP = limitPriceAsset;
				minQ = 0.0;
				maxQ = jJ[j];

				double ratio = minP / maxQ;
				double utility = limitPriceAsset - ( ratio * expectJ[j] );

				if ( utility > 0) {
					for (int i = numTrials; i > 0; i--) {
						pb = minP + agRand.nextDouble() * (maxP - minP);
						qb = minQ + agRand.nextDouble() * (maxQ - minQ);

						ratio = pb / qb;
						utility = limitPriceAsset - ( ratio * expectJ[j] );

						if ( utility >= 0 ) {
							// nonnegative utility
							// try to buy UNIT assets
							double loanAmount = pb * UNIT / qb;
							double lowerEndLoanPrice = Math.max( 0, pb * UNIT / ( freeAssetEndow + UNIT ) );
							// minimum loan price for fulfilling collateral
							// constraint
							if (qb >= lowerEndLoanPrice) {
								// nice: bid offer fulfills the collateral
								// requirement
								bidOfferings[j + 1] = new BidOfferingWithLoans(
										pb, UNIT, qb, j, loans.getJ()[j],
										loanAmount, this, j + 1, MarketType.ASSET_AGAINST_LOAN );
								break;
							} else {
								// not enough collateral for loan for UNIT
								// assets: try less
								if ((pb > qb) && (freeAssetEndow > 0)) {
									// quite nice: bid offer fulfills the
									// collateral requirement, but the
									// computation of the
									// limitAssetAmount requires some agent
									// intelligence
									assetAmount = qb * freeAssetEndow
											/ (pb - qb);
									if (qb < Math.max(0, pb * assetAmount
											/ (freeAssetEndow + assetAmount))
											- CONSUME_INSENSIBILITY)
										System.err
												.println("in calcBidOfferings: violation of collateral constraint");
									bidOfferings[j + 1] = new BidOfferingWithLoans(
											pb, assetAmount, qb, j,
											loans.getJ()[j], pb * assetAmount
													/ qb, this, j + 1, MarketType.ASSET_AGAINST_LOAN );
									break;
								}
							}
						}
						// else
						// System.err.println("in calcBidOfferings: negative utility gain");
					}
				}
				if (numTrials == 0)
					System.out
							.println("agent "
									+ h
									+ " didn't find any bid offering for market 1 after "
									+ MAXTRIALS + " trials");
			}
		}
		
		return bidOfferings;
	}

	/*
	public AskOffering[] calcAskOfferings()  {
		//draw a random price uniformly out of [minP,maxP] intersect [limitPriceAsset,pU]
		double minP = getPMin();
		double maxP = getPMax();
		double assetPrice = 0.0;
		AskOffering[] askOffering = new AskOffering[ NUMMARKETS ];


		if (maxP < limitPriceAsset)  //agent cannot offer at current price level 
			return new AskOffering[] { actAskOffer };
	
		// check if there is enough endowment left...
		if (TRADE_ONLY_FULL_UNITS ) {
			if (assetEndow < UNIT)
				return new AskOffering[] { actAskOffer };
		} else  {
			if (assetEndow < 0)
				return new AskOffering[] { actAskOffer };
		}
		
		// "...agents who always make bids which improve their utility but otherwise bid randomly."
		if ( limitPriceAsset > minP )
			assetPrice = limitPriceAsset + agRand.nextDouble() * ( maxP - limitPriceAsset );
		else
			assetPrice = minP + agRand.nextDouble() * ( maxP - minP );
			
		// at this point we know there is enough endowment left!
		if (false == TRADE_ONLY_FULL_UNITS) {
			actAskOffer = new AskOffering(assetPrice, this, 0, MarketType.ASSET_AGAINST_CASH );	
		} else  {
			actAskOffer = new AskOffering(assetPrice, Math.min(assetEndow,MAXUNIT), this, 0, MarketType.ASSET_AGAINST_CASH );	
		}
		
		
		if (assetEndow > 0 && maxP > limitPriceAsset) {
			// draw a uniform random asset price from [limitPriceAsset,PU]
			// intersect [minP,maxP]
			double r = agRand.nextDouble();
			
			// if improvement possible
			if ( limitPriceAsset > minP ) {
				// sell asset at least for limitPriceAsset and maximum in a range up to how much the optimism-factor h allows it
				assetPrice = limitPriceAsset + r * (maxP - limitPriceAsset);
			} else {
				// otherwise bid randomly in random range between min and max
				assetPrice = minP + r * (maxP - minP);
			}
			
			askOffering[0] = new AskOffering(assetPrice, Math.min(UNIT, assetEndow), this, 0, MarketType.ASSET_AGAINST_CASH );
			
		} else {
			// agent has no free assets, or cannot offer at current price level
			askOffering[0] = null;
		}
		
		return askOffering;
	}
	*/
	
	/*
	public BidOffering[] calcBidOfferings()  {
		double minP = getPMin();
		double maxP = getPMax();
		double assetPrice;
		BidOffering[] bidOffering = new BidOffering[ NUMMARKETS ];
		
		
		if (minP > limitPriceAsset)  //agent cannot offer at current price level 
			return new BidOffering[] { actBidOffer };
	
		// "...agents who always make bids which improve their utility but otherwise bid randomly."
		if ( limitPriceAsset < maxP ) {
			// draw a random price in the range betwen minP (the value of the asset tomorrow in Down) and limitPriceAsset (the value of the asset this agent expects it to be tomorrow)
			assetPrice = minP + agRand.nextDouble() * ( limitPriceAsset - minP );
		} else {
			assetPrice = minP + agRand.nextDouble() * ( maxP - minP );
		}
		
		if (TRADE_ONLY_FULL_UNITS) {
			if (assetPrice * UNIT <= consumEndow )
				actBidOffer = new BidOffering(assetPrice, this, 0, MarketType.ASSET_AGAINST_CASH );
			else
				actBidOffer = null;
		} else {
			if (consumEndow > 0)
				actBidOffer = new BidOffering(assetPrice, Math.min( consumEndow / assetPrice, MAXUNIT ), this, 0, MarketType.ASSET_AGAINST_CASH );
			else
				actBidOffer = null;			
		}
		
		return new BidOffering[] { actBidOffer };
		
		
		if (consumEndow > 0 && minP <= limitPriceAsset) {
			double r = agRand.nextDouble();
			if (maxP > limitPriceAsset) {
				assetPrice = minP + r * (limitPriceAsset - minP);
			} else {
				assetPrice = minP + r * (maxP - minP);
			}
			
			double assetAmount = Math.min(UNIT, consumEndow / assetPrice);
			bidOffering[0] = new BidOffering(assetPrice, assetAmount, this, 0, MarketType.ASSET_AGAINST_CASH );
		}
		
		return bidOffering;
	}
	*/
	
	/*
	public boolean execTransaction(Offering[] match, boolean first)  {
		if (match[0].getAgent()==this)  {
		//agent is asker
			return execSellTransaction(match,true);
		} else {
			return execBuyTransaction(match,true);		
		}		
	}
	*/
	
	public boolean execSellTransaction(Offering[] match, boolean first)  {
		//first is true, if bid was before myAsk: is used for a double dispatch pattern
		AskOffering myAsk = (AskOffering)match[0]; 
		BidOffering bid = (BidOffering)match[1];
		double toSell = Math.min(myAsk.getAssetAmount(), bid.getAssetAmount()); 
		assetEndow -= toSell;
		
		if (assetEndow < ASSET_INSENSIBILITY)  {
			if (assetEndow < -ASSET_INSENSIBILITY)
				System.err.println("error in agent.execAskTransaction: wants to sell too much");
			else
				assetEndow = 0;
		}
		
		if (first)  {
			consumEndow += bid.getFinalAssetPrice()*toSell;
			bid.getAgent().execBuyTransaction(match, !first);
		} else {
			consumEndow += myAsk.getFinalAssetPrice()*toSell;
		}
		
		// need to reset when match to force a recalculation of offers
		// INFO: don't really need to reset because this will result in a successful TX which
		// will be returned immediately. Best Offerings will be cleared bevore next TX
		//this.clearBestOfferings();
		
		return true;
	}
	
	public boolean execBuyTransaction(Offering[] match, boolean first)  {
		//first is true, if ask was before myBid: is used for a double dispatch pattern 	
		BidOffering myBid = (BidOffering)match[1];
		AskOffering ask = (AskOffering)match[0];
		double toBuy = Math.min(myBid.getAssetAmount(), ask.getAssetAmount());	
		//potential improvement (accelerates auction?): bidding agent could buy more with his consume endowment 
		//since ask asset price <= bid asset price
		assetEndow += toBuy;
	
		if (first)  {
			consumEndow -= ask.getFinalAssetPrice()*toBuy;
			ask.getAgent().execSellTransaction(match, !first);
		}
		else {
			consumEndow -= myBid.getFinalAssetPrice()*toBuy;	
		}
		
		if (consumEndow < CONSUME_INSENSIBILITY)  {
	
			if (consumEndow < -CONSUME_INSENSIBILITY)
				System.err.println("error in agent.execBidTransaction: wants to buy too much");
			else 
				consumEndow = 0;
		}
		
		// need to reset when match to force a recalculation of offers
		// INFO: don't really need to reset because this will result in a successful TX which
		// will be returned immediately. Best Offerings will be cleared bevore next TX
		//this.clearBestOfferings();
		
		return true;
	}
	
	public void clearBestOfferings() {
		if ( null == this.bestAskOfferings ) {
			return;
		}
		
		for ( int i = 0; i < NUMMARKETS; ++i ) {
			this.bestAskOfferings.get( i ).clear();
			this.bestBidOfferings.get( i ).clear();
		}
	}
	
	public AskOffering[] getCurrentAskOfferings() {
		return currentAskOfferings;
	}

	public BidOffering[] getCurrentBidOfferings() {
		return currentBidOfferings;
	}

	public List<List<AskOffering>> getBestAskOfferings() {
		return bestAskOfferings;
	}

	public List<List<BidOffering>> getBestBidOfferings() {
		return bestBidOfferings;
	}
	
	private void copyBestOfferingsTo( Agent copy ) {
		if ( null != this.bestAskOfferings ) {
			copy.bestAskOfferings = new ArrayList<List<AskOffering>>();
			
			for ( List<AskOffering> marketOfferings : this.bestAskOfferings ) {
				// note: shallow-copy is ok, offerings won't change
				copy.bestAskOfferings.add( new ArrayList<AskOffering>( marketOfferings ) );
			}
		}
		
		if ( null != this.bestBidOfferings ) {
			copy.bestBidOfferings = new ArrayList<List<BidOffering>>();
			
			for ( List<BidOffering> marketOfferings : this.bestBidOfferings ) {
				// note: shallow-copy is ok, offerings won't change
				copy.bestBidOfferings.add( new ArrayList<BidOffering>( marketOfferings ) );
			}
		}
	}
	
	private void detLimitPriceAsset() {
		// calculate expected value (E, Erwartungswert) of the Asset
		
		// derived from the formula: h*pU+(1-h)*pD
		limitPriceAsset = (asset.getPU() - asset.getPD())*h + asset.getPD();
	}
	
	private double getPMax() {
		double refP = asset.getP();
		return Math.min(asset.getPU(), refP + PMARGIN);
	}
	
	private double getPMin() {
		double refP = asset.getP();
		return Math.max(asset.getPD(), refP - PMARGIN);
	}
	
	public boolean execTransaction(Offering[] match, boolean first) {
		// when first==true, this method distributes actions to all agents
		// participating at the transaction
		// otherwise, method executes only action on this agent
		boolean result = false;
		if (match[0].getAgent() == this) {
			// agent is asker: sell asset
			result = execSellTransaction(match);
			if (first)
				match[1].getAgent().execTransaction(match, false);
			else if (match[2] != null)
				match[2].getAgent().execTransaction(match, false);
		} else if (match[1].getAgent() == this) {
			// agent is bidder: buy asset
			result = execBuyTransaction(match);
			if (first)
				match[0].getAgent().execTransaction(match, false);
			else if (match[2] != null)
				match[2].getAgent().execTransaction(match, false);
		} else if (match[2].getAgent() == this) {
			// agent is loan asker: borrow/give cash as a loan to match[1].agent
			return execLoanTransaction(match);
		}
		return result;

	}

	public boolean execLoanTransaction(Offering[] match) {
		BidOfferingWithLoans bid = (BidOfferingWithLoans) match[1];
		double loanAmount = bid.getLoanAmount();
		consumEndow -= loanAmount * bid.getFinalLoanPrice();
		loanGiven[bid.getLoanType()] += loanAmount;

		if (consumEndow < CONSUME_INSENSIBILITY) {
			if (consumEndow < -CONSUME_INSENSIBILITY)
				System.err.println("error in agent.execLoanTransaction: wants to loan too much");
			else
				consumEndow = 0;
		}

		// need to reset when match to force a recalculation of offers
		// INFO: don't really need to reset because this will result in a successful TX which
		// will be returned immediately. Best Offerings will be cleared bevore next TX
		//this.clearBestOfferings();
		
		return true;
	}

	public boolean execSellTransaction(Offering[] match) {
		AskOffering myAsk = (AskOffering) match[0];
		BidOffering bid = (BidOffering) match[1];
		double toSell = Math.min(myAsk.getAssetAmount(), bid.getAssetAmount());

		// adjust askers amount
		myAsk.setAssetAmount(toSell);

		assetEndow -= toSell;
		freeAssetEndow -= toSell;

		if (assetEndow < ASSET_INSENSIBILITY) {
			if (assetEndow < -ASSET_INSENSIBILITY)
				System.err.println("error in agent.execAskTransaction: wants to sell too much");
			else
				assetEndow = 0;
		}

		if (freeAssetEndow < ASSET_INSENSIBILITY) {
			if (freeAssetEndow < -ASSET_INSENSIBILITY)
				System.err.println("error in agent.execAskTransaction: wants to sell too much");
			else
				freeAssetEndow = 0;
		}

		if ( MarketType.ASSET_AGAINST_CASH == myAsk.getMarketType() ) {
			// asset against cash
			consumEndow += myAsk.getFinalAssetPrice() * toSell;

			if (consumEndow < CONSUME_INSENSIBILITY) {
				if (consumEndow < -CONSUME_INSENSIBILITY)
					System.err.println("error in agent.execAskTransaction: wants to loan too much");
				else
					consumEndow = 0;
			}
		} else {
			// market type 1: asset against loan
			double loanAmount = toSell * myAsk.getFinalAssetPrice() / ((AskOfferingWithLoans) myAsk).getFinalLoanPrice();

			loanGiven[((AskOfferingWithLoans) myAsk).getLoanType()] += loanAmount;
		}
		
		// need to reset when match to force a recalculation of offers
		// INFO: don't really need to reset because this will result in a successful TX which
		// will be returned immediately. Best Offerings will be cleared bevore next TX
		//this.clearBestOfferings();
		
		return true;
	}

	public boolean execBuyTransaction(Offering[] match) {
		AskOffering ask = (AskOffering) match[0];
		BidOffering myBid = (BidOffering) match[1];

		double toBuy = Math.min(myBid.getAssetAmount(), ask.getAssetAmount());
		double loanAmount = 0;

		// adjust bidders amounts
		myBid.setAssetAmount(toBuy);

		if ( MarketType.ASSET_AGAINST_LOAN == myBid.getMarketType() ) {
			// adjust loan amount
			loanAmount = toBuy * myBid.getFinalAssetPrice()
					/ ((BidOfferingWithLoans) myBid).getFinalLoanPrice();
			((BidOfferingWithLoans) myBid).setLoanAmount(loanAmount);

		}
		
		assetEndow += toBuy;
		freeAssetEndow += toBuy;

		if ( MarketType.ASSET_AGAINST_CASH == myBid.getMarketType() ) {
			// asset against cash
			consumEndow -= myBid.getFinalAssetPrice() * toBuy;
			if (consumEndow < CONSUME_INSENSIBILITY) {
				if (consumEndow < -CONSUME_INSENSIBILITY)
					System.err.println("error in agent.execBidTransaction: wants to buy too much");
				else
					consumEndow = 0;
			}
		} else {
			// asset against loan
			freeAssetEndow -= loanAmount;
			loanTaken[((BidOfferingWithLoans) myBid).getLoanType()] += loanAmount;

			if (freeAssetEndow < ASSET_INSENSIBILITY) {
				if (freeAssetEndow < -ASSET_INSENSIBILITY)
					System.err.println("error in agent.execBidTransaction: wants to collaterate too much");
				else
					freeAssetEndow = 0;
			}
		}

		// need to reset when match to force a recalculation of offers
		// INFO: don't really need to reset because this will result in a successful TX which
		// will be returned immediately. Best Offerings will be cleared bevore next TX
		//this.clearBestOfferings();
		
		return true;
	}

	public double[] getLoanTaken() {
		return loanTaken;
	}

	public boolean matchesBid(double pb, double qb, BidOfferingWithLoans offer) {
		// pb and qb are offered prices by another asking agent: matching occurs
		// when this agent
		// as a bidder would achieve positive utility
		if (limitPriceAsset - (pb / qb) * expectJ[offer.getLoanType()] > 0) {
			// nonnegative utility
			// try to buy UNIT assets
			double loanAmount = pb * UNIT / qb;
			double lowerEndLoanPrice = Math.max(0, pb * UNIT
					/ (freeAssetEndow + UNIT));
			// minimum loan price for fulfilling collateral constraint
			if (qb >= lowerEndLoanPrice) {
				// nice: bid offer fulfills the collateral requirement
				offer.setAssetAmount(UNIT);
				offer.setAssetPrice(pb);
				offer.setLoanPrice(qb);
				offer.setLoanAmount(loanAmount);
				return true;

			} else {
				// not enough collateral for loan for UNIT assets: try less
				if ((pb > qb) && (freeAssetEndow > 0)) {
					// quite nice: bid offer fulfills the collateral
					// requirement, but the computation of the
					// limitAssetAmount requires some agent intelligence
					double assetAmount = qb * freeAssetEndow / (pb - qb);
					if (qb < Math.max(0, pb * assetAmount
							/ (freeAssetEndow + assetAmount))
							- CONSUME_INSENSIBILITY)
						System.err
								.println("in Agent.matchesBid: violation of collateral constraint");
					offer.setAssetAmount(assetAmount);
					offer.setAssetPrice(pb);
					offer.setLoanPrice(qb);
					offer.setLoanAmount(pb * assetAmount / qb);
					return true;
				} else
					return false;
			}
		}
		
		return false;
	}

	public boolean matchesAsk(double pa, double qa, AskOfferingWithLoans offer) {
		// pa and qa are offered prices by another bidding agent: matching
		// occurs when this agent
		// as an asker would achieve positive utility
		if ((-limitPriceAsset + (pa / qa) * expectJ[offer.getLoanType()] > 0)
				&& (freeAssetEndow > 0)) {
			offer.setAssetAmount(Math.min(UNIT, freeAssetEndow));
			offer.setAssetPrice(pa);
			offer.setLoanPrice(qa);
			offer.setLoanAmount(pa * offer.getAssetAmount() / qa);
			return true;
		}
		return false;
	}

	@Override
	public Object clone() {
		Agent clone = new Agent(this.id, this.h, this.consumEndow, this.assetEndow, this.asset, this.loans );
		clone.freeAssetEndow = this.freeAssetEndow;
		clone.loanGiven = Arrays.copyOf( this.loanGiven, this.loanGiven.length );
		clone.loanTaken = Arrays.copyOf( this.loanTaken, this.loanTaken.length );
		
		this.copyBestOfferingsTo( clone );
		
		return clone;
	}
	
	protected double getQMax(int loanType) {
		double refQ = loans.getLoanPrices()[loanType];
		
		return Math.min(jJ[loanType], refQ + QMARGIN);
	}

	protected double getQMin(int loanType) {
		double refQ = loans.getLoanPrices()[loanType];

		return Math.max(0, refQ - QMARGIN);
	}

	protected void detLimitPricesLoansBuyer() {
		double pU = asset.getPU();
		double pD = asset.getPD();
		double p = asset.getP();

		// limit prices buy
		for (int j = 0; j < NUMLOANS; j++) {
			limitPricesLoansBuy[j] = p * ((jJ[j] - pD) * h + pD)
					/ ((pU - pD) * h + pD);
		}
	}

	protected void detLimitPricesLoansSeller() {
		double pD = asset.getPD();

		// limit prices sell
		for (int j = 0; j < NUMLOANS; j++)
			limitPricesLoansSell[j] = (jJ[j] - pD) * h + pD;
	}
}
