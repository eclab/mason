/*
   Copyright 2006 by Sean Luke and George Mason University
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package sim.app.dheatbugs;

import java.util.ArrayList;
import java.util.HashMap;

import mpi.MPIException;
import sim.app.dflockers.DFlocker;
import sim.app.dflockers.DFlockers;
import sim.engine.DSimState;
import sim.engine.Schedule;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.grid.DDoubleGrid2D;
import sim.field.grid.DDenseGrid2D;
import sim.field.partitioning.QuadTreePartition;
import sim.field.partitioning.DoublePoint;
import sim.field.partitioning.IntPoint;
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
		valgrid = new DDoubleGrid2D(getPartitioning(), this.aoi, 0, this);
		valgrid2 = new DDoubleGrid2D(getPartitioning(), this.aoi, 0, this);
		bugs = new DDenseGrid2D<DHeatBug>(getPartitioning(), this.aoi, this);

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
		return DHeatBugs.MAX_HEAT;
	}
	
	
	@Override
	protected HashMap<String, Object>[] startRoot() {
		HashMap<IntPoint,ArrayList<DHeatBug>> agents = new HashMap<IntPoint, ArrayList<DHeatBug>>();
		final double rangeIdealTemp = maxIdealTemp - minIdealTemp;
		final double rangeOutputHeat = maxOutputHeat - minOutputHeat;
		for (int x = 0; x < bugCount; x++) { 
			final double idealTemp = random.nextDouble() * rangeIdealTemp + minIdealTemp;
			final double heatOutput = random.nextDouble() * rangeOutputHeat + minOutputHeat;
			int px = random.nextInt(gridWidth);
			int	py = random.nextInt(gridHeight);
			final DHeatBug b = new DHeatBug(idealTemp, heatOutput, randomMovementProbability, px, py);
			IntPoint point = new IntPoint(px, py);
			if(!agents.containsKey(point))
				agents.put(point, new ArrayList<DHeatBug>());
			agents.get(point).add(b);
		}
		
		HashMap<String,Object>[] sendObjs = new HashMap[partition.getNumProc()];
		for(int i = 0;i<partition.getNumProc();i++) {
			sendObjs[i] = new HashMap<String, Object>();
			sendObjs[i].put("agents", agents);
		}
		
		return sendObjs;
	}

	public void start() {
		super.start();


		HashMap<IntPoint,ArrayList<DHeatBug>>agents = (HashMap<IntPoint,ArrayList<DHeatBug>>) rootInfo.get("agents");
		
		for(IntPoint p : agents.keySet()) {
			for (DHeatBug a : agents.get(p)) {
				if(partition.getPartition().contains(p))
					bugs.addAgent(p, a );
			}
			
			
		}
		
		//bugs.globalAgentsInitialization(agents);
		

		// Does this have to happen here? I guess.
		schedule.scheduleRepeating(Schedule.EPOCH, 2, new Diffuser(), 1);

		// TODO: Balancer is broken,
		// the items on the edge find themselves in the wrong pId

//		schedule.scheduleRepeating(Schedule.EPOCH, 4, new Balancer(), 1);
//		schedule.scheduleRepeating(Schedule.EPOCH, 5, new Inspector(), 10);
	}

	@SuppressWarnings("serial")
	class Balancer implements Steppable {
		public void step(final SimState state) {
			final DHeatBugs hb = (DHeatBugs) state;
			try {
				final QuadTreePartition ps = (QuadTreePartition) hb.getPartitioning();
				final Double runtime = Timing.get(Timing.LB_RUNTIME).getMovingAverage();
				Timing.start(Timing.LB_OVERHEAD);
				ps.balance(runtime, 0);
				Timing.stop(Timing.LB_OVERHEAD);

				// if (hb.lb.balance((int)hb.schedule.getSteps()) > 0) {
				// myPart = p.getPartition();
				// MPITest.execInOrder(x -> System.out.printf("[%d] Balanced at step %d new
				// Partition %s\n", x, hb.schedule.getSteps(), p.getPartition()), 500);
				// }
			} catch (final Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}

	@SuppressWarnings("serial")
	class Inspector implements Steppable {
		public void step(final SimState state) {
			final DHeatBugs hb = (DHeatBugs) state;
			// String s = String.format("PID %d Step %d Agent Count %d\n", hb.partition.pid,
			// hb.schedule.getSteps(), hb.queue.size());
			// state.logger.info(String.format("PID %d Step %d Agent Count %d\n", hb.p.pid,
			// hb.schedule.getSteps(), hb.privBugCount));
			// if (DNonUniformPartition.getPartitionScheme().getPid() == 0) {
			DSimState.logger.info(String.format("[%d][%d] Step Runtime: %g \tSync Runtime: %g \t LB Overhead: %g\n",
					hb.getPartitioning().getPid(), hb.schedule.getSteps(),
					Timing.get(Timing.LB_RUNTIME).getMovingAverage(),
					Timing.get(Timing.MPI_SYNC_OVERHEAD).getMovingAverage(),
					Timing.get(Timing.LB_OVERHEAD).getMovingAverage()));
			// }
			// for (Stopping i : hb.queue) {
			// DHeatBug a = (DHeatBug)i;
			// s += a.toString() + "\n";
			// }
			// System.out.print(s);
		}
	}

	public static void main(final String[] args) throws MPIException {
		doLoopMPI(DHeatBugs.class, args);
		System.exit(0);
	}
}