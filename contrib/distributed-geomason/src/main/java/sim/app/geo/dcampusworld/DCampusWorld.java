package sim.app.geo.dcampusworld;

import java.net.URL;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.planargraph.Node;

import sim.app.geo.dcampusworld.data.DCampusWorldData;
import sim.engine.DSimState;
import sim.field.continuous.DContinuous2D;
import sim.field.geo.GeomVectorField;
import sim.io.geo.ShapeFileExporter;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;
import sim.util.geo.GeomPlanarGraph;
import sim.util.geo.MasonGeometry;

public class DCampusWorld extends DSimState {
    private static final long serialVersionUID = 1;

    public static final int width = 300;
    public static final int height = 300;
	public final static int aoi = 6;//TODO what value???

    /** How many agents in the simulation */
	public int numAgents = 1000;

    public int discretization;
    /** Distributed locations of each agent across all partitions **/
    public DContinuous2D<DAgent> agentLocations;
    // serializable ^
	
    // TODO NOT distributed
    /** Fields to hold the associated GIS information */
    public GeomVectorField walkways = new GeomVectorField(DCampusWorld.width, DCampusWorld.height);
    public GeomVectorField roads = new GeomVectorField(DCampusWorld.width, DCampusWorld.height);
    public GeomVectorField buildings = new GeomVectorField(DCampusWorld.width,DCampusWorld.height);

    // where all the agents live
    public GeomVectorField agents = new GeomVectorField(DCampusWorld.width, DCampusWorld.height);
    
    // Stores the walkway network connections.  We represent the walkways as a PlanarGraph, which allows
    // easy selection of new waypoints for the agents.
    public GeomPlanarGraph network = new GeomPlanarGraph();
    public GeomVectorField junctions = new GeomVectorField(DCampusWorld.width, DCampusWorld.height); // nodes for intersections



    public DCampusWorld(final long seed)
    {
        super(seed, width, height, aoi);
    }


    public int getNumAgents() { return numAgents; }
    public void setNumAgents(final int n) { if (n > 0) numAgents = n; }

    /**
     * Add agents to the simulation and to the agent GeomVectorField. Note that
     * each agent does not have any attributes.
     */
    void addAgents()
    {
        for (int i = 0; i < numAgents; i++)
        {
            final DAgent a = new DAgent(this);

            agents.addGeometry(a.getGeoLocation());

            schedule.scheduleRepeating(a);

            // we can set the userData field of any MasonGeometry.  If the userData is inspectable,
            // then the inspector will show this information
            //if (i == 10)
            //	buildings.getGeometry("CODE", "JC").setUserData(a);
        }
    }

    @Override
    public void finish()
    {
        super.finish();

        // Save the agents layer, which has no corresponding originating
        // shape file.
        ShapeFileExporter.write("agents", agents);
    }

    
//    protected void startRoot() {        
//    	sendRootInfoToAll("agents", agentLocations);
//    }

    void loadStatic() {
        try
        {
            System.out.println("reading buildings layer");

            // this Bag lets us only display certain fields in the Inspector, the non-masked fields
            // are not associated with the object at all
            final Bag masked = new Bag();
            masked.add("NAME");
            masked.add("FLOORS");
            masked.add("ADDR_NUM");

//                System.out.println(System.getProperty("user.dir"));

            // read in the buildings GIS file

            final URL bldgGeometry = DCampusWorldData.class.getResource("bldg.shp");
            final URL bldgDB = DCampusWorldData.class.getResource("bldg.dbf");
            ShapeFileImporter.read(bldgGeometry, bldgDB, buildings, masked);

            // We want to save the MBR so that we can ensure that all GeomFields
            // cover identical area.
            final Envelope MBR = buildings.getMBR();

            System.out.println("reading roads layer");

            final URL roadGeometry = DCampusWorldData.class.getResource("roads.shp");
            final URL roadDB = DCampusWorldData.class.getResource("roads.dbf");
            ShapeFileImporter.read(roadGeometry, roadDB, roads);

            MBR.expandToInclude(roads.getMBR());

            System.out.println("reading walkways layer");

            final URL walkWayGeometry = DCampusWorldData.class.getResource("walk_ways.shp");
            final URL walkWayDB = DCampusWorldData.class.getResource("walk_ways.dbf");
            ShapeFileImporter.read(walkWayGeometry, walkWayDB, walkways);

            MBR.expandToInclude(walkways.getMBR());

            System.out.println("Done reading data");

            // Now synchronize the MBR for all GeomFields to ensure they cover the same area
            buildings.setMBR(MBR);
            roads.setMBR(MBR);
            walkways.setMBR(MBR);

            network.createFromGeomField(walkways);

            addIntersectionNodes(network.nodeIterator(), junctions);
            
            
            // Distributed locations
            //TODO how many subdivisions?
            discretization = 6;
//            agentLocations = new DContinuous2D<DMasonPoint>(getPartitioning(), aoi[0], discretization, this);
            agentLocations = new DContinuous2D<>(discretization, this);
        } catch (final Exception ex)
        {
            Logger.getLogger(DCampusWorld.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void start()
    {
        super.start();
        
        // dump static info to each partition here at start of sim instead of in constructor
        loadStatic();
        
        //TODO???
//        // add agents
//        agents.clear(); // clear any existing agents from previous runs
//        addAgents();
//        agents.setMBR(buildings.getMBR());
 
//        for (Object geom : agents.getGeometries()) {
//        	MasonGeometry masonGeom = (MasonGeometry) geom;
////        	Point centroid = agents.getGeometryLocation(masonGeom);
//        	Point centroid = masonGeom.geometry.getCentroid();
//        	Coordinate[] coords = centroid.getCoordinates();
//        	assert(coords.length == 1); // only 1 point
//            Double2D location = new Double2D(coords[0].x, coords[0].y);
//            
//            // Get all local agents, move them and schedule them
//            if (partition.getBounds().contains(location)) {
//            	//TODO agentLocations.addLocal(location, masonGeom);
//            }
//
//            // TODO unhook AGENTS from old schedule and reschedule to new processor
//        }
        
        
        for (int i = 0; i < numAgents; i++)
        {
            final DAgent a = new DAgent(this);
            System.out.println(a);
            //TODO? agents.addGeometry(g);
            agentLocations.addAgent(a.loc, a, 0, 1);
        }
        
        //TODO distributed hashmap? Carmine et al
        
        // Ensure that the spatial index is made aware of the new agent
        // positions.  Scheduled to guaranteed to run after all agents moved.
        schedule.scheduleRepeating( agents.scheduleSpatialIndexUpdater(), Integer.MAX_VALUE, 1.0);

//      for(int x=0;x<size;x++)
//          {
//          Double2D location = new Double2D(_,_);
//          DAgent agent = new DAgent(location);
//          agent.setObjectLocation(agent, location);
//          schedule.scheduleRepeating(agent);
//          }
    }



    /** adds nodes corresponding to road intersections to GeomVectorField
     *
     * @param nodeIterator Points to first node
     * @param intersections GeomVectorField containing intersection geometry
     *
     * Nodes will belong to a planar graph populated from LineString network.
     */
    private void addIntersectionNodes(final Iterator nodeIterator, final GeomVectorField intersections)
    {
        final GeometryFactory fact = new GeometryFactory();
        Coordinate coord = null;
        Point point = null;
        int counter = 0;

        while (nodeIterator.hasNext())
            {
                final Node node = (Node) nodeIterator.next();
                coord = node.getCoordinate();
                point = fact.createPoint(coord);

                junctions.addGeometry(new MasonGeometry(point));
                counter++;
            }
    }

    public static void main(final String[] args) {
    	doLoopDistributed(DCampusWorld.class, args);
        System.exit(0);
    }
}
