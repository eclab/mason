/*
  Copyright 2020 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

/**
 * Overrides scheduleRepeating and scheduleOnce to use DistributedTentativeStep
 *
 */
public class DistributedSchedule extends Schedule
{
	private static final long serialVersionUID = 1;

	void throwStoppingException(Steppable event)
		{
		throw new RuntimeException("DistributedSchedule only accepts Stopping agents.  Attempt to schedule " + event + ", which is not Stopping.");
		}
		
	public boolean scheduleOnce(final Steppable event)
	{
		if (!(event instanceof Stopping)) throwStoppingException(event);
		Key k = new Key(/* must lock for: */time + 1.0, 0);
		synchronized (lock)
		{
			return _scheduleOnce(k, new DistributedTentativeStep((Stopping)event, k));
		}
	}

	public boolean scheduleOnceIn(final double delta, final Steppable event)
	{
		if (!(event instanceof Stopping)) throwStoppingException(event);
		Key k = new Key(/* must lock for: */ time + delta, 0);
		synchronized (lock)
		{
			return _scheduleOnce(k, new DistributedTentativeStep((Stopping)event, k));
		}
	}

	public boolean scheduleOnce(final Steppable event, final int ordering)
	{
		if (!(event instanceof Stopping)) throwStoppingException(event);
		Key k = new Key(/* must lock for: */time + 1.0, ordering);
		synchronized (lock)
		{
			return _scheduleOnce(k, new DistributedTentativeStep((Stopping)event, k));
		}
	}

	public boolean scheduleOnceIn(final double delta, final Steppable event, final int ordering)
	{
		if (!(event instanceof Stopping)) throwStoppingException(event);
		Key k = new Key(/* must lock for: */ time + delta, ordering);
		synchronized (lock)
		{
			return _scheduleOnce(k, new DistributedTentativeStep((Stopping)event, k));
		}
	}

	public boolean scheduleOnce(double time, final Steppable event)
	{
		if (!(event instanceof Stopping)) throwStoppingException(event);
		Key k = new Key(time, 0);
		synchronized (lock)
		{
			return _scheduleOnce(k, new DistributedTentativeStep((Stopping)event, k));
		}
	}

	public boolean scheduleOnce(double time, final int ordering, final Steppable event)
	{
		if (!(event instanceof Stopping)) throwStoppingException(event);
		Key k = new Key(time, ordering);
		synchronized (lock)
		{
			return _scheduleOnce(k, new DistributedTentativeStep((Stopping)event, k));
		}
	}

	public DistributedIterativeRepeat scheduleRepeating(final double time, final int ordering, final Steppable event, final double interval)
	{
		if (!(event instanceof Stopping)) throwStoppingException(event);
		if (interval <= 0)
			throw new IllegalArgumentException("The steppable " + event
					+ " was scheduled repeating with an impossible interval (" + interval + ")");
		DistributedIterativeRepeat r = new DistributedIterativeRepeat((Stopping)event, time, interval, ordering);

		synchronized (lock)
		{
			if (_scheduleOnce(r.getKey(), r))
				return r;
			else
				return null;
		}
	}

	public DistributedIterativeRepeat scheduleRepeating(final Steppable event, final int ordering, final double interval)
	{
		if (!(event instanceof Stopping)) throwStoppingException(event);
		return scheduleRepeating(time + interval, ordering, (Stopping)event, interval);
	}
	

}
