/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import java.util.*;
import sim.util.distribution.*;

/**
   A provider of resources. Providers also have a TYPICAL resource, 
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

public abstract class Provider implements Named, Resettable
    {
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

    /** Throws an exception indicating that the given resource does not match the Provider's typical resource. */
    protected void throwUnequalTypeException(Resource res)
        {
        throw new RuntimeException("Expected resource type " + this.typical.getName() + "(" + this.typical.getType() + ")" +
            " but got resource type " + res.getName() + "(" + res.getType() + ")" );
        }

	/** Throws an exception indicating that atLeast and atMost are out of legal bounds. */
    protected void throwInvalidAtLeastAtMost(double atLeast, double atMost)
        {
        throw new RuntimeException("Requested resource amounts are between " + atLeast + " and " + atMost + ", which is out of bounds.");
        }

    // receivers registered with the provider
    ArrayList<Receiver> receivers = new ArrayList<Receiver>();

    /** The typical kind of resource the Provider provides.  This should always be zero and not used except for type checking. */
    protected Resource typical;
    /** A resource pool available to subclasses.  null by default. */
    protected CountableResource resource;
    /** An entity pool available to subclasses.  null by default. */
    protected LinkedList<Entity> entities;
    /** The model. */
    protected SimState state;
        
        
    /** First in First Out Offer Order for entities. */
    public static final int OFFER_ORDER_FIFO = 0;
    /** Last in First Out Offer Order for entities. */
    public static final int OFFER_ORDER_LIFO = 1;
    public int offerOrder = OFFER_ORDER_FIFO;
    
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
        if (offerPolicy < OFFER_POLICY_FORWARD || offerPolicy > OFFER_POLICY_RANDOM)
            throw new IllegalArgumentException("Offer Policy " + offerPolicy + " out of bounds.");
        this.offerPolicy = offerPolicy; 
        roundRobinPosition = 0; 
        }
                
    /** Returns the receiver offer policy */
    public int getOfferPolicy() { return offerPolicy; }
         
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
        

    /** Returns the receiver offer policy to OFFER_POLICY_RANDOM, and
        sets the appropriate distribution for selecting a receiver.  If the distribution
        is set to null, receivers are selected randomly.
    */
    public AbstractDistribution getOfferRandomDistribution()
        {
        return offerDistribution;
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
        Returns the typical kind of resource the Provider provides.  This should always be zero and not used except for type checking.
    */
    public Resource getTypicalResource() { return typical; }
        
    /**  
         Returns whether receivers are offered take-it-or-leave-it offers.
         A take-it-or-leave-it offer requires the Receiver to accept all of the offered Resource,
         or else reject it all.
    */
    public boolean getOffersTakeItOrLeaveIt() { return offersTakeItOrLeaveIt; }
        
    /** 
        Sets whether receivers are offered take-it-or-leave-it offers.
        A take-it-or-leave-it offer requires the Receiver to accept all of the offered Resource,
        or else reject it all.
    */
    public void setOffersTakeItOrLeaveIt(boolean val) { offersTakeItOrLeaveIt = val; }

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
        
    /** 
        Builds a provider with the given typical resource type.
    */
    public Provider(SimState state, Resource typical)
        {
        this.typical = typical.duplicate();
        this.typical.clear();
                
        if (typical instanceof Entity)
            {
            entities = new LinkedList<Entity>();
            }
        else
            {
            resource = (CountableResource) (typical.duplicate());
            resource.setAmount(0.0);
            }
        this.state = state;
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
    void shuffle() 
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
        Makes an offer to the given receiver.
        Returns true if the offer was accepted.
        
        <p>If the resource in question is an ENTITY, then it is removed
        according to the current OFFER ORDER.  If the offer order is FIFO
        (default), then the entity is removed from the FRONT of the entities 
        linked list (normally entities are added to the END of the linked list
        via entities.add()).  If the offer order is LIFO, then the entity
        is removed from the END of the entities linked list.
    */
    protected boolean offerReceiver(Receiver receiver)
        {
        if (entities == null)
            {
            CountableResource cr = (CountableResource) resource;
            double amt = cr.getAmount();
            return receiver.accept(this, cr, getOffersTakeItOrLeaveIt() ? amt : 0, amt);
            }
        else if (offerOrder == OFFER_ORDER_FIFO)
            {
            Entity e = entities.getFirst();
            boolean result = receiver.accept(this, e, 0, 0);
            if (result) entities.removeFirst();
            return result;
            }
         else // if (offerOrder == OFFER_ORDER_LIFO)
            {
            Entity e = entities.getLast();
            boolean result = receiver.accept(this, e, 0, 0);
            if (result) entities.removeLast();
            return result;
            }
       }
    
    // only warn about problems with the distribution a single time
    boolean warned = false; 
    
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
        offering = true;
        boolean result = false;
                        
        switch(offerPolicy)
            {            
            case OFFER_POLICY_FORWARD:
                {
                for(Receiver r : receivers)
                    {
                    result = result || offerReceiver(r);
                    if (result && getOffersTakeItOrLeaveIt()) break;
                    }
                }
            break;
            case OFFER_POLICY_BACKWARD:
                {
                for(int i = receivers.size() - 1; i >= 0; i--)
                    {
                    Receiver r = receivers.get(i);
                    result = result || offerReceiver(r);
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
                    result = result || offerReceiver(r);
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
                shuffle();
                while(true)
                    {
                    Receiver r = nextShuffledReceiver();
                    if (r == null) break;
                    result = result || offerReceiver(r);
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
                    result = offerReceiver(receivers.get(state.random.nextInt(size)));
                    }
                else
                    {
                    int val = offerDistribution.nextInt();
                    if (val < 0 || val >= size )
                        {
                        if (!warned)
                            {
                            new RuntimeException("Warning: Offer distribution for Provider " + this + " returned a value outside the Receiver range: " + val);
                            warned = true;
                            }
                        result = false;
                        }
                    else
                        {
                        result = offerReceiver(receivers.get(val));
                        }
                    }
                }
            break;
            }
            
        offering = false;
        return result;
        }
        
    /**
       Asks the Provider to make a unilateral offer to the given Receiver.  This can be used to implement
       a simple pull. The Receiver does not need to be registered with the Provider.
       Returns true if the offer was accepted; though since the Receiver itself likely made this call, 
       it's unlikely that this would ever return anything other than TRUE in a typical simulation.
    */
    public boolean provide(Receiver receiver)
        {
        return offerReceiver(receiver);
        }

    String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public void reset(SimState state) 
    	{
    	clear();
    	}
    }
