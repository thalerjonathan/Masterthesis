package frontend.experimenter.xml.result;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement( name = "equilibrium" )
public class EquilibriumBean {

	private double assetPrice;
	private double loanPrice;
	private double assetLoanPrice;
	private double i0;
	private double i1;
	private double i2;
	private double P;
	private double M;
	private double O;
	
	public EquilibriumBean() {
		
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
	
	public double getP() {
		return P;
	}
	
	@XmlElement
	public void setP(double p) {
		P = p;
	}
	
	public double getM() {
		return M;
	}
	
	@XmlElement
	public void setM(double m) {
		M = m;
	}
	
	public double getO() {
		return O;
	}
	
	@XmlElement
	public void setO(double o) {
		O = o;
	}
}
