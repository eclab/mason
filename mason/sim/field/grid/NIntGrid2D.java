package sim.field.grid;

import java.io.IOException;
import java.rmi.RemoteException;

import mpi.*;

import sim.util.IntPoint;
import sim.util.IntHyperRect;
import sim.util.MPITest;
import sim.field.DPartition;
import sim.field.DNonUniformPartition;
import sim.field.HaloField;
import sim.field.storage.IntGridStorage;

public class NIntGrid2D extends HaloField {

	public NIntGrid2D(DPartition ps, int[] aoi, int initVal) {
		super(ps, aoi, new IntGridStorage(ps.getPartition(), initVal));

		if (this.nd != 2)
			throw new IllegalArgumentException("The number of dimensions is expected to be 2, got: " + this.nd);
	}

	public int[] getStorageArray() {
		return (int[])field.getStorage();
	}

	public Integer getRMI(IntPoint p) throws RemoteException {
		if (!inLocal(p))
			throw new RemoteException("The point " + p + " does not exist in this partition " + ps.getPid() + " " + ps.getPartition());

		return getStorageArray()[field.getFlatIdx(toLocalPoint(p))];
	}

	public final int get(final int x, final int y) {
		return get(new IntPoint(x, y));
	}

	public final int get(IntPoint p) {
		if (!inLocalAndHalo(p)) {
			System.out.println(String.format("PID %d get %s is out of local boundary, accessing remotely through RMI", ps.getPid(), p.toString()));
			return (int)getFromRemote(p);
		}

		return getStorageArray()[field.getFlatIdx(toLocalPoint(p))];
	}

	public final void set(final int x, final int y, final int val) {
		set(new IntPoint(x, y), val);
	}

	public final void set(IntPoint p, final int val) {
		// In this partition but not in ghost cells
		if (!inLocal(p))
			throw new IllegalArgumentException(String.format("PID %d set %s is out of local boundary", ps.getPid(), p.toString()));

		getStorageArray()[field.getFlatIdx(toLocalPoint(p))] = val;
	}

	public static void main(String[] args) throws MPIException, IOException {
		MPI.Init(args);

		int[] aoi = new int[] {2, 2};
		int[] size = new int[] {10, 10};

		DNonUniformPartition p = DNonUniformPartition.getPartitionScheme(size, true, aoi);
		p.initUniformly(null);
		p.commit();

		NIntGrid2D f = new NIntGrid2D(p, aoi, p.getPid());

		f.sync();

		MPITest.execInOrder(i -> System.out.println(f), 500);

		MPITest.execOnlyIn(0, i -> System.out.println("Testing RMI remote calls"));
		sim.field.RemoteProxy.Init(0);
		f.initRemote();

		// Choose the points that are out of halo area
		int pid = p.getPid();
		int x = f.stx(2 + 5 * ((pid + 1) / 2));
		int y = f.sty(2 + 5 * ((pid + 1) % 2));
		MPITest.execInOrder(i -> System.out.println(String.format("PID %d accessing <%d, %d> result %d", i, x, y, f.get(x, y))), 200);

		sim.field.RemoteProxy.Finalize();
		MPI.Finalize();
	}
}
