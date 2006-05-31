/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

/**
   Sequence is a Steppable which, on being stepped, in turn stepps several other
   Steppable objects in turn.  These objects are stored in its steps array.
*/

public class Sequence implements Steppable
    {
    public Steppable[] steps;
    
    /** Assumes all the steps are filled.  Will use the steps provided. */
    public Sequence(Steppable[] steps)
        {
        this.steps = steps;
        }
    
    public void step(SimState state)
        {
        for(int x=0;x<steps.length;x++)
            {
            if (steps[x]!=null) steps[x].step(state);
            }
        }
    }
