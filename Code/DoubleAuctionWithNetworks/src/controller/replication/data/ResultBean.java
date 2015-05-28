package controller.replication.data;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import backend.agents.network.export.GraphBean;
import controller.experiment.data.ExperimentBean;

@XmlRootElement( name = "result" )
public class ResultBean {

	private ExperimentBean experiment;
	private EquilibriumBean equilibriumMean;
	private EquilibriumBean equilibriumVariance;
	private List<AgentBean> agents;
	private List<ReplicationBean> replications;
	private List<double[]> medianMarkets;
	
	private Date startingTime;
	private Date endingTime;
	private int duration;
	
	private double meanTotalTransactions;
	private double meanSuccessfulTransactions;
	private double meanFailedTransactions;
	
	private double stdTotalTransactions;
	private double stdSuccessfulTransactions;
	private double stdFailedTransactions;
	
	private double meanDuration;
	
	private GraphBean graph;
	
	public List<double[]> getMedianMarkets() {
		return medianMarkets;
	}

	public void setMedianMarkets(List<double[]> medianMarkets) {
		this.medianMarkets = medianMarkets;
	}

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
	
	public EquilibriumBean getEquilibriumMean() {
		return equilibriumMean;
	}

	public void setEquilibriumMean(EquilibriumBean equilibriumMean) {
		this.equilibriumMean = equilibriumMean;
	}

	public EquilibriumBean getEquilibriumVariance() {
		return equilibriumVariance;
	}

	public void setEquilibriumVariance(EquilibriumBean equilibriumVariance) {
		this.equilibriumVariance = equilibriumVariance;
	}

	public List<AgentBean> getAgents() {
		return agents;
	}
	
	@XmlElement( name = "agents" )
	public void setAgents(List<AgentBean> agents) {
		this.agents = agents;
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

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public double getMeanDuration() {
		return meanDuration;
	}

	public void setMeanDuration(double meanDuration) {
		this.meanDuration = meanDuration;
	}

	public double getMeanSuccessfulTransactions() {
		return meanSuccessfulTransactions;
	}

	public void setMeanSuccessfulTransactions(double meanSuccessfulTransactions) {
		this.meanSuccessfulTransactions = meanSuccessfulTransactions;
	}

	public double getStdTotalTransactions() {
		return stdTotalTransactions;
	}

	public void setStdTotalTransactions(double stdTotalTransactions) {
		this.stdTotalTransactions = stdTotalTransactions;
	}

	public double getStdSuccessfulTransactions() {
		return stdSuccessfulTransactions;
	}

	public void setStdSuccessfulTransactions(double stdSuccessfulTransactions) {
		this.stdSuccessfulTransactions = stdSuccessfulTransactions;
	}

	public double getStdFailedTransactions() {
		return stdFailedTransactions;
	}

	public void setStdFailedTransactions(double stdFailedTransactions) {
		this.stdFailedTransactions = stdFailedTransactions;
	}

	public GraphBean getGraph() {
		return graph;
	}

	public void setGraph(GraphBean graph) {
		this.graph = graph;
	}
}
