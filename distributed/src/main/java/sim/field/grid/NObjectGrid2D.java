package sim.field.grid;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.function.IntFunction;

import sim.engine.DSimState;
import sim.field.DPartition;
import sim.field.HaloField;
import sim.field.storage.ObjectGridStorage;
import sim.util.IntPoint;

public class NObjectGrid2D<T extends Serializable> extends HaloField<T, IntPoint, ObjectGridStorage<T>> {

	public NObjectGrid2D(final DPartition ps, final int[] aoi, final IntFunction<T[]> allocator,
			final DSimState state) {
		super(ps, aoi, new ObjectGridStorage<T>(ps.getPartition(), allocator), state);

		if (numDimensions != 2)
			throw new IllegalArgumentException("The number of dimensions is expected to be 2, got: " + numDimensions);
	}

	@SuppressWarnings("unchecked")
	public T[] getStorageArray() {
		return (T[]) localStorage.getStorage();
	}

	public T getLocal(final IntPoint p) {
		return getStorageArray()[localStorage.getFlatIdx(toLocalPoint(p))];
	}

	public T getRMI(final IntPoint p) throws RemoteException {
		return getLocal(p);
	}

	public void addLocal(final IntPoint p, final T t) {
		getStorageArray()[localStorage.getFlatIdx(toLocalPoint(p))] = t;
	}

	public void removeLocal(final IntPoint p, final T t) {
		removeLocal(p);
	}

	public void removeLocal(final IntPoint p) {
		addLocal(p, null);
	}

	@SuppressWarnings("unchecked")
	public T get(final IntPoint p) {
		if (!inLocalAndHalo(p)) {
			System.out.println(String.format("PID %d get %s is out of local boundary, accessing remotely through RMI",
					partition.getPid(), p.toString()));
			// Should be the same return type as the getLocal method
			return (T) getFromRemote(p);
		} else
			return getLocal(p);
	}

	/*
	 * public static void main(String[] args) throws MPIException, IOException {
	 * MPI.Init(args);
	 *
	 * int[] aoi = new int[] {2, 2}; int[] size = new int[] {8, 8};
	 *
	 * DNonUniformPartition p = DNonUniformPartition.getPartitionScheme(size, true,
	 * aoi); p.initUniformly(null); p.commit();
	 *
	 * NObjectGrid2D<TestObj> f = new NObjectGrid2D<TestObj>(p, aoi, s -> new
	 * TestObj[s]);
	 *
	 * MPITest.execOnlyIn(0, x -> { f.set(0, 3, new TestObj(0)); f.set(1, 2, new
	 * TestObj(1)); f.set(2, 1, new TestObj(2)); f.set(3, 0, new TestObj(3)); });
	 *
	 * MPITest.execOnlyIn(2, x -> { f.set(4, 0, new TestObj(4)); f.set(5, 1, new
	 * TestObj(5)); f.set(6, 2, new TestObj(6)); f.set(7, 3, new TestObj(7)); });
	 *
	 * MPITest.execOnlyIn(0, i -> System.out.println("Before Sync..."));
	 * MPITest.execInOrder(i -> System.out.println(f), 500);
	 *
	 * f.sync();
	 *
	 * MPITest.execOnlyIn(0, i -> System.out.println("After Sync..."));
	 * MPITest.execInOrder(i -> System.out.println(f), 500);
	 *
	 * GridStorage full = new ObjectGridStorage<TestObj>(p.getField(), s -> new
	 * TestObj[s]); f.collect(0, full);
	 *
	 * MPITest.execOnlyIn(0, i -> System.out.println("Full Field...\n" + full));
	 *
	 * TestObj[] array = (TestObj[])full.getStorage(); array[5] = new TestObj(99);
	 * MPITest.execOnlyIn(0, i -> System.out.println("Modified Full Field...\n" +
	 * full));
	 *
	 * f.distribute(0, full);
	 *
	 * MPITest.execOnlyIn(0, i ->
	 * System.out.println("After distributing modified Full Field..."));
	 * MPITest.execInOrder(i -> System.out.println(f), 500);
	 *
	 * MPI.Finalize(); }
	 */
}
