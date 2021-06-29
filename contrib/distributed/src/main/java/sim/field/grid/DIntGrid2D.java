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
	
	public DIntGrid2D(DSimState state) 
		{
		super(state);
		storage = new IntGridStorage(state.getPartition().getHaloBounds());
		try 
			{
			halo = new HaloGrid2D<Integer, IntGridStorage>(storage, state);
			} 
		catch (RemoteException e) 
			{
			throw new RuntimeException(e);
			}
		}

	/** Returns the underlying storage array for the DIntGrid2D.  This array
		is a one-dimensional array, in row-major order, of all the cells in
		the halo region. */
	public int[] getStorageArray()
		{
		return storage.storage;
		}

	/** Returns the data associated with the given point.  This point
		must lie within the halo region or an exception will be thrown.  */
	public int getLocal(Int2D p) 
		{
		if (!isHalo(p)) throwNotLocalException(p);
		return storage.storage[storage.getFlatIndex(storage.toLocalPoint(p))];
		}

	/** Returns the data associated with the given point.  This point
		must lie within the (non-halo) local region or an exception will be thrown.  */
	public void setLocal(Int2D p, int t) 
		{
		if (!isLocal(p)) throwNotLocalException(p);
		storage.storage[storage.getFlatIndex(storage.toLocalPoint(p))] = t;
		}
	
	public HaloGrid2D getHaloGrid()
		{
		return halo;
		}

	/** Returns a Promise which will eventually (immediately or within one timestep)
		hold the data located at the given point.  This point can be outside
		the local and halo regions. */
	public Promised get(Int2D p) 
		{
		if (isHalo(p))
			return new Promise(storage.storage[storage.getFlatIndex(storage.toLocalPoint(p))]);
		else return halo.getFromRemote(p);
		}

	/** Sets the data located at the given point.  This point can be outside
		the local and halo regions; if so, it will be set after the end of this timestep.  */
	public void set(Int2D p, int val) 
		{
		if (isLocal(p))
			storage.storage[storage.getFlatIndex(storage.toLocalPoint(p))] = val;
		else
			halo.addToRemote(p, val);
		}
	}
