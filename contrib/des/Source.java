/** 
    A blocking source of resources.  You can subclass this to provide resources 
    in your own fashion if you like, by overriding the update() and computeAvailable() methods.
    Sources have a maximum CAPACITY, which by default is infinite.
*/


import sim.engine.*;
import sim.util.distribution.*;
import java.util.*;

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

    public Source(SimState state, Resource typical)
        {
        super(state, typical);
        }
                
    double capacity = Double.POSITIVE_INFINITY;    

    double rate = 1.0;
    boolean randomOffset = true;
    AbstractDistribution rateDistribution = null;
    double nextTime;
    boolean reschedulesByRate = false;
    int rescheduleOrdering = 0;
        
    public static final int REJECTION_TRIES = 20;
        
    AbstractDistribution productionDistribution = null;
    double production = 1.0;
        
    public static boolean isPositiveNonNaN(double val)
        {
        return (val >= 0);
        }

    /** Returns the maximum available resources that may be built up. */
    public double getCapacity() { return capacity; }
    /** Set the maximum available resources that may be built up. */
    public void setCapacity(double d) 
        { 
        if (!isPositiveNonNaN(d))
            throwInvalidCapacityException(d); 
        capacity = d; 
        }


    public void setRateDistribution(AbstractDistribution rateDistribution)
        {
        this.rateDistribution = rateDistribution;
        }
        
    public AbstractDistribution getRateDistribution()
        {
        return this.rateDistribution;
        }

	public void setReschedulesByRate(boolean val)
		{
		reschedulesByRate = val;
		rescheduleOrdering = 0;
		}

	public void setReschedulesByRate(int ordering)
		{
		reschedulesByRate = true;
		rescheduleOrdering = ordering;
		}

	public boolean setReschedulesByRate()
		{
		return reschedulesByRate;
		}

    public void setRate(double rate, boolean randomOffset)
        {
        this.randomOffset = randomOffset;
        if (isPositiveNonNaN(rate)) 
            this.rate = rate;
        else throwInvalidProductionException(rate);
        }

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
        
    public void setProductionDistribution(AbstractDistribution productionDistribution)
        {
        this.productionDistribution = productionDistribution;
        }
        
    public AbstractDistribution getProductionDistribution()
        {
        return this.productionDistribution;
        }

    public void setProduction(double amt)
        {
        if (isPositiveNonNaN(amt)) 
            production = amt;
        else throwInvalidProductionException(amt);
        }

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
        
        If we reschedule by rate, then we next reschedule ourselves at the next time and ordering.

        <li>Now we determine how much to produce.
        <ul>
        	<li>If we are producing a deterministic production amount, then we use the production amount.
        	<li>If we are producing an amount determined by a distribution, then we select a random value 
        	under this distribution.
        </ul>
                    
        <p>New entities are produced by calling the method buildEntity().
                
        Total production cannot exceed the stated capacity. 
    */
                
    protected void update()
        {
        double time = state.schedule.getTime();
        if (time == Schedule.EPOCH)
        	{
        	return;		// we produce NOTHING the first time
        	}
        else if (time < nextTime)		// not ready yet
        	{
        	return;
        	}
        
        // at this point we're ready to produce!
        // First compute a new time beyond this one
        nextTime = getNextProductionTime();
        
        if (reschedulesByRate)
        	state.schedule.scheduleOnce(nextTime, rescheduleOrdering, this);
        
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
                
    public void step(SimState state)
        {
        update();
		offerReceivers();
        }

    public String getName()
        {
        return "Source(" + typical.getName() + ")";
        }               
    }
        
