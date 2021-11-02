/** 
    A blocking source of resources.  You can subclass this to provide resources 
    in your own fashion if you like, by overriding the update() and computeAvailable() methods.
    Sources have a maximum CAPACITY, which by default is infinite.
*/


import sim.engine.*;
import sim.util.distribution.*;
import java.util.*;

public class Source extends Provider
    {
    void throwInvalidCapacityException(double capacity)
        {
        throw new RuntimeException("Capacities may not be negative or NaN.  capacity was: " + capacity);
        }

    void throwInvalidProductionException(double production)
        {
        throw new RuntimeException("Production amounts may not be negative, infinite, or NaN.  capacity was: " + capacity);
        }

    void throwNANException(double threshold)
        {
        throw new RuntimeException("Threshold may not be NaN.  threshold was: " + threshold);
        }

    void throwCriterionExceptionException(int criterion)
        {
        throw new RuntimeException("Success criterion is not valid: " + criterion);
        }

    public Source(SimState state, Resource typical)
        {
        super(state, typical);
        }
                
    double capacity = Double.POSITIVE_INFINITY;
    AbstractDistribution successDistribution = null;
    double successThreshold;
    int successCriterion;
        
    public static final int CRITERION_GREATER = 0;
    public static final int CRITERION_GREATER_OR_EQUAL = 1;
    public static final int CRITERION_LESS = 2;
    public static final int CRITERION_LESS_OR_EQUAL = 3;
        
    public static final int REJECTION_TRIES = 20;
        
    AbstractDistribution amountDistribution = null;
    double productionAmount = 1.0;
        
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


    public void setSuccess(AbstractDistribution distribution, double threshold, int criterion)
        {
        successDistribution = distribution;
        if (threshold != threshold)
            throwNANException(threshold);
        successThreshold = threshold;
        if (criterion < CRITERION_GREATER || criterion > CRITERION_LESS_OR_EQUAL)
            throwCriterionExceptionException(criterion);
        successCriterion = criterion;
        }
        
    public AbstractDistribution getSuccessDistribution()
        {
        return successDistribution;
        }
        
    public double getSuccessThreshold()
        {
        return successThreshold;
        }

    public int getSuccessCriterion()
        {
        return successCriterion;
        }
        
    public void setAmountDistribution(AbstractDistribution amountDistribution)
        {
        this.amountDistribution = amountDistribution;
        }
        
    public AbstractDistribution getAmountDistribution()
        {
        return this.amountDistribution;
        }


    public void setProductionAmount(double amt)
        {
        if (isPositiveNonNaN(amt) && amt != Double.POSITIVE_INFINITY) 
            productionAmount = amt;
        else throwInvalidProductionException(amt);
        }

    public double getProductionAmount()
        {
        return productionAmount;
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
            " tries, and was forced to use 0.0.  That's not good.\nSource: " + this + "\nDistribution: " + amountDistribution);
        warned = true;
        }
        
    double produceAmount()
        {
        for(int i = 0; i < REJECTION_TRIES; i++)
            {
            double amt = amountDistribution.nextDouble();
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
                
        <p>The default version of this method increases the pool by use of
        two distributions: a SUCCESS distribution and an AMOUNT distribution, both of which the
        modeler can set.   This works as follows:
                
        <ul>
        <li>If both distributions are null, then exactly productionAmount of the new resource, 
        or FLOOR(productionAmount) new entities, are produced each timestep
        <li>If amountDistribution is null but not successDistribution, then exactly 
        productionAmount of the new resource, or FLOOR(productionAmount) new entities, 
        are produced each timestep if a random value drawn from successDistribution exceeds
        the success threshold according to the success criterion.
        <li>If successDistribution is null but not amountDistribution, then a random amount is
        drawn from the amountDistribution, and either the amount in resource, or 
        FLOOR(number) new entities, are produced each timestep.
        <li>If neither distribution is null, then if a random value drawn from successDistribution exceeds
        the success threshold according to the success criterion: then a random amount is
        drawn from the amountDistribution, and either the amount in resource, or 
        FLOOR(number) new entities, are produced each timestep.
        </ul>
                
        <p>New entities are produced by calling the method buildEntity().
                
        Total production cannot exceed the stated capacity. 
    */
                
    protected void update()
        {
        // check for capacityh
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
        	
        	
        if (successDistribution == null || 
            ((successCriterion == CRITERION_GREATER) && (successDistribution.nextDouble() > successThreshold)) ||
            ((successCriterion == CRITERION_GREATER_OR_EQUAL) && (successDistribution.nextDouble() >= successThreshold)) ||
            ((successCriterion == CRITERION_LESS) && (successDistribution.nextDouble() < successThreshold)) ||
            ((successCriterion == CRITERION_LESS_OR_EQUAL) && (successDistribution.nextDouble() <= successThreshold)))
            {
            double amt = 1.0;
            if (amountDistribution != null)
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
        }
                
    public void step(SimState state)
        {
        update();
        super.step(state);              // offerReceivers();
        }

    public String getName()
        {
        return "Source(" + typical.getName() + ")";
        }               
    }
        
