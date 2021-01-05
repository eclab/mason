package sim.field.partitioning;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

import mpi.Comm;
import mpi.Info;
import mpi.MPI;
import mpi.MPIException;
import sim.util.*;

/**
 * Quad tree partition divides the world into partitions and arranging them as a
 * quad tree.
 *
 * @param <P> Type of point
 */
public class QuadTreePartition extends PartitionInterface {
	QuadTree qt;
	QuadTreeNode myLeafNode; // the leaf node that this pid is mapped to
	Map<Integer, GroupComm> groups; // Map the level to its corresponding comm group
	int treeDepth;

	public QuadTreePartition(final int[] size, final boolean isToroidal, final int[] aoi) {
		super(size, isToroidal, aoi);
		qt = new QuadTree(new IntRect2D(size[0], size[1]), numProcessors);
	}

	public IntRect2D getBounds() {
		return myLeafNode.getShape();
	}

	public IntRect2D getBounds(final int pid) {
		for (final QuadTreeNode node : qt.getAllLeaves())
			if (node.getProcessor() == pid)
				return node.getShape();

		throw new IllegalArgumentException("The partition for " + pid + " does not exist");
	}

	public IntRect2D getHaloBounds() {
		return myLeafNode.getShape().resize(aoi);
	}

	public ArrayList<IntRect2D> getAllBounds() {
		ArrayList<IntRect2D> allBounds = new ArrayList<>();

		// init with nulls
		for (int i = 0; i < numProcessors; i++)
			allBounds.add(null);

		for (final QuadTreeNode node : qt.getAllLeaves())
			allBounds.set(node.getProcessor(), node.getShape());
		return allBounds;
	}

	public QuadTree getQt() {
		return qt;
	}

	public int getNumNeighbors() {
		return getNeighborIds().length;
	}

	public int[] getNeighborIds() {
		return qt.getNeighborPids(myLeafNode, aoi, isToroidal);
	}

	public int toPartitionId(final NumberND p) {
		return qt.getLeafNode(p).getProcessor();
	}

	public int toPartitionId(final int[] c) {
		return toPartitionId(new Int2D(c));
	}

	public int toPartitionId(final double[] c) {
		return toPartitionId(new Double2D(c));
	}

	/**
	 * Creates the MPI comm world by defining the MPI topology as this quad tree.
	 */
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
	void initQuadTree(final ArrayList<Int2D> splitPoints) {
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

		if ((numProcessors & numProcessors - 1) != 0 || nz % 2 != 0) // 2 == number of dimensions, width and height
			throw new IllegalArgumentException(
					"Currently only support the number processors that is power of " + 4);

		for (int level = 0; level < nz / 2; level++) // 2 == number of dimensions, width and height
		{
			final ArrayList<QuadTreeNode> leaves = qt.getAllLeaves();
			for (final QuadTreeNode leaf : leaves) {
				// qt.split(leaf.getShape().getInt2DCenter());
				Double2D d = leaf.getShape().getCenter();
				qt.split(new Int2D((int) Math.floor(d.x), (int) Math.floor(d.y)));
			}

		}
		treeDepth = (nz / 2) - 1;
		mapNodeToProc();
		createMPITopo();
	}

	/**
	 * Maps node to a processor
	 * 
	 */
	protected void mapNodeToProc() {
		final ArrayList<QuadTreeNode> leaves = qt.getAllLeaves();

		if (leaves.size() != numProcessors)
			throw new IllegalArgumentException("The number of leaves " + leaves.size()
					+ " does not equal to the number of processors " + numProcessors);

		// Map the leaf nodes first
		for (int i = 0; i < numProcessors; i++)
			leaves.get(i).setProcessor(i);

		// if pid == 0 then myLeafNode is root node
		myLeafNode = leaves.get(pid);

		// Map non-leaf nodes - Use the first children node to hold itself
		while (leaves.size() > 0) {
			final QuadTreeNode curr = leaves.remove(0);
			final QuadTreeNode parent = curr.getParent();
			if (parent == null || parent.getChild(0) != curr)
				continue;
			parent.setProcessor(curr.getProcessor());
			leaves.add(parent);
		}

		// Set the proc id to the IntRect2D so it can be printed out when debugging
		// it is not used by the program itself (TODO double-check)

		// removed by Raj Patel
		// for (final QuadTreeNode leaf : qt.getAllLeaves())
		// leaf.getShape().setId(leaf.getProcessor());
	}

	/**
	 * Create groups for MPI communication
	 * 
	 * @throws MPIException
	 */
	protected void createGroups() throws MPIException {
		int currDepth = 0;
		groups = new HashMap<Integer, GroupComm>();

		// Iterate level by level to create groups
		ArrayList<QuadTreeNode> currLevel = new ArrayList<>();
		currLevel.add(qt.getRoot());
		while (currLevel.size() > 0) {
			final ArrayList<QuadTreeNode> nextLevel = new ArrayList<>();

			for (final QuadTreeNode node : currLevel) {
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

	public int[] getProcessorNeighborhood(int level) throws RemoteException {
		ArrayList<QuadTreeNode> leaves = groups.get(level).leaves;
		int[] pids = new int[leaves.size()];
		for (int i = 0; i < pids.length; i++)
			pids[i] = leaves.get(i).getProcessor();
		return pids;
	}

	/**
	 * @param gc
	 * @return true if the calling pid is the master node of the given GroupComm
	 */
	public boolean isGroupMaster(final GroupComm gc) {
		return gc != null && gc.master.getProcessor() == pid;
	}

	/**
	 * @param level
	 * @return true if the calling pid is the master node of the GroupComm at the
	 *         given level
	 */
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
	public IntRect2D getNodeShapeAtLevel(final int level) {
		final GroupComm gc = getGroupComm(level);
		if (isGroupMaster(gc))
			return gc.master.getShape();
		return null;
	}

	/**
	 * @return the treeDepth
	 */
	public int getTreeDepth() {
		return treeDepth;
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

	/**
	 * Balance the partitions by moving the centroids for the given level.
	 * 
	 * @param myRuntime
	 * @param level
	 * 
	 * @throws MPIException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void balance(final double myRuntime, final int level) throws MPIException {
		final GroupComm gc = groups.get(level);

		Object[] sendCentroids = new Object[] { null };

		if (gc != null) {

			Double2D d = myLeafNode.getShape().getCenter();
			final Int2D ctr = new Int2D((int) Math.floor(d.x), (int) Math.floor(d.y));
			// final Int2D ctr = myLeafNode.getShape().getInt2DCenter();
			final double[] sendData = new double[2 + 1], recvData = new double[2 + 1]; // 2 == num dimensions

			sendData[0] = myRuntime;
			sendData[1] = ctr.x * myRuntime;
			sendData[2] = ctr.y * myRuntime;

			
			gc.comm.reduce(sendData, recvData, recvData.length, MPI.DOUBLE, MPI.SUM, gc.groupRoot);

			if (isGroupMaster(gc)) {
				int[] locVals = new int[recvData.length - 1];
				// skip first (i = 1)
				for (int i = 1; i < recvData.length; i++) {
					double x = recvData[i];
					locVals[i - 1] = (int) (x / recvData[0]);
				}
				sendCentroids = new Object[] {
						gc.master.getId(),
						new Int2D(locVals)
				};
			}
			
		}

		// broadcast to all nodes
		final ArrayList<Object[]> newCentroids = MPIUtil.<Object[]>allGather(MPI.COMM_WORLD, sendCentroids);

		// call precommit
		for (final Consumer r : (ArrayList<Consumer>) preCallbacks)
			r.accept(level);

		// apply changes
		for (final Object[] obj : newCentroids)
			if (obj[0] != null)
				qt.moveOrigin((int) obj[0], (Int2D) obj[1]);

		// Assigns new neighbors after balancing
		// Recreates MPI topology based on that
		createMPITopo();

		// call postcommit
		for (final Consumer r : (ArrayList<Consumer>) postCallbacks)
			r.accept(level);
	}

	private static void testBalance() throws MPIException {
		MPITest.printOnlyIn(0, "Testing balance()......");

		final QuadTreePartition p = new QuadTreePartition(new int[] { 100, 100 }, false, new int[] { 1, 1 });

		final Int2D[] splitPoints = new Int2D[] {
				new Int2D(50, 50),
				new Int2D(25, 25),
				new Int2D(75, 75),
				new Int2D(60, 90),
				new Int2D(10, 10)
		};

		p.initQuadTree(new ArrayList<>(Arrays.asList(splitPoints)));

		final Random rand = new Random();
		final double myRt = rand.nextDouble() * 10;

		p.balance(myRt, 0);

		MPITest.printOnlyIn(0, p.qt.toString());
	}

	private static void testInitWithPoints() throws MPIException {
		MPITest.printOnlyIn(0, "Testing init with points......");

		final QuadTreePartition p = new QuadTreePartition(new int[] { 100, 100 }, false, new int[] { 1, 1 });

		final Int2D[] splitPoints = new Int2D[] {
				new Int2D(50, 50),
				new Int2D(25, 25),
				new Int2D(75, 75),
				new Int2D(60, 90),
				new Int2D(10, 10)
		};

		p.initQuadTree(new ArrayList<>(Arrays.asList(splitPoints)));

		for (int i = 0; i < 3; i++) {
			p.testIntraGroupComm(i);
			p.testInterGroupComm(i);
		}
	}

	private static void testInitUniformly() throws MPIException {
		MPITest.printOnlyIn(0, "Testing init uniformly......");

		final QuadTreePartition p = new QuadTreePartition(new int[] { 100, 100 }, false, new int[] { 1, 1 });
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
