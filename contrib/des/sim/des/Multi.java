package sim.des;

import sim.engine.*;
import sim.util.*;
import java.util.*;
import sim.des.portrayal.*;
import sim.portrayal.simple.*;
import sim.portrayal.*;

/**
   MULTI is a general Steppable object which is meant to enable objects which Provide and Receive multiple channels and multiple resources.
   For example, if you want to build a single object which takes Seats, Frames, and Tires and produces Bicycles and Trash, you could do it
   with a Multi.  To do this, Multi maintains an array of Receivers and a separate array of Providers.  Outside providers can offer
   resources to the Multi by offering to one of its Receivers as appropriate: and in turn, the Multi can offer to outside Receivers by 
   having one of its Providers make the offer.  Each of the Multi's Receivers is specified by a receiver port, which is just its position
   in the array.  Similarly each of the Multi's Providers is specified by a provider port.  The Receivers and Providers are created 
   during initialization, where you pass in the typical Resource for each one of them.
**/

public abstract class Multi extends DESPortrayal implements Parented
    {
    private static final long serialVersionUID = 1;

    protected SimState state;
    
    public SimState getState()
    	{
    	return state;
    	}

    // Collections of receivers and providers that may be connected to outside receivers and providers      
    MultiReceiver[] multiReceivers;
    MultiProvider[] multiProviders;
        
    // This is a mapping of types to Receivers
    HashMap<Integer, MultiReceiver> mappedReceivers = new HashMap<>();
    // This is a mapping of types to Providers
    HashMap<Integer, MultiProvider> mappedProviders = new HashMap<>();

    /** Called when a Multi receiver receives an offer.  The receiver in question is specified by its receiverPort. 
        Override this to process offers, in the same way that a Receiver processes an offer via its accept(...) method.  
        By default this method returns false.  */
    protected boolean accept(int receiverPort, Provider provider, Resource resource, double atLeast, double atMost)
        {
        return false;
        }

    /** Called when a Multi provider receives a request to make an offer.  The provider in question is specified by its providerPort. 
        Override this to make an offer if you see fit by calling provide(...).  By default this method returns false.  */
    protected boolean provide(int providerPort, Receiver receiver, Resource resource, double atMost)
        {
        return false;
        }
        
    /** Instructs a Multi receiver to ask some provider to make an offer by calling its provide(..., atMost) method.
        The receiver in question is specified by its receiverPort. */
    protected boolean requestProvide(int receiverPort, Provider provider, double atMost)
        {
        return provider.provide(multiReceivers[receiverPort], atMost);
        }

    /** Instructs a Multi receiver to ask some provider to make an offer by calling its provide(...) method.
        The receiver in question is specified by its receiverPort. */
    protected boolean requestProvide(int receiverPort, Provider provider)
        {
        return provider.provide(multiReceivers[receiverPort]);
        }
        
    /** Instructs a Multi provider to make an offer by calling offerReceivers(...) method, and then
        offer the resource as specified. */
    protected boolean offerReceivers(int providerPort, Resource resource, double atLeast, double atMost)
        {
        return multiProviders[providerPort].provide(resource, atLeast, atMost);
        }

    /** Instructs a Multi to offer a transaction to a Middleman, notionally from the Multi's provider and receiver ports,
    	though they really won't come into it. If the transaction is agreed to, your provided resource will be accordingly 
    	modified (reduced) and the requested resource will have been provided.  Otherwise null will be returned.
		
		<p>The transaction is offering atLeast and atMost a certain amount of provided resouce in exchange for
    	(from you) a requested resource.  atLeastRequested is the amount of requested resource to be provided
    	in exchange for the *least* amount of provided resource.  If you decide to take some X provided resource
    	where X is between atLeast and atMost, then the resource amount you provide in return is X * atLeastRequested / atMost.
    	For requested CountableResources, I suggest that the amount returned in response to a request would best be
    	(int)(X * atLeastRequested / atMost), but you can do as your model deems appropriate.
    	
    	<p>For Entities, only a single Entity can be provided.  If an Entity is being provided, then atLeast = atMost = 1.
    	
    	<p>For Entities, only a single Entity can be requested.  If an Entity is being requested, atLeastRequested = 1
    	and exactly one Entity should be returned regardless of its value, and atLeast = atMost. 
    	
    	<p>Don't override this method.  Instead, override performTransaction().
	    */
    protected Resource offerTransaction(int providerPort, int receiverPort, Middleman middleman, Resource provided, double atLeast, double atMost, Resource requestedType, double atLeastRequested)
        {
        return middleman.transact(multiProviders[providerPort], multiReceivers[receiverPort], provided, atLeast, atMost, requestedType, atLeastRequested);
        }
        
    boolean offering;
    /** Returns true if the Provider is currently making an offer to a receiver (this is meant to allow you
        to check for offer cycles. */
    protected boolean isOffering() { return offering; }

    /** Throws an exception indicating that an offer cycle was detected. */
    protected void throwCyclicOffers()
        {
        throw new RuntimeException("Zero-time cycle found among offers including this one." );
        }
        
    /** Builds a Middleman from the given provider and receiver ports solely for the purpose of performing a transaction.
    	This is thus a "Broker", a simplified Middleman which refuses offers and requests to make offers: it only responds
    	to requests to make transactions, that is, transact(...).  Note that if you call this method twice with the
    	same provider and receiver ports, you will receive different Middlement.
    	
    	<p>This Middleman will refuse a transaction if the Receiver associated with the underlying receiver port
    	is set to refuse offers. 
    	
    	<p>Also note that the Multi has a global check for zero-time transaction cycles going 
    	through it.  That is, you can't make a transaction through one provider/receiver port pair, and have it make
    	its way through a chain of zero-time transactions to wind up trying to transact through a second
    	provider/receiver port pair on the same Multi. 
    	*/
    public Middleman getBroker(final int providerPort, final int receiverPort)
    	{
    	return new Middleman(state, multiProviders[providerPort].getTypicalProvided())
    		{
    		public Resource getTypicalReceived() { return multiReceivers[receiverPort].getTypicalReceived(); }
    		public boolean accept(Provider provider, Resource resource, double atLeast, double atMost) { return false; }
    		public boolean provide(Receiver receiver, double atMost) { return false; }
    		
    		protected Resource performTransaction(Provider provider, Receiver receiver, Resource provided, 
    			double atLeast, double atMost, Resource requestedType, double atLeastRequested)
		    	{
		    	if (isOffering())
		    		{
		    		throwCyclicOffers();
		    		}
		    	if (multiReceivers[receiverPort].getRefusesOffers())
		    		{
		    		return null;
		    		}
		    	offering = true;
		    	try 
		    		{
		    		return Multi.this.performTransaction(providerPort, receiverPort, provider, receiver, 
		    			provided, atLeast, atMost, requestedType, atLeastRequested);
		    		}
		    	finally
		    		{
		    		offering = false;
		    		}
		    	}
    		};
    	}
        
    /** Received by the Multi when an external Provider and Receiver are asking for a transaction of one resource for another,
    	by building a Middleman to broker with a provider port and receiver port on the Multi, though the ports really won't
    	come into it.  
    	The Provider would provide a resource to the Multi and a Receiver would receive the transacted returned Resource.
    	Very commonly this Provider and Receiver are one and the same: they are a Middleman or perhaps a Multi. 
    	But this does not have to be the case.  	
		If the transaction is agreed to, you should modify the provided resource and return the requested resource.
		Otherwise, return null.  The default form simply returns null. 
		
		<p> By the time this method has been called, refuses-offers,
		cyclic, and type compatibility checks have already been performed, but you might still benefit from 
		knowing the requestedType, so it is provided: but you should not modify this resource nor return it.
				
		<p>The transaction is offering atLeast and atMost a certain amount of provided resouce in exchange for
    	(from you) a requested resource.  atLeastRequested is the amount of requested resource to be provided
    	in exchange for the *least* amount of provided resource.  If you decide to take some X provided resource
    	where X is between atLeast and atMost, then the resource amount you provide in return X * atLeastRequested / atMost.
    	For requested CountableResources, I suggest that the amount returned in response to a request would best be
    	(int)(X * atLeastRequested / atMost), but you can do as your model deems appropriate.
    	
    	<p>For Entities, only a single Entity can be provided.  If an Entity is being provided, then atLeast = atMost = 1.
    	
    	<p>For Entities, only a single Entity can be requested.  If an Entity is being requested, atLeastRequested = 1
    	and exactly one Entity should be returned regardless of its value. 
	    */
    	 
    protected Resource performTransaction(int myProviderPort, int myReceiverPort, Provider provider, Receiver receiver, 
    			Resource provided, double atLeast, double atMost, Resource requestedType, double atLeastRequested)
    	{
    	return null;
    	}

    /** Builds a Multi with a set of Receivers and a set of Providers, each with the following typical resources. */
    public Multi(SimState state, Resource[] receiverResources, Resource[] providerResources)
        {
        multiReceivers = new MultiReceiver[receiverResources.length];

        for(int i = 0; i < receiverResources.length; i++)
            {
            multiReceivers[i] = new MultiReceiver(state, receiverResources[i], i);
            multiReceivers[i].setParent(this);
			mappedReceivers.put(receiverResources[i].getType(), multiReceivers[i]);
            }
                        
        multiProviders = new MultiProvider[providerResources.length];
        for(int i = 0; i < providerResources.length; i++)
            {
            multiProviders[i] = new MultiProvider(state, providerResources[i], i);
            multiProviders[i].setParent(this);
			mappedProviders.put(providerResources[i].getType(), multiProviders[i]);
            }
        setName("Multi");
        }
         
    public void reset(SimState state)
        {
        // For the time being this does nothing because there's nothing to reset
                
        //for(int i = 0; i < multiReceivers.length; i++)
        //      {
        //      multiReceivers[i].reset(state);
        //      }

        //for(int i = 0; i < multiProviders.length; i++)
        //      {
        //      multiProviders[i].reset(state);
        //      }
        }
                
    public void step(SimState state)
        {
        // does nothing by default
        }
       
    public int getNumReceivers() { return multiReceivers.length; }
    public int getNumProviders() { return multiProviders.length; }
    
    public Receiver[] getReceivers() { return multiReceivers; }
    public Provider[] getProviders() { return multiProviders; }
             
    /** Returns the Receiver corresponding to the given receiver port.  You can customize it as you see fit.  */ 
    public Receiver getReceiver(int receiverPort)
        {
        return multiReceivers[receiverPort];
        }
                
    /** Returns the Provider corresponding to the given provider port.  You can customize it as you see fit.  */ 
    public Provider getProvider(int providerPort)
        {
        return multiProviders[providerPort];
        }
                
	/** Returns the Multi Receiver meant to receive the following kind of resource, or null if there isn't one.  Note that if
		this Resource was given multiple times in the constructor, only the last Receiver is returned. 
		It's possible, indeed reasonable for you to have multiple receivers for a given resource for some
		modeling task: in this case, if the resource appeared in slots 5 and 7 (say) of the receiverResources[]
		array passed into the constructor, then the two corresonding receivers would be at ports 5 and 7. */
    public Receiver getReceiver(Resource resource)
    	{
    	return mappedReceivers.get(resource);
    	}

	/** Returns the Multi Provider meant to receive the following kind of resource, or null if there isn't one.  Note that if
		this Resource was given multiple times in the constructor, only the last Provider is returned. 
		It's possible, indeed reasonable for you to have multiple providers for a given resource for some
		modeling task: in this case, if the resource appeared in slots 5 and 7 (say) of the providerResources[]
		array passed into the constructor, then the two corresonding Providers would be at ports 5 and 7. */
    public Provider getProvider(Resource resource)
    	{
    	return mappedProviders.get(resource);
    	}
    	
    /** The subclass of Provider used internally by Multi.  This is largely a stub which connects methods to 
        Multi's internal provide() and offerReceivers() methods. */
    public class MultiProvider extends Provider
        {
        int providerPort;
        double _atLeast = 0;
        double _atMost = 0;
                
        // Called by Multi.provide() to make an offer via offerReceivers();
        boolean provide(Resource amount, double atLeast, double atMost)
            {
            if (!getTypicalProvided().isSameType(amount))
                {
                throwUnequalTypeException(amount);
                }

            boolean val;
            if (entities != null)
                {
                _atLeast = atLeast;
                _atMost = atMost;
                CountableResource oldResource = resource;
                resource = (CountableResource)amount;
                val = offerReceivers();
                                
                // reset
                resource = oldResource;
                _atLeast = 0;
                _atMost = 0;
                }
            else
                {
                entities.clear();
                entities.add((Entity)amount);
                val = offerReceivers();
                entities.clear();
                }
            return val;
            }
                
        // Guarantees that _atLeast is respected when calling accept
         boolean offerReceiver(Receiver receiver, double atMost)
            {
            if (_atLeast > atMost) return false;    // can't even make an offer
            else 
                {
                //return receiver.accept(this, resource, Math.min(_atLeast, atMost), Math.min(_atMost, atMost));
                double originalAmount = resource.getAmount();
                lastOfferTime = state.schedule.getTime();
                boolean result = receiver.accept(this, resource, Math.min(_atLeast, atMost), Math.min(_atMost, atMost));
                if (result)
                    {
                    CountableResource removed = (CountableResource)(resource.duplicate());
                    removed.setAmount(originalAmount - resource.getAmount());
                    updateLastAcceptedOffers(removed, receiver);
                    }
                return result;
                }
            }

         boolean offerReceiver(Receiver receiver, Entity entity)
            {
//                      return receiver.accept(this, entity, 0, 0);
            lastOfferTime = state.schedule.getTime();
            boolean result = receiver.accept(this, entity, 1, 1);
            if (result)
                {
                updateLastAcceptedOffers(entity, receiver);
                }
            return result;
            }
                
        /** Routes to Multi.provide(...) */
        public boolean provide(Receiver receiver, double atMost)
            {
            if (!isPositiveNonNaN(atMost))
                throwInvalidNumberException(atMost);
            return Multi.this.provide(providerPort, receiver, getTypicalProvided(), atMost);
            }

        public MultiProvider(SimState state, Resource typical, int providerPort)
            {
            super(state, typical);
            this.providerPort = providerPort;
            }
                        
        public String toString()
            {
       		return "MultiProvider@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : (getName() + ": ")) + getTypicalProvided().getName() + ")";
            }               
        } 

    /** The subclass of Receiver used internally by Multi.  This is largely a stub which connects methods to 
        Multi's internal accept() and (in a roundabout fashion) provide() methods. */
    public class MultiReceiver extends Sink
        {
        int receiverPort;
                
        public MultiReceiver(SimState state, Resource typical, int receiverPort)
            {
            super(state, typical);
            this.receiverPort = receiverPort;
            }
                
        public boolean accept(Provider provider, Resource resource, double atLeast, double atMost)
            {
            if (getRefusesOffers()) { return false; }
            if (!getTypicalReceived().isSameType(resource)) throwUnequalTypeException(resource);
                
        	if (!(atLeast >= 0 && atMost >= atLeast && atMost > 0 && atMost <= resource.getAmount()))
            	throwInvalidAtLeastAtMost(atLeast, atMost, resource);

            return Multi.this.accept(receiverPort, provider, resource, atLeast, atMost);
            }

        public String toString()
            {
        	return "MultiReceiver@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : (getName() + ": ")) + getTypicalReceived().getName() + ")";
            }               
        } 

    String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean hideName() { return true; }
	Object parent;
    public Object getParent() { return parent; }
    public void setParent(Object parent) { this.parent = parent; }    

    public SimplePortrayal2D buildDefaultPortrayal(double scale)
      {
      return new ShapePortrayal2D(ShapePortrayal2D.SHAPE_PILL, 
      getFillPaint(), getStrokePaint(), getStrokeWidth(), scale);
      }

	public String toString()
		{
		return "Multi@" + System.identityHashCode(this);
		}               
    }
