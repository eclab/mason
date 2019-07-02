/**
 ** Raindrop.java
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
package sim.app.geo.waterworld;

import java.util.ArrayList;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.field.grid.ObjectGrid2D;
import sim.util.Bag;



/** 
 *  A Steppable object that updates its position on the landscape as a function
 *  of its local cost surface.
 */
public class Raindrop implements Steppable
{

    WaterWorld world;		// the simulation of which the Raindrop is a part
    Basin basin; 			// the Basin in which the Raindrop currently resides
    ObjectGrid2D landscape; // the landscape of elevations and water
    Stoppable stopper;		// a variable used to unschedule the Raindrop
    private static final long serialVersionUID = 1L;



    /**
     * Constructor function.
     * @param ww - the WaterWorld object, kept so the Raindrop can update the
     * 		simulation if it exits the simulation
     * @param l - the ObjectGrid2D landscape object, kept so the Raindrop can
     * 		determine its local cost surface
     * @param b - the Basin in which the Raindrop finds itself
     */
    public Raindrop(WaterWorld ww, ObjectGrid2D l, Basin b)
    {
        world = ww;
        landscape = l;
        basin = b;
        stopper = null;
    }



    /**
     * Steppable that moves the Raindrop across the landscape.
     */
    @Override
    public void step(SimState state)
    {

        // get a copy of all of the neighbors of this tile
        Bag neighbors = new Bag();
        landscape.getNeighborsMaxDistance(
            basin.loc_x, basin.loc_y, 1, true, neighbors, null, null);

        // find the set of neighbors that is of minimal height
        ArrayList<Basin> mins = new ArrayList<Basin>();
        double minheight = Double.MAX_VALUE;
        for (Object o : neighbors)
        {
            Basin b = (Basin) o;
            if (b.cumulativeHeight < minheight)
            {
                mins = new ArrayList<Basin>(); // set up a new list of minimal neighbors
                mins.add(b); // add our new find to it
                minheight = b.cumulativeHeight;
            } else if (b.cumulativeHeight == minheight)
            {
                mins.add(b);
            }
        }


        // if we haven't found anything better than where we currently are, stay where we are!
        if (minheight >= basin.cumulativeHeight)
        {

            // if we're on the edge, fall off the edge
            if (basin.loc_x == 0 || basin.loc_y == 0
                || basin.loc_x == landscape.getWidth() - 1 || basin.loc_y == landscape.getHeight() - 1)
            {
                stopper.stop();
                basin.removeDrop(this);
                world.drops.remove(this);
            }

            // otherwise just hang out in this same basin!
            return;
        }

        // select randomly from the eligible neighbors
        Basin newbasin = mins.get(state.random.nextInt(mins.size()));

        // move to this new spot
        basin.removeDrop(this);
        newbasin.addDrop(this);
        basin = newbasin;
    }

}
