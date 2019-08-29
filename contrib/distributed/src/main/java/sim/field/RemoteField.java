package sim.field;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

import sim.util.NdPoint;

/**
 * Implemented by fields that will be used for RMI
 *
 *
 * @param <P> The Type of NdPoint to use
 * @param <T> The Type of Object in the field
 */
public interface RemoteField<T extends Serializable, P extends NdPoint> extends Remote {

	/**
	 * Used internally for RMI
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

	/**
	 * Used internally for RMI
	 *
	 * @param p location
	 * @param t Object
	 *
	 * @throws RemoteException If the point requested is not local to the remote
	 *                         field
	 */
	void addRMI(P p, T t) throws RemoteException;

	/**
	 * Used internally for RMI
	 *
	 * @param p location
	 * @param t Object
	 *
	 * @throws RemoteException If the point requested is not local to the remote
	 *                         field
	 */
	void removeRMI(P p, T t) throws RemoteException;

	/**
	 * Used internally for RMI
	 *
	 * @param p location
	 *
	 * @throws RemoteException If the point requested is not local to the remote
	 *                         field
	 */
	void removeRMI(P p) throws RemoteException;

	/**
	 * Used internally for RMI
	 *
	 * @param fromP
	 * @param toP
	 * @param t
	 *
	 * @throws RemoteException If the points are not local to the remote field
	 */
	default void moveRMI(final P fromP, final P toP, final T t) throws RemoteException {
		removeRMI(fromP, t);
		addRMI(toP, t);
	}
}
