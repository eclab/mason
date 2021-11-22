/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import java.util.*;

public class Service extends Macro
    {
    SimpleDelay delay;
    Lock lock;
    Unlock unlock;
    Pool pool;
        
    public Service(SimState state, Resource typical, Pool pool, double allocation, double delayTime)
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

    public Service(SimState state, Resource typical, int initialResourceAllocation, double delayTime)
        {
        this(state, typical, new Pool(initialResourceAllocation), 1.0, delayTime);
        }
                
    public String getName()
        {
        return "Service(" + pool.getResource() + ", " + delay.getDelayTime() + ")";
        }
    }
