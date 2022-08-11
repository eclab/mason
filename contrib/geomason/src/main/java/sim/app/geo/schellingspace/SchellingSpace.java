/**
 ** SchellingSpace.java
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
package sim.app.geo.schellingspace;
import sim.app.geo.schellingspace.data.SchellingSpaceData;

import com.vividsolutions.jts.geom.*;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import sim.engine.SimState;
import sim.field.geo.GeomVectorField;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;



//@SuppressWarnings("restriction")
public class SchellingSpace extends SimState
    {
    private static final long serialVersionUID = 1L;

    /** Contains polygons defining DC ward boundaries
     */
    public GeomVectorField world = new GeomVectorField();

    /** The agents moving through DC wards
     *
     */
    public GeomVectorField agents = new GeomVectorField();

    /**
     *
     */
    ArrayList<SchellingGeometry> polys = new ArrayList<SchellingGeometry>();

    /**
     *
     */
    ArrayList<Person> people = new ArrayList<Person>();


    // used by PolySchellingWithUI to keep track of the percent of unhappy Persons
    public int totalReds = 0;
    public int totalBlues = 0;


    /**
     *  constructor function
     */
    public SchellingSpace(long seed)
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
        GeometryFactory geometryFactory = new GeometryFactory();

        System.out.println("Computing adjacencies and populating polygons");
        
        // process the polygons for neighbor and Person info
        for (int i = 0; i < polys.size(); i++)
            {
            if ( i % 10 == 0 ) { System.out.print("."); }

            SchellingGeometry p1 = polys.get(i);
            p1.init();

            // add all neighbors
            for (int j = i + 1; j < polys.size(); j++)
                {
                SchellingGeometry p2 = polys.get(j);
                if (p1.geometry.touches(p2.geometry))
                    {
                    p1.neighbors.add(p2);
                    p2.neighbors.add(p1);
                    }
                }

            // add all of the Red People in this SchellingGeometry
            for (int k = 0; k < p1.initRed; k++)
                {

                // initialize the Person
                Person p = new Person(Person.Affiliation.RED);
                p.region = p1;
                p.location = randomPointInsidePolygon((Polygon) p1.geometry, geometryFactory);
                p.location.isMovable = true;
                p.location.setUserData(p);

                // place the Person in the GeomVectorField

                // store information
                agents.addGeometry(p.location);
                people.add(p);
                p1.residents.add(p);
                }

            // add all of the blue People in this SchellingGeometry
            for (int k = 0; k < p1.initBlue; k++)
                {

                // initialize the Person
                Person p = new Person(Person.Affiliation.BLUE);
                p.region = p1;
                p.location = randomPointInsidePolygon((Polygon) p1.geometry, geometryFactory);
                p.location.isMovable = true;
                p.location.setUserData(p);
                // place the Person in the GeomVectorField

                // store information
                agents.addGeometry(p.location);
                people.add(p);
                p1.residents.add(p);
                }
            // update the total population counts
            totalReds += p1.initRed;
            totalBlues += p1.initBlue;

            }

        // schedule all of the Persons to update every tick. By default, they are called
        // in random order

        System.out.println("\nScheduling agents");

        int i = 0;
        for (Person p : people)
            {
            schedule.scheduleRepeating(p);
            i++;
            }

        }



    /**
     *  returns a Point inside the polygon
     * @param p the Polygon within which the point should lie
     * @param gfact the GeometryFactory that will create new points
     * @return
     */
    MasonGeometry randomPointInsidePolygon(Polygon p, GeometryFactory gfact)
        {

        if (p == null)
            {
            return null;
            } // nothing here
        if (p.isEmpty())
            {
            return null;
            } // can never find anything inside this empty geometry!

        Envelope e = p.getEnvelopeInternal();

        // calcuate where the point can be
        double xmin = e.getMinX(), ymin = e.getMinY(),
            xmax = e.getMaxX(), ymax = e.getMaxY();
        double addX = random.nextDouble() * (xmax - xmin) + xmin; // the proposed x value
        double addY = random.nextDouble() * (ymax - ymin) + ymin; // the proposed y value
        Point pnt = gfact.createPoint(new Coordinate(addX, addY));

        // continue searching until the point found is within the polygon
        while (!p.covers(pnt))
            {//p.contains(pnt) ){
            addX = random.nextDouble() * (xmax - xmin) + xmin; // the proposed x value
            addY = random.nextDouble() * (ymax - ymin) + ymin; // the proposed y value
            pnt = gfact.createPoint(new Coordinate(addX, addY));
            }

        // return the found point
        return new MasonGeometry(pnt);
        }



    /** Import the data and then set up the simulation */
    @Override
    public void start()
        {
        super.start();

        try // to import the data from the shapefile
            {
            System.out.print("Reading boundary data ... ");
            
            URL wardsFile = SchellingSpaceData.class.getResource("DCreprojected.shp");
            URL wardsDB = SchellingSpaceData.class.getResource("DCreprojected.dbf");

            ShapeFileImporter.read( wardsFile, wardsDB, world, SchellingGeometry.class);
            }
        catch (Exception ex)
            {
            System.out.println("Error opening shapefile!" + ex);
            System.exit(-1);
            }

        // Sync MBRs
        agents.setMBR(world.getMBR());

        System.out.println("done");

        System.out.print("Computing convex hull ... ");
        world.computeConvexHull();

        System.out.print("done.\nComputing union ... ");
        world.computeUnion();

        System.out.println("done");

        // once the data is read in, set up the Polygons and Persons
        setup();
        }



    /**
     * Called to run PolySchelling without the GUI
     * @param args
     */
    public static void main(String[] args)
        {
        doLoop(SchellingSpace.class, args);
        System.exit(0);
        }

    }
