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
   value of the selectReceiver(...) method.  Afterwards, the offerSuccessful(...) method will be called
   if the receiver accepted the offer.
*/

public abstract class If extends Middleman
    {
    public SimplePortrayal2D buildDefaultPortrayal(double scale)
        {
        return new ShapePortrayal2D(ShapePortrayal2D.POLY_TRIANGLE_RIGHT, 
            getFillPaint(), getStrokePaint(), getStrokeWidth(), scale);
        }

    private static final long serialVersionUID = 1;

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
        if (!getTypicalReceived().isSameType(amount)) throwUnequalTypeException(amount);
        if (isOffering()) throwCyclicOffers();  // cycle
        
        if (!(atLeast >= 0 && atMost >= atLeast && atMost > 0 && atMost <= amount.getAmount()))
            throwInvalidAtLeastAtMost(atLeast, atMost, amount);

        if (entities != null)
            {
            entities.clear();
            entities.add((Entity)amount);
            boolean result = offerReceivers();
            entities.clear();           // just to be safe
            if (result) totalReceivedResource += 1.0;
            return result;
            }
        else
            {
            CountableResource oldResource = resource;
            oldResource = (CountableResource)amount;
            boolean result = offerReceivers();
            resource = oldResource;
            if (result) totalReceivedResource += atMost;
            return result;
            }
        }

    /** Returns false always and does nothing: If is push-only. */
    public boolean provide(Receiver receiver)
        {
        return false;
        }

    public String toString()
        {
        return "If@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : (getName() + ": ")) + getTypicalProvided().getName() + ")";
        }  
 
    /**
       If the offer policy is OFFER_POLICY_SELECT, then when the receivers are non-empty,
       this method will be called to specify which receiver should be offered the given resource.
    */
    public abstract Receiver selectReceiver(ArrayList<Receiver> receivers, Resource resource);
    }
