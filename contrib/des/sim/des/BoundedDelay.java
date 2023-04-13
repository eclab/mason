/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import sim.util.distribution.*;
import sim.util.*;
import java.util.*;


/**
   A delay pipeline with a maximum legal delay time, and where all delay times must
   be integers > 0.  If a delay distribution is used, and it returns a non-integer
   value, then the ceiling of the value is used.  If the value is zero, the distribution
   is requeried over and over until a non-zero value is returned.
   
   <p>Bounded Delays insert themselves in the schedule to be pulsed every integer timestep,
   at ordering zero.  They ignore autoscheduling requests and ordering setting.
   
   <p>The point of a BoundedDelay is to allow for an O(1) delay insertion and removal even
   with arbitrary delay times (like a heap), using a very simple calendar queue.
*/

public class BoundedDelay extends Delay
    {
    private static final long serialVersionUID = 1;

	int maxDelayTime = -1;
	int currentDelayPos;
	LinkedList[] delays;

    void throwBoundedDelayTimeException(double time, int max)
        {
        throw new RuntimeException("In the BoundedDelay " + this + ", delay times must be <= " + max + ", but the delay time provided was " + time);
        }

    void throwNonIntegerDelayTimeException(double time)
        {
        throw new RuntimeException("In the BoundedDelay " + this + ", delay times must be integers, was " + time);
        }

    void throwNonPositiveMaxDelayTime(int time)
        {
        throw new RuntimeException("In the BoundedDelay " + this + ", max delay times must be > 0.  Max time provided was " + time);
        }

    void throwZeroDelayException()
        {
        throw new RuntimeException("In the BoundedDelay " + this + ", we tried " + MAX_DELAY_TRIES + " times to generate a non-zero delay from the delay distribution, and failed.  Giving up.  Use a better distribution.");
        }

    protected void buildDelay()
        {
        if (maxDelayTime < 0) return;	// not ready yet
        delays = new LinkedList[maxDelayTime + 1];
        for(int i = 0; i < delays.length; i++)
        	delays[i] = new LinkedList<DelayNode>();
        }
                
    /** Do not call this method: it will do nothing for the time being. */
    public void setRescheduleOrdering(int ordering) { System.err.println("Warning: BoundedDelay.setRescheduleOrdering(...) called, which does nothing."); }

    /** Creates a BoundedDelay with a 0 ordering, the given delay time, max delay time, and typical resource. */
    public BoundedDelay(SimState state, double delayTime, Resource typical, int maxDelayTime)
        {
        super(state, delayTime, typical);
        if (maxDelayTime <= 0)
        	throwNonPositiveMaxDelayTime(maxDelayTime);
        this.maxDelayTime = maxDelayTime;
        buildDelay(); 
        state.schedule.scheduleRepeating(this);
        rescheduleOrdering = 0;
        currentDelayPos = maxDelayTime;
        }

    /** Creates a Delay with a 0 ordering, a delay time of 1.0, max delay time, and typical resource. */
    public BoundedDelay(SimState state, Resource typical, int maxDelayTime)
        {
        super(state, typical);
        if (maxDelayTime <= 0)
        	throwNonPositiveMaxDelayTime(maxDelayTime);
        this.maxDelayTime = maxDelayTime;
        buildDelay(); 
        state.schedule.scheduleRepeating(this);
        rescheduleOrdering = 0;
        currentDelayPos = maxDelayTime;
        }
        
    public DelayNode[] getDelayedResources()
        {
        DelayNode[] nodes = new DelayNode[(int)getSize()];
        
        int pos = 0;
        for(int i = 0; i < delays.length; i++)
        	{
        	for(Object obj : delays[i])
        		{
        		nodes[pos++] = (DelayNode)obj;
        		}
        	}
        return nodes;
        }
    public boolean hideDelayedResources() { return true; }
    
    public double getSize() 
    	{ 
    	// This could be improved
        int count = 0;
        for(int i = 0; i < delays.length; i++)
        	{
        	count += delays[i].size();
        	}
    	return count; 
    	}

    public void clear()
        {
        super.clear();
        buildDelay();
        }
    
    public void setDelayTime(double delayTime) 
    	{
    	if (delayTime < 0 || (delayTime != delayTime)) 
    		 throwInvalidDelayTimeException(delayTime);
    	if (delayTime > maxDelayTime)
    		throwBoundedDelayTimeException(delayTime, maxDelayTime);
    	if (delayTime != (int)delayTime)
    		throwNonIntegerDelayTimeException(delayTime);
    	this.delayTime = delayTime; 
    	}

    public static final double MAX_DELAY_TRIES = 1000;
    protected double getDelay(Provider provider, Resource amount)
        {
        if (getUsesLastDelay() && recent != null)
        	{
        	// use the existing delay time
        	}
        else if (distribution == null) 
        	{
        	lastDelay = getDelayTime();
        	}
        else 
        	{
        	boolean failed = true;
        	for(int i = 0; i < MAX_DELAY_TRIES; i++)
        		{
	        	lastDelay = Math.ceil(Math.abs(distribution.nextDouble()));
	        	if (lastDelay != 0) { failed = false; break; }
        		}
        	if (failed)
        		throwZeroDelayException();
        	}
        return lastDelay;
        }
    
    void insert(DelayNode node, double time)
    	{
    	int pos = currentDelayPos + (int)time;
		if (pos > maxDelayTime) pos -= maxDelayTime;
    	delays[pos].add(node);
    	}
    	
    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
        if (getRefusesOffers()) { return false; }
        if (!getTypicalReceived().isSameType(amount)) 
            throwUnequalTypeException(amount);
                
        if (isOffering()) throwCyclicOffers();  // cycle
        
        if (!(atLeast >= 0 && atMost >= atLeast && atMost > 0 && atMost <= amount.getAmount()))
            throwInvalidAtLeastAtMost(atLeast, atMost, amount);

        double nextTime = getDelay(provider, amount);

        if (entities == null)
            {
            CountableResource cr = (CountableResource)amount;
            double maxIncoming = Math.min(Math.min(getCapacity() - totalDelayedResource - (getIncludesRipeResourcesInTotal() ? resource.getAmount() : 0), atMost), cr.getAmount());
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
            if (delayHeap.size() + (getIncludesRipeResourcesInTotal() ? entities.size() : 0) >= getCapacity()) return false; // we're at capacity
            DelayNode node = new DelayNode(amount, nextTime, provider);
            if (lookup != null) lookup.put(amount, node);
            insert(node, nextTime);
            totalDelayedResource += 1.0;            
            totalReceivedResource += 1.0;
            }
        
        return true;
        }

    protected void update()
        {
        // push 
        currentDelayPos++;
        
        if (getDropsResourcesBeforeUpdate()) 
            {
            drop();
            }

		if (delays[currentDelayPos].size() == 0)
			{
			return;
			}
		
		LinkedList<DelayNode> list = delays[currentDelayPos];
		delays[currentDelayPos] = new LinkedList<DelayNode>();
		
        if (entities == null)
        	{
        	for(DelayNode node : list)
        		{
           	 	if (lookup != null) lookup.remove(node.resource);
				if (node == recent) recent = null;	// all gone
				
				if (!node.dead)
					{	
					CountableResource res = (CountableResource)(node.getResource());
					totalDelayedResource -= res.getAmount();
					resource.add(res);
					}
				}
        	}
        else
        	{
        	for(DelayNode node : list)
        		{
           	 	if (lookup != null) lookup.remove(node.resource);
				if (node == recent) recent = null;	// all gone

				if (!node.dead)
					{	
					Entity res = (Entity)(node.getResource());
					entities.add(res);
					totalDelayedResource--;
					}   
				}
			}
        }

    public String toString()
        {
        return "BoundedDelay@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : (getName() + ": ")) + getTypicalProvided().getName() + ")";
        }               
    }
        
