package sim.field.grid;

import java.rmi.RemoteException;

import sim.engine.DSimState;
import sim.field.DPartition;
//import sim.field.DNonUniformPartition;
import sim.field.HaloField;
import sim.field.storage.DoubleGridStorage;
import sim.util.IntPoint;

public class NDoubleGrid2D extends HaloField {

	public NDoubleGrid2D(final DPartition ps, final int[] aoi, final int initVal, final DSimState state) {
		super(ps, aoi, new DoubleGridStorage(ps.getPartition(), initVal), state);

		if (numDimensions != 2)
			throw new IllegalArgumentException("The number of dimensions is expected to be 2, got: " + numDimensions);
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

	public final double get(final int x, final int y) {
		return get(new IntPoint(x, y));
	}

	public final double get(final IntPoint p) {
		if (!inLocalAndHalo(p)) {
			System.out.println(String.format("PID %d get %s is out of local boundary, accessing remotely through RMI",
					ps.getPid(), p.toString()));
			return (double) getFromRemote(p);
		}

		return getStorageArray()[field.getFlatIdx(toLocalPoint(p))];
	}

	public final void set(final int x, final int y, final double val) {
		set(new IntPoint(x, y), val);
	}

	public final void set(final IntPoint p, final double val) {
		// In this partition but not in ghost cells
		if (!inLocal(p))
			throw new IllegalArgumentException(
					String.format("PID %d set %s is out of local boundary", ps.getPid(), p.toString()));

		getStorageArray()[field.getFlatIdx(toLocalPoint(p))] = val;
	}
}
