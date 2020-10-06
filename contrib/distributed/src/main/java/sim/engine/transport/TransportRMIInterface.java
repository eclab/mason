package sim.engine.transport;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Implemented by fields that will be used for RMI
 *
 *
 * @param <P> The Type of PointND to use
 * @param <T> The Type of Object in the field
 */
public abstract class TransportRMIInterface<T extends Serializable, P> implements Remote {
	protected final Object lockRMI = new Object();

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
	public abstract Serializable getRMI(P p) throws RemoteException;

	/**
	 * Used internally for RMI
	 *
	 * @param p location
	 * @param t Object
	 *
	 * @throws RemoteException If the point requested is not local to the remote
	 *                         field
	 */
	public abstract void addRMI(P p, T t) throws RemoteException;

	/**
	 * Used internally for RMI
	 *
	 * @param p location
	 * @param t Object
	 *
	 * @throws RemoteException If the point requested is not local to the remote
	 *                         field
	 */
	public abstract void removeRMI(P p, T t) throws RemoteException;

	/**
	 * Used internally for RMI
	 *
	 * @param p location
	 *
	 * @throws RemoteException If the point requested is not local to the remote
	 *                         field
	 */
	public abstract void removeRMI(P p) throws RemoteException;

	/**
	 * Used internally for RMI
	 *
	 * @param fromP
	 * @param toP
	 * @param t
	 *
	 * @throws RemoteException If the points are not local to the remote field
	 */
	public void moveRMI(final P fromP, final P toP, final T t) throws RemoteException {
		synchronized (lockRMI) {
			removeRMI(fromP, t);
			addRMI(toP, t);
		}
	}
}
