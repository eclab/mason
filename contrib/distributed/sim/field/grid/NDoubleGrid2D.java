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
import sim.field.storage.DoubleGridStorage;

public class NDoubleGrid2D extends HaloField {

	public NDoubleGrid2D(DPartition ps, int[] aoi, double initVal) {
		super(ps, aoi, new DoubleGridStorage(ps.getPartition(), initVal));

		if (this.nd != 2)
			throw new IllegalArgumentException("The number of dimensions is expected to be 2, got: " + this.nd);
	}

	public double[] getStorageArray() {
		return (double[])field.getStorage();
	}

	// Use Double instead of double because of the serializable type
	public Double getRMI(IntPoint p) throws RemoteException {
		if (!inLocal(p))
			throw new RemoteException("The point " + p + " does not exist in this partition " + ps.getPid() + " " + ps.getPartition());

		return getStorageArray()[field.getFlatIdx(toLocalPoint(p))];
	}

	public final double get(final int x, final int y) {
		return get(new IntPoint(x, y));
	}

	public final double get(IntPoint p) {
		if (!inLocalAndHalo(p)) {
			System.out.println(String.format("PID %d get %s is out of local boundary, accessing remotely through RMI", ps.getPid(), p.toString()));
			return (double)getFromRemote(p);
		}

		return getStorageArray()[field.getFlatIdx(toLocalPoint(p))];
	}

	public final void set(final int x, final int y, final double val) {
		set(new IntPoint(x, y), val);
	}

	public final void set(IntPoint p, final double val) {
		// In this partition but not in ghost cells
		if (!inLocal(p))
			throw new IllegalArgumentException(String.format("PID %d set %s is out of local boundary", ps.getPid(), p.toString()));

		getStorageArray()[field.getFlatIdx(toLocalPoint(p))] = val;
	}

	public static void main(String args[]) throws MPIException, IOException {
		int[] size = new int[] {10, 10};
		int[] aoi = new int[] {1, 1};

		MPI.Init(args);

		/**
		* Create the following partition scheme
		*
		*	 0		4		7			10
		*	0 ---------------------------
		*	  |				|			|
		*	  |		P0		|	  P1	|
		*	5 |-------------------------|
		*	  |		|					|
		*	  |	P2	|		 P3			|
		*  10 ---------------------------
		*
		**/

		DNonUniformPartition p = DNonUniformPartition.getPartitionScheme(size, true, aoi);

		assert p.np == 4;

		p.insertPartition(new IntHyperRect(0, new IntPoint(new int[] {0, 0}), new IntPoint(new int[] {5, 7})));
		p.insertPartition(new IntHyperRect(1, new IntPoint(new int[] {0, 7}), new IntPoint(new int[] {5, 10})));
		p.insertPartition(new IntHyperRect(2, new IntPoint(new int[] {5, 0}), new IntPoint(new int[] {10, 4})));
		p.insertPartition(new IntHyperRect(3, new IntPoint(new int[] {5, 4}), new IntPoint(new int[] {10, 10})));
		p.commit();

		NDoubleGrid2D hf = new NDoubleGrid2D(p, aoi, p.pid);

		assert hf.inGlobal(new IntPoint(new int[] {5, 8}));
		assert !hf.inGlobal(new IntPoint(new int[] { -3, 0}));
		assert !hf.inGlobal(new IntPoint(new int[] {7, 240}));

		MPITest.execOnlyIn(0, i -> {
			assert hf.inLocal(new IntPoint(new int[] {0, 6}));
			assert !hf.inLocal(new IntPoint(new int[] {5, 0}));

			assert hf.inPrivate(new IntPoint(new int[] {1, 1}));
			assert !hf.inPrivate(new IntPoint(new int[] {0, 0}));
			assert !hf.inPrivate(new IntPoint(new int[] {5, 7}));

			assert hf.inShared(new IntPoint(new int[] {4, 6}));
			assert !hf.inShared(new IntPoint(new int[] {50, 100}));
			assert !hf.inShared(new IntPoint(new int[] {-1, 0}));

			assert hf.inHalo(new IntPoint(new int[] {-1, -1}));
			assert hf.inHalo(new IntPoint(new int[] {5, 6}));
			assert !hf.inHalo(new IntPoint(new int[] {7, 6}));
		});

		hf.sync();

		MPITest.execOnlyIn(0, i -> System.out.println("After Sync..."));
		MPITest.execInOrder(i -> System.out.println(hf), 500);

		DoubleGridStorage allStor = p.pid == 0 ? new DoubleGridStorage(p.getField(), p.pid) : null;
		hf.collect(0, allStor);
		MPITest.execOnlyIn(0, i -> System.out.println("Entire Field\n" + allStor));

		MPITest.execOnlyIn(0, i -> System.out.println("\nTest Repartitioning #1...\n"));
		/**
		* Change the partition to the following
		*
		*	 0		   5 6	 			10
		*	0 ---------------------------
		*	  |			 | <-			|
		*	  |		P0	 | <- 	 P1		|
		*	5 |-------------------------|
		*	  |		 ->|				|
		*	  |	P2	 ->|		 P3		|
		*  10 ---------------------------
		*
		**/
		p.updatePartition(new IntHyperRect(0, new IntPoint(new int[] {0, 0}), new IntPoint(new int[] {5, 6})));
		p.updatePartition(new IntHyperRect(1, new IntPoint(new int[] {0, 6}), new IntPoint(new int[] {5, 10})));
		p.updatePartition(new IntHyperRect(2, new IntPoint(new int[] {5, 0}), new IntPoint(new int[] {10, 5})));
		p.updatePartition(new IntHyperRect(3, new IntPoint(new int[] {5, 5}), new IntPoint(new int[] {10, 10})));
		p.commit();

		hf.reload();
		hf.sync();

		MPITest.execInOrder(i -> System.out.println(hf), 500);

		MPITest.execOnlyIn(0, i -> System.out.println("\nTest Repartitioning #2...\n"));
		/**
		* Change the partition to the following
		*
		*	 0		     6	 			10
		*	0 ---------------------------
		*	  |			 | 				|
		*	  |		P0	 | 	 	 P1		|
		*	5 |-------------------------|
		*	  |		  -> |				|
		*	  |	P2	  -> |		 P3		|
		*  10 ---------------------------
		*
		**/
		p.updatePartition(new IntHyperRect(2, new IntPoint(new int[] {5, 0}), new IntPoint(new int[] {10, 6})));
		p.updatePartition(new IntHyperRect(3, new IntPoint(new int[] {5, 6}), new IntPoint(new int[] {10, 10})));
		p.commit();

		hf.reload();
		hf.sync();

		MPITest.execInOrder(i -> System.out.println(hf), 500);

		MPITest.execOnlyIn(0, i -> System.out.println("\nTest Repartitioning #3...\n"));
		/**
		* Change the partition to the following
		*
		*	 0		  	 6	 			10
		*	0 ---------------------------
		*	  |		P0	 | 				|
		*	4 |----------| 	 	P1		|
		*	  |		^^	 |	||			|
		*	6 |		 	 |--------------|
		*	  |	P2	 	 |		 P3		|
		*  10 ---------------------------
		*
		**/
		p.updatePartition(new IntHyperRect(0, new IntPoint(new int[] {0, 0}), new IntPoint(new int[] {4, 6})));
		p.updatePartition(new IntHyperRect(1, new IntPoint(new int[] {0, 6}), new IntPoint(new int[] {6, 10})));
		p.updatePartition(new IntHyperRect(2, new IntPoint(new int[] {4, 0}), new IntPoint(new int[] {10, 6})));
		p.updatePartition(new IntHyperRect(3, new IntPoint(new int[] {6, 6}), new IntPoint(new int[] {10, 10})));
		p.commit();

		hf.reload();
		hf.sync();

		MPITest.execInOrder(i -> System.out.println(hf), 500);

		MPI.Finalize();
	}
}
