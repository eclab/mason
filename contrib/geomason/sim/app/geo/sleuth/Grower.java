/**
 ** Copyright 2011 by Sarah Wise, Mark Coletti, Andrew Crooks, and
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 ** $Id$
 **/
package sim.app.geo.sleuth;

import java.util.ArrayList;
import sim.engine.SimState;
import sim.engine.Steppable;



/**
 */
class Grower implements Steppable
{
    private static final long serialVersionUID = 1L;


    Grower()
    {
        super();
    }



    @Override
    public void step(SimState state)
    {
        SleuthWorld world = (SleuthWorld) state;

        // calculate the coefficients
        double dispersion_value = (world.dispersionCoefficient * 0.0050) * Math.sqrt(world.grid_width * world.grid_width + world.grid_height * world.grid_height);
        double rg_value = (world.roadGravityCoefficient / world.maxCoefficient) * ((world.grid_width + world.grid_height) / 16.0);
        double max_search_index = 4.0 * (rg_value * (1.0 + rg_value));

        // spontaneously urbanizes cells with some probability
        ArrayList<Tile> spontaneouslyUrbanized = world.spontaneousGrowth(dispersion_value);

        // determines whether any of the new, spontaneously urbanized cells will
        // become new urban spreading centers. If the cell is allowed to become
        // a spreading center, two additional cells adjacent to the new spreading
        // center cell also have to be urbanized
        ArrayList<Tile> spreadFromUrbanized = world.newSpreadingCenters(spontaneouslyUrbanized);

        // growth propagates both the new centers generated in newSpreadingCenters
        // in this time step and the more established centers from earlier Steps
        ArrayList<Tile> growthAroundCenters = world.edgeGrowth();

        // compile a list of all cells urbanized this turn; preallocate the array
        // to accommodate the stuff we're about to add to it.
        ArrayList<Tile> allGrowthThisTurn = new ArrayList<Tile>(spontaneouslyUrbanized.size() + spreadFromUrbanized.size() + growthAroundCenters.size());
        
        allGrowthThisTurn.addAll(spontaneouslyUrbanized);
        allGrowthThisTurn.addAll(spreadFromUrbanized);
        allGrowthThisTurn.addAll(growthAroundCenters);
        
        // newly urbanized cells search for nearby roads. If they encounter them,
        // they build on this infrastructure by establishing a new urban area
        // some random walk along the road away from themselves. If this area is
        // prime for urbanization, two further neighbors of our new roadside cell
        // are urbanized.
        world.roadInfluencedGrowth(max_search_index, allGrowthThisTurn);
    }

}
