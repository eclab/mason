/*
  Copyright 2020 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

/** 
    A simple abstract agent superclass which implements the Stopping interface so you don't
    have to.  You are not required to subclass your distributed agents from this class, 
    as long as they implement the Stopping interface themselves in a similar fashion.
*/

public abstract class DSteppable implements Stopping
    {
    private static final long serialVersionUID = 1L;

    Stoppable stop;
    public Stoppable getStoppable() { return stop; }
    public void setStoppable(Stoppable stop) { this.stop = stop; }
    
    public abstract void step(SimState state);
    }
