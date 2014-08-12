/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;
import java.util.concurrent.*;
import java.util.*;
import sim.util.*;

/** Spawns all the sequence elements in parallel on separate threads.
    This should ONLY be used if you know that all of the elements in
    the sequence can be executed independently of one another without
    any race conditions.  No synchronization on the model data is done --
    you're responsible for that if you need it.
    
    <p>For example, keep in mind that the random number generator is unsynchronized.
    If you access the random number generator from within a ParallelSequence, or
    indeed from multiple threads you've spawned in other situations, you need
    to remember to lock on the random number generator itself.
    
    <p>In the same vein, if you use a RandomSequence within a ParallelSequence, you need
    to let the RandomSequence know this so that it will lock on the random number generator
    properly.  This is done by setting the <b>shouldSynchronize</b> flag in the RandomSequence.
    
    <p>ParallelSequences are lightweight: they reuse the same threads
    if stepped repeatedly.  This means that you must never attach a ParallelSequence
    inside itself -- that'd be an infinite loop, but it also would create weird thread
    errors.
    
    <p>While ParallelSequences might LOOK cool, generally speaking the only time you should
    ever think to use them is if you actually HAVE multiple CPUs on your computer.  Otherwise
    they're almost certainly not the solution to your odd multiple-thread needs.

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
    
    <p>Be sure to read the class documentation on sim.engine.Sequence</b>
*/

public class ParallelSequence extends Sequence
    {
    ThreadPool threads;
    boolean pleaseDie = false;
    Object operatingLock = new Object();
    boolean operating = false;  // checking for circularity
    boolean destroysThreads = false;
    int numThreads = 0;
    
    /** Indicates that MASON should determine how many threads to use based on the number of CPUs. */ 
    public static final int CPUS = -1;
    public static final int STEPPABLES = -2;
    static int availableProcessors = Runtime.getRuntime().availableProcessors();
        
    public boolean getDestroysThreads() { return destroysThreads; }
    public void setDestroysThreads(boolean val) { destroysThreads = val; }
        
    /// Threads are not serializable, so we must manually rebuild here
    private void writeObject(java.io.ObjectOutputStream p)
        throws java.io.IOException
        {
        p.writeBoolean(pleaseDie);
        p.writeBoolean(destroysThreads);
        p.writeInt(numThreads);
        // don't write operating
        // dont' write threads
        }
        
    /// Threads are not serializable, so we must manually rebuild here
    private void readObject(java.io.ObjectInputStream p)
        throws java.io.IOException, ClassNotFoundException
        {
        pleaseDie = p.readBoolean();
        destroysThreads = p.readBoolean();
        numThreads = p.readInt();
        // don't write operating
        // dont' write threads
        // rebuild locks
        operatingLock = new Object();
        }
        
    public Steppable getCleaner()
        {
        return new Steppable() { public void step(SimState state) { cleanup(); } };
        }
                
    /** Call this just before you get rid of a ParallelSequence: for example, one good place is the stop() method of
        your simulation.  Never call this method inside the ParallelSequence's own step() method.  This method
        deletes the threads so the ParallelSequence is ready to be thrown away.  We also do this in
        finalize() but finalize() is not guaranteed to be called at any particular time, which can result in
        unexpected memory leaks.  Think of this method as the same kind of thing as a Graphics or 
        Window's dispose() method.  */
    public void cleanup()
        {
        pleaseDie = true;
        if (threads != null)
            threads.killThreads();
        pleaseDie = false;
        threads = null;
        }

    protected void finalize() throws Throwable
        {
        try { cleanup(); } 
        finally { super.finalize(); }
        }

    /** Creates an immutable ParallelSequence with the specified number of threads, or if threads==ParallelSequence.CPUS, then the number of threads is determined
        at runtime based on the number of CPUs or cores on the system, or if threads == ParallelSequence.STEPPABLES, then the number of threads
        is the size of the steps array passed in. */
    public ParallelSequence(Steppable[] steps, int threads)
        {
        super(steps);
        numThreads = threads;
        }

    /** Creates an immutable ParallelSequence with one thread per steppable. */
    public ParallelSequence(Steppable[] steps)
        {
        this(steps, STEPPABLES);
        }

    /** Creates an immutable ParallelSequence with the specified number of threads, or if threads==ParallelSequence.CPUS, then the number of threads is determined
        at runtime based on the number of CPUs or cores on the system, or if threads == ParallelSequence.STEPPABLES, then the number of threads
        is the size of the collection passed in (and may change as the collection grows or shrinks). */
    public ParallelSequence(Collection steps, int threads)
        {
        super(steps);
        numThreads = threads;
        }

    /** Creates an immutable  ParallelSequence with one thread per steppable in the collection. */
    public ParallelSequence(Collection steps)
        {
        this(steps, STEPPABLES);
        }

    protected boolean canEnsureOrder() { return false; }

    // steps once (in parallel) through the steppable things
    public void step(final SimState state)
        {
        // just to be safe, we'll avoid the HIGHLY unlikely race condition of being stepped in parallel or nested here
        synchronized(operatingLock)  // some random object we own
            {
            if (operating) 
                throw new RuntimeException("ParallelSequence stepped, but it's already in progress.\n" +
                    "Probably you have the same ParallelSequence nested, or the same ParallelSequence being stepped in parallel.\n" +
                    "Either way, it's a bug.");
            operating = true;
            }

        loadSteps();

        if (threads == null)  // rebuild threads
            threads = new ThreadPool();

        // How many threads?
        int size = this.size;
        int n = numThreads;
        if (n == CPUS)
            n = availableProcessors;
        else if (n == STEPPABLES)
            n = size;
        if (n > size)
            n = size;
        
        int jump = size / n;
        for(int i = 0; i < n; i++)
            {
            // The commented out code interleaves the threads with regard to the steppables,
            // which is a bad idea with multiple CPUs because it causes cache contention (generally
            // you want to have different CPUs working on completely different areas of memory). 
            // But in fact it makes almost no difference at all.
            
            // this.threads.startThread(new Worker(state, i, numSteps, n));
            
            // This code instead starts each thread on a different chunk of the steppables array
            this.threads.startThread(new Worker(state, i * jump, Math.min( (i+1) * jump, size), 1),
                "ParallelSequence");
            }

        if (destroysThreads)
            cleanup();
        else
            threads.joinThreads();

        // don't need to synchronize to turn operating off
        operating = false;
        }


    // a worker is a semaphore and also implements a runnable
    class Worker implements Runnable
        {
        SimState state;
        int start;
        int end;
        int modulo;
        public Worker(SimState state, int start, int end, int modulo)
            {
            this.state = state;
            this.start = start;
            this.end = end;
            this.modulo = modulo;
            }
        
        public void run()
            {
            Steppable[] steps = ParallelSequence.this.steps;
            int modulo = this.modulo;
            for(int s = start; s < end; s += modulo)
                {
                if (pleaseDie) break;
                Steppable step = steps[s];
                assert sim.util.LocationLog.set(step);
                steps[s].step(state);
                assert sim.util.LocationLog.clear();
                }
            }

        // explicitly state a UID in order to be 'cross-platform' serializable 
        // because we ARE an inner class and compilers come up with all sorts
        // of different UIDs for inner classes and their parents.
        private static final long serialVersionUID = 1;
        }
        
    // explicitly state a UID in order to be 'cross-platform' serializable
    // because we contain an inner class and compilers come up with all
    // sorts of different UIDs for inner classes and their parents.
    private static final long serialVersionUID = 1;
    }
    

// Why do we have a ThreadPool object here instead of using Java's thread
// pool facility in java.util.concurrent?  Two reasons.  First, this is
// faster and far simpler.  Second, java.util.concurrent has ridiculous
// design errors in it -- it's nearly impossible to do basic tasks like
// join threads etc.  Was Sun's code written by monkeys?  It appears so.
// Seriously, how screwed up a company do you have to be to mess up a
// ** thread pool ** ?


class ThreadPool
    {
    class Node implements Runnable
        {
        boolean die = false;
        boolean go = false;
        public Thread thread;
        public Runnable toRun;
            
        public Node(String name) 
            {
            thread = new Thread(this); 
            thread.setDaemon(true);
            thread.setName(name);
            }
            
        public void run()
            {
            while(true)
                {
                synchronized(this) 
                    {
                    while(!go && !die)
                        {
                        try { wait(); }
                        catch (InterruptedException e) { } // ignore
                        }
                    go = false;
                    if (die) { die = false; return; }
                    }
                toRun.run();
                toRun = null;
                // add myself back in the list
                synchronized(threads)
                    {
                    threads.add(this);  // adds to the tail
                    if (totalThreads == threads.size())  // we're all in the bag, let the pool know if it's joining
                        threads.notify();
                    }
                // let the pool know I'm home
                }
            }
        }
        
    LinkedList threads = new LinkedList();
    int totalThreads = 0;  // synchronize on threads first
                
    // Joins and kills all threads, both those running and those sitting in the pool
    void killThreads()
        {
        synchronized(threads)
            {
            joinThreads();
            while(!threads.isEmpty())
                {
                Node node = (Node)(threads.remove());  // removes from head
                synchronized(node) { node.die = true; node.notify(); }  // reel it in
                try { node.thread.join(); }
                catch (InterruptedException e) { } // ignore
                totalThreads--;
                }
            }
        }
            
    // Waits for all presently running threads to complete
    void joinThreads()
        {
        synchronized(threads)
            {
            while(totalThreads > threads.size())  // there are still outstanding threads
                try { threads.wait(); }
                catch (InterruptedException e) { }  // ignore
            }
        }
        
    // Starts a thread running on the given runnable
    void startThread(Runnable run) { startThread(run, "ParallelSequence"); }
        
    void startThread(Runnable run, String name)
        {
        Node node;
        // ensure we have at least one thread
        synchronized(threads) 
            {
            if (threads.isEmpty())
                {
                node = new Node(name + " Thread " + totalThreads);
                node.thread.start();
                totalThreads++;
                }
            else  // pull a thread
                {
                node = (Node)(threads.remove());  // removes from the head
                }
            }
        synchronized(node) { node.toRun = run; node.go = true; node.notify(); }  // get it running
        }

    private static final long serialVersionUID = 1;
    }
    