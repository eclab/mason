/* 
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 *
 * See the file "LICENSE" for more information
 *
 * $Id: CampusWorld.java 848 2013-01-08 22:56:43Z mcoletti $
*/
package sim.app.geo.dcampusworld;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.planargraph.Node;

import mpi.MPIException;

import java.net.URL;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import sim.engine.Schedule;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.DNonUniformPartition;
import sim.field.DObjectMigratorNonUniform;
import sim.field.continuous.NContinuous2D;
import sim.field.geo.GeomNContinuous2D;
import sim.field.geo.GeomVectorField;
import sim.field.storage.ContStorage;
import sim.field.storage.TestObj;
import sim.io.geo.ShapeFileExporter;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;
import sim.util.IntHyperRect;
import sim.util.Timing;
import sim.util.geo.GeomPlanarGraph;
import sim.util.geo.MasonGeometry;

/**
 * This simple example shows how to setup GeomVectorFields and run agents around
 * the fields. The simulation has multiple agents following the walkways at
 * George Mason University. GIS information about the walkways, buildings, and
 * roads provides the environment for the agents. During the simulation, the
 * agents wander randomly on the walkways.
 */
public class DCampusWorld extends SimState
{
	private static final long serialVersionUID = 1L;

	public static final int WIDTH = 300;
	public static final int HEIGHT = 300;

	/** How many agents in the simulation */
	public int numAgents = 4000;

	/** Fields to hold the associated GIS information */
	public GeomVectorField walkways = new GeomVectorField(WIDTH, HEIGHT);
	public GeomVectorField roads = new GeomVectorField(WIDTH, HEIGHT);
	public GeomVectorField buildings = new GeomVectorField(WIDTH, HEIGHT);

	// where all the agents live
	public GeomVectorField agents = new GeomVectorField(WIDTH, HEIGHT);

	double[] discretizations;
	public GeomNContinuous2D<DAgent> communicator;
	DNonUniformPartition partition;
	public IntHyperRect myPart;
	public DObjectMigratorNonUniform queue;

	// Stores the walkway network connections. We represent the walkways as a
	// PlanarGraph, which allows
	// easy selection of new waypoints for the agents.
	public GeomPlanarGraph network = new GeomPlanarGraph();
	public GeomVectorField junctions = new GeomVectorField(WIDTH, HEIGHT); // nodes
																			 // for
																			 // intersections

	public DCampusWorld(long seed)
	{
		super(seed);

		try
		{
			System.out.println("reading buildings layer");

			// this Bag lets us only display certain fields in the Inspector,
			// the non-masked fields
			// are not associated with the object at all
			Bag masked = new Bag();
			masked.add("NAME");
			masked.add("FLOORS");
			masked.add("ADDR_NUM");

			// read in the buildings GIS file
			URL bldgGeometry = DCampusWorld.class.getResource("data/bldg.shp");
			ShapeFileImporter.read(bldgGeometry, buildings, masked);

			// We want to save the MBR so that we can ensure that all GeomFields
			// cover identical area.
			Envelope MBR = buildings.getMBR();

			System.out.println("reading roads layer");

			URL roadGeometry = DCampusWorld.class.getResource("data/roads.shp");
			ShapeFileImporter.read(roadGeometry, roads);

			MBR.expandToInclude(roads.getMBR());

			System.out.println("reading walkways layer");

			URL walkWayGeometry = DCampusWorld.class.getResource("data/walk_ways.shp");
			ShapeFileImporter.read(walkWayGeometry, walkways);

			MBR.expandToInclude(walkways.getMBR());

			System.out.println("Done reading data");

			// Each agent move independently, seems neighborhood is not really
			// playing any role here
			int[] aoi = new int[] { 10, 10 };
			int[] size = new int[] { (int) WIDTH, (int) HEIGHT };
			discretizations = new double[] { 7, 7 };
			partition = DNonUniformPartition.getPartitionScheme(size, true, aoi);
			partition.initUniformly(null);
			partition.commit();
			NContinuous2D<DAgent> continuousField = new NContinuous2D<DAgent>(partition, aoi, discretizations);
			communicator = new GeomNContinuous2D<DAgent>(continuousField);
			queue = new DObjectMigratorNonUniform(partition);
			myPart = partition.getPartition();

			// Now synchronize the MBR for all GeomFields to ensure they cover
			// the same area
			buildings.setMBR(MBR);
			roads.setMBR(MBR);
			walkways.setMBR(MBR);
			communicator.setMBR(MBR);
			//System.out.print(
			//		"width and height is:" + communicator.getPixelWidth() + ", " + communicator.getPixelHeight());

			network.createFromGeomField(walkways);

			addIntersectionNodes(network.nodeIterator(), junctions);

		} catch (Exception ex)
		{
			Logger.getLogger(DCampusWorld.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public int getNumAgents()
	{
		return numAgents;
	}

	public void setNumAgents(int n)
	{
		if (n > 0)
			numAgents = n;
	}

	@Override
	public void finish()
	{
		super.finish();

		// Save the agents layer, which has no corresponding originating
		// shape file.
		ShapeFileExporter.write("agents", agents);
	}

	public void start()
	{
		super.start();
		agents.clear();
		try
		{
			// add all agents into agents field in processor 0
			ContStorage<DAgent> packgeField = new ContStorage<DAgent>(partition.getField(), discretizations);
			if (partition.getPid() == 0)
			{
				for (int i = 0; i < numAgents; ++i)
				{
					DAgent agent = new DAgent(this);
					// Do not add to agents field, we will add that later
					// after distribution
					packgeField.setLocation(agent, agent.position);
				}
			}
			// After distribute is called, communicator will have agents
			communicator.field.distribute(0, packgeField);

			// Then each processor access these agents, put them in 
			// agents field and schedule them
			Set<DAgent> receivedAgents = ((ContStorage)communicator.field.getStorage()).m.keySet();
			for (DAgent agent : receivedAgents)
			{
				agents.addGeometry(agent.getGeometry());
				schedule.scheduleOnce(agent);
			}
		} catch (MPIException e)
		{
			e.printStackTrace();
		}

		agents.setMBR(buildings.getMBR());
		schedule.scheduleRepeating(Schedule.EPOCH, 2, new Synchronizer(), 1);

		// Ensure that the spatial index is made aware of the new agent
		// positions. Scheduled to guaranteed to run after all agents moved.
		schedule.scheduleRepeating(agents.scheduleSpatialIndexUpdater(), Integer.MAX_VALUE, 1.0);
	}


	/**
	 * adds nodes corresponding to road intersections to GeomVectorField
	 *
	 * @param nodeIterator
	 *            Points to first node
	 * @param intersections
	 *            GeomVectorField containing intersection geometry
	 *
	 *            Nodes will belong to a planar graph populated from LineString
	 *            network.
	 */
	private void addIntersectionNodes(Iterator nodeIterator, GeomVectorField intersections)
	{
		GeometryFactory fact = new GeometryFactory();
		Coordinate coord = null;
		Point point = null;
		int counter = 0;

		while (nodeIterator.hasNext())
		{
			Node node = (Node) nodeIterator.next();
			coord = node.getCoordinate();
			point = fact.createPoint(coord);
			junctions.addGeometry(new MasonGeometry(point));
			counter++;
		}
	}

	public static void main(String[] args) throws MPIException
	{
		Timing.setWindow(20);
		doLoopMPI(DCampusWorld.class, args);
		System.exit(0);
	}

	private class Synchronizer implements Steppable
	{
		private static final long serialVersionUID = 1;

		public void step(SimState state)
		{
			DCampusWorld world = (DCampusWorld) state;
			Timing.stop(Timing.LB_RUNTIME);
			// Timing.start(Timing.MPI_SYNC_OVERHEAD);
			try
			{
				world.communicator.sync();
				world.queue.sync();
				// String s = String.format("PID %d Steps %d Number of Agents
				// %d\n", partition.pid, schedule.getSteps(), flockers.size() -
				// flockers.ghosts.size());
				// System.out.print(s);
			} catch (Exception e)
			{
				e.printStackTrace();
				System.exit(-1);
			}
			// TODO
			// Need to put the agent in communicator (halo region) into
			// agents field, but so far we do not have code to easily access
			// all the agents in that region, and since this does not effect
			// the logic of simulation so we do this in future

			// Retrieve the migrated agents from queue and schedule them
			for (Object obj : world.queue)
			{
				DAgent agent = (DAgent) obj;
				world.communicator.setLocation(agent, agent.position);
				world.agents.addGeometry(agent.getGeometry());
				schedule.scheduleOnce(agent, 1);
			}
			// Clear the queue
			world.queue.clear();
			// Timing.stop(Timing.MPI_SYNC_OVERHEAD);
		}
	}
}
