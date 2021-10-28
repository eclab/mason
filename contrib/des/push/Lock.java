import sim.engine.*;
import sim.util.*;
import java.util.*;

/*
  LOCKS allow up to N resources to pass through before refusing any more
*/

public class Lock extends Provider implements Receiver
    {
    Pool pool;
    double allocation;
        
    public Lock(SimState state, Resource typical, Pool pool, double allocation)
        {
        super(state, typical);
        this.allocation = allocation;
        this.pool = pool;
        }
        
    public Lock(SimState state, Resource typical, Pool pool)
        {
        this(state, typical, pool, Double.POSITIVE_INFINITY);
        }
        
    public Lock(SimState state, Resource typical, String name)
        {
        this(state, typical, new Pool(new CountableResource(name, 0.0), 1.0), Double.POSITIVE_INFINITY);
        }
        
    public Lock(Lock other)
        {
        super(other.state, other.typical);
        this.pool = other.pool;
        this.allocation = other.allocation;
        }
                
    public double getAllocation() { return allocation; }
    public void setAllocation(double val) { allocation = val; }
        
    // Locks only make take-it-or-leave-it offers
    public boolean getOffersTakeItOrLeaveIt() { return true; }

    protected boolean offerReceiver(Receiver receiver)
        {
        return receiver.accept(this, _amount, _atLeast, _atMost);
        }
        
    double _atLeast;
    double _atMost;
    Resource _amount;
        
    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
        if (!typical.isSameType(amount)) throwUnequalTypeException(amount);

        if (pool.getResource().getAmount() < allocation) return false;

        _amount = amount;
        _atLeast = atLeast;
        _atMost = atMost;
        boolean result = offerReceivers();
                
        if (result)
            {
            pool.getResource().decrease(allocation);
            }
        return result;
        }

    public void step(SimState state)
        {
        // do nothing
        }

    public String getName()
        {
        return "Lock(" + typical.getName() + ", " + pool + ", " + allocation + ")";
        }               
    }
