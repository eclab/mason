/*
  Copyright 2024 by Sean Luke and George Mason University
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
	A variation of Queue which restricts its typical provided and typical received resource types
	to only be Entity.  When it offers an Entity, it offers a *random* Entity from among those in
	its internal storage.  Note that RandomQueue neither uses the its entities linked list nor its
	internal resource pool.  By default entities is set to an (empty) LinkedList, not to null, but
	do not use or rely on it in subclasses.
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
        
