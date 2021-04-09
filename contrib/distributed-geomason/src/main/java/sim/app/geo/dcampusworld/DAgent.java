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

//TODO rm JTS dependencies

public class DAgent extends DSteppable {//add have id

    private static final long serialVersionUID = -1113018274619047013L;
    // point that denotes agent's position
    private transient MasonGeometry location = null;
    Double2D loc;
    // The base speed of the agent.
    private double basemoveRate = 1.0;
    // How much to move the agent by in each step(); may become negative if
    // agent is moving from the end to the start of current line.
    private double moveRate = basemoveRate;
    // Used by agent to walk along line segment; assigned in setNewRoute()
    private LengthIndexedLine segment = null;
    double startIndex = 0.0; // start position of current line
    double endIndex = 0.0; // end position of current line
    double currentIndex = 0.0; // current location along line
    PointMoveTo pointMoveTo = new PointMoveTo();
    //TODO transient fields?

    String type;
    int age;
    
    static private GeometryFactory fact = new GeometryFactory();

    public DAgent(DCampusWorld state, Double2D loc) {
        this.loc = loc;
        System.out.println(loc);

        location = new MasonGeometry(fact.createPoint(new Coordinate(loc.x, loc.y))); // magic numbers
        location.isMovable = true;
        
        // Find the first line segment and set our position over the start coordinate.
        int walkway = state.random.nextInt(state.walkways.getGeometries().numObjs);
        MasonGeometry mg = (MasonGeometry) state.walkways.getGeometries().objs[walkway];
        setNewRoute((LineString) mg.getGeometry(), true);

        // Now set up attributes for this agent
        if (state.random.nextBoolean())
        {
        	type = "STUDENT";
        	
            location.addStringAttribute("TYPE", "STUDENT");

            age = (int) (20.0 + 2.0 * state.random.nextGaussian());

            location.addIntegerAttribute("AGE", age);
        }
        else
        {
        	type = "FACULTY";

            location.addStringAttribute("TYPE", "FACULTY");

            age = (int) (40.0 + 9.0 * state.random.nextGaussian());

            location.addIntegerAttribute("AGE", age);
        }

        // Not everyone walks at the same speed
        basemoveRate *= Math.abs(state.random.nextGaussian());
        location.addDoubleAttribute("MOVE RATE", basemoveRate);
    }
    
    public DAgent(DCampusWorld state) {
    	this(state, new Double2D(10, 10));
    }

    /**
     * @return geometry representing agent location
     */
    public MasonGeometry getGeoLocation()
    {
    	if (location == null) {
            location = new MasonGeometry(fact.createPoint(new Coordinate(loc.x, loc.y))); // magic numbers
            location.isMovable = true;
            location.addStringAttribute("TYPE", type);
            location.addIntegerAttribute("AGE", age);
            location.addDoubleAttribute("MOVE RATE", basemoveRate);
    	}
        return location;
    }
    
    /** true if the agent has arrived at the target intersection
     */
    private boolean arrived()
    {
        // If we have a negative move rate the agent is moving from the end to
        // the start, else the agent is moving in the opposite direction.
        if ((moveRate > 0 && currentIndex >= endIndex)
            || (moveRate < 0 && currentIndex <= startIndex))
        {
            return true;
        }

        return false;
    }



    /** @return string indicating whether we are "FACULTY" or a "STUDENT" */
    public String getType()
    {
        return getGeoLocation().getStringAttribute("TYPE");
    }


    /** randomly selects an adjacent route to traverse
    */
    private void findNewPath(DCampusWorld geoTest)
    {
        // find all the adjacent junctions
        Node currentJunction = geoTest.network.findNode(getGeoLocation().getGeometry().getCoordinate());

        if (currentJunction != null)
        {
            DirectedEdgeStar directedEdgeStar = currentJunction.getOutEdges();
            Object[] edges = directedEdgeStar.getEdges().toArray();

            if (edges.length > 0)
            {
                // pick one randomly
                int i = geoTest.random.nextInt(edges.length);
                GeomPlanarGraphDirectedEdge directedEdge = (GeomPlanarGraphDirectedEdge) edges[i];
                GeomPlanarGraphEdge edge = (GeomPlanarGraphEdge) directedEdge.getEdge();

                // and start moving along it
                LineString newRoute = edge.getLine();
                Point startPoint = newRoute.getStartPoint();
                Point endPoint = newRoute.getEndPoint();

                if (startPoint.equals(getGeoLocation().geometry))
                {
                    setNewRoute(newRoute, true);
                } else
                {
                    if (endPoint.equals(getGeoLocation().geometry))
                    {
                        setNewRoute(newRoute, false);
                    } else
                    {
                        System.err.println("Where am I?");
                    }
                }
            }
        }
    }



    /**
     * have the agent move along new route
     *
     * @param line defining new route
     * @param start true if agent at start of line else agent placed at end
     */
    private void setNewRoute(LineString line, boolean start)
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
        } else
        {
            startCoord = segment.extractPoint(endIndex);
            currentIndex = endIndex;
            moveRate = -basemoveRate; // ensure we move backward along segment
        }

        moveTo(startCoord);
    }

    // move the agent to the given coordinates
    // TODO reimplement w/o jts dependencies

    public void moveTo(Coordinate c)
    {
    	System.out.println("moveTo coordinate: " + c);
    	loc = new Double2D(c.x, c.y);
        pointMoveTo.setCoordinate(c);
        getGeoLocation().getGeometry().apply(pointMoveTo);
        getGeoLocation().geometry.geometryChanged();
    }

    //print to see if scheduled
    public void step(SimState state)
    {
        DCampusWorld campState = (DCampusWorld) state;
        move(campState);
//        campState.agents.setGeometryLocation(getGeometry(), pointMoveTo);
        
        System.out.println(this.firstpid + ": " + campState.getPartition().getPID());
    }



    /**
     * moves the agent along the grid
     *
     * @param geoTest handle on the base SimState
     *
     * The agent will randomly select an adjacent junction and then move along
     * the line segment to it. Then it will repeat.
     */
    private void move(DCampusWorld geoTest)
    {
        // if we're not at a junction move along the current segment
        if (!arrived())
        {
            moveAlongPath();
        } else
        {
            findNewPath(geoTest);
        }
    }



    // move agent along current line segment
    private void moveAlongPath()
    {
        currentIndex += moveRate;

        // Truncate movement to end of line segment
        if (moveRate < 0)
        { // moving from endIndex to startIndex
            if (currentIndex < startIndex)
            {
                currentIndex = startIndex;
            }
        } else
        { // moving from startIndex to endIndex
            if (currentIndex > endIndex)
            {
                currentIndex = endIndex;
            }
        }

        Coordinate currentPos = segment.extractPoint(currentIndex);

        moveTo(currentPos);
    }

	@Override
	public String toString() {
		return "DAgent [loc=" + loc + ", basemoveRate=" + basemoveRate + ", moveRate=" + moveRate + ", segment="
				+ segment + ", startIndex=" + startIndex + ", endIndex=" + endIndex + ", currentIndex=" + currentIndex
				+ ", pointMoveTo=" + pointMoveTo + ", type=" + type + ", age=" + age + "]";
	}

}
