/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;
import java.io.Serializable;

import sim.util.*;
import ec.util.*;



public class DistributedSchedule extends Schedule
    {
    private static final long serialVersionUID = 1;

    public boolean scheduleOnce(final Steppable event)
        {
        Key k = new Key(/*must lock for:*/time +1.0,0);
        synchronized(lock)
            {
            return _scheduleOnce(k,new DistributedTentativeStep(event, k));
            }
        }

    public boolean scheduleOnceIn(final double delta, final Steppable event)
        {
        Key k = new Key(/*must lock for:*/ time + delta, 0);
        synchronized(lock)
            {
            return _scheduleOnce(k, new DistributedTentativeStep(event, k));
            }
        }
        
    public boolean scheduleOnce(final Steppable event, final int ordering)
        {
        Key k = new Key(/*must lock for:*/time +1.0,ordering);
        synchronized(lock)
            {
            return _scheduleOnce(k,new DistributedTentativeStep(event, k));
            }
        }

    public boolean scheduleOnceIn(final double delta, final Steppable event, final int ordering)
        {
        Key k = new Key(/*must lock for:*/ time + delta, ordering);
        synchronized(lock)
            {
            return _scheduleOnce(k, new DistributedTentativeStep(event, k));
            }
        }

    public boolean scheduleOnce(double time, final Steppable event)
        {
        Key k = new Key(time,0);
        synchronized(lock)
            {
            return _scheduleOnce(k,new DistributedTentativeStep(event, k));
            }
        }
        
    public boolean scheduleOnce(double time, final int ordering, final Steppable event)
        {
        Key k = new Key(time,ordering);
        synchronized(lock)
            {
            return _scheduleOnce(k,new DistributedTentativeStep(event, k));
            }
        }
    
    public IterativeRepeat scheduleRepeating(final double time, final int ordering, final Steppable event, final double interval)
        {
        if (interval <= 0) throw new IllegalArgumentException("The steppable " +  event + " was scheduled repeating with an impossible interval ("+interval+")");
        DistributedIterativeRepeat r = new DistributedIterativeRepeat(event, time, interval, ordering);

        synchronized(lock)
            {
            if (_scheduleOnce(r.getKey(),r)) return r;
            else return null;
            }
        }
    }
