package sim.field.grid;

import java.rmi.*;
import sim.engine.*;
import sim.field.*;
import sim.field.partitioning.*;
import sim.field.storage.*;
import sim.util.*;

/**
 * A grid that contains integers. Analogous to Mason's IntGrid2D
 * 
 */
public class DIntGrid2D extends DAbstractGrid2D
	{
	private static final long serialVersionUID = 1L;

	HaloGrid2D<Integer, IntGridStorage> halo;
	IntGridStorage storage;
	
	public DIntGrid2D(Partition ps, int aoi, DSimState state) 
		{
		super(ps, state);
		storage = new IntGridStorage(ps.getBounds());
		try 
			{
			halo = new HaloGrid2D<Integer, IntGridStorage>(ps, aoi, storage, state);
			} 
		catch (RemoteException e) 
			{
			throw new RuntimeException(e);
			}
		}

	/** Returns the underlying storage array for the DIntGrid2D.  This array
		is a one-dimensional array, in row-major order, of all the cells in
		the halo region. */
	public int[] getStorageArray() { return storage.storage; }

	/** Returns the data associated with the given point.  This point
		must lie within the halo region or an exception will be thrown.  */
	public int getLocal(Int2D p) 
		{
		if (!isHalo(p)) throwNotLocalException(p);
		return storage.storage[storage.getFlatIdx(halo.toLocalPoint(p))];
		}

	/** Returns the data associated with the given point.  This point
		must lie within the (non-halo) local region or an exception will be thrown.  */
	public void setLocal(Int2D p, int t) 
		{
		if (!isLocal(p)) throwNotLocalException(p);
		storage.storage[storage.getFlatIdx(halo.toLocalPoint(p))] = t;
		}
	
	/** Returns the local (non-halo) region.  */
	public IntRect2D localBounds()  { return halo.origPart; }

	/** Returns the halo region.  */
	public IntRect2D haloBounds()  { return halo.haloPart; }

	/** Returns true if the point is within the local (non-halo) region.  */
	public boolean isLocal(Int2D p) { return halo.inLocal(p); }

	/** Returns true if the point is within the halo region.  */
	public boolean isHalo(Int2D p) { return halo.inLocalAndHalo(p); }

	/** Returns a Promise which will eventually (immediately or within one timestep)
		hold the data located at the given point.  This point can be outside
		the local and halo regions. */
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
	public void set(Int2D p, int val) 
		{
		if (isLocal(p))
			storage.storage[storage.getFlatIdx(halo.toLocalPoint(p))] = val;
		else
			halo.addToRemote(p, val);
		}
	}
