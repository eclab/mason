package sim.field;

import mpi.MPIException;
import sim.engine.transport.PayloadWrapper;

/**
 * A synchronizable object used by DSimSate.
 * @author Carmine Spagnuolo
 *
 */

public interface Synchronizable {
	
	public void initRemote();

	public void syncHalo() throws MPIException;
	
	public void syncObject(PayloadWrapper payloadWrapper);
	

}
