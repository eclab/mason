package sim.app.geo.riftland.household;

import ec.util.MersenneTwisterFast;
import sim.app.geo.riftland.World;
import sim.app.geo.riftland.parcel.GrazableArea;

/**
 *
 * @author Eric 'Siggy' Scott
 */
interface MoveStrategy
{
    /** Moves the Herding and returns the new location. */
    public GrazableArea computeTarget(World world, MersenneTwisterFast random, Herding herding);
}
