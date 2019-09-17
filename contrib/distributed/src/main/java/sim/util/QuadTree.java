package sim.util;

import java.util.*;
import java.util.stream.*;

public class QuadTree {
	int depth = 0;

	QTNode root;
	List<Integer> availIds;
	Map<Integer, QTNode> allNodes;

	// Shape of field and the maximum number of partitions it can hold
	public QuadTree(final IntHyperRect shape, final int np) {
		final int div = 1 << shape.getNd();

		if (np % (div - 1) != 1)
			throw new IllegalArgumentException("The given number of processors " + np + " is illegal");

		root = new QTNode(shape, null);
		availIds = IntStream.range(1, np / (div - 1) * div + 1).boxed().collect(Collectors.toList());

		allNodes = new HashMap<Integer, QTNode>();
		// root node is the 0th node
		allNodes.put(0, root);
	}

	public int getDepth() {
		return depth;
	}

	public QTNode getRoot() {
		return root;
	}

	public QTNode getNode(final int id) {
		return allNodes.get(id);
	}

	public QTNode getLeafNode(final NdPoint p) {
		return root.getLeafNode(p);
	}

	public List<QTNode> getAllNodes() {
		return new ArrayList<QTNode>(allNodes.values());
	}

	public List<QTNode> getAllLeaves() {
		return allNodes.values().stream().filter(node -> node.isLeaf()).collect(Collectors.toList());
	}

	public String toString() {
		return root.toStringAll();
	}

	public void split(final IntPoint p) {
		root.getLeafNode(p).split(p).forEach(x -> addNode(x));
	}

	public void split(final List<IntPoint> ps) {
		ps.forEach(p -> split(p));
	}

	public void moveOrigin(final QTNode node, final IntPoint newOrig) {
		node.split(newOrig).forEach(x -> addNode(x));
	}

	public void moveOrigin(final int id, final IntPoint newOrig) {
		moveOrigin(getNode(id), newOrig);
	}

	public void merge(final QTNode node) {
		node.merge().forEach(x -> delNode(x));
	}

	public void merge(final int id) {
		merge(getNode(id));
	}

	protected void addNode(final QTNode node) {
		if (availIds.size() == 0)
			throw new IllegalArgumentException("Reached maximum number of regions, cannot add more child");

		final int id = availIds.remove(0);
		node.setId(id);
		allNodes.put(id, node);
		depth = Math.max(depth, node.getLevel());
	}

	protected void delNode(final QTNode node) {
		final int id = node.getId();
		allNodes.remove(id);
		availIds.add(id);
		if (depth == node.getLevel())
			depth = allNodes.values().stream().mapToInt(x -> x.getLevel()).max().orElse(0);
	}

	// Find the neighbors of the given node in the tree
	public HashSet<QTNode> getNeighbors(final QTNode node, final int[] aoi) {
		// Root node has no neighbors
		if (node.isRoot())
			return new HashSet<QTNode>();

		final IntHyperRect myHalo = node.getShape().resize(aoi);
		final HashSet<QTNode> ret = new HashSet<QTNode>();
		final ArrayList<QTNode> stack = new ArrayList<QTNode>();

		// Add neighbors from my siblings
		for (final QTNode sibling : node.getSiblings())
			if (sibling.isLeaf())
				ret.add(sibling);
			else
				sibling.getLeaves().stream().filter(x -> myHalo.isIntersect(x.getShape())).forEach(x -> ret.add(x));

		// Add neighbors on the other directions
		for (int dim = 0; dim < node.nd; dim++) {
			final boolean dir = node.getDir(dim);

			// Go up to find the first node that contains my neighbor on the given direction
			// of the given dimension
			QTNode curr = node.getParent();
			while (!curr.isRoot() && curr.getDir(dim) == dir)
				curr = curr.getParent();
			for (final QTNode sibling : curr.getSiblings())
				if (sibling.getDir(dim) == dir)
					stack.add(sibling);

			// Next go down to find all the leaf nodes that intersect with my halo
			while (stack.size() > 0 && (curr = stack.remove(0)) != null)
				if (!myHalo.isIntersect(curr.getShape()))
					continue;
				else if (curr.isLeaf())
					ret.add(curr);
				else
					for (final QTNode child : curr.getChildren())
						if (child.getDir(dim) != dir)
							stack.add(child);
		}

		return ret;
	}

	public int[] getNeighborIds(final QTNode node, final int[] aoi) {
		return getNeighbors(node, aoi).stream().mapToInt(x -> x.getId()).sorted().toArray();
	}

	public int[] getNeighborPids(final QTNode node, final int[] aoi) {
		return getNeighbors(node, aoi).stream().mapToInt(x -> x.getProc()).sorted().toArray();
	}

	private static void testFindNeighbor() {
		final IntHyperRect field = new IntHyperRect(-1, new IntPoint(0, 0), new IntPoint(100, 100));
		final QuadTree qt = new QuadTree(field, 22);
		final int[] aoi = new int[] { 1, 1 };

		final IntPoint[] splitPoints = new IntPoint[] {
				new IntPoint(50, 50),
				new IntPoint(25, 25),
				new IntPoint(25, 75),
				new IntPoint(75, 25),
				new IntPoint(75, 75),
				new IntPoint(35, 15),
				new IntPoint(40, 35),
		};

		final HashMap<Integer, int[]> tests = new HashMap<Integer, int[]>() {
			{
				put(22, new int[] { 5, 6, 21, 23, 24, 25 });
				put(24, new int[] { 13, 14, 21, 22, 23, 25, 27 });
				put(13, new int[] { 14, 15, 16, 23, 24, 27 });
				put(15, new int[] { 13, 14, 16 });
				put(20, new int[] { 17, 18, 19 });
				put(10, new int[] { 9, 11, 12 });
				put(5, new int[] { 6, 21, 22, 25 });
				put(6, new int[] { 5, 9, 11, 22, 25, 26 });
				put(26, new int[] { 6, 9, 11, 25, 27, 28 });
				put(11, new int[] { 6, 9, 10, 12, 14, 17, 18, 26, 28 });
				put(17, new int[] { 11, 12, 14, 16, 18, 19, 20, 28 });
				put(10, new int[] { 9, 11, 12 });
			}
		};

		for (final IntPoint p : splitPoints)
			qt.split(p);

		System.out.println("Testing neighbor finding in the following tree...\n" + qt);

		for (final Map.Entry<Integer, int[]> test : tests.entrySet()) {
			final QTNode node = qt.getNode(test.getKey());
			final int[] got = qt.getNeighborIds(node, aoi);
			final int[] want = test.getValue();
			final boolean isPass = Arrays.equals(want, got);
			System.out.println(
					"Testing neighbor finding for node " + node.getId() + ":\t" + (isPass ? "< Pass >" : "< Fail >"));
			if (!isPass) {
				System.out.println("Want: " + Arrays.toString(want));
				System.out.println("Got : " + Arrays.toString(got));
			}
		}
	}

	public static void main(final String[] args) {
		final IntHyperRect field = new IntHyperRect(-1, new IntPoint(0, 0), new IntPoint(100, 100));

		final QuadTree qt = new QuadTree(field, 7);

		qt.split(new IntPoint(40, 60));
		System.out.println(qt);

		qt.split(new IntPoint(10, 80));
		System.out.println(qt);

		final IntPoint p1 = new IntPoint(50, 50);
		System.out.println("Point " + p1 + " is in node " + qt.getLeafNode(p1));

		qt.moveOrigin(qt.getRoot(), new IntPoint(60, 70));
		System.out.println(qt);

		System.out.println("Point " + p1 + " is in node " + qt.getLeafNode(p1));

		System.out.println("------------");
		System.out.println(qt.availIds);
		for (final QTNode node : qt.allNodes.values())
			System.out.println(node);
		System.out.println(qt.depth);

		System.out.println("Merge one of root's children");
		qt.merge(qt.getRoot().getChild(1));
		System.out.println(qt.availIds);
		for (final QTNode node : qt.getAllNodes())
			System.out.println("Node " + node);
		for (final QTNode node : qt.getAllLeaves())
			System.out.println("Leaf " + node);
		System.out.println(qt.depth);

		System.out.println("Merge root");
		qt.merge(qt.getRoot());
		System.out.println(qt.availIds);
		for (final QTNode node : qt.getAllNodes())
			System.out.println("Node " + node);
		System.out.println(qt.depth);

		testFindNeighbor();
	}
}
