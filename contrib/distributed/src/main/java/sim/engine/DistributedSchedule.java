/*
  Copyright 2020 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

/**
	DISTRIBUTED SCHEDULE is a subclass of Schedule designed to work with Distributed MASON.
	It automatically wraps all scheduled objects in either a DistributedIterativeRepeat or
	a DistributedTentativeStep.  All Steppables which are added to the DistributedSchedule
	must be Stopping or an exception will be thrown.
 */
 
public class DistributedSchedule extends Schedule
{
	private static final long serialVersionUID = 1;

	void throwStoppingException(Steppable event)
		{
		throw new IllegalArgumentException("DistributedSchedule only accepts Stopping agents.  Attempt to schedule " + event + ", which is not Stopping.");
		}

	void nonIntegerRepeatingException(double n, Steppable event)
		{
		throw new IllegalArgumentException("The event " + event + " was scheduled to repeat starting at (time + interval) = " + n + ", which is not an integer.  DistributedSchedule requires that all timestamps be integers.");
		}
		
	void nonIntegerIntervalException(double n, Steppable event)
		{
		throw new IllegalArgumentException("The event " + event + " was scheduled with interval = " + n + ", which is not an integer.  DistributedSchedule requires that all timestamps be integers.");
		}

	void nonIntegerException(double n, Steppable event)
		{
		throw new IllegalArgumentException("The event " + event + " was scheduled at time = " + n + ", which is not an integer.  DistributedSchedule requires that all timestamps be integers.");
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
		double newtime = time + delta;
		if ((newtime != AFTER_SIMULATION) && (newtime != (int) newtime))
			nonIntegerException(newtime, event);
		Key k = new Key(/* must lock for: */ newtime, 0);
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
		double newtime = time + delta;
		if ((newtime != AFTER_SIMULATION) && (newtime != (int) newtime))
			nonIntegerException(newtime, event);
		Key k = new Key(/* must lock for: */ newtime, ordering);
		synchronized (lock)
		{
			return _scheduleOnce(k, new DistributedTentativeStep((Stopping)event, k));
		}
	}

	public boolean scheduleOnce(double time, final Steppable event)
	{
		if (!(event instanceof Stopping)) throwStoppingException(event);
		if ((time != AFTER_SIMULATION) && (time != (int) time))
			nonIntegerException(time, event);
		Key k = new Key(time, 0);
		synchronized (lock)
		{
			return _scheduleOnce(k, new DistributedTentativeStep((Stopping)event, k));
		}
	}

	public boolean scheduleOnce(double time, final int ordering, final Steppable event)
	{
		if (!(event instanceof Stopping)) throwStoppingException(event);
		if ((time != AFTER_SIMULATION) && (time != (int) time))
			nonIntegerException(time, event);
		Key k = new Key(time, ordering);
		synchronized (lock)
		{
			return _scheduleOnce(k, new DistributedTentativeStep((Stopping)event, k));
		}
	}

	public DistributedIterativeRepeat scheduleRepeating(final double time, final int ordering, final Steppable event, final double interval)
	{
		if (!(event instanceof Stopping)) throwStoppingException(event);
		if ((time + interval != AFTER_SIMULATION) && (time + interval != (int) (time + interval)))
			 nonIntegerRepeatingException(time + interval, event);
		if (interval != (int) interval)
			 nonIntegerIntervalException(interval, event);
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
		if ((time + interval != AFTER_SIMULATION) && (time + interval != (int) (time + interval)))
			 nonIntegerRepeatingException(time + interval, event);
		if (interval != (int) interval)
			 nonIntegerIntervalException(interval, event);
		return scheduleRepeating(time + interval, ordering, (Stopping)event, interval);
	}
	

}
