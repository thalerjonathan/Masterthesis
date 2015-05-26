package backend;

import controller.replication.data.EquilibriumBean;

//used purely as a data-structure
public class EquilibriumStatistics {
	public double assetPrice;
	public double loanPrice;
	public double assetLoanPrice;
	public double collateralPrice;
	
	public double i0;
	public double i1;
	public double i2;
	
	public double pessimistWealth;
	public double medianistWealth;
	public double optimistWealth;
	
	public int i0Index;
	public int i1Index;
	public int i2Index;
	
	public EquilibriumStatistics() {
		this.i0Index = -1;
		this.i1Index = -1;
		this.i2Index = -1;
	}
	
	public EquilibriumStatistics( EquilibriumBean bean ) {
		this.assetPrice = bean.getAssetPrice();
		this.loanPrice = bean.getLoanPrice();
		this.assetLoanPrice = bean.getAssetLoanPrice();
		this.collateralPrice = bean.getCollateralPrice();
		
		this.i0 = bean.getI0();
		this.i1 = bean.getI1();
		this.i2 = bean.getI2();
		
		this.pessimistWealth = bean.getPessimistWealth();
		this.medianistWealth = bean.getMedianistWealth();
		this.optimistWealth = bean.getOptimistWealth();
	}
}
