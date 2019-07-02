package sim.field.grid;

import java.rmi.RemoteException;

import sim.engine.DSimState;
import sim.field.DPartition;
import sim.field.HaloField;
import sim.field.storage.DoubleGridStorage;
import sim.util.IntPoint;

public class NDoubleGrid2D extends HaloField<Double, IntPoint, DoubleGridStorage> {

	public final double initVal;

	public NDoubleGrid2D(final DPartition ps, final int[] aoi, final int initVal, final DSimState state) {
		super(ps, aoi, new DoubleGridStorage(ps.getPartition(), initVal), state);

		if (numDimensions != 2)
			throw new IllegalArgumentException("The number of dimensions is expected to be 2, got: " + numDimensions);
		this.initVal = initVal;
	}

	public double[] getStorageArray() {
		return (double[]) localStorage.getStorage();
	}

	public double getLocal(final IntPoint p) {
		return getStorageArray()[localStorage.getFlatIdx(toLocalPoint(p))];
	}

	public void addLocal(final IntPoint p, final double t) {
		getStorageArray()[localStorage.getFlatIdx(toLocalPoint(p))] = t;
	}

	public Double getRMI(final IntPoint p) {
		return getLocal(p);
	}

	public void addLocal(final IntPoint p, final Double t) {
		getStorageArray()[localStorage.getFlatIdx(toLocalPoint(p))] = t;
	}

	public void removeLocal(final IntPoint p, final Double t) {
		removeLocal(p);
	}

	public void removeLocal(final IntPoint p) {
		addLocal(p, initVal);
	}

	public double get(final IntPoint p) {
		if (!inLocalAndHalo(p)) {
			System.out.println(String.format("PID %d get %s is out of local boundary, accessing remotely through RMI",
					partition.getPid(), p.toString()));
			// TODO: Should this be (Double)?
			return (double) getFromRemote(p);
		} else
			return getLocal(p);
	}

	// Overloading to prevent AutoBoxing-UnBoxing
	public void add(final IntPoint p, final double val) {
		if (!inLocal(p))
			addToRemote(p, val);
		else
			addLocal(p, val);
	}

	// Overloading to prevent AutoBoxing-UnBoxing
	public void remove(final IntPoint p, final double t) {
		remove(p);
	}

	// Overloading to prevent AutoBoxing-UnBoxing
	public void move(final IntPoint fromP, final IntPoint toP, final double t) {
		final int fromPid = partition.toPartitionId(fromP);
		final int toPid = partition.toPartitionId(fromP);

		if (fromPid == toPid && fromPid != partition.pid) {
			// So that we make only a single RMI call instead of two
			try {
				proxy.getField(partition.toPartitionId(fromP)).moveRMI(fromP, toP, t);
			} catch (final RemoteException e) {
				throw new RuntimeException(e);
			}
		} else {
			remove(fromP, t);
			add(toP, t);
		}
	}
}
