package sim.field.grid;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.rmi.RemoteException;
import sim.engine.DSimState;
import sim.engine.DistributedIterativeRepeat;
import sim.field.DAbstractGrid2D;
import sim.field.DGrid;
import sim.field.HaloGrid2D;
import sim.field.partitioning.PartitionInterface;
import sim.field.storage.ObjectGridStorage;
import sim.util.*;

/**
 * A grid that contains objects of type T. Analogous to Mason's ObjectGrid2D
 *
 * @param <T> Type of object stored in the grid
 */
public class DObjectGrid2D<T extends Serializable> extends DAbstractGrid2D implements DGrid<T, NumberND> {
	private static final long serialVersionUID = 1L;

	private HaloGrid2D<T, NumberND, ObjectGridStorage<T>> halo;

	@SuppressWarnings("unchecked")
	public DObjectGrid2D(final PartitionInterface ps, final int[] aoi, final DSimState state, Class<T> clazz) {
		super(ps);
		halo = new HaloGrid2D<T, NumberND, ObjectGridStorage<T>>(ps, aoi,
				new ObjectGridStorage<T>(ps.getBounds(), s -> (T[]) Array.newInstance(clazz, s)), state);
	}

	public T getLocal(final Int2D p) {
		return halo.localStorage.getStorageArray()[halo.localStorage.getFlatIdx(halo.toLocalPoint(p))];
	}

	public T getRMI(final Int2D p) throws RemoteException {
		return getLocal(p);
	}

	public void addLocal(final Int2D p, final T t) {
		halo.localStorage.getStorageArray()[halo.localStorage.getFlatIdx(halo.toLocalPoint(p))] = t;
	}

	public void removeLocal(final Int2D p, final T t) {
		removeLocal(p);
	}

	public void removeLocal(final Int2D p) {
		addLocal(p, null);
	}

	@SuppressWarnings("unchecked")
	public T get(final Int2D p) {
		if (!halo.inLocalAndHalo(p)) {
			System.out.println(String.format("PID %d get %s is out of local boundary, accessing remotely through RMI",
					halo.partition.getPid(), p.toString()));
			// Should be the same return type as the getLocal method
			return (T) halo.getFromRemote(p);
		} else
			return getLocal(p);
	}

	/* UTILS METHODS */
/*
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
*/

	public void addAgent(final NumberND p, final T t) {
		halo.addAgent(p, t);
	}

	public void moveAgent(final NumberND fromP, final NumberND toP, final T t) {
		halo.moveAgent(fromP, toP, t);
	}

	public void addRepeatingAgent(final NumberND p, final T t, final int ordering, final double interval) {
		halo.addRepeatingAgent(p, t, ordering, interval);
	}

	public void moveRepeatingAgent(final NumberND fromP, final NumberND toP, final T t) {
		halo.moveRepeatingAgent(fromP, toP, t);
	}

	public void add(NumberND p, T t) {
		halo.add(p, t);
	}

	public void remove(NumberND p, T t) {
		halo.remove(p, t);
	}

	public void remove(NumberND p) {
		halo.remove(p);
	}

	public void move(NumberND fromP, NumberND toP, T t) {
		halo.move(fromP, toP, t);
	}

	public void addAgent(NumberND p, T t, int ordering, double time) {
		halo.addAgent(p, t, ordering, time);

	}

	public void moveAgent(NumberND fromP, NumberND toP, T t, int ordering, double time) {
		halo.moveAgent(fromP, toP, t, ordering, time);
	}

	public void addRepeatingAgent(NumberND p, T t, double time, int ordering, double interval) {
		halo.addRepeatingAgent(p, t, time, ordering, interval);
	}

	public void removeAndStopRepeatingAgent(NumberND p, T t) {
		halo.removeAndStopRepeatingAgent(p, t);
	}

	public void removeAndStopRepeatingAgent(NumberND p, DistributedIterativeRepeat iterativeRepeat) {
		halo.removeAndStopRepeatingAgent(p, iterativeRepeat);
	}

}
