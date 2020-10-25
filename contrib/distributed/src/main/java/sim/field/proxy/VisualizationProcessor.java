package sim.field.proxy;

import sim.field.partitioning.*;
import sim.field.storage.*;
import java.rmi.*;

/**
 * This is implemented by a single object for each processor and added to the
 * registry.
 */

public interface VisualizationProcessor extends Remote {
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
	public IntRect2D getBounds() throws RemoteException;

	/** Returns a full copy of GridStorage object number STORAGE. */
	public GridStorage getStorage(int storage) throws RemoteException;

	/**
	 * Returns the number of processors in the distributed model. We presume their
	 * pids go 0...n
	 */
	public int getNumProcessors() throws RemoteException;

	/** Returns the world (non-toroidal) bounds of the distributed model */
	public IntRect2D getWorldBounds() throws RemoteException;
}