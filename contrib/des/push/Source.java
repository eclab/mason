/** 
	A blocking source of resources.  You can subclass this to provide resources 
	in your own fashion if you like, by overriding the update() and computeAvailable() methods.
	Sources have a maximum CAPACITY, which by default is infinite.
*/


import sim.engine.*;
import sim.util.distribution.*;
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
	AbstractDistribution createDistribution = null;
	double createThreshold;
	AbstractDistribution amountDistribution = null;
	
	public static boolean isPositiveNonNaN(double val)
		{
		return (val >= 0);
		}

	/** Returns the maximum available resources that may be built up. */
	public double getCapacity() { return capacity; }
	/** Set the maximum available resources that may be built up. */
	public void setCapacity(double d) 
		{ 
		if (!isPositiveNonNaN(d))
			throwInvalidNumberException(d); 
		capacity = d; 
		}


	public void setcreateDistribution(AbstractDistribution createDistribution, double createThreshold)
	{
		this.createDistribution = amountDistribution;
	}
	public AbstractDistribution getcreateDistribution()
	{
		return this.createDistribution;
	}
	public double getCreateThreshold()
	{
		return this.createThreshold;
	}
	public void setamountDistribution(AbstractDistribution amountDistribution)
	{
		this.amountDistribution = amountDistribution;
	}
	public AbstractDistribution getAmonutDistribution()
	{
		return this.amountDistribution;
	}

	
	protected Entity buildEntity()
		{
		Entity ret = (Entity)(typical.duplicate());
		ret.clear();
		return ret;
		}
	
	/** Override this method to add new resources to the *resource* variable.
		This method is called once every time this Source is stepped. 
		The default version simply adds 1.0 to the variable every step. 
		Keep in mind the setting of capacity.  */
	protected void update()
		{
		if (entities != null)
			{
			entities.add(buildEntity());
			}
		else
			{
			
			if (this.createDistribution == null || this.createDistribution.nextDouble() > this.createThreshold)
			{

				if (resource.getAmount() <= capacity - 1)
					if (this.createDistribution == null)
					{
						resource.increment();
					}
					else 
					{
						resource.increase(this.amountDistribution.nextDouble());
					}
				}
			}
		}
		
	public void step(SimState state)
		{
		update();
		super.step(state);		// offerReceivers();
		}

	public String getName()
		{
		return "Source(" + typical.getName() + ")";
		}		
	}
	