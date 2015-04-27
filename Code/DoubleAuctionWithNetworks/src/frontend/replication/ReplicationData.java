package frontend.replication;

import java.util.Date;
import java.util.List;

import backend.Auction.EquilibriumStatistics;
import backend.agents.Agent;
import frontend.replication.ReplicationPanel.TerminationMode;

public class ReplicationData {

	private int number;
	private int taskId;
	private long txCount;
	
	private boolean canceled;
	private boolean equilibrium;
	private boolean tradingHalted;
	
	private TerminationMode termination;
	
	private Date finishTime;
	
	private List<Agent> finalAgents;
	
	private EquilibriumStatistics stats;
	
	public ReplicationData() {
		this.finishTime = new Date();
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

	public long getTxCount() {
		return txCount;
	}

	public void setTxCount(long txCount) {
		this.txCount = txCount;
	}

	public boolean isCanceled() {
		return canceled;
	}

	public void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}

	public boolean isEquilibrium() {
		return equilibrium;
	}

	public void setEquilibrium(boolean equilibrium) {
		this.equilibrium = equilibrium;
	}

	public boolean isTradingHalted() {
		return tradingHalted;
	}

	public void setTradingHalted(boolean tradingHalted) {
		this.tradingHalted = tradingHalted;
	}

	public TerminationMode getTermination() {
		return termination;
	}

	public void setTermination(TerminationMode termination) {
		this.termination = termination;
	}

	public Date getFinishTime() {
		return finishTime;
	}

	public EquilibriumStatistics getStats() {
		return stats;
	}

	public void setStats(EquilibriumStatistics stats) {
		this.stats = stats;
	}
}
