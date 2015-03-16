package agents.markets;

// the asset-market
public class Asset {
	// current price
	protected double p = 0;
   // price when market moves UP
   protected double pU = 1;
   // price when market moves DOWN
   protected double pD = 0.2;
   // minimal price, won't fall below
   protected double pMin = pD;
   // price ranges
   protected double[] pMax = {pU,pU};
   // TODO: ?
   protected double[] priceLag;
   // TODO: ?
   protected int lagCount = 0;
   protected double MAXPRICE = 1;
   protected double PRECISION = 0;
   protected double STARTSTEPSIZE = 0.01;
   protected double stepSize = 0.01;
   protected double lastStepSize = 0.01;
   protected double MAXASSETS = 0;
   protected int LAGSIZE = 10;

   public Asset(double p, double maxassets)  {
	   this.p = p;
	   MAXASSETS = maxassets;
	   priceLag = new double[LAGSIZE];
   }
   
	public void setStepsize(double stepSize) {
		lastStepSize = this.stepSize;
		this.stepSize = stepSize;
	}
	
	public void resetStepsize() {
		this.stepSize = STARTSTEPSIZE;
	}
	
	public void resetStepsizeToLast() {
//		stepSize = lastStepSize;
	}
	
	public double getStepsize() {
		return stepSize;
	}
	
   public double getP() {
	   return p;
   }
   
   public void setP(double price)  {
	   p = price;
   }
   
   public double getMAXASSETS()  {
	   return MAXASSETS;
   }
   
   public double getPU() {
	   return pU;
   }
   
   public double getPD() {
	   return pD;
   }

	public double getPMin() {
		return pMin;
	}
	
	public void setPMin(double pMin) {
		this.pMin = pMin;
	}
	
	public double[] getPMax() {
		return pMax;
	}
	
	public void setPMax(double[] pMax) {
		this.pMax = pMax;
	}
	
	public void updatePrice(double assetPrice) {
		if (lagCount >= LAGSIZE) {
			p = (p*LAGSIZE-priceLag[lagCount%LAGSIZE] + assetPrice)/LAGSIZE;  //running average over last LAGSIZE prices
			priceLag[lagCount%LAGSIZE] = assetPrice;
		}
		else  {
			p = p*lagCount + assetPrice;
			p = p/(lagCount+1);
			priceLag[lagCount] = assetPrice;
		}
		lagCount++;		
	}
}
