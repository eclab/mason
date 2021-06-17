/*
   Copyright 2006 by Sean Luke and George Mason University
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package sim.app.dheatbugs;

import java.util.ArrayList;

import java.util.HashMap;
import sim.engine.DSimState;
import sim.engine.DSteppable;
import sim.engine.Schedule;
import sim.engine.SimState;
import sim.field.grid.DDenseGrid2D;
import sim.field.grid.DDoubleGrid2D;
import sim.util.Interval;
import sim.util.*;

public class DHeatBugs extends DSimState
{
	private static final long serialVersionUID = 1;

	static
	{
		DSimState.setMultiThreaded(true);
	}

	public double minIdealTemp = 17000;
	public double maxIdealTemp = 31000;
	public double minOutputHeat = 6000;
	public double maxOutputHeat = 10000;

	public double evaporationRate = 0.993;
	public double diffusionRate = 1.0;
	public static final double MAX_HEAT = 32000;
	public double randomMovementProbability = 0.1;

	public int gridHeight;
	public int gridWidth;
	public int bugCount;
	// TODO: Should this be updated after migration/balancing?
	public final int privBugCount; // the replacement for get/setBugCount ?

	/*
	 * Missing get/setGridHeight get/setGridWidth get/setBugCount
	 */
	public DDoubleGrid2D valgrid; // Instead of DoubleGrid2D
	public DDoubleGrid2D valgrid2; // Instead of DoubleGrid2D
	public DDenseGrid2D<DHeatBug> bugs; // Instead of SparseGrid2D

	public DHeatBugs(final long seed)
	{
		this(seed, 100, 100, 100, 1);
	}

	public DHeatBugs(final long seed, final int width, final int height, final int count, final int aoi)
	{
		super(seed, width, height, aoi);
		gridWidth = width;
		gridHeight = height;
		bugCount = count;
		privBugCount = bugCount / getPartition().getNumProcessors();
		try
		{
			valgrid = new DDoubleGrid2D(this);
			valgrid2 = new DDoubleGrid2D(this);
			bugs = new DDenseGrid2D<DHeatBug>(this);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
			System.exit(-1);
		}

//		balanceInterval = 100000;
	}

	// Same getters and setters as HeatBugs
	public double getMinimumIdealTemperature()
	{
		return minIdealTemp;
	}

	public void setMinimumIdealTemperature(final double temp)
	{
		if (temp <= maxIdealTemp)
			minIdealTemp = temp;
	}

	public double getMaximumIdealTemperature()
	{
		return maxIdealTemp;
	}

	public void setMaximumIdealTemperature(final double temp)
	{
		if (temp >= minIdealTemp)
			maxIdealTemp = temp;
	}

	public double getMinimumOutputHeat()
	{
		return minOutputHeat;
	}

	public void setMinimumOutputHeat(final double temp)
	{
		if (temp <= maxOutputHeat)
			minOutputHeat = temp;
	}

	public double getMaximumOutputHeat()
	{
		return maxOutputHeat;
	}

	public void setMaximumOutputHeat(final double temp)
	{
		if (temp >= minOutputHeat)
			maxOutputHeat = temp;
	}

	public double getEvaporationConstant()
	{
		return evaporationRate;
	}

	public void setEvaporationConstant(final double temp)
	{
		if (temp >= 0 && temp <= 1)
			evaporationRate = temp;
	}

	public Object domEvaporationConstant()
	{
		return new Interval(0.0, 1.0);
	}

	public double getDiffusionConstant()
	{
		return diffusionRate;
	}

	public void setDiffusionConstant(final double temp)
	{
		if (temp >= 0 && temp <= 1)
			diffusionRate = temp;
	}

	public Object domDiffusionConstant()
	{
		return new Interval(0.0, 1.0);
	}

	public Object domRandomMovementProbability()
	{
		return new Interval(0.0, 1.0);
	}

	public double getRandomMovementProbability()
	{
		return randomMovementProbability;
	}

	public double getMaximumHeat()
	{
		return DHeatBugs.MAX_HEAT;
	}

	protected void startRoot()
	{
		HashMap<Int2D, ArrayList<DHeatBug>> agents = new HashMap<Int2D, ArrayList<DHeatBug>>();
		final double rangeIdealTemp = maxIdealTemp - minIdealTemp;
		final double rangeOutputHeat = maxOutputHeat - minOutputHeat;
		for (int x = 0; x < bugCount; x++)
		{
			final double idealTemp = random.nextDouble() * rangeIdealTemp + minIdealTemp;
			final double heatOutput = random.nextDouble() * rangeOutputHeat + minOutputHeat;
			int px = random.nextInt(gridWidth);
			int py = random.nextInt(gridHeight);
			final DHeatBug b = new DHeatBug(idealTemp, heatOutput, randomMovementProbability, px, py);
			Int2D point = new Int2D(px, py);
			if (!agents.containsKey(point))
				agents.put(point, new ArrayList<DHeatBug>());
			agents.get(point).add(b);
		}

		sendRootInfoToAll("agents", agents);

		schedule.scheduleRepeating(new DSteppable()
		{
			public void step(SimState state)
			{

			}
		}
		, 10, 1);
	}

	// @Override
	// public void preSchedule() {
	// super.preSchedule();
	// try {
	// MPI.COMM_WORLD.barrier();
	// } catch (MPIException e2) {
	// // TODO Auto-generated catch block
	// e2.printStackTrace();
	// }
	// if (schedule.getSteps() > 0) {
	// int[] dstDispl = new int[partition.numProcessors];
	// final int[] dstCount = new int[partition.numProcessors];
	// int[] recv = new int[bugCount];

	// int[] ids = new int[idLocal.size()];
	// for (int i = 0; i < idLocal.size(); i++) {
	// ids[i] = idLocal.get(i);
	// }

	// int num = ids.length;

	// try {

	// MPI.COMM_WORLD.gather(new int[] { num }, 1, MPI.INT, dstCount, 1, MPI.INT,
	// 0);

	// dstDispl = IntStream.range(0, dstCount.length)
	// .map(x -> Arrays.stream(dstCount).limit(x).sum())
	// .toArray();

	// MPI.COMM_WORLD.gatherv(ids, num, MPI.INT, recv, dstCount, dstDispl, MPI.INT,
	// 0);

	// } catch (MPIException e1) {
	// // TODO Auto-generated catch block
	// e1.printStackTrace();
	// }

	// if (partition.getPid() == 0) {
	// System.out.println("STEP "+schedule.getSteps()+" count ");
	// for (int i = 0; i < dstCount.length; i++) {
	// System.out.print(dstCount[i]+" ");
	// }
	// System.out.println();
	// System.out.println("STEP "+schedule.getSteps()+" disp ");
	// for (int i = 0; i < dstDispl.length; i++) {
	// System.out.print(dstDispl[i]+" ");
	// }
	// System.out.println();
	// try {
	// Thread.sleep(1000);
	// } catch (InterruptedException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// Arrays.sort(recv);
	// for (int i = 0; i < idAgents.size(); i++) {
	// if (idAgents.get(i) != recv[i]) {
	// System.err.println("ERROR: something wrong happens --> idAgents.get(i)
	// "+idAgents.get(i)+" recv[i] "+recv[i]);
	// System.exit(1);
	// }
	// }
	// }
	// idLocal.clear();
	// }
	// }

	public void start()
	{
		super.start();

		HashMap<Int2D, ArrayList<DHeatBug>> agents = (HashMap<Int2D, ArrayList<DHeatBug>>) getRootInfo("agents");
		for (Int2D p : agents.keySet())
		{
			for (DHeatBug a : agents.get(p))
			{
				if (partition.getLocalBounds().contains(p))
				{
					bugs.addAgent(p, a, 0, 0, 1);
					System.out.println("start : " + a+" "+a.loc_x+" "+a.loc_y+p);
				}

			}
		}
		schedule.scheduleRepeating(Schedule.EPOCH, 1, new Diffuser(), 1);

//		// Stats Example:
//		schedule.scheduleRepeating(new DSteppable() {
//			public void step(SimState state) {
//				int count = 0;
//				for (Int2D p : agents.keySet()) {
//					for (DHeatBug a : agents.get(p)) {
//						if (partition.getLocalBounds().contains(p)) {
//							count++;
//						}
//					}
//				}
//				addStat("hello");
//				addStat(count);
//				addStat(null);
//			}}, 10, 1);
	}

	public static void main(final String[] args)
	{
		doLoopDistributed(DHeatBugs.class, args);
		System.exit(0);
	}
}