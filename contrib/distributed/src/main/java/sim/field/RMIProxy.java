package sim.field;

import java.io.Serializable;
import java.rmi.RemoteException;

import sim.engine.RemoteProcessor;
import sim.engine.transport.TransportRMIInterface;
import sim.field.partitioning.PartitionInterface;
import sim.util.NumberND;

/**
 * RMIProxy for using Java RMI. RMI is used for point to point communication
 * between nodes that are not neighbors.
 *
 * @param <P> The Type of PointND to use
 * @param <T> The Type of Object in the field
 */
@SuppressWarnings("rawtypes")
public class RMIProxy<T extends Serializable, P extends NumberND> {

	final TransportRMIInterface[] cache;
	final int fieldId;

	public RMIProxy(final PartitionInterface ps, HaloGrid2D haloGrid) {
		this.fieldId = haloGrid.fieldIndex;
		this.cache = new TransportRMIInterface[ps.numProcessors];
	}

	@SuppressWarnings("unchecked")
	public TransportRMIInterface<T, P> getField(final int pid) throws RemoteException {
		TransportRMIInterface<T, P> transportRMI = cache[pid];
		if (transportRMI == null) {
			transportRMI = RemoteProcessor.getProcessor(pid).getTransportRMI(fieldId);
			cache[pid] = transportRMI;
		}
		return transportRMI;
	}
}
