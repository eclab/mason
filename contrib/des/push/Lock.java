import sim.engine.*;
import sim.util.*;
import java.util.*;

/*
LOCKS allow up to N resources to pass through before refusing any more
*/

public class Lock extends Provider implements Receiver
	{
	MutableDouble maxLocks;
	MutableDouble locks;
	
	public Lock(SimState state, Resource typical, int numLocks)
		{
		super(state, typical);
		if (numLocks < 0) numLocks = 0;
		maxLocks = new MutableDouble(numLocks);
		locks = new MutableDouble(numLocks);
		}
	
	public Lock(Lock other)
		{
		super(other.state, other.typical);
		maxLocks = other.maxLocks;
		locks = other.locks;
		}
	
	/**
	Call this to inform all receivers
	*/
	void offerReceivers(Resource amount, double atLeast, double atMost)
		{
		double amt = amount.getAmount();
		if (shufflesReceivers)
			{
			shuffle();
			while(true)
				{
				Receiver r = nextShuffledReceiver();
				if (r == null) return;
				r.accept(this, amount, atLeast, atMost);
				if (amount.getAmount() != amt)
					return;
				}
			}
		else
			{
			for(Receiver r : receivers)
				{
				r.accept(this, amount, atLeast, atMost);
				if (amount.getAmount() != amt)
					return;
				}
			}
		}

	public void accept(Provider provider, Resource amount, double atLeast, double atMost)
		{
		double amt = amount.getAmount();
		offerReceivers(amount, atLeast, atMost);
		if (amt != amount.getAmount())
			{
			locks.val++;
			}
		}

	public void step(SimState state)
		{
		// do nothing
		}

	public String getName()
		{
		return "";
		}		
	}