/** 
    A delay pipeline.  Elements placed in the delay are only available
    after a fixed amount of time.
*/


import sim.engine.*;
import sim.util.distribution.*;
import sim.util.*;
import java.util.*;


/**
   A heap-based delay which allows different submitted elements to have different
   delay times.  Delay times can be based on a provided distribution, or you can override
   the method getDelay(...) to customize delay times entirely based on the provide
   and resource being provided. 
*/

public class Delay extends Source implements Receiver
    {
    Heap heap;
        
    class Node
        {
        public Resource resource;
        public double timestamp;
                
        public Node(Resource resource, double timestamp)
            {
            this.resource = resource;
            this.timestamp = timestamp;
            }
        }
        
    double totalResource = 0.0;
    AbstractDistribution distribution = null;

    void throwInvalidNumberException(double capacity)
        {
        throw new RuntimeException("Capacities may not be negative or NaN.  capacity was: " + capacity);
        }

    public Delay(SimState state, Resource typical)
        {
        super(state, typical);
        heap = new Heap();
        }
        
    public void clear()
        {
        heap = new Heap();
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
        if (distribution == null) return 1.0;
        else return Math.abs(distribution.nextDouble());
        }
                
    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
        if (!resource.isSameType(amount)) 
            throwUnequalTypeException(amount);
                
        if (entities == null)
            {
            CountableResource cr = (CountableResource)amount;
            double maxIncoming = Math.min(capacity - totalResource, atMost);
            if (maxIncoming < atLeast) return false;
                
            CountableResource token = (CountableResource)(cr.duplicate());
            token.setAmount(maxIncoming);
            cr.decrease(maxIncoming);
            heap.add(token, state.schedule.getTime() + getDelay(provider, amount));
            }
        else
            {
            if (heap.size() >= capacity) return false;      // we're at capacity
            heap.add(amount, getDelay(provider, amount));
            }
        return true;
        }

    protected void drop()
        {
        if (entities == null)
            resource.clear();
        else
            entities.clear();
        }
                
    protected void update()
        {
        drop();
        double time = state.schedule.getTime();
                
        while(((double)heap.getMinKey()) >= time)
            {
            Resource _res = (Resource)(heap.extractMin());
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
        
