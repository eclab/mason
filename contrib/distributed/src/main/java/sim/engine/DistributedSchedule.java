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
        return super.scheduleOnce(new DistributedTentativeStep(event));
        }

    public boolean scheduleOnceIn(final double delta, final Steppable event)
        {
        return super.scheduleOnceIn(delta, new DistributedTentativeStep(event));
        }
        
    public boolean scheduleOnce(final Steppable event, final int ordering)
        {
        return super.scheduleOnce(new DistributedTentativeStep(event), ordering);
        }

    public boolean scheduleOnceIn(final double delta, final Steppable event, final int ordering)
        {
        return super.scheduleOnceIn(delta, new DistributedTentativeStep(event), ordering);
        }

    public boolean scheduleOnce(double time, final Steppable event)
        {
        return super.scheduleOnce(time, new DistributedTentativeStep(event));
        }
        
    public boolean scheduleOnce(double time, final int ordering, final Steppable event)
        {
        return super.scheduleOnce(time, ordering, new DistributedTentativeStep(event));
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
