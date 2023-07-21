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
    double lastDelayTime = Schedule.BEFORE_SIMULATION;
    boolean cumulative = false;
    
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
        super(state, typical);
        }
        
    /** Sets the delay time.  Unlike a SimpleDelay, a Delay does not also clear its delay queue
    	when setting the delay time: and so you are free to call this method any time you need 
    	to without issues.  The default delay time is 1.0. Delay times may not be negative or NaN.  
    	*/
    public void setDelayTime(double delayTime) 
    	{
    	if (delayTime < 0 || (delayTime != delayTime)) 
    		 throwInvalidDelayTimeException(delayTime);
    	this.delayTime = delayTime; 
    	}


	/** Returns whether the delay is cumulative. 
	When you submit a Resource to a Delay, its final delay time is computed.  
	Normally this is an absolute time: for example, if the current time is 9.2, 
	and the relative delay interval is computed as 2.3 steps, then the Resource 
	will become available at 11.5.  However, another "cumulative" option is to compute the 
	final delay time relative to the last delay time used.   For example, if 
	the final delay time of the previous Resource entered was 13.4, and the 
	relative delay interval is 2.3 steps, then the new Resource will become 
	available at 15.6.  This makes it easy to compute each Resource as 
	"processed" over a period of time after the previous Resource was 
	processed. 
	
	<p>For the first Resource, or after you reset the Delay, or after you restart
	the simulation, or if the Delay is empty, the delay time will be relative to 
	the current time, or the Simulation Epoch, whichever is later. */

	public boolean isCumulative() { return cumulative; }

	/** Returns whether the delay is cumulative. 
	When you submit a Resource to a Delay, its final delay time is computed.  
	Normally this is an absolute time: for example, if the current time is 9.2, 
	and the relative delay interval is computed as 2.3 steps, then the Resource 
	will become available at 11.5.  However, another "cumulative" option is to compute the 
	final delay time relative to the last delay time used.   For example, if 
	the final delay time of the previous Resource entered was 13.4, and the 
	relative delay interval is 2.3 steps, then the new Resource will become 
	available at 15.6.  This makes it easy to compute each Resource as 
	"processed" over a period of time after the previous Resource was 
	processed. 
	
	<p>For the first Resource, or after you reset the Delay, or after you restart
	the simulation, or if the Delay is empty, the delay time will be relative to 
	the current time, or the Simulation Epoch, whichever is later. */
	public void setCumulative(boolean val) { cumulative = val; }
	
	public double getLastDelayTime()
		{
		return lastDelayTime;
		}	

    /** Returns in an array all the Resources currently being delayed and not yet ready to provide,
        along with their timestamps (when they are due to become available), combined as a DelayNode.  
        Note that this is a different set of Resources than Provider.getEntities() returns.  
        You can modify the array (it's yours), but do not modify the DelayNodes nor the 
        Resources stored inside them, as they are the actual Resources being delayed.
    */
    public DelayNode[] getDelayedResources()
        {
        Object[] objs = delayHeap.getObjects();
		if (objs.length ==  0) return new DelayNode[0];
		
        // Count the actual number of nodes by following the node.next chains
        int count = 0;
        for(int i = 0; i < objs.length; i++)
        	{
        	DelayNode node = (DelayNode)(objs[i]);
        	while(node != null)
        		{
        		count++;
        		node = node.next;
        		}
        	}
        	
        // Load the actual nodes by following the node.next chains
        DelayNode[] nodes = new DelayNode[count];
        count = 0;	// we'll reuse it as a position
        for(int i = 0; i < objs.length; i++)
        	{
        	DelayNode node = (DelayNode)(objs[i]);
        	while(node != null)
        		{
        		nodes[count++] = node;
        		node = node.next;
        		}
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
    
    /** Sets whether getDelay(...) should simply return the delay time used by the most recent resource
    	added to the Delay.  If there is no such resource, or if that resource has since been 
    	removed from the Delay, or if its delay time has passed, then a delay value of 1.0 will
    	be used as a default. */
    public void setUsesLastDelay(boolean val) { usesLastDelay = val; }
    
    /** Sets whether getDelay(...) should simply return the delay time used by the most recent resource
    	added to the Delay. If there is no such resource, or if that resource has since been 
    	removed from the Delay, or if its delay time has passed, then a delay value of 1.0 will
    	be used as a default. */
    public boolean getUsesLastDelay() { return usesLastDelay; }
    	
    /** Sets the distribution used to independently select the delay time for each separate incoming 
        resource.  If null, the value of getDelayTime() is used for the delay time. */
    public void setDelayDistribution(AbstractDistribution distribution)
        {
        this.distribution = distribution;
        }
                
    /** Returns the distribution used to independently select the delay time for each separate incoming 
        resource.  If null, the value of getDelayTime() is used for the delay time.  
        When a value is drawn from this distribution to determine
        delay, it will be put through Absolute Value first to make it positive.  Note that if your 
        distribution covers negative regions, you need to consider what will happen as a result and 
        make sure it's okay (or if you should be considering a positive-only distribution).  */
    public AbstractDistribution getDelayDistribution()
        {
        return this.distribution;
        }
                
	double lastDelay = 1.0;
	protected void setLastDelay(double val) { lastDelay = val; }
	protected double getLastDelay() { return lastDelay; }
	
    /** Returns the appropriate delay value for the given provider and resource amount.
    	You can override this as you see fit, though the defaults should work fine in most 
    	cases.  The defaults are: if getUsesLastDelay(), and there has been at least one 
    	previous resource entered into the Delay already, then the most recent previous 
    	delay time is used.  Otherwise if the delay distribution has been set, it is queried
    	and its absolute value is used to produce a random delay time under the distribution
    	(delay times may not be negative or NaN).  Otherwise the fixed delay time is used 
    	(which defaults to 1.0).  Override this to provide a custom delay given the 
        provider and resource amount or type. */
    protected double getDelay(Provider provider, Resource amount)
        {
        if (getUsesLastDelay() && recent != null)
        	{
        	// use the existing delay time
        	}
        else if (distribution == null) 
        	{
        	setLastDelay(getDelayTime());
        	}
        else 
        	{
        	setLastDelay(Math.abs(distribution.nextDouble()));
        	}
        return getLastDelay();
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
        
        if (!(atLeast >= 0 && atMost >= atLeast && atMost > 0 && atMost <= amount.getAmount()))
            throwInvalidAtLeastAtMost(atLeast, atMost, amount);

        double nextTime = 0;
        if (cumulative)
    		{
    		if (lastDelayTime <= Schedule.BEFORE_SIMULATION ||
    			lastDelayTime > state.schedule.time() ||
    			delayHeap.isEmpty())		// so it's possible that the new delay time will be earlier than the current time
    			{
    			lastDelayTime = state.schedule.time();
    			if (lastDelayTime <= Schedule.BEFORE_SIMULATION)
    				lastDelayTime = Schedule.EPOCH;
    			}
    		nextTime = lastDelayTime + getDelay(provider, amount);
    		lastDelayTime = nextTime;
    		}
    	else 
    		{
    		nextTime = state.schedule.getTime() + getDelay(provider, amount);
    		}

        if (entities == null)
            {
            CountableResource cr = (CountableResource)amount;
            double maxIncoming = Math.min(Math.min(getCapacity() - totalDelayedResource - (getIncludesAvailableResourcesInTotal() ? resource.getAmount() : 0), atMost), cr.getAmount());
            if (maxIncoming < atLeast) return false;
                
            CountableResource token = (CountableResource)(cr.duplicate());
            token.setAmount(maxIncoming);
            cr.decrease(maxIncoming);
            DelayNode node = new DelayNode(token, nextTime, provider);
            if (lookup != null) lookup.put(token, node);
            insert(node, nextTime);
            totalDelayedResource += maxIncoming;            
            totalReceivedResource += maxIncoming;
            }
        else
            {
            if (delayHeap.size() + (getIncludesAvailableResourcesInTotal() ? entities.size() : 0) >= getCapacity()) return false; // we're at capacity
            DelayNode node = new DelayNode(amount, nextTime, provider);
            if (lookup != null) lookup.put(amount, node);
            insert(node, nextTime);
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
           	 	if (lookup != null) lookup.remove(node.resource);
				if (node == recent) recent = null;	// all gone
				
				// We'll walk down the node's internal linked list and update all of them.
				while (node != null)
					{			
					if (!node.dead)
						{	
						CountableResource res = (CountableResource)(node.getResource());
						totalDelayedResource -= res.getAmount();
						resource.add(res);
						}
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
           	 	if (lookup != null) lookup.remove(node.resource);
				if (node == recent) recent = null;	// all gone

				// We'll walk down the node's internal linked list and update all of them.
				while (node != null)
					{
					if (!node.dead)
						{	
						Entity res = (Entity)(node.getResource());
						entities.add(res);
						totalDelayedResource--;
						}   
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
        
    public void reset()
    	{
    	super.reset();
    	lastDelayTime = Schedule.BEFORE_SIMULATION;
    	}            
        
    boolean refusesOffers = false;
    public void setRefusesOffers(boolean value) { refusesOffers = value; }
    public boolean getRefusesOffers() { return refusesOffers; }
    }
        
