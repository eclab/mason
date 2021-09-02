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
		
	public double getFirstAtLeast()
		{
		if (blocked.isEmpty()) return NO_AMOUNT;
		else return blocked.getLast().atLeast;
		}

	public double getFirstAtMost()
		{
		if (blocked.isEmpty()) return NO_AMOUNT;
		else return blocked.getLast().atMost;
		}

	public Blockable getFirstBlockable()
		{
		if (blocked.isEmpty()) return null;
		else return blocked.getLast().blockable;
		}
		
	/// FIXME: this is O(n), can we cache this?  We can't maintain this list on the fly
	public double getTotalAtLeast()
		{
		// we need a more accurate summing mechanism if we're summing reals
		double total = 0.0;
		for(Node node : blocked)
			total += node.atLeast;
		return total;
		}
		
	/// FIXME: this is O(n), can we cache this?  We can't maintain this list on the fly
	public double getTotalAtMost()
		{
		// we need a more accurate summing mechanism if we're summing reals
		double total = 0.0;
		for(Node node : blocked)
			total += node.atMost;
		return total;
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
	
	/** Unblocks 1 agent.  The agent is informed that is is
		unblocked and removed from the BlockList.  Returns FALSE
		if there were no agents to unblock.  */
	public boolean unblock()
		{
		if (blocked.isEmpty()) return false;
		Node node = blocked.removeLast();
		node.blockable.unblock(blockingProvider);
		return true;
		}
	}
