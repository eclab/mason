/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;
import java.io.Serializable;

import sim.util.*;
import ec.util.*;

/**
   This is a helper class used internally in Schedule to schedule steppables repeating.  It's exposed
   here to enable access by distributed MASON.  You generally shouldn't play with this class.
*/

public class IterativeRepeat implements Steppable, Stoppable
    {
    private static final long serialVersionUID = 1;

    double interval;
    Steppable step;  // if null, does not reschedule
    Schedule.Key key;
    protected Object[] lock = new Object[0];
    
    public int getOrdering() { return key.ordering; }
    public double getInterval() { return interval; }
    public double getTime() { return key.time; }
    public Steppable getSteppable() { return step; }
    public Schedule.Key getKey() { return key; }
    
    public IterativeRepeat(final Steppable step, final double time, final double interval, final int ordering)
        {
        if (interval < 0)
            throw new IllegalArgumentException("For the Steppable...\n\n" + step +
                "\n\n...the interval provided ("+interval+") is less than zero");
        else if (interval != interval)  /* NaN */
            throw new IllegalArgumentException("For the Steppable...\n\n" + step +
                "\n\n...the interval provided ("+interval+") is NaN");
        
        this.step = step;
        this.interval = interval;
        this.key = new Schedule.Key(time,ordering);
        }
        
    public void step(final SimState state)
        {
        synchronized(lock)
        	{
        if (step!=null)
            {
            try
                {
                // reuse the Key to save some gc perhaps -- it's been pulled out and discarded at this point
                key.time += interval;
                if (key.time < Schedule.AFTER_SIMULATION) 
                    state.schedule.scheduleOnce(key, this);  // may return false if we couldn't schedule, which is fine
                }
            catch (IllegalArgumentException e)
                {
                e.printStackTrace(); // something bad happened
                }
            assert sim.util.LocationLog.set(step);
            step.step(state);
            assert sim.util.LocationLog.clear();
            }
            }
        }
        
    public void stop()  
        {
        synchronized(lock)
        	{
        	step = null;
        	}
        }
        
    public String toString() { return "Schedule.IterativeRepeat[" + step + "]"; }
    }


