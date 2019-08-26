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

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.planargraph.Node;

import mpi.MPIException;
import sim.app.geo.dcampusworld.data.DCampusWorldData;
import sim.engine.DSimState;
import sim.engine.Schedule;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.HaloField;
import sim.field.continuous.NContinuous2D;
import sim.field.geo.GeomNContinuous2D;
import sim.field.geo.GeomVectorField;
import sim.io.geo.ShapeFileExporter;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;
import sim.util.DoublePoint;
import sim.util.MPIUtil;
import sim.util.NdPoint;
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
public class DCampusWorld extends DSimState {
	private static final long serialVersionUID = 1L;

	public static final int WIDTH = 300;
	public static final int HEIGHT = 300;
	public static final int AOISIZE = 10;

	final Path pathToAgentOutput = Paths.get("DCampusAgents.txt");

	/** How many agents in the simulation */
	public int numAgents = 8;

	/** Fields to hold the associated GIS information */
	public GeomVectorField walkways = new GeomVectorField(DCampusWorld.WIDTH, DCampusWorld.HEIGHT);
	public GeomVectorField roads = new GeomVectorField(DCampusWorld.WIDTH, DCampusWorld.HEIGHT);
	public GeomVectorField buildings = new GeomVectorField(DCampusWorld.WIDTH, DCampusWorld.HEIGHT);

	// where all the agents live
	public GeomVectorField agents = new GeomVectorField(DCampusWorld.WIDTH, DCampusWorld.HEIGHT);

	double[] discretizations;
	public GeomNContinuous2D<DAgent> communicator;

	// Stores the walkway network connections. We represent the walkways as a
	// PlanarGraph, which allows
	// easy selection of new waypoints for the agents.
	public GeomPlanarGraph network = new GeomPlanarGraph();
	public GeomVectorField junctions = new GeomVectorField(DCampusWorld.WIDTH, DCampusWorld.HEIGHT); // nodes
	// for
	// intersections

	public DCampusWorld(final long seed) {
		super(seed, DCampusWorld.WIDTH, DCampusWorld.HEIGHT, DCampusWorld.AOISIZE);

		try {
			System.out.println("reading buildings layer");

			// this Bag lets us only display certain fields in the Inspector,
			// the non-masked fields
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

			// Each agent move independently, seems neighborhood is not really
			// playing any role here
			final int[] aoi = new int[] { DCampusWorld.AOISIZE, DCampusWorld.AOISIZE };
			discretizations = new double[] { 7, 7 };
			final NContinuous2D<DAgent> continuousField = new NContinuous2D<DAgent>(partition,
					aoi, discretizations, this);
			communicator = new GeomNContinuous2D<DAgent>(continuousField);

			// final int[] size = new int[] { DCampusWorld.WIDTH, DCampusWorld.HEIGHT };
			// partition = DQuadTreeParti÷tion.getPartitionScheme(size, true, aoi);
			// partition.initialize();
			// partition.commit();

			// Now synchronize the MBR for all GeomFields to ensure they cover the same area
			buildings.setMBR(MBR);
			roads.setMBR(MBR);
			walkways.setMBR(MBR);
			communicator.setMBR(MBR);
			// System.out.print("width and height is:" + communicator.getPixelWidth() + ", "
			// + communicator.getPixelHeight());

			network.createFromGeomField(walkways);

			addIntersectionNodes(network.nodeIterator(), junctions);

		} catch (final Exception ex) {
			Logger.getLogger(DCampusWorld.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public int getNumAgents() {
		return numAgents;
	}

	public void setNumAgents(final int n) {
		if (n > 0)
			numAgents = n;
	}

	@Override
	public void finish() {
		super.finish();

		// Save the agents layer, which has no corresponding originating shape file
		ShapeFileExporter.write("agents", agents);
	}

	public void start() {
		super.start();
		agents.clear();

		for (int i = 0; i < numAgents / 4; ++i) {
			try {
				final DAgent agent = new DAgent(this);
				do {
					// updating position of the agent
					final int walkway = random.nextInt(walkways.getGeometries().numObjs);
					final MasonGeometry mg = (MasonGeometry) walkways.getGeometries().objs[walkway];
					agent.setNewRoute((LineString) mg.getGeometry(), true);
					final double x = communicator.toXCoord(agent.position.c[0]);
					final double y = communicator.toYCoord(agent.position.c[1]);
					agent.position = new DoublePoint(x, y);
				} while (!communicator.field.inLocal(agent.position));

				// This will effectively move the agent and also schedule it
				communicator.field.addAgent(agent.position, agent);
				agents.addGeometry(agent.getGeometry());

			} catch (final MPIException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}

		agents.setMBR(buildings.getMBR());

		schedule.scheduleRepeating(Schedule.EPOCH, 2, new Printer(), 10);

//		schedule.scheduleRepeating(Schedule.EPOCH, 2, new Synchronizer(), 1);

		// Ensure that the spatial index is made aware of the new agent
		// positions. Scheduled to guaranteed to run after all agents moved.
		schedule.scheduleRepeating(agents.scheduleSpatialIndexUpdater(), Integer.MAX_VALUE, 1.0);

		try {
//			Files.write(pathToAgentOutput, Arrays.asList(""), StandardCharsets.UTF_8, StandardOpenOption.CREATE);
			Files.newBufferedWriter(pathToAgentOutput).close();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * adds nodes corresponding to road intersections to GeomVectorField
	 *
	 * @param nodeIterator  Points to first node
	 * @param intersections GeomVectorField containing intersection geometry
	 *
	 *                      Nodes will belong to a planar graph populated from
	 *                      LineString network.
	 */
	private void addIntersectionNodes(final Iterator<Node> nodeIterator, final GeomVectorField intersections) {
		final GeometryFactory fact = new GeometryFactory();
		while (nodeIterator.hasNext()) {
			final Node node = nodeIterator.next();
			final Coordinate coord = node.getCoordinate();
			final Point point = fact.createPoint(coord);
			junctions.addGeometry(new MasonGeometry(point));
		}
	}

	void printAll() throws MPIException {
		final ArrayList<ArrayList<DAgent>> allObjects = MPIUtil.<ArrayList<DAgent>>gather(partition,
				(ArrayList<DAgent>) communicator.field.getAllObjects(), 0);

		if (partition.pid == 0) {
			final ArrayList<String> outputStrings = new ArrayList<>();

			outputStrings.add("Agents - ");
			for (final ArrayList<DAgent> arrayList : allObjects)
				for (final DAgent agent : arrayList)
					outputStrings.add(agent.toString());
			outputStrings.add("------");

			try {
				Files.write(pathToAgentOutput, outputStrings, StandardOpenOption.APPEND);
			} catch (final IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}

		}
	}

	public static void main(final String[] args) throws MPIException {
		Timing.setWindow(20);
		doLoopMPI(DCampusWorld.class, args);
		System.exit(0);
	}

	// Overriding here because we want to add Geometry to the
	// GeomVectorField which is not Distributed
	// TODO; Is there a better way to handle Geometry?
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void addToField(final Serializable obj, final NdPoint p, final int fieldIndex) {
		// if the fieldIndex < 0 we assume that
		// the agent is not supposed to be added to any field

		// If the fieldIndex is correct then the type-cast below will be safe
		if (communicator.field.fieldIndex == fieldIndex) {
			// if all communicator agents have a geometry associated with them
			final DAgent agent = (DAgent) obj;
			communicator.add(p, agent);
			agents.addGeometry(agent.getGeometry());
		} else if (fieldIndex >= 0)
			((HaloField) fieldRegistry.get(fieldIndex)).add(p, obj);
	}

	// Move the Synchronizer login to preSchedule() in DSimState

	private class Printer implements Steppable {
		public void step(final SimState state) {
			final DCampusWorld world = (DCampusWorld) state;
			Timing.stop(Timing.LB_RUNTIME);
			try {
				world.printAll();
			} catch (final Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}

//	private class Synchronizer implements Steppable {
//		private static final long serialVersionUID = 1;
//
//		public void step(final SimState state) {
//			final DCampusWorld world = (DCampusWorld) state;
//			Timing.stop(Timing.LB_RUNTIME);
//			// Timing.start(Timing.MPI_SYNC_OVERHEAD);
//			try {
//				world.communicator.syncHalo();
//				world.queue.sync();
//				// String s = String.format("PID %d Steps %d Number of Agents
//				// %d\n", partition.pid, schedule.getSteps(), flockers.size() -
//				// flockers.ghosts.size());
//				// System.out.print(s);
//			} catch (final Exception e) {
//				e.printStackTrace();
//				System.exit(-1);
//			}
//			// TODO
//			// Need to put the agent in communicator (halo region) into
//			// agents field, but so far we do not have code to easily access
//			// all the agents in that region, and since this does not effect
//			// the logic of simulation so we do this in future
//
//			// Retrieve the migrated agents from queue and schedule them
//			for (final Object obj : world.queue) {
//				final DAgent agent = (DAgent) obj;
//				world.communicator.setLocation(agent, agent.position);
//				world.agents.addGeometry(agent.getGeometry());
//				schedule.scheduleOnce(agent, 1);
//			}
//			// Clear the queue
//			world.queue.clear();
//			// Timing.stop(Timing.MPI_SYNC_OVERHEAD);
//		}
//	}
}
