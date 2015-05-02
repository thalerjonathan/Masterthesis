package frontend.experimenter.xml.result;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import frontend.experimenter.xml.experiment.ExperimentBean;

@XmlRootElement( name = "result" )
public class ResultBean {

	private ExperimentBean experiment;
	private EquilibriumBean equilibrium;
	private List<AgentBean> agents;
	private List<ReplicationBean> replications;
	
	private Date startingTime;
	private Date endingTime;
	private int durationSeconds;
	
	private double meanTotalTransactions;
	private double meanFailedTransactions;
	
	public List<ReplicationBean> getReplications() {
		return replications;
	}

	@XmlElement( name = "replications" )
	public void setReplications(List<ReplicationBean> replications) {
		this.replications = replications;
	}

	public ExperimentBean getExperiment() {
		return experiment;
	}
	
	@XmlElement( name = "experiment" )
	public void setExperiment(ExperimentBean experiment) {
		this.experiment = experiment;
	}
	
	public EquilibriumBean getEquilibrium() {
		return equilibrium;
	}
	
	@XmlElement( name = "equilibrium" )
	public void setEquilibrium(EquilibriumBean equilibrium) {
		this.equilibrium = equilibrium;
	}
	
	public List<AgentBean> getAgents() {
		return agents;
	}
	
	@XmlElement( name = "agents" )
	public void setAgents(List<AgentBean> agents) {
		this.agents = agents;
	}

	public int getDurationSeconds() {
		return durationSeconds;
	}

	public void setDurationSeconds(int durationSeconds) {
		this.durationSeconds = durationSeconds;
	}

	public double getMeanTotalTransactions() {
		return meanTotalTransactions;
	}

	public void setMeanTotalTransactions(double meanTotalTransactions) {
		this.meanTotalTransactions = meanTotalTransactions;
	}

	public double getMeanFailedTransactions() {
		return meanFailedTransactions;
	}

	public void setMeanFailedTransactions(double meanFailedTransactions) {
		this.meanFailedTransactions = meanFailedTransactions;
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
