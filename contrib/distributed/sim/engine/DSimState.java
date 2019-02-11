/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;
import ec.util.*;
import java.util.*;
import java.io.*;
import java.util.zip.*;
import java.text.*;
import java.util.logging.*;
import java.lang.reflect.*;

public class DSimState extends SimState
{
    public static Logger logger;
    public DSimState(long seed)
    {
        super(seed);
    }

    protected DSimState(MersenneTwisterFast random, Schedule schedule)
    {
        super(0, random, schedule);  // 0 is a bogus value.  In fact, MT can't have 0 as its seed value.
    }

    protected DSimState(long seed, Schedule schedule)
    {
        super(seed, new MersenneTwisterFast(seed), schedule);
    }

    protected DSimState(MersenneTwisterFast random)
    {
        super(0, random, new Schedule());  // 0 is a bogus value.  In fact, MT can't have 0 as its seed value.
    }
    private static void initRemoteLogger(String loggerName, String logServAddr, int logServPort) throws IOException {
        SocketHandler sh = new SocketHandler(logServAddr, logServPort);
        sh.setLevel(Level.ALL);
        sh.setFormatter(new java.util.logging.Formatter() {
                public String format(LogRecord rec) {
                    return String.format("[%s][%s][%s:%s][%-7s]\t %s",
                                         new SimpleDateFormat("MM-dd-YYYY HH:mm:ss.SSS").format(new Date(rec.getMillis())),
                                         rec.getLoggerName(),
                                         rec.getSourceClassName(),
                                         rec.getSourceMethodName(),
                                         rec.getLevel().getLocalizedName(),
                                         rec.getMessage()
                                         );
                }
            });


        logger = Logger.getLogger(loggerName);
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        logger.addHandler(sh);
    }

    private static void initLocalLogger(String loggerName) {
        logger = Logger.getLogger(loggerName);
        logger.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);

        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new java.util.logging.Formatter() {
                public synchronized String format(LogRecord rec) {
                    return String.format("[%s][%-7s] %s%n",
                                         new SimpleDateFormat("MM-dd-YYYY HH:mm:ss.SSS").format(new Date(rec.getMillis())),
                                         rec.getLevel().getLocalizedName(),
                                         rec.getMessage()
                                         );
                }
            });
        logger.addHandler(handler);
    }
    public static void doLoopMPI(final Class c, String[] args) throws mpi.MPIException 
    {
        mpi.MPI.Init(args);

        // Setup Logger
        String loggerName = String.format("MPI-Job-%d", mpi.MPI.COMM_WORLD.getRank());
        String logServAddr = argumentForKey("-logserver", args);
        String logServPortStr = argumentForKey("-logport", args);
        if (logServAddr != null && logServPortStr != null)
            try
                {
                    initRemoteLogger(loggerName, logServAddr, Integer.parseInt(logServPortStr));
                } catch (IOException e) 
                {
                    e.printStackTrace();
                    System.exit(-1);
                }
        else
            initLocalLogger(loggerName);

        // start do loop 
        doLoop(c, args);

        mpi.MPI.Finalize();
    }

    public boolean isDistributed() { return true; }

    /** Use MPI_allReduce to get the current minimum timestamp in the schedule of all the LPs */
    protected double reviseTime(double localTime) 
    {
        double[] buf = new double[]{localTime};
        try 
            {
                mpi.MPI.COMM_WORLD.allReduce(buf, 1, mpi.MPI.DOUBLE, mpi.MPI.MIN);
            } catch (Exception e) 
            {
                e.printStackTrace();
                System.exit(-1);
            }
        return buf[0];
    }

}

