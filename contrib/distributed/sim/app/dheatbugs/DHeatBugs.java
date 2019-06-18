/*
   Copyright 2006 by Sean Luke and George Mason University
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package sim.app.dheatbugs;

import mpi.MPIException;
import sim.engine.DSimState;
import sim.engine.Schedule;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.DQuadTreePartition;
import sim.field.RemoteProxy;
import sim.field.Transportee;
import sim.field.grid.NDoubleGrid2D;
import sim.field.grid.NObjectsGrid2D;
import sim.util.Interval;
import sim.util.Timing;

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
	public int bugCount, privBugCount; // the replacement for get/setBugCount ?

	/*
	 * Missing get/setGridHeight get/setGridWidth get/setBugCount
	 */
	public NDoubleGrid2D valgrid; // Instead of DoubleGrid2D
	public NDoubleGrid2D valgrid2; // Instead of DoubleGrid2D
	public NObjectsGrid2D<DHeatBug> bugs; // Instead of SparseGrid2D

	// Same getters and setters as HeatBugs
	public double getMinimumIdealTemperature() {
		return minIdealTemp;
	}

	public void setMinimumIdealTemperature(double temp) {
		if (temp <= maxIdealTemp)
			minIdealTemp = temp;
	}

	public double getMaximumIdealTemperature() {
		return maxIdealTemp;
	}

	public void setMaximumIdealTemperature(double temp) {
		if (temp >= minIdealTemp)
			maxIdealTemp = temp;
	}

	public double getMinimumOutputHeat() {
		return minOutputHeat;
	}

	public void setMinimumOutputHeat(double temp) {
		if (temp <= maxOutputHeat)
			minOutputHeat = temp;
	}

	public double getMaximumOutputHeat() {
		return maxOutputHeat;
	}

	public void setMaximumOutputHeat(double temp) {
		if (temp >= minOutputHeat)
			maxOutputHeat = temp;
	}

	public double getEvaporationConstant() {
		return evaporationRate;
	}

	public void setEvaporationConstant(double temp) {
		if (temp >= 0 && temp <= 1)
			evaporationRate = temp;
	}

	public Object domEvaporationConstant() {
		return new Interval(0.0, 1.0);
	}

	public double getDiffusionConstant() {
		return diffusionRate;
	}

	public void setDiffusionConstant(double temp) {
		if (temp >= 0 && temp <= 1)
			diffusionRate = temp;
	}

	public Object domDiffusionConstant() {
		return new Interval(0.0, 1.0);
	}

	// Missing getBugXPos, getBugYPos

	// public void setRandomMovementProbability( double t ) {
	// if (t >= 0 && t <= 1) {
	// randomMovementProbability = t;
	// for ( int i = 0 ; i < bugCount ; i++ )
	// if (bugs[i] != null)
	// bugs[i].setRandomMovementProbability( randomMovementProbability );
	// }
	// }
	public Object domRandomMovementProbability() {
		return new Interval(0.0, 1.0);
	}

	public double getRandomMovementProbability() {
		return randomMovementProbability;
	}

	public double getMaximumHeat() {
		return MAX_HEAT;
	}

	public DHeatBugs(long seed) {
		this(seed, 1000, 1000, 1000, 5);
	}

	public DHeatBugs(long seed, int width, int height, int count, int aoi) {
		super(seed, width, height, aoi);
		gridWidth = width;
		gridHeight = height;
		bugCount = count;

		try {
			// DNonUniformPartition ps = DNonUniformPartition.getPartitionScheme(new int[]
			// {width, height}, true, this.aoi);
			// assert ps.np == 4;
			// ps.insertPartition(new IntHyperRect(0, new IntPoint(0, 0), new IntPoint(100,
			// 100)));
			// ps.insertPartition(new IntHyperRect(1, new IntPoint(0, 100), new
			// IntPoint(100, 1000)));
			// ps.insertPartition(new IntHyperRect(2, new IntPoint(100, 0), new
			// IntPoint(1000, 100)));
			// ps.insertPartition(new IntHyperRect(3, new IntPoint(100, 100), new
			// IntPoint(1000, 1000)));
			// ps.commit();

			createGrids();
			privBugCount = bugCount / partition.numProcessors;

			// lb = new LoadBalancer(this.aoi, 100);
		} catch (Exception e) {
			e.printStackTrace(System.out);
			System.exit(-1);
		}

		// myPart = p.getPartition();
	}

	protected void createGrids() {
		valgrid = new NDoubleGrid2D(partition, aoi, 0);
		valgrid2 = new NDoubleGrid2D(partition, aoi, 0);
		bugs = new NObjectsGrid2D<DHeatBug>(partition, aoi);
	}

	public void start() {
		super.start();

		// TODO: properly init
		for (int i = 0; i < partition.numProcessors; i++) {
			RemoteProxy.Init(i);
		}
		syncFields();
		bugs.initRemote();
		valgrid.initRemote();
		valgrid2.initRemote();
		// /init

		int[] size = partition.getPartition().getSize();
		double rangeIdealTemp = maxIdealTemp - minIdealTemp;
		double rangeOutputHeat = maxOutputHeat - minOutputHeat;

		for (int x = 0; x < privBugCount; x++) { // privBugCount = bugCount / p.numProcessors;
			double idealTemp = random.nextDouble() * rangeIdealTemp + minIdealTemp;
			double heatOutput = random.nextDouble() * rangeOutputHeat + minOutputHeat;
			int px, py; // Why are we doing this? Relationship?
			do {
				px = random.nextInt(size[0]) + partition.getPartition().ul().getArray()[0];
				py = random.nextInt(size[1]) + partition.getPartition().ul().getArray()[1];
			} while (bugs.get(px, py) != null);
			DHeatBug b = new DHeatBug(idealTemp, heatOutput, randomMovementProbability, px, py);
			bugs.add(px, py, b);
			schedule.scheduleOnce(b, 1);
		}

		// Does this have to happen here? I guess.
		schedule.scheduleRepeating(Schedule.EPOCH, 2, new Diffuser(), 1);
//		schedule.scheduleRepeating(Schedule.EPOCH, 3, new Synchronizer(), 1);
		schedule.scheduleRepeating(Schedule.EPOCH, 4, new Balancer(), 1);
		schedule.scheduleRepeating(Schedule.EPOCH, 5, new Inspector(), 10);
	}

	// Missing availableProcessors() ; Is this really needed? Maybe not.

	// Includes three private classes
	// Synchronizer, Balancer, Inspector
	// Doesn't have stop()

//	private class Synchronizer implements Steppable {
//		private static final long serialVersionUID = 1;
//
//		public void step(SimState state) {
//			DHeatBugs hb = (DHeatBugs) state;
	// Here we have completed all the computation work - stop the timer
//			Timing.stop(Timing.LB_RUNTIME);
//
//			Timing.start(Timing.MPI_SYNC_OVERHEAD);
//
//			try {
//				hb.valgrid.sync();
//				hb.bugs.sync();
//				hb.transporter.sync();
//			} catch (Exception e) {
//				e.printStackTrace();
//				System.exit(-1);
//			}

//			for (Transportee transportee : hb.transporter.objectQueue) {
//				privBugCount++;
//				DHeatBug heatBug = (DHeatBug) transportee.wrappedObject;
//				bugs.add(heatBug.loc_x, heatBug.loc_y, heatBug);
//				schedule.scheduleOnce(heatBug, 1);
//			}
//			hb.transporter.objectQueue.clear();
//
//			Timing.stop(Timing.MPI_SYNC_OVERHEAD);
//		}
//	}

	public void addToLocation(Transportee transportee) {
		// Here we have completed all the computation work - stop the timer
		privBugCount++;
		DHeatBug heatBug = (DHeatBug) transportee.wrappedObject;
		bugs.add(heatBug.loc_x, heatBug.loc_y, heatBug);
	}

	public void syncFields() {
		Timing.stop(Timing.LB_RUNTIME);
		Timing.start(Timing.MPI_SYNC_OVERHEAD);
		try {
			valgrid.sync();
			bugs.sync();
			transporter.sync();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void postSync() {
		Timing.stop(Timing.MPI_SYNC_OVERHEAD);
	}

	private class Balancer implements Steppable {
		public void step(final SimState state) {
			DHeatBugs hb = (DHeatBugs) state;
			try {
				DQuadTreePartition ps = (DQuadTreePartition) hb.partition;
				Double runtime = Timing.get(Timing.LB_RUNTIME).getMovingAverage();
				Timing.start(Timing.LB_OVERHEAD);
				ps.balance(runtime, 0);
				Timing.stop(Timing.LB_OVERHEAD);

				// if (hb.lb.balance((int)hb.schedule.getSteps()) > 0) {
				// myPart = p.getPartition();
				// MPITest.execInOrder(x -> System.out.printf("[%d] Balanced at step %d new
				// Partition %s\n", x, hb.schedule.getSteps(), p.getPartition()), 500);
				// }
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}

	private class Inspector implements Steppable {
		public void step(final SimState state) {
			DHeatBugs hb = (DHeatBugs) state;
			// String s = String.format("PID %d Step %d Agent Count %d\n", hb.partition.pid,
			// hb.schedule.getSteps(), hb.queue.size());
			// state.logger.info(String.format("PID %d Step %d Agent Count %d\n", hb.p.pid,
			// hb.schedule.getSteps(), hb.privBugCount));
			// if (DNonUniformPartition.getPartitionScheme().getPid() == 0) {
			DSimState.logger.info(String.format("[%d][%d] Step Runtime: %g \tSync Runtime: %g \t LB Overhead: %g\n",
					hb.partition.getPid(), hb.schedule.getSteps(), Timing.get(Timing.LB_RUNTIME).getMovingAverage(),
					Timing.get(Timing.MPI_SYNC_OVERHEAD).getMovingAverage(),
					Timing.get(Timing.LB_OVERHEAD).getMovingAverage()));
			// }
			// for (Steppable i : hb.queue) {
			// DHeatBug a = (DHeatBug)i;
			// s += a.toString() + "\n";
			// }
			// System.out.print(s);
		}
	}

	public static void main(String[] args) throws MPIException {
		doLoopMPI(DHeatBugs.class, args);
		System.exit(0);
	}
}
