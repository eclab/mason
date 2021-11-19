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
	
	/** Returns true if we're currently blocked. */
	public boolean isBlocked()
		{
		return blocker != null;
		}
		
	/** Returns the BlockingProvider which has blocked us. */
	public BlockingProvider getBlocker()
		{
		return blocker;
		}

	public void block(BlockingProvider blocker)
		{
		this.blocker = blocker;
		}
		
	/** Unblocks the agent.  Override this method to call blocker.provide(...),
		but be sure to also call super(blocker, amount) */
	public void unblock(BlockingProvider blocker, double amount)
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