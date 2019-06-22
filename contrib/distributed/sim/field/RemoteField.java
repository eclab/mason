package sim.field;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

import sim.util.NdPoint;

/**
 * Contains a method to get data from remote field
 *
 *
 * @param <P> The Type of NdPoint to use
 */
public interface RemoteField<P extends NdPoint> extends Remote {

	/**
	 * Used internally, returns item(s) stored on the point p if and only if p is
	 * local to the remote field
	 *
	 * @param p location
	 *
	 * @return the item(s) stored at the location p
	 *
	 * @throws RemoteException If the point requested is not <b>local</B> to the
	 *                         <b>remote</B> field
	 */
	Serializable getRMI(P p) throws RemoteException;
}
