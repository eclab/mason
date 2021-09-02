/** 
	A delay pipeline.  Elements placed in the delay are only available
	after a fixed amount of time.
*/


import sim.engine.*;
import java.util.*;

/// FIXME: Anylogic has delay times which can be distributions, we should do that too
/// FIXME: we're going to have drifts in totalResource due to IEEE 754

public class Delay extends BlockingProvider implements Receiver
	{
	Resource resource;		// this holds all the "ripe" resources from the list
	double totalResource = 0.0;
	
	class Node
		{
		public Resource resource;
		public double timestamp;
		
		public Node(Resource resource, double timestamp)
			{
			this.resource = resource;
			this.timestamp = timestamp;
			}
		}
	
	LinkedList<Node> resources = new LinkedList<>();
	double delay;
	Provider provider;

	double capacity = Double.POSITIVE_INFINITY;
	/** Returns the maximum available resources that may be built up. */
	public double getCapacity() { return capacity; }
	/** Set the maximum available resources that may be built up. */
	public void setCapacity(double d) { capacity = d; }

	public Delay(SimState state, double delay, Resource typical)
		{
		super(state, typical);
		this.delay = delay;
		resource = typical.duplicate();
		resource.setAmount(0.0);
		}
		
	public Provider getProvider() { return provider; }
	public void setProvider(Provider provider) { this.provider = provider; }

	protected double computeAvailable()
		{
		double avail = 0;
		Iterator<Node> iterator = resources.descendingIterator();
		while(iterator.hasNext())
			{
			Node node = iterator.next();
			if (node.timestamp <= state.schedule.getTime())
				{
				avail += node.resource.getAmount();
				}
			}
		return avail;
		}

	void acquire(double atLeast, double atMost)
		{
		if (resources.isEmpty())
			return;

		double time = state.schedule.getTime();
		
		// acquire this amount
		Iterator<Node> iterator = resources.descendingIterator();
		while(iterator.hasNext())
			{
			Node node = iterator.next();
			if (node.timestamp >= time)	// it's ripe
				{
				double amt = node.resource.getAmount();
				resource.add(node.resource);
				iterator.remove();
				}
			else break;		// don't process any more
			}
		}

	public Resource provide(double atLeast, double atMost)
		{
		if (!blocklist.isEmpty()) return null;		// someone is ahead of us in the queue
		// we always check the list so we avoid a leak
		acquire(atLeast, atMost);
		Resource res = resource.reduce(atLeast, atMost);
		if (res != null) 
			{
			totalResource -= res.getAmount();
			/// This hack makes sure that the drift doesn't drop us below 0
			if (totalResource < 0) totalResource = 0;
			}
		return res;
		}
	
	public void consider(Provider provider, double amount)
		{
		Resource otherTyp = provider.getTypicalResource();
		if (!resource.isSameType(otherTyp)) 
			throwUnequalTypeException(otherTyp);
		
		double request = Math.max(amount, capacity - totalResource);  // request no more than our capacity
		Resource token = provider.provide(0, request);
		if (token != null)
			{
			totalResource += token.getAmount();
			resources.addFirst(new Node(token, state.schedule.getTime() + delay));
			informBlocked();
			}
		}

	public void step(SimState state)
		{
		if (provider != null) 
			{
			Resource res = provider.provide(0, Double.POSITIVE_INFINITY);
			if (res != null)
				resources.addFirst(new Node(res, state.schedule.getTime() + delay));
			}
		super.step(state);
		}
	}
	