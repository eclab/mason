/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;
import ec.util.*;
import java.util.*;
import java.io.*;
import java.util.zip.*;
import java.text.*;

/** SimState represents the simulation proper.  Your simulations generally will contain one top-level object which subclasses from SimState.

    <p>A SimState contains the random number generator and the simulator's schedule.  You should not change the schedule to another Schedule object.

    <p>When a simulation is begun, SimState's start() method is called.  Then the schedule is stepped some N times.  Last, the SimState's finish() method is called, and the simulation is over.

    <p>SimStates are serializable; if you wish to be able to checkpoint your simulation and read from checkpoints, you should endeavor to make all objects in the simulation serializable as well.  Prior to serializing to a checkpoint, preCheckpoint() is called.  Then after serialization, postCheckpoint() is called.  When a SimState is loaded from a checkpoint, awakeFromCheckpoint() is called to give you a chance to make any adjustments.  SimState also implements several methods which call these methods and then serialize the SimState to files and to streams.

    <p>SimState also maintains a private registry of AsynchronousSteppable objects (such as YoYoYo), and handles pausing and resuming
    them during the checkpointing process, and killing them during finish() in case they had not completed yet.

    <p>If you override any of the methods foo() in SimState, should remember to <b>always</b> call super.foo() for any such method foo().
*/

public class SimState implements java.io.Serializable
    {
    /** The SimState's random number generator */
    public MersenneTwisterFast random;
    
    /** SimState's schedule */
    public Schedule schedule;
    
    // All registered AsynchronousSteppables
    HashSet asynchronous = new HashSet();
    // Lock for accessing the HashSet
    Object asynchronousLock = new boolean[1];  // an array is a unique, serializable object
    // Are we cleaning house and replacing the HashSet?
    public boolean cleaningAsynchronous = false;
        
    /** Creates a SimState with a new random number generator initialized to the given seed,
        plus a new, empty schedule. */
    public SimState(long seed)
        {
        this(new MersenneTwisterFast(seed));
        }
    
    /** Creates a SimState with a new, empty Schedule and the provided random number generator. */
    public SimState(MersenneTwisterFast random)
        {
        this(random, new Schedule());
        }
        
    /** Creates a SimState with the provided random number generator and schedule. */
    public SimState(MersenneTwisterFast random, Schedule schedule)
        {
        this.random = random;
        this.schedule = schedule;
        }
    
    public void setRandom(MersenneTwisterFast random)
        {
        this.random = random;
        }
    
    /** Called immediately prior to starting the simulation, or in-between
        simulation runs.  This gives you a chance to set up initially,
        or reset from the last simulation run. The default version simply
        replaces the Schedule with a completely new one.  */
    public void start()
        {
        // just in case
        cleanupAsynchronous();
        // reset schedule
        schedule.reset();
        }
        
    /** Called either at the proper or a premature end to the simulation. 
        If the user quits the program, this function may not be called.  It is
        possible for this method to be called multiple times.  If you need to
        check for this possibility, the easiest way is to set a flag in start()
        and clear it in the first finish(). */
    public void finish()
        {
        kill();  // cleans up asynchroonous and resets the schedule, a good ending
        }

    /** A Steppable on the schedule can call this method to cancel the simulation.
        All existing YoYoYos are stopped, and then the schedule is
        reset.  YoYoYos, ParallelSteppables, 
        and non-main threads should not call this method directly -- it will deadlock.
        Instead, they may kill the simulation by scheduling a Steppable
        for the next timestep which calls state.kill(). */
    public void kill()
        {
        cleanupAsynchronous();
        schedule.pushToAfterSimulation();
        }

    /** Registers an AsynchronousSteppable to get its pause() method called prior to checkpointing,
        its resume() method to be called after checkpointing or recovery, and its stop()
        method to be called at finish() time.  The purpose of the addToCleanup() method is to provide
        the simulation with a way of stopping existing threads which the user has created in the background.
        
        <P> An AsynchronousSteppable cannot be added multiple times
        to the same registry -- if it's there it's there.  Returns false if the AsynchronousSteppable could
        not be added, either because the simulation is stopped or in the process of finish()ing.
    */
    public boolean addToAsynchronousRegistry(AsynchronousSteppable stop)
        {
        if (stop==null) return false;
        synchronized(asynchronousLock) 
            { 
            if (cleaningAsynchronous) return false;  
            asynchronous.add(stop);
            return true;
            }
        }
        
    /**
       Unregisters an AsynchronousSteppable from the asynchronous registry.
    */
    public void removeFromAsynchronousRegistry(AsynchronousSteppable stop)
        {
        if (stop==null) return;
        synchronized(asynchronousLock) 
            { 
            if (!cleaningAsynchronous) 
                asynchronous.remove(stop);
            }
        }
        
    /** Returns all the AsynchronousSteppable items presently in the registry.  The returned array is not used internally -- you are free to modify it. */
    public AsynchronousSteppable[] asynchronousRegistry()
        {
        synchronized(asynchronousLock)
            {
            AsynchronousSteppable[] b = new AsynchronousSteppable[asynchronous.size()];
            int x = 0;
            Iterator i = asynchronous.iterator();
            while(i.hasNext())
                b[x++] = (AsynchronousSteppable)(i.next());
            return b;
            }
        }
        
    /*
      Calls all the registered Asynchronnous.  During this period, any methods which attempt to
      register things for the schedule will simply be ignored.  
    */
    // perhaps use a LinkedHashSet instead of a HashSet?
    void cleanupAsynchronous()
        {
        AsynchronousSteppable[] b = null;
        synchronized(asynchronousLock)
            {
            b = asynchronousRegistry();
            cleaningAsynchronous = true;
            }
        final int len = b.length;
        for(int x=0;x<len;x++) b[x].stop();
        synchronized(asynchronousLock) 
            {
            asynchronous = new HashSet(asynchronous.size());
            cleaningAsynchronous = false;
            }
        }


    /** Called just before the SimState is being checkpointed (serialized out to a file to be
        unserialized and fired up at a future time).  You should override this to prepare 
        your SimState object appropriately. Be sure to call super.preCheckpoint(). */
    public void preCheckpoint()
        {
        AsynchronousSteppable[] b = asynchronousRegistry();
        final int len = b.length;
        for(int x=0;x<len;x++) b[x].pause();
        }
    
    /** Called just after the SimState was checkpointed (serialized out to a file to be
        unserialized and fired up at a future time).  You cam override this as you see fit. 
        Be sure to call super.postCheckpoint(). */
    public void postCheckpoint()
        {
        AsynchronousSteppable[] b = asynchronousRegistry();
        final int len = b.length;
        for(int x=0;x<len;x++) b[x].resume();
        }

    /** Called after the SimState was created by reading from a checkpointed object.  You should
        set up your SimState in any way necessary (reestablishing file connections, etc.) to fix
        anything that may no longer exist.  Be sure to call super.awakeFromCheckpoint(). */
    public void awakeFromCheckpoint()
        {
        AsynchronousSteppable[] b = asynchronousRegistry();
        final int len = b.length;
        for(int x=0;x<len;x++) b[x].resume();
        }

    /** Serializes out the SimState, and the entire simulation state (not including the graphical interfaces)
        to the provided stream. Calls preCheckpoint() before and postCheckpoint() afterwards.
        Throws an IOException if the stream becomes invalid (prematurely closes, etc.).  Does not close or flush
        the stream. */
    public void writeToCheckpoint(OutputStream stream)
        throws IOException
        {
        preCheckpoint();

        GZIPOutputStream g = 
            new GZIPOutputStream(
                new BufferedOutputStream(stream));

        ObjectOutputStream s = 
            new ObjectOutputStream(g);
            
        s.writeObject(this);
        s.flush();
        g.finish();  // need to force out the gzip stream AND manually flush it.  Java's annoying.  Took a while to find this bug...
        g.flush();
        postCheckpoint();
        }
    
    /** Writes the state to a checkpoint and returns the state.
        If an exception is raised, it is printed and null is returned. */
    public SimState writeToCheckpoint(File file)
        {
        try {
            FileOutputStream f = new FileOutputStream(file);
            writeToCheckpoint(f);
            f.close();
            return this;
            }
        catch(Exception e) { e.printStackTrace(); return null; }
        }
    
    /** Creates a SimState from checkpoint.  If an exception is raised, it is printed and null is returned. */
    public static SimState readFromCheckpoint(File file)
        {
        try {
            FileInputStream f = new FileInputStream(file);
            SimState state = readFromCheckpoint(f);
            f.close();
            return state;
            }
        catch(Exception e) { e.printStackTrace(); return null; }
        }

    /** Creates and returns a new SimState object read in from the provided stream.  Calls awakeFromCheckpoint().
        Throws an IOException if the stream becomes invalid (prematurely closes etc.).  Throws a ClassNotFoundException
        if a serialized object is not found in the CLASSPATH and thus cannot be created.  Throws an OptionalDataException
        if the stream is corrupted.  Throws a ClassCastException if the top-level object is not actually a SimState.
        Does not close or flush the stream. */
    public static SimState readFromCheckpoint(InputStream stream)
        throws IOException, ClassNotFoundException, OptionalDataException, ClassCastException
        {
        ObjectInputStream s = 
            new ObjectInputStream(
                new GZIPInputStream (
                    new BufferedInputStream(stream)));
        SimState state = (SimState) (s.readObject());
        state.awakeFromCheckpoint();
        return state;
        }
    
    static int indexAfterArgumentForKey(String key, String[] args, int startingAt)
        {
        for(int x=0;x<args.length-1;x++)  // key can't be the last string
            if (args[x].equalsIgnoreCase(key))
                return x + 2;
        return args.length;
        }

    static boolean keyExists(String key, String[] args, int startingAt)
        {
        for(int x=0;x<args.length;x++)  // key can't be the last string
            if (args[x].equalsIgnoreCase(key))
                return true;
        return false;
        }

    static String argumentForKey(String key, String[] args, int startingAt)
        {
        for(int x=0;x<args.length-1;x++)  // key can't be the last string
            if (args[x].equalsIgnoreCase(key))
                return args[x + 1];
        return null;
        }
    
    long job = 0;
    long seed = 0;  // considered bad
        
    /** Returns the job number set by the doLoop(...) facility.  This number
        is not incremented by the GUI. */
    public long job()
        {
        return job;
        }

    /** Calls doLoop(MakesSimState,args), passing in a MakesSimState which creates
        SimStates of the provided Class c, using the constructor new <simState>(<random seed>). */
    public static void doLoop(final Class c, String[] args)
        {
        doLoop(new MakesSimState()
            {
            public SimState newInstance(long seed, String[] args)
                {
                try
                    {
                    return (SimState)(c.getConstructor(new Class[] { Long.TYPE }).newInstance(new Object[] { new Long(seed) } ));
                    }
                catch (Exception e)
                    {
                    throw new RuntimeException("Exception occurred while trying to construct the simulation: " + e);
                    }
                }
            public Class simulationClass() { return c; }
            }, args);
        }
    
    /** A convenient top-level loop for the simulation command-line.  Takes a MakesSimState which is
        responsible for providing a SimState to run the simulation on, plus the application's argument
        list in args.  This loop is capable of:
        <ul>
        <li> Repeating a job multiple times
        </ul>
    */
    public static void doLoop(MakesSimState generator, String[] args)
        {
        // print help?
        if (keyExists("-help", args, 0))
            {
            System.err.println(
                "Format:           java " + generator.simulationClass().getName() + " \\\n" +
                "                       [-help] [-repeat R] [-seed S] [-until U] \\\n" +
                "                       [-for F] [-time T] [-docheckpoint D] [-checkpoint C] \n\n" +
                "-help             Shows this message and exits.\n\n" +
                "-repeat R         Long value > 0: Runs the job R times.  Unless overridden by a\n" +
                "                  checkpoint recovery (see -checkpoint), the random seed for\n" +
                "                  each job is the provided -seed plus the job# (starting at 0).\n" +
                "                  Default: runs once only: job number is 0.\n\n" +
                "-seed S           Long value not 0: the random number generator seed, unless \n" +
                "                  overridden by a checkpoint recovery (see -checkpoint).\n" +
                "                  Default: the system time in milliseconds.\n\n" +
                "-until U          Double value >= 0: the simulation must stop when the\n" +
                "                  simulation time U has been reached or exceeded.\n" +
                "                  Default: don't stop.\n\n" +
                "-for N            Long value >= 0: the simulation must stop when N\n" +
                "                  simulation steps have transpired.\n" +
                "                  Default: don't stop.\n\n" +
                "-time T           Long value >= 0: print a timestamp every T simulation steps.\n" +
                "                  If 0, nothing is printed.\n" +
                "                  Default: auto-chooses number of steps based on how many\n" +
                "                  appear to fit in one second of wall clock time.  Rounds to\n" +
                "                  one of 1, 2, 5, 10, 25, 50, 100, 250, 500, 1000, 2500, etc.\n\n" +
                "-docheckpoint D   Long value > 0: checkpoint every D simulation steps.\n" +
                "                  Default: never.\n" +
                "                  Checkpoint files named\n"+
                "                  <steps>.<job#>." + 
                generator.simulationClass().getName().substring(generator.simulationClass().getName().lastIndexOf(".") + 1) + 
                ".checkpoint\n\n" + 
                "-checkpoint C     String: loads the simulation from file C, recovering the job\n" +
                "                  number and the seed.  If the checkpointed simulation was begun\n" +
                "                  on the command line but was passed through the GUI for a while\n" +
                "                  (even multiply restarted in the GUI) and then recheckpointed,\n" +
                "                  then the seed and job numbers will be the same as when they\n" +
                "                  were last on the command line.  If the checkpointed simulation\n" +
                "                  was begun on the GUI, then the seed will not be recovered and\n"+
                "                  job will be set to 0. Further jobs and seeds are incremented\n" +
                "                  from the recovered job and seed.\n" +
                "                  Default: starts a new simulation rather than loading one, at\n" +
                "                  job 0 and with the seed given in -seed.\n");
            System.exit(0);
            }

        java.text.NumberFormat n = java.text.NumberFormat.getInstance();
        n.setMinimumFractionDigits(0);
        System.err.println("MASON Version " + n.format(version()) + ".  For further options, try adding ' -help' at end.");

        // figure the checkpoint modulo
        double until = Double.POSITIVE_INFINITY;
        String until_s = argumentForKey("-until", args, 0);
        if (until_s != null)
            try
                {
                until = Double.parseDouble(until_s);
                if (until < 0.0) throw new Exception();
                }
            catch (Exception e)
                {
                throw new RuntimeException("Invalid 'until' value: " + until_s + ", must be a positive real value");
                }

        long seed = System.currentTimeMillis();
        String seed_s = argumentForKey("-seed", args, 0);
        if (seed_s != null)
            try
                {
                seed = Long.parseLong(seed_s);
                if (seed == 0) throw new Exception();
                }
            catch (Exception e)
                {
                throw new RuntimeException("Invalid 'seed' value: " + seed_s + ", must be a non-zero integer, or nonexistent to seed by clock time");
                }
        
        long _for = -1;
        String _for_s = argumentForKey("-for", args, 0);
        if (_for_s != null)
            try
                {
                _for = Long.parseLong(_for_s);
                if (_for < 0) throw new Exception();
                }
            catch (Exception e)
                {
                throw new RuntimeException("Invalid 'for' value: " + _for_s + ", must be an integer >= 0");
                }
        
        long time = -1;
        String time_s = argumentForKey("-time", args, 0);
        if (time_s != null)
            try
                {
                time = Long.parseLong(time_s);
                if (time < 0) throw new Exception();
                }
            catch (Exception e)
                {
                throw new RuntimeException("Invalid 'time' value: " + time_s + ", must be a positive integer");
                }
        
        long cmod = 0;
        String cmod_s = argumentForKey("-docheckpoint", args, 0);
        if (cmod_s != null)
            try
                {
                cmod = Long.parseLong(cmod_s);
                if (cmod <= 0) throw new Exception();
                }
            catch (Exception e)
                {
                throw new RuntimeException("Invalid checkpoint modulo: " + cmod_s + ", must be a positive integer");
                }
        
        long repeat = 1;
        String repeat_s = argumentForKey("-repeat", args, 0);
        if (repeat_s != null)
            try
                {
                repeat = Long.parseLong(repeat_s);
                if (repeat <= 0) throw new Exception();
                }
            catch (Exception e)
                {
                throw new RuntimeException("Invalid repeat value: " + repeat + ", must be a positive integer");
                }
       
        // okay, now we actually get down to brass tacks
        
        long job = 0;
        for(long rep = 0 ; rep < repeat; rep++)
            {
            SimState state = null;
        
            // start from checkpoint?
            String checkpointFile = argumentForKey("-checkpoint", args, 0);
            if (rep == 0 && checkpointFile!=null)  // only job 0 loads from checkpoint
                {
                System.err.println("Loading from checkpoint " + checkpointFile);
                state = SimState.readFromCheckpoint(new File(checkpointFile));
                if (state == null)   // there was an error -- it got printed out to the screen, so just quit
                    System.exit(1);
                else if (state.getClass() != generator.simulationClass())  // uh oh, wrong simulation stored in the file!
                    {
                    System.err.println("Checkpoint contains some other simulation: " + state + ", should have been of class " + generator.simulationClass());
                    System.exit(1);
                    }
                                        
                job = state.job;
                if (state.seed != 0) // likely good seed from the command line earlier
                    {
                    seed = state.seed;
                    System.err.println("Recovered job: " + job + " Seed: " + seed);
                    }
                else System.err.println("Renamed job: " + job + " (unknown seed)");
                }

            // ...or should we start fresh?
            if (state==null)  // no checkpoint file requested
                {
                state = generator.newInstance(seed,args);
                state.job = job;
                state.seed = seed;
                System.err.println("Job: " + job + " Seed: " + seed);
                System.err.println("Starting " + state.getClass().getName());
                state.start();
                }
            
            job++;
            seed++;
                        
            NumberFormat rateFormat = NumberFormat.getInstance();
            rateFormat.setMaximumFractionDigits(5);
            rateFormat.setMinimumIntegerDigits(1);

            // do the loop
            boolean retval = false;
            long steps = 0;
            long clock;
            long oldClock = System.currentTimeMillis();
            Schedule schedule = state.schedule;
            long firstSteps = schedule.getSteps();
            
            while((_for == -1 || steps < _for) && schedule.time() <= until)
                {
                if (!schedule.step(state)) 
                    {
                    retval=true; 
                    break;
                    }
                steps = schedule.getSteps();
                if (time < 0)  // don't know how long to make the time yet
                    {
                    if (System.currentTimeMillis() - oldClock > 1000L)  // time to set the time
                        {
                        time = figureTime(steps - firstSteps);
                        }
                    }
                if (time > 0 && steps % time == 0)
                    {
                    clock = System.currentTimeMillis();
                    System.err.println("Steps: " + steps + " Time: " + state.schedule.getTimestamp("At Start", "Done") + " Rate: " + rateFormat.format((1000.0 *(steps - firstSteps)) / (clock - oldClock)));
                    firstSteps = steps;
                    oldClock = clock;
                    }
                if (cmod > 0 && steps % cmod == 0)
                    {
                    String s = "" + steps + "." + state.job +  "." + state.getClass().getName().substring(state.getClass().getName().lastIndexOf(".") + 1) + ".checkpoint";
                    System.err.println("Checkpointing to file: " + s);
                    state.writeToCheckpoint(new File(s));
                    }
                }
                
            state.finish();
            
            if (retval) System.err.println("Exhausted");
            else System.err.println("Quit");
            }
        }
    
    public static double version()
        {
        return 14.0;
        }
    
    // compute how much time per step 
    // it's possible this could go into an infinite loop if time is gigantic
    // but that's not likely.  Otherwise takes O(lg(time)) time, which is
    // reasonable for a long
    static long figureTime(long time)
        {
        long n = 1;
        while(true)
            {
            if (n >= time) return n;
            if ((n*10)/4 >= time) return (n*10)/4;
            if ((n*10)/2 >= time) return (n*10)/2;
            n = n*10;
            }
        }

    }

