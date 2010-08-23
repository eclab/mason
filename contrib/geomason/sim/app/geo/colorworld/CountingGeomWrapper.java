package sim.app.geo.colorworld;


import sim.util.Bag;
import sim.util.geo.MasonGeometry;


/** Maintains count of population to be used in color LUT
 *
 * Is associated with a FFX counting voting district polygon.  It will return
 * the number of agents in that polygon.
 *
 * @author mcoletti
 */
public class CountingGeomWrapper extends MasonGeometry {

    private static final long serialVersionUID = 3186655744206152969L;

    /** Returned value corresponds to number of agents in associated region
     *
     * @return
     */
    public int numAgentsInGeometry()
    {
        Bag coveredAgents = ColorWorld.agents.getCoveredObjects(geometry);
        return coveredAgents.numObjs;
    }

}
