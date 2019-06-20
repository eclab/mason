package sim.field.continuous;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import mpi.MPIException;
import sim.engine.DSimState;
import sim.field.DPartition;
import sim.field.HaloField;
import sim.field.storage.ContStorage;
import sim.util.IntPoint;
import sim.util.MPIParam;
import sim.util.NdPoint;

public class NContinuous2D<T extends Serializable> extends HaloField {

	public NContinuous2D(final DPartition ps, final int[] aoi, final double[] discretizations, final DSimState state) {
		super(ps, aoi, new ContStorage<T>(ps.getPartition(), discretizations), state);

		if (numDimensions != 2)
			throw new IllegalArgumentException("The number of dimensions is expected to be 2, got: " + numDimensions);
	}

	public final NdPoint getLocation(final T obj) {
		return ((ContStorage) field).getLocation(obj);
	}

	public final void setLocation(final T obj, final NdPoint p) {
		if (!haloPart.contains(p))
			throw new IllegalArgumentException(String.format("PID %d Point %s Invalid setLocation() myHalo %s",
					ps.getPid(), p.toString(), haloPart));
		((ContStorage) field).setLocation(obj, p);
	}

	public final List<T> getObjects(final NdPoint p) {
		return ((ContStorage) field).getObjects(p);
	}

	public final List<T> getNearestNeighbors(final T obj, final int k) {
		return ((ContStorage) field).getNearestNeighbors(obj, k);
	}

	public final List<T> getNeighborsWithin(final T obj, final double r) {
		return ((ContStorage) field).getNeighborsWithin(obj, r);
	}

	public final void removeObject(final T obj) {
		((ContStorage) field).removeObject(obj);
	}

	public final void removeObjects(final NdPoint p) {
		((ContStorage) field).removeObjects(p);
	}

	// TODO refactor this after new pack/unpack is introduced in Storage
	public final List<T> getAllObjects() {
		Serializable data = null;

		try {
			data = field.pack(new MPIParam(origPart, haloPart, MPIBaseType));
		} catch (final MPIException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		final List<T> objs = ((ArrayList<ArrayList<T>>) data).get(0);

		return IntStream.range(0, objs.size())
				.filter(n -> n % 2 == 0)
				.mapToObj(objs::get)
				.collect(Collectors.toList());
	}

	// TODO
	public Serializable getRMI(final IntPoint p) throws RemoteException {
		// return ((ContStorage)field).getObjects(obj);
		return null;
	}

	/*
	 * public static void main(String[] args) throws MPIException { MPI.Init(args);
	 *
	 * int[] aoi = new int[] {10, 10}; int[] size = new int[] {1000, 1000}; double[]
	 * discretizations = new double[] {10, 10};
	 *
	 * DNonUniformPartition p = DNonUniformPartition.getPartitionScheme(size, true,
	 * aoi); p.initUniformly(null); p.commit();
	 *
	 * NContinuous2D<TestObj> f = new NContinuous2D<TestObj>(p, aoi,
	 * discretizations);
	 *
	 * MPITest.execOnlyIn(0, x -> { f.setLocation(new TestObj(0), new
	 * DoublePoint(5.5, 5.5)); f.setLocation(new TestObj(1), new DoublePoint(10.1,
	 * 10.1)); f.setLocation(new TestObj(2), new DoublePoint(5.5, 250));
	 * f.setLocation(new TestObj(3), new DoublePoint(495, 495)); });
	 *
	 * MPITest.execOnlyIn(2, x -> { f.setLocation(new TestObj(4), new
	 * DoublePoint(500, 250)); f.setLocation(new TestObj(5), new DoublePoint(750,
	 * 495)); f.setLocation(new TestObj(6), new DoublePoint(750, 250));
	 * f.setLocation(new TestObj(7), new DoublePoint(751, 495)); });
	 *
	 * MPITest.execOnlyIn(0, i -> System.out.println("Before Sync..."));
	 * MPITest.execInOrder(i -> System.out.println(f), 200);
	 *
	 * f.sync();
	 *
	 * MPITest.execOnlyIn(0, i -> System.out.println("After Sync..."));
	 * MPITest.execInOrder(i -> System.out.println(f), 200);
	 *
	 * ContStorage full = new ContStorage<TestObj>(p.getField(), discretizations);
	 * f.collect(0, full);
	 *
	 * MPITest.execOnlyIn(0, i -> System.out.println("Full Field...\n" + full));
	 *
	 * MPI.Finalize(); }
	 */
}
