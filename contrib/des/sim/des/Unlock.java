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

/*
  A Unlock unlocks (releases) resources to a pool before permitting resources to pass through it
  from a provider to its receivers.
  Upon receiving an offer, UNLOCKS up to N resources and returns then to a Pool, then accepts
  the offer and offers it in turn to registered receivers.
*/

public class Unlock extends Lock
    {
    public SimplePortrayal2D buildDefaultPortrayal(double scale)
        {
        return new ShapePortrayal2D(ShapePortrayal2D.POLY_BOWTIE, 
            getFillPaint(), getStrokePaint(), getStrokeWidth(), scale);
        }

    private static final long serialVersionUID = 1;

    /** Throws an exception indicating that a partnering cycle was detected. */
    protected void throwCyclicPartnering()
        {
        throw new RuntimeException("Zero-time cycle found where, during an accept(...), the Unlock has asked its partner Lock to ask its provider to provide(), resulting in accept(...) being called on the Unlock." );
        }

    /** Builds an Unlock attached to the given pool and with the given amount of resources released each time. */
    public Unlock(SimState state, Resource typical, Pool pool, double numResources)
        {
        super(state, typical, pool, numResources);
        setName("Unlock (" + (pool.getName() == null ? "Pool " + System.identityHashCode(pool) : pool.getName()) + ")");
        }
        
    /** Builds an Unlock attached to the given pool and with 1.0 of the resource returned each time. */
    public Unlock(SimState state, Resource typical, Pool pool)
        {
        this(state, typical, pool, 1.0);
        }
        
    /** Builds an Unlock with the same parameters as the provided Lock.  Does not set the Lock as its partner.*/
    public Unlock(Lock other)
        {
        super(other);
        }

    boolean partnering;
    /** Returns true if the Unlock is currently asking its partner Lock to make a provide(...) request
        to its provider (if there is one).  This is meant to allow you
        to check for partnering cycles. */
    protected boolean isPartnering() { return partnering; }
    
    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
        if (getRefusesOffers()) { return false; }
        if (!getTypicalReceived().isSameType(amount)) throwUnequalTypeException(amount);

        if (isOffering()) throwCyclicOffers();  // cycle
        if (isPartnering()) throwCyclicPartnering(); // cycle
        
        if (!(atLeast >= 0 && atMost >= atLeast && atMost > 0 && atMost <= amount.getAmount()))
            throwInvalidAtLeastAtMost(atLeast, atMost, amount);

        double free = pool.getMaximum() - pool.getResource().getAmount();
        double increment = Math.min(numResources, free);                // don't increment above maximum
                
        // release the resource
        pool.getResource().increase(increment);
                
        Resource oldAmount = null;
        if (amount instanceof CountableResource)
            oldAmount = amount.duplicate();

        _amount = amount;
        _atLeast = atLeast;
        _atMost = atMost;
        boolean result = offerReceivers();
                
        if (!result) // gotta put it back
            {
            pool.getResource().decrease(increment);
            // pool.getResource().bound(pool.getMaximum());         // not needed
            }

        if (result)
            {
            double diff = amount.getAmount() - (oldAmount == null ? 1.0 : oldAmount.getAmount());
            totalAcceptedOfferResource += diff;
            totalReceivedResource += diff;
            }
                        
        _amount = null;         /// let it gc
        
        if (result && partner != null && partner.provider != null)
            {
            partnering = true;
            partner.provider.provide(partner);
            partnering = false;
            }
        return result;
        }

    public String toString()
        {
        return "Unlock@" + System.identityHashCode(this) + "(" + 
            (getName() == null ? "" : (getName() + ": ")) +
            (pool.getName() == null ? "Pool@" + System.identityHashCode(pool) : pool.getName()) + ", " +
            getTypicalProvided().getName() + ", " + numResources + ")";
        }  
                     
    public String getName() 
        { 
        return "Unlock (" + (pool.getName() == null ? "Pool " + System.identityHashCode(pool) : pool.getName()) + ")";
        }  
        
    
    Lock partner;
    public Lock getPartner() { return partner; }
    public void setPartner(Lock lock) { partner = lock; }  
    }
