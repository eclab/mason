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
   A delay pipeline where all delay amounts must be multiples of integers > 0 and less 
   than or equal to a certain maximum bound, which is equal to a delay step length 
   times the integer multiple. A BoundedDelay must be auto-scheduled at a certain 
   time to start it. Thus we have:
   
   <ul>
   <li> An initial start time
   <li> A maximum delay step length (an integer > 0)
   <li> A delay interval (an integer > 0)
   </ul>
   
   <p>The point of BoundedDelay is that it can do many things that you would ordinarily
   use a Delay for, but it has an O(1) operation rather than an O(lg n) operation, often
   resulting in dramatic increases in efficiency for some models.
   
   <p> When a BoundedDelay accepts a Resource, it must first compute the delay length.
   If you are using a fixed delay time, this delay time must be greater than 0 and less
   than or equal to the maximum delay time, which is equal to the delay step length
   times the delay interval.  If you are using a distribution, then BoundedDelay will
   try up to MAX_DELAY_TRIES times to pull a value from the distribution and take
   the absolute value, until it finds a result that is within these bounds.  If it cannot
   find a valid result, it will issue an exception -- you don't want that.  So use a 
   distribution which does the job properly.
   
   <p>Once the BoundedDelay has a valid delay length, it adds the Resource to its delay array.
   
   <p>BoundedDelay should be stepped by the Schedule at multiples of the delay step length 
   starting at some initial time.  You should probably do this by calling autoScheduleAt(...).
   If the BoundedDelay is stepped at other intervals or times, it will not work properly.
   */

public class BoundedDelay extends Delay
    {
    private static final long serialVersionUID = 1;

	// The delay array
	ArrayList[] delays;
	// The size of the delays array (- 1)
	int maxDelaySteps = -1;
	// The delay interval represented by one delay in the array
	int delayInterval = 1;
	// Where we are in the array right now
	int currentDelayPos;
	// What time it is right now
	double currentDelayTime = Schedule.BEFORE_SIMULATION;
	// The number of resources currently being delayed
	int size = 0;

    void throwInvalidDelayIntervalException(int interval)
        {
        throw new RuntimeException("In the BoundedDelay " + this + ", delay intervals must be > 0.  The interval provided was " +  interval);
        }

    void throwBoundedDelayTimeException(double time, int max)
        {
        throw new RuntimeException("In the BoundedDelay " + this + ", delay times must be <= " + max + ", but the delay time provided was " + time);
        }

    void throwNonPositiveMaxDelay(int time)
        {
        throw new RuntimeException("In the BoundedDelay " + this + ", max delay times must be > 0.  Max time provided was " + time);
        }

    void throwZeroOrMaxDelayException()
        {
        throw new RuntimeException("In the BoundedDelay " + this + ", we tried " + MAX_DELAY_TRIES + " times to generate a non-zero delay from the delay distribution that was <= " + (maxDelaySteps * delayInterval) + " and failed.  Giving up.  Use a better distribution.");
        }

    protected void buildDelay()
        {
        if (maxDelaySteps < 0) return;	// not ready yet
        delays = new ArrayList[maxDelaySteps + 1];
        for(int i = 0; i < delays.length; i++)
        	delays[i] = new ArrayList<DelayNode>();
        size = 0;
        }
                
    /** Creates a BoundedDelay with a 0 ordering, the given max delay time, delay interval, and typical resource. */
    public BoundedDelay(SimState state, double delayTime, Resource typical, int maxDelaySteps, int delayInterval)
        {
        super(state, delayTime, typical);
        if (maxDelaySteps <= 0)
        	throwNonPositiveMaxDelay(maxDelaySteps);
        this.maxDelaySteps = maxDelaySteps;
        this.delayInterval = delayInterval;
        buildDelay(); 
        rescheduleOrdering = 0;
        currentDelayPos = maxDelaySteps;
        }

    /** Creates a BoundedDelay with a 0 ordering, the given delay time, max delay time, a delay interval of 1, and typical resource. */
    public BoundedDelay(SimState state, double delayTime, Resource typical, int maxDelaySteps)
        {
        this(state, delayTime, typical, maxDelaySteps, 1);
        }

    /** Creates a Delay with a 0 ordering, a delay time of 1.0, max delay time, delay interval, and typical resource. */
    public BoundedDelay(SimState state, Resource typical, int maxDelaySteps, int delayInterval)
        {
        super(state, typical);
        if (maxDelaySteps <= 0)
        	throwNonPositiveMaxDelay(maxDelaySteps);
        this.maxDelaySteps = maxDelaySteps;
        this.delayInterval = delayInterval;
        buildDelay(); 
        rescheduleOrdering = 0;
        currentDelayPos = maxDelaySteps;
        }

    /** Creates a Delay with a 0 ordering, a delay time of 1.0, max delay time, a delay interval of 1, and typical resource. */
    public BoundedDelay(SimState state, Resource typical, int maxDelaySteps)
        {
        this(state, typical, maxDelaySteps, 1);
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
    
    public double getSize() 
    	{ 
    	return size;
    	}
    
    public int getDelayInterval() { return delayInterval; }
    	
    public void setDelayInterval(int val)
    	{
    	if (val < 1)
    		throwInvalidDelayIntervalException(val);
    	delayInterval = val;
    	clear();
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
    	if (delayTime > maxDelaySteps)
    		throwBoundedDelayTimeException(delayTime, maxDelaySteps);
    	this.delayTime = delayTime; 
    	}

    public static final double MAX_DELAY_TRIES = 10000;
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
        	boolean failed = true;
        	for(int i = 0; i < MAX_DELAY_TRIES; i++)
        		{
	        	setLastDelay(Math.abs(distribution.nextDouble()));
	        	if (getLastDelay() != 0 || getLastDelay() > maxDelaySteps) { failed = false; break; }
        		}
        	if (failed)
        		throwZeroOrMaxDelayException();
        	}
        
        return getLastDelay();
        }
    
    void insert(DelayNode node, double delay)
    	{
    	int pos = currentDelayPos + (int)delay;
		if (pos > maxDelaySteps) pos -= maxDelaySteps;
    	delays[pos].add(node);
    	size++;
    	}
    	
	public boolean isCumulative() { return false; }
	public void setCumulative(boolean val) { throw new RuntimeException("BoundedDelay cannot be cumulative.  Use Delay instead."); }

    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
        if (getRefusesOffers()) { return false; }
        if (!getTypicalReceived().isSameType(amount)) 
            throwUnequalTypeException(amount);
                
        if (isOffering()) throwCyclicOffers();  // cycle
        
        if (!(atLeast >= 0 && atMost >= atLeast && atMost > 0 && atMost <= amount.getAmount()))
            throwInvalidAtLeastAtMost(atLeast, atMost, amount);

        int delay = (int)Math.ceil(getDelay(provider, amount) / delayInterval);
		double nextTime = state.schedule.getTime() + delay * delayInterval;
		
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
            insert(node, delay);
            totalDelayedResource += maxIncoming;            
            totalReceivedResource += maxIncoming;
            }
        else
            {
            if (delayHeap.size() + (getIncludesAvailableResourcesInTotal() ? entities.size() : 0) >= getCapacity()) return false; // we're at capacity
            DelayNode node = new DelayNode(amount, nextTime, provider);
            if (lookup != null) lookup.put(amount, node);
            insert(node, delay);
            totalDelayedResource += 1.0;            
            totalReceivedResource += 1.0;
            }
        
        return true;
        }

    /** A convenience method which calls setAutoSchedules(true), then schedules the BoundedDelay on the Schedule using 
    	the current rescheduleOrdering.  The BoundedDelay is initially scheduled at the given time.  
    */
    public void autoScheduleAt(double time)
        {
        if (!isPositiveOrZeroNonNaN(time))
        	throw new RuntimeException("BoundedDelay " + this + " had autoScheduleAt(" + time + ") called, but this value must be >= 0");  
        	    	
        setAutoSchedules(true);
		state.schedule.scheduleOnce(time, rescheduleOrdering, this);
        }

    public void reset()
        {
        super.reset();
        currentDelayTime = Schedule.BEFORE_SIMULATION;
        }

    protected void update()
        {
        // push 
        double time = state.schedule.getTime();
        
        // is the time right?
        while (currentDelayTime == Schedule.BEFORE_SIMULATION ||
        	currentDelayTime + delayInterval <= time)
        	{
        	// let's do it!
			currentDelayPos++;
			currentDelayTime += delayInterval;
		
			if (getAutoSchedules()) 
				{
				double nextDelayTime = currentDelayTime + delayInterval;
				state.schedule.scheduleOnce(nextDelayTime, getRescheduleOrdering(), this);
				}
		
			if (getDropsResourcesBeforeUpdate()) 
				{
				drop();
				}

			if (delays[currentDelayPos].size() == 0)
				{
				return;
				}
		
			ArrayList<DelayNode> list = delays[currentDelayPos];
			size -= list.size();
			delays[currentDelayPos] = new ArrayList<DelayNode>();
		
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
        }

    public String toString()
        {
        return "BoundedDelay@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : (getName() + ": ")) + getTypicalProvided().getName() + ")";
        }               
    }
        
