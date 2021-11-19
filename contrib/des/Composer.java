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
        public Resource resource;
        // The maximum amount we fill up to before we become full
        public double maximum;
        // Have we accepted at least one of these resources so far?
        public boolean loaded;
                
        public Node(Resource resource, double maximum)
            {
            this.resource = resource;
            this.maximum = maximum;
            this.loaded = false;
            }
        }
    
    // This is a mapping of types to total-nodes
    HashMap<Integer, Node> mappedTotals;
    // This is the same set of total-nodes organized as an array for fast scanning
    Node[] totals;
    // Do we send when we're completely full up or when we have received at least 1 of each item?
    boolean sendsOnlyWhenFull = true;
    
    public Composer(SimState state, Entity typical, Resource[] maximums)
        {
        super(state, typical);
    	mappedTotals = new HashMap<Integer, Node>();
    	totals = new Node[maximums.length];
    	
        for(int i = 0; i < maximums.length; i++)
            {
            if (mappedTotals.get(maximums[i].getType()) != null)  // uh oh
            	throwDuplicateType(maximums[i]);
            else
            	{
            	// The provided maximums[] array contains resources which have
            	// maximum amounts in them.  We need to set these aside in the
            	// separate maximum variable, then make a copy of these elements
            	// and clear them out so their values are 0 so we can start using
            	// them.  Note that the maximum for an Entity should be set to 1.0,
            	// since this is the amount of an Entity.  
            	
				double maximum = maximums[i].getAmount();
				Resource res = maximums[i].duplicate();
				res.clear();		// so it's 0.0 if a CountableResource
				Node node = new Node(res, maximum);
            	mappedTotals.put(maximums[i].getType(), node);
            	totals[i] = node;
	            }
            }
        }
        
    public boolean getSendsOnlyWhenFull() { return sendsOnlyWhenFull; }
    public void setSendsOnlyWhenFull(boolean val) { sendsOnlyWhenFull = val; }
                                
    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
        if (isOffering()) throwCyclicOffers();  // cycle
        
        Node total = mappedTotals.get(amount.getType());
        if (total == null) throwNotComposableResource(amount);
        
        if (total.resource instanceof Entity)
        	{
        	// We just check to see if we got one yet
        	if (total.loaded)
        		return false;  // we've already got one
        	else total.loaded = true;
        	return true;
        	}
        else
        	{
        	// We can load this multiple times to fill it up
        	CountableResource res = (CountableResource)(total.resource);
        	if (total.maximum - res.getAmount() < atLeast)	// cannot accept
        		return false;
        	else
				{
				double amt = Math.min(total.maximum - res.getAmount(), atMost);
				res.increase(amt);
				((CountableResource)amount).decrease(amt);
				total.loaded = true;
				return true;
				}
        	}
        }
        
    public void step(SimState state)
        {
        // are we done yet?

        if (sendsOnlyWhenFull)
        	{
        	for(int i = 0; i < totals.length; i++)
        		{
				if (!totals[i].loaded)
					return;			// this could happen for Entities 
        		if (totals[i].resource.getAmount() < totals[i].maximum) 
        			return;
        		}
        	}
        else
        	{
        	for(int i = 0; i < totals.length; i++)
        		{
        		if (!totals[i].loaded) 
        			return;
        		}
        	}

		// do a load
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
			totals[i].loaded = false;
			totals[i].resource.clear();
			}
        }
        
    public String getName()
        {
        return "Composer(" + typical + ")";
        }

    }
