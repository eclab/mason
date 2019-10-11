/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

public class DistributedTentativeStep extends TentativeStep
    {
    private static final long serialVersionUID = 1;

    Schedule.Key key;
    public int getOrdering() { return key.ordering; }
    public double getTime() { return key.time; }
    public Schedule.Key getKey() { return key; }

    public DistributedTentativeStep(final Steppable step, final Schedule.Key key)
        {
        super(step);

//        System.err.println("Creating DTS for " + System.identityHashCode(step) + " " + key.getTime() + " " + key.getOrdering());
//        new Throwable().printStackTrace();

		if (step instanceof Stopping)
			{
			((Stopping)step).setStoppable(this);
			}
		else throw new RuntimeException("DistributedTentativeStep built on a non-Stopping Steppable");
        }

    public void stop()
        {
        synchronized(lock)
        {
        if (step != null)
        	{
        	((Stopping)step).setStoppable(null);
        	}
        super.stop();
        }
        }
    }
