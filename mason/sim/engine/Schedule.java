/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;
import java.io.Serializable;

import sim.util.*;
import ec.util.*;

/**
   Schedule defines a threadsafe scheduling queue in which events can be scheduled to occur
   at future time.  The time of the most recent event which has already occured
   is given by the <b>getTime()</b> method.  If the current time is <tt>BEFORE_SIMULATION</tt> (defined
   to be <tt>EPOCH - 1</tt>),
   then the schedule is set to the "time before time" (the schedule hasn't started running
   yet).  If the current time is <tt>AFTER_SIMULATION</tt> (positive infinity), then the schedule has run
   out of time.  <tt>EPOCH</tt> (0.0) is defined as the first timestep for which you can legally schedule a value.
   <tt>EPOCH_PLUS_ESPILON</tt> is defined as the smallest possible second timestep for which you can legally sechedule a value.
   If you're scheduling events to occur on integer timesteps, you may want to ensure that your simulation 
   does not run beyond <tt>MAXIMUM_INTEGER</tt> (9007199254740992L or 9.007199254740992E15).  For values of a 
   double d >= <tt>MAXIMUM_INTEGER</tt>, d + 1 == d !

   <p>An event is defined as a <b>Steppable</b> object. You can schedule events to either 
   occur a single time or to occur repeatedly at some interval.  If the event occurs repeatedly,
   the schedule will provide you with a <b>Stoppable</b> object on which you can call <b>stop()</b>
   to cancel all future repeats of the event.  If instead you wish to "stop" a single-time event from occuring
   before its time has come, you should do so through the use of a <b>TentativeStep</b> object.  At present
   you cannot delete objects from the Schedule -- just stop them and let them drop out in due course.
   
   <p>The schedule is pulsed by calling its <b>step(...)</b> method.  Each pulse, the schedule
   finds the minimum time at which events are scheduled, moves ahead to that time, and then calls
   all the events scheduled at that time.    Multiple events may be scheduled for the same time.
   No event may be scheduled for a time earlier than getTime().  If at time getTime() you schedule a new
   event for time getTime(), then actually this event will occur at time getTime()+epsilon, that is, the
   smallest possible slice of time greater than getTime().

   <p><b>IMPORTANT NOTE:</b> we have disabled the setShuffling() procedure by making the methods private.  The reason for this is that although turning off shuffling causes the Steppables to be stepped in a <i>predictable order</i>, they will not necessarily be stepped in <i>the order in which they were submitted</i>, which was the whole point of the methods.  The reason for this is that a binary heap is not "stable": it doesn't break ties by returning elements in the same order in which they appeared.  This potentially could cause bugs in simulations and we want to make it very clear.
  
   <p>Events at a step are further subdivided and scheduled according to their <i>ordering</i>, an integer.
   Objects for scheduled for lower orderings for a given time will be executed before objects with
   higher orderings for the same time.  If objects are scheduled for the same time and
   have the same ordering value, their execution will be randomly ordered with respect to one another.
   
   <!-- 
   unless (in the very rare case) you have called setShuffling(false);.  Generally speaking, most experiments with
   good model methodologies will want random shuffling left on, and if you need an explicit ordering, it may be
   better to rely on Steppable's orderings or to use a Sequence.
   -->
   
   <p>You might be wondering: why bother with using orderings?  After all, can't you achieve the same thing by just
   stretching elements out in time?  There are two reasons to use orderings.  First, it allows you to use the getTime()
   method to keep tabs on the current time in a way that might be convenient to you.  But second and more importantly,
   MASON's GUI facility will update its displays and inspectors only after all Steppables scheduled for a 
   given timestamp have completed, and so orderings give you a way of subdividing the interval of time between
   GUI updates.
   
   <p>A schedule may be <i>sealed</i> meaning that it will refuse to accept any further scheduled events
   even if its time is not yet AFTER_SIMULATION.  This is largely done internally by MASON code: you probably
   will never want to do this.  Once a schedule is sealed it cannot be unsealed until it is reset().
   
   <p>You can clear out the entire Schedule, unseal it, and restart it to BEFORE_SIMULATION by calling reset().
   However, this does not prevent AsynchronousSteppables from suddenly rescheduling themselves
   in the queue.  Stopping the simulation from within a Steppable object's step() method is best done by
   calling SimState.kill().  From the main thread, the most straightforward way to stop a simulation is to just
   stop calling schedule.step(...), and proceed directly to SimState.finish().
   
   <p>You can get the number of times that step(...) has been called on the schedule by calling the getSteps() method.
   This value is incremented just as the Schedule exits its step(...) method and only if the method returned true.
   Additionally, you can get a string version of the current time with the getTimestamp(...) method.
   
   <p><b>Note on Synchronization</b>.  In order to maximize the ability for threads to access the Schedule at any time, 
   Schedule uses two locks for synchronization.  First, the <b>step() method synchronizes on the Schedule</b> itself.  This
   prevents step() from being called simultaneously from different threads; also step() tests to make sure that it's not
   called reentrantly from within the same thread.  Second, <b>many methods synchronize on an internal lock</b>, including step().
   This allows step() to synchronize on the lock only to suck out the relevant Steppables from the Heap and to advance the timestep;
   all other portions of step() are outside of the lock.  Thus when step() actually steps the Steppables, even in different threads
   (like AsynchronousSteppable or ParallelSequence), they can turn around and submit step-requests to the Schedule even while it's still
   in its step() method.
   
   <p>One downside to this flexibility is that it's very inefficient to check, at each step of a Steppable, whether the Schedule
   has been reset or not.  Thus now if you call reset() or [better] SimState.kill(), the Schedule will continue to step Steppables
   until it has exhausted ones scheduled for the current timestep.  Only at that point will it cease.
   
   <p><b>Heaps and Calendar Queues</b>.  Schedule uses a plain-old binary heap for its queueing mechanism.  This is reasonably efficient,
   but it could be made more efficient with a Calendar Queue designed for the purposes of your simulation.  We settled on a Heap because
   we do not know what the expected scheduling pattern will be for any given simulation, and so had to go for the most general case.  If you'd
   care to customize your queue, you can do so by overriding the createHeap() method in a custom Schedule.  We imagine this would be rare.
*/
    

public class Schedule implements java.io.Serializable
    {
    private static final long serialVersionUID = 1;

    /** The first possible schedulable time. */
    public static final double EPOCH = 0.0;
    /** The time which indicates that the Schedule hasn't started yet. Less than EPOCH. */
    public static final double BEFORE_SIMULATION = EPOCH - 1.0;
    /** The time which indicates that the Schedule is finished.  Equal positive infinity, and thus greater than any schedulable time. */
    public static final double AFTER_SIMULATION = Double.POSITIVE_INFINITY;
    /** The second possible schedulable time. */
    public static final double EPOCH_PLUS_EPSILON = Double.longBitsToDouble(Double.doubleToRawLongBits(EPOCH)+1L);
    /** The last time beyond which the schedule is no longer able to precisely maintain integer values due to loss of precision.  That is, MAXIMUM_INTEGER + 1.0 == MAXIMUM_INTEGER. */
    public static final double MAXIMUM_INTEGER = 9.007199254740992E15;

    // should we shuffle individuals with the same timestep and ordering?
    boolean shuffling = true;  // by default, we WANT to shuffle

    /** The Schedule's queue. */
    protected Heap queue = createHeap();
    
    /** Returns a Heap to be used by the Schedule.  By default, returns a
        binary heap.  Override this to provide your own
        subclass of Heap tuned for your particular problem. */
    protected Heap createHeap() { return new Heap(); }
    
    /** The current time, as returned by getTime().  
        If you modify this in a subclass, be sure to synchronize on Schedule.lock first. */
    protected double time;
    
    /** The current steps, as returned by getSteps().  
        If you modify this in a subclass, be sure to synchronize on Schedule.lock first. */
    protected long steps;
        
    /** Whether the schedule is sealed, as returned by isSealed().  
        If you modify this in a subclass, be sure to synchronize on Schedule.lock first. */
    protected boolean sealed = false;
                
    /** The schedule lock.  Many methods synchronize on this lock before modifying internal variables. */
    protected Object lock = new boolean[1];  // an array is a unique, serializable object
    
    /** Sets the schedule to randomly shuffle the order of Steppables (the default), or to not do so, when they
        have identical orderings and are scheduled for the same time.  If the Steppables are not randomly shuffled,
        they will be executed in the order in which they were inserted into the schedule, if they have identical
        orderings.  You should set this to
        FALSE only under unusual circumstances when you know what you're doing -- in the vast majority of cases you
        will want it to be TRUE (the default).  */
    private void setShuffling(boolean val)
        {
        synchronized(lock)
            {
            shuffling = val;
            }
        }
        
    /** Returns true (the default) if the Steppables' order is randomly shuffled when they have identical orderings
        and are scheduled for the same time; else returns false, indicating that Steppables with identical orderings
        will be executed in the order in which they were inserted into the schedule. */
    private boolean isShuffling()
        {
        synchronized(lock)
            {
            return shuffling;
            }
        }
        
    /** Creates a Schedule. */
    public Schedule()
        {
        time = BEFORE_SIMULATION;
        steps = 0;
        }
    
    /** Returns the current timestep 
        @deprecated use getTime()
    */
    public double time() { return getTime(); }

    /** Returns the current timestep */
    public double getTime() { synchronized(lock) { return time; } }
    
    /** Returns whether or not the schedule is sealed (nothing more can be scheduled, even 
        if the schedule isn't at AFTER_SIMULATION yet).   Calling reset() will unseal
        a Schedule, and calling seal() will seal it.  */
    public boolean isSealed() { synchronized(lock) { return sealed; } }
        
    /** Returns the current time in string format. If the time is BEFORE_SIMULATION, then beforeSimulationString is
        returned.  If the time is AFTER_SIMULATION, then afterSimulationString is returned.  Otherwise a numerical
        representation of the time is returned. */
    public String getTimestamp(final String beforeSimulationString, final String afterSimulationString)
        {
        return getTimestamp(getTime(), beforeSimulationString, afterSimulationString);
        }
    
    /** Returns a given time in string format. If the time is earlier than EPOCH (such as BEFORE_SIMULATION), then beforeSimulationString is
        returned.  If the time is AFTER_SIMULATION, then afterSimulationString is returned.  Otherwise a numerical
        representation of the time is returned. */
    // could be static, but why not let it be overridden?
    public String getTimestamp(double time, final String beforeSimulationString, final String afterSimulationString)
        {
        double _time = getTime();
        if (_time < EPOCH) return beforeSimulationString;
        if (_time >= AFTER_SIMULATION) return afterSimulationString;
        if (_time == (long)_time) return Long.toString((long)_time);
        return Double.toString(_time);
        }

    /** Returns the number of steps the Schedule has pulsed so far. */
    public long getSteps() { synchronized(lock) { return steps; } }

    // pushes the time to AFTER_SIMULATION and attempts to kill all
    // remaining scheduled items
    // @deprecated don't use this
    private void pushToAfterSimulation()
        {
        synchronized(lock)
            {
            time = AFTER_SIMULATION;
            queue = createHeap();  // let 'em GC  -- must be inside the lock so scheduleOnce doesn't try to add more
            }
        }

    /** Empties out the schedule but does not reset the time or steps.  If you're
        looking for a way to kill your simulation from a Steppable, use SimState.kill() instead.  Note that
        any agents presently at THIS TIME STEP will STILL be stepped -- including possibly reinserting themselves
        in the schedule.  */
    public void clear()
        {
        synchronized(lock)
            {
            queue = createHeap();  // let 'em GC  -- must be inside the lock so scheduleOnce doesn't try to add more
            }
        }

    /** Seals the schedule: after a schedule is sealed, no further Steppables may be scheduled on it. 
        To unseal a schedule, you must reset() it.  If you're looking for a way to kill your simulation
        from a Steppable, use SimState.kill() instead.  */
    public void seal()
        {
        synchronized(lock)
            {
            sealed = true;
            }
        }

    /** Empties out the schedule and resets it to a pristine state BEFORE_SIMULATION, with steps = 0.  If you're
        looking for a way to kill your simulation from a Steppable, use SimState.kill() instead.  */
    public void reset()
        {
        synchronized(lock)
            {
            time = BEFORE_SIMULATION;
            steps = 0;
            queue = createHeap();  // let 'em GC  -- must be inside the lock so scheduleOnce doesn't try to add more
            sealed = false;
            }
        }
    
    /** Returns true if the schedule has nothing left to do. */
    public boolean scheduleComplete()
        {
        synchronized(lock)
            {
            return queue.isEmpty();
            }
        }
    
    /**
       Merge a given schedule into this one.  The other schedule is not modified, but the queue of the
       original schedule is changed.  NOTE: this method is not threadsafe and should be only performed 
       when there are NO other threads which might want to manipulate the schedule.
    */
    public void merge(Schedule other) 
        {
        if (inStep || other.inStep) 
            throw new RuntimeException("May not merge with another schedule while inside a step method.");
        if (sealed || other.sealed)
            throw new RuntimeException("May not merge with a sealed schedule.");
        if (!other.queue.isEmpty())
            {
            double minKey = ((Key)(other.queue.getMinKey())).getTime();
            if (minKey <= getTime())  // uh oh
                throw new RuntimeException("May not merge with a schedule which has Steppables scheduled for an earlier time than my current time value."); 
            }
        
        queue = queue.merge(other.queue);
        }

    Bag currentSteps = new Bag();
    Bag substeps = new Bag();
    boolean inStep = false;  // prevents reentrancy
    /** Steps the schedule, gathering and ordering all the items to step on the next time step (skipping
        blank time steps), and then stepping all of them in the decided order.  
        Returns FALSE if nothing was stepped -- the schedule is exhausted or time has run out. */
    public synchronized boolean step(final SimState state)
        {
        if (inStep)  // check for reentrant calls and deny
            {
            throw new RuntimeException("Schedule.step() is not reentrant, yet is being called recursively.");
            }
            
        inStep = true;
        Bag currentSteps = this.currentSteps;  // locals are faster
        final MersenneTwisterFast random = state.random; // locals are faster
        
        int topSubstep = 0;  // we set this as a hack to avoid having to clear all the substeps each time until the very end

        // grab the events as quickly as possible
        synchronized(lock)
            {
            if (time == AFTER_SIMULATION || queue.isEmpty())
                { time = AFTER_SIMULATION; inStep = false; return false; }  // bump the time for the queue.isEmpty() bit
            
            // now change the time
            time = ((Key)(queue.getMinKey())).time;  // key shouldn't be able to be null; time should always be one bigger

            final boolean shuffling = this.shuffling; // locals are faster.  This one needs to be synchronized inside lock

            // grab all of the steppables in the right order.  To do this, we employ two Bags:
            // 1. Each iteration of the while-loop, we grab all the steppables of the next ordering, put into the substeps Bag
            // 2. Next we either shuffle or reverse the substeps.
            // 3. Next we add them all to the end of the currentSteps Bag
            // 4. Then we clear the substeps bag, but we don't let them GC yet
            // 5. Last, out of the while-loop, we clear the substeps bag "for real", allowing them to GC
            while(true)
                {
                // Suck out the contents of the next ordering
                queue.extractMin(substeps);  // come out in reverse order

                // shuffle
                if (substeps.numObjs > 1) 
                    {
                    if (shuffling) substeps.shuffle(random);  // no need to flip -- we're randomizing
                    else substeps.reverse();  // they came out in reverse order; we need to flip 'em
                    }
                                                                
                // dump
                if (topSubstep < substeps.numObjs) topSubstep = substeps.numObjs;  // remember index of largest substep since we're violating clear()
                currentSteps.addAll(substeps);
                substeps.numObjs = 0;  // temporarily clear
                
                // check next key and break if we don't need to go on
                Key currentKey = (Key)(queue.getMinKey());
                if (currentKey == null || currentKey.time != time) break;  // looks like no more substeps at this timestamp
                }
            }
            
        // now finally clear out the substeps for real
        substeps.numObjs = topSubstep;
        substeps.clear();  // clear for real so everything can GC
                        
        // execute
        int len = currentSteps.numObjs;
        Object[] objs = currentSteps.objs;
        try
            {
            for(int x=0;x<len;x++)  // if we're not being killed...
                {
                assert sim.util.LocationLog.set(((Steppable)(objs[x])));
                ((Steppable)(objs[x])).step(state);
                assert sim.util.LocationLog.clear();
                objs[x] = null;  // let gc even if being killed
                }
            }
        finally
            {
            // reuse currentSteps -- all objects should have been released to gc already, no need to call clear()
            currentSteps.numObjs = 0;
                
            synchronized(lock) { steps++; }
            inStep = false;
            }
        return true;
        }
        
    /** Schedules the event to occur at getTime() + 1.0, 0 ordering. If this is a valid time
        and event, schedules the event and returns TRUE.
        This method at present returns FALSE if the schedule cannot
        schedule any more events (it's sealed or the time is AFTER_SIMULATION), or if the
        event is being scheduled for AFTER_SIMULATION.  The method 
        throws an IllegalArgumentException if the event is being scheduled for an invalid time, or is null. */
    
    // synchronized so getting the time can be atomic with the subsidiary scheduleOnce function call
    public boolean scheduleOnce(final Steppable event)
        {
        synchronized(lock)
            {
            return _scheduleOnce(new Key(/*must lock for:*/time +1.0,0),event);
            }
        }
    
    /** Schedules the event to occur at getTime() + delta, 0 ordering. If this is a valid time
        and event, schedules the event and returns TRUE.
        This method at present returns FALSE if the schedule cannot
        schedule any more events (it's sealed or the time is AFTER_SIMULATION), or if the
        event is being scheduled for AFTER_SIMULATION.  The method 
        throws an IllegalArgumentException if the event is being scheduled for an invalid time, or is null. */
    
    // synchronized so getting the time can be atomic with the subsidiary scheduleOnce function call
    public boolean scheduleOnceIn(final double delta, final Steppable event)
        {
        synchronized(lock)
            {
            return _scheduleOnce(new Key(/*must lock for:*/ time + delta, 0), event);
            }
        }
        
    /** Schedules the event to occur at getTime() + 1.0, and in the ordering provided. If this is a valid time
        and event, schedules the event and returns TRUE.
        This method at present returns FALSE if the schedule cannot
        schedule any more events (it's sealed or the time is AFTER_SIMULATION), or if the
        event is being scheduled for AFTER_SIMULATION.  The method 
        throws an IllegalArgumentException if the event is being scheduled for an invalid time, or is null. */
    
    // synchronized so getting the time can be atomic with the subsidiary scheduleOnce function call
    public boolean scheduleOnce(final Steppable event, final int ordering)
        {
        synchronized(lock)
            {
            return _scheduleOnce(new Key(/*must lock for:*/time +1.0,ordering),event);
            }
        }

    /** Schedules the event to occur at getTime() + delta, and in the ordering provided. If this is a valid time
        and event, schedules the event and returns TRUE.
        This method at present returns FALSE if the schedule cannot
        schedule any more events (it's sealed or the time is AFTER_SIMULATION), or if the
        event is being scheduled for AFTER_SIMULATION.  The method 
        throws an IllegalArgumentException if the event is being scheduled for an invalid time, or is null. */
    
    // synchronized so getting the time can be atomic with the subsidiary scheduleOnce function call
    public boolean scheduleOnceIn(final double delta, final Steppable event, final int ordering)
        {
        synchronized(lock)
            {
            return _scheduleOnce(new Key(/*must lock for:*/ time + delta, ordering), event);
            }
        }

    /** Schedules the event to occur at the provided time, 0 ordering.  If the getTime() == the provided
        time, then the event is instead scheduled to occur at getTime() + epsilon (the minimum possible next
        timestamp). If this is a valid time
        and event, schedules the event and returns TRUE.
        This method at present returns FALSE if the schedule cannot
        schedule any more events (it's sealed or the time is AFTER_SIMULATION), or if the
        event is being scheduled for AFTER_SIMULATION.  The method 
        throws an IllegalArgumentException if the event is being scheduled for an invalid time, or is null. */
    
    public boolean scheduleOnce(double time, final Steppable event)
        {
        synchronized(lock)
            {
            return _scheduleOnce(new Key(time,0),event);
            }
        }
        
    /** Schedules the event to occur at the provided time, and in the ordering provided.  If the getTime() == the provided
        time, then the event is instead scheduled to occur at getTime() + epsilon (the minimum possible next
        timestamp). If this is a valid time, ordering,
        and event, schedules the event and returns TRUE.
        This method at present returns FALSE if the schedule cannot
        schedule any more events (it's sealed or the time is AFTER_SIMULATION), or if the
        event is being scheduled for AFTER_SIMULATION.  The method 
        throws an IllegalArgumentException if the event is being scheduled for an invalid time, or is null. 
    */
    public boolean scheduleOnce(double time, final int ordering, final Steppable event)
        {
        synchronized(lock)
            {
            return _scheduleOnce(new Key(time,ordering),event);
            }
        }
    
    /** Schedules an item. 
        This method at present returns FALSE if the schedule cannot
        schedule any more events (it's sealed or the time is AFTER_SIMULATION), or if the
        event is being scheduled for AFTER_SIMULATION.  The method 
        throws an IllegalArgumentException if the event is being scheduled for an invalid time, or is null. */
    protected boolean scheduleOnce(Key key, final Steppable event)
        {
        synchronized(lock)
            {
            return _scheduleOnce(key, event);
            }
        }

    
    /** Schedules an item.  You must synchronize on this.lock before calling this method.   This allows us to avoid synchronizing twice,
        and incurring any overhead (not sure if that's an issue really).  This method at present returns FALSE if the schedule cannot
        schedule any more events (it's sealed or the time is AFTER_SIMULATION), or if the
        event is being scheduled for AFTER_SIMULATION.  The method 
        throws an IllegalArgumentException if the event is being scheduled for an invalid time, or is null. */
    boolean _scheduleOnce(Key key, final Steppable event)
        {
        // locals are a teeny bit faster
        double time = this.time;
        double t = key.time;

        // check to see if we're scheduling for the same exact time -- even if of different orderings, that doesn't matter
        if (t == time && t != AFTER_SIMULATION)
            // bump up time to the next possible item, unless we're at infinity already (AFTER_SIMULATION)
            t = key.time = Double.longBitsToDouble(Double.doubleToRawLongBits(t)+1L);

        if (sealed || t >= AFTER_SIMULATION)             // situations where no further events can be added
            {
            return false;
            }
        else if (t < EPOCH)
            throw new IllegalArgumentException("For the Steppable...\n\n"+event+
                "\n\n...the time provided ("+t+") is < EPOCH (" + EPOCH + ")");
        else if (t != t /* NaN */)
            throw new IllegalArgumentException("For the Steppable...\n\n"+event+
                "\n\n...the time provided ("+t+") is NaN");
        else if (t < time)
            throw new IllegalArgumentException("For the Steppable...\n\n"+event+
                "\n\n...the time provided ("+t+") is less than the current time (" + time + ")");
        else if (event == null)
            throw new IllegalArgumentException("The provided Steppable is null");
        
        queue.add(event, key);
        return true;
        }


    /** Schedules the event to recur at an interval of 1.0 starting at getTime() + 1.0, and at 0 ordering.
        If this is a valid event, schedules the event and returns a Stoppable, else returns null.
        The recurrence will continue until getTime() >= AFTER_SIMULATION, the Schedule is cleared out,
        or the Stoppable's stop() method is called, whichever happens first.

        <p>This method at present returns null if the schedule cannot
        schedule any more events (it's sealed or the time is AFTER_SIMULATION).  The method 
        throws an IllegalArgumentException if the event is being scheduled for an invalid time, or is null. 

        <p> Note that calling stop() on the Stoppable
        will not only stop the repeating, but will <i>also</i> make the Schedule completely
        forget (lose the pointer to) the Steppable scheduled here.  This is particularly useful
        if you need to make the Schedule NOT serialize certain Steppable objects. */
    
    // synchronized so getting the time can be atomic with the subsidiary scheduleRepeating function call
    public Stoppable scheduleRepeating(final Steppable event)
        {
        synchronized(lock)
            {
            return scheduleRepeating(/*must lock for:*/time +1.0,0,event,1.0);
            }
        }

    /** Schedules the event to recur at the specified interval starting at getTime() + interval, and at 0 ordering.
        If this is a valid interval (must be > 0)
        and event, schedules the event and returns a Stoppable, else returns null.
        The recurrence will continue until getTime() >= AFTER_SIMULATION, the Schedule is cleared out,
        or the Stoppable's stop() method is called, whichever happens first.

        <p>This method at present returns null if the schedule cannot
        schedule any more events (it's sealed or the time is AFTER_SIMULATION).  The method 
        throws an IllegalArgumentException if the event is being scheduled for an invalid time, or is null. 

        <p> Note that calling stop() on the Stoppable
        will not only stop the repeating, but will <i>also</i> make the Schedule completely
        forget (lose the pointer to) the Steppable scheduled here.  This is particularly useful
        if you need to make the Schedule NOT serialize certain Steppable objects. */
    
    // synchronized so getting the time can be atomic with the subsidiary scheduleRepeating function call
    public Stoppable scheduleRepeating(final Steppable event, final double interval)
        {
        synchronized(lock)
            {
            return scheduleRepeating(/*must lock for:*/time +interval,0,event,interval);
            }
        }

    /** Schedules the event to recur at the specified interval starting at getTime() + interval, and at the provided ordering.
        If this is a valid interval (must be > 0)
        and event, schedules the event and returns a Stoppable, else returns null.
        The recurrence will continue until getTime() >= AFTER_SIMULATION, the Schedule is cleared out,
        or the Stoppable's stop() method is called, whichever happens first.

        <p>This method at present returns null if the schedule cannot
        schedule any more events (it's sealed or the time is AFTER_SIMULATION).  The method 
        throws an IllegalArgumentException if the event is being scheduled for an invalid time, or is null. 

        <p> Note that calling stop() on the Stoppable
        will not only stop the repeating, but will <i>also</i> make the Schedule completely
        forget (lose the pointer to) the Steppable scheduled here.  This is particularly useful
        if you need to make the Schedule NOT serialize certain Steppable objects. */
    
    // synchronized so getting the time can be atomic with the subsidiary scheduleRepeating function call
    public Stoppable scheduleRepeating(final Steppable event, final int ordering, final double interval)
        {
        synchronized(lock)
            {
            return scheduleRepeating(/*must lock for:*/time +interval,ordering,event,interval);
            }
        }

    /** Schedules the event to recur at the specified interval starting at the provided time, and at 0 ordering.
        If the getTime() == the provided
        time, then the first event is instead scheduled to occur at getTime() + epsilon (the minimum possible next
        timestamp). If this is a valid time, ordering, interval (must be positive), 
        and event, schedules the event and returns a Stoppable, else returns null.
        The recurrence will continue until getTime() >= AFTER_SIMULATION, the Schedule is cleared out,
        or the Stoppable's stop() method is called, whichever happens first.
    
        <p>This method at present returns null if the schedule cannot
        schedule any more events (it's sealed or the time is AFTER_SIMULATION).  The method 
        throws an IllegalArgumentException if the event is being scheduled for an invalid time, or is null. 

        <p> Note that calling stop() on the Stoppable
        will not only stop the repeating, but will <i>also</i> make the Schedule completely
        forget (lose the pointer to) the Steppable scheduled here.  This is particularly useful
        if you need to make the Schedule NOT serialize certain Steppable objects. */

    public Stoppable scheduleRepeating(final double time, final Steppable event)
        {
        // No need to lock -- we're not grabbing time from the schedule
        return scheduleRepeating(time,0,event,1.0);
        }

    /** Schedules the event to recur at the specified interval starting at the provided time, 
        in ordering 0.  If the getTime() == the provided
        time, then the first event is instead scheduled to occur at getTime() + epsilon (the minimum possible next
        timestamp). If this is a valid time, interval (must be > 0), 
        and event, schedules the event and returns a Stoppable, else returns null.
        The recurrence will continue until getTime() >= AFTER_SIMULATION, the Schedule is cleared out,
        or the Stoppable's stop() method is called, whichever happens first.
    
        <p>This method at present returns null if the schedule cannot
        schedule any more events (it's sealed or the time is AFTER_SIMULATION).  The method 
        throws an IllegalArgumentException if the event is being scheduled for an invalid time, or is null. 

        <p> Note that calling stop() on the Stoppable
        will not only stop the repeating, but will <i>also</i> make the Schedule completely
        forget (lose the pointer to) the Steppable scheduled here.  This is particularly useful
        if you need to make the Schedule NOT serialize certain Steppable objects. */

    public Stoppable scheduleRepeating(final double time, final Steppable event, final double interval)
        {
        // No need to lock -- we're not grabbing time from the schedule
        return scheduleRepeating(time,0,event,interval);
        }

    /** Schedules the event to recur at an interval of 1.0 starting at the provided time, 
        and in the ordering provided.  If the getTime() == the provided
        time, then the first event is instead scheduled to occur at getTime() + epsilon (the minimum possible next
        timestamp). If this is a valid time, ordering,
        and event, schedules the event and returns a Stoppable, else returns null.
        The recurrence will continue until getTime() >= AFTER_SIMULATION, the Schedule is cleared out,
        or the Stoppable's stop() method is called, whichever happens first.
    
        <p>This method at present returns null if the schedule cannot
        schedule any more events (it's sealed or the time is AFTER_SIMULATION).  The method 
        throws an IllegalArgumentException if the event is being scheduled for an invalid time, or is null. 

        <p> Note that calling stop() on the Stoppable
        will not only stop the repeating, but will <i>also</i> make the Schedule completely
        forget (lose the pointer to) the Steppable scheduled here.  This is particularly useful
        if you need to make the Schedule NOT serialize certain Steppable objects. */

    public Stoppable scheduleRepeating(final double time, final int ordering, final Steppable event)
        {
        // No need to lock -- we're not grabbing time from the schedule
        return scheduleRepeating(time,ordering,event,1.0);
        }

    /** Schedules the event to recur at the specified interval starting at the provided time, 
        and in the ordering provided.  If the getTime() == the provided
        time, then the first event is instead scheduled to occur at getTime() + epsilon (the minimum possible next
        timestamp). If this is a valid time, ordering, interval (must be > 0), 
        and event, schedules the event and returns a Stoppable.
        The recurrence will continue until getTime() >= AFTER_SIMULATION, the Schedule is cleared out,
        or the Stoppable's stop() method is called, whichever happens first.
                
        <p>This method at present returns null if the schedule cannot
        schedule any more events (it's sealed or the time is AFTER_SIMULATION).  The method 
        throws an IllegalArgumentException if the event is being scheduled for an invalid time, or is null. 
    
        <p> Note that calling stop() on the Stoppable
        will not only stop the repeating, but will <i>also</i> make the Schedule completely 
        forget (lose the pointer to) the Steppable scheduled here.  This is particularly useful
        if you need to make the Schedule NOT serialize certain Steppable objects. */

    public Stoppable scheduleRepeating(final double time, final int ordering, final Steppable event, final double interval)
        {
        if (interval <= 0) throw new IllegalArgumentException("The steppable " +  event + " was scheduled repeating with an impossible interval ("+interval+")");
        Schedule.Key k = new Schedule.Key(time,ordering);
        IterativeRepeat r = new IterativeRepeat(event,interval,k);

        synchronized(lock)
            {
            if (_scheduleOnce(k,r)) return r; 
            else return null;
            }
        }

    /** Timestamps stored as keys in the heap.  Comps are comparable by their time first, and their ordering second. */
    protected static class Key implements Comparable, Serializable
        {
        double time;
        int ordering;
                
        public int getOrdering() { return ordering; }
        public double getTime() { return time; }
            
        public Key(double time, int ordering)
            {
            this.time = time;
            this.ordering = ordering;
            }
                
        public boolean equals(Object obj)
            {
            if (obj != null && obj instanceof Key)
                {
                Key o = (Key)obj;
                return (o.time == time && o.ordering == ordering);
                }
            else return false;
            }
                        
        public int hashCode()
            {
            int y = ordering;
            y += ~(y << 15);
            y ^=  (y >>> 10);
            y +=  (y << 3);
            y ^=  (y >>> 6);
            y += ~(y << 11);
            y ^=  (y >>> 16);

            long key = Double.doubleToRawLongBits(time);  // we can't ever be NaN or infinity, so this is okay
            key += ~(key << 32);
            key ^= (key >>> 22);
            key += ~(key << 13);
            key ^= (key >>> 8);
            key += (key << 3);
            key ^= (key >>> 15);
            key += ~(key << 27);
            key ^= (key >>> 31);

            return (int)(key ^ (key >> 32)) ^ y;
            }
                    
        public int compareTo(Object obj)
            {
            Key o = (Key)obj;
            double time = this.time;
            double time2 = o.time;
            if (time == time2)  // the most common situation
                {
                int ordering = this.ordering;
                int ordering2 = o.ordering;
                if (ordering == ordering2) return 0;  // the most common situation
                if (ordering < ordering2) return -1;
                /* if (ordering > ordering2) */ return 1;
                }
            // okay, so they're different times
            if (time < time2) return -1;
            /* if (time > time2) */ return 1;
            }
        }
    }


/**
   Handles repeated steps.  This is done by wrapping the Steppable with a IterativeRepeat object
   which is itself Steppable, and on its step calls its subsidiary Steppable, then reschedules
   itself.  IterativeRepeat is stopped by setting its subsidiary to null, and so the next time it's
   scheduled it won't reschedule itself (or call the subsidiary).   A private class for
   Schedule.  We've moved it out of being an inner class of Schedule and will ultimately make
   it a separate class in the package.
*/

class IterativeRepeat implements Steppable, Stoppable
    {
    double interval;
    Steppable step;  // if null, does not reschedule
    Schedule.Key key;
        
    public IterativeRepeat(final Steppable step, final double interval, final Schedule.Key key)
        {
        if (interval < 0)
            throw new IllegalArgumentException("For the Steppable...\n\n" + step +
                "\n\n...the interval provided ("+interval+") is less than zero");
        else if (interval != interval)  /* NaN */
            throw new IllegalArgumentException("For the Steppable...\n\n" + step +
                "\n\n...the interval provided ("+interval+") is NaN");

        this.step = step;
        this.interval = interval;
        this.key = key;
        }
        
    public synchronized void step(final SimState state)
        {
        if (step!=null)
            {
            try
                {
                // reuse the Key to save some gc perhaps -- it's been pulled out and discarded at this point
                key.time += interval;
                if (key.time < Schedule.AFTER_SIMULATION) 
                    state.schedule.scheduleOnce(key,this);  // may return false if we couldn't schedule, which is fine
                }
            catch (IllegalArgumentException e)
                {
                e.printStackTrace(); // something bad happened
                }
            assert sim.util.LocationLog.set(step);
            step.step(state);
            assert sim.util.LocationLog.clear();
            }
        }
        
    public synchronized void stop()  
        {
        step = null;
        }
        
    public String toString() { return "Schedule.IterativeRepeat[" + step + "]"; }
    }

