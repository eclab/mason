package sim.app.geo.colorworld;

import sim.field.geo.GeomField;
import sim.util.Bag;
import sim.util.geo.*;


/** Maintains count of population to be used in color LUT
 *
 * Is associated with a FFX counting voting district polygon.  It will return
 * the number of agents in that polygon.
 *
 * @author mcoletti
 */
public class CountingGeomWrapper extends MasonGeometry {

    // We need to access this to count agents in our district
    public GeomField agents; // refers to encompassing GeomField

    public CountingGeomWrapper()
    {
        super();
    }

    /** Returned value corresponds to number of agents in associated region
     *
     * @return
     */
    public double doubleValue()
    {
        // Grind through all the agents and find out how many are inside our
        // polygon
        Bag coveredAgents = agents.getCoveredObjects(geometry);
        return coveredAgents.numObjs;
    }

}
