/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dflockers;

import mpi.MPIException;
import sim.app.dheatbugs.DHeatBug;
import sim.engine.DSimState;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.DRemoteTransporter;
import sim.field.Transportee;
import sim.field.continuous.NContinuous2D;
import sim.util.DoublePoint;
import sim.util.IntHyperRect;
import sim.util.Timing;

public class DFlockers extends DSimState {
	private static final long serialVersionUID = 1;

	public NContinuous2D<DFlocker> flockers;
	public double width = 600;
	public double height = 600;
	public int numFlockers = 21600;
	public double cohesion = 1.0;
	public double avoidance = 1.0;
	public double randomness = 1.0;
	public double consistency = 1.0;
	public double momentum = 1.0;
	public double deadFlockerProbability = 0.1;
	public double neighborhood = 10;
	public double jump = 0.7; // how far do we move in a timestep?

	public IntHyperRect myPart;
	public DRemoteTransporter migrator;

	/** Creates a Flockers simulation with the given random number seed. */
	public DFlockers(long seed) {
		super(seed);
		try {
			int[] aoi = new int[] { (int) neighborhood, (int) neighborhood };
			// int[] size = new int[] { (int) width, (int) height };
			double[] discretizations = new double[] { neighborhood / 1.5, neighborhood / 1.5 };

			/*
			 * partition = DNonUniformPartition.getPartitionScheme(size, true, aoi);
			 * partition.initUniformly(null); partition.commit(); flockers = new
			 * NContinuous2D<DFlocker>(partition, aoi, discretizations); queue = new
			 * DObjectMigratorNonUniform(partition);
			 */
			flockers = new NContinuous2D<DFlocker>(partition, aoi, discretizations);
			migrator = new DRemoteTransporter(partition);

		} catch (Exception e) {
			e.printStackTrace(System.out);
			System.exit(-1);
		}
		myPart = partition.getPartition();

	}

	public void addToLocation(Transportee transportee) {
		// TODO: Move schedule.addAfter stuff here
	}

	public void syncFields() {
		// TODO: Move schedule.addAfter stuff here
	}

	public void postSync() {
		// TODO: Move schedule.addAfter stuff here
	}

	public void start() {
		super.start();
		int[] size = myPart.getSize();
		for (int x = 0; x < numFlockers / partition.numProcessors; x++) {
			double px, py;
			px = random.nextDouble() * size[0] + myPart.ul().getArray()[0];
			py = random.nextDouble() * size[1] + myPart.ul().getArray()[1];
			DoublePoint location = new DoublePoint(px, py);
			DFlocker flocker = new DFlocker(location);

			if (random.nextBoolean(deadFlockerProbability))
				flocker.dead = true;

			flockers.setLocation(flocker, location);
			schedule.scheduleOnce(flocker, 1);
		}
		// schedule.scheduleRepeating(Schedule.EPOCH, 2, new Synchronizer(), 1);
		schedule.addAfter(new Steppable() {
			public void step(SimState state) {
				DFlockers dflockers = (DFlockers) state;
				Timing.stop(Timing.LB_RUNTIME);
				// Timing.start(Timing.MPI_SYNC_OVERHEAD);
				try {
					// Sync agents in halo area
					dflockers.flockers.sync();
					// Actual migration of agents
					dflockers.migrator.sync();
					// String s = String.format("PID %d Steps %d Number of Agents %d\n",
					// partition.pid, schedule.getSteps(), flockers.size() -
					// flockers.ghosts.size());
					// System.out.print(s);
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}

				// Retrieve the migrated agents from queue and schedule them
				for (Transportee transportee : dflockers.migrator.objectQueue) {
					DFlocker flocker = (DFlocker) transportee.wrappedObject;
					dflockers.flockers.setLocation(flocker, flocker.loc);
					schedule.scheduleOnce(flocker, 1);
				}
				// Clear the queue
				dflockers.migrator.clear();
				// Timing.stop(Timing.MPI_SYNC_OVERHEAD);
			}
		});
	}

	public static void main(String[] args) throws MPIException {
		Timing.setWindow(20);
		doLoopMPI(DFlockers.class, args);
		System.exit(0);
	}
}
