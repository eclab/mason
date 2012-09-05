/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;
import java.util.*;

/**
   RandomSequence is a Sequence which executes its Steppable objects in random order each time.

   <p>The RandomSequence uses the random number generator to do its shuffling.   
   If you use a RandomSequence within a ParallelSequence, or inside an AsynchronousSteppable,
   or in some other multithreaded condition, you should let the RandomSequence know this so that 
   it will lock on the random number generator
   properly.  This is done by setting the <b>shouldSynchronize</b> flag in the RandomSequence.
   Likewise, whenever in other threads you access the generator in a multithreaded context, you
   should have them synchronize on the generator first.
   
   <p>Be sure to read the class documentation on sim.engine.Sequence</b>
*/

public class RandomSequence extends Sequence
    {
    private static final long serialVersionUID = 1;

    final boolean shouldSynchronize;
    
    /** Creates an immutable RandomSequence.  Does not synchronize before using the random number generator */
    public RandomSequence(Steppable[] steps)
        {
        this(steps, false);
        }
    
    /** Creates an immutable RandomSequence.  Synchronizes on the random number generator only if shouldSynchronize is true */
    public RandomSequence(Steppable[] steps, boolean shouldSynchronize)
        {
        super(steps);
        this.shouldSynchronize = shouldSynchronize;
        }
    
    /** Creates an immutable RandomSequence.  Does not synchronize before using the random number generator */
    public RandomSequence(Collection steps)
        {
        this(steps, false);
        }
    
    /** Creates an immutable RandomSequence.  Synchronizes on the random number generator only if shouldSynchronize is true */
    public RandomSequence(Collection steps, boolean shouldSynchronize)
        {
        super(steps);
        this.shouldSynchronize = shouldSynchronize;
        }
    
    int nextInt(SimState state, int n) 
        {
        synchronized (state.random) 
            { return state.random.nextInt(n); }
        }
    
    protected boolean canEnsureOrder() { return false; }
    
    public void step(SimState state)
        {
        // first load the steps
        loadSteps();

        final boolean shouldSynchronize = this.shouldSynchronize;
        int size = this.size;
        Steppable[] steps = this.steps;


        // Then shuffle steps
        Steppable temp;
        for(int x=size-1; x>=1 ; x--)
            {
            int i = (shouldSynchronize ? nextInt(state,x+1) : state.random.nextInt(x+1));
            temp = steps[i];
            steps[i] = steps[x];
            steps[x] = temp;
            }
            
        // finally execute
        for(int x=0;x<size;x++)
            {
            if (steps[x]!=null) 
                {
                assert sim.util.LocationLog.set(steps[x]);
                steps[x].step(state);
                assert sim.util.LocationLog.clear();
                }
            }
        }
    }
