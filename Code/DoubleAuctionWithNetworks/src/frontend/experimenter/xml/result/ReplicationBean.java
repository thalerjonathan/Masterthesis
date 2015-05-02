package frontend.experimenter.xml.result;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import frontend.replication.ReplicationData;

@XmlRootElement( name = "replication" )
public class ReplicationBean {

	private int replication;
	private int task;
	
	private int totalTransactions;
	private int failedTransactions;
	
	private boolean canceled;
	private boolean tradingHalted;
	
	private Date endingTime;
	private Date startingTime;
	
	private EquilibriumBean equilibrium;
	
	public ReplicationBean() {
	}

	public ReplicationBean( ReplicationData data ) {
		EquilibriumBean replicationEquilibriumBean = new EquilibriumBean( data.getStats() );
		
		this.setCanceled( data.isCanceled() );
		this.setStartingTime( data.getStartingTime() );
		this.setEndingTime( data.getEndingTime() );
		this.setReplication( data.getNumber() );
		this.setTask( data.getTaskId() );
		this.setTradingHalted( data.isTradingHalted() );
		this.setTotalTransactions( data.getTotalTxCount() );
		this.setFailedTransactions( data.getFailedTxCount() );
		
		this.setEquilibrium( replicationEquilibriumBean );
	}
	
	public EquilibriumBean getEquilibrium() {
		return equilibrium;
	}

	public void setEquilibrium(EquilibriumBean equilibrium) {
		this.equilibrium = equilibrium;
	}

	public int getReplication() {
		return replication;
	}

	public void setReplication(int replication) {
		this.replication = replication;
	}

	public int getTask() {
		return task;
	}

	public void setTask(int task) {
		this.task = task;
	}

	public int getTotalTransactions() {
		return totalTransactions;
	}

	public void setTotalTransactions(int transactions) {
		this.totalTransactions = transactions;
	}

	public int getFailedTransactions() {
		return failedTransactions;
	}

	public void setFailedTransactions(int failedTransactions) {
		this.failedTransactions = failedTransactions;
	}

	public boolean isCanceled() {
		return canceled;
	}

	public void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}

	public boolean isTradingHalted() {
		return tradingHalted;
	}

	public void setTradingHalted(boolean tradingHalted) {
		this.tradingHalted = tradingHalted;
	}

	public Date getEndingTime() {
		return endingTime;
	}

	public void setEndingTime(Date endingTime) {
		this.endingTime = endingTime;
	}

	public Date getStartingTime() {
		return startingTime;
	}

	public void setStartingTime(Date startingTime) {
		this.startingTime = startingTime;
	}
}
