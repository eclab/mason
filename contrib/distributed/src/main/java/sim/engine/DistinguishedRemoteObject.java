package sim.engine;

import java.rmi.RemoteException;
import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import sim.engine.rmi.RemotePromise;

/*
 * Wrapper of the Remote Object containing
 * the real object
 * the queue of remote message that has to be fulfilled
 * and the corresponding methods
*/

public class DistinguishedRemoteObject implements Remote 
	{
	// real object within the field
	Distinguished object;
	DSimState state;

	public DistinguishedRemoteObject(Distinguished object, DSimState state) 
		{
		this.object = object;
		this.state = state;
	}

	// add a Promise that has been exported on the Dregistry on the queue on DSimstate
	public void remoteMessage(int tag, Serializable arguments, Promised callback) throws RemoteException 
		{
		DistinguishedRemoteMessage remoteMessage = new DistinguishedRemoteMessage(object, tag, arguments, callback);
        state.addRemoteMessage(remoteMessage);
		}
}