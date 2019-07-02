/**
 ** Agent.java
 **
 ** Copyright 2011 by Sarah Wise, Mark Coletti, Andrew Crooks, and
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 *
 * $Id$
 **
 **/
package sim.app.geo.gridlock;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import com.vividsolutions.jts.planargraph.Node;
import java.util.ArrayList;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.geo.GeomPlanarGraphDirectedEdge;
import sim.util.geo.GeomPlanarGraphEdge;
import sim.util.geo.MasonGeometry;
import sim.util.geo.PointMoveTo;



/**
 *  Our simple agent for the CampusWorld GeoMASON example.  The agent randomly wanders
 *   around the campus walkways.  When
 *  the agent reaches an intersection, it chooses a random direction and continues on.   
 *
 */
@SuppressWarnings("restriction")
public final class Agent implements Steppable
{

    private static final long serialVersionUID = -1113018274619047013L;
    Gridlock world;
    // Residence/Work Attributes
    String homeTract = "";
    String workTract = "";
    Node homeNode = null;
    Node workNode = null;
    // point that denotes agent's position
//    private Point location;
    private MasonGeometry location;
    // How much to move the agent by in each step()
    private double moveRate = .001;
    // Used by agent to walk along line segment
    private LengthIndexedLine segment = null;
    double startIndex = 0.0; // start position of current line
    double endIndex = 0.0; // end position of current line
    double currentIndex = 0.0; // current location along line
    GeomPlanarGraphEdge currentEdge = null;
    int linkDirection = 1;
    double speed = 0; // useful for graph
    ArrayList<GeomPlanarGraphDirectedEdge> pathFromHomeToWork =
        new ArrayList<GeomPlanarGraphDirectedEdge>();
    int indexOnPath = 0;
    int pathDirection = 1;
    boolean reachedDestination = false;
    PointMoveTo pointMoveTo = new PointMoveTo();

    /** This is the wrapper object in the agents layer.  We need a handle on
     * it so that we can update our location with each step().
     */
//    private MasonGeometry renderedGeometry;
//
//
//
//    public MasonGeometry getRenderedGeometry()
//    {
//        return renderedGeometry;
//    }
//
//
//
//    public void setRenderedGeometry(MasonGeometry renderedGeometry)
//    {
//        this.renderedGeometry = renderedGeometry;
//    }

    



    /** Constructor Function */
    public Agent(Gridlock g, String home, String work,
                 GeomPlanarGraphEdge startingEdge, GeomPlanarGraphEdge goalEdge)
    {
        world = g;

        // set up information about where the node is and where it's going
        homeNode = startingEdge.getDirEdge(0).getFromNode();
        workNode = goalEdge.getDirEdge(0).getToNode();
        homeTract = home;
        workTract = work;

        // set the location to be displayed
        GeometryFactory fact = new GeometryFactory();
        location = new MasonGeometry(fact.createPoint(new Coordinate(10, 10))) ;
        Coordinate startCoord = null;
        startCoord = homeNode.getCoordinate();
        updatePosition(startCoord);
    }



    /** Initialization of an Agent: find an A* path to work!
     *
     * @param state
     * @return whether or not the agent successfully found a path to work
     */
    public boolean start(Gridlock state)
    {
        findNewAStarPath(state);

        if (pathFromHomeToWork.isEmpty())
        {
            System.out.println("Initialization of agent failed: it is located in a part "
                + "of the network that cannot access the given goal node");
            return false;
        } else
        {
            return true;
        }
    }



    /** Plots a path between the Agent's home Node and its work Node */
    private void findNewAStarPath(Gridlock geoTest)
    {

        // get the home and work Nodes with which this Agent is associated
        Node currentJunction = geoTest.network.findNode(location.geometry.getCoordinate());
        Node destinationJunction = workNode;

        if (currentJunction == null)
        {
            return; // just a check
        }
        // find the appropriate A* path between them
        AStar pathfinder = new AStar();
        ArrayList<GeomPlanarGraphDirectedEdge> path =
            pathfinder.astarPath(currentJunction, destinationJunction);

        // if the path works, lay it in
        if (path != null && path.size() > 0)
        {

            // save it
            pathFromHomeToWork = path;

            // set up how to traverse this first link
            GeomPlanarGraphEdge edge =
                (GeomPlanarGraphEdge) path.get(0).getEdge();
            setupEdge(edge);

            // update the current position for this link
            updatePosition(segment.extractPoint(currentIndex));

        }
    }



    double progress(double val)
    {
        double edgeLength = currentEdge.getLine().getLength();
        double traffic = world.edgeTraffic.get(currentEdge).size();
        double factor = 1000 * edgeLength / (traffic * 5);
        factor = Math.min(1, factor);
        return val * linkDirection * factor;
    }



    /** Called every tick by the scheduler */
    /** moves the agent along the path */
    public void step(SimState state)
    {
        // check that we've been placed on an Edge
        if (segment == null)
        {
            return;
        } // check that we haven't already reached our destination
        else if (reachedDestination)
        {
            return;
        }

        // make sure that we're heading in the right direction
        boolean toWork = ((Gridlock) state).goToWork;
        if ((toWork && pathDirection < 0) || (!toWork && pathDirection > 0))
        {
            flipPath();
        }

        // move along the current segment
        speed = progress(moveRate);
        currentIndex += speed;

        // check to see if the progress has taken the current index beyond its goal
        // given the direction of movement. If so, proceed to the next edge
        if (linkDirection == 1 && currentIndex > endIndex)
        {
            Coordinate currentPos = segment.extractPoint(endIndex);
            updatePosition(currentPos);
            transitionToNextEdge(currentIndex - endIndex);
        } else if (linkDirection == -1 && currentIndex < startIndex)
        {
            Coordinate currentPos = segment.extractPoint(startIndex);
            updatePosition(currentPos);
            transitionToNextEdge(startIndex - currentIndex);
        } else
        { // just update the position!
            Coordinate currentPos = segment.extractPoint(currentIndex);

            updatePosition(currentPos);
        }

    }



    /** Flip the agent's path around */
    void flipPath()
    {
        reachedDestination = false;
        pathDirection = -pathDirection;
        linkDirection = -linkDirection;
    }



    /**
     * Transition to the next edge in the path
     * @param residualMove the amount of distance the agent can still travel
     * this turn
     */
    void transitionToNextEdge(double residualMove)
    {

        // update the counter for where the index on the path is
        indexOnPath += pathDirection;

        // check to make sure the Agent has not reached the end
        // of the path already
        if ((pathDirection > 0 && indexOnPath >= pathFromHomeToWork.size())
            || (pathDirection < 0 && indexOnPath < 0))// depends on where you're going!
        {
            System.out.println(this + " has reached its destination");
            reachedDestination = true;
            indexOnPath -= pathDirection; // make sure index is correct
            return;
        }

        // move to the next edge in the path
        GeomPlanarGraphEdge edge =
            (GeomPlanarGraphEdge) pathFromHomeToWork.get(indexOnPath).getEdge();
        setupEdge(edge);
        speed = progress(residualMove);
        currentIndex += speed;

        // check to see if the progress has taken the current index beyond its goal
        // given the direction of movement. If so, proceed to the next edge
        if (linkDirection == 1 && currentIndex > endIndex)
        {
            transitionToNextEdge(currentIndex - endIndex);
        } else if (linkDirection == -1 && currentIndex < startIndex)
        {
            transitionToNextEdge(startIndex - currentIndex);
        }
    }

    ///////////// HELPER FUNCTIONS ////////////////////////////


    /** Sets the Agent up to proceed along an Edge
     * @param edge the GeomPlanarGraphEdge to traverse next
     * */
    void setupEdge(GeomPlanarGraphEdge edge)
    {

        // clean up on old edge
        if (currentEdge != null)
        {
            ArrayList<Agent> traffic = world.edgeTraffic.get(currentEdge);
            traffic.remove(this);
        }
        currentEdge = edge;

        // update new edge traffic
        if (world.edgeTraffic.get(currentEdge) == null)
        {
            world.edgeTraffic.put(currentEdge, new ArrayList<Agent>());
        }
        world.edgeTraffic.get(currentEdge).add(this);

        // set up the new segment and index info
        LineString line = edge.getLine();
        segment = new LengthIndexedLine(line);
        startIndex = segment.getStartIndex();
        endIndex = segment.getEndIndex();
        linkDirection = 1;

        // check to ensure that Agent is moving in the right direction
        double distanceToStart = line.getStartPoint().distance(location.geometry),
            distanceToEnd = line.getEndPoint().distance(location.geometry);
        if (distanceToStart <= distanceToEnd)
        { // closer to start
            currentIndex = startIndex;
            linkDirection = 1;
        } else if (distanceToEnd < distanceToStart)
        { // closer to end
            currentIndex = endIndex;
            linkDirection = -1;
        }

    }



    /** move the agent to the given coordinates */
    public void updatePosition(Coordinate c)
    {
        pointMoveTo.setCoordinate(c);
//        location.geometry.apply(pointMoveTo);

        world.agents.setGeometryLocation(location, pointMoveTo);
    }



    /** return geometry representing agent location */
    public MasonGeometry getGeometry()
    {
        return location;
    }

}
