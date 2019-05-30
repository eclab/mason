/**
 ** Space.java
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
package sim.app.geo.sillypeds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import sim.field.geo.GeomGridField;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.ObjectGrid2D;



public class Space
{
    /** Indicates grid cells that are outside the building.
     *
     */
    private static final int NO_DATA = -9999;

    int width;
    int height;

    ObjectGrid2D field;

    /** The set of navigable tiles
     */
    ArrayList<Tile> validTiles;

    /** maps a certain exit to go to a given
     * other space and the tile in that space where the exit dumps you
     *
     */
    HashMap<Tile, Entrance> exits = new HashMap<Tile, Entrance>();


    Space(GeomGridField floorPlan)
    {
        width = floorPlan.getGridWidth();
        height = floorPlan.getGridHeight();

        field = new ObjectGrid2D(width, height);

        // list of all tiles that can be accessed, e.g. not dead space
        validTiles = new ArrayList<Tile>();

        DoubleGrid2D heightField = (DoubleGrid2D) floorPlan.getGrid();

        Tile t = null;

        // initialize the Tiles in the correct places with the appropriate gradients
        for (int x = 0; x < width; x++)
        {
            for (int y = 0; y < height; y++)
            {
                double baseHeight = heightField.get(x, y);

                // If the current grid cell is outside the building, we need
                // to ensure that the gradient is Double.MAX_VALUE because
                // otherwise the value of NO_DATA, -9999, would be *lower* than
                // the gradients found *inside* the building; if we didn't
                // change the tiles outside to use Double.MAX_VALUE, then
                // pedestrians would eagerly pass through the walls of the
                // building.
                if (baseHeight == NO_DATA)
                {
                    t = new Tile(x, y, Double.MAX_VALUE);
                } else
                {
                    t = new Tile(x, y, baseHeight);
                    validTiles.add(t);
                }
                
                field.set(x, y, t);
            }
        }
    }



    /**
     * add Pedestrians to the Space
     * <p>
     * XXX I <em>think</em> the idea is to randomly place pedestrians in
     * valid tiles, ensuring that there is only one pedestrian per tile.
     *
     * @param sp - the SillyPeds object
     * @param numPeds - number of Pedestrians to add to the simulation
     * @return list of all new, added Pedestrians
     */
    ArrayList<Pedestrian> populate(SillyPeds sp, int numPeds)
    {
        ArrayList<Pedestrian> peds = new ArrayList<Pedestrian>();

        // Copy over the valid tiles and shuffle them.  Then we iterate
        // through the shuffled copy setting a pedestrian to each tile.
        ArrayList<Tile> randomTiles = new ArrayList<Tile>(this.validTiles);

        // Unfortunately we cannot use the MASON RNG (i.e., SillyPeds.random)
        // because it isn't compatible with Colletions.shuffle().  So we have
        // to fall back on the legacy Java RNG.
        Random rng = new Random();
        Collections.shuffle(randomTiles, rng);

        // Having more pedestrians than available space is a Bad Thing.
        assert validTiles.size() >= numPeds : "not enough valid tiles";

        // go through all Pedestrians to create and schedule them
        for (int i = 0; i < numPeds; i++)
        {
            Tile t = randomTiles.get(i);

            Pedestrian p = new Pedestrian(sp, this, t);
            
            t.addPed(p);
            peds.add(p);

            // only schedule it once: the Pedestrian will reschedule itself
            // also note that it schedules its ordering according to the value
            // of its current tile's gradient, i.e. distance from an exit
            sp.schedule.scheduleOnce(p, (int) (1 + t.baseheight));
        }

        return peds;
    }

    // tells a Pedestrian what space a Tile/exit leads to


    public Entrance exit(Tile t)
    {
        return exits.get(t);
    }

}
