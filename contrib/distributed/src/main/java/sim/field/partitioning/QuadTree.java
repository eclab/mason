package sim.field.partitioning;

import java.util.*;
import sim.util.*;

public class QuadTree
{
	private static final long serialVersionUID = 1L;

	int depth = 0;

	QuadTreeNode root;
	List<Integer> availIds;
	Map<Integer, QuadTreeNode> allNodes;

	// we presume the user doesn't have more than a million processors.  :-)
	public static final int[] POWERS_OF_FOUR = new int[] { 1, 4, 16, 64, 256, 1024, 2048, 4096, 16384, 65536, 262144, 1048576};

	// Shape of field and the maximum number of partitions it can hold
	public QuadTree(final IntRect2D shape, final int np) 
	{
		// there are clever ways of determining powers of four, but we're just gonna use a lookup table here
		boolean found = false;
		for(int i = 0; i < POWERS_OF_FOUR.length; i++)
			{
			if (np == POWERS_OF_FOUR[i])
				{
				found = true;
				break;
				}
			}
			
		// if (!found)
		// 	throw new IllegalArgumentException("The given number of processors " + np + " must be a power of four");

		int div = 4;			// 4 divisions
		root = new QuadTreeNode(shape, null);
		availIds = new ArrayList<Integer>();
		for (int i = 1; i < (np / (div - 1) * div + 1)+4; i++)
		{
			availIds.add(i);
		}
		allNodes = new HashMap<Integer, QuadTreeNode>();
		// root node is the 0th node
		allNodes.put(0, root);
	}

	public int getDepth()
	{
		return depth;
	}

	public QuadTreeNode getRoot()
	{
		return root;
	}

	public QuadTreeNode getNode(final int id)
	{
		return allNodes.get(id);
	}

	public QuadTreeNode getLeafNode(final NumberND p)
	{
		return root.getLeafNode(p);
	}

	public List<QuadTreeNode> getAllNodes()
	{
		return new ArrayList<QuadTreeNode>(allNodes.values());
	}

	public ArrayList<QuadTreeNode> getAllLeaves()
	{
		ArrayList<QuadTreeNode> leaves = new ArrayList<>();
		ArrayList<QuadTreeNode> nodes = new ArrayList<>();
		nodes.addAll(allNodes.values());
		for (int i = 0; i < nodes.size(); i++)
		{
			QuadTreeNode node = nodes.get(i);
			if (node.isLeaf())
				leaves.add(node);
		}
		return leaves;
	}

	public String toString()
	{
		return root.toStringAll();
	}

	public void split(final Int2D p)
	{
		List<QuadTreeNode> newNodes = root.getLeafNode(p).split(p);
		for (int i = 0; i < newNodes.size(); i++)
		{
			addNode(newNodes.get(i));
		}
	}

	public void split(final List<Int2D> ps)
	{
		for (int i = 0; i < ps.size(); i++)
		{
			split(ps.get(i));
		}
	}

	public void moveOrigin(final QuadTreeNode node, final Int2D newOrig)
	{
		List<QuadTreeNode> newNodes = node.split(newOrig);
		for (int i = 0; i < newNodes.size(); i++)
		{
			addNode(newNodes.get(i));
		}
	}

	public void moveOrigin(final int id, final Int2D newOrig)
	{
		moveOrigin(getNode(id), newOrig);
	}

	public void merge(final QuadTreeNode node)
	{
		List<QuadTreeNode> merged = node.merge();
		for (int i = 0; i < merged.size(); i++)
		{
			delNode(merged.get(i));
		}
	}

	public void merge(final int id)
	{
		merge(getNode(id));
	}

	protected void addNode(final QuadTreeNode node)
	{
		if (availIds.size() == 0)
			throw new IllegalArgumentException("Reached maximum number of regions, cannot add more child");

		final int id = availIds.remove(0);
		node.setId(id);
		allNodes.put(id, node);
		depth = Math.max(depth, node.getLevel());
	}

	protected void delNode(final QuadTreeNode node)
	{
		final int id = node.getId();
		allNodes.remove(id);
		availIds.add(id);
		if (depth == node.getLevel())
		{
			List<QuadTreeNode> nodes = new ArrayList<QuadTreeNode>();
			nodes.addAll(allNodes.values());
			int max =
				nodes.get(0) != null
				? nodes.get(0).getLevel()
				: Integer.MIN_VALUE; // TODO <- maybe unnecessary
			for (int i = 0; i < nodes.size(); i++)
			{
				if (nodes.get(i).getLevel() > max)
					max = nodes.get(i).getLevel();
			}
			depth = max;
		}
	}
	
	protected void delLeaf(final QuadTreeNode leaf)
	{
		final int id = leaf.getId();
		allNodes.remove(id);
		availIds.add(id);
		leaf.getParent().children.remove(leaf);
	}

	public HashSet<QuadTreeNode> getNeighbors(final QuadTreeNode node, int aoi, final boolean isToroidal)
	{
		if(isToroidal)
			return getNeighborsToroidal(node, aoi);
		else 
			return getNeighborsNoToroidal(node, aoi);
	}
	
	public HashSet<QuadTreeNode> getNeighborsNoToroidal(final QuadTreeNode node, int aoi)
	{
		// Root node has no neighbors
		if (node.isRoot())
			return new HashSet<QuadTreeNode>();

		final IntRect2D myHalo = node.getShape().expand(aoi);
		final HashSet<QuadTreeNode> ret = new HashSet<QuadTreeNode>();
		final ArrayList<QuadTreeNode> stack = new ArrayList<QuadTreeNode>();

		// Add neighbors from my siblings
		for (int i = 0; i < node.getSiblings().size(); i++)
		{
			final QuadTreeNode sibling = node.getSiblings().get(i);
			if (sibling.isLeaf())
				ret.add(sibling);
			else
			{
				List<QuadTreeNode> nodes = sibling.getLeaves();
				for (int j = 0; j < nodes.size(); j++)
				{
					QuadTreeNode x = nodes.get(j);
					if (myHalo.intersects(x.getShape()))
					{
						ret.add(x);
					}
				}
			}
		}

		// Add neighbors on the other directions
		for (int dim = 0; dim < 2; dim++) 				// 2 is num dimensions
			{
			final boolean dir = node.getDir(dim);

			// Go up to find the first node that contains my neighbor on the given direction
			// of the given dimension
			QuadTreeNode curr = node.getParent();
			while (!curr.isRoot() && curr.getDir(dim) == dir)
				curr = curr.getParent();
//			for (final QuadTreeNode sibling : curr.getSiblings())
			for (int i = 0; i < curr.getSiblings().size(); i++)
			{
				final QuadTreeNode sibling = curr.getSiblings().get(i);
				if (sibling.getDir(dim) == dir)
					stack.add(sibling);
			}
			
			// Next go down to find all the leaf nodes that intersect with my halo
			while (stack.size() > 0 && (curr = stack.remove(0)) != null)
				
				if (!myHalo.intersects(curr.getShape()))
					continue;
				else if (curr.isLeaf())
					ret.add(curr);
				else
				{
//					for (final QuadTreeNode child : curr.getChildren())
					for (int i = 0; i < curr.getChildren().size(); i++)
					{
						final QuadTreeNode child = curr.getChildren().get(i);
						if (child.getDir(dim) != dir)
							stack.add(child);
					}
				}
		}

		return ret;
	}
	
	public HashSet<QuadTreeNode> getNeighborsToroidal(final QuadTreeNode node, int aoi)
	{
		// Root node has no neighbors
		if (node.isRoot())
			return new HashSet<QuadTreeNode>();

		//calculate all Halo Region
		final IntRect2D myShape = node.getShape();
		
		
		//fieldsize
		final IntRect2D rootShape = root.getShape();
		final int width = rootShape.br().x - rootShape.ul().x ;
		final int height = rootShape.br().y -rootShape.ul().y ;
		
		//IntRect2D point
		final int[] ul = new int[] {myShape.ul().x,myShape.ul().y};
		final int[] br = new int[] {myShape.br().x,myShape.br().y};
	
		//TODO maybe I can use myShape.toToroidal() --- maybe not
		//final List<IntRect2D> haloRegions = myShape.expand(aoi).toToroidal(myShape);
		final List<IntRect2D> haloRegions = new ArrayList<IntRect2D>();
		
		//north
		try
		{
		haloRegions.add(new IntRect2D(new Int2D((ul[0]+width)%width,(ul[1]-aoi+height)%height),
				new Int2D((br[0]+width)%width==0?width:(br[0]+width)%width,(ul[1]+height)%height==0?height:(ul[1]+height)%height)));
		}
		catch (Exception e)
		{
			System.out.println("error in north of "+ myShape + "heigth "+height+" width "+width );
			e.printStackTrace();
			System.exit(-1);
		}
		//south
		try
		{
		haloRegions.add(new IntRect2D(new Int2D((ul[0]+width)%width,(br[1]+height)%height),
				new Int2D((br[0]+width)%width==0?width:(br[0]+width)%width,(br[1]+aoi+height)%height==0?height:(br[1]+aoi+height)%height)));
		}
		catch (Exception e)
		{
			System.out.println("error in south of "+ myShape);
			e.printStackTrace();
			System.exit(-1);
		}
		//west
		try
		{
		haloRegions.add(new IntRect2D(new Int2D((ul[0]-aoi+width)%width,(ul[1]+height)%height),
				new Int2D((ul[0]+width)%width==0?width:(ul[0]+width)%width,(br[1]+height)%height==0?height:(br[1]+height)%height)));
		}
		catch (Exception e)
		{
			// TODO: handle exception
			System.out.println("error in west of "+ myShape);
			e.printStackTrace();
			System.exit(-1);
		}
		//east
		try
		{
		haloRegions.add(new IntRect2D(new Int2D((br[0]+width)%width,(ul[1]+height)%height),
				new Int2D((br[0]+aoi+width)%width==0?width:(br[0]+aoi+width)%width,(br[1]+height)%height==0?height:(br[1]+height)%height)));
		}
		catch (Exception e)
		{
			// TODO: handle exception
			System.out.println("error in east of "+ myShape);
			e.printStackTrace();
			System.exit(-1);
		}
		//north-west
		try
		{
		haloRegions.add(new IntRect2D(new Int2D((ul[0]-aoi+width)%width,(ul[1]-aoi+height)%height),
				new Int2D((ul[0]+width)%width==0?width:(ul[0]+width)%width,(ul[1]+height)%height==0?height:(ul[1]+height)%height)));
		}
		catch (Exception e)
		{
			// TODO: handle exception
			System.out.println("error in north-west of "+ myShape);
			e.printStackTrace();
			System.exit(-1);
		}
		//north-east
		try
		{
		haloRegions.add(new IntRect2D(new Int2D((br[0]+width)%width,(ul[1]-aoi+height)%height),
				new Int2D((br[0]+aoi+width)%width==0?width:(br[0]+aoi+width)%width,(ul[1]+height)%height==0?height:(ul[1]+height)%height)));
		}
		catch (Exception e)
		{
			// TODO: handle exception
			System.out.println("error in north-east of "+ myShape);
			e.printStackTrace();
			System.exit(-1);
		}
		//south-west
		try
		{
		haloRegions.add(new IntRect2D(new Int2D((ul[0]-aoi+width)%width,(br[1]+height)%height),
				new Int2D((ul[0]+width)%width==0?width:(ul[0]+width)%width,(br[1]+aoi+height)%height==0?height:(br[1]+aoi+height)%height)));
		}
		catch (Exception e)
		{
			// TODO: handle exception
			System.out.println("error in south-west of "+ myShape);
			e.printStackTrace();
			System.exit(-1);
		}
		//south-east
		try
		{
		haloRegions.add(new IntRect2D(new Int2D((br[0]+width)%width,(br[1]+height)%height),
				new Int2D((br[0]+aoi+width)%width==0?width:(br[0]+aoi+width)%width,(br[1]+aoi+height)%height==0?height:(br[1]+aoi+height)%height)));
		}
		catch (Exception e)
		{
			// TODO: handle exception
			System.out.println("error in south-east of "+ myShape);
			e.printStackTrace();
			System.exit(-1);
		}
		
		final HashSet<QuadTreeNode> ret = new HashSet<QuadTreeNode>();
		final ArrayList<QuadTreeNode> stack = new ArrayList<QuadTreeNode>();
		
		for(final QuadTreeNode sibling : node.getSiblings())
		{
			if(sibling.isLeaf())
				ret.add(sibling);
			else
				for(final QuadTreeNode leaf : sibling.getLeaves())
				{
					// if intersect at least one of my halo regions add to ret
					for(final IntRect2D region : haloRegions)
					{
						if(leaf.getShape().intersects(region))
						{
							ret.add(leaf);
							break;
						}
					}
				}
		}
		
		QuadTreeNode curr = node.getParent();
		while(!curr.isRoot())
		{
			for (final QuadTreeNode sibling : curr.getSiblings())
			{
				if (sibling.isLeaf())
				{
					// if intersect at least one of my halo regions add to ret
					for(final IntRect2D region : haloRegions)
					{
						if(sibling.getShape().intersects(region))
						{
							ret.add(sibling);
							break;
						}
					}
				}
				else
				{
					for(final QuadTreeNode leaf : sibling.getLeaves())
					{
						// if intersect at least one of my halo regions add to ret
						for(final IntRect2D region : haloRegions)
						{
							if(leaf.getShape().intersects(region))
							{
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

	public int[] getNeighborPIDs(final QuadTreeNode node, int aoi,final boolean isToroidal)
	{
		List<QuadTreeNode> neighbors = new ArrayList<QuadTreeNode>();
		neighbors.addAll(getNeighbors(node, aoi,isToroidal));
		int[] ids = new int[neighbors.size()];
		for (int i = 0; i < neighbors.size(); i++)
		{
			QuadTreeNode x = neighbors.get(i);
			ids[i] = x.getId();
		}
		Arrays.sort(ids);
		return ids;
	}

	public int[] getNeighborPids(final QuadTreeNode node, int aoi,final boolean isToroidal)
	{
		List<QuadTreeNode> neighbors = new ArrayList<QuadTreeNode>();
		neighbors.addAll(getNeighbors(node, aoi,isToroidal));
		int[] pids = new int[neighbors.size()];
		for (int i = 0; i < neighbors.size(); i++)
		{
			QuadTreeNode x = neighbors.get(i);
			pids[i] = x.getProcessor();
		}
		Arrays.sort(pids);
		return pids;
	}

	private static void testFindNeighbor()
	{
		final IntRect2D field = new IntRect2D(new Int2D(0, 0), new Int2D(100, 100));
		final QuadTree qt = new QuadTree(field, 16);
		final int aoi = 1;

		final Int2D[] splitPoints = new Int2D[]
				{
				new Int2D(50, 50),
				new Int2D(25, 25),
				new Int2D(25, 75),
				new Int2D(75, 25),
				new Int2D(75, 75),
				//new Int2D(35, 15),
				//new Int2D(40, 35),
				};

		final HashMap<Integer, int[]> tests = new HashMap<Integer, int[]>()
		{
			{
				// put(22, new int[] { 5, 6, 21, 23, 24, 25 });
				// put(24, new int[] { 13, 14, 21, 22, 23, 25, 27 });
				// put(13, new int[] { 14, 15, 16, 23, 24, 27 });
				// put(15, new int[] { 13, 14, 16 });
				// put(20, new int[] { 17, 18, 19 });
				// put(10, new int[] { 9, 11, 12 });
				// put(5, new int[] { 6, 21, 22, 25 });
				// put(6, new int[] { 5, 9, 11, 22, 25, 26 });
				// put(26, new int[] { 6, 9, 11, 25, 27, 28 });
				// put(11, new int[] { 6, 9, 10, 12, 14, 17, 18, 26, 28 });
				// put(17, new int[] { 11, 12, 14, 16, 18, 19, 20, 28 });
				// put(10, new int[] { 9, 11, 12 });
				put(8,new int[]{});
				put(5,new int[]{});
			}
		};

		for (final Int2D p : splitPoints)
			qt.split(p);

		System.out.println("Testing neighbor finding in the following tree...\n" + qt);

		for (final Map.Entry<Integer, int[]> test : tests.entrySet())
		{
			final QuadTreeNode node = qt.getNode(test.getKey());
			final int[] got = qt.getNeighborPIDs(node, aoi,true);
			final int[] want = test.getValue();
			final boolean isPass = Arrays.equals(want, got);
			System.out.println(
					"Testing neighbor finding for node " + node.getId() + ":\t" + (isPass ? "< Pass >" : "< Fail >"));
			if (!isPass)
			{
				System.out.println("Want: " + Arrays.toString(want));
				System.out.println("Got : " + Arrays.toString(got));
			}
		}
	}

	public static void main(final String[] args)
	{
		final IntRect2D field = new IntRect2D(new Int2D(0, 0), new Int2D(100, 100));

		final QuadTree qt = new QuadTree(field, 8);

		qt.split(new Int2D(40, 60));
		System.out.println(qt);

		// qt.split(new Int2D(10, 80));
		// System.out.println(qt);

		// final Int2D p1 = new Int2D(50, 50);
		// System.out.println("Point " + p1 + " is in node " + qt.getLeafNode(p1));

		// qt.moveOrigin(qt.getRoot(), new Int2D(60, 70));
		// System.out.println(qt);

		// System.out.println("Point " + p1 + " is in node " + qt.getLeafNode(p1));

		// System.out.println("------------");
		// System.out.println(qt.availIds);
		// for (final QuadTreeNode node : qt.allNodes.values())
		// 	System.out.println(node);
		// System.out.println(qt.depth);

		// System.out.println("Merge one of root's children");
		// qt.merge(qt.getRoot().getChild(1));
		// System.out.println(qt.availIds);
		// for (final QuadTreeNode node : qt.getAllNodes())
		// 	System.out.println("Node " + node);
		// for (final QuadTreeNode node : qt.getAllLeaves())
		// 	System.out.println("Leaf " + node);
		// System.out.println(qt.depth);

		// System.out.println("Merge root");
		// qt.merge(qt.getRoot());
		// System.out.println(qt.availIds);
		// for (final QuadTreeNode node : qt.getAllNodes())
		// 	System.out.println("Node " + node);
		// System.out.println(qt.depth);

		//testFindNeighbor();
	}
}