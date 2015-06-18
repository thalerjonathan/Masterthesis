package controller.network.export;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement( name = "graph" )
@XmlAccessorType( XmlAccessType.FIELD )
public class GraphBean {

	@XmlAttribute( name = "mode" )
	private GraphMode mode;
	@XmlAttribute( name = "defaultedgetype" )
	private EdgeType defaultedgetype;
	
	@XmlElementWrapper(name="nodes")
	@XmlElement (name = "node")
	private List<NodeBean> nodes;
	@XmlElementWrapper(name="edges")
	@XmlElement (name = "edge")
	private List<EdgeBean> edges;
	
	public GraphBean() {
		this.mode = GraphMode.STATIC;
		this.defaultedgetype = EdgeType.UNDIRECTED;
		
		this.nodes = new ArrayList<NodeBean>();
		this.edges = new ArrayList<EdgeBean>();
	}

	public GraphMode getMode() {
		return mode;
	}

	public void setMode(GraphMode mode) {
		this.mode = mode;
	}

	public EdgeType getDefaultedgetype() {
		return defaultedgetype;
	}

	public void setDefaultedgetype(EdgeType defaultedgetype) {
		this.defaultedgetype = defaultedgetype;
	}

	public List<NodeBean> getNodes() {
		return nodes;
	}

	public void setNodes(List<NodeBean> nodes) {
		this.nodes = nodes;
	}

	public List<EdgeBean> getEdges() {
		return edges;
	}

	public void setEdges(List<EdgeBean> edges) {
		this.edges = edges;
	}
}
