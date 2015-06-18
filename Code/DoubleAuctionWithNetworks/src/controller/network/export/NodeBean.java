package controller.network.export;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType( XmlAccessType.FIELD )
public class NodeBean {

	@XmlAttribute( name = "id" )
	private int id;
	@XmlAttribute( name = "label" )
	private double label;
	
	public NodeBean() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public double getLabel() {
		return label;
	}

	public void setLabel(double label) {
		this.label = label;
	}
}
