/*
 * CountingGeomWrapper.java
 *
 * $Id: CountingGeomWrapper.java,v 1.1 2010-04-05 17:21:53 mcoletti Exp $
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
    private GeomField agents;

    public CountingGeomWrapper(GeomField agents, Geometry g)
    {
        super(g);

        this.agents = agents;
    }

    public CountingGeomWrapper(GeomField agents, Geometry g, GeometryInfo gi)
    {
        super(g, gi);

        this.agents = agents;
    }

    public CountingGeomWrapper(GeomField agents, GeomWrapper gw)
    {
        super(gw.geometry,gw.fetchGeometryInfo());

        this.agents = agents;
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
