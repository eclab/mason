/** 
    A delay pipeline.  Elements placed in the delay are only available
    after a fixed amount of time.
*/


import sim.engine.*;
import sim.util.distribution.*;
import sim.util.*;
import java.util.*;


/**
   A Heap-based delay which allows different submitted elements to have different
   delay times.  Delay times can be based on a provided distribution, or you can override
   the method getDelay(...) to customize delay times entirely based on the provide
   and resource being provided. 
*/

public class Delay extends SimpleDelay
    {
    Heap delayHeap;
    AbstractDistribution distribution = null;

	protected void buildDelay()
		{
        delayHeap = new Heap();
		}
		
    public Delay(SimState state, Resource typical)
        {
        super(state, 1.0, typical);
        }
        
    public void clear()
        {
        delayHeap = new Heap();
        totalResource = 0.0;
        }
        
    public void setDelayDistribution(AbstractDistribution distribution)
        {
        this.distribution = distribution;
        }
                
    public AbstractDistribution getDelayDistribution()
        {
        return this.distribution;
        }
                
    /** By default, provides Math.abs(getDelayDistribution().nextDouble()), or 1.0 if there is
        no provided distribution.  Override this to provide a custom delay given the 
        provider and resource amount or type. */
    protected double getDelay(Provider provider, Resource amount)
        {
        if (distribution == null) return getDelayTime();
        else return Math.abs(distribution.nextDouble());
        }
                
    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
        if (!resource.isSameType(amount)) 
            throwUnequalTypeException(amount);
                
        if (isOffering()) throwCyclicOffers();  // cycle
        
        double nextTime = state.schedule.getTime() + getDelay(provider, amount);

        if (entities == null)
            {
            CountableResource cr = (CountableResource)amount;
            double maxIncoming = Math.min(capacity - totalResource, atMost);
            if (maxIncoming < atLeast) return false;
                
            CountableResource token = (CountableResource)(cr.duplicate());
            token.setAmount(maxIncoming);
            cr.decrease(maxIncoming);
            delayHeap.add(token, nextTime);
            }
        else
            {
            if (delayHeap.size() >= capacity) return false;      // we're at capacity
            delayHeap.add(amount, nextTime);
            }
       
        if (getAutoSchedules()) state.schedule.scheduleOnce(nextTime, getOrdering(), this);
        
        return true;
        }

    protected void update()
        {
        drop();
        double time = state.schedule.getTime();
                
        while(((double)delayHeap.getMinKey()) >= time)
            {
            Resource _res = (Resource)(delayHeap.extractMin());
            if (entities == null)
                {
                CountableResource res = ((CountableResource)_res);
                resource.add(res);
                totalResource -= res.getAmount();
                }
            else
                {
                entities.add((Entity)(_res));
                }
            }
        }

    public String getName()
        {
        return "Delay(" + typical.getName() + ")";
        }               
    }
        
