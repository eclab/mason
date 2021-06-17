package sim.field;

import java.rmi.RemoteException;

import mpi.MPIException;
import sim.engine.transport.PayloadWrapper;

/**
 * A synchronizable object used by DSimSate.
 * 
 * @author Carmine Spagnuolo
 *
 */

public interface Synchronizable
{

	/**
	 * Initializes remote proxy
	 * 
	 */
	public void initRemote();

	/**
	 * Sync all Halos
	 * 
	 * @throws MPIException
	 * @throws RemoteException
	 */
	public void syncHalo() throws MPIException, RemoteException;

	/**
	 * adds payload to Halo Grid (for when agents or objects are transported in)
	 * 
	 * @param payloadWrapper
	 */
	public void addPayload(PayloadWrapper payloadWrapper);

}
