import sim.engine.*;
import sim.util.*;
import java.util.*;

public class Pool
	{
	CountableResource resource;
	double maximum;
	
	public Pool(CountableResource resource, double maximum)
		{
		this.maximum = maximum;
		this.resource = resource;
		}

	public Pool(CountableResource resource)
		{
		this.maximum = Double.POSITIVE_INFINITY;
		this.resource = resource;
		}
	
	public Pool(int initialResourceAllocation)
		{
		this(new CountableResource("", initialResourceAllocation));
		}
	
	public Pool()
		{
		this(0);
		}
	
	public CountableResource getResource() { return resource; }
	public void setResource(CountableResource val) { resource = val; }
	
	public double getMaximum() { return maximum; }
	public void setMaximum(double val) { maximum = val; }

	public String getName()
		{
		return "Pool(" + resource + ", " + maximum + ")";
		}		
	}