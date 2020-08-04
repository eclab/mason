package sim.field;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

import mpi.Comm;
import mpi.Info;
import mpi.MPI;
import mpi.MPIException;
import sim.util.DoublePoint;
import sim.util.GroupComm;
import sim.util.IntHyperRect;
import sim.util.IntPoint;
import sim.util.MPITest;
import sim.util.MPIUtil;
import sim.util.NdPoint;
import sim.util.QTNode;
import sim.util.QuadTree;

public class DQuadTreePartition extends DPartition {
	QuadTree qt;
	QTNode myLeafNode; // the leaf node that this pid is mapped to
	Map<Integer, GroupComm> groups; // Map the level to its corresponding comm group

	public DQuadTreePartition(final int[] size, final boolean isToroidal, final int[] aoi) {
		super(size, isToroidal, aoi);
		qt = new QuadTree(new IntHyperRect(size), numProcessors);
	}

	public IntHyperRect getPartition() {
		return myLeafNode.getShape();
	}

	public IntHyperRect getPartition(final int pid) {
		for (final QTNode node : qt.getAllLeaves())
			if (node.getProc() == pid)
				return node.getShape();

		throw new IllegalArgumentException("The partition for " + pid + " does not exist");
	}

	public int getNumNeighbors() {
		return getNeighborIds().length;
	}

	public int[] getNeighborIds() {
		return qt.getNeighborPids(myLeafNode, aoi);
	}

	public int toPartitionId(final NdPoint p) {
		return qt.getLeafNode(p).getProc();
	}

	public int toPartitionId(final int[] c) {
		return toPartitionId(new IntPoint(c));
	}

	public int toPartitionId(final double[] c) {
		return toPartitionId(new DoublePoint(c));
	}

	protected void createMPITopo() {
		final int[] ns = getNeighborIds();

		try {
			// Create a unweighted & undirected graph for neighbor communication
			comm = MPI.COMM_WORLD.createDistGraphAdjacent(ns, ns, new Info(), false);

			// Create the group comms for nodes at the same level (intercomm) and for nodes
			// and its all leaves (intracomm)
			createGroups();
		} catch (final MPIException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * This method is only used internally, for init use initialize instead
	 *
	 * @param splitPoints
	 */
	void initQuadTree(final List<IntPoint> splitPoints) {
		// Create the quad tree based on the given split points
		qt.split(splitPoints);

		// map all quad tree nodes to processors
		mapNodeToProc();
		createMPITopo();
	}

	public void initialize() {
		initUniformly();
	}

	/**
	 * This method is only used internally, for init use initialize instead
	 */
	void initUniformly() {
		// Init into a full quad tree

		// Check whether np is power of (2 * nd)
		// np's binary represention only contains a single one i.e.,
		// (np & (np - 1)) == 0
		// and the number of zeros before it is evenly divided by nd
		int nz = 0;
		while ((numProcessors >> nz & 0x1) != 0x1)
			nz++;

		if ((numProcessors & numProcessors - 1) != 0 || nz % numDimensions != 0)
			throw new IllegalArgumentException(
					"Currently only support the number processors that is power of " + (2 * numDimensions));

		for (int level = 0; level < nz / numDimensions; level++) {
			final List<QTNode> leaves = qt.getAllLeaves();
			for (final QTNode leaf : leaves)
				qt.split(leaf.getShape().getCenter());
		}
		mapNodeToProc();
		createMPITopo();
	}

	protected void mapNodeToProc() {
		final List<QTNode> leaves = qt.getAllLeaves();

		if (leaves.size() != numProcessors)
			throw new IllegalArgumentException("The number of leaves " + leaves.size()
					+ " does not equal to the number of processors " + numProcessors);

		// Map the leaf nodes first
		for (int i = 0; i < numProcessors; i++)
			leaves.get(i).setProc(i);

		// if pid == 0 then myLeafNode is root node
		myLeafNode = leaves.get(pid);

		// Map non-leaf nodes - Use the first children node to hold itself
		while (leaves.size() > 0) {
			final QTNode curr = leaves.remove(0);
			final QTNode parent = curr.getParent();
			if (parent == null || parent.getChild(0) != curr)
				continue;
			parent.setProc(curr.getProc());
			leaves.add(parent);
		}

		// Set the proc id to the IntHyperRect so it can be printed out when debugging
		// it is not used by the program itself (TODO double-check)
		for (final QTNode leaf : qt.getAllLeaves())
			leaf.getShape().setId(leaf.getProc());
	}

	protected void createGroups() throws MPIException {
		int currDepth = 0;
		groups = new HashMap<Integer, GroupComm>();

		// Iterate level by level to create groups
		List<QTNode> currLevel = new ArrayList<QTNode>();
		currLevel.add(qt.getRoot());
		while (currLevel.size() > 0) {
			final List<QTNode> nextLevel = new ArrayList<QTNode>();

			for (final QTNode node : currLevel) {
				nextLevel.addAll(node.getChildren());

				// whether this pid should participate in this group
				if (node.isAncestorOf(myLeafNode))
					groups.put(currDepth, new GroupComm(node));

				// Others will wait until the group is created
				MPI.COMM_WORLD.barrier();
			}

			final GroupComm gc = groups.get(currDepth);
			if (isGroupMaster(gc))
				gc.setInterComm(currLevel);

			MPI.COMM_WORLD.barrier();

			currLevel = nextLevel;
			currDepth++;
		}
	}

	/**
	 * @param gc
	 * @return whether the calling pid is the master node of the given GroupComm
	 */
	public boolean isGroupMaster(final GroupComm gc) {
		return gc != null && gc.master.getProc() == pid;
	}

	public boolean isGroupMaster(final int level) {
		return isGroupMaster(getGroupComm(level));
	}

	public boolean isGlobalMaster() {
		// The Global Master of Quad Tree is global root for MPI as well
		return isGroupMaster(0);
	}

	/**
	 * @param level
	 * @return the GroupComm instance if the calling pid should be involved in the
	 *         group communication of the given level <br>
	 *         null otherwise
	 */
	public GroupComm getGroupComm(final int level) {
		return groups.get(level);
	}

	/**
	 * @param level
	 * @return the shape when the calling pid holds one of the master nodes of this
	 *         level <br>
	 *         null otherwise
	 */
	public IntHyperRect getNodeShapeAtLevel(final int level) {
		final GroupComm gc = getGroupComm(level);
		if (isGroupMaster(gc))
			return gc.master.getShape();
		return null;
	}

	private void testIntraGroupComm(final int depth) throws MPIException {
		MPITest.printOnlyIn(0, "Testing intra group comm at depth " + depth);

		if (groups.containsKey(depth)) {
			final Comm gcomm = groups.get(depth).comm;
			final int[] buf = new int[16];

			buf[gcomm.getRank()] = pid;
			gcomm.allGather(buf, 1, MPI.INT);
			System.out.println(String.format("PID %2d %s", pid, Arrays.toString(buf)));
		}

		MPI.COMM_WORLD.barrier();
	}

	private void testInterGroupComm(final int depth) throws MPIException {
		MPITest.printOnlyIn(0, "Testing inter group comm at depth " + depth);

		final GroupComm gc = getGroupComm(depth);
		if (isGroupMaster(gc)) {
			final Comm gcomm = gc.interComm;
			final int[] buf = new int[16];

			buf[gcomm.getRank()] = pid;
			gcomm.allGather(buf, 1, MPI.INT);
			System.out.print(String.format("PID %2d %s\n", pid, Arrays.toString(buf)));
		}

		MPI.COMM_WORLD.barrier();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void balance(final double myRt, final int level) throws MPIException {
		final GroupComm gc = groups.get(level);

		Object[] sendCentroids = new Object[] { null };

		if (gc != null) {
			final IntPoint ctr = myLeafNode.getShape().getCenter();
			final double[] sendData = new double[ctr.getNd() + 1], recvData = new double[ctr.getNd() + 1];
			sendData[0] = myRt;
			for (int i = 1; i < sendData.length; i++)
				sendData[i] = ctr.c[i - 1] * myRt;

			gc.comm.reduce(sendData, recvData, recvData.length, MPI.DOUBLE, MPI.SUM, gc.groupRoot);

			if (isGroupMaster(gc))
				sendCentroids = new Object[] { gc.master.getId(),
						new IntPoint(Arrays.stream(recvData).skip(1).mapToInt(x -> (int) (x / recvData[0])).toArray())
				};
		}

		// broadcast to all nodes
		final ArrayList<Object[]> newCentroids = MPIUtil.<Object[]>allGather(MPI.COMM_WORLD, sendCentroids);

		// call precommit
		for (final Consumer r : preCallbacks)
			r.accept(level);

		// apply changes
		for (final Object[] obj : newCentroids)
			if (obj[0] != null)
				qt.moveOrigin((int) obj[0], (IntPoint) obj[1]);

		// Assigns new neighbors after balancing
		// Recreates MPI topology based on that
		createMPITopo();

		// call postcommit
		for (final Consumer r : postCallbacks)
			r.accept(level);
	}

	private static void testBalance() throws MPIException {
		MPITest.printOnlyIn(0, "Testing balance()......");

		final DQuadTreePartition p = new DQuadTreePartition(new int[] { 100, 100 }, false, new int[] { 1, 1 });

		final IntPoint[] splitPoints = new IntPoint[] {
				new IntPoint(50, 50),
				new IntPoint(25, 25),
				new IntPoint(75, 75),
				new IntPoint(60, 90),
				new IntPoint(10, 10)
		};

		p.initQuadTree(Arrays.asList(splitPoints));

		final Random rand = new Random();
		final double myRt = rand.nextDouble() * 10;

		p.balance(myRt, 0);

		MPITest.printOnlyIn(0, p.qt.toString());
	}

	private static void testInitWithPoints() throws MPIException {
		MPITest.printOnlyIn(0, "Testing init with points......");

		final DQuadTreePartition p = new DQuadTreePartition(new int[] { 100, 100 }, false, new int[] { 1, 1 });

		final IntPoint[] splitPoints = new IntPoint[] {
				new IntPoint(50, 50),
				new IntPoint(25, 25),
				new IntPoint(75, 75),
				new IntPoint(60, 90),
				new IntPoint(10, 10)
		};

		p.initQuadTree(Arrays.asList(splitPoints));

		for (int i = 0; i < 3; i++) {
			p.testIntraGroupComm(i);
			p.testInterGroupComm(i);
		}
	}

	private static void testInitUniformly() throws MPIException {
		MPITest.printOnlyIn(0, "Testing init uniformly......");

		final DQuadTreePartition p = new DQuadTreePartition(new int[] { 100, 100 }, false, new int[] { 1, 1 });
		p.initUniformly();

		for (int i = 0; i < 3; i++) {
			p.testIntraGroupComm(i);
			p.testInterGroupComm(i);
		}
	}

	public static void main(final String[] args) throws MPIException {
		MPI.Init(args);

		testInitWithPoints();
		testInitUniformly();
		testBalance();

		MPI.Finalize();
	}

	public String toString() {
		return "DQuadTreePartition [qt=" + qt + ", myLeafNode=" + myLeafNode + ", groups=" + groups + ", aoi="
				+ Arrays.toString(aoi) + "]";
	}
}
