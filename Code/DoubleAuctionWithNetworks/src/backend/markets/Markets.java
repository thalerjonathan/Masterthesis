package backend.markets;

public class Markets {
	// price when market moves UP
	private double pU = Markets.UP_STATE_DEFAULT;
	// price when market moves DOWN
	private double pD = Markets.DOWN_STATE_DEFAULT;
	// the type of the loan
	private LoanType loanType = LoanType.LOAN_05;
	
	private boolean abm;
	private boolean bp;
	private boolean loanMarket;
	private boolean collateralMarket;
	
	private double cashEndow = 1.0;
	private double assetEndow = 1.0;
	
	public final static int NUMMARKETS = 4;
	
	public final static double TRADING_UNIT_ASSET = 0.1;
	public final static double TRADING_UNIT_LOAN = 0.2;

	// not too small otherwise would trade Bond/Cash for very long time 
	// as fractions get smaller and smaller
	public final static double TRADING_EPSILON = 0.0001;

	private final static double UP_STATE_DEFAULT = 1.0;
	private final static double DOWN_STATE_DEFAULT = 0.2;
	
	public Markets() {
		this.abm = true;
		this.bp = true;
		this.loanMarket = true;
		this.collateralMarket = true;
	}
	
	public Markets( LoanType loantype ) {
		this( Markets.DOWN_STATE_DEFAULT, Markets.UP_STATE_DEFAULT, loantype );
	}
	
	private Markets( double pD, double pU, LoanType loantype ) {
		this();
		this.pD = pD;
		this.pU = pU;		
		this.loanType = loantype;
	}

	public double calculateLimitPriceAsset( double h ) {
		return h * this.pU + ( 1.0 - h ) * this.pD;
	}
	
	public double calculateLimitPriceLoan( double h ) {
		return h * this.loanType.V() + ( 1.0 - h ) * this.pD;
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

	public void setCollateralMarket( boolean collateralMarket ) {
		this.collateralMarket = collateralMarket;
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
	
	public boolean isCollateralMarket() {
		return collateralMarket;
	}
	
	public double pU() {
		return this.pU;
	}
	
	public double pD() {
		return this.pD;
	}
	
	public double V() {
		return this.loanType.V();
	}

	public void setLoanType( LoanType loanType ) {
		this.loanType = loanType;
	}
	
	public double getCashEndowment() {
		return cashEndow;
	}

	public double getAssetEndowment() {
		return assetEndow;
	}
}
