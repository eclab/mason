/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dtest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import sim.engine.DSimState;
import sim.field.continuous.DContinuous2D;
import sim.util.Double2D;
import sim.util.Timing;

public class DSimulation extends DSimState
{
	private static final long serialVersionUID = 1;

	public final static int width = 600;
	public final static int height = 600;
	public final static int numFlockers = 9;
	public final static int neighborhood = 10; // aoi

	public final DContinuous2D<DAgent> field;

	final SimpleDateFormat format = new SimpleDateFormat("ss-mm-HH-yyyy-MM-dd");
	String dateString = format.format(new Date());
	String dirname = System.getProperty("user.dir") + File.separator + dateString;

	/** Creates a Flockers simulation with the given random number seed. */
	public DSimulation(final long seed)
	{
		super(seed, DSimulation.width, DSimulation.height, DSimulation.neighborhood, false);

		// final double[] discretizations = new double[] { DSimulation.neighborhood /
		// 1.5, DSimulation.neighborhood / 1.5 };
		field = new DContinuous2D<>((int) (DSimulation.neighborhood / 1.5), this);
	}

	@Override
	public void preSchedule()
	{
		super.preSchedule();

		if (schedule.getSteps() == 92)
		{
			System.exit(0);
		}

		// if (schedule.getSteps() % 10 == 0 ) {
		String filename = dirname + File.separator +
				getPartition().getPID() + "." + (schedule.getSteps());

		File testdir = new File(dirname);
		testdir.mkdir();

		File myfileagent = new File(filename);
		System.out.println("Create file " + filename);

		PrintWriter out = null;
		try
		{
			myfileagent.createNewFile();
			out = new PrintWriter(new FileOutputStream(myfileagent, false));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		//for (DAgent f : ((ContinuousStorage<DAgent>) field.getHaloGrid().getStorage())
		//		.getObjects(field.getHaloGrid().getStorage().getShape()))
		//{
//			out.println("agent " + f.getId() + " in position " + f.loc + " num neighbours: " + f.neighbours.size() + " neighbours " + f.neighbours);
		//	out.println("agent " + f.ID() + " in position " + f.loc + " num neighbours: " + f.neighbours.size()
		//			+ " neighbours " + f.neighbours);
		//}

		out.close();
		// }

	}

	@Override
	protected void startRoot()
	{
		ArrayList<DAgent> agents = new ArrayList<DAgent>();
//		int c = 0;
		for (int i = 75; i < 600; i = i + 150)
		{
			for (int j = 75; j < 600; j = j + 150)
			{
				Double2D loc = new Double2D(i, j);
//				int id = 100 * partition.toPartitionPID(loc) + c;
//				c++;
				agents.add(new DAgent(loc));
			}
		}

		sendRootInfoToAll("agents", agents);
	}

	@Override
	public void start()
	{
		super.start(); // do not forget this line

		ArrayList<Object> agents = (ArrayList<Object>) getRootInfo("agents");

		for (Object p : agents)
		{
			DAgent a = (DAgent) p;
			if (partition.getLocalBounds().contains(a.loc))
			{
				field.addAgent(a.loc, a, 0, 0);
				System.out.println("pid " + partition.getPID() + " add agent " + a);
			}
		}

	}

	public static void main(final String[] args)
	{
		Timing.setWindow(20);
		doLoopDistributed(DSimulation.class, args);
		System.exit(0);
	}
}