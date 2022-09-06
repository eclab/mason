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

public class Decomposer extends Provider implements Receiver
    {
    public SimplePortrayal2D buildDefaultPortrayal(double scale)
        {
        return new ShapePortrayal2D(ShapePortrayal2D.POLY_COMPASS, 
            getFillPaint(), getStrokePaint(), getStrokeWidth(), scale);
        }
    private static final long serialVersionUID = 1;

    HashMap<Integer, Receiver> output;
    
    public Resource getTypicalReceived() { return typical; }
    public boolean hideTypicalReceived() { return true; }

    void throwNotCompositeEntity(Entity res)
        {
        throw new RuntimeException("The provided entity " + res + " was not composite (its storage didn't consist of an array of Resources).");
        }

    void throwDoNotUse()
        {
        throw new RuntimeException("Decomposers do not respond to addReceiver(Receiver).  Instead, use addReceiver(Receiver, Resource).");
        }

    public Decomposer(SimState state, Entity typical)
        {
        super(state, typical);
        output = new HashMap<Integer, Receiver>();
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
        if (!typical.isSameType(amount)) throwUnequalTypeException(amount);

        if (isOffering()) throwCyclicOffers();  // cycle
        
        if (!(atLeast >= 0 && atMost >= atLeast && atMost > 0))
            throwInvalidAtLeastAtMost(atLeast, atMost);

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
        
    public String toString()
        {
        return "Unpacker@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : getName()) + typical.getName() + ", " + typical + ")";
        }

    /** Does nothing.  There's no reason to step a Decomposer. */
    public void step(SimState state)
        {
        // do nothing
        }
        
    boolean refusesOffers = false;
    public void setRefusesOffers(boolean value) { refusesOffers = value; }
    public boolean getRefusesOffers() { return refusesOffers; }
    }
