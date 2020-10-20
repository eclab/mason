package sim.field.partitioning;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.function.Consumer;
import mpi.*;
import sim.util.*;

// Consumer is Raw Type because it's parameter is of type int
/**
 * An interface for dividing the world into multiple partitions. Each partition
 * then gets assigned to a node.
 */
@SuppressWarnings("rawtypes")
public abstract class PartitionInterface {
	public int pid, numProcessors;
	public int[] size;
	boolean isToroidal;
	public Comm comm;
	public int[] aoi;

	ArrayList<Consumer> preCallbacks, postCallbacks;

	PartitionInterface(final int[] size, final boolean isToroidal, final int[] aoi) {
		//numDimensions = size.length;
		this.size = (int[])(size.clone());
		this.size = Arrays.copyOf(size, 2);
		this.isToroidal = isToroidal;
		this.aoi = (int[])(aoi.clone());

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

	public int getPid() {
		return pid;
	}

	public int getNumProc() {
		return numProcessors;
	}

	public boolean isToroidal() {
		return isToroidal;
	}

	public Comm getCommunicator() {
		return comm;
	}

	public int[] getFieldSize() {
		return (int[])(size.clone());
	}

	public IntHyperRect getWorldBounds() {
		return new IntHyperRect(size);
	}

	/**
	 * @return partition for the current node
	 */
	public abstract IntHyperRect getBounds();

	/**
	 * @param pid
	 * @return partition for pid node
	 */
	public abstract IntHyperRect getBounds(int pid);

	public abstract ArrayList<IntHyperRect> getAllBounds();
	
	public abstract int getNumNeighbors();

	public abstract int[] getNeighborIds();
	// public abstract int[][] getNeighborIdsInOrder();

	/**
	 * @param p
	 * @return partition id (pid) for the point p
	 */
	public abstract int toPartitionId(NumberND p);

	/**
	 * @param c point as an int array
	 * @return partition id (pid) for the point c[]
	 */
	public abstract int toPartitionId(int[] c);

	/**
	 * @param c point as an double array
	 * @return partition id (pid) for the point c[]
	 */
	public abstract int toPartitionId(double[] c);

	/**
	 *
	 * @return true if calling pid is the global root
	 */
	public abstract boolean isGlobalMaster();

	// TODO let other classes who depend on the partition scheme to register proper
	// actions when partiton changes
	/**
	 * Register pre commit callbacks
	 * 
	 * @param r
	 */
	public void registerPreCommit(final Consumer r) {
		preCallbacks.add(r);
	}

	/**
	 * Register post commit callbacks
	 * 
	 * @param r
	 */
	public void registerPostCommit(final Consumer r) {
		postCallbacks.add(r);
	}

	/**
	 * Initialize partition
	 */
	public abstract void initialize(); // How to initialize the partition
}
