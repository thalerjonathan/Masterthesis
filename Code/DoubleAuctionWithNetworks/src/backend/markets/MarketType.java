package backend.markets;

public enum MarketType {
	// Trading ASSETS against CASH
	// ASKER sells an amount of assets and gets cash for it
	// BIDER buys an amount of assets and pays cash for it
	ASSET_CASH,
	
	// Trading LOANS against CASH
	// ASKER sells a loan and gets cash for it but needs to collateralize the amount of assets the sold loan is worth
	// BIDER buys a loan and pays cash for it - no need to collateralize any assets
	LOAN_CASH,
	
	// Trading ASSETS against LOANS
	// ASKER sells asset to buyer by giving loan to buyer
	// BIDER buys asset from seller by taking loan from seller
	ASSET_LOAN,
	
	// Trading COLLATERALIZED ASSETS against CASH
	// ASKER sells an amount of COLLATERLIZED assets by giving asset AND loan to buyer
	// BIDER buys an amount of COLLATERALZED assets by giving cash to buyer
	COLLATERAL_CASH
}
