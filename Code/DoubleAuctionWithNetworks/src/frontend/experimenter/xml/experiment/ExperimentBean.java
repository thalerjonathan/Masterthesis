package frontend.experimenter.xml.experiment;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import frontend.replication.ReplicationPanel.TerminationMode;

@XmlRootElement( name = "experiment" )
public class ExperimentBean {

	private String name;
	private int agentCount;
	private double faceValue;
	private String topology;
	private boolean assetLoanMarket;
	private boolean loanCashMarket;
	private boolean bondsPledgeability;
	private boolean parallelEvaluation;
	private boolean importanceSampling;
	private TerminationMode terminationMode;
	private int maxTx;
	private int replications;
	
	public ExperimentBean() {
		
	}
	
	public String getName() {
		return name;
	}

	@XmlElement
	public void setName(String name) {
		this.name = name;
	}

	public int getAgentCount() {
		return agentCount;
	}
	
	@XmlElement
	public void setAgentCount(int agentCount) {
		this.agentCount = agentCount;
	}
	
	public double getFaceValue() {
		return faceValue;
	}
	
	@XmlElement
	public void setFaceValue(double faceValue) {
		this.faceValue = faceValue;
	}
	
	public String getTopology() {
		return topology;
	}
	
	@XmlElement
	public void setTopology(String topology) {
		this.topology = topology;
	}
	
	public boolean isAssetLoanMarket() {
		return assetLoanMarket;
	}
	
	@XmlElement
	public void setAssetLoanMarket(boolean assetLoanMarket) {
		this.assetLoanMarket = assetLoanMarket;
	}
	
	public boolean isLoanCashMarket() {
		return loanCashMarket;
	}
	
	@XmlElement
	public void setLoanCashMarket(boolean loanCashMarket) {
		this.loanCashMarket = loanCashMarket;
	}
	
	public boolean isBondsPledgeability() {
		return bondsPledgeability;
	}
	
	@XmlElement
	public void setBondsPledgeability(boolean bondsPledgeability) {
		this.bondsPledgeability = bondsPledgeability;
	}
	
	public boolean isParallelEvaluation() {
		return parallelEvaluation;
	}
	
	@XmlElement
	public void setParallelEvaluation(boolean parallelEvaluation) {
		this.parallelEvaluation = parallelEvaluation;
	}
	
	public boolean isImportanceSampling() {
		return importanceSampling;
	}
	
	@XmlElement
	public void setImportanceSampling(boolean importanceSampling) {
		this.importanceSampling = importanceSampling;
	}
	
	public TerminationMode getTerminationMode() {
		return terminationMode;
	}
	
	@XmlElement
	public void setTerminationMode(TerminationMode terminationMode) {
		this.terminationMode = terminationMode;
	}
	
	public int getMaxTx() {
		return maxTx;
	}
	
	@XmlElement
	public void setMaxTx(int maxTx) {
		this.maxTx = maxTx;
	}
	
	public int getReplications() {
		return replications;
	}
	
	@XmlElement
	public void setReplications(int replications) {
		this.replications = replications;
	}
}
