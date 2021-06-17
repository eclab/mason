/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

/**
 * Changes constructor to set Stoppable. Overrides stop() to set Stoppable as null
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
