/*
   Copyright 2006 by Sean Luke and George Mason University
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package sim.app.dRepeatingHeatBugs;

import java.util.ArrayList;
import java.util.HashMap;

import sim.engine.DSimState;
import sim.engine.Schedule;
import sim.field.grid.DDenseGrid2D;
import sim.field.grid.DDoubleGrid2D;
import sim.util.Interval;
import sim.util.*;

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
	public DDoubleGrid2D valgrid; // Instead of DoubleGrid2D
	public DDoubleGrid2D valgrid2; // Instead of DoubleGrid2D
	public DDenseGrid2D<DHeatBug> bugs; // Instead of SparseGrid2D

	public DHeatBugs(final long seed) {
		this(seed, 1000, 1000, 1000, 5);
	}

	public DHeatBugs(final long seed, final int width, final int height, final int count, final int aoi) {
		super(seed, width, height, aoi);
		gridWidth = width;
		gridHeight = height;
		bugCount = count;
		privBugCount = bugCount / getPartitioning().numProcessors;
		try {
			valgrid = new DDoubleGrid2D(getPartitioning(), this.aoi, 0, this);
			valgrid2 = new DDoubleGrid2D(getPartitioning(), this.aoi, 0, this);
			bugs = new DDenseGrid2D<DHeatBug>(getPartitioning(), this.aoi, this);
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
		HashMap<Int2D, ArrayList<DHeatBug>> agents = new HashMap<Int2D, ArrayList<DHeatBug>>();
		final double rangeIdealTemp = maxIdealTemp - minIdealTemp;
		final double rangeOutputHeat = maxOutputHeat - minOutputHeat;
		for (int x = 0; x < bugCount; x++) {
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
		
		sendRootInfoToAll("agents",agents);

	}

	public void start() {
		super.start();

		HashMap<Int2D, ArrayList<DHeatBug>> agents = (HashMap<Int2D, ArrayList<DHeatBug>>) getRootInfo("agents");
		for (Int2D p : agents.keySet()) {
			for (DHeatBug a : agents.get(p)) {
				if (partition.getPartition().contains(p))
					bugs.addRepeatingAgent(p, a, 1, 1);
			}
		}
		schedule.scheduleRepeating(Schedule.EPOCH, 2, new Diffuser(), 1);
	}

	public static void main(final String[] args) {
		doLoopDistributed(DHeatBugs.class, args);
		System.exit(0);
	}
}