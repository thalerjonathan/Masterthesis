package agents;

import agents.markets.Asset;
import agents.markets.Loans;
import doubleAuction.offer.AskOffering;
import doubleAuction.offer.AskOfferingWithLoans;
import doubleAuction.offer.BidOffering;
import doubleAuction.offer.BidOfferingWithLoans;
import doubleAuction.offer.Offering;

public class AgentWithLoans extends Agent {

	// a reference to the loan-market (bonds)
	protected Loans loans;
	protected int NUMLOANS;
	protected double[] jJ, limitPricesLoansBuy, limitPricesLoansSell;

	protected double[] expectJ;
	// the amount of unpledged assets, this amount can be sold or pledged when selling bonds
	protected double freeAssetEndow;
	// the amount of loans taken from other agents
	protected double[] loanTaken;
	// the amount of loans given to other agents
	protected double[] loanGiven;
	// the loan typ: is an index into the loan-array
	protected int loanType;

	protected static final double QMARGIN = 1.0; // loan prices are samples from
									// U(q-PMARGIN,q+PMARGIN

	public AgentWithLoans(int id, double h, double consumEndow,
			double assetEndow, Loans loans, Asset asset) {

		super(id, h, consumEndow, assetEndow, asset);
		this.loans = loans;
		this.jJ = loans.getJ();
		NUMLOANS = Loans.NUMLOANS;
		limitPricesLoansBuy = new double[NUMLOANS];
		limitPricesLoansSell = new double[NUMLOANS];
		detLimitPricesLoansSeller();

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

	public double[] getLoanGiven() {
		return loanGiven;
	}

	public void setLoanGiven(double[] loanGiven) {
		this.loanGiven = loanGiven;
	}

	public double getFreeAssetEndow() {
		return freeAssetEndow;
	}

	public void setFreeAssetEndow(double freeAssetEndow) {
		this.freeAssetEndow = freeAssetEndow;
	}

	@Override
	public void calcOfferings(AskOffering[] askOfferings, BidOffering[] bidOfferings) {
		calcAskOfferings(askOfferings);
		calcBidOfferings(bidOfferings);
	}

	public void calcAskOfferings(AskOffering[] askOfferings) {
		int numTrials;
		int MAXTRIALS = 500;
		double assetAmount;

		double minP = getPMin();
		double maxP = getPMax();
		double minQ;
		double maxQ;
		double pa, qa; // assetPrice, loanPrice

		// market type 0: generate an ask offer for market m=0: sell an asset
		// against cash
		if (freeAssetEndow > 0 && maxP > limitPriceAsset) {
			// draw a uniform random asset price from [limitPriceAsset,PU]
			// intersect [minP,maxP]
			// double pa = limitPriceAsset +
			// agRand.nextDouble()*(asset.getPU()-limitPriceAsset);
			
			double r = agRand.nextDouble();
			
			// if improment possible
			if (minP < limitPriceAsset) {
				// sell asset at least for limitPriceAsset and maximum in a range up to how much the optimism-factor h allows it
				pa = limitPriceAsset + r * (maxP - limitPriceAsset);
			} else {
				// otherwise bid randomly in random range between min and max
				pa = minP + r * (maxP - minP);
			}
			
			askOfferings[0] = new AskOffering(pa, Math.min(UNIT, freeAssetEndow), this, 0, 0);
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

		if (!Loans.NOASSETFORLOANMARKET)
			for (int j = 0; j < NUMLOANS; j++) {
				// loop an markets/loan types
				// V2
				// minQ = getQMin(j);
				// maxQ = getQMax(j);
				// end V2

				// V1
				minQ = 0.0;
				maxQ = jJ[j];
				minP = limitPriceAsset;
				maxP = 1.0;
				// endV1

				if (freeAssetEndow <= 0) {
					askOfferings[j + 1] = null;
				} else if (minQ > 0 && (limitPriceAsset + (maxP / minQ) * expectJ[j]) <= 0) {
					askOfferings[j + 1] = null;
				} else {
					numTrials = MAXTRIALS;
					// double posUtilFactor = limitPriceAsset/expectJ[j];
					// double qa=loans.getJ()[j]*agRand.nextDouble();; //initial
					// asset price and loan price such that we enter always the
					// "while"
					qa = minQ + agRand.nextDouble() * (maxQ - minQ);
					pa = minP + agRand.nextDouble() * (maxP - minP);
					// pa= asset.pD + agRand.nextDouble()*(asset.pU - asset.pD);
					// while ((numTrials > 0) && posUtilFactor*qa>=1) {
					while ((numTrials > 0)
							&& (-limitPriceAsset + (pa / qa) * expectJ[j] < 0)) {
						// negative utility and we may try another draw from
						// random prices
						// pa = asset.pD + agRand.nextDouble()*(asset.pU -
						// asset.pD);
						pa = minP + agRand.nextDouble() * (maxP - minP);
						qa = minQ + agRand.nextDouble() * (maxQ - minQ);
						// qa = loans.getJ()[j]*agRand.nextDouble();
						numTrials--;
					}
					
					if (numTrials > 0) {
						// found asset and loan price with non-negative utility
						// pa = posUtilFactor*qa +
						// agRand.nextDouble()*(1-posUtilFactor*qa);
						// if ((pa-limitPriceAsset+(pa/qa)*(expectJ[j]-qa) < 0))
						// System.err.println("in calcAskOfferings: negative utility gain");
						assetAmount = Math.min(UNIT, freeAssetEndow);
						askOfferings[j + 1] = new AskOfferingWithLoans(pa,
								assetAmount, qa, j, loans.getJ()[j],
								assetAmount * pa / qa, this, j + 1, 1);
					} else {
						System.out
								.println("in calcAskOfferings: after "
										+ MAXTRIALS
										+ " trials no (pa,qa) with positive utility found");
						askOfferings[j + 1] = null;
					}
				}
			}

		// market type 2: find an ask offer for market m=NUMLOANS+1 to
		// 2*NUMLOANS: give (buy) a loan of type j=m-NUMLOANS-1
		// draw random loan price qa from Uniform(0,expectJ[j]),
		// i.e. prices that guarantee a non-negative utility for agent h

		if (!Loans.NOLOANMARKET)
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
							this, 1 + NUMLOANS + j, 2);
				} else {
					// agent has nothing to borrow
					askOfferings[1 + NUMLOANS + j] = null;
				}
			}

	}

	public void calcBidOfferings(BidOffering[] bidOfferings) {
		int numTrials;
		int MAXTRIALS = 200;
		double assetAmount;
		double minP = getPMin();
		double maxP = getPMax();
		double minQ;
		double maxQ;
		double pb, qb;

		// market type 0: generate an bid offer for market m=0: buy an asset
		// against cash

		if (consumEndow > 0) {
			// pb = asset.getPD() +
			// agRand.nextDouble()*(limitPriceAsset-asset.getPD());
			if (minP > limitPriceAsset) { // agent cannot buy at current price
											// level
				bidOfferings[0] = null;
			} else {
				double r = agRand.nextDouble();
				if (maxP > limitPriceAsset) {
					pb = minP + r * (limitPriceAsset - minP);
				} else {
					pb = minP + r * (maxP - minP);
				}
				
				assetAmount = Math.min(UNIT, consumEndow / pb);
				bidOfferings[0] = new BidOffering(pb, assetAmount, this, 0, 0);
			}

			for (int i = 1; i < bidOfferings.length; i++)
				bidOfferings[i] = null;

		} else {
			// no cash or : try to buy with loans
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
				// V2
				//minQ = getQMin(j);
				//maxQ = getQMax(j);
				// end V2
				// V1
				 minP = 0.2;
				 maxP = limitPriceAsset;
				 minQ = 0.0;
				 maxQ = jJ[j];
				// end V1
				if (limitPriceAsset - (minP / maxQ) * expectJ[j] > 0) {
					for (int i = numTrials; i > 0; i--) {
						// pb = asset.pD + agRand.nextDouble()*(asset.pU -
						// asset.pD);
						pb = minP + agRand.nextDouble() * (maxP - minP);
						// double qb = agRand.nextDouble();
						// if ((numTrials > 0) && (limitPriceAsset-pb +
						// (pb/qb)*(qb - expectJ[j]) >= 0)) {
						// double posUtilFactor = limitPriceAsset/expectJ[j];
						qb = minQ + agRand.nextDouble() * (maxQ - minQ);
						// qb=loans.getJ()[j]*agRand.nextDouble();
						// double pb = asset.pD +
						// agRand.nextDouble()*(Math.min(asset.pU,
						// qb*posUtilFactor));
						if (limitPriceAsset - (pb / qb) * expectJ[j] >= 0) {
							// nonnegative utility
							// try to buy UNIT assets
							double loanAmount = pb * UNIT / qb;
							double lowerEndLoanPrice = Math.max(0, pb * UNIT
									/ (freeAssetEndow + UNIT));
							// minimum loan price for fulfilling collateral
							// constraint
							if (qb >= lowerEndLoanPrice) {
								// nice: bid offer fulfills the collateral
								// requirement
								bidOfferings[j + 1] = new BidOfferingWithLoans(
										pb, UNIT, qb, j, loans.getJ()[j],
										loanAmount, this, j + 1, 1);
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
													/ qb, this, j + 1, 1);
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
	}

	@Override
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
		accUtility += loanAmount * (expectJ[bid.getLoanType()] - bid.getFinalLoanPrice());
		
		if (consumEndow < CONSUME_INSENSIBILITY) {
			if (consumEndow < -CONSUME_INSENSIBILITY)
				System.err.println("error in agent.execLoanTransaction: wants to loan too much");
			else
				consumEndow = 0;
		}
		
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

		if (myAsk.getMarketType() == 0) {
			// asset against cash
			consumEndow += myAsk.getFinalAssetPrice() * toSell;
			accUtility += (myAsk.getFinalAssetPrice() - limitPriceAsset)
					* toSell;
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
			accUtility += (myAsk.getFinalAssetPrice() - limitPriceAsset)
					* toSell
					+ loanAmount
					* (expectJ[((AskOfferingWithLoans) myAsk).getLoanType()] - ((AskOfferingWithLoans) myAsk)
							.getFinalLoanPrice());
		}
		return true;
	}

	public boolean execBuyTransaction(Offering[] match) {
		AskOffering ask = (AskOffering) match[0];
		BidOffering myBid = (BidOffering) match[1];

		double toBuy = Math.min(myBid.getAssetAmount(), ask.getAssetAmount());
		double loanAmount = 0;

		// adjust bidders amounts
		myBid.setAssetAmount(toBuy);

		if (myBid.getMarketType() == 1) {
			// adjust loan amount
			loanAmount = toBuy * myBid.getFinalAssetPrice()
					/ ((BidOfferingWithLoans) myBid).getFinalLoanPrice();
			((BidOfferingWithLoans) myBid).setLoanAmount(loanAmount);

		}
		
		assetEndow += toBuy;
		freeAssetEndow += toBuy;

		if (myBid.getMarketType() == 0) {
			// asset against cash
			consumEndow -= myBid.getFinalAssetPrice() * toBuy;
			accUtility += (limitPriceAsset - myBid.getFinalAssetPrice()) * toBuy;
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
			accUtility += (limitPriceAsset - myBid.getFinalAssetPrice())
					* toBuy
					+ loanAmount
					* (((BidOfferingWithLoans) myBid).getFinalLoanPrice() - expectJ[((BidOfferingWithLoans) myBid)
							.getLoanType()]);
			if (freeAssetEndow < ASSET_INSENSIBILITY) {
				if (freeAssetEndow < -ASSET_INSENSIBILITY)
					System.err.println("error in agent.execBidTransaction: wants to collaterate too much");
				else
					freeAssetEndow = 0;
			}
		}
		
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
