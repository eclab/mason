/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

/** Fires up a separate thread which runs until the simulation model requests it be halted.
    This mechanism makes possible parallel threads which run in the background independently
    of the schedule being stepped.  Note that the use of such threads makes the simulation
    unable to guarantee replicability.
    
    <p>Like all multithreaded stuff, AsynchronousSteppables are inherently dangerous and not to be trifled with: they
    access data at the same time as other threads, and so must deal with locking.  In general
    if you lock on the Schedule, you are guaranteed atomic access to the underlying simulation
    model.  You'll need to do this for even basic things such as accessing the random number
    generator.  Locking on the Schedule is fairly course-grained, however: the simulation model
    obtains a lock on the Schedule for the whole duration of a Schedule's step.  Instead you
    might create your own lock shared between the AsynchronousSteppable and the main thread which
    allows access to some piece of data you both need in a more fine-grained fashion.  In this case,
    make certain that the GUI isn't trying to read that data (to display it, say), or that the GUI
    obtains a lock when it needs to as well.  If you have no idea what we're talking about:
    don't use an AsynchronousSteppable.
    
    <p>When an AsynchronousSteppable is stepped, it fires off a thread which performs the asynchronous
    task.  This task could be an infinite loop (or otherwise very long process) 
    or it could be a short one-shot thing which runs and ends.
    Infinite loops can be paused and resumed (for checkpointing) and they can be stopped entirely.
    
    <p>AsynchronousSteppables automatically register themselves to be stopped at the end of the simulation
    (and when stopped, they unregister themselves).
    But if the task is an infinite loop, it's possible you may wish to stop the loop before the simulation
    ends, perhaps at an agreed-upon point in the schedule.  The easiest way to do this is to get a stopper()
    and schedule it on the schedule, along these lines:
    
    <pre><tt>
    *   AsynchronousSteppable s = ...
    *   Steppable stopper = s.stopper();
    *   schedule.scheduleOnce(s....);
    *   schedule.scheduleOnce(stopper....);
    </tt></pre>

    <p>If the task is a SHORT, one-shot process and the user can reasonably wait for the task to complete 
    after he has presed the 'stop' button, then run(false) should perform the asynchronous task, 
    run(true) should be set to do nothing, and halt(true) and halt(false) should both do nothing.
    Here's some code to show how to form such a beast.

    <pre><tt>
    *   AsynchronousSteppable s = new AsynchronousSteppable()
    *       {
    *       protected void run(boolean resuming)
    *           {
    *           if (!resuming)
    *               {
    *               // do your stuff here
    *               }
    *           }
    *
    *       protected void halt(boolean pausing) { } // nothing
    *       };
    </tt></pre>
    
    <p>If the task is an infinite loop or otherwise long process, 
    it needs to be pausable, resumable, and haltable.  In this case,
    run(false) should perform the asynchronous task, halt(false) and halt(true) should both cause the thread
    to die or trigger events which will soon lead to thread death halt(...) returns, and run(true) should
    fire up the task loop again after it had been halted with halt(true).  The most common situation is where
    you don't distinguish between your thread being killed temporarily or being killed permanently.  Here's
    some code for this situation:
    
    <pre><tt>
    *   AsynchronousSteppable s = new AsynchronousSteppable()
    *       {
    *       boolean shouldQuit = false;
    *       double[] shouldQuitLock = new double[1]; // an array is a unique, serializable object
    *
    *       boolean shouldQuit()
    *           {
    *           synchronized(shouldQuitLock) { return shouldQuit; }
    *           }
    *
    *       protected void run(boolean resuming)
    *           {
    *           while(!shouldQuit())
    *               {
    *               // do your stuff here -- assuming it doesn't block...
    *               }
    *           // we're quitting -- do cleanup here if you need to
    *
    *           // now reset our flag
    *           shouldQuit = false;
    *           }
    *
    *       protected void halt(boolean pausing)
    *           {
    *           synchronized(shouldQuitLock) { shouldQuit = val; }
    *           }
    *       };
    </tt></pre>

    <p>Let's say the task needs to distinguish between being paused and being quit.  In this case
    you need a custom way of handling pausing and knowing that you're resuming (perhaps to save state
    away).  Here's some code for this situation:

    <pre><tt>
    *   AsynchronousSteppable s = new AsynchronousSteppable()
    *       {
    *       boolean shouldQuit = false;
    *       double[] shouldQuitLock = new boolean[1]; // an array is a unique, serializable object
    *       boolean shouldPause = false;
    *       double[] shouldPauseLock = new boolean[1]; // an array is a unique, serializable object
    *
    *       boolean shouldQuit()
    *           {
    *           synchronized(shouldQuitLock) { return shouldQuit; }
    *           }
    *
    *       boolean shouldPause()
    *           {
    *           synchronized(shouldPauseLock) { return shouldPause; }
    *           }
    *
    *       protected void run(boolean resuming)
    *           {
    *           if (resuming)
    *               {
    *               // we're resuming from a pause -- re-set up here if you have to
    *               }
    *           else // (!resuming)
    *               {
    *               // we're starting fresh -- set up here if you have to
    *               }
    *
    *           while(!shouldQuit() && !shouldPause())
    *               {
    *               // do your stuff here -- assuming it doesn't block...
    *               }
    *
    *           if (shouldPause())
    *               {
    *               // we're pausing -- do cleanup here if you need to
    *               }
    *           else // if (shouldQuit())
    *               {
    *               // we're quitting -- do cleanup here if you need to
    *               }
    *
    *           // now reset our flags
    *           shouldPause = false;
    *           shouldQuit = false;
    *           }
    *
    *       protected void halt(boolean pausing)
    *           {
    *           if (pausing) synchronized(shouldPauseLock) { shouldPause = val; }
    *           else synchronized(shouldQuitLock) { shouldQuit = val; }
    *           }
    *       };
    </tt></pre>


*/

public abstract class AsynchronousSteppable
// implements Asynchronous
    {
    Thread thread;
    boolean running = false;
    boolean paused = false;
    protected SimState state;
    
    /** This method should enter the parallel thread's loop.  If resuming is true, then you may assume
        the parallel steppable is being resumed in the middle of a simulation after being paused (likely to checkpoint),
        as opposed to being started fresh.  */
    protected abstract void run(boolean resuming);
    
    /** This method should cause the loop created in run(...) to die.  If pausing is true, then you may assume
        the parallel steppable is being paused in the middle of a simulation (likely to checkpoint),
        as opposed to being entirely stopped due to the end of the simulation.  */
    protected abstract void halt(boolean pausing);
    
    /** Fires up the AsynchronousSteppable and registers it with the SimState.
        If it's already running, nothing happens. */
    public final synchronized void step(SimState state)
        {
        if (running) return;
        running = true;
        this.state = state;
        state.addToAsynchronousRegistry(this);
        thread = new Thread(new Runnable() { public void run() { AsynchronousSteppable.this.run(false); } });
        thread.start();
        }
    
    /** Requests that the AsynchronousSteppable shut down its thread, and blocks until this occurs. If it's already stopped, nothing happens. */
    public final synchronized void stop()
        {
        if (!running) return;
        halt(false);
        try { thread.join(); }
        catch (InterruptedException e) { System.err.println("AsynchronousSteppable interrupted while trying to stop its underlying thread.  Thread may never stop now."); }
        state.removeFromAsynchronousRegistry(this);
        running = false;
        }
    
    /** Requests that the AsynchronousSteppable shut down its thread (temporarily) and blocks until this occurs. If it's already paused or not running, nothing happens.  */
    public final synchronized void pause()
        {
        if (paused || !running) return;
        halt(true);
        try { thread.join(); }
        catch (InterruptedException e) { System.err.println("AsynchronousSteppable interrupted while trying to stop its underlying thread.  Thread may never stop now."); }
        paused = true;
        }
        
    /** Fires up the AsynchronousSteppable after a pause().
        If it's already unpaused or not running, nothing happens. */
    public final synchronized void resume()
        {
        if (!paused || !running) return;
        paused = false;
        thread = new Thread(new Runnable() { public void run() { AsynchronousSteppable.this.run(true); } });
        thread.start();
        }
    
    
    /// Threads are not serializable, so we must manually rebuild here
    private void writeObject(java.io.ObjectOutputStream p)
        throws java.io.IOException
        {
        p.writeBoolean(running);
        p.writeBoolean(paused);
        p.writeObject(state);
        }
        
    /// Threads are not serializable, so we must manually rebuild here
    private void readObject(java.io.ObjectInputStream p)
        throws java.io.IOException, ClassNotFoundException
        {
        running = p.readBoolean();
        paused = p.readBoolean();
        state = (SimState)(p.readObject());
        }
        
    protected void finalize() throws Throwable
        {
        try { stop(); } 
        finally { super.finalize(); }
        }
        
    /** Call this method to get a Steppable, which when called, executes top() on the AsynchornousSteppable.
        You can then schedule this Steppable to occur at some point in the future on a schedule. */
    public final Steppable stopper()
        {
        return new Steppable() { public void step(SimState state) { stop(); } };
        }
    }
    
