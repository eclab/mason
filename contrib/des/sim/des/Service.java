/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import java.util.*;

/**
   A simple Macro which consists of a lock (the receiver), a delay, an unlock (the provider), 
   and a Pool shared by the lock and unlock.
*/

public class Service extends Macro
    {
    SimpleDelay delay;
    Lock lock;
    Unlock unlock;
    Pool pool;
    
    /** Returns the provider. */
    public Provider getProvider() { return unlock; }
    
    /** Returns the receiver. */
    public Receiver getReceiver() { return lock; }
     
    /** Creates a service with the given pool, allocation from the pool, and delay time. */
    public Service(SimState state, Resource typical, Pool pool, double allocation, double delayTime)
        {
        lock = new Lock(state, typical, pool, allocation);
        unlock = new Unlock(lock);
        delay = new SimpleDelay(state, delayTime, typical);
        addReceiver(lock, true);
        addProvider(unlock, true);
        add(delay, true);
        lock.addReceiver(delay);
        delay.addReceiver(unlock);
        this.pool = pool;
        }

    /** Creates a service with a brand new pool, initial resources in the pool, and delay time. 
        Allocation is assumed to be 1.0.  */
    public Service(SimState state, Resource typical, int initialResourceAllocation, double delayTime)
        {
        this(state, typical, new Pool(initialResourceAllocation), 1.0, delayTime);
        }
                
    public String toString()
        {
        return "Service@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : getName()) + ", " + pool.getResource() + ", " + delay.getDelayTime() + ")";
        }
    }
