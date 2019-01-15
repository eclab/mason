package sim.field.grid;

import java.io.IOException;
import java.rmi.RemoteException;

import mpi.*;

import sim.util.IntPoint;
import sim.util.IntHyperRect;
import sim.util.MPITest;
import sim.field.DPartition;
import sim.field.DQuadTreePartition;
import sim.field.HaloField;
import sim.field.storage.IntGridStorage;

public class NIntQTGrid2D extends HaloField {

	public NIntQTGrid2D(DPartition ps, int[] aoi, int initVal) {
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

		int[] aoi = new int[] {1, 1};

		DQuadTreePartition p = new DQuadTreePartition(new int[] {100, 100}, false, aoi);

		IntPoint[] splitPoints = new IntPoint[] {
		    new IntPoint(50, 50),
		    new IntPoint(25, 25),
		    new IntPoint(75, 75),
		    new IntPoint(60, 90),
		    new IntPoint(10, 10)
		};

		p.initQuadTree(java.util.Arrays.asList(splitPoints));

		NIntQTGrid2D f = new NIntQTGrid2D(p, aoi, p.getPid());

		int level = 1;

		IntHyperRect shape = p.getNodeShapeAtLevel(level);
		IntGridStorage s = null;
		if (shape != null)
			s = new IntGridStorage(shape, 0);
		
		f.collectGroup(level, s);

		if (shape != null) {
			System.out.println(s);
			int[] array = (int[])s.getStorage();
			array[0] = 999;
		}

		f.distributeGroup(level, s);

		MPITest.printInOrder(f.toString());

		java.util.Random rand = new java.util.Random();
		double myRt = rand.nextDouble() * 10;

		p.balance(myRt, 2);
		p.balance(myRt, 1);
		p.balance(myRt, 0);

		MPI.Finalize();
	}
}
