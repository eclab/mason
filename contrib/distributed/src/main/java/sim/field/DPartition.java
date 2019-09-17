package sim.field;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.function.Consumer;
import mpi.*;

import sim.util.IntHyperRect;
import sim.util.NdPoint;

// Consumer is Raw Type because it's parameter is of type int
@SuppressWarnings("rawtypes")
public abstract class DPartition {

	public int pid, numProcessors, numDimensions;
	public int[] size;
	boolean isToroidal;
	public Comm comm;
	public int[] aoi;

	ArrayList<Consumer> preCallbacks, postCallbacks;

	DPartition(final int[] size, final boolean isToroidal, final int[] aoi) {
		numDimensions = size.length;
		this.size = Arrays.copyOf(size, numDimensions);
		this.isToroidal = isToroidal;
		this.aoi = aoi;

		try {
			pid = MPI.COMM_WORLD.getRank();
			numProcessors = MPI.COMM_WORLD.getSize();
		} catch (final MPIException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		preCallbacks = new ArrayList<Consumer>();
		postCallbacks = new ArrayList<Consumer>();
	}

	// TODO move the neighbor comm init to here
	// protected void setNeighborComm() {
	// int[] nids = getNeighborIds();

	// try {
	// comm = MPI.COMM_WORLD.createDistGraphAdjacent(ns, ns, new Info(), false);
	// } catch (MPIException e) {
	// e.printStackTrace();
	// System.exit(-1);
	// }
	// }

	public int getPid() {
		return pid;
	}

	public int getNumProc() {
		return numProcessors;
	}

	public int getNumDim() {
		return numDimensions;
	}

	public boolean isToroidal() {
		return isToroidal;
	}

	public Comm getCommunicator() {
		return comm;
	}

	public int[] getFieldSize() {
		return Arrays.copyOf(size, numDimensions);
	}

	public IntHyperRect getField() {
		return new IntHyperRect(size);
	}

	public abstract IntHyperRect getPartition();

	public abstract IntHyperRect getPartition(int pid);

	public abstract int getNumNeighbors();

	public abstract int[] getNeighborIds();
	// public abstract int[][] getNeighborIdsInOrder();

	public abstract int toPartitionId(NdPoint p);

	public abstract int toPartitionId(int[] c);

	public abstract int toPartitionId(double[] c);

	/**
	 *
	 * @return true if calling pid is the global root
	 */
	public abstract boolean isGlobalMaster();

	// TODO let other classes who depend on the partition scheme to register proper
	// actions when partiton changes
	public void registerPreCommit(final Consumer r) {
		preCallbacks.add(r);
	}

	public void registerPostCommit(final Consumer r) {
		postCallbacks.add(r);
	}

	public abstract void initialize(); // How to initialize the partition
}
