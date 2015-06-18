package controller.network.export;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement( name = "gefx" )
@XmlType( namespace = "http://www.gexf.net/1.2draft" )
public class GexfBean {

	private GraphBean graph;

	public GraphBean getGraph() {
		return graph;
	}

	public void setGraph(GraphBean graph) {
		this.graph = graph;
	}
	
	
}
