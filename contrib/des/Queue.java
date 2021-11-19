/** 
    A blocking resource queue with a capacity.  Resources placed in the queue are available 
    immediately, but when the queue is empty, the Queue will attempt to satisfy
    requests by requesting from its upstream Provider if any.
*/

import sim.engine.*;
import java.util.*;

public class Queue extends Provider implements Receiver, Steppable
    {
    void throwInvalidCapacityException(double capacity)
        {
        throw new RuntimeException("Capacities may not be negative or NaN.  capacity was: " + capacity);
        }

    public static boolean isPositiveNonNaN(double val)
        {
        return (val >= 0);
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

    boolean offersImmediately = true;
    
    /** Returns whether the Middleman offers items immediately upon accepting (when possible), without
    	waiting for a timestep. */
    public boolean getOffersImmediately() { return offersImmediately; }

    /** Sets whether the Middleman offers items immediately upon accepting (when possible), without
    	waiting for a timestep. */
    public void setOffersImmediately(boolean val) { offersImmediately = val; }

    public Queue(SimState state, Resource typical)
        {
        super(state, typical);
        }

   public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
        if (isOffering()) throwCyclicOffers();  // cycle
        
        if (!typical.isSameType(amount)) throwUnequalTypeException(amount);
        if (entities == null)
            {
            if (capacity - resource.getAmount() >= atLeast)
            	{
            	double transfer = Math.min(capacity - resource.getAmount(), atMost);
            	resource.increase(transfer);
            	((CountableResource)amount).decrease(transfer);
            	 if (getOffersImmediately()) offerReceivers(); 
            	return true;
            	}
            else return false;
            }
        else
            {
            if (capacity - entities.size() >= 1)
                {
                entities.add((Entity)amount);
            	 if (getOffersImmediately()) offerReceivers(); 
                return true;
                }
            else return false;
            }
        }
        
    public void update()
        {
        // do nothing
        }

    public String getName()
        {
        return "Queue(" + typical.getName() + ")";
        }               

    public void step(SimState state)
        {
        offerReceivers();
        }
    }
        
