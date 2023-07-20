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

public abstract class Provider extends DESPortrayal implements ProvidesBarData, Parented
    {
    public boolean hideDrawState() { return true; }
    public boolean getDrawState() { return false; }

    private static final long serialVersionUID = 1;

    /** Throws an exception indicating that an offer cycle was detected. */
    protected void throwInvalidMinMax()
        {
        throw new RuntimeException("accept(...) was called with atLeast > atMost." );
        }

    /** Throws an exception indicating that an offer cycle was detected. */
    protected void throwCyclicOffers()
        {
        throw new RuntimeException("Zero-time cycle found among offers including this one." );
        }

    /** Throws an exception indicating that the given resource does not match the Provider's typical provided resource. */
    protected void throwUnequalTypeException(Resource res)
        {
        throw new RuntimeException("Expected resource type " + this.getTypicalProvided().getName() + "(" + this.getTypicalProvided().getType() + ")" +
            " but got resource type " + res.getName() + "(" + res.getType() + ")" );
        }

    /** Throws an exception indicating that atLeast and atMost are out of legal bounds. */
    protected void throwInvalidAtLeastAtMost(double atLeast, double atMost, Resource amount)
        {
        if (atMost <= 0)
        	throw new RuntimeException("Requested resource may not be at most 0.");
        else if (atMost >= amount.getAmount())
        	throw new RuntimeException("Requested resource " + atMost + " may not be larger than actual resource amount: " + amount);
        else
	        throw new RuntimeException("Requested resource amounts are between " + atLeast + " and " + atMost + ", which is out of bounds.");
        }

    /** Throws an exception indicating that entities were requested from this Provider, but it does not provide them. */
    protected void throwDoesNotProvideEntities()
        {
        throw new RuntimeException("This Provider was asked to provide Entities, but it does not.");
        }

    /** Throws an exception indicating that an entity index was requested which cannot be provided. */
    protected void throwInvalidEntityNumber(int num)
        {
        throw new RuntimeException("This Provider asked to provide entity number " + num + " which is outside the range 0 ... " + entities.size() + ".");
        }

    /** Tests if val is non-NaN and positive. */
    protected boolean isPositiveNonNaN(double val)
        {
        return (val > 0);
        }
    public boolean hidePositiveNonNaN() { return true; }

    /** Tests if val is non-NaN and positive or zero. */
    protected boolean isPositiveOrZeroNonNaN(double val)
        {
        return (val >= 0);
        }
    public boolean hidePositiveOrZeroNonNaN() { return true; }

    /** Throws an exception indicating that atMost is illegal. */
    void throwInvalidNumberException(double amount)
        {
        throw new RuntimeException("atMost may not be negative, zero, or NaN.  Amount provided was: " + amount);
        }



    // receivers registered with the provider
    ArrayList<Receiver> receivers = new ArrayList<Receiver>();

    /** The typical kind of resource the Provider provides.  This should always be zero and not used except for type checking. */
    Resource typicalProvided;
    /** A resource pool available to subclasses.  null by default. */
    protected CountableResource resource;
    /** An entity pool available to subclasses.  null by default. */
    protected LinkedList<Entity> entities;
    /** The model. */
    protected SimState state;
    
    public SimState getState()
        {
        return state;
        }
        
    /** 
        Returns the typical kind of resource the Provider provides.  
        This should always be zero in size and not used except for type checking.
        If (rarely) the Provider may provide a variety of types, such as a Decomposer,
        then this method should return null. 
    */
    public Resource getTypicalProvided() { return typicalProvided; }

    
    ////// OFFER STATISTICS
    //////
    ////// These methods maintain the most recent offers made, which can be used to update graphical
    ////// interface information regarding offers between nodes.
    
    /// The timestampof the most recent offers
    double lastOfferTime = Schedule.BEFORE_SIMULATION;
    /// The most recent offers
    ArrayList<Resource> lastAcceptedOffers = new ArrayList<Resource>();
    /// The most receivers for the most recent offers
    ArrayList<Receiver> lastAcceptedOfferReceivers = new ArrayList<Receiver>();
    /// The timestamp of the most recent accepted offers
    double lastAcceptedOfferTime = Schedule.BEFORE_SIMULATION;
    /** Returns the most recent offers accepted. */
    public ArrayList<Resource> getLastAcceptedOffers() { return lastAcceptedOffers; }
    /** Returns the receivers for the most recent offers accepted. */
    public ArrayList<Receiver> getLastAcceptedOfferReceivers() { return lastAcceptedOfferReceivers; }
    /** Returns the timestamp for the most recent offers made. */
    public double getLastOfferTime() { return lastOfferTime; }
    /** Returns the timestamp for the most recent offers accepted. */
    public double getLastAcceptedOfferTime() { return lastAcceptedOfferTime; }
    
    // Clears the last accepted offers, and sets the time to the given time.
    void clearLastAcceptedOffers(double newTime) { lastAcceptedOffers.clear(); lastAcceptedOfferReceivers.clear(); lastAcceptedOfferTime = newTime; }
    
    // If the last offer time is less than the current time, clears the offers to the current time.
    // Adds the given resource and receiver to the new offers.
    void updateLastAcceptedOffers(Resource resource, Receiver receiver)
        {
        double currentTime = state.schedule.getTime();
        clearLastAcceptedOffers(currentTime);
        lastAcceptedOffers.add(resource.duplicate());
        lastAcceptedOfferReceivers.add(receiver);
        totalAcceptedOfferResource += resource.getAmount();
        }
    
    double totalAcceptedOfferResource;
    public double getTotalAcceptedOfferResource() { return totalAcceptedOfferResource; }
    public double getOfferResourceRate() { double time = state.schedule.getTime(); if (time <= 0) return 0; else return totalAcceptedOfferResource / time; }
    
        
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

    /** Offer Policy: offers are made to the first receiver, then the second, and so on, until available resources or receivers are exhausted. */
    public static final int OFFER_POLICY_FORWARD = 0;
    /** Offer Policy: offers are made to the last receiver, then the second to last, and so on, until available resources or receivers are exhausted. */
    public static final int OFFER_POLICY_BACKWARD = 1;
    /** Offer Policy: offers are made to the least recent receiver, then next, and so on, until available resources or receivers are exhausted. */
    public static final int OFFER_POLICY_ROUND_ROBIN = 2;
    /** Offer Policy: offers are made to the receivers in a randomly shuffled order, until available resources or receivers are exhausted. */
    public static final int OFFER_POLICY_SHUFFLE = 3;
    /** Offer Policy: offers are made to only one random receiver, chosen via an offer distribution or, if the offer distribution is null, chosen uniformly. */
    public static final int OFFER_POLICY_RANDOM = 4;
    /** Offer Policy: offers are made to only one receiver, chosen via selectReceiver. */
    public static final int OFFER_POLICY_SELECT = 5;
    int offerPolicy;
    int roundRobinPosition = 0;
    boolean offersTakeItOrLeaveIt;
    AbstractDiscreteDistribution offerDistribution;

    boolean offering;
    /** Returns true if the Provider is currently making an offer to a receiver (this is meant to allow you
        to check for offer cycles. */
    protected boolean isOffering() { return offering; }

    /** Sets the receiver offer policy.  Throws IllegalArgumentException if the policy is out of bounds. */
    public void setOfferPolicy(int offerPolicy) 
        { 
        if (offerPolicy < OFFER_POLICY_FORWARD || offerPolicy > OFFER_POLICY_SELECT)
            throw new IllegalArgumentException("Offer Policy " + offerPolicy + " out of bounds.");
        this.offerPolicy = offerPolicy; 
        roundRobinPosition = 0; 
        }
                
    /** Returns the receiver offer policy */
    public int getOfferPolicy() { return offerPolicy; }
    public boolean hideOfferPolicy() { return true; }
         
    /** Sets the receiver offer policy to OFFER_POLICY_RANDOM, and
        sets the appropriate distribution for selecting a receiver.  If null is provided 
        for the distribution, receivers are selected randomly.  Selection via an offer
        distribution works as follows: a random integer is selected from the distribution.
        If this integer is < 0 or >= the number of receivers registered, then a warning
        is produced and no offer is made (this should NOT happen).  Otherwise an offer
        is made to the registered receiver corresponding to the selected integer.
    */
    public void setOfferDistribution(AbstractDiscreteDistribution distribution)
        {
        setOfferPolicy(OFFER_POLICY_RANDOM);
        offerDistribution = distribution;
        }
        
    /** Sets the receiver offer policy to OFFER_POLICY_RANDOM, and
        sets the appropriate distribution for selecting a receiver.  If null is provided 
        for the distribution, receivers are selected randomly.  Selection via an offer
        distribution works as follows: a random integer is selected from the distribution.
        An offer is made to the registered receiver corresponding to the index of the 
        selected slot in the distribution.  The distribution must exactly match the size
        of the number of registered receivers.
    */
    public void setOfferDistribution(double[] distribution)
        {
        setOfferDistribution(new EmpiricalWalker(distribution, Empirical.NO_INTERPOLATION, state.random));
        }
        

    /** Returns the current offer distribution, or null if none.
    */
    public AbstractDistribution getOfferDistribution()
        {
        return offerDistribution;
        }
    public boolean hideOfferDistribution() { return true; }
    
    /** 
        Clears any current entites and resources ready to be provided.
    */
    public void clear()
        {
        if (entities != null) entities.clear();
        if (resource != null) resource.clear();
        }
                
    /** 
        Sets whether receivers are offered take-it-or-leave-it offers.
        A take-it-or-leave-it offer requires the Receiver to accept all of the offered Resource,
        or else reject it all.
    */
    public void setOffersTakeItOrLeaveIt(boolean val) { offersTakeItOrLeaveIt = val; }

    /**  
         Returns whether receivers are offered take-it-or-leave-it offers.
         A take-it-or-leave-it offer requires the Receiver to accept all of the offered Resource,
         or else reject it all.
    */
    public boolean getOffersTakeItOrLeaveIt() { return offersTakeItOrLeaveIt; }
    public boolean hideOffersTakeItOrLeaveIt() { return true; }

    /** 
        Registers a receiver with the Provider.  Returns false if the receiver was already registered.
    */
    public boolean addReceiver(Receiver receiver)
        {
        if (receivers.contains(receiver)) return false;
        receivers.add(receiver);
        return true;
        }
                                
    /** 
        Returns all registered receivers.
    */
    public ArrayList<Receiver> getReceivers()
        {
        return receivers;
        }
                                
    /** 
        Unregisters a receiver with the Provider.  Returns false if the receiver was not registered.
    */
    public boolean removeReceiver(Receiver receiver)
        {
        if (receivers.contains(receiver)) return false;
        receivers.remove(receiver);
        return true;
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
    

    /** 
        Builds a provider with the given typical resource type.
    */
    public Provider(SimState state, Resource typicalProvided)
        {
        this.typicalProvided = typicalProvided.duplicate();
        this.typicalProvided.clear();
                
        if (typicalProvided instanceof Entity)
            {
            entities = new LinkedList<Entity>();
            }
        else
            {
            resource = (CountableResource) (typicalProvided.duplicate());
            resource.setAmount(0.0);
            }
        this.state = state;
        setName("Provider");
        }
        
    //// SHUFFLING PROCEDURE
    //// You'd think that shuffling would be easy to implement but it's not.
    //// We want to avoid an O(n) shuffle just to (most likely) select the
    //// very first receiver.  So this code attemps to do shuffling on the
    //// fly as necessary.
        
    int currentShuffledReceiver;
    /** 
        Resets the receiver shuffling
    */
    void shuffleReceivers() 
        {
        currentShuffledReceiver = 0;
        }
                
    /** 
        Returns the next receiver in the shuffling
    */
    Receiver nextShuffledReceiver()
        {
        int size = receivers.size();
        if (currentShuffledReceiver == size) 
            return null;
        int pos = state.random.nextInt(size - currentShuffledReceiver) + currentShuffledReceiver;

        Receiver ret = receivers.get(pos);
        receivers.set(pos, receivers.get(currentShuffledReceiver));
        receivers.set(currentShuffledReceiver, ret);
                
        currentShuffledReceiver++;
        return ret;
        }
       
    
    /** 
        Offers the given entity to the given receiver, returning true if it was
        accepted.  You probably should not override this method; instead you probably
        want to override offerReceiver(Receiver, double) 
    */
        
    protected boolean offerReceiver(Receiver receiver, Entity entity)
        {
        lastOfferTime = state.schedule.getTime();
        boolean result = receiver.accept(this, entity, 0, 1);
        if (result)
            {
            updateLastAcceptedOffers(entity, receiver);
            }
        return result;
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
       
    // only warn about problems with the distribution a single time
    boolean offerDistributionWarned = false; 
    boolean offerSelectWarned = false;
    
    boolean offersAllEntities = false;

	/** Returns whether the Provider will, duing offerReceivers(...), attempt to offer every single entity 
		that it has available, until offers start to be refused by downstream receivers.  By fault this is 
		FALSE: the Provider offers only one Entity. This only matters if the Provider provides entities.  
		This capability is largely useful for Delays and SimpleDelays rather than other kinds of Providers.  */
	public boolean getOffersAllEntities() { return offersAllEntities; }

	/** Sets whether the Provider will, duing offerReceivers(...), attempt to offer every single entity 
		that it has available, until offers start to be refused by downstream receivers.  By fault this is 
		FALSE: the Provider offers only one Entity. This only matters if the Provider provides entities.  
		This capability is largely useful for Delays and SimpleDelays rather than other kinds of Providers.  */
	public void setOffersAllEntities(boolean val) { offersAllEntities = val; }
    
    /** Simply calls offerReceivers(receivers). */
    protected boolean offerReceivers()
        {
        return offerReceivers(receivers);
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
                        
        switch(offerPolicy)
            {            
            case OFFER_POLICY_FORWARD:
                {
                for(Receiver r : receivers)
                    {
                    result = result || offerReceiver(r, Double.POSITIVE_INFINITY);
                    if (result && getOffersTakeItOrLeaveIt()) break;
                    }
                }
            break;
            case OFFER_POLICY_BACKWARD:
                {
                for(int i = receivers.size() - 1; i >= 0; i--)
                    {
                    Receiver r = receivers.get(i);
                    result = result || offerReceiver(r, Double.POSITIVE_INFINITY);
                    if (result && getOffersTakeItOrLeaveIt()) break;
                    }
                }
            break;
            case OFFER_POLICY_ROUND_ROBIN:
                {
                if (receivers.size() == 0) 
                    break;
                                        
                if (roundRobinPosition >= receivers.size())
                    roundRobinPosition = 0;
                                        
                int oldRoundRobinPosition = roundRobinPosition;
                while(true)
                    {
                    Receiver r = receivers.get(roundRobinPosition);
                    result = result || offerReceiver(r, Double.POSITIVE_INFINITY);
                    if (result && getOffersTakeItOrLeaveIt()) 
                        {
                        roundRobinPosition++;           // we have to advance the round robin pointer anyway
                        break;
                        }
                    else
                        {
                        roundRobinPosition++;
                        }
                                                                                                
                    if (roundRobinPosition >= receivers.size())
                        roundRobinPosition = 0;
                    if (roundRobinPosition == oldRoundRobinPosition) break;         // all done!
                    }
                }
            break;
            case OFFER_POLICY_SHUFFLE:
                {
                shuffleReceivers();
                while(true)
                    {
                    Receiver r = nextShuffledReceiver();
                    if (r == null) break;
                    result = result || offerReceiver(r, Double.POSITIVE_INFINITY);
                    if (result && getOffersTakeItOrLeaveIt()) break;
                    }
                }
            break;
            case OFFER_POLICY_RANDOM:
                {
                int size = receivers.size();
                if (size == 0) 
                    break;
                
                if (offerDistribution == null)  // select uniformly
                    {
                    result = offerReceiver(receivers.get(state.random.nextInt(size)), Double.POSITIVE_INFINITY);
                    }
                else
                    {
                    int val = offerDistribution.nextInt();
                    if (val < 0 || val >= size )
                        {
                        if (!offerDistributionWarned)
                            {
                            new RuntimeException("Warning: Offer distribution for Provider " + this + " returned a value outside the Receiver range: " + val).printStackTrace();
                            offerDistributionWarned = true;
                            }
                        result = false;
                        }
                    else
                        {
                        result = offerReceiver(receivers.get(val), Double.POSITIVE_INFINITY);
                        }
                    }
                }
            break;
            case OFFER_POLICY_SELECT:
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
                    Receiver receiver = selectReceiver(receivers);
                    if (receiver == null) break;
                    result = offerReceiver(receiver, Double.POSITIVE_INFINITY);
                    if (result)
                    	{
                    	selectedOfferAccepted(receiver, oldResource, resource);
                    	}
                    }
                }
            break;
            }
            
        offering = false;
        return result;
        }
    
    // This is here so that If can override it
    Receiver selectReceiver(ArrayList<Receiver> receivers)
    	{
    	return selectReceiver(receivers, entities == null ? resource : entities.getFirst());
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
       Asks the Provider to make a unilateral offer to the given Receiver.  This can be used to implement
       a simple pull. The Receiver does not need to be registered with the Provider.
       Returns true if the offer was accepted; though since the Receiver itself likely made this call, 
       it's unlikely that this would ever return anything other than TRUE in a typical simulation.
    */
    public boolean provide(Receiver receiver) 
        {
        return provide(receiver, Double.POSITIVE_INFINITY);
        }

    /**
       Asks the Provider to make a unilateral offer of up to the given amount to the given Receiver.  
       If the typical provided resource is an ENTITY, then atMost is ignored. This can be used to implement
       a simple pull. The Receiver does not need to be registered with the Provider.
       Returns true if the offer was accepted; though since the Receiver itself likely made this call, 
       it's unlikely that this would ever return anything other than TRUE in a typical simulation.
       
       <p>atMost must be a positive non-zero, non-NAN number.
    */
    public boolean provide(Receiver receiver, double atMost) 
        {
        if (!isPositiveNonNaN(atMost))
            throwInvalidNumberException(atMost);
        return offerReceiver(receiver, atMost);
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

    String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
//    public boolean hideName() { return true; }
    
	Object parent;
    public Object getParent() { return parent; }
    public void setParent(Object parent) { this.parent = parent; }    
    
    public void reset(SimState state) 
        {
        clear();
        clearLastAcceptedOffers(Schedule.BEFORE_SIMULATION);
        lastOfferTime = Schedule.BEFORE_SIMULATION;
        totalAcceptedOfferResource = 0;
        }
        
    boolean makesOffers = true;
    public void setMakesOffers(boolean value) { makesOffers = value; }
    public boolean getMakesOffers() { return makesOffers; }

    public boolean hideDataBars() { return true; }
    public double[] getDataBars() 
        {
        return new double[0];
        }
        
    public boolean hideDataValues() { return true; }
    public String[] getDataValues() 
        {
        return new String[0];
        }
        
    public boolean hideDataLabels() { return true; }
    public String[] getDataLabels()
        {
        return new String[0];
        }

    /** Does nothing. */
    public void step(SimState state)
        {
        // do nothing by default
        }
    }
