/** 
	A blocking source of resources.  You can subclass this to provide resources 
	in your own fashion if you like, by overriding the update() and computeAvailable() methods.
	Sources have a maximum CAPACITY, which by default is infinite.
*/


import sim.engine.*;
import java.util.*;

public class Source extends Provider
	{
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
		
	/** Override this method to add new resources to the *resource* variable.
		This method is called once every time this Source is stepped. 
		The default version simply adds 1.0 to the variable every step. 
		Keep in mind the setting of capacity.  */
	protected void update()
		{
		if (resource.getAmount() <= capacity - 1)
			resource.increment();
		}
		
	public void step(SimState state)
		{
		update();
		super.step(state);
		}

	public String getName()
		{
		return "";
		}		
	}
	