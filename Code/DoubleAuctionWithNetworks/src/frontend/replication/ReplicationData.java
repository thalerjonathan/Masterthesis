package frontend.replication;

import java.util.Date;
import java.util.List;

import backend.EquilibriumStatistics;
import backend.agents.Agent;
import frontend.experimenter.xml.result.ReplicationBean;

public class ReplicationData {

	private int number;
	private int taskId;
	private int totalTxCount;
	private int failedTxCount;
	
	private boolean canceled;
	private boolean tradingHalted;
	
	private Date startingTime;
	private Date endingTime;
	
	private List<Agent> finalAgents;
	
	private EquilibriumStatistics stats;
	
	public ReplicationData() {
	}

	public ReplicationData( ReplicationBean bean ) {
		this.setCanceled( bean.isCanceled() );
		this.setNumber( bean.getReplication() );
		this.setTaskId( bean.getTask() );
		this.setTradingHalted( bean.isTradingHalted() );
		this.setTotalTxCount( bean.getTotalTransactions() );
		this.setFailedTxCount( bean.getFailedTransactions() );
		this.setStartingTime( bean.getStartingTime() );
		this.setEndingTime( bean.getEndingTime() );
		this.setStats( new EquilibriumStatistics( bean.getEquilibrium() ) );
	}
	
	public List<Agent> getFinalAgents() {
		return finalAgents;
	}

	public void setFinalAgents(List<Agent> finalAgents) {
		this.finalAgents = finalAgents;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public int getTaskId() {
		return taskId;
	}

	public void setTaskId(int taskId) {
		this.taskId = taskId;
	}

	public int getTotalTxCount() {
		return totalTxCount;
	}

	public void setTotalTxCount(int totalTxCount) {
		this.totalTxCount = totalTxCount;
	}

	public int getFailedTxCount() {
		return failedTxCount;
	}

	public void setFailedTxCount(int failedTxCount) {
		this.failedTxCount = failedTxCount;
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

	public EquilibriumStatistics getStats() {
		return stats;
	}

	public void setStats(EquilibriumStatistics stats) {
		this.stats = stats;
	}

	public Date getStartingTime() {
		return startingTime;
	}

	public void setStartingTime(Date startingTime) {
		this.startingTime = startingTime;
	}

	public Date getEndingTime() {
		return endingTime;
	}

	public void setEndingTime(Date endingTime) {
		this.endingTime = endingTime;
	}
}
