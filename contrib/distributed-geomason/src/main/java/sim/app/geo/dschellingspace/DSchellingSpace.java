package sim.app.geo.dschellingspace;

import java.net.URL;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import sim.app.geo.dturkana.DTurkanaSouthModel;
import sim.app.geo.schellingspace.SchellingGeometry;
import sim.app.geo.dschellingspace.data.DSchellingSpaceData;
import sim.engine.DSimState;
import sim.field.geo.DGeomVectorField;
import sim.field.geo.GeomVectorField;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.geo.MasonGeometry;

public class DSchellingSpace extends DSimState{
	
	  private static final long serialVersionUID = 1L;
	  
		public static final int width = 100;
		public static final int height = 100;
		public static final int aoi = 1;// TODO what value???

	    /** Contains polygons defining DC ward boundaries
	     */
	    public DGeomVectorField world;

	    /** The agents moving through DC wards
	     *
	     */
	    public DGeomVectorField agents; ;

	    /**
	     *
	     */
	    ArrayList<DSchellingGeometry> polys = new ArrayList<DSchellingGeometry>();

	    /**
	     *
	     */
	    ArrayList<DPerson> people = new ArrayList<DPerson>();


	    // used by PolySchellingWithUI to keep track of the percent of unhappy Persons
	    public int totalReds = 0;
	    public int totalBlues = 0;


	    /**
	     *  constructor function
	     */
	    public DSchellingSpace(long seed)
	    {
	        //super(seed);
			super(seed, width, height, aoi, false);
			
			GeomVectorField tempGeomVectorField = new GeomVectorField();
			
	        try // to import the data from the shapefile
	        {
	            System.out.print("Reading boundary data ... ");
	            
	            URL wardsFile = DSchellingSpaceData.class.getResource("DCreprojected.shp");
	            URL wardsDB = DSchellingSpaceData.class.getResource("DCreprojected.dbf");
	            
	            
                //I may need to figure out a better way to do this, does my distributed geomvecfields work here?
	            //ShapeFileImporter.read( wardsFile, wardsDB, world.getStorage().getGeomVectorField(), DSchellingGeometry.class);
	            ShapeFileImporter.read( wardsFile, wardsDB, tempGeomVectorField, DSchellingGeometry.class);

	        }
	        catch (Exception ex)
	        {
	            System.out.println("Error opening shapefile!" + ex);
	            System.exit(-1);
	        }
	        
	        world = new DGeomVectorField(1, this, tempGeomVectorField.getMBR()); 
	        agents = new DGeomVectorField(1, this, tempGeomVectorField.getMBR());
	        
	        world.getStorage().setGeomVectorField(tempGeomVectorField);

	    }



	    /**
	     * Takes the geometries after they have been read in and constructs each Polygon's
	     * list of neighbors. Also extracts information about the mobile agents from the
	     * Polygons and sets up the list of Persons.
	     */
	    void setup()
	    {

	        // copy over the geometries into a list of Polygons
	        Bag ps = world.getStorage().getGeomVectorField().getGeometries();
	        polys.addAll(ps);
	        GeometryFactory geometryFactory = new GeometryFactory();

	        System.out.println("Computing adjacencies and populating polygons");
	        
	        // process the polygons for neighbor and Person info
	        for (int i = 0; i < polys.size(); i++)
	        {
	            if ( i % 10 == 0 ) { System.out.print("."); }

	            DSchellingGeometry p1 = polys.get(i);
	            p1.init();

	            // add all neighbors
	            for (int j = i + 1; j < polys.size(); j++)
	            {
	                DSchellingGeometry p2 = polys.get(j);
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
	                DPerson p = new DPerson(DPerson.Affiliation.RED);
	                p.region = p1;
	                MasonGeometry chosenpoint = randomPointInsidePolygon((Polygon) p1.geometry, geometryFactory);
	                p.setGeometry(chosenpoint);
	                p.getMasonGeometry().isMovable = true;
	                p.getMasonGeometry().setUserData(p);

	                // place the Person in the GeomVectorField

	                // store information
	                //agents.addGeometry(p.location);
	                Double2D d = new Double2D((((Point)p.getMasonGeometry().geometry).getCoordinate()).x, (((Point)p.getMasonGeometry().geometry).getCoordinate()).y);
	                agents.addAgent(d, p, 0, 0, 1);
	                
	                
	                people.add(p);
	                p1.residents.add(p);
	            }

	            // add all of the blue People in this SchellingGeometry
	            for (int k = 0; k < p1.initBlue; k++)
	            {

	                // initialize the Person
	                DPerson p = new DPerson(DPerson.Affiliation.BLUE);
	                p.region = p1;
	                p.setGeometry(randomPointInsidePolygon((Polygon) p1.geometry, geometryFactory));
	                p.getMasonGeometry().isMovable = true;
	                p.getMasonGeometry().setUserData(p);
	                // place the Person in the GeomVectorField

	                // store information
	                //agents.addGeometry(p.location);
	                Double2D d = new Double2D((((Point)p.getMasonGeometry().geometry).getCoordinate()).x, (((Point)p.getMasonGeometry().geometry).getCoordinate()).y);
	                agents.addAgent(d, p, 0, 0, 1);
	                people.add(p);
	                p1.residents.add(p);
	            }
	            // update the total population counts
	            totalReds += p1.initRed;
	            totalBlues += p1.initBlue;

	        }

	        // schedule all of the Persons to update every tick. By default, they are called
	        // in random order
	        
	        //addAgent() handles this!
            /*
	        System.out.println("\nScheduling agents");

	        int i = 0;
	        for (DPerson p : people)
	        {
	            schedule.scheduleRepeating(p);
	            i++;
	        }
	        */

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



	        // Sync MBRs
	        agents.getStorage().getGeomVectorField().setMBR(world.getStorage().getGeomVectorField().getMBR());

	        System.out.println("done");

	        System.out.print("Computing convex hull ... ");
	        world.getStorage().getGeomVectorField().computeConvexHull();

	        System.out.print("done.\nComputing union ... ");
	        world.getStorage().getGeomVectorField().computeUnion();

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
			doLoopDistributed(DSchellingSpace.class, args);
			System.exit(0);
	    }

}
