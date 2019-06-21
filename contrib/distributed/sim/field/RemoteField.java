package sim.field;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

import sim.util.NdPoint;

/**
 * Contains a method to get data from remote field
 *
 *
 * @param <P> The Type of NdPoint to use
 */
public interface RemoteField<P extends NdPoint> extends Remote {
	Serializable getRMI(P p) throws RemoteException;
}
