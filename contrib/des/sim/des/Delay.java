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


/**
   A delay pipeline which allows different submitted elements to have different
   delay times.  Delay times are normally based on a provided distribution, or you can override
   the method getDelay(...) to customize delay times entirely based on the provider
   and resource being provided. 
   
   <p>If multiple resources are inserted into the Delay scheduled to come available
   at the exact same time, the order in which they will be offered is undefined.
*/

public class Delay extends SimpleDelay
    {
    private static final long serialVersionUID = 1;

    Heap delayHeap;
    AbstractDistribution distribution = null;
    
	// this is a cache of the most recently inserted node to enable a little linked list for a bit of optimization
	DelayNode recent = null;
	
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
            nodes[i] = (DelayNode)objs[i];
            }
        return nodes;
        }
    public boolean hideDelayedResources() { return true; }
    
    public double getSize() { return delayHeap.size(); }

    public void clear()
        {
        super.clear();
        delayHeap = new Heap();
        }
    
    boolean usesLastDelay = false;
    
    /** Sets whether getDelay should simply return the delay time used by the most recent resource
    	added to the Delay. If there is no such resource, or if that resource has since been 
    	removed from the Delay, or if its delay time has passed, then a delay value of 1.0 will
    	be used as a default. */
    public void setUsesLastDelay(boolean val) { usesLastDelay = val; }
    
    /** Sets whether getDelay should simply return the delay time used by the most recent resource
    	added to the Delay. If there is no such resource, or if that resource has since been 
    	removed from the Delay, or if its delay time has passed, then a delay value of 1.0 will
    	be used as a default. */
    public boolean getUsesLastDelay() { return usesLastDelay; }
    	
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
                
	double lastDelay = 1.0;
    /** By default, provides Math.abs(getDelayDistribution().nextDouble()), or 1.0 if there is
        no provided distribution.  The point here is to guarantee that the delay will be positive;
        but note that if your distribution covers negative regions, you need to consider what
        will happen as a result and make sure it's okay (or if you should be considering
        a positive-only distribution).  Override this to provide a custom delay given the 
        provider and resource amount or type. */
    protected double getDelay(Provider provider, Resource amount)
        {
        if (getUsesLastDelay())
        	{
        	if (recent == null) lastDelay = 1.0;
        	}
        else if (distribution == null) 
        	{
        	lastDelay = getDelayTime();
        	}
        else 
        	{
        	lastDelay = Math.abs(distribution.nextDouble());
        	}
        return lastDelay;
        }
        
    void insert(DelayNode node, double nextTime)
    	{
    	// Handle caching.  If the most recently inserted DelayNode is still there
    	// and has the exact same timestamp, let's add ourselves as a linked list hanging off of it
    	// rather than O(lg n) insertion into the heap
    	if (recent != null && recent.timestamp == nextTime)
    		{
    		// insert the node right after recent
    		node.next = recent.next;
    		recent.next = node;
    		}
    	else
    		{
	        delayHeap.add(recent = node, nextTime);
	        }
    	}


    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
        if (getRefusesOffers()) { return false; }
        if (!getTypicalReceived().isSameType(amount)) 
            throwUnequalTypeException(amount);
                
        if (isOffering()) throwCyclicOffers();  // cycle
        
        if (!(atLeast >= 0 && atMost >= atLeast && atMost > 0))
            throwInvalidAtLeastAtMost(atLeast, atMost);

        double nextTime = state.schedule.getTime() + getDelay(provider, amount);

        if (entities == null)
            {
            CountableResource cr = (CountableResource)amount;
            double maxIncoming = Math.min(Math.min(getCapacity() - totalDelayedResource - (getIncludesRipeResourcesInTotal() ? resource.getAmount() : 0), atMost), cr.getAmount());
            if (maxIncoming < atLeast) return false;
                
            CountableResource token = (CountableResource)(cr.duplicate());
            token.setAmount(maxIncoming);
            cr.decrease(maxIncoming);
            insert(new DelayNode(amount, nextTime, provider), nextTime);
            totalDelayedResource += maxIncoming;            
            totalReceivedResource += maxIncoming;
            }
        else
            {
            if (delayHeap.size() + (getIncludesRipeResourcesInTotal() ? entities.size() : 0) >= getCapacity()) return false; // we're at capacity
            insert(new DelayNode(amount, nextTime, provider), nextTime);
            totalDelayedResource += 1.0;            
            totalReceivedResource += 1.0;
            }
       
        if (getAutoSchedules()) state.schedule.scheduleOnce(nextTime, getRescheduleOrdering(), this);
        
        return true;
        }


	static final boolean clearWhenEmpty = true;
    protected void update()
        {
        if (getDropsResourcesBeforeUpdate()) 
            {
            drop();
            }

        double time = state.schedule.getTime();
		Double minKey = (Double)delayHeap.getMinKey();
		
        if (entities == null)
        	{
			while(minKey != null && minKey <= time)
				{
				DelayNode node = (DelayNode)(delayHeap.extractMin());
				if (node == recent) recent = null;	// all gone
				
				// We'll walk down the node's internal linked list and update all of them.
				while (node != null)
					{				
					CountableResource res = (CountableResource)(node.getResource());
					totalDelayedResource -= res.getAmount();
					resource.add(res);
					node = node.next;     // handle caching
					}
				minKey = (Double)delayHeap.getMinKey();         // grab the next one
				}
        	}
        else
        	{
			while(minKey != null && minKey <= time)
				{
				DelayNode node = (DelayNode)(delayHeap.extractMin());
				if (node == recent) recent = null;	// all gone

				// We'll walk down the node's internal linked list and update all of them.
				while (node != null)
					{
					Entity res = (Entity)(node.getResource());
					entities.add(res);
					totalDelayedResource--;            
					node = node.next;     // handle caching
					}
				minKey = (Double)delayHeap.getMinKey();         // grab the next one
				}
			}
			
		if (minKey == null && clearWhenEmpty)
			{
			delayHeap.clear();
			}
        }

    public String toString()
        {
        return "Delay@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : (getName() + ": ")) + getTypicalProvided().getName() + ")";
        }               
        
    boolean refusesOffers = false;
    public void setRefusesOffers(boolean value) { refusesOffers = value; }
    public boolean getRefusesOffers() { return refusesOffers; }
    }
        
