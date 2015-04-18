package frontend.replication;

import java.util.Date;
import java.util.List;

import backend.agents.Agent;

public class ReplicationData {

	private int number;
	private int taskId;
	private long txCount;
	
	private boolean wasCanceled;
	private boolean reachedEquilibrium;
	private Date finishTime;
	
	private List<Agent> finalAgents;
	
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

	public boolean isWasCanceled() {
		return wasCanceled;
	}

	public void setWasCanceled(boolean wasCanceled) {
		this.wasCanceled = wasCanceled;
	}

	public boolean isReachedEquilibrium() {
		return reachedEquilibrium;
	}

	public void setReachedEquilibrium(boolean reachedEquilibrium) {
		this.reachedEquilibrium = reachedEquilibrium;
	}

	public Date getFinishTime() {
		return finishTime;
	}

	public void setFinishTime(Date finishTime) {
		this.finishTime = finishTime;
	}
	
	
}
