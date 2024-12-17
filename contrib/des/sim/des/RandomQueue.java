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
	A variation of Queue which returns random 
    A blocking resource queue with a capacity: you can think of Queue as a warehouse with a maximum
    amount of space.  Resources placed in the queue by default are offered to downstream
    receivers immediately.  You can change this behavior by setting setOffersImmediately(false).
    Whenever it is stepped by the Schedule, the Queue will also offer to its receivers.  You can
    prevent this by not scheduling it in the first place.  Like all Providers, the Queue will
    make an offer if possible to any Receiver that requests one via provide(...). 
*/

public class RandomQueue extends Queue
    {
    public SimplePortrayal2D buildDefaultPortrayal(double scale)
        {
        return new ShapePortrayal2D(ShapePortrayal2D.SHAPE_STORAGE, 
            getFillPaint(), getStrokePaint(), getStrokeWidth(), scale);
        }

    private static final long serialVersionUID = 1;

    ArrayList<Entity> randomEntities = new ArrayList<Entity>();
    
    void throwNotEntityException()
        {
        throw new RuntimeException("RandomQueue only works with Entities.");
        }

    public RandomQueue(SimState state, Entity typical)
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
        
    public String toString()
        {
        return "RandomQueue@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : (getName() + ": ")) + getTypicalProvided().getName() + ")";
        }               
    }
        
