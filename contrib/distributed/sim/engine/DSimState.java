/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SocketHandler;

import ec.util.MersenneTwisterFast;
import sim.field.DRemoteTransporter;
import sim.field.Transportee;
import sim.field.DPartition;
import sim.field.DQuadTreePartition;
import sim.util.Timing;

public abstract class DSimState extends SimState {
	private static final long serialVersionUID = 1L;
	public DPartition partition;
	public DRemoteTransporter transporter;
	// public LoadBalancer lb;
	// Maybe refactor to "loadbalancer" ? Also, there's a line that hasn't been
	// used: lb = new LoadBalancer(aoi, 100);
	public static Logger logger;
	public int[] aoi; // Area of Interest

	public DSimState(long seed) {
		this(seed, 1000, 1000, 5);
	}

	public DSimState(long seed, int width, int height, int aoi) {
		super(seed);
		this.aoi = new int[] { aoi, aoi };
		DQuadTreePartition dq = new DQuadTreePartition(new int[] { width, height }, true, this.aoi);
		partition = dq;
		partition.initialize();
		transporter = new DRemoteTransporter(partition);

	}

	protected DSimState(MersenneTwisterFast random, Schedule schedule) {
		super(0, random, schedule); // 0 is a bogus value. In fact, MT can't have 0 as its seed value.
		this.aoi = new int[] { 5, 5 };
		DQuadTreePartition dq = new DQuadTreePartition(new int[] { 1000, 1000 }, true, this.aoi);
		dq.initialize();
		partition = dq;
		transporter = new DRemoteTransporter(partition);
	}

	protected DSimState(long seed, Schedule schedule) {
		super(seed, new MersenneTwisterFast(seed), schedule);
	}

	protected DSimState(MersenneTwisterFast random) {
		super(0, random, new Schedule()); // 0 is a bogus value. In fact, MT can't have 0 as its seed value.
	}

	/**
	 * Adds the object to the field
	 * 
	 * @param transportee
	 */
	public abstract void addToLocation(Transportee transportee);

	/**
	 * Calls Sync on all the fields
	 */
	public abstract void syncFields();

	public abstract void postSync();

	public void preSchedule() {
		// TODO
		// Emigrate or Sync the agents
		// Immigrate or Schedule agents
		// Synchronizer
		syncFields();
		for (Transportee transportee : transporter.objectQueue) {
			schedule.scheduleOnce((Steppable) transportee.wrappedObject, 1);
			addToLocation(transportee);
		}
		transporter.objectQueue.clear();
	}

	private static void initRemoteLogger(String loggerName, String logServAddr, int logServPort) throws IOException {
		SocketHandler sh = new SocketHandler(logServAddr, logServPort);
		sh.setLevel(Level.ALL);
		sh.setFormatter(new java.util.logging.Formatter() {
			public String format(LogRecord rec) {
				return String.format("[%s][%s][%s:%s][%-7s]\t %s",
						new SimpleDateFormat("MM-dd-YYYY HH:mm:ss.SSS").format(new Date(rec.getMillis())),
						rec.getLoggerName(), rec.getSourceClassName(), rec.getSourceMethodName(),
						rec.getLevel().getLocalizedName(), rec.getMessage());
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
						rec.getLevel().getLocalizedName(), rec.getMessage());
			}
		});
		logger.addHandler(handler);
	}

	public static void doLoopMPI(final Class<?> c, String[] args) throws mpi.MPIException {
		doLoopMPI(c, args, 20);
	}

	public static void doLoopMPI(final Class<?> c, String[] args, int window) throws mpi.MPIException {
		Timing.setWindow(window);
		mpi.MPI.Init(args);
		Timing.start(Timing.LB_RUNTIME);

		// Setup Logger
		String loggerName = String.format("MPI-Job-%d", mpi.MPI.COMM_WORLD.getRank());
		String logServAddr = argumentForKey("-logserver", args);
		String logServPortStr = argumentForKey("-logport", args);
		if (logServAddr != null && logServPortStr != null)
			try {
				initRemoteLogger(loggerName, logServAddr, Integer.parseInt(logServPortStr));
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		else
			initLocalLogger(loggerName);

		// start do loop
		doLoop(c, args);

		mpi.MPI.Finalize();
	}

	public boolean isDistributed() {
		return true;
	}

	/**
	 * Use MPI_allReduce to get the current minimum timestamp in the schedule of all
	 * the LPs
	 */
	protected double reviseTime(double localTime) {
		double[] buf = new double[] { localTime };
		try {
			mpi.MPI.COMM_WORLD.allReduce(buf, 1, mpi.MPI.DOUBLE, mpi.MPI.MIN);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return buf[0];
	}

}
