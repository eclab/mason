package sim.field.grid;

import java.rmi.RemoteException;

import sim.engine.DSimState;
import sim.field.DPartition;
import sim.field.HaloField;
import sim.field.storage.IntGridStorage;
import sim.util.IntPoint;

public class NIntGrid2D extends HaloField<Integer, IntPoint, IntGridStorage> {
	public final int initVal;

	public NIntGrid2D(final DPartition ps, final int[] aoi, final int initVal, final DSimState state) {
		super(ps, aoi, new IntGridStorage(ps.getPartition(), initVal), state);
		if (numDimensions != 2)
			throw new IllegalArgumentException("The number of dimensions is expected to be 2, got: " + numDimensions);
		this.initVal = initVal;
	}

	public int[] getStorageArray() {
		return (int[]) localStorage.getStorage();
	}

	public int getLocal(final IntPoint p) {
		return getStorageArray()[localStorage.getFlatIdx(toLocalPoint(p))];
	}

	public void addLocal(final IntPoint p, final int t) {
		getStorageArray()[localStorage.getFlatIdx(toLocalPoint(p))] = t;
	}

	public Integer getRMI(final IntPoint p) {
		return getLocal(p);
	}

	public void addLocal(final IntPoint p, final Integer t) {
		getStorageArray()[localStorage.getFlatIdx(toLocalPoint(p))] = t;
	}

	public void removeLocal(final IntPoint p, final Integer t) {
		removeLocal(p);
	}

	public void removeLocal(final IntPoint p) {
		addLocal(p, initVal);
	}

	public final int get(final IntPoint p) {
		if (!inLocalAndHalo(p)) {
			System.out.println(String.format("PID %d get %s is out of local boundary, accessing remotely through RMI",
					partition.getPid(), p.toString()));
			return (int) getFromRemote(p);
		} else
			return getLocal(p);
	}

	// Overloading to prevent AutoBoxing-UnBoxing
	public void add(final IntPoint p, final int t) {
		if (!inLocal(p))
			addToRemote(p, t);
		else
			addLocal(p, t);
	}

	// Overloading to prevent AutoBoxing-UnBoxing
	public void remove(final IntPoint p, final int t) {
		remove(p);
	}

	// Overloading to prevent AutoBoxing-UnBoxing
	public void move(final IntPoint fromP, final IntPoint toP, final int t) {
		final int fromPid = partition.toPartitionId(fromP);
		final int toPid = partition.toPartitionId(fromP);

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

	/*
	 * public static void main(String[] args) throws MPIException, IOException {
	 * MPI.Init(args);
	 *
	 * int[] aoi = new int[] {2, 2}; int[] size = new int[] {10, 10};
	 *
	 * DNonUniformPartition p = DNonUniformPartition.getPartitionScheme(size, true,
	 * aoi); p.initUniformly(null); p.commit();
	 *
	 * NIntGrid2D f = new NIntGrid2D(p, aoi, p.getPid());
	 *
	 * f.sync();
	 *
	 * MPITest.execInOrder(i -> System.out.println(f), 500);
	 *
	 * MPITest.execOnlyIn(0, i -> System.out.println("Testing RMI remote calls"));
	 * sim.field.RemoteProxy.Init(0); f.initRemote();
	 *
	 * // Choose the points that are out of halo area int pid = p.getPid(); int x =
	 * f.stx(2 + 5 * ((pid + 1) / 2)); int y = f.sty(2 + 5 * ((pid + 1) % 2));
	 * MPITest.execInOrder(i ->
	 * System.out.println(String.format("PID %d accessing <%d, %d> result %d", i, x,
	 * y, f.get(x, y))), 200);
	 *
	 * sim.field.RemoteProxy.Finalize(); MPI.Finalize(); }
	 */
}
