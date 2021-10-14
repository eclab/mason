package sim.engine.rmi;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;


public interface GridRMI<T extends Serializable, P> extends Remote
{
	/**
	 * Used internally for RMI
	 *
	 * @param p       location
	 * @param promise
	 *
	 * @return item(s) stored on the point p if and only if p is local to the remote
	 *         field
	 *
	 * @throws RemoteException If the point requested is not local to the remote
	 *                         field
	 */
	public abstract void getRMI(P p, RemotePromise promise) throws RemoteException;

	public abstract void getRMI(P p, long id, RemotePromise promise) throws RemoteException;

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
	 * @param t Agent
	 * @param ordering
	 * @param time
	 * @throws RemoteException If the point requested is not local to the remote
	 *                         field
	 */
	public abstract void addRMI(P p, T t, int ordering, double time) throws RemoteException;
	
	/**
	 * Used internally for RMI
	 *
	 * @param p location
	 * @param t Agent
	 * @param ordering
	 * @param time
	 * @param interval
	 * @throws RemoteException If the point requested is not local to the remote
	 *                         field
	 */
	public abstract void addRMI(P p, T t, int ordering, double time, double interval) throws RemoteException;

	/**
	 * Used internally for RMI
	 *
	 * @param p location
	 * @param t Object
	 *
	 * @throws RemoteException If the point requested is not local to the remote
	 *                         field
	 */
	public abstract void removeRMI(P p, long id) throws RemoteException;

	/**
	 * Used internally for RMI
	 *
	 * @param p location
	 *
	 * @throws RemoteException If the point requested is not local to the remote
	 *                         field
	 */
	public abstract void removeAllRMI(P p) throws RemoteException;
}
