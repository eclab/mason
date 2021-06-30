/*
  Copyright 2020 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

/*
 * Interface that should be implemented by a class that needs to be a remote (Distinguish) Object
 * The modeler needs to implement the method of the class based on needs
 */

public interface Distinguished extends Remote {
	
	/*
	 * The method returns the data required to fill out the RemotePromise
	 * See other classes for more information
	 */
	public Serializable respondToRemote() throws RemoteException;
	
}
