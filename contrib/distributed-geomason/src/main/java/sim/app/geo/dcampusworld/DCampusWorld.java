package sim.app.geo.dcampusworld;

import java.net.URL;



import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.planargraph.Node;

import sim.app.geo.dcampusworld.data.DCampusWorldData;
import sim.engine.DObject;
import sim.engine.DSimState;
import sim.field.HaloGrid2D;
//import sim.field.Synchronizable;
import sim.field.continuous.DContinuous2D;
import sim.field.geo.DGeomVectorField;
import sim.field.geo.GeomVectorContinuousStorage;
import sim.field.geo.GeomVectorField;
import sim.field.storage.ContinuousStorage;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.IntRect2D;
import sim.util.geo.GeomPlanarGraph;
import sim.util.geo.MasonGeometry;

public class DCampusWorld extends DSimState
{
	private static final long serialVersionUID = 1;

	public static final int width = 300;
	public static final int height = 300;
	public static final int aoi = 1;// TODO what value???
	public static final int discretization = 6;
	public static final int numAgents = 1000; // 1000

	/** Convex hull of all static JTS objects **/
	public Envelope MBR;

	/** Distributed locations of each agent across all partitions **/
	//public final DContinuous2D<DAgent> agentLocations;// = new DContinuous2D<>(discretization, this);
    public final DGeomVectorField<DAgent> agentLocations;
	// TODO Remove these
	// NOT distributed. Load these remotely in a distributed way.
	/** Fields to hold the associated GIS information */
	public GeomVectorField walkways = new GeomVectorField(DCampusWorld.width, DCampusWorld.height);
	public GeomVectorField roads = new GeomVectorField(DCampusWorld.width, DCampusWorld.height);
	public GeomVectorField buildings = new GeomVectorField(DCampusWorld.width, DCampusWorld.height);

	// TODO Remove these
	// where all the agents live
//	public GeomVectorField agents = new GeomVectorField(DCampusWorld.width, DCampusWorld.height);

	// Stores the walkway network connections. We represent the walkways as a
	// PlanarGraph, which allows
	// easy selection of new waypoints for the agents.
	public GeomPlanarGraph network = new GeomPlanarGraph();
	public GeomVectorField junctions = new GeomVectorField(DCampusWorld.width, DCampusWorld.height); // nodes for intersections

	public DCampusWorld(final long seed)
	{
		super(seed, width, height, aoi, false);
		//agentLocations = new DContinuous2D<>(discretization, this);
		
		loadStatic(); //do it here so we can build our Envelope/MBR
		agentLocations = new DGeomVectorField<DAgent>(discretization, this, MBR);
//		balanceInterval = 100000;
	}

	public int getNumAgents()
		{
		return numAgents;
		}

	// TODO Remove this
	void loadStatic()
	{
		try
		{
			System.out.println("reading buildings layer");

			// this Bag lets us only display certain fields in the Inspector, the non-masked
			// fields
			// are not associated with the object at all
			final Bag masked = new Bag();
			masked.add("NAME");
			masked.add("FLOORS");
			masked.add("ADDR_NUM");

			// read in the buildings GIS file
			final URL bldgGeometry = DCampusWorldData.class.getResource("bldg.shp");
			final URL bldgDB = DCampusWorldData.class.getResource("bldg.dbf");
			ShapeFileImporter.read(bldgGeometry, bldgDB, buildings, masked);

			// We want to save the MBR so that we can ensure that all GeomFields
			// cover identical area.
			MBR = buildings.getMBR();

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
			
			//MBR.expandToInclude(agentLocations.getStorage().getGeomVectorField().getMBR());

			System.out.println("Done reading data");

			// Now synchronize the MBR for all GeomFields to ensure they cover the same area
			buildings.setMBR(MBR);
			roads.setMBR(MBR);
			walkways.setMBR(MBR);

			network.createFromGeomField(walkways);

			addIntersectionNodes(network.nodeIterator(), junctions);

		}
		catch (final Exception ex)
		{
			Logger.getLogger(DCampusWorld.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
//	@Override
//	protected void startRoot() {
//		ArrayList<DAgent> agents = new ArrayList<DAgent>();
//		for (int x = 0; x < DCampusWorld.numAgents; x++) {
//			DAgent agent = new DAgent(this);
//			agents.add(agent);
//		}
//		sendRootInfoToAll("agents", agents);
//	}
	
	@Override
	public void start()
	{
		super.start();

		// dump static info to each partition here at start of sim
		//loadStatic();// TODO Remove this
		


		
		
		System.out.println("buildings: "+buildings.getMBR());

		System.out.println("super before setting: "+agentLocations.getStorage().getGeomVectorField().getMBR());

		// add agents (when created, the agent adds itself to agentLocations)
		// here we are equally distributing agents per partition, we can use startRoot instead if we don't want to do this
		for (int i = 0; i < numAgents/this.getPartition().getNumProcessors(); i++) {
			new DAgent(this);
		}
		
		System.out.println("before setting: "+agentLocations.getStorage().getGeomVectorField().getMBR());
		
		
		agentLocations.getStorage().getGeomVectorField().getMBR().expandToInclude(buildings.getMBR());  //currently incorrect
		
		
		agentLocations.getStorage().globalEnvelope = agentLocations.getStorage().getGeomVectorField().getMBR(); //this is hacky, figure out better way
		
		
		System.out.println("after setting: "+agentLocations.getStorage().getGeomVectorField().getMBR());

	}

	/**
	 * adds nodes corresponding to road intersections to GeomVectorField
	 *
	 * @param nodeIterator  Points to first node
	 * @param intersections GeomVectorField containing intersection geometry
	 *
	 *                      Nodes will belong to a planar graph populated from LineString network.
	 */
	void addIntersectionNodes(final Iterator<Node> nodeIterator, final GeomVectorField intersections)
	{
		final GeometryFactory geometryFactory = new GeometryFactory();

		while (nodeIterator.hasNext())
		{
			final Node node = nodeIterator.next();
			junctions.addGeometry(new MasonGeometry(geometryFactory.createPoint(node.getCoordinate())));
		}
	}
	
	//for testing
	protected int countLocal(HaloGrid2D field) {
		
		HaloGrid2D haloGrid2D = (HaloGrid2D) field;
		int count = 0;

		// ContinousStorage, do we need its own case anymore? We may be able to combine with else code.
		if (haloGrid2D.getStorage() instanceof GeomVectorContinuousStorage)
		{

			GeomVectorContinuousStorage st = (GeomVectorContinuousStorage) haloGrid2D.getStorage();
			
			for (Object x: st.getGeomVectorField().getGeometries()) {
				//System.out.println(this.getPID()+": "+x);
			}
			
			// for cell

			
			for (int i = 0; i < st.storage.length; i++)
			{
				HashSet agents = new HashSet(((HashMap) st.storage[i]).values());

				for (Object a : agents) {
					Double2D loc = st.getObjectLocation((DObject) a);
					
					if (partition.getLocalBounds().contains(loc)) {
						count = count + 1;
					}
					
					
					

				}
			}
			
			return st.getGeomVectorField().getGeometries().size();

			
		}
		

		
		return count;
	}

	public static void main(final String[] args)
	{
		doLoopDistributed(DCampusWorld.class, args);
		System.exit(0);
	}
}
