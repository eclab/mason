/** 
	A resource queue or semaphore.  Resources placed in the queue are available 
	immediately, but when the queue is empty 
*/

import sim.engine.*;
import java.util.*;

/// FIXME: Anylogic has queues with different ordering policies.  That doesn't
/// make sense for us because we just have blobs, but maybe we should revisit this.

public class Queue extends Source implements Receiver
	{
	Provider provider;
	
	public Queue(SimState state, Resource typical)
		{
		super(state, typical);
		}
		
	public Provider getProvider() { return provider; }
	public void setProvider(Provider provider) { this.provider = provider; }

	protected double computeAvailable()
		{
		return resource.getAmount() + (provider == null ? 0 : provider.available());
		}

	void acquire(double atLeast, double atMost)
		{
		if (provider != null)
			{
			double avail = resource.getAmount();
			if (avail < atMost)
				{
				Resource r = provider.provide(atLeast - avail < 0 ? 0 : atLeast - avail, atMost - avail);		// we are conservative, we don't request the max available
				if (r != null) resource.add(r);
				}
			}
		}

	public Resource provide(double atLeast, double atMost)
		{
		if (!blocklist.isEmpty()) return null;		// someone is ahead of us in the queue
		acquire(atLeast, atMost);
		return resource.reduce(atLeast, atMost);
		}

	protected void informBlocked()
		{
		if (blocklist.isEmpty()) return;

		// FIXME: this can cause lots of problems because it's O(n), and I'm not
		// sure it's needed
		//
		//
		// request from the provider?
		//double totalAtLeast = blocklist.getTotalAtLeast();
		//double totalAtMost = blocklist.getTotalAtMost();
		//acquire(totalAtLeast, totalAtMost);
		
		super.informBlocked();
		}

	public boolean consider(Provider provider, double amount)
		{
		if (!resource.isSameType(provider.provides())) 
			throwUnequalTypeException(provider.provides());
		
		double request = Math.max(amount, capacity - resource.getAmount());  // request no more than our capacity
		Resource token = provider.provide(0, request);
		if (token != null)
			{
			resource.add(token);
			informBlocked();
			informReceivers();
			return true;
			}
		return false;
		}
	
	public void update()
		{
		acquire(0, capacity - resource.getAmount());
		}
	}
	