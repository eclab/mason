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
	public Queue(SimState state, Resource typical)
		{
		super(state, typical);
		}
	
	public void accept(Provider provider, Resource amount, double atLeast, double atMost)
		{
		if (!typical.isSameType(amount)) throwUnequalTypeException(amount);
		
		// FOR NOW... 
		//
		// If my downstream receivers don't take anough of the offer,
		// (1) I need to have atLeast extra capacity so I can take the offer myself
		// OR
		// (2) I need to have atLeast in my resource so I can refuse the offer and pay it back
		//
		// Otherwise I have to query downstream and I don't want to do that
		//
		// FIXME: should we query the downstream receivers for how much they can accept?
		// May be costly.
		
		if (capacity - resource.getAmount() >= atLeast)		// I can cover the offer myself
			{
			// Load no more than atMost into my resource.  This may push me above capacity.
			resource.add(amount, atMost);
		
			offerReceivers(resource);
		
			// Reduce to capacity
			if (resource.getAmount() > capacity)
				{
				amount.increase(resource.getAmount() - capacity);
				resource.setAmount(capacity);
				}
			}
		else if (resource.getAmount() >= atLeast)		// I can pay the offer back if nobody takes it
			{
			double originalAmount = amount.getAmount();
			double maximumRemaining = originalAmount - atLeast;
			
			// I need to squirrel away the amount to pay back
			resource.decrease(atLeast);
			
			// Load no more than atMost into my resource.  This may push me above capacity.
			resource.add(amount, atMost);

			offerReceivers(resource);

			// Reduce to capacity
			if (resource.getAmount() > capacity)
				{
				amount.increase(resource.getAmount() - capacity);
				resource.setAmount(capacity);
				}
			
			double finalAmount = amount.getAmount();
			if (finalAmount > maximumRemaining)		// I didn't take enough, put it all back
				{
				amount.increase(originalAmount - finalAmount);
				resource.decrease(originalAmount - finalAmount);
				}
			}
		else	
			{
			// oh well, cannot accept
			}
		}
	
	public void update()
		{
		// do nothing
		}

	public String getName()
		{
		return "";
		}		
	}
	