package sim.field.grid;

import java.rmi.RemoteException;

import sim.engine.DSimState;
import sim.engine.DistributedIterativeRepeat;
import sim.field.DAbstractGrid2D;
import sim.field.DGrid;
import sim.field.HaloGrid2D;
import sim.field.partitioning.IntPoint;
import sim.field.partitioning.PartitionInterface;
import sim.field.storage.IntGridStorage;

/**
 * A grid that contains integers. Analogous to Mason's IntGrid2D
 * 
 */
public class DIntGrid2D extends DAbstractGrid2D implements DGrid<Integer, IntPoint> {

	private HaloGrid2D<Integer, IntPoint, IntGridStorage<Integer>> halo;
	public final int initVal;

	public DIntGrid2D(final PartitionInterface ps, final int[] aoi, final int initVal, final DSimState state) {
		super(ps);
		if (ps.getNumDim() != 2)
			throw new IllegalArgumentException("The number of dimensions is expected to be 2, got: " + ps.getNumDim());

		halo = new HaloGrid2D<Integer, IntPoint, IntGridStorage<Integer>>(ps, aoi,
				new IntGridStorage(ps.getPartition(), initVal), state);
		fieldSize = ps.getFieldSize();

		this.initVal = initVal;
	}

	public int[] getStorageArray() {
		return (int[]) halo.localStorage.getStorage();
	}

	public int getLocal(final IntPoint p) {
		return getStorageArray()[halo.localStorage.getFlatIdx(halo.toLocalPoint(p))];
	}

	public void addLocal(final IntPoint p, final int t) {
		getStorageArray()[halo.localStorage.getFlatIdx(halo.toLocalPoint(p))] = t;
	}

	public Integer getRMI(final IntPoint p) {
		return getLocal(p);
	}

	public void addLocal(final IntPoint p, final Integer t) {
		getStorageArray()[halo.localStorage.getFlatIdx(halo.toLocalPoint(p))] = t;
	}

	public void removeLocal(final IntPoint p, final Double t) {
		removeLocal(p);
	}

	public void removeLocal(final IntPoint p) {
		addLocal(p, initVal);
	}

	public int get(final IntPoint p) {
		if (!halo.inLocalAndHalo(p)) {
			System.out.println(String.format("PID %d get %s is out of local boundary, accessing remotely through RMI",
					halo.partition.getPid(), p.toString()));
			// TODO: Should this be (Double)?
			return (int) halo.getFromRemote(p);
		} else
			return getLocal(p);
	}

	// Overloading to prevent AutoBoxing-UnBoxing
	public void add(final IntPoint p, final int val) {
		if (!halo.inLocal(p))
			halo.addToRemote(p, val);
		else
			addLocal(p, val);
	}

	// Overloading to prevent AutoBoxing-UnBoxing
	public void remove(final IntPoint p, final int t) {
		halo.remove(p);
	}

	// Overloading to prevent AutoBoxing-UnBoxing
	public void move(final IntPoint fromP, final IntPoint toP, final int t) {
		final int fromPid = halo.partition.toPartitionId(fromP);
		final int toPid = halo.partition.toPartitionId(fromP);

		if (fromPid == toPid && fromPid != halo.partition.pid) {
			// So that we make only a single RMI call instead of two
			try {
				halo.proxy.getField(halo.partition.toPartitionId(fromP)).moveRMI(fromP, toP, t);
			} catch (final RemoteException e) {
				throw new RuntimeException(e);
			}
		} else {
			remove(fromP, t);
			add(toP, t);
		}
	}

	public void add(IntPoint p, Integer t) {
		halo.add(p, t);
	}

	public void remove(IntPoint p, Integer t) {
		halo.remove(p, t);
	}

	public void remove(IntPoint p) {
		halo.remove(p);
	}

	public void move(IntPoint fromP, IntPoint toP, Integer t) {
		halo.move(fromP, toP, t);
	}

	public void addAgent(IntPoint p, Integer t) {
		halo.addAgent(p, t);
	}

	public void addAgent(IntPoint p, Integer t, int ordering, double time) {
		halo.addAgent(p, t, ordering, time);
	}

	public void moveAgent(IntPoint fromP, IntPoint toP, Integer t) {
		halo.moveAgent(fromP, toP, t);
	}

	public void moveAgent(IntPoint fromP, IntPoint toP, Integer t, int ordering, double time) {
		halo.moveAgent(fromP, toP, t, ordering, time);
	}

	public void addRepeatingAgent(IntPoint p, Integer t, double time, int ordering, double interval) {
		halo.addRepeatingAgent(p, t, time, ordering, interval);
	}

	public void addRepeatingAgent(IntPoint p, Integer t, int ordering, double interval) {
		halo.addRepeatingAgent(p, t, ordering, interval);
	}

	public void removeAndStopRepeatingAgent(IntPoint p, Integer t) {
		halo.removeAndStopRepeatingAgent(p, t);
	}

	public void removeAndStopRepeatingAgent(IntPoint p, DistributedIterativeRepeat iterativeRepeat) {
		halo.removeAndStopRepeatingAgent(p, iterativeRepeat);
	}

	public void moveRepeatingAgent(IntPoint fromP, IntPoint toP, Integer t) {
		halo.moveRepeatingAgent(fromP, toP, t);
	}

}
