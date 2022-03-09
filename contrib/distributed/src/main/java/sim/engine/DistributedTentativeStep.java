/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

/**
	DISTRIBUTED TENTATIVE STEP is a TentativeStep designed to work with Distributed MASON.
	All Steppables scheduled on Distributed MASON's schedule are either wrapped in a
	DistributedTentativeStep or in a DistributedIterativeRepeat, so that they are stoppable
	when the steppable is migrated to another Schedule on another Partition. The difference
	between DistributedTentativeStep and TentativeStep is that DistributedTentativeStep also
	contains the Key for when the Steppable was scheduled, which allows us to easily
	reschedule it for the same time and ordering elsewhere.  Additionally, all Steppables
	attached to this object must be Stopping.
 */
 
 public class DistributedTentativeStep extends TentativeStep
 {
	private static final long serialVersionUID = 1;

    Schedule.Key key;
    public int getOrdering()
    	{
    	return key.ordering;
    	}
    
    public double getTime()
    	{
    	return key.time;
    	}

	public DistributedTentativeStep(final Stopping step, Schedule.Key key)
	{
		super(step);
		step.setStoppable(this);
    	this.key = key;
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
}
