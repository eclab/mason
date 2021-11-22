/** 
    A delay pipeline.  Elements placed in the delay are only available
    after a fixed amount of time.
*/


import sim.engine.*;
import java.util.*;

/**
   A simple linked list-based delay with a fixed delay time for all submitted elements.
*/

public class SimpleDelay extends Source implements Receiver, Steppable
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
    LinkedList<Node> delayQueue = new LinkedList<>();
    double delayTime;
    int ordering = 0;
    boolean autoSchedules = true;
    
    public boolean getAutoSchedules() { return autoSchedules; }
    public void setAutoScheduled(boolean val) { autoSchedules = val; }
        
    public void clear()
        {
        super.clear();
        delayQueue.clear();
        totalResource = 0.0;
        }
                
    public double getDelayTime() { return delayTime; }
    public void setDelayTime(double delayTime) { this.delayTime = delayTime; }

	public int getOrdering() { return ordering; }
	public void setOrdering(int ordering) { this.ordering = ordering; }

    void throwInvalidNumberException(double capacity)
        {
        throw new RuntimeException("Capacities may not be negative or NaN.  capacity was: " + capacity);
        }

	protected void buildDelay()
		{
        delayQueue = new LinkedList<>();
		}
		
    public SimpleDelay(SimState state, double delayTime, Resource typical)
        {
        super(state, typical);
        this.delayTime = delayTime;
        buildDelay();
        }

    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
        if (!resource.isSameType(amount)) 
            throwUnequalTypeException(amount);
        
        if (isOffering()) throwCyclicOffers();  // cycle
        
        double nextTime = state.schedule.getTime() + delayTime;
        if (entities == null)
            {
            CountableResource cr = (CountableResource)amount;
            double maxIncoming = Math.min(capacity - totalResource, atMost);
            if (maxIncoming < atLeast) return false;
                
            CountableResource token = (CountableResource)(cr.duplicate());
            token.setAmount(maxIncoming);
            cr.decrease(maxIncoming);
            delayQueue.add(new Node(token, nextTime));
            }
        else
            {
            if (delayQueue.size() >= capacity) return false; // we're at capacity
            delayQueue.add(new Node(amount, nextTime));
            }
        if (getAutoSchedules()) state.schedule.scheduleOnce(nextTime, getOrdering(), this);
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
                
        Iterator<Node> iterator = delayQueue.descendingIterator();
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
                     
    public void step(SimState state)
        {
        offerReceivers();
        }
    }
        
