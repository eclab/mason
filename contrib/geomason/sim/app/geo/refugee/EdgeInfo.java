package refugee;

import sim.util.Int2D;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

class EdgeInfo {
	// private List<Int2D> path;//list of places this person needs to go
	private double distance;
	private double speed;
	private int population;
	private double cost;
	private double transportLevel;
	private double deaths;

	public EdgeInfo(double speed, double distance, double cost, double transportLevel, double deaths) {
		this.population = 0; // starts with no one traveling
		this.speed = speed;
		this.distance = distance;
		this.cost = cost;
		this.transportLevel = transportLevel;
		this.deaths = deaths;

	}

	/**
	 * @return next location to move, null if no more moves
	 */

	public double getWeightedDistance() {

		return (this.distance - Parameters.MIN_EDGE_LENGTH) / (Parameters.MAX_EDGE_LENGTH - Parameters.MIN_EDGE_LENGTH);
	}

	public double getDistance() {
		return distance;
	}

	public double getSpeed() {
		return speed;
	}

	public double getPopulation() {
		return population;
	}

	public double getCost() {
		return cost;
	}

	public double getTransportLevel() {
		return transportLevel;
	}

	public double getDeaths() {
		return deaths;
	}

}
