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
package sim.app.geo.dschellingspace;

import com.vividsolutions.jts.geom.*;

import mpi.MPIException;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Set;

import sim.app.geo.dcampusworld.DAgent;
import sim.app.geo.dcampusworld.DCampusWorld;
import sim.engine.SimState;
import sim.field.DNonUniformPartition;
import sim.field.DObjectMigratorNonUniform;
import sim.field.continuous.NContinuous2D;
import sim.field.geo.GeomNContinuous2D;
import sim.field.geo.GeomVectorField;
import sim.field.storage.ContStorage;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;
import sim.util.IntHyperRect;
import sim.util.Timing;
import sim.util.geo.MasonGeometry;



//@SuppressWarnings("restriction")
public class DSchellingSpace extends SimState
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
    IdentityHashMap<SchellingGeometry, Integer> polyIndiceMap = new IdentityHashMap<SchellingGeometry, Integer>();

    /**
     *
     */
    ArrayList<DPerson> people = new ArrayList<DPerson>();


    // used by PolySchellingWithUI to keep track of the percent of unhappy Persons
    public int totalReds = 0;
    public int totalBlues = 0;
    
    public double minDist = 100.0;
    
    double[] discretizations;
	public GeomNContinuous2D<DAgent> communicator;
	DNonUniformPartition partition;
	public IntHyperRect myPart;
	public DObjectMigratorNonUniform queue;


    /**
     *  constructor function
     */
    public DSchellingSpace(long seed)
    {
        super(seed);
    }


    void setupPersons()
    {    	
        GeometryFactory geometryFactory = new GeometryFactory();
        
        // process the polygons for neighbor and Person info
        for (int i = 0; i < polys.size(); i++)
        {           
            SchellingGeometry p1 = polys.get(i);     
            // add all of the Red People in this SchellingGeometry
            for (int k = 0; k < p1.initRed; k++)
            {

                // initialize the Person
                DPerson p = new DPerson(DPerson.Affiliation.RED, minDist);
                p.region = p1;
                p.location = randomPointInsidePolygon((Polygon) p1.geometry, geometryFactory);
                p.updatePosition();
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
                DPerson p = new DPerson(DPerson.Affiliation.BLUE, minDist);
                p.region = p1;
                p.location = randomPointInsidePolygon((Polygon) p1.geometry, geometryFactory);
                p.updatePosition();
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
    }
    
    
    void setupPolygons()
    {
    	// copy over the geometries into a list of Polygons
        Bag ps = world.getGeometries();
        polys.addAll(ps);

        System.out.println("Computing adjacencies and populating polygons");
        
        // process the polygons for neighbor and Person info
        for (int i = 0; i < polys.size(); i++)
        {
            if ( i % 10 == 0 ) { System.out.print("."); }

            SchellingGeometry p1 = polys.get(i);
            p1.init();
            // put item to the polygon to index map
            polyIndiceMap.put(p1, i);

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
            URL wardsFile = DSchellingSpace.class.getResource("data/DCreprojected.shp");
            ShapeFileImporter.read( wardsFile, world, SchellingGeometry.class);
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
        
        
        // see minDist in DPerson class
		int[] aoi = new int[] { (int) (minDist * 2), (int) (minDist * 2) };
		int width =  (int) Math.ceil(agents.getWidth());
        int height = (int) Math.ceil(agents.getHeight());
		int[] size = new int[] { width, height };
		discretizations = new double[] { 7, 7 };
		partition = DNonUniformPartition.getPartitionScheme(size, true, aoi);
		partition.initUniformly(null);
		partition.commit();
		NContinuous2D<DAgent> continuousField = new NContinuous2D<DAgent>(partition, aoi, discretizations);
		communicator = new GeomNContinuous2D<DAgent>(continuousField);
		queue = new DObjectMigratorNonUniform(partition);
		myPart = partition.getPartition();       
        
        // once the data is read in, set up the Polygons and Persons
		agents.clear();
		setupPolygons();
		try
		{
			// add all agents into agents field in processor 0
			ContStorage<DPerson> packgeField = new ContStorage<DPerson>(partition.getField(), discretizations);
			// setup polygons and persons in first node
			if (partition.getPid() == 0)
			{
				setupPersons();
				for (DPerson p:people)
				{					
					packgeField.setLocation(p, p.position);
				}
			}			
			
			decoupleRegions();
			// After distribute is called, communicator will have agents
			communicator.field.distribute(0, packgeField);

			// Then each processor access these agents, put them in 
			// agents field and schedule them
			Set<DPerson> receivedAgents = ((ContStorage)communicator.field.getStorage()).m.keySet();
			for (DPerson agent : receivedAgents)
			{
				// get region from region id
				// agent.region = 
				agents.addGeometry(agent.location);
				schedule.scheduleOnce(agent);
			}
		} catch (MPIException e)
		{
			e.printStackTrace();
		}
    }
    
    private void decoupleRegions()
    {
    	
    }



    /**
     * Called to run PolySchelling without the GUI
     * @param args
     * @throws MPIException 
     */
    public static void main(String[] args) throws MPIException
    {
    	Timing.setWindow(20);
		doLoopMPI(DSchellingSpace.class, args);
		System.exit(0);
    }

}