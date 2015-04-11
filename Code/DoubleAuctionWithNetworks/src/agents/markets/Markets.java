package agents.markets;

public class Markets {
	public final static double TRADING_UNIT = 0.1;
	public final static int NUMMARKETS = 3;
	public final static int NUMLOANS = 1;
	public final static int NUMMARKETTYPES = 3;
	
	public static boolean TRADE_ONLY_FULL_UNITS;

	private Asset asset;
	private Loans loans;
	
	public Markets() {
		double assetPrice = 0.6;
		double J = 0.2;
		double initialLoanPrice = 0.2;
		
		TRADE_ONLY_FULL_UNITS = true;
		
		this.asset = new Asset( assetPrice );
		this.loans = new Loans( initialLoanPrice, J );
	}
	
	public boolean isABM() {
		return false;
	}

	public boolean isBP() {
		return false;
	}
	
	public boolean isLoanMarket() {
		return false;
	}
	
	public Asset getAsset() {
		return this.asset;
	}
	
	public Loans getLoans() {
		return this.loans;
	}
}
