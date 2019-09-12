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
import java.util.HashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SocketHandler;

import ec.util.MersenneTwisterFast;
import mpi.MPIException;
import sim.field.DPartition;
import sim.field.DQuadTreePartition;
import sim.field.HaloField;
import sim.field.RemoteProxy;
import sim.field.storage.GridStorage;
import sim.util.NdPoint;
import sim.util.Timing;

public class DSimState extends SimState {
	private static final long serialVersionUID = 1L;
	public static Logger logger;

	protected DPartition partition;
	protected DRemoteTransporter transporter;
	public int[] aoi; // Area of Interest

	// A list of all fields in the Model.
	// Any HaloField that is created will register itself here
	protected final ArrayList<HaloField<? extends Serializable, ? extends NdPoint, ? extends GridStorage>> fieldRegistry;

	// A map from agent to IterativeRepeat (Stoppable) for that Agent
	protected final HashMap<Steppable, IterativeRepeat> iterativeRepeatRegistry;

	// public LoadBalancer lb;
	// Maybe refactor to "loadbalancer" ? Also, there's a line that hasn't been
	// used: lb = new LoadBalancer(aoi, 100);

	protected DSimState(final long seed, final MersenneTwisterFast random, final Schedule schedule, final int width,
			final int height, final int aoiSize) {
		super(seed, random, schedule);
		aoi = new int[] { aoiSize, aoiSize };
		partition = new DQuadTreePartition(new int[] { width, height }, true, aoi);
		partition.initialize();
		transporter = new DRemoteTransporter(partition);
		iterativeRepeatRegistry = new HashMap<>();
		fieldRegistry = new ArrayList<>();
	}

	protected DSimState(final long seed, final MersenneTwisterFast random, final Schedule schedule,
			final DPartition partition) {
		super(seed, random, schedule);
		aoi = partition.aoi;
		this.partition = partition;
		partition.initialize();
		transporter = new DRemoteTransporter(partition);
		iterativeRepeatRegistry = new HashMap<>();
		fieldRegistry = new ArrayList<>();
	}

	public DSimState(final long seed, final int width, final int height, final int aoiSize) {
		this(seed, new MersenneTwisterFast(seed), new Schedule(), width, height, aoiSize);
	}

	protected DSimState(final long seed, final Schedule schedule) {
		this(seed, new MersenneTwisterFast(seed), schedule, 1000, 1000, 5);
	}

	public DSimState(final long seed) {
		this(seed, new MersenneTwisterFast(seed), new Schedule(), 1000, 1000, 5);
	}

	protected DSimState(final MersenneTwisterFast random, final Schedule schedule) {
		this(0, random, schedule, 1000, 1000, 5);// 0 is a bogus value. In fact, MT can't have 0 as its seed
	}

	protected DSimState(final MersenneTwisterFast random) {
		this(0, random, new Schedule(), 1000, 1000, 5);// 0 is a bogus value. In fact, MT can't have 0 as its seed
	}

	/**
	 * All HaloFields register themselves here.<br>
	 * Do not call this method explicitly, it's called in the HaloField constructor
	 *
	 * @param haloField
	 * @return index of the field
	 */
	public int registerField(
			final HaloField<? extends Serializable, ? extends NdPoint, ? extends GridStorage> haloField) {
		// Must be called in a deterministic manner
		final int index = fieldRegistry.size();
		fieldRegistry.add(haloField);
		return index;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void addToField(final Serializable obj, final NdPoint p, final int fieldIndex) {
		// if the fieldIndex < 0 we assume that
		// the agent is not supposed to be added to any field

		// If the fieldIndex is correct then the type-cast below will be safe
		if (fieldIndex >= 0)
			((HaloField) fieldRegistry.get(fieldIndex)).add(p, obj);
	}

	/**
	 * Calls Sync on all the fields
	 *
	 * @throws MPIException
	 */
	protected void syncFields() throws MPIException {
		for (final HaloField<?, ?, ?> haloField : fieldRegistry)
			haloField.syncHalo();
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
		for (final PayloadWrapper payloadWrapper : transporter.objectQueue) {

			/*
			 * Assumptions about what is to be added to the field using addToField method
			 * rely on the fact that the wrapper classes are not directly used By the
			 * modelers
			 *
			 * In case of IterativeRepeat step is added to the field. For PayloadWrapper we
			 * add agent and, for all other cases we add the object itself to the field
			 *
			 * Improperly using the wrappers and/or fieldIndex will cause Class cast
			 * exceptions to be thrown
			 */
			if (payloadWrapper.payload instanceof IterativeRepeat) {
				final IterativeRepeat iterativeRepeat = (IterativeRepeat) payloadWrapper.payload;

				// TODO: how to schedule for a specified time?
				// Not adding it to specific time because we get an error -
				// "time provided is less than the current time"

				registerIterativeRepeat(
						schedule.scheduleRepeating(iterativeRepeat.step, iterativeRepeat.ordering,
								iterativeRepeat.interval));

				// Add agent to the field
				addToField(iterativeRepeat.step, payloadWrapper.loc, payloadWrapper.fieldIndex);

			} else if (payloadWrapper.payload instanceof AgentWrapper) {
				final AgentWrapper agentWrapper = (AgentWrapper) payloadWrapper.payload;
				if (agentWrapper.time < 0)
					schedule.scheduleOnce(agentWrapper.agent, agentWrapper.ordering);
				else
					schedule.scheduleOnce(agentWrapper.time, agentWrapper.ordering, agentWrapper.agent);

				// Add agent to the field
				addToField(agentWrapper.agent, payloadWrapper.loc, payloadWrapper.fieldIndex);

			} else {
				addToField(payloadWrapper.payload, payloadWrapper.loc, payloadWrapper.fieldIndex);
			}

		}
		transporter.objectQueue.clear();

		Timing.stop(Timing.MPI_SYNC_OVERHEAD);
	}

	/**
	 * Adds the given iterativeRepeat to the Registry
	 *
	 * @param iterativeRepeat
	 */
	public void registerIterativeRepeat(final IterativeRepeat iterativeRepeat) {
		iterativeRepeatRegistry.put(iterativeRepeat.step, iterativeRepeat);
	}

	/**
	 * Removes the given iterativeRepeat from the Registry and calls stop on it
	 *
	 * @param iterativeRepeat
	 */
	public IterativeRepeat stopIterativeRepeat(final IterativeRepeat iterativeRepeat) {
		iterativeRepeat.stop();
		return iterativeRepeatRegistry.remove(iterativeRepeat.step);
	}

	/**
	 * Removes iterativeRepeat corresponding to the given steppable from the
	 * Registry and calls stop on it
	 *
	 * @param steppable
	 */
	public IterativeRepeat stopIterativeRepeat(final Steppable steppable) {
		final IterativeRepeat iterativeRepeat = iterativeRepeatRegistry.remove(steppable);
		iterativeRepeat.stop();
		return iterativeRepeat;
	}

	/**
	 * Removes the given iterativeRepeat from the Registry
	 *
	 * @param iterativeRepeat
	 * @return iterativeRepeat
	 */
	public IterativeRepeat unRegisterIterativeRepeat(final IterativeRepeat iterativeRepeat) {
		return iterativeRepeatRegistry.remove(iterativeRepeat.step);
	}

	/**
	 * Removes iterativeRepeat corresponding to the given steppable from the
	 * Registry
	 *
	 * @param steppable
	 * @return iterativeRepeat corresponding to the given steppable
	 */
	public IterativeRepeat unRegisterIterativeRepeat(final Steppable steppable) {
		return iterativeRepeatRegistry.remove(steppable);
	}

	/**
	 * @param steppable
	 * @return iterativeRepeat corresponding to the given steppable from the
	 *         Registry
	 */
	public IterativeRepeat getIterativeRepeat(final Steppable steppable) {
		return iterativeRepeatRegistry.get(steppable);
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

	/**
	 * Modelers must override this method if they want to add any logic that is
	 * unique to the root processor
	 */
	protected void startRoot() {
		System.out.println("Master level 0 pid: " + partition.pid);
	}

	public void start() {
		super.start();

		RemoteProxy.Init();

		try {
			syncFields();
		} catch (final MPIException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		for (final HaloField<? extends Serializable, ? extends NdPoint, ? extends GridStorage> haloField : fieldRegistry)
			haloField.initRemote();

		if (partition.isRoot())
			startRoot();
		// TODO: do we need to sync all processors here?
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

	/**
	 * @return the partition
	 */
	public DPartition getPartition() {
		return partition;
	}

	/**
	 * @param partition the partition to set
	 */
	public void setPartition(final DPartition partition) {
		this.partition = partition;
		aoi = partition.aoi;
		partition.initialize();
		transporter = new DRemoteTransporter(partition);
	}

	/**
	 * @return the transporter
	 */
	public DRemoteTransporter getTransporter() {
		return transporter;
	}

}
