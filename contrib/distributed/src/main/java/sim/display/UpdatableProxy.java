package sim.display;
import java.rmi.*;


/**
	Each Proxy for a field in MASON implements this. This is called to ask the proxy
	to update itself by extracting information remotely.  The field's storage number
	is provided.  This number will be the order in which the field was registered with the SimState.
*/

public interface UpdatableProxy
	{
	public void update(SimStateProxy proxy, int storage, int[] partition_list) throws RemoteException, NotBoundException;
	}



