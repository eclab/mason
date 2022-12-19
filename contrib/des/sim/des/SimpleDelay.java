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
import java.awt.*;

/** 
    A simple deterministic delay pipeline.  Elements placed in the delay are only available
    after a fixed amount of time which is always the same regardless of the element and its
    provider.  Unless you turn off auto-scheduling 
*/

public class SimpleDelay extends Source implements Receiver, Steppable, StatReceiver
    {
    public SimplePortrayal2D buildDefaultPortrayal(double scale)
        {
        return new ShapePortrayal2D(ShapePortrayal2D.SHAPE_DELAY, 
            getFillPaint(), getStrokePaint(), getStrokeWidth(), scale);
        }

    private static final long serialVersionUID = 1;

    public Resource getTypicalReceived() { return typical; }
    public boolean hideTypicalReceived() { return true; }

    double totalDelayedResource = 0.0;
    LinkedList<DelayNode> delayQueue = new LinkedList<>();
    double delayTime;
    int rescheduleOrdering = 0;
    boolean autoSchedules = true;
    boolean dropsResourcesBeforeUpdate = true;
    boolean includesRipeResourcesInTotal = false;
    
    /** Returns in an array all the Resources currently being delayed and not yet ready to provide,
        along with their timestamps (when they are due to become available), combined as a DelayNode.  
        Note that this is a different set of Resources than Provider.getEntities() returns.  
        You can modify the array (it's yours), but do not modify the Resources stored inside, as they
        are the actual Resources being delayed.
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
    public void setAutoScheduled(boolean val) { autoSchedules = val; }
        
    /** Clears all resources currently in the SimpleDelay. */
    public void clear()
        {
        super.clear();
        delayQueue.clear();
        totalDelayedResource = 0.0;
        }

    /** Returns the number of items currently being delayed. */
    public double getSize() { return delayQueue.size(); }

    /** Returns the number AMOUNT of resource currently being delayed. */
    public double getDelayed() { if (entities == null) return totalDelayedResource; else return delayQueue.size(); }

    /** Returns the number AMOUNT of resource currently being delayed, plus the current available resources. */
    public double getDelayedPlusAvailable() { return getDelayed() + getAvailable(); }
        
    /** Returns the delay time. */
    public double getDelayTime() { return delayTime; }
    public boolean hideDelayTime() { return true; }

    /** Sets the delay time and clears the delay entirely. */
    public void setDelayTime(double delayTime) { clear(); this.delayTime = delayTime; }

    /** Returns the delay ordering. */
    public int getRescheduleOrdering() { return rescheduleOrdering; }
    public boolean hideRescheduleOrdering() { return true; }

    /** Returns the delay ordering and clears the delay entirely. */
    public void setRescheduleOrdering(int ordering) { clear();  this.rescheduleOrdering = ordering; }

    double totalReceivedResource;
    public double getTotalReceivedResource() { return totalReceivedResource; }
    public double getReceiverResourceRate() { double time = state.schedule.getTime(); if (time <= 0) return 0; else return totalReceivedResource / time; }

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
        setName("Delay");
        }

    /** Creates a SimpleDelay with a given delayTime, 0 ordering, a delay time of 1.0, and typical resource. */
    public SimpleDelay(SimState state, Resource typical)
        {
        this(state, 1.0, typical);
        }
        
    /** Returns whether the ripe resources (no longer in the delay queue) should be included as part of the delay's
    	total resource count for purposes of comparing against its capacity to determine if it's full. Note that
    	if setDropsResourcesBeforeUpdate() is TRUE, then the ripe resources will be included PRIOR to when drop()
    	is called, but they disappear afterwards.  drop() is called during offerReceivers(), which in turn may
    	be called during step() after update().
    	 */
    public boolean getIncludesRipeResourcesInTotal()
    	{
    	return includesRipeResourcesInTotal;
    	}
    	
    /** Sets whether the ripe resources (no longer in the delay queue) should be included as part of the delay's
    	total resource count for purposes of comparing against its capacity to determine if it's full. Note that
    	if setDropsResourcesBeforeUpdate() is TRUE, then the ripe resources will be included PRIOR to when drop()
    	is called, but they disappear afterwards.  drop() is called during offerReceivers(), which in turn may
    	be called during step() after update().
    	 */
    public void setIncludesRipeResourcesInTotal(boolean val)
    	{
		includesRipeResourcesInTotal = val;
    	}

    /** Accepts up to CAPACITY of the given resource and places it in the delay,
        then auto-reschedules the delay if that feature is on. */
    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
        if (getRefusesOffers()) { return false; }
        if (!typical.isSameType(amount)) 
            throwUnequalTypeException(amount);
        
        if (isOffering()) throwCyclicOffers();  // cycle
        
        if (!(atLeast >= 0 && atMost >= atLeast && atMost > 0))
            throwInvalidAtLeastAtMost(atLeast, atMost);

        double nextTime = state.schedule.getTime() + delayTime;
        if (entities == null)
            {
            CountableResource cr = (CountableResource)amount;
            double maxIncoming = Math.min(Math.min(getCapacity() - totalDelayedResource - (getIncludesRipeResourcesInTotal() ? resource.getAmount() : 0), atMost), cr.getAmount());
            if (maxIncoming == 0 || maxIncoming < atLeast) return false;
                
            CountableResource token = (CountableResource)(cr.duplicate());
            token.setAmount(maxIncoming);
            cr.decrease(maxIncoming);
            delayQueue.add(new DelayNode(token, nextTime, provider));
            totalDelayedResource += maxIncoming;            
            totalReceivedResource += maxIncoming;
            }
        else
            {
            if (delayQueue.size() + (getIncludesRipeResourcesInTotal() ? entities.size() : 0) >= getCapacity()) return false; // we're at capacity
            delayQueue.add(new DelayNode(amount, nextTime, provider));
            totalDelayedResource += 1;            
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
        if (getDropsResourcesBeforeUpdate()) 
            {
            drop();
            }
                
        double time = state.schedule.getTime();
                
        Iterator<DelayNode> iterator = delayQueue.iterator();
        while(iterator.hasNext())
            {
            DelayNode node = iterator.next();
            if (node.timestamp <= time)     // it's ripe
                {
                if (entities == null)
                    {
                    CountableResource res = ((CountableResource)(node.resource));
                    iterator.remove();
                    totalDelayedResource -= res.getAmount();
                    resource.add(res);
                    }
                else
                    {
                    Entity entity = ((Entity)(node.resource));
                    iterator.remove();
                    entities.add(entity);
                    totalDelayedResource--;            
                    }
                }
            else break;             // don't process any more
            }
        }

    public String toString()
        {
        return "SimpleDelay@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : getName()) + ", " + typical.getName() + ", " + delayTime + ")";
        }  
                     
    /** Upon being stepped, the Delay calls update() to reap all ripe resources.  It then
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
    		slackProvider.offer(this);
    	return returnval;
    	}
    	
    public void reset()
        {
        clear();
        totalReceivedResource = 0; 
        }
        
    Provider slackProvider;
    
    /** Returns the slack provider.  Whenever a queue's offerReceivers(...) call is made, and it has slack afterwards,
    	it will call the slack provider to ask it to fill the slack up to capacity. */
    public Provider getSlackProvider()
    	{
    	return slackProvider;
    	}
    	
    /** Sets the slack provider.  Whenever a queue's offerReceivers(...) call is made, and it has slack afterwards,
    	it will call the slack provider to ask it to fill the slack up to capacity. */
    public void setSlackProvider(Provider provider)
    	{
    	slackProvider = provider;
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
        
