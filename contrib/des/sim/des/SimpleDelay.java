/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;
import sim.engine.*;
import java.util.*;
import sim.portrayal.*;
import sim.portrayal.simple.*;

/** 
    A simple deterministic delay pipeline.  Elements placed in the delay are only available
    after a fixed amount of time which is always the same regardless of the element and its
    provider.  Unless you turn off auto-scheduling 
*/

public class SimpleDelay extends Middleman implements Steppable, StatReceiver
    {
    public SimplePortrayal2D buildDefaultPortrayal(double scale)
        {
        return new ShapePortrayal2D(ShapePortrayal2D.SHAPE_DELAY, 
            getFillPaint(), getStrokePaint(), getStrokeWidth(), scale);
        }

    private static final long serialVersionUID = 1;

    double totalDelayedResource = 0.0;
    LinkedList<DelayNode> delayQueue = new LinkedList<>();
    double delayTime;
    int rescheduleOrdering = 0;
    boolean autoSchedules = true;
    boolean dropsResourcesBeforeUpdate = true;
    boolean includesAvailableResourcesInTotal = false;
    Hashtable<Resource,DelayNode> lookup = null;
    
    double capacity = Double.POSITIVE_INFINITY;    

    /** Returns the maximum available resources that may be built up. */
    public double getCapacity() { return capacity; }
    
    /** Set the maximum available resources that may be built up. */
    public void setCapacity(double d) 
        { 
        if (!isPositiveOrZeroNonNaN(d))
            throwInvalidCapacityException(d); 
        capacity = d; 
        }

    void throwInvalidCapacityException(double capacity)
        {
        throw new RuntimeException("Capacities may not be negative or NaN.  capacity was: " + capacity);
        }

    void throwInvalidDelayTimeException(double time)
        {
        throw new RuntimeException("Delay Times may not be negative or NaN.  delay time: " + time);
        }

    void throwNoLookupException(Resource resource)
        {
        throw new RuntimeException("Resource " + resource + " was looked up, but that facility has not been turned on.");
        }

    /** Returns in an array all the Resources currently being delayed and not yet ready to provide,
        along with their timestamps (when they are due to become available), combined as a DelayNode.  
        Note that this is a different set of Resources than Provider.getEntities() returns.  
        You can modify the array (it's yours), but do not modify the DelayNodes nor the 
        Resources stored inside them, as they are the actual Resources being delayed.
    */
    public DelayNode[] getDelayedResources()
        {
        return (DelayNode[])(delayQueue.toArray(new DelayNode[delayQueue.size()]));
        }
        
    public boolean hideDelayedResources() { return true; }
    
    /** Returns whether the SimpleDelay schedules itself on the Schedule automatically to handle
        the next timestep at which a delayed resource will become available.  If you turn this
        off you will have to schedule the SimpleDelay yourself. */
    public boolean getAutoSchedules() { return autoSchedules; }
    public boolean hideAutoSchedules() { return true; }

    /** Sets whether the SimpleDelay schedules itself on the Schedule automatically to handle
        the next timestep at which a delayed resource will become available.  If you turn this
        off you will have to schedule the SimpleDelay yourself. */
    public void setAutoSchedules(boolean val) { autoSchedules = val; }
        
    /** Clears all resources currently in the SimpleDelay. */
    public void clear()
        {
        super.clear();
        if (delayQueue != null) delayQueue.clear();
        totalDelayedResource = 0.0;
        }

    /** Returns the number of items currently being delayed. */
    public double getSize() { return delayQueue.size(); }

    /** Returns the AMOUNT of resource currently being delayed. */
    public double getDelayed() { return totalDelayedResource; }

    /** Returns the AMOUNT of resource currently being delayed, plus the current available resources. */
    public double getDelayedPlusAvailable() { return getDelayed() + getAvailable(); }
        
    /** Returns the delay time. */
    public double getDelayTime() { return delayTime; }
    public boolean hideDelayTime() { return true; }

    /** Sets the delay time.  In a SimpleDelay (not a Delay) this also clears the delay queue entirely,
    	because not doing so would break the internal linked list.  In a Delay, the delay queue is not
    	cleared, and you are free to call this method any time you need to without issues.  
    	Delay times may not be negative or NaN.  */
    public void setDelayTime(double delayTime) 
    	{ 
    	if (delayTime < 0 || (delayTime != delayTime)) 
    		 throwInvalidDelayTimeException(delayTime);
 		this.delayTime = delayTime; 
		clear();
    	}

    /** Returns the delay ordering. */
    public int getRescheduleOrdering() { return rescheduleOrdering; }
    public boolean hideRescheduleOrdering() { return true; }

    /** Sets the delay ordering and clears the delay entirely. */
    public void setRescheduleOrdering(int ordering) { this.rescheduleOrdering = ordering; }

    double totalReceivedResource;
    public double getTotalReceivedResource() { return totalReceivedResource; }
    public double getReceiverResourceRate() { double time = state.schedule.getTime(); if (time <= 0) return 0; else return totalReceivedResource / time; }

    /** Builds the delay structure. */
    protected void buildDelay()
        {
        delayQueue = new LinkedList<>();
        }
                
    /** Sets whether lookup is used.  If TRUE, then every time a Resource is added to the SimpleDelay it is also
    	added to a HashMap so its DelayNode can be quickly looked up with lookup().  When the Resource exits
    	the SimpleDelay it is removed from the HashMap.  This is primarily used to make it fast to
    	set a resource as "dead" (see DelayNode). However it incurs a constant overhead and so this feature
    	is turned off by default.  */
	public void setUsesLookup(boolean val)
		{
		if (val)
			{
			if (lookup == null) lookup = new Hashtable<Resource,DelayNode>();
			}
		else
			{
			lookup = null;
			}
		}

    /** Returns whether lookup is used.  If TRUE, then every time a Resource is added to the SimpleDelay it is also
    	added to a HashMap so its DelayNode can be quickly looked up with lookup().  When the Resource exits
    	the SimpleDelay it is removed from the HashMap.  This is primarily used to make it fast to
    	set a resource as "dead" (see DelayNode). However it incurs a constant overhead and so this feature
    	is turned off by default.  */
	public boolean getUsesLookup()
		{
		return lookup != null;
		}
		
    /** Looks up a resource, if lookup is presently being used (otherwise issues a RuntimeException).
    	This is primarily used to make it fast to
    	set a resource as "dead" (see DelayNode). However it incurs a constant overhead and so this feature
    	is turned off by default (see setUsesLookup()).  */
	public DelayNode lookup(Resource resource)
		{
		if (lookup == null) throwNoLookupException(resource);
		return lookup.get(resource);
		}

    /** Creates a SimpleDelay with a given delayTime, 0 ordering, and typical resource. */
    public SimpleDelay(SimState state, double delayTime, Resource typical)
        {
        super(state, typical);
        this.delayTime = delayTime;
        buildDelay();
        setName("Delay");
        }

    /** Creates a SimpleDelay with a 0 ordering, a delay time of 1.0, and typical resource. */
    public SimpleDelay(SimState state, Resource typical)
        {
        this(state, 1.0, typical);
        }
        
    /** Returns whether the available resources (no longer in the delay queue) should be included as part of the delay's
    	total resource count for purposes of comparing against its capacity to determine if it's full. Note that
    	if setDropsResourcesBeforeUpdate() is TRUE, then the available resources will be included PRIOR to when drop()
    	is called, but they disappear afterwards.  drop() is called during offerReceivers(), which in turn may
    	be called during step() after update().
    	 */
    public boolean getIncludesAvailableResourcesInTotal()
    	{
    	return includesAvailableResourcesInTotal;
    	}
    	
    /** Sets whether the available resources (no longer in the delay queue) should be included as part of the delay's
    	total resource count for purposes of comparing against its capacity to determine if it's full. Note that
    	if setDropsResourcesBeforeUpdate() is TRUE, then the available resources will be included PRIOR to when drop()
    	is called, but they disappear afterwards.  drop() is called during offerReceivers(), which in turn may
    	be called during step() after update().
    	 */
    public void setIncludesAvailableResourcesInTotal(boolean val)
    	{
		includesAvailableResourcesInTotal = val;
    	}

    /** Accepts up to CAPACITY of the given resource and places it in the delay,
        then auto-reschedules the delay if that feature is on. */
    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
        if (getRefusesOffers()) { return false; }
        if (!getTypicalReceived().isSameType(amount)) 
            throwUnequalTypeException(amount);
        
        if (isOffering()) throwCyclicOffers();  // cycle
        
        if (!(atLeast >= 0 && atMost >= atLeast && atMost > 0 && atMost <= amount.getAmount()))
            throwInvalidAtLeastAtMost(atLeast, atMost, amount);

        double nextTime = state.schedule.getTime() + delayTime;
        if (entities == null)
            {
            CountableResource cr = (CountableResource)amount;
            double maxIncoming = Math.min(Math.min(getCapacity() - totalDelayedResource - (getIncludesAvailableResourcesInTotal() ? resource.getAmount() : 0), atMost), cr.getAmount());
            if (maxIncoming == 0 || maxIncoming < atLeast) return false;
                
            CountableResource token = (CountableResource)(cr.duplicate());
            token.setAmount(maxIncoming);
            cr.decrease(maxIncoming);
            DelayNode node = new DelayNode(token, nextTime, provider);
            if (lookup != null) lookup.put(token, node);
            delayQueue.add(node);
            totalDelayedResource += maxIncoming;            
            totalReceivedResource += maxIncoming;
            }
        else
            {
            if (delayQueue.size() + (getIncludesAvailableResourcesInTotal() ? entities.size() : 0) >= getCapacity()) return false; // we're at capacity
            DelayNode node = new DelayNode(amount, nextTime, provider);
            if (lookup != null) lookup.put(amount, node);
            delayQueue.add(node);
            totalDelayedResource += 1.0;            
            totalReceivedResource += 1.0;
            }
            
        if (getAutoSchedules()) state.schedule.scheduleOnce(nextTime, getRescheduleOrdering(), this);
        return true;
        }

    /** Sets whether available resources are cleared prior to loading new delayed resources
        during update().  By default this is TRUE.  If this is FALSE, then resources will build
        potentialy forever if not accepted by downstream receivers, as there is no maximum 
        capacity to the available resources. */
    public void setDropsResourcesBeforeUpdate(boolean val)
        {
        dropsResourcesBeforeUpdate = val; 
        }

    /** Returns whether available resources are cleared prior to loading new delayed resources
        during update().  By default this is TRUE.  If this is FALSE, then resources will build
        potentialy forever if not accepted by downstream receivers, as there is no maximum 
        capacity to the available resources. */
    public boolean getDropsResourcesBeforeUpdate()
        {
        return dropsResourcesBeforeUpdate; 
        }

    /** Removes all currently available resources. */
    protected void drop()
        {
        if (entities == null)
            resource.clear();
        else
            entities.clear();
        }
                
    /** Deletes exiting available resources, then 
        checks the delay pipeline to determine if any resources have come available, and makes
        them available to registered receivers in zero time. */
    protected void update()
        {
        if (getDropsResourcesBeforeUpdate()) 
            {
            drop();
            }
                
        double time = state.schedule.getTime();
                
        Iterator<DelayNode> iterator = delayQueue.iterator();
        while(iterator.hasNext())
            {
            DelayNode node = iterator.next();
            if (lookup != null) lookup.remove(node.resource);
            if (node.timestamp <= time)     // it's available
                {
                if (entities == null)
                    {
					iterator.remove();
					if (!node.dead)
						{
						CountableResource res = ((CountableResource)(node.resource));
						totalDelayedResource -= res.getAmount();
                    	resource.add(res);
                    	}
                    }
                else
                    {
                    iterator.remove();
					if (!node.dead)
						{
                    	Entity entity = ((Entity)(node.resource));
						entities.add(entity);
						totalDelayedResource--;
						}        
                    }
                }
            else break;             // don't process any more
            }
        }

    public String toString()
        {
        return "SimpleDelay@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : (getName() + ": ")) + getTypicalProvided().getName() + ")";
        }  
                     
    /** Upon being stepped, the Delay calls update() to reap all available resources.  It then
        calls offerReceivers to make offers to registered receivers.  You don't have to
        schedule the Delay at all, unless you have turned off auto-scheduling. */

    public void step(SimState state)
        {
        update();
        offerReceivers();
        }
    
    protected boolean offerReceivers(ArrayList<Receiver> receivers)
    	{
    	boolean returnval = super.offerReceivers(receivers);

    	if (slackProvider != null && getCapacity() > getDelayed())
    		slackProvider.provide(slackReceiver);
    	return returnval;
    	}
    	
    public void reset()
        {
        clear();
        totalReceivedResource = 0; 
        }
        
    Provider slackProvider;
    
    /** Returns the slack provider.  Whenever a SimpleDelay's offerReceivers(...) call is made, and it has slack afterwards,
    	it will call the slack provider to ask it to fill the slack up to capacity. 
    	The slack provider does this by making an offer to the slack receiver, which is often the SimpleDelay,
    	but not always: for example, perhaps the SimpleDelay has a Lock or a Queue in front of it -- it may wish some
    	slack provider to provide to the Lock or Queue, which will in turn offer to the SimpleDelay if possible. */
    public Provider getSlackProvider()
    	{
    	return slackProvider;
    	}
    	
    /** Sets the slack provider.  Whenever a SimpleDelay's offerReceivers(...) call is made, and it has slack afterwards,
    	it will call the slack provider to ask it to fill the slack up to capacity. The slack provider does this by making an offer to the slack receiver, which is often the SimpleDelay,
    	but not always: for example, perhaps the SimpleDelay has a Lock or a Queue in front of it -- it may wish some
    	slack provider to provide to the Lock or Queue, which will in turn offer to the SimpleDelay if possible.*/
    public void setSlackProvider(Provider provider)
    	{
    	slackProvider = provider;
    	}

    Receiver slackReceiver = this;
    
    /** Returns the slack receiver.  Whenever a SimpleDelay's offerReceivers(...) call is made, and it has slack afterwards,
    	it will call the slack provider to ask it to fill the slack up to capacity. The slack provider does this by making an offer to the slack receiver, which is often the SimpleDelay,
    	but not always: for example, perhaps the SimpleDelay has a Lock or a Queue in front of it -- it may wish some
    	slack provider to provide to the Lock or Queue, which will in turn offer to the SimpleDelay if possible.
    	*/
    public Receiver getSlackReceiver()
    	{
    	return slackReceiver;
    	}
    	
    /** Sets the slack provider.  Whenever a SimpleDelay's offerReceivers(...) call is made, and it has slack afterwards,
    	it will call the slack provider to ask it to fill the slack up to capacity. The slack provider does this by making an offer to the slack receiver, which is often the SimpleDelay,
    	but not always: for example, perhaps the SimpleDelay has a Lock or a Queue in front of it -- it may wish some
    	slack provider to provide to the Lock or Queue, which will in turn offer to the SimpleDelay if possible.
    	*/
    public void setSlackReceiver(Receiver receiver)
    	{
    	slackReceiver = receiver;
    	}
        
    boolean refusesOffers = false;
    public void setRefusesOffers(boolean value) { refusesOffers = value; }
    public boolean getRefusesOffers() { return refusesOffers; }
    
    public double[] getDataBars() 
        {
        if (getDropsResourcesBeforeUpdate())
            return new double[] { getCapacity() == 0 ? -1 : getDelayed() / (double)getCapacity() };
        else
            return new double[] { getCapacity() == 0 ? -1 : getDelayed() / (double)getCapacity(), -1 };
        }
    public String[] getDataValues() 
        {
        if (getDropsResourcesBeforeUpdate())
            return new String[] { "" + getDelayed() /*+ "/" + getCapacity()*/ };
        else
            return new String[] { "" + getDelayed() /*+ "/" + getCapacity()*/, "" + getAvailable() };
        }
    public String[] getDataLabels()
        {
        if (getDropsResourcesBeforeUpdate())
            return new String[] { "Delayed" };
        else
            return new String[] { "Delayed", "Available"};
        }
    }
        
