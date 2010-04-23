/*
 * ColorWorld.java
 *
 * $Id: ColorWorld.java,v 1.3 2010-04-23 21:39:12 mcoletti Exp $
 */
package sim.app.geo.colorworld;

import com.vividsolutions.jts.geom.Polygon;
import java.io.*;
import sim.engine.*;
import sim.io.geo.*;
import sim.field.geo.*;
import sim.util.Bag;
import sim.util.geo.GeomWrapper;

/** Set up a GeoField with a number of points and a corresponding portrayal.
 *
 * @author mcoletti
 */
public class ColorWorld extends SimState
{
    // number of agents in the simulation
    public static int NUM_AGENTS = 10;

    // where all the county geometry lives
    public GeomField county = new GeomField();

    // where all the agents live
    public GeomField agents = new GeomField();


    private static final String dataDirectory = "sim/app/data/";


    // getters and setters for inspectors
    public int getNumAgents() { return NUM_AGENTS; }
    public void setNumAgents(int a) { if (a > 0) NUM_AGENTS = a; }

    public ColorWorld(long seed)
    {
        super(seed);
    }

    private void addAgents()
    {
        Agent a = null;

        for (int i = 0; i < NUM_AGENTS; i++)
            {
                // pick a random political region to plop the agent in
                Bag allRegions = county.getGeometry();

                if (allRegions.isEmpty())
                    {
                        // Something went wrong.  We *should* have regions.
                        throw new RuntimeException("No regions found.");
                    }
                Polygon region = (Polygon) ((GeomWrapper)allRegions.objs[random.nextInt(allRegions.numObjs)]).geometry;

                // give each agent a random direction to initially move in
                a = new Agent(random.nextInt(8), region);

                // set each agent in the center of corresponding region
                a.setLocation(region.getCentroid());

                // place the agents in the GeomField
                agents.addGeometry(new GeomWrapper(a.getGeometry()));

                // add the new agent the schedule
                schedule.scheduleRepeating(a);
            }
    }



    public void start()
    {
        super.start();
        try
            {
                // Open simple Shape file of county.

                // Optional, alternative importer implementations:
                //    OGRImporter importer = new OGRImporter(); // alternative importer
                //    GeoToolsImporter importer = new GeoToolsImporter(); // another alternative importer
                ShapeFileImporter importer =
                    new ShapeFileImporter(CountingGeomWrapper.class); // native MASON Shape file importer

                importer.ingest( dataDirectory + "pol.shp", county, null);
            }
        catch (FileNotFoundException ex)
            {
                System.out.println("Error opening shapefile!" + ex);
                System.exit(-1);
            }

        // We need to link the GeomField "agents" to all the
        // GeomWrappers so that they can update their counts of
        // occupying agents.

        Bag geometry = county.getGeometry();

        for (int i = 0; i < geometry.size(); i++)
            {
                ((CountingGeomWrapper)geometry.objs[i]).agents = this.agents;
            }

        county.computeConvexHull();
        county.computeUnion();

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
