/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import sim.util.distribution.*;
import java.util.*;
import sim.portrayal.*;
import sim.portrayal.simple.*;
import java.awt.*;

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
    public SimplePortrayal2D buildDefaultPortrayal(double scale)
        {
        return new ShapePortrayal2D(ShapePortrayal2D.POLY_POINTER_UP, 
            getFillPaint(), getStrokePaint(), getStrokeWidth(), scale);
        }

    public double[] getDataBars() 
        {
        return new double[] { getCapacity() == Double.POSITIVE_INFINITY || getCapacity() == 0 ? -1 : getAvailable() / (double)getCapacity() };
        }

    public String[] getDataValues()
        {
        return new String[] { getCapacity() == Double.POSITIVE_INFINITY ? "" + getAvailable() :
                              getAvailable() + "/" + getCapacity() };
        }
    public String[] getDataLabels()
        {
        return new String[] { "Available" };
        }
        
    private static final long serialVersionUID = 1;

    void throwInvalidCapacityException(double capacity)
        {
        throw new RuntimeException("Capacities may not be negative or NaN.  capacity was: " + capacity);
        }

    void throwInvalidProductionException(double amt)
        {
        throw new RuntimeException("Production amounts and rates may not be negative or NaN.  Was was: " + amt);
        }

    /** 
        Builds a source with the given typical provided resource type.
    */
    public Source(SimState state, Resource typicalProvided)
        {
        super(state, typicalProvided);
        setName("Source");
        }
                
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
        
        <p>When a value is drawn from this distribution to determine
        delay, it will be put through Absolute Value first to make it positive.  Note that if your 
        distribution covers negative regions, you need to consider what will happen as a result and 
        make sure it's okay (or if you should be considering a positive-only distribution).  
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
        
        <p>Throws a runtime exception if the rate is negative or NaN.
    */
    public void setRate(double rate, boolean randomOffset)
        {
        this.randomOffset = randomOffset;
        if (isPositiveOrZeroNonNaN(rate)) 
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

    /** A convenience method, which calls setAutoSchedules(true), then schedules the Source on the Schedule
    	at the current time (usually timestep 0, as this method ought to be called during start()) with
    	a 0 ordering.
    */
    public void autoSchedule()
        {
        setAutoSchedules(true);
		state.schedule.scheduleOnce(this);
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

	public boolean hideAutoSchedules() { return true; }

    /** Returns the reschedule ordering. */
    public int getRescheduleOrdering() { return rescheduleOrdering; }

    /** Returns the reschedule ordering and clears the delay entirely. */
    public void setRescheduleOrdering(int ordering) { this.rescheduleOrdering = ordering; }

	public boolean hideRescheduleOrdering() { return true; }

    double getNextProductionTime()
        {
        double time = state.schedule.getTime();
        double currentTime = time;
        if (currentTime < Schedule.EPOCH) 				// this shouldn't be able to happen
            {
        	System.err.println("WARNING: Source being asked to produce before the epoch of the simulation.");
            currentTime = Schedule.EPOCH;
            }
        double val = 0;
        if (rateDistribution != null)
            {
            double d = rateDistribution.nextDouble();
            if (d <= 0)
	        	{ 
	        	System.err.println("WARNING: Rate distribution returned a value <= 0: " + d);
	        	d = 0;
	        	}
            val = d + currentTime;
            }
        else
            {
            val = rate + currentTime;
            }
                                
        if (val == time) 
            val = Math.nextUp(time);
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
        
        <p>Depending on your needs, you might wish to select a discrete distribution rather than 
        a continuous one.
        
        <p>When a value is drawn from this distribution to determine
        delay, it will be put through Absolute Value first to make it positive.  Note that if your 
        distribution covers negative regions, you need to consider what will happen as a result and 
        make sure it's okay (or if you should be considering a positive-only distribution).  
    */
    public AbstractDistribution getProductionDistribution()
        {
        return this.productionDistribution;
        }

    /** Sets the deterministic production value used to determine how much resource is produced each time the Source
        decides to produce resources (only when there is no distribution provided).

        <p>Throws a runtime exception if the rate is negative, zero, or NaN.
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
       By default this is done by duplicating the typical provided entity.
       You can override this if you feel so inclined.
    */
    protected Entity buildEntity()
        {
        Entity ret = (Entity)(getTypicalProvided().duplicate());
        ret.clear();
        return ret;
        }
        
    /*
      boolean warned = false;
      void warnRejectionFailed()
      {
      if (warned) return;
      System.err.println("ONE-TIME WARNING: Source could not produce a positive amount from the provided distribution after " + REJECTION_TRIES + 
      " tries, and was forced to use 0.0.  That's not good.\nSource: " + this + "\nDistribution: " + productionDistribution);
      warned = true;
      }
    */
        
    /** Builds *amt* number of Entities and adds them to the entities list.  
        The amount could be a real-value, in which it should be
        simply rounded to the nearest positive integer >= 0.  By default this
        generates entities using buildEntity().  */
    protected void buildEntities(double amt)
        {
        for(int i = 0; i < Math.round(amt); i++)					// FIXME: should this be roundor floor?
            {
            if (entities.size() < capacity)
                entities.add(buildEntity());
            else break;
            }
        }
        
    /** Builds *amt* of Countable or Uncountable Resource and adds it to the resource pool. 
        By default this simply adds resource out of thin air. */
    protected void buildResource(double amt)
        {
        CountableResource res = (CountableResource)resource;
        if (res.isCountable())
            amt = Math.round(amt);								// FIXME: should this be roundor floor?
                                                                                                                                        
        res.increase(amt);
        if (res.getAmount() > capacity)
            res.setAmount(capacity);
        }
        
        
    /** This method is called once every time this Source is stepped, and is used to produce new
        resources or entities and add them to the available pool in the Source.  You can override
        this method to add them as you see fit (you can check to see if you should add entities
        by seeing if the entities variable is non-null: otherwise you should be adding to the 
        existing resource).  
                
        <p>The modeler can set the RATE at which production occurs by setting either a deterministic
        RATE (via setRate()) or by setting a distribution to determine the rate (via setRateDistribution() --
        you may find sim.util.distribution.Scale to be a useful utility class here).
        
        <p>The modeler can also change the AMOUNT which is produced by setting either a deterministic
        PRODUCTION (via setProduction()) or by setting a distribution to determine the production (via
        setProductionDistribution() -- again you may find sim.util.distribution.Scale to be a useful 
        utility class here).
        
        <p>By default update() then works as follows.
        
        <ol>
        <li>First, if we are autoscheduling, we need to reschedule ourselves.
        <ul>
        <li>If we have a rate distribution, select from the distribution and add to our current time
        	 to get the rescheduling time.
            The distribution value should be >= 0, else it will be set to 0.
        <li>If we have a fixed rate, add it to our current time to get the rescheduling time.
        <li>If the resulting rescheduling time hasn't changed (we added 0 to it, probably an error), 
        reschedule at the immediate soonest theoretical time in the future.  Else schedule at the rescheduling time.
		</ul>
		
        <li>Next, if we're >= capacity, return.
        
        <li>Othrwise we determine how much to produce.
        <ul>
        <li>If we are producing a deterministic production amount, then we use the production amount.
        <li>If we are producing an amount determined by a distribution, then we select a random value 
        under this distribution.
        </ul>
        </ol>
                    
        <p>New entities are produced by calling the method buildEntity().
                
        <p>Total production cannot exceed the stated capacity. 
        
        <p>Note that when a value is drawn from either the RATE or PRODUCTION distributions, 
        it will be put through Absolute Value first to make it positive.  Note that if your 
        distribution covers negative regions, you need to consider what will happen as a result and 
        make sure it's okay (or if you should be considering a positive-only distribution).  
    */
                
    protected void update()
        {
        if (getAutoSchedules())
            {
            // First, let's reschedule ourselves
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
                
        // determine how much to produce
        double amt = production;
        if (productionDistribution != null)
            {
            amt = Math.abs(productionDistribution.nextDouble());
            
            /*
            // produce
            for(int i = 0; i < REJECTION_TRIES; i++)
            {
            amt = productionDistribution.nextDouble();
            if (amt >= 0)
            break;
            }
                                
            if (amt < 0)
            {
            warnRejectionFailed();
            amt = 0;
            }
            */
            }
    
        // produce it                                      
        if (entities != null)
            {
            buildEntities(amt);
            }       
        else
            {
            buildResource(amt);
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
        return "Source@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : (getName() + ": ")) + getTypicalProvided().getName() + ")";
        }               
    }
        
