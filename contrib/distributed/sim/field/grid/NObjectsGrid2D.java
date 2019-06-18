package sim.field.grid;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;

import sim.field.DPartition;
//import sim.field.DNonUniformPartition;
import sim.field.HaloField;
import sim.field.storage.ObjectGridStorage;
import sim.util.IntPoint;

@SuppressWarnings("unchecked")
public class NObjectsGrid2D<T extends Serializable> extends HaloField {

	public NObjectsGrid2D(DPartition ps, int[] aoi) {
		super(ps, aoi, new ObjectGridStorage<ArrayList<T>>(ps.getPartition(), s -> new ArrayList[s]));

		if (this.numDimensions != 2)
			throw new IllegalArgumentException("The number of dimensions is expected to be 2, got: " + this.numDimensions);
	}

	public ArrayList<T>[] getStorageArray() {
		return (ArrayList<T>[]) field.getStorage();
	}

	public ArrayList<T> getRMI(IntPoint p) throws RemoteException {
		if (!inLocal(p))
			throw new RemoteException(
					"The point " + p + " does not exist in this partition " + ps.getPid() + " " + ps.getPartition());

		return getStorageArray()[field.getFlatIdx(toLocalPoint(p))];
	}

	public final ArrayList<T> get(final int x, final int y) {
		return get(new IntPoint(x, y));
	}

	public final ArrayList<T> get(IntPoint p) {
		if (!inLocalAndHalo(p)) {
			System.out.println(String.format("PID %d get %s is out of local boundary, accessing remotely through RMI",
					ps.getPid(), p.toString()));
			return (ArrayList<T>) getFromRemote(p);
		}

		return getStorageArray()[field.getFlatIdx(toLocalPoint(p))];
	}

	public final void add(final int x, final int y, final T t) {
		add(new IntPoint(x, y), t);
	}

	public final void add(final IntPoint p, final T t) {
		// In this partition but not in ghost cells
		if (!inLocal(p))
			// TODO: should there be a way to set the remote stuff as well?
			throw new IllegalArgumentException(
					String.format("PID %d set %s is out of local boundary", ps.getPid(), p.toString()));

		ArrayList<T>[] array = getStorageArray();
		int idx = field.getFlatIdx(toLocalPoint(p));

		if (array[idx] == null)
			array[idx] = new ArrayList<T>();

		array[idx].add(t);
	}

	public final boolean remove(final int x, final int y, final T t) {
		return remove(new IntPoint(x, y), t);
	}

	public final boolean remove(IntPoint p, final T t) {
		// In this partition but not in ghost cells
		if (!inLocal(p))
			throw new IllegalArgumentException(
					String.format("PID %d set %s is out of local boundary", ps.getPid(), p.toString()));

		ArrayList<T>[] array = getStorageArray();
		int idx = field.getFlatIdx(toLocalPoint(p));

		if (array[idx] == null)
			return false;

		// TODO: if it's empty should it be GCed?
		return array[idx].remove(t);
	}

}