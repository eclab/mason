/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
 */

package sim.engine;

import java.io.IOException;



import java.io.Serializable;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
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
import java.rmi.server.UnicastRemoteObject;

import ec.util.*;
import mpi.*;
import sim.display.*;
import sim.engine.mpi.*;
import sim.engine.rmi.*;
import sim.field.*;
import sim.field.partitioning.*;
import sim.field.storage.*;
import sim.util.*;

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
	protected Partition partition;
	/** The DSimState's Transporter interface */
	
	Transporter transporter;
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
	// Not to be confused with the DistinguishedRegistry.
	ArrayList<HaloGrid2D<?, ?>> fieldList;

	// The RMI registry
	DistinguishedRegistry registry;

	// The number of steps between load balances
	protected int balanceInterval = 100;
	
	// How often globals are updated
	int updateGlobalsInterval = 100;
	
	protected int maxStatSize = 10000;
	
	// The current balance level FIXME: This looks primitive, and also requires that
	// be properly in sync
	int balancerLevel;
	
	// Arraylist where the RemoteMessage are stored
	// the methods invoked on it have to be synchronized to avoid concurrent modification
	ArrayList<DistinguishedRemoteMessage> distinguishedMessageQueue = new ArrayList<DistinguishedRemoteMessage>();


	
	/**
	 * Builds a new DSimState with the given random number SEED, the WIDTH and HEIGIHT of the entire model (not just the
	 * partition), and the AREA OF INTEREST (AOI) for the halo field
	 */
	public DSimState(long seed, int width, int height, int aoi)
	{
		this(seed, width, height, aoi, true);
	}

	public DSimState(long seed, int width, int height, int aoi, boolean isToroidal)
	{
		super(seed, new MersenneTwisterFast(seed), new DistributedSchedule());
		this.partition = new Partition(width, height, isToroidal, aoi);
		partition.initialize();
		balancerLevel = ((Partition) partition).getQt().getDepth() - 1;
		transporter = new Transporter(partition);
		fieldList = new ArrayList<>();
		rootInfo = new HashMap<>();
	}	
	
	
	
	
	///// PID 
	

	
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





	////// MULTITHREADING


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






	////// HALO FIELD INTERACTION


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
	 * @return the partition
	 */
	public Partition getPartition()
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
	 * @return The Transporter
	 */
	public Transporter getTransporter()
		{
		return transporter;
		}









	


	//// DISTINGUISHED OBJECTS



	/**
	 * @return the DistinguishedRegistry instance, or null if the registry is not available. You can call this method after calling the
	 *         start() method.
	 */
	public DistinguishedRegistry getDistinguishedRegistry()
		{
		return registry;
		}

	/**
	Sends a message to a Distinguished object registered on the registry with the name NAME.
	 */
	public Promised sendRemoteMessage(String name, int tag, Serializable arguments) throws RemoteException
	{
		RemotePromise callback = new RemotePromise();
		try 
		{
			// DistinguishedRegistry.getInstance().registerObject("0", callback);
			UnicastRemoteObject.exportObject(callback, 0);
			((DistinguishedRemoteObject) DistinguishedRegistry.getInstance().getObject(name)).remoteMessage(tag, arguments, callback);
			return callback;
		} 
		catch (Exception e) 
		{
		e.printStackTrace();
		}
		return null;
	}

	
	// Called by DistinguishedRemoteObject when it receives a message it must put on the queue to process
	void addRemoteMessage(DistinguishedRemoteMessage message)
    	{
		synchronized(this.distinguishedMessageQueue)
		{
			distinguishedMessageQueue.add(message);
		}
	}
	
	



	////// INITIALIZATION

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







	////// STARTING


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


	/**
	 * Modelers must override this method if they want to add any logic that is unique to the root processor
	 */
	protected void startRoot()
	{
	}


	public void start()
	{
		super.start();

		// distributed registry inizialization
		registry = DistinguishedRegistry.getInstance();

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

			for (HaloGrid2D haloField : fieldList)
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







	//// STATISTICS FACILITY


	/**
	 * Log statistics data for this timestep. This data will then be sent to a remote statistics computer.
	 */
	public void addStat(Serializable data)
	{
		synchronized (statLock)
		{
			if (recordStats)
				statList.add(new Stat(data, schedule.getSteps(), schedule.getTime()));
		}
	}
	
	public void emptyStats() 
	{	
		if (recordStats) 
		{	
			if (statList.size() > maxStatSize) 
			{
				statList = new ArrayList<>();
			}
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
				debugList.add(new Stat(data, schedule.getSteps(), schedule.getTime()));
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








	//// GLOBALS FACILITY
	
		
	// implement in subclass. Default simply returns the first one.
	protected Serializable[] arbitrateGlobals(ArrayList<Serializable[]> allGlobals)
	{
		if (allGlobals != null && allGlobals.get(0) != null)
			return allGlobals.get(0);
		else return null;
	}
	
	// implement in subclass
	protected Serializable[] getPartitionGlobals()
	{
		return null;

	}

	// implement in subclass
	protected void setPartitionGlobals(Serializable[] globals)
	{
		return;
	}

	protected void setUpdateGlobalsInterval(int val) { updateGlobalsInterval = val; }
	protected int getUpdateGlobalsInterval() { return updateGlobalsInterval; }

	// after determining the overall global using arbitration, send that one back to each partition
	// uses setPartitionGlobals(), should be implemented in subclass (to match getPartititonGlobals())
	void distributeGlobals(Serializable[] global)
	{
		try
		{
			// broadcast
			global = MPIUtil.bcast(partition.getCommunicator(), global, 0);
			setPartitionGlobals(global);
		}
		catch (Exception e) { }
	}

	// takes the set of globals from each partition the set of variables this is is implemented in getPartitionGlobals(),
	// implemented in the specific subclass
	ArrayList<Serializable[]> gatherGlobals()
	{
		try
		{
			// call getPartitionGlobals() for each partition
			Serializable[] g = this.getPartitionGlobals();
			
			//should be null when getPartitionGlobals() not implemented in subclass
			if (g == null) { return null; }
			else 
				{	
				return MPIUtil.gather(partition, g, 0);
				}
		}
		catch (Exception e)
		{
			System.out.println("error in gatherGlobals");
			System.out.println(e);
			System.exit(-1);
		}
		// cannot be reached
		return null;
	}
	
	// for communicating global variables (usually best) at each time step
	// takes set of variables from each partition, picks the best from them in some
	// way, then distributes the best back to each partition.
	// To use, user must implement getPartitionGlobals, arbitrateGlobals, and setGlobals in subclass
	// this method is called every "updateGlobalsInterval" steps, which is a field that can be changed by user
	// Example: DPSO has a best fitness score and an x and y associated with that score
	// 1) gather each best score and corresponding x and y from each partition (gatherGlobals())
	// 2) arbitrate (pick the best score and its x and y out of the partition candidates (arbitrateGlobal)
	// 3) distributed the winner back to each partition, each partition keeps track of the global
	void updateGlobals()
	{
		if (schedule.getSteps() > 0 && (schedule.getSteps() % updateGlobalsInterval == 0)) 
			{
			Serializable[] g = null;
			ArrayList<Serializable[]> gg = gatherGlobals();
		
			//gg will be null if gatherPartitionGlobals is not implemented
			if (gg != null) 
			{
				if (partition.isRootProcessor())
				{
					g = arbitrateGlobals(gg);
				}
				distributeGlobals(g);
			}	
		}
	}


	






	////// TOP-LEVEL LOOP
	
	

	/**
	 * Calls Sync on all the fields
	 *
	 * @throws MPIException
	 * @throws RemoteException
	 */
	void syncFields() throws MPIException, RemoteException
	{
		for (HaloGrid2D haloField : fieldList)
			{
			haloField.syncHalo();
			}
	}

	void syncRemoveAndAdd() throws MPIException, RemoteException
	{
		for (HaloGrid2D haloField : fieldList)
			{
			haloField.syncRemoveAndAdd();
			}
	}


	// This is only here so it will show up in the documentation
	/**
	 * This method is called immediately before after the schedule. At present it is empty. Nonetheless, if you override this
	 * method, you absolutely need to call super.postSchedule() first.
	 */
	public void postSchedule()
	{
		super.postSchedule();
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
			
			// Sync all the Remove and Add queues for RMI
			syncRemoveAndAdd();
			
			transporter.sync();

			// After the synchronization we can unregister migrated object!
			// remove exported-migrated object from local node
			for (DistinguishedRemoteObject exportedObj : DistinguishedRegistry.getInstance().getAllLocalExportedObjects())
			{
				try
				{
					// if the object is migrated unregister it
					if (DistinguishedRegistry.getInstance().isMigrated(exportedObj.object)) {
						DistinguishedRegistry.getInstance().unregisterObject(exportedObj.object);
					}
				}
				catch (NotBoundException e)
				{
					e.printStackTrace();
				}
			}
			// for (String mo : DistinguishedRegistry.getInstance().getMigratedNames())
			// {
			// 	try
			// 	{
			// 		DistinguishedRegistry.getInstance().unregisterObject(mo);
			// 	}
			// 	catch (NotBoundException e)
			// 	{
			// 		e.printStackTrace();
			// 	}
			// }
			DistinguishedRegistry.getInstance().clearMigratedNames();
			//wait all nodes to finish the unregister phase.
			MPI.COMM_WORLD.barrier();

		}
		catch (ClassNotFoundException | MPIException | IOException e)
		{
			throw new RuntimeException(e);
		}


		for (final PayloadWrapper payloadWrapper : transporter.objectQueue)
		{
			/*
			 * Assumptions about what is to be added to the field using addToField method rely on the fact that the wrapper
			 * classes are not directly used By the modelers
			 *
			 * In case of IterativeRepeat step is added to the field. For PayloadWrapper we add agent and, for all other cases we
			 * add the object itself to the field
			 *
			 * Improperly using the wrappers and/or fieldIndex will cause Class cast exceptions to be thrown
			 */

//			if (payloadWrapper.fieldIndex >= 0)
			{
				// add the object to the field
				fieldList.get(payloadWrapper.fieldIndex).addPayload(payloadWrapper);
			}

			if (payloadWrapper.isAgent())
			{

				if(payloadWrapper.payload instanceof Distinguished)
					{
						try
						{
							DistinguishedRegistry.getInstance().registerObject((Distinguished) payloadWrapper.payload, this);
						}
						catch (RemoteException e)
						{
							e.printStackTrace();
						}
					}

				if (payloadWrapper.isRepeating())
					{
					schedule.scheduleRepeating(payloadWrapper.time, payloadWrapper.ordering, (Steppable)(payloadWrapper.payload), payloadWrapper.interval);
					}
				else
					{
					schedule.scheduleOnce(payloadWrapper.time, payloadWrapper.ordering, (Steppable)(payloadWrapper.payload));
					}
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
		
		updateGlobals(); //only happens every updateGlobalInterval steps
		
		int x = countTotalAgents(fieldList.get(0));

		/* we invoke the fullfill for every messagge in the distinguishedMessageQueue
		   to make the Promise ready
		*/
		try {
			synchronized(this.distinguishedMessageQueue)
			{
			   for(DistinguishedRemoteMessage message: distinguishedMessageQueue)
			   {
				   Serializable data =
					   message.object.remoteMessage(message.tag, message.arguments);
				   message.callback.fulfill(data);
			   }
			   distinguishedMessageQueue.clear();
		   }
		   
		   DistinguishedRegistry.getInstance().unregisterObjects();
	   } 
	   catch (Exception e) 
	   {
		   e.printStackTrace();
	   }
		
	}









	//// LOAD BALANCING


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
				
				emptyStats();


				
				balancePartitions(balancerLevel);
		        
				try
				{

                    //sync transporter (objects moved to transporter.objectQueue)
					transporter.sync();
					
					int x2 = countTotalAgents(fieldList.get(0));
					
										
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
//					if (payloadWrapper.fieldIndex >= 0)
					{
						// add the object to the field
						fieldList.get(payloadWrapper.fieldIndex).addPayload(payloadWrapper);
						//verify it was added to the correct location!
					}
					
					if (payloadWrapper.isAgent())
					{

						// I am currently unclear on how this works
							if(payloadWrapper.payload instanceof Distinguished)
							{
								try
								{
									DistinguishedRegistry.getInstance().registerObject((Distinguished) payloadWrapper.payload, this);
								}
								catch (RemoteException e)
								{
									e.printStackTrace();
								}
							}

					if (payloadWrapper.isRepeating())
						{
						schedule.scheduleRepeating(payloadWrapper.time, payloadWrapper.ordering, (Steppable)(payloadWrapper.payload), payloadWrapper.interval);
						}
					else
						{
						schedule.scheduleOnce(payloadWrapper.time, payloadWrapper.ordering, (Steppable)(payloadWrapper.payload));
						}
					}
					

				}

				int x3 = countTotalAgents(fieldList.get(0));
				
				// Wait that all nodes have registered their new objects in the distributed registry.
				try
				{
					MPI.COMM_WORLD.barrier();
					syncFields();
				}
				catch (MPIException e)
				{
					throw new RuntimeException(e);
				}
				
				int x4 = countTotalAgents(fieldList.get(0));
		        //System.exit(-1);

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
				balancerLevel = ((Partition) partition).getQt().getDepth() - 1;
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
		
		final IntRect2D oldPartition = partition.getLocalBounds();
		final int oldPID = partition.getPID();

		final Double runtime = Timing.get(Timing.LB_RUNTIME).getMovingAverage(); // used to compute the position of the new centroids
		Timing.start(Timing.LB_OVERHEAD);

		((Partition) partition).balance(runtime, level); // balance the partition moving the centroid for the given level
		MPI.COMM_WORLD.barrier();

		// Raj rewrite
		for (HaloGrid2D field : fieldList)
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
					IntRect2D storageBound = st.getCellBounds(i);

					// if storageBound entirely in haloGrid localBounds, no need to check
					if (!haloGrid2D.getLocalBounds().contains(storageBound))
					{
						// for agent/entity in cell
						// HashSet agents = new HashSet(((HashMap) st.storage[i].clone()).values());
						// clones to avoid ConcurrentModificationException
						HashSet agents = new HashSet(((HashMap) st.storage[i]).values());

						for (Object a : agents)
						{
							Double2D loc = st.getObjectLocation((DObject) a);

							if (a instanceof Stopping && !migratedAgents.contains(a) && oldPartition.contains(loc)
									&& !partition.getLocalBounds().contains(loc))
							{
								final int locToP = partition.toPartitionPID(loc); // we need to use this, not toP

								Stopping stopping = ((Stopping) a);
								Stoppable stoppable = stopping.getStoppable();

								// stop agent in schedule, then migrate it
								if (stopping.getStoppable() instanceof DistributedTentativeStep)
								{
									DistributedTentativeStep step = (DistributedTentativeStep) stoppable;
									stoppable.stop();
									transporter.transport((DObject)stopping, locToP, loc, ((HaloGrid2D) field).getFieldIndex(), step.getOrdering(), step.getTime());

								}

								// stop agent in schedule, then migrate it
								else if (stopping.getStoppable() instanceof DistributedIterativeRepeat)
								{
									final DistributedIterativeRepeat step = (DistributedIterativeRepeat) stopping.getStoppable();
									stoppable.stop();
									transporter.transport((DObject)stopping, locToP, loc, ((HaloGrid2D) field).getFieldIndex(), step.getOrdering(), step.getTime(), step.getInterval());
								}

								// keeps track of agents being moved so not added again
								migratedAgents.add(a);
								System.out.println("PID: " + partition.getPID() + " processor " + oldPID + " move " + a
										+ " from " + loc + " to processor " + locToP);
								// here the agent is removed from the old location TOCHECK!!!
							}

							// not stoppable (transport a double or something) transporter call transportObject?
							else if (oldPartition.contains(loc) && !partition.getLocalBounds().contains(loc))
							{
								final int locToP = partition.toPartitionPID(loc); // we need to use this, not toP
								transporter.transport((DObject) a, locToP, loc, ((HaloGrid2D) field).getFieldIndex());
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
				for (Int2D p : oldPartition.getPointList()) //should we ignore halobound here?
					{
					
					// check if the new partition contains the point
					if (!partition.getLocalBounds().contains(p))
					{
						final int toP = partition.toPartitionPID(p);

						Serializable aList = st.getAllObjects(p);

						if (aList != null)
						{

							// go backwards, so removing is safe
							for (int i = ((ArrayList<Serializable>) aList).size() - 1; i >= 0; i--)
							{
								Serializable a = ((ArrayList<Serializable>) aList).get(i);
																
								// if a is stoppable
								if (a != null && a instanceof Stopping && !migratedAgents.contains(a) && oldPartition.contains(p) && !partition.getLocalBounds().contains(p))
								{
									DSteppable stopping = ((DSteppable) a);
									Stoppable stoppable = (Stoppable)(stopping.getStoppable());
									
								// stop agent in schedule, then migrate it
								if (stoppable instanceof DistributedTentativeStep)
									{
									DistributedTentativeStep step = (DistributedTentativeStep) stoppable;
									stoppable.stop();
									transporter.transport(stopping, toP, p, ((HaloGrid2D) field).getFieldIndex(), step.getOrdering(), step.getTime());
									}

									// stop and migrate
								else if (stoppable instanceof DistributedIterativeRepeat)
									{
									final DistributedIterativeRepeat step = (DistributedIterativeRepeat) stopping.getStoppable();
									stoppable.stop();
									transporter.transport(stopping, toP, p, ((HaloGrid2D) field).getFieldIndex(), step.getOrdering(), step.getTime(), step.getInterval());
									}

									migratedAgents.add(stopping);

									// here the agent is removed from the old location TOCHECK!!!
									// haloGrid2D.removeLocal(p, stopping.ID());
									
									st.removeObject(p, stopping.ID());
								}

								// not stoppable (transport a double or something) transporter call transportObject?
								else if (oldPartition.contains(p) && !partition.getLocalBounds().contains(p) && !migratedAgents.contains(a))
								{
									transporter.transport(a, toP, p,((HaloGrid2D) field).getFieldIndex());
								}
								else
								{
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






	////// GUNK





	
	//testing method: counts agents in each storage (not in halo) and sums them.  Should remain constant!
	int countTotalAgents(HaloGrid2D field) 
	{
		int total = 0;

		try {
			//System.out.println(partition.getPID()+"-----");
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
	
	protected int countLocal(HaloGrid2D field) 
	{

		int count = 0;

		// ContinousStorage, do we need its own case anymore? We may be able to combine with else code.
		if (field.getStorage() instanceof ContinuousStorage)
		{

			ContinuousStorage st = (ContinuousStorage) field.getStorage();
			// for cell
			for (int i = 0; i < st.storage.length; i++)
			{
				HashSet agents = new HashSet(((HashMap) st.storage[i]).values());

				for (Object a : agents) 
				{
					Double2D loc = st.getObjectLocation((DObject) a);
					
					if (partition.getLocalBounds().contains(loc)) 
					{
						count = count + 1;
					}

				}
			}
		}
		else 
		{
			GridStorage st = field.getStorage();

			// go by point here
			for (Int2D p : st.getShape().getPointList())
			{
				
				// check if the partition contains the point
				if (partition.getLocalBounds().contains(p))
				{

					Serializable aList = st.getAllObjects(p);

					if (aList != null)
					{
						count = count + ((ArrayList<Serializable>) aList).size();
					}
				}
			}
		}
		
		return count;
		
	}
	

	

	
	/*
	public static void loc_disagree(Int2D p, DHeatBug h, Partition p2, String s)
	{
		
		Int2D hLoc = new Int2D(h.loc_x, h.loc_y);
		
		int newPx = p.x;
		int newPy = p.y;
		

		
		Int2D newP = new Int2D(newPx, newPy);
		//System.out.println(s+" "+h +" hLoc "+hLoc+" p "+ p );
		
		if (!newP.equals(hLoc))
		{
			
			
			
			System.out.println(s+" loc disagree "+h+" hLoc "+hLoc+" p "+ p + " "+p2.getLocalBounds());
			System.exit(-1);
		}
		
	}
    */




	//// DO WE NEED THE LOGGER ANY MORE?

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
	
}
