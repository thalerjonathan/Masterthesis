package backend.agents;

import java.util.Arrays;

import controller.replication.data.AgentBean;
import utils.Utils;
import backend.markets.MarketType;
import backend.markets.Markets;
import backend.offers.AskOffering;
import backend.offers.BidOffering;
import backend.tx.Match;

public class Agent {
	private int id;
	// optimism factor
	private double h;
	// Expected Value (E, Erwartungswert) of the Asset in CASH
	private double limitPriceAsset;
	// Expected Value (E, Erwartungswert) of the loan in CASH
	private double limitPriceLoan;
	// expected value of asset/loan price: the value of an asset in LOANS
	private double limitPriceAssetLoans;
	// expected value of collateral/cash price: the value of an asset in cash
	private double limitPriceCollateral;
		
	// amount of consumption-good endowment (cash) still available
	private double cash;
	// amount of assets this agent owns REALLY 
	private double assets;
	// the amount of loans sold to other agents for cash or assets. is the amount of collateralized assets
	private double loansTaken;
	// the amount of loans bought from other agents for cash or assets. is the amount of UN-collateralized assets
	private double loansGiven;
	
	//first index: 0: buy, 1: sell; second index: 0: lower 1: upper limit
	private double[][] assetLimits; 
	private double[][] loanLimits;
	private double[][] assetLoanLimits;
	
	private double minAssetPriceInCash;
	private double minLoanPriceInCash;
	private double minAssetPriceInLoans;
	private double minCollateralPriceInCash;
	
	private double maxAssetPriceInCash;
	private double maxLoanPriceInCash;
	private double maxAssetPriceInLoans;
	private double maxCollateralPriceInCash;
	
	// NOTE: holds the offerings of the current sweeping-round
	private AskOffering[] currentAskOfferings;
	private BidOffering[] currentBidOfferings;
	
	// NOTE: linear order on offerings on each market thus there can only be one BEST offering on each market for buy/sell
	private AskOffering[] bestAskOfferings;
	private BidOffering[] bestBidOfferings;
		
	private Markets markets;
	private boolean highlighted;
	private boolean cantTrade;
	
	// NOTE: use only for visualization purposes!!
	public Agent( AgentBean bean, Markets markets ) {
		this.h = bean.getH();
		this.cash = bean.getCash();
		this.assets = bean.getAssets();
		this.loansGiven = bean.getLoanGiven();
		this.loansTaken = bean.getLoanTaken();
		this.markets = markets;
	}
	
	// NOTE: use only for visualization purposes!!
	public Agent( int id, double h ) {
		this.id = id;
		this.h = h;
	}
		
	public Agent( int id, double h, Markets markets ) {
		this.id = id;
		this.h = h;
		this.markets = markets;

		//first index: 0: buy, 1: sell; second index: 0: lower 1: upper limit
		this.assetLimits = new double[ 2 ][ 2 ]; 
		this.loanLimits = new double[ 2 ][ 2 ];
		this.assetLoanLimits = new double[ 2 ][ 2 ];
	
		this.currentAskOfferings = new AskOffering[ Markets.NUMMARKETS ];
		this.currentBidOfferings = new BidOffering[ Markets.NUMMARKETS ];
		
		this.bestAskOfferings = new AskOffering[ Markets.NUMMARKETS ];
		this.bestBidOfferings = new BidOffering[ Markets.NUMMARKETS ];
		
		this.calculateOfferingLimits();
		this.resetOfferingLimits();
		this.reset();
	}
	
	public void reset() {
		this.cash = this.markets.getCashEndowment(); 
		this.assets = this.markets.getAssetEndowment();
		this.highlighted = false;
		this.cantTrade = false;
		
		this.loansTaken = 0;
		this.loansGiven = 0;
		
		Arrays.fill( this.currentAskOfferings, null );
		Arrays.fill( this.currentBidOfferings, null );
		
		this.clearBestOfferings();
	}
	
	public void calcNewOfferings() {
		this.calcAskOfferings( this.currentAskOfferings );
		this.calcBidOfferings( this.currentBidOfferings );
	}

	public void execSellTransaction( Match match ) {
		// NOTE: executing a sell-transaction on this agent which means this agent is SELLING
		
		// SELLING an asset for cash
		// => giving asset to buyer
		// => getting cash from buyer
		if ( MarketType.ASSET_CASH == match.getMarket() ) {
			this.assets -= match.getAmount();			// giving asset to buyer
			this.cash += match.getNormalizedPrice();	// getting cash from buyer
			
		// SELLING a loan for cash
		// => collateralizing the amount of assets which correspond to the sold amount of loans
		// => getting money from buyer
		} else if ( MarketType.LOAN_CASH == match.getMarket() ) {
			this.loansTaken += match.getAmount();			// collateralize assets
			this.cash += match.getNormalizedPrice();		// getting money from buyer
			
		// SELLING asset for loan
		// => giving asset to buyer
		// => giving loan to buyer (buyer takes a loan)
		} else if ( MarketType.ASSET_LOAN == match.getMarket() ) {
			// price is in this case the asset-price in LOANS: amount of loans for 1.0 unit of assets
			// amount is in this case the asset-amount traded
			// getNormalizedPrice returns in this case the amount of loans needed for the given asset-amount
			this.loansGiven += match.getNormalizedPrice();	// giving loan to buyer
			this.assets -= match.getAmount();				// giving asset to buyer
			
		// SELLING collateral for cash
		// => giving COLLATERALIZED asset to buyer (is asset + the amount of loan)
		// => getting cash from buyer
		} else if ( MarketType.COLLATERAL_CASH == match.getMarket() ) {
			this.loansGiven += match.getAmount();
			this.assets -= match.getAmount();
			this.cash += match.getNormalizedPrice();
		}
	}
	
	public void execBuyTransaction( Match match ) {
		// NOTE: executing a buy-transaction on this agent which means this agent is BUYING
				
		// BUYING an asset for cash
		// => getting assets from seller
		// => paying cash to seller
		if ( MarketType.ASSET_CASH == match.getMarket() ) {
			this.assets += match.getAmount();			// getting asset from seller
			this.cash -= match.getNormalizedPrice();	// paying cash to seller
			
		// BUYING a loan for cash
		// => getting loans from the seller: "un"-collateralizes assets
		// => paying cash to the seller 
		} else if ( MarketType.LOAN_CASH == match.getMarket() ) {
			this.loansGiven += match.getAmount();			// giving loan to seller
			this.cash -= match.getNormalizedPrice();		// paying money to seller
			
		// BUYING an asset for loan
		// => getting assets from seller
		// => taking loan from seller: need to collateralize same amount of assets
		} else if ( MarketType.ASSET_LOAN == match.getMarket() ) {
			// price is in this case the asset-price in LOANS: amount of loans for 1.0 unit of assets
			// amount is in this case the asset-amount traded
			// getNormalizedPrice returns in this case the amount of loans needed for the given asset-amount
			this.loansTaken += match.getNormalizedPrice();	// collateralize asset for loan
			this.assets += match.getAmount();				// getting asset from seller
		
		// BUYING collateral for cash
		// => getting COLLATERALIZED asset from seller (is asset + the amount of loan)
		// => giving cash to seller
		} else if ( MarketType.COLLATERAL_CASH == match.getMarket() ) {
			this.loansTaken += match.getAmount();
			this.assets += match.getAmount();
			this.cash -= match.getNormalizedPrice();
		}
	}
	
	public double getUncollateralizedAssets() {
		// the uncollateralized assets are those which are available for trade
		// this is simply the amount of assets hold in total, which includes collateralized assets 
		// and thus needs to subtract the collateral obligations
		return this.getAssets() - this.getCurrentObligations();
	}
	
	public void clearBestOfferings() {
		Arrays.fill( this.bestAskOfferings, null );
		Arrays.fill( this.bestBidOfferings, null );
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

	public double getLoansTaken() {
		return loansTaken;
	}
	
	public double getLimitPriceLoans() {
		return limitPriceLoan;
	}
	
	public double getLimitPriceCollateral() {
		return limitPriceCollateral;
	}

	public double getLoansGiven() {
		return loansGiven;
	}

	public boolean isHighlighted() {
		return this.highlighted;
	}

	public void setHighlighted( boolean highlighted ) {
		this.highlighted = highlighted;
	}

	public boolean isCantTrade() {
		return cantTrade;
	}

	public void setCantTrade(boolean cantTrade) {
		this.cantTrade = cantTrade;
	}
	
	public double getLoans() {
		return this.getLoansGiven() - this.getLoansTaken();
	}
	
	public int getId() {
		return id;
	}

	public double getCash()  {
		return cash;
	}
	
	public double getAssets()  {
		return assets;
	}
	
	public double getH()  {
		return h;
	}
	
	public double getLimitPriceAsset() {
		return limitPriceAsset;
	}
	
	public double getLimitPriceAssetLoans() {
		return limitPriceAssetLoans;
	}

	public double getMinAssetPriceInCash() {
		return minAssetPriceInCash;
	}

	public double getMinLoanPriceInCash() {
		return minLoanPriceInCash;
	}

	public double getMinAssetPriceInLoans() {
		return minAssetPriceInLoans;
	}

	public double getMinCollateralPriceInCash() {
		return minCollateralPriceInCash;
	}

	public double getMaxAssetPriceInCash() {
		return maxAssetPriceInCash;
	}

	public double getMaxLoanPriceInCash() {
		return maxLoanPriceInCash;
	}

	public double getMaxAssetPriceInLoans() {
		return maxAssetPriceInLoans;
	}

	public double getMaxCollateralPriceInCash() {
		return maxCollateralPriceInCash;
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
		Agent clone = new Agent( this.id, this.h, this.markets );
		clone.assets = this.assets;
		clone.cash = this.cash;
		clone.loansGiven = this.loansGiven;
		clone.loansTaken = this.loansTaken;
		
		clone.assetLimits = this.assetLimits;
		clone.loanLimits = this.loanLimits;
		clone.assetLoanLimits = this.assetLoanLimits;
		
		for ( int i = 0; i < Markets.NUMMARKETS; ++i ) {
			clone.bestAskOfferings[ i ] = this.bestAskOfferings[ i ];
			clone.bestBidOfferings[ i ] = this.bestBidOfferings[ i ];
		}
		
		clone.cantTrade = this.cantTrade;
		
		return clone;
	}
	
	public void setImportanceSamplingData(double[][] limitAssets, double[][] limitLoans,
			double[][] limitAssetLoans) {
		this.assetLimits = limitAssets;
		this.loanLimits = limitLoans;
		this.assetLoanLimits = limitAssetLoans;
	}
	
	public void resetOfferingLimits() {
		this.assetLimits[0][0] = minAssetPriceInCash; //buy lower
		this.assetLimits[0][1] = limitPriceAsset; //buy upper
		this.assetLimits[1][0] = limitPriceAsset; //sell lower
		this.assetLimits[1][1] = maxAssetPriceInCash; //sell upper
		this.loanLimits[0][0] = minLoanPriceInCash; //buy lower
		this.loanLimits[0][1] = limitPriceLoan; //buy upper
		this.loanLimits[1][0] = limitPriceLoan; //sell lower
		this.loanLimits[1][1] = maxLoanPriceInCash; //sell upper
		this.assetLoanLimits[0][0] = minAssetPriceInLoans; //buy lower
		this.assetLoanLimits[0][1] = limitPriceAssetLoans; //buy upper
		this.assetLoanLimits[1][0] = limitPriceAssetLoans; //sell lower
		this.assetLoanLimits[1][1] = maxAssetPriceInLoans; //sell upper  
	}
	
	/* calculates the collateral-obligations 
	 * if > 0 then more loans are taken than loans are given => have debt obligations through securitization
	 * collateralAdjustment allows to increase/decrease the obligations e.g. when necessary to caluclate
	 * the obligations AFTER a trade (to calculate future obligations)
	 */
	private double getCollateralObligationsAfterTrade( double collateralTraded ) {
		// when no BP Mechanism, loans given to other agents cannot be traded
		// thus the collateral is the loans taken from other agents which implies
		// assets as collateral 
		double collateral = this.getLoansTaken();
		
		// when using Bonds-Pledgeability (BP) Mechanism, then it is possible
		// to trade assets aquired by loans given to other agents
		if ( this.markets.isBP() ) {
			// loanGiven decreases the collateral in case of BP because it is available for trades
			collateral -= this.getLoansGiven();
		}
		
		return Math.max( 0.0, collateral + collateralTraded );
	}
	
	private double getCurrentObligations() {
		return this.getCollateralObligationsAfterTrade( 0.0 );
	}
	
	private void calcBidOfferings( BidOffering[] offerings ) {		
		// the price for 1.0 Units of asset - will be normalized during a Match
		// to the given amount below - the unit of this variable is CASH
		//double assetPriceInCash = randomRange( minAssetPriceInCash, limitPriceAsset );
		double assetPriceInCash = randomRange( assetLimits[ 0 ][ 0 ], assetLimits[ 0 ][ 1 ] );
		
		// if there is enough cash left to buy the given amount of assets
		if ( this.cash >= Markets.TRADING_UNIT_ASSET * assetPriceInCash ) {
			// want to BUY an asset against cash 
			// => paying cash to seller
			// => getting asset from seller
			// => need to have positive amount of cash

			offerings[ MarketType.ASSET_CASH.ordinal() ] = new BidOffering( assetPriceInCash, Markets.TRADING_UNIT_ASSET, this, MarketType.ASSET_CASH );
			
		// not enough cash left to place buy-offer for asset
		} else {
			offerings[ MarketType.ASSET_CASH.ordinal() ] = null;
		}
			
		// loan-market is open AND there is still cash left for buying a bond
		if ( this.markets.isLoanMarket() && this.cash > Markets.TRADING_EPSILON ) {
			// want to BUY a loan against cash: GIVING the seller a loan, lending money to seller
			// => paying cash to seller (lending money to seller)
			// => getting bond from seller (giving loan to seller)
			// => need to have positive amount of cash
			
			// the price for 1.0 Units of loans - will be normalized during a Match
			// to the given amount below - the unit of this variable is CASH
			//double loanPriceInCash = randomRange( minLoanPriceInCash, limitPriceLoan );
			double loanPriceInCash = randomRange( loanLimits[ 0 ][ 0 ], loanLimits[ 0 ][ 1 ] );
			
			// calculate which amount of loans we can buy MAX
			double loanAmount = this.cash / loanPriceInCash;
			// upper limit: trade at most TRADING_UNIT_LOAN loans - if below take the lesser loanAmount
			// => trading down till reaching 0
			loanAmount = Math.min( Markets.TRADING_UNIT_LOAN, loanAmount );
			
			offerings[ MarketType.LOAN_CASH.ordinal() ] = new BidOffering( loanPriceInCash, loanAmount, this, MarketType.LOAN_CASH );
		
		// either not enough cash left to buy a bond OR loan-market is closed
		} else {
			offerings[ MarketType.LOAN_CASH.ordinal() ] = null;
		}
		
		// asset-against-bond market is open, check if agent can place buy-offers
		if ( this.markets.isABM() ) {
			// want to BUY an asset against loan
			// => getting asset from seller
			// => paying with a loan: taking loan from seller
			// => because of taking loan: need to have enough amount of uncollateralized assets
			
			// the price for 1.0 unit of assets in loans => the unit of this variable is LOANS
			//double assetPriceInLoans = randomRange( minAssetPriceInLoans, expectedAssetPriceInLoans );
			double assetPriceInLoans = randomRange( assetLoanLimits[ 0 ][ 0 ], assetLoanLimits[ 0 ][ 1 ] );
			// calculate how much loans will be taken (because selling a loan)
			double loanTakenAmount = Markets.TRADING_UNIT_ASSET * assetPriceInLoans;

			// calculate the amoung of uncollateralized assets AFTER trade. MUST ALWAYS be >= 0
			double uncollAssetsAfterTrade = this.assets;
			// collateral will be bound (taking loan from seller), thus increasing collateral obligations
			// thus reducing uncollateralized assets after trade
			uncollAssetsAfterTrade -= getCollateralObligationsAfterTrade( loanTakenAmount );
			// getting asset from seller, thus increasing uncollateralized assets after trade
			uncollAssetsAfterTrade += Markets.TRADING_UNIT_ASSET;
			
			// cannot go short on assets: uncollateralized assets can NEVER be negative
			if ( uncollAssetsAfterTrade >= 0 ) {
				offerings[ MarketType.ASSET_LOAN.ordinal() ] = new BidOffering( assetPriceInLoans, Markets.TRADING_UNIT_ASSET, this, MarketType.ASSET_LOAN );
				
			} else {
				offerings[ MarketType.ASSET_LOAN.ordinal() ] = null;
			}
			
		// asset-against-bond market is closed
		} else {
			offerings[ MarketType.ASSET_LOAN.ordinal() ] = null;
		}
		
		// collateral-cash market is open, can only place an offer if there is enough cash left
		if ( this.markets.isCollateralMarket() && this.cash > Markets.TRADING_EPSILON ) {
			// pick random asset price from range: is the price of 1.0 unit of assets
			assetPriceInCash = randomRange( minCollateralPriceInCash, limitPriceCollateral );
			// calculate how much assets could be bought with the cash owned
			double assetAmount = this.cash / assetPriceInCash;
			// trade in chunks of TRADING_UNIT_ASSET but if TRADING_UNIT_ASSET > than the
			// tradeable amount assetAmount then trade the left amount of assetAmount assets
			assetAmount = Math.min( assetAmount, Markets.TRADING_UNIT_ASSET );
			
			offerings[ MarketType.COLLATERAL_CASH.ordinal() ] = 
					new BidOffering( assetPriceInCash, assetAmount, this, MarketType.COLLATERAL_CASH );	
		} else {
			offerings[ MarketType.COLLATERAL_CASH.ordinal() ] = null;
		}
	}

	private void calcAskOfferings( AskOffering[] offerings ) {
		double uncollAssets = this.getUncollateralizedAssets();
		double currentObligations = this.getCurrentObligations();
		
		// if there are still uncollateralized assets left, create a sell-offer
		if ( uncollAssets > Markets.TRADING_UNIT_ASSET ) {
			// want to SELL an asset against cash 
			// => giving asset to buayer
			// => getting cash from buyer
			// => can only do so it if there are uncollateralized assets left

			// this is always the price for 1.0 Units of asset - will be normalized during a Match
			// to the given amount below - the unit of this variable is CASH
			//double assetPriceInCash = randomRange( limitPriceAsset, maxAssetPriceInCash );
			double assetPriceInCash = randomRange( assetLimits[ 1 ][ 0 ], assetLimits[ 1 ][ 1 ] );
			offerings[ MarketType.ASSET_CASH.ordinal() ] = new AskOffering( assetPriceInCash, Markets.TRADING_UNIT_ASSET, this, MarketType.ASSET_CASH );
			
		// no more (not enough) uncollateralized assets left, can't sell anymore, don't place a sell-offer
		} else {
			offerings[ MarketType.ASSET_CASH.ordinal() ] = null;
		}
			
		//if ( this.markets.isLoanMarket() && this.assetEndow - Math.max( 0, tmp ) > Markets.TRADING_EPSILON ) {
		// loan-market is open AND there are still uncollateralized assets left
		// agent can place a sell-offer because when selling a loan, it needs to be secured by collateralizing 
		// the same amount of assets
		if ( this.markets.isLoanMarket() && uncollAssets > Markets.TRADING_EPSILON ) {
			// want to SELL a loan against cash: borrowing money from buyer by TAKING a loan
			// => collateralize assets of the same amount as security (taking loan)
			// => getting money from buyer  (borrowing money from buyer) 
			// => need to have enough uncollateralized assets
			
			// this is always the price for 1.0 Units of loans - will be normalized during a Match
			// to the given amount below - the unit of this variable is CASH
			//double loanPriceInCash = randomRange( limitPriceLoan, maxLoanPriceInCash );
			double loanPriceInCash = randomRange( loanLimits[ 1 ][ 0 ], loanLimits[ 1 ][ 1 ] );
			
			//double loanAmount = this.assetEndow - Math.max( 0, tmp );
			// the maximum of loans we can sell is the uncollateralized assets left (1:1 relationship when collateralizing)
			// but don't trad everything at once, break down into small chungs: Markets.TRADING_UNIT_LOAN
			double loanAmount = Math.min( uncollAssets, Markets.TRADING_UNIT_LOAN );
			
			offerings[ MarketType.LOAN_CASH.ordinal() ] = new AskOffering( loanPriceInCash, loanAmount, this, MarketType.LOAN_CASH );
			
		// either no more uncollaterlized assets left, can't sell loans because don't have assets to
		// secure the loan by collateralizing OR loan-market is closed
		} else {
			offerings[ MarketType.LOAN_CASH.ordinal() ] = null;
		}
		
		// asset-against-bond market is open, check if this agent can place sell-offers
		if ( this.markets.isABM() ) {
			// want to SELL a loan against an asset 
			// => giving asset to buyer
			// => getting bond from buyer: giving loan
			// => because of giving asset: need to have enough amount of uncollateralized assets
			
			// the price for 1.0 unit of assets in loans => the unit of this variable is LOANS
			//double assetPriceInLoans = randomRange( expectedAssetPriceInLoans, maxAssetPriceInLoans );
			double assetPriceInLoans = randomRange( assetLoanLimits[ 1 ][ 0 ], assetLoanLimits[ 1 ][ 1 ] );
			// calculating the amount loans which will be given to buyer
			double loanGivingAmount = Markets.TRADING_UNIT_ASSET * assetPriceInLoans;
			
			// calculate the amoung of uncollateralized assets AFTER trade. MUST ALWAYS be >= 0
			double uncollAssetsAfterTrade = this.assets;
			// collateral will be freed (giving loan), thus reducing collateral obligations
			// thus increasing uncollateralized assets after trade
			uncollAssetsAfterTrade -= this.getCollateralObligationsAfterTrade( -loanGivingAmount );
			// giving asset to buyer, thus decreasing uncollateralized assets after trade
			uncollAssetsAfterTrade -= Markets.TRADING_UNIT_ASSET;
						
			// cannot go short on assets: uncollateralized assets can NEVER be negative
			if ( uncollAssetsAfterTrade >= 0 ) {
				offerings[ MarketType.ASSET_LOAN.ordinal() ] = new AskOffering( assetPriceInLoans, Markets.TRADING_UNIT_ASSET, this, MarketType.ASSET_LOAN );
				
			} else {
				offerings[ MarketType.ASSET_LOAN.ordinal() ] = null;
			}
			
		// asset-against-bond market is closed, no sell-offers on this market
		} else {
			offerings[ MarketType.ASSET_LOAN.ordinal() ] = null;
		}

		// collateral-cash market is open, can only place offer if there are any collateralized assets around
		if ( this.markets.isCollateralMarket() && currentObligations > Markets.TRADING_EPSILON ) {
			// pick a random price from range: is the price for 1.0 Unit of (collateral) assets
			double assetPriceInCash = randomRange( limitPriceCollateral, maxCollateralPriceInCash );
			// calculate the amount of assets to trade: don't trade all but in small chunks of TRADING_UNIT_ASSET
			// if TRADING_UNIT_ASSET > than the tradeable amount of currentObligations take the rest of 
			// currentObligations to trade down to 0
			double assetAmount = Math.min( currentObligations, Markets.TRADING_UNIT_ASSET );	
			
			offerings[ MarketType.COLLATERAL_CASH.ordinal() ] = 
					new AskOffering( assetPriceInCash, assetAmount, this, MarketType.COLLATERAL_CASH );
		} else {
			offerings[ MarketType.COLLATERAL_CASH.ordinal() ] = null;
		}
	}

	public double calculateCashValueOfAsset( double assetAmount ) {
		return this.limitPriceAsset * assetAmount;
	}
	
	public double calculateCashValueOfLoan( double loanAmount ) {
		return this.limitPriceLoan * loanAmount;
	}
	
	public double calculateLoanValueOfAsset( double assetAmount ) {
		return this.limitPriceAssetLoans * assetAmount;
	}
	
	private void calculateOfferingLimits() {
		double pD = this.markets.pD();
		double pU = this.markets.pU();
		double V = this.markets.V();
		
		double minAssetPrice = pD;
		double maxLoanPrice = Math.min( pU, V );
		
		double maxAssetPrice = pU;
		double minLoanPrice = Math.min( pD, V );
		
		this.limitPriceAsset = this.markets.calculateLimitPriceAsset( this.h );
		this.limitPriceLoan = this.markets.calculateLimitPriceLoan( this.h );
		
		this.minAssetPriceInCash = Math.min( pD, this.limitPriceAsset );
		this.minLoanPriceInCash = Math.min( minLoanPrice, this.limitPriceLoan );
		this.minAssetPriceInLoans = minAssetPrice / maxLoanPrice;
		this.minCollateralPriceInCash = Math.min( 0, this.minAssetPriceInCash - this.maxLoanPriceInCash );
		
		this.maxAssetPriceInCash = Math.max( pU, this.limitPriceAsset );
		this.maxLoanPriceInCash = Math.max( maxLoanPrice, this.limitPriceLoan );
		this.maxAssetPriceInLoans = maxAssetPrice / minLoanPrice;
		this.maxCollateralPriceInCash = this.maxAssetPriceInCash - this.minLoanPriceInCash;
		
		this.limitPriceAssetLoans = this.limitPriceAsset / this.limitPriceLoan;
		this.limitPriceCollateral = this.limitPriceAsset - this.limitPriceLoan;
	}
	
	private static double randomRange( double min, double max ) {
		return min + Utils.THREADLOCAL_RANDOM.get().nextDouble() * ( max - min );
	}
}
