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
import sim.engine.*;
import java.util.function.Consumer;

/**
 * All fields in distributed MASON must contain this class. Stores
 * objects/agents and implements methods to add, move and remove them.
 *
 * @param <T> The Class of Object to store in the field
 * @param <S> The Type of Storage to use
 */
public class HaloGrid2D<T extends Serializable, S extends GridStorage>
		extends UnicastRemoteObject
		implements TransportRMIInterface<T, NumberND>, Synchronizable {
	private static final long serialVersionUID = 1L;

	 final ArrayList<Pair<Promised, Serializable>> getQueue = new ArrayList<>();
	 final ArrayList<Pair<NumberND, T>> inQueue = new ArrayList<>();
	 final ArrayList<Pair<NumberND, Long>> removeQueue = new ArrayList<>();
	 final ArrayList<NumberND> removeAllQueue = new ArrayList<>();

	/**
	 * Helper class to organize neighbor-related data structures and methods
	 */
	class Neighbor {
		MPIParam sendParam;
		MPIParam recvParam;

		public Neighbor(final IntRect2D neighborPart) {
			final ArrayList<IntRect2D> sendOverlaps = generateOverlaps(localPart, neighborPart.resize(partition.getAOI()));
			final ArrayList<IntRect2D> recvOverlaps = generateOverlaps(haloPart, neighborPart);

			assert sendOverlaps.size() == recvOverlaps.size();

			// Sort these overlaps so that they corresponds to each other
			// Collections.sort(sendOverlaps);
			// Collections.sort(recvOverlaps, Collections.reverseOrder());

			sendParam = new MPIParam(sendOverlaps, haloPart, MPIBaseType);
			recvParam = new MPIParam(recvOverlaps, haloPart, MPIBaseType);
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

	 int worldWidth;
	 int worldHeight;
	 IntRect2D world;
	 IntRect2D haloPart;
	 IntRect2D localPart;
	// IntRect2D privatePart;

	protected List<Neighbor> neighbors; // pointer to the processors who's partitions neighbor me
	public final S localStorage;
	public Partition partition;
	transient public Datatype MPIBaseType;

	public final int fieldIndex;

	public RMIProxy<T, NumberND> proxy;
	private final DSimState state;

	private final Object lockRMI = new boolean[1];

	public HaloGrid2D(S storage, DSimState state) throws RemoteException {
		super();
		partition = state.getPartition();
		localStorage = storage;
		this.state = state;
		// init variables that don't change with the partition scheme
		world = partition.getWorldBounds();
		worldWidth = partition.getWorldWidth();
		worldHeight = partition.getWorldHeight();
		MPIBaseType = localStorage.getMPIBaseType();
		registerCallbacks();
		// init variables that may change with the partition scheme
		reload();
		fieldIndex = state.registerField(this);
	}

	private void registerCallbacks() {

		final List<GridStorage> tempStor = new ArrayList<GridStorage>();
		final QuadTreePartition q = (QuadTreePartition) partition;
		partition.registerPreCommit(new Consumer() {
			public void accept(Object arg) { }
		});
		partition.registerPostCommit(new Consumer() {
			public void accept(Object t) { reload(); }
		});

	}

	/**
	 * Resizes partition and halo. Also sets up the neighbors
	 */
	public void reload() {
		localPart = partition.getLocalBounds();
		// Get the partition representing halo and local area by expanding the original
		// partition by aoi at each dimension
		haloPart = localPart.resize(partition.getAOI());
		localStorage.reshape(haloPart);
		// Get the partition representing private area by shrinking the original
		// partition by aoi at each dimension

		//privatePart = localPart.resize(0 - partition.getAOI()); // Negative aoi
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
		return p.subtract(haloPart.ul().toArray());
	}

	/**
	 * Wraps point around to give location on the local partition
	 * 
	 * @param p
	 * @return location in a toroidal plane
	 */
	public Int2D toToroidal(final Int2D p) {
		return world.toToroidal(p);
	}

	public void removeAllAgentsAndObjects(final NumberND p) {
		if (!inLocal(p))
			removeFromRemote(p);
		else {
			if (localStorage instanceof ContinuousStorage)
				localStorage.removeObjects(p);
			else
				localStorage.removeObjects(toLocalPoint((Int2D) p));
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
		// TODO: Should we have remote here?
		// TODO: add RMI case
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
			//state.getTransporter().migrateRepeatingAgent(iterativeRepeat, partition.toPartitionPID(p));
			state.getTransporter().migrateRepeatingAgent(iterativeRepeat, partition.toPartitionPID(p), p, this.fieldIndex);

		}
	}

	public void removeAgent(final NumberND p, long id) {
		if (!inLocal(p))
			throw new IllegalArgumentException("p must be local");

		T t = getLocal(p, id);

		// TODO: Should we remove the instanceOf check and assume that the
		// pre-conditions are always met?
		if (!(t instanceof Stopping))
			throw new IllegalArgumentException("t must be a Stopping");

		final Stopping stopping = (Stopping) t;
		removeLocal(p, id);
		stopping.getStoppable().stop();
	}

	// TODO make a copy of the storage which will be used by the remote field access

	// TODO: Do we need to check for type safety here?
	// If the getField method returns the current field then
	// this cast should work

	/**
	 * Get using RMI the object (or objects) contained at a point p
	 * 
	 * @param p point to get from
	 */
	public Promised getFromRemote(final NumberND p) {
		try {
			RemotePromise remotePromise = new RemotePromise();
			// Make promise remote
			// update promise remotely
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

	public IntRect2D getLocalBounds()
		{
		return localPart;
		}
		
	public IntRect2D getHaloBounds()
		{
		return haloPart;
		}

//	public IntRect2D getPrivateBounds()
//		{
//		return privatePart;
//		}
		
	/**
	 * @param point
	 * @return true if point is local
	 */
	public boolean inLocal(final NumberND point) {
		return localPart.contains(point);
	}

	/**
	 * @param point
	 * @return true if point is local and not in the halo
	 */
//	public boolean inPrivate(final NumberND point) {
//		return privatePart.contains(point);
//	}

	/**
	 * @param point
	 * @return true if point is local or in the halo
	 */
	public boolean inHalo(final NumberND point) {
		return haloPart.contains(point);
	}


	/**
	 * @param point
	 * @return a point wrapped around considering the toroidal halo region, if possible.  If out of the halo region, returns null.
	 */
	public Double2D toHaloToroidal(final Double2D point) 
		{
		if (inHalo(point)) return point;
		double x = point.x;
		double y = point.y;
		double aoi = partition.getAOI();
		double height = worldHeight;
		double width = worldWidth;
		
		double lowx = (x - width);
		if (lowx >= 0 - aoi)
			{
			double lowy = (y - height);
			if (lowy >= 0 - aoi)
				{
				if (haloPart.contains(lowx, lowy))
					return new Double2D(lowx, lowy);
				else return null;
				}
			double highy = (y + height);
			if (highy < height + aoi)
				{
				if (haloPart.contains(lowx, highy))
					return new Double2D(lowx, highy);
				else return null;
				}
			else
				{
				if (haloPart.contains(lowx, y))
					return new Double2D(lowx, y);
				else return null;
				}
			}

		double highx = (x + width);
		if (highx < width + aoi)
			{
			double lowy = (y - height);
			if (lowy >= 0 - aoi)
				{
				if (haloPart.contains(highx, lowy))
					return new Double2D(highx, lowy);
				else return null;
				}
			double highy = (y + height);
			if (highy < height + aoi)
				{
				if (haloPart.contains(highx, highy))
					return new Double2D(highx, highy);
				else return null;
				}
			else
				{
				if (haloPart.contains(highx, y))
					return new Double2D(highx, y);
				else return null;
				}
			}

		double lowy = (y - height);
		if (lowy >= 0 - aoi)
			{
			if (haloPart.contains(x, lowy))
				return new Double2D(x, lowy);
				else return null;
			}
			
		double highy = (y + height);
		if (highy < height + aoi)
			{
			if (haloPart.contains(x, highy))
				return new Double2D(x, highy);
				else return null;
			}
		
		return null;
		}


	/**
	 * @param point
	 * @return a point wrapped around considering the toroidal halo region, if possible.  If out of the halo region, returns null.
	 */
	public Int2D toHaloToroidal(final Int2D point) 
		{
		if (inHalo(point)) return point;  // easy
		int x = point.x;
		int y = point.y;
		int aoi = partition.getAOI();
		int height = worldHeight;
		int width = worldWidth;
		
		int lowx = (x - width);
		if (lowx >= 0 - aoi)
			{
			int lowy = (y - height);
			if (lowy >= 0 - aoi)
				{
				if (haloPart.contains(lowx, lowy))
					return new Int2D(lowx, lowy);
				else return null;
				}
			int highy = (y + height);
			if (highy < height + aoi)
				{
				if (haloPart.contains(lowx, highy))
					return new Int2D(lowx, highy);
				else return null;
				}
			else
				{
				if (haloPart.contains(lowx, y))
					return new Int2D(lowx, y);
				else return null;
				}
			}

		int highx = (x + width);
		if (highx < width + aoi)
			{
			int lowy = (y - height);
			if (lowy >= 0 - aoi)
				{
				if (haloPart.contains(highx, lowy))
					return new Int2D(highx, lowy);
				else return null;
				}
			int highy = (y + height);
			if (highy < height + aoi)
				{
				if (haloPart.contains(highx, highy))
					return new Int2D(highx, highy);
				else return null;
				}
			else
				{
				if (haloPart.contains(highx, y))
					return new Int2D(highx, y);
				else return null;
				}
			}

		int lowy = (y - height);
		if (lowy >= 0 - aoi)
			{
			if (haloPart.contains(x, lowy))
				return new Int2D(x, lowy);
				else return null;
			}
			
		int highy = (y + height);
		if (highy < height + aoi)
			{
			if (haloPart.contains(x, highy))
				return new Int2D(x, highy);
				else return null;
			}
		
		return null;
		}
		
	/**
	 * @param point
	 * @return true if point is local or in the halo considering toroidal wrap-around
	 */
	public boolean inHaloToroidal(NumberND point) 
		{
		double[] p = point.toArrayAsDouble();
		return inHaloToroidal(p[0], p[1]);
		}
		

	public boolean inHaloToroidal(double x, double y) 
		{
		if (haloPart.contains(x, y)) return true;
		
		double aoi = partition.getAOI();
		double height = worldHeight;
		double width = worldWidth;
		
		double lowx = (x - width);
		if (lowx >= 0 - aoi)
			{
			double lowy = (y - height);
			if (lowy >= 0 - aoi)
				{
				return (haloPart.contains(lowx, lowy));
				}
			double highy = (y + height);
			if (highy < height + aoi)
				{
				return (haloPart.contains(lowx, highy));
				}
			else
				{
				return (haloPart.contains(lowx, y));
				}
			}

		double highx = (x + width);
		if (highx < width + aoi)
			{
			double lowy = (y - height);
			if (lowy >= 0 - aoi)
				{
				return (haloPart.contains(highx, lowy));
				}
			double highy = (y + height);
			if (highy < height + aoi)
				{
				return (haloPart.contains(highx, highy));
				}
			else
				{
				return (haloPart.contains(highx, y));
				}
			}

		double lowy = (y - height);
		if (lowy >= 0 - aoi)
			{
			return (haloPart.contains(x, lowy));
			}
			
		double highy = (y + height);
		if (highy < height + aoi)
			{
			return (haloPart.contains(x, highy));
			}
		
		return false;
		}

	/**
	 * @param point
	 * @return true if point p is local and in the halo
	 */
//	public boolean inShared(final NumberND point) {
//		return inLocal(point) && !inPrivate(point);
//	}

	/**
	 * @param point
	 * @return true if point p is in the halo (and not local)
	 */
//	public boolean inHalo(final NumberND point) {
//		return inLocalAndHalo(point) && !inLocal(point);
//	}

	/**
	 * Sends the local storage to the destination dst. All nodes will call this
	 * together.
	 * 
	 * @param dst
	 * @param fullField
	 * 
	 * @throws MPIException
	 */
	public void collect(final int dst, final GridStorage fullField) throws MPIException {
		final Serializable sendObj = localStorage.pack(new MPIParam(localPart, haloPart, MPIBaseType));

		final ArrayList<Serializable> recvObjs = MPIUtil.<Serializable>gather(partition, sendObj, dst);

		if (partition.getPID() == dst)
			for (int i = 0; i < partition.getNumProcessors(); i++)
				fullField.unpack(new MPIParam(partition.getLocalBounds(i), world, MPIBaseType), recvObjs.get(i));
	}

	/**
	 * Sends the local storage to the group root. All nodes from group with
	 * DQuadTreePartition will call this together.
	 * 
	 * @param level
	 * @param groupField
	 * @throws MPIException
	 */
	public void collectGroup(final int level, final GridStorage groupField) throws MPIException {
		if (!(partition instanceof QuadTreePartition))
			throw new UnsupportedOperationException(
					"Can only collect from group with DQuadTreePartition, got " + partition.getClass().getSimpleName());

		final QuadTreePartition qt = (QuadTreePartition) partition;
		final GroupComm gc = qt.getGroupComm(level);

		if (gc != null) {
			final Serializable sendObj = localStorage.pack(new MPIParam(localPart, haloPart, MPIBaseType));

			final ArrayList<Serializable> recvObjs = MPIUtil.<Serializable>gather(gc.comm, sendObj, gc.groupRoot);

			if (qt.isGroupMaster(gc))
				for (int i = 0; i < recvObjs.size(); i++)
					groupField.unpack(new MPIParam(gc.leaves.get(i).getShape(), gc.master.getShape(), MPIBaseType),
							recvObjs.get(i));
		}
		MPI.COMM_WORLD.barrier();
	}

	/**
	 * The group root sends local storage for each node within a group. All nodes
	 * from group with DQuadTreePartition will call this together.
	 * 
	 * @param level
	 * @param groupField
	 * @throws MPIException
	 * @throws RemoteException
	 */
	public void distributeGroup(final int level, final GridStorage groupField) throws MPIException, RemoteException {
		if (!(partition instanceof QuadTreePartition))
			throw new UnsupportedOperationException(
					"Can only distribute to group with DQuadTreePartition, got "
							+ partition.getClass().getSimpleName());

		final QuadTreePartition qt = (QuadTreePartition) partition;
		final GroupComm gc = qt.getGroupComm(level);
		Serializable[] sendObjs = null;

		if (gc != null) {
			if (qt.isGroupMaster(gc)) {
				sendObjs = new Serializable[gc.leaves.size()];
				for (int i = 0; i < gc.leaves.size(); i++)
					sendObjs[i] = groupField
							.pack(new MPIParam(gc.leaves.get(i).getShape(), gc.master.getShape(), MPIBaseType));
			}
			final Serializable recvObj = MPIUtil.<Serializable>scatter(gc.comm, sendObjs, gc.groupRoot);

			localStorage.unpack(new MPIParam(localPart, haloPart, MPIBaseType), recvObj);
		}
		syncHalo();
	}

	/* METHODS SYNCHO */

	public void initRemote() {
		proxy = new RMIProxy<>(partition, this);
	}

	public void syncRemoveAndAdd() throws MPIException, RemoteException {
		for (final Pair<NumberND, Long> pair : removeQueue)
			removeLocal(pair.a, pair.b);
		removeQueue.clear();

		for (final NumberND p : removeAllQueue)
			removeAllLocal(p);
		removeAllQueue.clear();

		for (final Pair<NumberND, T> pair : inQueue)
			addLocal(pair.a, pair.b);
		inQueue.clear();
	}

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
			//System.out.println(pair);
		}
		getQueue.clear();
	}

	public void syncObject(PayloadWrapper payloadWrapper) {
		if (payloadWrapper.payload instanceof DistributedIterativeRepeat) {
			final DistributedIterativeRepeat iterativeRepeat = (DistributedIterativeRepeat) payloadWrapper.payload;
			//System.out.println((T) iterativeRepeat.getSteppable()+" being synced");
			addLocal((NumberND) payloadWrapper.loc, (T) iterativeRepeat.getSteppable());

		} else if (payloadWrapper.payload instanceof AgentWrapper) {
			final AgentWrapper agentWrapper = (AgentWrapper) payloadWrapper.payload;
			addLocal((NumberND) payloadWrapper.loc, (T) agentWrapper.agent);

		} else {
			addLocal((NumberND) payloadWrapper.loc, (T) payloadWrapper.payload);
		}
	}

	/* RMI METHODS */
	public void addRMI(NumberND p, T t) throws RemoteException {
		inQueue.add(new Pair<>(p, t));
	}

	public void removeRMI(NumberND p, long id) throws RemoteException {
		removeQueue.add(new Pair<>(p, id));
	}

	public void removeRMI(final NumberND p) throws RemoteException {
		removeAllQueue.add(p);
	}

	public void getRMI(NumberND p, Promised promise) throws RemoteException {
		// Make promise remote
		// update promise remotely
		// Must happen asynchronously
		// Do it in the queue
		// Add to a queue here
		getQueue.add(new Pair<>(promise, getLocal(p)));
	}

	public void getRMI(NumberND p, long id, Promised promise) throws RemoteException {
		// Make promise remote
		// update promise remotely
		// Must happen asynchronously
		getQueue.add(new Pair<>(promise, getLocal(p, id)));
	}

	// TODO: what add do we need?

//	public void add(final NumberND p, final T t) {
//		if (!inLocal(p)) {
//			addToRemote(p, t);
//		} else {
//			if (localStorage instanceof ContinuousStorage)
//				localStorage.addToLocation(t, p);
//			else
//				localStorage.addToLocation(t, toLocalPoint((Int2D) p));
//		}
//	}



	///// REMOTE FIELD METHODS
	///// These methods are how 


	public void addLocal(final NumberND p, final T t) {
		if (localStorage instanceof ContinuousStorage)
			localStorage.addToLocation(t, p);
		else
		{
			//System.out.println(t+" added to point "+p);
			localStorage.addToLocation(t, toLocalPoint((Int2D) p));
		}
	}

	public T getLocal(NumberND p) {
		if (localStorage instanceof ContinuousStorage)
			return (T) localStorage.getObjects(p);
		else
			return (T) localStorage.getObjects(toLocalPoint((Int2D) p));
	}

	public T getLocal(NumberND p, long id) {
		if (localStorage instanceof ContinuousStorage)
			return (T) localStorage.getObjects(p, id);
		else
			return (T) localStorage.getObjects(toLocalPoint((Int2D) p), id);
	}

	public void removeLocal(NumberND p, long id) {
		if (localStorage instanceof ContinuousStorage) {
			int old_size = ((ContinuousStorage)localStorage).getCell((Double2D)p).size();
			localStorage.removeObject(p, id);
			if (old_size == ((ContinuousStorage)localStorage).getCell((Double2D)p).size())
			{
				System.out.println("remove not successful!");
				System.out.println(p+" "+id);
				System.exit(-1);
			}
		}
		else {

			localStorage.removeObject(toLocalPoint((Int2D) p), id);
		}
	}

	public void removeAllLocal(NumberND p) {
		if (localStorage instanceof ContinuousStorage) {
			int old_size = ((ContinuousStorage)localStorage).getCell((Double2D)p).size();

			localStorage.removeObjects(p);
			
			if (old_size == ((ContinuousStorage)localStorage).getCell((Double2D)p).size())
			{
				System.out.println("remove not sucessful!");
				System.out.println(p);
				System.exit(-1);
			}
		}
		
		else
			localStorage.removeObjects(toLocalPoint((Int2D) p));
	}

//	public void remove(final NumberND p, final T t) {
//		if (!inLocal(p))
//			removeFromRemote(p, t);
//		else {
//			if (localStorage instanceof ContinuousStorage)
//				localStorage.removeObject(t, p);
//			else
//				localStorage.removeObject(t, toLocalPoint((Int2D) p));
//		}
//	}

	/**
	 * @return DSimState for the current sim
	 */
	public DSimState getState() {
		return state;
	}

	public String toString() {
		return String.format("PID %d Storage %s", partition.getPID(), localStorage);
	}

}

class Pair<A, B> implements Serializable 
{
	private static final long serialVersionUID = 1L;

	public final A a;
	public final B b;

	public Pair(A a, B b) {
		this.a = a;
		this.b = b;
	}
}
		
		
	
