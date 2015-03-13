package doubleAuction.stats;

public class SingleAuctionStatsWithLoans extends SingleAuctionStats {

	protected double finalLoanPrices[];
	protected int finalLoanType;
	protected double[][] finalMeanLoanAskers;
	protected double[][] finalMeanLoanBidders;
	protected double y0 = -1;
	protected double y1 = -1;
	protected double meanCashP;
	protected double meanLoanM;
	protected double meanAssetsO;

	public double[] getFinalLoanPrices() {
		return finalLoanPrices;
	}

	public void setFinalLoanPrices(double[] finalLoanPrices) {
		this.finalLoanPrices = finalLoanPrices;
	}

	public double[][] getFinalMeanLoanAskers() {
		return finalMeanLoanAskers;
	}

	public void setFinalMeanLoanAskers(double[][] finalMeanLoanAskers) {
		this.finalMeanLoanAskers = finalMeanLoanAskers;
	}

	public double[][] getFinalMeanLoanBidders() {
		return finalMeanLoanBidders;
	}

	public void setFinalMeanLoanBidders(double[][] finalMeanLoanBidders) {
		this.finalMeanLoanBidders = finalMeanLoanBidders;
	}

	public int getFinalLoanType() {
		return finalLoanType;
	}

	public void setFinalLoanType(int finalLoanType) {
		this.finalLoanType = finalLoanType;
	}

	public void setY0(double y0) {
		this.y0 = y0;
	}

	public void setY1(double y1) {
		this.y1 = y1;		
	}

	public void setMeanCashP(double meanCashPs) {
		this.meanCashP = meanCashPs;	
	}

	public void setMeanLoanM(double meanLoanMs) {
		this.meanLoanM = meanLoanMs;	
	}

	public void setMeanAssetsO(double meanAssetsOs) {
		this.meanAssetsO = meanAssetsOs;	
	}

	public double getY0() {
		return y0;
	}

	public double getY1() {
		return y1;
	}

	public double getMeanCashP() {
		return meanCashP;
	}

	public double getMeanLoanM() {
		return meanLoanM;
	}
	
	public double getMeanAssetsO() {
		return meanAssetsO;
	}
}
