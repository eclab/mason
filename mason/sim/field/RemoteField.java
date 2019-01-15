package sim.field;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

import sim.util.IntPoint;

// TODO replace IntPoint with NdPoint
public interface RemoteField extends Remote {
	Serializable getRMI(IntPoint p) throws RemoteException;
}