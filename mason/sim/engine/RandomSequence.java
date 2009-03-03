/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

/**
   RandomSequence is a Sequence which executes its Steppable objects in random order each time.
   RandomSequence does not ordinarily synchronize on the random number generator -- unless you
   set shouldSynchronize to true.  This is commonly only necessary if you're running multithreaded
   (you're embedded inside a ParallelSequence for example).
*/

public class RandomSequence extends Sequence
    {
    final boolean shouldSynchronize;
    
    /** Does not synchronize on the random number generator */
    public RandomSequence(Steppable[] steps)
        {
        this(steps,false);
        }
    
    /** Synchronizes on the random number generator only if shouldSynchronize is true */
    public RandomSequence(Steppable[] steps, boolean shouldSynchronize)
        {
        super(steps);
        this.shouldSynchronize = shouldSynchronize;
        }
    
    int nextInt(SimState state, int n) 
        {
        synchronized (state.random) 
            { return state.random.nextInt(n); }
        }
    
    public void step(SimState state)
        {
        final boolean shouldSynchronize = this.shouldSynchronize;
        // shuffle steps first
        Steppable temp;
        for(int x=steps.length-1; x>=1 ; x--)
            {
            int i = (shouldSynchronize ? nextInt(state,x+1) : state.random.nextInt(x+1));
            temp = steps[i];
            steps[i] = steps[x];
            steps[x] = temp;
            }
        super.step(state);
        }
    }
