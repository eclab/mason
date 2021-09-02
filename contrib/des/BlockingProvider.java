
/** 
	An object responsible for blocking a Blockable.  Each BlockList has a BlockingProvider
	which owns the BlockList.  The BlockingProvider is provided to the
	Blockable so it knows (particularly during unblock()) who blocked it.  We
	might eliminate this in the future if it proves useless.
*/

import sim.engine.*;
import java.util.*;

public abstract class BlockingProvider extends Provider implements Steppable
	{
	BlockList blocklist;
	SimState state;
	
	protected void throwUnequalTypeException(Resource res)
		{
		throw new RuntimeException("Expected resource type " + this.typical.getName() + "(" + this.typical.getType() + ")" +
			 	" but got resource type " + res.getName() + "(" + res.getType() + ")" );
		}

	protected void throwInvalidProvisionException()
		{
		throw new RuntimeException("provide(...) called with atLeast > atMost");
		}

	public BlockingProvider(SimState state, Resource typical)
		{
		super(state, typical);
		this.state = state;
		blocklist = new BlockList(this);
		receivers = new ArrayList<Receiver>();
		}
		
	protected abstract double computeAvailable();

	protected void informBlocked()
		{
		int n = blocklist.getSize();
		for(int i = 0; i < n; i++)			// we do no more than this to avoid starvation and infinite loops
			{
			double avail = available();
//			if (avail < 0) break;			// FIXME, should we bother with this to avoid calling getLastAtLeast unnecessarily?
			double atLeast = blocklist.getFirstAtLeast();
//			if (atleast == NO_AMOUNT) break;		// FIXME: this should NEVER happen because it can only happen if the list is empty
			if (avail >= atLeast)
				{
				blocklist.unblock();
				}
			else
				{
				break;
				}
			}
		}
		
	public Resource provideBlocking(Blockable blockable)
		{
		return provideBlocking(blockable, 1.0, 1.0);
		}

	public Resource provideBlocking(Blockable blockable, double atLeast, double atMost)
		{
		Resource r = provide(atLeast, atMost);
		if (r != null) 
			{
			return r;
			}
		else
			{
			// block 
			blocklist.block(blockable, atLeast, atMost);
			return null;
			}
		}

	public void step(SimState state)
		{
		informBlocked();			// do this FIRST before calling super(), which calls informReceivers
		super.step(state);
		}

	}