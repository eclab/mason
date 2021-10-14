/** 
	A delay pipeline.  Elements placed in the delay are only available
	after a fixed amount of time.
*/


import sim.engine.*;
import sim.util.*;
import java.util.*;

/// FIXME: Anylogic has delay times which can be distributions, we should do that too
/// FIXME: we're going to have drifts in totalResource due to IEEE 754

public class Delay extends Source implements Receiver
	{
	Heap heap;
	
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
	
	double totalResource = 0.0;

	void throwInvalidNumberException(double capacity)
		{
		throw new RuntimeException("Capacities may not be negative or NaN.  capacity was: " + capacity);
		}

	public Delay(SimState state, Resource typical)
		{
		super(state, typical);
		heap = new Heap();
		}
	
	public void clear()
		{
		heap = new Heap();
		}
		
	protected double getDelayTime(Provider provider, Resource amount)
		{
		return 1.0;
		}
		
	public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
		{
		if (!resource.isSameType(amount)) 
			throwUnequalTypeException(amount);
		
		if (entities == null)
			{
			CountableResource cr = (CountableResource)amount;
			double maxIncoming = Math.min(capacity - totalResource, atMost);
			if (maxIncoming < atLeast) return false;
		
			CountableResource token = (CountableResource)(cr.duplicate());
			token.setAmount(maxIncoming);
			cr.decrease(maxIncoming);
			heap.add(token, getDelayTime(provider, amount));
			}
		else
			{
			if (heap.size() >= capacity) return false;	// we're at capacity
			heap.add(amount, getDelayTime(provider, amount));
			}
		return true;
		}

	protected void drop()
		{
		if (entities == null)
			resource.clear();
		else
			entities.clear();
		}
		
	protected void update()
		{
		drop();
		double time = state.schedule.getTime();
		
		while(((double)heap.getMinKey()) >= time)
			{
			Resource _res = (Resource)(heap.extractMin());
			if (entities == null)
				{
				CountableResource res = ((CountableResource)_res);
				resource.add(res);
				totalResource -= res.getAmount();
				}
			else
				{
				entities.add((Entity)(_res));
				}
			}
		}

	public String getName()
		{
		return "Delay(" + typical.getName() + ")";
		}		
	}
	