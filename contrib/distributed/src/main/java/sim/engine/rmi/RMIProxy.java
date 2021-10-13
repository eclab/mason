package sim.engine.rmi;

import java.io.Serializable;

import java.rmi.RemoteException;
import sim.field.*;
import sim.field.partitioning.*;
import sim.util.Number2D;

/**
 * RMIProxy for using Java RMI. RMI is used for point to point communication
 * between nodes that are not neighbors.
 *
 * @param <P> The Type of PointND to use
 * @param <T> The Type of Object in the field
 */
@SuppressWarnings("rawtypes")
public class RMIProxy<T extends Serializable, P extends Number2D>
{
	private static final long serialVersionUID = 1L;

	GridRMI[] cache;
	int fieldId;

	public RMIProxy(Partition ps, HaloGrid2D haloGrid)
	{
		this.fieldId = haloGrid.getFieldIndex();
		this.cache = new GridRMI[ps.getNumProcessors()];
	}

	@SuppressWarnings("unchecked")
	public GridRMI<T, P> getField(int pid) throws RemoteException
	{
		GridRMI<T, P> grid = cache[pid];
		if (grid == null)
		{
			grid = RemoteProcessor.getProcessor(pid).getGrid(fieldId);
			cache[pid] = grid;
		}
		return grid;
	}
}
