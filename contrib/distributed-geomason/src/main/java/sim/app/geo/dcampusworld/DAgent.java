package sim.app.geo.dcampusworld;

import com.vividsolutions.jts.geom.Coordinate;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import com.vividsolutions.jts.planargraph.DirectedEdgeStar;
import com.vividsolutions.jts.planargraph.Node;

import sim.engine.DSimState;
import sim.engine.DSteppable;
import sim.engine.SimState;
import sim.field.geo.GeomVectorContinuousStorage;
import sim.field.geo.GeomVectorField;
import sim.util.Double2D;
import sim.util.geo.DGeomSteppable;
import sim.util.geo.GeomPlanarGraphDirectedEdge;
import sim.util.geo.GeomPlanarGraphEdge;
import sim.util.geo.MasonGeometry;
import sim.util.geo.PointMoveTo;

public class DAgent extends DGeomSteppable
{
	static final long serialVersionUID = 1L;
	// the agent's geometry
	//transient MasonGeometry agentGeometry = null;
	MasonGeometry agentGeometry = null;
	// the JTS position of the agent
	Double2D jtsCoordinate;
	// The base speed of the agent.
	double basemoveRate = 1.0;
	// How much to move the agent by in each step(); may become negative if
	// agent is moving from the end to the start of current line.
	double moveRate = basemoveRate;
	// Used by agent to walk along line segment; assigned in setNewRoute()
	transient LengthIndexedLine segment = null;
	// the current walkway the agent is on
	int walkway;
	
	//TODO can we remove these?
	double startIndex = 0.0; // start position of current line
	double endIndex = 0.0; // end position of current line
	double currentIndex = 0.0; // current location along line
	PointMoveTo pointMoveTo = new PointMoveTo();

	// Stored Attributes
	public final String type;
	public final int age;
	private final Envelope MBR; // world's original envelope

	static private GeometryFactory fact = new GeometryFactory();

	public DAgent(DCampusWorld state)
	{
		MBR = state.MBR;
		agentGeometry = new MasonGeometry(fact.createPoint(new Coordinate()));
		// Set up attributes for this agent
		if (state.random.nextBoolean())
		{
			type = "STUDENT";
			age = (int) (20.0 + 2.0 * state.random.nextGaussian());
		}
		else
		{
			type = "FACULTY";
			age = (int) (40.0 + 9.0 * state.random.nextGaussian());
		}
		// Not everyone walks at the same speed
		basemoveRate *= Math.abs(state.random.nextGaussian());

		agentGeometry.isMovable = true;
		agentGeometry.addStringAttribute("TYPE", type);
		agentGeometry.addIntegerAttribute("AGE", age);
		agentGeometry.addDoubleAttribute("MOVE RATE", basemoveRate);

		//TODO this is hacky
		// Now set the location of the agent
		while (true)
		{
			// Find the first line segment and set our position over the start coordinate.
			walkway = state.random.nextInt(state.walkways.getGeometries().numObjs);
			MasonGeometry mg = (MasonGeometry) state.walkways.getGeometries().objs[walkway];
			Coordinate c = initiateRoute((LineString) mg.getGeometry());
			Double2D initialLoc = jtsToPartitionSpace(state, c);
			if (state.agentLocations.isLocal(initialLoc))
			{
				jtsCoordinate = new Double2D(c.x, c.y);

				// agent needs to be added to storage before moving
				state.agentLocations.addAgent(initialLoc, this, 0, 0, 1);
				moveTo(state, c);
				break;
			}
		}
		//System.out.println("size of agentLocations: " + state.agentLocations.getStorage().getStorageMap().keySet().size());
	}

	/**
	 * Return the geometry representing the agent, insuring it's never null
	 */
	
	//Instead, due to how partitions handle geometries, we want to reset the coordinates when changing them, not now, as this creates
	//an issue where we are returning a agentGeometry that is not in the storage!
	
	/*
	 
	public MasonGeometry getAgentGeometry()
	{
		if (agentGeometry == null || agentGeometry.getGeometry().getCoordinate().x != jtsCoordinate.x ||
				agentGeometry.getGeometry().getCoordinate().y != jtsCoordinate.y)
		{
		
			agentGeometry = new MasonGeometry(fact.createPoint(new Coordinate(jtsCoordinate.x, jtsCoordinate.y)));
			agentGeometry.isMovable = true;
			agentGeometry.addStringAttribute("TYPE", type);
			agentGeometry.addIntegerAttribute("AGE", age);
			agentGeometry.addDoubleAttribute("MOVE RATE", basemoveRate);
			
			
		}

			
			
		return agentGeometry;
	}
	*/
	

	//do this when jts is updated, need to be careful about storage issues!
	public void updateAgentGeometry_with_new_point(GeomVectorContinuousStorage storage, Coordinate coordJTS) {

			this.jtsCoordinate = new Double2D(coordJTS.x, coordJTS.y);
			
		    		    
			agentGeometry = new MasonGeometry(fact.createPoint(new Coordinate(jtsCoordinate.x, jtsCoordinate.y)));
			agentGeometry.isMovable = true;
			agentGeometry.addStringAttribute("TYPE", type);
			agentGeometry.addIntegerAttribute("AGE", age);
			agentGeometry.addDoubleAttribute("MOVE RATE", basemoveRate);
			

		
	}
	
	
	//do this when jts is updated, need to be careful about storage issues!
	public void updateAgentGeometry(DSimState state) {
		if (agentGeometry == null || agentGeometry.getGeometry().getCoordinate().x != jtsCoordinate.x ||
				agentGeometry.getGeometry().getCoordinate().y != jtsCoordinate.y)
		{
			DCampusWorld state2 = (DCampusWorld)state;
		    state2.agentLocations.getStorage().getGeomVectorField().removeGeometry(agentGeometry);
		    		    
			agentGeometry = new MasonGeometry(fact.createPoint(new Coordinate(jtsCoordinate.x, jtsCoordinate.y)));
			agentGeometry.isMovable = true;
			agentGeometry.addStringAttribute("TYPE", type);
			agentGeometry.addIntegerAttribute("AGE", age);
			agentGeometry.addDoubleAttribute("MOVE RATE", basemoveRate);
			
		    state2.agentLocations.getStorage().getGeomVectorField().addGeometry(agentGeometry);

		}
	}
	
	public MasonGeometry getAgentGeometry()
	{

		return agentGeometry;
	}
	
	public MasonGeometry getMasonGeometry() {
		
		return this.getAgentGeometry();
		
		
	}
	
	
	/**
	 * true if the agent has arrived at the target intersection
	 */
	private boolean arrived()
	{
		// If we have a negative move rate the agent is moving from the end to
		// the start, else the agent is moving in the opposite direction.
		return (moveRate > 0 && currentIndex >= endIndex) || (moveRate < 0 && currentIndex <= startIndex);
	}

	/** Return a string indicating whether we are "FACULTY" or a "STUDENT" */
	public String getType()
		{
		return type;
		}

	/**
	 * randomly selects an adjacent route to traverse
	 */
	private void findNewPath(DCampusWorld state)
	{
//		System.out.println("Finding New Path jtsCoordinate: " + jtsCoordinate + ", agentGeometry" + getAgentGeometry() + "MBR: " + state.MBR + "; network: " + 
//				state.network.getNodes().stream().map(new Function<com.vividsolutions.jts.planargraph.Node, String>() {
//					public String apply(Node t) {
//						return t.getCoordinate().toString();
//					}
//				}).collect(Collectors.toList())
//		);
				
		// find all the adjacent junctions
		Node currentJunction = state.network.findNode(getAgentGeometry().getGeometry().getCoordinate());
		if (currentJunction != null)
		{
			DirectedEdgeStar directedEdgeStar = currentJunction.getOutEdges();
			Object[] edges = directedEdgeStar.getEdges().toArray();

			if (edges.length > 0)
			{
				// pick one randomly
				int i = state.random.nextInt(edges.length);
				GeomPlanarGraphDirectedEdge directedEdge = (GeomPlanarGraphDirectedEdge) edges[i];
				GeomPlanarGraphEdge edge = (GeomPlanarGraphEdge) directedEdge.getEdge();

				// and start moving along it
				LineString newRoute = edge.getLine();
				Point startPoint = newRoute.getStartPoint();
				Point endPoint = newRoute.getEndPoint();

				if (startPoint.equals(getAgentGeometry().geometry))
				{
					setNewRoute(state, newRoute, true);
				}
				else
				{
					if (endPoint.equals(getAgentGeometry().geometry))
						setNewRoute(state, newRoute, false);
					else {
						System.err.println("Where am I?");
					}
				}
			}
		}
		else
		{
			//System.err.println("No Junction Found");
		}
	}

	/**
	 * have the agent move along new route
	 *
	 * @param line  defining new route
	 * @param start true if agent at start of line else agent placed at end
	 */
	private void setNewRoute(DCampusWorld state, LineString line, boolean start)
	{
		segment = new LengthIndexedLine(line);
		startIndex = segment.getStartIndex();
		endIndex = segment.getEndIndex();

		Coordinate startCoord = null;

		if (start)
		{
			startCoord = segment.extractPoint(startIndex);
			currentIndex = startIndex;
			moveRate = basemoveRate; // ensure we move forward along segment
		}
		else
		{
			startCoord = segment.extractPoint(endIndex);
			currentIndex = endIndex;
			moveRate = -basemoveRate; // ensure we move backward along segment
		}

		moveTo(state, startCoord);
	}

	private Coordinate initiateRoute(LineString line)
	{
		segment = new LengthIndexedLine(line);
		endIndex = segment.getEndIndex();
		Coordinate startCoord = segment.extractPoint(startIndex);
		currentIndex = segment.getStartIndex();
		moveRate = basemoveRate; // ensure we move forward along segment

		return startCoord;
	}

	/**
	 * Return a mapping from JTS's UTM coords -> our world coords
	 */
	
	
	
	public Double2D jtsToPartitionSpace(DSimState cw, Coordinate coordJTS)
	{
		return jtsToPartitionSpace((DCampusWorld)cw, coordJTS);
	}
	
	/**
	 * Return a mapping from our world coords -> JTS's UTM coords
	 */

	
	
	public Double2D jtsToPartitionSpace(DCampusWorld cw, Coordinate coordJTS)
	{
		double xJTS = coordJTS.x - cw.MBR.getMinX();
		double yJTS = coordJTS.y - cw.MBR.getMinY();

		double wP = xJTS / cw.MBR.getWidth();
		double hP = yJTS / cw.MBR.getHeight();

		double partX = DCampusWorld.width * wP;
		double partY = DCampusWorld.height * hP;

		return new Double2D(partX, partY);
	}
	
	/**
	 * Return a mapping from our world coords -> JTS's UTM coords
	 */
	public Coordinate partitionSpaceToJTS(Double2D d)
	{
		double xP = d.x - DCampusWorld.width;
		double yP = d.y - DCampusWorld.height;
		
		double wJTS = xP / DCampusWorld.width;
		double hJTS = yP / DCampusWorld.height;
		
		double partX = MBR.getWidth() * wJTS;
		double partY = MBR.getHeight() * hJTS;
		
		return new Coordinate(partX, partY);
	}

	/**
	 * Move the agent to the given coordinates
	 */
	public void moveTo(DCampusWorld state, Coordinate c)
	{
		jtsCoordinate = new Double2D(c.x, c.y);
		
		//unlike it regular geomason, we need to make sure we update the MasonGeometry here and update it in storage
		//regular Agent in CampusWorld updates its MasonGeometry in getGeometry, but we do it here so we can update 
		//state storage as well
		updateAgentGeometry(state);

		Double2D toP = jtsToPartitionSpace(state, c);

		state.agentLocations.moveAgent(toP, this);
	}
	
	/**
	 * Move the agent
	 */
	public void transfer(Double2D point, GeomVectorField g)
	{

		Coordinate jtsCoordinate = partitionSpaceToJTS(point);
//		pointMoveTo.setCoordinate(new Coordinate(point.x, point.y));
		pointMoveTo.setCoordinate(jtsCoordinate);
		getAgentGeometry().getGeometry().apply(pointMoveTo);
		getAgentGeometry().geometry.geometryChanged();
		g.setGeometryLocation(getAgentGeometry(), pointMoveTo);
	}
	

	public void step(SimState state)
	{
		move((DCampusWorld) state);
	}

	/**
	 * moves the agent along the grid
	 * 
	 * The agent will randomly select an adjacent junction and then move along the line segment to it. Then it will repeat.
	 */
	private void move(DCampusWorld state)
	{
		// if we're not at a junction move along the current segment
		if (!arrived())
			moveAlongPath(state);
		else
			findNewPath(state);
	}

	// move agent along current line segment
	private void moveAlongPath(DCampusWorld state)
	{
		//System.out.println("Moving along Path: " + System.identityHashCode(this));
		currentIndex += moveRate;

		// Truncate movement to end of line segment
		if (moveRate < 0) // moving from endIndex to startIndex
		{
			if (currentIndex < startIndex)
			{
				currentIndex = startIndex;
			}
		}
		else // moving from startIndex to endIndex
		{
			if (currentIndex > endIndex)
			{
				currentIndex = endIndex;
			}
		}

		if (segment == null)
		{
			// Find the first line segment and set our position over the start coordinate.
//			int walkway = state.random.nextInt(state.walkways.getGeometries().numObjs);
			MasonGeometry mg = (MasonGeometry) state.walkways.getGeometries().objs[walkway];
			segment = new LengthIndexedLine((LineString) mg.getGeometry());
//			setNewRoute(state, (LineString) mg.getGeometry(), true);
			// TODO ^ why is this commented out?....
		}

		Coordinate currentPos = segment.extractPoint(currentIndex);

		moveTo(state, currentPos);
	}

	public String toString()
	{
		return "DAgent [jtsCoordinate=" + jtsCoordinate + ", basemoveRate=" + basemoveRate + ", moveRate=" + moveRate
				+ ", segment=" + segment + ", startIndex=" + startIndex + ", endIndex=" + endIndex + ", currentIndex="
				+ currentIndex + ", pointMoveTo=" + pointMoveTo + ", type=" + type + ", age=" + age + "]";
	}

}
