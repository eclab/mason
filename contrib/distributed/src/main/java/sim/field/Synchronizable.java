package sim.field;

import mpi.MPIException;
import sim.engine.transport.PayloadWrapper;

/**
 * A synchronizable object used by DSimSate.
 * 
 * @author Carmine Spagnuolo
 *
 */

public interface Synchronizable {

	/**
	 * Initializes remote proxy
	 * 
	 */
	public void initRemote();

	/**
	 * Sync all Halos
	 * 
	 * @throws MPIException
	 */
	public void syncHalo() throws MPIException;

	/**
	 * adds payload to Halo Grid (for when agents or objects are transported in)
	 * 
	 * @param payloadWrapper
	 */
	public void syncObject(PayloadWrapper payloadWrapper);

}
