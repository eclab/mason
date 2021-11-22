/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import java.util.*;

/** 
    A blocking resource queue with a capacity: you can think of Queue as a warehouse with a maximum
    amount of space.  Resources placed in the queue by default are offered to downstream
    recievers immediately.  You can change this behavior by setting setOffersImmediately(false).
    Whenever it is stepped by the Schedule, the Queue will also offer to its receivers.  You can
    prevent this by not scheduling it in the first place.  Like all Providers, the Queue will
    make an offer if possible to any Receiver that requests one via provide(...). 
*/

public class Queue extends Provider implements Receiver, Steppable
    {
    void throwInvalidCapacityException(double capacity)
        {
        throw new RuntimeException("Capacities may not be negative or NaN.  capacity was: " + capacity);
        }

    boolean isPositiveNonNaN(double val)
        {
        return (val >= 0);
        }

    double capacity = Double.POSITIVE_INFINITY;    

    /** Returns the maximum available resources that may be aquired by the Queue. */
    public double getCapacity() { return capacity; }
    
    /** Set the maximum available resources that may be aquired by the Queue. */
    public void setCapacity(double d) 
        { 
        if (!isPositiveNonNaN(d))
            throwInvalidCapacityException(d); 
        capacity = d; 
        }

    boolean offersImmediately = true;
    
    /** Returns whether the Queue offers items immediately upon accepting (when possible) in zero time,
        as opposed to when it is stepped. */
    public boolean getOffersImmediately() { return offersImmediately; }

    /** Sets whether the Queue offers items immediately upon accepting (when possible) in zero time,
        as opposed to when it is stepped. */
    public void setOffersImmediately(boolean val) { offersImmediately = val; }

    /** 
        Builds a queue with the given typical resource type.
    */
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
        
    public String toString()
        {
        return "Queue@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : getName()) + ", " + typical.getName() + ")";
        }               

    /** Upon being stepped, the Queue offers to registered receivers.  You don't have to
        schedule the Queue at all; in which case this method would never be called. */
                
    public void step(SimState state)
        {
        offerReceivers();
        }
    }
        
