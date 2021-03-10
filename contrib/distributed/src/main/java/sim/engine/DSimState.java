/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
 */

package sim.engine;

import java.io.*;
import java.rmi.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;
import ec.util.*;
import mpi.*;
import sim.engine.transport.*;
import sim.field.*;
import sim.field.partitioning.*;
import sim.field.storage.*;
import sim.util.*;
import sim.engine.rmi.*;
import sim.display.*;

/**
 * Analogous to Mason's SimState. This class represents the entire distributed
 * simulation.
 * 
 * A Distributed Mason model will be implemented by extending this class and
 * overriding the start and startRoot methods. The model must also implement a
 * main method calling doLoopDistributed method from this class.
 *
 */
public class DSimState extends SimState {
	private static final long serialVersionUID = 1L;

	// Our PID
	static int pid = -1;

	// Is the model runing in multithreaded mode? This is used to allocate DObject
	// IDs efficiently.
	static boolean multiThreaded = false;
	// Have we set up the multiThreaded variable yet?
	static boolean multiThreadedSet = false;

	// Logger for debugging
	static Logger logger;

	/** The Partition of the DSimState */
	protected QuadTreePartition partition;
	/** The DSimState's TransporterMPI interface */
	protected TransporterMPI transporter;
	HashMap<String, Serializable> rootInfo = null;
	HashMap<String, Serializable>[] init = null;

	// The statistics queue lock
	final Object statLock = new Object[0];
	// The statistics queue
	ArrayList<Stat> statList = new ArrayList<>();
	public boolean recordStats = false;

	// The debug queue lock
	final Object debugStatLock = new Object[0];
	// The debug queue
	ArrayList<Stat> debugList = new ArrayList<>();
	public boolean recordDebug = false;

	// The RemoteProcessor interface for communicating via RMI
	RemoteProcessor processor;

	// A list of all fields in the Model. Any HaloField that is created will
	// register itself here.
	// Not to be confused with the DRegistry.
	ArrayList<HaloGrid2D<?, ?>> fieldRegistry;

	// The RMI registry
	protected DRegistry registry;

	// FIXME: what is this for?
	protected boolean withRegistry;

	// The number of steps between load balances
	int balanceInterval = 100;
	// The current balance level FIXME: This looks primitive, and also requires that
	// be properly in sync
	int balancerLevel;

	/**
	 * Builds a new DSimState with the given random number SEED, the WIDTH and
	 * HEIGIHT of the entire model (not just the partition), and the AREA OF
	 * INTEREST (AOI) for the halo field
	 */
	public DSimState(long seed, int width, int height, int aoi) {
		super(seed, new MersenneTwisterFast(seed), new DistributedSchedule());
		this.partition = new QuadTreePartition(width, height, true, aoi);
		partition.initialize();
		balancerLevel = ((QuadTreePartition) partition).getQt().getDepth() - 1;
		transporter = new TransporterMPI(partition);
		fieldRegistry = new ArrayList<>();
		rootInfo = new HashMap<>();
		withRegistry = false;
	}

	/** Sets the rate at which load balancing occurs (in steps). */
	public void setBalanceInterval(int val) {
		balanceInterval = val;
	}

	/** Returns the rate at which load balancing occurs (in steps). */
	public int getBalanceInterval() {
		return balanceInterval;
	}

	// loads and stores the pid.
	// Only call this after COMM_WORLD has been set up.
	static void loadPID() {
		try {
			pid = MPI.COMM_WORLD.getRank();
		} catch (MPIException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Only call this method after COMM_WORLD has been setup. </br>
	 * It's safe to call it in the start method and after.
	 * 
	 * @return Current pid
	 */
	public static final int getPID() {
		if (pid == -1) {
			loadPID();
		}
		return pid;
	}

	/**
	 * Returns whether the DSimState is assuming that you may be allocating DObjects
	 * in a multithreaded environment. In general you should try to run in
	 * single-threaded mode, it will cause far fewer headaches.
	 */
	public static boolean isMultiThreaded() {
		return multiThreaded;
	}

	/**
	 * Sets whether the DSimState is assuming that you may be allocating DObjects in
	 * a multithreaded environment. In general you should try to run in
	 * single-threaded mode, it will cause far fewer headaches. </br>
	 * To set multiThreaded add the following line to the top of your simulation -
	 * </br>
	 * 
	 * static { DSimState.setMultiThreaded(true); }
	 * 
	 * @param multi
	 */
	public static void setMultiThreaded(boolean multi) {
		if (!multiThreadedSet) {
			multiThreaded = multi;
			multiThreadedSet = true;
		} else
			throw new RuntimeException("multiThreaded(...) may only be called once.");
	}

	/**
	 * All HaloFields register themselves here.<br>
	 * Do not call this method explicitly, it's called in the HaloField constructor
	 *
	 * @param haloField
	 * @return index of the field
	 */
	public int registerField(final HaloGrid2D<?, ?> halo) {
		// Must be called in a deterministic manner
		final int index = fieldRegistry.size();
		fieldRegistry.add(halo);
		return index;
	}

	/**
	 * Calls Sync on all the fields
	 *
	 * @throws MPIException
	 * @throws RemoteException
	 */
	void syncFields() throws MPIException, RemoteException {
		for (final Synchronizable haloField : fieldRegistry)
			haloField.syncHalo();
	}

	void syncRemoveAndAdd() throws MPIException, RemoteException {
		for (final HaloGrid2D<?, ?> haloField : fieldRegistry)
			haloField.syncRemoveAndAdd();
	}

	/**
	 * This method is called immediately before after the schedule. At present it is
	 * empty. Nonetheless, if you override this method, you absolutely need to call
	 * super.postSchedule() first.
	 */
	public void postSchedule() {
		// does nothing
	}

	/**
	 * This method is called immediately before stepping the schedule, and it
	 * handles all the partition-to-partition transfer and communication between
	 * steps. If you override this method, you absolutely need to call
	 * super.preSchedule() first.
	 */
	public void preSchedule() {
		Timing.stop(Timing.LB_RUNTIME);
		Timing.start(Timing.MPI_SYNC_OVERHEAD);
		try {
			// Wait for all agents globally to stop moving
			MPI.COMM_WORLD.barrier();

			// give time for Visualizer
			try {
				processor.unlock();
				processor.lock();
			} catch (RemoteException e1) {
				throw new RuntimeException(e1);
			}

			// Sync all the Remove and Add queues for RMI
			syncRemoveAndAdd();
			transporter.sync();

			if (withRegistry) {
				// All nodes have finished the synchronization and can unregister exported
				// objects.
				MPI.COMM_WORLD.barrier();

				// After the synchronization we can unregister migrated object!
				// remove exported-migrated object from local node
				for (String mo : DRegistry.getInstance().getMigratedNames()) {
					try {
						DRegistry.getInstance().unRegisterObject(mo);
					} catch (NotBoundException e) {
						e.printStackTrace();
					}
				}
				DRegistry.getInstance().clearMigratedNames();
				MPI.COMM_WORLD.barrier();
			}
		} catch (ClassNotFoundException | MPIException | IOException e) {
			throw new RuntimeException(e);
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

			if (payloadWrapper.fieldIndex >= 0)
				// add the object to the field
				fieldRegistry.get(payloadWrapper.fieldIndex).addPayload(payloadWrapper);

			if (payloadWrapper.payload instanceof DistributedIterativeRepeat) {
				final DistributedIterativeRepeat iterativeRepeat = (DistributedIterativeRepeat) payloadWrapper.payload;

				// TODO: how to schedule for a specified time?
				// Not adding it to specific time because we get an error -
				// "the time provided (-1.0000000000000002) is < EPOCH (0.0)"

				Stopping stopping = iterativeRepeat.getSteppable();
				stopping.setStoppable(schedule.scheduleRepeating(stopping, iterativeRepeat.getOrdering(),
						iterativeRepeat.interval));
			} else if (payloadWrapper.payload instanceof AgentWrapper) {
				final AgentWrapper agentWrapper = (AgentWrapper) payloadWrapper.payload;

				if (withRegistry) {
					if (agentWrapper.getExportedName() != null) {
						try {
							DRegistry.getInstance().registerObject(agentWrapper.getExportedName(),
									(Remote) agentWrapper.agent);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
				}
				if (agentWrapper.time < 0)
					schedule.scheduleOnce(agentWrapper.agent, agentWrapper.ordering);
				else
					schedule.scheduleOnce(agentWrapper.time, agentWrapper.ordering, agentWrapper.agent);
			}
		}
		transporter.objectQueue.clear();

		// Wait that all nodes have registered their new objects in the distributed
		// registry.
		try {
			MPI.COMM_WORLD.barrier();
			syncFields();
		} catch (MPIException | RemoteException e) {
			e.printStackTrace();
		}

		Timing.stop(Timing.MPI_SYNC_OVERHEAD);
		loadBalance();
	}

	void loadBalance() {
		/*
		 * Check if it's time to run load balance based on the balanceInterval defined
		 */
		if (schedule.getSteps() > 0 && (schedule.getSteps() % balanceInterval == 0)) {
			try {
				// Balance the partitions for the given level migrating the agents
				balancePartitions(balancerLevel); 
				try {
					// Synchronize all objects and agents.
					transporter.sync(); 
				} catch (ClassNotFoundException | IOException e1) {
					throw new RuntimeException(e1);
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

					if (payloadWrapper.fieldIndex >= 0)
						// add the object to the field
						fieldRegistry.get(payloadWrapper.fieldIndex).addPayload(payloadWrapper);

					if (payloadWrapper.payload instanceof DistributedIterativeRepeat) {
						final DistributedIterativeRepeat iterativeRepeat = (DistributedIterativeRepeat) payloadWrapper.payload;

						// TODO: how to schedule for a specified time?
						// Not adding it to specific time because we get an error -
						// "the time provided (-1.0000000000000002) is < EPOCH (0.0)"

						Stopping stopping = iterativeRepeat.getSteppable();
						stopping.setStoppable(schedule.scheduleRepeating(stopping, iterativeRepeat.getOrdering(),
								iterativeRepeat.interval));
					} else if (payloadWrapper.payload instanceof AgentWrapper) {
						final AgentWrapper agentWrapper = (AgentWrapper) payloadWrapper.payload;

						if (withRegistry) {
							if (agentWrapper.getExportedName() != null) {
								try {
									DRegistry.getInstance().registerObject(agentWrapper.getExportedName(),
											(Remote) agentWrapper.agent);
								} catch (RemoteException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
						if (agentWrapper.time < 0)
							schedule.scheduleOnce(agentWrapper.agent, agentWrapper.ordering);
						else
							schedule.scheduleOnce(agentWrapper.time, agentWrapper.ordering, agentWrapper.agent);
					}

				}

				// Wait that all nodes have registered their new objects in the distributed
				// registry.
				try {
					MPI.COMM_WORLD.barrier();
					syncFields();
				} catch (MPIException e) {
					throw new RuntimeException(e);
				}

				transporter.objectQueue.clear();

			} catch (MPIException | RemoteException e) {
				throw new RuntimeException(e);
			}
			if (balancerLevel != 0)
				balancerLevel--;
			else
				balancerLevel = ((QuadTreePartition) partition).getQt().getDepth() - 1;
			try {
				MPI.COMM_WORLD.barrier();
			} catch (MPIException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/*
	 * Balance the partition for the given level by moving the agent according to the new shape (new centroid).
	 * Takes all the agents inside the partition before the balance, clones them
	 * and moves them to the new location.
	 * Then the moved agents are removed from the old partition.
	 */
	void balancePartitions(int level) throws MPIException {
		final IntRect2D old_partition = partition.getLocalBounds();
		final int old_pid = partition.getPID();
		final Double runtime = Timing.get(Timing.LB_RUNTIME).getMovingAverage(); //used to compute the position of the new centroids
		Timing.start(Timing.LB_OVERHEAD);
		((QuadTreePartition) partition).balance(runtime, level); // balance the partition moving the centroid for the given level
		MPI.COMM_WORLD.barrier();

		// iterates through the old partition's points 
		for (Int2D p : old_partition.getPointList()) {

			// check if the new partition contains the point 
			if (!partition.getLocalBounds().contains(p)) {
				final int toP = partition.toPartitionPID(p);
				
				//iterates through all the fields (HaloField) in the Model
				for (Synchronizable field : fieldRegistry) {
					ArrayList<Object> migratedAgents = new ArrayList<>();
					HaloGrid2D haloGrid2D = (HaloGrid2D) field;

					if (haloGrid2D.getStorage() instanceof ContinuousStorage) {

						// all the agents of the field are cloned to avoid the ConcurrentModificationException
						ContinuousStorage st = (ContinuousStorage) ((HaloGrid2D) field).getStorage();
						Double2D doublep = new Double2D(p);
						HashSet agents = new HashSet(((HashMap) st.getCell(doublep).clone()).values());
						
						/* 
						 * Each agent is stopped and added to the list of the already migrated agents
						 * but is removed from the old partition only at the end using the agent ID.
						 */
						
						for (Object a : agents) {
							Double2D loc = st.getObjectLocation((DObject) a);
							final int locToP = partition.toPartitionPID(loc);
							
							if (a instanceof Stopping && !migratedAgents.contains(a) && old_partition.contains(loc)
									&& !partition.getLocalBounds().contains(loc)) {

								Stopping stopping = ((Stopping) a);
								if (stopping.getStoppable() instanceof TentativeStep) {

									try {
										stopping.getStoppable().stop();
									} catch (Exception e) {
										System.out.println("PID: " + partition.getPID() + " exception on " + a);
									}
									transporter.migrateAgent((Stopping) a, locToP, loc,
											((HaloGrid2D) field).getFieldIndex());
								}

								if (stopping.getStoppable() instanceof IterativeRepeat) {
									final IterativeRepeat iterativeRepeat = (IterativeRepeat) stopping.getStoppable();
									final DistributedIterativeRepeat distributedIterativeRepeat = new DistributedIterativeRepeat(
											stopping,
											iterativeRepeat.getTime(), iterativeRepeat.getInterval(),
											iterativeRepeat.getOrdering());

									// transporter.migrateRepeatingAgent(distributedIterativeRepeat, toP,
									// p,((HaloGrid2D) field).getFieldIndex());
									transporter.migrateRepeatingAgent(distributedIterativeRepeat, locToP, loc,
											((HaloGrid2D) field).getFieldIndex());

									iterativeRepeat.stop();
								}

								migratedAgents.add(a);
								System.out.println("PID: " + partition.getPID() + " processor " + old_pid + " move " + a
										+ " from " + loc + " (point " + p + ") to processor " + toP); 
								
								// here the agent is removed from the old location 
								// TOCHECK!!!
								st.removeObjectUsingGlobalLoc(loc, ((DObject) a).ID());
							}

							// not stoppable (transport a double or something) transporter call
							// transportObject?
							else if (old_partition.contains(loc) && !partition.getLocalBounds().contains(loc)) {
								transporter.transportObject((Serializable) a, locToP, loc,
										((HaloGrid2D) field).getFieldIndex());
							}
						}
					}

					else { // Note that IntStorage and DoubleStorage don't move anything here because their
							// a is not Stoppable (int or double)
						GridStorage st = ((HaloGrid2D) field).getStorage();
						Serializable a_list = st.getAllObjects(haloGrid2D.toLocalPoint(p));

						if (a_list != null) {
							System.out.println(a_list);

							ArrayList<Serializable> a_list_copy = new ArrayList();
							for (int i = 0; i < ((ArrayList) a_list).size(); i++) {
								Serializable a = ((ArrayList<Serializable>) a_list).get(i);
								a_list_copy.add(a);
							}

							for (int i = 0; i < a_list_copy.size(); i++) {

								Serializable a = a_list_copy.get(i);

								System.out.println(a + " considered");

								if (a != null && a instanceof Stopping && !migratedAgents.contains(a)
										&& old_partition.contains(p) && !partition.getLocalBounds().contains(p)) {
									DSteppable stopping = ((DSteppable) a);

									if (stopping.getStoppable() instanceof TentativeStep) {
										stopping.getStoppable().stop();
										transporter.migrateAgent(stopping, toP, p,
												((HaloGrid2D) field).getFieldIndex());
									}

									if (stopping.getStoppable() instanceof IterativeRepeat) {
										final IterativeRepeat iterativeRepeat = (IterativeRepeat) stopping
												.getStoppable();
										final DistributedIterativeRepeat distributedIterativeRepeat = new DistributedIterativeRepeat(
												stopping, iterativeRepeat.getTime(), iterativeRepeat.getInterval(),
												iterativeRepeat.getOrdering());
										transporter.migrateRepeatingAgent(distributedIterativeRepeat, toP, p,
												((HaloGrid2D) field).getFieldIndex());
										iterativeRepeat.stop();
									}

									// migrateRepeatingAgent(final DistributedIterativeRepeat iterativeRepeat, final
									// int dst,final NumberND loc,final int fieldIndex)
									migratedAgents.add(stopping);
									System.out.println(
											"PID: " + partition.getPID() + " processor " + old_pid + " move " + stopping
													+ " from " + p + " (point " + p + ") to processor " + toP);
									
									// here the agent is removed from the old location 
									// TOCHECK!!!
									haloGrid2D.removeLocal(p, stopping.ID());

								}

								// not stoppable (transport a double or something) transporter call
								// transportObject?
								else if (old_partition.contains(p) && !partition.getLocalBounds().contains(p)) {
									transporter.transportObject((Serializable) a, toP, p,
											((HaloGrid2D) field).getFieldIndex());
								}

								else {
									System.out.println(a + " not moved over");
								}
							}
						}
					}
				}
			}
		}

		MPI.COMM_WORLD.barrier();
		Timing.stop(Timing.LB_OVERHEAD);
	}

	static void initRemoteLogger(final String loggerName, final String logServAddr, final int logServPort)
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

	static void initLocalLogger(final String loggerName) {
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

	public static final int DEFAULT_TIMING_WINDOW = 20;

	public static void doLoopDistributed(final Class<?> c, final String[] args) {
		doLoopDistributed(c, args, DEFAULT_TIMING_WINDOW);
	}

	public static void doLoopDistributed(final Class<?> c, final String[] args, final int window) {
		try {
			Timing.setWindow(window);
			MPI.Init(args);
			Timing.start(Timing.LB_RUNTIME);

			// Setup Logger
			final String loggerName = String.format("MPI-Job-%d", MPI.COMM_WORLD.getRank());
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

			SimState.doLoop(c, args);
			MPI.Finalize();
		} catch (MPIException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Modelers must override this method if they want to add any logic that is
	 * unique to the root processor
	 */
	protected void startRoot() {
	}

	/**
	 * @return the DRegistry instance, or null if the registry is not available. You
	 *         can call this method after calling the start() method.
	 */
	public DRegistry getDRegistry() {
		return registry;
	}

	public void start() {
		super.start();
//		RMIProxy.init();

		if (withRegistry) {
			/* distributed registry inizialization */
			registry = DRegistry.getInstance();
		}

		try {
			processor = new RemoteProcessor(this);
			processor.lock();
			// unlocks in preSchedule
		} catch (RemoteException e1) {
			throw new RuntimeException(e1);
		}

		try {
			syncFields();

			for (final Synchronizable haloField : fieldRegistry)
				haloField.initRemote();

			if (partition.isRootProcessor()) {
				init = new HashMap[partition.getNumProcessors()];
				for (int i = 0; i < init.length; i++)
					init[i] = new HashMap<String, Serializable>();
				// startRoot(init);
				startRoot();
			}
			// synchronize using one to many communication
			rootInfo = (HashMap<String, Serializable>) MPIUtil.scatter(partition.getCommunicator(), init, 0);

			// schedule a zombie agent to prevent that a processor with no agent is stopped
			// when the simulation is still going on
			schedule.scheduleRepeating(new DSteppable() {
				public void step(SimState state) {}
			});

			// On all processors, wait for the start to finish
			MPI.COMM_WORLD.barrier();
		} catch (final MPIException | RemoteException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/*
	 * Use MPI_allReduce to get the current minimum timestamp in the schedule of all
	 * the LPs
	 */
	/*
	 * protected double reviseTime(final double localTime) { final double[] buf =
	 * new double[] { localTime }; try { MPI.COMM_WORLD.allReduce(buf, 1,
	 * MPI.DOUBLE, MPI.MIN); } catch (final Exception e) { e.printStackTrace();
	 * System.exit(-1); } return buf[0]; }
	 */

	/**
	 * @return the partition
	 */
	public QuadTreePartition getPartition() {
		return partition;
	}

	/**
	 * @return an arraylist of all the HaloGrid2Ds registered with the SimState
	 */
	public ArrayList<HaloGrid2D<?, ?>> getFieldRegistry() {
		return fieldRegistry;
	}

	/*
	 * @return the Transporter
	 */
	public TransporterMPI getTransporter() {
		return transporter;
	}

	/**
	 * Distribute the following keyed information from the root to all the nodes.
	 * This may be called inside startRoot().
	 */
	public void sendRootInfoToAll(String key, Serializable sendObj) {
		for (int i = 0; i < partition.getNumProcessors(); i++) {
			init[i].put(key, sendObj);
		}
	}

	/**
	 * Distribute the following keyed information from the root to a specific node
	 * (given by the pid). This may be called inside startRoot().
	 */
	public void sendRootInfoToProcessor(int pid, String key, Serializable sendObj) {
		init[pid].put(key, sendObj);
	}

	/**
	 * Extract information set to a processor by the root. This may be called inside
	 * start().
	 */
	public Serializable getRootInfo(String key) {
		return rootInfo.get(key);
	}

	public void enableRegistry() {
		withRegistry = true;
	}

	/**
	 * Log statistics data for this timestep. This data will then be sent to a
	 * remote statistics computer.
	 */
	public void addStat(Serializable data) {
		synchronized (statLock) {
			if (recordStats)
				statList.add(new Stat(data, schedule.getSteps()));
		}
	}

	/**
	 * Log debug statistics data for this timestep. This data will then be sent to a
	 * remote statistics computer.
	 */
	public void addDebug(Serializable data) {
		synchronized (debugStatLock) {
			if (recordDebug)
				debugList.add(new Stat(data, schedule.getSteps()));
		}
	}

	/** Return the current list of logged statistics data and clear it. */
	public ArrayList<Stat> getStatList() {
		synchronized (statLock) {
			ArrayList<Stat> ret = statList;
			statList = new ArrayList<>();
			return ret;
		}
	}

	/** Return the current list of logged debug statistics data and clear it. */
	public ArrayList<Stat> getDebugList() {
		synchronized (debugStatLock) {
			ArrayList<Stat> ret = debugList;
			debugList = new ArrayList<>();
			return ret;
		}
	}

	protected void updateGlobal() {
		Object[][] gg = gatherGlobals();
		Object[] g = arbitrateGlobal(gg);
		distributeGlobals(g);

	}

	// should be overwritten in subclass
	// this version picks based on the highest value of index 0
	Object[] arbitrateGlobal(Object[][] gg) {

		int chosen_index = 0;
		Object chosen_item = gg[0][0];

		double best_val = (double) chosen_item; // make type invariant

		for (int i = 0; i < partition.getNumProcessors(); i++) {
			if ((double) gg[i][0] > best_val) {
				best_val = (double) gg[i][0];
				chosen_index = i;
			}
		}

		return gg[chosen_index];

	}

	Object[][] gatherGlobals() {

		try {
			// call getGlobals() for each partition
			Object[] g = this.getGlobals();

			Object[][] gg = new Object[partition.getNumProcessors()][g.length];

			partition.getCommunicator().gather(g, 1, MPI.DOUBLE, gg, 1, MPI.DOUBLE, 0); // fix type!

			return gg;
		}

		catch (Exception e) {

			System.out.println("error in gatherGlobals");
			System.out.println(e);
			System.exit(-1);

		}

		return null;

	}

	void distributeGlobals(Object[] global) {

		// need to do typing
		try {
			partition.getCommunicator().bcast(global, 1, MPI.DOUBLE, 0);

			setGlobals(global);
		}

		catch (Exception e) {

		}
	}

	// implement in subclass
	protected Object[] getGlobals() {

		System.out.println("getGlobals() should be implemented in subclass");
		System.exit(-1);
		return null;

	}

	// implement in subclass
	protected void setGlobals(Object[] o) {

		// does nothing (subclass inheritance)
		System.out.println("setGlobals() should be implemented in subclass");
		System.exit(-1);

	}

}
