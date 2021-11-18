package sim.engine;

import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/*
* Interface of the RemoteMessage required to make it a Remote Object
* and be able to be registered on the DRegistry
*/

public interface DistinguishedRemoteMessage extends Remote{
    public Serializable getValue() throws RemoteException, NotBoundException; 
    public String getId() throws RemoteException; 
    public boolean isReady() throws RemoteException; 
}