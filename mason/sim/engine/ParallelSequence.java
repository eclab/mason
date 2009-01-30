/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

/** Spawns all the sequence elements in parallel on separate threads.
    This should ONLY be used if you know that all of the elements in
    the sequence can be executed independently of one another without
    any race conditions.  No synchronization on the model data is done --
    you're responsible for that if you need it.
    
    <p>For example, keep in mind that the random number generator is unsynchronized.
    You should not embed RandomSequences inside a ParallelSequence unless
    you've set their shouldSynchronize value to true, and elsewhere in your
    embedded steppables you're synchronizing on the random number generator.
    
    <!-- This is false: we're doing wait(), which releases locks held.
    <p>Also, keep in mind that the main thread which fired the ParallelSequence is still
    holding onto the schedule lock; thus inside the ParallelSequence threads you can't
    schedule anything (the Schedule is synchronized).
    -->

    <p>ParallelSequences are lightweight: they reuse the same threads
    if stepped repeatedly.  This means that you must never attach a ParallelSequence
    inside itself -- that'd be an infinite loop, but it also would create weird thread
    errors.
    
    <p>Because ParallelSequences are lightweight, their threads are persistent -- even
    after your main() loop exits!  So if you use ParallelSequences, you have to remember to call
    System.exit(0); to exit your program instead.
    
    <p>While ParallelSequences might LOOK cool, generally speaking the only time you should
    ever think to use them is if you actually HAVE multiple CPUs on your computer.  Otherwise
    they're almost certainly not the solution to your odd multiple-thread needs.
*/

public class ParallelSequence extends Sequence
    {
    Semaphore semaphore = new Semaphore(0);
    Worker[] workers;
    Thread[] threads;
    boolean pleaseDie = false;
    boolean operating = false;  // checking for circularity
    
    /// Threads are not serializable, so we must manually rebuild here
    private void writeObject(java.io.ObjectOutputStream p)
        throws java.io.IOException
        {
        p.writeObject(semaphore);
        p.writeObject(workers);
        // don't write the threads
        p.writeBoolean(pleaseDie);
        }
        
    /// Threads are not serializable, so we must manually rebuild here
    private void readObject(java.io.ObjectInputStream p)
        throws java.io.IOException, ClassNotFoundException
        {
        semaphore = (Semaphore)(p.readObject());
        workers = (Worker[])(p.readObject());
        pleaseDie = p.readBoolean();
        // recreate threads
        buildThreads();
        }

    // creates the worker threads. used in the constructor and when reading the object from serialization.
    // We presume that the existing threads are nonexistent -- we're loading from readObject or constructing.
    void buildThreads()
        {
        threads = new Thread[steps.length];
        for (int i = 0; i < steps.length; i++)
            {
            threads[i] = new Thread( workers[i] );
            threads[i].start();
            }
        }

    // sends all worker threads an EXIT signal
    void gatherThreads()
        {
        pleaseDie = true;
        for(int x=0;x<steps.length;x++)
            workers[x].V();
        for(int x=0;x<steps.length;x++)
            try { threads[x].join(); }  
            catch (InterruptedException e) { /* shouldn't happen */ }
        threads = null;
        }

    protected void finalize() throws Throwable
        {
        try { gatherThreads(); } 
        finally { super.finalize(); }
        }

    // sets up the worker threads. the number of steppable things the ParallelSequence handles cannot be modified after the constructor is called
    public ParallelSequence(Steppable[] steps)
        {
        super(steps);
        workers = new Worker[steps.length];
        for( int i = 0 ; i < steps.length ; i++ )
            workers[i] = new Worker();
        buildThreads();
        }

    // steps once (in parallel) through the steppable things
    public void step(final SimState state)
        {
        // just to be safe, we'll avoid the HIGHLY unlikely race condition of being stepped in parallel or nested here
        synchronized(workers)  // some random object we own
            {
            if (operating) 
                throw new RuntimeException("ParallelSequence stepped, but it's already in progress.\n" +
                    "Probably you have the same ParallelSequence nested, or the same ParallelSequence being stepped in parallel.\n" +
                    "Either way, it's a bug.");
            operating = true;
            }
        for(int x=0;x<steps.length;x++)
            {
            workers[x].step = steps[x];
            workers[x].state = state;
            workers[x].V();
            }
        for(int x=0;x<steps.length;x++)
            semaphore.P();
        // don't need to synchronize to turn operating off
        operating = false;
        }

    // a small semaphore class
    static class Semaphore implements java.io.Serializable
        {
        private int count;
        public Semaphore(int c) { count = c; }
        public synchronized void V()
            {
            if (count == 0)
                this.notify();
            count++;
            }
        public synchronized void P()
            {
            while (count == 0)
                try{ this.wait(); } 
            // we will do nothing if interrupted: just wait again.  Note that
            // the interrupted flag will be raised however when we leave.  That
            // should be fine.
                catch(InterruptedException e) { }
            count--;
            }
        // static inner classes don't need serialVersionUIDs
        }

    // a worker is a semaphore and also implements a runnable
    class Worker extends Semaphore implements Runnable
        {
        Steppable step;
        SimState state;
        public Worker()
            {
            super(0);
            }
        public void run()
            {
            while(true)
                {
                P();
                if (pleaseDie) return;
                step.step(state);
                semaphore.V();
                }
            }

        // explicitly state a UID in order to be 'cross-platform' serializable 
        // because we ARE an inner class and compilers come up with all sorts
        // of different UIDs for inner classes and their parents.
        static final long serialVersionUID = -7832866872102525417L;
        }

    // explicitly state a UID in order to be 'cross-platform' serializable
    // because we contain an inner class and compilers come up with all
    // sorts of different UIDs for inner classes and their parents.
    static final long serialVersionUID = 2731888904476273479L;
    }
    
