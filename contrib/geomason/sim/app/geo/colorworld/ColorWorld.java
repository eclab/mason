package sim.app.geo.colorworld;

import com.vividsolutions.jts.geom.Geometry;
import java.io.*;
import sim.engine.*;
import sim.io.geo.*;
import sim.field.geo.*;
import sim.util.Bag;
import sim.util.geo.*;

public class ColorWorld extends SimState
{
    private static final long serialVersionUID = -2568637684893865458L;

	// number of agents in the simulation
    public static int NUM_AGENTS = 10;

    // where all the county geometry lives
    public GeomVectorField county = new GeomVectorField();

    // where all the agents live
    public static GeomVectorField agents = new GeomVectorField();


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
                Bag allRegions = county.getGeometries();

                if (allRegions.isEmpty())
                    {
                        // Something went wrong.  We *should* have regions.
                        throw new RuntimeException("No regions found.");
                    }
                Geometry region = ((MasonGeometry)allRegions.objs[random.nextInt(allRegions.numObjs)]).geometry;

                // give each agent a random direction to initially move in
                a = new Agent(random.nextInt(8));

                // set each agent in the center of corresponding region
                a.setLocation(region.getCentroid());

                // place the agents in the GeomVectorField
                agents.addGeometry(new MasonGeometry(a.getGeometry()));

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
                ShapeFileImporter importer = new ShapeFileImporter(); 
                importer.masonGeometryClass = CountingGeomWrapper.class; 
                
                importer.ingest( dataDirectory + "pol.shp", county, null);
            }
        catch (FileNotFoundException ex)
            {
                System.out.println("Error opening shapefile!" + ex);
                System.exit(-1);
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
