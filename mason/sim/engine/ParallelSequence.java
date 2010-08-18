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
    
    <p>ParallelSequences are lightweight: they reuse the same threads
    if stepped repeatedly.  This means that you must never attach a ParallelSequence
    inside itself -- that'd be an infinite loop, but it also would create weird thread
    errors.
    
    <p>While ParallelSequences might LOOK cool, generally speaking the only time you should
    ever think to use them is if you actually HAVE multiple CPUs on your computer.  Otherwise
    they're almost certainly not the solution to your odd multiple-thread needs.

    <!-- This is no longer true: we're now setting the thread to be a daemon thread
    <p><b>Important Note 1:</b>
    Because ParallelSequences are lightweight, their threads are persistent -- even
    after your main() loop exits!  So if you use ParallelSequences, you have to remember to call
    System.exit(0); to exit your program instead.
    -->

    <p><b>Important Note</b>
    Because ParallelSequences are lightweight, their threads are persistent, even after your step()
    method has completed (this allows them to be reused for the next step() method.  If the ParallelSequence
    is garbage collected, we automatically delete all its threads in its finalize() method.  And that's the
    rub: even if you get rid of your ParallelSequence, it's often the case that its garbage collection is
    delayed, or even that the VM will <i>never</i> garbage collect it.
        
    <p>Thus when you're done with your ParallelSequence and wish to throw it away, you should always
    call <b>cleanup()</b>, which deletes the threads manually.  Otherwise the thread resources will leak
    and quickly consume all your available memory.
        
    <p>Alternatively you can call <b>setDestroysThreads(true)</b>  on your ParallelSequence.  This will
    cause the ParallelSequence to destroy its threads every single time the ParallelSequence's step()
    method completes.  This is expensive but you don't have to keep track of the ParallelSequence
    at the end of the run to call cleanup() on it.  It's not a bad idea for a ParallelSequence which
    is one-shot rather than repeating.
*/

public class ParallelSequence extends Sequence
    {
    Semaphore semaphore = new Semaphore(0);
    Worker[] workers;
    Thread[] threads;
    boolean pleaseDie = false;
    boolean operating = false;  // checking for circularity
    boolean destroysThreads = false;

    public boolean getDestroysThreads() { return destroysThreads; }
    public void setDestroysThreads(boolean val) { destroysThreads = val; }
        
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
            threads[i].setDaemon(true);
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
        pleaseDie = false;
        threads = null;
        }
                
    /** Call this just before you get rid of a ParallelSequence: for example, one good place is the stop() method of
        your simulation.  Never call this method inside the ParallelSequence's own step() method.  This method
        deletes the threads so the ParallelSequence is ready to be thrown away.  We also do this in
        finalize() but finalize() is not guaranteed to be called at any particular time, which can result in
        unexpected memory leaks.  Think of this method as the same kind of thing as a Graphics or 
        Window's dispose() method.  */
    public void cleanup()
        {
        gatherThreads();
        }

    protected void finalize() throws Throwable
        {
        try { cleanup(); } 
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

        if (threads == null)
            buildThreads();
                
        for(int x=0;x<steps.length;x++)
            {
            workers[x].step = steps[x];
            workers[x].state = state;
            workers[x].V();
            }
        for(int x=0;x<steps.length;x++)
            semaphore.P();

        if (destroysThreads)
            cleanup();

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
                
        // Thread currentThread = null;
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
                // this line was BAD -- because Thread is not serializable.  So I've commented it out 
                // but left it as a warning for others.  :-)
                //if (currentThread == null) currentThread = Thread.currentThread();  // a little efficiency?
                Thread.currentThread().setName("Parallel Sequence: " + step);
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
    
