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
   An If conditionally offers received resources to exactly one of N possible receivers depending the 
   value of the selectReceiver(...) method.
*/

public abstract class If extends Filter
    {
    public SimplePortrayal2D buildDefaultPortrayal(double scale)
    	{
    	return new ShapePortrayal2D(ShapePortrayal2D.POLY_TRIANGLE_RIGHT, 
    		getFillPaint(), getStrokePaint(), getStrokeWidth(), scale);
    	}

    private static final long serialVersionUID = 1;

    /** Builds a lock attached to the given pool and with the given amount of resources acquired each time. */
    public If(SimState state, Resource typical)
        {
        super(state, typical);
        setName("If");
        super.setOfferPolicy(OFFER_POLICY_SELECT);
        }
    
    public void setOfferPolicy(int offerPolicy)
    	{
    	throw new IllegalArgumentException("Offer Policy may not be set in an If.");
    	}
    	
    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
    	if (getRefusesOffers()) { return false; }
        if (!typical.isSameType(amount)) throwUnequalTypeException(amount);
        if (isOffering()) throwCyclicOffers();  // cycle
        
        if (!(atLeast >= 0 && atMost >= atLeast))
        	throwInvalidAtLeastAtMost(atLeast, atMost);

        _amount = amount;
        _atLeast = atLeast;
        _atMost = atMost;
        boolean result = offerReceivers();
        return result;
        }

    public String toString()
        {
        return "If@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : getName()) + ", " + typical.getName() + ")";
        }  
                     
    /**
       If the offer policy is OFFER_POLICY_SELECT, then when the receivers are non-empty,
       this method will be called to specify which receiver should be offered the given resource.
    */
    public abstract Receiver selectReceiver(ArrayList<Receiver> receivers, Resource resource);
    }
