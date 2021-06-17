package sim.field.partitioning;

import java.awt.print.Printable;
import java.util.ArrayList;
import sim.util.*;

// TODO Currently all shapes are restricted to IntRect2D - switch to NdRectangle once it is completed
/**
 * A node in a Quad Tree.
 *
 */
public class QuadTreeNode
{
	private static final long serialVersionUID = 1L;

	/** level in the tree the node is in */
	int level;
	/** its node id */
	int id;
	/** which processer this node is mapped to */
	int processor;

	Int2D origin;
	IntRect2D shape;

	QuadTreeNode parent = null;
	ArrayList<QuadTreeNode> children;

	public QuadTreeNode(final IntRect2D shape, final QuadTreeNode parent)
	{
		this.shape = shape;
		this.parent = parent;
		children = new ArrayList<QuadTreeNode>();
		level = parent == null ? 0 : parent.getLevel() + 1;
	}

	public int getId()
	{
		return id;
	}

	public void setId(final int newId)
	{
		id = newId;
	}

	public int getProcessor()
	{
		return processor;
	}

	public void setProcessor(final int newProcessor)
	{
		processor = newProcessor;
	}

	public int getLevel()
	{
		return level;
	}

	public Int2D getOrigin()
	{
		return origin;
	}

	public IntRect2D getShape()
	{
		return shape;
	}

	public QuadTreeNode getParent()
	{
		return parent;
	}

	// Get my child of the given index
	public QuadTreeNode getChild(final int i)
	{
		return children.get(i);
	}

	public ArrayList<QuadTreeNode> getChildren()
	{
		return children;
	}

	public boolean isRoot()
	{
		return parent == null;
	}

	public boolean isLeaf()
	{
		return children.size() == 0;
	}

	// Return whether I am the ancester of the given node
	public boolean isAncestorOf(final QuadTreeNode node)
	{
		QuadTreeNode curr = node;

		while (curr != null)
		{
			curr = curr.getParent();
			if (curr == this)
				return true;
		}

		return false;
	}

	// Return siblings (not including the node itself) if exist, empty list
	// otherwise
	public ArrayList<QuadTreeNode> getSiblings()
	{
		final ArrayList<QuadTreeNode> ret = new ArrayList<>();

		if (isRoot())
			return ret;

		ret.addAll(parent.getChildren());
		ret.remove(this);

		return ret;
	}

	// Get the immediate child node that contains the given point
	public QuadTreeNode getChildNode(final NumberND p)
	{
		return children.get(toChildIdx(p));
	}

	// Get the leaf node that contains the given point
	public QuadTreeNode getLeafNode(final NumberND p)
	{
		QuadTreeNode curr = this;

		while (!curr.isLeaf())
			curr = curr.getChildNode(p);

		return curr;
	}

	// Get all the leaves that are my offsprings
	public ArrayList<QuadTreeNode> getLeaves()
	{
		final ArrayList<QuadTreeNode> ret = new ArrayList<>();
		final ArrayList<QuadTreeNode> stack = new ArrayList<QuadTreeNode>() {{addAll(children);}};

		while (stack.size() > 0)
		{
			final QuadTreeNode curr = stack.remove(0);
			if (curr.isLeaf())
				ret.add(curr);
			else
				stack.addAll(curr.getChildren());
		}

		return ret;
	}

	/**
	 * Split this node based on the given origin or move the origin if already split
	 * 
	 * @param newOrigin
	 * @return the newly created QTNodes (if any)
	 */
	public ArrayList<QuadTreeNode> split(final Int2D newOrigin)
	{
		final ArrayList<QuadTreeNode> ret = new ArrayList<>();

		if (!shape.contains(newOrigin))
			throw new IllegalArgumentException("newOrigin " + newOrigin + " is outside the region " + shape);

		origin = newOrigin;

		if (isLeaf())
		{
			children = new ArrayList<>();
			for (int i = 0; i < 4; i++)
			{
				children.add(new QuadTreeNode(getChildShape(i), this));
			}
			ret.addAll(children);
		}
		else
			for (int i = 0; i < children.size(); i++)
				children.get(i).reshape(getChildShape(i));

		return ret;
	}

	/**
	 * Merge all the children and make this node a leaf node
	 * 
	 * @return all the nodes that are merged
	 */
	public ArrayList<QuadTreeNode> merge()
	{
		final ArrayList<QuadTreeNode> ret = new ArrayList<QuadTreeNode>();

		if (!isLeaf())
		{
			for (final QuadTreeNode child : children)
			{
				ret.addAll(child.merge());
				ret.add(child);
			}

			children.clear();
			origin = null;
		}

		return ret;
	}

	private int getIndexInSiblings()
	{
		if (isRoot())
			throw new IllegalArgumentException("root node does not have position");

		return parent.getChildren().indexOf(this);
	}

	/**
	 * @param dim
	 * @return the direction (forward/backward) on the given dimension
	 */
	public boolean getDir(final int dim)
	{
		return ((getIndexInSiblings() >> (2 - dim - 1)) & 0x1) == 0x1; // 2 is num dimensions
	}

	/**
	 * Print the current QTNode only
	 */
	public String toString()
	{
		String s = String.format("ID %2d PID %2d L%1d %s", id, processor, level, shape.toString());

		if (origin != null)
			s += " Origin " + origin;

		return s;
	}

	/**
	 * Print the QTNode and all its children
	 */
	public String toStringAll()
	{
		return toStringRecursive(new StringBuffer("Quad Tree\n"), "", true).toString();
	}

	protected StringBuffer toStringRecursive(final StringBuffer buf, final String prefix, final boolean isTail)
	{
		buf.append(prefix + (isTail ? "└── " : "├── ") + this + "\n");

		for (int i = 0; i < children.size() - 1; i++)
			children.get(i).toStringRecursive(buf, prefix + (isTail ? "    " : "│   "), false);

		if (children.size() > 0)
			children.get(children.size() - 1).toStringRecursive(buf, prefix + (isTail ? "    " : "│   "), true);

		return buf;
	}

	/**
	 * Change my shape as well as all my children's
	 * 
	 * @param newShape
	 */
	protected void reshape(final IntRect2D newShape)
	{
		shape = newShape;
		if (isLeaf())
			return;

		if (!newShape.contains(origin))
		{
			// origin = newShape.getCenter();
			Double2D d = newShape.getCenter();
			origin = new Int2D((int) Math.floor(d.x), (int) Math.floor(d.y));
		}

		for (int i = 0; i < children.size(); i++)
			children.get(i).reshape(getChildShape(i));
	}

	/**
	 * Construct the child's shape based the given id and the origin
	 * 
	 * @param childId
	 * @return child's shape
	 */
	protected IntRect2D getChildShape(final int childId)
		{
		final int[] ul = shape.ul().toArray();
		final int[] br = origin.toArray();
		final int[] sbr = shape.br().toArray();

		for (int i = 0; i < 2; i++)
			if (((childId >> (2 - i - 1)) & 0x1) == 1) // FIXME: what is this?
				{
				ul[i] = br[i];
				br[i] = sbr[i];
				}

		return new IntRect2D(new Int2D(ul), new Int2D(br));
		}

	/**
	 * @param p
	 * @return the index of my immediate child that contains the given point
	 */
	protected int toChildIdx(final NumberND p)
	{
		if (!shape.contains(p))
			throw new IllegalArgumentException("p " + p + " must be inside the shape " + shape);

		final double[] oc = origin.toArrayAsDouble(), pc = p.toArrayAsDouble();

		int[] mapd = new int[2];
		for (int i = 0; i < 2; i++)
		{
			int map = 1; // 0 or 1
			if (pc[i] < oc[i])
			{
				map = 0;
			}
			mapd[i] = map;
		}
		int r = 0;
		for (int i = 0; i < 2; i++)
		{
			int x = mapd[i];
			r = r << 1 | x;
		}
		
		for(int i=0; i<children.size(); i++)
		{
			if(children.get(i).shape.contains(p))
			{
				//System.out.println("*************p " + p + " is contained in " + children.get(i)+" "+children.get(i).shape);
				// System.out.println("*************r " + r);
				return i;
			}
		}
		return -1; //error
	}
}