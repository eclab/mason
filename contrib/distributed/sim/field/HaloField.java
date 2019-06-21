package sim.field;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import mpi.Comm;
import mpi.Datatype;
import mpi.MPI;
import mpi.MPIException;
import sim.engine.DSimState;
import sim.field.storage.GridStorage;
import sim.util.GroupComm;
import sim.util.IntHyperRect;
import sim.util.IntPoint;
import sim.util.IntPointGenerator;
import sim.util.MPIParam;
import sim.util.MPIUtil;
import sim.util.NdPoint;

// TODO refactor HaloField to accept
// continuous: double, int, object

public abstract class HaloField<T extends Serializable> implements RemoteField {

	protected int numDimensions, numNeighbors;
	protected int[] aoi, fieldSize, haloSize;

	public IntHyperRect world, haloPart, origPart, privPart;
	// TODO: Fix the comment -
	// pointer to the processors who's partitions neighbor me
	protected List<Neighbor> neighbors;
	// TODO: Fix the comment -
	// Local storage
	protected GridStorage field;
	// TODO: Fix the comment -
	// Partition data structure
	protected DPartition ps;
	protected Comm comm;
	protected Datatype MPIBaseType;
	public final int fieldIndex;

	protected RemoteProxy proxy;

	public HaloField(final DPartition ps, final int[] aoi, final GridStorage stor, final DSimState state) {
		this.ps = ps;
		this.aoi = aoi;
		field = stor;

		// init variables that don't change with the partition scheme
		numDimensions = ps.getNumDim();
		world = ps.getField();
		fieldSize = ps.getFieldSize();
		MPIBaseType = field.getMPIBaseType();

		registerCallbacks();

		// init variables that may change with the partition scheme
		reload();
		fieldIndex = state.register(this);
	}

	public abstract boolean add(final IntPoint p, final T t);

//	public abstract boolean remove(final IntPoint p, final T t);
//
//	public abstract boolean move(final IntPoint fromP, final IntPoint toP, final T t);

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
			final DQuadTreePartition q = (DQuadTreePartition) ps;

			ps.registerPreCommit(arg -> {
				final int level = (int) arg;
				GridStorage s = null;

				if (q.isGroupMaster(level))
					s = field.getNewStorage(q.getNodeShapeAtLevel(level));

				try {
					collectGroup(level, s);
				} catch (final Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}

				if (q.isGroupMaster(level))
					tempStor.add(s);
			});

			ps.registerPostCommit(arg -> {
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
		comm = ps.getCommunicator();
		origPart = ps.getPartition();

		// Get the partition representing halo and local area by expanding the original
		// partition by aoi at each dimension
		haloPart = origPart.resize(aoi);
		haloSize = haloPart.getSize();

		field.reshape(haloPart);

		// Get the partition representing private area by shrinking the original
		// partition by aoi at each dimension
		privPart = origPart.resize(Arrays.stream(aoi).map(x -> -x).toArray());

		// Get the neighbors and create Neighbor objects
		neighbors = Arrays.stream(ps.getNeighborIds()).mapToObj(x -> new Neighbor(ps.getPartition(x)))
				.collect(Collectors.toList());
		numNeighbors = neighbors.size();
	}

	public void initRemote() {
		proxy = new RemoteProxy(ps, this);
	}

	// TODO make a copy of the storage which will be used by the remote field access
	protected Serializable getFromRemote(final IntPoint p) {
		Serializable ret = null;
		final int pid = ps.toPartitionId(p);

		try {
			ret = proxy.getField(pid).getRMI(p);
		} catch (final RemoteException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (final NullPointerException e) {
			throw new IllegalArgumentException("Remote Proxy is not initialized");
		}

		return ret;
	}

	public GridStorage getStorage() {
		return field;
	}

	// Various stabbing queries
	public boolean inGlobal(final IntPoint p) {
		return IntStream.range(0, numDimensions).allMatch(i -> p.c[i] >= 0 && p.c[i] < fieldSize[i]);
	}

	public boolean inLocal(final IntPoint p) {
		return origPart.contains(p);
	}

	public boolean inPrivate(final IntPoint p) {
		return privPart.contains(p);
	}

	public boolean inLocalAndHalo(final IntPoint p) {
		return haloPart.contains(p);
	}

	public boolean inShared(final IntPoint p) {
		return inLocal(p) && !inPrivate(p);
	}

	public boolean inHalo(final IntPoint p) {
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

	public void sync() throws MPIException {
		final Serializable[] sendObjs = new Serializable[numNeighbors];
		for (int i = 0; i < numNeighbors; i++)
			sendObjs[i] = field.pack(neighbors.get(i).sendParam);

		final ArrayList<Serializable> recvObjs = MPIUtil.<Serializable>neighborAllToAll(ps, sendObjs);

		for (int i = 0; i < numNeighbors; i++)
			field.unpack(neighbors.get(i).recvParam, recvObjs.get(i));
	}

	public void collect(final int dst, final GridStorage fullField) throws MPIException {
		final Serializable sendObj = field.pack(new MPIParam(origPart, haloPart, MPIBaseType));

		final ArrayList<Serializable> recvObjs = MPIUtil.<Serializable>gather(ps, sendObj, dst);

		if (ps.getPid() == dst)
			for (int i = 0; i < ps.getNumProc(); i++)
				fullField.unpack(new MPIParam(ps.getPartition(i), world, MPIBaseType), recvObjs.get(i));
	}

	public void distribute(final int src, final GridStorage fullField) throws MPIException {
		final Serializable[] sendObjs = new Serializable[ps.getNumProc()];

		if (ps.getPid() == src)
			for (int i = 0; i < ps.getNumProc(); i++)
				sendObjs[i] = fullField.pack(new MPIParam(ps.getPartition(i), world, MPIBaseType));

		final Serializable recvObj = MPIUtil.<Serializable>scatter(ps, sendObjs, src);
		field.unpack(new MPIParam(origPart, haloPart, MPIBaseType), recvObj);

		// Sync the halo
		sync();
	}

	public void collectGroup(final int level, final GridStorage groupField) throws MPIException {
		if (!(ps instanceof DQuadTreePartition))
			throw new UnsupportedOperationException(
					"Can only collect from group with DQuadTreePartition, got " + ps.getClass().getSimpleName());

		final DQuadTreePartition qt = (DQuadTreePartition) ps;
		final GroupComm gc = qt.getGroupComm(level);

		if (gc != null) {
			final Serializable sendObj = field.pack(new MPIParam(origPart, haloPart, MPIBaseType));

			final ArrayList<Serializable> recvObjs = MPIUtil.<Serializable>gather(gc.comm, sendObj, gc.groupRoot);

			if (qt.isGroupMaster(gc))
				for (int i = 0; i < recvObjs.size(); i++)
					groupField.unpack(new MPIParam(gc.leaves.get(i).getShape(), gc.master.getShape(), MPIBaseType),
							recvObjs.get(i));
		}

		MPI.COMM_WORLD.barrier();
	}

	public void distributeGroup(final int level, final GridStorage groupField) throws MPIException {
		if (!(ps instanceof DQuadTreePartition))
			throw new UnsupportedOperationException(
					"Can only distribute to group with DQuadTreePartition, got " + ps.getClass().getSimpleName());

		final DQuadTreePartition qt = (DQuadTreePartition) ps;
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

			field.unpack(new MPIParam(origPart, haloPart, MPIBaseType), recvObj);
		}

		sync();
	}

	public String toString() {
		return String.format("PID %d Storage %s", ps.getPid(), field);
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

			if (ps.isToroidal())
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
