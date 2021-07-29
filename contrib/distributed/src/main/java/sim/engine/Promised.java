package sim.engine;

import java.io.*;
import java.rmi.*;

public interface Promised extends Remote
{
	
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
