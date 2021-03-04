package sim.app.geo.dcampusworld;

import sim.engine.SimState;
import sim.util.Double2D;
import sim.util.geo.GeomPlanarGraphDirectedEdge;
import sim.util.geo.GeomPlanarGraphEdge;
import sim.util.geo.MasonGeometry;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import com.vividsolutions.jts.planargraph.DirectedEdgeStar;
import com.vividsolutions.jts.planargraph.Node;

import sim.engine.DSteppable;

public class DMasonPoint extends DSteppable {

    private static final long serialVersionUID = 1;
//    // point that denotes agent's position
//    private MasonGeometry location;
    private Double2D location;
    //TODO attributes
//    private final Map<String,AttributeValue> attributes;
    
    // The base speed of the agent.
    private double basemoveRate = 1.0;
    // How much to move the agent by in each step(); may become negative if
    // agent is moving from the end to the start of current line.
    private double moveRate = basemoveRate;
    
    
    
    
    // Used by agent to walk along line segment; assigned in setNewRoute()
//    private LengthIndexedLine segment = null;
    double startIndex = 0.0; // start position of current line
    double endIndex = 0.0; // end position of current line
    double currentIndex = 0.0; // current location along line
//    PointMoveTo pointMoveTo = new PointMoveTo();

//    static private GeometryFactory fact = new GeometryFactory();

    public DMasonPoint(DCampusWorld state)
    {
    	location = new Double2D(10,10); // magic numbers
//        location = new MasonGeometry(fact.createPoint(new Coordinate(10, 10))); // magic numbers
//        location.isMovable = true;
        
                // Find the first line segment and set our position over the start coordinate.
        int walkwayIndex = state.random.nextInt(state.walkways.getGeometries().numObjs);
        MasonGeometry mg = (MasonGeometry) state.walkways.getGeometries().objs[walkwayIndex];
        LineString walkway = (LineString) mg.getGeometry();
        setNewRoute(state, walkway, true);

//TODO
//        // Now set up attributes for this agent
//        if (state.random.nextBoolean())
//        {
//            location.addStringAttribute("TYPE", "STUDENT");
//
//            int age = (int) (20.0 + 2.0 * state.random.nextGaussian());
//
//            location.addIntegerAttribute("AGE", age);
//        }
//        else
//        {
//            location.addStringAttribute("TYPE", "FACULTY");
//
//            int age = (int) (40.0 + 9.0 * state.random.nextGaussian());
//
//            location.addIntegerAttribute("AGE", age);
//        }
//
//        // Not everyone walks at the same speed
//        basemoveRate *= Math.abs(state.random.nextGaussian());
//        location.addDoubleAttribute("MOVE RATE", basemoveRate);
    	
    	
    	
    	
    }



//    /**
//     * @return geometry representing agent location
//     */
//    public MasonGeometry getGeometry()
//    {
//        return location;
//    }

    public Double2D getGeometry() {//TODO rename?
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


//TODO
//    /** @return string indicating whether we are "FACULTY" or a "STUDENT" */
//    public String getType()
//    {
//        return location.getStringAttribute("TYPE");
//    }


    /** randomly selects an adjacent route to traverse
    */
    private void findNewPath(DCampusWorld state)
    {
        // find all the adjacent junctions
    	Coordinate coord = new Coordinate(location.getX(), location.getY());
        Node currentJunction = state.network.findNode(coord);
//        Node currentJunction = state.network.findNode(location.getGeometry().getCoordinate());

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

                
                Double2D start = new Double2D(startPoint.getX(), startPoint.getY());
                Double2D end = new Double2D(endPoint.getX(), endPoint.getY());
//                if (startPoint.equals(location.geometry))
//                {
//                    setNewRoute(state, newRoute, true);
//                } else
//                {
//                    if (endPoint.equals(location.geometry))
//                    {
//                        setNewRoute(state, newRoute, false);
//                    } else
//                    {
//                        System.err.println("Where am I?");
//                    }
//                }
                if (start.equals(location))
                {
                    setNewRoute(state, newRoute, true);
                } else
                {
                    if (end.equals(location))
                    {
                        setNewRoute(state, newRoute, false);
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
    private void setNewRoute(DCampusWorld state, LineString line, boolean start)
    {
    	LengthIndexedLine segment = new LengthIndexedLine(line);
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

        Double2D toLocation = new Double2D(startCoord.x, startCoord.y);
        
        moveTo(state, toLocation);
    }

//    public void moveTo(Coordinate c)
    public void moveTo(DCampusWorld state, Double2D to)
    {
//        pointMoveTo.setCoordinate(c);
//        location.getGeometry().apply(pointMoveTo);
//        getGeometry().geometry.geometryChanged();
    	state.agentLocations.moveAgent(to, this);
    }

    public void step(SimState state)
    {
        DCampusWorld campState = (DCampusWorld) state;
        move(campState);
    }

    /**
     * moves the agent along the grid
     *
     * @param geoTest handle on the base SimState
     *
     * The agent will randomly select an adjacent junction and then move along
     * the line segment to it. Then it will repeat.
     */
    private void move(DCampusWorld state)
    {
        // if we're not at a junction move along the current segment
        if (!arrived())
        {
            moveAlongPath(state);
        } else
        {
            findNewPath(state);
        }
    }



    // move agent along current line segment
    private void moveAlongPath(DCampusWorld state)
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

        // Copied from above >>>>
        int walkwayIndex = state.random.nextInt(state.walkways.getGeometries().numObjs);
        MasonGeometry mg = (MasonGeometry) state.walkways.getGeometries().objs[walkwayIndex];
        LineString walkway = (LineString) mg.getGeometry();
        // <<<<<<<<<<<<<<<<<<<<<<
        LengthIndexedLine segment = new LengthIndexedLine(walkway);
        Coordinate currentPosJTS = segment.extractPoint(currentIndex);
        Double2D currentPos = new Double2D(currentPosJTS.x, currentPosJTS.y);

        moveTo(state, currentPos);
    }

}
