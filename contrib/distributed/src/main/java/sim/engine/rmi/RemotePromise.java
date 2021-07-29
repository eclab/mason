package sim.engine.rmi;

import java.io.*;
import java.rmi.server.UnicastRemoteObject;
import sim.engine.*;
import java.rmi.*;

public class RemotePromise extends UnicastRemoteObject implements Promised {
	private static final long serialVersionUID = 1L;

	boolean ready = false;
	Serializable object = null;
	public int tag; //tag used to understand which method to use to fill the promise
	public Serializable args; // optional arguments that could be needed
	public String promiseId; // id used to register the promise on the registry

	public void setPromiseId(String id){
		this.promiseId = id;
	}
	
	public String getPromiseId(){
		return promiseId;
	}

	public int getTag(){
		return tag;
	}
	
	public Serializable getArgs(){
		return args;
	}

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
	 * Constructs an unfulfilled promise
	 * 
	 * @throws RemoteException
	 */
	public RemotePromise(int tag, Serializable args, String id) throws RemoteException{
		this.tag = tag;
		this.args = args;
		this.promiseId = id;
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
