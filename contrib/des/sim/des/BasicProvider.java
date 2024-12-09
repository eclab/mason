/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import java.util.*;
import sim.util.distribution.*;
import sim.portrayal.simple.*;
import sim.portrayal.*;
import sim.display.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;
import sim.des.portrayal.*;

/**
   A provider of resources. Providers also have a TYPICAL PROVIDED resource, 
   which exists only to provide a resource type.  Providers also have
   a RESOURCE, initially zero, of the same type, which is used as a 
   pool for resources.  The Provider class does not use the resource
   variable, and only makes it available as a convenience for subclasses
   to use as they see fit.  
   
   <p>A provider has any number of RECEIVERS which register
   themselves with it.  When a provider is ready to make an offer, it will
   do so using the offerReceivers() method, which offers to the receivers
   using one of several POLICIES.  Offers can be normal or TAKE-IT-OR-LEAVE-IT
   offers.
*/

public abstract class BasicProvider extends Provider
    {
    private static final long serialVersionUID = 1;

    /** Throws an exception indicating that entities were requested from this Provider, but it does not provide them. */
    protected void throwDoesNotProvideEntities()
        {
        throw new RuntimeException("This Provider was asked to provide Entities, but it does not.\n" + this);
        }

    /** Throws an exception indicating that an entity index was requested which cannot be provided. */
    protected void throwInvalidEntityNumber(int num)
        {
        throw new RuntimeException("This Provider asked to provide entity number " + num + " which is outside the range 0 ... " + entities.size() + ".\n" + this);
        }


    /** A resource pool available to subclasses.  null by default. */
    protected CountableResource resource;
    /** An entity pool available to subclasses.  null by default. */
    protected LinkedList<Entity> entities;

        
    /** First in First Out Offer Order for entities. */
    public static final int OFFER_ORDER_FIFO = 0;
    /** Last in First Out Offer Order for entities. */
    public static final int OFFER_ORDER_LIFO = 1;
    int offerOrder = OFFER_ORDER_FIFO;
    
    public void setOfferOrder(int offerOrder)
        {
        if (offerOrder < OFFER_ORDER_FIFO || offerOrder > OFFER_ORDER_LIFO)
            throw new IllegalArgumentException("Offer Order " + offerOrder + " out of bounds.");
        this.offerOrder = offerOrder;
        }
                
    public int getOfferOrder()
        {
        return offerOrder;
        }
    public boolean hideOfferOrder() { return true; }

    /** Offer Policy: offers are made to only one receiver, chosen via selectReceiver. */
    public static final int OFFER_POLICY_SELECT = 5;

    /** Sets the receiver offer policy.  Throws IllegalArgumentException if the policy is out of bounds. */
    public void setOfferPolicy(int offerPolicy) 
        { 
        if (offerPolicy != OFFER_POLICY_SELECT)
            super.setOfferPolicy(offerPolicy);
        else
            {
            this.offerPolicy = offerPolicy; 
            roundRobinPosition = 0; 
            }
        }
                
    /** 
        Clears any current entites and resources ready to be provided.
    */
    public void clear()
        {
        if (entities != null) entities.clear();
        if (resource != null) resource.clear();
        }
                
    /** 
        Computes the available resources this provider can provide.
    */
    public double getAvailable()
        {
        if (resource != null)
            return resource.getAmount();
        else if (entities != null)
            return entities.size();
        else
            return 0;
        }
        
    /** Returns in an array all the current Entities the Provider can provide.  You can
        modify the array (it's yours), but do not modify the Entities stored inside, as they
        are the actual Entities stored in the Provider.  If this Provider does not provide
        Entities, then null is returned.  */
    public Entity[] getEntities()
        {
        if (entities != null)
            return (Entity[])(entities.toArray(new Entity[entities.size()]));
        else return null;
        }
    public boolean hideEntities() { return true; }
    
    /** Builds a provider with no typical provided resource type at all.  This
        should only be called by classes such as Decomposer which do not
        have a typical provided resource type. */
    protected BasicProvider(SimState state)
        {
        super(state);
        entities = new LinkedList<Entity>();    // we'll use entities
        setName("BasicProvider");
        }

    /** 
        Builds a provider with the given typical resource type.
    */
    public BasicProvider(SimState state, Resource typicalProvided)
        {
        super(state, typicalProvided);

        if (typicalProvided instanceof Entity)
            {
            entities = new LinkedList<Entity>();         // even for null typicalProvided....
            }
        else
            {
            resource = (CountableResource) (typicalProvided.duplicate());
            resource.setAmount(0.0);
            }
        setName("BasicProvider");
        }
        

    /** 
        Makes an offer of up to the given amount to the given receiver.
        If the typical provided resource is an ENTITY, then atMost is ignored.
        Returns true if the offer was accepted.
        
        <p>If the resource in question is an ENTITY, then it is removed
        according to the current OFFER ORDER.  If the offer order is FIFO
        (default), then the entity is removed from the FRONT of the entities 
        linked list (normally entities are added to the END of the linked list
        via entities.add()).  If the offer order is LIFO, then the entity
        is removed from the END of the entities linked list.  Then this entity
        is offered to the receiver by calling offerReceiver(receiver, entity).
        
        <p>The only real reason for the atMost parameter is so that receivers
        can REQUEST to be offered atMost resource from a provider.
    */
    
    protected boolean offerReceiver(Receiver receiver, double atMost)
        {
        if (!getMakesOffers())
            {
            return false;
            }
                
        if (entities == null)
            {
            CountableResource cr = (CountableResource) resource;
            double originalAmount = cr.getAmount();
            double offer = originalAmount;
            if (offer > atMost) offer = atMost;
            if (offer <= 0) return false;
            lastOfferTime = state.schedule.getTime();
            boolean result = receiver.accept(this, cr, getOffersTakeItOrLeaveIt() ? offer : 0, offer);
            if (result)
                {
                CountableResource removed = (CountableResource)(resource.duplicate());
                removed.setAmount(originalAmount - cr.getAmount());
                updateLastAcceptedOffers(removed, receiver);
                }
            return result;
            }
        else if (offerOrder == OFFER_ORDER_FIFO)
            {
            if (entities.isEmpty()) return false;
            Entity e = entities.getFirst();
            boolean result = offerReceiver(receiver, e);                        // CHECK
            if (result)
                {
                entities.removeFirst();
                }
            return result;
            }
        else // if (offerOrder == OFFER_ORDER_LIFO)
            {
            if (entities.isEmpty()) return false;
            Entity e = entities.getLast();
            boolean result = offerReceiver(receiver, e);                        // CHECK
            if (result)
                {
                entities.removeLast();
                }
            return result;
            }
        }
       
                    
    /**
       Makes offers to the receivers according to the current offer policy.    
       Returns true if at least one offer was accepted.
    */
    protected boolean offerReceivers(ArrayList<Receiver> receivers)
        {
        boolean returnval = false;
        if (getOffersAllEntities() && entities != null)
            {
            while(!entities.isEmpty())
                {
                boolean result = offerReceiversOnce(receivers);
                returnval = returnval || result;
                if (!result) break;
                }
            }
        else returnval = offerReceiversOnce(receivers);
        return returnval;
        }
        
    boolean offerReceiversOnce(ArrayList<Receiver> receivers)
        {
        offering = true;
        boolean result = false;

        if (offerPolicy == OFFER_POLICY_SELECT)
            {
            int size = receivers.size();
            if (size == 0) 
                {
                if (!offerSelectWarned)
                    {
                    new RuntimeException("Warning: Offer policy is SELECT but there are no receivers to select from in " + this).printStackTrace();
                    offerSelectWarned = true;
                    }
                }
            else
                {                
                Resource oldResource;
                if (entities == null) 
                    {
                    oldResource = resource.duplicate();
                    }
                else
                    {
                    oldResource = entities.getFirst();
                    }
                Receiver receiver = selectReceiver(receivers, entities == null ? resource : entities.getFirst());
                if (receiver != null)
                    {
                    result = offerReceiver(receiver, Double.POSITIVE_INFINITY);
                    if (result)
                        {
                        selectedOfferAccepted(receiver, oldResource, resource);
                        }
                    }
                }
            }
        else
            {
            result = super.offerReceiversOnce(receivers);
            }
            
        offering = false;
        return result;
        }
    
    
    
    /**
       If the offer policy is OFFER_POLICY_SELECT, then when the receivers are non-empty,
       this method will be called to specify which receiver should be offered the given resource.
       Override this method as you see fit.  The default implementation simply returns the first one.
    */
    public Receiver selectReceiver(ArrayList<Receiver> receivers, Resource resource)
        {
        return receivers.get(0);
        }
        
    /**
       If the offer policy is OFFER_POLICY_SELECT, then if a receiver accepts an offer of a resource,
       this method is called, with (a copy of) the original resource, the revised resource after then
       receiver accepted it. If the resource was an ENTITY, then the revised resource will likely be unchanged.
       If the resource was a COUNTABLE RESOURCE, then the revised resource will be reduced by the amount
       that the receiver accepted (relative to the original resource).
    */
    protected void selectedOfferAccepted(Receiver receiver, Resource originalResource, Resource revisedResource)
        {
        return;
        }
        
    /**
       Asks the Provider to offer to the given receiver entity #entityNumber in its entities list.  You can get this
       entity number by requesting getEntities(), then returning the index of the entity of interest
       in the resulting array.  If you want to grab multiple entities, call this method multiple times; but
       beware that as you pull entities out, the entity list shrinks and the indexes change.  The easiest
       way to deal with this is to call getEntities() once, and then request entities one by one going BACKWARDS 
       through the resulting list.    If this Provider does not offer entities, or if
       the entityNumber is invalid, an exception is thrown.
    */
    public boolean requestEntity(Receiver receiver, int entityNumber)
        {
        Entity e = getEntity(entityNumber);
        boolean result = offerReceiver(receiver, e);                                    // CHECK
        if (result)
            {
            entities.remove(entityNumber);
            }
        return result;
        }

    /**
       Returns the available entity with the given number for inspection, but does not remove it from the available pool.
       Numbers range from 0 to (int)getAvailable();  You probably should not modify this Entity: it is the actual
       Entity in the Provider and is owned by the Provider.  If this Provider does not offer entities, or if
       the entityNumber is invalid, an exception is thrown.
    */
    public Entity getEntity(int entityNumber)
        {
        if (entities == null)
            throwDoesNotProvideEntities();
        if (entityNumber < 0 || entityNumber > entities.size())
            throwInvalidEntityNumber(entityNumber);
                
        return entities.get(entityNumber);
        }

    }
