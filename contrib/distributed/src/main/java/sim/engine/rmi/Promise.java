package sim.engine.rmi;

import java.io.*;
import java.rmi.RemoteException;

/**
 * This class eventually provides data in the future (usually one MASON timestep
 * away). It is in many ways like a simplified and easier to use version of
 * java.util.concurrent.Future.
 */

public class Promise implements Serializable, Promised {
	private static final long serialVersionUID = 1L;

	boolean ready = false;
	Serializable object = null;

	/** Returns TRUE if the promised data is ready, else FALSE. */
	public boolean isReady() {
		return ready;
	}

	/** Returns the data. This data is only valid if isReady() is TRUE. */
	public Serializable get() {
		return object;
	}

	/**
	 * Returns the data, which should be an integer. This data is only valid if
	 * isReady() is TRUE.
	 */
	public int getInt() {
		return (Integer) object;
	}

	/**
	 * Returns the data, which should be an double. This data is only valid if
	 * isReady() is TRUE.
	 */
	public double getDouble() {
		return (Double) object;
	}

	/** Provides the data and makes the promise ready. */
	public void fulfill(Serializable object) {
		ready = true;
		this.object = object;
	}

	/** Copies the data and readiness from another promise. */
	public void setTo(Promise promise) {
		ready = promise.ready;
		object = promise.object;
	}

	/**
	 * Constructs an unfulfilled promise
	 * 
	 * @throws RemoteException
	 */
	public Promise() throws RemoteException {
	}

	/**
	 * Constructs an already fulfilled promise
	 * 
	 * @throws RemoteException
	 */
	public Promise(Serializable object) throws RemoteException {
		this.object = object;
		ready = true;
	}

	/**
	 * Constructs an already fulfilled promise
	 * 
	 * @throws RemoteException
	 */
	public Promise(int value) throws RemoteException {
		this.object = Integer.valueOf(value);
		ready = true;
	}

	/**
	 * Constructs an already fulfilled promise
	 * 
	 * @throws RemoteException
	 */
	public Promise(double value) throws RemoteException {
		this.object = Double.valueOf(value);
		ready = true;
	}
}
