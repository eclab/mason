package sim.engine;

import java.rmi.RemoteException;
import java.io.Serializable;

import sim.engine.rmi.RemotePromise;

/*
 * Wrapper of the Remote Object containing
 * the real object
 * the queue of remote message that has to be fulfilled
 * and the corresponding methods
*/

public class DistinguishedObject implements DistinguishedRemote {
	
	// real object within the field
	protected Distinguished object;
	protected DSimState simstate;

	// queue of remoteMessage
	//private Queue<DistinguishedRemoteMessageObject> queue = new ConcurrentLinkedQueue<DistinguishedRemoteMessageObject>();


	public DistinguishedObject(Distinguished object, DSimState simstate) {
		this.object = object;
		this.simstate = simstate;
	}

	// add a Promise that has been exported on the Dregistry on the queue on DSimstate
	public void remoteMessage(int tag, Serializable arguments, Promised callback) throws RemoteException {
		DistinguishedRemoteMessage remoteMessage 
			= new DistinguishedRemoteMessage(object, tag, arguments, callback);
        simstate.addRemoteMessage(remoteMessage);
	}

}