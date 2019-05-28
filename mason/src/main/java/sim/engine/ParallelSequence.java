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

            // we load the steps inside the lock here so we can guarantee
            // people aren't trying to add or remove stuff
            loadSteps();
            }

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
        int extra = size % n;
        int current = 0;
        
        // AT THIS POINT:
        // size is the total number of steppables
    	// n is the total number of threads
        // jump is the nominal number of steppables each thread must do
        // extra is the left over steppables -- we'll share 1 each of these with the early threads
        // current is the current position where the next thread should start its stepping 
        
        // FOR EXAMPLE
        // size = 17
        // n = 3
        // jump = 17 / 3 = 5
        // extra= 17 % 3 = 2
        // thread 0 : 0 to 6  (extra -> 1
        // thread 1 : 6 to 12 (extra -> 0)
        // thread 3 : 12 to 17 (extra = 0)
        
        // ANOTHER EXAMPLE
        // size = 18
        // n = 3
        // jump = 18 / 3 = 6
        // extra = 18 % 3 = 0
        // thread 0 : 0 to 6 (extra = 0)
        // thread 1 : 6 to 12 (extra = 0)
        // thread 2 : 12 to 18 (extra = 0)
        
        // ANOTHER EXAMPLE
        // size = 16
        // n = 3
        // jump = 16 / 3 = 5
        // extra = 18 % 3 = 1
        // thread 0 : 0 to 6 (extra -> 0)
        // thread 1 : 6 to 11 (extra = 0)
        // thread 2 : 11 to 16 (extra = 0)
        
        Runnable[] workers = new Runnable[n];
    	for(int i = 0; i < n; i++)
            {
            if (extra > 0)
            	{
            	workers[i] = new Worker(state, current, current + jump + 1, 1);
            	current += (jump + 1);
            	extra--;
            	}
            else
            	{
            	workers[i] = new Worker(state, current, current + jump, 1);
            	current += jump;
            	}
            }

        this.threads.startThreads(workers, "ParallelSequence");

        if (destroysThreads)
            cleanup();
        else
            threads.joinThreads();

        // don't need to synchronize to turn operating off
        operating = false;
        }


    public void replaceSteppables(Collection collection)
        {
        synchronized(operatingLock)
            {
            super.replaceSteppables(collection);
            }
        }

    public void replaceSteppables(Steppable[] steppables)
        {
        synchronized(operatingLock)
            {
            super.replaceSteppables(steppables);
            }
        }

    public void addSteppable(Steppable steppable)
        {
        synchronized(operatingLock)
            {
            super.addSteppable(steppable);
            }
        }

    public void addSteppables(Steppable[] steppables)
        {
        synchronized(operatingLock)
            {
            super.addSteppables(steppables);
            }
        }

    public void addSteppables(Collection steppables)
        {
        synchronized(operatingLock)
            {
            super.addSteppables(steppables);
            }
        }

    public void removeSteppable(Steppable steppable)
        {
        synchronized(operatingLock)
            {
            super.removeSteppable(steppable);
            }
        }

    public void removeSteppables(Steppable[] steppables)
        {
        synchronized(operatingLock)
            {
            super.removeSteppables(steppables);
            }
        }

    public void removeSteppables(Collection steppables)
        {
        synchronized(operatingLock)
            {
            super.removeSteppables(steppables);
            }
        }

    public boolean getEnsuresOrder() 
        {
        synchronized(operatingLock)
            {
            return super.getEnsuresOrder();
            }
        }
        
    public void setEnsuresOrder(boolean val)
        {
        synchronized(operatingLock)
            {
            super.setEnsuresOrder(val);
            }
        }
        
    public boolean getUsesSets() 
        {
        synchronized(operatingLock)
            {
            return super.getUsesSets();
            }
        }

    public void setUsesSets(boolean val) 
        { 
        synchronized(operatingLock)
            {
            super.setUsesSets(val);
            }
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
    
// Here we use our own thread pool.  This pool is constructed so that we
// can fire off N threads with a minimum of locking and wait()ing, which
// just kills us when we have lots of very short-length jobs as is the case
// for something like a ParallelSequence.

class ThreadPool
    {
    // object for notifying threads to all start.  This lets us do a notifyAll() in bulk
    // rather than separate notify()s on each of the threads, which is very costly.
    Object[] all = new Object[0];
    
    // Thread pool
    ArrayList threads = new ArrayList();
    int totalThreads = 0;
                
	// holds a thread
    class Node implements Runnable
        {
        volatile boolean die = false;  // raised when the Node is asked to kill its thread and die.
        volatile boolean go = false;  // raised when the Node is asked to have its thread run the runnable toRun.
        volatile public Runnable toRun;  // the runnable to run
        public Thread thread;
            
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
                // this is outside because these are booleans and are atomic, and it
                // doesn't matter anyway because they're READ here and only WRITTEN
                // elsewhere.  It gives us a small chance of escaping the synchronization
                // immediately below.
                if (!go && !die)
                	{
		            synchronized(all) 
	                	{
	                	while (!go && !die)
							{
		                	try { all.wait(0); }
		                	catch (InterruptedException e) { } // ignore
		                	}
	                    }
	                }

	            // at this point either go or die has been raised and toRun won't be updated again
	            // until after adding back into the list, so we can access them here safely without synchronization
	            
				if (die) { die = false; return; }
				go = false;
                toRun.run();
                
                // add myself back in the list
                synchronized(threads)
                    {
                    threads.add(this);  // adds to the head -- it seems we get a 20% performance boost pulling hot threads from the head when doing nothing with them.
                    if (totalThreads == threads.size())  // we're all in the bag, let the pool know if it's joining
                        threads.notify();
                    }
                }
            }
        }
    
    
    // Joins and kills all threads, both those running and those sitting in the pool
    void killThreads()
        {
        synchronized(threads)
            {
            joinThreads();
            
            // at this point size == totalthreads
            int size = threads.size();
            
            for(int i = 0; i < size; i++)
                {
                Node node = (Node)(threads.get(i));
                node.die = true;  // it's okay if this isn't synchronized
                }
                
            // wake up threads to die
            synchronized(all) { all.notifyAll(); }

            for(int i = 0; i < size; i++)
                {
                Node node = (Node)(threads.remove(size - i - 1));
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
                try { threads.wait(0); }
                catch (InterruptedException e) { }  // ignore
            }
        }
        
    
    void startThreads(Runnable[] run, String name)
    	{
        Node[] nodes = new Node[run.length];
        
        // we're going to do this in bulk rather than individually, so
        // we need to first gather all the needed threads into nodes
        synchronized(threads) 
            {
            int available = threads.size();
            for (int i = 0; i < run.length; i++)
            	{
				if (available == 0)
					{
					nodes[i] = new Node(name + " " + totalThreads);
                   	nodes[i].toRun = run[i]; 
                	nodes[i].go = true; 
					
                    nodes[i].thread.start();  // since go is already set, this thread won't even bother to wait()
					totalThreads++;
					}
				else  // pull a thread
					{
					nodes[i] = (Node)(threads.remove(available-1));  // removes from the head
					
					// this can be done without synchronization on the node
					// because the node is waiting for go or die to be true
					// before it accesses toRun at this stage
					nodes[i].toRun = run[i]; 

					// this may cause the node to prematurely fire without waiting on 'all',
					// but that's a good thing.
					nodes[i].go = true; 
					available--;
					}
				}
            }
        
        synchronized(all) 
        	{
	       	// get all the nodes going
	       	all.notifyAll();
			}
    	}

    private static final long serialVersionUID = 1;
    }
    

/****
	Thread Pool using only Spin-waits (ugh, I know).
	This is for testing and experiment only -- Sean

// Here we use our own thread pool.  This pool is constructed so that we
// can fire off N threads with a minimum of locking and wait()ing, which
// just kills us when we have lots of very short-length jobs as is the case
// for something like a ParallelSequence.

class ThreadPool
    {
	// the main thread waits on threads.
	// the individual threads wait on all
    Object[] all = new Object[0];
	    
    // Thread pool
    ArrayList threads = new ArrayList();
    int totalThreads = 0;
    
	// holds a thread
    class Node implements Runnable
        {
        volatile boolean die = false;  // raised when the Node is asked to kill its thread and die.
        volatile boolean go = false;  // raised when the Node is asked to have its thread run the runnable toRun.
        volatile public Runnable toRun;  // the runnable to run
        volatile public int index;
        public Thread thread;
        
        public Node(String name, int index) 
            {
            thread = new Thread(this); 
            thread.setDaemon(true);
            thread.setName(name);
            this.index = index;
            }
        
    	volatile boolean notifyRaised = false;
        public boolean dowait()
        	{
			while (!notifyRaised);
        	notifyRaised = false;         	
			return true;
        	}
        
		public void donotify()
			{
			mainNotifyRaised = true;
			}
	

        public void run()
            {
            while(true)
                {
                // this is outside because these are booleans and are atomic, and it
                // doesn't matter anyway because they're READ here and only WRITTEN
                // elsewhere.  It gives us a small chance of escaping the synchronization
                // immediately below.
	            while (!go && !die)
					dowait();

	            // at this point either go or die has been raised and toRun won't be updated again
	            // until after adding back into the list, so we can access them here safely without synchronization
	            
				if (die) { die = false; return; }
				go = false;
                toRun.run();
                
                threads.set(index, this);
                
                boolean last = false;
                synchronized(threads)
                    {
                    last = ((--outstanding) == 0);
                	}
                if (last)
                	donotify();
                }
            }
        }
    
	public void donotify(int upto)
		{
		for(int i = 0; i < upto; i++)
			((Node)(threads.get(i))).notifyRaised = true;
		}
	
	volatile boolean mainNotifyRaised = false;
	public boolean dowait()
		{
        while(!mainNotifyRaised);
		mainNotifyRaised = false;
		return true; 
		}
		
            
    // Joins and kills all threads, both those running and those sitting in the pool
    void killThreads()
        {
        joinThreads();
        
        int size = threads.size();
        
        for(int i = 0; i < size; i++)
			{
			Node node = (Node)(threads.get(i));
			node.die = true;  // it's okay if this isn't synchronized
			}
        
        // wake up threads to die
        donotify(size);

        for(int i = 0; i < size; i++)
            {
			Node node = (Node)(threads.get(i));
            try { node.thread.join(); }
            catch (InterruptedException e) { } // ignore
            }
        
        threads.clear();
        }
            
    // Waits for all presently running threads to complete
    void joinThreads()
        {
		boolean notdone = false;
        while(!dowait());
        }
        
    volatile int outstanding = 0;
    
    void startThreads(Runnable[] run, String name)
    	{
    	// this is only called when we have NO threads running
    	int size = threads.size();
    	outstanding = run.length;
    	int upto = Math.min(size, outstanding);

		// start ready threads
    	for(int i = 0; i < upto; i++)
    		{
    		Node node = (Node)(threads.get(i));
    		node.toRun = run[i];
    		node.go = true;
    		}
    		
    	donotify(upto);
    	
    	// build new threads
    	for(int i = upto; i < outstanding; i++)
    		{
    		Node node = new Node(name + " " + i, i);
    		threads.add(node);
    		node.toRun = run[i];
    		node.go = true;
    		node.thread.start();  // since go is already set, this thread won't even bother to wait(), no need to notify
    		}
    	}

    private static final long serialVersionUID = 1;
    }
*****/

