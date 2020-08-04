package sim.field.grid;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;

import sim.engine.DSimState;
import sim.field.DPartition;
import sim.field.HaloField;
import sim.field.storage.ObjectGridStorage;
import sim.util.IntPoint;

@SuppressWarnings("unchecked")
public class NObjectsGrid2D<T extends Serializable> extends HaloField<T, IntPoint, ObjectGridStorage<ArrayList<T>>> {

	public NObjectsGrid2D(final DPartition ps, final int[] aoi, final DSimState state) {
		super(ps, aoi, new ObjectGridStorage<ArrayList<T>>(ps.getPartition(), s -> new ArrayList[s]), state);

		if (numDimensions != 2)
			throw new IllegalArgumentException(
					"The number of dimensions is expected to be 2, got: " + numDimensions);
	}

	public ArrayList<T>[] getStorageArray() {
		return (ArrayList<T>[]) localStorage.getStorage();
	}

	public ArrayList<T> getLocal(final IntPoint p) {
		return getStorageArray()[localStorage.getFlatIdx(toLocalPoint(p))];
	}

	public ArrayList<T> getRMI(final IntPoint p) throws RemoteException {
		return getLocal(p);
	}

	public void addLocal(final IntPoint p, final T t) {
		final ArrayList<T>[] array = getStorageArray();
		final int idx = localStorage.getFlatIdx(toLocalPoint(p));

		if (array[idx] == null)
			array[idx] = new ArrayList<T>();

		array[idx].add(t);
	}

	public void removeLocal(final IntPoint p, final T t) {
		final ArrayList<T>[] array = getStorageArray();
		final int idx = localStorage.getFlatIdx(toLocalPoint(p));

		if (array[idx] != null)
			array[idx].remove(t);

	}

	public void removeLocal(final IntPoint p) {
		final ArrayList<T>[] array = getStorageArray();
		final int idx = localStorage.getFlatIdx(toLocalPoint(p));

		if (array[idx] != null)
			array[idx].clear();

	}

	public ArrayList<T> get(final IntPoint p) {
		if (!inLocalAndHalo(p)) {
			System.out.println(String.format("PID %d get %s is out of local boundary, accessing remotely through RMI",
					partition.getPid(), p.toString()));
			return (ArrayList<T>) getFromRemote(p);
		} else
			return getLocal(p);
	}
}
