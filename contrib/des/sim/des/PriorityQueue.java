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
    A blocking resource queue with a capacity: you can think of Queue as a warehouse with a maximum
    amount of space.  Resources placed in the queue by default are offered to downstream
    recievers immediately.  You can change this behavior by setting setOffersImmediately(false).
    Whenever it is stepped by the Schedule, the Queue will also offer to its receivers.  You can
    prevent this by not scheduling it in the first place.  Like all Providers, the Queue will
    make an offer if possible to any Receiver that requests one via provide(...). 
*/

public class PriorityQueue extends RandomQueue
    {
    public SimplePortrayal2D buildDefaultPortrayal(double scale)
        {
        return new ShapePortrayal2D(ShapePortrayal2D.SHAPE_STORAGE, 
            getFillPaint(), getStrokePaint(), getStrokeWidth(), scale);
        }

    private static final long serialVersionUID = 1;

    Heap priorityEntities = new Heap();
    
   /** Throws an exception indicating that a null info was provided. */
    protected void throwInfoNullException(Entity entity)
        {
        throw new RuntimeException("For the following PriorityQueue, the default getComparable(Entity) method " +
            "is being used, but an Entity provided has a null Info object and so cannot be compared.\n" +
            "PriorityQueue:  " + this + "\n" +
            "Entity:         " + entity);
        }

    /** Throws an exception indicating that a non-Comparable info was provided. */
    protected void throwInfoNotComparableException(Entity entity)
        {
        throw new RuntimeException("For the following PriorityQueue, the default getComparable(Entity) method " +
            "is being used, but an Entity provided has a non-Comparable Info object and so cannot be compared.\n" +
            "PriorityQueue:  " + this + "\n" +
            "Entity:         " + entity + "\n" +
            "Entity Info:    " + entity.info);
        }

    void throwNotEntityException()
        {
        throw new RuntimeException("RandomQueue only works with Entities.");
        }

    public PriorityQueue(SimState state, Entity typical)
        {
        super(state, typical);
        randomEntities = null;          // just in case
        setName("PriorityQueue");
        }
        
    public Comparable getComparable(Entity entity)
        {
        if (entity.info == null) throwInfoNullException(entity);
        if (!(entity.info instanceof Comparable)) throwInfoNotComparableException(entity);
        return (Comparable)(entity.info);
        }
        
    protected int numEntities()
        {
        return priorityEntities.size();
        }

    protected void clearEntities()
        {
        priorityEntities.clear();
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
                priorityEntities.add((Entity)amount, getComparable((Entity)amount));
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
        Object[] objs = priorityEntities.getObjects();
        Entity[] ents = new Entity[objs.length];
        for(int i = 0; i < ents.length; i++) ents[i] = (Entity)(objs[i]);
        return ents;
        }

    protected boolean offerReceiver(Receiver receiver, double atMost)
        {
        if (!getMakesOffers())
            {
            return false;
            }
                
        if (numEntities() == 0) 
            {
            return false;
            }
        else
            {
            Entity e = (Entity)(priorityEntities.getMin());
            boolean result = offerReceiver(receiver, e);                        // CHECK
            if (result)
                {
                priorityEntities.extractMin();
                }
            return result;
            }
        }
        
    public String toString()
        {
        return "PriorityQueue@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : (getName() + ": ")) + getTypicalProvided().getName() + ")";
        }               
    }
        
