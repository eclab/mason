/** 
	A delay pipeline.  Elements placed in the delay are only available
	after a fixed amount of time.
*/


import sim.engine.*;
import java.util.*;

/// FIXME: Anylogic has delay times which can be distributions, we should do that too
/// FIXME: we're going to have drifts in totalResource due to IEEE 754

public class Delay extends Source implements Receiver
	{
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
	LinkedList<Node> resources = new LinkedList<>();
	double delay;

	void throwInvalidNumberException(double capacity)
		{
		throw new RuntimeException("Capacities may not be negative or NaN.  capacity was: " + capacity);
		}

	public Delay(SimState state, double delay, Resource typical)
		{
		super(state, typical);
		this.delay = delay;
		}
		
	public void accept(Provider provider, Resource amount, double atLeast, double atMost)
		{
		if (!resource.isSameType(amount)) 
			throwUnequalTypeException(amount);
		
		double maxIncoming = Math.min(capacity - totalResource, atMost);
		if (maxIncoming < atLeast) return;
		
		Resource token = amount.duplicate();
		token.setAmount(maxIncoming);
		amount.decrease(maxIncoming);
		resources.addFirst(new Node(token, state.schedule.getTime() + delay));
		}

	protected void update()
		{
		resource.clear();
		
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

	public String getName()
		{
		return "";
		}		
	}
	