package backend.agents;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import backend.markets.MarketType;
import backend.markets.Markets;
import backend.offers.AskOffering;
import backend.offers.BidOffering;
import backend.tx.Match;

public class Agent {
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
	private double loan;
	// the amount of loans sold to other agents for cash or assets. is the amount of collateralized assets
	private double loanTaken;
	// the amount of loans bought from other agents for cash or assets. is the amount of UN-collateralized assets
	private double loanGiven;
	
	//first index: 0: buy, 1: sell; second index: 0: lower 1: upper limit
	double[][] assetLimits; 
	double[][] loanLimits;
	double[][] assetLoanLimits;
	
	double minAssetPriceInCash;
	double minLoanPriceInCash;
	double minAssetPriceInLoans;
	
	double maxAssetPriceInCash;
	double maxLoanPriceInCash;
	double maxAssetPriceInLoans;
	
	double expectedAssetPriceInLoans;
	
	private Markets markets;
	
	private boolean highlighted;
	
	// NOTE: holds the offerings of the current sweeping-round
	private AskOffering[] currentAskOfferings;
	private BidOffering[] currentBidOfferings;
	
	// NOTE: linear order on offerings on each market thus there can only be one BEST offering on each market for buy/sell
	private AskOffering[] bestAskOfferings;
	private BidOffering[] bestBidOfferings;
	
	private List<List<Double>> askSampleRanges;
	private List<List<Double>> bidSampleRanges;
	
	private final static int MAX_SAMPLES = 1000;
	
	public Agent(int id, double h, Markets markets ) {
		this.id = id;
		this.h = h;
		this.markets = markets;
	
		this.bidSampleRanges = new ArrayList<>();
		this.askSampleRanges = new ArrayList<>();
		
		for ( int i = 0; i < Markets.NUMMARKETS; ++i ) {
			this.bidSampleRanges.add( new ArrayList<>() );
			this.askSampleRanges.add( new ArrayList<>() );
		}

		this.assetLimits = new double[2][2]; //first index: 0: buy, 1: sell; second index: 0: lower 1: upper limit
		this.loanLimits = new double[2][2];
		this.assetLoanLimits = new double[2][2];
		
		this.calculateOfferingLimits();
		
		this.resetImportanceSamplingData();
		  
		this.defineBidSamples();
		this.defineAskSamples();
		
		this.reset();
	}
	
	private void calculateOfferingLimits() {
		double pD = markets.pD();
		double pU = markets.pU();
		double V = markets.V();
		
		double minAssetPrice = pD;
		double maxLoanPrice = Math.min( pU, V );
		
		double maxAssetPrice = pU;
		double minLoanPrice = Math.min( pD, V );
		
		this.limitPriceAsset = markets.calculateLimitPriceAsset( h );
		this.limitPriceLoan = markets.calculateLimitPriceLoan( h );
		
		this.minAssetPriceInCash = Math.min( pD, limitPriceAsset );
		this.minLoanPriceInCash = Math.min( Math.min( pD, V ), limitPriceLoan );
		this.minAssetPriceInLoans = minAssetPrice / maxLoanPrice;
		
		this.maxAssetPriceInCash = Math.max( pU, limitPriceAsset );
		this.maxLoanPriceInCash = Math.max( Math.min( pU, V ), limitPriceLoan );
		this.maxAssetPriceInLoans = maxAssetPrice / minLoanPrice;

		this.expectedAssetPriceInLoans = limitPriceAsset / limitPriceLoan;
	}
	
	public void printSamples() {
		for ( int i = 0; i < Markets.NUMMARKETS; ++i ) {
			java.util.Collections.sort( bidSampleRanges.get( i ) );
			java.util.Collections.sort( askSampleRanges.get( i ) );
			
			System.out.println( "bidSamples_" + MarketType.values()[ i ] + " = [" );
			for ( int j = 0; j < bidSampleRanges.get( i ).size(); ++j ) {
				System.out.println( bidSampleRanges.get( i ).get( j ) );
			}
			System.out.println( "]" );

			System.out.println( "askSamples_" + MarketType.values()[ i ] + " = [" );
			for ( int j = 0; j < askSampleRanges.get( i ).size(); ++j ) {
				System.out.println( askSampleRanges.get( i ).get( j ) );
			}
			System.out.println( "]" );
		}
		
		System.out.println();
	}
	
	private void defineBidSamples() {
		double pD = markets.pD();
		double pU = markets.pU();
		double V = markets.V();
		
		this.bidSampleRanges.get( 0 ).clear();
		this.bidSampleRanges.get( 1 ).clear();
		this.bidSampleRanges.get( 2 ).clear();
		
		double minAssetPriceInCash = Math.min( pD, limitPriceAsset );
		calculateSampleRange( minAssetPriceInCash, limitPriceAsset, MAX_SAMPLES, bidSampleRanges.get( 0 ) );
		
		double minLoanPriceInCash = Math.min( Math.min( pD, V ), limitPriceLoan );
		calculateSampleRange( minLoanPriceInCash, limitPriceLoan, MAX_SAMPLES, bidSampleRanges.get( 1 ) );
		
		double minAssetPrice = pD;
		double maxLoanPrice = Math.min( pU, V );
		double minAssetPriceInLoans = minAssetPrice / maxLoanPrice;
		double expectedAssetPriceInLoans = limitPriceAsset / limitPriceLoan;
		
		calculateSampleRange( minAssetPriceInLoans, expectedAssetPriceInLoans, MAX_SAMPLES, bidSampleRanges.get( 2 ) );
	}
	
	private void defineAskSamples() {
		double pD = markets.pD();
		double pU = markets.pU();
		double V = markets.V();
		
		this.askSampleRanges.get( 0 ).clear();
		this.askSampleRanges.get( 1 ).clear();
		this.askSampleRanges.get( 2 ).clear();
		
		double maxAssetPriceInCash = Math.max( pU, limitPriceAsset );
		calculateSampleRange( limitPriceAsset, maxAssetPriceInCash, MAX_SAMPLES, askSampleRanges.get( 0 ) );
		
		double maxLoanPriceInCash = Math.max( Math.min( pU, V ), limitPriceLoan );
		calculateSampleRange( limitPriceLoan, maxLoanPriceInCash, MAX_SAMPLES, askSampleRanges.get( 1 ) );
		
		double expectedAssetPriceInLoans = limitPriceAsset / limitPriceLoan;
		double maxAssetPrice = pU;
		double minLoanPrice = Math.min( pD, V );
		double maxAssetPriceInLoans = maxAssetPrice / minLoanPrice;
		calculateSampleRange( expectedAssetPriceInLoans, maxAssetPriceInLoans, MAX_SAMPLES, askSampleRanges.get( 2 ) );
	}
	
	private void calculateSampleRange( double min, double max, int sampleCount, List<Double> range ) {
		if ( min == max ) {
			range.add( min );
			return;
		}
		
		for ( int i = 0; i < sampleCount; ++i ) {
			double value = min + ( ( max - min ) * ( double ) i / sampleCount );
			
			range.add( value );
		}
	}
	
	public void reset() {
		this.cashEndow = this.markets.getConsumEndow(); 
		this.assetEndow = this.markets.getAssetEndow();
		this.highlighted = false;

		this.loan = 0;
		this.loanTaken = 0;
		this.loanGiven = 0;
		
		this.currentAskOfferings = new AskOffering[ Markets.NUMMARKETS ];
		this.currentBidOfferings = new BidOffering[ Markets.NUMMARKETS ];
		
		this.bestAskOfferings = new AskOffering[ Markets.NUMMARKETS ];
		this.bestBidOfferings = new BidOffering[ Markets.NUMMARKETS ];
	
		//defineBidSamples();
		//defineAskSamples();
	}
	
	public void calcNewOfferings() {
		this.calcAskOfferings( this.currentAskOfferings );
		this.calcBidOfferings( this.currentBidOfferings );
	}
	
	public double getLoan() {
		return this.loan;
	}
	
	private void calcBidOfferings( BidOffering[] offerings ) {		
		// want to BUY an asset against cash 
		// => paying cash to seller
		// => getting asset from seller
		// => need to have positive amount of cash

		// the price for 1.0 Units of asset - will be normalized during a Match
		// to the given amount below - the unit of this variable is CASH
		//double assetPriceInCash = randomRange( minAssetPriceInCash, limitPriceAsset );
		//double assetPriceInCash = drawRandomFromRange( bidSampleRanges.get( 0 ) );
		double assetPriceInCash = randomRange( assetLimits[ 0 ][ 0 ], assetLimits[ 0 ][ 1 ] );
		
		if ( this.cashEndow >= Markets.TRADING_UNIT_ASSET * assetPriceInCash ) {
			offerings[ MarketType.ASSET_CASH.ordinal() ] = new BidOffering( assetPriceInCash, Markets.TRADING_UNIT_ASSET, this, MarketType.ASSET_CASH );
			
		} else {
			offerings[ MarketType.ASSET_CASH.ordinal() ] = null;
		}
			
		// want to BUY a loan against cash 
		// => paying cash to seller
		// => getting bond from seller
		// => need to have positive amount of cash
		if ( this.markets.isLoanMarket() && this.cashEndow > 0 ) {
			// the price for 1.0 Units of loans - will be normalized during a Match
			// to the given amount below - the unit of this variable is CASH
			//double loanPriceInCash = randomRange( minLoanPriceInCash, limitPriceLoan );
			//double loanPriceInCash = drawRandomFromRange( bidSampleRanges.get( 1 ) );
			double loanPriceInCash = randomRange( loanLimits[ 0 ][ 0 ], loanLimits[ 0 ][ 1 ] );
			
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
		if ( this.markets.isABM() ) {
			// the price for 1.0 unit of assets in loans => the unit of this variable is LOANS
			//double assetPriceInLoans = randomRange( minAssetPriceInLoans, expectedAssetPriceInLoans );
			//double assetPriceInLoans = drawRandomFromRange( bidSampleRanges.get( 2 ) );
			double assetPriceInLoans = randomRange( assetLoanLimits[ 0 ][ 0 ], assetLoanLimits[ 0 ][ 1 ] );
			
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
			
		} else {
			offerings[ MarketType.ASSET_LOAN.ordinal() ] = null;
		}
	}

	private void calcAskOfferings( AskOffering[] offerings ) {
		// want to SELL an asset against cash 
		// => giving asset to buayer
		// => getting cash from buyer
		// => can only do so it if there are uncollateralized assets left

		// this is always the price for 1.0 Units of asset - will be normalized during a Match
		// to the given amount below - the unit of this variable is CASH
		//double assetPriceInCash = randomRange( limitPriceAsset, maxAssetPriceInCash );
		//double assetPriceInCash = drawRandomFromRange( askSampleRanges.get( 0 ) );
		double assetPriceInCash = randomRange( assetLimits[ 1 ][ 0 ], assetLimits[ 1 ][ 1 ] );
		
		double tmp=this.markets.isBP()?(-loan):(loanTaken);
		if ( this.assetEndow - Markets.TRADING_UNIT_ASSET >= Math.max( 0, tmp ) ) {
			offerings[ MarketType.ASSET_CASH.ordinal() ] = new AskOffering( assetPriceInCash, Markets.TRADING_UNIT_ASSET, this, MarketType.ASSET_CASH );
			
		} else {
			offerings[ MarketType.ASSET_CASH.ordinal() ] = null;
		}
			
		// want to SELL a loan against cash
		// => collateralize assets of the same amount as security
		// => getting money from buyer 
		// => need to have enough uncollateralized assets
		if ( this.markets.isLoanMarket() && this.assetEndow - Math.max( 0, tmp ) > Markets.TRADING_EPSILON ) {
			// this is always the price for 1.0 Units of loans - will be normalized during a Match
			// to the given amount below - the unit of this variable is CASH
			//double loanPriceInCash = randomRange( limitPriceLoan, maxLoanPriceInCash );
			//double loanPriceInCash = drawRandomFromRange( askSampleRanges.get( 1 ) );
			double loanPriceInCash = randomRange( loanLimits[1][0], loanLimits[1][1] );
			
			double loanAmount = this.assetEndow - Math.max( 0, tmp );
			loanAmount = Math.min( loanAmount, Markets.TRADING_UNIT_LOAN );
			
			offerings[ MarketType.LOAN_CASH.ordinal() ] = new AskOffering( loanPriceInCash, loanAmount, this, MarketType.LOAN_CASH );
			
		} else {
			offerings[ MarketType.LOAN_CASH.ordinal() ] = null;
		}
		
		// want to SELL a loan against an asset 
		// => giving asset to buyer
		// => getting bond from buyer: giving loan, "un"-collateralize assets
		// => because of giving asset: need to have enough amount of uncollateralized assets
		if ( this.markets.isABM() ) {
			// the price for 1.0 unit of assets in loans => the unit of this variable is LOANS
			//double assetPriceInLoans = randomRange( expectedAssetPriceInLoans, maxAssetPriceInLoans );
			//double assetPriceInLoans = drawRandomFromRange( askSampleRanges.get( 2 ) );
			double assetPriceInLoans = randomRange( assetLoanLimits[ 1 ][ 0 ], assetLoanLimits[ 1 ][ 1 ] );
			
			if ( markets.isBP() ) {
				tmp = -( loan + Markets.TRADING_UNIT_ASSET * assetPriceInLoans );
			} else {
				tmp = ( loanTaken - Markets.TRADING_UNIT_ASSET * assetPriceInLoans );
			}
			
			if ( this.assetEndow - Markets.TRADING_UNIT_ASSET >= Math.max( 0, tmp ) ) {
				offerings[ MarketType.ASSET_LOAN.ordinal() ] = new AskOffering( assetPriceInLoans, Markets.TRADING_UNIT_ASSET, this, MarketType.ASSET_LOAN );
				
			} else {
				offerings[ MarketType.ASSET_LOAN.ordinal() ] = null;
			}
			
		} else {
			offerings[ MarketType.ASSET_LOAN.ordinal() ] = null;
		}
	}
	
	public void execSellTransaction( Match match ) {
		// NOTE: executing a sell-transaction on this agent which means this agent is SELLING
		
		// NOTE: add sample with probability p
		// probability p: need to account how often the ask-price exists already in the range. the more often the lower the probability p
		/*
		int askPriceSampleCount = 0;
		int totalSampleCount = this.askSampleRanges.get( match.getMarket().ordinal() ).size();
		
		for ( Double s : this.askSampleRanges.get( match.getMarket().ordinal() ) ) {
			if ( s == match.getSellOffer().getPrice() ) {
				askPriceSampleCount++;
			}
		}
		
		// TODO: still seems to be biased - need to account for the imporance of this sample:
		// if the function is sampled already "enough" then reduce likelihood for adding this sample. BUT HOW?
		double p = 1.0 - ( ( double ) askPriceSampleCount / ( double ) totalSampleCount );
		if ( p > ThreadLocalRandom.current().nextDouble() ) {
			this.askSampleRanges.get( match.getMarket().ordinal() ).add( match.getSellOffer().getPrice() );
		}
		*/
		
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
			this.loan -= match.getAmount();
			this.loanTaken += match.getAmount();			// collateralize assets
			this.cashEndow += match.getNormalizedPrice();	// getting money from buyer
			
		// SELLING asset for loan
		// => giving asset to buyer
		// => giving loan to buyer - "un"-collateralizes the amount of assets which corresponds to the sold amount of loans
		} else if ( MarketType.ASSET_LOAN == match.getMarket() ) {
			this.loan += match.getNormalizedPrice();
			
			// price is in this case the asset-price in LOANS: amount of loans for 1.0 unit of assets
			// amount is in this case the asset-amount traded
			// getNormalizedPrice returns in this case the amount of loans needed for the given asset-amount
			this.loanGiven += match.getNormalizedPrice();	// "un"-collateralize assets
			this.assetEndow -= match.getAmount();			// giving asset to buyer
		}
	}
	
	public void execBuyTransaction( Match match ) {
		// NOTE: executing a buy-transaction on this agent which means this agent is BUYING
	
		/*
		// NOTE: add sample with probability p
		// probability p: need to account how often the bid-price exists already in the range. the more often the lower the probability p
		int bidPriceSampleCount = 0;
		int totalSampleCount = this.bidSampleRanges.get( match.getMarket().ordinal() ).size();
		
		for ( Double s : this.bidSampleRanges.get( match.getMarket().ordinal() ) ) {
			if ( s == match.getBuyOffer().getPrice() ) {
				bidPriceSampleCount++;
			}
		}
		
		double p = 1.0 - ( ( double ) bidPriceSampleCount / ( double ) totalSampleCount );
		if ( p > ThreadLocalRandom.current().nextDouble() ) {
			//this.bidSampleRanges.get( match.getMarket().ordinal() ).add( match.getBuyOffer().getPrice() );
		}*/
		
				
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
			this.loan += match.getAmount();
			this.loanGiven += match.getAmount();			// "un"-collateralizes assets
			this.cashEndow -= match.getNormalizedPrice();	// paying money to seller
			
		// BUYING an asset for loan
		// => getting assets from seller
		// => taking loan from seller: need to collateralize same amount of assets
		} else if ( MarketType.ASSET_LOAN == match.getMarket() ) {
			this.loan -= match.getNormalizedPrice();
			
			// price is in this case the asset-price in LOANS: amount of loans for 1.0 unit of assets
			// amount is in this case the asset-amount traded
			// getNormalizedPrice returns in this case the amount of loans needed for the given asset-amount
			this.loanTaken += match.getNormalizedPrice();	// collateralize asset for loan
			this.assetEndow += match.getAmount();			// getting asset from seller
		}
	}
	
	public double getUncollateralizedAssets() {
		// the uncollateralized assets are those which are available for trades
		return Math.max( 0, this.getAssetEndow() - this.getCollateral() - Math.max( 0, this.getLoan() ) );
	}

	public double getCollateral() {
		// when no BP Mechanism, loans given to other agents cannot be traded
		// thus the collateral is the loans taken from other agents which implies
		// assets as collateral 
		double collateral = this.getLoanTaken();
		
		// when using Bonds-Pledgeability (BP) Mechanism, then it is possible
		// to trade assets aquired by loans given to other agents
		if ( this.markets.isBP() ) {
			// loanGiven decreases the collateral in case of BP because it is available for trades
			collateral -= this.getLoanGiven();
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
		Agent clone = new Agent(this.id, this.h, this.markets );
		clone.assetEndow = this.assetEndow;
		clone.cashEndow = this.cashEndow;
		clone.loan = this.loan;
		clone.loanGiven = this.loanGiven;
		clone.loanTaken = this.loanTaken;
		
		clone.assetLimits = this.assetLimits;
		clone.loanLimits = this.loanLimits;
		clone.assetLoanLimits = this.assetLoanLimits;
		
		for ( int i = 0; i < Markets.NUMMARKETS; ++i ) {
			clone.bestAskOfferings[ i ] = this.bestAskOfferings[ i ];
			clone.bestBidOfferings[ i ] = this.bestBidOfferings[ i ];
		}
		
		return clone;
	}
	
	private static double randomRange( double min, double max ) {
		return min + ThreadLocalRandom.current().nextDouble() * ( max - min );
	}

	public void setImportanceSamplingData(double[][] limitAssets, double[][] limitLoans,
			double[][] limitAssetLoans) {
		this.assetLimits = limitAssets;
		this.loanLimits = limitLoans;
		this.assetLoanLimits = limitAssetLoans;
	}
	
	public void resetImportanceSamplingData() {
		this.assetLimits[0][0] = minAssetPriceInCash; //buy lower
		this.assetLimits[0][1] = limitPriceAsset; //buy upper
		this.assetLimits[1][0] = limitPriceAsset; //sell lower
		this.assetLimits[1][1] = maxAssetPriceInCash; //sell upper
		this.loanLimits[0][0] = minLoanPriceInCash; //buy lower
		this.loanLimits[0][1] = limitPriceLoan; //buy upper
		this.loanLimits[1][0] = limitPriceLoan; //sell lower
		this.loanLimits[1][1] = maxLoanPriceInCash; //sell upper
		this.assetLoanLimits[0][0] = minAssetPriceInLoans; //buy lower
		this.assetLoanLimits[0][1] = expectedAssetPriceInLoans; //buy upper
		this.assetLoanLimits[1][0] = expectedAssetPriceInLoans; //sell lower
		this.assetLoanLimits[1][1] = maxAssetPriceInLoans; //sell upper  
	}
	
	@SuppressWarnings("unused")
	private static double drawRandomFromRange( List<Double> range ) {
		return range.get( (int) (range.size() * ThreadLocalRandom.current().nextDouble()) );
	}
}
