package frontend.replication;

import java.util.List;

import backend.agents.Agent;

public class ReplicationData {

	private int number;
	private int taskId;
	private long txCount;
	
	private List<Agent> finalAgents;
	
	public ReplicationData() {
		
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
	
	
}
