/** 
	A blocking source of resources.  You can subclass this to provide resources 
	in your own fashion if you like, by overriding the update() and computeAvailable() methods.
	Sources have a maximum CAPACITY, which by default is infinite.
*/


import sim.engine.*;
import java.util.*;

public class Source extends BlockingProvider
	{
	void throwNonIntegerAmountException(double atLeast, double atMost)
		{
		throw new RuntimeException("Source provides countable Resources, but amounts requested are not integers. atLeast was: " + atLeast + " and atMost was: " + atMost);
		}

	void throwInvalidNumberException(double atLeast, double atMost)
		{
		throw new RuntimeException("Amounts may not be negative or NaN.  atLeast was: " + atLeast + " and atMost was: " + atMost);
		}

	void throwInvalidNumberException(double capacity)
		{
		throw new RuntimeException("Capacities may not be negative or NaN.  capacity was: " + capacity);
		}


	public Source(SimState state, Resource typical)
		{
		super(state, typical);
		}
		
	double capacity = Double.POSITIVE_INFINITY;
	
	/** Returns the maximum available resources that may be built up. */
	public double getCapacity() { return capacity; }
	/** Set the maximum available resources that may be built up. */
	public void setCapacity(double d) 
		{ 
		if (!Resource.isPositiveNonNaN(d))
			throwInvalidNumberException(d); 
		capacity = d; 
		}

	/** Provides a resource. */
	public Resource provide(double atLeast, double atMost)
		{
		if (!Resource.isPositiveNonNaN(atLeast) || !Resource.isPositiveNonNaN(atMost)) 
			throwInvalidNumberException(atLeast, atMost);
		if (atLeast > atMost) throwInvalidProvisionException();
		if (!blocklist.isEmpty()) return null;		// someone is ahead of us in the queue
		
		double totalAmount = resource.getAmount();
		if (totalAmount < atLeast) return null;
		double avail = (atMost > totalAmount ? totalAmount : atMost);
		
		if (typical.isCountable())
			{
			if (!Resource.isInteger(atLeast) || !Resource.isInteger(atMost)) 
				throwNonIntegerAmountException(atLeast, atMost);
			return new Resource(typical, avail);
			}
		else
			{
			return new UncountableResource((UncountableResource)typical, avail);
			}
		}

	protected double computeAvailable()
		{
		return resource.getAmount();
		}
		
	/** Override this method to add new resources to the *resource* variable.
		This method is called once every time this Source is stepped. 
		The default version simply adds 1.0 to the variable every step. 
		Keep in mind the setting of capacity.  */
	public void update()
		{
		if (resource.getAmount() <= capacity - 1)
			resource.increment();
		}
		
	public void step(SimState state)
		{
		update();
		super.step(state);
		}

	}
	