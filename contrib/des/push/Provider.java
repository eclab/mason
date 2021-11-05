import sim.engine.*;
import java.util.*;
import sim.util.distribution.*;

/**
   A non-blocking provider of resources.  A provider can be attached
   to any number of Receivers.  If a provider can make offers, it will
   do so to all the receivers, either in the order in which they were
   registered with the Provider, or in random shuffled order. 
   Providers also have a TYPICAL resource, 
   which exists only to provide a resource type.  Providers also have
   a RESOURCE, initially zero, of the same type, which is used as a 
   pool for resources.  The Provider class does not use the resource
   variable, and only makes it available as a convenience for subclasses
   to use as they see fit.  
*/

public abstract class Provider implements Named
    {
    protected void throwUnequalTypeException(Resource res)
        {
        throw new RuntimeException("Expected resource type " + this.typical.getName() + "(" + this.typical.getType() + ")" +
            " but got resource type " + res.getName() + "(" + res.getType() + ")" );
        }

    protected void throwNANException(double threshold)
        {
        throw new RuntimeException("Threshold may not be NaN.  threshold was: " + threshold);
        }

    protected void throwCriterionException(int criterion)
        {
        throw new RuntimeException("Criterion is not valid: " + criterion);
        }

    /** The typical kind of resource the Provider provides.  This should always be zero and not used except for type checking. */
    protected Resource typical;
    /** A resource pool available to subclasses.  null by default. */
    protected CountableResource resource;
    /** An entity pool available to subclasses.  null by default. */
    protected LinkedList<Entity> entities;
    /** The model. */
    protected SimState state;
    protected ArrayList<Receiver> receivers;
    
    public static final int CRITERION_GREATER = 0;
    public static final int CRITERION_GREATER_OR_EQUAL = 1;
    public static final int CRITERION_LESS = 2;
    public static final int CRITERION_LESS_OR_EQUAL = 3;
        
    AbstractDistribution offerDistribution = null;
    double offerThreshold = 0;
    int offerCriterion = CRITERION_GREATER;
        
    /** Offer Policy: offers are made to the first receiver, then the second, and so on, until available resources or receivers are exhausted. */
    public static final int OFFER_POLICY_FORWARD = 0;
    /** Offer Policy: offers are made to the last receiver, then the second to last, and so on, until available resources or receivers are exhausted. */
    public static final int OFFER_POLICY_BACKWARD = 1;
    /** Offer Policy: offers are made to the least recent receiver, then next, and so on, until available resources or receivers are exhausted. */
    public static final int OFFER_POLICY_ROUND_ROBIN = 2;
    /** Offer Policy: offers are made to the receivers in a randomly shuffled order, until available resources or receivers are exhausted. */
    public static final int OFFER_POLICY_SHUFFLE = 3;
    /** Offer Policy: offers are made to only one random receiver, chosen uniformly. */
    public static final int OFFER_POLICY_ONE_RANDOM = 4;
    /** Offer Policy: offers are made randomly to the first receiver, and if not, then randomly to the next receiver, and if not then to the third receiver, and so on, using a distribution. */
    public static final int OFFER_POLICY_RANDOM_DISTRIBUTION = 5;
    int offerPolicy;
    int roundRobinPosition = 0;
    boolean offersTakeItOrLeaveIt;

    /** Sets the receiver offer policy */
    public void setOfferPolicy(int offerPolicy) 
        { 
        if (offerPolicy < OFFER_POLICY_FORWARD || offerPolicy > OFFER_POLICY_RANDOM_DISTRIBUTION)
            throw new IllegalArgumentException("Offer Policy " + offerPolicy + " out of bounds.");
        this.offerPolicy = offerPolicy; 
        roundRobinPosition = 0; 
        }
                
    /** Returns the receiver offer policy */
    public int getOfferPolicy() { return offerPolicy; }
         
	/** Sets the receiver offer policy to OFFER_POLICY_RANDOM_DISTRIBUTION, and
		sets the appropriate distribution, threshold, and criterion for meeting the threshold. 
		If the distribution is null, then the threshold will always be met. */
    public void setOfferRandom(AbstractDistribution distribution, double threshold, int criterion)
    	{
    	offerDistribution = distribution;
        if (threshold != threshold)
            throwNANException(threshold);
    	offerThreshold = threshold;
        if (criterion < CRITERION_GREATER || criterion > CRITERION_LESS_OR_EQUAL)
            throwCriterionException(criterion);
    	offerCriterion = criterion;
    	setOfferPolicy(OFFER_POLICY_RANDOM_DISTRIBUTION);
    	}
    	
	/** Returns the offer criterion for meeting the threshold, assuming
		that the offer policy is presently OFFER_POLICY_RANDOM_DISTRIBUTION. */
    public int getOfferRandomCriterion()
    	{
    	return offerCriterion;
    	}
    	
	/** Returns the offer threshold, assuming
		that the offer policy is presently OFFER_POLICY_RANDOM_DISTRIBUTION. */
    public double getOfferRandomThreshold()
    	{
    	return offerThreshold;
    	}
    
	/** Returns the offer distribution, assuming
		that the offer policy is presently OFFER_POLICY_RANDOM_DISTRIBUTION. */
    public AbstractDistribution getOfferRandomDistribution()
    	{
    	return offerDistribution;
    	}
    
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
    */
    public boolean getOffersTakeItOrLeaveIt() { return offersTakeItOrLeaveIt; }
        
    /** 
        Sets whether receivers are offered take-it-or-leave-it offers.
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
        
    protected boolean offerReceiver(Receiver receiver)
        {
        if (entities == null)
            {
            CountableResource cr = (CountableResource) resource;
            double amt = cr.getAmount();
            return receiver.accept(this, cr, getOffersTakeItOrLeaveIt() ? amt : 0, amt);
            }
        else
            {
            Entity e = entities.getLast();
            boolean result = receiver.accept(this, e, 0, 0);
            if (result) entities.remove();
            return result;
            }
        }
        
    /**
       Call this to inform all receivers
    */
    protected boolean offerReceivers()
        {
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
            case OFFER_POLICY_ONE_RANDOM:
                {
                if (receivers.size() == 0) 
                    break;
                                        
                Receiver r = receivers.get(state.random.nextInt(receivers.size()));
                result = offerReceiver(r);
                }
            break;
            case OFFER_POLICY_RANDOM_DISTRIBUTION:
                {
                if (receivers.size() == 0) 
                    break;
                                        
                Receiver last = receivers.get(receivers.size() - 1);
                for(Receiver r : receivers)
                    {
					if (offerDistribution == null || 
						((offerCriterion == CRITERION_GREATER) && (offerDistribution.nextDouble() > offerThreshold)) ||
						((offerCriterion == CRITERION_GREATER_OR_EQUAL) && (offerDistribution.nextDouble() >= offerThreshold)) ||
						((offerCriterion == CRITERION_LESS) && (offerDistribution.nextDouble() < offerThreshold)) ||
						((offerCriterion == CRITERION_LESS_OR_EQUAL) && (offerDistribution.nextDouble() <= offerThreshold)))
						{
                    	result = offerReceiver(r);
                    	break;
						}
                    }
                }
            break;
            }
        return result;
        }
        
    public CountableResource provide(CountableResource type, double amount)
        {
        if (resource != null && 
            typical.isSameType(type) &&
            resource.getAmount() >= amount)
            {
            return resource.reduce(amount);
            }
        else return null;
        }

    public Entity[] provide(Entity type, int amount)
        {
        if (amount >= 0 &&
            entities != null && 
            typical.isSameType(type) &&
            entities.size() >= amount)
            {
            Entity[] stuff = new Entity[amount];
            for(int i = 0; i < amount; i++)
                {
                stuff[i] = entities.remove();
                }
            return stuff;
            }
        else return null;
        }

    public Entity provide(Entity type)
        {
        if (entities != null && 
            typical.isSameType(type) &&
            entities.size() >= 1)
            {
            return entities.remove();
            }
        else return null;
        }
        
    public void step(SimState state)
        {
        offerReceivers();
        }
    }
