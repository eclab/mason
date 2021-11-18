package sim.engine;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;


/*
 * Interface that should be implemented by a class that needs to be a remote (Distinguish) Object
 * The modeler needs to implement the method of the class based on needs
*/

public interface DistinguishedRemote extends Remote 
{
	/**
	 * Respond to a remote request of some data.
	 * @param message is the tag used to understand which method to use to fill the RemoteMessage  
	 * @param argument is the optional argument that could be needed
	 * 
	 * 
	 * @return the id of the message used to register it on the DRegistry
	 */
	public String remoteMessage(int message, Serializable arguments) throws RemoteException;
}
