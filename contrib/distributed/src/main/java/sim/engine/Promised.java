package sim.engine;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.Remote;

/**
	PROMISED is an interface for data which is promised to be available at a future time.
	Promised data is fulfilled by calling FULFILL, and accessed via GET, GETINT, or GETDOUBLE.
	You can check to see if the promise has been fulfilledyet with ISREADY.

**/


public interface Promised extends Remote
{
	/** Returns TRUE if the promised data has arrived and may be extracted. */
	public boolean isReady() throws RemoteException;

	/** Returns the data. This data is only valid if isReady() is TRUE. */
	public Serializable get() throws RemoteException;

	/**
	 * Returns the data, which should be an integer. This data is only valid if
	 * isReady() is TRUE.
	 */
	public int getInt() throws RemoteException;

	/**
	 * Returns the data, which should be a double. This data is only valid if
	 * isReady() is TRUE.
	 */
	public double getDouble() throws RemoteException;

	/** Provides the data and makes the promise ready. */
	public void fulfill(Serializable object) throws RemoteException;

	/** Copies the data and readiness from another promise. */
	public void setTo(Promise promise) throws RemoteException;
}
