/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

/**
 * Changes constructor to set Stoppable. Overrides stop() to set Stoppable as null
 */
public class DistributedIterativeRepeat extends IterativeRepeat 
{
	private static final long serialVersionUID = 1;

	public DistributedIterativeRepeat(final Stopping step, final double time, final double interval, final int ordering)
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
		return "Schedule.DistributedIterativeRepeat[" + step + "]";
	}
}
