import sim.engine.*;
import java.util.*;

public class Decomposer extends Provider implements Receiver
    {
    HashMap<Integer, Receiver> output;
    
	protected void throwNotCompositeEntity(Entity res)
		{
		throw new RuntimeException("The provided entity " + res + " was not composite (its storage didn't consist of an array of Resources).");
		}

	public Decomposer(SimState state, Entity typical)
		{
		super(state, typical);
		output = new HashMap<Integer, Receiver>();
		}
		
	public boolean addReceiver(Receiver receiver, Resource res)
		{
		Integer type = res.getType();
		if (output.get(type) != null) return false;		// someone is already registered for this resource
		boolean result = super.addReceiver(receiver);
		
		if (result)
			{
			output.put(type, receiver);
			}
		return result;
		}

	// we don't use this facility
	public boolean addReceiver(Receiver receiver)
		{
		return false;
		}

	public boolean removeReceiver(Receiver receiver)
		{
		for(Integer type : output.keySet())
			{
			if (output.get(type) == receiver)
				{
				output.remove(type);
				break;
				}
			}
		return super.removeReceiver(receiver);
		}

		
    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {       
		if (!typical.isSameType(amount)) throwUnequalTypeException(amount);

		// unpack
		Entity entity = (Entity)amount;
		
		boolean accepted = false;
		if (entity.getStorage() == null)
			{
			throwNotCompositeEntity(entity);
			return false;						// not reachable, Java is stupid
			}
		else if (entity.getStorage() instanceof Resource[])
			{
			Resource[] res = entity.decompose();
			for(int i = 0; i < res.length; i++)
				{
				Receiver recv = output.get(res[i].getType());
				if (recv != null)
					{
					accepted = accepted || recv.accept(this, res[i], 0, res[i].getAmount());
					}
				}
			return accepted;
			}
		else 
			{
			throwNotCompositeEntity(entity);
			return false;						// not reachable, Java is stupid
			}
        }
        
    public String getName()
        {
		return "Unpacker(" + typical + ")";
		}

    }