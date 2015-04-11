package agents.markets;

// the loan-market
public class Loans {
	protected double loanPrice;
	protected double faceValue;
	protected double[] priceLag;
	protected int lagCount;
	protected double MAXPRICE = 1;
	protected double PRECISION = 0;
	protected double STARTSTEPSIZE = 0.01;
	protected double stepSize = 0.01;
	protected int LAGSIZE = 10;

	public Loans( double loanPrice, double faceValue )  {
		this.loanPrice = loanPrice;
		this.faceValue = faceValue;
		priceLag = new double[LAGSIZE];
		lagCount = 0;
	}
	
	public void setStepsize(double stepSize) {
		this.stepSize = stepSize;
	}
	
	public double getStepsize() {
		return stepSize;
	}
	
	public void resetStepsize() {
		this.stepSize = STARTSTEPSIZE;
	}
	
	public double getLoanPrice() {
		return this.loanPrice;
	}
	
	/*
	public void setLoanPrice(double price) {
		this.loanPrice = price;	
	}
	
	
	public void setLoanPrice (double price, int type)  {
		loanPrices[type] = price;
	}

	public double[] getLoanPrices()  {
		double[] rLoanPrices = new double[NUMLOANS];
		
		for (int i=0; i<NUMLOANS; i++)  {
			rLoanPrices[i] = loanPrices[i];
		}		
		return rLoanPrices;
	}
	*/
	
	public void updatePrice(double price)  {
		if (lagCount >= LAGSIZE) {
			loanPrice = (loanPrice*LAGSIZE-priceLag[lagCount%LAGSIZE] + price)/LAGSIZE;  //running average over last LAGSIZE prices
			priceLag[lagCount%LAGSIZE] = price;
		}
		else  {
			loanPrice = loanPrice*lagCount + price;
			loanPrice = loanPrice/(lagCount+1);
			priceLag[lagCount] = price;
		}
		lagCount++;		
	}
	
	public double getFaceValue()  {
		return this.faceValue;
	}
}
