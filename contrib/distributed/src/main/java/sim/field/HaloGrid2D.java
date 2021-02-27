package sim.field;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import mpi.*;
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
public class HaloGrid2D<T extends Serializable, S extends GridStorage> extends UnicastRemoteObject
		implements TransportRMIInterface<T, NumberND>, Synchronizable {
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

	// The following four queues are how RMI adds, removes, and fetches elements on behalf of remote processors.  

	// Queue of Promised results from getRMI
	 ArrayList<Pair<Promised, Serializable>> getQueue = new ArrayList<>();
	 // Queue of requests to add things to the grid via RMI
	 ArrayList<Pair<NumberND, T>> addQueue = new ArrayList<>();
	 // Queue of requests to remove things from the grid via RMI
	 ArrayList<Pair<NumberND, Long>> removeQueue = new ArrayList<>();
	 // Queue of requests to remove all elements at certain locations from the grid via RMI
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

		// register callbacks
		final List<GridStorage> tempStor = new ArrayList<GridStorage>();
		final QuadTreePartition q = (QuadTreePartition) partition;
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

	/** Returns the HaloGrid2D's partition. */	
	public Partition getPartition() { return partition; }

	/** Returns the HaloGrid2D's field index. */	
	public int getFieldIndex() { return fieldIndex; }
	
	/**
	 * Resizes the partition and halo region, and reloads the neighbors.
	 */
	public void reload() {
		localBounds = partition.getLocalBounds();
		// Get the partition representing halo and local area by expanding the original
		// partition by aoi at each dimension
		haloBounds = localBounds.resize(partition.getAOI());
		localStorage.reshape(haloBounds);
		// Get the partition representing private area by shrinking the original
		// partition by aoi at each dimension

		// Get the neighbors and create Neighbor objects
		neighbors = new ArrayList<Neighbor>();
		for (int id : partition.getNeighborPIDs()) {
			neighbors.add(new Neighbor(partition.getLocalBounds(id)));
		}
	}

	/**
	 * Shifts point p to give location on the local partition
	 * 
	 * @param p
	 * @return location on the local partition
	 */
	public Int2D toLocalPoint(final Int2D p) {
		return p.subtract(haloBounds.ul().toArray());
	}

	/**
	 * Removes all Objects from the point p
	 * 
	 * @param p
	 */
	public void removeAllAgentsAndObjects(final NumberND p) {
		if (!inLocal(p))
			removeFromRemote(p);
		else {
			if (localStorage instanceof ContinuousStorage)
				localStorage.clear(p);
			else
				localStorage.clear(toLocalPoint((Int2D) p));
		}
	}

	public void addAgent(final NumberND p, final T t, final int ordering, final double time) {
		if (!(t instanceof Stopping))
			throw new IllegalArgumentException("t must be a Stopping");

		final Stopping agent = (Stopping) t;

		if (inLocal(p)) {
			// TODO: remove local case
			// TODO: check this
			// add(p, t);
			addLocal(p, t);

			state.schedule.scheduleOnce(time, ordering, agent);
		} else
			state.getTransporter().migrateAgent(ordering, time, agent, partition.toPartitionPID(p), p, this.fieldIndex);
	}

	public void addAgent(final NumberND p, final T t, final int ordering, final double time, final double interval) {
		if (!(t instanceof Stopping))
			throw new IllegalArgumentException("t must be a Stopping");
		Stopping stopping = (Stopping) t;

		if (inLocal(p)) {
			// TODO: check this
			// add(p, t);
			addLocal(p, t);

			final IterativeRepeat iterativeRepeat = state.schedule.scheduleRepeating(time, ordering, stopping,
					interval);
			stopping.setStoppable(iterativeRepeat);
		} else {
			final DistributedIterativeRepeat iterativeRepeat = new DistributedIterativeRepeat(stopping, time, interval,
					ordering);
			state.getTransporter().migrateRepeatingAgent(iterativeRepeat, partition.toPartitionPID(p), p, this.fieldIndex);
		}
	}

	public void removeAgent(final NumberND p, long id) {
		if (!inLocal(p))
			throw new IllegalArgumentException("p must be local");

		T t = getLocal(p, id);

		if (!(t instanceof Stopping))
			throw new IllegalArgumentException("t must be a Stopping");

		final Stopping stopping = (Stopping) t;
		removeLocal(p, id);
		stopping.getStoppable().stop();
	}

	/**
	 * Get using RMI the object (or objects) contained at a point p
	 * 
	 * @param p point to get from
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
	 * Get using RMI the object (or objects) contained at a point p
	 * 
	 * @param p point to get from
	 */
	public Promised getFromRemote(final NumberND p, long id) {
		try {
			RemotePromise remotePromise = new RemotePromise();
			// Make promise remote
			// update promise remotely
			proxy.getField(partition.toPartitionPID(p)).getRMI(p, id, remotePromise);
			return remotePromise;
		} catch (final NullPointerException e) {
			throw new IllegalArgumentException("Remote Proxy is not initialized");
		} catch (final RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Add using RMI an object t contained at a point p
	 * 
	 * @param p point to add at
	 * @param t object to add
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
	 * Remove using RMI an object t contained at a point p
	 * 
	 * @param p point to remove from
	 * @param t object to remove
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
	 * Remove using RMI all objects contained at a point p
	 * 
	 * @param p point to remove from
	 */
	public void removeFromRemote(final NumberND p) {
		try {
			proxy.getField(partition.toPartitionPID(p)).removeRMI(p);
		} catch (final NullPointerException e) {
			throw new IllegalArgumentException("Remote Proxy is not initialized");
		} catch (final RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @return local storage (for this partition)
	 */
	public GridStorage getStorage() {
		return localStorage;
	}

	// Various point queries
	/**
	 * @param point
	 * @return true if point is within the global grid
	 */
	public boolean inGlobal(final Int2D point) {
		if (!(point.x >= 0 && point.x < worldWidth)) {
			return false;
		}
		if (!(point.y >= 0 && point.y < worldHeight)) {
			return false;
		}
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

	/* METHODS SYNCHO */

	/**
	 * Initializes the RMIProxy for this halogrid.
	 *
	 */
	public void initRemote() {
		proxy = new RMIProxy<>(partition, this);
	}

	/**
	 * Processes and clears the RMI queues for this halogrid.
	 * 
	 * @throws MPIException
	 * @throws RemoteException
	 */
	public void syncRemoveAndAdd() throws MPIException, RemoteException {
		for (final Pair<NumberND, Long> pair : removeQueue)
			removeLocal(pair.a, pair.b);
		removeQueue.clear();

		for (final NumberND p : removeAllQueue)
			removeAllLocal(p);
		removeAllQueue.clear();

		for (final Pair<NumberND, T> pair : addQueue)
			addLocal(pair.a, pair.b);
		addQueue.clear();
	}

	/**
	 * Syncs the halo regions of this grid with its neighbors
	 *
	 */
	public void syncHalo() throws MPIException, RemoteException {
		int numNeighbors = neighbors.size();
		Serializable[] sendObjs = new Serializable[numNeighbors];
		for (int i = 0; i < numNeighbors; i++) {
			sendObjs[i] = localStorage.pack(neighbors.get(i).sendParam);
		}

		ArrayList<Serializable> recvObjs = MPIUtil.<Serializable>neighborAllToAll(partition, sendObjs);

		for (int i = 0; i < numNeighbors; i++)
			localStorage.unpack(neighbors.get(i).recvParam, recvObjs.get(i));

		for (final Pair<Promised, Serializable> pair : getQueue) {
			pair.a.fulfill(pair.b);
			// System.out.println(pair);
		}
		getQueue.clear();
	}

	/**
	 * Adds an incoming to the field. Has cases for the type of object we are
	 * adding.
	 *
	 */
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
	 * This method queues an object t at a point p to be added at the end of the
	 * time step.
	 */
	public void addRMI(NumberND p, T t) throws RemoteException {
		addQueue.add(new Pair<>(p, t));
	}

	/**
	 * This method queues an object at a point p with the given id to be removed at
	 * the end of the time step.
	 */
	public void removeRMI(NumberND p, long id) throws RemoteException {
		removeQueue.add(new Pair<>(p, id));
	}

	/**
	 * This method queues all objects at a point p to be removed at the end of the
	 * time step.
	 */
	public void removeRMI(final NumberND p) throws RemoteException {
		removeAllQueue.add(p);
	}

	/**
	 * This method entertains the get requests via RMI. From the Perspective of the
	 * requesting node this grid is remote, it will add the result to a queue and
	 * return all the objects in the queue after the current time step.
	 */
	public void getRMI(NumberND p, Promised promise) throws RemoteException {
		// Make promise remote
		// update promise remotely
		// Must happen asynchronously
		// Do it in the queue
		// Add to a queue here
		getQueue.add(new Pair<>(promise, getLocal(p)));
	}

	/**
	 * This method entertains the get requests via RMI. From the Perspective of the
	 * requesting node this grid is remote, it will add the result to a queue and
	 * return all the objects in the queue after the current time step.
	 */
	public void getRMI(NumberND p, long id, Promised promise) throws RemoteException {
		// Make promise remote
		// update promise remotely
		// Must happen asynchronously
		getQueue.add(new Pair<>(promise, getLocal(p, id)));
	}

	/**
	 * This method only works locally, it uses the global coordinates of an Object
	 * for ContinuousStorage and local coordinates for everything else.
	 * 
	 * 
	 * @param p location
	 * @param t Object to add
	 */
	public void addLocal(final NumberND p, final T t) {
		if (localStorage instanceof ContinuousStorage)
			localStorage.addObject(p, t);
		else
			localStorage.addObject(toLocalPoint((Int2D) p), t);
	}

	//// FIXME -- this method definitely looks wrong

	/**
	 * This method only works locally, it uses the global coordinates of an Object
	 * for ContinuousStorage and local coordinates for everything else.
	 * 
	 * @param p location
	 * @return All objects at p
	 */
	public T getLocal(NumberND p) {
		if (localStorage instanceof ContinuousStorage)
			return (T) localStorage.getAllObjects(p);
		else
			return (T) localStorage.getAllObjects(toLocalPoint((Int2D) p));
	}

	//// FIXME -- this method definitely looks wrong

	/**
	 * This method only works locally, it uses the global coordinates of an Object
	 * for ContinuousStorage and local coordinates for everything else.
	 * 
	 * @param p  location
	 * @param id
	 * @return Object with the given id at the point p
	 */
	public T getLocal(NumberND p, long id) {
		if (localStorage instanceof ContinuousStorage)
			return (T) localStorage.getObject(p, id);
		else
			return (T) localStorage.getObject(toLocalPoint((Int2D) p), id);
	}

	/**
	 * Removes the object with the given ID from the local storage at the given
	 * point. Note that this does not work for DIntGrid2D nor DDoubleGrid2D, which
	 * will throw exceptions.
	 */
	public void removeLocal(NumberND p, long id) {
		if (localStorage instanceof ContinuousStorage) {
			int old_size = ((ContinuousStorage) localStorage).getCell((Double2D) p).size();
			localStorage.removeObject(p, id);
			if (old_size == ((ContinuousStorage) localStorage).getCell((Double2D) p).size()) {
				System.out.println("remove not successful!");
				System.out.println(p + " " + id);
				System.exit(-1);
			}
		} else {

			localStorage.removeObject(toLocalPoint((Int2D) p), id);
		}
	}

	/** Clears all objects from the local storage at the given point. */
	public void removeAllLocal(NumberND p) {
		if (localStorage instanceof ContinuousStorage) {
			int old_size = ((ContinuousStorage) localStorage).getCell((Double2D) p).size();

			localStorage.clear(p);

			if (old_size == ((ContinuousStorage) localStorage).getCell((Double2D) p).size()) {
				System.out.println("remove not sucessful!");
				System.out.println(p);
				System.exit(-1);
			}
		}

		else
			localStorage.clear(toLocalPoint((Int2D) p));
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
					final IntRect2D sp = p2.shift(new int[] { p.x, p.y });
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
