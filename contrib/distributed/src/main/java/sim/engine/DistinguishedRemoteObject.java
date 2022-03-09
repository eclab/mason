package sim.engine;

import java.rmi.RemoteException;
import java.io.Serializable;

/**
	The actual object registered on the RMI Registry by DistinguishedRegistry on behalf of a Distinguished
	object.  This object contains the Distinguished object and the DSimState in which it is stored,
	so it can add the message the DSimState's queue.
**/

public class DistinguishedRemoteObject implements DistinguishedRemote 
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