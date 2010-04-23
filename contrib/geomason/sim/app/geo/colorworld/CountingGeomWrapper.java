/*
 * CountingGeomWrapper.java
 *
 * $Id: CountingGeomWrapper.java,v 1.3 2010-04-23 21:39:12 mcoletti Exp $
 */

package sim.app.geo.colorworld;

import com.vividsolutions.jts.geom.*;
import sim.field.geo.GeomField;
import sim.io.geo.GeometryInfo;
import sim.util.Bag;
import sim.util.geo.GeomWrapper;
import com.vividsolutions.jts.algorithm.locate.*;


/** Maintains count of population to be used in color LUT
 *
 * Is associated with a FFX counting voting district polygon.  It will return
 * the number of agents in that polygon.
 *
 * @author mcoletti
 */
public class CountingGeomWrapper extends GeomWrapper {

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
