package sim.field;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.function.Consumer;
import mpi.*;

import sim.util.IntHyperRect;
import sim.util.IntPoint;
import sim.util.NdPoint;

public abstract class DPartition {

	public int pid, np, nd;
	public int[] size;
	boolean isToroidal;
	public Comm comm;

	ArrayList<Consumer> preCallbacks, postCallbacks;

	DPartition(int[] size, boolean isToroidal) {
		this.nd = size.length;
		this.size = Arrays.copyOf(size, nd);
		this.isToroidal = isToroidal;

		try {
			pid = MPI.COMM_WORLD.getRank();
			np = MPI.COMM_WORLD.getSize();
		} catch (MPIException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		preCallbacks = new ArrayList<Consumer>();
		postCallbacks = new ArrayList<Consumer>();
	}

	// TODO move the neighbor comm init to here
	// protected void setNeighborComm() {
	// 	int[] nids = getNeighborIds();

	// 	try {
	// 		comm = MPI.COMM_WORLD.createDistGraphAdjacent(ns, ns, new Info(), false);
	// 	} catch (MPIException e) {
	// 		e.printStackTrace();
	// 		System.exit(-1);
	// 	}
	// }

	public int getPid() {
		return pid;
	}

	public int getNumProc() {
		return np;
	}

	public int getNumDim() {
		return nd;
	}

	public boolean isToroidal() {
		return isToroidal;
	}

	public Comm getCommunicator() {
		return comm;
	}

	public int[] getFieldSize() {
		return Arrays.copyOf(size, nd);
	}

	public IntHyperRect getField() {
		return new IntHyperRect(size);
	}

	public abstract IntHyperRect getPartition();
	public abstract IntHyperRect getPartition(int pid);

	public abstract int getNumNeighbors();
	public abstract int[] getNeighborIds();
	//public abstract int[][] getNeighborIdsInOrder();

	public abstract int toPartitionId(NdPoint p);
	public abstract int toPartitionId(int[] c);
	public abstract int toPartitionId(double[] c);

	// TODO let other classes who depend on the partition scheme to register proper actions when partiton changes
	public void registerPreCommit(Consumer r) {
		preCallbacks.add(r);
	}

	public void registerPostCommit(Consumer r) {
		postCallbacks.add(r);
	}
}