package sim.field.grid;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.function.IntFunction;
import java.util.ArrayList;

import mpi.*;

import sim.util.IntPoint;
import sim.util.IntHyperRect;
import sim.util.MPITest;
import sim.field.DPartition;
import sim.field.DNonUniformPartition;
import sim.field.HaloField;
import sim.field.storage.GridStorage;
import sim.field.storage.ObjectGridStorage;
import sim.field.storage.TestObj;

public class NObjectsGrid2D<T extends Serializable> extends HaloField {

	public NObjectsGrid2D(DPartition ps, int[] aoi) {
		super(ps, aoi, new ObjectGridStorage<ArrayList>(ps.getPartition(), s -> new ArrayList[s]));

		if (this.nd != 2)
			throw new IllegalArgumentException("The number of dimensions is expected to be 2, got: " + this.nd);
	}

	public ArrayList<T>[] getStorageArray() {
		return (ArrayList<T>[])field.getStorage();
	}

	public ArrayList<T> getRMI(IntPoint p) throws RemoteException {
		if (!inLocal(p))
			throw new RemoteException("The point " + p + " does not exist in this partition " + ps.getPid() + " " + ps.getPartition());

		return getStorageArray()[field.getFlatIdx(toLocalPoint(p))];
	}

	public final ArrayList<T> get(final int x, final int y) {
		return get(new IntPoint(x, y));
	}

	public final ArrayList<T> get(IntPoint p) {
		if (!inLocalAndHalo(p)) {
			System.out.println(String.format("PID %d get %s is out of local boundary, accessing remotely through RMI", ps.getPid(), p.toString()));
			return (ArrayList<T>)getFromRemote(p);
		}

		return getStorageArray()[field.getFlatIdx(toLocalPoint(p))];
	}

	public final void set(final int x, final int y, final T val) {
		set(new IntPoint(x, y), val);
	}

	public final void set(IntPoint p, final T val) {
		// In this partition but not in ghost cells
		if (!inLocal(p))
			throw new IllegalArgumentException(String.format("PID %d set %s is out of local boundary", ps.getPid(), p.toString()));

		ArrayList<T>[] array = getStorageArray();
		int idx = field.getFlatIdx(toLocalPoint(p));

		if (array[idx] == null)
			array[idx] = new ArrayList<T>();

		array[idx].add(val);
	}

	public static void main(String[] args) throws MPIException, IOException {
		MPI.Init(args);

		int[] aoi = new int[] {2, 2};
		int[] size = new int[] {8, 8};

		DNonUniformPartition p = DNonUniformPartition.getPartitionScheme(size, true, aoi);
		p.initUniformly(null);
		p.commit();

		NObjectsGrid2D<TestObj> f = new NObjectsGrid2D<TestObj>(p, aoi);

		MPITest.execOnlyIn(0, x -> {
			f.set(0, 3, new TestObj(10));
			f.set(1, 2, new TestObj(11));
			f.set(2, 1, new TestObj(12));
			f.set(3, 0, new TestObj(13));
			f.set(1, 2, new TestObj(21));
			f.set(2, 1, new TestObj(22));
		});

		MPITest.execOnlyIn(2, x -> {
			f.set(4, 0, new TestObj(34));
			f.set(5, 1, new TestObj(35));
			f.set(6, 2, new TestObj(36));
			f.set(7, 3, new TestObj(37));
			f.set(4, 0, new TestObj(44));
			f.set(7, 3, new TestObj(47));
		});

		MPITest.execOnlyIn(0, i -> System.out.println("Before Sync..."));
		MPITest.execInOrder(i -> System.out.println(f), 500);

		f.sync();

		MPITest.execOnlyIn(0, i -> System.out.println("After Sync..."));
		MPITest.execInOrder(i -> System.out.println(f), 500);

		GridStorage full = new ObjectGridStorage<ArrayList<TestObj>>(p.getField(), s -> new ArrayList[s]);
		f.collect(0, full);

		MPITest.execOnlyIn(0, i -> System.out.println("Full Field...\n" + full));

		MPI.Finalize();
	}
}
