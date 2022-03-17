package sim.engine.rmi;

import java.io.Serializable;
import java.rmi.RemoteException;
import sim.engine.Promise;
import sim.engine.Promised;

public class RemotePromise implements Promised, Serializable {
	private static final long serialVersionUID = 1L;

	boolean ready = false;
	Serializable object = null;

	/** Returns TRUE if the promised data is ready, else FALSE. */
	public boolean isReady() throws RemoteException
	{
		return ready;
	}

	/** Returns the data. This data is only valid if isReady() is TRUE. */
	public Serializable get() throws RemoteException {
		return object;
	}

	/**
	 * Returns the data, which should be an integer. This data is only valid if
	 * isReady() is TRUE.
	 */
	public int getInt() throws RemoteException {
		return (Integer) object;
	}

	/**
	 * Returns the data, which should be an double. This data is only valid if
	 * isReady() is TRUE.
	 */
	public double getDouble() throws RemoteException {
		return (Double) object;
	}

	/** Provides the data and makes the promise ready. */
	public void fulfill(Serializable object) throws RemoteException {
		ready = true;
		this.object = object;
	}

	/** Copies the data and readiness from another promise. */
	public void setTo(Promise promise) throws RemoteException {
		ready = promise.isReady();
		object = promise.get();
	}

	/**
	 * Constructs an unfulfilled promise
	 * 
	 * @throws RemoteException
	 */
	public RemotePromise() throws RemoteException {
		super();
	}

	/**
	 * Constructs an already fulfilled promise
	 * 
	 * @throws RemoteException
	 */
	public RemotePromise(Serializable object) throws RemoteException {
		super();
		this.object = object;
		ready = true;
	}

	/**
	 * Constructs an already fulfilled promise
	 * 
	 * @throws RemoteException
	 */
	public RemotePromise(int value) throws RemoteException {
		super();
		this.object = Integer.valueOf(value);
		ready = true;
	}

	/**
	 * Constructs an already fulfilled promise
	 * 
	 * @throws RemoteException
	 */
	public RemotePromise(double value) throws RemoteException {
		super();
		this.object = Double.valueOf(value);
		ready = true;
	}
}
