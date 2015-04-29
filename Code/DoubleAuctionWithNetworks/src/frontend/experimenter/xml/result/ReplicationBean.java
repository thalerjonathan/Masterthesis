package frontend.experimenter.xml.result;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import frontend.replication.ReplicationData;

@XmlRootElement( name = "replication" )
public class ReplicationBean {

	private EquilibriumBean equilibrium;
	private int replication;
	private int task;
	private int transactions;
	private boolean reachedEquilibrium;
	private boolean canceled;
	private boolean tradingHalted;
	private Date finishedAt;
	
	public ReplicationBean() {
	}

	public ReplicationBean( ReplicationData data ) {
		EquilibriumBean replicationEquilibriumBean = new EquilibriumBean( data.getStats() );
		
		this.setCanceled( data.isCanceled() );
		this.setFinishedAt( data.getFinishTime() );
		this.setReachedEquilibrium( data.isEquilibrium() );
		this.setReplication( data.getNumber() );
		this.setTask( data.getTaskId() );
		this.setTradingHalted( data.isTradingHalted() );
		this.setTransactions( data.getTxCount() );
		
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

	public int getTransactions() {
		return transactions;
	}

	public void setTransactions(int transactions) {
		this.transactions = transactions;
	}

	public boolean isReachedEquilibrium() {
		return reachedEquilibrium;
	}

	public void setReachedEquilibrium(boolean reachedEquilibrium) {
		this.reachedEquilibrium = reachedEquilibrium;
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

	public Date getFinishedAt() {
		return finishedAt;
	}

	public void setFinishedAt(Date finishedAt) {
		this.finishedAt = finishedAt;
	}
}
