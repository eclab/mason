package sim.field.partitioning;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.function.Consumer;
import mpi.*;
import sim.util.*;

/**
 * An interface for dividing the world into multiple partitions. Each partition
 * then gets assigned to a node.
 */
// Consumer is Raw Type because its parameter is of type int
//@SuppressWarnings("rawtypes")
public abstract class Partition {
 	int pid;
	int numProcessors;
	int width;
	int height;
	boolean toroidal;
	protected Comm comm;
	protected int aoi;

	ArrayList<Consumer> preCallbacks, postCallbacks;

	Partition(int width, int height, boolean toroidal, int aoi) {
		this.width = width;
		this.height = height;
		this.toroidal = toroidal;
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

	public int getPID() {
		return pid;
	}

	public int getNumProcessors() {
		return numProcessors;
	}

	public boolean isToroidal() {
		return toroidal;
	}

	public Comm getCommunicator() {
		return comm;
	}

	public int[] getFieldSize() {
		return new int[] { width, height };
	}

	public IntRect2D getWorldBounds() {
		return new IntRect2D(width, height);
	}

	/**
	 * @return partition for the current node
	 */
	public abstract IntRect2D getBounds();

	/**
	 * @param pid
	 * @return partition for pid node
	 */
	public abstract IntRect2D getBounds(int pid);

	public abstract IntRect2D getHaloBounds();
	
	public abstract ArrayList<IntRect2D> getAllBounds();
	
	public abstract int getNumNeighbors();

	public abstract int[] getNeighborIds();
	
	public int getAOI() { return aoi; }

	/**
	 * @param p
	 * @return partition id (pid) for the point p
	 */
	public abstract int toPartitionId(NumberND p);

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
}
