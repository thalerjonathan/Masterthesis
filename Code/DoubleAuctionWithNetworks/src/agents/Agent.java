package agents;

import agents.markets.Markets;
import doubleAuction.offer.AskOffering;
import doubleAuction.offer.BidOffering;
import doubleAuction.offer.MarketType;

public class Agent {
	private final static double PMARGIN = 1.0; 

	private Markets markets;
	
	private int id;
	// optimism factor
	private double h;
	// amount of consumption-good endowment (cash) still available
	private double cashEndow;
	// amount of asset endowment still available
	private double assetEndow;
	// Expected Value (E, Erwartungswert) of the Asset
	private double limitPriceAsset;
	// Expected Value (E, Erwartungswert) of the loan
	private double limitPriceLoan;
	
	// the amount of unpledged assets, this amount can be sold or pledged when selling bonds
	private double freeAssetEndow;
	// the amount of loans taken from other agents
	private double loanTaken;
	// the amount of loans given to other agents
	private double loanGiven;
	
	// NOTE: holds the offerings of the current sweeping-round
	private AskOffering[] currentAskOfferings;
	private BidOffering[] currentBidOfferings;
	
	// NOTE: linear order on offerings on each market thus there can only be one BEST offering on each market for buy/sell
	private AskOffering[] bestAskOfferings;
	private BidOffering[] bestBidOfferings;
	
	private boolean highlighted;

	public Agent(int id, double h, double consumEndow, double assetEndow, Markets markets ) {
		this.id = id;
		this.h = h;
		this.markets = markets;
		
		this.reset( consumEndow, assetEndow );
	}
	
	public void reset( double consumEndow, double assetEndow ) {
		this.cashEndow = consumEndow; 
		this.assetEndow = assetEndow;
		this.highlighted = false;

		this.loanTaken = 0;
		this.loanGiven = 0;
		this.limitPriceAsset = h * markets.getAsset().getPU() + ( 1.0 - h ) * markets.getAsset().getPD();
		this.limitPriceLoan = h * Math.min( markets.getAsset().getPU(), markets.getLoans().getFaceValue() ) + 
				( 1.0 - h ) * Math.min( markets.getAsset().getPD(), markets.getLoans().getFaceValue() );
		
		this.freeAssetEndow = assetEndow;
		
		this.currentAskOfferings = new AskOffering[ Markets.NUMMARKETS ];
		this.currentBidOfferings = new BidOffering[ Markets.NUMMARKETS ];
		
		this.bestAskOfferings = new AskOffering[ Markets.NUMMARKETS ];
		this.bestBidOfferings = new BidOffering[ Markets.NUMMARKETS ];
	}

	public double getLimitPriceLoans() {
		return limitPriceLoan;
	}

	public double getLoanGiven() {
		return loanGiven;
	}

	public double getFreeAssetEndow() {
		return freeAssetEndow;
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
		return cashEndow;
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
		for ( int i = 0; i < Markets.NUMMARKETS; ++i ) {
			AskOffering ask = this.currentAskOfferings[ i ];
			if ( null != ask ) {
				if ( null == this.bestAskOfferings[ i ] || 
						ask.dominates( this.bestAskOfferings[ i ] ) ) {
					this.bestAskOfferings[ i ] = ask;
				}
			}
			
			BidOffering bid = this.currentBidOfferings [ i ];
			if ( null != bid ) {
				if ( null == this.bestBidOfferings[ i ] || 
						bid.dominates( this.bestBidOfferings[ i ] ) ) {
					this.bestBidOfferings[ i ] = bid;
				}
			}
		}
	}
	
	public void calcNewOfferings() {
		this.calcAskOfferings( this.currentAskOfferings );
		this.calcBidOfferings( this.currentBidOfferings );
	}
	
	private static double randomRange( double min, double max ) {
		return min + Math.random() * ( max - min );
	}
	
	private void calcBidOfferings( BidOffering[] offerings ) {		
		double pD = getPMin();
		double pU = getPMax();
		double V = markets.getLoans().getFaceValue();
		
		if ( this.cashEndow > 0 ) {
			double assetPrice = randomRange( Math.min( pD, limitPriceAsset), limitPriceAsset );
			double assetAmount = Math.min( Markets.TRADING_UNIT, this.cashEndow / assetPrice );
			
			offerings[ MarketType.ASSET_CASH.ordinal() ] = new BidOffering( assetPrice, assetAmount, this, MarketType.ASSET_CASH );
		
		} else {
			offerings[ MarketType.ASSET_CASH.ordinal() ] = null;
		}
		
		if ( this.markets.isLoanMarket() && this.cashEndow > 0 ) {
			double loanPrice = randomRange( Math.min(Math.min( pD, V ), limitPriceLoan ), limitPriceLoan );
			double loanAmount = Math.min( this.cashEndow / loanPrice, Markets.TRADING_UNIT );
			
			offerings[ MarketType.LOAN_CASH.ordinal() ] = new BidOffering( loanPrice, loanAmount, this, MarketType.LOAN_CASH );
		
		} else {
			offerings[ MarketType.LOAN_CASH.ordinal() ] = null;
		}
		
		if ( this.markets.isABM() && this.freeAssetEndow > 0 ) {
			double price = randomRange( pD / Math.min( pU, V ), limitPriceAsset / limitPriceLoan );
			// TODO: adjust amount
			double amount = Math.min( Markets.TRADING_UNIT, this.freeAssetEndow );
				
			offerings[ MarketType.ASSET_LOAN.ordinal() ] = new BidOffering( price, amount, this, MarketType.ASSET_LOAN );
			
		} else {
			offerings[ MarketType.ASSET_LOAN.ordinal() ] = null;
		}
	}
	
	private void  calcAskOfferings( AskOffering[] offerings ) {
		double pD = getPMin();
		double pU = getPMax();
		double V = markets.getLoans().getFaceValue();
		
		// NOTE: maxP should ALWAYS be > limitPriceAsset
		if ( this.freeAssetEndow > 0 && pU > limitPriceAsset ) {
			double assetPrice = randomRange( limitPriceAsset, Math.max( pU, limitPriceAsset ) );
			double assetAmount = Math.min( Markets.TRADING_UNIT, this.freeAssetEndow );
			
			offerings[ MarketType.ASSET_CASH.ordinal() ] = new AskOffering( assetPrice, assetAmount, this, MarketType.ASSET_CASH );
		
		} else {
			offerings[ MarketType.ASSET_CASH.ordinal() ] = null;
		}

		if ( this.markets.isLoanMarket() && this.freeAssetEndow > 0 ) {
			double loanPrice = randomRange( limitPriceLoan, Math.max( Math.min( pU, V ), limitPriceLoan ) );
			double loanAmount = Math.min( this.freeAssetEndow, Markets.TRADING_UNIT );

			offerings[ MarketType.LOAN_CASH.ordinal() ] = new AskOffering( loanPrice, loanAmount, this, MarketType.LOAN_CASH );
			
		} else {
			offerings[ MarketType.LOAN_CASH.ordinal() ] = null;
		}
		
		if ( this.markets.isABM() && this.freeAssetEndow > 0 ) {
			double price = randomRange( limitPriceAsset / limitPriceLoan, pU / Math.min( pD, V ) );
			
			// TODO: adjust amount
			double amount = Math.min( Markets.TRADING_UNIT, this.freeAssetEndow );
			
			offerings[ MarketType.ASSET_LOAN.ordinal() ] = new AskOffering( price, amount, this, MarketType.ASSET_LOAN );
		
		} else {
			offerings[ MarketType.ASSET_LOAN.ordinal() ] = null;
		}
	}
	
	public void execSellTransaction( BidOffering buyOffer ) {
		// NOTE: executing a sell-transaction on this agent which means this agent is SELLING to the "other" agent
		// the Bid-Offer is the buy-offer of the "other" agent who is the buyer
		// the matching ask-offer is this agents ask-offer, thus its a sell transaction
		
		AskOffering sellOffer = this.currentAskOfferings[ buyOffer.getMarketType().ordinal() ];
		
		// NOTE: just for debugging purpose, should at this point never happen
		if ( sellOffer.getMarketType() != buyOffer.getMarketType() ) {
			System.err.println( "Warning: attempt of executing a buy- and Sell-Offer on different markets!" );
			return;
		}
		
		if ( MarketType.ASSET_CASH == sellOffer.getMarketType() ) {
			//double price = ( buyOffer.getPrice() + sellOffer.getPrice() ) / 2.0;
			
			this.assetEndow -= sellOffer.getAmount();
			this.freeAssetEndow -= sellOffer.getAmount();
			this.cashEndow += sellOffer.getAmount() * sellOffer.getPrice();
			
		} else if ( MarketType.LOAN_CASH == sellOffer.getMarketType() ) {
			
		} else if ( MarketType.ASSET_LOAN == sellOffer.getMarketType() ) {
			
		}
	}
	
	public void execBuyTransaction( AskOffering sellOffer ) {
		// NOTE: executing a buy-transaction on this agent which means this agent is BUYING from the "other" agent
		// the Ask-Offer is the sell-offer of the "other" agent who is the seller
		// the matching Bid-offer is this agents Bid-offer, thus its a buy transaction
		
		BidOffering buyOffer = this.currentBidOfferings[ sellOffer.getMarketType().ordinal() ];
		
		// NOTE: just for debugging purpose, should at this point never happen
		if ( buyOffer.getMarketType() != sellOffer.getMarketType() ) {
			System.err.println( "Warning: attempt of executing a buy- and Sell-Offer on different markets!" );
			return;
		}
		
		if ( MarketType.ASSET_CASH == buyOffer.getMarketType() ) {
			//double price = ( buyOffer.getPrice() + sellOffer.getPrice() ) / 2.0;

			this.assetEndow += buyOffer.getAmount();
			this.freeAssetEndow += buyOffer.getAmount();
			this.cashEndow -= buyOffer.getAmount() * buyOffer.getPrice();
			
		} else if ( MarketType.LOAN_CASH == buyOffer.getMarketType() ) {
			
		} else if ( MarketType.ASSET_LOAN == buyOffer.getMarketType() ) {
			
		}
	}
	
	/*
	public AskOffering[] calcAskOfferings() {
		int numTrials;
		int MAXTRIALS = 500;
		double assetAmount;

		double minP = getPMin();
		double maxP = getPMax();
		double minQ;
		double maxQ;
		double pa, qa; // assetPrice, loanPrice

		AskOffering[] askOfferings = new AskOffering[ Markets.NUMMARKETS ];
		
		// market type 0: generate an ask offer for market m=0: sell an asset
		// against cash
		if (freeAssetEndow > 0 && maxP > limitPriceAsset) {
			// draw a uniform random asset price from [limitPriceAsset,PU]
			// intersect [minP,maxP]
			double r = Math.random();

			// if improvement possible
			if ( limitPriceAsset > minP ) {
				// sell asset at least for limitPriceAsset and maximum in a range up to how much the optimism-factor h allows it
				pa = limitPriceAsset + r * (maxP - limitPriceAsset);
			} else {
				// otherwise bid randomly in random range between min and max
				pa = minP + r * (maxP - minP);
			}
			
			askOfferings[0] = new AskOffering(pa, Math.min(UNIT, freeAssetEndow), this, MarketType.ASSET_CASH );
			
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

		if ( this.markets.isABM() ) {
			minQ = 0.0;
			maxQ = markets.getLoans().getJ();
			minP = limitPriceAsset;
			maxP = 1.0;
			
			if (freeAssetEndow <= 0) {
				askOfferings[1] = null;
			} else if (minQ > 0 && (limitPriceAsset + (maxP / minQ) * expectJ) <= 0) {
				askOfferings[1] = null;
			} else {
				numTrials = MAXTRIALS;
				//initial asset price and loan price such that we enter always the "while"
				qa = minQ + Math.random() * (maxQ - minQ);
				pa = minP + Math.random() * (maxP - minP);
				while ((numTrials > 0)
						&& (-limitPriceAsset + (pa / qa) * expectJ< 0)) {
					// negative utility and we may try another draw from
					// random prices
					pa = minP + Math.random() * (maxP - minP);
					qa = minQ + Math.random() * (maxQ - minQ);
					numTrials--;
				}
				
				if (numTrials > 0) {
					// found asset and loan price with non-negative utility
					assetAmount = Math.min(UNIT, freeAssetEndow);
					askOfferings[1] = new AskOfferingWithLoans(pa,
							assetAmount, qa, this.markets.getLoans().getJ(),
							assetAmount * pa / qa, this, MarketType.ASSET_LOAN );
				} else {
					System.out
							.println("in calcAskOfferings: after "
									+ MAXTRIALS
									+ " trials no (pa,qa) with positive utility found");
					askOfferings[1] = null;
				}
			}
		}
		
		// market type 2: find an ask offer for market m=NUMLOANS+1 to
		// 2*NUMLOANS: give (buy) a loan of type j=m-NUMLOANS-1
		// draw random loan price qa from Uniform(0,expectJ[j]),
		// i.e. prices that guarantee a non-negative utility for agent h

		if ( this.markets.isLoanMarket() ) {
			// loop on loan types
			minQ = getQMin();
			maxQ = getQMax();

			if (consumEndow > 0 && minQ < expectJ) {
				// qa = agRand.nextDouble()*expectJ[j];
				qa = minQ +  Math.random()
						* (Math.min(maxQ, expectJ) - minQ);
				askOfferings[2] = new AskOfferingWithLoans(
						0, 0, qa, this.markets.getLoans().getJ(), consumEndow / qa,
						this, MarketType.LOAN_CASH );
			} else {
				// agent has nothing to borrow
				askOfferings[2] = null;
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

		BidOffering[] bidOfferings = new BidOffering[ Markets.NUMMARKETS ];
		
		// market type 0: generate an bid offer for market m=0: buy an asset against cash
		if (consumEndow > 0) {
			if (minP > limitPriceAsset) { // agent cannot buy at current price level
				bidOfferings[0] = null;
				
			} else {
				double r = Math.random();
				if (maxP > limitPriceAsset) {
					pb = minP + r * (limitPriceAsset - minP);
				} else {
					pb = minP + r * (maxP - minP);
				}
				
				assetAmount = Math.min(UNIT, consumEndow / pb);
				bidOfferings[0] = new BidOffering(pb, assetAmount, this, MarketType.ASSET_CASH );
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

			// loop an markets/loan types
			numTrials = MAXTRIALS;
			bidOfferings[1] = null;
			minP = 0.2;
			maxP = limitPriceAsset;
			minQ = 0.0;
			maxQ = markets.getLoans().getJ();

			double ratio = minP / maxQ;
			double utility = limitPriceAsset - ( ratio * expectJ );

			if ( utility > 0) {
				for (int i = numTrials; i > 0; i--) {
					pb = minP + Math.random() * (maxP - minP);
					qb = minQ + Math.random() * (maxQ - minQ);

					ratio = pb / qb;
					utility = limitPriceAsset - ( ratio * expectJ );

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
							bidOfferings[1] = new BidOfferingWithLoans(
									pb, UNIT, qb, this.markets.getLoans().getJ(),
									loanAmount, this, MarketType.ASSET_LOAN );
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
								bidOfferings[1] = new BidOfferingWithLoans(
										pb, assetAmount, qb,
										this.markets.getLoans().getJ(), pb * assetAmount
												/ qb, this, MarketType.ASSET_LOAN );
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

		// TODO: bid-offerings on loan
		
		return bidOfferings;
	}
	*/
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
	
	/*
	public boolean execSellTransaction(Offering[] match, boolean first)  {
		//first is true, if bid was before myAsk: is used for a double dispatch pattern
		AskOffering myAsk = (AskOffering)match[0]; 
		BidOffering bid = (BidOffering)match[1];
		double toSell = Math.min(myAsk.getAmount(), bid.getAmount()); 
		assetEndow -= toSell;
		
		if (assetEndow < ASSET_INSENSIBILITY)  {
			if (assetEndow < -ASSET_INSENSIBILITY)
				System.err.println("error in agent.execAskTransaction: wants to sell too much");
			else
				assetEndow = 0;
		}
		
		if (first)  {
			cashEndow += bid.getFinalAssetPrice()*toSell;
			bid.getAgent().execBuyTransaction(match, !first);
		} else {
			cashEndow += myAsk.getFinalAssetPrice()*toSell;
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
		double toBuy = Math.min(myBid.getAmount(), ask.getAmount());	
		//potential improvement (accelerates auction?): bidding agent could buy more with his consume endowment 
		//since ask asset price <= bid asset price
		assetEndow += toBuy;
	
		if (first)  {
			cashEndow -= ask.getFinalAssetPrice()*toBuy;
			ask.getAgent().execSellTransaction(match, !first);
		}
		else {
			cashEndow -= myBid.getFinalAssetPrice()*toBuy;	
		}
		
		if (cashEndow < CONSUME_INSENSIBILITY)  {
	
			if (cashEndow < -CONSUME_INSENSIBILITY)
				System.err.println("error in agent.execBidTransaction: wants to buy too much");
			else 
				cashEndow = 0;
		}
		
		// need to reset when match to force a recalculation of offers
		// INFO: don't really need to reset because this will result in a successful TX which
		// will be returned immediately. Best Offerings will be cleared bevore next TX
		//this.clearBestOfferings();
		
		return true;
	}
	*/
	
	public void clearBestOfferings() {
		for ( int i = 0; i < Markets.NUMMARKETS; ++i ) {
			this.bestAskOfferings[ i ] = null;
			this.bestBidOfferings[ i ] = null;
		}
	}
	
	public AskOffering[] getCurrentAskOfferings() {
		return currentAskOfferings;
	}

	public BidOffering[] getCurrentBidOfferings() {
		return currentBidOfferings;
	}

	public AskOffering[] getBestAskOfferings() {
		return bestAskOfferings;
	}

	public BidOffering[] getBestBidOfferings() {
		return bestBidOfferings;
	}

	private double getPMax() {
		double refP = this.markets.getAsset().getP();
		return Math.min(this.markets.getAsset().getPU(), refP + PMARGIN);
	}
	
	private double getPMin() {
		double refP = this.markets.getAsset().getP();
		return Math.max(this.markets.getAsset().getPD(), refP - PMARGIN);
	}
	
	/*
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
		cashEndow -= loanAmount * bid.getFinalLoanPrice();
		loanGiven += loanAmount;

		if (cashEndow < CONSUME_INSENSIBILITY) {
			if (cashEndow < -CONSUME_INSENSIBILITY)
				System.err.println("error in agent.execLoanTransaction: wants to loan too much");
			else
				cashEndow = 0;
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
		double toSell = Math.min(myAsk.getAmount(), bid.getAmount());

		// adjust askers amount
		myAsk.setAmount(toSell);

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

		if ( MarketType.ASSET_CASH == myAsk.getMarketType() ) {
			// asset against cash
			cashEndow += myAsk.getFinalAssetPrice() * toSell;

			if (cashEndow < CONSUME_INSENSIBILITY) {
				if (cashEndow < -CONSUME_INSENSIBILITY)
					System.err.println("error in agent.execAskTransaction: wants to loan too much");
				else
					cashEndow = 0;
			}
		} else {
			// market type 1: asset against loan
			double loanAmount = toSell * myAsk.getFinalAssetPrice() / ((AskOfferingWithLoans) myAsk).getFinalLoanPrice();

			loanGiven += loanAmount;
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

		double toBuy = Math.min(myBid.getAmount(), ask.getAmount());
		double loanAmount = 0;

		// adjust bidders amounts
		myBid.setAmount(toBuy);

		if ( MarketType.ASSET_LOAN == myBid.getMarketType() ) {
			// adjust loan amount
			loanAmount = toBuy * myBid.getFinalAssetPrice()
					/ ((BidOfferingWithLoans) myBid).getFinalLoanPrice();
			((BidOfferingWithLoans) myBid).setLoanAmount(loanAmount);

		}
		
		assetEndow += toBuy;
		freeAssetEndow += toBuy;

		if ( MarketType.ASSET_CASH == myBid.getMarketType() ) {
			// asset against cash
			cashEndow -= myBid.getFinalAssetPrice() * toBuy;
			if (cashEndow < CONSUME_INSENSIBILITY) {
				if (cashEndow < -CONSUME_INSENSIBILITY)
					System.err.println("error in agent.execBidTransaction: wants to buy too much");
				else
					cashEndow = 0;
			}
		} else {
			// asset against loan
			freeAssetEndow -= loanAmount;
			loanTaken += loanAmount;

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
	*/
	
	public double getLoanTaken() {
		return loanTaken;
	}
	
	@Override
	public Object clone() {
		Agent clone = new Agent(this.id, this.h, this.cashEndow, this.assetEndow, this.markets );
		clone.freeAssetEndow = this.freeAssetEndow;
		clone.loanGiven = this.loanGiven;
		clone.loanTaken = this.loanTaken;
		
		for ( int i = 0; i < Markets.NUMMARKETS; ++i ) {
			clone.bestAskOfferings[ i ] = this.bestAskOfferings[ i ];
			clone.bestBidOfferings[ i ] = this.bestBidOfferings[ i ];
		}
		
		return clone;
	}
}
