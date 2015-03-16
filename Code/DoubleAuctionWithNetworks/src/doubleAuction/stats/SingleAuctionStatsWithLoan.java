package doubleAuction.stats;

public class SingleAuctionStatsWithLoan extends SingleAuctionStats {

	protected double finalLoanPrice;
	protected double finalMeanLoanAskers;
	protected double finalMeanLoanBidders;
	
	public double getFinalLoanPrice() {
		return finalLoanPrice;
	}

	public void setFinalLoanPrice(double finalLoanPrice) {
		this.finalLoanPrice = finalLoanPrice;
	}

	public double getFinalMeanLoanAskers() {
		return finalMeanLoanAskers;
	}

	public void setFinalMeanLoanAskers(double finalMeanLoanAskers) {
		this.finalMeanLoanAskers = finalMeanLoanAskers;
	}

	public double getFinalMeanLoanBidders() {
		return finalMeanLoanBidders;
	}

	public void setFinalMeanLoanBidders(double finalMeanLoanBidders) {
		this.finalMeanLoanBidders = finalMeanLoanBidders;
	}
}
