/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import java.util.*;
import sim.portrayal.*;
import sim.portrayal.simple.*;
import java.awt.*;

/**
   A currency converter, so to speak.  Transformer takes a RESOURCE and returns a COUNTABLE RESOURCE, 
   normally of a different type, by converting the value of the resource into the value of the new one,
   and then offering the new resource.
        
   <p>If the original resource was also a countable resource, then it is converted by multiplying it by 
   a conversion factor.  For example, if we were converting dollars to euros, and one dollar was worth 
   1.5 euros, we might set the conversion to 1.5.  If the downstream receiver accepts the offer but only
   partially, then the original resource is modified accordingly according to the conversion factor.
        
   <p>If the original resource was an Entity, then we simply offer the conversion factor.  Thus one entity
   might be worth 1.5 euros.  By default, if the original resource type is an Entity, Transformer is 
   automatically set to take-it-or-leave-it offers.  Thus if the downstream receiver must accept the offer
   totally.  You can change this by setting take-it-or-leave-it to false; now if the downstream receiver
   accepts the offer only partially, the Entity is consumed regardless.
*/

public class Transformer extends Filter
    {
    public SimplePortrayal2D buildDefaultPortrayal(double scale)
        {
        return new ShapePortrayal2D(ShapePortrayal2D.POLY_POINTER_RIGHT, 
            getFillPaint(), getStrokePaint(), getStrokeWidth(), scale);
        }

    private static final long serialVersionUID = 1;

    Resource typicalReceived;
    double conversion;
        
    public Transformer(SimState state, CountableResource typicalProvided, Resource typicalReceived, double conversion)
        {
        // typical being our _amount CountableResource
        super(state, typicalProvided);
        this.typicalReceived = typicalReceived.duplicate();
        this.conversion = conversion;
        if (typicalReceived instanceof Entity)
            {
            setOffersTakeItOrLeaveIt(true);
            }
        setName("Transformer");
        }

    public boolean provide(Receiver receiver, double atMost) 
        {
        if (isProviding())
            {
            throwCyclicProvisions();
            }
        if (provider == null) return false;
        if (!isPositiveNonNaN(atMost))
            throwInvalidNumberException(atMost);
        providing = true;
        boolean val = provider.provide(this, atMost / conversion);
        providing = false;
        return val;
        }

    public Resource getTypicalReceived()
        {
        return typicalReceived;
        }

    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {       
        if (getRefusesOffers()) { return false; }
        if (!getTypicalReceived().isSameType(amount)) throwUnequalTypeException(amount);

        if (isOffering()) throwCyclicOffers();  // cycle
        
        if (!(atLeast >= 0 && atMost >= atLeast && atMost > 0 && atMost <= amount.getAmount()))
            throwInvalidAtLeastAtMost(atLeast, atMost, amount);
                
        if (getTypicalReceived() instanceof Entity)
            {
            double _atLeast = 1.0 * conversion;
            double _atMost = 1.0 * conversion;
            CountableResource _amount = (CountableResource)(getTypicalProvided().duplicate());
            double oldAmount = 1.0 * conversion;
            _amount.setAmount(oldAmount);
            boolean retval = offerReceivers(_amount, _atLeast, _atMost);
            // We assume that the Entity has been consumed if retval = true
            return retval;
            }
        else 
            {
            double _atLeast = atLeast * conversion;
            double _atMost = atMost * conversion;
            CountableResource _amount = (CountableResource)(getTypicalProvided().duplicate());
            double oldAmount = amount.getAmount() * conversion;
            _amount.setAmount(oldAmount);
            boolean retval = offerReceivers(_amount, _atLeast, _atMost);
            if (retval)
                {
                // modify original
                ((CountableResource)amount).setAmount((oldAmount - amount.getAmount()) / conversion);
                }
            return retval;
            }
        }
        
    public String toString()
        {
        return "Transformer@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : (getName() + ": ")) + getTypicalProvided().getName() + " -> " + getTypicalReceived().getName() + ")";
        }
    }
