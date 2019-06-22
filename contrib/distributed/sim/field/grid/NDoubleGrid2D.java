package sim.field.grid;

import java.rmi.RemoteException;

import sim.engine.DSimState;
import sim.field.DPartition;
//import sim.field.DNonUniformPartition;
import sim.field.HaloField;
import sim.field.storage.DoubleGridStorage;
import sim.util.IntPoint;

public class NDoubleGrid2D extends HaloField<Double, IntPoint> {

	public final double initVal;

	public NDoubleGrid2D(final DPartition ps, final int[] aoi, final int initVal, final DSimState state) {
		super(ps, aoi, new DoubleGridStorage(ps.getPartition(), initVal), state);

		if (numDimensions != 2)
			throw new IllegalArgumentException("The number of dimensions is expected to be 2, got: " + numDimensions);
		this.initVal = initVal;
	}

	public double[] getStorageArray() {
		return (double[]) field.getStorage();
	}

	public Double getRMI(final IntPoint p) throws RemoteException {
		if (!inLocal(p))
			throw new RemoteException(
					"The point " + p + " does not exist in this partition " + ps.getPid() + " " + ps.getPartition());

		return getStorageArray()[field.getFlatIdx(toLocalPoint(p))];
	}

	public double get(final int x, final int y) {
		return get(new IntPoint(x, y));
	}

	public double get(final IntPoint p) {
		if (!inLocalAndHalo(p)) {
			System.out.println(String.format("PID %d get %s is out of local boundary, accessing remotely through RMI",
					ps.getPid(), p.toString()));
			try {
				return (double) getFromRemote(p);
			} catch (final RemoteException e) {
				System.exit(-1);
				e.printStackTrace();
			}
		}

		return getStorageArray()[field.getFlatIdx(toLocalPoint(p))];
	}

	public void addObject(final IntPoint p, final double val) {
		// In this partition but not in ghost cells
		if (!inLocal(p))
			throw new IllegalArgumentException(
					String.format("PID %d set %s is out of local boundary", ps.getPid(), p.toString()));

		getStorageArray()[field.getFlatIdx(toLocalPoint(p))] = val;
	}

	public void addObject(final IntPoint p, final Double val) {
		addObject(p, val.doubleValue());
	}

	public void removeObject(final IntPoint p, final Double t) {
		removeObject(p);
	}

	// Overloading to prevent AutoBoxing-UnBoxing
	public void removeObject(final IntPoint p, final double t) {
		removeObject(p);
	}

	public void removeObject(final IntPoint p) {
		addObject(p, initVal);
	}

	// Overloading to prevent AutoBoxing-UnBoxing
	public void moveObject(final IntPoint fromP, final IntPoint toP, final double t) {
		removeObject(fromP);
		addObject(toP, t);
	}
}
