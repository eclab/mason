/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

/** 
    A delay pipeline.  Elements placed in the delay are only available
    after a fixed amount of time.
*/


import sim.engine.*;
import sim.util.distribution.*;
import sim.util.*;
import java.util.*;


/**
   A delay pipeline which allows different submitted elements to have different
   delay times.  Delay times are normally based on a provided distribution, or you can override
   the method getDelay(...) to customize delay times entirely based on the provider
   and resource being provided. 
*/

public class Delay extends SimpleDelay
    {
    Heap delayHeap;
    AbstractDistribution distribution = null;

    protected void buildDelay()
        {
        delayHeap = new Heap();
        }
                
    /** Creates a Delay with a 0 ordering and typical resource. */
    public Delay(SimState state, Resource typical)
        {
        super(state, 1.0, typical);
        }
        
	public double getSize() { return delayHeap.size(); }

	public double getTotal() { if (entities == null) return totalResource; else return delayHeap.size(); }

    public void clear()
        {
        delayHeap = new Heap();
        totalResource = 0.0;
        }
        
    /** Sets the distribution used to independently select the delay time for each separate incoming 
        resource.  If null, 1.0 will be used. */
    public void setDelayDistribution(AbstractDistribution distribution)
        {
        this.distribution = distribution;
        }
                
    /** Returns the distribution used to independently select the delay time for each separate incoming 
        resource.  If null, 1.0 will be used.  When a value is drawn from this distribution to determine
        delay, it will be put through Absolute Value first to make it positive.  Note that if your 
        distribution covers negative regions, you need to consider what will happen as a result and 
        make sure it's okay (or if you should be considering a positive-only distribution).  */
    public AbstractDistribution getDelayDistribution()
        {
        return this.distribution;
        }
                
    /** By default, provides Math.abs(getDelayDistribution().nextDouble()), or 1.0 if there is
        no provided distribution.  The point here is to guarantee that the delay will be positive;
        but note that if your distribution covers negative regions, you need to consider what
        will happen as a result and make sure it's okay (or if you should be considering
        a positive-only distribution).  Override this to provide a custom delay given the 
        provider and resource amount or type. */
    protected double getDelay(Provider provider, Resource amount)
        {
        if (distribution == null) return getDelayTime();
        else return Math.abs(distribution.nextDouble());
        }
                
    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
        if (!resource.isSameType(amount)) 
            throwUnequalTypeException(amount);
                
        if (isOffering()) throwCyclicOffers();  // cycle
        
        if (!(atLeast >= 0 && atMost >= atLeast))
        	throwInvalidAtLeastAtMost(atLeast, atMost);

        double nextTime = state.schedule.getTime() + getDelay(provider, amount);

        if (entities == null)
            {
            CountableResource cr = (CountableResource)amount;
            double maxIncoming = Math.min(capacity - totalResource, atMost);
            if (maxIncoming < atLeast) return false;
                
            CountableResource token = (CountableResource)(cr.duplicate());
            token.setAmount(maxIncoming);
            cr.decrease(maxIncoming);
            delayHeap.add(token, nextTime);
			totalResource += maxIncoming;            
            }
        else
            {
            if (delayHeap.size() >= capacity) return false;      // we're at capacity
            delayHeap.add(amount, nextTime);
			totalResource += 1;            
            }
       
        if (getAutoSchedules()) state.schedule.scheduleOnce(nextTime, getRescheduleOrdering(), this);
        
        return true;
        }

    protected void update()
        {
        drop();
        double time = state.schedule.getTime();
        
        Double minKey = (Double)delayHeap.getMinKey();
        while(minKey != null && minKey <= time)
            {
            Resource _res = (Resource)(delayHeap.extractMin());
            if (entities == null)
                {
                CountableResource res = ((CountableResource)_res);
                totalResource -= res.getAmount();
                resource.add(res);
                }
            else
                {
                entities.add((Entity)(_res));
				totalResource--;            
                }
 			minKey = (Double)delayHeap.getMinKey();		// grab the next one
            }
        }

    public String toString()
        {
        return "Delay@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : getName()) + typical.getName() + ", " + typical.getName() + ")";
        }               

	public void reset()
		{
		clear();
		}
    }
        
