package sim.field.grid;

import java.rmi.*;
import sim.engine.*;
import sim.field.*;
import sim.field.partitioning.*;
import sim.field.storage.*;
import sim.util.*;

/**
 * A grid that contains Doubles. Analogous to Mason's DoubleGrid2D
 * 
 */
 
public class DDoubleGrid2D extends DAbstractGrid2D
	{
	private static final long serialVersionUID = 1L;

	HaloGrid2D<Double, DoubleGridStorage> halo;
	DoubleGridStorage storage;
	
	public DDoubleGrid2D(DSimState state) 
		{
		super(state);
		storage = new DoubleGridStorage(state.getPartition().getBounds());
		try 
			{
			halo = new HaloGrid2D<Double, DoubleGridStorage>(storage, state);
			} 
		catch (RemoteException e) 
			{
			throw new RuntimeException(e);
			}
		}

	/** Returns the underlying storage array for the DDoubleGrid2D.  This array
		is a one-dimensional array, in row-major order, of all the cells in
		the halo region. */
	public double[] getStorageArray() { return storage.storage; }

	/** Returns the data associated with the given point.  This point
		must lie within the halo region or an exception will be thrown.  */
	public double getLocal(Int2D p) 
		{
		if (!isHalo(p)) throwNotLocalException(p);
		return storage.storage[storage.getFlatIdx(halo.toLocalPoint(p))];
		}

	/** Returns the data associated with the given point.  This point
		must lie within the (non-halo) local region or an exception will be thrown.  */
	public void setLocal(Int2D p, double t) 
		{
		if (!isLocal(p)) throwNotLocalException(p);
		storage.storage[storage.getFlatIdx(halo.toLocalPoint(p))] = t;
		}
	
	public HaloGrid2D getHaloGrid() { return halo; }

	/** Returns a Promise which will eventually (immediately or within one timestep)
		hold the data located at the given point.  This point can be outside
		the loal and halo regions. */
	public RemoteFulfillable get(Int2D p) 
		{
		if (isHalo(p))
			try {
				return new Promise(storage.storage[storage.getFlatIdx(halo.toLocalPoint(p))]);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		else return halo.getFromRemote(p);
		}

	/** Sets the data located at the given point.  This point can be outside
		the local and halo regions; if so, it will be set after the end of this timestep.  */
	public void set(Int2D p, double val) 
		{
		if (isLocal(p))
			storage.storage[storage.getFlatIdx(halo.toLocalPoint(p))] = val;
		else
			halo.addToRemote(p, val);
		}

	/** Multiplies all elements in the local storage array byThisMuch */
	public void multiply(double byThisMuch) {
			if (byThisMuch != 1.0)
				{			
				double[] s = storage.storage;
				for(int i = 0; i < s.length; i++)
					{
					s[i] *= byThisMuch;
					}
				}
		}

/*	
public DDoubleGrid2D multiply(double byThisMuch) {
		if (byThisMuch == 1.0)
			return this;

		for (Int2D p : halo.partition.getBounds().getPointList()) {
			Double obj = get(p);
			removeLocal(p);
			add(p, obj * byThisMuch);
		}
		return this;
	}
*/
}
