package controller.replication.data;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import backend.EquilibriumStatistics;


@XmlRootElement( name = "equilibrium" )
public class EquilibriumBean {

	private double assetPrice;
	private double loanPrice;
	private double assetLoanPrice;
	private double collateralPrice;
	
	private double i0;
	private double i1;
	private double i2;
	
	private double pessimistWealth;
	private double medianistWealth;
	private double optimistWealth;
	
	public EquilibriumBean() {
	}
	
	
	public EquilibriumBean( EquilibriumStatistics stats ) {
		this.setAssetPrice( stats.assetPrice );
		this.setLoanPrice( stats.loanPrice );
		this.setAssetLoanPrice( stats.assetLoanPrice );
		this.setI0( stats.i0 );
		this.setI1( stats.i1 );
		this.setI2( stats.i2 );
		this.setPessimistWealth( stats.pessimistWealth );
		this.setMedianistWealth( stats.medianistWealth );
		this.setOptimistWealth( stats.optimistWealth );
		this.setCollateralPrice( stats.collateralPrice );
	}
	
	public double getAssetPrice() {
		return assetPrice;
	}
	
	@XmlElement
	public void setAssetPrice(double assetPrice) {
		this.assetPrice = assetPrice;
	}
	
	public double getLoanPrice() {
		return loanPrice;
	}
	
	@XmlElement
	public void setLoanPrice(double loanPrice) {
		this.loanPrice = loanPrice;
	}
	
	public double getAssetLoanPrice() {
		return assetLoanPrice;
	}
	
	@XmlElement
	public void setAssetLoanPrice(double assetLoanPrice) {
		this.assetLoanPrice = assetLoanPrice;
	}
	
	public double getI0() {
		return i0;
	}
	
	@XmlElement
	public void setI0(double i0) {
		this.i0 = i0;
	}
	
	public double getI1() {
		return i1;
	}
	
	@XmlElement
	public void setI1(double i1) {
		this.i1 = i1;
	}
	
	public double getI2() {
		return i2;
	}
	
	@XmlElement
	public void setI2(double i2) {
		this.i2 = i2;
	}
	
	public double getPessimistWealth() {
		return pessimistWealth;
	}

	public void setPessimistWealth(double pessimistWealth) {
		this.pessimistWealth = pessimistWealth;
	}

	public double getMedianistWealth() {
		return medianistWealth;
	}

	public void setMedianistWealth(double medianistWealth) {
		this.medianistWealth = medianistWealth;
	}

	public double getOptimistWealth() {
		return optimistWealth;
	}

	public void setOptimistWealth(double optimistWealth) {
		this.optimistWealth = optimistWealth;
	}

	public double getCollateralPrice() {
		return collateralPrice;
	}
	
	public void setCollateralPrice(double collateralPrice) {
		this.collateralPrice = collateralPrice;
	}
}
