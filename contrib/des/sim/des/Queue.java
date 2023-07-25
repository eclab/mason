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
    A blocking resource queue with a capacity: you can think of Queue as a warehouse with a maximum
    amount of space.  Resources placed in the queue by default are offered to downstream
    recievers immediately.  You can change this behavior by setting setOffersImmediately(false).
    Whenever it is stepped by the Schedule, the Queue will also offer to its receivers.  You can
    prevent this by not scheduling it in the first place.  Like all Providers, the Queue will
    make an offer if possible to any Receiver that requests one via provide(...). 
*/

public class Queue extends Middleman implements Steppable
    {
    public SimplePortrayal2D buildDefaultPortrayal(double scale)
        {
        return new ShapePortrayal2D(ShapePortrayal2D.SHAPE_STORAGE, 
            getFillPaint(), getStrokePaint(), getStrokeWidth(), scale);
        }
    private static final long serialVersionUID = 1;

    void throwInvalidCapacityException(double capacity)
        {
        throw new RuntimeException("Capacities may not be negative or NaN.  capacity was: " + capacity);
        }

    double capacity = Double.POSITIVE_INFINITY;    

    /** Returns the maximum available resources that may be aquired by the Queue. */
    public double getCapacity() { return capacity; }
    public boolean hideCapacity() { return true; }
    
    /** Set the maximum available resources that may be aquired by the Queue. 
            
        <p>Throws a runtime exception if the capacity is negative or NaN.
    */
    public void setCapacity(double d) 
        { 
        if (!isPositiveOrZeroNonNaN(d))
            throwInvalidCapacityException(d); 
        capacity = d; 
        }

    boolean offersImmediately = true;
    
    /** Returns whether the Queue offers items immediately upon accepting (when possible) in zero time,
        as opposed to when it is stepped. */
    public boolean getOffersImmediately() { return offersImmediately; }
    public boolean hideOffersImmediately() { return true; }

    /** Sets whether the Queue offers items immediately upon accepting (when possible) in zero time,
        as opposed to when it is stepped. */
    public void setOffersImmediately(boolean val) { offersImmediately = val; }

    /** 
        Builds a queue with the given typical resource type.
    */
    public Queue(SimState state, Resource typical)
        {
        super(state, typical);
        setName("Queue");
        }

    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
        if (getRefusesOffers()) { return false; }
        if (isOffering()) throwCyclicOffers();  // cycle
        
        if (!getTypicalReceived().isSameType(amount)) throwUnequalTypeException(amount);

        if (!(atLeast >= 0 && atMost >= atLeast && atMost > 0 && atMost <= amount.getAmount()))
            throwInvalidAtLeastAtMost(atLeast, atMost, amount);

        if (entities == null)
            {
            if (capacity - resource.getAmount() >= atLeast)
                {
                double transfer = Math.min(Math.min(capacity - resource.getAmount(), atMost), amount.getAmount());
                resource.increase(transfer);
                ((CountableResource)amount).decrease(transfer);
                totalReceivedResource += atMost;
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
                totalReceivedResource += 1.0;
                if (getOffersImmediately()) offerReceivers(); 
                return true;
                }
            else return false;
            }
        }
        
    public String toString()
        {
        return "Queue@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : (getName() + ": ")) + getTypicalProvided().getName() + ")";
        }               

    /** Upon being stepped, the Queue offers to registered receivers.  You don't have to
        schedule the Queue at all; in which case this method would never be called. */
                
    public void step(SimState state)
        {
        offerReceivers();
        }
    }
        
