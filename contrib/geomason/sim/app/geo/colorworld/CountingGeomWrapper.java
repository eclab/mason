/* 
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 *
 * See the file "LICENSE" for more information
 *
 * $Id$
*/
package colorworld;


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


    public int numAgentsInGeometry()
    {
        Bag coveredAgents = ColorWorld.agents.getCoveredObjects(this);
        return coveredAgents.numObjs;
    }
}
