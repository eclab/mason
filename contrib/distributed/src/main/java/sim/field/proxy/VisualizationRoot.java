package sim.field.proxy;
import sim.field.partitioning.*;
import java.rmi.*;

/**
	This is implemented by some object owned by the root PID and registered with the registry.
*/

public interface VisualizationRoot extends Remote
	{
	/** Returns the number of processors in the distributed model.  We presume their pids go 0...n */
	public int getNumProcessors() throws RemoteException;

	/** Returns the world (non-toroidal) bounds of the distributed model */
	public IntHyperRect getWorldBounds() throws RemoteException;
	}
	