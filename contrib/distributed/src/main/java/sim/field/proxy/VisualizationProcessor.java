package sim.field.proxy;
import sim.field.partitioning.*;
import sim.field.storage.*;
import java.rmi.*;

/**
	This is implemented by a single object for each processor and added to the registry.
*/

public interface VisualizationProcessor extends Remote
	{
	/** Blocks until the remote processor is in a state where we can safely grab storage objects
		without creating a race condition; then holds a lock to prevent the remote processor from
		continuing. */
	public void lock() throws RemoteException;
	
	/** Releases the lock obtained by lock(), thus letting the remote processor continue its work. */
	public void unlock() throws RemoteException;
	
	/** Returns the current bounds of the processor's local region. */
	public IntHyperRect getBounds() throws RemoteException;

	/** Returns a full copy of GridStorage object number STORAGE. */
	public GridStorage getStorage(int storage) throws RemoteException;
	}
