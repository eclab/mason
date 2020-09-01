package sim.field.partitioning;

import java.util.*;
import java.util.stream.*;
import sim.util.*;

public class QuadTree {
	int depth = 0;

	QuadTreeNode root;
	List<Integer> availIds;
	Map<Integer, QuadTreeNode> allNodes;

	// Shape of field and the maximum number of partitions it can hold
	public QuadTree(final IntHyperRect shape, final int np) {
		final int div = 1 << shape.getNd();

		if (np % (div - 1) != 1)
			throw new IllegalArgumentException("The given number of processors " + np + " is illegal");

		root = new QuadTreeNode(shape, null);
		availIds = IntStream.range(1, np / (div - 1) * div + 1).boxed().collect(Collectors.toList());

		allNodes = new HashMap<Integer, QuadTreeNode>();
		// root node is the 0th node
		allNodes.put(0, root);
	}

	public int getDepth() {
		return depth;
	}

	public QuadTreeNode getRoot() {
		return root;
	}

	public QuadTreeNode getNode(final int id) {
		return allNodes.get(id);
	}

	public QuadTreeNode getLeafNode(final NumberND p) {
		return root.getLeafNode(p);
	}

	public List<QuadTreeNode> getAllNodes() {
		return new ArrayList<QuadTreeNode>(allNodes.values());
	}

	public List<QuadTreeNode> getAllLeaves() {
		return allNodes.values().stream().filter(node -> node.isLeaf()).collect(Collectors.toList());
	}

	public String toString() {
		return root.toStringAll();
	}

	public void split(final Int2D p) {
		root.getLeafNode(p).split(p).forEach(x -> addNode(x));
	}

	public void split(final List<Int2D> ps) {
		ps.forEach(p -> split(p));
	}

	public void moveOrigin(final QuadTreeNode node, final Int2D newOrig) {
		node.split(newOrig).forEach(x -> addNode(x));
	}

	public void moveOrigin(final int id, final Int2D newOrig) {
		moveOrigin(getNode(id), newOrig);
	}

	public void merge(final QuadTreeNode node) {
		node.merge().forEach(x -> delNode(x));
	}

	public void merge(final int id) {
		merge(getNode(id));
	}

	protected void addNode(final QuadTreeNode node) {
		if (availIds.size() == 0)
			throw new IllegalArgumentException("Reached maximum number of regions, cannot add more child");

		final int id = availIds.remove(0);
		node.setId(id);
		allNodes.put(id, node);
		depth = Math.max(depth, node.getLevel());
	}

	protected void delNode(final QuadTreeNode node) {
		final int id = node.getId();
		allNodes.remove(id);
		availIds.add(id);
		if (depth == node.getLevel())
			depth = allNodes.values().stream().mapToInt(x -> x.getLevel()).max().orElse(0);
	}

	public HashSet<QuadTreeNode> getNeighbors(final QuadTreeNode node, final int[] aoi, final boolean isToroidal) {
		if(isToroidal)
			return getNeighborsToroidal(node, aoi);
		else 
			return getNeighborsNoToroidal(node, aoi);
	}
	
	public HashSet<QuadTreeNode> getNeighborsNoToroidal(final QuadTreeNode node, final int[] aoi){
		// Root node has no neighbors
		if (node.isRoot())
			return new HashSet<QuadTreeNode>();

		final IntHyperRect myHalo = node.getShape().resize(aoi);
		final HashSet<QuadTreeNode> ret = new HashSet<QuadTreeNode>();
		final ArrayList<QuadTreeNode> stack = new ArrayList<QuadTreeNode>();

		// Add neighbors from my siblings
		for (final QuadTreeNode sibling : node.getSiblings())
			if (sibling.isLeaf())
				ret.add(sibling);
			else
				sibling.getLeaves().stream().filter(x -> myHalo.isIntersect(x.getShape())).forEach(x -> ret.add(x));

		// Add neighbors on the other directions
		for (int dim = 0; dim < node.nd; dim++) {
			final boolean dir = node.getDir(dim);

			// Go up to find the first node that contains my neighbor on the given direction
			// of the given dimension
			QuadTreeNode curr = node.getParent();
			while (!curr.isRoot() && curr.getDir(dim) == dir)
				curr = curr.getParent();
			for (final QuadTreeNode sibling : curr.getSiblings())
				if (sibling.getDir(dim) == dir)
					stack.add(sibling);
			
			// Next go down to find all the leaf nodes that intersect with my halo
			while (stack.size() > 0 && (curr = stack.remove(0)) != null)
				
				if (!myHalo.isIntersect(curr.getShape()))
					continue;
				else if (curr.isLeaf())
					ret.add(curr);
				else
					for (final QuadTreeNode child : curr.getChildren())
						if (child.getDir(dim) != dir)
							stack.add(child);
		}

		return ret;
	}
	
	public HashSet<QuadTreeNode> getNeighborsToroidal(final QuadTreeNode node, final int[] aoi){
		// Root node has no neighbors
		if (node.isRoot())
			return new HashSet<QuadTreeNode>();

		//calculate all Halo Region
		final IntHyperRect myShape = node.getShape();
		
		
		//fieldsize
		final IntHyperRect rootShape = root.getShape();
		final int width = rootShape.br.c(0) - rootShape.ul.c(0) ;
		final int height = rootShape.br.c(1) -rootShape.ul.c(1) ;
		
		//IntHyperRect point
		final int[] ul = myShape.ul.c();
		final int[] br = myShape.br.c();
	
		//TODO maybe I can use myShape.toToroidal() --- maybe not
		//final List<IntHyperRect> haloRegions = myShape.resize(aoi).toToroidal(myShape);
		final List<IntHyperRect> haloRegions = new ArrayList<IntHyperRect>();
		
		//north
		try {
		haloRegions.add(new IntHyperRect(-1, new Int2D((ul[0]+width)%width,(ul[1]-aoi[1]+height)%height),
				new Int2D((br[0]+width)%width==0?width:(br[0]+width)%width,(ul[1]+height)%height==0?height:(ul[1]+height)%height)));
		}catch (Exception e) {
			System.out.println("error in north of "+ myShape + "heigth "+height+" width "+width );
			e.printStackTrace();
			System.exit(-1);
		}
		//south
		try {
		haloRegions.add(new IntHyperRect(-1,new Int2D((ul[0]+width)%width,(br[1]+height)%height),
				new Int2D((br[0]+width)%width==0?width:(br[0]+width)%width,(br[1]+aoi[1]+height)%height==0?height:(br[1]+aoi[1]+height)%height)));
		}catch (Exception e) {
			System.out.println("error in south of "+ myShape);
			e.printStackTrace();
			System.exit(-1);
		}
		//west
		try {
		haloRegions.add(new IntHyperRect(-1,new Int2D((ul[0]-aoi[0]+width)%width,(ul[1]+height)%height),
				new Int2D((ul[0]+width)%width==0?width:(ul[0]+width)%width,(br[1]+height)%height==0?height:(br[1]+height)%height)));
		}catch (Exception e) {
			// TODO: handle exception
			System.out.println("error in west of "+ myShape);
			e.printStackTrace();
			System.exit(-1);
		}
		//east
		try {
		haloRegions.add(new IntHyperRect(-1,new Int2D((br[0]+width)%width,(ul[1]+height)%height),
				new Int2D((br[0]+aoi[0]+width)%width==0?width:(br[0]+aoi[0]+width)%width,(br[1]+height)%height==0?height:(br[1]+height)%height)));
		}catch (Exception e) {
			// TODO: handle exception
			System.out.println("error in east of "+ myShape);
			e.printStackTrace();
			System.exit(-1);
		}
		//north-west
		try {
		haloRegions.add(new IntHyperRect(-1,new Int2D((ul[0]-aoi[0]+width)%width,(ul[1]-aoi[1]+height)%height),
				new Int2D((ul[0]+width)%width==0?width:(ul[0]+width)%width,(ul[1]+height)%height==0?height:(ul[1]+height)%height)));
		}catch (Exception e) {
			// TODO: handle exception
			System.out.println("error in north-west of "+ myShape);
			e.printStackTrace();
			System.exit(-1);
		}
		//north-east
		try {
		haloRegions.add(new IntHyperRect(-1,new Int2D((br[0]+width)%width,(ul[1]-aoi[1]+height)%height),
				new Int2D((br[0]+aoi[0]+width)%width==0?width:(br[0]+aoi[0]+width)%width,(ul[1]+height)%height==0?height:(ul[1]+height)%height)));
		}catch (Exception e) {
			// TODO: handle exception
			System.out.println("error in north-east of "+ myShape);
			e.printStackTrace();
			System.exit(-1);
		}
		//south-west
		try {
		haloRegions.add(new IntHyperRect(-1,new Int2D((ul[0]-aoi[0]+width)%width,(br[1]+height)%height),
				new Int2D((ul[0]+width)%width==0?width:(ul[0]+width)%width,(br[1]+aoi[1]+height)%height==0?height:(br[1]+aoi[1]+height)%height)));
		}catch (Exception e) {
			// TODO: handle exception
			System.out.println("error in south-west of "+ myShape);
			e.printStackTrace();
			System.exit(-1);
		}
		//south-east
		try {
		haloRegions.add(new IntHyperRect(-1,new Int2D((br[0]+width)%width,(br[1]+height)%height),
				new Int2D((br[0]+aoi[0]+width)%width==0?width:(br[0]+aoi[0]+width)%width,(br[1]+aoi[1]+height)%height==0?height:(br[1]+aoi[1]+height)%height)));
		}catch (Exception e) {
			// TODO: handle exception
			System.out.println("error in south-east of "+ myShape);
			e.printStackTrace();
			System.exit(-1);
		}
		
		final HashSet<QuadTreeNode> ret = new HashSet<QuadTreeNode>();
		final ArrayList<QuadTreeNode> stack = new ArrayList<QuadTreeNode>();
		
		for(final QuadTreeNode sibling : node.getSiblings()) {
			if(sibling.isLeaf())
				ret.add(sibling);
			else
				for(final QuadTreeNode leaf : sibling.getLeaves()) {
					// if intersect at least one of my halo regions add to ret
					for(final IntHyperRect region : haloRegions) {
						if(leaf.getShape().isIntersect(region)) {
							ret.add(leaf);
							break;
						}
					}
				}
		}
		
		QuadTreeNode curr = node.getParent();
		while(!curr.isRoot()) {
			for (final QuadTreeNode sibling : curr.getSiblings()) {
				if (sibling.isLeaf()) {
					// if intersect at least one of my halo regions add to ret
					for(final IntHyperRect region : haloRegions) {
						if(sibling.getShape().isIntersect(region)) {
							ret.add(sibling);
							break;
						}
					}
				} else {
					for(final QuadTreeNode leaf : sibling.getLeaves()) {
						// if intersect at least one of my halo regions add to ret
						for(final IntHyperRect region : haloRegions) {
							if(leaf.getShape().isIntersect(region)) {
								ret.add(leaf);
								break;
							}
						}
					}
				}
			}
			curr = curr.getParent();
		}

		return ret;
	}

	public int[] getNeighborIds(final QuadTreeNode node, final int[] aoi,final boolean isToroidal) {
		return getNeighbors(node, aoi,isToroidal).stream().mapToInt(x -> x.getId()).sorted().toArray();
	}

	public int[] getNeighborPids(final QuadTreeNode node, final int[] aoi,final boolean isToroidal) {
		return getNeighbors(node, aoi,isToroidal).stream().mapToInt(x -> x.getProcessor()).sorted().toArray();
	}

	private static void testFindNeighbor() {
		final IntHyperRect field = new IntHyperRect(-1, new Int2D(0, 0), new Int2D(100, 100));
		final QuadTree qt = new QuadTree(field, 22);
		final int[] aoi = new int[] { 1, 1 };

		final Int2D[] splitPoints = new Int2D[] {
				new Int2D(50, 50),
				new Int2D(25, 25),
				new Int2D(25, 75),
				new Int2D(75, 25),
				new Int2D(75, 75),
				new Int2D(35, 15),
				new Int2D(40, 35),
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

		for (final Int2D p : splitPoints)
			qt.split(p);

		System.out.println("Testing neighbor finding in the following tree...\n" + qt);

		for (final Map.Entry<Integer, int[]> test : tests.entrySet()) {
			final QuadTreeNode node = qt.getNode(test.getKey());
			final int[] got = qt.getNeighborIds(node, aoi,false);
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
		final IntHyperRect field = new IntHyperRect(-1, new Int2D(0, 0), new Int2D(100, 100));

		final QuadTree qt = new QuadTree(field, 7);

		qt.split(new Int2D(40, 60));
		System.out.println(qt);

		qt.split(new Int2D(10, 80));
		System.out.println(qt);

		final Int2D p1 = new Int2D(50, 50);
		System.out.println("Point " + p1 + " is in node " + qt.getLeafNode(p1));

		qt.moveOrigin(qt.getRoot(), new Int2D(60, 70));
		System.out.println(qt);

		System.out.println("Point " + p1 + " is in node " + qt.getLeafNode(p1));

		System.out.println("------------");
		System.out.println(qt.availIds);
		for (final QuadTreeNode node : qt.allNodes.values())
			System.out.println(node);
		System.out.println(qt.depth);

		System.out.println("Merge one of root's children");
		qt.merge(qt.getRoot().getChild(1));
		System.out.println(qt.availIds);
		for (final QuadTreeNode node : qt.getAllNodes())
			System.out.println("Node " + node);
		for (final QuadTreeNode node : qt.getAllLeaves())
			System.out.println("Leaf " + node);
		System.out.println(qt.depth);

		System.out.println("Merge root");
		qt.merge(qt.getRoot());
		System.out.println(qt.availIds);
		for (final QuadTreeNode node : qt.getAllNodes())
			System.out.println("Node " + node);
		System.out.println(qt.depth);

		testFindNeighbor();
	}
}
