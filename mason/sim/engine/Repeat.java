/*
  Copyright 2012 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;
import java.io.Serializable;

import sim.util.*;
import ec.util.*;

/**
   Repeat is an abstract Steppable and Stoppable which generalizes the notion of repeated
   Steppables.  You can do simple repeated steppables directly in the Schedule (using
   scheduleRepeating(...) ) but these steppables just repeat every N timesteps.  If you want
   to repeat with a more complex pattern, use this class instead.  You could do it by hand, but
   the objective of this class is to make it a simpler process.
   
   Repeat takes a subsidiary Steppable, and each time the Repeat has step() called, it first
   reschedules itself in the Schedule, then calls step() on the subsidiary Steppable.
   Repeat defines an abstract method, getNextTime(...), which you must override to specify
   the next timestep to repeat.  Repeats are stoppable: if you call stop() on a Repeat that
   is presently scheduled, it will not call step() on its subsidiary Steppable and will instead
   drop out of the Schedule instead of rescheduling itself any more.
      
   <p>Let's say that you want to repeat on timesteps 0, 1, 3, 4, 6, 7, 9, 10, etc., that is,
   jump 1, then jump 2, then jump 1, then 2, etc.  The ordering will always be 1. 
   You could create a Repeated steppable as follows:
   
   <pre><tt>
   Steppable step = ...
   Repeat repeat = new Repeat(step, 1)
   {
   double jump = 2;
   protected double getNextTime(SimState state, double currentTime)
   {
   jump = (jump == 1 ? 2 : 1);
   return currentTime + jump;
   }
   };
   schedule.scheduleOnce(0.0, 1, repeat);
   </tt></pre>

   <p>Repeat can also handle random numbers.  For example, suppose you wanted to repeat
   at the current time, plus 1.0, plus a random number chosen from a power law distribution
   with an alpha of 3.0 and a cutoff of 0.5.  Starting at timestep 0.0, ordering 1.
   You could do this:
   
   <pre><tt>
   Steppable step = ...
   Repeat repeat = new Repeat(step, 1)
   {
   protected double getNextTime(SimState state, double currentTime)
   {
   return currentTime + 1.0 + sim.util.distributions.Distributions.nextPowLaw(3.0, 0.5, state.random);
   }
   };
   schedule.scheduleOnce(0.0, 1, repeat);
   </tt></pre>
   
   <p>Like RandomSequence, Repeat might
   be used in a multithreaded environment (inside a ParallelSequence, for example).  In this
   situation, you need to synchronize on the random number generator inside the getNextTime()
   method like this:
   
   <pre><tt>
   Steppable step = ...
   Repeat repeat = new Repeat(step, 1)
   {
   protected double getNextTime(SimState state, double currentTime)
   {
   synchronized(state.random)
   { 
   return currentTime + 1.0 + sim.util.distributions.Distributions.nextPowLaw(3.0, 0.5, state.random);
   }
   }
   };
   schedule.scheduleOnce(0.0, 1, repeat);
   </tt></pre>
   
   <p>You can also change the ordering.  Ordinarily the Repeat reschedules your Steppable
   under the same ordering that you passed into the constructor.  But you can update that
   (this is a rare need).  For example, to increase the ordering by 1 each time:
   
   <pre><tt>
   Steppable step = ...
   Repeat repeat = new Repeat(step, 1)
   {
   protected double getNextTime(SimState state, double currentTime)
   {
   setOrdering(getOrdering() + 1);
   return currentTime + 1.0 + sim.util.distributions.Distributions.nextPowLaw(3.0, 0.5, state.random);
   }
   };
   schedule.scheduleOnce(0.0, 1, repeat);
   </tt></pre>
   
   <p>Note that some distributions in sim.util.Distribution require instances be maintained, rather than
   just simple function calls (like nextPowLaw).  You can do this too.  For example, suppose you want
   to reschedule at the current time, plus 1.0, plus a Poisson-distributed value with a mean of 10.0.  
   You could do this (note the "final" declaration):
   
   <pre><tt>
   Steppable step = ...
   SimState state = ...
   final sim.util.distributions.Poisson poisson = new sim.util.distributions.Poisson(10.0, state);
   Repeat repeat = new Repeat(step, 1)
   {
   protected double getNextTime(SimState state, double currentTime)
   {
   return currentTime + 1.0 + poisson.nextInt();
   }
   };
   schedule.scheduleOnce(0.0, 1, repeat);
   </tt></pre>
   
   <p>Of course you might want the Repeat to also schedule itself at some random timestep initially as well.
   At present Repeat does not support this via an API -- you have to do it manually.  This is mostly
   because the using API would usually be just as long as doing it by hand.  But in the future we might provide
   something if there is demand.  Anyway, let's say you want to pick a uniform time in the future to schedule
   initially, between 0.0 and 10.0, inclusive.  Thereafter you want to reschedule using the Poisson distribution shown
   earlier.  You could do it like this:
   
   <pre><tt>
   Steppable step = ...
   SimState state = ...
   final sim.util.distributions.Poisson poisson = new sim.util.distributions.Poisson(10.0, state);
   Repeat repeat = new Repeat(step, 1)
   {
   protected double getNextTime(SimState state, double currentTime)
   {
   return currentTime + 1.0 + poisson.nextInt();
   }
   };
   schedule.scheduleOnce(state.random.nextDouble(true, true) * 10.0, 1, repeat);
   </tt></pre>
*/

public abstract class Repeat implements Steppable, Stoppable
    {
    Steppable step;  // if null, does not reschedule
    Schedule.Key key = null;
    int ordering;
    
    public Repeat(final Steppable step, int ordering)
        {
        this.step = step;
        this.ordering = ordering;
        }
    
    protected abstract double getNextTime(SimState state, double currentTime);
    
    public synchronized void setOrdering(int val)
        {
        ordering = val;
        }
        
    public synchronized int getOrdering()
        {
        return ordering;
        }
        
    public synchronized void step(final SimState state)
        {        
        if (step!=null)
            {
            try
                {
                if (key == null)
                    key = new Schedule.Key(state.schedule.getTime(), ordering);  // could be BEFORE_SIMULATION if you're manually calling this
                key.time = getNextTime(state, key.time);
                if (key.time < Schedule.AFTER_SIMULATION) 
                    state.schedule.scheduleOnce(key, this);  // may return false if we couldn't schedule, which is fine
                else return;
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
        
    public synchronized void stop()  
        {
        step = null;
        }
        
    public String toString() { return "Repeat[" + step + "]"; }
    }

