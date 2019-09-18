/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dflockers;

import mpi.MPIException;
import sim.engine.DSimState;
import sim.field.continuous.NContinuous2D;
import sim.util.DoublePoint;
import sim.util.Timing;

public class DFlockers extends DSimState {
	private static final long serialVersionUID = 1;

	public final static int width = 600;
	public final static int height = 600;
	public final static int numFlockers = 21600;
	public final static double cohesion = 1.0;
	public final static double avoidance = 1.0;
	public final static double randomness = 1.0;
	public final static double consistency = 1.0;
	public final static double momentum = 1.0;
	public final static double deadFlockerProbability = 0.1;
	public final static int neighborhood = 6; // aoi
	public final static double jump = 0.7; // how far do we move in a time step?

	public final NContinuous2D<DFlocker> flockers;

	/** Creates a Flockers simulation with the given random number seed. */
	public DFlockers(final long seed) {
		super(seed, DFlockers.width, DFlockers.height, DFlockers.neighborhood);

		final double[] discretizations = new double[] { DFlockers.neighborhood / 1.5, DFlockers.neighborhood / 1.5 };
		flockers = new NContinuous2D<DFlocker>(getPartition(), aoi, discretizations, this);
	}

	public void start() {
		super.start();
		final int[] size = getPartition().getPartition().getSize();
		for (int x = 0; x < DFlockers.numFlockers / getPartition().numProcessors; x++) {
			final double px = random.nextDouble() * size[0] + getPartition().getPartition().ul().getArray()[0];
			final double py = random.nextDouble() * size[1] + getPartition().getPartition().ul().getArray()[1];
			final DoublePoint location = new DoublePoint(px, py);
			final DFlocker flocker = new DFlocker(location);

			if (random.nextBoolean(DFlockers.deadFlockerProbability))
				flocker.dead = true;

			flockers.addAgent(location, flocker);
//			schedule.scheduleOnce(flocker, 1);
		}

		// schedule.scheduleRepeating(Schedule.EPOCH, 2, new Synchronizer(), 1);
//		schedule.addAfter(new Stopping() {
//			public void step(SimState state) {
//				DFlockers dflockers = (DFlockers) state;
//				Timing.stop(Timing.LB_RUNTIME);
//				// Timing.start(Timing.MPI_SYNC_OVERHEAD);
//				try {
//					// Sync agents in halo area
//					dflockers.flockers.sync();
//					// Actual migration of agents
//					dflockers.migrator.sync();
//					// String s = String.format("PID %d Steps %d Number of Agents %d\n",
//					// partition.pid, schedule.getSteps(), flockers.size() -
//					// flockers.ghosts.size());
//					// System.out.print(s);
//				} catch (Exception e) {
//					e.printStackTrace();
//					System.exit(-1);
//				}
//
//				// Retrieve the migrated agents from queue and schedule them
//				for (Transportee<?> transportee : dflockers.migrator.objectQueue) {
//					DFlocker flocker = (DFlocker) transportee.wrappedObject;
//					dflockers.flockers.setLocation(flocker, flocker.loc);
//					schedule.scheduleOnce(flocker, 1);
//				}
//				// Clear the queue
//				dflockers.migrator.clear();
//				// Timing.stop(Timing.MPI_SYNC_OVERHEAD);
//			}
//		});
	}

	public static void main(final String[] args) throws MPIException {
		Timing.setWindow(20);
		doLoopMPI(DFlockers.class, args);
		System.exit(0);
	}
}
