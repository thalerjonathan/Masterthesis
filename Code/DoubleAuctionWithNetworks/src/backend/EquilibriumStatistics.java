package backend;

import frontend.experimenter.xml.result.EquilibriumBean;

//used purely as a data-structure
public class EquilibriumStatistics {
	public double p;
	public double q;
	public double pq;
	public double i0;
	public double i1;
	public int i0Index;
	public int i1Index;
	public double i2;
	public double P;
	public double M;
	public double O;
	
	public EquilibriumStatistics() {
		this.i0Index = -1;
		this.i1Index = -1;
	}
	
	public EquilibriumStatistics( EquilibriumBean bean ) {
		this.p = bean.getAssetPrice();
		this.q = bean.getLoanPrice();
		this.pq = bean.getAssetLoanPrice();
		this.i0 = bean.getI0();
		this.i1 = bean.getI1();
		this.i2 = bean.getI2();
		this.P = bean.getP();
		this.M = bean.getM();
		this.O = bean.getO();
	}
}
