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
import sim.portrayal.simple.*;
import sim.portrayal.*;
import java.awt.*;


/**
   A delay pipeline which allows different submitted elements to have different
   delay times.  Delay times are normally based on a provided distribution, or you can override
   the method getDelay(...) to customize delay times entirely based on the provider
   and resource being provided. 
*/

public class Delay extends SimpleDelay
    {
    private static final long serialVersionUID = 1;

    Heap delayHeap;
    AbstractDistribution distribution = null;

    protected void buildDelay()
        {
        delayHeap = new Heap();
        }
                
    /** Creates a Delay with a 0 ordering, the given delay time, and typical resource. */
    public Delay(SimState state, double delayTime, Resource typical)
        {
        super(state, delayTime, typical);
        }

    /** Creates a Delay with a 0 ordering, a delay time of 1.0, and typical resource. */
    public Delay(SimState state, Resource typical)
        {
        this(state, 1.0, typical);
        }
        
    /** Returns in an array all the Resources currently being delayed and not yet ready to provide,
    	along with their timestamps (when they are due to become available), combined as a DelayNode.  
    	Note that this is a different set of Resources than Provider.getEntities() returns.  
    	You can modify the array (it's yours), but do not modify the Resources stored inside, as they
    	are the actual Resources being delayed.
      */
    public DelayNode[] getDelayedResources()
    	{
    	DelayNode[] nodes = new DelayNode[delayHeap.size()];
    	if (nodes.length == 0) return nodes;
    	
    	Comparable[] keys = delayHeap.getKeys();
    	Object[] objs = delayHeap.getObjects();
    	for(int i = 0; i < nodes.length; i++)
    		{
    		nodes[i] = new DelayNode((Resource)(objs[i]),((Double)(keys[i])).doubleValue());
    		}
    	return nodes;
    	}
    public boolean hideDelayedResources() { return true; }
    
	public double getSize() { return delayHeap.size(); }

	public double getDelayed() { if (entities == null) return totalDelayedResource; else return delayHeap.size(); }

    public void clear()
        {
        delayHeap = new Heap();
        totalDelayedResource = 0.0;
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
    	if (getRefusesOffers()) { return false; }
        if (!typical.isSameType(amount)) 
            throwUnequalTypeException(amount);
                
        if (isOffering()) throwCyclicOffers();  // cycle
        
        if (!(atLeast >= 0 && atMost >= atLeast))
        	throwInvalidAtLeastAtMost(atLeast, atMost);

        double nextTime = state.schedule.getTime() + getDelay(provider, amount);

        if (entities == null)
            {
            CountableResource cr = (CountableResource)amount;
            double maxIncoming = Math.min(Math.min(capacity - totalDelayedResource, atMost), cr.getAmount());
            if (maxIncoming < atLeast) return false;
                
            CountableResource token = (CountableResource)(cr.duplicate());
            token.setAmount(maxIncoming);
            cr.decrease(maxIncoming);
            delayHeap.add(token, nextTime);
			totalDelayedResource += maxIncoming;            
			totalReceivedResource += maxIncoming;
            }
        else
            {
            if (delayHeap.size() >= capacity) return false;      // we're at capacity
            delayHeap.add(amount, nextTime);
			totalDelayedResource += 1;            
			totalReceivedResource += 1.0;
            }
       
        if (getAutoSchedules()) state.schedule.scheduleOnce(nextTime, getRescheduleOrdering(), this);
        
        return true;
        }

    protected void update()
        {
        if (getDropsResourcesBeforeUpdate()) 
        	{
        	drop();
        	}

        double time = state.schedule.getTime();
        
        Double minKey = (Double)delayHeap.getMinKey();
        while(minKey != null && minKey <= time)
            {
            Resource _res = (Resource)(delayHeap.extractMin());
            if (entities == null)
                {
                CountableResource res = ((CountableResource)_res);
                totalDelayedResource -= res.getAmount();
                resource.add(res);
                }
            else
                {
                entities.add((Entity)(_res));
				totalDelayedResource--;            
                }
 			minKey = (Double)delayHeap.getMinKey();		// grab the next one
            }
        }

    public String toString()
        {
        return "Delay@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : getName()) + typical.getName() + ", " + typical.getName() + ")";
        }               
        
    boolean refusesOffers = false;
	public void setRefusesOffers(boolean value) { refusesOffers = value; }
    public boolean getRefusesOffers() { return refusesOffers; }
    }
        
