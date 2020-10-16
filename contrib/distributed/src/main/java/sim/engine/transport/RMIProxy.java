package sim.engine.transport;

import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import sim.engine.DSimState;
import sim.engine.registry.DRegistry;
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
	/** This registry object points to the registry in DRegistry */
	final DRegistry registry;
	final String prefix = "field: ";
	final String suffix;
	final String fieldName;

	public RMIProxy(final PartitionInterface ps, HaloGrid2D<T, P, ?> field) {
		suffix = "," + field.fieldIndex;
		fieldName = prefix + DSimState.getPID() + suffix;
		registry = DRegistry.getInstance();

		try {
			if (!registry.registerObject(fieldName, field))
				throw new RuntimeException("Failed to register field: " + field);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public TransportRMIInterface<T, P> getField(final int pid) {
		try {
			return (TransportRMIInterface<T, P>) registry.getObject(prefix + pid + suffix);
		} catch (RemoteException | NotBoundException e) {
			throw new RuntimeException(e);
		}
	}

}
