/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;
import java.lang.ref.*;

/**
   WeakStep is a wrapper for steppable objects in the situation where we only want
   the schedule to tenuously hold onto the steppable object -- that is, if everyone
   else has forgotten about the object, the schedule should as well.  This is commonly
   the case for inspectors and other kinds of windows which might be closed by the user
   without any way to get back to the schedule and remove themselves.

   You schedule a Steppable weakly something like this:

   <tt><br>
   mySchedule.schedule(new WeakStep(mySteppable));  <br>
   </tt>
   <br>

   A WeakStep can also wrap a Steppable you plan on scheduling repeating.  You'd
   do it along these lines:

   <tt><br>
   WeakStep weak = new WeakStep(mySteppable);  <br>
   Stoppable stop = mySchedule.scheduleRepeating(weak);  <br>
   weak.setStoppable(stop);  <br>
   </tt>

   In this case, when the underlying Steppable is garbage-collected, then the
   schedule will automatically stop repeatedly stepping it.  Note that the Stoppable
   is <i>not</i> stored weakly.
*/

public class WeakStep implements Steppable
    {
    private static final long serialVersionUID = 1;

    WeakReference weakStep;
    Stoppable stop;  // will be null unless setStoppable() called
    
    // WeakReferences are not serializable -- so we need
    // to unwrap them here.
    private void writeObject(java.io.ObjectOutputStream p)
        throws java.io.IOException
        {
        p.writeObject(weakStep.get());
        p.writeBoolean(stop!=null);
        if (stop != null)
            p.writeObject(stop);
        }
        
    // WeakReferences are not serializable -- so we need
    // to rewrap them here.
    private void readObject(java.io.ObjectInputStream p)
        throws java.io.IOException, ClassNotFoundException
        {
        weakStep = new WeakReference(p.readObject());
        if (p.readBoolean())  // weakStop != null
            stop = (Stoppable)(p.readObject());
        else stop = null;  // just in case
        }

    public WeakStep(Steppable step)
        {
        weakStep = new WeakReference(step);
        }
    
    public void setStoppable(Stoppable stop)
        {
        this.stop = stop;
        }
    
    public void step(SimState state)
        {
        Steppable step = (Steppable)(weakStep.get());
        if (step != null)
            step.step(state);
        else if (stop != null)
            stop.stop();
        }
    }
