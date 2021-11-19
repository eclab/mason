import sim.engine.*;

/** 
	A multiplexing distributor: when offered a resource, it in turn offers to
	each of its receivers.  When a receiver requests a resource, the Distributor
	in turn requests it of its upstream provider
	
	<p>This is a blocking provider.
	
	
	<p>Note that while the consolidator can be registered with multiple receivers,
	it's probably best used with a single receiver
	
*/

public class Distributor extends BlockingProvider implements Receiver
	{
	Provider provider;
	
	public Distributor(SimState state, Resource typical)
		{
		super(state, typical);
		}

	public Provider getProvider() { return provider; }
	public void setProvider(Provider provider) { this.provider = provider; }

	public Resource provide(double atLeast, double atMost)
		{
		return provider.provide(atLeast, atMost);
		}
		
	protected double computeAvailable()
		{
		return provider.available();
		}

	public void consider(Provider provider, double amount)
		{
		offerReceivers();
		}
	}
