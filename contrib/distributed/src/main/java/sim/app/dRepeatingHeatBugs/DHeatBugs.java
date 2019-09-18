/*
   Copyright 2006 by Sean Luke and George Mason University
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package sim.app.dRepeatingHeatBugs;

import mpi.MPIException;
import sim.engine.DSimState;
import sim.engine.Schedule;
import sim.field.grid.NDoubleGrid2D;
import sim.field.grid.NObjectsGrid2D;
import sim.util.IntPoint;
import sim.util.Interval;

public class DHeatBugs extends DSimState {
	private static final long serialVersionUID = 1;

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
	public NDoubleGrid2D valgrid; // Instead of DoubleGrid2D
	public NDoubleGrid2D valgrid2; // Instead of DoubleGrid2D
	public NObjectsGrid2D<DHeatBug> bugs; // Instead of SparseGrid2D

	public DHeatBugs(final long seed) {
		this(seed, 1000, 1000, 1000, 5);
	}

	public DHeatBugs(final long seed, final int width, final int height, final int count, final int aoi) {
		super(seed, width, height, aoi);
		gridWidth = width;
		gridHeight = height;
		bugCount = count;
		privBugCount = bugCount / getPartition().numProcessors;
		try {
			valgrid = new NDoubleGrid2D(getPartition(), this.aoi, 0, this);
			valgrid2 = new NDoubleGrid2D(getPartition(), this.aoi, 0, this);
			bugs = new NObjectsGrid2D<DHeatBug>(getPartition(), this.aoi, this);
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	// Same getters and setters as HeatBugs
	public double getMinimumIdealTemperature() {
		return minIdealTemp;
	}

	public void setMinimumIdealTemperature(final double temp) {
		if (temp <= maxIdealTemp)
			minIdealTemp = temp;
	}

	public double getMaximumIdealTemperature() {
		return maxIdealTemp;
	}

	public void setMaximumIdealTemperature(final double temp) {
		if (temp >= minIdealTemp)
			maxIdealTemp = temp;
	}

	public double getMinimumOutputHeat() {
		return minOutputHeat;
	}

	public void setMinimumOutputHeat(final double temp) {
		if (temp <= maxOutputHeat)
			minOutputHeat = temp;
	}

	public double getMaximumOutputHeat() {
		return maxOutputHeat;
	}

	public void setMaximumOutputHeat(final double temp) {
		if (temp >= minOutputHeat)
			maxOutputHeat = temp;
	}

	public double getEvaporationConstant() {
		return evaporationRate;
	}

	public void setEvaporationConstant(final double temp) {
		if (temp >= 0 && temp <= 1)
			evaporationRate = temp;
	}

	public Object domEvaporationConstant() {
		return new Interval(0.0, 1.0);
	}

	public double getDiffusionConstant() {
		return diffusionRate;
	}

	public void setDiffusionConstant(final double temp) {
		if (temp >= 0 && temp <= 1)
			diffusionRate = temp;
	}

	public Object domDiffusionConstant() {
		return new Interval(0.0, 1.0);
	}

	public Object domRandomMovementProbability() {
		return new Interval(0.0, 1.0);
	}

	public double getRandomMovementProbability() {
		return randomMovementProbability;
	}

	public double getMaximumHeat() {
		return DHeatBugs.MAX_HEAT;
	}

	protected void startRoot() {
		super.startRoot();

		System.out.println("Starting Root");

		final double rangeIdealTemp = maxIdealTemp - minIdealTemp;
		final double rangeOutputHeat = maxOutputHeat - minOutputHeat;

		for (int x = 0; x < bugCount; x++) {
			final double idealTemp = random.nextDouble() * rangeIdealTemp + minIdealTemp;
			final double heatOutput = random.nextDouble() * rangeOutputHeat + minOutputHeat;
			int px, py;
			do {
				px = random.nextInt(gridWidth);
				py = random.nextInt(gridHeight);
			} while (bugs.get(new IntPoint(px, py)) != null);
			final DHeatBug b = new DHeatBug(idealTemp, heatOutput, randomMovementProbability, px, py);
			bugs.addRepeatingAgent(new IntPoint(px, py), b, 1, 1);
		}

		System.out.println("Root Started");
	}

	public void start() {
		super.start();
//		final int[] size = getPartition().getPartition().getSize();
//		final double rangeIdealTemp = maxIdealTemp - minIdealTemp;
//		final double rangeOutputHeat = maxOutputHeat - minOutputHeat;
//
//		for (int x = 0; x < privBugCount; x++) {
//			final double idealTemp = random.nextDouble() * rangeIdealTemp + minIdealTemp;
//			final double heatOutput = random.nextDouble() * rangeOutputHeat + minOutputHeat;
//			int px, py; // Why are we doing this? Relationship?
//			do {
//				px = random.nextInt(size[0]) + getPartition().getPartition().ul().getArray()[0];
//				py = random.nextInt(size[1]) + getPartition().getPartition().ul().getArray()[1];
//			} while (bugs.get(new IntPoint(px, py)) != null);
//			final DHeatBug b = new DHeatBug(idealTemp, heatOutput, randomMovementProbability, px, py);
//			// bugs.add(new IntPoint(px, py), b);
//			// schedule.scheduleOnce(b, 1);
//
//			bugs.addRepeatingAgent(new IntPoint(px, py), b, 1, 1);
//		}
//
		schedule.scheduleRepeating(Schedule.EPOCH, 2, new Diffuser(), 1);
	}

	public static void main(final String[] args) throws MPIException {
		doLoopMPI(DHeatBugs.class, args);
		System.exit(0);
	}
}