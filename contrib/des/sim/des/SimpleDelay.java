/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;
import sim.engine.*;
import java.util.*;

/** 
    A simple deterministic delay pipeline.  Elements placed in the delay are only available
    after a fixed amount of time which is always the same regardless of the element and its
    provider.  Unless you turn off auto-scheduling 
*/

public class SimpleDelay extends Source implements Receiver, Steppable
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
    LinkedList<Node> delayQueue = new LinkedList<>();
    double delayTime;
    int rescheduleOrdering = 0;
    boolean autoSchedules = true;
    
    /** Returns whether the SimpleDelay schedules itself on the Schedule automatically to handle
    	the next timestep at which a delayed resource will become available.  If you turn this
    	off you will have to schedule the SimpleDelay yourself. */
    public boolean getAutoSchedules() { return autoSchedules; }

    /** Sets whether the SimpleDelay schedules itself on the Schedule automatically to handle
    	the next timestep at which a delayed resource will become available.  If you turn this
    	off you will have to schedule the SimpleDelay yourself. */
    public void setAutoScheduled(boolean val) { autoSchedules = val; }
        
    /** Clears all resources currently in the SimpleDelay. */
    public void clear()
        {
        super.clear();
        delayQueue.clear();
        totalResource = 0.0;
        }
                
    /** Returns the delay time. */
    public double getDelayTime() { return delayTime; }

    /** Sets the delay time and clears the delay entirely. */
    public void setDelayTime(double delayTime) { clear(); this.delayTime = delayTime; }

    /** Returns the delay ordering. */
	public int getRescheduleOrdering() { clear(); return rescheduleOrdering; }

    /** Returns the delay ordering and clears the delay entirely. */
	public void setRescheduleOrdering(int ordering) { this.rescheduleOrdering = ordering; }

    void throwInvalidNumberException(double capacity)
        {
        throw new RuntimeException("Capacities may not be negative or NaN.  capacity was: " + capacity);
        }

    /** Builds the delay structure. */
	protected void buildDelay()
		{
        delayQueue = new LinkedList<>();
		}
		
    /** Creates a SimpleDelay with a given delayTime, 0 ordering, and typical resource. */
    public SimpleDelay(SimState state, double delayTime, Resource typical)
        {
        super(state, typical);
        this.delayTime = delayTime;
        buildDelay();
        }

    /** Accepts up to CAPACITY of the given resource and places it in the delay,
    	then auto-reschedules the delay if that feature is on.. */
    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
        if (!resource.isSameType(amount)) 
            throwUnequalTypeException(amount);
        
        if (isOffering()) throwCyclicOffers();  // cycle
        
        double nextTime = state.schedule.getTime() + delayTime;
        if (entities == null)
            {
            CountableResource cr = (CountableResource)amount;
            double maxIncoming = Math.min(capacity - totalResource, atMost);
            if (maxIncoming < atLeast) return false;
                
            CountableResource token = (CountableResource)(cr.duplicate());
            token.setAmount(maxIncoming);
            cr.decrease(maxIncoming);
            delayQueue.add(new Node(token, nextTime));
            }
        else
            {
            if (delayQueue.size() >= capacity) return false; // we're at capacity
            delayQueue.add(new Node(amount, nextTime));
            }
        if (getAutoSchedules()) state.schedule.scheduleOnce(nextTime, getRescheduleOrdering(), this);
        return true;
        }

    /** Removes all currently ripe resources. */
    protected void drop()
        {
        if (entities == null)
            resource.clear();
        else
            entities.clear();
        }
                
    /** Deletes exiting ripe resources, then 
    	checks the delay pipeline to determine if any resources have come ripe, and makes
    	them available to registered receivers in zero time. */
    protected void update()
        {
        drop();
        double time = state.schedule.getTime();
                
        Iterator<Node> iterator = delayQueue.descendingIterator();
        while(iterator.hasNext())
            {
            Node node = iterator.next();
            if (node.timestamp >= time)     // it's ripe
                {
                if (entities == null)
                    {
                    CountableResource res = ((CountableResource)(node.resource));
                    iterator.remove();
                    resource.add(res);
                    totalResource -= res.getAmount();
                    }
                else
                    {
                    entities.add((Entity)(node.resource));
                    }
                }
            else break;             // don't process any more
            }
        }

    public String getName()
        {
        return "SimpleDelay(" + typical.getName() + ", " + delayTime + ")";
        }  
                     
	/** Upon being stepped, the Delay calls update() to reap all ripe resources.  It then
		calls offerReceivers to make offers to registered receivers.  You don't have to
		schedule the Delay at all, unless you have turned off auto-scheduling. */

    public void step(SimState state)
        {
        update();
        offerReceivers();
        }
    }
        
