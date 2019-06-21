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
public class NObjectsGrid2D<T extends Serializable> extends HaloField<T> {

	public NObjectsGrid2D(final DPartition ps, final int[] aoi, final DSimState state) {
		super(ps, aoi, new ObjectGridStorage<ArrayList<T>>(ps.getPartition(), s -> new ArrayList[s]), state);

		if (numDimensions != 2)
			throw new IllegalArgumentException(
					"The number of dimensions is expected to be 2, got: " + numDimensions);
	}

//	public void move(final IntPoint fromLoc, final IntPoint toLoc, final T t) {
//		// TODO
//		// moves anything thats not an agent, the location can be remote or local
//		throw (new UnsupportedOperationException());
//	}
//
//	public void moveAgent(final IntPoint fromLoc, final IntPoint toLoc, final T t, Schedule schedule) {
//		// TODO
//		// moves an agent and schedules it, the location can be remote or local
//		throw (new UnsupportedOperationException());
//	}
//
//	public void moveAgent(final IntPoint fromLoc, final IntPoint toLoc, final T t, final Stoppable stoppable,
//			Schedule schedule) {
//		// TODO
//		// moves an 'repeating' agent and schedules it,
//		// the location can be remote or local
//		// call stoppable.stop()?
//		throw (new UnsupportedOperationException());
//	}

	public ArrayList<T>[] getStorageArray() {
		return (ArrayList<T>[]) field.getStorage();
	}

	public ArrayList<T> getRMI(final IntPoint p) throws RemoteException {
		if (!inLocal(p))
			throw new RemoteException(
					"The point " + p + " does not exist in this partition " + ps.getPid() + " " + ps.getPartition());

		return getStorageArray()[field.getFlatIdx(toLocalPoint(p))];
	}

	public ArrayList<T> get(final int x, final int y) {
		return get(new IntPoint(x, y));
	}

	public ArrayList<T> get(final IntPoint p) {
		if (!inLocalAndHalo(p)) {
			System.out.println(String.format("PID %d get %s is out of local boundary, accessing remotely through RMI",
					ps.getPid(), p.toString()));
			return (ArrayList<T>) getFromRemote(p);
		}

		return getStorageArray()[field.getFlatIdx(toLocalPoint(p))];
	}

	public void addObject(final IntPoint p, final T t) {
		// In this partition but not in ghost cells
		if (!inLocal(p))
			// TODO: should there be a way to set the remote stuff as well?
			throw new IllegalArgumentException(
					String.format("PID %d set %s is out of local boundary", ps.getPid(), p.toString()));

		final ArrayList<T>[] array = getStorageArray();
		final int idx = field.getFlatIdx(toLocalPoint(p));

		if (array[idx] == null)
			array[idx] = new ArrayList<T>();

		array[idx].add(t);
	}

	public void removeObject(final IntPoint p, final T t) {
		// In this partition but not in ghost cells
		if (!inLocal(p))
			throw new IllegalArgumentException(
					String.format("PID %d set %s is out of local boundary", ps.getPid(), p.toString()));

		final ArrayList<T>[] array = getStorageArray();
		final int idx = field.getFlatIdx(toLocalPoint(p));

		if (array[idx] != null)
			// TODO: if it's empty should it be GCed?
			array[idx].remove(t);
	}

}
