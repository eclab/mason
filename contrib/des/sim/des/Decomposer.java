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
   A decomposer breaks up a composite Entity and offers its composed elements to receivers.
   At present only one receiver may be attached to a given resource type, though this may change.
**/

public class Decomposer extends Middleman
    {
    public SimplePortrayal2D buildDefaultPortrayal(double scale)
        {
        return new ShapePortrayal2D(ShapePortrayal2D.POLY_COMPASS, 
            getFillPaint(), getStrokePaint(), getStrokeWidth(), scale);
        }
    private static final long serialVersionUID = 1;

    HashMap<Integer, Receiver> output;
    Resource typicalReceived;
    
    void throwNotCompositeEntity(Entity res)
        {
        throw new RuntimeException("The provided entity " + res + " was not composite (its storage didn't consist of an array of Resources).");
        }

    void throwDoNotUse()
        {
        throw new RuntimeException("Decomposers do not respond to addReceiver(Receiver).  Instead, use addReceiver(Receiver, Resource).");
        }

    public Decomposer(SimState state, Entity typicalReceived)
        {
        super(state, null);
        this.typicalReceived = typicalReceived;
        output = new HashMap<Integer, Receiver>();
        }
                
    /** Returns null because the Decomposer can provide anything which is packed into the composite entities it receives. */
    public Resource getTypicalProvided() 
        { 
        return null; 
        }

    public Resource getTypicalReceived() 
        { 
        return typicalReceived; 
        }

    /**
       Registers a receiver.  Only one receiver may be registered for a given type.
       If a receiver cannot be registered because another has already been registered
       for that type, FALSE is returned.
    **/
    public boolean addReceiver(Receiver receiver)
        {
        Integer type = receiver.getTypicalReceived().getType();
        if (output.get(type) != null) return false;             // someone is already registered for this resource
        boolean result = super.addReceiver(receiver);
                
        if (result)
            {
            output.put(type, receiver);
            }
        return result;
        }

    public boolean removeReceiver(Receiver receiver)
        {
        for(Integer type : output.keySet())
            {
            if (output.get(type) == receiver)
                {
                output.remove(type);
                break;
                }
            }
        return super.removeReceiver(receiver);
        }

    /**
       Accepts the resource, which must be a composite Entity, and offers the resources in its
       storage to downstream receivers.
    **/
    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {       
        if (getRefusesOffers()) { return false; }
        if (!getTypicalReceived().isSameType(amount)) throwUnequalTypeException(amount);

        if (isOffering()) throwCyclicOffers();  // cycle
        
        if (!(atLeast >= 0 && atMost >= atLeast && atMost > 0 && atMost <= amount.getAmount()))
            throwInvalidAtLeastAtMost(atLeast, atMost, amount);

        // unpack
        Entity entity = (Entity)amount;
                
        boolean accepted = false;
        if (!entity.isComposite())
            {
            throwNotCompositeEntity(entity);
            return false;                                           // not reachable, Java is stupid
            }
        else if (entity.getStorage() instanceof Resource[])
            {
            processEntityInfoFor(entity);            
            Resource[] res = entity.getStorage();
            for(int i = 0; i < res.length; i++)
                {
                Receiver recv = output.get(res[i].getType());
                if (recv != null)
                    {
                    accepted = accepted || recv.accept(this, res[i], 0, res[i].getAmount());
                    }
                }
            return accepted;
            }
        else 
            {
            throwNotCompositeEntity(entity);
            return false;                                           // not reachable, Java is stupid
            }
        }
        
    /** This is called when the Decomposer breaks apart a composite entity,
        immediately before extracting the elements in its Storage and offering
        them to downstream Receivers.  It's meant to give you an opportunity
        to process the Entity's Info object if you need to. By default this does nothing. */
    protected void processEntityInfoFor(Entity entity)
        {
        }
    
    public String toString()
        {
        return "Decomposer@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : (getName() + ": ")) + getTypicalProvided().getName() + " -> " + getTypicalReceived().getName() + ")";
        }

    /** Does nothing.  There's no reason to step a Decomposer. */
    public void step(SimState state)
        {
        // do nothing
        }
    }
