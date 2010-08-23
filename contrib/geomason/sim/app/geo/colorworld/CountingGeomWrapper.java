package sim.app.geo.colorworld;

import sim.field.geo.GeomVectorField;
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

    private static final long serialVersionUID = 3186655744206152969L;
	// We need to access this to count agents in our district
    public GeomVectorField agents; // refers to encompassing GeomVectorField

    public CountingGeomWrapper()
    {
        super();
    }

    /** Returned value corresponds to number of agents in associated region
     *
     * @return
     */
    public int numAgentsInGeometry()
    {
        // Grind through all the agents and find out how many are inside our
        // polygon
        Bag coveredAgents = agents.getCoveredObjects(geometry);
        return coveredAgents.numObjs;
    }

}
