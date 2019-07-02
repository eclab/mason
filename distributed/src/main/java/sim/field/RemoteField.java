package sim.field;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

import sim.util.NdPoint;

/**
 * Implemented by remote field
 *
 *
 * @param <P> The Type of NdPoint to use
 */
public interface RemoteField<P extends NdPoint> extends Remote {

	/**
	 * Used internally
	 *
	 * @param p location
	 *
	 * @return item(s) stored on the point p if and only if p is local to the remote
	 *         field
	 *
	 * @throws RemoteException If the point requested is not local to the remote
	 *                         field
	 */
	Serializable getRMI(P p) throws RemoteException;
}
