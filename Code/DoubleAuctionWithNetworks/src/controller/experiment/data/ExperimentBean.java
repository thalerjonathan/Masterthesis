package controller.experiment.data;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import controller.replication.ReplicationsRunner.TerminationMode;
import backend.markets.LoanType;

@XmlRootElement( name = "experiment" )
public class ExperimentBean {

	private String name;
	private int agentCount;
	private LoanType loanType;
	private boolean assetLoanMarket;
	private boolean loanCashMarket;
	private boolean collateralCashMarket;
	private boolean importanceSampling;
	private TerminationMode terminationMode;
	private int maxTx;
	private int replications;
	private TopologyBean topology;
	
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
	
	public LoanType getLoanType() {
		return loanType;
	}

	public void setLoanType(LoanType loanType) {
		this.loanType = loanType;
	}

	public TopologyBean getTopology() {
		return topology;
	}
	
	@XmlElement
	public void setTopology(TopologyBean topology) {
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

	public boolean isCollateralCashMarket() {
		return collateralCashMarket;
	}

	public void setCollateralCashMarket(boolean collateralCashMarket) {
		this.collateralCashMarket = collateralCashMarket;
	}
}
