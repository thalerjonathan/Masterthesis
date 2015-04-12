package agents.markets;

public class Markets {
	// price when market moves UP
	private double pU = 1.0;
	// price when market moves DOWN
	private double pD = 0.2;
	// the face-value of the loan
	private double V = 0.2;
	
	private boolean abm;
	private boolean bp;
	private boolean loanMarket;
	
	public final static double TRADING_UNIT_ASSET = 0.1;
	public final static double TRADING_UNIT_LOAN = 0.2;
	
	public final static int NUMMARKETS = 3;
	public final static int NUMLOANS = 1;
	public final static int NUMMARKETTYPES = 3;
	
	public final static boolean TRADE_ONLY_FULL_UNITS = true;

	public Markets() {
		this.abm = true;
		this.bp = false;
		this.loanMarket = false;
	}
	
	public Markets( double pD, double pU, double V ) {
		this();
		this.pD = pD;
		this.pU = pU;		
		this.V = V;
	}

	public void setABM(boolean abm) {
		this.abm = abm;
	}

	public void setBP(boolean bp) {
		this.bp = bp;
	}

	public void setLoanMarket(boolean loanMarket) {
		this.loanMarket = loanMarket;
	}

	public boolean isABM() {
		return abm;
	}

	public boolean isBP() {
		return bp;
	}
	
	public boolean isLoanMarket() {
		return loanMarket;
	}
	
	public double pU() {
		return this.pU;
	}
	
	public double pD() {
		return this.pD;
	}
	
	public double V() {
		return this.V;
	}
}
