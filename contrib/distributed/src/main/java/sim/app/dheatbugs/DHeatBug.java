/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dheatbugs;

import sim.engine.SimState;
import sim.field.grid.NDoubleGrid2D;
import sim.util.IntPoint;
import sim.engine.AbstractStopping;

public class DHeatBug extends AbstractStopping {
	private static final long serialVersionUID = 1;

	public int loc_x, loc_y;
	public boolean isFirstStep = true;

	public double idealTemp;
	public double heatOutput;
	public double randomMovementProbability;

	public DHeatBug(final double idealTemp, final double heatOutput, final double randomMovementProbability,
			final int loc_x, final int loc_y) {
		this.heatOutput = heatOutput;
		this.idealTemp = idealTemp;
		this.randomMovementProbability = randomMovementProbability;
		this.loc_x = loc_x;
		this.loc_y = loc_y;
	}

	public void addHeat(final NDoubleGrid2D grid, final int x, final int y, final double heat) {
		double new_heat = grid.get(new IntPoint(x, y)) + heat;
		if (new_heat > DHeatBugs.MAX_HEAT)
			new_heat = DHeatBugs.MAX_HEAT;
		grid.add(new IntPoint(x, y), new_heat);

//		try {
//			double new_heat = grid.get(x, y) + heat;
//			if (new_heat > DHeatBugs.MAX_HEAT)
//				new_heat = DHeatBugs.MAX_HEAT;
//			grid.set(x, y, new_heat);
//		} catch (Exception e) {
//			e.printStackTrace();
//			System.out.println("HeatBug - " + this +
//					"\nBugs: haloPart - " + hb.bugs.haloPart +
//					"\nBugs: origPart - " + hb.bugs.origPart +
//					"\nBugs: privPart - " + hb.bugs.privPart +
//					"\nCurrent pId - " + hb.partition.getPid() +
//					"\npId according to QuadTree - " +
//					hb.partition.toPartitionId(new int[] { loc_x, loc_y }) + "\nPartition Info - " + hb.partition);
//		}
	}

	public void step(final SimState state) {
		final DHeatBugs dHeatBugs = (DHeatBugs) state;

		// Skip addHeat for the first step
		if (!isFirstStep) {
			addHeat(dHeatBugs.valgrid, loc_x, loc_y, heatOutput);
		} else {
			isFirstStep = false;
		}

		final int START = -1;
		int bestx = START;
		int besty = 0;

		if (state.random.nextBoolean(randomMovementProbability)) { // go to random place
			bestx = state.random.nextInt(3) - 1 + loc_x;
			besty = state.random.nextInt(3) - 1 + loc_y;
		} else if (dHeatBugs.valgrid.get(new IntPoint(loc_x, loc_y)) > idealTemp) { // go to coldest place
			for (int x = -1; x < 2; x++)
				for (int y = -1; y < 2; y++)
					if (!(x == 0
							&& y == 0)) {
						final int xx = (x + loc_x);
						final int yy = (y + loc_y);
						if (bestx == START
								|| (dHeatBugs.valgrid.get(new IntPoint(xx, yy)) < dHeatBugs.valgrid
										.get(new IntPoint(bestx, besty)))
								|| (dHeatBugs.valgrid.get(new IntPoint(xx, yy)) == dHeatBugs.valgrid
										.get(new IntPoint(bestx, besty))
										&& state.random.nextBoolean())) // not uniform, but enough to break up the
																		// go-up-and-to-the-left syndrome
						{
							bestx = xx;
							besty = yy;
						}
					}
		} else if (dHeatBugs.valgrid.get(new IntPoint(loc_x, loc_y)) < idealTemp) { // go to warmest place
			for (int x = -1; x < 2; x++)
				for (int y = -1; y < 2; y++)
					if (!(x == 0
							&& y == 0)) {
						final int xx = (x + loc_x);
						final int yy = (y + loc_y);
						if (bestx == START
								|| (dHeatBugs.valgrid.get(new IntPoint(xx, yy)) > dHeatBugs.valgrid
										.get(new IntPoint(bestx, besty)))
								|| (dHeatBugs.valgrid.get(new IntPoint(xx, yy)) == dHeatBugs.valgrid
										.get(new IntPoint(bestx, besty))
										&& state.random.nextBoolean())) // not uniform, but enough to break up the
																		// go-up-and-to-the-left syndrome
						{
							bestx = xx;
							besty = yy;
						}
					}
		} else { // stay put
			bestx = loc_x;
			besty = loc_y;
		}

		final int old_x = loc_x;
		final int old_y = loc_y;
		loc_x = dHeatBugs.valgrid.stx(bestx);
		loc_y = dHeatBugs.valgrid.sty(besty);

		dHeatBugs.bugs.moveAgent(new IntPoint(old_x, old_y), new IntPoint(loc_x, loc_y), this);

//
//		try {
//			final int dst = dHeatBugs.partition.toPartitionId(new int[] { loc_x, loc_y });
//			if (dst != dHeatBugs.partition.getPid()) {
//				dHeatBugs.bugs.remove(new IntPoint(old_x, old_y), this);
//
////				if (!hb.bugs.remove(old_x, old_y, this))
////					System.err.println("Failed to remove!");
////				System.out.println("Migrating Bug from - [" + old_x + ", " + old_y + "] to - [" + loc_x + ", " + loc_y);
//
//				// TODO: Abstract away the migration from the model
//				dHeatBugs.transporter.migrateAgent(this, dst, new IntPoint(loc_x, loc_y),
//						dHeatBugs.bugs.fieldIndex);
//			} else {
//				// TODO: this should be moveAgent
//				dHeatBugs.bugs.move(new IntPoint(old_x, old_y), new IntPoint(loc_x, loc_y), this);
//				dHeatBugs.schedule.scheduleOnce(this, 1);
//			}
//		} catch (final Exception e) {
//			e.printStackTrace(System.out);
//			System.exit(-1);
//		}
	}

	public double getIdealTemperature() {
		return idealTemp;
	}

	public void setIdealTemperature(final double t) {
		idealTemp = t;
	}

	public double getHeatOutput() {
		return heatOutput;
	}

	public void setHeatOutput(final double t) {
		heatOutput = t;
	}

	public double getRandomMovementProbability() {
		return randomMovementProbability;
	}

	public void setRandomMovementProbability(final double t) {
		if (t >= 0
				&& t <= 1)
			randomMovementProbability = t;
	}

	public String toString() {
		return String.format("[%d, %d]", loc_x, loc_y);
	}
}
