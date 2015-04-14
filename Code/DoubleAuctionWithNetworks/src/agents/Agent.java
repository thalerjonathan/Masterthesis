package agents;

import agents.markets.MarketType;
import agents.markets.Markets;
import doubleAuction.offer.AskOffering;
import doubleAuction.offer.BidOffering;
import doubleAuction.tx.Match;

public class Agent {
	private Markets markets;
	
	private int id;
	// optimism factor
	private double h;
	// Expected Value (E, Erwartungswert) of the Asset
	private double limitPriceAsset;
	// Expected Value (E, Erwartungswert) of the loan
	private double limitPriceLoan;
	
	// amount of consumption-good endowment (cash) still available
	private double cashEndow;
	// amount of assets this agent owns REALLY 
	private double assetEndow;
	// = loanGiven-loanTaken
	// if negative then there are more collaterlized assets
	// if positive then there are more uncollaterlized assets
	//private double loan;
	// the amount of loans sold to other agents for cash or assets. is the amount of collateralized assets
	private double loanTaken;
	// the amount of loans bought from other agents for cash or assets. is the amount of UN-collateralized assets
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
	
		this.limitPriceAsset = markets.calculateLimitPriceAsset( h );
		this.limitPriceLoan = markets.calculateLimitPriceLoan( h );
		
		this.reset( consumEndow, assetEndow );
	}
	
	public void reset( double consumEndow, double assetEndow ) {
		this.cashEndow = consumEndow; 
		this.assetEndow = assetEndow;
		this.highlighted = false;

		//this.loan = 0;
		this.loanTaken = 0;
		this.loanGiven = 0;
		
		this.currentAskOfferings = new AskOffering[ Markets.NUMMARKETS ];
		this.currentBidOfferings = new BidOffering[ Markets.NUMMARKETS ];
		
		this.bestAskOfferings = new AskOffering[ Markets.NUMMARKETS ];
		this.bestBidOfferings = new BidOffering[ Markets.NUMMARKETS ];
	}
	
	public void calcNewOfferings() {
		this.calcAskOfferings( this.currentAskOfferings );
		this.calcBidOfferings( this.currentBidOfferings );
	}
	
	private void calcBidOfferings( BidOffering[] offerings ) {		
		double pD = markets.pD();
		double pU = markets.pU();
		double V = markets.V();
		
		double uncollateralizedAssets = this.getUncollateralizedAssets();
		
		// want to BUY an asset against cash 
		// => paying cash to seller
		// => getting asset from seller
		// => need to have positive amount of cash
		if ( this.cashEndow > 0 ) {
			double minAssetPriceInCash = Math.min( pD, limitPriceAsset );
			
			// the price for 1.0 Units of asset - will be normalized during a Match
			// to the given amount below - the unit of this variable is CASH
			double assetPriceInCash = randomRange( minAssetPriceInCash, limitPriceAsset );
			// calculate how much assets we can buy MAX
			double assetAmount = this.cashEndow / assetPriceInCash;
			// upper limit: trade at most TRADING_UNIT_ASSETS assets - if below take the lesser assetAmount
			// => trading down till reaching 0
			assetAmount = Math.min( Markets.TRADING_UNIT_ASSET, assetAmount );
			
			offerings[ MarketType.ASSET_CASH.ordinal() ] = new BidOffering( assetPriceInCash, assetAmount, this, MarketType.ASSET_CASH );
		} else {
			offerings[ MarketType.ASSET_CASH.ordinal() ] = null;
		}
		
		// want to BUY a loan against cash 
		// => paying cash to seller
		// => getting bond from seller
		// => need to have positive amount of cash
		if ( this.markets.isLoanMarket() && this.cashEndow > 0 ) {
			double minLoanPriceInCash = Math.min( Math.min( pD, V ), limitPriceLoan );
			
			// the price for 1.0 Units of loans - will be normalized during a Match
			// to the given amount below - the unit of this variable is CASH
			double loanPriceInCash = randomRange( minLoanPriceInCash, limitPriceLoan );
			// calculate which amount of loans we can buy MAX
			double loanAmount = this.cashEndow / loanPriceInCash;
			// upper limit: trade at most TRADING_UNIT_LOAN loans - if below take the lesser loanAmount
			// => trading down till reaching 0
			loanAmount = Math.min( Markets.TRADING_UNIT_LOAN, loanAmount );
			
			offerings[ MarketType.LOAN_CASH.ordinal() ] = new BidOffering( loanPriceInCash, loanAmount, this, MarketType.LOAN_CASH );
		
		} else {
			offerings[ MarketType.LOAN_CASH.ordinal() ] = null;
		}
		
		// want to BUY a asset against loan
		// => giving loan to seller: taking loan, collateralizing asset
		// => getting asset from seller
		// => because of taking loan: need to have enough amount of uncollateralized assets
		if ( this.markets.isABM() && uncollateralizedAssets > 0 ) {
			double minAssetPrice = pD;
			double maxLoanPrice = Math.min( pU, V );
			double minAssetPriceInLoans = minAssetPrice / maxLoanPrice;
			
			double expectedAssetPriceInLoans = limitPriceAsset / limitPriceLoan;
			
			// the price for 1.0 unit of assets in loans => the unit of this variable is LOANS
			double assetPriceInLoans = randomRange( minAssetPriceInLoans, expectedAssetPriceInLoans );
			
			// TODO: need to account for the asset we get from seller
			
			double amount = Math.min( Markets.TRADING_UNIT_ASSET, uncollateralizedAssets );
			
			offerings[ MarketType.ASSET_LOAN.ordinal() ] = new BidOffering( assetPriceInLoans, amount, this, MarketType.ASSET_LOAN );
	

			/*
			double tmp = 0.0;
			
			if ( markets.isBP() ) {
				tmp = -( loan - Markets.TRADING_UNIT_ASSET * assetPriceInLoans );
			} else {
				tmp = ( loanTaken + Markets.TRADING_UNIT_ASSET * assetPriceInLoans );
			}
			
			if ( this.assetEndow + Markets.TRADING_UNIT_ASSET >= Math.max( 0, tmp ) ) {
				offerings[ MarketType.ASSET_LOAN.ordinal() ] = new BidOffering( assetPriceInLoans, Markets.TRADING_UNIT_ASSET, this, MarketType.ASSET_LOAN );
				
			} else {
				offerings[ MarketType.ASSET_LOAN.ordinal() ] = null;
			}
		*/
			
		} else {
			offerings[ MarketType.ASSET_LOAN.ordinal() ] = null;
		}
	}
	
	private void calcAskOfferings( AskOffering[] offerings ) {
		double pD = markets.pD();
		double pU = markets.pU();
		double V = markets.V();
		
		double uncollateralizedAssets = this.getUncollateralizedAssets();
		
		// want to SELL an asset against cash 
		// => giving asset to buayer
		// => getting cash from buyer
		// => can only do so it if there are uncollateralized assets left
		if ( uncollateralizedAssets > 0 ) {
			double maxAssetPriceInCash = Math.max( pU, limitPriceAsset );
			
			// this is always the price for 1.0 Units of asset - will be normalized during a Match
			// to the given amount below - the unit of this variable is CASH
			double assetPriceInCash = randomRange( limitPriceAsset, maxAssetPriceInCash );
			// determine the amount of asset to trade: at most TRADING_UNIT_ASSET, or the
			// rest of uncollateralizedAssets if any left
			double assetAmount = Math.min( Markets.TRADING_UNIT_ASSET, uncollateralizedAssets );

			offerings[ MarketType.ASSET_CASH.ordinal() ] = new AskOffering( assetPriceInCash, assetAmount, this, MarketType.ASSET_CASH );
			
		} else {
			offerings[ MarketType.ASSET_CASH.ordinal() ] = null;
		}

		// want to SELL a loan against cash
		// => collateralize assets of the same amount as security
		// => getting money from buyer 
		// => need to have enough uncollateralized assets
		if ( this.markets.isLoanMarket() && uncollateralizedAssets > 0 ) {
			double maxLoanPriceInCash = Math.max( Math.min( pU, V ), limitPriceLoan );
			
			// this is always the price for 1.0 Units of loans - will be normalized during a Match
			// to the given amount below - the unit of this variable is CASH
			double loanPriceInCash = randomRange( limitPriceLoan, maxLoanPriceInCash );
			// calculate which amount of loans we can sell MAX
			double loanAmount = uncollateralizedAssets / loanPriceInCash;
			// upper limit: trade at most TRADING_UNIT_LOAN loans - if below take the lesser loanAmount
			// => trading down till reaching 0		
			loanAmount = Math.min( Markets.TRADING_UNIT_LOAN, loanAmount );
			
			offerings[ MarketType.LOAN_CASH.ordinal() ] = new AskOffering( loanPriceInCash, loanAmount, this, MarketType.LOAN_CASH );
			
			/*
			double tmp = this.getCollateral();
			if ( this.assetEndow - Math.max( 0, tmp ) > 0 ) {
				double loanAmount = this.assetEndow - Math.max( 0, tmp );
				loanAmount = Math.min( loanAmount, Markets.TRADING_UNIT_LOAN );
				
				offerings[ MarketType.LOAN_CASH.ordinal() ] = new AskOffering( loanPrice, loanAmount, this, MarketType.LOAN_CASH );
			} else {
				offerings[ MarketType.LOAN_CASH.ordinal() ] = null;
			}
			*/
		} else {
			offerings[ MarketType.LOAN_CASH.ordinal() ] = null;
		}
		
		// want to SELL a loan against an asset 
		// => giving asset to buyer
		// => getting bond from buyer: giving loan, "un"-collateralize assets
		// => beecause of giving asset: need to have enough amount of uncollateralized assets
		if ( this.markets.isABM() && uncollateralizedAssets > 0 ) {
			double expectedAssetPriceInLoans = limitPriceAsset / limitPriceLoan;
			
			double maxAssetPrice = pU;
			double minLoanPrice = Math.min( pD, V );
			double maxAssetPriceInLoans = maxAssetPrice / minLoanPrice;
			
			// the price for 1.0 unit of assets in loans => the unit of this variable is LOANS
			double assetPriceInLoans = randomRange( expectedAssetPriceInLoans, maxAssetPriceInLoans );
			
			
			double amount = Math.min( Markets.TRADING_UNIT_ASSET, uncollateralizedAssets );
			offerings[ MarketType.ASSET_LOAN.ordinal() ] = new AskOffering( assetPriceInLoans, amount, this, MarketType.ASSET_LOAN );
	
			/*
			double tmp = 0.0;
			if ( markets.isBP() ) {
				tmp = -( loan + Markets.TRADING_UNIT_ASSET * pricePerAssetInLoans );
			} else {
				tmp = ( loanTaken - Markets.TRADING_UNIT_ASSET * pricePerAssetInLoans );
			}
			
			if ( this.assetEndow - Markets.TRADING_UNIT_ASSET >= Math.max( 0, tmp ) ) {
				offerings[ MarketType.ASSET_LOAN.ordinal() ] = new AskOffering( pricePerAssetInLoans, Markets.TRADING_UNIT_ASSET, this, MarketType.ASSET_LOAN );
				
			} else {
				offerings[ MarketType.ASSET_LOAN.ordinal() ] = null;
			}
			*/
			
		} else {
			offerings[ MarketType.ASSET_LOAN.ordinal() ] = null;
		}
	}
	
	public void execSellTransaction( Match match ) {
		// NOTE: executing a sell-transaction on this agent which means this agent is SELLING
		
		// SELLING an asset for cash
		// => giving asset to buyer
		// => getting cash from buyer
		if ( MarketType.ASSET_CASH == match.getMarket() ) {
			this.assetEndow -= match.getAmount();			// giving asset to buyer
			this.cashEndow += match.getNormalizedPrice();	// getting cash from buyer
			
		// SELLING a loan for cash
		// => collateralizing the amount of assets which correspond to the sold amount of loans
		// => getting money from buyer
		} else if ( MarketType.LOAN_CASH == match.getMarket() ) {
			//this.loan -= match.getAmount();
			this.loanTaken += match.getAmount();			// collateralize assets
			this.cashEndow += match.getNormalizedPrice();	// getting money from buyer
			
		// SELLING asset for loan
		// => giving asset to buyer
		// => giving loan to buyer - "un"-collateralizes the amount of assets which corresponds to the sold amount of loans
		} else if ( MarketType.ASSET_LOAN == match.getMarket() ) {
			//this.loan += match.getNormalizedPrice();
			
			// price is in this case the asset-price in LOANS: amount of loans for 1.0 unit of assets
			// amount is in this case the asset-amount traded
			// getNormalizedPrice returns in this case the amount of loans needed for the given asset-amount
			this.loanGiven += match.getNormalizedPrice();	// "un"-collateralize assets
			this.assetEndow -= match.getAmount();			// giving asset to buyer
		}
	}
	
	public void execBuyTransaction( Match match ) {
		// NOTE: executing a buy-transaction on this agent which means this agent is BUYING

		// BUYING an asset for cash
		// => getting assets from seller
		// => paying cash to seller
		if ( MarketType.ASSET_CASH == match.getMarket() ) {
			this.assetEndow += match.getAmount();			// getting asset from seller
			this.cashEndow -= match.getNormalizedPrice();	// paying cash to seller
			
		// BUYING a loan for cash
		// => getting loans from the seller: "un"-collateralizes assets
		// => paying cash to the seller 
		} else if ( MarketType.LOAN_CASH == match.getMarket() ) {
			//this.loan += match.getAmount();
			this.loanGiven += match.getAmount();			// "un"-collateralizes assets
			this.cashEndow -= match.getNormalizedPrice();	// paying money to seller
			
		// BUYING an asset for loan
		// => getting assets from seller
		// => taking loan from seller: need to collateralize same amount of assets
		} else if ( MarketType.ASSET_LOAN == match.getMarket() ) {
			//this.loan -= match.getNormalizedPrice();
			
			// price is in this case the asset-price in LOANS: amount of loans for 1.0 unit of assets
			// amount is in this case the asset-amount traded
			// getNormalizedPrice returns in this case the amount of loans needed for the given asset-amount
			this.loanTaken += match.getNormalizedPrice();	// collateralize asset for loan
			this.assetEndow += match.getAmount();			// getting asset from seller
		}
	}
	
	public double getUncollateralizedAssets() {
		// the uncollateralized assets are those which are available for trades
		// assetEndow
		return this.assetEndow - this.getCollateral();
	}

	public double getCollateral() {
		// when no BP Mechanism, loans given to other agents cannot be traded
		// thus the collateral is the loans taken from other agents which implies
		// assets as collateral 
		double collateral = this.loanTaken;
		
		// when using Bonds-Pledgeability (BP) Mechanism, then it is possible
		// to trade assets aquired by loans given to other agents
		if ( this.markets.isBP() ) {
			// loanGiven decreases the collateral in case of BP because it is available for trades
			collateral -= this.loanGiven;
		}
		
		return collateral;
	}
	
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

	public double getLoanTaken() {
		return loanTaken;
	}
	
	public double getLimitPriceLoans() {
		return limitPriceLoan;
	}
	
	/*
	public double getLoan() {
		return loan;
	}
*/
	
	public double getLoanGiven() {
		return loanGiven;
	}

	public boolean isHighlighted() {
		return highlighted;
	}

	public void setHighlighted( boolean highlighted ) {
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
	
	@Override
	public Object clone() {
		Agent clone = new Agent(this.id, this.h, this.cashEndow, this.assetEndow, this.markets );
		//clone.loan = this.loan;
		clone.loanGiven = this.loanGiven;
		clone.loanTaken = this.loanTaken;
		
		for ( int i = 0; i < Markets.NUMMARKETS; ++i ) {
			clone.bestAskOfferings[ i ] = this.bestAskOfferings[ i ];
			clone.bestBidOfferings[ i ] = this.bestBidOfferings[ i ];
		}
		
		return clone;
	}
	
	private static double randomRange( double min, double max ) {
		return min + Math.random() * ( max - min );
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
}
