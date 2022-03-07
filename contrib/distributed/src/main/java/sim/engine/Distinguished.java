/*
  Copyright 2020 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

import java.io.Serializable;
import java.rmi.RemoteException;


/*
 * Interface that should be implemented by a class that needs to be a distinguished Object.
 */

public interface Distinguished 
{
	/**
	 * Respond to a remote request of some data.
	 * @param tag is the tag used to understand which method to use to fill the RemoteMessage  
	 * @param arguments is the optional argument that could be needed
	 * 
	 */
	public Serializable remoteMessage(int tag, Serializable arguments) throws RemoteException;
	
	/**
	 * Return the name of the remote object.
	 */
	// Note this isn't a get...() so it doesn't appear in MASON's inspectors
	public String distinguishedName();
}
