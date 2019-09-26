package sim.field;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import mpi.Datatype;
import mpi.MPI;
import mpi.MPIException;
import sim.engine.DSimState;
import sim.engine.IterativeRepeat;
import sim.engine.Steppable;
import sim.engine.Stopping;
import sim.field.storage.GridStorage;
import sim.util.GroupComm;
import sim.util.IntHyperRect;
import sim.util.IntPoint;
import sim.util.IntPointGenerator;
import sim.util.MPIParam;
import sim.util.MPIUtil;
import sim.util.NdPoint;

/**
 * All fields in distributed MASON must extend this class.
 *
 * @param <T> The Class of Object to store in the field
 * @param <P> The Type of NdPoint to use
 * @param <S> The Type of Storage to use
 */
public abstract class HaloField<T extends Serializable, P extends NdPoint, S extends GridStorage>
		implements DField<T, P>, RemoteField<T, P> {
	protected int numDimensions, numNeighbors;
	protected int[] aoi, fieldSize, haloSize;

	public IntHyperRect world, haloPart, origPart, privatePart;

	protected List<Neighbor> neighbors; // pointer to the processors who's partitions neighbor me
	protected S localStorage;
	protected DPartition partition;
	protected Datatype MPIBaseType;

	public final int fieldIndex;

	protected RemoteProxy<T, P> proxy;
	protected final DSimState state;

	public HaloField(final DPartition ps, final int[] aoi, final S stor, final DSimState state) {
		this.partition = ps;
		this.aoi = aoi;
		localStorage = stor;
		this.state = state;

		// init variables that don't change with the partition scheme
		numDimensions = ps.getNumDim();
		world = ps.getField();
		fieldSize = ps.getFieldSize();
		MPIBaseType = localStorage.getMPIBaseType();

		registerCallbacks();

		// init variables that may change with the partition scheme
		reload();
		fieldIndex = state.registerField(this);
	}

	protected void registerCallbacks() {
		/*
		 * if (ps instanceof DNonUniformPartition) { ps.registerPreCommit(arg -> { try {
		 * sync(); } catch (Exception e) { e.printStackTrace(); System.exit(-1); } });
		 *
		 * ps.registerPostCommit(arg -> { try { reload(); sync(); } catch (Exception e)
		 * { e.printStackTrace(); System.exit(-1); } }); }
		 */
		// else
		// if (ps instanceof DQuadTreePartition)
		{
			// Used for temporarily storing data when the underlying partition changes
			// The list is used to hold the refernece to the temporary GridStorage
			// because Java's lambda expression limits the variable to final.
			final List<GridStorage> tempStor = new ArrayList<GridStorage>();
			final DQuadTreePartition q = (DQuadTreePartition) partition;

			partition.registerPreCommit(arg -> {
				final int level = (int) arg;
				GridStorage s = null;

				if (q.isGroupMaster(level))
					s = localStorage.getNewStorage(q.getNodeShapeAtLevel(level));

				try {
					collectGroup(level, s);
				} catch (final Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}

				if (q.isGroupMaster(level))
					tempStor.add(s);
			});

			partition.registerPostCommit(arg -> {
				final int level = (int) arg;
				GridStorage s = null;

				reload();

				if (q.isGroupMaster(level))
					s = tempStor.remove(0);

				try {
					distributeGroup(level, s);
				} catch (final Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}
			});
		}
	}

	public void reload() {
		origPart = partition.getPartition();

		// Get the partition representing halo and local area by expanding the original
		// partition by aoi at each dimension
		haloPart = origPart.resize(aoi);
		haloSize = haloPart.getSize();

		localStorage.reshape(haloPart);

		// Get the partition representing private area by shrinking the original
		// partition by aoi at each dimension
		privatePart = origPart.resize(Arrays.stream(aoi).map(x -> -x).toArray());

		// Get the neighbors and create Neighbor objects
		neighbors = Arrays.stream(partition.getNeighborIds()).mapToObj(x -> new Neighbor(partition.getPartition(x)))
				.collect(Collectors.toList());
		numNeighbors = neighbors.size();
	}

	public void initRemote() {
		proxy = new RemoteProxy<>(partition, this);
	}

	public void add(final P p, final T t) {
		if (!inLocal(p))
			addToRemote(p, t);
		else
			addLocal(p, t);
	}

	public void remove(final P p, final T t) {
		if (!inLocal(p))
			removeFromRemote(p, t);
		else
			removeLocal(p, t);
	}

	public void remove(final P p) {
		if (!inLocal(p))
			removeFromRemote(p);
		else
			removeLocal(p);
	}

	public void move(final P fromP, final P toP, final T t) {
		final int fromPid = partition.toPartitionId(fromP);
		final int toPid = partition.toPartitionId(toP);

		if (fromPid == toPid && fromPid != partition.pid) {
			// So that we make only a single RMI call instead of two
			try {
				proxy.getField(partition.toPartitionId(fromP)).moveRMI(fromP, toP, t);
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
		if (!inLocal(fromP))
			throw new IllegalArgumentException("fromP must be local");

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

		if (inLocal(p)) {
			add(p, t);
			final IterativeRepeat iterativeRepeat = state.schedule.scheduleRepeating(time, ordering,
					(Stopping) t, interval);
		} else {
			final IterativeRepeat iterativeRepeat = new IterativeRepeat((Stopping) t, time, interval, ordering);
			state.getTransporter().migrateRepeatingAgent(iterativeRepeat, partition.toPartitionId(p));
		}
	}

	public void addRepeatingAgent(final P p, final T t, final int ordering, final double interval) {
		if (!(t instanceof Stopping))
			throw new IllegalArgumentException("t must be a Stopping");

		if (inLocal(p)) {
			add(p, t);
			final IterativeRepeat iterativeRepeat = state.schedule.scheduleRepeating((Stopping) t,
					ordering, interval);
		} else {
			// TODO: look at the time here
			final IterativeRepeat iterativeRepeat = new IterativeRepeat((Stopping) t, -1, interval, ordering);
			state.getTransporter().migrateRepeatingAgent(iterativeRepeat, partition.toPartitionId(p));
		}
	}

//	public void addRepeatingAgent(final P p, final IterativeRepeat iterativeRepeat) {
//		final T t = (T) iterativeRepeat.getStopping();
//
//		if (inLocal(p)) {
//			add(p, t);
//			state.registerIterativeRepeat(iterativeRepeat);
//		} else
//			state.transporter.migrateRepeatingAgent(iterativeRepeat, partition.toPartitionId(p));
//	}

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

	public void removeAndStopRepeatingAgent(final P p, final IterativeRepeat iterativeRepeat) {
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
			state.getTransporter().migrateRepeatingAgent(iterativeRepeat, partition.toPartitionId(toP), toP,
					fieldIndex);
			iterativeRepeat.stop();
		}
	}

	public void moveRepeatingAgent(final P fromP, final P toP, final IterativeRepeat iterativeRepeat) {
		if (!inLocal(fromP))
			throw new IllegalArgumentException("fromP must be local");

		// We cannot use checked cast for generics because of erasure
		// TODO: is there a safe way of doing this?

		final T t = (T) iterativeRepeat.getSteppable();

		remove(fromP, t);

		if (inLocal(toP))
			add(toP, t);
		else {
			state.getTransporter().migrateRepeatingAgent(iterativeRepeat, partition.toPartitionId(toP), toP,
					fieldIndex);
			iterativeRepeat.stop();
		}
	}

	public void moveLocal(final P fromP, final P toP, final T t) {
		removeLocal(fromP, t);
		addLocal(toP, t);
	}

	// TODO make a copy of the storage which will be used by the remote field access

	// TODO: Do we need to check for type safety here?
	// If the getField method returns the current field then
	// this cast should work

	protected Serializable getFromRemote(final P p) {
		try {
			return proxy.getField(partition.toPartitionId(p)).getRMI(p);
		} catch (final NullPointerException e) {
			throw new IllegalArgumentException("Remote Proxy is not initialized");
		} catch (final RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	protected void addToRemote(final P p, final T t) {
		try {
			proxy.getField(partition.toPartitionId(p)).addRMI(p, t);
		} catch (final NullPointerException e) {
			throw new IllegalArgumentException("Remote Proxy is not initialized");
		} catch (final RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	protected void removeFromRemote(final P p, final T t) {
		try {
			proxy.getField(partition.toPartitionId(p)).removeRMI(p, t);
		} catch (final NullPointerException e) {
			throw new IllegalArgumentException("Remote Proxy is not initialized");
		} catch (final RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	protected void removeFromRemote(final P p) {
		try {
			proxy.getField(partition.toPartitionId(p)).removeRMI(p);
		} catch (final NullPointerException e) {
			throw new IllegalArgumentException("Remote Proxy is not initialized");
		} catch (final RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	// TODO: Should we throw exception here if not in local?

	public void addRMI(final P p, final T t) throws RemoteException {
		addLocal(p, t);
	}

	public void removeRMI(final P p, final T t) throws RemoteException {
		removeLocal(p, t);
	}

	public void removeRMI(final P p) throws RemoteException {
		removeLocal(p);
	}

	public GridStorage getStorage() {
		return localStorage;
	}

	// Various stabbing queries
	public boolean inGlobal(final IntPoint p) {
		return IntStream.range(0, numDimensions).allMatch(i -> p.c[i] >= 0
				&& p.c[i] < fieldSize[i]);
	}

	public boolean inLocal(final P p) {
		return origPart.contains(p);
	}

	public boolean inPrivate(final P p) {
		return privatePart.contains(p);
	}

	public boolean inLocalAndHalo(final P p) {
		return haloPart.contains(p);
	}

	public boolean inShared(final P p) {
		return inLocal(p) && !inPrivate(p);
	}

	public boolean inHalo(final P p) {
		return inLocalAndHalo(p) && !inLocal(p);
	}

	public IntPoint toLocalPoint(final IntPoint p) {
		return p.rshift(haloPart.ul().getArray());
	}

	public IntPoint toToroidal(final IntPoint p) {
		return p.toToroidal(world);
	}

	public int toToroidal(final int x, final int dim) {
		final int s = fieldSize[dim];
		if (x >= s)
			return x - s;
		else if (x < 0)
			return x + s;
		return x;
	}

	public double toToroidal(final double x, final int dim) {
		final int s = fieldSize[dim];
		if (x >= s)
			return x - s;
		else if (x < 0)
			return x + s;
		return x;
	}

	public double toToroidalDiff(final double x1, final double x2, final int dim) {
		final int s = fieldSize[dim];
		if (Math.abs(x1 - x2) <= s / 2)
			return x1 - x2; // no wraparounds -- quick and dirty check

		final double dx = toToroidal(x1, dim) - toToroidal(x2, dim);
		if (dx * 2 > s)
			return dx - s;
		if (dx * 2 < -s)
			return dx + s;
		return dx;
	}

	public int stx(final int x) {
		return toToroidal(x, 0);
	}

	public int sty(final int y) {
		return toToroidal(y, 1);
	}

	public double stx(final double x) {
		return toToroidal(x, 0);
	}

	public double sty(final double y) {
		return toToroidal(y, 1);
	}

	public double tdx(final double x1, final double x2) {
		return toToroidalDiff(x1, x2, 0);
	}

	public double tdy(final double y1, final double y2) {
		return toToroidalDiff(y1, y2, 1);
	}

	public int getWidth() {
		return fieldSize[0];
	}

	public int getHeight() {
		return fieldSize[1];
	}

	public void syncHalo() throws MPIException {
		final Serializable[] sendObjs = new Serializable[numNeighbors];
		for (int i = 0; i < numNeighbors; i++)
			sendObjs[i] = localStorage.pack(neighbors.get(i).sendParam);

		final ArrayList<Serializable> recvObjs = MPIUtil.<Serializable>neighborAllToAll(partition, sendObjs);

		for (int i = 0; i < numNeighbors; i++)
			localStorage.unpack(neighbors.get(i).recvParam, recvObjs.get(i));
	}

	public void collect(final int dst, final GridStorage fullField) throws MPIException {
		final Serializable sendObj = localStorage.pack(new MPIParam(origPart, haloPart, MPIBaseType));

		final ArrayList<Serializable> recvObjs = MPIUtil.<Serializable>gather(partition, sendObj, dst);

		if (partition.getPid() == dst)
			for (int i = 0; i < partition.getNumProc(); i++)
				fullField.unpack(new MPIParam(partition.getPartition(i), world, MPIBaseType), recvObjs.get(i));
	}

	public void distribute(final int src, final GridStorage fullField) throws MPIException {
		final Serializable[] sendObjs = new Serializable[partition.getNumProc()];

		if (partition.getPid() == src)
			for (int i = 0; i < partition.getNumProc(); i++)
				sendObjs[i] = fullField.pack(new MPIParam(partition.getPartition(i), world, MPIBaseType));

		final Serializable recvObj = MPIUtil.<Serializable>scatter(partition, sendObjs, src);
		localStorage.unpack(new MPIParam(origPart, haloPart, MPIBaseType), recvObj);

		syncHalo();
	}

	public void collectGroup(final int level, final GridStorage groupField) throws MPIException {
		if (!(partition instanceof DQuadTreePartition))
			throw new UnsupportedOperationException(
					"Can only collect from group with DQuadTreePartition, got " + partition.getClass().getSimpleName());

		final DQuadTreePartition qt = (DQuadTreePartition) partition;
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

	public void distributeGroup(final int level, final GridStorage groupField) throws MPIException {
		if (!(partition instanceof DQuadTreePartition))
			throw new UnsupportedOperationException(
					"Can only distribute to group with DQuadTreePartition, got "
							+ partition.getClass().getSimpleName());

		final DQuadTreePartition qt = (DQuadTreePartition) partition;
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

	public String toString() {
		return String.format("PID %d Storage %s", partition.getPid(), localStorage);
	}

	// Helper class to organize neighbor-related data structures and methods
	class Neighbor {
		int pid;
		MPIParam sendParam, recvParam;

		public Neighbor(final IntHyperRect neighborPart) {
			pid = neighborPart.getId();
			final ArrayList<IntHyperRect> sendOverlaps = generateOverlaps(origPart, neighborPart.resize(aoi));
			final ArrayList<IntHyperRect> recvOverlaps = generateOverlaps(haloPart, neighborPart);

			assert sendOverlaps.size() == recvOverlaps.size();

			// Sort these overlaps so that they corresponds to each other
			Collections.sort(sendOverlaps);
			Collections.sort(recvOverlaps, Collections.reverseOrder());

			sendParam = new MPIParam(sendOverlaps, haloPart, MPIBaseType);
			recvParam = new MPIParam(recvOverlaps, haloPart, MPIBaseType);
		}

		private ArrayList<IntHyperRect> generateOverlaps(final IntHyperRect p1, final IntHyperRect p2) {
			final ArrayList<IntHyperRect> overlaps = new ArrayList<IntHyperRect>();

			if (partition.isToroidal())
				for (final IntPoint p : IntPointGenerator.getLayer(numDimensions, 1)) {
					final IntHyperRect sp = p2
							.shift(IntStream.range(0, numDimensions).map(i -> p.c[i] * fieldSize[i]).toArray());
					if (p1.isIntersect(sp))
						overlaps.add(p1.getIntersection(sp));
				}
			else
				overlaps.add(p1.getIntersection(p2));

			return overlaps;
		}
	}
}
