import sim.engine.*;
import java.util.*;

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

	/** The typical kind of resource the Provider provides.  This should always be zero and not used except for type checking. */
	protected Resource typical;
	/** A resource pool available to subclasses. By default, step() offers this resource to all receivers. */
	protected Resource resource;
	/** The model. */
	protected SimState state;
	ArrayList<Receiver> receivers;
	boolean shufflesReceivers;
	boolean offersExactly;
	
	/** 
	Returns the typical kind of resource the Provider provides.  This should always be zero and not used except for type checking.
	*/
	public Resource getTypicalResource() { return typical; }
	
	/** 
	Returns whether receivers are shuffled prior to being given offers.
	*/
	public boolean getShufflesReceivers() { return shufflesReceivers; }
	
	/** 
	Sets whether receivers are shuffled prior to being given offers.
	*/
	public void setShufflesReceivers(boolean val) { shufflesReceivers = val; }

	/** 
	Returns whether receivers are offered take-it-or-leave-it offers.
	*/
	public boolean getOffersExactly() { return offersExactly; }
	
	/** 
	Sets whether receivers are offered take-it-or-leave-it offers.
	*/
	public void setOffersExactly(boolean val) { offersExactly = val; }

	/** 
	Registers a receiver with the Provider.  Returns false if the receiver was already registered.
	*/
	public boolean registerReceiver(Receiver receiver)
		{
		if (receivers.contains(receiver)) return false;
		receivers.add(receiver);
		return true;
		}
				
	/** 
	Unregisters a receiver with the Provider.  Returns false if the receiver was not registered.
	*/
	public boolean unregisterReceiver(Receiver receiver)
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
		return resource.getAmount();
		}
	
	public Provider(SimState state, Resource typical)
		{
		this.typical = typical.duplicate();
		this.typical.setAmount(0.0);
		resource = typical.duplicate();
		resource.setAmount(0.0);
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
	Call this to inform all receivers
	*/
	protected void offerReceivers(Resource amount)
		{
		double amt = amount.getAmount();
		if (shufflesReceivers)
			{
			shuffle();
			while(true)
				{
				Receiver r = nextShuffledReceiver();
				if (r == null) return;
				r.accept(this, amount, offersExactly ? amount.getAmount() : 0, amount.getAmount());
				if (amount.getAmount() == 0)
					return;
				}
			}
		else
			{
			for(Receiver r : receivers)
				{
				r.accept(this, amount, offersExactly ? amount.getAmount() : 0, amount.getAmount());
				if (amount.getAmount() == 0)
					return;
				}
			}
		}
		
	public void step(SimState state)
		{
		offerReceivers(resource);
		}
	}