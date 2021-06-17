/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dflockerstest;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.IntStream;

import mpi.MPI;
import mpi.MPIException;
import sim.app.dflockers.DFlockers;
import sim.engine.DSimState;
import sim.field.continuous.DContinuous2D;
import sim.util.Timing;
import sim.util.*;

public class DFlockersTest extends DSimState
{
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

	// public ArrayList<Long> idAgents;
	public ArrayList<Long> idLocal;

	final SimpleDateFormat format = new SimpleDateFormat("ss-mm-HH-yyyy-MM-dd");
	String dateString = format.format(new Date());
	String dirname = System.getProperty("user.dir") + File.separator + dateString;

	/** Creates a Flockers simulation with the given random number seed. */
	public DFlockersTest(final long seed)
	{
		super(seed, DFlockersTest.width, DFlockersTest.height, DFlockersTest.neighborhood);

		// final double[] discretizations = new double[] { DFlockersTest.neighborhood /
		// 1.5, DFlockersTest.neighborhood / 1.5 };
		flockers = new DContinuous2D<>((int) (neighborhood / 1.5), this);
		// idAgents = new ArrayList<>();
		idLocal = new ArrayList<>();
	}

	@Override
	public void preSchedule()
	{
		super.preSchedule();
		try
		{
			MPI.COMM_WORLD.barrier();
		}
		catch (MPIException e2)
		{
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		if (schedule.getSteps() > 0)
		{
			int[] dstDispl = new int[partition.getNumProcessors()];
			final int[] dstCount = new int[partition.getNumProcessors()];
			long[] recv = new long[numFlockers];

//			int[] ids = new int[idLocal.size()];
//			for (int i = 0; i < idLocal.size(); i++) {
//				ids[i] = idLocal.get(i);
//			}
//			Long[] ids = (Long[]) idLocal.toArray();
			long[] ids = new long[idLocal.size()];
			for (int i = 0; i < idLocal.size(); i++)
			{
				ids[i] = idLocal.get(i);
			}

			int num = ids.length;

			try
			{

				MPI.COMM_WORLD.gather(new int[] { num }, 1, MPI.INT, dstCount, 1, MPI.INT, 0);

				dstDispl = IntStream.range(0, dstCount.length)
						.map(x -> Arrays.stream(dstCount).limit(x).sum())
						.toArray();

				MPI.COMM_WORLD.gatherv(ids, num, MPI.LONG, recv, dstCount, dstDispl, MPI.LONG, 0);

			}
			catch (MPIException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			if (partition.getPID() == 0)
			{
				System.out.println("STEP " + schedule.getSteps() + " count ");
				for (int i = 0; i < dstCount.length; i++)
				{
					System.out.print(dstCount[i] + " ");
				}
				System.out.println();
				System.out.println("STEP " + schedule.getSteps() + " disp ");
				for (int i = 0; i < dstDispl.length; i++)
				{
					System.out.print(dstDispl[i] + " ");
				}
				System.out.println();
				try
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Arrays.sort(recv);
				/*
				 * for (int i = 0; i < idAgents.size(); i++) { if (idAgents.get(i) != recv[i]) {
				 * System.err.println("ERROR: something wrong happens --> idAgents.get(i) " +
				 * idAgents.get(i) + " recv[i] " + recv[i]); System.exit(1); } }
				 */
			}
			idLocal.clear();
		}
	}

	protected void startRoot()
	{
		ArrayList<DFlocker> agents = new ArrayList<DFlocker>();
		for (int x = 0; x < DFlockersTest.numFlockers; x++)
		{
			final Double2D loc = new Double2D(random.nextDouble() * width, random.nextDouble() * height);
			DFlocker flocker = new DFlocker(loc);
			// idAgents.add(flocker.getId());
			if (random.nextBoolean(deadFlockerProbability))
				flocker.dead = true;
			agents.add(flocker);

		}
		// System.out.println(agents);
		sendRootInfoToAll("agents", agents);
	}

	public void start()
	{
		// TODO Auto-generated method stub
		super.start(); // do not forget this line

		ArrayList<Object> agents = (ArrayList<Object>) getRootInfo("agents");

		for (Object p : agents)
		{
			DFlocker a = (DFlocker) p;
			if (partition.getLocalBounds().contains(a.loc))
				flockers.addAgent(a.loc, a, 0, 0);
		}

	}

	public static void main(final String[] args)
	{
		Timing.setWindow(20);
		doLoopDistributed(DFlockersTest.class, args);
		System.exit(0);
	}
}