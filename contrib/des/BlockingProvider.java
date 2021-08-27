
/** 
	An object responsible for blocking a Blockable.  Each BlockList has a BlockingProvider
	which owns the BlockList.  The BlockingProvider is provided to the
	Blockable so it knows (particularly during unblock()) who blocked it.  We
	might eliminate this in the future if it proves useless.
*/

import sim.engine.*;
import java.util.*;

public abstract class BlockingProvider implements Steppable, Provider
	{
	Resource typical;
	Resource resource;
	BlockList blocklist;
	SimState state;
	ArrayList<Receiver> receivers;
	
	protected void throwUnequalTypeException(Resource res)
		{
		throw new RuntimeException("Expected resource type " + this.typical.getName() + "(" + this.typical.getType() + ")" +
			 	" but got resource type " + res.getName() + "(" + res.getType() + ")" );
		}

	protected void throwInvalidProvisionException()
		{
		throw new RuntimeException("provide(...) called with atLeast > atMost");
		}

	public Resource provides() { return typical; }

	public BlockingProvider(SimState state, Resource typical)
		{
		this.typical = typical.duplicate();
		this.typical.setAmount(0.0);
		resource = typical.duplicate();
		resource.setAmount(0.0);
		this.state = state;
		blocklist = new BlockList(this);
		receivers = new ArrayList<Receiver>();
		}
		
	public boolean registerReceiver(Receiver receiver)
		{
		if (receivers.contains(receiver)) return false;
		receivers.add(receiver);
		return true;
		}
		
	public boolean unregisterReceiver(Receiver receiver)
		{
		if (receivers.contains(receiver)) return false;
		receivers.remove(receiver);
		return true;
		}

	protected abstract double computeAvailable();

	// This could be costly to recompute over and over again (by pushing down to the provider)
	// Perhaps we could compute it once and cache it...
	double availableCacheTimestamp = Double.NaN;
	double availableCache = 0;
	public double available()
		{
		if (availableCacheTimestamp == state.schedule.getTime())
			{
			return availableCache;
			}
		else
			{
			availableCacheTimestamp = state.schedule.getTime();
			availableCache = computeAvailable();
			return availableCache;
			}
		}

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
		
	protected void informReceivers()
		{
		double avail = available();
		if (avail == 0) return;
		for(Receiver r : receivers)
			{
			r.consider(this, avail);
			avail = available();
			if (avail == 0)
				return;
			}
		}
		
	public Resource provide()
		{
		return provide(1.0, 1.0);
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
		informBlocked();
		informReceivers();
		}

	}