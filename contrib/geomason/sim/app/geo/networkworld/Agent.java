/*
 * Agent.java
 *
 * The agent that will be moving around the GeomField.  It will report
 * all other objects that are within a certain distance.
 *
 * $Id: Agent.java,v 1.2 2010-04-10 18:20:04 kemsulli Exp $
 */

package sim.app.geo.networkworld;

import sim.util.geo.NetworkDirectedEdge;
import sim.util.geo.PointMoveTo;
import sim.util.geo.NetworkEdge;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import com.vividsolutions.jts.planargraph.DirectedEdgeStar;
import com.vividsolutions.jts.planargraph.Node;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.geo.GeomWrapper;

/** Agent that moves through GeomField
 *
 * Agent moves back and forth across a line segment.
 *
 * @author mcoletti
 */
public class Agent implements Steppable {

    // point that denotes agent's position
    private Point location;

    // How much to move the agent by in each step(); may become negative if
    // agent is moving from the end to the start of current line.
    private double moveRate = 1.0;

    // Used by agent to walk along line segment; assigned in setNewRoute()
    private LengthIndexedLine segment = null;

    double startIndex = 0.0; // start position of current line
    double endIndex = 0.0; // end position of current line
    double currentIndex = 0.0; // current location along line

    /** used to change location_
     * 
     */
    private PointMoveTo pointMoveTo = new PointMoveTo();

    public Agent()
    {
        GeometryFactory fact = new GeometryFactory();
        location = fact.createPoint(new Coordinate(10,10)); // XXX magic numbers
    }

    /** return geometry representing agent location
     * 
     * @return geometry of location
     */
    public Geometry getGeometry() { return location; }

    /** 
     * 
     * @return true if the agent has arrived at the target intersection
     */
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


    /** Used to set up the agent for simulation start
     *
     * @param state
     */
    public void start(GeoTest state)
    {
        // Find the first line segment and set our position over the start coordinate.

        GeomWrapper line = (GeomWrapper) state.world.getGeometry().objs[0];
        setNewRoute((LineString) line.geometry, true);
    }

    /** randomly selects an adjacent route to traverse
     *
     * @param geoTest contains the network topology used to find route
     */
    private void findNewPath(GeoTest geoTest)
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
                        NetworkDirectedEdge directedEdge = (NetworkDirectedEdge) edges[i];
                        NetworkEdge edge = (NetworkEdge) directedEdge.getEdge();

                        // and start moving along it
                        LineString newRoute = edge.getLine();
                        Point startPoint = newRoute.getStartPoint();
                        Point endPoint = newRoute.getEndPoint();

                        if (startPoint.equals(location))
                            {
                                setNewRoute(newRoute, true);
                            }
                        else
                            {
                                if (endPoint.equals(location))
                                    {
                                        setNewRoute(newRoute, false);
                                    }
                                else // some how the agent is neither at the beginning or
                                    { // the end of the selected line segment
                                        System.err.println("Where the hell am I?");
                                    }
                            }
                    }
            }
        else
            {
                System.err.println("Cannot find node nearest to " + location);
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
                moveRate = 1.0; // ensure we move forward along segment
            }
        else
            {
                startCoord = segment.extractPoint(endIndex);
                currentIndex = endIndex;
                moveRate = -1.0; // ensure we move backward along segment
            }

        moveTo(startCoord);
    }

    /** move the agent to the given coordinates
     *
     * @param c position to move the agent to
     */
    public void moveTo(Coordinate c)
    {
        pointMoveTo.setCoordinate(c);
        location.apply(pointMoveTo);
    }


    public void step(SimState state)
    {
        move((GeoTest) state);
    }


    /** moves the agent along the grid
     *
     * @param geoTest handle on the base SimState
     *
     * The agent will randomly select an adjacent junction and then move
     * along the line segment to it.  Then it will repeat.
     */
    private void move(GeoTest geoTest)
    {
        // if we're not at a junction move along the current segment
        if ( ! arrived() )
            moveAlongPath();
        else
            findNewPath(geoTest);
    }

    /** move agent along current line segment
     * 
     */
    private void moveAlongPath()
    {
        currentIndex += moveRate;
        Coordinate currentPos = segment.extractPoint(currentIndex);
        moveTo(currentPos);
    }
}
