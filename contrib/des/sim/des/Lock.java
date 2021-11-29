/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import sim.util.*;
import java.util.*;

/**
   A Lock locks (seizes, acquires) resources from a pool before permitting resources to pass through it
   from a provider to its receivers.
   Upon receiving an offer, first tries to LOCK (acquire) N resources from a Pool, and if successful,
   accepts the offer and offers it in turn to registered receivers.
*/

public class Lock extends Provider implements Receiver
    {
    Pool pool;
    double numResources;
    
    public Resource getTypical() { return typical; }

    /** Builds a lock attached to the given pool and with the given amount of resources acquired each time. */
    public Lock(SimState state, Resource typical, Pool pool, double numResources)
        {
        super(state, typical);
        this.numResources = numResources;
        this.pool = pool;
        }
        
    /** Builds a lock attached to the given pool and with 1.0 of the resource acquired each time. */
    public Lock(SimState state, Resource typical, Pool pool)
        {
        this(state, typical, pool, 1.0);
        }
        
    /** Builds a Lock with the same parameters as the provided Lock. */
    public Lock(Lock other)
        {
        super(other.state, other.typical);
        this.pool = other.pool;
        this.numResources = other.numResources;
        }
                
    /** Returns the number of resources allocated each time */
    public double getNumResources() { return numResources; }
    
    /** Sets the number of resources allocated each time */
    public void setNumResources(double val) { numResources = val; }
        
    /** Always returns true: locks only make take-it-or-leave-it offers */
    public boolean getOffersTakeItOrLeaveIt() { return true; }

    /** Returns false always and does nothing. */
    public boolean provide(Receiver receiver)
        {
        return false;
        }

    protected boolean offerReceiver(Receiver receiver)
        {
        return receiver.accept(this, _amount, _atLeast, _atMost);
        }
        
    double _atLeast;
    double _atMost;
    Resource _amount;
        
    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
        if (!typical.isSameType(amount)) throwUnequalTypeException(amount);

        if (isOffering()) throwCyclicOffers();  // cycle
        
        if (pool.getResource().getAmount() < numResources) return false;

        _amount = amount;
        _atLeast = atLeast;
        _atMost = atMost;
        boolean result = offerReceivers();
                
        if (result)
            {
            pool.getResource().decrease(numResources);
            }

        _amount = null;		/// let it gc
        return result;
        }

    public String toString()
        {
        return "Lock@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : getName()) + typical.getName() + ", " + typical.getName() + ", " + pool + ", " + numResources + ")";
        }  
                     
    /** Does nothing. */
    public void step(SimState state)
        {
        // do nothing
        }
    }
