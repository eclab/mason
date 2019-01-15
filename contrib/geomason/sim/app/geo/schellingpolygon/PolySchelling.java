/**
 ** PolySchelling.java
 **
 ** Copyright 2011 by Sarah Wise, Mark Coletti, Andrew Crooks, and
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 ** $Id$
 **/
package sim.app.geo.schellingpolygon;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import sim.engine.SimState;
import sim.field.geo.GeomVectorField;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;



public class PolySchelling extends SimState
{

    private static final long serialVersionUID = 1L;
    // storing the data
    public GeomVectorField world = new GeomVectorField();
    ArrayList<Polygon> polys = new ArrayList<Polygon>();
    ArrayList<Person> people = new ArrayList<Person>();
    // used by PolySchellingWithUI to keep track of the percent of unhappy Persons
    int totalReds = 0;
    int totalBlues = 0;



    /**
     *  constructor function
     */
    public PolySchelling(long seed)
    {
        super(seed);
    }



    /**
     * Takes the geometries after they have been read in and constructs each Polygon's
     * list of neighbors. Also extracts information about the mobile agents from the
     * Polygons and sets up the list of Persons.
     */
    void setup()
    {

        // copy over the geometries into a list of Polygons
        Bag ps = world.getGeometries();
        polys.addAll(ps);

        // process the polygons for neighbor and Person info
        for (int i = 0; i < polys.size(); i++)
        {
            Polygon p1 = polys.get(i);
            p1.init();

            for (int j = i + 1; j < polys.size(); j++)
            {
                Polygon p2 = polys.get(j);
                if (p1.geometry.touches(p2.geometry))
                {
                    p1.neighbors.add(p2);
                    p2.neighbors.add(p1);
                }
            }

            if (p1.soc == null) // no agent is initialized in this location
            {
                continue;
            } else if (p1.soc.equals("RED"))
            { // a red Person is initialized here
                Person p = new Person("RED");
                p.updateLocation(p1);
                totalReds++;
                people.add(p);
            } else if (p1.soc.equals("BLUE"))
            { // a blue Person is initialized here
                Person p = new Person("BLUE");
                p.updateLocation(p1);
                totalBlues++;
                people.add(p);
            }
        }

        // schedule all of the Persons to update. One agent updates per tick,
        // so agents start updating in the order they appear in the list of Persons
        // and update every (number of Persons) ticks.
        int i = 0;
        for (Person p : people)
        {
            schedule.scheduleRepeating(i, p, people.size());
            i++;
        }

    }



    /** Import the data and then set up the simulation */
    public void start()
    {
        super.start();

        try // to import the data from the shapefile
        {
            URL wardsFile = PolySchelling.class.getResource("data/1991_wards_disolved_Project.shp");

            ShapeFileImporter.read(wardsFile, world, Polygon.class);

        } catch (Exception ex)
        {
            System.out.println("Error opening shapefile!" + ex);
            System.exit(-1);
        }

        // once the data is read in, set up the Polygons and Persons
        setup();
    }



    /**
     * Called to run PolySchelling without the GUI
     * @param args
     */
    public static void main(String[] args)
    {
        doLoop(PolySchelling.class, args);
        System.exit(0);
    }

}
