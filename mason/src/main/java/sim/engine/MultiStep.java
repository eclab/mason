/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

/**
   MultiStep takes an integer N, a boolean called countdown, and a steppable.  
   EITHER the subsidiary steppable is stepped N times for each MultiStep.step(...), OR the
   subsidiary steppable is stepped once every N MultiStep.step(...) calls.
   The second one occurs if countdown is set to true.  If N is <=0, then the subsidiary
   steppable is never stepped no matter what.

   <p>The count down (if we're doing that) can be reset by calling resetCountdown().
   MultiStep is properly synchronized.

   <p>If you're using MultiStep to schedule an agent to occur repeating over time, you'd probably
   do better to use Schedule.scheduleRepeating(...) instead.
*/

public class MultiStep implements Steppable
    {
    private static final long serialVersionUID = 1;

    int current;
    final boolean countdown;
    final int n;
    final Steppable step;
    
    /** If countdown is true, then we call step.step(...) once every N times we're stepped.
        if countdown is false, then we call step.step(...) N times every time
        we're stepped.*/
    public MultiStep(Steppable step, int n, boolean countdown)
        {
        if (n < 0) n = 0;
        this.n = n;
        this.step = step;
        this.countdown = countdown;
        current = n;
        }
    
    /** If we're counting down, then this resets the countdown. */
    public synchronized void resetCountdown()
        {
        current = n;
        }
    
    /** If we're counting down, then this resets the countdown to the given value, which should be > 0 and &lt;= n. 
        Note that if n = 0, this method has no valid value you can pass in. */
    public synchronized void resetCountdown(int val)
        {
        if (val <= n && val > 0)
            current = val;
        }
    
    // this allows us to jump in and out of the steppable so that the
    // MultiStep can be reset() even in the middle of a step(state)...
    synchronized boolean go() 
        { 
        if (--current == 0) { current = n; return true; }
        return false;
        }
    
    public void step(SimState state)
        {
        if (n == 0) { }  // do nothing
        else if (countdown)   // do once every n times
            {
            if (go())
                step.step(state);
            }
        else  // do n times
            {
            for(int x=0;x<n;x++)
                step.step(state);
            }
        }
    }
