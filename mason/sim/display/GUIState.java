/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.display;
import sim.engine.*;
import sim.portrayal.*;
import java.io.*;
import sim.util.*;
import ec.util.*;
import java.util.*;

/** A wrapper for SimState and Schedule which provides additional functionality for
    GUI objects. This wrapper extends the functionality of SimState and
    Schedule through encapsulation, NOT subclassing.  Why?  Because the
    general idea is that SimState, and *anything* which hangs off of it,
    should be serializable out to a checkpoint and not know or care about whether
    or not it's running under a GUI or running headless.
    
    <p>Displays and Controllers (such as Display2D and the Console) do not care about the
    SimState, and make precious few calls to the Schedule.  Instead, they generally only
    work with you through the GUIState class.
    
    <p>GUIState has the same start() and finish() methods as SimState, and indeed
    the default forms of these methods just call start() and finish() on the underlying
    SimState.  Additionally, GUIState has init(Controller) and quit() methods.  The
    init(Controller) method sets up the GUIState to work in an environment controlled
    by the specified Controller.  The quit() method is called to tell the GUIState to
    shut down and free any of its resources (perhaps the simulation document is being
    closed in the GUI, or the GUI is quitting).  
    
    <p>GUIState also has two methods used by the Controller to specify things about it.
    In particular, <tt>public static String getName(Class class)</tt>
    should return an intelligent name for the simulation,
    and <tt>public static Object getInfo(Class class)</tt> should return an
    HTML or textual description of the simulation either as a URL or a String.
    
    <p>You can create a global inspector for your model (as opposed to the individual
    per-object inspectors created by various portrayals).  This is done by overriding either 
    getInspector() or getSimulationInspectedObject().
    
    <p>GUIState has a wrapper step() method which in turn calls the Schedule's
    step(state) method.  However, this wrapper lso provides a hook for objects such as
    displays to schedule themselves without using the Schedule.  This hook is the
    scheduleImmediate(...) and scheduleImmediateRepeat(...) methods.  There is also
    a reset() method which resets both the immediates and the underlying Schedule.
    
    <p>Last, GUIState has a wrapper function to make it convenient to read in a new SimState from
    a serialized file: readNewStateFromCheckpoint().  This function checks to see if the
    serialized file is valid for this simulation.  To do this, it calls validSimState(),
    which is a hook that you should override to return TRUE if the provided state is a valid SimState
    for your simulation (usually this means that it's the right subclass of SimState for your
    purposes).
    
    <p> Generally speaking, if you have access to a GUIState, you should use GUIstate methods
    start(), finish(), step(), reset(), and readNewStateFromCheckpoint() 
    instead of the underlying methods in the SimState and Controller.  Otherwise, feel free
    to use the underlying methods (such as Schedule.getTime()).
    
    <p><b>Exception Handling</b>.  It's a common error to schedule a null event, or one with an invalid time.
    Like Schedule, GUIState previously returned false or null in such situations, 
    but this leaves the burden on the programmer to check,
    and programmers are forgetful!  We have changed GUIState and Schedule to throw exceptions by default instead.  
    You can change them both back to returning false or null (perhaps if you want to handle the 
    situations yourself more efficiently than catching an exception, or if you know what you're doing schedule-wise)
    by setting Schedule.setThrowingScheduleExceptions(false).
*/

public abstract class GUIState
    {
    /** An additional random number generator available for GUI and drawing purposes,
        separate from the one used in the model.  If you use this generator to do things
        like specify the colors of agents on-screen, rather than use the model's generator,
        you can guarantee identical simulation results with the model regardless of whether
        it runs under the model or the GUI.   Also, unlike state.random, using guirandom
        doesn't require synchronizing on state.schedule first. */
    public MersenneTwisterFast guirandom = new MersenneTwisterFast();
        
    /** The underlying SimState */
    public SimState state;
    
    /** The controller for the GUIState.  This field may be null if there is no controller
        or no controller YET */
    public Controller controller;

    public HashMap storage = new HashMap();

    /** Override this constructor in your subclass to call <code>super(state)</code> where state is a
        properly constructed SimState appropriate to your problem -- do NOT call <code>super()</code>*/
    private GUIState()
        {
        }
    
    /** You may optionally override this constructor to call <code>super(state)</code> but you should
        be sure to override the no-argument GUIState() constructor as stipulated. */
    public GUIState(SimState state)
        {
        this.state = state;
        resetQueues();
        }
                
    /** Returns the short name of the class.  If the Class is foo.bar.baz.Quux, then Quux
        is returned. */
    public static String getTruncatedName(Class theClass)
        {
        // do the default
        if (theClass==null) return "";
        String fullName = theClass.getName();
        int lastPeriod = fullName.lastIndexOf(".");
        return fullName.substring(lastPeriod + 1);  // nifty, works even if lastPeriod = -1
        }
    
    /** Call this method to get the simulation name for any class.  If a class does not implement
        getName(), then the fully qualified classname is used instead.  To provide a descriptive
        name for your class, override getName(). */
                
    public final static String getName(Class theClass)
        {
        if (theClass == null) return "";
        try
            {
            java.lang.reflect.Method m = theClass.getMethod("getName", (Class[])null);
            if (m.getDeclaringClass().equals(GUIState.class)) // it wasn't overridden
                return getTruncatedName(theClass);
            else return (String)(m.invoke(null,(Object[])null));
            }
        catch (NoSuchMethodException e)
            {
            e.printStackTrace();
            return getTruncatedName(theClass);
            }
        catch (Throwable e)
            {
            e.printStackTrace();
            return "Error in retrieving simulation name";
            }
        }
        
    /** Creates and returns a controller ready for the user to manipulate.
        By default this method creates a Console, sets it visible, and
        returns it.  You can override this to provide some other kind of
        controller. */
    public Controller createController()
        {
        Console console = new Console(this);
        console.setVisible(true);
        return console;
        }
        
    /** Override this method in your subclass to provide a descriptive 
        name for your simulation;
        otherwise the default will be used: the short classname (that is,
        if your class is foo.bar.Baz, Baz will be used).  That might not
        be very descriptive!
                        
        <p>Notice that this is a static method, and yet you're overriding it as if it
        were an instance method.  This is because the method <b>getInfo(Class)</b>
        uses reflection to call the proper method on a per-class basis.  Like magic!
    */
    public static String getName() 
        {
        return "This is GUIState's getName() method.  It probably shouldn't have been called.";
        }
                
    static Object doDefaultInfo(Class theClass)
        {
        java.net.URL url = theClass.getResource("index.html");
        if (url == null)
            return "<html><head><title></title></head><body bgcolor=\"white\"></body></html>";
        else return url;
        }
                
    /** Returns either a String or a URL which provides descriptive information about the simulation hosted by
        the given class (which should be a GUIState subclass).  To change the information string about your
        own simulation, override the <b>getInfo()</b> method instead.  If you don't override this method,
        MASON will look for a file called "index.html" in the same directory as the class file and use that.
        However if this then fails, Java will hunt for <i>any</i> "index.html" file and use that instead --
        that's probably not what you wanted.  Long story short, either override the getInfo() method,
        or provide an "index.html" file.
    */
    public final static Object getInfo(Class theClass)
        {
        if (theClass == null) return "";
        try
            {
            java.lang.reflect.Method m = theClass.getMethod("getInfo", (Class[])null);
            if (m.getDeclaringClass().equals(GUIState.class)) // it wasn't overridden
                return doDefaultInfo(theClass);
            else return m.invoke(null,(Object[])null);
            }
        catch (NoSuchMethodException e)
            {
            return doDefaultInfo(theClass);
            }
        catch (Throwable e)
            {
            e.printStackTrace();
            return "Error in retrieving simulation info";
            }
        }
        
    /** Override this method with a static method of your own 
        in your subclass to provide an object (a URL or a String) describing information about
        your simulation;  if you do not override this method, then the system will
        look for a file called <tt>index.html</tt> located next to your <tt>.class</tt> file
        and use a URL to that file as the information.  If there is no such file, Java may then go
        on a hunting expedition to find and return some file called <tt>index.html</tt> in your CLASSPATH;
        this is probably not what you intended, but we can't easily prevent it.  If there's absolutely no
        such file, getInfo() will return a blank HTML page.
                
        <p>Notice that this is a static method, and yet you're overriding it as if it
        were an instance method.  This is because the method <b>getInfo(Class)</b>
        uses reflection to call the proper method on a per-class basis.  Like magic!
    */
    public static Object getInfo()
        {
        return "This is GUIState's getInfo() method.  It probably shouldn't have been called.";
        }
        
    /** Override this to provide a custom Properties object for your simuation.  This should be
        very rare: by default returns null.*/
    public sim.util.Properties getSimulationProperties()
        {
        return null;
        }
        
    public int getMaximumPropertiesForInspector() { return SimpleInspector.DEFAULT_MAX_PROPERTIES; }

    /** By default returns a non-volatile Inspector which wraps around
        getSimulationInspectedObject(); if getSimulationInspectedObject() returns null, then getInspector()
        will return null also.  Override this to provide a custom inspector as you see fit.  */
    public Inspector getInspector()
        {
        Object object = getSimulationInspectedObject();
        if (object != null)
            {
            Inspector i = Inspector.getInspector(object, this, null);  // will use getMaximumPropertiesForInspector() 
            i.setVolatile(false);
            return i;
            }
        sim.util.Properties prop = getSimulationProperties();
        if (prop != null)
            {
            Inspector i = new SimpleInspector(prop, this, null);    // will use getMaximumPropertiesForInspector() 
            i.setVolatile(true);  // dynamic properties like this are likely volatile
            return i;
            }
        return null;
        }
    
    /** Returns an object with various property methods (getFoo(...), isFoo(...), setFoo(...)) whose
        properties will be accessible by the user.  This gives you an easy way to allow the user to
        set certain global properties of your model from the GUI.   If null is returned (the default),
        then nothing will be displayed to the user.  One trick you should know about your object:
        it should be public, as well its property methods, and if it's anonymous, it should not
        introduce any property methods not defined in its superclass.  Otherwise Java's reflection
        API can't access those methods -- they're considered private.  GUIState also supports
        sim.util.Properties's domFoo(...) domain declarations to allow for sliders and pop-up lists.*/
    public Object getSimulationInspectedObject()
        {
        return null;
        }
        
    /** Use Inspector.isVolatile() instead.  This method returns getInspector().isVolatile().
        This is an expensive method and you should not use it.
        @deprecated
    */
    public boolean isInspectorVolatile()
        {
        return getInspector().isVolatile();
        }
    
    /** Called to initialize (display) windows etc. 
        You can use this to set up the windows, then register them with the Controller so it can manage
        hiding, showing, and moving them.  The default version of this method simply calls
        this.controller=controller; */
    public void init(Controller controller)
        {
        this.controller = controller;
        }
    
    /** Called immediately prior to starting the simulation, or in-between
        simulation runs.  Ordinarily, you wouldn't need to override this hook. */
    boolean started = false;
    public void start()
        {
        started = true;
        state.start();
        
        // schedule the global inspector if there is one
        
        // run the start queue stuff
        synchronized(state.schedule)
            {
            // execute the start queue
            Steppable[] _start2 = start2;
            System.arraycopy(start,0,start2,0,startSize);
            int _startSize = startSize;
            startSize = 0;
            
            // do the start stuff
            for(int x=0;x<_startSize;x++)
                _start2[x].step(state);
            }
        }
        
        
    /** Called either at the proper or a premature end to the simulation. 
        If the user quits the program, this function may not be called.
        Ordinarily, you wouldn't need to override this hook.  Does nothing
        if the GUIState hasn't been started or loaded yet. */
    public void finish()
        {
        if (!started) return;  // no reason to call
        synchronized(state.schedule)
            {
            // execute the finish queue
            Steppable[] _finish2 = finish2;
            System.arraycopy(finish,0,finish2,0,finishSize);
            int _finishSize = finishSize;
            finishSize = 0;
            
            // do the finish stuff
            for(int x=0;x<_finishSize;x++)
                _finish2[x].step(state);
            }

        state.finish();
        resetQueues();
        started = false;
        }

    /** Called by the Console when the user is quitting the SimState.  A good place
        to stick stuff that you'd ordinarily put in a finalizer -- finalizers are
        tenuous at best. So here you'd put things like the code that closes the relevant
        display windows etc.*/
    public void quit()
        {
        }

    /** This method should be set to return TRUE if state can be validly used -- mostly likely
        all you need to check is that it's the right class for this simulation.  The default
        returns TRUE if state is non-null and the same class as the current state; that's often sufficient. */
    public boolean validSimState(SimState state)
        {
        return (state != null && state.getClass().equals(this.state.getClass()));
        }

    /** Called by the Console when the user is loading in a new state from a checkpoint.  The
        new state is passed in as an argument.  You should override this, calling super.load(state) first, 
        to reset your portrayals etc. to reflect the new state.
        state.start() will NOT be called.  Thus anything you handled in start() that needs
        to be reset to accommodate the new state should be handled here.  We recommend that you 
        call repaint() on any Display2Ds. */
    public void load(SimState state)
        {
        this.state = state;
        started = true;  // just in case
        
        // schedule the global inspector if there is one
        
        // run the start queue stuff
        synchronized(state.schedule)
            {
            // execute the start queue
            Steppable[] _start2 = start2;
            System.arraycopy(start,0,start2,0,startSize);
            int _startSize = startSize;
            startSize = 0;
            
            // do the start stuff
            for(int x=0;x<_startSize;x++)
                _start2[x].step(state);
            }
        }

    /** Loads a new SimState from the provided file.  Do not call this in an unthreadsafe
        situation -- it doesn't check.  Returns false if the state was not valid.  Returns
        various errors if bad things occurred trying to serialize in from the checkpoint. 
        If false is returned or an error is thrown, the old SimState is retained. */
    public boolean readNewStateFromCheckpoint(File file)
        throws IOException, ClassNotFoundException, OptionalDataException, ClassCastException, Exception
        {
        FileInputStream f = new FileInputStream(file);
        SimState state = SimState.readFromCheckpoint(f);
        f.close();
        if (!validSimState(state)) return false;
        finish();  // let it clean up
        load(state);
        return true;
        }



    // the before and after queues
    protected Steppable[] before;
    Steppable[] before2;
    protected int beforeSize;
    protected Steppable[] after;
    Steppable[] after2;
    protected int afterSize;
  
    // the start and finish queues
    Steppable[] start;
    Steppable[] start2;
    int startSize;
    Steppable[] finish;
    Steppable[] finish2;
    int finishSize;
      
    /** Don't call this unless you know what you're doing. */
    protected void resetQueues()
        {
        before = new Steppable[11];
        before2 = new Steppable[11];
        beforeSize = 0;
        after = new Steppable[11];
        after2 = new Steppable[11];
        afterSize = 0;
        start = new Steppable[11];
        start2 = new Steppable[11];
        startSize = 0;
        finish = new Steppable[11];
        finish2 = new Steppable[11];
        finishSize = 0;
        }
        
    /** Empties out the schedule and resets it to a pristine state BEFORE_SIMULATION.
        If you're using a GUIState, you should call this version instead of Schedule's
        version. 
        @deprecated.  Do not use.
    */
    private synchronized final void reset(SimState state)
        {
        state.schedule.reset();
        resetQueues();
        }
        
    /** Returns FALSE if nothing was stepped -- the schedule is exhausted or time has run out. */
    public boolean step()
        {
        boolean returnval = false;
        synchronized(state.schedule)
            {
            // grab the before and after queues so no one add to them
            // while we're using them.
            Steppable[] _before2 = before2;
            Steppable[] _after2 = after2;
            System.arraycopy(before,0,before2,0,beforeSize);
            System.arraycopy(after,0,after2,0,afterSize);
            int _beforeSize = beforeSize;
            int _afterSize = afterSize;
            afterSize = 0;
            beforeSize = 0;
            
            // do the before stuff first
            for(int x=0;x<_beforeSize;x++)
                _before2[x].step(state);
            
            // step the schedule
            returnval = state.schedule.step(state);
            
            // do the after stuff
            for(int x=0;x<_afterSize;x++)
                _after2[x].step(state);
            }
        return returnval;
        }
        
        
    /** Roughly doubles the array size, retaining the existing elements */
    protected Steppable[] increaseSubsteps(Steppable[] substeps)
        {
        Steppable[] newsubstep = new Steppable[substeps.length*2+1];
        System.arraycopy(substeps,0,newsubstep,0,substeps.length);
        return newsubstep;
        }


    /** Schedules an item to occur (in no particular order) immediately before 
        the schedule is stepped on the next time step (not including blank steps).  
        Pass in FALSE to indicate you want to be immediately BEFORE the next timestep;
        pass in TRUE if you want to be immediately AFTER the next time step (the more common
        situation).  Returns false if the current time is AFTER_SIMULATION or if the event is null.
        
        <p>Why would you use this method?  Primarily to get things scheduled which aren't stored
        in the Schedule itself, so it can be serialized out without them. 
    */
        
    public boolean scheduleImmediatelyBefore(Steppable event)
        {
        return _scheduleImmediate(false, event);
        }
        
    /** Schedules an item to occur (in no particular order) immediately after
        the schedule is stepped on the next time step (not including blank steps).  
        Pass in FALSE to indicate you want to be immediately BEFORE the next timestep;
        pass in TRUE if you want to be immediately AFTER the next time step (the more common
        situation).  Returns false if the current time is AFTER_SIMULATION or if the event is null.
        
        <p>Why would you use this method?  Primarily to get things scheduled which aren't stored
        in the Schedule itself, so it can be serialized out without them. 
    */
        
    public boolean scheduleImmediatelyAfter(Steppable event)
        {
        return _scheduleImmediate(true, event);
        }
        
    /** Schedules an item to occur (in no particular order) immediately before or immediately after
        the schedule is stepped on the next time step (not including blank steps).  
        Pass in FALSE to indicate you want to be immediately BEFORE the next timestep;
        pass in TRUE if you want to be immediately AFTER the next time step (the more common
        situation).  Returns false if the current time is AFTER_SIMULATION or if the event is null.
        
        <p>Why would you use this method?  Primarily to get things scheduled which aren't stored
        in the Schedule itself, so it can be serialized out without them. 

        @deprecated use scheduleImmediatelyBefore and scheduleImmediatelyAfter instead
    */
        
    public boolean scheduleImmediate(boolean immediatelyAfter, Steppable event)
        {
        return _scheduleImmediate(immediatelyAfter, event);
        }
                
    boolean _scheduleImmediate(boolean immediatelyAfter, Steppable event)
        {
        synchronized(state.schedule)
            {
            if (event == null || state.schedule.getTime() >= Schedule.AFTER_SIMULATION)
                {
                if (event == null)
                    {
                    throw new IllegalArgumentException("The provided Steppable is null");
                    }
                else if (state.schedule.getTime() >= Schedule.AFTER_SIMULATION)
                    {
                    throw new IllegalArgumentException("The simulation is over and the item cannot be scheduled.");
                    }
                }
            if (immediatelyAfter)
                {
                if (afterSize == after.length)
                    {
                    after = increaseSubsteps(after);
                    after2 = new Steppable[after.length];
                    }
                after[afterSize++] = event;
                }
            else
                {
                if (beforeSize == before.length)
                    {
                    before = increaseSubsteps(before);
                    before2 = new Steppable[before.length];
                    }
                before[beforeSize++] = event;
                }
            }
        return true;
        }
    
    /** Schedules an item to occur (in no particular order) immediately before
        all future steps the Schedule takes (not including blank steps).  
        Pass in FALSE to indicate you want to be immediately BEFORE the next timestep;
        pass in TRUE if you want to be immediately AFTER the next time step (the more common
        situation).  Returns a Stoppable, or null if the current time is AFTER_SIMULATION or if the event is null.
        The recurrence will continue until state.schedule.getTime() >= AFTER_SIMULATION, state.schedule is cleared out,
        or the Stoppable's stop() method is called, whichever happens first.
        
        <p>Why would you use this method?  Primarily to get things scheduled which aren't stored
        in the Schedule itself, so it can be serialized out without them.
    */
        
    public Stoppable scheduleRepeatingImmediatelyBefore(Steppable event)
        {
        return _scheduleImmediateRepeat(false, event);
        }
        

    /** Schedules an item to occur (in no particular order) immediately after
        all future steps the Schedule takes (not including blank steps).  
        Pass in FALSE to indicate you want to be immediately BEFORE the next timestep;
        pass in TRUE if you want to be immediately AFTER the next time step (the more common
        situation).  Returns a Stoppable, or null if the current time is AFTER_SIMULATION or if the event is null.
        The recurrence will continue until state.schedule.getTime() >= AFTER_SIMULATION, state.schedule is cleared out,
        or the Stoppable's stop() method is called, whichever happens first.
        
        <p>Why would you use this method?  Primarily to get things scheduled which aren't stored
        in the Schedule itself, so it can be serialized out without them.
    */
        
    public Stoppable scheduleRepeatingImmediatelyAfter(Steppable event)
        {
        return _scheduleImmediateRepeat(true, event);
        }
        
    /** Schedules an item to occur (in no particular order) immediately before or immediately after
        all future steps the Schedule takes (not including blank steps).  
        Pass in FALSE to indicate you want to be immediately BEFORE the next timestep;
        pass in TRUE if you want to be immediately AFTER the next time step (the more common
        situation).  Returns a Stoppable, or null if the current time is AFTER_SIMULATION or if the event is null.
        The recurrence will continue until state.schedule.getTime() >= AFTER_SIMULATION, state.schedule is cleared out,
        or the Stoppable's stop() method is called, whichever happens first.
        
        <p>Why would you use this method?  Primarily to get things scheduled which aren't stored
        in the Schedule itself, so it can be serialized out without them.

        @deprecated use scheduleRepeatingImmediatelyBefore and scheduleRepeatingImmediatelyAfter instead
    */
        
    public Stoppable scheduleImmediateRepeat(boolean immediatelyAfter, Steppable event)
        {
        return _scheduleImmediateRepeat(immediatelyAfter, event);
        }
        
    Stoppable _scheduleImmediateRepeat(boolean immediatelyAfter, Steppable event)
        {
        Repeat r = new Repeat(immediatelyAfter, event);
        if (scheduleImmediate(immediatelyAfter, r)) return r;
        else return null;
        }

    /** Schedules an item to occur when the user starts or stops the simulator, or when it stops on its own accord.
        If atEnd is TRUE, then the item is scheduled to occur when the finish() method is executed.  If
        atEnd is FALSE, then the item is scheduled to occur when the start() method is executed.
        Returns true if scheduling succeeded.
        
        @deprecated use scheduleAtStart and scheduleAtEnd instead
    */
    public boolean scheduleAtExtreme(Steppable event, boolean atEnd)
        {
        return _scheduleAtExtreme(event, atEnd);
        }
    
    // this has been pulled out to avoid deprecation warnings for scheduleAtStart and scheduleAtEnd
    boolean _scheduleAtExtreme(Steppable event, boolean atEnd)
        {
        synchronized(state.schedule)
            {
            if (event == null || state.schedule.getTime() >= Schedule.AFTER_SIMULATION)
                {
                if (event == null)
                    {
                    throw new IllegalArgumentException("The provided Steppable is null");
                    }
                else if (state.schedule.getTime() >= Schedule.AFTER_SIMULATION)
                    {
                    throw new IllegalArgumentException("The simulation is over and the item cannot be scheduled.");
                    }
                }
            if (atEnd)
                {
                if (finishSize == finish.length)
                    {
                    finish = increaseSubsteps(finish);
                    finish2 = new Steppable[finish.length];
                    }
                finish[finishSize++] = event;
                }
            else
                {
                if (startSize == start.length)
                    {
                    start = increaseSubsteps(start);
                    start2 = new Steppable[start.length];
                    }
                start[startSize++] = event;
                }
            }
        return true;
        }
        
    /** Schedules an item to occur when the user starts the simulator (when the start() method is executed) or loads one (when load() is executed).  Identical to scheduleAtExtreme(event,false). Returns true if scheduling succeeded. 
     */
    public boolean scheduleAtStart(Steppable event)
        {
        return _scheduleAtExtreme(event,false);
        }
        
    /** Schedules an item to occur when the user stops the simulator (when the stop() method is executed), when it stops on its own accord, or when the user has load()ed another simulation to replace it.  Identical to scheculeAtExtreme(event,false).  Returns true if scheduling succeeded. 
     */
    public boolean scheduleAtEnd(Steppable event)
        {
        return _scheduleAtExtreme(event,true);
        }
        

    /** Handles repeated steps.  This is done by wrapping the Steppable with a Repeat object
        which is itself Steppable, and on its step calls its subsidiary Steppable, then reschedules
        itself.  Repeat is stopped by setting its subsidiary to null, and so the next time it's
        scheduled it won't reschedule itself (or call the subsidiary).   A private class for
        GUIState.  Notice almost exactly the same as Schedule.Repeat.  */
    
    class Repeat implements Steppable, Stoppable
        {
        protected boolean immediatelyAfter;
        protected Steppable step;  // if null, does not reschedule
        
        public Repeat(boolean immediatelyAfter, Steppable step)
            {
            this.immediatelyAfter = immediatelyAfter;
            this.step = step;
            }
        
        public synchronized void step(SimState state)
            {
            if (step!=null)
                {
                try
                    {
                    scheduleImmediate(immediatelyAfter,this);
                    }
                catch (IllegalArgumentException e) { /* Only occurs if time has run out */}
                step.step(state);
                }
            }
        
        public synchronized void stop()  
            {
            step = null;
            }
        }
    }
    
