import sim.engine.*;
import java.util.*;

public class Server extends Macro
    {
    SimpleDelay delay;
    Lock lock;
    Unlock unlock;
    Pool pool;
        
    public Server(SimState state, Resource typical, Pool pool, double allocation, double delayTime)
        {
        lock = new Lock(state, typical, pool, allocation);
        unlock = new Unlock(lock);
        this.delay = new SimpleDelay(state, delayTime, typical);
        addReceiver(lock);
        addProvider(unlock);
        add(this.delay);
        lock.addReceiver(this.delay);
        this.delay.addReceiver(unlock);
        this.pool = pool;
        }

    public Server(SimState state, Resource typical, int initialResourceAllocation, double delayTime)
        {
        this(state, typical, new Pool(initialResourceAllocation), 1.0, delayTime);
        }
                
    public String getName()
        {
        return "Process(" + pool.getResource() + ", " + delay.getDelayTime() + ")";
        }
    }
