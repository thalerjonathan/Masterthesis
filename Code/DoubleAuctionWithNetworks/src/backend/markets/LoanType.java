package backend.markets;

public enum LoanType {

	LOAN_02( 0.2 ),
	LOAN_03( 0.3 ),
	LOAN_04( 0.4 ),
	LOAN_05( 0.5 ),
	LOAN_06( 0.6 ),
	LOAN_07( 0.7 ),
	LOAN_08( 0.8 ),
	LOAN_09( 0.9 );
	
	private double v;
	
	private LoanType( double V ) {
		this.v = V;
	}
	
	public double V() {
		return this.v;
	}
}
