import sim.engine.*;
import java.util.*;

public class Composer extends Provider implements Receiver
    {
	protected void throwNotPackableResource(Resource res)
		{
		throw new RuntimeException("Provided resource " + res + " is not among the ones listed as valid for packing by this Packer.");
		}

	ArrayList<Resource> resources;
	HashSet<Integer> incoming;
	boolean sendsOnlyWhenFull = true;
    
	public Composer(SimState state, Entity typical, Resource[] incomingTypes)
		{
		super(state, typical);
		resources = new ArrayList<Resource>();
		incoming = new HashSet<Integer>();
		for(int i = 0; i < incomingTypes.length; i++)
			{
			incoming.add(incomingTypes[i].getType());
			}
		}
	
	public boolean getSendsOnlyWhenFull() { return sendsOnlyWhenFull; }
	public void setSendsOnlyWhenFull(boolean val) { sendsOnlyWhenFull = val; }
				
    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {       
		if (!typical.isSameType(amount)) throwUnequalTypeException(amount);

		int type = amount.getType();
		if (incoming.contains(type))
			{
			for(int i = 0; i < resources.size(); i++)
				{
				if (resources.get(i).getType() == type)  // uh oh, already there
					return false;
				}
			if (amount instanceof Entity)
				resources.add(amount);
			else 
				resources.add(((CountableResource)amount).reduce(atMost));
			return true;
			}
		else
			{
			throwNotPackableResource(amount);
			return false;		// not reachable but Java is stupid
			}
        }
        
    public void step(SimState state)
    	{
    	if ((getSendsOnlyWhenFull() && incoming.size() == resources.size()) || 
    		(!getSendsOnlyWhenFull() && resources.size() > 0))
    		{
    		entities.clear();
			Entity entity = (Entity)(typical.duplicate());
			entity.compose(resources);
			entities.add(entity);
			resources.clear();
    		}
    	}
        
    public String getName()
        {
		return "Packer(" + typical + ")";
		}

    }