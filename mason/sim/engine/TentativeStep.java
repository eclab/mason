/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

/**
   A Steppable wrapper which can be stopped.  This is a convenience class for those situations where you'd
   like to schedule an agent to get stepped once in the future, but then think better of it 
   and would like to prevent it from happening.  Wrap your agent in the TentativeStep, then 
   schedule the TentativeStep on the Schedule.  When you want to prevent the agent's step() method from 
   being called, simply call stop() on the TentativeStep.  When stop() is called on a TentativeStep, it 
   sets its underlying agent to null and forgets about it.  Note that the TentativeStep itself is still
   scheduled, and so at some point the Schedule's time step will advance to that point even if the
   underlying Steppable has been removed from the TentativeStep.
    
   <p>Example usage:
    
   <pre><tt>
   double scheduleTime = ... 
   Steppable mySteppable = ...
    
   TentativeStep tent = new TentativeStep(mySteppable);
   state.schedule.scheduleOnce(scheduleTime,tent);
   </tt></pre>
    
   <p>Now, to stop mySteppable from being called before scheduleTime has come, you'd say:
    
   <p><pre><tt>
   tent.stop();
   </tt></pre>
*/

public class TentativeStep implements Steppable, Stoppable
    {
    private static final long serialVersionUID = 1;

    public Steppable step;
    public TentativeStep(Steppable step)
        {
        this.step = step;
        }
        
    public synchronized void step(SimState state)
        {
        if (step!=null)
            step.step(state);
        }
    
    public synchronized void stop()
        {
        step = null;
        }
    }
