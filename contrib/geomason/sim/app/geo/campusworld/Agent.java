/* 
Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
George Mason University Mason University Licensed under the Academic
Free License version 3.0

See the file "LICENSE" for more information
*/
package sim.app.geo.campusworld;

import sim.util.geo.*; 
import com.vividsolutions.jts.geom.*; 
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import com.vividsolutions.jts.planargraph.DirectedEdgeStar;
import com.vividsolutions.jts.planargraph.Node;
import sim.engine.SimState;
import sim.engine.Steppable;

/**
 *  Our simple agent for the CampusWorld GeoMASON example.  The agent randomly wanders around the campus walkways.  When
 *  the agent reaches an intersection, it chooses a random direction and continues on.   
 *
 */
public class Agent implements Steppable {

    private static final long serialVersionUID = -1113018274619047013L;

	// point that denotes agent's position
    private Point location;

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

    public Agent()
    {
        GeometryFactory fact = new GeometryFactory();
        location = fact.createPoint(new Coordinate(10,10)); // magic numbers
    }

    /** return geometry representing agent location */
    public Geometry getGeometry()
    {
        return location;
    }

    // true if the agent has arrived at the target intersection
    private boolean arrived()
    {
        // If we have a negative move rate the agent is moving from the end to
        // the start, else the agent is moving in the opposite direction.
        if ( (moveRate > 0 && currentIndex >= endIndex) ||
             (moveRate < 0 && currentIndex <= startIndex) )
            {
                return true;
            }

        return false;
    }


    public void start(CampusWorld state)
    {
        // Find the first line segment and set our position over the start coordinate.
    	int walkway = state.random.nextInt(state.walkways.getGeometries().numObjs);
        MasonGeometry mg = (MasonGeometry)state.walkways.getGeometries().objs[walkway];
        setNewRoute((LineString) mg.getGeometry(), true);
    }
    
    // randomly selects an adjacent route to traverse
    private void findNewPath(CampusWorld geoTest)
    {
        // find all the adjacent junctions
        Node currentJunction = geoTest.network.findNode(location.getCoordinate());
        
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

                        if (startPoint.equals(location))
                            setNewRoute(newRoute, true);
                        else
                            {
                                if (endPoint.equals(location))
                                    setNewRoute(newRoute, false);
                                else // some how the agent is neither at the beginning or the end of the selected line segment
                                    System.err.println("Where am I?");
                            }
                    }
            }
    }

    

    /** have the agent move along new route
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

        if ( start )
            {
                startCoord = segment.extractPoint(startIndex);
                currentIndex = startIndex;
                moveRate = basemoveRate; // ensure we move forward along segment
            }
        else
            {
                startCoord = segment.extractPoint(endIndex);
                currentIndex = endIndex;
                moveRate = - basemoveRate; // ensure we move backward along segment
            }

        moveTo(startCoord);
    }

    // move the agent to the given coordinates
    public void moveTo(Coordinate c)
    {
        pointMoveTo.setCoordinate(c);
        location.apply(pointMoveTo);
    }

    

    public void step(SimState state)
    {
    	CampusWorld campState = (CampusWorld)state; 
        move(campState);
        campState.agents.setGeometryLocation(getGeometry(), pointMoveTo);         
    }


    /** moves the agent along the grid
     *
     * @param geoTest handle on the base SimState
     *
     * The agent will randomly select an adjacent junction and then move
     * along the line segment to it.  Then it will repeat.
     */
    private void move(CampusWorld geoTest)
    {
        // if we're not at a junction move along the current segment
        if ( ! arrived() )
        	moveAlongPath();
        else
        	findNewPath(geoTest);
    }

    

   // move agent along current line segment
    private void moveAlongPath()
    {
        currentIndex += moveRate;
        
        // Truncate movement to end of line segment
        if ( moveRate < 0)
            { // moving from endIndex to startIndex
                if ( currentIndex < startIndex)
                    {
                        currentIndex = startIndex;
                    }
            }
        else
            { // moving from startIndex to endIndex
                if (currentIndex > endIndex)
                    {
                        currentIndex = endIndex;
                    }
            }

        Coordinate currentPos = segment.extractPoint(currentIndex);

        moveTo(currentPos);
    }
}
