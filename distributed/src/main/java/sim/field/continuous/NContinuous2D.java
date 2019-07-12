package sim.field.continuous;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import sim.engine.DSimState;
import sim.field.DPartition;
import sim.field.HaloField;
import sim.field.storage.ContStorage;
import sim.util.MPIParam;
import sim.util.NdPoint;

public class NContinuous2D<T extends Serializable> extends HaloField<T, NdPoint, ContStorage<T>> {

	public NContinuous2D(final DPartition ps, final int[] aoi, final double[] discretizations, final DSimState state) {
		super(ps, aoi, new ContStorage<T>(ps.getPartition(), discretizations), state);

		if (numDimensions != 2)
			throw new IllegalArgumentException("The number of dimensions is expected to be 2, got: " + numDimensions);
	}

	public NdPoint getLocation(final T obj) {
		return localStorage.getLocation(obj);
	}

	@SuppressWarnings("unchecked")
	public List<T> get(final NdPoint p) {
		if (!inLocalAndHalo(p))
			return (List<T>) getFromRemote(p);
		else
			return getLocal(p);
	}

	public List<T> getNearestNeighbors(final T obj, final int k) {
		return localStorage.getNearestNeighbors(obj, k);
	}

	public List<T> getNeighborsWithin(final T obj, final double r) {
		return localStorage.getNeighborsWithin(obj, r);
	}

	// Overriding this because
	// add also moves the objects in this field
	public void move(final NdPoint fromP, final NdPoint toP, final T t) {
		final int fromPid = partition.toPartitionId(fromP);
		final int toPid = partition.toPartitionId(fromP);

		if (fromPid == toPid) {
			if (fromPid != partition.pid)
				try {
					proxy.getField(partition.toPartitionId(fromP)).addRMI(toP, t);
				} catch (final RemoteException e) {
					throw new RuntimeException(e);
				}
			else
				add(toP, t);
		} else {
			// move cannot be with a single call
			// Since, the fromP and toP are different

			// Remove from one partition
			remove(fromP, t);

			// add to another
			add(toP, t);
		}
	}

	// TODO refactor this after new pack/unpack is introduced in Storage
	@SuppressWarnings("unchecked")
	public final List<T> getAllObjects() {
		Serializable data = null;

		data = localStorage.pack(new MPIParam(origPart, haloPart, MPIBaseType));

		final List<T> objs = ((ArrayList<ArrayList<T>>) data).get(0);

		return IntStream.range(0, objs.size())
				.filter(n -> n % 2 == 0)
				.mapToObj(objs::get)
				.collect(Collectors.toList());
	}

	public ArrayList<T> getLocal(final NdPoint p) {
		return localStorage.getObjects(p);
	}

	public void addLocal(final NdPoint p, final T t) {
		localStorage.setLocation(t, p);
	}

	public void removeLocal(final NdPoint p, final T t) {
		// TODO: Remove from just p
		localStorage.removeObject(t);
	}

	public void removeLocal(final NdPoint p) {
		localStorage.removeObjects(p);
	}

	public Serializable getRMI(final NdPoint p) throws RemoteException {
		return getLocal(p);
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
