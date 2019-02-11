package sim.field;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import sim.util.*;

import mpi.*;

public class DNonUniformPartition extends DPartition {

	static DNonUniformPartition instance;

	AugmentedSegmentTree[] st;
	// TODO Use IntHyperRect for now, need to use something like generic or create separate files for int and double.
	Map<Integer, IntHyperRect> ps;

	final double epsilon = 0.0001;

	boolean isDirty = false;
	ArrayList<UpdateAction> updates;

	int[] aoi;

	protected DNonUniformPartition(int size[], boolean isToroidal, int[] aoi) {
		super(size, isToroidal);

		this.aoi = aoi;
		this.st = new AugmentedSegmentTree[nd];
		for (int i = 0; i < nd; i++)
			this.st[i] = new AugmentedSegmentTree(isToroidal);

		ps = new HashMap<Integer, IntHyperRect>();

		updates = new ArrayList<UpdateAction>();
	}

	// public static DNonUniformPartition getPartitionScheme(int size[]) {
	// 	if (instance == null)
	// 		instance = new DNonUniformPartition(size);

	// 	if (instance.isToroidal == true)
	// 		throw new IllegalArgumentException("DNonUniformPartition has already been initialized to be Toroidal");

	// 	return instance;
	// }

	public static DNonUniformPartition getPartitionScheme(int size[], boolean isToroidal, int[] aoi) {
		if (instance == null)
			instance = new DNonUniformPartition(size, isToroidal, aoi);

		if (instance.isToroidal != isToroidal)
			throw new IllegalArgumentException("DNonUniformPartition has already been initialized to be " + (instance.isToroidal ? "Toroidal" : "non-Toroidal"));

		return instance;
	}

	public static DNonUniformPartition getPartitionScheme() {
		if (instance == null)
			throw new IllegalArgumentException("DNonUniformPartition has not been initialized");

		return instance;
	}

	protected void setMPITopo() {
		// TODO Currently a LP holds one partition. need to add support for cases that a LP holds multiple partitions
		if (ps.size() != np)
			throw new IllegalArgumentException(String.format("The number of partitions (%d) must equal to the number of LPs (%d)", ps.size(), np));

		// Need to ensure that the order of the neighbors is the same with that in MPI
		// not a problem with NonUniformPartition
		// may be a problem for UniformPartition where the cartesian topo is used
		int[] ns = getNeighborIds();

		// Create a unweighted & undirected graph
		try {
			comm = MPI.COMM_WORLD.createDistGraphAdjacent(
			           ns,
			           ns,
			           new Info(),
			           false
			       );
		} catch (MPIException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		// TODO: re-map LPs to Partitions for optimal LP placement
	}

	// Try to divide the field into np grid-like partitions
	// dims represents how many processors you want to assign on each dimension
	// Any zero value in dims means you want the system to decide the best value
	// null dims means the system will compute the best values on all dimensions
	public void initUniformly(int[] dims) {
		int[] psize = new int[nd], coord = new int[nd];

		if (dims == null)
			dims = new int[nd];

		// Generate a nd mesh of np processors
		try {
			CartComm.createDims(np, dims);
		} catch (MPIException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		for (int i = 0; i < nd; i++)
			psize[i] = Math.round((float)size[i] / dims[i]);

		final int[] fdims = dims; // used by the lambda expr below
		int count = 0;
		IntHyperRect r = new IntHyperRect(dims);

		for (IntPoint p : r) {
			int[] ul = IntStream.range(0, nd).map(i -> p.c[i] * psize[i]).toArray();
			int[] br = IntStream.range(0, nd).map(i -> p.c[i] == fdims[i] - 1 ? size[i] : (p.c[i] + 1) * psize[i]).toArray();
			insertPartition(new IntHyperRect(count++, new IntPoint(ul), new IntPoint(br)));
		}
	}

	// Insert a partition into the DNonUniformPartition scheme
	public void insertPartition(IntHyperRect p) {
		updates.add(UpdateAction.insert(p));
		isDirty = true;
	}

	public void removePartition(final int pid) {
		updates.add(UpdateAction.remove(pid));
		isDirty = true;
	}

	public void updatePartition(IntHyperRect p) {
		updates.add(UpdateAction.update(p));
		isDirty = true;
	}

	public IntHyperRect getPartition() {
		return getPartition(this.pid);
	}

	public IntHyperRect getPartition(int pid) {
		IntHyperRect rect = ps.get(pid);

		if (rect == null)
			throw new IllegalArgumentException("PID " + pid + " has no corresponding partition");

		return rect;
	}

	// Stabbing query
	public int toPartitionId(final int[] c) {
		return toPartitionId(Arrays.stream(c).mapToDouble(x -> (double)x).toArray());
	}

	public int toPartitionId(NdPoint p) {
		return toPartitionId(p.getArrayInDouble());
	}

	public int toPartitionId(final double[] c) {
		Set<Integer> ret = st[0].toPartitions(c[0]);

		for (int i = 1; i < nd; i++)
			ret.retainAll(st[i].toPartitions(c[i]));

		if (ret.size() != 1)
			throw new IllegalArgumentException("Point " + Arrays.toString(c) + " belongs to multiple pids or no pid: " + ret);

		return ret.toArray(new Integer[0])[0];
	}

	// Range query
	public Set<Integer> coveredPartitionIds(final int[] ul, final int[] br) {
		return coveredPartitionIds(Arrays.stream(ul).mapToDouble(x -> (double)x).toArray(),
		                           Arrays.stream(br).mapToDouble(x -> (double)x).toArray());
	}

	public Set<Integer> coveredPartitionIds(final double[] ul, final double[] br) {
		Set<Integer> ret = st[0].toPartitions(ul[0], br[0]);

		for (int i = 1; i < nd; i++)
			ret.retainAll(st[i].toPartitions(ul[i], br[i]));

		if (ret.size() < 1)
			throw new IllegalArgumentException("Rectangle <" + Arrays.toString(ul) + ", " + Arrays.toString(br) + "> covers no pid: " + ret);

		return ret;
	}

	public int[] getNeighborIds(int id) {
		IntHyperRect rect = getPartition(id).resize(aoi);

		// // TODO Better way?
		// // Expanded all dimensions by epsilon
		// double[] exp_ul = Arrays.stream(rect.ul().getArray()).mapToDouble(x -> (double)x - epsilon).toArray();
		// double[] exp_br = Arrays.stream(rect.br().getArray()).mapToDouble(x -> (double)x + epsilon).toArray();

		// Remove self
		return coveredPartitionIds(rect.ul().c, rect.br().c).stream()
		       .filter(i -> i != id).mapToInt(i -> i).toArray();
	}

	public int[] getNeighborIds() {
		return getNeighborIds(pid);
	}

	public int getNumNeighbors() {
		int nc = 0;

		try {
			nc = ((GraphComm)comm).getDistGraphNeighbors().getOutDegree();
		} catch (MPIException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		return nc;
	}

	// Get neighbor ids on each dimension (backward first, then forward)
	public int[][] getNeighborIdsInOrder() {
		int[][] ret = new int[nd * 2][];

		for (int i = 0; i < nd * 2; i++) {
			final int curr_dim = i / 2, dir = i % 2 - 1;
			ret[i] = Arrays.stream(getNeighborIdsShift(curr_dim, dir))
			         .mapToObj(x -> ps.get(x).reduceDim(curr_dim)).sorted()
			         .mapToInt(x -> x.getId()).toArray();
		}

		return ret;
	}

	// Get the neighbor id specified by dimension and direction (forward >=0 / backward < 0)
	public int[] getNeighborIdsShift(int dim, int dir) {
		IntHyperRect rect = getPartition();

		// double[] exp_ul = Arrays.stream(rect.ul().getArray()).mapToDouble(x -> (double)x).toArray();
		// double[] exp_br = Arrays.stream(rect.br().getArray()).mapToDouble(x -> (double)x).toArray();
		double[] exp_ul = rect.ul().getArrayInDouble();
		double[] exp_br = rect.br().getArrayInDouble();

		// if (dir >= 0)
		// 	exp_br[dim] += epsilon;
		// else
		// 	exp_ul[dim] -= epsilon;
		if (dir >= 0)
			exp_br[dim] += aoi[dim];
		else
			exp_ul[dim] -= aoi[dim];

		return coveredPartitionIds(exp_ul, exp_br).stream()
		       .filter(i -> i != pid).mapToInt(i -> i).toArray();
	}

	protected void applyUpdates() {
		for (UpdateAction u : updates)
			switch (u.action) {
			case INSERT:
				if (ps.containsKey(u.pid))
					throw new IllegalArgumentException("The partition id " + u.pid + " to be inserted already exists");

				for (int i = 0; i < nd; i++)
					st[i].insert(u.rect.getSegment(i));
				ps.put(u.pid, u.rect);

				break;
			case REMOVE:
				if (!ps.containsKey(u.pid))
					throw new IllegalArgumentException("The partition id " + u.pid + " to be removed does not exist");

				for (int i = 0; i < nd; i++)
					st[i].delete(u.pid);
				ps.remove(u.pid);

				break;
			case UPDATE:
				if (!ps.containsKey(u.pid))
					throw new IllegalArgumentException("The partition id " + u.pid + " to be updated does not exist");
				for (int i = 0; i < nd; i++) {
					st[i].delete(u.pid);
					st[i].insert(u.rect.getSegment(i));
				}
				ps.replace(u.pid, u.rect);

				break;
			}

		updates.clear();
	}

	// TODO add flags to avoid certain pre/post callbacks
	public int commit() {
		if (!isDirty)
			return 0;

		for (Consumer r : preCallbacks)
			r.accept(null);

		int count = updates.size();

		applyUpdates();
		setMPITopo();
		isDirty = false;

		for (Consumer r : postCallbacks)
			r.accept(null);

		return count;
	}

	public void abort() {
		updates.clear();
		isDirty = false;
	}

	public static void main(String args[]) throws MPIException, InterruptedException {
		MPI.Init(args);

		testNonUniform();

		testNonUniformToroidal();

		// testInitUniformly();

		testUpdate();

		MPI.Finalize();
	}

	public static void testUpdate() throws MPIException, InterruptedException {
		DNonUniformPartition p = new DNonUniformPartition(new int[] {10, 20}, false, new int[] {1, 1});
		assert p.np == 5;

		p.initUniformly(new int[] {0, 0});
		p.commit();

		MPITest.execInOrder(x -> {
			System.out.println("[Before] PID " + p.pid + " Partition " + p.getPartition());
			System.out.println("[Before] PID " + p.pid + " Neighbors: " + Arrays.toString(p.getNeighborIds()));
		}, 500);

		p.updatePartition(new IntHyperRect(0, new IntPoint(new int[] {0, 0}), new IntPoint(new int[] {3, 12})));
		p.updatePartition(new IntHyperRect(1, new IntPoint(new int[] {0, 12}), new IntPoint(new int[] {7, 20})));
		p.updatePartition(new IntHyperRect(2, new IntPoint(new int[] {7, 8}), new IntPoint(new int[] {10, 20})));
		p.updatePartition(new IntHyperRect(3, new IntPoint(new int[] {3, 0}), new IntPoint(new int[] {10, 8})));
		p.updatePartition(new IntHyperRect(4, new IntPoint(new int[] {3, 8}), new IntPoint(new int[] {7, 12})));
		p.commit();

		MPITest.execInOrder(x -> {
			System.out.println("[After] PID " + p.pid + " Partition " + p.getPartition());
			System.out.println("[After] PID " + p.pid + " Neighbors: " + Arrays.toString(p.getNeighborIds()));
		}, 500);
	}

	public static void testInitUniformly() {
		DNonUniformPartition p = new DNonUniformPartition(new int[] {12, 24}, false, new int[] {1, 1});

		p.initUniformly(new int[] {0, 0});
		p.commit();

		MPITest.execInOrder(x -> {
			System.out.println("[Before] PID " + p.pid + " Partition " + p.getPartition());
			System.out.println("[Before] PID " + p.pid + " Neighbors: " + Arrays.toString(p.getNeighborIds()));
		}, 500);
	}

	public static void testNonUniformToroidal() throws MPIException, InterruptedException {
		DNonUniformPartition p = new DNonUniformPartition(new int[] {10, 20}, true, new int[] {1, 1});
		assert p.np == 5;

		/**
		* Create the following partition scheme
		*
		*	 0		8		12			20
		*	0 ---------------------------
		*	  |		0		|			|
		*	3 |-------------|	  1		|
		*	  |		|	4	|			|
		*	7 |	 3	|-------------------|
		*	  |		|		 2			|
		*  10 ---------------------------
		*
		**/

		p.insertPartition(new IntHyperRect(0, new IntPoint(new int[] {0, 0}), new IntPoint(new int[] {3, 12})));
		p.insertPartition(new IntHyperRect(1, new IntPoint(new int[] {0, 12}), new IntPoint(new int[] {7, 20})));
		p.insertPartition(new IntHyperRect(2, new IntPoint(new int[] {7, 8}), new IntPoint(new int[] {10, 20})));
		p.insertPartition(new IntHyperRect(3, new IntPoint(new int[] {3, 0}), new IntPoint(new int[] {10, 8})));
		p.insertPartition(new IntHyperRect(4, new IntPoint(new int[] {3, 8}), new IntPoint(new int[] {7, 12})));
		p.commit();

		double[] c, c1, c2;

		if (p.pid == 0) {

			c = new double[] {0, 0};
			System.out.println("Point " + Arrays.toString(c) + " belongs to pid " + p.toPartitionId(c));

			c = new double[] { -1, -1};
			System.out.println("Point " + Arrays.toString(c) + " belongs to pid " + p.toPartitionId(c));

			c = new double[] {14, 21};
			System.out.println("Point " + Arrays.toString(c) + " belongs to pid " + p.toPartitionId(c));

			c = new double[] {7, 24};
			System.out.println("Point " + Arrays.toString(c) + " belongs to pid " + p.toPartitionId(c));

			c = new double[] {27, 45};
			System.out.println("Point " + Arrays.toString(c) + " belongs to pid " + p.toPartitionId(c));

			c1 = new double[] {7, 8};
			c2 = new double[] {11, 20};
			System.out.println("Rectangle <" + Arrays.toString(c1) + ", " + Arrays.toString(c2) + "> covers pids: " +
			                   Arrays.toString(p.coveredPartitionIds(c1, c2).toArray()));

			c1 = new double[] {0, -1};
			c2 = new double[] {3, 12};
			System.out.println("Rectangle <" + Arrays.toString(c1) + ", " + Arrays.toString(c2) + "> covers pids: " +
			                   Arrays.toString(p.coveredPartitionIds(c1, c2).toArray()));

			c1 = new double[] { -1, -1};
			c2 = new double[] {4, 13};
			System.out.println("Rectangle <" + Arrays.toString(c1) + ", " + Arrays.toString(c2) + "> covers pids: " +
			                   Arrays.toString(p.coveredPartitionIds(c1, c2).toArray()));

			c1 = new double[] { -1, -2};
			c2 = new double[] {11, 2};
			System.out.println("Rectangle <" + Arrays.toString(c1) + ", " + Arrays.toString(c2) + "> covers pids: " +
			                   Arrays.toString(p.coveredPartitionIds(c1, c2).toArray()));

			c1 = new double[] { -1, -2};
			c2 = new double[] {2, 22};
			System.out.println("Rectangle <" + Arrays.toString(c1) + ", " + Arrays.toString(c2) + "> covers pids: " +
			                   Arrays.toString(p.coveredPartitionIds(c1, c2).toArray()));
		}

		MPITest.execInOrder(x -> {
			System.out.println("PID " + p.pid + " Neighbors: " + Arrays.toString(p.getNeighborIds()));
		}, 500);
	}

	public static void testNonUniform() throws MPIException {
		DNonUniformPartition p = new DNonUniformPartition(new int[] {10, 20}, false, new int[] {1, 1});
		assert p.np == 5;

		/**
		* Create the following partition scheme
		*
		*	 0		8		12			20
		*	0 ---------------------------
		*	  |		0		|			|
		*	3 |-------------|	  1		|
		*	  |		|	4	|			|
		*	7 |	 3	|-------------------|
		*	  |		|		 2			|
		*  10 ---------------------------
		*
		**/
		p.insertPartition(new IntHyperRect(0, new IntPoint(new int[] {0, 0}), new IntPoint(new int[] {3, 12})));
		p.insertPartition(new IntHyperRect(1, new IntPoint(new int[] {0, 12}), new IntPoint(new int[] {7, 20})));
		p.insertPartition(new IntHyperRect(2, new IntPoint(new int[] {7, 8}), new IntPoint(new int[] {10, 20})));
		p.insertPartition(new IntHyperRect(3, new IntPoint(new int[] {3, 0}), new IntPoint(new int[] {10, 8})));
		p.insertPartition(new IntHyperRect(4, new IntPoint(new int[] {3, 8}), new IntPoint(new int[] {7, 12})));
		p.commit();

		double[] c, c1, c2;

		if (p.pid == 0) {

			c = new double[] {0, 0};
			System.out.println("Point " + Arrays.toString(c) + " belongs to pid " + p.toPartitionId(c));

			c = new double[] {4, 9};
			System.out.println("Point " + Arrays.toString(c) + " belongs to pid " + p.toPartitionId(c));

			c = new double[] {4, 12};
			System.out.println("Point " + Arrays.toString(c) + " belongs to pid " + p.toPartitionId(c));

			c = new double[] {7, 8};
			System.out.println("Point " + Arrays.toString(c) + " belongs to pid " + p.toPartitionId(c));

			c = new double[] {7, 5};
			System.out.println("Point " + Arrays.toString(c) + " belongs to pid " + p.toPartitionId(c));

			c1 = new double[] {3, 8};
			c2 = new double[] {7, 12};
			System.out.println("Rectangle <" + Arrays.toString(c1) + ", " + Arrays.toString(c2) + "> covers pids: " +
			                   Arrays.toString(p.coveredPartitionIds(c1, c2).toArray()));

			c1 = new double[] {3, 8};
			c2 = new double[] {8, 12};
			System.out.println("Rectangle <" + Arrays.toString(c1) + ", " + Arrays.toString(c2) + "> covers pids: " +
			                   Arrays.toString(p.coveredPartitionIds(c1, c2).toArray()));

			c1 = new double[] {3, 8};
			c2 = new double[] {7, 20};
			System.out.println("Rectangle <" + Arrays.toString(c1) + ", " + Arrays.toString(c2) + "> covers pids: " +
			                   Arrays.toString(p.coveredPartitionIds(c1, c2).toArray()));

			c1 = new double[] {2, 8};
			c2 = new double[] {7, 20};
			System.out.println("Rectangle <" + Arrays.toString(c1) + ", " + Arrays.toString(c2) + "> covers pids: " +
			                   Arrays.toString(p.coveredPartitionIds(c1, c2).toArray()));

			c1 = new double[] {2, 7};
			c2 = new double[] {8, 20};
			System.out.println("Rectangle <" + Arrays.toString(c1) + ", " + Arrays.toString(c2) + "> covers pids: " +
			                   Arrays.toString(p.coveredPartitionIds(c1, c2).toArray()));
		}

		MPITest.execInOrder(x -> {
			System.out.println("PID " + p.pid + " Neighbors: " + Arrays.toString(p.getNeighborIds()));
			System.out.println("PID " + p.pid + " Neighbors in order: " + Arrays.toString(Arrays.stream(p.getNeighborIdsInOrder()).flatMapToInt(Arrays::stream).toArray()));
		}, 500);

		GraphComm gc = (GraphComm)p.comm;
		DistGraphNeighbors nsobj = gc.getDistGraphNeighbors();
		int[] ns = new int[nsobj.getOutDegree()];
		for (int i = 0; i < nsobj.getOutDegree(); i++)
			ns[i] = nsobj.getDestination(i);

		MPITest.execInOrder(x -> {
			System.out.println("PID " + p.pid + " MPI Neighbors: " + Arrays.toString(ns));
		}, 500);
	}
}

class AugmentedSegmentTree extends SegmentTree {

	public AugmentedSegmentTree(boolean isToroidal) {
		super(isToroidal);
	}

	public Set<Integer> toPartitions(int target) {
		List<Segment> res = contains((double)target);
		Set<Integer> s = new HashSet<Integer>();
		res.forEach(seg -> s.add(seg.pid));

		return s;
	}

	public Set<Integer> toPartitions(double target) {
		List<Segment> res = contains(target);
		Set<Integer> s = new HashSet<Integer>();
		res.forEach(seg -> s.add(seg.pid));

		return s;
	}

	public Set<Integer> toPartitions(int st, int ed) {
		List<Segment> res = intersect((double)st, (double)ed);
		Set<Integer> s = new HashSet<Integer>();
		res.forEach(seg -> s.add(seg.pid));

		return s;
	}

	public Set<Integer> toPartitions(double st, double ed) {
		List<Segment> res = intersect(st, ed);
		Set<Integer> s = new HashSet<Integer>();
		res.forEach(seg -> s.add(seg.pid));

		return s;
	}
}


enum Action {
	INSERT, REMOVE, UPDATE;
}

class UpdateAction {

	public IntHyperRect rect;
	public int pid;
	public Action action;

	private UpdateAction(IntHyperRect rect, int pid, Action action) {
		this.rect = rect;
		this.pid = pid;
		this.action = action;
	}

	public static UpdateAction insert(IntHyperRect r) {
		return new UpdateAction(r, r.getId(), Action.INSERT);
	}

	public static UpdateAction remove(int id) {
		return new UpdateAction(null, id, Action.REMOVE);
	}

	public static UpdateAction update(IntHyperRect r) {
		return new UpdateAction(r, r.getId(), Action.UPDATE);
	}
}
