
/** 
	A Provider which can give blocking provide requests.  
	Each Provider has a blocklist, and if an incoming blocking request cannot be
	fulfilled, its Blockable is put on the blocklist to wait until the requests
	can be fulfilled later.  Requests are fulfilled in order to the waiting
	Blockables on the blocklist, and only if they cannot accept available
	resources are the resources offered to registered Receivers.
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

	/** Make offers to all the agents on the blocklist, in order. */
	protected void offerBlocked()
		{
		int n = blocklist.getSize();
		for(int i = 0; i < n; i++)			// we do no more than this to avoid starvation and infinite loops
			{
			if (!blocklist.unblock(available()))
				break;
			}
		}
		
	/** Returns whether the blocklist is empty. */
	public boolean isBlocklistEmpty() { return blocklist.isEmpty(); }

	/** Removes the Blockable from the blocklist.  Does not call unblock() on the Blockable. 
		Returns false if the blockable was not found on the blocklist. */
	public boolean removeBlockable(Blockable blockable)
		{
		return blocklist.removeBlockable(blockable);
		}
		
	/** Provide exactly 1.0 of a resource, in a blocking fashion, to a Blockable, else return null and put the Blockable on the block list. */
	public Resource provideBlocking(Blockable blockable)
		{
		return provideBlocking(blockable, 1.0, 1.0);
		}

	/** Provide a resource, in a blocking fashion, to a Blockable, else return null and put the Blockable on the block list. */
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
		offerBlocked();			// do this FIRST before calling super(), which calls offerReceivers
		if (isBlocklistEmpty()) 	// we ONLY call super(), which offers to the receivers, if we've satisfied all the blockables
			super.step(state);
		}

	}