package sim.app.geo.colorworld;


import sim.util.Bag;
import sim.util.geo.MasonGeometry;

/**
 *  Our custom extension of MasonGeometry.  This extension is applied to the districts, not the 
 *  agents.  All this class does, is add the ability to count the number of agents currently 
 *  inside the voting district. 
 */

public class CountingGeomWrapper extends MasonGeometry {

    private static final long serialVersionUID = 3186655744206152969L;

    public int numAgentsInGeometry()
    {
        Bag coveredAgents = ColorWorld.agents.getCoveredObjects(geometry);
    	//Bag coveredAgents = ColorWorld.agents.queryField(geometry.getEnvelopeInternal()); 
        return coveredAgents.numObjs;
    }
}
