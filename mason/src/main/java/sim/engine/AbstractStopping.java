/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

/** A simple implementation of Stopping in a Steppable. */

public abstract class AbstractStopping implements Steppable, Stopping 
    {
    Stoppable stop = null;
    public Stoppable getStoppable() { return stop; }
    public void setStoppable(Stoppable stop) { this.stop = stop; }
    }
