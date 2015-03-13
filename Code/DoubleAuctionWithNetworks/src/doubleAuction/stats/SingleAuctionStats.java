package doubleAuction.stats;

public class SingleAuctionStats {
	protected double finalPrice;
	protected double totalUtility;
	protected double totalAskUtility;
	protected double totalBidUtility;
	protected int auctionLength;
	protected int lastTransactionLength;
	protected double finalAskAssetPrice; 
	protected double finalBidAssetPrice;
	protected double finalMeanAskH;
	protected double finalMeanBidH;
	
	public double getFinalPrice() {
		return finalPrice;
	}
	public void setFinalPrice(double finalPrice) {
		this.finalPrice = finalPrice;
	}
	public double getFinalAskAssetPrice() {
		return finalAskAssetPrice;
	}
	public void setFinalAskAssetPrice(double finalAskAssetPrice) {
		this.finalAskAssetPrice = finalAskAssetPrice;
	}
	public double getFinalBidAssetPrice() {
		return finalBidAssetPrice;
	}
	public void setFinalBidAssetPrice(double finalBidAssetPrice) {
		this.finalBidAssetPrice = finalBidAssetPrice;
	}
	public double getFinalMeanAskH() {
		return finalMeanAskH;
	}
	public void setFinalMeanAskH(double finalMeanAskH) {
		this.finalMeanAskH = finalMeanAskH;
	}
	public double getFinalMeanBidH() {
		return finalMeanBidH;
	}
	public void setFinalMeanBidH(double finalMeanBidH) {
		this.finalMeanBidH = finalMeanBidH;
	}
	public int getAuctionLength() {
		return auctionLength;
	}
	public void setAuctionLength(int auctionLength) {
		this.auctionLength = auctionLength;
	}
	public int getLastTransactionLength() {
		return lastTransactionLength;
	}
	public void setLastTransactionLength(int lastTransactionLength) {
		this.lastTransactionLength = lastTransactionLength;
	}
	public double getTotalUtility() {
		return totalUtility;
	}
	public void setTotalUtility(double totalUtility) {
		this.totalUtility = totalUtility;
	}
	public double getTotalAskUtility() {
		return totalAskUtility;
	}
	public void setTotalAskUtility(double totalAskUtility) {
		this.totalAskUtility = totalAskUtility;
	}
	public double getTotalBidUtility() {
		return totalBidUtility;
	}
	public void setTotalBidUtility(double totalBidUtility) {
		this.totalBidUtility = totalBidUtility;
	}

}
