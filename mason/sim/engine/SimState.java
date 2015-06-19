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

    <p>SimState also maintains a private registry of AsynchronousSteppable objects, and handles pausing and resuming
    them during the checkpointing process, and killing them during finish() in case they had not completed yet.

    <p>If you override any of the methods foo() in SimState, should remember to <b>always</b> call super.foo() for any such method foo().
*/

public class SimState implements java.io.Serializable
    {
    private static final long serialVersionUID = 1;

    /** The SimState's random number generator */
    public MersenneTwisterFast random;
    
    /** SimState's schedule */
    public Schedule schedule;
    
    // All registered AsynchronousSteppables
    HashSet asynchronous = new HashSet();
    // Lock for accessing the HashSet
    Object asynchronousLock = new boolean[1];  // an array is a unique, serializable object
    // Are we cleaning house and replacing the HashSet?
    boolean cleaningAsynchronous = false;
        
    SimState(long seed, MersenneTwisterFast random, Schedule schedule)
        {
        this.random = random;
        this.schedule = schedule;
        this.seed = (int) seed;   // force to 32 bits since that's what MTF will be using anyway
        }

    /** Creates a SimState with a new random number generator initialized to the given seed,
        plus a new, empty schedule. */
    public SimState(long seed)
        {
        this(seed, new MersenneTwisterFast(seed), new Schedule());
        }
    
    /** Creates a SimState with the given random number generator and schedule, and
        sets the seed to a bogus value (0).  This should only be used by SimState 
        subclasses which need to use an existing random number generator and schedule.
    */
    protected SimState(MersenneTwisterFast random, Schedule schedule)
        {
        this(0, random, schedule);  // 0 is a bogus value.  In fact, MT can't have 0 as its seed value.
        }
                
    /** Creates a SimState with the schedule, creating a new random number generator.
        This should only be used by SimState subclasses which need
        to use an existing schedule.
    */
    protected SimState(long seed, Schedule schedule)
        {
        this(seed, new MersenneTwisterFast(seed), schedule);
        }

    /** Creates a SimState with a new schedule, the provided random number generator,
        and a bogus seed (0).  This should only be used by SimState subclasses which need
        to use an existing random number generator.
    */
    protected SimState(MersenneTwisterFast random)
        {
        this(0, random, new Schedule());  // 0 is a bogus value.  In fact, MT can't have 0 as its seed value.
        }

    public void setSeed(long seed)
        {
        seed = (int) seed;  // force to 32 bits since that's what MTF will be using anyway
        random = new MersenneTwisterFast(seed);
        this.seed = seed;
        }
                
    /** Primes the generator.  Mersenne Twister seeds its first 624 numbers using a basic
        linear congruential generator; thereafter it uses the MersenneTwister algorithm to
        build new seeds.  Those first 624 numbers are generally just fine, but to be extra
        safe, you can prime the generator by calling nextInt() on it some (N>1) * 624 times.
        This method does exactly that, presently with N=2 ( + 1 ). */
    public static MersenneTwisterFast primeGenerator(MersenneTwisterFast generator)
        {
        // 624 = MersenneTwisterFast.N  which is private duh
        for(int i = 0; i < 624 * 2 + 1; i++)
            generator.nextInt();
        return generator;
        }

    /** Called immediately prior to starting the simulation, or in-between
        simulation runs.  This gives you a chance to set up initially,
        or reset from the last simulation run. The default version simply
        replaces the Schedule with a completely new one.  */
    public void start()
        {
        // prime the generator so it's got better statistial properties
        random = primeGenerator(random);
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
        kill();  // cleans up asynchronous and resets the schedule, a good ending
        }

    /** A Steppable on the schedule can call this method to cancel the simulation.
        All existing AsynchronousSteppables are stopped, and then the schedule is
        reset.  AsynchronousSteppables, ParallelSequences, 
        and non-main threads should not call this method directly -- it will deadlock.
        Instead, they may kill the simulation by scheduling a Steppable
        for the next timestep which calls state.kill(). */
    public void kill()
        {
        cleanupAsynchronous();
        schedule.clear();
        schedule.seal();
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
        for(int x=0;x<len;x++) b[x].resume(false);
        }

    /** Called after the SimState was created by reading from a checkpointed object.  You should
        set up your SimState in any way necessary (reestablishing file connections, etc.) to fix
        anything that may no longer exist.  Be sure to call super.awakeFromCheckpoint(). */
    public void awakeFromCheckpoint()
        {
        AsynchronousSteppable[] b = asynchronousRegistry();
        final int len = b.length;
        for(int x=0;x<len;x++) b[x].resume(true);
        }

    /** Serializes out the SimState, and the entire simulation state (not including the graphical interfaces)
        to the provided stream. Calls preCheckpoint() before and postCheckpoint() afterwards.
        Throws an IOException if the stream becomes invalid (prematurely closes, etc.).  Does not close or flush
        the stream. */
    public void writeToCheckpoint(OutputStream stream) throws IOException
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
        FileOutputStream f = null;
        try {
            f = new FileOutputStream(file);
            writeToCheckpoint(f);
            f.close();
            return this;
            }
        catch(Exception e) 
            {
            try { if (f != null) f.close(); } catch (Exception e2) { }
            e.printStackTrace(); 
            return null; 
            }
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
    
    static boolean keyExists(String key, String[] args)
        {
        for(int x=0;x<args.length;x++)
            if (args[x].equalsIgnoreCase(key))
                return true;
        return false;
        }

    static String argumentForKey(String key, String[] args)
        {
        for(int x=0;x<args.length-1;x++)  // if a key has an argument, it can't be the last string
            if (args[x].equalsIgnoreCase(key))
                return args[x + 1];
        return null;
        }
    
    long job = 0;
    long seed = 0;  // considered bad value
        
    /** Returns the seed set by the doLoop(...) facility and by the constructor.
        Only to be used for GUIs to display possible seed values.  */
    public long seed()
        {
        return (int) seed;
        }

    public void setJob(long job)
        {
        this.job = job;
        }
        
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
                    return (SimState)(c.getConstructor(new Class[] { Long.TYPE }).newInstance(new Object[] { Long.valueOf(seed) } ));
                    }
                catch (Exception e)
                    {
                    throw new RuntimeException("Exception occurred while trying to construct the simulation " + c + "\n" + e);
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
    public static void doLoop(final MakesSimState generator, final String[] args)
        {
        // print help?
        if (keyExists("-help", args))
            {
            System.err.println(
                "Format:           java " + generator.simulationClass().getName() + " \\\n" +
                "                       [-help] [-repeat R] [-parallel P] [-seed S] \\\n" +
                "                       [-until U] [-for F] [-time T] [-docheckpoint D] \\\n" +
                "                       [-checkpoint C] [-quiet] \n\n" +
                "-help             Shows this message and exits.\n\n" +
                "-repeat R         Long value > 0: Runs R jobs.  Unless overridden by a\n" +
                "                  checkpoint recovery (see -checkpoint), the random seed for\n" +
                "                  each job is the provided -seed plus the job# (starting at 0).\n" +
                "                  Default: runs once only: job number is 0.\n\n" +
                "-parallel P       Long value > 0: Runs P separate batches of jobs in parallel,\n" +
                "                  each one containing R jobs (as specified by -repeat).  Each\n" +
                "                  batch has its own independent set of checkpoint files.  Job\n" +
                "                  numbers are 0, P, P*2, ... for the first batch, then 1, P+1,\n" +
                "                  P*2+1, ... for the second batch, then 2, P+2, P*2+2, ... for\n" +
                "                  the third batch, and so on.  -parallel may not be used in\n" +
                "                  combination with -checkpoint.\n" +
                "                  Default: one batch only (no parallelism).\n\n" +
                "-seed S           Long value not 0: the random number generator seed, unless \n" +
                "                  overridden by a checkpoint recovery (see -checkpoint).\n" +
                "                  Default: the system time in milliseconds.\n\n" +
                "-until U          Double value >= 0: the simulation must stop when the\n" +
                "                  simulation time U has been reached or exceeded.\n" +
                "                  If -for is also included, the simulation terminates when\n" + 
                "                  either of them is completed.\n" + 
                "                  Default: don't stop.\n\n" +
                "-for N            Long value >= 0: the simulation must stop when N\n" +
                "                  simulation steps have transpired.   If -until is also\n" +
                "                  included, the simulation terminates when either of them is\n" + 
                "                  completed.\n" +
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
                "                  job 0 and with the seed given in -seed.\n\n" + 
                "-quiet            Does not print messages except for errors and warnings.\n" + 
                "                  This option implies -time 0.\n" +
                "                  Default: prints all messages.\n"
                );
            System.exit(0);
            }

        final boolean quiet = keyExists("-quiet", args);

        java.text.NumberFormat n = java.text.NumberFormat.getInstance();
        n.setMinimumFractionDigits(0);
        if (!quiet) System.err.println("MASON Version " + n.format(version()) + ".  For further options, try adding ' -help' at end.");

        // figure the checkpoint modulo
        double _until = Double.POSITIVE_INFINITY;
        String until_s = argumentForKey("-until", args);
        if (until_s != null)
            try
                {
                _until = Double.parseDouble(until_s);
                if (_until < 0.0) throw new Exception();
                }
            catch (Exception e)
                {
                throw new RuntimeException("Invalid 'until' value: " + until_s + ", must be a positive real value");
                }
        final double until = _until; // stupid Java

        long _seed = System.currentTimeMillis();
        String seed_s = argumentForKey("-seed", args);
        if (seed_s != null)
            try
                {
                _seed = Long.parseLong(seed_s);
                if (_seed == 0) throw new Exception();
                }
            catch (Exception e)
                {
                throw new RuntimeException("Invalid 'seed' value: " + seed_s + ", must be a non-zero integer, or nonexistent to seed by clock time");
                }
        final long seed_init = _seed;  // grrrr
        
        long __for = -1;
        String for_s = argumentForKey("-for", args);
        if (for_s != null)
            try
                {
                __for = Long.parseLong(for_s);
                if (__for < 0) throw new Exception();
                }
            catch (Exception e)
                {
                throw new RuntimeException("Invalid 'for' value: " + for_s + ", must be an integer >= 0");
                }
        final long _for = __for;  // argh
        
        long _time = -1;
        String time_s = argumentForKey("-time", args);
        if (time_s != null)
            try
                {
                _time = Long.parseLong(time_s);
                if (_time < 0) throw new Exception();
                }
            catch (Exception e)
                {
                throw new RuntimeException("Invalid 'time' value: " + time_s + ", must be a positive integer");
                }
        final long time_init = _time;  //blah
        
        long _cmod = 0;
        String cmod_s = argumentForKey("-docheckpoint", args);
        if (cmod_s != null)
            try
                {
                _cmod = Long.parseLong(cmod_s);
                if (_cmod <= 0) throw new Exception();
                }
            catch (Exception e)
                {
                throw new RuntimeException("Invalid checkpoint modulo: " + cmod_s + ", must be a positive integer");
                }
        final long cmod = _cmod;
        
        long _repeat = 1;
        String repeat_s = argumentForKey("-repeat", args);
        if (repeat_s != null)
            try
                {
                _repeat = Long.parseLong(repeat_s);
                if (_repeat <= 0) throw new Exception();
                }
            catch (Exception e)
                {
                throw new RuntimeException("Invalid repeat value: " + repeat_s + ", must be a positive integer");
                }
        final long repeat = _repeat;
       
        int _parallel = 1;
        String parallel_s = argumentForKey("-parallel", args);
        if (parallel_s != null)
            try
                {
                _parallel = Integer.parseInt(parallel_s);
                if (_parallel <= 0) throw new Exception();
                }
            catch (Exception e)
                {
                throw new RuntimeException("Invalid parallel value: " + parallel_s + ", must be a positive integer");
                }
        final int parallel = _parallel;
                
        // check for parallelism with checkpoints
        final String checkpointFile = argumentForKey("-checkpoint", args);
        if (parallel > 1 && checkpointFile != null)
            {
            System.err.println("Cannot load from checkpoint and run in parallel at the same time.  Sorry.");
            System.exit(1);
            }
       
       
        // okay, now we actually get down to brass tacks
        
        // Note that this is kind of a mess -- we create a new thread even if we have
        // a single thread to run.  Furthermore, note that we load from checkpoint the
        // initial job within a thread.  This will likely change the job number, which
        // could conflict with other job numbers in other threads, so this is only permitted
        // if there is a SINGLE thread.  We already checked for that situation above.
        
        Thread[] threads = new Thread[parallel];
        for(int _thread = 0; _thread < parallel; _thread++)
            {
            final int thread = _thread;  // stupid Java
            threads[thread] = new Thread(new Runnable()
                {
                public void run()
                    {
                    long time = time_init - 1;
                    long job = thread * repeat;
                    long seed = seed_init + job;  // initially anyway
                    for(long rep = 0 ; rep < repeat; rep++)
                        {
                        SimState state = null;
                
                        // start from checkpoint?  Note this will only happen if there is only ONE thread, so it's okay to change the job number here
                        if (rep == 0 && checkpointFile!=null)  // only job 0 loads from checkpoint
                            {
                            if (!quiet) printlnSynchronized("Loading from checkpoint " + checkpointFile);
                            state = SimState.readFromCheckpoint(new File(checkpointFile));
                            if (state == null)   // there was an error -- it got printed out to the screen, so just quit
                                System.exit(1);
                            else if (state.getClass() != generator.simulationClass())  // uh oh, wrong simulation stored in the file!
                                {
                                printlnSynchronized("Checkpoint contains some other simulation: " + state + ", should have been of class " + generator.simulationClass());
                                System.exit(1);
                                }
                                                                                
                            state.nameThread();

                            job = state.job();
                            if (state.seed() != 0) // likely good seed from the command line earlier
                                {
                                seed = state.seed();
                                if (!quiet) printlnSynchronized("Recovered job: " + state.job() + " Seed: " + state.seed());
                                }
                            else if (!quiet) printlnSynchronized("Renamed job: " + state.job() + " (unknown seed)");
                            }

                        // ...or should we start fresh?
                        if (state==null)  // no checkpoint file requested
                            {
                            state = generator.newInstance(seed,args);
                            state.nameThread();
                            state.job = job;
                            state.seed = seed;
                            if (!quiet) printlnSynchronized("Job: " + state.job() + " Seed: " + state.seed());
                            state.start();
                            }
                        
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
                        
                        while((_for == -1 || steps < _for) && schedule.getTime() <= until)
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
                                if (!quiet) printlnSynchronized("Job " + job + ": " + "Steps: " + steps + " Time: " + state.schedule.getTimestamp("At Start", "Done") + " Rate: " + rateFormat.format((1000.0 *(steps - firstSteps)) / (clock - oldClock)));
                                firstSteps = steps;
                                oldClock = clock;
                                }
                            if (cmod > 0 && steps % cmod == 0)
                                {
                                String s = "" + steps + "." + state.job() +  "." + state.getClass().getName().substring(state.getClass().getName().lastIndexOf(".") + 1) + ".checkpoint";
                                if (!quiet) printlnSynchronized("Job " + job + ": " + "Checkpointing to file: " + s);
                                state.writeToCheckpoint(new File(s));
                                }
                            }
                                
                        state.finish();
                        
                        if (retval) 
                            {
                            if (!quiet) printlnSynchronized("Job " + job + ": " + "Exhausted " + state.job );
                            }
                        else
                            {
                            if (!quiet) printlnSynchronized("Job " + job + ": " + "Quit " + state.job);
                            }

                        job++;
                        seed++;
                        }
                    }
                });
            threads[thread].start();
            }
        for(int thread = 0; thread < parallel; thread++)
            {
            try { threads[thread].join(); } catch (InterruptedException ex) {  }  // do nothing
            }
        System.exit(0);
        }
        
    static Object printLock = new Object[0];
    public static void printlnSynchronized(String val)
        {
        synchronized(printLock) { System.err.println(val); }
        }
    
    /** Names the current thread an appropriate name given the SimState */
    public void nameThread()
        {
        // name my thread for the profiler
        Thread.currentThread().setName("MASON Model: " + this.getClass());
        }

    /** Returns MASON's Version */
    public static double version()
        {
        return 19.0;
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
