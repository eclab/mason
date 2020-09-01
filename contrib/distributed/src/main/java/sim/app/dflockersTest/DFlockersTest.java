/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dflockersTest;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.stream.IntStream;

import mpi.MPI;
import mpi.MPIException;
import sim.engine.DSimState;
import sim.field.continuous.DContinuous2D;
import sim.util.Timing;
import sim.util.*;

public class DFlockersTest extends DSimState {
	private static final long serialVersionUID = 1;

	public final static int width = 600;
	public final static int height = 600;
	public final static int numFlockers = 1000;
	public final static double cohesion = 1.0;
	public final static double avoidance = 1.0;
	public final static double randomness = 1.0;
	public final static double consistency = 1.0;
	public final static double momentum = 1.0;
	public final static double deadFlockerProbability = 0;
	public final static int neighborhood = 6; // aoi
	public final static double jump = 0.7; // how far do we move in a time step?

	public final DContinuous2D<DFlocker> flockers;

	public ArrayList<Integer> idAgents;
	public ArrayList<Integer> idLocal;

	final SimpleDateFormat format = new SimpleDateFormat("ss-mm-HH-yyyy-MM-dd");
	String dateString = format.format(new Date());
	String dirname = System.getProperty("user.dir") + File.separator + dateString;

	/** Creates a Flockers simulation with the given random number seed. */
	public DFlockersTest(final long seed) {
		super(seed, DFlockersTest.width, DFlockersTest.height, DFlockersTest.neighborhood);

		final double[] discretizations = new double[] { DFlockersTest.neighborhood / 1.5, DFlockersTest.neighborhood / 1.5 };
		flockers = new DContinuous2D<DFlocker>(getPartitioning(), aoi, discretizations, this);
		idAgents = new ArrayList<Integer>();
		idLocal = new ArrayList<Integer>();
	}

	@Override
	public void preSchedule() {
		super.preSchedule();

		if (schedule.getSteps() > 0) {
			int[] dstDispl = new int[partition.numProcessors];
			final int[] dstCount = new int[partition.numProcessors];
			int[] recv = new int[numFlockers];

			int[] ids = new int[idLocal.size()];
			for (int i = 0; i < idLocal.size(); i++) {
				ids[i] = idLocal.get(i);
			}

			int num = ids.length;

			try {

				MPI.COMM_WORLD.gather(new int[] { num }, 1, MPI.INT, dstCount, 1, MPI.INT, 0);

				dstDispl = IntStream.range(0, dstCount.length)
						.map(x -> Arrays.stream(dstCount).limit(x).sum())
						.toArray();

				MPI.COMM_WORLD.gatherv(ids, num, MPI.INT, recv, dstCount, dstDispl, MPI.INT, 0);

			} catch (MPIException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			if (partition.getPid() == 0) {
//				System.out.println("count ");
//				for (int i = 0; i < dstCount.length; i++) {
//					System.out.print(dstCount[i]+" ");
//				}
//				System.out.println();
//				System.out.println("disp ");
//				for (int i = 0; i < dstDispl.length; i++) {
//					System.out.print(dstDispl[i]+" ");
//				}
//				System.out.println();
//				try {
//					Thread.sleep(1000);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
				Arrays.sort(recv);
				for (int i = 0; i < idAgents.size(); i++) {
					if (idAgents.get(i) != recv[i]) {
						System.err.println("ERROR: something wrong happens --> idAgents.get(i) "+idAgents.get(i)+" recv[i] "+recv[i]);
						System.exit(1);
					}
				}
			}
			idLocal.clear();
		}
	}

	protected void startRoot() {
		ArrayList<DFlocker> agents = new ArrayList<DFlocker>();
		for (int x = 0; x < DFlockersTest.numFlockers; x++) {
			final Double2D loc = new Double2D(random.nextDouble() * width, random.nextDouble() * height);
			DFlocker flocker = new DFlocker(loc, x);
			idAgents.add(x);
			if (random.nextBoolean(deadFlockerProbability))
				flocker.dead = true;
			agents.add(flocker);

		}
		//System.out.println(agents);
		sendRootInfoToAll("agents",agents);
	}

	public void start() {
		// TODO Auto-generated method stub
		super.start(); // do not forget this line

		ArrayList<Object> agents = (ArrayList<Object>) getRootInfo("agents");

		for (Object p : agents) {
			DFlocker a = (DFlocker) p;
			if (partition.getPartition().contains(a.loc))
				flockers.addAgent(a.loc, a);
		}

	}

	public static void main(final String[] args) {
		Timing.setWindow(20);
		doLoopDistributed(DFlockersTest.class, args);
		System.exit(0);
	}
}
