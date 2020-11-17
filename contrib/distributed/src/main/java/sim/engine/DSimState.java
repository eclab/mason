/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
 */

package sim.engine;

import java.io.IOException;

import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SocketHandler;

import ec.util.MersenneTwisterFast;
import mpi.MPI;
import mpi.MPIException;
import sim.app.dheatbugs.DHeatBug;
import sim.engine.registry.DRegistry;
import sim.engine.transport.AgentWrapper;
import sim.engine.transport.PayloadWrapper;
import sim.engine.transport.TransporterMPI;
import sim.field.HaloGrid2D;
import sim.field.Synchronizable;
import sim.field.partitioning.IntRect2D;
import sim.field.partitioning.QuadTreePartition;
import sim.field.storage.ContinuousStorage;
import sim.field.storage.DenseGridStorage;
import sim.field.storage.DoubleGridStorage;
import sim.field.storage.GridStorage;
import sim.field.storage.ObjectGridStorage;
import sim.util.*;

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

	private static boolean multiThreaded = false;
	private static boolean multiThreadedSet = false;

	public static boolean isMultiThreaded() {
		return multiThreaded;
	}

	/**
	 * Only call this method after COMM_WORLD has been setup. </br>
	 * It's safe to call it in the start method and after.
	 * 
	 * @return Current pid
	 */
	public static int getPID() {
		try {
			return MPI.COMM_WORLD.getRank();
		} catch (MPIException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Set multiThreaded as true if any processing node (e.g. pid = 0) uses more
	 * than one thread. This must be set in a static block and before any DObjects
	 * have been created. </br>
	 * </br>
	 * To set multiThreaded add the followings line to the top of your simulation -
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
			throw new RuntimeException("multiThreaded can only be set once");
	}

	public static Logger logger;

	protected QuadTreePartition partition;
	protected TransporterMPI transporter;
	public int[] aoi; // Area of Interest
	HashMap<String, Object> rootInfo = null;
	HashMap<String, Object>[] init = null;

	RemoteProcessor processor;

	// A list of all fields in the Model.
	// Any HaloField that is created will register itself here
	protected final ArrayList<HaloGrid2D> fieldRegistry;

	protected DRegistry registry;
	protected boolean withRegistry;

	protected int balanceInterval = 100;

	protected int balancerLevel;

	protected DSimState(final long seed, final MersenneTwisterFast random, final DistributedSchedule schedule,
			final int width, final int height, final int aoiSize) {
		super(seed, random, schedule);
		aoi = new int[] { aoiSize, aoiSize };
		partition = new QuadTreePartition(new int[] { width, height }, true, aoi);
		partition.initialize();
		balancerLevel = ((QuadTreePartition) partition).getQt().getDepth() - 1;
		transporter = new TransporterMPI(partition);
		fieldRegistry = new ArrayList<>();
		rootInfo = new HashMap<>();
		withRegistry = false;
	}

	protected DSimState(final long seed, final MersenneTwisterFast random, final DistributedSchedule schedule,
			final QuadTreePartition partition) {
		super(seed, random, schedule);
		aoi = partition.aoi;
		this.partition = partition;
		partition.initialize();
		balancerLevel = ((QuadTreePartition) partition).getQt().getDepth() - 1;
		transporter = new TransporterMPI(partition);
		fieldRegistry = new ArrayList<>();
		rootInfo = new HashMap<>();
		withRegistry = false;
	}

	public DSimState(final long seed, final int width, final int height, final int aoiSize) {
		this(seed, new MersenneTwisterFast(seed), new DistributedSchedule(), width, height, aoiSize);
	}

	protected DSimState(final long seed, final DistributedSchedule schedule) {
		this(seed, new MersenneTwisterFast(seed), schedule, 1000, 1000, 5);
	}

	public DSimState(final long seed) {
		this(seed, new MersenneTwisterFast(seed), new DistributedSchedule(), 1000, 1000, 5);
	}

	protected DSimState(final MersenneTwisterFast random, final DistributedSchedule schedule) {
		this(0, random, schedule, 1000, 1000, 5);// 0 is a bogus value. In fact, MT can't have 0 as its seed
	}

	protected DSimState(final MersenneTwisterFast random) {
		this(0, random, new DistributedSchedule(), 1000, 1000, 5);// 0 is a bogus value. In fact, MT can't have 0 as its
		// seed
	}

	/**
	 * All HaloFields register themselves here.<br>
	 * Do not call this method explicitly, it's called in the HaloField constructor
	 *
	 * @param haloField
	 * @return index of the field
	 */
	public int registerField(final HaloGrid2D halo) {
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
	protected void syncFields() throws MPIException, RemoteException {
		for (final Synchronizable haloField : fieldRegistry)
			haloField.syncHalo();
	}

	public void preSchedule() {
		Timing.stop(Timing.LB_RUNTIME);
		Timing.start(Timing.MPI_SYNC_OVERHEAD);

		try {
			// Wait for all agents globally to stop moving
			MPI.COMM_WORLD.barrier();

			// give time for Visualizer
			try {
				// TODO: How to handle the exception here?
				processor.unlock();
				processor.lock();
			} catch (RemoteException e1) {
				throw new RuntimeException(e1);
			}

//			for (final HaloGrid2D<?, ?> haloField : fieldRegistry)
//				for (final Pair<RemoteFulfillable, Serializable> pair : haloField.getQueue)
//					pair.getA().fulfill(pair.getB());
//			
//			for (final HaloGrid2D<?, ?> haloField : fieldRegistry)
//				for (final Pair<NumberND, ? extends Serializable> pair : haloField.inQueue)
//					haloField.addLocal(pair.getA(), pair.getB());

			// TODO inQueue and outQueue

			// Stop the world and wait for the Visualizer to unlock
//			MPI.COMM_WORLD.barrier();

			syncFields();
			transporter.sync();
			// TODO: Load RMI queue

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
			e.printStackTrace();
			System.exit(-1);
		}

		if (withRegistry) {
			// objects on new nodes.
			try {
				MPI.COMM_WORLD.barrier();
			} catch (MPIException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
				fieldRegistry.get(payloadWrapper.fieldIndex).syncObject(payloadWrapper);

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
		if (withRegistry)
			try {
				MPI.COMM_WORLD.barrier();
			} catch (MPIException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		transporter.objectQueue.clear();
		Timing.stop(Timing.MPI_SYNC_OVERHEAD);

		loadBalancing();
	}

	private void loadBalancing() {
		if (schedule.getSteps() > 0 && (schedule.getSteps() % balanceInterval == 0)) {
			try {
				balancePartitions(balancerLevel);

				syncFields();
				try {
					transporter.sync();
				} catch (ClassNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
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
						fieldRegistry.get(payloadWrapper.fieldIndex).syncObject(payloadWrapper);

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
				} catch (MPIException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				transporter.objectQueue.clear();

			} catch (MPIException | RemoteException e) {
				// TODO: handle exception
				throw new RuntimeException(e);
			}
			if (balancerLevel != 0)
				balancerLevel--;
			else
				balancerLevel = ((QuadTreePartition) partition).getQt().getDepth() - 1;

			try {
				MPI.COMM_WORLD.barrier();
			} catch (MPIException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void balancePartitions(int level) throws MPIException {
		final IntRect2D old_partition = partition.getBounds();
		final int old_pid = partition.getPid();
		final Double runtime = Timing.get(Timing.LB_RUNTIME).getMovingAverage();
		Timing.start(Timing.LB_OVERHEAD);
		((QuadTreePartition) partition).balance(runtime, level);
		MPI.COMM_WORLD.barrier();
		// System.out.println("pid "+partition.getPid()+"
		// old_partitioning"+old_partition);
		System.out.println("pid " + partition.getPid() + " new partition" + partition.getBounds());

		for (Int2D p : old_partition.getPointList()) {
			if (!partition.getBounds().contains(p)) {
				final int toP = partition.toPartitionId(p);
				for (Synchronizable field : fieldRegistry) {
					ArrayList<Object> migratedAgents = new ArrayList<>();
					HaloGrid2D haloGrid2D = (HaloGrid2D) field;
					if (haloGrid2D.getStorage() instanceof ContinuousStorage) {

						ContinuousStorage st = (ContinuousStorage) ((HaloGrid2D) field).getStorage();
						Double2D doublep = new Double2D(p);
						HashSet agents = (HashSet) st.getCell(doublep).clone(); // create a clone to avoid the
						// ConcurrentModificationException
						for (Object a : agents) {
							NumberND loc = st.getLocation((DObject) a);
							final int locToP = partition.toPartitionId(loc);
							if (a instanceof Stopping && !migratedAgents.contains(a) && old_partition.contains(loc)
									&& !partition.getBounds().contains(loc)) {

								Stopping stopping = ((Stopping) a);
								if (stopping.getStoppable() instanceof TentativeStep) {

									try {
										stopping.getStoppable().stop();
									} catch (Exception e) {
										System.out.println("PID: " + partition.pid + " exception on " + a);
									}
//<<<<<<< HEAD
//								final int locToP = partition.toPartitionId(loc);
//								transporter.migrateAgent((Stopping) a, locToP, loc, ((HaloGrid2D) field).fieldIndex);
//								transporter.migrateAgent((Stopping) a, toP, loc, ((HaloGrid2D) field).fieldIndex);
//=======
									transporter.migrateAgent((Stopping) a, locToP, loc,
											((HaloGrid2D) field).fieldIndex);
								}

								if (stopping.getStoppable() instanceof IterativeRepeat) {
									final IterativeRepeat iterativeRepeat = (IterativeRepeat) stopping.getStoppable();
									final DistributedIterativeRepeat distributedIterativeRepeat = new DistributedIterativeRepeat(
											stopping,
											iterativeRepeat.getTime(), iterativeRepeat.getInterval(),
											iterativeRepeat.getOrdering());
									transporter.migrateRepeatingAgent(distributedIterativeRepeat, toP, p,
											((HaloGrid2D) field).fieldIndex);
									iterativeRepeat.stop();
								}

//>>>>>>> 5a7347af137247b63139aa9f8f7b6717db8010f9
								migratedAgents.add(a);
								System.out.println("PID: " + partition.pid + " processor " + old_pid + " move " + a
										+ " from " + loc + " (point " + p + ") to processor " + toP);
								st.removeObject((DObject) a);
							}
						}
					} else if (haloGrid2D.getStorage() instanceof ObjectGridStorage) {

						ObjectGridStorage st = (ObjectGridStorage) ((HaloGrid2D) field).getStorage();
						Serializable a = st.getObjects(haloGrid2D.toLocalPoint(p));
						if (a != null && a instanceof Stopping && !migratedAgents.contains(a)
								&& old_partition.contains(p) && !partition.getBounds().contains(p)) {
//<<<<<<< HEAD
//							DSteppable stopping = ((DSteppable) a);
//							stopping.getStoppable().stop();
//							transporter.migrateAgent(stopping, toP, p, ((HaloGrid2D) field).fieldIndex);
//=======
							DSteppable stopping = ((DSteppable) a);

							if (stopping.getStoppable() instanceof TentativeStep) {
								stopping.getStoppable().stop();
								transporter.migrateAgent(stopping, toP, p, ((HaloGrid2D) field).fieldIndex);
							}

							else if (stopping.getStoppable() instanceof IterativeRepeat) {
								final IterativeRepeat iterativeRepeat = (IterativeRepeat) stopping.getStoppable();
								final DistributedIterativeRepeat distributedIterativeRepeat = new DistributedIterativeRepeat(
										stopping,
										iterativeRepeat.getTime(), iterativeRepeat.getInterval(),
										iterativeRepeat.getOrdering());
								transporter.migrateRepeatingAgent(distributedIterativeRepeat, toP, p,
										((HaloGrid2D) field).fieldIndex);
								iterativeRepeat.stop();
							}

//>>>>>>> 5a7347af137247b63139aa9f8f7b6717db8010f9
							migratedAgents.add(stopping);
							System.out.println("PID: " + partition.pid + " processor " + old_pid + " move " + stopping
									+ " from " + p + " (point " + p + ") to processor " + toP);
							haloGrid2D.removeLocal(p, stopping.getID());
						}
					}

					// is this needed?
					else if (haloGrid2D.getStorage() instanceof DoubleGridStorage) {

						// so here, move the point and the double associated with it to correct
						// partition?
						DoubleGridStorage st = (DoubleGridStorage) ((HaloGrid2D) field).getStorage();
						Serializable a = st.getObjects(haloGrid2D.toLocalPoint(p)); // this is a double[]

						// Do I need to do anything here!?!!!

					}

					else if (haloGrid2D.getStorage() instanceof DenseGridStorage) {
						GridStorage st = ((HaloGrid2D) field).getStorage();
						// System.out.println(st.getClass());
						Serializable a_list = st.getObjects(haloGrid2D.toLocalPoint(p));

						if (a_list != null) {
							for (int i = 0; i < ((ArrayList) a_list).size(); i++) {
								Serializable a = ((ArrayList<Serializable>) a_list).get(i);

								if (a != null && a instanceof Stopping && !migratedAgents.contains(a)
										&& old_partition.contains(p) && !partition.getBounds().contains(p)) {
									DSteppable stopping = ((DSteppable) a);

									if (stopping.getStoppable() instanceof TentativeStep) {
										stopping.getStoppable().stop();
										transporter.migrateAgent(stopping, toP, p, ((HaloGrid2D) field).fieldIndex);
									}

									if (stopping.getStoppable() instanceof IterativeRepeat) {
										final IterativeRepeat iterativeRepeat = (IterativeRepeat) stopping
												.getStoppable();
										final DistributedIterativeRepeat distributedIterativeRepeat = new DistributedIterativeRepeat(
												stopping,
												iterativeRepeat.getTime(), iterativeRepeat.getInterval(),
												iterativeRepeat.getOrdering());
										transporter.migrateRepeatingAgent(distributedIterativeRepeat, toP, p,
												((HaloGrid2D) field).fieldIndex);
										iterativeRepeat.stop();
									}

									// migrateRepeatingAgent(final DistributedIterativeRepeat iterativeRepeat, final
									// int dst,final NumberND loc,final int fieldIndex)
									migratedAgents.add(stopping);
									System.out.println(
											"PID: " + partition.pid + " processor " + old_pid + " move " + stopping
													+ " from " + p + " (point " + p + ") to processor " + toP);
									haloGrid2D.removeLocal(p, stopping.getID());
								}
							}
						}

					}

					else {// other GridStorage types, NOT tested!
						GridStorage st = ((HaloGrid2D) field).getStorage();
						Serializable a = st.getObjects(haloGrid2D.toLocalPoint(p));

						if (a != null && a instanceof Stopping && !migratedAgents.contains(a)
								&& old_partition.contains(p) && !partition.getBounds().contains(p)) {
							DSteppable stopping = ((DSteppable) a);

							if (stopping.getStoppable() instanceof TentativeStep) {
								stopping.getStoppable().stop();
								System.out.println("migrating!");
								transporter.migrateAgent(stopping, toP, p, ((HaloGrid2D) field).fieldIndex);
							}

							if (stopping.getStoppable() instanceof IterativeRepeat) {
								final IterativeRepeat iterativeRepeat = (IterativeRepeat) stopping.getStoppable();
								final DistributedIterativeRepeat distributedIterativeRepeat = new DistributedIterativeRepeat(
										stopping,
										iterativeRepeat.getTime(), iterativeRepeat.getInterval(),
										iterativeRepeat.getOrdering());
								transporter.migrateRepeatingAgent(distributedIterativeRepeat, toP, p,
										((HaloGrid2D) field).fieldIndex);
								iterativeRepeat.stop();
							}

							migratedAgents.add(stopping);
							System.out.println("PID: " + partition.pid + " processor " + old_pid + " move " + stopping
									+ " from " + p + " (point " + p + ") to processor " + toP);
							haloGrid2D.removeLocal(p, stopping.getID());
						}

					}
				}
			}
		}

		MPI.COMM_WORLD.barrier();
		Timing.stop(Timing.LB_OVERHEAD);
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

	public static void doLoopDistributed(final Class<?> c, final String[] args) {
		doLoopDistributed(c, args, 20);
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

			doLoop(c, args);
			MPI.Finalize();
		} catch (MPIException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Modelers must override this method if they want to add any logic that is
	 * unique to the root processor
	 */
	// protected void startRoot(HashMap<String, Object>[] maps) {
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

			if (partition.isGlobalMaster()) {
				init = new HashMap[partition.numProcessors];
				for (int i = 0; i < partition.getNumProc(); i++)
					init[i] = new HashMap<String, Object>();
				// startRoot(init);
				startRoot();
			}
			// synchronize using one to many communication
			rootInfo = MPIUtil.scatter(partition.comm, init, 0);

			// schedule a zombie agent to prevent that a processor with no agent is stopped
			// when the simulation is still going on
			schedule.scheduleRepeating(new DSteppable() {
				@Override
				public void step(SimState state) {

				}
			});

			// On all processors, wait for the start to finish
			MPI.COMM_WORLD.barrier();
		} catch (final MPIException | RemoteException e) {
			e.printStackTrace();
			System.exit(-1);
		}
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
			MPI.COMM_WORLD.allReduce(buf, 1, MPI.DOUBLE, MPI.MIN);
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return buf[0];
	}

	/**
	 * @return the partition
	 */
	public QuadTreePartition getPartitioning() {
		return partition;
	}

	public boolean isMasterProcess() {
		return partition.pid == 0;
	}

	/**
	 * @param partition the partition to set
	 */
	public void setPartition(final QuadTreePartition partition) {
		this.partition = partition;
		aoi = partition.aoi;
		partition.initialize();
		transporter = new TransporterMPI(partition);
	}

	/**
	 * @return the transporter
	 */
	public TransporterMPI getTransporter() {
		return transporter;
	}

	public void sendRootInfoToAll(String key, Object sendObj) {
		for (int i = 0; i < partition.getNumProc(); i++) {
			init[i].put(key, sendObj);
		}
	}

	public void sendRootInfoToProc(int pid, String key, Object sendObj) {
		init[pid].put(key, sendObj);
	}

	public Object getRootInfo(String key) {
		return rootInfo.get(key);
	}

	public void enableRegistry() {
		withRegistry = true;
	}

}
