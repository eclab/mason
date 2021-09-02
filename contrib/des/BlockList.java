/** 
	A list of blocked Steppables (called Blockables).  If a Blockable is
	placed on the list, it will be told that is blocked.  Blockables can
	be removed from the list and told that they are unblocked.  A BlockList
	is often used if a blocking resource is requested which cannot be provided.
	
	<p>BlockList is fair: the order in which arbitrary agents are unblocked is
	in the order in which they were blocked in the first place.  However to take
	advantage of this, your Blockable agent must do its useful work when its
	unblock() method is called.
*/



import java.util.*;

public class BlockList
	{
	public static final double NO_AMOUNT = -1;
	
	public static class Node
		{
		public Blockable blockable;
		public double atLeast;
		public double atMost;
		public Node(Blockable blockable, double atLeast, double atMost)
			{
			this.blockable = blockable;
			this.atLeast = atLeast;
			this.atMost = atMost;
			}
		}
		 
	LinkedList<Node> blocked = new LinkedList<>();
	BlockingProvider blockingProvider;
	
	public BlockList(BlockingProvider blockingProvider)
		{
		this.blockingProvider = blockingProvider;
		}
		
	public BlockingProvider getBlockingProvider()
		{
		return blockingProvider;
		}
	
	public boolean isEmpty()
		{
		return blocked.isEmpty();
		}
		
	public int getSize()
		{
		return blocked.size();
		}
		
	/** Blocks the given agent.  The agent is informed that he is
		blocked and put on the BlockList.  Returns FALSE
		if the agent was already on the BlockList.  */
	public boolean block(Blockable step, double amount, double atMost)
		{
		if (blocked.contains(step)) return false;			// FIXME: O(n), this is really ugly and worrisome
		blocked.add(new Node(step, amount, atMost));
		step.block(blockingProvider);
		return true;
		}
	
	/** Potentially unblocks 1 agent.  The agent is informed that is is
		unblocked and removed from the BlockList.  Returns FALSE
		if an agent was not unblocked.  */
	public boolean unblock(double amount)
		{
		if (blocked.isEmpty()) return false;
		Node node = blocked.getLast();
		if (node.atLeast <= amount)
			{
			blocked.remove();
			node.blockable.unblock(blockingProvider, amount < node.atMost ? amount : node.atMost);
			}
		return true;
		}

	/** Removes the Blockable from the blocklist.  Does not call unblock() on the Blockable.
		Returns false if the blockable was not found on the blocklist.  */
	public boolean removeBlockable(Blockable blockable)
		{
		Iterator<Node> iter = blocked.iterator();
		while(iter.hasNext())
			{
			Node n = iter.next();
			if (n.blockable == blockable)
				{
				iter.remove();
				return true;
				}
			}
		return false;
		}
	}
