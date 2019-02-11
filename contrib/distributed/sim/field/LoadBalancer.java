package sim.field;

import java.io.*;
import java.util.*;
import java.util.stream.IntStream;

import sim.util.IntHyperRect;
import sim.util.GraphColoring;
import sim.util.MPITest;
import sim.util.Timing;
import sim.util.MPIUtil;
import sim.field.grid.NDoubleGrid2D;

import mpi.*;

public class LoadBalancer {

	DNonUniformPartition p;
	GraphColoring gc;
	int interval;

	//Comm comm;

	// Offsets per adjustment on each dimension
	int[] offsets;

	public LoadBalancer(int[] offsets, int interval) {
		this.p = DNonUniformPartition.getPartitionScheme();
		this.offsets = offsets;
		this.interval = interval;
		this.gc = new GraphColoring(p);

		reload();

		p.registerPostCommit(arg -> {
			gc.color();
		});
	}

	private void reload() {
		gc.color();
		// try {
		// 	comm = MPI.COMM_WORLD.split(gc.myColor, p.getPid());
		// } catch (MPIException e) {
		// 	e.printStackTrace();
		// 	System.exit(-1);
		// }
	}

	// Get all the neighbor ids and then
	// filter out those who don't align with this partition
	// Return the pids grouped by the dimension
	private int[][] getAvailNeighborIds() {
		int[][] ret = new int[p.nd][];
		IntHyperRect self = p.getPartition();

		for (int d = 0; d < p.nd; d++) {
			final int fd = d;
			IntStream s = IntStream.concat(self.br().getArray()[d] == p.size[d] ? IntStream.empty() : Arrays.stream(p.getNeighborIdsShift(d, 0)),
			                               self.ul().getArray()[d] == 0 ? IntStream.empty() : Arrays.stream(p.getNeighborIdsShift(d, -1)));
			ret[d] = s.filter(i -> p.getPartition(i).isAligned(self, fd)).toArray();
		}

		return ret;
	}

	private boolean shouldBalance(int step) {
		if (interval < 0)
			return false;

		return step % (gc.numColors + interval) < gc.numColors;
	}

	private boolean isMyTurn(int step) {
		return step % (gc.numColors + interval) == gc.myColor;
	}

	private BalanceAction generateAction(int step, HashMap<Integer, Double> rts, double overhead) {
		int mdim = 0, mdir = 0, offset = 0;
		int[] mdst = new int[] {p.getPid()};;
		double maxDelta = 0;

		double myrt = rts.get(p.getPid());
		IntHyperRect myPart = p.getPartition();
		int[] size = myPart.getSize();

		BalanceAction myAction = BalanceAction.idle();

		if (!isMyTurn(step))
			return myAction;

		for (int dim = 0; dim < p.nd; dim++) {
			for (int dir : new int[] { -1, 1}) {
				if (dir == 1 && myPart.br().getArray()[dim] == p.size[dim] || dir == -1 && myPart.ul().getArray()[dim] == 0)
					continue; // skip the field boundaries
				int[] nids = p.getNeighborIdsShift(dim, dir);

				// Check the balance option with individual neighbors
				for (int nid : nids) {
					if (!p.getPartition(nid).isAligned(myPart, dim))
						continue; // skip any single neighbor that doesn't align with me
					double delta = (myrt - rts.get(nid)) * offsets[dim] / size[dim];
					if (Math.abs(delta) > maxDelta) {
						maxDelta = Math.abs(delta);
						mdim = dim;
						mdir = dir;
						mdst = new int[] {nid};
						offset = delta > 0 ? -offsets[dim] : offsets[dim];
					}
				}

				// Check the balance option with the group of the neighbors
				final int currDim = dim;
				IntHyperRect[] group = Arrays.stream(nids).mapToObj(i -> p.getPartition(i).reduceDim(currDim)).toArray(s -> new IntHyperRect[s]);
				IntHyperRect bbox = IntHyperRect.getBoundingRect(group);
				if (group.length == 0 || !bbox.equals(myPart.reduceDim(dim)))
					continue; // skip if the group of the neighbors doesn't align with me
				double avgRt = Arrays.stream(nids).mapToDouble(i -> rts.get(i)).sum() / nids.length;
				double delta = (myrt - avgRt) * offsets[dim] / size[dim];
				if (Math.abs(delta) > maxDelta) {
					maxDelta = Math.abs(delta);
					mdim = dim;
					mdir = dir;
					mdst = nids;
					offset = delta > 0 ? -offsets[dim] : offsets[dim];
				}
			}
		}

		// balance only if the delta is large enough
		if (maxDelta * (gc.numColors + interval) > overhead)
			myAction = new BalanceAction(p.getPid(), mdst, mdim, mdir, offset);

		return myAction;
	}

	private int doBalance(int step, double myrt, double overhead) throws MPIException, IOException {
		// Collect runtimes from the neighbors
		HashMap<Integer, Double> rts = new HashMap<Integer, Double>();
		int[] neighbors = p.getNeighborIds();
		double[] recvBuf = (double[])MPIUtil.neighborAllGather(p, myrt, MPI.DOUBLE);
		IntStream.range(0, neighbors.length).forEach(i -> rts.put(neighbors[i], recvBuf[i]));
		rts.put(p.getPid(), myrt);

		BalanceAction myAction = generateAction(step, rts, overhead);

		ArrayList<BalanceAction> actions = MPIUtil.<BalanceAction>allGather(p, myAction);

		// Apply the actions to the partition
		// Abort all the actions if there any illegal ones
		for (BalanceAction a : actions)
			try {
				a.applyToPartition(p);
			} catch (IllegalArgumentException e) {
				p.abort();
				System.err.println("Illegal partition adjustment: " + a + " - all partition change aborted...");
				break;
			}

		return p.commit();
	}

	public int balance(int step) throws MPIException, IOException {
		if (!shouldBalance(step))
			return 0;

		double runtime;
		try {
			runtime = Timing.get(Timing.LB_RUNTIME).getMovingAverage();
		} catch (NoSuchElementException e) {
			return 0;
		}

		Timing.start(Timing.LB_OVERHEAD);

		int count = doBalance(
		                step,
		                runtime,
		                //Timing.get(Timing.LB_OVERHEAD).getMovingAverage()
		                0.0
		            );

		Timing.stop(Timing.LB_OVERHEAD);

		return count;
	}

	public static void main(String args[]) throws MPIException, InterruptedException, IOException {
		int[] size = new int[] {10, 10};
		int[] aoi = new int[] {1, 1};

		MPI.Init(args);

		DNonUniformPartition p = DNonUniformPartition.getPartitionScheme(size, true, aoi);
		assert p.np == 4;
		p.initUniformly(null);
		p.commit();

		int pid = p.getPid();
		NDoubleGrid2D f = new NDoubleGrid2D(p, aoi, pid);
		f.sync();

		MPITest.execOnlyIn(0, i -> System.out.println("Initial field"));
		MPITest.execInOrder(i -> System.out.println(f), 500);

		LoadBalancer lb = new LoadBalancer(aoi, 0);

		double[] rts = new double[] {100, 100, 100, 100};

		final int res1 = lb.doBalance(0, rts[pid], 10);
		MPITest.execOnlyIn(0, i -> System.out.println("Load Balancing #1: " + res1));
		MPITest.execInOrder(i -> System.out.println(f), 500);

		rts[0] = 1100.0;
		rts[2] = 100.0;

		final int res2 = lb.doBalance(1, rts[pid], 10);
		MPITest.execOnlyIn(0, i -> System.out.println("Load Balancing #2: " + res2));
		MPITest.execInOrder(i -> System.out.println(f), 500);

		final int res3 = lb.doBalance(2, rts[pid], 400);
		MPITest.execOnlyIn(0, i -> System.out.println("Load Balancing #3: " + res3));
		MPITest.execInOrder(i -> System.out.println(f), 500);

		rts[2] = 10.0;

		final int res4 = lb.doBalance(3, rts[pid], 71);
		MPITest.execOnlyIn(0, i -> System.out.println("Load Balancing #4: " + res4));
		MPITest.execInOrder(i -> System.out.println(f), 500);

		MPI.Finalize();
	}
}
