/**
 ** Person.java
 **
 ** Copyright 2011 by Sarah Wise, Mark Coletti, Andrew Crooks, and
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 ** $Id$
 **/
package sim.app.geo.schellingspace;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import java.util.ArrayList;
import java.util.Random;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;



/**
 * The mobile agent in the simulation.
 * 
 */
//@SuppressWarnings("restriction")
public class Person implements Steppable
{
    private static final long serialVersionUID = 1L;

    /** What "class" the agent belongs to */
    public enum Affiliation { RED, BLUE }

    private Affiliation affiliation;


    // position information
    MasonGeometry location;
    SchellingGeometry region;

    // given parameters
    double personalThreshold = .5;
    double moveDist = 1000.;
    double minDist = 100.;

    // updated variables
    int numMoves = 0;


    /**
     * Constructor function
     */
    public Person(Affiliation a)
    {
        affiliation = a;
    }



    public Affiliation getAffiliation()
    {
        return affiliation;
    }



    public void setAffiliation(Affiliation affiliation)
    {
        this.affiliation = affiliation;
    }



    /**
     * @param world the SchellingSpace, which holds the GeomVectorFields
     * @return whether the location is acceptable for the Person,
     * based on the Person's personalThreshold
     */
    boolean acceptable(SchellingSpace world)
    {

        Bag neighbors = world.agents.getObjectsWithinDistance(location, minDist);

        // calculate the proportion of unlike neighbors
        double unlikeNeighbors = 0.;
        for (Object o : neighbors)
        {
            Person neighbor = (Person) ((MasonGeometry) o).getUserData();
            if (! neighbor.getAffiliation().equals(affiliation))
            {
                unlikeNeighbors++;
            }
        }

        // if the location is unacceptable, return false
        if (unlikeNeighbors / neighbors.numObjs > personalThreshold)
        {
            return false;
        } else // if it is acceptable, return true
        {
            return true;
        }
    }



    public MasonGeometry getGeometry()
    {
        return location;
    }



    /**
     * Moves the Person randomly in the space, updating the SchellingPolygons
     * about their contents as it goes
     * @param world the SchellingSpace instance, which holds the GeomVectorFields
     */
    public void moveRandomly(SchellingSpace world)
    {

        // the current location
        Coordinate coord = (Coordinate) location.geometry.getCoordinate().clone();

        // find a new position
        Random rand = new Random();
        double xinc = moveDist * (rand.nextDouble() - .5),
            yinc = moveDist * (rand.nextDouble() - .5);
        coord.x += xinc;
        coord.y += yinc;

        // while the new position is not inside the space, keep trying
        while (!world.world.isInsideUnion(coord))
        {
            coord.x -= xinc;
            coord.y -= yinc;
            xinc = moveDist * (rand.nextDouble() - .5);
            yinc = moveDist * (rand.nextDouble() - .5);
            coord.x += xinc;
            coord.y += yinc;
        }

        // once the location works, move to the new location
        location.geometry.apply(AffineTransformation.translationInstance(xinc, yinc));

        // if the Person has moved to a different region, update the SchellingPolygons
        // about their current contents
        if (!region.geometry.contains(location.geometry))
        {
            region.residents.remove(this);
            determineCurrentRegion(region);
            region.residents.add(this);
        }

        // update the number of moves made
        numMoves++;
    }



    /**
     * Determines whether the Person's current location is acceptable.
     * If the location is not acceptable, attempts to move the Person to
     *  a better location.
     */
    @Override
    public void step(SimState state)
    {

        SchellingSpace world = (SchellingSpace) state;

        // check to see if the number of neighbors exceeds a given tolerance
        if (!acceptable(world))
        {
            moveRandomly(world); // if it does, move randomly
        }
    }



    /**
     * breadth first search on the polygons to determine current location relative to
     * SchellingPolygons
     * @param poly the SchellingGeometry in which the Person last found himself
     */
    void determineCurrentRegion(SchellingGeometry poly)
    {

        // keep track of which SchellingPolygons have been investigated and which
        // are about to be investigated
        ArrayList<SchellingGeometry> checked = new ArrayList<SchellingGeometry>();
        ArrayList<SchellingGeometry> toCheck = new ArrayList<SchellingGeometry>();

        checked.add(poly); // we know it's not where it was anymore!
        toCheck.addAll(poly.neighbors); // first check the neighbors

        // while there is a Polygon to investigate, keep running
        while (toCheck.size() > 0)
        {
            SchellingGeometry p = toCheck.remove(0);

            if (p.geometry.contains(location.geometry))
            { // ---successfully located!---
                region = p;
                return;
            } else
            {
                checked.add(p); // we have investigated this polygon

                // add all uninvestigated neighbors not already slated for investigation
                for (SchellingGeometry n : p.neighbors)
                {
                    if (!checked.contains(n) && !toCheck.contains(n))
                    {
                        toCheck.add(n);
                    }
                }
            }
        }

        // if it's not anywhere, throw an error
        System.out.println("ERROR: Person is not located within any polygon");
    }

}
