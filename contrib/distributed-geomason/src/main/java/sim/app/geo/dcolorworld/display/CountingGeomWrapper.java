package sim.app.geo.dcolorworld.display;

import sim.app.geo.dcolorworld.DColorWorld;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;

/**
 *  Our custom extension of MasonGeometry.  This extension is applied to the districts, not the 
 *  agents.  All this class does, is add the ability to count the number of agents currently 
 *  inside the voting district. 
 */
public class CountingGeomWrapper extends MasonGeometry
{
    private static final long serialVersionUID = 3186655744206152969L;


    public CountingGeomWrapper()
    {
        super();
    }


    public int numAgentsInGeometry(DColorWorld d)
    {
    	
        //Bag coveredAgents = d.agents.getCoveredObjects(this);
        return d.agents.getAllAgentsInStorage().size();
    }
}