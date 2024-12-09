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
    receivers immediately.  You can change this behavior by setting setOffersImmediately(false).
    Whenever it is stepped by the Schedule, the Queue will also offer to its receivers.  You can
    prevent this by not scheduling it in the first place.  Like all Providers, the Queue will
    make an offer if possible to any Receiver that requests one via provide(...). 
*/

public class RandomQueue extends Provider
    {
    public SimplePortrayal2D buildDefaultPortrayal(double scale)
        {
        return new ShapePortrayal2D(ShapePortrayal2D.SHAPE_STORAGE, 
            getFillPaint(), getStrokePaint(), getStrokeWidth(), scale);
        }

    private static final long serialVersionUID = 1;

    ArrayList<Entity> randomEntities = new ArrayList<Entity>();
    
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

    void throwInvalidCapacityException(double capacity)
        {
        throw new RuntimeException("Capacities may not be negative or NaN.  capacity was: " + capacity);
        }

    void throwNotEntityException()
        {
        throw new RuntimeException("RandomQueue only works with Entities.");
        }

    public RandomQueue(SimState state, Resource typical)
        {
        super(state, typical);
        if (!(typical instanceof Entity))
            throwNotEntityException(); 
        setName("RandomQueue");
        }
        
    protected int numEntities()
        {
        return randomEntities.size();
        }

    protected void clearEntities()
        {
        randomEntities.clear();
        }

    /**
       Makes offers to the receivers according to the current offer policy.    
       Returns true if at least one offer was accepted.
    */
    protected boolean offerReceivers(ArrayList<Receiver> receivers)
        {
        boolean returnval = false;
        if (getOffersAllEntities())
            {
            while(numEntities() > 0)
                {
                boolean result = offerReceiversOnce(receivers);
                returnval = returnval || result;
                if (!result) break;
                }
            }
        else returnval = offerReceiversOnce(receivers);
        return returnval;
        }
        
    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
        if (!(amount instanceof Entity)) 
            {
            throwNotEntityException(); 
            return false;           // never happens
            }
        else
            {
            if (capacity - numEntities() >= 1)
                {
                randomEntities.add((Entity)amount);
                totalReceivedResource += 1.0;
                if (getOffersImmediately()) offerReceivers(); 
                return true;
                }
            else return false;
            }
        }

    public void clear()
        {
        clearEntities();
        }
        
    public double getAvailable()
        {
        return numEntities();
        }
        
    public Entity[] getEntities()
        {
        return (Entity[])(randomEntities.toArray(new Entity[numEntities()]));
        }

    protected boolean offerReceiver(Receiver receiver, double atMost)
        {
        if (!getMakesOffers())
            {
            return false;
            }
                
        int entityNumber = state.random.nextInt(numEntities());
        Entity e = randomEntities.get(entityNumber);
        boolean result = offerReceiver(receiver, e);                        // CHECK
        if (result)
            {
            // Move the top entity to that location
            randomEntities.set(entityNumber, randomEntities.get(numEntities() - 1));                // replace with top
            randomEntities.remove(numEntities() - 1);               // remove top
            }
        return result;
        }
        
    protected double totalReceivedResource;
    public double getTotalReceivedResource() { return totalReceivedResource; }
    public double getReceiverResourceRate() { double time = state.schedule.getTime(); if (time <= 0) return 0; else return totalReceivedResource / time; }

    public void reset(SimState state) 
        {
        super.reset(state);
        totalReceivedResource = 0; 
        }

    public String toString()
        {
        return "RandomQueue@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : (getName() + ": ")) + getTypicalProvided().getName() + ")";
        }               

    public void step(SimState state)
        {
        offerReceivers();
        }
    }
        
