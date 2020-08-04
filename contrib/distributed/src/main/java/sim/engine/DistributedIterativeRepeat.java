/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;
import java.io.Serializable;

import sim.util.*;
import ec.util.*;

public class DistributedIterativeRepeat extends IterativeRepeat
    {
    private static final long serialVersionUID = 1;

    public DistributedIterativeRepeat(final Steppable step, final double time, final double interval, final int ordering)
        {
        super(step, time, interval, ordering);
        if (step instanceof Stopping)
        	{
        	((Stopping)step).setStoppable(this);
        	}
		else throw new RuntimeException("DistributedIterativeRepeat built on a non-Stopping Steppable");
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
        
    public String toString() { return "Schedule.DistributedIterativeRepeat[" + step + "]"; }
    }


