/** 
	A multiplexing provider: when asked to provide a resource, it goes to each
	of its resources and asks them in turn, round-robin, until it has sufficient
	resources to provide.  Alternatively if SHUFFLE is true, then every time the
	providers are exhausted, they are shuffled randomly before being searched
	for resources.
	
	<p>This is a non-blocking provider only: it is primarily meant to be attached
	to a provider to a Queue or Delay etc. to enable them to extract from many
	sources at once.
*/

import sim.engine.*;
import java.util.*;

public class Multi extends BlockingProvider implements Receiver
	{
	ArrayList<Provider> providers = new ArrayList<>();
	int nextProvider = 0;
	boolean shuffle = false;
	
	public Multi(SimState state, Resource typical)
		{
		super(state, typical);
		}
		
	public Multi(SimState state, Resource typical, Provider[] providers)
		{
		this(state, typical);
		add(providers);
		}
	
	public boolean getShuffle() { return shuffle; }
	public void setShuffle(boolean val) { shuffle = val; }
	
	public void add(Provider provider)
		{
		providers.add(provider);
		}
	
	public void add(Provider[] providers)
		{
		for(int i = 0; i < providers.length; i++)
			add(providers[i]);
		}
	
	public Resource provide(double atLeast, double atMost)
		{
		if (resource.getAmount() >= atMost)
			{
			return resource.reduce(atMost);
			}
		
		if (providers.isEmpty()) return null;
		
		// go round robin starting at where we had left off...
		int len = providers.size();
		for(int i = 0; i < len; i++)
			{
			if (nextProvider >= len) 
				{
				if (shuffle)
					{
					Collections.shuffle(providers);
					}
				nextProvider = 0;
				}
			
			Provider provider = providers.get(nextProvider);
			double avail = resource.getAmount();
			Resource r = provider.provide(atLeast - avail < 0 ? 0 : atLeast - avail, atMost - avail);
			if (!typical.isSameType(r)) throwUnequalTypeException(r);
			if (r != null)
				{
				resource.add(r);
				}

			nextProvider++;
			}
			
		double avail = resource.getAmount();
		if (avail >= atLeast)
			{
			return resource.reduce(avail >= atMost ? atMost : avail);
			}
		else return null;
		}
	
	protected double computeAvailable()
		{
		double avail = resource.getAmount();
		for(Provider provider : providers)
			avail += provider.available();
		return avail;
		}

	// At present reception is all or nothing. Maybe that should change?
	// FIXME: For the moment I'm passing through the provider.  Should I do that?
	public boolean consider(Provider provider, double amount)
		{
		Resource providedType = provider.provides();
		if (!typical.isSameType(providedType)) throwUnequalTypeException(providedType);

		for(Receiver recv : receivers)
			{
			if (recv.consider(provider, amount))
				return true; 
			}
		return false;
		}
	}
	