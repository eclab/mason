package sim.display;

import sim.engine.rmi.*;
import sim.field.partitioning.*;
import sim.field.storage.*;
import sim.util.*;
import sim.display.*;

import java.rmi.*;
import java.util.ArrayList;

/**
 * This is implemented by a single object for each processor and added to the
 * registry.
 */

public interface VisualizationProcessor extends Remote
{
	/**
	 * Blocks until the remote processor is in a state where we can safely grab
	 * storage objects without creating a race condition; then holds a lock to
	 * prevent the remote processor from continuing.
	 */
	public void lock() throws RemoteException;

	/**
	 * Releases the lock obtained by lock(), thus letting the remote processor
	 * continue its work.
	 */
	public void unlock() throws RemoteException;

	/** Returns the current bounds of the processor's local region. */

	public IntRect2D getStorageBounds() throws RemoteException;

	/** Returns a full copy of GridStorage object number STORAGE. */
	public GridStorage getStorage(int storage) throws RemoteException;

	/**
	 * Returns the number of processors in the distributed model. We presume their
	 * pids go 0...n
	 */
	public int getNumProcessors() throws RemoteException;

	/** Returns the world (non-toroidal) bounds of the distributed model */

	public IntRect2D getWorldBounds() throws RemoteException;
	
	//// WE ALSO NEED THE FOLLOWING, WHICH DO *NOT* REQUIRE LOCKING BECAUSE THEY ARE SYNCHRONIZED.
	//// JUST CALL THE SECHEDULE'S VERSION

	/** Returns the schedule's current steps */
	public long getSteps() throws RemoteException;

	/** Returns the schedule's current time */
	public double getTime() throws RemoteException;

	/// Not sure how to do this one -- grab information from the quad tree?
	/// The IntHyperRects in this method should return the LOCAL storage size, not
	/// including the halo rect, I think.
	public ArrayList<IntRect2D> getAllLocalBounds() throws RemoteException;

	/// Need this one too
	public int getAOI() throws RemoteException;

	public int getProcessorLevels() throws RemoteException;

	public int[] getProcessorNeighborhood(int level) throws RemoteException;
	
	//Raj: input pids and get all neighbors in the lowest point in quadtree that contains inputed pids
	public int[] getMinimumNeighborhood(int[] proc_ids) throws RemoteException;

	public ArrayList<Stat> getStatList() throws RemoteException;
	
	public ArrayList<Stat> getDebugList() throws RemoteException;
	
	public void initStat() throws RemoteException;
	
	public void initDebug() throws RemoteException;
	
	public void stopStat() throws RemoteException;
	
	public void stopDebug() throws RemoteException;

	// TODO: Adding here for now
	// Not sure if one class can implement two remote interfaces
	public TransportRMIInterface getTransportRMI(int fieldId) throws RemoteException;

}