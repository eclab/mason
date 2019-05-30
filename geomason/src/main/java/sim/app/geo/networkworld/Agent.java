/* 
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 *
 * See the file "LICENSE" for more information
 *
 * $Id: Agent.java,v 1.7 2010-08-25 20:06:51 mcoletti Exp $
 */
package sim.app.geo.networkworld;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import com.vividsolutions.jts.planargraph.DirectedEdgeStar;
import com.vividsolutions.jts.planargraph.Node;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.geo.GeomPlanarGraphDirectedEdge;
import sim.util.geo.GeomPlanarGraphEdge;
import sim.util.geo.MasonGeometry;
import sim.util.geo.PointMoveTo;



/**
 * A simple agent for the NetworkWorld GeoMASON example. The agent moves
 * randomly along the network, choosing a random direction at each intersection.
 */
public class Agent implements Steppable
{

    private static final long serialVersionUID = -7060584745540577823L;
    // point that denotes agent's position
    private Point location;
    // How much to move the agent by in each step(); may become negative if
    // agent is moving from the end to the start of current line.
    private double moveRate = 0.01;
    // Used by agent to walk along line segment; assigned in setNewRoute()
    private LengthIndexedLine segment = null;
    double startIndex = 0.0; // start position of current line
    double endIndex = 0.0; // end position of current line
    double currentIndex = 0.0; // current location along line
    // used to update location 
    private PointMoveTo pointMoveTo = new PointMoveTo();



    public Agent()
    {
        GeometryFactory fact = new GeometryFactory();
        location = fact.createPoint(new Coordinate(10, 10)); // magic numbers
    }

    // return geometry representing agent location


    public Geometry getGeometry()
    {
        return location;
    }

    // true if the agent has arrived at the target intersection
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



    public void start(NetworkWorld state)
    {
        // Find the first line segment and set our position over the start coordinate.

        MasonGeometry line = (MasonGeometry) state.world.getGeometries().objs[0];
        setNewRoute((LineString) line.geometry, true);
    }


    // randomly selects an adjacent route to traverse
    private void findNewPath(NetworkWorld NetworkWorld)
    {
        // find all the adjacent junctions
        Node currentJunction = NetworkWorld.network.findNode(location.getCoordinate());

        if (currentJunction != null)
        {
            DirectedEdgeStar directedEdgeStar = currentJunction.getOutEdges();
            Object[] edges = directedEdgeStar.getEdges().toArray();

            if (edges.length > 0)
            {
                // pick one randomly
                int i = NetworkWorld.random.nextInt(edges.length);
                GeomPlanarGraphDirectedEdge directedEdge = (GeomPlanarGraphDirectedEdge) edges[i];
                GeomPlanarGraphEdge edge = (GeomPlanarGraphEdge) directedEdge.getEdge();

                // and start moving along it
                LineString newRoute = edge.getLine();
                Point startPoint = newRoute.getStartPoint();
                Point endPoint = newRoute.getEndPoint();

                if (startPoint.equals(location))
                {
                    setNewRoute(newRoute, true);
                } else
                {
                    if (endPoint.equals(location))
                    {
                        setNewRoute(newRoute, false);
                    } else // some how the agent is neither at the beginning or
                    { // the end of the selected line segment
                        System.err.println("Where the hell am I?");
                    }
                }
            }
        } else
        {
            System.err.println("Cannot find node nearest to " + location);
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
            moveRate = Math.abs(moveRate); // ensure we move forward along segment
        } // by using a positive value
        else
        {
            startCoord = segment.extractPoint(endIndex);
            currentIndex = endIndex;
            moveRate = -Math.abs(moveRate); // ensure we move backward along segment
        }                                    // by using a negative value

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
        // if we're not at a junction move along the current segment
        if (!arrived())
        {
            moveAlongPath((NetworkWorld) state);
        } else
        {
            findNewPath((NetworkWorld) state);
        }
    }



    /**
     * Ensure the current position in clipped to the line
     *
     * @param currentIndex that's guaranteed to be on the current line
     */
    private double clipCurrentIndex(double currentIndex)
    {
        // If move rate is positive ensure we're not off the end of the line.
        if (moveRate > 0)
        {
            return Math.min(currentIndex, endIndex);
        } else // else we're moving backwards from the other end, so ensure
        {    // we're not going to fall off the front
            return Math.max(currentIndex, startIndex);
        }
    }



    // move agent along current line segment
    private void moveAlongPath(NetworkWorld world)
    {
        currentIndex += moveRate;

        currentIndex = clipCurrentIndex(currentIndex);

        Coordinate currentPos = segment.extractPoint(currentIndex);
        moveTo(currentPos);

        world.agents.clear();
        world.agents.addGeometry(new MasonGeometry(this.location));
    }

}
