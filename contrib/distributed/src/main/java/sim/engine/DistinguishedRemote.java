package sim.engine;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;


/**
	The RMI interface for DistinguishedRemoteObject.
**/


public interface DistinguishedRemote extends Remote 
{
	/**
	 * Respond to a remote request of some data.
	 * @param tag is the tag used to understand which method to use to fill the RemoteMessage  
	 * @param arguments is the optional argument that could be needed
	 * @param callback Promised callback that has to be fullfilled
	 * 
	 * 
	 * @return the id of the message used to register it on the DRegistry
	 */
	public void remoteMessage(int tag, Serializable arguments, Promised callback) throws RemoteException;
}