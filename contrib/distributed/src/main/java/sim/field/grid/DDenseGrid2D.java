package sim.field.grid;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;

import sim.engine.DSimState;
import sim.field.DAbstractGrid2D;
import sim.field.HaloGrid2D;
import sim.field.partitioning.PartitionInterface;
import sim.field.partitioning.IntPoint;
import sim.field.partitioning.NdPoint;
import sim.field.storage.ObjectGridStorage;

public class DDenseGrid2D<T extends Serializable> extends DAbstractGrid2D {

	private HaloGrid2D<T, NdPoint, ObjectGridStorage<ArrayList<T>>> halo;
	
	
	public DDenseGrid2D(final PartitionInterface ps, final int[] aoi, final DSimState state) {
		super(ps);
		if(ps.getNumDim()!=2)
			throw new IllegalArgumentException("The number of dimensions is expected to be 2, got: " +ps.getNumDim());
		
		halo = new HaloGrid2D<T, NdPoint, ObjectGridStorage<ArrayList<T>>>
				(ps, aoi,new ObjectGridStorage<ArrayList<T>>(ps.getPartition(), s -> new ArrayList[s]), state);


	}
	public void addAgent(final NdPoint p, final T t) {
		halo.addAgent(p, t);
	}
	public void moveAgent(final NdPoint fromP, final NdPoint toP, final T t) {
		 halo.moveAgent(fromP, toP, t);
	}
	public void addRepeatingAgent(final NdPoint p, final T t, final int ordering, final double interval) {
		halo.addRepeatingAgent(p, t, ordering, interval);
	}
	public void moveRepeatingAgent(final NdPoint fromP, final NdPoint toP, final T t) {
	
		halo.moveRepeatingAgent(fromP, toP, t);
	}
	
	public ArrayList<T>[] getStorageArray() {
		return (ArrayList<T>[]) halo.localStorage.getStorage();
	}

	public ArrayList<T> getLocal(final IntPoint p) {
		return getStorageArray()[halo.localStorage.getFlatIdx(halo.toLocalPoint(p))];
	}

	public ArrayList<T> getRMI(final IntPoint p) throws RemoteException {
		return getLocal(p);
	}

	public void addLocal(final IntPoint p, final T t) {
		final ArrayList<T>[] array = getStorageArray();
		final int idx = halo.localStorage.getFlatIdx(halo.toLocalPoint(p));

		if (array[idx] == null)
			array[idx] = new ArrayList<T>();

		array[idx].add(t);
	}

	public void removeLocal(final IntPoint p, final T t) {
		final ArrayList<T>[] array = getStorageArray();
		final int idx = halo.localStorage.getFlatIdx(halo.toLocalPoint(p));

		if (array[idx] != null)
			array[idx].remove(t);

	}

	public void removeLocal(final IntPoint p) {
		final ArrayList<T>[] array = getStorageArray();
		final int idx = halo.localStorage.getFlatIdx(halo.toLocalPoint(p));

		if (array[idx] != null)
			array[idx].clear();

	}

	public ArrayList<T> get(final IntPoint p) {
		if (!halo.inLocalAndHalo(p)) {
			System.out.println(String.format("PID %d get %s is out of local boundary, accessing remotely through RMI",
					halo.partition.getPid(), p.toString()));
			return (ArrayList<T>) halo.getFromRemote(p);
		} else
			return getLocal(p);
	}
	
	
/*UTILS METHODS*/
	


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



}
