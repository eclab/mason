/** 
	A delay pipeline.  Elements placed in the delay are only available
	after a fixed amount of time.
*/


import sim.engine.*;
import java.util.*;

public class Delay extends BlockingProvider implements Receiver
	{
	Resource resource;		// this holds all the "ripe" resources from the list
	
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

	/// CONCERN: we could see piling up of resources if nobody takes them
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
		return resource.reduce(atLeast, atMost);
		}
	
	public boolean consider(Provider provider, double amount)
		{
		if (!typical.isSameType(provider.provides())) throwUnequalTypeException(provider.provides());
		Resource token = provider.provide(0, amount);
		if (token != null)
			{
			resources.addFirst(new Node(token, state.schedule.getTime() + delay));
			informBlocked();
			return true;
			}
		return false;
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
	