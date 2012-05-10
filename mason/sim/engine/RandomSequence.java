/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

/**
   RandomSequence is a Sequence which executes its Steppable objects in random order each time.

    <p>The RandomSequence uses the random number generator to do its shuffling.   
    If you use a RandomSequence within a ParallelSequence, or inside an AsynchronousSteppable,
    or in some other multithreaded condition, you should let the RandomSequence know this so that 
    it will lock on the random number generator
    properly.  This is done by setting the <b>shouldSynchronize</b> flag in the RandomSequence.
    Likewise, whenever in other threads you access the generator in a multithreaded context, you
    should have them synchronize on the generator first.

*/

public class RandomSequence extends Sequence
    {
    private static final long serialVersionUID = 1;

    final boolean shouldSynchronize;
    
    /** Does not synchronize before using the random number generator */
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
