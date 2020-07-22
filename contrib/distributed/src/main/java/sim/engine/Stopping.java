/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

/** Stopping objects can store Stoppables.  This class is primarily used by the Distributed code.
	Notably: TentativeStep and Iterative Repeat are *not* Stopping objects, so the Schedule can
	distinguish them properly. */

public interface Stopping extends Steppable
    {
    public Stoppable getStoppable();
    public void setStoppable(Stoppable stop);
    }
