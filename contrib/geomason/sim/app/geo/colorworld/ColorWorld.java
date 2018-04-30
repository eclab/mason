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

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.engine.SimState;
import sim.field.geo.GeomVectorField;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;



/**
 *  The ColorWorld example shows how to change the portrayal of individual geometries based on 
 *  simulation information.  To do this, we create our own portrayal and MasonGeometry.  The portrayal
 *  accesses the simulation core via the extended MasonGeometry class.  
 *  
 *  This simulation has agents wandering randomly around Fairfax County, VA, voting districts.  There are 
 *  12 districts total.  The color of the district changes shade based on the number of agents currently 
 *  inside the district.  If no agents are inside the district, then the district is not shown (actually, its 
 *  drawn as white, on a white background).  
 *  
 *  There are two special things about this simulation: First, we subclass MasonGeometry to count the 
 *  number of agents inside each district, and use this subclass as a replacement for the standard MasonGeometry.  The 
 *  replacement *MUST* be done prior to ingesting the files to ensure that the GeomField uses our subclass rather than 
 *  the standard MasonGeometry.  Second, we use the global Union of the voting districts to determine if the agents are 
 *  wandering out of the county.  Doing this instead of looping through all the districts provides at least an order of 
 *  magnitude speedup.  We also compute the ConvexHull, mainly to show how its done.    
 *
 */

public class ColorWorld extends SimState
{
    private static final long serialVersionUID = -2568637684893865458L;


	public static final int WIDTH = 300; 
	public static final int HEIGHT = 300; 

	// number of agents in the simulation
    public static int NUM_AGENTS = 20;

    // where all the county geometry lives
    public GeomVectorField county = new GeomVectorField(WIDTH, HEIGHT);

    // where all the agents live.  We use a GeomVectorField since we want to determine how 
    // many agents are inside each district.  The most efficient way to do this is via 
    // the GeomVectorField's spatial indexing.  
    public static GeomVectorField agents = new GeomVectorField(WIDTH, HEIGHT);

    // getters and setters for inspectors
    public int getNumAgents() { return NUM_AGENTS; }
    public void setNumAgents(int a) { if (a > 0) NUM_AGENTS = a; }



    public ColorWorld(long seed)
    {
        super(seed);

        // this line allows us to replace the standard MasonGeometry with our
        // own subclass of MasonGeometry; see CountingGeomWrapper.java for more info.
        // Note: this line MUST occur prior to ingesting the data
        URL politicalBoundaries = ColorWorld.class.getResource("data/pol.shp");

        Bag empty = new Bag();
        try
        {
            ShapeFileImporter.read(politicalBoundaries, county, empty, CountingGeomWrapper.class);
        } catch (Exception ex)
        {
            Logger.getLogger(ColorWorld.class.getName()).log(Level.SEVERE, null, ex);
        }

        // we use either the ConvexHull or Union to determine if the agents are within
        // Fairfax county or not
        county.computeConvexHull();
        county.computeUnion();

    }

    private void addAgents()
    {
        //Agent a = null;

        for (int i = 0; i < NUM_AGENTS; i++)
            {
                // pick a random political region to plop the agent in
                Bag allRegions = county.getGeometries();

                if (allRegions.isEmpty())
                    {
                        // Something went wrong.  We *should* have regions.
                        throw new RuntimeException("No regions found.");
                    }
                MasonGeometry region = ((MasonGeometry)allRegions.objs[random.nextInt(allRegions.numObjs)]);
           
                // give each agent a random direction to initially move in
                Agent a = new Agent(random.nextInt(8));

                // set each agent in the center of corresponding region
                a.setLocation(region.getGeometry().getCentroid());

                // place the agents in the GeomVectorField
                agents.addGeometry(new MasonGeometry(a.getGeometry()));

                // add the new agent the schedule
                schedule.scheduleRepeating(a);
            }        
    }



    @Override
    public void start()
    {
        super.start();
 
        agents.clear(); // remove any agents from previous runs

        // add agents to the simulation
        addAgents();

        // ensure both GeomFields Color same area
        agents.setMBR(county.getMBR());
    }

    public static void main(String[] args)
    {
        doLoop(ColorWorld.class, args);
        System.exit(0);
    }
}
