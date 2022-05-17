/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

/**
   DISTRIBUTED ITERATIVE REPEAT is a subclass of IterativeRepeat designed to work with Distributed MASON.
   All Steppables scheduled on Distributed MASON's schedule are either wrapped in a
   DTentativeStep or in a DIterativeRepeat, so that they are stoppable
   when the steppable is migrated to another Schedule on another Partition. The difference
   between DIterativeRepeat and IterativeRepeat is all Steppables
   attached to this object must be Stopping.
*/

public class DIterativeRepeat extends IterativeRepeat 
    {
    private static final long serialVersionUID = 1;

    public DIterativeRepeat(final Stopping step, final double time, final double interval, final int ordering)
        {
        super(step, time, interval, ordering);
        step.setStoppable(this);
        }

    public void stop()
        {
        synchronized (lock)
            {
            if (step != null)
                {
                ((Stopping) step).setStoppable(null);
                }
            super.stop();
            }
        }

    public Stopping getSteppable()
        {
        return (Stopping) step;
        }

    public String toString()
        {
        return "Schedule.DIterativeRepeat[" + step + "]";
        }
    }
