/** 
	A blocking resource queue with a capacity.  Resources placed in the queue are available 
	immediately, but when the queue is empty, the Queue will attempt to satisfy
	requests by requesting from its upstream Provider if any.
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
	
	/** Gets the Queue's provider, if any.  Anyone can offer to the queue, but
		this provider exists to provide additional resources if the queue cannot
		provide enough immediately. Also every timestep the Queue will ask the
		provider to provide it with resources up to its capacity. */
	public Provider getProvider() { return provider; }

	/** Sets the Queue's provider, if any.  Anyone can offer to the queue, but
		this provider exists to provide additional resources if the queue cannot
		provide enough immediately.  Also every timestep the Queue will ask the
		provider to provide it with resources up to its capacity. */
	public void setProvider(Provider provider) { this.provider = provider; }

	protected double computeAvailable()
		{
		return resource.getAmount() + (provider == null ? 0 : provider.available());
		}

	/** Attempts to acquire the given resources, either directly or from the upstream provider. */
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

	public void consider(Provider provider, double amount)
		{
		Resource otherTyp = provider.getTypicalResource();
		if (!resource.isSameType(otherTyp)) 
			throwUnequalTypeException(otherTyp);
		
		double request = Math.max(amount, capacity - resource.getAmount());  // request no more than our capacity
		Resource token = provider.provide(0, request);
		if (token != null)
			{
			resource.add(token);
			offerBlocked();
			offerReceivers();
			}
		}
	
	public void update()
		{
		acquire(0, capacity - resource.getAmount());
		}
	}
	