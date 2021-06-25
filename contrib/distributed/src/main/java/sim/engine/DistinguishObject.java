package sim.engine;

import java.io.Serializable;
import java.rmi.RemoteException;

import sim.engine.rmi.RemotePromiseImpl;

public abstract class DistinguishObject extends DSteppable implements DistinguishInterface {
	private static final long serialVersionUID = 1;
	
	public DistinguishObject() throws RemoteException {
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
	public RemotePromise contactFor(Serializable data) throws RemoteException {
		// create a RemotePromise unfilled with the required data and myself as author
		// DObject will fulfill this promise in future
		// separate remotepromise / data request / method tag maybe triplet
		RemotePromiseImpl promise = new RemotePromiseImpl(); 
		DSimState.addRemotePromise(promise, data, this);
		return promise;

	}

	public abstract Serializable respondToRemote() throws RemoteException;

	@Override
	public abstract void step(SimState state);
	
}
