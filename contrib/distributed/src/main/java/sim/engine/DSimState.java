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

	private static boolean multiThreaded = false;
	private static boolean multiThreadedSet = false;

	public static boolean isMultiThreaded() {
		return multiThreaded;
	}

	static int pid = -1;
	
	static void loadPID()
		{
		try 
			{
			pid = MPI.COMM_WORLD.getRank();
			} 
		catch (MPIException ex) 
			{
			ex.printStackTrace();
			System.exit(-1);
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
	HashMap<String, Serializable> rootInfo = null;
	HashMap<String, Serializable>[] init = null;

	final Object statLock = new Object[0];
	final Object debugStatLock = new Object[0];
	ArrayList<Stat> statList = new ArrayList<>();
	ArrayList<Stat> debugList = new ArrayList<>();

	RemoteProcessor processor;

	// A list of all fields in the Model.
	// Any HaloField that is created will register itself here
	ArrayList<HaloGrid2D> fieldRegistry;

	protected DRegistry registry;
	protected boolean withRegistry;

	protected int balanceInterval = 100;
	protected int balancerLevel;

	public DSimState(final long seed, final MersenneTwisterFast random, 
			final DistributedSchedule schedule,
			final QuadTreePartition partition) 
		{
		super(seed, random, schedule);
		this.partition = partition;
		partition.initialize();
		balancerLevel = ((QuadTreePartition) partition).getQt().getDepth() - 1;
		transporter = new TransporterMPI(partition);
		fieldRegistry = new ArrayList<>();
		rootInfo = new HashMap<>();
		withRegistry = false;
		}

	public DSimState(final long seed, final int width, final int height, int aoi) 
		{
		this(seed, 
			new MersenneTwisterFast(seed), 
			new DistributedSchedule(), 
			new QuadTreePartition(width, height, true, aoi));
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

	protected void syncRemoveAndAdd() throws MPIException, RemoteException {
		for (final HaloGrid2D<?, ?> haloField : fieldRegistry)
			haloField.syncRemoveAndAdd();
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
			


			// Sync all the Remove and Add queues for RMI
			syncRemoveAndAdd();
			


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
			final DistributedIterativeRepeat rrr = (DistributedIterativeRepeat) payloadWrapper.payload;	
			//System.out.println(rrr.getSteppable()+" 's field is "+payloadWrapper.fieldIndex);

			


			
			
			if (payloadWrapper.fieldIndex >= 0)
				// add the object to the field
				fieldRegistry.get(payloadWrapper.fieldIndex).addPayload(payloadWrapper);
			
			
			/*
			else { //added by Raj Patel, is fieldIndex -1 but agent has a location, figure out where it goes
				
				if (payloadWrapper.loc != null) {
					for (HaloGrid2D my_field : fieldRegistry) {
					
						if (my_field.haloPart.contains(payloadWrapper.loc)) {
							fieldRegistry.get(my_field.fieldIndex).syncObject(payloadWrapper);					}
					
					}
				}
				
			}
			*/
			

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
		transporter.objectQueue.clear();

		// Wait that all nodes have registered their new objects in the distributed
		// registry.
		try {
			MPI.COMM_WORLD.barrier();
			syncFields();
		} catch (MPIException | RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

//		if (withRegistry)
//			try {
//				MPI.COMM_WORLD.barrier();
//			} catch (MPIException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}

		
		Timing.stop(Timing.MPI_SYNC_OVERHEAD);
		
		
		loadBalance();
		

	}

	


	void loadBalance() {
		if (schedule.getSteps() > 0 && (schedule.getSteps() % balanceInterval == 0)) {
			try {
				balancePartitions(balancerLevel);
				try {
					transporter.sync();
				} catch (ClassNotFoundException | IOException e1) {
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
					System.out.println("MPI error here? 0");

					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				transporter.objectQueue.clear();

			} catch (MPIException | RemoteException e) {
				// TODO: handle exception
				System.out.println("MPI error here? 1");
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
				System.out.println("MPI error here? 2");
				e.printStackTrace();
			}
		}
	}

	private void balancePartitions(int level) throws MPIException {
		

		
		final IntRect2D old_partition = partition.getLocalBounds();
		final int old_pid = partition.getPID();
		final Double runtime = Timing.get(Timing.LB_RUNTIME).getMovingAverage();
		Timing.start(Timing.LB_OVERHEAD);
		((QuadTreePartition) partition).balance(runtime, level);
		MPI.COMM_WORLD.barrier();

		for (Int2D p : old_partition.getPointList()) {
			
			//check_if_point_matches_heatbug_locs(p);
			//print_all_agents(p);
			
			if (!partition.getLocalBounds().contains(p)) {
				final int toP = partition.toPartitionPID(p);
				for (Synchronizable field : fieldRegistry) {
					ArrayList<Object> migratedAgents = new ArrayList<>();
					HaloGrid2D haloGrid2D = (HaloGrid2D) field;
					
					



					
					
					if (haloGrid2D.getStorage() instanceof ContinuousStorage) {

						ContinuousStorage st = (ContinuousStorage) ((HaloGrid2D) field).getStorage();
						Double2D doublep = new Double2D(p);
						HashSet agents = new HashSet(((HashMap) st.getCell(doublep).clone()).values()); // create a clone to avoid the
						// ConcurrentModificationException
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
											((HaloGrid2D) field).fieldIndex);
								}

								if (stopping.getStoppable() instanceof IterativeRepeat) {
									final IterativeRepeat iterativeRepeat = (IterativeRepeat) stopping.getStoppable();
									final DistributedIterativeRepeat distributedIterativeRepeat = new DistributedIterativeRepeat(
											stopping,
											iterativeRepeat.getTime(), iterativeRepeat.getInterval(),
											iterativeRepeat.getOrdering());
									
									//transporter.migrateRepeatingAgent(distributedIterativeRepeat, toP, p,((HaloGrid2D) field).fieldIndex);
									transporter.migrateRepeatingAgent(distributedIterativeRepeat, locToP, loc,((HaloGrid2D) field).fieldIndex);

									iterativeRepeat.stop();
								}

								migratedAgents.add(a);
								System.out.println("PID: " + partition.getPID() + " processor " + old_pid + " move " + a
										+ " from " + loc + " (point " + p + ") to processor " + toP); //agent not being removed from getCell here
								st.removeObject(loc, ((DObject) a).ID());
								//System.out.println(st);
								//System.out.println("---");
							}
							
							//not stoppable (transport a double or something)  transporter call transportObject?
							else if (old_partition.contains(loc) && !partition.getLocalBounds().contains(loc)) {
								transporter.transportObject((Serializable)a, locToP, loc, ((HaloGrid2D) field).fieldIndex);
							}
						}
					}
					
					
					else { //Note that IntStorage and DoubleStorage don't move anything here because their a is not Stoppable (int or double)
						GridStorage st = ((HaloGrid2D) field).getStorage();
						Serializable a_list = st.getAllObjects(haloGrid2D.toLocalPoint(p));
						
						if (a_list != null) {
							System.out.println(a_list);
							
							ArrayList<Serializable> a_list_copy = new ArrayList();			
							for (int i = 0; i < ((ArrayList) a_list).size(); i++) {
								Serializable a = ((ArrayList<Serializable>) a_list).get(i);
								a_list_copy.add(a);
							}
							
						
						
							for (int i = 0; i< a_list_copy.size(); i++) {
								
								Serializable a = a_list_copy.get(i);
							
								System.out.println(a + " considered");

								if (a != null && a instanceof Stopping && !migratedAgents.contains(a)
									&& old_partition.contains(p) && !partition.getLocalBounds().contains(p)) {
									DSteppable stopping = ((DSteppable) a);

									if (stopping.getStoppable() instanceof TentativeStep) {
										stopping.getStoppable().stop();
										transporter.migrateAgent(stopping, toP, p, ((HaloGrid2D) field).fieldIndex);
									}

									if (stopping.getStoppable() instanceof IterativeRepeat) {
										final IterativeRepeat iterativeRepeat = (IterativeRepeat) stopping.getStoppable();
										final DistributedIterativeRepeat distributedIterativeRepeat = new DistributedIterativeRepeat(
											stopping, iterativeRepeat.getTime(), iterativeRepeat.getInterval(),iterativeRepeat.getOrdering());
										transporter.migrateRepeatingAgent(distributedIterativeRepeat, toP, p, ((HaloGrid2D) field).fieldIndex);
										iterativeRepeat.stop();
									}

									// migrateRepeatingAgent(final DistributedIterativeRepeat iterativeRepeat, final
									// int dst,final NumberND loc,final int fieldIndex)
									migratedAgents.add(stopping);
									System.out.println(
										"PID: " + partition.getPID() + " processor " + old_pid + " move " + stopping
												+ " from " + p + " (point " + p + ") to processor " + toP);
									haloGrid2D.removeLocal(p, stopping.ID());
								
							}
								
								//not stoppable (transport a double or something)  transporter call transportObject?
								else if (old_partition.contains(p) && !partition.getLocalBounds().contains(p)) {
									transporter.transportObject((Serializable)a, toP, p, ((HaloGrid2D) field).fieldIndex);
								}
							
								else {
									System.out.println(a + " not moved over");
								}
						}
					}


					}
					
					/*
					else if (haloGrid2D.getStorage() instanceof ObjectGridStorage) {

						ObjectGridStorage st = (ObjectGridStorage) ((HaloGrid2D) field).getStorage();
						
						//// FIXME: verify this.  I'm passing in -1 for the ID, since ObjectGridStorage ignores it.
						Serializable a = st.getObject(haloGrid2D.toLocalPoint(p), -1);
						if (a != null && a instanceof Stopping && !migratedAgents.contains(a)
								&& old_partition.contains(p) && !partition.getLocalBounds().contains(p)) {
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

							migratedAgents.add(stopping);
							System.out.println("PID: " + partition.getPID() + " processor " + old_pid + " move " + stopping
									+ " from " + p + " (point " + p + ") to processor " + toP);
							haloGrid2D.removeLocal(p, stopping.ID());
						}
					}
					


					// is this needed?
					else if (haloGrid2D.getStorage() instanceof DoubleGridStorage) {

						// so here, move the point and the double associated with it to correct
						// partition?
						DoubleGridStorage st = (DoubleGridStorage) ((HaloGrid2D) field).getStorage();
						//Serializable a = st.getObjects(haloGrid2D.toLocalPoint(p)); // this is a double[]

						// Do I need to do anything here!?!!!

					}

                    
					else if (haloGrid2D.getStorage() instanceof DenseGridStorage) {
						GridStorage st = ((HaloGrid2D) field).getStorage();
						// System.out.println(st.getClass());
						Serializable a_list = st.getAllObjects(haloGrid2D.toLocalPoint(p));
						
						

						
						
						if (a_list != null) {
							System.out.println(a_list);
							
							ArrayList<Serializable> a_list_copy = new ArrayList();			
							for (int i = 0; i < ((ArrayList) a_list).size(); i++) {
								Serializable a = ((ArrayList<Serializable>) a_list).get(i);
								a_list_copy.add(a);
							}

							
							//for (int i = 0; i < ((ArrayList) a_list).size(); i++) {	
						
							//	Serializable a = ((ArrayList<Serializable>) a_list).get(i);
							
							for (int i = 0; i< a_list_copy.size(); i++) {
								
								Serializable a = a_list_copy.get(i);
								
								System.out.println(a + " considered");

								if (a != null && a instanceof Stopping && !migratedAgents.contains(a)
										&& old_partition.contains(p) && !partition.getLocalBounds().contains(p)) {
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
											"PID: " + partition.getPID() + " processor " + old_pid + " move " + stopping
													+ " from " + p + " (point " + p + ") to processor " + toP);
									haloGrid2D.removeLocal(p, stopping.ID());
									
								}
								
								else {
									System.out.println(a + " not moved over");
								}
							}
						}

					}
					
					
					

					else {// other GridStorage types, NOT tested!
						GridStorage st = ((HaloGrid2D) field).getStorage();
						Serializable a = st.getAllObjects(haloGrid2D.toLocalPoint(p));

						if (a != null && a instanceof Stopping && !migratedAgents.contains(a)
								&& old_partition.contains(p) && !partition.getLocalBounds().contains(p)) {
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
							System.out.println("PID: " + partition.getPID() + " processor " + old_pid + " move " + stopping
									+ " from " + p + " (point " + p + ") to processor " + toP);
							haloGrid2D.removeLocal(p, stopping.ID());
						}

					}
					*/
					
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
	public QuadTreePartition getPartition() {
		return partition;
	}

	/**
	 * @return an arraylist of all the HaloGrid2Ds registered with the SimState
	 */
	public ArrayList<HaloGrid2D>  getFieldRegistry() {
		return fieldRegistry;
	}

	/*
	 * @param partition the partition to set
	 */
//	public void setPartition(final QuadTreePartition partition) {
//		this.partition = partition;
//		partition.initialize();
//		transporter = new TransporterMPI(partition);
//	}

	/**
	 * @return the transporter
	 */
	public TransporterMPI getTransporter() {
		return transporter;
	}

	public void sendRootInfoToAll(String key, Serializable sendObj) {
		for (int i = 0; i < partition.getNumProcessors(); i++) {
			init[i].put(key, sendObj);
		}
	}

	public void sendRootInfoToProcessor(int pid, String key, Serializable sendObj) {
		init[pid].put(key, sendObj);
	}

	public Serializable getRootInfo(String key) {
		return rootInfo.get(key);
	}

	public void enableRegistry() {
		withRegistry = true;
	}

	public void addStat(Serializable data) {
		synchronized (statLock) {
			statList.add(new Stat(data, schedule.getSteps()));
		}
	}

	public void addDebug(Serializable data) {
		synchronized (debugStatLock) {
			debugList.add(new Stat(data, schedule.getSteps()));
		}
	}

	public ArrayList<Stat> getStatList() {
		synchronized (statLock) {
			ArrayList<Stat> ret = statList;
			statList = new ArrayList<>();
			return ret;
		}
	}

	public ArrayList<Stat> getDebugList() {
		synchronized (debugStatLock) {
			ArrayList<Stat> ret = debugList;
			debugList = new ArrayList<>();
			return ret;
		}
	}
	
	/*
	//field -> instance var
	//for global values, MPI.MIN_LOC and MPI_MAX_LOC may be good ops here for reduce!
	//corresponding fields refer to the loc for the best value
	//For example, in DPSO, look for global best val (bestVal), with a corresponding x and y associated with it
	public void syncGlobalBestValue(String evalfieldName, String[] correspondingFields) {
		
		//TODO
		//a) combine  gather and bcast calls to make more efficient (I assume)
		//b) generalize typing, note that above may matter
		//c) pass function
		//d) double check barrier calls
		//e) pass function instead of just max?
		
		try {
		Class clazz = this.getClass();
		
		//1) Gather
		
		Field f = clazz.getDeclaredField(evalfieldName); //look up getField vs getDeclaredFiled max = null;
		
		double eachEvalFieldVal = (double) f.get(this);
		
		double[] val_list = new double[partition.numProcessors]; 
		
		MPI.COMM_WORLD.barrier();
		partition.getCommunicator().gather(new double[] { eachEvalFieldVal }, 1, MPI.DOUBLE, val_list, 1, MPI.DOUBLE, 0);
		//partition.getCommunicator().reduce(Object buf, int count, Datatype type, MPI.MAXLOC, 0); //Need to figure out maxloc
		
		ArrayList<double[]> correspondingFieldsList = new ArrayList<double[]>(); //define type here, perhaps Object[]
		//now, gather each correspondingField
		for (int i=0; i<correspondingFields.length; i++) {
			Field corr_f = clazz.getDeclaredField(correspondingFields[i]);
			double corr_val = (double) f.get(this);
			double[] corr_val_list = new double[partition.numProcessors]; 
			MPI.COMM_WORLD.barrier();
			partition.getCommunicator().gather(new double[] { corr_val }, 1, MPI.DOUBLE, corr_val_list, 1, MPI.DOUBLE, 0);
			correspondingFieldsList.add(corr_val_list);
		}
		
		//2) Apply operation (max)
		double chosen_val = 0.0;
		int chosen_ind = 0;
		
		if (partition.getPid() == 0) {
			chosen_val = val_list[0];
		
			for (int j=0; j<val_list.length; j++) {
				if (val_list[j] > chosen_val) {
					chosen_val = val_list[j];
					chosen_ind = j;
				}
			}		
		}
		
		//3) Broadcast 
		
		double[] chosen_val_holder = new double[] {chosen_val};
		MPI.COMM_WORLD.barrier();
		partition.getCommunicator().bcast(chosen_val_holder, 1, MPI.DOUBLE, 0);
		f.set(this, chosen_val_holder[0]);
		//System.out.println("partition "+this.getPID()+" : chosen_val "+chosen_val_holder[0]);
		
		for (int i=0; i<correspondingFields.length; i++) {
			Field corr_f = clazz.getDeclaredField(correspondingFields[i]);
			double chosen_corresponding_val = correspondingFieldsList.get(i)[chosen_ind]; //chosen val
			double[] chosen_corresponding_val_holder = new double[] {chosen_val};
			MPI.COMM_WORLD.barrier();
			partition.getCommunicator().bcast(chosen_corresponding_val_holder, 1, MPI.DOUBLE, 0);
			corr_f.set(this, chosen_corresponding_val_holder[0]);	
			
			
		}


		
		System.exit(-1);
		
		}
		catch(Exception e) {
			System.out.println(e);
			System.out.println("ssyncGlobalValue ran into an exception");
			System.exit(-1);
		}
		
		// MPI Gather this info:  f.get(this);
		//calculate best_val
		//MPI Broadcast using f.set(this, best_val);
		
		
		
		//bcast example
		 // if (world_rank == 0) {
		//	    data = 100;
		//	    printf("Process 0 broadcasting data %d\n", data);
		//	    my_bcast(&data, 1, MPI_INT, 0, MPI_COMM_WORLD);
		//	  } else {
		//	    my_bcast(&data, 1, MPI_INT, 0, MPI_COMM_WORLD);
		//	    printf("Process %d received data %d from root process\n", world_rank, data);
		//	  }
		

		
       //f.setAccessible(flag);  Look into this if field is private!
				
	}
	*/
	
	protected void updateGlobal() {
		Object[][] gg = gatherGlobals();
		Object[] g = arbitrateGlobal(gg);
		distributeGlobals(g);
		
	}

	//should be overwritten in subclass
	//this version picks based on the highest value of index 0
	Object[] arbitrateGlobal(Object[][] gg) {
		
		int chosen_index = 0;
		Object chosen_item = gg[0][0];
		
		double best_val = (double) chosen_item; //make type invariant
		
		for (int i = 0; i<partition.getNumProcessors(); i++) {
			if ((double)gg[i][0] > best_val) {
				best_val = (double)gg[i][0];
				chosen_index = i;
			}
		}
		
		return gg[chosen_index];
		
		
		
		
	}
	
	
	Object[][] gatherGlobals() {
		
		
		try {
		//call getGlobals()  for each partition
		Object[] g = this.getGlobals();
		
		Object[][] gg = new Object[partition.getNumProcessors()][g.length];
		
		partition.getCommunicator().gather(g, 1, MPI.DOUBLE, gg, 1, MPI.DOUBLE, 0); //fix type!
		
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
    	
    	//need to do typing
    	try {
    		
    		
    		
    		partition.getCommunicator().bcast(global, 1, MPI.DOUBLE, 0);

    	    setGlobals(global);
    	
    	
    	
    	
    	}
    	
    	catch(Exception e) {
    		
    	}
    	

    	
    }
     

	
	//implement in subclass
    protected Object[] getGlobals() {
    	
        System.out.println("getGlobals() should be implemented in subclass");
        System.exit(-1);
    	return null;
    	
    }

    
    //implement in subclass
    protected void setGlobals(Object[] o) {
    	
    	//does nothing (subclass inheritance)
        System.out.println("setGlobals() should be implemented in subclass");
        System.exit(-1);
    	
    }	
	
	
	
	/*
	
	private void check_if_point_matches_heatbug_locs(Int2D p) {
		ArrayList<Object> not_match_list = new ArrayList<Object>();
		for (Synchronizable field : fieldRegistry) {
			//ArrayList<Object> migratedAgents = new ArrayList<>();
			HaloGrid2D haloGrid2D = (HaloGrid2D) field;
			
			if (haloGrid2D.getStorage() instanceof DenseGridStorage) {
				//System.out.println("cat");
				GridStorage st = ((HaloGrid2D) field).getStorage();
				// System.out.println(st.getClass());
				Serializable a_list = st.getObjects(haloGrid2D.toLocalPoint(p));

				if (a_list != null) {
					for (int i = 0; i < ((ArrayList) a_list).size(); i++) {
						Serializable a = ((ArrayList<Serializable>) a_list).get(i);
						
						if (((DHeatBug) a).loc_x != p.x || ((DHeatBug) a).loc_y != p.y){
							not_match_list.add(a);
							System.out.println("a is at "+((DHeatBug) a).loc_x+" ,"+((DHeatBug) a).loc_y+" which is not "+p);
						}
					}
				}
			}
		}
		
		if (not_match_list.size() > 0) {
			System.exit(-1);
		}
		
	}
	
	private void print_all_agents(Int2D p) {
		ArrayList<Object> not_match_list = new ArrayList<Object>();
		for (Synchronizable field : fieldRegistry) {
			//ArrayList<Object> migratedAgents = new ArrayList<>();
			HaloGrid2D haloGrid2D = (HaloGrid2D) field;
			
			if (haloGrid2D.getStorage() instanceof DenseGridStorage) {
				//System.out.println("cat");
				GridStorage st = ((HaloGrid2D) field).getStorage();
				// System.out.println(st.getClass());
				Serializable a_list = st.getObjects(haloGrid2D.toLocalPoint(p));

				if (a_list != null) {
					for (int i = 0; i < ((ArrayList) a_list).size(); i++) {
						Serializable a = ((ArrayList<Serializable>) a_list).get(i);
						System.out.println(a);
						

					}
				}
			}
		}
	}
		*/

		
}
