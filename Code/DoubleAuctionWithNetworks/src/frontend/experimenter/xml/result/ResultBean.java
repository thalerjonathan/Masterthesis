package frontend.experimenter.xml.result;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import frontend.experimenter.xml.experiment.ExperimentBean;

@XmlRootElement( name = "result" )
public class ResultBean {

	private ExperimentBean experiment;
	private EquilibriumBean equilibrium;
	private List<AgentBean> agents;
	
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
}
