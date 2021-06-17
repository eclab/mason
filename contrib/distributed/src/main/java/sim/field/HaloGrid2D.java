package sim.field;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import mpi.*;
import sim.app.dheatbugs.DHeatBug;
import sim.engine.*;
import sim.engine.transport.*;
import sim.field.partitioning.*;
import sim.field.storage.*;
import sim.util.*;
import sim.engine.rmi.*;
import java.util.function.Consumer;

/**
 * All fields in distributed MASON must contain this class. Stores
 * objects/agents and implements methods to add, move and remove them.
 *
 * @param <T> The Class of Object to store in the field
 * @param <S> The Type of Storage to use
 */
public class HaloGrid2D<T extends Serializable, S extends GridStorage<T>>
		extends UnicastRemoteObject implements TransportRMIInterface<T, NumberND>, Synchronizable {
	private static final long serialVersionUID = 1L;

	// Backpointer to the SimState
	DSimState state;
	// The world rectangle
	IntRect2D world;
	// cached world width, to be slightly faster, probably doesn't matter
	int worldWidth;
	// cached world height, to be slightly faster, probably doesn't matter
	int worldHeight;
	// Bounds of the halo portion of the partition
	IntRect2D haloBounds;
	// Bounds of the inner local portion of the partition
	IntRect2D localBounds;
	// All neighboring partitions
	ArrayList<Neighbor> neighbors;
	// Storage object that owns me
	S localStorage;
	// My partition
	Partition partition;
	// My field's index
	int fieldIndex;
	// My RMI Proxy
	RMIProxy<T, NumberND> proxy;

	// The following four queues are how RMI adds, removes, and fetches elements on
	// behalf of remote processors.

	// Queue of Promised results from getRMI
	ArrayList<Pair<Promised, NumberND>> getAllQueue = new ArrayList<>();
	ArrayList<Triplet<Promised, NumberND, Long>> getQueue = new ArrayList<>();

	// Queue of requests to add things to the grid via RMI
	ArrayList<Triplet<NumberND, T, double[]>> addQueue = new ArrayList<>();
	// Queue of requests to remove things from the grid via RMI
	ArrayList<Pair<NumberND, Long>> removeQueue = new ArrayList<>();
	// Queue of requests to remove all elements at certain locations from the grid
	// via RMI
	ArrayList<NumberND> removeAllQueue = new ArrayList<>();

	public HaloGrid2D(S storage, DSimState state) throws RemoteException {
		super();
		partition = state.getPartition();
		localStorage = storage;
		this.state = state;
		// init variables that don't change with the partition scheme
		world = partition.getWorldBounds();
		worldWidth = partition.getWorldWidth();
		worldHeight = partition.getWorldHeight();

//		final List<GridStorage<T>> tempStor = new ArrayList<>();
//		final QuadTreePartition q = (QuadTreePartition) partition;

		// register callbacks
		partition.registerPreCommit(new Consumer() {
			public void accept(Object arg) {
			}
		});
		partition.registerPostCommit(new Consumer() {
			public void accept(Object t) {
				reload();
			}
		});
		// init variables that may change with the partition scheme
		reload();
		fieldIndex = state.registerField(this);
	}

	/**
	 * Resizes the partition and halo region, and reloads the neighbors.
	 */
	public void reload() {
		System.out.println("called reload");
		localBounds = partition.getLocalBounds();
		// Get the partition representing halo and local area by expanding the original
		// partition by aoi at each dimension
		haloBounds = localBounds.resize(partition.getAOI());
		
		//localStorage.setOffSet(haloBounds.ul()); // moving local point calculation to GridStorage

		
		localStorage.reshape(haloBounds);
		
		localStorage.setOffSet(haloBounds.ul()); // moving local point calculation to GridStorage



		// Get the partition representing private area by shrinking the original
		// partition by aoi at each dimension

		// Get the neighbors and create Neighbor objects
		neighbors = new ArrayList<Neighbor>();
		for (int id : partition.getNeighborPIDs()) {
			neighbors.add(new Neighbor(partition.getLocalBounds(id)));
		    System.out.println(localBounds+" : "+partition.getLocalBounds(id));
			//neighbors.add(new Neighbor(partition.getHaloBounds()));

		}
		
		//System.exit(-1);
	}

	//// Simple requests

	/** Returns the HaloGrid2D's partition. */
	public Partition getPartition() {
		return partition;
	}

	/** Returns the HaloGrid2D's field index. */
	public int getFieldIndex() {
		return fieldIndex;
	}

	/**
	 * @return local storage (for this partition)
	 */
	public GridStorage<T> getStorage() {
		return localStorage;
	}

	///// Various point queries

	/**
	 * Shifts point p to give location on the local partition
	 * 
	 * @param p
	 * @return location on the local partition
	 */

	public Int2D toLocalPoint(final Int2D p) {
		return p.subtract(haloBounds.ul());
	}

	/**
	 * @param point
	 * @return true if point is within the global grid
	 */
	public boolean inGlobal(final Int2D point) {
		if (!(point.x >= 0 && point.x < worldWidth))
			return false;

		if (!(point.y >= 0 && point.y < worldHeight))
			return false;

		return true;
	}

	public IntRect2D getLocalBounds() {
		return localBounds;
	}

	public IntRect2D getHaloBounds() {
		return haloBounds;
	}

	/**
	 * @param point
	 * @return true if point is local
	 */
	public boolean inLocal(final NumberND point) {
		return localBounds.contains(point);
	}

	/**
	 * @param point
	 * @return true if point is local or in the halo
	 */
	public boolean inHalo(final NumberND point) {
		return haloBounds.contains(point);
	}

	/**
	 * @param point
	 * @return a point wrapped around considering the toroidal halo region, if
	 *         possible. If out of the halo region, returns null.
	 */
	public Double2D toHaloToroidal(final Double2D point) {
		if (inHalo(point))
			return point;
		double x = point.x;
		double y = point.y;
		double aoi = partition.getAOI();
		double height = worldHeight;
		double width = worldWidth;

		double lowx = (x - width);
		if (lowx >= 0 - aoi) {
			double lowy = (y - height);
			if (lowy >= 0 - aoi) {
				if (haloBounds.contains(lowx, lowy))
					return new Double2D(lowx, lowy);
				else
					return null;
			}
			double highy = (y + height);
			if (highy < height + aoi) {
				if (haloBounds.contains(lowx, highy))
					return new Double2D(lowx, highy);
				else
					return null;
			} else {
				if (haloBounds.contains(lowx, y))
					return new Double2D(lowx, y);
				else
					return null;
			}
		}

		double highx = (x + width);
		if (highx < width + aoi) {
			double lowy = (y - height);
			if (lowy >= 0 - aoi) {
				if (haloBounds.contains(highx, lowy))
					return new Double2D(highx, lowy);
				else
					return null;
			}
			double highy = (y + height);
			if (highy < height + aoi) {
				if (haloBounds.contains(highx, highy))
					return new Double2D(highx, highy);
				else
					return null;
			} else {
				if (haloBounds.contains(highx, y))
					return new Double2D(highx, y);
				else
					return null;
			}
		}

		double lowy = (y - height);
		if (lowy >= 0 - aoi) {
			if (haloBounds.contains(x, lowy))
				return new Double2D(x, lowy);
			else
				return null;
		}

		double highy = (y + height);
		if (highy < height + aoi) {
			if (haloBounds.contains(x, highy))
				return new Double2D(x, highy);
			else
				return null;
		}

		return null;
	}

	/**
	 * @param point
	 * @return a point wrapped around considering the toroidal halo region, if
	 *         possible. If out of the halo region, returns null.
	 */
	public Int2D toHaloToroidal(final Int2D point) {
		if (inHalo(point))
			return point; // easy
		int x = point.x;
		int y = point.y;
		int aoi = partition.getAOI();
		int height = worldHeight;
		int width = worldWidth;

		int lowx = (x - width);
		if (lowx >= 0 - aoi) {
			int lowy = (y - height);
			if (lowy >= 0 - aoi) {
				if (haloBounds.contains(lowx, lowy))
					return new Int2D(lowx, lowy);
				else
					return null;
			}
			int highy = (y + height);
			if (highy < height + aoi) {
				if (haloBounds.contains(lowx, highy))
					return new Int2D(lowx, highy);
				else
					return null;
			} else {
				if (haloBounds.contains(lowx, y))
					return new Int2D(lowx, y);
				else
					return null;
			}
		}

		int highx = (x + width);
		if (highx < width + aoi) {
			int lowy = (y - height);
			if (lowy >= 0 - aoi) {
				if (haloBounds.contains(highx, lowy))
					return new Int2D(highx, lowy);
				else
					return null;
			}
			int highy = (y + height);
			if (highy < height + aoi) {
				if (haloBounds.contains(highx, highy))
					return new Int2D(highx, highy);
				else
					return null;
			} else {
				if (haloBounds.contains(highx, y))
					return new Int2D(highx, y);
				else
					return null;
			}
		}

		int lowy = (y - height);
		if (lowy >= 0 - aoi) {
			if (haloBounds.contains(x, lowy))
				return new Int2D(x, lowy);
			else
				return null;
		}

		int highy = (y + height);
		if (highy < height + aoi) {
			if (haloBounds.contains(x, highy))
				return new Int2D(x, highy);
			else
				return null;
		}

		return null;
	}

	/**
	 * @param point
	 * @return true if point is local or in the halo considering toroidal
	 *         wrap-around
	 */
	public boolean inHaloToroidal(NumberND point) {
		double[] p = point.toArrayAsDouble();
		return inHaloToroidal(p[0], p[1]);
	}

	/**
	 * @param point
	 * @return true if point is local or in the halo considering toroidal
	 *         wrap-around
	 */
	public boolean inHaloToroidal(double x, double y) {
		if (haloBounds.contains(x, y))
			return true;

		double aoi = partition.getAOI();
		double height = worldHeight;
		double width = worldWidth;

		double lowx = (x - width);
		if (lowx >= 0 - aoi) {
			double lowy = (y - height);
			if (lowy >= 0 - aoi) {
				return (haloBounds.contains(lowx, lowy));
			}
			double highy = (y + height);
			if (highy < height + aoi) {
				return (haloBounds.contains(lowx, highy));
			} else {
				return (haloBounds.contains(lowx, y));
			}
		}

		double highx = (x + width);
		if (highx < width + aoi) {
			double lowy = (y - height);
			if (lowy >= 0 - aoi) {
				return (haloBounds.contains(highx, lowy));
			}
			double highy = (y + height);
			if (highy < height + aoi) {
				return (haloBounds.contains(highx, highy));
			} else {
				return (haloBounds.contains(highx, y));
			}
		}

		double lowy = (y - height);
		if (lowy >= 0 - aoi) {
			return (haloBounds.contains(x, lowy));
		}

		double highy = (y + height);
		if (highy < height + aoi) {
			return (haloBounds.contains(x, highy));
		}

		return false;
	}

	/**
	 * Removes all Objects and Agents from the non-local point p. Called by
	 * DDenseGrid2D. Don't call this directly.
	 */
	public void removeAllAgentsAndObjectsFromRemote(final NumberND p) {
		removeAllFromRemote(p);
	}

	/**
	 * Adds and schedules an agent remotely. Called by various fields. Don't call
	 * this directly.
	 */
	public void addAgentToRemote(final NumberND p, final T t, final int ordering, final double time) {
		if (!(t instanceof Stopping))
			throw new IllegalArgumentException("t must be a Stopping");

		// If local, then MPI
		if (state.getTransporter().isNeighbor(partition.toPartitionPID(p))) {
			state.getTransporter().migrateAgent(ordering, time, (Stopping) t, partition.toPartitionPID(p), p, this.fieldIndex);
		}
		else { // ...otherwise, RMI
			try {

				// First, remove from schedule
				assert(t instanceof Stopping); // Assumes that t is not an agent if it's not Stopping
				unscheduleAgent((Stopping) t);
				proxy.getField(partition.toPartitionPID(p)).addRMI(p, t, ordering, time);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Adds and schedules an agent remotely repeating. Called by various fields.
	 * Don't call this directly.
	 */
	public void addAgentToRemote(final NumberND p, final T t, final int ordering, final double time,
			final double interval) {
		if (!(t instanceof Stopping))
			throw new IllegalArgumentException("t must be a Stopping");

		// If local, then MPI
		if (state.getTransporter().isNeighbor(partition.toPartitionPID(p))) {
			DistributedIterativeRepeat iterativeRepeat = new DistributedIterativeRepeat((Stopping) t, time, interval, ordering);
			state.getTransporter().migrateRepeatingAgent(iterativeRepeat, partition.toPartitionPID(p), p, this.fieldIndex);
		}
		else { // ...otherwise, RMI
			try {

				// First, remove from schedule
				assert(t instanceof Stopping); // Assumes that t is not an agent if it's not Stopping
				unscheduleAgent((Stopping) t);
				// Update using RMI
				proxy.getField(partition.toPartitionPID(p)).addRMI(p, t, ordering, time, interval);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns a promise for a remote object at a certain location. Called by
	 * various fields. Don't call this directly.
	 */
	public Promised getFromRemote(final NumberND p) {
		try {
			RemotePromise remotePromise = new RemotePromise();
			proxy.getField(partition.toPartitionPID(p)).getRMI(p, remotePromise);
			return remotePromise;
		} catch (final NullPointerException e) {
			throw new IllegalArgumentException("Remote Proxy is not initialized");
		} catch (final RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns a promise for a remote object by id. Called by various fields. Don't
	 * call this directly.
	 */
	public Promised getFromRemote(final NumberND p, long id) {
		try {
			RemotePromise remotePromise = new RemotePromise();
			// Make promise remote, then, update promise remotely
			proxy.getField(partition.toPartitionPID(p)).getRMI(p, id, remotePromise);
			return remotePromise;
		} catch (final NullPointerException e) {
			throw new IllegalArgumentException("Remote Proxy is not initialized");
		} catch (final RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Adds an object (not an agent) to a remote location. Called by various fields.
	 * Don't call this directly.
	 */
	public void addToRemote(final NumberND p, final T t) {
		try {
			proxy.getField(partition.toPartitionPID(p)).addRMI(p, t);
		} catch (final NullPointerException e) {
			throw new IllegalArgumentException("Remote Proxy is not initialized");
		} catch (final RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Removes an object (or an agent) from a remote location. Called by various
	 * fields. Don't call this directly.
	 */
	public void removeFromRemote(final NumberND p, long id) {
		try {
			proxy.getField(partition.toPartitionPID(p)).removeRMI(p, id);
		} catch (final NullPointerException e) {
			throw new IllegalArgumentException("Remote Proxy is not initialized");
		} catch (final RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Removes all objects (and agents) from a remote location. Called by various
	 * fields. Don't call this directly.
	 */
	public void removeAllFromRemote(final NumberND p) {
		try {
			proxy.getField(partition.toPartitionPID(p)).removeAllRMI(p);
		} catch (final NullPointerException e) {
			throw new IllegalArgumentException("Remote Proxy is not initialized");
		} catch (final RemoteException e) {
			throw new RuntimeException(e);
		}
	}

//	TODO: Rajdeep - I don't know if we can remove the below method 
	/**
	 * Sends the local storage to the destination dst. All nodes will call this
	 * together.
	 * 
	 * @param dst
	 * @param fullField
	 * 
	 * @throws MPIException
	 */
//	public void collect(final int dst, final GridStorage fullField) throws MPIException {
//		final Serializable sendObj = localStorage.pack(new MPIParam(localBounds, haloBounds, MPIBaseType));
//
//		final ArrayList<Serializable> recvObjs = MPIUtil.<Serializable>gather(partition, sendObj, dst);
//
//		if (partition.getPID() == dst)
//			for (int i = 0; i < partition.getNumProcessors(); i++)
//				fullField.unpack(new MPIParam(partition.getLocalBounds(i), world, MPIBaseType), recvObjs.get(i));
//	}

//	TODO: Rajdeep - I don't know if we can remove the below method 
	/**
	 * Sends the local storage to the group root. All nodes from group with
	 * DQuadTreePartition will call this together.
	 * 
	 * @param level
	 * @param groupField
	 * @throws MPIException
	 */
//	public void collectGroup(final int level, final GridStorage groupField) throws MPIException {
//		if (!(partition instanceof QuadTreePartition))
//			throw new UnsupportedOperationException(
//					"Can only collect from group with DQuadTreePartition, got " + partition.getClass().getSimpleName());
//
//		final QuadTreePartition qt = (QuadTreePartition) partition;
//		final GroupComm gc = qt.getGroupComm(level);
//
//		if (gc != null) {
//			final Serializable sendObj = localStorage.pack(new MPIParam(localBounds, haloBounds, MPIBaseType));
//
//			final ArrayList<Serializable> recvObjs = MPIUtil.<Serializable>gather(gc.comm, sendObj, gc.groupRoot);
//
//			if (qt.isGroupMaster(gc))
//				for (int i = 0; i < recvObjs.size(); i++)
//					groupField.unpack(new MPIParam(gc.leaves.get(i).getShape(), gc.master.getShape(), MPIBaseType),
//							recvObjs.get(i));
//		}
//		MPI.COMM_WORLD.barrier();
//	}

//	TODO: Rajdeep - I don't know if we can remove the below method 
	/**
	 * The group root sends local storage for each node within a group. All nodes
	 * from group with DQuadTreePartition will call this together.
	 * 
	 * @param level
	 * @param groupField
	 * @throws MPIException
	 * @throws RemoteException
	 */
//	public void distributeGroup(final int level, final GridStorage groupField) throws MPIException, RemoteException {
//		if (!(partition instanceof QuadTreePartition))
//			throw new UnsupportedOperationException(
//					"Can only distribute to group with DQuadTreePartition, got "
//							+ partition.getClass().getSimpleName());
//
//		final QuadTreePartition qt = (QuadTreePartition) partition;
//		final GroupComm gc = qt.getGroupComm(level);
//		Serializable[] sendObjs = null;
//
//		if (gc != null) {
//			if (qt.isGroupMaster(gc)) {
//				sendObjs = new Serializable[gc.leaves.size()];
//				for (int i = 0; i < gc.leaves.size(); i++)
//					sendObjs[i] = groupField
//							.pack(new MPIParam(gc.leaves.get(i).getShape(), gc.master.getShape(), MPIBaseType));
//			}
//			final Serializable recvObj = MPIUtil.<Serializable>scatter(gc.comm, sendObjs, gc.groupRoot);
//
//			localStorage.unpack(new MPIParam(localBounds, haloBounds, MPIBaseType), recvObj);
//		}
//		syncHalo();
//	}

	/**
	 * Initializes the RMIProxy for this halogrid. Called by DSimState. Don't call
	 * this directly.
	 *
	 */
	public void initRemote() {
		proxy = new RMIProxy<>(partition, this);
	}

	/**
	 * Processes and clears the RMI queues for this halogrid. Called by DSimState.
	 * Don't call this directly.
	 * 
	 * @throws MPIException
	 * @throws RemoteException
	 */
	public void syncRemoveAndAdd() throws MPIException, RemoteException {
		for (final Pair<NumberND, Long> pair : removeQueue) {
			// remove from schedule
			T t = getLocal(pair.a, pair.b);

			// Assumes that t is not an agent if it's not Stopping
			if (t instanceof Stopping)
				unscheduleAgent((Stopping) t);

			removeLocal(pair.a, pair.b);
		}
		removeQueue.clear();

		for (final NumberND p : removeAllQueue) {
			ArrayList<T> list = getLocal(p);
			// Assumes that t is not an agent if it's not Stopping
			for (final T t : list)
				if (t instanceof Stopping)
					unscheduleAgent((Stopping) t);
			removeAllLocal(p);
		}
		removeAllQueue.clear();

		for (final Triplet<NumberND, T, double[]> pair : addQueue) {
			addLocal(pair.a, pair.b);
			
			// Reschedule
			if (pair.c != null) {
				// 	pair.c is scheduling information holding: ordering, time, [interval]
				if (pair.c.length == 3) { // Repeating agent, if array contains interval information
					state.schedule.scheduleRepeating(pair.c[1], (int) pair.c[0], (Steppable) pair.b, pair.c[2]);
//					System.out.println("repeating agent rescheduled!");
				}
				else {
					assert(pair.c.length == 2);
					state.schedule.scheduleOnce(pair.c[1], (int) pair.c[0], (Steppable) pair.b);
//					System.out.println("agent rescheduled!");
				}
			}
		}
		
		addQueue.clear();
	}

	// TODO: Should we generalize this method for all grids
	void unscheduleAgent(Stopping stopping) {
		Stoppable stop = stopping.getStoppable();
		if (stop == null) {
			// we're done
		} else if ((stop instanceof DistributedTentativeStep))
			((DistributedTentativeStep) stop).stop();
		else if ((stop instanceof DistributedIterativeRepeat))
			((DistributedIterativeRepeat) stop).stop();
		else
			throw new RuntimeException("Cannot remove agent " + stopping
					+ " because it is wrapped in a Stoppable other than a DistributedIterativeRepeat or DistributedTenativeStep. This should not happen.");
	}

	/**
	 * Syncs the halo regions of this grid with its neighbors. Called by DSimState.
	 * Don't call this directly.
	 *
	 */
	public void syncHalo() throws MPIException, RemoteException {
		
		
		//loc_disagree_all_points("syncHalo1");
		
		
		int numNeighbors = neighbors.size();
		Serializable[] sendObjs = new Serializable[numNeighbors];
		for (int i = 0; i < numNeighbors; i++)
			sendObjs[i] = localStorage.pack(neighbors.get(i).sendParam);

		ArrayList<Serializable> recvObjs = MPIUtil.<Serializable>neighborAllToAll(partition, sendObjs);

		for (int i = 0; i < numNeighbors; i++)
			localStorage.unpack(neighbors.get(i).recvParam, recvObjs.get(i));
		
		//loc_disagree_all_points("syncHalo2");


		for (final Pair<Promised, NumberND> pair : getAllQueue)
			pair.a.fulfill(getLocal(pair.b));
		getAllQueue.clear();

		for (final Triplet<Promised, NumberND, Long> trip : getQueue)
			trip.a.fulfill(getLocal(trip.b, trip.c));
		getQueue.clear();
		
		//loc_disagree_all_points("syncHalo3");

		

	}
	
	public void loc_disagree_all_points(String s) {
		
		for (Int2D a : localStorage.getShape().getPointList()) {
			if (localStorage.getAllObjects(a) != null) {
				for (T t : localStorage.getAllObjects(a)) {
					if (t instanceof DHeatBug) {
						//System.out.println("in HaloGrid2D syncHalo");
						//DHeatBug t2 = (DHeatBug)t;
						//System.out.println(s+" "+t2 +" h_loc "+new Int2D(t2.loc_x, t2.loc_y)+" p "+ a );

						//DSimState.loc_disagree(a, (DHeatBug)t, this.partition, s);
					}
				}
			}
		}
	}

	/**
	 * Adds an incoming to the field. Has cases for the type of object we are
	 * adding. Called by DSimState. Don't call this directly.
	 *
	 */
	@SuppressWarnings("unchecked")
	public void addPayload(PayloadWrapper payloadWrapper) {
		if (payloadWrapper.payload instanceof DistributedIterativeRepeat) {
			final DistributedIterativeRepeat iterativeRepeat = (DistributedIterativeRepeat) payloadWrapper.payload;
			// System.out.println((T) iterativeRepeat.getSteppable()+" being synced");
			addLocal((NumberND) payloadWrapper.loc, (T) iterativeRepeat.getSteppable());

		} else if (payloadWrapper.payload instanceof AgentWrapper) {
			final AgentWrapper agentWrapper = (AgentWrapper) payloadWrapper.payload;
			addLocal((NumberND) payloadWrapper.loc, (T) agentWrapper.agent);

		} else {
			addLocal((NumberND) payloadWrapper.loc, (T) payloadWrapper.payload);
		}
	}

	/* RMI METHODS */
	/**
	 * This method queues an object t to be set at or added to point p at end of the
	 * time step, via addLocal(). This is called remotely via RMI, and is part of
	 * the TransportRMIInterface. Don't call this directly.
	 */
	public void addRMI(NumberND p, T t) throws RemoteException {
		addQueue.add(new Triplet<>(p, t, null));
	}
	
	/**
	 * This method queues an agent t and scheduling information to be set at or added to point p at end of the
	 * time step, via addLocal(). This is called remotely via RMI, and is part of
	 * the TransportRMIInterface. Don't call this directly.
	 */
	public void addRMI(NumberND p, T t, int ordering, double time) throws RemoteException {
		addQueue.add(new Triplet<>(p, t, new double[]{ordering, time}));
	}
	
	public void addRMI(NumberND p, T t, int ordering, double time, double interval) throws RemoteException {
		addQueue.add(new Triplet<>(p, t, new double[]{ordering, time, interval}));
	}


	/**
	 * This method queues an object at a point p with the given id to be removed at
	 * the end of the time step via removeLocal(). For DObjectGrid2D the object is
	 * replaced with null. For DIntGrid2D and DDoubleGrid2D the object is replaced
	 * with 0. This is called remotely via RMI, and is part of the
	 * TransportRMIInterface. Don't call this directly.
	 */
	public void removeRMI(NumberND p, long id) throws RemoteException {
		removeQueue.add(new Pair<>(p, id));
	}

	/**
	 * This method queues all objects at a point p to be removed at the end of the
	 * time step. This is only used by DDenseGrid2D. This is called remotely via
	 * RMI, and is part of the TransportRMIInterface. Don't call this directly.
	 */
	public void removeAllRMI(final NumberND p) throws RemoteException {
		removeAllQueue.add(p);
	}

	/**
	 * This method queues the get requests via RMI. From the Perspective of the
	 * requesting node this grid is remote, it will add the result to a queue and
	 * return all the objects in the queue after the current time step. This is
	 * called remotely via RMI, and is part of the TransportRMIInterface. Don't call
	 * this directly.
	 */
	public void getRMI(NumberND p, RemotePromise promise) throws RemoteException {
		// Make promise remote
		// update promise remotely
		// Must happen asynchronously
		// Do it in the queue
		// Add to a queue here
		getAllQueue.add(new Pair<>(promise, p));
	}

	/**
	 * This method entertains the get requests via RMI. From the Perspective of the
	 * requesting node this grid is remote, it will add the result to a queue and
	 * return all the objects in the queue after the current time step. This is
	 * called remotely via RMI, and is part of the TransportRMIInterface. Don't call
	 * this directly.
	 */
	public void getRMI(NumberND p, long id, RemotePromise promise) throws RemoteException {
		// Make promise remote
		// update promise remotely
		// Must happen asynchronously
		getQueue.add(new Triplet<>(promise, p, id));
	}

	//// LOCAL UPDATE METHODS CALLED BY THE RMI QUEUES

	/**
	 * Adds or sets an object in local storage at the given (global) point p.
	 * 
	 * @param p location
	 * @param t Object to add
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void addLocal(final NumberND p, final T t) {
		if (localStorage instanceof ContinuousStorage)
			((ContinuousStorage) localStorage).addObject(p, (DObject) t);
		else
			localStorage.addObject(p, t);
	}

	/**
	 * This method only works locally, it uses the global coordinates of an Object
	 * for ContinuousStorage and local coordinates for everything else.
	 * 
	 * @param p location
	 * @return All objects at p
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	ArrayList<T> getLocal(NumberND p) {
		if (localStorage instanceof ContinuousStorage)
			return ((ContinuousStorage) localStorage).getAllObjects(p);
		else
			return localStorage.getAllObjects(p);
	}

	/**
	 * This method only works locally, it uses the global coordinates of an Object
	 * for ContinuousStorage and local coordinates for everything else.
	 * 
	 * @param p  location
	 * @param id
	 * @return Object with the given id at the point p
	 */
	@SuppressWarnings("unchecked")
	T getLocal(NumberND p, long id) {
		if (localStorage instanceof ContinuousStorage)
			return (T) ((ContinuousStorage<?>) localStorage).getObject(p, id);
		else
			return localStorage.getObject(p, id);
	}

	/**
	 * Removes the object with the given ID from the local storage at the given
	 * point. Note that this does not work for DIntGrid2D nor DDoubleGrid2D, which
	 * will throw exceptions.
	 */
	void removeLocal(NumberND p, long id) {
		if (localStorage instanceof ContinuousStorage)
			((ContinuousStorage<?>) localStorage).removeObject(p, id);
		else
			localStorage.removeObject(p, id);
	}

	/** Clears all objects from the local storage at the given point. */
	void removeAllLocal(NumberND p) {
		if (localStorage instanceof ContinuousStorage)
			((ContinuousStorage<?>) localStorage).clear(p);
		else
			localStorage.clear(p);
	}

	public void removeAgent(final NumberND p, long id) {
		if (inLocal(p)) {
			T t = getLocal(p, id);

			if (!(t instanceof Stopping))
				throw new IllegalArgumentException("t must be a Stopping");

			removeLocal(p, id);
			((Stopping) t).getStoppable().stop();
		} else {
			removeFromRemote(p, id);
		}
	}

	public String toString() {
		return String.format("PID %d Storage %s", partition.getPID(), localStorage);
	}

	/**
	 * Helper class to organize neighbor-related data structures and methods
	 */
	class Neighbor {
		MPIParam sendParam;
		MPIParam recvParam;

		public Neighbor(final IntRect2D neighborPart) {
			final ArrayList<IntRect2D> sendOverlaps = generateOverlaps(localBounds,
					neighborPart.resize(partition.getAOI()));

			
			final ArrayList<IntRect2D> recvOverlaps = generateOverlaps(haloBounds, neighborPart);

			if (localBounds.contains(57,57)){
				
				for (IntRect2D r : sendOverlaps) {
					System.out.println("send "+r);
				}
				System.out.println("----");
				
				for (IntRect2D r : recvOverlaps) {
					System.out.println("rec "+r);
				}
				
				//System.exit(-1);
				
				
			}

			
			assert sendOverlaps.size() == recvOverlaps.size();

			sendParam = new MPIParam(sendOverlaps, haloBounds, localStorage.getMPIBaseType());
			recvParam = new MPIParam(recvOverlaps, haloBounds, localStorage.getMPIBaseType());
			
			

			
		}

		private ArrayList<IntRect2D> generateOverlaps(final IntRect2D p1, final IntRect2D p2) {
			final ArrayList<IntRect2D> overlaps = new ArrayList<IntRect2D>();

			if (partition.isToroidal()) {
				int xLen = worldWidth;
				int yLen = worldHeight;

				Int2D[] shifts = {
						new Int2D(-xLen, -yLen),
						new Int2D(-xLen, 0),
						new Int2D(-xLen, yLen),
						new Int2D(0, -yLen),
						new Int2D(0, yLen),
						new Int2D(xLen, -yLen),
						new Int2D(xLen, 0),
						new Int2D(xLen, yLen),
						new Int2D(0, 0),
				};

				for (final Int2D p : shifts) {
					final IntRect2D sp = p2.add(p);		// new int[] { p.x, p.y });
					if (p1.intersects(sp))
						overlaps.add(p1.getIntersection(sp));
				}
			} else
				overlaps.add(p1.getIntersection(p2));

			return overlaps;
		}
	}
}

class Pair<A, B> implements Serializable {
	private static final long serialVersionUID = 1L;

	public final A a;
	public final B b;

	public Pair(A a, B b) {
		this.a = a;
		this.b = b;
	}
}

class Triplet<A, B, C> implements Serializable {
	private static final long serialVersionUID = 1L;

	public final A a;
	public final B b;
	public final C c;

	public Triplet(A a, B b, C c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}
}
