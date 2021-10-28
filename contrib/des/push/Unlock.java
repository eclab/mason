import sim.engine.*;
import sim.util.*;
import java.util.*;

/*
  UNLOCKS allow up to N resources to pass through before refusing any more
*/

public class Unlock extends Lock
    {
    public Unlock(SimState state, Resource typical, Pool pool, double allocation)
        {
        super(state, typical, pool, allocation);
        }
        
    public Unlock(SimState state, Resource typical, Pool pool)
        {
        super(state, typical, pool);
        }
        
    public Unlock(SimState state, Resource typical, String name)
        {
        super(state, typical, name);
        }
        
    public Unlock(Lock other)
        {
        super(other);
        }

    protected boolean offerReceiver(Receiver receiver)
        {
        return receiver.accept(this, _amount, _atLeast, _atMost);
        }
        
    // Unlocks only make take-it-or-leave-it offers
    public boolean getOffersTakeItOrLeaveIt() { return true; }

    double _atLeast;
    double _atMost;
    Resource _amount;
        
    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
        if (!typical.isSameType(amount)) throwUnequalTypeException(amount);

        if (pool.getMaximum() - pool.getResource().getAmount() < allocation) return false;

        _amount = amount;
        _atLeast = atLeast;
        _atMost = atMost;
        boolean result = offerReceivers();
                
        if (true)                               // we always increment even if we fail
            {
            pool.getResource().increase(allocation);
            }
        return result;
        }

    public void step(SimState state)
        {
        // do nothing
        }

    public String getName()
        {
        return "Unlock(" + typical.getName() + ", " + pool + ", " + allocation + ")";
        }               
    }
