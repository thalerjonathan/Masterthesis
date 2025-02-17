package controller.replication.data;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import backend.agents.Agent;

@XmlRootElement( name = "agent" )
public class AgentBean {

	private double h;
	private double cash;
	private double loan;
	private double assets;
	private double loanGiven;
	private double loanTaken;
	
	public AgentBean() {
	}
	
	public AgentBean( Agent a ) {
		this.setH( a.getH() );
		this.setAssets( a.getAssets() );
		this.setCash( a.getCash() );
		this.setLoan( a.getLoans() );
		this.setLoanGiven( a.getLoansGiven() );
		this.setLoanTaken( a.getLoansTaken() );
	}
	
	public double getH() {
		return h;
	}

	@XmlElement
	public void setH(double h) {
		this.h = h;
	}
	
	public double getCash() {
		return cash;
	}
	
	@XmlElement
	public void setCash(double cash) {
		this.cash = cash;
	}
	
	public double getLoan() {
		return loan;
	}
	
	@XmlElement
	public void setLoan(double loan) {
		this.loan = loan;
	}
	
	public double getAssets() {
		return assets;
	}
	
	@XmlElement
	public void setAssets(double assets) {
		this.assets = assets;
	}
	
	public double getLoanGiven() {
		return loanGiven;
	}
	
	@XmlElement
	public void setLoanGiven(double loanGiven) {
		this.loanGiven = loanGiven;
	}
	
	public double getLoanTaken() {
		return loanTaken;
	}
	
	@XmlElement
	public void setLoanTaken(double loanTaken) {
		this.loanTaken = loanTaken;
	}
}
