package sim.engine.transport;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;

import sim.engine.RemoteProcessor;
import sim.field.HaloGrid2D;
import sim.field.partitioning.PartitionInterface;
import sim.util.NumberND;

/**
 * RMIProxy for using Java RMI. RMI is used for point to point communication
 * between nodes that are not neighbors.
 *
 * @param <P> The Type of PointND to use
 * @param <T> The Type of Object in the field
 */
public class RMIProxy<T extends Serializable, P extends NumberND> {
	final ArrayList<TransportRMIInterface<T, P>> cache;
	final int fieldId;

	public RMIProxy(final PartitionInterface ps, HaloGrid2D haloGrid) {
		this.fieldId = haloGrid.fieldIndex;
		this.cache = new ArrayList<>();

		// init with nulls
		for (int i = 0; i < ps.numProcessors; i++)
			cache.add(null);
	}

	@SuppressWarnings("unchecked")
	public TransportRMIInterface<T, P> getField(final int pid) throws RemoteException {
		TransportRMIInterface<T, P> transportRMI = cache.get(pid);
		if (transportRMI == null) {
			transportRMI = RemoteProcessor.getProcessor(pid).getTransportRMI(fieldId);
			cache.add(pid, transportRMI);
		}
		return transportRMI;
	}
}
