package agents.markets;

// the loan-market
public class Loans {
	public static int NUMLOANS;
	public static boolean NOLOANMARKET = true;
	public static boolean NOASSETFORLOANMARKET = false;
	protected double[] loanPrices;
	// there are just a limited number of loans/bonds
	protected double[] j;
	protected double[][] priceLag;
	protected int[] lagCount;
	protected double MAXPRICE = 1;
	protected double PRECISION = 0;
	protected double STARTSTEPSIZE = 0.01;
	protected double stepSize = 0.01;
	protected int LAGSIZE = 10;

	public Loans(double[] loanPrices, double[] J)  {
		NUMLOANS = J.length;
		this.loanPrices = new double[NUMLOANS];
		this.j = new double[NUMLOANS];
		priceLag = new double[NUMLOANS][LAGSIZE];
		lagCount = new int[NUMLOANS];
		for (int i=0;i<NUMLOANS;i++) {
	        this.loanPrices[i] = loanPrices[i];
	        this.j[i] = J[i];
		}
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
	
	public void setLoanPrices(double[] prices) {
		for (int i=0; i<NUMLOANS; i++)  {
			loanPrices[i] = prices[i];
		}		
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
	
	public void updatePrice(double price, int type)  {
		if (lagCount[type] >= LAGSIZE) {
			loanPrices[type] = (loanPrices[type]*LAGSIZE-priceLag[type][lagCount[type]%LAGSIZE] + price)/LAGSIZE;  //running average over last LAGSIZE prices
			priceLag[type][lagCount[type]%LAGSIZE] = price;
		}
		else  {
			loanPrices[type] = loanPrices[type]*lagCount[type] + price;
			loanPrices[type] = loanPrices[type]/(lagCount[type]+1);
			priceLag[type][lagCount[type]] = price;
		}
		lagCount[type]++;		
	}
	
	public double[] getJ()  {
		double[] rJ = new double[NUMLOANS];
		for (int i=0; i<NUMLOANS; i++)  {
			rJ[i] = j[i];
		}		
		return rJ;
	}
}
