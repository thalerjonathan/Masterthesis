package agents.network;

public class AgentConnection {
	private double weight;
	private boolean highlighted;

	public AgentConnection() {
		this.weight = Double.MAX_VALUE;
		this.highlighted = false;
	}
	
	public boolean isHighlighted() {
		return highlighted;
	}

	public void setHighlighted(boolean highlighted) {
		this.highlighted = highlighted;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	public void incrementWeight(double w) {
		this.weight += w;
	}

	public void reset() {
		this.weight = Double.MAX_VALUE;
		this.highlighted = false;
	}
}
