/*
  Copyright 2024 by Sean Luke and George Mason University
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
	A variation of Queue which restricts its typical provided and typical received resource types
	to only be Entity.  Each Entity has an associated Comparable value, and when it offers an Entity,
	it offers the lowest comparable Entity from among those in its internal storage.  The Comparable
	value of an Entity is determined by the getComparable(Entity) method, which you may override.
	The default version of this method assumes that each Entity has an info object which implements
	Comparable, and it uses that (else throws an error).  Note that PriorityQueue neither uses the 
	its entities linked list nor its internal resource pool.  By default entities is set to an 
	(empty) LinkedList, not to null, but do not use or rely on it in subclasses.
*/

public class PriorityQueue extends Queue
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
        throw new RuntimeException("PriorityQueue only works with Entities.");
        }

    public PriorityQueue(SimState state, Entity typical)
        {
        super(state, typical);
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
        
