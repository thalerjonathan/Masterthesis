package agents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import agents.markets.Asset;
import doubleAuction.Auction;
import doubleAuction.offer.AskOffering;
import doubleAuction.offer.BidOffering;
import doubleAuction.offer.MarketType;
import doubleAuction.offer.Offering;

public class Agent {
	//public static int NUM_AGENTS; 
	public static boolean TRADE_ONLY_FULL_UNITS;
	public final static double UNIT = 0.1;
	public final static double MAXUNIT = 0.5;
	
	protected final static double GAIN_INSENSIBILITY = 1E-04;
	protected final static double ASSETPRICE_INSENSIBILITY = 1E-04;
	protected final static double CONSUME_INSENSIBILITY = 1E-5;
	protected final static double ASSET_INSENSIBILITY = 1E-4;
	protected final static double DECISION_GAP = 2.0;
	protected final static double PMARGIN = 1.0;   //asset prices are samples from U(p-PMARGIN,p+PMARGIN
    
	protected Random agRand = new Random();
	
	protected int id;
	// optimism factor
	protected double h;
	// a reference to the asset-market
	protected Asset asset;
	// amount of consumption-good endowment (cash) still available
	protected double consumEndow;
	// amount of asset endowment still available
	protected double assetEndow;
	// Expected Value (E, Erwartungswert) of the Asset
	protected double limitPriceAsset;
	protected boolean assetBuyer=false, indifferent=false;

	public static int NUMMARKETS;
	
	private AskOffering[] currentAskOfferings;
	private BidOffering[] currentBidOfferings;
	
	private List<List<AskOffering>> bestAskOfferings;
	private List<List<BidOffering>> bestBidOfferings;
	
	protected double utility = 0, accUtility = 0, lastUtility = 0, lastLastUtility = 0, utilDiff=0;    
	protected double[][] decisions, lastDecisions, lastLastDecisions, assignedDecs;

	private boolean highlighted;
	

	public Agent(int id, double h, double consumEndow, double assetEndow, Asset asset) {
		this.id = id;
		this.h = h;
		this.consumEndow = consumEndow; 
		this.assetEndow = assetEndow;
		this.asset = asset;
		detLimitPriceAsset();
		this.highlighted = false;
	}
	
	public void reset( double consumEndow, double assetEndow ) {
		this.consumEndow = consumEndow; 
		this.assetEndow = assetEndow;
		this.highlighted = false;
		detLimitPriceAsset();
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
	
	public void setAsk()  {
		assetBuyer = false;
	}
	
	public void setBid() {
		assetBuyer = true;
	}
	
	public boolean isBidder()  {
		return assetBuyer;
	}
	
	public boolean isAsker()   {
		return (!assetBuyer);
	}
	
	public boolean isAssetBuyer()  {
		return assetBuyer;
	}
	
	public boolean isIndifferent()  {
		return indifferent;
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
	
	public AskOffering[] calcAskOfferings()  {
		//draw a random price uniformly out of [minP,maxP] intersect [limitPriceAsset,pU]
		double minP = getPMin();
		double maxP = getPMax();
		double assetPrice;
		AskOffering actAskOffer = null;
		
		if (maxP < limitPriceAsset)  //agent cannot offer at current price level 
			return null;
	
		// "...agents who always make bids which improve their utility but otherwise bid randomly."
		if ( minP < limitPriceAsset )
			assetPrice = limitPriceAsset + agRand.nextDouble()*(maxP-limitPriceAsset);
		else
			assetPrice = minP + agRand.nextDouble()*(maxP-minP);
			
		//assetPrice = limitPriceAsset + agRand.nextDouble()*(asset.getPU()-limitPriceAsset);
		
		if (TRADE_ONLY_FULL_UNITS) {
			if (assetEndow >= UNIT)
				actAskOffer = new AskOffering(assetPrice, this, 0, MarketType.ASSET_AGAINST_CASH );	
			else
				actAskOffer = null;
		} else  {
			if (assetEndow > 0)
				actAskOffer = new AskOffering(assetPrice, Math.min(assetEndow,MAXUNIT), this, 0, MarketType.ASSET_AGAINST_CASH );	
			else
				actAskOffer = null;
			
		}
		
		return new AskOffering[] { actAskOffer };
	}
	
	public BidOffering[] calcBidOfferings()  {
		//first version: draw a random price uniformly out of [minP,maxP] intersect [pD,limitPriceAsset] 
		double minP = getPMin();
		double maxP = getPMax();
		double assetPrice;
		BidOffering actBidOffer;
		
		if (minP > limitPriceAsset)  //agent cannot offer at current price level 
			return null;
	
		if ( maxP > limitPriceAsset )
			assetPrice = minP + agRand.nextDouble()*(limitPriceAsset-minP);
		else
			assetPrice = minP + agRand.nextDouble()*(maxP-minP);
			
	//		assetPrice = asset.getPD() + agRand.nextDouble()*(limitPriceAsset-asset.getPD());
			
		if (TRADE_ONLY_FULL_UNITS) {
			if (assetPrice*UNIT <= consumEndow)
				actBidOffer = new BidOffering(assetPrice, this, 0, MarketType.ASSET_AGAINST_CASH );
			else
				actBidOffer = null;
		} else {
			if (consumEndow>0)
				actBidOffer = new BidOffering(assetPrice, Math.min(consumEndow/assetPrice,MAXUNIT), this, 0, MarketType.ASSET_AGAINST_CASH );
			else
				actBidOffer = null;			
		}
		
		return new BidOffering[] { actBidOffer };
	}
	
	public boolean execTransaction(Offering[] match, boolean first)  {
		if (match[0].getAgent()==this)  {
		//agent is asker
			return execSellTransaction(match,true);
		} else {
			return execBuyTransaction(match,true);		
		}		
	}
	
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
			accUtility += (bid.getFinalAssetPrice() - limitPriceAsset)*UNIT;
		} else {
			consumEndow += myAsk.getFinalAssetPrice()*toSell;
			accUtility += (myAsk.getFinalAssetPrice() - limitPriceAsset)*UNIT;
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
			accUtility += (limitPriceAsset - ask.getFinalAssetPrice())*UNIT;
		}
		else {
			consumEndow -= myBid.getFinalAssetPrice()*toBuy;	
			accUtility += (limitPriceAsset - myBid.getFinalAssetPrice())*UNIT;
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

	public double getAccUtility() {
		return accUtility;
	}
	
	public void setAccUtility(double accUtility) {
		this.accUtility = accUtility;
	}
	
	protected void detLimitPriceAsset() {
		// calculate expected value (E, Erwartungswert) of the Asset
		limitPriceAsset = (asset.getPU() - asset.getPD())*h + asset.getPD();
	}
	
	protected double getPMax() {
		double refP = asset.getP();
		
		return Math.min(asset.getPU(), refP + PMARGIN);
	}
	
	protected double getPMin() {
		double refP = asset.getP();
		return Math.max(asset.getPD(), refP - PMARGIN);
	}
}
