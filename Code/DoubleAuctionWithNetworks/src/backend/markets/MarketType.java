package backend.markets;

public enum MarketType {
	// Trading ASSETS against CASH
	// ASKER sells an amount of assets and gets cash for it
	// BIDER buys an amount of assets and pays cash for it
	ASSET_CASH( true, false, true ),
	
	// Trading LOANS against CASH
	// ASKER sells a loan and gets cash for it but needs to collateralize the amount of assets the sold loan is worth
	// BIDER buys a loan and pays cash for it - no need to collateralize any assets
	LOAN_CASH( true, true, false ),
	
	// Trading ASSETS against LOANS
	// ASKER sells asset to buyer and gets loan from buyer
	// BIDER buys asset from seller and gives loan to seller
	ASSET_LOAN( false, true, true );
	
	private MarketType( boolean cashMarket, boolean loanMarket, boolean assetMarket ) {
		this.cashMarket = cashMarket;
		this.loanMarket = loanMarket;
		this.assetMarket = assetMarket;
	}
	
	private boolean cashMarket;
	private boolean loanMarket;
	private boolean assetMarket;
	
	public boolean isCashMarket() {
		return cashMarket;
	}
	
	public boolean isLoanMarket() {
		return loanMarket;
	}
	
	public boolean isAssetMarket() {
		return assetMarket;
	}
}
