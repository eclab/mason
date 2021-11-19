import sim.engine.*;
import java.util.*;

/*
UNLOCKS allow up to N resources to pass through before refusing any more
*/

public class Unlock extends Provider implements Receiver
	{
	Provider provider = null;		// this is temporary
	double offer = 0.0;
	
	public Unlock(Lock other)
		{
		super(other.state, other.typical);
		resource = other.resource;
		}
	
	public Resource provide(double atLeast, double atMost)
		{
		if (provider == null) return null;
		// I can only provide up to remainder
		double remainder = resource.getAmount();
		if (remainder < atLeast) return null;
		if (atMost > remainder) atMost = remainder;
		Resource result = provider.provide(atLeast, atMost);
		offer -= result.getAmount();		// FIXME:  better not go negative!
		resource.decrease(result.getAmount());		// FIXME:  better not go negative!
		return result;
		}

	public double available() { return computeAvailable(); } // bypass cache
	protected double computeAvailable() 
		{
		double amt = resource.getAmount();
		return (offer > amt ? amt : offer); 
	 	}
	
	public void consider(Provider provider, double amount)
		{
		this.provider = provider;
		offer = amount;
		offerReceivers();
		}

	// FIXME: should we allow super.step() to call offerReceivers()?
	}