/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

public class DistributedTentativeStep extends TentativeStep
    {
    private static final long serialVersionUID = 1;

    public DistributedTentativeStep(Steppable step)
        {
        super(step);
		if (step instanceof Stopping)
			{
			((Stopping)step).setStoppable(this);
			}
        }
        
    public void stop()
        {
        synchronized(lock)
        {
        if (step != null && step instanceof Stopping)
        	{
        	((Stopping)step).setStoppable(null);
        	}
        super.stop();
        }
        }
    }
