package sim.engine.rmi;

import java.io.Serializable;
import java.rmi.RemoteException;
import sim.field.*;
import sim.field.partitioning.*;
import sim.util.NumberND;

/**
 * RMIProxy for using Java RMI. RMI is used for point to point communication
 * between nodes that are not neighbors.
 *
 * @param <P> The Type of PointND to use
 * @param <T> The Type of Object in the field
 */
@SuppressWarnings("rawtypes")
public class RMIProxy<T extends Serializable, P extends NumberND>
{
	private static final long serialVersionUID = 1L;

	TransportRMIInterface[] cache;
	int fieldId;

	public RMIProxy(Partition ps, HaloGrid2D haloGrid)
	{
		this.fieldId = haloGrid.getFieldIndex();
		this.cache = new TransportRMIInterface[ps.getNumProcessors()];
	}

	@SuppressWarnings("unchecked")
	public TransportRMIInterface<T, P> getField(int pid) throws RemoteException
	{
		TransportRMIInterface<T, P> transportRMI = cache[pid];
		if (transportRMI == null)
		{
			transportRMI = RemoteProcessor.getProcessor(pid).getTransportRMI(fieldId);
			cache[pid] = transportRMI;
		}
		return transportRMI;
	}
}
