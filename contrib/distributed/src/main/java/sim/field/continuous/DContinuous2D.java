package sim.field.continuous;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import mpi.MPI;
import sim.engine.DSimState;
import sim.engine.IterativeRepeat;
import sim.engine.Stopping;
import sim.field.DAbstractGrid2D;
import sim.field.HaloGrid2D;
import sim.field.partitioning.PartitionInterface;
import sim.field.partitioning.IntPoint;
import sim.field.partitioning.NdPoint;
import sim.field.storage.ContStorage;
import sim.util.MPIParam;
import sim.util.MPIUtil;

public class DContinuous2D<T extends Serializable> extends DAbstractGrid2D {
	
	private HaloGrid2D<T, NdPoint, ContStorage<T>> halo;
	
	public DContinuous2D(final PartitionInterface ps, final int[] aoi, final double[] discretizations, final DSimState state) {
		
		super(ps);
		if(ps.getNumDim()!=2)
			throw new IllegalArgumentException("The number of dimensions is expected to be 2, got: " +ps.getNumDim());
		
		halo = new HaloGrid2D<T, NdPoint, ContStorage<T>>
				(ps, aoi, new ContStorage<T>(ps.getPartition(), discretizations), state);

	}
		
	public void addAgent(final NdPoint p, final T t) {
		halo.addAgent(p, t);
	}
	public void addLocal(final NdPoint p, final T t) {
		halo.localStorage.setLocation(t, p);
	}
	public void moveAgent(final NdPoint fromP, final NdPoint toP, final T t) {
		 halo.moveAgent(fromP, toP, t);
	}
	public void move(final NdPoint fromP, final NdPoint toP, final T t) {
		
		halo.move(fromP, toP, t);
	//TODO CHECK IT
	/**
		final int toPid = halo.partition.toPartitionId(fromP);

		if (fromPid == toPid) {
			if (fromPid != halo.partition.pid)
				try {
					halo.proxy.getField(halo.partition.toPartitionId(fromP)).addRMI(toP, t);
				} catch (final RemoteException e) {
					throw new RuntimeException(e);
				}
			else
				halo.add(toP, t);
		} else {
			// move cannot be with a single call
			// Since, the fromP and toP are different

			// Remove from one partition
			halo.remove(fromP, t);

			// add to another
			halo.add(toP, t);
		}**/
	}
	
	
	public NdPoint getLocation(final T obj) {
		return halo.localStorage.getLocation(obj);
	}

	public List<T> get(final NdPoint p) {
		if (!halo.inLocalAndHalo(p)) {
			System.out.println(String.format("PID %d get %s is out of local boundary, accessing remotely through RMI",
					halo.partition.getPid(), p.toString()));
			return ((List<T>) halo.getFromRemote(p));
			}
		else
			return getLocal(p);
	}
	
	public void removeLocal(final NdPoint p, final T t) {
		// TODO: Remove from just p
		halo.localStorage.removeObject(t);
	}

	public void removeLocal(final NdPoint p) {
		halo.localStorage.removeObjects(p);
	}

	public List<T> getNearestNeighbors(final T obj, final int k) {
		return halo.localStorage.getNearestNeighbors(obj, k);
	}

	public List<T> getNeighborsWithin(final T obj, final double r) {
		return halo.localStorage.getNeighborsWithin(obj, r);
	}
	
	// TODO refactor this after new pack/unpack is introduced in Storage
	@SuppressWarnings("unchecked")
	public final List<T> getAllObjects() {
		Serializable data = null;

		data = halo.localStorage.pack(new MPIParam(halo.origPart, halo.haloPart, halo.MPIBaseType));

		final List<T> objs = ((ArrayList<ArrayList<T>>) data).get(0);

		return IntStream.range(0, objs.size())
				.filter(n -> n % 2 == 0)
				.mapToObj(objs::get)
				.collect(Collectors.toList());
	}

	public ArrayList<T> getLocal(final NdPoint p) {
		return halo.localStorage.getObjects(p);
	}


	
}
