import sim.engine.*;
import java.util.*;

/*
LOCKS allow up to N resources to pass through before refusing any more
*/

public class Lock extends Provider implements Receiver
	{
	Provider provider = null;		// this is temporary
	double capacity = Double.POSITIVE_INFINITY;
	double offer = 0.0;
	
	public Lock(SimState state, Resource typical)
		{
		super(state, typical);
		}
	
	/** WARNING: if you copy a lock to allow multiple locks to share the same
		resource, make certain that they all have the same capacity (we don't
		check for that right now) */
	public Lock(Lock other)
		{
		super(other.state, other.typical);
		capacity = other.capacity;
		resource = other.resource;
		}
	
	void throwInvalidNumberException(double capacity)
		{
		throw new RuntimeException("Capacities may not be negative or NaN.  capacity was: " + capacity);
		}

	/** Returns the maximum available resources that may be built up. */
	public double getCapacity() { return capacity; }
	/** Set the maximum available resources that may be built up. */
	public void setCapacity(double d) 
		{ 
		if (!Resource.isPositiveNonNaN(d))
			throwInvalidNumberException(d); 
		capacity = d; 
		}
	
	public Resource provide(double atLeast, double atMost)
		{
		if (provider == null) return null;
		// I can only provide up to remainder
		double remainder = capacity - resource.getAmount();
		if (remainder < atLeast) return null;
		if (atMost > remainder) atMost = remainder;
		Resource result = provider.provide(atLeast, atMost);
		offer -= result.getAmount();		// FIXME:  better not go negative!a
		resource.increase(result.getAmount());
		return result;
		}

	public double available() { return computeAvailable(); } // bypass cache
	protected double computeAvailable() { return offer; }
	
	public void consider(Provider provider, double amount)
		{
		this.provider = provider;
		offer = amount;
		offerReceivers();
		}
		
	// FIXME: should we allow super.step() to call offerReceivers()?
	}