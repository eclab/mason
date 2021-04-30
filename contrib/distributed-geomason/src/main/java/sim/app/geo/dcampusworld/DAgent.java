package sim.app.geo.dcampusworld;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import com.vividsolutions.jts.planargraph.DirectedEdgeStar;
import com.vividsolutions.jts.planargraph.Node;

import sim.engine.DSteppable;
import sim.engine.SimState;
import sim.util.Double2D;
import sim.util.geo.GeomPlanarGraphDirectedEdge;
import sim.util.geo.GeomPlanarGraphEdge;
import sim.util.geo.MasonGeometry;
import sim.util.geo.PointMoveTo;

public class DAgent extends DSteppable {
	static final long serialVersionUID = 1L;
	// point that denotes agent's position
	transient MasonGeometry agentGeometry = null;
	Double2D jtsCoordinate;
	// The base speed of the agent.
	double basemoveRate = 1.0;
	// How much to move the agent by in each step(); may become negative if
	// agent is moving from the end to the start of current line.
	double moveRate = basemoveRate;
	// Used by agent to walk along line segment; assigned in setNewRoute()
	transient LengthIndexedLine segment = null;
	int walkway;

	double startIndex = 0.0; // start position of current line
	double endIndex = 0.0; // end position of current line
	double currentIndex = 0.0; // current location along line
	PointMoveTo pointMoveTo = new PointMoveTo();

	public final String type;
	public final int age;

	static private GeometryFactory fact = new GeometryFactory();

	public DAgent(DCampusWorld state) {
		agentGeometry = new MasonGeometry(fact.createPoint(new Coordinate()));
		// Set up attributes for this agent
		if (state.random.nextBoolean()) {
			type = "STUDENT";
			age = (int) (20.0 + 2.0 * state.random.nextGaussian());
		} else {
			type = "FACULTY";
			age = (int) (40.0 + 9.0 * state.random.nextGaussian());
		}
		// Not everyone walks at the same speed
		basemoveRate *= Math.abs(state.random.nextGaussian());

		agentGeometry.isMovable = true;
		agentGeometry.addStringAttribute("TYPE", type);
		agentGeometry.addIntegerAttribute("AGE", age);
		agentGeometry.addDoubleAttribute("MOVE RATE", basemoveRate);

		// Now set the location of the agent
		while (true) {
			// Find the first line segment and set our position over the start coordinate.
			walkway = state.random.nextInt(state.walkways.getGeometries().numObjs);
			MasonGeometry mg = (MasonGeometry) state.walkways.getGeometries().objs[walkway];
			Coordinate c = initiateRoute((LineString) mg.getGeometry());
			Double2D initialLoc = jtsToPartitionSpace(state, c);
			if (state.agentLocations.isLocal(initialLoc)) {
				// agent needs to be added to storage before moving
				state.agentLocations.addAgent(initialLoc, this, 0, 1, 1);
				moveTo(state, c);
				jtsCoordinate = new Double2D(c.x, c.y);
				break;
			}
		}
	}

	/**
	 * @return geometry representing agent location
	 */
	public MasonGeometry getAgentGeometry() {
		if (agentGeometry == null ||
				agentGeometry.getGeometry().getCoordinate().x != jtsCoordinate.x ||
				agentGeometry.getGeometry().getCoordinate().y != jtsCoordinate.y) {

			agentGeometry = new MasonGeometry(fact.createPoint(new Coordinate(jtsCoordinate.x, jtsCoordinate.y)));
			agentGeometry.isMovable = true;
			agentGeometry.addStringAttribute("TYPE", type);
			agentGeometry.addIntegerAttribute("AGE", age);
			agentGeometry.addDoubleAttribute("MOVE RATE", basemoveRate);
		}
		return agentGeometry;
	}

	/**
	 * true if the agent has arrived at the target intersection
	 */
	private boolean arrived() {
		// If we have a negative move rate the agent is moving from the end to
		// the start, else the agent is moving in the opposite direction.
		return (moveRate > 0 && currentIndex >= endIndex) || (moveRate < 0 && currentIndex <= startIndex);
	}

	/** @return string indicating whether we are "FACULTY" or a "STUDENT" */
	public String getType() {
		return type;
	}

	/**
	 * randomly selects an adjacent route to traverse
	 */
	private void findNewPath(DCampusWorld state) {
		System.out.println("Finding New Path jtsCoordinate: " + jtsCoordinate + ", agentGeometry" + getAgentGeometry());
		// find all the adjacent junctions

		Node currentJunction = state.network.findNode(getAgentGeometry().getGeometry().getCoordinate());

		if (currentJunction != null) {
			DirectedEdgeStar directedEdgeStar = currentJunction.getOutEdges();
			Object[] edges = directedEdgeStar.getEdges().toArray();

			if (edges.length > 0) {
				// pick one randomly
				int i = state.random.nextInt(edges.length);
				GeomPlanarGraphDirectedEdge directedEdge = (GeomPlanarGraphDirectedEdge) edges[i];
				GeomPlanarGraphEdge edge = (GeomPlanarGraphEdge) directedEdge.getEdge();

				// and start moving along it
				LineString newRoute = edge.getLine();
				Point startPoint = newRoute.getStartPoint();
				Point endPoint = newRoute.getEndPoint();

				if (startPoint.equals(getAgentGeometry().geometry)) {
					setNewRoute(state, newRoute, true);
				} else {
					if (endPoint.equals(getAgentGeometry().geometry))
						setNewRoute(state, newRoute, false);
					else
						System.err.println("Where am I?");
				}
			}
		} else {
			System.err.println("No Junction Found");
		}
	}

	/**
	 * have the agent move along new route
	 *
	 * @param line  defining new route
	 * @param start true if agent at start of line else agent placed at end
	 */
	private void setNewRoute(DCampusWorld state, LineString line, boolean start) {
		segment = new LengthIndexedLine(line);
		startIndex = segment.getStartIndex();
		endIndex = segment.getEndIndex();

		Coordinate startCoord = null;

		if (start) {
			startCoord = segment.extractPoint(startIndex);
			currentIndex = startIndex;
			moveRate = basemoveRate; // ensure we move forward along segment
		} else {
			startCoord = segment.extractPoint(endIndex);
			currentIndex = endIndex;
			moveRate = -basemoveRate; // ensure we move backward along segment
		}

		moveTo(state, startCoord);
	}

	private Coordinate initiateRoute(LineString line) {
		segment = new LengthIndexedLine(line);
		endIndex = segment.getEndIndex();
		Coordinate startCoord = segment.extractPoint(startIndex);
		currentIndex = segment.getStartIndex();
		moveRate = basemoveRate; // ensure we move forward along segment

		return startCoord;
	}

	Double2D jtsToPartitionSpace(DCampusWorld cw, Coordinate coordJTS) {
		double xJTS = coordJTS.x - cw.MBR.getMinX();
		double yJTS = coordJTS.y - cw.MBR.getMinY();
//    	System.out.println("x,y JTS: " + xJTS + ", " + yJTS);

		double wP = xJTS / cw.MBR.getWidth();
		double hP = yJTS / cw.MBR.getHeight();
//    	System.out.println("w/h JTS percentage: " + wP + ", " + hP);

		double partX = DCampusWorld.width * wP;
		double partY = DCampusWorld.height * hP;
//    	System.out.println("partition x,y: " + partX + ", " + partY);

		return new Double2D(partX, partY);
	}

	// move the agent to the given coordinates
	public void moveTo(DCampusWorld state, Coordinate c) {
//		System.out.println("move agent: " + jtsCoordinate + " -> " + new Double2D(c.x, c.y));

		jtsCoordinate = new Double2D(c.x, c.y);
		System.out.println("Move To: " + jtsCoordinate);

//    	System.out.println("moveTo coordinate: " + c);
//    	System.out.println("partition getWorldBounds: " + state.getPartition().getWorldBounds());
//    	System.out.println("partition getHaloBounds: " + state.getPartition().getHaloBounds());
//    	System.out.println("partition getLocalBounds: " + state.getPartition().getLocalBounds());

		Double2D toP = jtsToPartitionSpace(state, c);

		if (state.agentLocations.isLocal(toP)) {
			// Need to migrate
			state.agents.removeGeometry(getAgentGeometry());
		} else {
			// No need to migrate
			pointMoveTo.setCoordinate(c);
			getAgentGeometry().getGeometry().apply(pointMoveTo);
			getAgentGeometry().geometry.geometryChanged();
			state.agents.setGeometryLocation(getAgentGeometry(), pointMoveTo);
		}
		state.agentLocations.moveAgent(toP, this);
	}

	public void step(SimState state) {
//		System.out.println("step agent: " + System.identityHashCode(this));
		move((DCampusWorld) state);
	}

	/**
	 * moves the agent along the grid
	 * 
	 * The agent will randomly select an adjacent junction and then move along the line segment to it. Then it will repeat.
	 */
	private void move(DCampusWorld state) {
		// if we're not at a junction move along the current segment
		if (!arrived())
			moveAlongPath(state);
		else
			findNewPath(state);
	}

	// move agent along current line segment
	private void moveAlongPath(DCampusWorld state) {
		System.out.println("Moving along Path: " + System.identityHashCode(this));
		currentIndex += moveRate;

		// Truncate movement to end of line segment
		if (moveRate < 0) { // moving from endIndex to startIndex
			if (currentIndex < startIndex) {
				currentIndex = startIndex;
			}
		} else { // moving from startIndex to endIndex
			if (currentIndex > endIndex) {
				currentIndex = endIndex;
			}
		}

		if (segment == null) {
			// Find the first line segment and set our position over the start coordinate.
//			int walkway = state.random.nextInt(state.walkways.getGeometries().numObjs);
			MasonGeometry mg = (MasonGeometry) state.walkways.getGeometries().objs[walkway];
			segment = new LengthIndexedLine((LineString) mg.getGeometry());
//			setNewRoute(state, (LineString) mg.getGeometry(), true);
		}

		Coordinate currentPos = segment.extractPoint(currentIndex);

		moveTo(state, currentPos);
	}

	public String toString() {
		return "DAgent [jtsCoordinate=" + jtsCoordinate + ", basemoveRate=" + basemoveRate + ", moveRate=" + moveRate
				+ ", segment=" + segment + ", startIndex=" + startIndex + ", endIndex=" + endIndex + ", currentIndex="
				+ currentIndex + ", pointMoveTo=" + pointMoveTo + ", type=" + type + ", age=" + age + "]";
	}

}
