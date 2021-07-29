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
 * Analogous to Mason's SimState. This class represents the entire distributed simulation.
 * 
 * A Distributed Mason model will be implemented by extending this class and overriding the start and startRoot methods. The model
 * must also implement a main method calling doLoopDistributed method from this class.
 *
 */
public class DSimState extends SimState
{
	private static final long serialVersionUID = 1L;

	// Our PID
	static int pid = -1;

	// Is the model running in multithreaded mode? This is used to allocate DObject
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
	ArrayList<HaloGrid2D<?, ?>> fieldList;

	// The RMI registry
	protected DRegistry registry;

	// Flag to understand if the registry is enabled
	public static boolean withRegistry;


	// The number of steps between load balances
	protected int balanceInterval = 100;
	// The current balance level FIXME: This looks primitive, and also requires that
	// be properly in sync
	int balancerLevel;
	// Queue of RemotePromise to fill
	static ArrayList<DObject> globalRemotePromises = new ArrayList<>();
		
	// Queue of RemotePromise to filled and to unregister
	static ArrayList<RemotePromise> filledPromises = new ArrayList<>();

	/**
	 * Builds a new DSimState with the given random number SEED, the WIDTH and HEIGIHT of the entire model (not just the
	 * partition), and the AREA OF INTEREST (AOI) for the halo field
	 */
	public DSimState(long seed, int width, int height, int aoi)
	{
		super(seed, new MersenneTwisterFast(seed), new DistributedSchedule());
		this.partition = new QuadTreePartition(width, height, true, aoi);
		partition.initialize();
		balancerLevel = ((QuadTreePartition) partition).getQt().getDepth() - 1;
		transporter = new TransporterMPI(partition);
		fieldList = new ArrayList<>();
		rootInfo = new HashMap<>();
		withRegistry = false;
	}

	public DSimState(long seed, int width, int height, int aoi, boolean isToroidal)
	{
		super(seed, new MersenneTwisterFast(seed), new DistributedSchedule());
		this.partition = new QuadTreePartition(width, height, isToroidal, aoi);
		partition.initialize();
		balancerLevel = ((QuadTreePartition) partition).getQt().getDepth() - 1;
		transporter = new TransporterMPI(partition);
		fieldList = new ArrayList<>();
		rootInfo = new HashMap<>();
		withRegistry = false;
	}	
	
	
	// loads and stores the pid.
	// Only call this after COMM_WORLD has been set up.
	static void loadPID()
	{
		try
		{
			pid = MPI.COMM_WORLD.getRank();
		}
		catch (MPIException ex)
		{
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Only call this method after COMM_WORLD has been setup. </br>
	 * It's safe to call it in the start method and after.
	 * 
	 * @return Current pid
	 */
	public static final int getPID()
	{
		if (pid == -1)
		{
			loadPID();
		}
		return pid;
	}

	/**
	 * Returns whether the DSimState is assuming that you may be allocating DObjects in a multithreaded environment. In general
	 * you should try to run in single-threaded mode, it will cause far fewer headaches.
	 */
	public static boolean isMultiThreaded()
		{
		return multiThreaded;
		}

	/**
	 * Sets whether the DSimState is assuming that you may be allocating DObjects in a multithreaded environment. In general you
	 * should try to run in single-threaded mode, it will cause far fewer headaches. </br>
	 * To set multiThreaded add the following line to the top of your simulation - </br>
	 * 
	 * static { DSimState.setMultiThreaded(true); }
	 * 
	 * @param multi
	 */
	public static void setMultiThreaded(boolean multi)
	{
		if (!multiThreadedSet)
		{
			multiThreaded = multi;
			multiThreadedSet = true;
		}
		else
			throw new RuntimeException("multiThreaded(...) may only be called once.");
	}

	/**
	 * All HaloFields register themselves here.<br>
	 * Do not call this method explicitly, it's called in the HaloField constructor
	 *
	 * @param haloField
	 * @return index of the field
	 */
	public int registerField(final HaloGrid2D<?, ?> halo)
	{
		// Must be called in a deterministic manner
		final int index = fieldList.size();
		fieldList.add(halo);
		return index;
	}

	/**
	 * Calls Sync on all the fields
	 *
	 * @throws MPIException
	 * @throws RemoteException
	 */
	void syncFields() throws MPIException, RemoteException
	{
		for (final Synchronizable haloField : fieldList)
			haloField.syncHalo();
	}

	void syncRemoveAndAdd() throws MPIException, RemoteException
	{
		for (final HaloGrid2D<?, ?> haloField : fieldList)
			haloField.syncRemoveAndAdd();
	}

	/**
	 * This method is called immediately before after the schedule. At present it is empty. Nonetheless, if you override this
	 * method, you absolutely need to call super.postSchedule() first.
	 */
	public void postSchedule()
	{
		// does nothing
	}

	/**
	 * This method is called immediately before stepping the schedule, and it handles all the partition-to-partition transfer and
	 * communication between steps. If you override this method, you absolutely need to call super.preSchedule() first.
	 */
	public void preSchedule()
	{
		Timing.stop(Timing.LB_RUNTIME);
		Timing.start(Timing.MPI_SYNC_OVERHEAD);
		
		try
		{
			// Wait for all agents globally to stop moving
			MPI.COMM_WORLD.barrier();

			if (withRegistry) {
				try {
					for (RemotePromise promisesFilled : filledPromises) {
						this.getDRegistry().unRegisterObject(promisesFilled.getPromiseId());
					}
					filledPromises.clear();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			// give time for Visualizer
			try
			{
				processor.unlock();
				processor.lock();
			}
			catch (RemoteException e1)
			{
				throw new RuntimeException(e1);
			}

			// check for RemotePromise to fill and fill them
			if (withRegistry && !globalRemotePromises.isEmpty()) {				
				// search for all the agent that has some promises to fill
				
				for(DObject objWithPromises : globalRemotePromises) {
					// check if the agent is migrated or not
					
					if (!DRegistry.getInstance().getMigratedNames().contains(objWithPromises.getExportedName())) {
						
						// search for all the promises within the agent
						for (RemotePromise promise : objWithPromises.getUnfilledPromises()){
							// use the method of the remote interface to fill the promise
							Serializable content = objWithPromises.fillRemotePromise(promise.getTag(), promise.getArgs());
							
							promise.fulfill(content);
							// register the filled promise on the DRegistry
							this.getDRegistry().registerObject(promise.getPromiseId(), promise);
							// add the promise to the filledPromises queue
							filledPromises.add(promise);
						}
						// all the promises of the object have been fulfilled
						objWithPromises.getUnfilledPromises().clear();
					}
				}
				globalRemotePromises.clear();
			}
			
			// Sync all the Remove and Add queues for RMI
			syncRemoveAndAdd();
			
			transporter.sync();

			if (withRegistry)
			{
				// All nodes have finished the synchronization and can unregister exported objects.
				MPI.COMM_WORLD.barrier();

				// After the synchronization we can unregister migrated object!
				// remove exported-migrated object from local node
				for (String mo : DRegistry.getInstance().getMigratedNames())
				{
					try
					{
						DRegistry.getInstance().unRegisterObject(mo);
					}
					catch (NotBoundException e)
					{
						e.printStackTrace();
					}
				}
				DRegistry.getInstance().clearMigratedNames();
				MPI.COMM_WORLD.barrier();
			}
		}
		catch (ClassNotFoundException | MPIException | IOException e)
		{
			throw new RuntimeException(e);
		}


		for (final PayloadWrapper payloadWrapper : transporter.objectQueue)
		{

			// filling the remotePromise of migrated object
			DObject mobj = null;

			if(payloadWrapper.payload instanceof DistributedIterativeRepeat)
				mobj = ((DObject) ((DistributedIterativeRepeat)payloadWrapper.payload).getSteppable());
			else if ((payloadWrapper.payload instanceof AgentWrapper))
				mobj = ((DObject) ((AgentWrapper)payloadWrapper.payload).agent);

			if (withRegistry && mobj!=null)
				{
					
					if (mobj.getExportedName() != null)
					{
						try
						{
							DRegistry.getInstance().registerObject(mobj.getExportedName(), (Remote) mobj);
							DObject objWithPromises = (DObject) (mobj);
							// search for all the promises within the agent
							for (RemotePromise promise : objWithPromises.getUnfilledPromises()){
								// use the method of the remote interface to fill the promise
								Serializable content = objWithPromises.fillRemotePromise(promise.getTag(), promise.getArgs());
								promise.fulfill(content);
								// register the filled promise on the DRegistry
								this.getDRegistry().registerObject(promise.getPromiseId(), promise);
								
								// add the promise to the filledPromises queue
								filledPromises.add(promise);
							}
							// all the promises of the object have been fulfilled
							objWithPromises.getUnfilledPromises().clear();
						}
						catch (RemoteException e)
						{
							e.printStackTrace();
						}
					}
				}

			/*
			 * Assumptions about what is to be added to the field using addToField method rely on the fact that the wrapper
			 * classes are not directly used By the modelers
			 *
			 * In case of IterativeRepeat step is added to the field. For PayloadWrapper we add agent and, for all other cases we
			 * add the object itself to the field
			 *
			 * Improperly using the wrappers and/or fieldIndex will cause Class cast exceptions to be thrown
			 */

			if (payloadWrapper.fieldIndex >= 0)
			{
				// add the object to the field
				fieldList.get(payloadWrapper.fieldIndex).addPayload(payloadWrapper);
			}

			if (payloadWrapper.payload instanceof DistributedIterativeRepeat)
			{
				final DistributedIterativeRepeat iterativeRepeat = (DistributedIterativeRepeat) payloadWrapper.payload;

				// TODO: how to schedule for a specified time?
				// Not adding it to specific time because we get an error -
				// "the time provided (-1.0000000000000002) is < EPOCH (0.0)"

				Stopping stopping = iterativeRepeat.getSteppable();
				stopping.setStoppable(
						schedule.scheduleRepeating(stopping, iterativeRepeat.getOrdering(), iterativeRepeat.interval));
			}
			else if (payloadWrapper.payload instanceof AgentWrapper)
			{
				final AgentWrapper agentWrapper = (AgentWrapper) payloadWrapper.payload;

				if (agentWrapper.time < 0)
					schedule.scheduleOnce(agentWrapper.agent, agentWrapper.ordering);
				else
					schedule.scheduleOnce(agentWrapper.time, agentWrapper.ordering, agentWrapper.agent);
			}
		}

		transporter.objectQueue.clear();

		// Wait that all nodes have registered their new objects in the distributed
		// registry.
		try
		{
			MPI.COMM_WORLD.barrier();
			syncFields();
		}
		catch (MPIException | RemoteException e)
		{
			e.printStackTrace();
		}

		Timing.stop(Timing.MPI_SYNC_OVERHEAD);
		loadBalance();

	}

	void loadBalance()
	{
		/*
		 * Check if it's time to run load balance based on the balanceInterval defined
		 */
		if (schedule.getSteps() > 0 && (schedule.getSteps() % balanceInterval == 0))
		{
			try
			{
				// Balance the partitions for the given level migrating the agents
				balancePartitions(balancerLevel);

				try
				{


					transporter.sync();
					

					
					
				}
				catch (ClassNotFoundException | IOException e1)
				{
					throw new RuntimeException(e1);
				}

				// being transported from elsewhere, needs to be added to this partition's
				// HaloGrid and schedule
				for (final PayloadWrapper payloadWrapper : transporter.objectQueue)
				{

					/*
					 * Assumptions about what is to be added to the field using addToField method rely on the fact that the
					 * wrapper classes are not directly used By the modelers
					 *
					 * In case of IterativeRepeat step is added to the field. For PayloadWrapper we add agent and, for all other
					 * cases we add the object itself to the field
					 *
					 * Improperly using the wrappers and/or fieldIndex will cause Class cast exceptions to be thrown
					 */

					// add payload into correct HaloGrid
					if (payloadWrapper.fieldIndex >= 0)
					{
						// add the object to the field
						fieldList.get(payloadWrapper.fieldIndex).addPayload(payloadWrapper);
						//verify it was added to the correct location!
						

					}
					
					

					

					// DistributedIterativeRepeat
					if (payloadWrapper.payload instanceof DistributedIterativeRepeat)
					{
						final DistributedIterativeRepeat iterativeRepeat = (DistributedIterativeRepeat) payloadWrapper.payload;

						// TODO: how to schedule for a specified time?
						// Not adding it to specific time because we get an error -
						// "the time provided (-1.0000000000000002) is < EPOCH (0.0)"

						// add back to schedule
						Stopping stopping = iterativeRepeat.getSteppable();
						stopping.setStoppable(schedule.scheduleRepeating(stopping, iterativeRepeat.getOrdering(),
								iterativeRepeat.interval));

					}
					else if (payloadWrapper.payload instanceof AgentWrapper)
					{
						final AgentWrapper agentWrapper = (AgentWrapper) payloadWrapper.payload;

						// I am currently unclear on how this works
						if (withRegistry)
						{
							if (((DObject)agentWrapper.agent).getExportedName() != null)
							{
								try
								{
									DRegistry.getInstance().registerObject(((DObject)agentWrapper.agent).getExportedName(),
											(Remote) agentWrapper.agent);
								}
								catch (RemoteException e)
								{
									e.printStackTrace();
								}
							}
						}

						// add back to schedule
						if (agentWrapper.time < 0)
							schedule.scheduleOnce(agentWrapper.agent, agentWrapper.ordering);
						else
							schedule.scheduleOnce(agentWrapper.time, agentWrapper.ordering, agentWrapper.agent);
					}
					

				}


				
				// Wait that all nodes have registered their new objects in the distributed registry.
				try
				{
					MPI.COMM_WORLD.barrier();
					
					//for (Synchronizable field : fieldList) {
					//     ((HaloGrid2D) field).loc_disagree_all_points("lb4");
					//}
					
					syncFields();
				}
				catch (MPIException e)
				{
					throw new RuntimeException(e);
				}

				// clear queue
				transporter.objectQueue.clear();

			}
			catch (MPIException | RemoteException e)
			{
				throw new RuntimeException(e);
			}

			// I'm not sure about this bit exactly
			if (balancerLevel != 0)
				balancerLevel--;
			else
				balancerLevel = ((QuadTreePartition) partition).getQt().getDepth() - 1;
			try
			{
				MPI.COMM_WORLD.barrier();
			}
			catch (MPIException e)
			{
				throw new RuntimeException(e);
			}
		}
		

	}

	/*
	 * Balance the partition for the given level by moving the agent according to the new shape (new centroid). Takes all the
	 * agents inside the partition before the balance, clones them and moves them to the new location. Then the moved agents are
	 * removed from the old partition.
	 */
	void balancePartitions(int level) throws MPIException
	{
		

		int x = countTotalAgents(fieldList.get(0));

		if (this.getPID() == 0) {
			
			System.out.println("field num of agents : "+x);

			System.out.println("---");
		}
		
		//System.out.println("balancing");
		
		final IntRect2D old_partition = partition.getLocalBounds();
		final int old_pid = partition.getPID();

		final Double runtime = Timing.get(Timing.LB_RUNTIME).getMovingAverage(); // used to compute the position of the new
																					// centroids
		Timing.start(Timing.LB_OVERHEAD);

		((QuadTreePartition) partition).balance(runtime, level); // balance the partition moving the centroid for the given level
		MPI.COMM_WORLD.barrier();

		// Raj rewrite
		for (Synchronizable field : fieldList)
		{

			
			ArrayList<Object> migratedAgents = new ArrayList<>();
			HaloGrid2D haloGrid2D = (HaloGrid2D) field;

			// ContinousStorage, do we need its own case anymore? We may be able to combine with else code.
			if (haloGrid2D.getStorage() instanceof ContinuousStorage)
			{

				ContinuousStorage st = (ContinuousStorage) haloGrid2D.getStorage();
				// for cell
				for (int i = 0; i < st.storage.length; i++)
				{
					// don't bother with situations where no point would be valid
					IntRect2D storage_bound = st.getCellBounds(i);

					// if storage_bound entirely in haloGrid localBounds, no need to check
					if (!haloGrid2D.getLocalBounds().contains(storage_bound))
					{
						// for agent/entity in cell
						// HashSet agents = new HashSet(((HashMap) st.storage[i].clone()).values());
						// clones to avoid ConcurrentModificationException
						HashSet agents = new HashSet(((HashMap) st.storage[i]).values());

						for (Object a : agents)
						{
							Double2D loc = st.getObjectLocation((DObject) a);

							if (a instanceof Stopping && !migratedAgents.contains(a) && old_partition.contains(loc)
									&& !partition.getLocalBounds().contains(loc))
							{
								final int locToP = partition.toPartitionPID(loc); // we need to use this, not toP

								Stopping stopping = ((Stopping) a);

								// stop agent in schedule, then migrate it
								if (stopping.getStoppable() instanceof DistributedTentativeStep)
								{
									try
									{
										stopping.getStoppable().stop();

										transporter.migrateAgent((Stopping) a, locToP, loc,
												((HaloGrid2D) field).getFieldIndex());

									}
									catch (Exception e)
									{
										System.out.println("PID: " + partition.getPID() + " exception on " + a);
									}

								}

								// stop agent in schedule, then migrate it
								if (stopping.getStoppable() instanceof IterativeRepeat)
								{
									final IterativeRepeat iterativeRepeat = (IterativeRepeat) stopping.getStoppable();
									final DistributedIterativeRepeat distributedIterativeRepeat = new DistributedIterativeRepeat(
											stopping,
											iterativeRepeat.getTime(), iterativeRepeat.getInterval(),
											iterativeRepeat.getOrdering());

									transporter.migrateRepeatingAgent(distributedIterativeRepeat, locToP, loc,
											((HaloGrid2D) field).getFieldIndex());

									iterativeRepeat.stop();
								}

								// keeps track of agents being moved so not added again
								migratedAgents.add(a);
								System.out.println("PID: " + partition.getPID() + " processor " + old_pid + " move " + a
										+ " from " + loc + " to processor " + locToP);
								// here the agent is removed from the old location TOCHECK!!!
							}

							// not stoppable (transport a double or something) transporter call transportObject?
							else if (old_partition.contains(loc) && !partition.getLocalBounds().contains(loc))
							{
								final int locToP = partition.toPartitionPID(loc); // we need to use this, not toP
								transporter.transportObject((Serializable) a, locToP, loc, ((HaloGrid2D) field).getFieldIndex());
							}
						}
					}
				}
			}

			// other types of storage
			else
			{
				GridStorage st = ((HaloGrid2D) field).getStorage();

				// go by point here
				for (Int2D p : old_partition.getPointList()) //should we ignore halobound here?
					{
					
					// check if the new partition contains the point
					if (!partition.getLocalBounds().contains(p))
					{
						final int toP = partition.toPartitionPID(p);

						Serializable a_list = st.getAllObjects(p);

						if (a_list != null)
						{

							/*
							 * ArrayList<Serializable> a_list_copy = new ArrayList(); for (int i = 0; i < ((ArrayList)
							 * a_list).size(); i++) { Serializable a = ((ArrayList<Serializable>) a_list).get(i);
							 * a_list_copy.add(a); }
							 * 
							 * 
							 * for (int i = 0; i < a_list_copy.size(); i++) {
							 * 
							 * Serializable a = a_list_copy.get(i);
							 */

							// go backwards, so removing is safe
							for (int i = ((ArrayList<Serializable>) a_list).size() - 1; i >= 0; i--)
							{
								Serializable a = ((ArrayList<Serializable>) a_list).get(i);
								
								
								

								

								
								
								// if a is stoppable
								if (a != null && a instanceof Stopping && !migratedAgents.contains(a)
										&& old_partition.contains(p) && !partition.getLocalBounds().contains(p))
								{
									DSteppable stopping = ((DSteppable) a);
									
									

									// stop and migrate
									if (stopping.getStoppable() instanceof DistributedTentativeStep)
									{
										stopping.getStoppable().stop();
										transporter.migrateAgent(stopping, toP, p,
												((HaloGrid2D) field).getFieldIndex());
									}

									// stop and migrate
									if (stopping.getStoppable() instanceof IterativeRepeat)
									{
										final IterativeRepeat iterativeRepeat = (IterativeRepeat) stopping
												.getStoppable();
										iterativeRepeat.stop();
										final DistributedIterativeRepeat distributedIterativeRepeat = new DistributedIterativeRepeat(
												stopping, iterativeRepeat.getTime(), iterativeRepeat.getInterval(),
												iterativeRepeat.getOrdering());
										transporter.migrateRepeatingAgent(distributedIterativeRepeat, toP, p,
												((HaloGrid2D) field).getFieldIndex());
									}

									migratedAgents.add(stopping);
									System.out.println(
											"PID: " + partition.getPID() + " processor " + old_pid + " move " + stopping
													+ " from " + p + " (point " + p + ") to processor " + toP+ " "+partition.getLocalBounds(toP));

									// here the agent is removed from the old location TOCHECK!!!
									// haloGrid2D.removeLocal(p, stopping.ID());
									
									st.removeObject(p, stopping.ID());
									
						
									



								}

								// not stoppable (transport a double or something) transporter call transportObject?
								else if (old_partition.contains(p) && !partition.getLocalBounds().contains(p) && !migratedAgents.contains(a))
								{
									transporter.transportObject((Serializable) a, toP, p,
											((HaloGrid2D) field).getFieldIndex());
								}

								else
								{
									System.out.println(a + " not moved over");
								}
							}
						}
					}
				}
				
				//((HaloGrid2D) field).loc_disagree_all_points("bp3");


			}
		}
		MPI.COMM_WORLD.barrier();
		Timing.stop(Timing.LB_OVERHEAD);
		
		//System.out.println("done balancing");


	}

	static void initRemoteLogger(final String loggerName, final String logServAddr, final int logServPort)
			throws IOException
	{
		final SocketHandler sh = new SocketHandler(logServAddr, logServPort);
		sh.setLevel(Level.ALL);
		sh.setFormatter(new java.util.logging.Formatter()
		{
			public String format(final LogRecord rec)
			{
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

	static void initLocalLogger(final String loggerName)
	{
		DSimState.logger = Logger.getLogger(loggerName);
		DSimState.logger.setLevel(Level.ALL);
		DSimState.logger.setUseParentHandlers(false);

		final ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new java.util.logging.Formatter()
		{
			public synchronized String format(final LogRecord rec)
			{
				return String.format("[%s][%-7s] %s%n",
						new SimpleDateFormat("MM-dd-YYYY HH:mm:ss.SSS").format(new Date(rec.getMillis())),
						rec.getLevel().getLocalizedName(), rec.getMessage());
			}
		});
		DSimState.logger.addHandler(handler);
	}

	public static final int DEFAULT_TIMING_WINDOW = 20;

	public static void doLoopDistributed(final Class<?> c, final String[] args)
	{
		doLoopDistributed(c, args, DEFAULT_TIMING_WINDOW);
	}

	public static void doLoopDistributed(final Class<?> c, final String[] args, final int window)
	{
		try
		{
			Timing.setWindow(window);
			MPI.Init(args);
			Timing.start(Timing.LB_RUNTIME);

			// Setup Logger
			final String loggerName = String.format("MPI-Job-%d", MPI.COMM_WORLD.getRank());
			final String logServAddr = argumentForKey("-logserver", args);
			final String logServPortStr = argumentForKey("-logport", args);
			if (logServAddr != null && logServPortStr != null)
				try
				{
					initRemoteLogger(loggerName, logServAddr, Integer.parseInt(logServPortStr));
				}
				catch (final IOException e)
				{
					e.printStackTrace();
					System.exit(-1);
				}
			else
				initLocalLogger(loggerName);

			SimState.doLoop(c, args);
			MPI.Finalize();
		}
		catch (MPIException ex)
		{
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Modelers must override this method if they want to add any logic that is unique to the root processor
	 */
	protected void startRoot()
	{
	}

	/**
	 * @return the DRegistry instance, or null if the registry is not available. You can call this method after calling the
	 *         start() method.
	 */
	public DRegistry getDRegistry()
		{
		return registry;
		}

	public void start()
	{
		super.start();
//		RMIProxy.init();

		if (withRegistry)
		{
			/* distributed registry inizialization */
			registry = DRegistry.getInstance();
		}

		try
		{
			processor = new RemoteProcessor(this);
			processor.lock();
			// unlocks in preSchedule
		}
		catch (RemoteException e1)
		{
			throw new RuntimeException(e1);
		}

		try
		{
			syncFields();

			for (final Synchronizable haloField : fieldList)
				haloField.initRemote();

			if (partition.isRootProcessor())
			{
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
			schedule.scheduleRepeating(new Stopping()
			{
				public void step(SimState state)
					{
					}

				public Stoppable getStoppable()
					{
					return null;
					}

				public boolean isStopped()
					{
					return false;
					}

				public void setStoppable(Stoppable stop)
					{
					}
			});

			// On all processors, wait for the start to finish
			MPI.COMM_WORLD.barrier();
		}
		catch (final MPIException | RemoteException e)
		{
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * @return the partition
	 */
	public QuadTreePartition getPartition()
		{
		return partition;
		}

	/**
	 * @return an arraylist of all the HaloGrid2Ds registered with the SimState
	 */
	public ArrayList<HaloGrid2D<?, ?>> getFieldList()
		{
		return fieldList;
		}

	/*
	 * @return the Transporter
	 */
	public TransporterMPI getTransporter()
		{
		return transporter;
		}

	/**
	 * Distribute the following keyed information from the root to all the nodes. This may be called inside startRoot().
	 */
	public void sendRootInfoToAll(String key, Serializable sendObj)
	{
		for (int i = 0; i < partition.getNumProcessors(); i++)
		{
			init[i].put(key, sendObj);
		}
	}

	/**
	 * Distribute the following keyed information from the root to a specific node (given by the pid). This may be called inside
	 * startRoot().
	 */
	public void sendRootInfoToProcessor(int pid, String key, Serializable sendObj)
		{
		init[pid].put(key, sendObj);
		}

	/**
	 * Extract information set to a processor by the root. This may be called inside start().
	 */
	public Serializable getRootInfo(String key)
		{
		return rootInfo.get(key);
		}

	public void enableRegistry()
		{
		withRegistry = true;
		}

	/**
	 * Log statistics data for this timestep. This data will then be sent to a remote statistics computer.
	 */
	public void addStat(Serializable data)
	{
		synchronized (statLock)
		{
			if (recordStats)
				statList.add(new Stat(data, schedule.getSteps()));
		}
	}

	/**
	 * Log debug statistics data for this timestep. This data will then be sent to a remote statistics computer.
	 */
	public void addDebug(Serializable data)
	{
		synchronized (debugStatLock)
		{
			if (recordDebug)
				debugList.add(new Stat(data, schedule.getSteps()));
		}
	}

	/** Return the current list of logged statistics data and clear it. */
	public ArrayList<Stat> getStatList()
	{
		synchronized (statLock)
		{
			ArrayList<Stat> ret = statList;
			statList = new ArrayList<>();
			return ret;
		}
	}

	/** Return the current list of logged debug statistics data and clear it. */
	public ArrayList<Stat> getDebugList()
	{
		synchronized (debugStatLock)
		{
			ArrayList<Stat> ret = debugList;
			debugList = new ArrayList<>();
			return ret;
		}
	}

	// for communicating global variables (usually best) at each time step
	// takes set of variables from each partition, picks the best from them in some
	// way, then distributes the best back to each partition.
	// Example: DPSO has a best fitness score and an x and y associated with that score
	// 1) gather each best score and corresponding x and y from each parition (gatherGlobals())
	// 2) arbitrate (pick the best score and its x and y out of the partition candidates (arbitrateGlobal)
	// 3) distributed the winner back to each partition, each partition keeps track of the global
	protected void updateGlobal()
	{
		Object[] g = null;

		ArrayList<Object[]> gg = gatherGlobals();

		if (partition.isRootProcessor())
		{
			g = arbitrateGlobal(gg);
		}

		distributeGlobals(g);
	}

	// this one creates the best global out of the globals from each partiton (gg) should override in subclass
	// this version picks based on the highest value of index 0
	// TODO should we make this one throw an exception and force specific agent to implement its own?
	Object[] arbitrateGlobal(ArrayList<Object[]> gg)
	{
		int chosen_index = 0;
		Object chosen_item = gg.get(0)[0];

		double best_val = (double) chosen_item; // make type invariant

		for (int i = 0; i < partition.getNumProcessors(); i++)
		{
			if ((double) gg.get(i)[0] > best_val)
			{
				best_val = (double) gg.get(i)[0];
				chosen_index = i;
			}
		}

		return gg.get(chosen_index);
	}

	// takes the set of globals from each partition the set of variables this is is implemented in getPartitionGlobals(),
	// implemented in the specific subclass
	ArrayList gatherGlobals()
	{
		try
		{
			// call getPartitionGlobals() for each partition
			Object[] g = this.getPartitionGlobals();

			// System.out.println(g[0]+" "+g[1]+" "+g[2]);

			// Object[][] gg = new Object[partition.getNumProcessors()][g.length];

			// partition.getCommunicator().gather(g, 1, MPI.DOUBLE, gg, 1, MPI.DOUBLE, 0); // fix type!
			ArrayList<Object[]> gg = MPIUtil.gather(partition, g, 0);
			
			if (this.getPID() == 0) {
                for (int i=0; i<gg.size(); i++) {
                	System.out.println(i+"---");
                	for (Object ggg: gg.get(i)){
                		System.out.println(ggg);
                	}
                }
			}


			return gg;
		}
		catch (Exception e)
		{
			System.out.println("error in gatherGlobals");
			System.out.println(e);
			System.exit(-1);

		}

		return null;
	}

	// after determining the overall global using arbitration, send that one back to each partition
	// uses setPartitionGlobals(), should be implemented in subclass (to match getPartititonGlobals())
	void distributeGlobals(Object[] global)
	{
		// need to do typing
		try
		{
			// partition.getCommunicator().bcast(global, 1, MPI.DOUBLE, 0);
			global = MPIUtil.bcast(partition.getCommunicator(), global, 0);
			System.out.println("gl: "+global);
			setPartitionGlobals(global);
		}
		catch (Exception e)
		{

		}
	}

	// implement in subclass
	protected Object[] getPartitionGlobals()
	{
		throw new RuntimeException("getPartitionGlobals() should be implemented in subclass");
	}

	// implement in subclass
	protected void setPartitionGlobals(Object[] o)
	{
		throw new RuntimeException("setPartitionGlobals() should be implemented in subclass");
	}
	
	//testing method: counts agents in each storage (not in halo) and sums them.  Should remain constant!
	protected int countTotalAgents(Synchronizable field) {
		
	
		int total = 0;

		try {
			int count = countLocal(field);
			ArrayList<Integer> counts = MPIUtil.gather(partition, count, 0);
		
			for (Integer c: counts) {
				total = total + c;
			}
		}
		
		catch (Exception e) {
		}
		
		return total;

	}
	
	protected int countLocal(Synchronizable field) {
		
		HaloGrid2D haloGrid2D = (HaloGrid2D) field;
		int count = 0;

		// ContinousStorage, do we need its own case anymore? We may be able to combine with else code.
		if (haloGrid2D.getStorage() instanceof ContinuousStorage)
		{

			ContinuousStorage st = (ContinuousStorage) haloGrid2D.getStorage();
			// for cell
			for (int i = 0; i < st.storage.length; i++)
			{
				HashSet agents = new HashSet(((HashMap) st.storage[i]).values());

				for (Object a : agents) {
					Double2D loc = st.getObjectLocation((DObject) a);
					
					if (partition.getLocalBounds().contains(loc)) {
						count = count + 1;
					}

				}
			}
		}
		
		else {
			
			GridStorage st = ((HaloGrid2D) field).getStorage();

			// go by point here
			for (Int2D p : st.getShape().getPointList())
			{
				
				// check if the partition contains the point
				if (partition.getLocalBounds().contains(p))
				{

					Serializable a_list = st.getAllObjects(p);

					if (a_list != null)
					{
						count = count + ((ArrayList<Serializable>) a_list).size();
					}
					
			
			
			
				}
			}
		}
		
		return count;
		
	}
	
	/*
	public static void loc_disagree(Int2D p, DHeatBug h, Partition p2, String s)
	{
		
		Int2D h_loc = new Int2D(h.loc_x, h.loc_y);
		
		int new_px = p.x;
		int new_py = p.y;
		

		
		Int2D new_p = new Int2D(new_px, new_py);
		//System.out.println(s+" "+h +" h_loc "+h_loc+" p "+ p );
		
		if (!new_p.equals(h_loc))
		{
			
			
			
			System.out.println(s+" loc disagree "+h+" h_loc "+h_loc+" p "+ p + " "+p2.getLocalBounds());
			System.exit(-1);
		}
		
	}
    */
	
	

}
