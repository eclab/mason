/** 
	An Steppable which can be Blocked.  Blocking a BlockableAgent increments
	(and unblocking decrements) an internal block count in the object.
	During step(...) the BlockableAgent checks to see if the block count is zero.
	If it is, then it calls unblockedStep(), which you implement.  Otherwise it does nothing.
	Thus you can set up an agent which, once blocked, doesn't respond to step(...).
	*/


import sim.engine.*;

public abstract class BlockableAgent implements Steppable, Blockable
	{
	BlockingProvider blocker = null;
	
	public boolean isBlocked()
		{
		return blocker != null;
		}
		
	public BlockingProvider getBlocker()
		{
		return blocker;
		}

	public void block(BlockingProvider blocker)
		{
		this.blocker = blocker;
		}
		
	public void unblock(BlockingProvider blocker)
		{
		this.blocker = null;
		}
		
	public final void step(SimState state)
		{
		if (!isBlocked()) unblockedStep(state);
		}

	/** Instead of overriding step(...), override this instead.  It will
		be called only if the agent is fully unblocked and step(...) occurs. */
	public abstract void unblockedStep(SimState state);
	
	}