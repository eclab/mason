package sim.app.geo.dschellingspace;

import java.util.ArrayList;
import java.util.Random;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.util.AffineTransformation;

import sim.engine.SimState;
import sim.util.Bag;
import sim.util.geo.DGeomSteppable;
import sim.util.geo.MasonGeometry;

public class DPerson extends DGeomSteppable{
	
    private static final long serialVersionUID = 1L;

    /** What "class" the agent belongs to */
    public enum Affiliation { RED, BLUE }

    private Affiliation affiliation;


    // position information
    //MasonGeometry location;
    DSchellingGeometry region;

    // given parameters
    double personalThreshold = .5;
    double moveDist = 1000.;
    double minDist = 100.;

    // updated variables
    int numMoves = 0;


    /**
     * Constructor function
     */
    public DPerson(Affiliation a)
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
    boolean acceptable(DSchellingSpace world)
    {

        Bag neighbors = world.agents.getStorage().getGeomVectorField().getObjectsWithinDistance(mg, minDist);

        // calculate the proportion of unlike neighbors
        double unlikeNeighbors = 0.;
        for (Object o : neighbors)
        {
            DPerson neighbor = (DPerson) ((MasonGeometry) o).getUserData();
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


    /*
    public MasonGeometry getGeometry()
    {
        return mg;
    }
    */

    
    public void setGeometry(MasonGeometry mg)
    {
        this.mg = mg;
    }


    /**
     * Moves the Person randomly in the space, updating the SchellingPolygons
     * about their contents as it goes
     * @param world the SchellingSpace instance, which holds the GeomVectorFields
     */
    public void moveRandomly(DSchellingSpace world)
    {

        // the current location
        Coordinate coord = (Coordinate) mg.geometry.getCoordinate().clone();

        // find a new position
        Random rand = new Random();
        double xinc = moveDist * (rand.nextDouble() - .5),
            yinc = moveDist * (rand.nextDouble() - .5);
        coord.x += xinc;
        coord.y += yinc;

        // while the new position is not inside the space, keep trying
        while (!world.world.getStorage().getGeomVectorField().isInsideUnion(coord))
        {
            coord.x -= xinc;
            coord.y -= yinc;
            xinc = moveDist * (rand.nextDouble() - .5);
            yinc = moveDist * (rand.nextDouble() - .5);
            coord.x += xinc;
            coord.y += yinc;
        }

        // once the location works, move to the new location
        mg.geometry.apply(AffineTransformation.translationInstance(xinc, yinc));

        // if the Person has moved to a different region, update the SchellingPolygons
        // about their current contents
        if (!region.geometry.contains(mg.geometry))
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

        DSchellingSpace world = (DSchellingSpace) state;

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
    void determineCurrentRegion(DSchellingGeometry poly)
    {

        // keep track of which SchellingPolygons have been investigated and which
        // are about to be investigated
        ArrayList<DSchellingGeometry> checked = new ArrayList<DSchellingGeometry>();
        ArrayList<DSchellingGeometry> toCheck = new ArrayList<DSchellingGeometry>();

        checked.add(poly); // we know it's not where it was anymore!
        toCheck.addAll(poly.neighbors); // first check the neighbors

        // while there is a Polygon to investigate, keep running
        while (toCheck.size() > 0)
        {
            DSchellingGeometry p = toCheck.remove(0);

            if (p.geometry.contains(mg.geometry))
            { // ---successfully located!---
                region = p;
                return;
            } else
            {
                checked.add(p); // we have investigated this polygon

                // add all uninvestigated neighbors not already slated for investigation
                for (DSchellingGeometry n : p.neighbors)
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
