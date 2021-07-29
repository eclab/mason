package sim.app.dballs;

public class DBand implements java.io.Serializable {

	private static final long serialVersionUID = 1;

	public double laxDistance;
	public double strength;

	public DBand(double laxDistance, double strength) {
		this.laxDistance = laxDistance;
		this.strength = strength;
	}

	// Bean Properties for our Inspector
	public void setStrength(double val) {
		if (val > 0)
			strength = val;
	}

	public double getStrength() {
		return strength;
	}

	public void setLaxDistance(double val) {
		if (val >= 0)
			laxDistance = val;
	}

	public double getLaxDistance() {
		return laxDistance;
	}

	public String toString() {
		return "" + strength + " (" + laxDistance + ")";
	}
}
