/** 
    A delay pipeline.  Elements placed in the delay are only available
    after a fixed amount of time.
*/


import sim.engine.*;
import java.util.*;

/**
   A simple linked list-based delay with a fixed delay time for all submitted elements.
*/

public class SimpleDelay extends Source implements Receiver
    {
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
    LinkedList<Node> resources = new LinkedList<>();
    double delayTime;
        
    public void clear()
        {
        super.clear();
        resources.clear();
        totalResource = 0.0;
        }
                
    public double getDelayTime() { return delayTime; }
    public void setDelayTime(double delayTime) { this.delayTime = delayTime; }

    void throwInvalidNumberException(double capacity)
        {
        throw new RuntimeException("Capacities may not be negative or NaN.  capacity was: " + capacity);
        }

    public SimpleDelay(SimState state, double delayTime, Resource typical)
        {
        super(state, typical);
        this.delayTime = delayTime;
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
            resources.add(new Node(token, state.schedule.getTime() + delayTime));
            }
        else
            {
            if (resources.size() >= capacity) return false; // we're at capacity
            resources.add(new Node(amount, state.schedule.getTime() + delayTime));
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
                
        Iterator<Node> iterator = resources.descendingIterator();
        while(iterator.hasNext())
            {
            Node node = iterator.next();
            if (node.timestamp >= time)     // it's ripe
                {
                if (entities == null)
                    {
                    CountableResource res = ((CountableResource)(node.resource));
                    iterator.remove();
                    resource.add(res);
                    totalResource -= res.getAmount();
                    }
                else
                    {
                    entities.add((Entity)(node.resource));
                    }
                }
            else break;             // don't process any more
            }
        }

    public String getName()
        {
        return "SimpleDelay(" + typical.getName() + ", " + delayTime + ")";
        }               
    }
        
