/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dheatbugs;
import sim.engine.*;
import sim.field.grid.*;
import sim.util.*;

import sim.field.DUniformPartition;
import java.util.concurrent.TimeUnit;

import mpi.*;
import java.nio.*;

public class DHeatBugs extends SimState {
	private static final long serialVersionUID = 1;

	public double minIdealTemp = 17000;
	public double maxIdealTemp = 31000;
	public double minOutputHeat = 6000;
	public double maxOutputHeat = 10000;

	public double evaporationRate = 0.993;
	public double diffusionRate = 1.0;
	public static final double MAX_HEAT = 32000;
	public double randomMovementProbability = 0.1;

	DHeatBug[] bugs;

	public double getMinimumIdealTemperature() { return minIdealTemp; }
	public void setMinimumIdealTemperature( double temp ) { if ( temp <= maxIdealTemp ) minIdealTemp = temp; }
	public double getMaximumIdealTemperature() { return maxIdealTemp; }
	public void setMaximumIdealTemperature( double temp ) { if ( temp >= minIdealTemp ) maxIdealTemp = temp; }
	public double getMinimumOutputHeat() { return minOutputHeat; }
	public void setMinimumOutputHeat( double temp ) { if ( temp <= maxOutputHeat ) minOutputHeat = temp; }
	public double getMaximumOutputHeat() { return maxOutputHeat; }
	public void setMaximumOutputHeat( double temp ) { if ( temp >= minOutputHeat ) maxOutputHeat = temp; }
	public double getEvaporationConstant() { return evaporationRate; }
	public void setEvaporationConstant( double temp ) { if ( temp >= 0 && temp <= 1 ) evaporationRate = temp; }
	public Object domEvaporationConstant() { return new Interval(0.0, 1.0); }
	public double getDiffusionConstant() { return diffusionRate; }
	public void setDiffusionConstant( double temp ) { if ( temp >= 0 && temp <= 1 ) diffusionRate = temp; }
	public Object domDiffusionConstant() { return new Interval(0.0, 1.0); }
	public double getRandomMovementProbability() { return randomMovementProbability; }

	public void setRandomMovementProbability( double t ) {
		if (t >= 0 && t <= 1) {
			randomMovementProbability = t;
			for ( int i = 0 ; i < bugCount ; i++ )
				if (bugs[i] != null)
					bugs[i].setRandomMovementProbability( randomMovementProbability );
		}
	}
	public Object domRandomMovementProbability() { return new Interval(0.0, 1.0); }

	public double getMaximumHeat() { return MAX_HEAT; }

	public DDoubleGrid2D valgrid;
	public DDoubleGrid2D valgrid2;

	public DUniformPartition partition;
	public DistributedAgentQueue queue;

	int pw, ph, bugCount;

	public DHeatBugs(long seed) {
		this(seed, 1000, 1000, 1000, 1);
	}

	public DHeatBugs(long seed, int width, int height, int count, int aoi) {
		super(seed);

		bugCount = count;

		try {
			partition = new DUniformPartition(new int[] {width, height});
			valgrid = new DDoubleGrid2D(width, height, aoi, 0, partition);
			valgrid2 = new DDoubleGrid2D(width, height, aoi, 0, partition);
			queue = new DistributedAgentQueue(partition, this);
		} catch (Exception e) {
			e.printStackTrace(System.out);
			System.exit(-1);
		}

		pw = width / partition.dims[0];
		ph = height / partition.dims[1];
	}

	public void start() {
		super.start();

		for (int x = 0; x < bugCount / partition.np; x++) {
			double idealTemp = random.nextDouble() * (maxIdealTemp - minIdealTemp) + minIdealTemp;
			double heatOutput = random.nextDouble() * (maxOutputHeat - minOutputHeat) + minOutputHeat;
			int px = random.nextInt(pw) + pw * partition.coords[0];
			int py = random.nextInt(ph) + ph * partition.coords[1];
			try {
				queue.add( new DHeatBug(idealTemp, heatOutput, randomMovementProbability, px, py), px, py);
			} catch (Exception e) {
				e.printStackTrace(System.out);
				System.exit(-1);
			}
		}

		schedule.scheduleRepeating(Schedule.EPOCH, 0, new Inspector(), 1);
		schedule.scheduleRepeating(Schedule.EPOCH, 2, new Diffuser(), 1);
		schedule.scheduleRepeating(Schedule.EPOCH, 3, new Synchronizer(), 1);
	}

	public static void main(String[] args) throws MPIException {
		MPI.Init(args);
		doLoop(DHeatBugs.class, args);
		MPI.Finalize();
		System.exit(0);
	}

	private class Synchronizer implements Steppable {
		private static final long serialVersionUID = 1;

		public void step(SimState state) {
			DHeatBugs hb = (DHeatBugs)state;

			try {
				hb.valgrid.sync();
				hb.queue.sync();
				//TimeUnit.MILLISECONDS.sleep(500);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}

	private class Inspector implements Steppable {
		public void step( final SimState state ) {
			DHeatBugs hb = (DHeatBugs)state;
			String s = String.format("PID %d Step %d Agent Count %d\n", hb.partition.pid, hb.schedule.getSteps(), hb.queue.size());
			// for (Steppable i : hb.queue) {
			// 	DHeatBug a = (DHeatBug)i;
			// 	s += a.toString() + "\n";
			// }
			System.out.print(s);
		}
	}
}