/*
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 *
 * See the file "LICENSE" for more information
 *
 * $Id: Agent.java 846 2013-01-08 21:47:51Z mcoletti $
 */
package sim.app.geo.dcampusworld;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import com.vividsolutions.jts.planargraph.DirectedEdgeStar;
import com.vividsolutions.jts.planargraph.Node;

import mpi.MPIException;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.DoublePoint;
import sim.util.geo.GeomPlanarGraphDirectedEdge;
import sim.util.geo.GeomPlanarGraphEdge;
import sim.util.geo.MasonGeometry;
import sim.util.geo.PointMoveTo;



/**
 * Our simple agent for the CampusWorld GeoMASON example. The agent randomly
 * wanders around the campus walkways. When the agent reaches an intersection,
 * it continues in a random direction.
 *
 */
public class DAgent implements Steppable
{

    private static final long serialVersionUID = 1L;
    // point that denotes agent's position
    private MasonGeometry location;
    public DoublePoint position;
    // The base speed of the agent.
    private double basemoveRate = 1.0;
    // How much to move the agent by in each step(); may become negative if
    // agent is moving from the end to the start of current line.
    private double moveRate = basemoveRate;
    // Used by agent to walk along line segment; assigned in setNewRoute()

    // segmentGeometry is the hack to bypass the problem that LengthIndexedLine class is not serializable 
    private Geometry segmentGeometry = null;
    private transient LengthIndexedLine segment = null;
    double startIndex = 0.0; // start position of current line
    double endIndex = 0.0; // end position of current line
    double currentIndex = 0.0; // current location along line
    PointMoveTo pointMoveTo = new PointMoveTo();

    static private GeometryFactory fact = new GeometryFactory();
    
    public DAgent()
    {
    	
    }

    public DAgent(DCampusWorld state) throws MPIException
    {
        location = new MasonGeometry(fact.createPoint(new Coordinate(10, 10))); // magic numbers
        location.isMovable = true;
        
        // Find the first line segment and set our position over the start coordinate.
        int walkway = state.random.nextInt(state.walkways.getGeometries().numObjs);
        MasonGeometry mg = (MasonGeometry) state.walkways.getGeometries().objs[walkway];
        setNewRoute((LineString) mg.getGeometry(), true);
    	double x = state.communicator.toXCoord(position.c[0]);
    	double y = state.communicator.toYCoord(position.c[1]);
    	position = new DoublePoint(x, y);


        // Now set up attributes for this agent
        if (state.random.nextBoolean())
        {
            location.addStringAttribute("TYPE", "STUDENT");

            int age = (int) (20.0 + 2.0 * state.random.nextGaussian());

            location.addIntegerAttribute("AGE", age);
        }
        else
        {
            location.addStringAttribute("TYPE", "FACULTY");

            int age = (int) (40.0 + 9.0 * state.random.nextGaussian());

            location.addIntegerAttribute("AGE", age);
        }

        // Not everyone walks at the same speed
        basemoveRate *= Math.abs(state.random.nextGaussian());
        location.addDoubleAttribute("MOVE RATE", basemoveRate);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
    	// just do not write segment to stream
    	out.defaultWriteObject();
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    	in.defaultReadObject();
    	// reconstruct segment from segmentGeometry
    	if (segmentGeometry != null)
        {
            segment = new LengthIndexedLine(segmentGeometry);
        }
    }


    /**
     * @return geometry representing agent location
     */
    public MasonGeometry getGeometry()
    {
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
        return location.getStringAttribute("TYPE");
    }


    /** randomly selects an adjacent route to traverse
    */
    private void findNewPath(DCampusWorld geoTest)
    {
        // find all the adjacent junctions
        Node currentJunction = geoTest.network.findNode(location.getGeometry().getCoordinate());

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

                if (startPoint.equals(location.geometry))
                {
                    setNewRoute(newRoute, true);
                } else
                {
                    if (endPoint.equals(location.geometry))
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
    	// update segmentGeometry in case of serialization
    	segmentGeometry = line;
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
	public void moveTo(Coordinate c)
	{
		pointMoveTo.setCoordinate(c);
		location.getGeometry().apply(pointMoveTo);
		getGeometry().geometry.geometryChanged();
		updatePosition(c);
	}

	private void updatePosition(Coordinate c)
	{
		// record the original coordinates
		position = new DoublePoint(c.x, c.y);
	}


    public void step(SimState state)
    {
        DCampusWorld campState = (DCampusWorld) state;
        move(campState);
        
        // update position to coordinate in pixels (see pixelwidth method in GeomGridField)
        double x = campState.communicator.toXCoord(position.c[0]);
        double y = campState.communicator.toYCoord(position.c[1]);
        DoublePoint loc = new DoublePoint(x, y);
        this.position = loc;
        
        try {
            int dst = campState.partition.toPartitionId(new double[] {loc.c[0], loc.c[1]});
            if (dst != campState.partition.getPid()) {
            	// Need to migrate to other partition, 
            	// remove from current partition 
            	campState.agents.removeGeometry(this.getGeometry());
            	campState.communicator.removeObject(this);
            	campState.queue.migrate(this, dst, loc);           
            } else {        
            	// Set to new location in current partition
            	campState.communicator.setLocation(this, loc);
            	campState.schedule.scheduleOnce(this, 1);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(-1);
        }
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
}
