/** 
	A multiplexing provider: when asked to provide a resource, it goes to each
	of its resources and asks them in turn, round-robin, until it has sufficient
	resources to provide.  Alternatively if SHUFFLE is true, then every time the
	providers are exhausted, they are shuffled randomly before being searched
	for resources.
	
	<p>This is a non-blocking provider only: it is primarily meant to be attached
	to a provider to a Queue or Delay etc. to enable them to extract from many
	sources at once.
	
	<p>The Consolidator can be <i>consolidating</i> or (um...) <i>non-consoliating</i>.
	In non-consolidating mode, the consolidator must be able to obtain ALL of the 
	requested resource from a SINGLE incoming provider, else it fails an returns nothing.
	In non-consolidating mode, the consoliator can fulfill the request by grouping
	together resources from multiple providers.  It does so by making full requests in turn
	until it has gathered enough resources.  The default is NON-consolidating.
*/

import sim.engine.*;
import java.util.*;

public class Consolidator extends Provider implements Receiver
	{
	ArrayList<Provider> providers = new ArrayList<>();
	Provider currentProvider = null;
	
	public Consolidator(SimState state, Resource typical)
		{
		this(state, typical, false);
		}

	public Consolidator(SimState state, Resource typical, boolean consolidating)
		{
		super(state, typical);
		this.consolidating = consolidating;
		}

	boolean consolidating;
	public boolean getConsolidating() { return consolidating; }
	public void setConsolidating(boolean val) { consolidating = val; }
	
	boolean shuffleProviders;
	public boolean getShuffleProviders() { return shuffleProviders; }
	public void setShuffleProviders(boolean val) { shuffleProviders = val; }
	
	public boolean registerProvider(Provider provider)
		{
		if (providers.contains(provider)) return false;
		providers.add(provider);
		return true;
		}
		
	public boolean unregisterProvider(Provider provider)
		{
		if (providers.contains(provider)) return false;
		providers.remove(provider);
		return true;
		}
	
	int currentShuffledProvider;
	protected void shuffle() 
		{
		currentShuffledProvider = 0;
		}
		
	protected Provider nextShuffledProvider()
		{
		int size = providers.size();
		if (currentShuffledProvider == size) 
			return null;
		int pos = state.random.nextInt(size - currentShuffledProvider) + currentShuffledProvider;

		Provider ret = providers.get(pos);
		providers.set(pos, providers.get(currentShuffledProvider));
		providers.set(currentShuffledProvider, ret);
		
		currentShuffledProvider++;
		return ret;
		}


	public Resource provide(double atLeast, double atMost)
		{
		if (currentProvider != null)
			{
			return currentProvider.provide(atLeast, atMost);
			}
		else if (consolidating)
			{
			double avail = available();
			if (avail > atLeast)
				{
				Resource total = typical.duplicate();
				total.clear();
				double totalAmt = 0;
				
				if (shuffleProviders)
					{
					shuffle();
					while(true)
						{
						Provider r = nextShuffledProvider();
						if (r == null) return null;  // uh oh
						Resource result = r.provide(0, atMost - totalAmt);
						if (result != null)
							{
							totalAmt += result.getAmount();
							total.add(result);
							if (totalAmt >= atLeast)
								return total;
							}
						}
					}
				else
					{
					for(Provider r : providers)
						{
						Resource result = r.provide(atLeast, atMost);
						if (result != null)
							{
							totalAmt += result.getAmount();
							total.add(result);
							if (totalAmt >= atLeast)
								return total;
							}
						}
					}
				return null;
				}
			else return null;
			}
		else
			{
			if (shuffleProviders)
				{
				shuffle();
				while(true)
					{
					Provider r = nextShuffledProvider();
					if (r == null) return null;
					Resource result = r.provide(atLeast, atMost);
					if (result != null)
						return result;
					}
				}
			else
				{
				for(Provider r : providers)
					{
					Resource result = r.provide(atLeast, atMost);
					if (result != null)
						return result;
					}
				}
			return null;
			}
		}
		
	public Resource provides() { return typical; }

	public double available()
		{
		if (currentProvider == null)
			{
			return super.available();
			}
		else
			{
			return currentProvider.available();
			}
		}
		
	protected double computeAvailable()
		{
		double avail = 0;
		if (consolidating)
			{
			for(Provider provider : providers)
				avail += provider.available();
			}
		else
			{
			for(Provider provider : providers)
				{
				double pa = provider.available();
				if (pa > avail) avail = pa;
				}
			}
		return avail;
		}

	public void consider(Provider provider, double amount)
		{
		currentProvider = provider;
		offerReceivers();
		}
	}
	