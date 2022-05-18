/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import sim.util.*;
import java.util.*;
import sim.portrayal.*;
import sim.portrayal.simple.*;
import java.awt.*;

/**
   A Lock locks (seizes, acquires) resources from a pool before permitting resources to pass through it
   from a provider to its receivers.
   Upon receiving an offer, first tries to LOCK (acquire) N resources from a Pool, and if successful,
   accepts the offer and offers it in turn to registered receivers.
*/

public class Lock extends Filter
    {
    public SimplePortrayal2D buildDefaultPortrayal(double scale)
        {
        return new ShapePortrayal2D(ShapePortrayal2D.POLY_HOURGLASS, 
            getFillPaint(), getStrokePaint(), getStrokeWidth(), scale);
        }

    private static final long serialVersionUID = 1;

    Pool pool;
    double numResources;
    boolean blocked = false;
    
    /** Builds a lock attached to the given pool and with the given amount of resources acquired each time. */
    public Lock(SimState state, Resource typical, Pool pool, double numResources)
        {
        super(state, typical);
        this.numResources = numResources;
        this.pool = pool;
        setName("Lock ( " + pool.getName() + ")");
        }
        
    /** Builds a lock attached to the given pool and with 1.0 of the resource acquired each time. */
    public Lock(SimState state, Resource typical, Pool pool)
        {
        this(state, typical, pool, 1.0);
        }
        
    /** Builds a Lock with the same parameters as the provided Lock. */
    public Lock(Lock other)
        {
        this(other.state, other.typical, other.pool, other.numResources);
        }
                
    /** Returns the number of resources allocated each time */
    public double getNumResources() { return numResources; }
    public boolean hideNumResources() { return true; }
    
    /** Sets the number of resources allocated each time */
    public void setNumResources(double val) { numResources = val; }
        
    /** Always returns true: locks only make take-it-or-leave-it offers */
    public boolean getOffersTakeItOrLeaveIt() { return true; }
    public boolean hideOffersTakeItOrLeaveIt() { return true; }

    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
        blocked = false;
        
        if (getRefusesOffers()) { return false; }
        if (!typical.isSameType(amount)) throwUnequalTypeException(amount);

        if (isOffering()) throwCyclicOffers();  // cycle
        
        if (!(atLeast >= 0 && atMost >= atLeast))
            throwInvalidAtLeastAtMost(atLeast, atMost);

        // try to acquire resources
        if (pool.getResource().getAmount() < numResources) 
            {
            blocked = true;
            return false;
            }

        // pre-grab the resource
        pool.getResource().decrease(numResources);

        boolean result = offerReceivers(amount, atLeast, atMost);
                
        if (!result) // gotta put it back
            {
            pool.getResource().increase(numResources);
            pool.getResource().bound(pool.getMaximum());
            }

        _amount = null;         /// let it gc
        return result;
        }

    public String toString()
        {
        return "Lock@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : getName()) + typical.getName() + ", " + typical.getName() + ", " + pool + ", " + numResources + ")";
        }  
                     
    public String getName() 
        { 
        return "Lock (" + (pool.getName() == null ? "Pool " + System.identityHashCode(pool) : pool.getName()) + ")";
        }    

    public boolean getDrawState() { return blocked; }
    }
