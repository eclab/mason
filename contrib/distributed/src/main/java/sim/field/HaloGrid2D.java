package sim.field;

import java.io.Serializable;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import mpi.Datatype;
import mpi.MPI;
import mpi.MPIException;
import sim.engine.DSimState;
import sim.engine.DistributedIterativeRepeat;
import sim.engine.IterativeRepeat;
import sim.engine.Steppable;
import sim.engine.Stopping;
import sim.engine.transport.AgentWrapper;
import sim.engine.transport.PayloadWrapper;
import sim.engine.transport.TransportRMIInterface;
import sim.field.partitioning.IntRect2D;
import sim.field.partitioning.PartitionInterface;
import sim.field.partitioning.QuadTreePartition;
import sim.field.storage.ContinuousStorage;
import sim.field.storage.GridStorage;
import sim.util.GroupComm;
import sim.util.Int2D;
import sim.util.MPIParam;
import sim.util.MPIUtil;
import sim.util.NumberND;

/**
 * All fields in distributed MASON must contain this class. Stores
 * objects/agents and implements methods to add, move and remove them.
 *
 * @param <T> The Class of Object to store in the field
 * @param <P> The Type of P to use
 * @param <S> The Type of Storage to use
 */
public class HaloGrid2D<T extends Serializable, P extends NumberND, S extends GridStorage>
		extends UnicastRemoteObject
		implements TransportRMIInterface<T, P>, Synchronizable, DGrid<T, P> {
	private static final long serialVersionUID = 1L;

	/**
	 * Helper class to organize neighbor-related data structures and methods
	 */
	class Neighbor {
		//final int pid;
		MPIParam sendParam, recvParam;

		public Neighbor(final IntRect2D neighborPart) {
			//pid = neighborPart.getId();
			final ArrayList<IntRect2D> sendOverlaps = generateOverlaps(origPart, neighborPart.resize(aoi));
			final ArrayList<IntRect2D> recvOverlaps = generateOverlaps(haloPart, neighborPart);

			assert sendOverlaps.size() == recvOverlaps.size();

			// Sort these overlaps so that they corresponds to each other
			//Collections.sort(sendOverlaps);
			//Collections.sort(recvOverlaps, Collections.reverseOrder());

			sendParam = new MPIParam(sendOverlaps, haloPart, MPIBaseType);
			recvParam = new MPIParam(recvOverlaps, haloPart, MPIBaseType);
		}

		private ArrayList<IntRect2D> generateOverlaps(final IntRect2D p1, final IntRect2D p2) {
			final ArrayList<IntRect2D> overlaps = new ArrayList<IntRect2D>();

			if (partition.isToroidal()) {
				int xLen = fieldSize[0];
				int yLen = fieldSize[1];

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
//						new Int2D(-2 * xLen, -2 * yLen) // This is probably a bug
				};

				for (final Int2D p : shifts) {
					final IntRect2D sp = p2.shift(new int[]{p.x,p.y});
					if (p1.intersects(sp))
						overlaps.add(p1.getIntersection(sp));
				}
			} else
				overlaps.add(p1.getIntersection(p2));

			return overlaps;
		}
	}

	protected int numNeighbors;
	protected int[] aoi, fieldSize, haloSize;

	public IntRect2D world, haloPart, origPart, privatePart;

	protected List<Neighbor> neighbors; // pointer to the processors who's partitions neighbor me
	public final S localStorage;
	public PartitionInterface partition;
	transient public Datatype MPIBaseType;

	public final int fieldIndex;

	public RMIProxy<T, P> proxy;
	private final DSimState state;

	private final Object lockRMI = new boolean[1];

	public HaloGrid2D(final PartitionInterface ps, final int[] aoi, final S stor, final DSimState state)
			throws RemoteException {
		super();
		this.partition = ps;
		this.aoi = aoi;
		localStorage = stor;
		this.state = state;
		// init variables that don't change with the partition scheme
		world = ps.getWorldBounds();
		fieldSize = ps.getFieldSize();
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
			public void accept(Object arg) {
//			final int level = (int) arg;
//			GridStorage s = null;
//
//			if (q.isGroupMaster(level))
//				s = localStorage.getNewStorage(q.getNodeShapeAtLevel(level));
//
//			try { collectGroup(level, s); } catch (final Exception e) {
//				e.printStackTrace();
//				System.exit(-1);
//			}
//			if (q.isGroupMaster(level)) tempStor.add(s);
			}
		});
		partition.registerPostCommit(new Consumer() {
			public void accept(Object t) {
//			final int level = (int) arg;
//			GridStorage s = null;

				reload();

//			if (q.isGroupMaster(level))
//				s = tempStor.remove(0);
//
//			try {
//				distributeGroup(level, s);
//			} catch (final Exception e) {
//				e.printStackTrace();
//				System.exit(-1);
//			}
			}
		});

	}

	/**
	 * Resizes partition and halo. Also sets up the neighbors
	 */
	public void reload() {
		origPart = partition.getBounds();
		// Get the partition representing halo and local area by expanding the original
		// partition by aoi at each dimension
		haloPart = origPart.resize(aoi);
		haloSize = haloPart.getSizes();
		localStorage.reshape(haloPart);
		// Get the partition representing private area by shrinking the original
		// partition by aoi at each dimension

		int[] negAoi = new int[aoi.length];
		for (int i = 0; i < aoi.length; i++) {
			negAoi[i] = -aoi[i];
		}
		privatePart = origPart.resize(negAoi);
		// privatePart = origPart.resize(Arrays.stream(aoi).map(x -> -x).toArray()());
		// Get the neighbors and create Neighbor objects
		neighbors = new ArrayList<Neighbor>();
		for (int id : partition.getNeighborIds()) {
			neighbors.add(new Neighbor(partition.getBounds(id)));
		}
		numNeighbors = neighbors.size();
	}

	/**
	 * Shifts point p to give location on the local partition
	 * 
	 * @param p
	 * @return location on the local partition
	 */
	public Int2D toLocalPoint(final Int2D p) {
		return p.rshift(haloPart.ul().toArray());
	}

	/**
	 * Wraps point around to give location on the local partition
	 * 
	 * @param p
	 * @return location in a toroidal plane
	 */
	public Int2D toToroidal(final Int2D p) {
		// return p.toToroidal(world);
		return world.toToroidal(p);
	}

	public void add(final P p, final T t) {
		if (!inLocal(p)) {
			addToRemote(p, t);
		} else {
			if (localStorage instanceof ContinuousStorage)
				localStorage.addToLocation(t, p);
			else
				localStorage.addToLocation(t, toLocalPoint((Int2D) p));
		}
	}

	public void remove(final P p, final T t) {
		if (!inLocal(p))
			removeFromRemote(p, t);
		else {
			if (localStorage instanceof ContinuousStorage)
				localStorage.removeObject(t, p);
			else
				localStorage.removeObject(t, toLocalPoint((Int2D) p));
		}
	}

	public void remove(final P p) {
		if (!inLocal(p))
			removeFromRemote(p);
		else {
			if (localStorage instanceof ContinuousStorage)
				localStorage.removeObjects(p);
			else
				localStorage.removeObjects(toLocalPoint((Int2D) p));
		}
	}

	public void move(final P fromP, final P toP, final T t) {
		final int fromPid = partition.toPartitionId(fromP);
		final int toPid = partition.toPartitionId(toP);

		if (fromPid == toPid && fromPid != partition.pid) {
			// So that we make only a single RMI call instead of two
			try {
				proxy.getField(fromPid).moveRMI(fromP, toP, t);

			} catch (final RemoteException e) {
				throw new RuntimeException(e);
			}
		} else {
			remove(fromP, t);
			add(toP, t);
		}
	}

	public void addAgent(final P p, final T t) {
		// TODO: is there a better way than just doing a Type Cast?
		if (!(t instanceof Stopping))
			throw new IllegalArgumentException("t must be a Stopping");

		final Stopping agent = (Stopping) t;

		if (inLocal(p)) {
			add(p, t);
			state.schedule.scheduleOnce(agent);
		} else
			state.getTransporter().migrateAgent(agent, partition.toPartitionId(p));
	}

	public void addAgent(final P p, final T t, final int ordering, final double time) {
		if (!(t instanceof Stopping))
			throw new IllegalArgumentException("t must be a Stopping");

		final Stopping agent = (Stopping) t;

		if (inLocal(p)) {
			add(p, t);
			state.schedule.scheduleOnce(time, ordering, agent);
		} else
			state.getTransporter().migrateAgent(ordering, time, agent, partition.toPartitionId(p));
	}

	public void moveAgent(final P fromP, final P toP, final T t) {

		if (!inLocal(fromP)) {
			System.out.println("pid " + partition.pid + " agent" + t);
			System.out.println("partitioning " + partition.getBounds());
			throw new IllegalArgumentException("fromP must be local");
		}

		if (!(t instanceof Stopping))
			throw new IllegalArgumentException("t must be a Stopping");

		final Stopping agent = (Stopping) t;

		remove(fromP, t);

		if (inLocal(toP)) {
			add(toP, t);
			state.schedule.scheduleOnce(agent);
		} else
			state.getTransporter().migrateAgent(agent,
					partition.toPartitionId(toP), toP, fieldIndex);
	}

	public void moveAgent(final P fromP, final P toP, final T t, final int ordering, final double time) {
		// TODO: Call the top method here
		if (!inLocal(fromP))
			throw new IllegalArgumentException("fromP must be local");

		if (!(t instanceof Stopping))
			throw new IllegalArgumentException("t must be a Stopping");

		final Stopping agent = (Stopping) t;

		remove(fromP, t);

		if (inLocal(toP)) {
			add(toP, t);
			state.schedule.scheduleOnce(time, ordering, agent);
		} else
			state.getTransporter().migrateAgent(ordering, time, agent,
					partition.toPartitionId(toP), toP, fieldIndex);
	}

	public void addRepeatingAgent(final P p, final T t, final double time, final int ordering, final double interval) {
		if (!(t instanceof Stopping))
			throw new IllegalArgumentException("t must be a Stopping");
		Stopping stopping = (Stopping) t;

		if (inLocal(p)) {
			add(p, t);
			final IterativeRepeat iterativeRepeat = state.schedule.scheduleRepeating(time, ordering, stopping,
					interval);
			stopping.setStoppable(iterativeRepeat);
		} else {
			final DistributedIterativeRepeat iterativeRepeat = new DistributedIterativeRepeat(stopping, time, interval,
					ordering);
			state.getTransporter().migrateRepeatingAgent(iterativeRepeat, partition.toPartitionId(p));
		}
	}

	public void addRepeatingAgent(final P p, final T t, final int ordering, final double interval) {
		if (!(t instanceof Stopping))
			throw new IllegalArgumentException("t must be a Stopping");
		Stopping stopping = (Stopping) t;

		if (inLocal(p)) {
			add(p, t);
			final IterativeRepeat iterativeRepeat = state.schedule.scheduleRepeating(stopping, ordering, interval);
			stopping.setStoppable(iterativeRepeat);
		} else {
			// TODO: look at the time here
			final DistributedIterativeRepeat iterativeRepeat = new DistributedIterativeRepeat(stopping, -1, interval,
					ordering);
			state.getTransporter().migrateRepeatingAgent(iterativeRepeat, partition.toPartitionId(p));
		}
	}

	public void removeAndStopRepeatingAgent(final P p, final T t) {
		if (!inLocal(p))
			throw new IllegalArgumentException("p must be local");

		// TODO: Should we remove the instanceOf check and assume that the
		// pre-conditions are always met?
		if (!(t instanceof Stopping))
			throw new IllegalArgumentException("t must be a Stopping");

		final Stopping stopping = (Stopping) t;
		remove(p, t);
		stopping.getStoppable().stop();
	}

	public void removeAndStopRepeatingAgent(final P p, final DistributedIterativeRepeat iterativeRepeat) {
		if (!inLocal(p))
			throw new IllegalArgumentException("p must be local");

		final Steppable step = iterativeRepeat.getSteppable();

		if (!(step instanceof Stopping))
			throw new IllegalArgumentException("t must be a Stopping");

		final Stopping stopping = (Stopping) step;
		remove(p, (T) step);
		stopping.getStoppable().stop();
	}

	public void moveRepeatingAgent(final P fromP, final P toP, final T t) {
		if (!inLocal(fromP))
			throw new IllegalArgumentException("fromP must be local");

		remove(fromP, t);

		// TODO: Should we remove the instanceOf check and assume that the
		// pre-conditions are always met?
		if (inLocal(toP))
			add(toP, t);
		else if (!(t instanceof Stopping))
			throw new IllegalArgumentException("t must be a Stopping");
		else {
			final Stopping stopping = (Stopping) t;
			final IterativeRepeat iterativeRepeat = (IterativeRepeat) stopping.getStoppable();

			final DistributedIterativeRepeat distributedIterativeRepeat = new DistributedIterativeRepeat(stopping,
					iterativeRepeat.getTime(), iterativeRepeat.getInterval(), iterativeRepeat.getOrdering());
			state.getTransporter().migrateRepeatingAgent(distributedIterativeRepeat, partition.toPartitionId(toP), toP,
					fieldIndex);

			iterativeRepeat.stop();
		}
	}

//	public void moveRepeatingAgent(final P fromP, final P toP, final DistributedIterativeRepeat iterativeRepeat) {
//		if (!inLocal(fromP))
//			throw new IllegalArgumentException("fromP must be local");
//
//		// We cannot use checked cast for generics because of erasure
//		// TODO: is there a safe way of doing this?
//
//		final T t = (T) iterativeRepeat.getSteppable();
//
//		remove(fromP, t);
//
//		if (inLocal(toP))
//			add(toP, t);
//		else {
//			state.getTransporter().migrateRepeatingAgent(iterativeRepeat, partition.toPartitionId(toP), toP,
//					fieldIndex);
//			iterativeRepeat.stop();
//		}
//	}

	/**
	 * Move for when both the from and to are local
	 * 
	 * @param fromP
	 * @param toP
	 * @param t     object to move
	 */
	public void moveLocal(final P fromP, final P toP, final T t) {
		if (localStorage instanceof ContinuousStorage) {
			localStorage.removeObject(t, fromP);
			localStorage.addToLocation(t, toP);
		} else {
			localStorage.removeObject(t, toLocalPoint((Int2D) fromP));
			localStorage.addToLocation(t, toLocalPoint((Int2D) toP));
		}
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
	public Serializable getFromRemote(final P p) {
		try {
			return proxy.getField(partition.toPartitionId(p)).getRMI(p);
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
	public void addToRemote(final P p, final T t) {
		try {
			proxy.getField(partition.toPartitionId(p)).addRMI(p, t);
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
	public void removeFromRemote(final P p, final T t) {
		try {
			proxy.getField(partition.toPartitionId(p)).removeRMI(p, t);
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
	public void removeFromRemote(final P p) {
		try {
			proxy.getField(partition.toPartitionId(p)).removeRMI(p);
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

	// Various stabbing queries
	/**
	 * @param point
	 * @return true if point is within the global grid
	 */
	public boolean inGlobal(final Int2D point) {
		if (!(point.x >= 0 && point.x < fieldSize[0])) {
			return false;
		}
		if (!(point.y >= 0 && point.y < fieldSize[1])) {
			return false;
		}
		return true;
	}

	/**
	 * @param point
	 * @return true if point is local
	 */
	public boolean inLocal(final P point) {
		return origPart.contains(point);
	}

	/**
	 * @param point
	 * @return true if point is local and not in the halo
	 */
	public boolean inPrivate(final P point) {
		return privatePart.contains(point);
	}

	/**
	 * @param point
	 * @return true if point is local or in the halo
	 */
	public boolean inLocalAndHalo(final P point) {
		return haloPart.contains(point);
	}

	/**
	 * @param point
	 * @return true if point p is local and in the halo
	 */
	public boolean inShared(final P point) {
		return inLocal(point) && !inPrivate(point);
	}

	/**
	 * @param point
	 * @return true if point p is in the halo (and not local)
	 */
	public boolean inHalo(final P point) {
		return inLocalAndHalo(point) && !inLocal(point);
	}

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
		final Serializable sendObj = localStorage.pack(new MPIParam(origPart, haloPart, MPIBaseType));

		final ArrayList<Serializable> recvObjs = MPIUtil.<Serializable>gather(partition, sendObj, dst);

		if (partition.getPid() == dst)
			for (int i = 0; i < partition.getNumProc(); i++)
				fullField.unpack(new MPIParam(partition.getBounds(i), world, MPIBaseType), recvObjs.get(i));
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
			final Serializable sendObj = localStorage.pack(new MPIParam(origPart, haloPart, MPIBaseType));

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
	 */
	public void distributeGroup(final int level, final GridStorage groupField) throws MPIException {
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

			localStorage.unpack(new MPIParam(origPart, haloPart, MPIBaseType), recvObj);
		}
		syncHalo();
	}

	/* METHODS SYNCHO */

	public void initRemote() {
		proxy = new RMIProxy<>(partition, this);
	}

	public void syncHalo() throws MPIException {
		final Serializable[] sendObjs = new Serializable[numNeighbors];
		for (int i = 0; i < numNeighbors; i++)
			sendObjs[i] = localStorage.pack(neighbors.get(i).sendParam);

		final ArrayList<Serializable> recvObjs = MPIUtil.<Serializable>neighborAllToAll(partition, sendObjs);

		for (int i = 0; i < numNeighbors; i++)
			localStorage.unpack(neighbors.get(i).recvParam, recvObjs.get(i));
	}

	public void syncObject(PayloadWrapper payloadWrapper) {
		if (payloadWrapper.payload instanceof DistributedIterativeRepeat) {
			final DistributedIterativeRepeat iterativeRepeat = (DistributedIterativeRepeat) payloadWrapper.payload;
			add((P) payloadWrapper.loc, (T) iterativeRepeat.getSteppable());

		} else if (payloadWrapper.payload instanceof AgentWrapper) {
			final AgentWrapper agentWrapper = (AgentWrapper) payloadWrapper.payload;
			add((P) payloadWrapper.loc, (T) agentWrapper.agent);

		} else {
			add((P) payloadWrapper.loc, (T) payloadWrapper.payload);
		}
	}

	/* RMI METHODS */
	// TODO: Should we throw exception here if not in local?
	public void addRMI(P p, T t) throws RemoteException {
		synchronized (lockRMI) {
//		Checking instanceof because ContStorage store global point 
			if (localStorage instanceof ContinuousStorage)
				localStorage.addToLocation(t, p);
			else
				localStorage.addToLocation(t, toLocalPoint((Int2D) p));
		}
	}

	public void removeRMI(P p, T t) throws RemoteException {
		synchronized (lockRMI) {
			if (localStorage instanceof ContinuousStorage)
				localStorage.removeObject(t, p);
			else
				localStorage.removeObject(t, toLocalPoint((Int2D) p));
		}
	}

	public void removeRMI(final P p) throws RemoteException {
		synchronized (lockRMI) {
			if (localStorage instanceof ContinuousStorage)
				localStorage.removeObjects(p);
			else
				localStorage.removeObjects(toLocalPoint((Int2D) p));
		}
	}

	public Serializable getRMI(P p) throws RemoteException {
		synchronized (lockRMI) {
			if (localStorage instanceof ContinuousStorage)
				return localStorage.getObjects(p);
			else
				return localStorage.getObjects(toLocalPoint((Int2D) p));
		}
	}

	public void moveRMI(final P fromP, final P toP, final T t) throws RemoteException {
		synchronized (lockRMI) {
			removeRMI(fromP, t);
			addRMI(toP, t);
		}
	}

	/**
	 * @return DSimState for the current sim
	 */
	public DSimState getState() {
		return state;
	}

	public String toString() {
		return String.format("PID %d Storage %s", partition.getPid(), localStorage);
	}

}
