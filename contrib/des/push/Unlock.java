import sim.engine.*;
import sim.util.;
import java.util.*;

/*
UNLOCKS allow up to N resources to pass through before refusing any more
*/

public class Unlock extends Provider implements Receiver
	{
	MutableDouble maxLocks;
	MutableDouble locks;
	
	public Unlock(Lock other)
		{
		super(other.state, other.typical);
		maxLocks = other.maxLocks;
		locks = other.locks;
		setOffersOnce(true);
		}
	
	public void accept(Provider provider, Resource amount, double atLeast, double atMost)
		{
		if (locks.val == 0) return;
		offerReceivers();
		locks.val++;			// we always increment even if we fail
		if (locks.val > maxLocks.val)
			locks.val = maxLocks.val;
		}

	public void step(SimState state)
		{
		// do nothing
		}

	}