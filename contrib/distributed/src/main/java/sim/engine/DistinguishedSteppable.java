package sim.engine;

import java.io.Serializable;
import java.rmi.RemoteException;
import sim.engine.rmi.RemotePromise;


public abstract class DistinguishedSteppable extends DSteppable implements Distinguished {
	private static final long serialVersionUID = 1;
	
	public DistinguishedSteppable() throws RemoteException {
		super();
	}

	/*
	 * Creates a RemotePromise with all the information needed to fill out it 
	 * and puts the RemotePromise in the queue
	 * 
	 * @param data information required
	 * 
	 * @returns promiseUnfilled promise that will be filled out
	 */
	public Promised contactFor(Serializable data) throws RemoteException {
		// create a RemotePromise unfilled with the required data and myself as author
		// DObject will fulfill this promise in future
		// separate remotepromise / data request / method tag maybe triplet
		RemotePromise promise = new RemotePromise(); 
		DSimState.addRemotePromise(promise, data, this);
		return promise;

	}

	public abstract Serializable respondToRemote() throws RemoteException;

}
