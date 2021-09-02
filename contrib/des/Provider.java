import sim.engine.*;
import java.util.*;

/**
	A non-blocking provider of resources.
*/

public abstract class Provider
	{
	protected Resource typical;
	protected Resource resource;
	protected SimState state;
	ArrayList<Receiver> receivers;
	boolean shufflesReceivers;
	
	public Resource getTypicalResource() { return typical; }
	public boolean getShufflesReceivers() { return shufflesReceivers; }
	public void setShufflesReceivers(boolean val) { shufflesReceivers = val; }

	public boolean registerReceiver(Receiver receiver)
		{
		if (receivers.contains(receiver)) return false;
		receivers.add(receiver);
		return true;
		}
				
	public boolean unregisterReceiver(Receiver receiver)
		{
		if (receivers.contains(receiver)) return false;
		receivers.remove(receiver);
		return true;
		}

	public abstract Resource provide(double atLeast, double atMost);

	protected abstract double computeAvailable();

	// This could be costly to recompute over and over again (by pushing down to the provider)
	// Perhaps we could compute it once and cache it...
	double availableCacheTimestamp = Double.NaN;
	double availableCache = 0;
	public double available()
		{
		if (availableCacheTimestamp == state.schedule.getTime())
			{
			return availableCache;
			}
		else
			{
			availableCacheTimestamp = state.schedule.getTime();
			availableCache = computeAvailable();
			return availableCache;
			}
		}
	
	public Provider(SimState state, Resource typical)
		{
		this.typical = typical.duplicate();
		this.typical.setAmount(0.0);
		resource = typical.duplicate();
		resource.setAmount(0.0);
		this.state = state;
		}
	
	int currentShuffledReceiver;
	protected void shuffle() 
		{
		currentShuffledReceiver = 0;
		}
		
	protected Receiver nextShuffledReceiver()
		{
		int size = receivers.size();
		if (currentShuffledReceiver == size) 
			return null;
		int pos = state.random.nextInt(size - currentShuffledReceiver) + currentShuffledReceiver;

		Receiver ret = receivers.get(pos);
		receivers.set(pos, receivers.get(currentShuffledReceiver));
		receivers.set(currentShuffledReceiver, ret);
		
		currentShuffledReceiver++;
		return ret;
		}
	

	protected void informReceivers()
		{
		double avail = available();
		if (avail == 0) return;
		
		if (shufflesReceivers)
			{
			shuffle();
			while(true)
				{
				Receiver r = nextShuffledReceiver();
				if (r == null) return;
				r.consider(this, avail);
				avail = available();
				if (avail == 0)
					return;
				}
			}
		else
			{
			for(Receiver r : receivers)
				{
				r.consider(this, avail);
				avail = available();
				if (avail == 0)
					return;
				}
			}
		}
		
	public Resource provide()
		{
		return provide(1.0, 1.0);
		}

	public void step(SimState state)
		{
		informReceivers();
		}
	}