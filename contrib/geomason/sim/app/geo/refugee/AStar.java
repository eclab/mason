package refugee;

import java.util.*;

import sim.field.network.Edge;
import sim.field.network.Network;
import sim.util.Bag;
import sim.util.Heap;
import sim.util.Int2D;
import ec.util.MersenneTwisterFast;
//import org.apache.commons.collections4.sequence.SequencedHashMap;

@SuppressWarnings("restriction")
public class AStar {

	/**
	 * Assumes that both the start and end location are Cities as opposed to
	 * LOCATIONS
	 *
	 * @param start
	 * @param goal
	 * @return
	 */
	public static Network roadNetwork = MigrationBuilder.migrationSim.roadNetwork;
	public static MersenneTwisterFast random = new MersenneTwisterFast();

	static public Route astarPath(City start, City goal, RefugeeFamily refugee) {

		//
		// initial check
		long startTime = System.currentTimeMillis();
		if (start == null || goal == null) {
			System.out.println("Error: invalid City provided to AStar");
		}

		// containers for the metainformation about the Cities relative to the
		// A* search
		HashMap<City, AStarCityWrapper> foundCities = new HashMap<City, AStarCityWrapper>();

		AStarCityWrapper startCity = new AStarCityWrapper(start);
		AStarCityWrapper goalCity = new AStarCityWrapper(goal);
		foundCities.put(start, startCity);
		foundCities.put(goal, goalCity);

		startCity.gx = 0;
		startCity.hx = heuristic(start, goal);
		startCity.fx = heuristic(start, goal);

		// A* containers: allRoadCities to be investigated, allRoadCities that
		// have been investigated
		HashSet<AStarCityWrapper> closedSet = new HashSet<>(10000), openSet = new HashSet<>(10000);
		PriorityQueue<AStarCityWrapper> openSetQueue = new PriorityQueue<>(10000);
		openSet.add(startCity);
		openSetQueue.add(startCity);
		while (openSet.size() > 0) { // while there are reachable allRoadCities
										// to investigate

			AStarCityWrapper x = openSetQueue.peek();
			//AStarCityWrapper x = findMin(openSet);
			System.out.println("Peek: " + x.city.getName());
			if (x == null) {
				AStarCityWrapper n = findMin(openSet);
			}
			if (x.city == goal) { // we have found the shortest possible path to
									// the goal!
				// Reconstruct the path and send it back.
				if (x.cameFrom == null)
					System.out.println(x.city.getName());
				return reconstructRoute(goalCity, startCity, goalCity, refugee);
			}
			openSet.remove(x); // maintain the lists
			openSetQueue.remove();
			closedSet.add(x);

			// check all the neighbors of this location
			Bag edges = roadNetwork.getEdgesOut(x.city);
			for (Object l : edges) {
				Edge e = (Edge) l;
				City n = (City) e.from();
 				if (n == x.city)
					n = (City) e.to();

				// get the A* meta information about this City
				AStarCityWrapper nextCity;

				if (foundCities.containsKey(n))
					nextCity = foundCities.get(n);
				else {
					nextCity = new AStarCityWrapper(n);
					foundCities.put(n, nextCity);
				}
				System.out.println(nextCity.city.getName());
				if (closedSet.contains(nextCity)) // it has already been
													// considered
					continue;

				// otherwise evaluate the cost of this City/edge combo
				RoadInfo edge = (RoadInfo) e.getInfo();
				// System.out.println(edge.getWeightedDistance());
				double edgeweight = edge.getWeightedDistance() * Parameters.DISTANCE_WEIGHT
						+ edge.getSpeed() * Parameters.SPEED_WEIGHT - edge.getScaledPopulation() * Parameters.POP_WEIGHT
						+ edge.getScaledCost() * Parameters.COST_WEIGHT
						+ edge.getTransportLevel() * Parameters.TRANSPORT_LEVEL_WEIGHT
						+ edge.getDeaths() * Parameters.RISK_WEIGHT * refugee.dangerCare();
				System.out.println(edge.getScaledPopulation());
				System.out.println("gx: " + x.gx + " edgeweight: " + edgeweight);
				double tentativeCost = x.gx + edgeweight; // changed from
															// integer, still
															// need to change
															// the weighting of
										// the edge weight

				boolean better = false;

				if (!openSet.contains(nextCity)) {
					openSet.add(nextCity);
					openSetQueue.add(nextCity);
					nextCity.hx = heuristic(n, goal);
					better = true;
				} else if (tentativeCost < nextCity.gx) {
					better = true;
				}

				// store A* information about this promising candidate City
				if (better) {
					nextCity.cameFrom = x;
					System.out.println("Edge: " + tentativeCost);
					nextCity.gx = tentativeCost;
					System.out.println("hx: " + nextCity.hx);
					nextCity.fx = nextCity.gx + nextCity.hx;
					 System.out.println("gx: " +
					 nextCity.gx + "fx: " + nextCity.fx);
				}
			}

		}
		return null;
	}



	/**
	 * Takes the information about the given City n and returns the path that
	 * found it.
	 *
	 * @param n
	 *            the end point of the path
	 * @return an Route from start to goal
	 */
	static Route reconstructRoute(AStarCityWrapper n, AStarCityWrapper start, AStarCityWrapper end,
			RefugeeFamily refugee) {

		List<Int2D> locations = new ArrayList<Int2D>(100);
		List<Edge> edges = new ArrayList<Edge>(100);

		// double mod_speed = speed;
		double totalDistance = 0;
		AStarCityWrapper x = n;

		// start by adding the last one

		locations.add(0, x.city.location);
		Edge edge = null;

		if (x.cameFrom != null) {
			edge = (Edge) roadNetwork.getEdge(x.cameFrom.city, x.city);
			edges.add(0, edge);
			RoadInfo edgeInfo = (RoadInfo) edge.getInfo();
			//RoadInfo edge = (RoadInfo) roadNetwork.getEdge(x.cameFrom.city, x.city).getInfo();
			double mod_speed = edgeInfo.getSpeed() * Parameters.TEMPORAL_RESOLUTION;// now
																				// km
																				// per
																				// step
			// convert speed to cell block per step
			mod_speed = Parameters.convertFromKilometers(mod_speed);
			// System.out.println("" + mod_speed);
			AStarCityWrapper to = x;
			x = x.cameFrom;

			while (x != null) {

				double dist = x.city.location.distance(locations.get(0));
				edge =  roadNetwork.getEdge(x.city, to.city);
				 edgeInfo = (RoadInfo) edge.getInfo();
				mod_speed = edgeInfo.getSpeed() * Parameters.TEMPORAL_RESOLUTION;// now
																				// km
																				// per
																				// step
				// convert speed to cell block per step
				mod_speed = Parameters.convertFromKilometers(mod_speed);

				while (dist > mod_speed) {
					locations.add(0, getPointAlongLine(locations.get(0), x.city.location, mod_speed / dist));
					//System.out.println(x.city.getName());
					edges.add(0, edge);
					dist = x.city.location.distance(locations.get(0));
				}
                locations.add(0, getPointAlongLine(locations.get(0), x.city.location, 1)); //**CRUCIAL***
                edges.add(0,  edge);

				/*if (x.cameFrom != null) {
					edge = roadNetwork.getEdge(x.cameFrom.city, x.city);
					 edgeInfo = (RoadInfo) edge.getInfo();
					mod_speed = edgeInfo.getSpeed() * Parameters.TEMPORAL_RESOLUTION;// now
																					// km
																					// per
																					// step
					// convert speed to cell block per step
					mod_speed = Parameters.convertFromKilometers(mod_speed);
				}

				if (x.cameFrom == null) {
					refugee.setCurrent(x.city);
				}*/
				to = x;
				x = x.cameFrom;
				if (x != null && x.cameFrom != null)
					totalDistance += x.city.location.distance(x.cameFrom.city.location);
			}
		}
		else{
		edges.add(0, edge);

		}
		//locations.add(0, start.city.location);
		edges.add(0, edge);
		return new Route(locations, edges, totalDistance, start.city, end.city, Parameters.WALKING_SPEED);
		//return new Route(locations, totalDistance, start.city, end.city, Parameters.WALKING_SPEED);
	}

	/**
	 * Gets a point a certain percent a long the line
	 *
	 * @param start
	 * @param end
	 * @param percent
	 *            the percent along the line you want to get. Must be less than
	 *            1
	 * @return
	 */
	public static Int2D getPointAlongLine(Int2D start, Int2D end, double percent) {
		return new Int2D((int) Math.round((end.getX() - start.getX()) * percent + start.getX()),
				(int) Math.round((end.getY() - start.getY()) * percent + start.getY()));
	}

	/**
	 * Measure of the estimated distance between two Cities.
	 *
	 * @return notional "distance" between the given allRoadCities.
	 */
	static double heuristic(City x, City y) {
		return x.location.distance(y.location) * Parameters.HEU_WEIGHT;
	}

	/**
	 * Considers the list of Cities open for consideration and returns the City
	 * with minimum fx value
	 *
	 * @param set
	 *            list of open Cities
	 * @return
	 */
	static AStarCityWrapper findMin(Collection<AStarCityWrapper> set) {
		double min = Double.MAX_VALUE;
		AStarCityWrapper minCity = null;
		for (AStarCityWrapper n : set) {
			if (n.fx < min) {
				min = n.fx;
				minCity = n;
			}
		}
		return minCity;
	}

	/**
	 * A wrapper to contain the A* meta information about the Cities
	 *
	 */
	static class AStarCityWrapper implements Comparable<AStarCityWrapper> {

		// the underlying City associated with the metainformation
		City city;
		// the City from which this City was most profitably linked
		AStarCityWrapper cameFrom;
		double gx, hx, fx;

		public AStarCityWrapper(City n) {
			city = n;
			gx = 0;
			hx = 0;
			fx = 0;
			cameFrom = null;
		}

		@Override
		public int compareTo(AStarCityWrapper aStarCityWrapper) {
			return Double.compare(this.fx, aStarCityWrapper.fx);
		}
	}
}