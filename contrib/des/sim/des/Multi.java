package sim.des;

import sim.engine.*;
import sim.util.*;
import java.util.*;

public abstract class Multi implements Named, Resettable
	{
    private static final long serialVersionUID = 1;

	protected SimState state;
	
	MultiReceiver[] multiReceivers;
	MultiProvider[] multiProviders;
	
	protected abstract boolean accept(int port, Provider provider, Resource resource, double atLeast, double atMost);

	protected boolean provide(int port, Receiver receiver, Resource resource, double atMost)
		{
		return false;
		}
	
	protected boolean offer(int port, Resource resource, double atLeast, double atMost)
		{
		return multiProviders[port].offer(resource, atLeast, atMost);
		}
	
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
		}
		
	public void step(SimState state)
		{
		// does nothing by default
		}
		
	public Receiver getReceiver(int port)
		{
		return multiReceivers[port];
		}
		
	public Provider getProvider(int port)
		{
		return multiProviders[port];
		}
		
	public class MultiProvider extends Provider
		{
		int port;
		Resource amount;
		double _atLeast;
		double _atMost;
		
		boolean offer(Resource amount, double atLeast, double atMost)
			{
			this.amount = amount;
			_atLeast = atLeast;
			_atMost = atMost;
			return offerReceivers();
			}
		
	    protected boolean offerReceiver(Receiver receiver, double atMost)
	    	{
        	return receiver.accept(this, amount, Math.min(_atLeast, atMost), Math.min(_atMost, atMost));
	    	}

	    protected boolean offerReceiver(Receiver receiver, Entity entity)
    		{
			return receiver.accept(this, entity, 0, 0);
			}
	    	
		public boolean provide(Receiver receiver, double atMost)
			{
			if (!isPositiveNonNaN(atMost))
				throwInvalidNumberException(atMost);
			return Multi.this.provide(port, receiver, typical, atMost);
			}

		public MultiProvider(SimState state, Resource typical, int port)
			{
			super(state, typical);
			this.port = port;
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

	public class MultiReceiver extends Sink
		{
		int port;
		
		public MultiReceiver(SimState state, Resource typical, int port)
			{
			super(state, typical);
			this.port = port;
			}
		
    	public boolean accept(Provider provider, Resource resource, double atLeast, double atMost)
    		{
			if (getRefusesOffers()) { return false; }
			if (!typical.isSameType(resource)) throwUnequalTypeException(resource);
		
			if (!(atLeast >= 0 && atMost >= atLeast))
				throwInvalidAtLeastAtMost(atLeast, atMost);

			return Multi.this.accept(port, provider, resource, atLeast, atMost);
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