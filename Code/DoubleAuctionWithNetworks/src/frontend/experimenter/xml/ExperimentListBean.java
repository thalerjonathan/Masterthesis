package frontend.experimenter.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement( name = "experiments" )
public class ExperimentListBean {

	private List<ExperimentBean> experiments;

	public List<ExperimentBean> getExperiments() {
		return experiments;
	}

	@XmlElement( name = "experiment" )
	public void setExperiments(List<ExperimentBean> experiments) {
		this.experiments = experiments;
	}
	
	
}
