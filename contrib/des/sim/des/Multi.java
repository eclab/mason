package sim.des;

import sim.engine.*;
import sim.util.*;
import java.util.*;

/**
	MULTI is a general Steppable object which is meant to enable objects which Provide and Receive multiple channels and multiple resources.
	For example, if you want to build a single object which takes Seats, Frames, and Tires and produces Bicycles and Trash, you could do it
	with a Multi.  To do this, Multi maintains an array of Receivers and a separate array of Providers.  Outside providers can offer
	resources to the Multi by offering to one of its Receivers as appropriate: and in turn, the Multi can offer to outside Receivers by 
	having one of its Providers make the offer.  Each of the Multi's Receivers is specified by a receiver port, which is just its position
	in the array.  Similarly each of the Multi's Providers is specified by a provider port.  The Receivers and Providers are created 
	during initialization, where you pass in the typical Resource for each one of them.
	
**/

public abstract class Multi implements Named, Resettable
	{
    private static final long serialVersionUID = 1;

	protected SimState state;

	// Collections of receivers and providers that may be connected to outside receivers and providers 	
	MultiReceiver[] multiReceivers;
	MultiProvider[] multiProviders;
	
	/** Called when a Multi receiver receives an offer.  The receiver in question is specified by its receiverPort. 
		Override this to process offers, in the same way that a Receiver processes an offer via its accept(...) method.  
		By default this method returns false.  */
	protected boolean accept(int receiverPort, Provider provider, Resource resource, double atLeast, double atMost)
		{
		return false;
		}

	/** Called when a Multi provider receives a request to make an offer.  The provider in question is specified by its providerPort. 
		Override this to make an offer if you see fit by calling offer(...).  By default this method returns false.  */
	protected boolean provide(int providerPort, Receiver receiver, Resource resource, double atMost)
		{
		return false;
		}
	
	/** Instructs a Multi receiver to ask some provider to make an offer by calling its provide(..., atMost) method.
		The receiver in question is specified by its receiverPort. */
	protected boolean request(int receiverPort, Provider provider, double atMost)
		{
		return provider.provide(multiReceivers[receiverPort], atMost);
		}

	/** Instructs a Multi receiver to ask some provider to make an offer by calling its provide(...) method.
		The receiver in question is specified by its receiverPort. */
	protected boolean request(int receiverPort, Provider provider)
		{
		return provider.provide(multiReceivers[receiverPort]);
		}
	
	/** Instructs a Multi provider to ask offer to make an offer by calling offerReceivers(...) method, and then
		offer the resource as specified. */
	protected boolean offerReceivers(int providerPort, Resource resource, double atLeast, double atMost)
		{
		return multiProviders[providerPort].offer(resource, atLeast, atMost);
		}
	
	/** Builds a Multi with a set of Receivers and a set of Providers, each with the following typical resources. */
	public Multi(SimState state, Resource[] receiverResources, Resource[] providerResources)
		{
		multiReceivers = new MultiReceiver[receiverResources.length];
		for(int i = 0; i < receiverResources.length; i++)
			{
			multiReceivers[i] = new MultiReceiver(state, receiverResources[i], i);
			}
			
		multiProviders = new MultiProvider[providerResources.length];
		for(int i = 0; i < providerResources.length; i++)
			{
			multiProviders[i] = new MultiProvider(state, providerResources[i], i);
			}
        setName("Multi");
		}
		
	public void reset(SimState state)
		{
		// For the time being this does nothing because there's nothing to reset
		
		//for(int i = 0; i < multiReceivers.length; i++)
		//	{
		//	multiReceivers[i].reset(state);
		//	}

		//for(int i = 0; i < multiProviders.length; i++)
		//	{
		//	multiProviders[i].reset(state);
		//	}
		}
		
	public void step(SimState state)
		{
		// does nothing by default
		}
		
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
		
		
	/** The subclass of Provider used internally by Multi.  This is largely a stub which connects methods to 
		Multi's internal provide() and offerReceivers() methods. */
	public class MultiProvider extends Provider
		{
		int providerPort;
		double _atLeast = 0;
		double _atMost = 0;
		
	// Called by Multi.offer() to make an offer via offerReceivers();
		boolean offer(Resource amount, double atLeast, double atMost)
			{
			if (!typical.isSameType(amount))
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
	    protected boolean offerReceiver(Receiver receiver, double atMost)
	    	{
	    	if (_atLeast > atMost) return false;	// can't even make an offer
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

	    protected boolean offerReceiver(Receiver receiver, Entity entity)
    		{
//			return receiver.accept(this, entity, 0, 0);
			lastOfferTime = state.schedule.getTime();
			boolean result = receiver.accept(this, entity, 0, 0);
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
			return Multi.this.provide(providerPort, receiver, typical, atMost);
			}

		public MultiProvider(SimState state, Resource typical, int providerPort)
			{
			super(state, typical);
			this.providerPort = providerPort;
			}
			
		public String toString()
			{
			return "MultiProvider@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : getName()) + ", " + typical.getName() + ")";
			}               

		public void step(SimState state)
			{
			// do nothing
			}
		} 

	/** The subclass of Receiver used internally by Multi.  This is largely a stub which connects methods to 
		Multi's internal accept() and (in a roundabout fashion) request() methods. */
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
			if (!typical.isSameType(resource)) throwUnequalTypeException(resource);
		
			if (!(atLeast >= 0 && atMost >= atLeast))
				throwInvalidAtLeastAtMost(atLeast, atMost);

			return Multi.this.accept(receiverPort, provider, resource, atLeast, atMost);
    		}

		public String toString()
			{
			return "MultiReceiver@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : getName()) + ", " + typical.getName() + ")";
			}               

		public void step(SimState state)
			{
			// do nothing
			}
		} 
	}