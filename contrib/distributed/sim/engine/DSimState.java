/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SocketHandler;

import ec.util.MersenneTwisterFast;
import mpi.MPIException;
import sim.field.DPartition;
import sim.field.DQuadTreePartition;
import sim.field.DRemoteTransporter;
import sim.field.HaloField;
import sim.field.RemoteProxy;
import sim.field.Transportee;
import sim.util.NdPoint;
import sim.util.Timing;

public abstract class DSimState extends SimState {
	private static final long serialVersionUID = 1L;
	public DPartition partition;
	public DRemoteTransporter transporter;
	public static Logger logger;
	public int[] aoi; // Area of Interest
	ArrayList<HaloField<Serializable, NdPoint>> fields = new ArrayList<>();

	// public LoadBalancer lb;
	// Maybe refactor to "loadbalancer" ? Also, there's a line that hasn't been
	// used: lb = new LoadBalancer(aoi, 100);

	public DSimState(final long seed) {
		this(seed, 1000, 1000, 5);
	}

	public DSimState(final long seed, final int width, final int height, final int aoi) {
		super(seed);
		this.aoi = new int[] { aoi, aoi };
		partition = new DQuadTreePartition(new int[] { width, height }, true, this.aoi);
		partition.initialize();
		transporter = new DRemoteTransporter(partition);
	}

	protected DSimState(final MersenneTwisterFast random, final Schedule schedule) {
		super(0, random, schedule); // 0 is a bogus value. In fact, MT can't have 0 as its seed value.
		aoi = new int[] { 5, 5 };
		partition = new DQuadTreePartition(new int[] { 1000, 1000 }, true, aoi);
		partition.initialize();
		transporter = new DRemoteTransporter(partition);
	}

	protected DSimState(final long seed, final Schedule schedule) {
		super(seed, new MersenneTwisterFast(seed), schedule);
	}

	protected DSimState(final MersenneTwisterFast random) {
		super(0, random, new Schedule()); // 0 is a bogus value. In fact, MT can't have 0 as its seed value.
	}

	/**
	 * All HaloFields register themselves here.<br>
	 * Do not call this method explicitly, it's called in the HaloField constructor
	 *
	 * @param haloField
	 * @return index of the field
	 */
	@SuppressWarnings("unchecked")
	public int register(final HaloField<? extends Serializable, ? extends NdPoint> haloField) {
		// Must be called in a deterministic manner
		final int index = fields.size();
		fields.add((HaloField<Serializable, NdPoint>) haloField);
		return index;
	}

	/**
	 * Adds the object to the field
	 *
	 * @param transportee
	 */
	protected void addToField(final Transportee<? extends Serializable, ? extends NdPoint> transportee) {
		if (transportee.fieldIndex >= 0)
			fields.get(transportee.fieldIndex).addObject(transportee.loc, transportee.wrappedObject);
	}

	/**
	 * Calls Sync on all the fields
	 *
	 * @throws MPIException
	 */
	protected void syncFields() throws MPIException {
		for (final HaloField<? extends Serializable, ? extends NdPoint> haloField : fields)
			haloField.sync();
	}

	public void preSchedule() {
		Timing.stop(Timing.LB_RUNTIME);
		Timing.start(Timing.MPI_SYNC_OVERHEAD);
		try {
			syncFields();
			transporter.sync();
		} catch (ClassNotFoundException | MPIException | IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		for (final Transportee<? extends Serializable, ? extends NdPoint> transportee : transporter.objectQueue) {
			if (transportee.wrappedObject instanceof IterativeRepeat) {
				final IterativeRepeat iterativeRepeat = (IterativeRepeat) transportee.wrappedObject;
				schedule.scheduleRepeating(iterativeRepeat.time, iterativeRepeat.ordering, iterativeRepeat.step,
						iterativeRepeat.interval);
			} else if (transportee.wrappedObject instanceof Steppable) {
//				System.out.println("partition - " + partition + "\nTransportee - " + transportee);
				schedule.scheduleOnce((Steppable) transportee.wrappedObject, transportee.ordering);
			}

			// if location is null we assume that
			// the agent is not supposed to be added to any field
			addToField(transportee);
		}
		transporter.objectQueue.clear();

		Timing.stop(Timing.MPI_SYNC_OVERHEAD);
	}

	private static void initRemoteLogger(final String loggerName, final String logServAddr, final int logServPort)
			throws IOException {
		final SocketHandler sh = new SocketHandler(logServAddr, logServPort);
		sh.setLevel(Level.ALL);
		sh.setFormatter(new java.util.logging.Formatter() {
			public String format(final LogRecord rec) {
				return String.format("[%s][%s][%s:%s][%-7s]\t %s",
						new SimpleDateFormat("MM-dd-YYYY HH:mm:ss.SSS").format(new Date(rec.getMillis())),
						rec.getLoggerName(), rec.getSourceClassName(), rec.getSourceMethodName(),
						rec.getLevel().getLocalizedName(), rec.getMessage());
			}
		});

		DSimState.logger = Logger.getLogger(loggerName);
		DSimState.logger.setUseParentHandlers(false);
		DSimState.logger.setLevel(Level.ALL);
		DSimState.logger.addHandler(sh);
	}

	private static void initLocalLogger(final String loggerName) {
		DSimState.logger = Logger.getLogger(loggerName);
		DSimState.logger.setLevel(Level.ALL);
		DSimState.logger.setUseParentHandlers(false);

		final ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new java.util.logging.Formatter() {
			public synchronized String format(final LogRecord rec) {
				return String.format("[%s][%-7s] %s%n",
						new SimpleDateFormat("MM-dd-YYYY HH:mm:ss.SSS").format(new Date(rec.getMillis())),
						rec.getLevel().getLocalizedName(), rec.getMessage());
			}
		});
		DSimState.logger.addHandler(handler);
	}

	public static void doLoopMPI(final Class<?> c, final String[] args) throws mpi.MPIException {
		doLoopMPI(c, args, 20);
	}

	public static void doLoopMPI(final Class<?> c, final String[] args, final int window) throws mpi.MPIException {
		Timing.setWindow(window);
		mpi.MPI.Init(args);
		Timing.start(Timing.LB_RUNTIME);

		// Setup Logger
		final String loggerName = String.format("MPI-Job-%d", mpi.MPI.COMM_WORLD.getRank());
		final String logServAddr = argumentForKey("-logserver", args);
		final String logServPortStr = argumentForKey("-logport", args);
		if (logServAddr != null && logServPortStr != null)
			try {
				initRemoteLogger(loggerName, logServAddr, Integer.parseInt(logServPortStr));
			} catch (final IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		else
			initLocalLogger(loggerName);

		doLoop(c, args);
		mpi.MPI.Finalize();
	}

	public void start() {
		super.start();
		// TODO: properly init
		for (int i = 0; i < partition.numProcessors; i++) {
			RemoteProxy.Init(i);
		}
		try {
			syncFields();
		} catch (final MPIException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		for (final HaloField<? extends Serializable, ? extends NdPoint> haloField : fields)
			haloField.initRemote();
		// /init
	}

	public boolean isDistributed() {
		return true;
	}

	/**
	 * Use MPI_allReduce to get the current minimum timestamp in the schedule of all
	 * the LPs
	 */
	protected double reviseTime(final double localTime) {
		final double[] buf = new double[] { localTime };
		try {
			mpi.MPI.COMM_WORLD.allReduce(buf, 1, mpi.MPI.DOUBLE, mpi.MPI.MIN);
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return buf[0];
	}

}
