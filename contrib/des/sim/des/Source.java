/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import sim.util.distribution.*;
import java.util.*;


/** 
    A source of resources.  You will typically subclass Source to provide resources in a custom
    way appropriate to your simulation.   The default works more or less as follows.  Upon being
    stepped, the Source determines the NEXT timestep to be stepped using either a distribution or
    a deterministic value you have provided.  It then determines the AMOUNT of resource to produce
    this time around, again using either a distribution or a determinstic value yu have provided.
    It then produces the resources, reschedules itself, and offers resources to registered receivers.
    Sources have a maximum CAPACITY, which by default is infinite.
    
    <p>You can override this in several ways.  First, if the Source produces Entities, you can override
    buildEntity to customize the nature of the Entities generated.  Second, you can turn off the Source's
    auto-rescheduling, and schedule the Source yourself how you see fit.  Third, you can completely
    customize the Source's production, rescheduling, etc. by overriding the update() method.
*/


public class Source extends Provider implements Steppable
    {
    void throwInvalidCapacityException(double capacity)
        {
        throw new RuntimeException("Capacities may not be negative or NaN.  capacity was: " + capacity);
        }

    void throwInvalidProductionException(double amt)
        {
        throw new RuntimeException("Production amounts and rates may not be negative or NaN.  Was was: " + amt);
        }

    boolean isPositiveNonNaN(double val)
        {
        return (val >= 0);
        }

    /** 
        Builds a source with the given typical resource type.
    */
    public Source(SimState state, Resource typical)
        {
        super(state, typical);
        }
                
    double capacity = Double.POSITIVE_INFINITY;    

    /** Returns the maximum available resources that may be built up. */
    public double getCapacity() { return capacity; }
    
    /** Set the maximum available resources that may be built up. */
    public void setCapacity(double d) 
        { 
        if (!isPositiveNonNaN(d))
            throwInvalidCapacityException(d); 
        capacity = d; 
        }

    double rate = 1.0;
    boolean randomOffset = true;
    AbstractDistribution rateDistribution = null;
    double nextTime;
    boolean autoSchedules = false;
    int rescheduleOrdering = 0;
        
    public static final int REJECTION_TRIES = 20;
        
    AbstractDistribution productionDistribution = null;
    double production = 1.0;
        
    /** Sets the distribution used to determine the rate at which the source produces resources.
        When the source is update()ed (via a step() method), it draws from this distribution the
        next time at which it should schedule itself to be stepped() again.  If this distribution
        is null, it instead uses getRate() to deterministically acquire the next timestep.  Note that
        the if the time is currently Schedule.EPOCH, no resources will be produced this timestep.  
    */
    public void setRateDistribution(AbstractDistribution rateDistribution)
        {
        this.rateDistribution = rateDistribution;
        }
        
    /** Returns the distribution used to determine the rate at which the source produces resources.
        When the source is update()ed (via a step() method), it draws from this distribution the
        next time at which it should schedule itself to be stepped() again.  If this distribution
        is null, it instead uses getRate() to deterministically acquire the next timestep.  Note that
        the if the time is currently Schedule.EPOCH, no resources will be produced this timestep.  
    */
    public AbstractDistribution getRateDistribution()
        {
        return this.rateDistribution;
        }

    /** Sets the deterministic rate and potential random offset for producing resources.  If the rate
        distribution is null, then the deterministic rate and random offset are used instead as follows.
        If the Source is initially scheduled for Schedule.EPOCH, then at that time it does not produce
        any resources, but rather determines the initial time to reschedule itself.  If the random offset
        is TRUE then the initial time will be the EPOCH plus a uniform random value between 0 and the
        deterministic rate.  If the random offset is FALSE then the initial time will simply be the EPOCH plus
        the deterministic rate.  Thereafter the next scheduled time will be the current time plus the rate. 
    */
    public void setRate(double rate, boolean randomOffset)
        {
        this.randomOffset = randomOffset;
        if (isPositiveNonNaN(rate)) 
            this.rate = rate;
        else throwInvalidProductionException(rate);
        }

    /** Returns the deterministic rate for producing resources.  If the rate
        distribution is null, then the deterministic rate and random offset are used instead as follows.
        If the Source is initially scheduled for Schedule.EPOCH, then at that time it does not produce
        any resources, but rather determines the initial time to reschedule itself.  If the random offset
        is TRUE then the initial time will be the EPOCH plus a uniform random value between 0 and the
        deterministic rate.  If the random offset is FALSE then the initial time will simply be the EPOCH plus
        the deterministic rate.  Thereafter the next scheduled time will be the current time plus the rate. 
    */
    public double getRate()
        {
        return rate;
        }
        
    /** Returns the random offset for producing resources.  If the rate
        distribution is null, then the deterministic rate and random offset are used instead as follows.
        If the Source is initially scheduled for Schedule.EPOCH, then at that time it does not produce
        any resources, but rather determines the initial time to reschedule itself.  If the random offset
        is TRUE then the initial time will be the EPOCH plus a uniform random value between 0 and the
        deterministic rate.  If the random offset is FALSE then the initial time will simply be the EPOCH plus
        the deterministic rate.  Thereafter the next scheduled time will be the current time plus the rate. 
    */
    public boolean getRandomOffset()
        {
        return randomOffset;
        }

    /** Sets whether the Source reschedules itself automatically using either a deterministic or distribution-based
        rate scheme.  If FALSE, you are responsible for scheduling the Source as you see fit. If TRUE,
        then the ordering used when scheduling is set to 0.
    */
    public void setAutoSchedules(boolean val)
        {
        autoSchedules = val;
        rescheduleOrdering = 0;
        }

    /** Returns whether the Source reschedules itself automatically using either a deterministic or distribution-based
        rate scheme.
    */
    public boolean getAutoSchedules()
        {
        return autoSchedules;
        }

    /** Returns the reschedule ordering. */
    public int getRescheduleOrdering() { return rescheduleOrdering; }

    /** Returns the reschedule ordering and clears the delay entirely. */
    public void setRescheduleOrdering(int ordering) { this.rescheduleOrdering = ordering; }

    double getNextProductionTime()
        {
        double currentTime = state.schedule.getTime();
        if (currentTime < Schedule.EPOCH) 
            currentTime = Schedule.EPOCH;
        double val = 0;
        if (rateDistribution != null)
            {
            val = rateDistribution.nextDouble() + nextTime;
            }
        else if (currentTime == Schedule.EPOCH)
            {
            val = (randomOffset ? state.random.nextDouble() * rate : 0.0) + nextTime;
            }
        else
            {
            val = rate + nextTime;
            }
                                
        if (val == currentTime) 
            val = Math.nextUp(currentTime); 
        return val;
        }
    
    
    /** Sets the distribution used to determine how much resource is produced each time the Source
        decides to produce resources.  If this is null, then the determinstic production value is used instead.
    */
    public void setProductionDistribution(AbstractDistribution productionDistribution)
        {
        this.productionDistribution = productionDistribution;
        }
        
    /** Returns the distribution used to determine how much resource is produced each time the Source
        decides to produce resources.  If this is null, then the determinstic production value is used instead.
    */
    public AbstractDistribution getProductionDistribution()
        {
        return this.productionDistribution;
        }

    /** Sets the deterministic production value used to determine how much resource is produced each time the Source
        decides to produce resources (only when there is no distribution provided).
    */
    public void setProduction(double amt)
        {
        if (isPositiveNonNaN(amt)) 
            production = amt;
        else throwInvalidProductionException(amt);
        }

    /** Returns the deterministic production value used to determine how much resource is produced each time the Source
        decides to produce resources (only when there is no distribution provided).
    */
    public double getProduction()
        {
        return production;
        }
        
    /**
       Produces ONE new entity to add to the collection of entities.
       By default this is done by duplicating the typical entity.
       You can override this if you feel so inclined.
    */
    protected Entity buildEntity()
        {
        Entity ret = (Entity)(typical.duplicate());
        ret.clear();
        return ret;
        }
        
    boolean warned = false;
    void warnRejectionFailed()
        {
        if (warned) return;
        System.err.println("ONE-TIME WARNING: Source could not produce a positive amount from the provided distribution after " + REJECTION_TRIES + 
            " tries, and was forced to use 0.0.  That's not good.\nSource: " + this + "\nDistribution: " + productionDistribution);
        warned = true;
        }
        
    double produceAmount()
        {
        for(int i = 0; i < REJECTION_TRIES; i++)
            {
            double amt = productionDistribution.nextDouble();
            if (amt >= 0)
                {
                return amt;
                }
            }
        warnRejectionFailed();
        return 0;
        }
        
        
    /** This method is called once every time this Source is stepped, and is used to produce new
        resources or entities and add them to the available pool in the Source.  You can override
        this method to add them as you see fit (you can check to see if you should add entities
        by seeing if the entities variable is non-null: otherwise you should be adding to the 
        existing resource).  
                
        <p>The modeler can change the RATE at which production occurs by setting either a deterministic
        RATE (via setRate()) or by setting a distribution to determine the rate (via setRateDistribution() --
        you may find sim.util.distribution.Scale to be a useful utility class here).
        
        <p>The modeler can also change the AMOUNT which is produced by setting either a deterministic
        PRODUCTION (via setProduction()) or by setting a distribution to determine the production (via
        setProductionDistribution() -- again you may find sim.util.distribution.Scale to be a useful 
        utility class here).
        
        <p>update() then works as follows.
        
        <ol>
        <li>First, if we are rescheduling by rate...
        <ol>
        <li>First we must determine if we are producing anything at all this go-around.
        <ul>
        <li>If we are at the Schedule.EPOCH, we produce nothing and return immediately.
        <li>If we are scheduled to produce something at a future time, and have not reached it yet,
        again, we produce nothing and return immediately.
        <li>Else we'll produce something.
        </ul>
                
        <li>If we've gotten this far, we next compute the next time we should produce something.
        <ul>
        <li>If we are at the Schedule.EPOCH, and we are producing at a deterministic rate, then
        the next time is just the previous determined time plus either the rate or (if we are using a
        random offset) the rate time a random number between 0 and 1.
        <li>If we are NOT at the Schedule.EPOCH, and we are producing at a deterministic rate, then
        the next time is just the previous determined time plus either the rate.
        <li>If we are producing at a rate determined by a distribution, then the next time is 
        the previous determined time plus a random value under this distribution.
        </ul>
                
        <li>Now we next reschedule ourselves at the next time and ordering.
        </ol>

        <li>Now we determine how much to produce.
        <ul>
        <li>If we are producing a deterministic production amount, then we use the production amount.
        <li>If we are producing an amount determined by a distribution, then we select a random value 
        under this distribution.
        </ul>
        </ol>
                    
        <p>New entities are produced by calling the method buildEntity().
                
        Total production cannot exceed the stated capacity. 
    */
                
    protected void update()
        {
        if (!autoSchedules)
            {
            double time = state.schedule.getTime();
            if (time == Schedule.EPOCH)
                {
                nextTime = getNextProductionTime();
                state.schedule.scheduleOnce(nextTime, rescheduleOrdering, this);
                return;         // we produce NOTHING the first time
                }
            else if (time < nextTime)               // not ready yet -- FIXME, how could this possibly happen?
                {
                return;
                }
                
            // at this point we're ready to produce!
            // First compute a new time beyond this one
            nextTime = getNextProductionTime();
            state.schedule.scheduleOnce(nextTime, rescheduleOrdering, this);
            }
        
        // check for capacity
        if (entities != null)
            {
            if (entities.size() >= capacity)
                return;
            }
        else
            {
            CountableResource res = (CountableResource)resource;
            if (res.getAmount() >= capacity)
                return;
            }
                
        double amt = production;
        if (productionDistribution != null)
            {
            amt = produceAmount();
            }
                                                        
        if (entities != null)
            {
            for(int i = 0; i < Math.round(amt); i++)
                {
                if (entities.size() < capacity)
                    entities.add(buildEntity());
                else break;
                }
            }       
        else
            {
            CountableResource res = (CountableResource)resource;
            if (res.isCountable())
                amt = Math.round(amt);
                                                                        
            res.increase(amt);
            if (res.getAmount() > capacity)
                res.setAmount(capacity);
            }               
        }
                
    /** Upon being stepped, the Source updates its resources (potentially building some new ones), then
        makes offers to registered receivers.  If you are automatically rescheduling, you don't have to
        schedule the Source at all; it'll handle it. */
                
    public void step(SimState state)
        {
        update();
        offerReceivers();
        }

    public String toString()
        {
        return "Source@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : getName()) + ", " + typical.getName() + ")";
        }               
    }
        
