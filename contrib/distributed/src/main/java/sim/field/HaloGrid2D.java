package sim.field;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.function.Consumer;

import mpi.MPIException;
import sim.engine.DObject;
import sim.engine.DSimState;
import sim.engine.DistributedIterativeRepeat;
import sim.engine.DistributedTentativeStep;
import sim.engine.Promised;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.engine.Stopping;
import sim.engine.mpi.*;
import sim.engine.rmi.GridRMI;
import sim.engine.rmi.RemoteProcessor;
import sim.engine.rmi.RemotePromise;
import sim.field.partitioning.Partition;
import sim.field.storage.ContinuousStorage;
import sim.field.storage.GridStorage;
import sim.util.Double2D;
import sim.util.Int2D;
import sim.util.IntRect2D;
import sim.util.MPIParam;
import sim.util.MPIUtil;
import sim.util.Number2D;

/**
 * All fields in distributed MASON must contain this class. Stores
 * objects/agents and implements methods to add, move and remove them.
 *
 * @param <T> The Class of Object to store in the field
 * @param <S> The Type of Storage to use
 */
public class HaloGrid2D<T extends Serializable, S extends GridStorage<T>>
		extends UnicastRemoteObject implements GridRMI<T, Number2D>
		{
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
	// Storage object of the field that owns me
	S localStorage;
	// My field's index
	int fieldIndex;
	// My RMI Proxy
	GridRMICache<T, Number2D> proxy;

	// The following four queues are how RMI adds, removes, and fetches elements on
	// behalf of remote processors.

	// Queue of Promised results from getRMI
	ArrayList<Pair<Promised, Number2D>> getAllQueue = new ArrayList<>();
	ArrayList<Triplet<Promised, Number2D, Long>> getQueue = new ArrayList<>();

	// Queue of requests to add things to the grid via RMI
	ArrayList<Triplet<Number2D, T, double[]>> addQueue = new ArrayList<>();
	// Queue of requests to remove things from the grid via RMI
	ArrayList<Pair<Number2D, Long>> removeQueue = new ArrayList<>();
	// Queue of requests to remove all elements at certain locations from the grid
	// via RMI
	ArrayList<Number2D> removeAllQueue = new ArrayList<>();

	public HaloGrid2D(S storage, DSimState state) throws RemoteException
	{
		super();
		localStorage = storage;
		this.state = state;
		// init variables that don't change with the partition scheme
		Partition partition = state.getPartition();
		world = partition.getWorldBounds();
		worldWidth = partition.getWorldWidth();
		worldHeight = partition.getWorldHeight();


		// register callbacks
		partition.registerPreCommit(new Consumer()
		{
			public void accept(Object arg)
			{
			}
		});
		partition.registerPostCommit(new Consumer()
		{
			public void accept(Object t)
			{
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
	void reload()
	{
		
		Partition partition = getPartition();
		
		//delete all HaloGrid information to prevent leaking into real space!
		this.deleteHaloInfo();
		
		localBounds = partition.getLocalBounds();
		
		// Get the partition representing halo and local area by expanding the original
		// partition by aoi at each dimension
		haloBounds = localBounds.expand(partition.getAOI());
				
		localStorage.setShape(haloBounds);
		
		localStorage.setOffset(haloBounds.ul()); // moving local point calculation to GridStorage

		// Get the neighbors and create Neighbor objects
		neighbors = new ArrayList<Neighbor>();
		for (int id : partition.getNeighborPIDs())
		{
			neighbors.add(new Neighbor(partition.getLocalBounds(id)));

		}
		
	}

	// this deletes agents in the Halo before reloading, in order to prevent agents being moved to real space by bound changing
	void deleteHaloInfo() {
		
		if (this.haloBounds != null && this.localBounds != null) {
		
			//clear each point in halo
			for (Int2D a : this.haloBounds.getPointList()) {
			
				if (!this.localBounds.contains(a)) {
			
					if (localStorage.getAllObjects(a) != null) {
					
						localStorage.clear(a);
					
					
					}
				}
			
			}
		}
	}

	//// Simple requests

	/** Returns the HaloGrid2D's partition. */
	public Partition getPartition()
	{
		return state.getPartition();
	}

	/** Returns the HaloGrid2D's field index. */
	public int getFieldIndex()
	{
		return fieldIndex;
	}

	/**
	 * @return local storage (for this partition)
	 */
	public GridStorage<T> getStorage()
	{
		return localStorage;
	}

	///// Various point queries

	/**
	 * @param point
	 * @return true if point is within the global grid
	 */
	public boolean inWorld(Int2D point)
	{
		if (!(point.x >= 0 && point.x < worldWidth))
			return false;

		if (!(point.y >= 0 && point.y < worldHeight))
			return false;

		return true;
	}

	public IntRect2D getWorldBounds()
	{
		return getPartition().getWorldBounds();
	}

	public IntRect2D getLocalBounds()
	{
		return localBounds;
	}

	public IntRect2D getHaloBounds()
	{
		return haloBounds;
	}

	/**
	 * @param point
	 * @return true if point is local
	 */
	public boolean inLocal(Number2D point)
	{
		return localBounds.contains(point);
	}

	/**
	 * @param point
	 * @return true if point is local or in the halo
	 */
	public boolean inHalo(Number2D point)
	{
		return haloBounds.contains(point);
	}

	/**
	 * @param point
	 * @return a point wrapped around considering the toroidal halo region, if
	 *         possible. If out of the halo region, returns null.
	 */
	//@TODO does this work correctly?
	public Double2D toHaloToroidal(Double2D point)
	{
		if (inHalo(point))
			return point;
		double x = point.x;
		double y = point.y;
		double aoi = getPartition().getAOI();
		double height = worldHeight;
		double width = worldWidth;

		double lowx = (x - width);
		if (lowx >= 0 - aoi)
		{
			double lowy = (y - height);
			if (lowy >= 0 - aoi)
			{
				if (haloBounds.contains(lowx, lowy))
					return new Double2D(lowx, lowy);
				else
					return null;
			}
			double highy = (y + height);
			if (highy < height + aoi)
			{
				if (haloBounds.contains(lowx, highy))
					return new Double2D(lowx, highy);
				else
					return null;
			}
			else
			{
				if (haloBounds.contains(lowx, y))
					return new Double2D(lowx, y);
				else
					return null;
			}
		}

		double highx = (x + width);
		if (highx < width + aoi)
		{
			double lowy = (y - height);
			if (lowy >= 0 - aoi)
			{
				if (haloBounds.contains(highx, lowy))
					return new Double2D(highx, lowy);
				else
					return null;
			}
			double highy = (y + height);
			if (highy < height + aoi)
			{
				if (haloBounds.contains(highx, highy))
					return new Double2D(highx, highy);
				else
					return null;
			}
			else
			{
				if (haloBounds.contains(highx, y))
					return new Double2D(highx, y);
				else
					return null;
			}
		}

		double lowy = (y - height);
		if (lowy >= 0 - aoi)
		{
			if (haloBounds.contains(x, lowy))
				return new Double2D(x, lowy);
			else
				return null;
		}

		double highy = (y + height);
		if (highy < height + aoi)
		{
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
	public Int2D toHaloToroidal(Int2D point)
	{
		if (inHalo(point))
			return point; // easy
		int x = point.x;
		int y = point.y;
		int aoi = getPartition().getAOI();
		int height = worldHeight;
		int width = worldWidth;

		int lowx = (x - width);
		if (lowx >= 0 - aoi)
		{
			int lowy = (y - height);
			if (lowy >= 0 - aoi)
			{
				if (haloBounds.contains(lowx, lowy))
					return new Int2D(lowx, lowy);
				else
					return null;
			}
			int highy = (y + height);
			if (highy < height + aoi)
			{
				if (haloBounds.contains(lowx, highy))
					return new Int2D(lowx, highy);
				else
					return null;
			}
			else
			{
				if (haloBounds.contains(lowx, y))
					return new Int2D(lowx, y);
				else
					return null;
			}
		}

		int highx = (x + width);
		if (highx < width + aoi)
		{
			int lowy = (y - height);
			if (lowy >= 0 - aoi)
			{
				if (haloBounds.contains(highx, lowy))
					return new Int2D(highx, lowy);
				else
					return null;
			}
			int highy = (y + height);
			if (highy < height + aoi)
			{
				if (haloBounds.contains(highx, highy))
					return new Int2D(highx, highy);
				else
					return null;
			}
			else
			{
				if (haloBounds.contains(highx, y))
					return new Int2D(highx, y);
				else
					return null;
			}
		}

		int lowy = (y - height);
		if (lowy >= 0 - aoi)
		{
			if (haloBounds.contains(x, lowy))
				return new Int2D(x, lowy);
			else
				return null;
		}

		int highy = (y + height);
		if (highy < height + aoi)
		{
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
	public boolean inHaloToroidal(Number2D point)
	{
		double[] p = point.toArrayAsDouble();
		return inHaloToroidal(p[0], p[1]);
	}

	/**
	 * @param point
	 * @return true if point is local or in the halo considering toroidal
	 *         wrap-around
	 */
	public boolean inHaloToroidal(double x, double y)
	{
		if (haloBounds.contains(x, y))
			return true;

		double aoi = getPartition().getAOI();
		double height = worldHeight;
		double width = worldWidth;

		double lowx = (x - width);
		if (lowx >= 0 - aoi)
		{
			double lowy = (y - height);
			if (lowy >= 0 - aoi)
			{
				return (haloBounds.contains(lowx, lowy));
			}
			double highy = (y + height);
			if (highy < height + aoi)
			{
				return (haloBounds.contains(lowx, highy));
			}
			else
			{
				return (haloBounds.contains(lowx, y));
			}
		}

		double highx = (x + width);
		if (highx < width + aoi)
		{
			double lowy = (y - height);
			if (lowy >= 0 - aoi)
			{
				return (haloBounds.contains(highx, lowy));
			}
			double highy = (y + height);
			if (highy < height + aoi)
			{
				return (haloBounds.contains(highx, highy));
			}
			else
			{
				return (haloBounds.contains(highx, y));
			}
		}

		double lowy = (y - height);
		if (lowy >= 0 - aoi)
		{
			return (haloBounds.contains(x, lowy));
		}

		double highy = (y + height);
		if (highy < height + aoi)
		{
			return (haloBounds.contains(x, highy));
		}

		return false;
	}

	/**
	 * Adds and schedules an agent remotely. Called by various fields. Don't call
	 * this directly.
	 */
	public void addAgentToRemote(Number2D p, T t, int ordering, double time)
	{
		if (!(t instanceof Stopping))
			throw new IllegalArgumentException("t must be a Stopping");

		// If local, then MPI
		if (state.getTransporter().isNeighbor(getPartition().toPartitionPID(p)))
			{
			state.getTransporter().transport((DObject) t, getPartition().toPartitionPID(p), p, this.fieldIndex, ordering, time);
			}
		else // ...otherwise, RMI
			{
			System.out.println("using RMI");
			//System.exit(-1);
			try
			{
				// First, remove from schedule
				assert(t instanceof Stopping); // Assumes that t is not an agent if it's not Stopping
				unscheduleAgent((Stopping) t);
				proxy.getField(getPartition().toPartitionPID(p)).addRMI(p, t, ordering, time);
			}
			catch (RemoteException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Adds and schedules an agent remotely repeating. Called by various fields.
	 * Don't call this directly.
	 */
	public void addAgentToRemote(Number2D p, T t, int ordering, double time, double interval)
	{
		if (!(t instanceof Stopping))
			throw new IllegalArgumentException("t must be a Stopping");

		// If local, then MPI
		if (state.getTransporter().isNeighbor(getPartition().toPartitionPID(p)))
			{
			state.getTransporter().transport((DObject) t, getPartition().toPartitionPID(p), p, this.fieldIndex, ordering, time, interval);
			}
		else // ...otherwise, RMI
			{

			try
			{
				// First, remove from schedule
				assert(t instanceof Stopping); // Assumes that t is not an agent if it's not Stopping
				unscheduleAgent((Stopping) t);
				// Update using RMI
				proxy.getField(getPartition().toPartitionPID(p)).addRMI(p, t, ordering, time, interval);
			}
			catch (RemoteException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns a promise for a remote object at a certain location. Called by
	 * various fields. Don't call this directly.
	 */
	public Promised getFromRemote(Number2D p)
	{
		try
		{
			RemotePromise remotePromise = new RemotePromise();
			proxy.getField(getPartition().toPartitionPID(p)).getRMI(p, remotePromise);
			return remotePromise;
		}
		catch (NullPointerException e)
		{
			throw new IllegalArgumentException("Remote Proxy is not initialized");
		}
		catch (RemoteException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns a promise for a remote object by id. Called by various fields. Don't
	 * call this directly.
	 */
	public Promised getFromRemote(Number2D p, long id)
	{
		try
		{
			RemotePromise remotePromise = new RemotePromise();
			// Make promise remote, then, update promise remotely
			proxy.getField(getPartition().toPartitionPID(p)).getRMI(p, id, remotePromise);
			return remotePromise;
		}
		catch (NullPointerException e)
		{
			throw new IllegalArgumentException("Remote Proxy is not initialized");
		}
		catch (RemoteException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Adds an object (not an agent) to a remote location. Called by various fields.
	 * Don't call this directly.
	 */
	public void addToRemote(Number2D p, T t)
	{
		try
		{
			// If local, then MPI
			if (state.getTransporter().isNeighbor(getPartition().toPartitionPID(p)))
				{
				state.getTransporter().transport((Serializable) t, getPartition().toPartitionPID(p), p, this.fieldIndex);
				}
			
			else 
				{
			    proxy.getField(getPartition().toPartitionPID(p)).addRMI(p, t);
				}
		}
		catch (NullPointerException e)
		{
			throw new IllegalArgumentException("Remote Proxy is not initialized");
		}
		catch (RemoteException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Removes an object (or an agent) from a remote location. Called by various
	 * fields. Don't call this directly.
	 */
	public void removeFromRemote(Number2D p, long id)
	{
		try
		{
			proxy.getField(getPartition().toPartitionPID(p)).removeRMI(p, id);
		}
		catch (NullPointerException e)
		{
			throw new IllegalArgumentException("Remote Proxy is not initialized");
		}
		catch (RemoteException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Removes all objects (and agents) from a remote location. Called by various
	 * fields. Don't call this directly.
	 */
	public void removeAllFromRemote(Number2D p)
	{
		try
		{
			proxy.getField(getPartition().toPartitionPID(p)).removeAllRMI(p);
		}
		catch (NullPointerException e)
		{
			throw new IllegalArgumentException("Remote Proxy is not initialized");
		}
		catch (RemoteException e)
		{
			throw new RuntimeException(e);
		}
	}



	/**
	 * Initializes the GridRMICache for this halogrid. Called by DSimState. Don't call
	 * this directly.
	 *
	 */
	public void initRemote()
	{
		proxy = new GridRMICache<>(getPartition(), this);
	}

	/**
	 * Processes and clears the RMI queues for this halogrid. Called by DSimState.
	 * Don't call this directly.
	 * 
	 * @throws MPIException
	 * @throws RemoteException
	 */
	public void syncRemoveAndAdd() throws MPIException, RemoteException
	{
		//synchronized(removeRMILock) {

		for (Pair<Number2D, Long> pair : removeQueue)
		{
			// remove from schedule
			T t = getLocal(pair.a, pair.b);

			// Assumes that t is not an agent if it's not Stopping
			if (t instanceof Stopping)
				unscheduleAgent((Stopping) t);

			removeLocal(pair.a, pair.b);
		}
		removeQueue.clear();
		


		for (Number2D p : removeAllQueue)
		{
			ArrayList<T> list = getLocal(p);
			// Assumes that t is not an agent if it's not Stopping
			for (T t : list)
				if (t instanceof Stopping)
					unscheduleAgent((Stopping) t);
			removeAllLocal(p);
		}
		removeAllQueue.clear();
		//}

		//synchronized(addRMILock) {
			

		
		for (Triplet<Number2D, T, double[]> pair : addQueue)
		{
			addLocal(pair.a, pair.b);
			if (pair.b instanceof DObject) ((DObject)(pair.b)).migrated(state);
			
			// Reschedule
			if (pair.c != null)
			{
				// 	pair.c is scheduling information holding: ordering, time, [interval]
				if (pair.c.length == 3) // Repeating agent, if array contains interval information
				{
					state.schedule.scheduleRepeating(pair.c[1], (int) pair.c[0], (Steppable) pair.b, pair.c[2]);
//					System.out.println("repeating agent rescheduled!");
				}
				else
				{
					assert(pair.c.length == 2);
					state.schedule.scheduleOnce(pair.c[1], (int) pair.c[0], (Steppable) pair.b);
//					System.out.println("agent rescheduled!");
				}
			}
		}
		
		addQueue.clear();
		
		}
	//}

	// TODO: Should we generalize this method for all grids
	void unscheduleAgent(Stopping stopping)
	{
		Stoppable stop = stopping.getStoppable();
		if (stop == null)
		{
			// we're done
		}
		else if ((stop instanceof DistributedTentativeStep))
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
	public void syncHalo() throws MPIException, RemoteException
	{
		int numNeighbors = neighbors.size();
		Serializable[] sendObjs = new Serializable[numNeighbors];
		for (int i = 0; i < numNeighbors; i++)
			sendObjs[i] = localStorage.pack(neighbors.get(i).sendParam);

		ArrayList<Serializable> recvObjs = MPIUtil.<Serializable>neighborAllToAll(getPartition(), sendObjs);

		for (int i = 0; i < numNeighbors; i++)
			localStorage.unpack(neighbors.get(i).recvParam, recvObjs.get(i));

		for (Pair<Promised, Number2D> pair : getAllQueue)
			pair.a.fulfill(getLocal(pair.b));
		getAllQueue.clear();

		for (Triplet<Promised, Number2D, Long> trip : getQueue)
			trip.a.fulfill(getLocal(trip.b, trip.c));
		getQueue.clear();
	}
	

	/**
	 * Adds an incoming to the field. Has cases for the type of object we are
	 * adding. Called by DSimState. Don't call this directly.
	 *
	 */
	@SuppressWarnings("unchecked")
	public void addPayload(PayloadWrapper payloadWrapper)
	{
	addLocal((Number2D) payloadWrapper.loc, (T) payloadWrapper.payload);
	
	if (payloadWrapper.payload instanceof DObject) {
		((DObject)payloadWrapper.payload).migrated(state);
	}

	}
	
	
	
	
	
	
	
	
	
	
	/* RMI METHODS */
	/**
	 * This method queues an object t to be set at or added to point p at end of the
	 * time step, via addLocal(). This is called remotely via RMI, and is part of
	 * the GridRMI. Don't call this directly.
	 */
	
	Object addRMILock = new Object[0];
	Object removeRMILock = new Object[0];
	Object getRMILock = new Object[0];

	
	public void addRMI(Number2D p, T t) throws RemoteException
	{
		synchronized(addRMILock){
			addQueue.add(new Triplet<>(p, t, null));

		}
	}
	
	/**
	 * This method queues an agent t and scheduling information to be set at or added to point p at end of the
	 * time step, via addLocal(). This is called remotely via RMI, and is part of
	 * the GridRMI. Don't call this directly.
	 */
	public void addRMI(Number2D p, T t, int ordering, double time) throws RemoteException
	{
		synchronized(addRMILock){

		addQueue.add(new Triplet<>(p, t, new double[]{ordering, time}));
	}
	}
	
	public void addRMI(Number2D p, T t, int ordering, double time, double interval) throws RemoteException
	{
		synchronized(addRMILock){

		addQueue.add(new Triplet<>(p, t, new double[]{ordering, time, interval}));
	}
	}


	/**
	 * This method queues an object at a point p with the given id to be removed at
	 * the end of the time step via removeLocal(). For DObjectGrid2D the object is
	 * replaced with null. For DIntGrid2D and DDoubleGrid2D the object is replaced
	 * with 0. This is called remotely via RMI, and is part of the
	 * GridRMI. Don't call this directly.
	 */
	public void removeRMI(Number2D p, long id) throws RemoteException
	{
		synchronized(removeRMILock){

		removeQueue.add(new Pair<>(p, id));
	}
	}

	/**
	 * This method queues all objects at a point p to be removed at the end of the
	 * time step. This is only used by DDenseGrid2D. This is called remotely via
	 * RMI, and is part of the GridRMI. Don't call this directly.
	 */
	public void removeAllRMI(Number2D p) throws RemoteException
	{
		synchronized(removeRMILock){

		removeAllQueue.add(p);
		}
	}

	/**
	 * This method queues the get requests via RMI. From the Perspective of the
	 * requesting node this grid is remote, it will add the result to a queue and
	 * return all the objects in the queue after the current time step. This is
	 * called remotely via RMI, and is part of the GridRMI. Don't call
	 * this directly.
	 */
	public void getRMI(Number2D p, RemotePromise promise) throws RemoteException
	{
		// Make promise remote
		// update promise remotely
		// Must happen asynchronously
		// Do it in the queue
		// Add to a queue here
		
		synchronized(getRMILock){

		getAllQueue.add(new Pair<>(promise, p));
		}
	}

	/**
	 * This method entertains the get requests via RMI. From the Perspective of the
	 * requesting node this grid is remote, it will add the result to a queue and
	 * return all the objects in the queue after the current time step. This is
	 * called remotely via RMI, and is part of the GridRMI. Don't call
	 * this directly.
	 */
	public void getRMI(Number2D p, long id, RemotePromise promise) throws RemoteException
	{
		// Make promise remote
		// update promise remotely
		// Must happen asynchronously
		synchronized(getRMILock){

		
		getQueue.add(new Triplet<>(promise, p, id));
		}
	}

	//// LOCAL UPDATE METHODS CALLED BY THE RMI QUEUES

	/**
	 * Adds or sets an object in local storage at the given (global) point p.
	 * 
	 * @param p location
	 * @param t Object to add
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void addLocal(Number2D p, T t)
	{
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
	ArrayList<T> getLocal(Number2D p)
	{
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
	T getLocal(Number2D p, long id)
	{
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
	void removeLocal(Number2D p, long id)
	{
		if (localStorage instanceof ContinuousStorage)
			((ContinuousStorage<?>) localStorage).removeObject(p, id);
		else
			localStorage.removeObject(p, id);
	}

	/** Clears all objects from the local storage at the given point. */
	void removeAllLocal(Number2D p)
	{
		if (localStorage instanceof ContinuousStorage)
			((ContinuousStorage<?>) localStorage).clear(p);
		else
			localStorage.clear(p);
	}

	public void removeAgent(Number2D p, long id)
	{
		if (inLocal(p))
		{
			T t = getLocal(p, id);

			if (!(t instanceof Stopping))
				throw new IllegalArgumentException("t must be a Stopping");

			removeLocal(p, id);
			((Stopping) t).getStoppable().stop();
		}
		else
		{
			removeFromRemote(p, id);
		}
	}

	public String toString()
	{
		return String.format("PID %d Storage %s", getPartition().getPID(), localStorage);
	}

	/**
	 * Helper class to organize neighbor-related data structures and methods
	 */
	class Neighbor
	{
		MPIParam sendParam;
		MPIParam recvParam;

		 Neighbor(IntRect2D neighborPart)
		{
			ArrayList<IntRect2D> sendOverlaps = generateOverlaps(localBounds,
					neighborPart.expand(getPartition().getAOI()));

			
			ArrayList<IntRect2D> recvOverlaps = generateOverlaps(haloBounds, neighborPart);



			
			assert sendOverlaps.size() == recvOverlaps.size();

			sendParam = new MPIParam(sendOverlaps, haloBounds, localStorage.getMPIBaseType());
			recvParam = new MPIParam(recvOverlaps, haloBounds, localStorage.getMPIBaseType());
			
			

			
		}

		 ArrayList<IntRect2D> generateOverlaps(IntRect2D p1, IntRect2D p2)
		{
			ArrayList<IntRect2D> overlaps = new ArrayList<IntRect2D>();

			if (getPartition().isToroidal())
			{
				int xLen = worldWidth;
				int yLen = worldHeight;

				Int2D[] shifts =
					{
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

				for (Int2D p : shifts)
				{
					IntRect2D sp = p2.add(p);		// new int[] { p.x, p.y });
					if (p1.intersects(sp))
						overlaps.add(p1.getIntersection(sp));
				}
			}
			else
				overlaps.add(p1.getIntersection(p2));

			return overlaps;
		}
	}
}


// An GridRMICache holds a cache of GridRMI objects (RMI pointers to remote HaloGrids) for a given HaloGrid2D
@SuppressWarnings("rawtypes")
class GridRMICache<T extends Serializable, P extends Number2D>
{
	private static final long serialVersionUID = 1L;

	GridRMI[] cache;
	int fieldId;

	public GridRMICache(Partition ps, HaloGrid2D haloGrid)
	{
		this.fieldId = haloGrid.getFieldIndex();
		this.cache = new GridRMI[ps.getNumProcessors()];
	}

	@SuppressWarnings("unchecked")
	public GridRMI<T, P> getField(int pid) throws RemoteException
	{
		GridRMI<T, P> grid = cache[pid];
		if (grid == null)
		{
			grid = RemoteProcessor.getProcessor(pid).getGrid(fieldId);
			cache[pid] = grid;
		}
		return grid;
	}
}

class Pair<A, B> implements Serializable
{
	private static final long serialVersionUID = 1L;

	 A a;
	 B b;

	 Pair(A a, B b)
	{
		this.a = a;
		this.b = b;
	}
}

class Triplet<A, B, C> implements Serializable
{
	private static final long serialVersionUID = 1L;

	 A a;
	 B b;
	 C c;

	 Triplet(A a, B b, C c)
	{
		this.a = a;
		this.b = b;
		this.c = c;
	}
}
