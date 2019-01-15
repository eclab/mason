package sim.field;

import java.util.*;
import mpi.*;

/*
 * partition the field into nodes in the graph
 */
public class DUniformPartition {

	// Field sizes, number of dimensions, number of LPs, and PID
	public int size[], nd, np, pid;
	// Partitions in each dimension and this partition's coordinates
	public int dims[], coords[];
	// Number of extended neighbors (plus diagonal ones) and number of direct neighbors
	public int totalNeighbors, nNeighbors;

	public CartComm comm;

	public DUniformPartition(final int[] size) {
		CartParms topoParams = null;

		this.nd = size.length;
		this.size = Arrays.copyOf(size, nd);

		dims = new int[nd]; // 
		coords = new int[nd];

		boolean[] periods = new boolean[nd];
		Arrays.fill(periods, Boolean.TRUE);

		try {
			pid = MPI.COMM_WORLD.getRank();
			np = MPI.COMM_WORLD.getSize();
			CartComm.createDims(np, dims);
			comm = ((Intracomm)MPI.COMM_WORLD).createCart(dims, periods, false);
			topoParams = comm.getTopo();
		} catch (MPIException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		for (int i = 0; i < nd; i++)
			coords[i] = topoParams.getCoord(i);

		totalNeighbors = (int)Math.pow(3, nd) - 1;
		nNeighbors = 2 * nd;
	}

	// public int toPartitionId(final int x, final int y) throws MPIException {
	// 	int px = x / size[0] * dims[0];
	// 	int py = y / size[1] * dims[1];
	// 	return comm.getRank(new int[] {px, py});
	// }

	// global int coordinates to partition id
	public int toPartitionId(final int[] c) throws MPIException {
		int[] pc = new int[nd];

		if (c.length != nd)
			throw new IllegalArgumentException(String.format("Number of dimensions does not match: want %d got %d", nd, c.length));

		for (int i = 0; i < nd; i++)
			pc[i] = c[i] / (size[i] / dims[i]);

		return comm.getRank(pc);
	}

	// global double coordinates to partition id
	public int toPartitionId(final double[] c) throws MPIException {
		int[] pc = new int[nd];

		if (c.length != nd)
			throw new IllegalArgumentException(String.format("Number of dimensions does not match: want %d got %d", nd, c.length));

		for (int i = 0; i < nd; i++)
			pc[i] = (int)(c[i] / (double)(size[i] / dims[i]));

		return comm.getRank(pc);
	}

	/**
	 * Find the ids of direct neighbors.
	 * @return
	 * @throws MPIException
	 */
	public int[] getNeighborIds() throws MPIException {
		int[] ret = new int[nNeighbors];
		for (int d = 0, i = 0; d < nd; d++) {
			coords[d] -= 1; ret[i++] = comm.getRank(coords); coords[d] += 1;
			coords[d] += 1; ret[i++] = comm.getRank(coords); coords[d] -= 1;
		}
		return ret;
	}

	// public int[] getExtendedNeighborIds() throws MPIException {
	// 	int[] ret = new int[totalNeighbors - nNeighbors];
	// 	ret[0] = comm.getRank(new int[] {(coords[0] - 1) % dims[0], (coords[1] - 1) % dims[1]});
	// 	ret[1] = comm.getRank(new int[] {(coords[0] + 1) % dims[0], (coords[1] + 1) % dims[1]});
	// 	ret[2] = comm.getRank(new int[] {(coords[0] - 1) % dims[0], (coords[1] + 1) % dims[1]});
	// 	ret[3] = comm.getRank(new int[] {(coords[0] + 1) % dims[0], (coords[1] - 1) % dims[1]});
	// 	return ret;
	// }

	/**
	 * Find the id's of all neighbors (including diagonals)
	 * @param includeDirectNeighbor
	 * @return
	 * @throws MPIException
	 */
	public int[] getExtendedNeighborIds(boolean includeDirectNeighbor) throws MPIException {
		ArrayList<Integer> l = new ArrayList<Integer>();
		getExtendedNeighborIdsRecursive(l, coords, 0, includeDirectNeighbor);
		if (includeDirectNeighbor)
			l.remove(0); // remove self;
		return l.stream().mapToInt(i->i).toArray();
	}

	private void getExtendedNeighborIdsRecursive(ArrayList<Integer> l, int[] c, int d, boolean includeDirectNeighbor) throws MPIException {
		if (d == nd) {
			l.add(comm.getRank(c));
			return;
		}
		if (includeDirectNeighbor)
			getExtendedNeighborIdsRecursive(l, c, d + 1, includeDirectNeighbor);
		c[d] -= 1; getExtendedNeighborIdsRecursive(l, c, d + 1, includeDirectNeighbor); c[d] += 1;
		c[d] += 1; getExtendedNeighborIdsRecursive(l, c, d + 1, includeDirectNeighbor); c[d] -= 1;
	}

	/**
	 * Find id's of neighbors by shift.
	 * @param dim
	 * @param shift
	 * @return
	 * @throws MPIException
	 */
	public int[] getExtendedNeighborsByShift(int dim, int shift) throws MPIException {
		if (shift > 1 || shift < -1)
			throw new IllegalArgumentException(String.format("Shift can only be 1, 0 or -1: got %d", shift));

		ArrayList<Integer> l = new ArrayList<Integer>();
		getExtendedNeighborsByShiftRecursive(l, coords, 0, dim, shift);
		return l.stream().mapToInt(i->i).toArray();
	}

	private void getExtendedNeighborsByShiftRecursive(ArrayList<Integer> l, int[] c, int d, int dfix, int shift) throws MPIException {
		if (d == nd) {
			l.add(comm.getRank(c));
			return;
		}
		if (d == dfix) {
			c[d] += shift; getExtendedNeighborsByShiftRecursive(l, c, d + 1, dfix, shift); c[d] -= shift;
		} else {
			getExtendedNeighborsByShiftRecursive(l, c, d + 1, dfix, shift);
			c[d] -= 1; getExtendedNeighborsByShiftRecursive(l, c, d + 1, dfix, shift); c[d] += 1;
			c[d] += 1; getExtendedNeighborsByShiftRecursive(l, c, d + 1, dfix, shift); c[d] -= 1;
		}
	}

	// private static void printArray(int[] a) {
	// 	for (int i = 0; i < a.length; i++) {
	// 		if (i > 0) {
	// 			System.out.print(", ");
	// 		}
	// 		System.out.print(a[i]);
	// 	}
	// 	System.out.println();
	// }

	public static void main(String args[]) throws MPIException {
		MPI.Init(args);

		// Test 2D
		DUniformPartition dp = new DUniformPartition(new int[] {32, 32});

		assert dp.np == 4; // assume 4 LPs

		assert dp.toPartitionId(new int[] {0, 0}) == 0;
		assert dp.toPartitionId(new int[] {0, 16}) == 1;
		assert dp.toPartitionId(new int[] {16, 0}) == 2;
		assert dp.toPartitionId(new int[] {16, 16}) == 3;

		if (dp.pid == 0) {
			int[] want = new int[] {2, 2, 1, 1};
			int[] got = dp.getNeighborIds();
			assert Arrays.equals(want, got);
			assert want.length == dp.nNeighbors;

			want = new int[] {1, 1, 2, 3, 3, 2, 3, 3};
			got = dp.getExtendedNeighborIds(true);
			assert Arrays.equals(want, got);
			assert want.length == dp.totalNeighbors;

			want = new int[] {2, 3, 3};
			got = dp.getExtendedNeighborsByShift(0, 1);
			assert Arrays.equals(want, got);
		} else if (dp.pid == 1) {
			int[] want = new int[] {3, 3, 0, 0};
			int[] got = dp.getNeighborIds();
			assert Arrays.equals(want, got);
			assert want.length == dp.nNeighbors;

			want = new int[] {2, 2, 2, 2};
			got = dp.getExtendedNeighborIds(false);
			assert Arrays.equals(want, got);
			assert want.length == dp.totalNeighbors - dp.nNeighbors;
		}

		MPI.Finalize();
	}
}