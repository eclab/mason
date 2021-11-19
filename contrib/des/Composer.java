import sim.engine.*;
import java.util.*;

public class Composer extends Provider implements Receiver
    {
    protected void throwDuplicateType(Resource res)
        {
        throw new RuntimeException("Resource " + res + " may not be provided multiple times with identical types.");
        }

    protected void throwNotComposableResource(Resource res)
        {
        throw new RuntimeException("Provided resource " + res + " is not among the ones listed as valid for composition by this Composer.");
        }
        
    class Node
        {
        // The resource we'll send out at the end of the day.  We fill up its amount.
         CountableResource resource = null;
         Entity[] entity = null;
        // The maximum amount we fill up
         double maximum;
        // The minimum amount we fill up
         double minimum;
         int entityCount = 0;
    	
    	 double spaceLeft()
    		{
    		if (entity != null)
    			{
    			return maximum - entityCount;
    			}
    		else
    			{
    			return maximum - resource.getAmount();
    			}
    		}
    		
    	 boolean getMeetsMinimum()
    		{
    		if (entity != null)
    			{
    			return minimum <= entityCount;
    			}
    		else
    			{
    			return minimum <= resource.getAmount();
    			}
    		}
    		
         Node(Resource resource, double minimum, double maximum)
            {
            this.maximum = maximum;
            this.minimum = minimum;
            if (resource instanceof CountableResource)
            	{
            	this.resource = (CountableResource)resource;
            	}
            else
            	{
            	entity = new Entity[(int)maximum];
            	this.entityCount = 0;
            	}
            }
        }
    
    // This is a mapping of types to total-nodes
    HashMap<Integer, Node> mappedTotals;
    
    // This is the same set of total-nodes organized as an array for faster scanning
    Node[] totals;
    
    public Composer(SimState state, Entity typical, Resource[] minimums, Resource[] maximums)
        {
        super(state, typical);
    	mappedTotals = new HashMap<Integer, Node>();
    	totals = new Node[maximums.length];
    	
        for(int i = 0; i < maximums.length; i++)
            {
            if (mappedTotals.get(maximums[i].getType()) != null)  // uh oh, already have one!
            	throwDuplicateType(maximums[i]);
            else
            	{
				Resource res = maximums[i].duplicate();
				res.clear();		// so it's 0.0 if a CountableResource
				Node node = new Node(res, maximums[i].getAmount(), minimums[i].getAmount());
            	mappedTotals.put(maximums[i].getType(), node);
            	totals[i] = node;
	            }
            }
        }

    public Composer(SimState state, Entity typical, double[] minimums, Resource[] maximums)
        {
        super(state, typical);
    	mappedTotals = new HashMap<Integer, Node>();
    	totals = new Node[maximums.length];
    	
        for(int i = 0; i < maximums.length; i++)
            {
            if (mappedTotals.get(maximums[i].getType()) != null)  // uh oh, already have one!
            	throwDuplicateType(maximums[i]);
            else
            	{
				Resource res = maximums[i].duplicate();
				res.clear();		// so it's 0.0 if a CountableResource
				Node node = new Node(res, maximums[i].getAmount(), minimums[i]);
            	mappedTotals.put(maximums[i].getType(), node);
            	totals[i] = node;
	            }
            }
        }
                                
    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
        if (isOffering()) throwCyclicOffers();  // cycle
        
        // Find the appropriate node
        Node total = mappedTotals.get(amount.getType());
        if (total == null) throwNotComposableResource(amount);
        
        if (total.entity != null)
        	{
        	if (total.maximum - total.entityCount > 0)
        		{
        		total.entity[total.entityCount++] = (Entity)amount;
        		return true;
        		}
        	else return false;
        	}
        else
        	{
        	CountableResource res = (CountableResource)(total.resource);
        	if (total.maximum - res.getAmount() < atLeast)	// cannot accept
        		return false;
        	else
				{
				double amt = Math.min(total.maximum - res.getAmount(), atMost);
				res.increase(amt);
				((CountableResource)amount).decrease(amt);
				return true;
				}
        	}
        }
        
    void deploy()
    	{
    	// have we met the minimum counts yet?
    	for(int i = 0; i < totals.length; i++)
    		{
    		if (!totals[i].getMeetsMinimum()) // crap, failed
    			return;
    		}
    		
		// do a load into entities
		entities.clear();
		Entity entity = (Entity)(typical.duplicate());
		Resource[] resources = new Resource[totals.length];
		for(int i = 0; i < totals.length; i++)
			resources[i] = totals[i].resource.duplicate();
		entity.compose(resources);
		entities.add(entity);
		
		// reset totals
		for(int i = 0; i < totals.length; i++)
			{
			totals[i].entityCount = 0;
			if (totals[i].resource != null)
				{
				totals[i].resource.clear();
				}
			}
    	}
        
    public void step(SimState state)
        {
        deploy();
        }
        
    public String getName()
        {
        return "Composer(" + typical + ")";
        }

    }
