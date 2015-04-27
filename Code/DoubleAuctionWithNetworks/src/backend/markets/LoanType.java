package backend.markets;

public enum LoanType {

	LOAN_03( 0.3, 0.737, 0.257, 0.576, 0.724 ),
	LOAN_04( 0.4, 0.726, 0.316, 0.579, 0.762 ),
	LOAN_05( 0.5, 0.716, 0.375, 0.583, 0.801 );
	
	private double v;
	private double p;
	private double q;
	private double i1;
	private double i2;
	
	private LoanType( double V, double p, double q, double i1, double i2 ) {
		this.v = V;
		this.p = p;
		this.q = q;
		this.i1 = i1;
		this.i2 = i2;
	}
	
	public double V() {
		return this.v;
	}
	
	public double p() {
		return this.p;
	}
	
	public double q() {
		return this.q;
	}
	
	public double i1() {
		return this.i1;
	}
	
	public double i2() {
		return this.i2;
	}
}
