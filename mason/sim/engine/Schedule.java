/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;
import java.io.Serializable;

import sim.util.*;

/**
   Schedule defines a threadsafe scheduling queue in which events can be scheduled to occur
   at future time.  The time of the most recent event which has already occured
   is given by the <b>time()</b> method.  If the current time is <tt>BEFORE_SIMULATION</tt> (defined
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
   No event may be scheduled for a time earlier than time().  If at time time() you schedule a new
   event for time time(), then actually this event will occur at time time()+epsilon, that is, the
   smallest possible slice of time greater than time().
   
   <p>Events at a step are further subdivided and scheduled according to their <i>ordering</i>, an integer.
   Objects for scheduled for lower orderings for a given time will be executed before objects with
   higher orderings for the same time.  If objects are scheduled for the same time and
   have the same ordering value, their execution will be randomly ordered with respect to one another
   unless (in the very rare case) you have called setShuffling(false);.  Generally speaking, most experiments with
   good model methodologies will want random shuffling left on, and if you need an explicit ordering, it may be
   better to rely on Steppable's orderings or to use a Sequence.
   
   <p>Previous versions of Schedule required you to specify the number of orderings when instantiating a Schedule.
   This is no longer the case.  The constructor is still there, but the number of orderings passed in is entirely
   ignored.
   
   <p>You might be wondering: why bother with using orderings?  After all, can't you achieve the same thing by just
   stretching elements out in time?  There are two reasons to use orderings.  First, it allows you to use the time()
   method to keep tabs on the current time in a way that might be convenient to you.  But second and more importantly,
   MASON's GUI facility will update its displays and inspectors only after all Steppables scheduled for a 
   given timestamp have completed, and so orderings give you a way of subdividing the interval of time between
   GUI updates.
   
   <p>You can clear out the entire Schedule by calling reset(), including about-to-be executed Steppables in the
   current timestep.  However, this does not prevent AsynchronousSteppables from suddenly rescheduling themselves
   in the queue.  Stopping the simulation from within a Steppable object's step() method is best done by
   calling SimState.kill().  From the main thread, the most straightforward way to stop a simulation is to just
   stop calling schedule.step(...), and proceed directly to SimState.finish().
   
   <p>You can get the number of times that step(...) has been called on the schedule by calling the getSteps() method.
   This value is incremented just as the Schedule exits its step(...) method and only if the method returned true.
   Additionally, you can get a string version of the current time with the getTimestamp(...) method.

   <p><b>Exception Handling</b>.  It's a common error to schedule a null event, or one with an invalid ordering or time.
   Schedule previously returned false or null in such situations, but this leaves the burden on the programmer to check,
   and programmers are forgetful!  We have changed Schedule to throw exceptions by default instead.  You can change
   Schedule back to returning false or null (perhaps if you want to handle the situations yourself more efficiently than
   catching an exception, or if you know what you're doing schedule-wise) by setting setThrowingScheduleExceptions(false).
*/
    

public class Schedule implements java.io.Serializable
    {
    public static final double EPOCH = 0.0;
    public static final double BEFORE_SIMULATION = EPOCH - 1.0;
    public static final double AFTER_SIMULATION = Double.POSITIVE_INFINITY;
    public static final double EPOCH_PLUS_EPSILON = Double.longBitsToDouble(Double.doubleToRawLongBits(EPOCH)+1L);
    public static final double MAXIMUM_INTEGER = 9.007199254740992E15;

	// should we shuffle individuals with the same timestep and ordering?
	boolean shuffling = true;  // by default, we WANT to shuffle

    Heap queue = new Heap();
    
    // the time
    double time;
    
    // the number of times step() has been called on m
    long steps;
    
    // whether or not the Schedule throws errors when it encounters an exceptional condition
    // on attempting to schedule an item
    boolean throwingScheduleExceptions = true;
	
	/** Sets the schedule to randomly shuffle the order of Steppables (the default), or to not do so, when they
		have identical orderings and are scheduled for the same time.  If the Steppables are not randomly shuffled,
		they will be executed in the order in which they were inserted into the schedule.  You should set this to
		FALSE only under unusual circumstances when you know what you're doing -- in the vast majority of cases you
		will want it to be TRUE.  */
	public synchronized void setShuffling(boolean val)
		{
		shuffling = val;
		}
	
	/** Returns true (the default) if the Steppables' order is randomly shuffled when they have identical orderings
		and are scheduled for the same time; else returns false. */
	public synchronized boolean isShuffling()
		{
		return shuffling;
		}
	
	/** Sets the Schedule to either throw exceptions or return false when a Steppable is scheduled
        in an invalid fashion -- an invalid time, or a null Steppable, etc.  By default, throwing
        exceptions is set to TRUE.  You should change this only if you require backward-compatability. */
    public synchronized void setThrowingScheduleExceptions(boolean val)
        {
        throwingScheduleExceptions = val;
        }
        
    /** Returns if the Schedule is set to either throw exceptions or return false when a Steppable is scheduled
        in an invalid fashion -- an invalid time, or a null Steppable, etc.  By default, throwing
        exceptions is set to TRUE.  */
    public synchronized boolean isThrowingScheduleExceptions()
        {
        return throwingScheduleExceptions;
        }
    
    /** Creates a Schedule.  The <i>numOrders</i> argument is ignored. */
    public Schedule(final int numOrders)
        {
        time = BEFORE_SIMULATION;
        steps = 0;
        }
    
    /** Creates a Schedule. */
    public Schedule()
        {
        this(1);
        }
    
    public synchronized double time() { return time; }
    
    /** Returns the current time in string format. If the time is BEFORE_SIMULATION, then beforeSimulationString is
        returned.  If the time is AFTER_SIMULATION, then afterSimulationString is returned.  Otherwise a numerical
        representation of the time is returned. */
    public synchronized String getTimestamp(final String beforeSimulationString, final String afterSimulationString)
        {
        return getTimestamp(time(), beforeSimulationString, afterSimulationString);
        }
    
    /** Returns a given time in string format. If the time is BEFORE_SIMULATION, then beforeSimulationString is
        returned.  If the time is AFTER_SIMULATION, then afterSimulationString is returned.  Otherwise a numerical
        representation of the time is returned. */
    public String getTimestamp(double time, final String beforeSimulationString, final String afterSimulationString)
        {
        if (time <= BEFORE_SIMULATION) return beforeSimulationString;
        if (time >= AFTER_SIMULATION) return afterSimulationString;
        if (time == (long)time) return Long.toString((long)time);
        return Double.toString(time);
        }

    /** Returns the number of steps the Schedule has pulsed so far. */
    public synchronized long getSteps() { return steps; }

    // pushes the time to AFTER_SIMULATION and attempts to kill all
    // remaining scheduled items
    synchronized void pushToAfterSimulation()
        {
        time = AFTER_SIMULATION;
        if (inStep)
            killStep = true;
        queue = new Heap();  // let 'em GC
        }

    /** Empties out the schedule and resets it to a pristine state BEFORE_SIMULATION, with steps = 0.  If you're
        looking for a way to kill your simulation from a Steppable, use SimState.kill() instead.  */
    public synchronized void reset()
        {
        time = BEFORE_SIMULATION;
        steps = 0;
        if (inStep)  // we're doing this inside the step(...) method
            killStep = true;
        queue = new Heap();  // let 'em GC
        }
    
    /** Returns true if the schedule has nothing left to do. */
    public synchronized boolean scheduleComplete()
        {
        return _scheduleComplete();
        }
    
    boolean _scheduleComplete()
        {
        return queue.isEmpty();
        }

    // substeps is now private to the step(...) function
    Bag substeps = new Bag();
    boolean inStep = false;     // are we inside a step() method?
    boolean killStep = false;   // has a request been made to stop stepping?
    /** Steps the schedule, gathering and ordering all the items to step on the next time step (skipping
        blank time steps), and then stepping all of them in the decided order.  
        Returns FALSE if nothing was stepped -- the schedule is exhausted or time has run out. */
    public synchronized boolean step(final SimState state)
        {
        inStep = true;
        final double AFTER_SIMULATION = Schedule.AFTER_SIMULATION;  // a little faster
        
        if (time==AFTER_SIMULATION) return false;
        Bag substeps = this.substeps;  // a little faster
                
        if (!_scheduleComplete())
            {
			boolean shuffling = this.shuffling;  // a little faster

            // figure the current time 
            time = ((Key)(queue.getMinKey())).time;

            // loop as long as there are elements left in the heap that are the
            // same timzeone as the minimum key's time
            while(true)
                {
                Key key = (Key)(queue.getMinKey());
                if (key == null || key.time != time) break;
                                
                // Suck out the contents -- but just the ones in the minimum ordering
                queue.extractMin(substeps);  // come out in reverse order

                // shuffle
                if (substeps.numObjs > 1) 
					{
					if (shuffling) substeps.shuffle(state.random);  // no need to flip -- we're randomizing
					else substeps.reverse();  // they came out in reverse order; we need to flip 'em
					}
                                
                // execute
                int len = substeps.numObjs;
                Object[] objs = substeps.objs;
                                
                inStep = true;  // so reset() knows it can kill me
                for(int x=0;x<len;x++)  // if we're not being killed...
                    {
                    if (!killStep) // lots of overhead here... :-(
                        ((Steppable)(objs[x])).step(state);
                    objs[x] = null;  // let gc even if being killed
                    }
                inStep = false;  // we're done
                killStep = false;
                                
                // reuse substeps -- all objects should have been released to gc already
                substeps.numObjs = 0;
                }
            }
        else
            {
            time = AFTER_SIMULATION;
            inStep = false;
            killStep = false;
            return false;
            }
        steps++;
        inStep = false;
        killStep = false;
        return true;
        }
        
    /** Schedules the event to occur at time() + 1.0, 0 ordering. If this is a valid time
        and event, schedules the event and returns TRUE, else returns FALSE.  */
    
    // synchronized so getting the time can be atomic with the subsidiary scheduleOnce function call
    public synchronized boolean scheduleOnce(final Steppable event)
        {
        return scheduleOnce(new Key(time+1.0,0),event);
        }
        
    /** Schedules the event to occur at time() + 1.0, 0 ordering. If this is a valid time
        and event, schedules the event and returns TRUE, else returns FALSE.  */
    
    // synchronized so getting the time can be atomic with the subsidiary scheduleOnce function call
    public synchronized boolean scheduleOnce(final Steppable event, final int ordering)
        {
        return scheduleOnce(new Key(time+1.0,ordering),event);
        }

    /** Schedules the event to occur at the provided time, 0 ordering.  If the time() == the provided
        time, then the event is instead scheduled to occur at time() + epsilon (the minimum possible next
        timestamp). If this is a valid time
        and event, schedules the event and returns TRUE, else returns FALSE.*/
    
    public boolean scheduleOnce(final double time, final Steppable event)
        {
        return scheduleOnce(new Key(time,0),event);
        }
        
    /** Schedules the event to occur at the provided time, and in the ordering provided.  If the time() == the provided
        time, then the event is instead scheduled to occur at time() + epsilon (the minimum possible next
        timestamp). If this is a valid time, ordering,
        and event, schedules the event and returns TRUE, else returns FALSE.
    */
    public synchronized boolean scheduleOnce(double time, final int ordering, final Steppable event)
        {
        return scheduleOnce(new Key(time,ordering),event);
        }
    
    synchronized boolean scheduleOnce(Key key, final Steppable event)
        {
        // locals are a teeny bit faster
        double time = this.time;
        double t = key.time;

        // check to see if we're scheduling for the same exact time -- even if of different orderings, that doesn't matter
        if (t == time && t != AFTER_SIMULATION)
            // bump up time to the next possible item, unless we're at infinity already (AFTER_SIMULATION)
            t = key.time = Double.longBitsToDouble(Double.doubleToRawLongBits(t)+1L);

        if (t < EPOCH || t >= AFTER_SIMULATION || t != t /* NaN */ || t < time || event == null)
            {
            if (!isThrowingScheduleExceptions()) 
                return false;
            else if (t < EPOCH)
                throw new IllegalArgumentException("For the Steppable...\n\n"+event+
                                                   "\n\n...the time provided ("+t+") is < EPOCH (" + EPOCH + ")");
            else if (t >= AFTER_SIMULATION)
                throw new IllegalArgumentException("For the Steppable...\n\n"+event+
                                                   "\n\n...the time provided ("+t+") is >= AFTER_SIMULATION (" + AFTER_SIMULATION + ")");
            else if (t != t /* NaN */)
                throw new IllegalArgumentException("For the Steppable...\n\n"+event+
                                                   "\n\n...the time provided ("+t+") is NaN");
            else if (t < time)
                throw new IllegalArgumentException("For the Steppable...\n\n"+event+
                                                   "\n\n...the time provided ("+t+") is less than the current time (" + time + ")");
            else if (event == null)
                throw new IllegalArgumentException("The provided Steppable is null");
            }
        
        if (!killStep) queue.add(event, key);  // only bother adding if we're not being killed
        return true;
        }

    /** Schedules the event to recur at an interval of 1.0 starting at time() + 1.0, and at 0 ordering.
        If this is a valid event, schedules the event and returns a Stoppable, else returns null.
        The recurrence will continue until time() >= AFTER_SIMULATION, the Schedule is cleared out,
        or the Stoppable's stop() method is called, whichever happens first.

        <p> Note that calling stop() on the Stoppable
        will not only stop the repeating, but will <i>also</i> make the Schedule completely
        forget (lose the pointer to) the Steppable scheduled here.  This is particularly useful
        if you need to make the Schedule NOT serialize certain Steppable objects. */
    
    // synchronized so getting the time can be atomic with the subsidiary scheduleRepeating function call
    public synchronized Stoppable scheduleRepeating(final Steppable event)
        {
        return scheduleRepeating(time+1.0,0,event,1.0);
        }

    /** Schedules the event to recur at the specified interval starting at time() + interval, and at 0 ordering.
        If this is a valid interval (must be >= 0)
        and event, schedules the event and returns a Stoppable, else returns null.
        If interval is 0, then the recurrence will be scheduled at the current time + epsilon.
        The recurrence will continue until time() >= AFTER_SIMULATION, the Schedule is cleared out,
        or the Stoppable's stop() method is called, whichever happens first.

        <p> Note that calling stop() on the Stoppable
        will not only stop the repeating, but will <i>also</i> make the Schedule completely
        forget (lose the pointer to) the Steppable scheduled here.  This is particularly useful
        if you need to make the Schedule NOT serialize certain Steppable objects. */
    
    // synchronized so getting the time can be atomic with the subsidiary scheduleRepeating function call
    public synchronized Stoppable scheduleRepeating(final Steppable event, final double interval)
        {
        return scheduleRepeating(time+interval,0,event,interval);
        }

    /** Schedules the event to recur at the specified interval starting at time() + interval, and at the provided ordering.
        If this is a valid interval (must be >=> 0)
        and event, schedules the event and returns a Stoppable, else returns null.
        If interval is 0, then the recurrence will be scheduled at the current time + epsilon.
        The recurrence will continue until time() >= AFTER_SIMULATION, the Schedule is cleared out,
        or the Stoppable's stop() method is called, whichever happens first.

        <p> Note that calling stop() on the Stoppable
        will not only stop the repeating, but will <i>also</i> make the Schedule completely
        forget (lose the pointer to) the Steppable scheduled here.  This is particularly useful
        if you need to make the Schedule NOT serialize certain Steppable objects. */
    
    // synchronized so getting the time can be atomic with the subsidiary scheduleRepeating function call
    public synchronized Stoppable scheduleRepeating(final Steppable event, final int ordering, final double interval)
        {
        return scheduleRepeating(time+interval,ordering,event,interval);
        }

    /** Schedules the event to recur at the specified interval starting at the provided time, and at 0 ordering.
        If the time() == the provided
        time, then the first event is instead scheduled to occur at time() + epsilon (the minimum possible next
        timestamp). If this is a valid time, ordering, interval (must be positive), 
        and event, schedules the event and returns a Stoppable, else returns null.
        The recurrence will continue until time() >= AFTER_SIMULATION, the Schedule is cleared out,
        or the Stoppable's stop() method is called, whichever happens first.
    
        <p> Note that calling stop() on the Stoppable
        will not only stop the repeating, but will <i>also</i> make the Schedule completely
        forget (lose the pointer to) the Steppable scheduled here.  This is particularly useful
        if you need to make the Schedule NOT serialize certain Steppable objects. */

    public Stoppable scheduleRepeating(final double time, final Steppable event)
        {
        return scheduleRepeating(time,0,event,1.0);
        }

    /** Schedules the event to recur at the specified interval starting at the provided time, 
        in ordering 0.  If the time() == the provided
        time, then the first event is instead scheduled to occur at time() + epsilon (the minimum possible next
        timestamp). If this is a valid time, interval (must be >=0), 
        and event, schedules the event and returns a Stoppable, else returns null.
        If interval is 0, then the recurrence will be scheduled at the current time + epsilon.
        The recurrence will continue until time() >= AFTER_SIMULATION, the Schedule is cleared out,
        or the Stoppable's stop() method is called, whichever happens first.
    
        <p> Note that calling stop() on the Stoppable
        will not only stop the repeating, but will <i>also</i> make the Schedule completely
        forget (lose the pointer to) the Steppable scheduled here.  This is particularly useful
        if you need to make the Schedule NOT serialize certain Steppable objects. */

    public Stoppable scheduleRepeating(final double time, final Steppable event, final double interval)
        {
        return scheduleRepeating(time,0,event,interval);
        }

    /** Schedules the event to recur at an interval of 1.0 starting at the provided time, 
        and in the ordering provided.  If the time() == the provided
        time, then the first event is instead scheduled to occur at time() + epsilon (the minimum possible next
        timestamp). If this is a valid time, ordering,
        and event, schedules the event and returns a Stoppable, else returns null.
        The recurrence will continue until time() >= AFTER_SIMULATION, the Schedule is cleared out,
        or the Stoppable's stop() method is called, whichever happens first.
    
        <p> Note that calling stop() on the Stoppable
        will not only stop the repeating, but will <i>also</i> make the Schedule completely
        forget (lose the pointer to) the Steppable scheduled here.  This is particularly useful
        if you need to make the Schedule NOT serialize certain Steppable objects. */

    public Stoppable scheduleRepeating(final double time, final int ordering, final Steppable event)
        {
        return scheduleRepeating(time,ordering,event,1.0);
        }

    /** Schedules the event to recur at the specified interval starting at the provided time, 
        and in the ordering provided.  If the time() == the provided
        time, then the first event is instead scheduled to occur at time() + epsilon (the minimum possible next
        timestamp). If this is a valid time, ordering, interval (must be >= 0), 
        and event, schedules the event and returns a Stoppable, else returns null.
        If interval is 0, then the recurrence will be scheduled at the current time + epsilon.
        The recurrence will continue until time() >= AFTER_SIMULATION, the Schedule is cleared out,
        or the Stoppable's stop() method is called, whichever happens first.
    
        <p> Note that calling stop() on the Stoppable
        will not only stop the repeating, but will <i>also</i> make the Schedule completely 
        forget (lose the pointer to) the Steppable scheduled here.  This is particularly useful
        if you need to make the Schedule NOT serialize certain Steppable objects. */

    public Stoppable scheduleRepeating(final double time, final int ordering, final Steppable event, final double interval)
        {
        if (event==null || interval < 0 || interval != interval /* NaN */) // don't check for interval being infinite -- that might be valid!
            {
            if (!isThrowingScheduleExceptions())
                return null;  // 0 is okay because it's "the immediate next"
            else if (event == null)
                throw new IllegalArgumentException("The provided Steppable is null");
            else if (interval < 0)
                throw new IllegalArgumentException("For the Steppable...\n\n"+event+
                                                   "\n\n...the interval provided ("+interval+") is less than zero");
            else if (interval != interval)  /* NaN */
                throw new IllegalArgumentException("For the Steppable...\n\n"+event+
                                                   "\n\n...the interval provided ("+interval+") is NaN");
            }
                        
        Key k = new Key(time,ordering);
        Repeat r = new Repeat(event,interval,k);
        if (scheduleOnce(k,r)) return r; 
        else return null;
        }
    }



/**
   Handles repeated steps.  This is done by wrapping the Steppable with a Repeat object
   which is itself Steppable, and on its step calls its subsidiary Steppable, then reschedules
   itself.  Repeat is stopped by setting its subsidiary to null, and so the next time it's
   scheduled it won't reschedule itself (or call the subsidiary).   A private class for
   Schedule.  We've moved it out of being an inner class of Schedule and will ultimately make
   it a separate class in the package.
*/

class Repeat implements Steppable, Stoppable
    {
    double interval;
    Steppable step;  // if null, does not reschedule
    Key key;
        
    public Repeat(final Steppable step, final double interval, final Key key)
        {
        this.step = step;
        this.interval = interval;
        this.key = key;
        }
        
    public synchronized void step(final SimState state)
        {
        if (step!=null)
            {
            // this occurs WITHIN the schedule's synchronized step, so time()
            // and scheduleOnce() will both occur together without the time
            // changing
            try
                {
                // reuse the Key to save some gc perhaps -- it's been pulled out and discarded at this point
                key.time += interval; //  = time()+interval;
                state.schedule.scheduleOnce(key,this);  // will return false if time has run out and throwingScheduleExceptions = false
                }
            catch (IllegalArgumentException e) { } // occurs if time has run out and throwingScheduleExceptions = true
            step.step(state);
            }
        }
        
    public synchronized void stop()  
        {
        step = null;
        }
    }

/** Timestamps stored as keys in the heap.  Comps are comparable by their time first, and their ordering second. */
class Key implements Comparable, Serializable
    {
    double time;
    int ordering;
        
    public Key(double time, int ordering)
        {
        this.time = time;
        this.ordering = ordering;
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
