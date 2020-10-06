package sim.field.continuous;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
//import java.util.stream.Collectors;
//import java.util.stream.IntStream;

import sim.engine.DSimState;
import sim.engine.DistributedIterativeRepeat;
import sim.engine.Stopping;
import sim.field.DAbstractGrid2D;
import sim.field.DGrid;
import sim.field.HaloGrid2D;
import sim.field.partitioning.PartitionInterface;
import sim.field.storage.ContStorage;
import sim.util.MPIParam;
import sim.util.*;

/**
 * A countinous field that contains lists of objects of type T. Analogous to
 * Mason's Continuous2D.
 * 
 * @param <T> Type of object stored in the field
 */
public class DContinuous2D<T extends Serializable> extends DAbstractGrid2D implements DGrid<T, Double2D> {

	private HaloGrid2D<T, Double2D, ContStorage<T>> halo;

	public DContinuous2D(final PartitionInterface ps, final int[] aoi, final double[] discretizations,
			final DSimState state) {

		super(ps);
		if (ps.getNumDim() != 2)
			throw new IllegalArgumentException("The number of dimensions is expected to be 2, got: " + ps.getNumDim());

		halo = new HaloGrid2D<T, Double2D, ContStorage<T>>(ps, aoi,
				new ContStorage<T>(ps.getPartition(), discretizations), state);

	}

	public NumberND getLocation(final T obj) {
		return halo.localStorage.getLocation(obj);
	}

	public List<T> get(final Double2D p) {
		if (!halo.inLocalAndHalo(p)) {
			System.out.println(String.format("PID %d get %s is out of local boundary, accessing remotely through RMI",
					halo.partition.getPid(), p.toString()));
			return ((List<T>) halo.getFromRemote(p));
		} else
			return getLocal(p);
	}

	public void addLocal(final Double2D p, final T t) {
		halo.localStorage.addToLocation(t, p);
	}

	public void removeLocal(final Double2D p, final T t) {
		// TODO: Remove from just p
		halo.localStorage.removeObject(t);
	}

	public void removeLocal(final Double2D p) {
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
		
		List<T> list = new ArrayList<T>();
		for (int i = 0; i < objs.size(); i += 2) {
			list.add(objs.get(i));
		}
		return list;
		
//		return IntStream.range(0, objs.size())	// 0, 1, 2, ..., obj.size() - 1
//				.filter(n -> n % 2 == 0)		// 0, 2, 4, ..., 
//				.mapToObj(objs::get)
//				.collect(Collectors.toList());
	}

	public ArrayList<T> getLocal(final Double2D p) {
		return halo.localStorage.getObjects(p);
	}

	public void addAgent(final Double2D p, final T t) {
		halo.addAgent(p, t);
	}

	public void moveAgent(final Double2D fromP, final Double2D toP, final T t) {
		halo.moveAgent(fromP, toP, t);
	}

	// Re-implementing this because
	// add also moves the objects in this field
	public void move(final Double2D fromP, final Double2D toP, final T t) {
		final int fromPid = halo.partition.toPartitionId(fromP);
		final int toPid = halo.partition.toPartitionId(fromP);

		if (fromPid == toPid) {
			if (fromPid != halo.partition.pid)
				try {
					halo.proxy.getField(fromPid).addRMI(toP, t);
				} catch (final RemoteException e) {
					throw new RuntimeException(e);
				}
			else
				add(toP, t);
		} else {
			// move cannot be with a single call
			// Since, the fromP and toP are different Remove from one partition &
			// add to another
			remove(fromP, t);
			add(toP, t);
		}
	}

	public void add(Double2D p, T t) {
		halo.add(p, t);
	}

	public void remove(Double2D p, T t) {
		halo.remove(p, t);
	}

	public void remove(Double2D p) {
		halo.remove(p);
	}

	public void addAgent(Double2D p, T t, int ordering, double time) {
		halo.addAgent(p, t, ordering, time);
	}

	// Re-implementing this because
	// add also moves the objects in this field
	public void moveAgent(Double2D fromP, Double2D toP, T t, int ordering, double time) {
		if (!halo.inLocal(fromP)) {
			// System.out.println("pid " + halo.partition.pid + " agent" + t);
			// System.out.println("partitioning " + halo.partition.getPartition());
			throw new IllegalArgumentException("fromP must be local");
		}

		if (!(t instanceof Stopping))
			throw new IllegalArgumentException("t must be a Stopping");

		final Stopping agent = (Stopping) t;

		if (halo.inLocal(toP)) {
			// no need to remove for local moves
			add(toP, t);
			halo.getState().schedule.scheduleOnce(agent);
		} else {
			remove(fromP, t);
			halo.getState().getTransporter().migrateAgent(agent,
					halo.partition.toPartitionId(toP), toP, halo.fieldIndex);
		}
	}

	public void addRepeatingAgent(Double2D p, T t, double time, int ordering, double interval) {
		halo.addRepeatingAgent(p, t, time, ordering, interval);
	}

	public void addRepeatingAgent(Double2D p, T t, int ordering, double interval) {
		halo.addRepeatingAgent(p, t, ordering, interval);
	}

	public void removeAndStopRepeatingAgent(Double2D p, T t) {
		halo.removeAndStopRepeatingAgent(p, t);
	}

	public void removeAndStopRepeatingAgent(Double2D p, DistributedIterativeRepeat iterativeRepeat) {
		halo.removeAndStopRepeatingAgent(p, iterativeRepeat);
	}

	// Re-implementing this because
	// add also moves the objects in this field
	public void moveRepeatingAgent(Double2D fromP, Double2D toP, T t) {
		if (!halo.inLocal(fromP))
			throw new IllegalArgumentException("fromP must be local");

		// TODO: Should we remove the instanceOf check and assume that the
		// pre-conditions are always met?
		if (halo.inLocal(toP))
			add(toP, t);
		else if (!(t instanceof Stopping))
			throw new IllegalArgumentException("t must be a Stopping");
		else {
			remove(fromP, t);
			final Stopping stopping = (Stopping) t;
			// TODO: check type cast here
			final DistributedIterativeRepeat iterativeRepeat = (DistributedIterativeRepeat) stopping.getStoppable();
			halo.getState().getTransporter()
					.migrateRepeatingAgent(iterativeRepeat, halo.partition.toPartitionId(toP), toP, halo.fieldIndex);
			iterativeRepeat.stop();
		}
	}

	// Re-implementing this because
	// add also moves the objects in this field
	public void moveRepeatingAgent(Double2D fromP, Double2D toP, DistributedIterativeRepeat iterativeRepeat) {
		if (!halo.inLocal(fromP))
			throw new IllegalArgumentException("fromP must be local");

		// We cannot use checked cast for generics because of erasure
		// TODO: is there a safe way of doing this?
		final T t = (T) iterativeRepeat.getSteppable();

		if (halo.inLocal(toP))
			add(toP, t);
		else {
			remove(fromP, t);
			halo.getState().getTransporter()
					.migrateRepeatingAgent(iterativeRepeat, halo.partition.toPartitionId(toP), toP, halo.fieldIndex);
			iterativeRepeat.stop();
		}
	}
	
	
	//// FIXME SEAN:
	//// We can't getNeighborsWithinDistance any more very easily
	//// But we need to figure out how to do something like that (see Continuous2D.java)

	//// FIXME SEAN:
	//// Width and height are ints :-(
	
    /** Toroidal x */
    // slight revision for more efficiency
    public final double tx(double x) 
        { 
        final double width = this.width;
        if (x >= 0 && x < width) return x;  // do clearest case first
        x = x % width;
        if (x < 0) x = x + width;
        return x;
        }
        
    /** Toroidal y */
    // slight revision for more efficiency
    public final double ty(double y) 
        { 
        final double height = this.height;
        if (y >= 0 && y < height) return y;  // do clearest case first
        y = y % height;
        if (y < 0) y = y + height;
        return y;
        }


    /** Simple [and fast] toroidal x.  Use this if the values you'd pass in never stray
        beyond (-width ... width * 2) not inclusive.  It's a bit faster than the full
        toroidal computation as it uses if statements rather than two modulos.
        The following definition:<br>
        { double width = this.width; if (x >= 0) { if (x < width) return x; return x - width; } return x + width; } <br>
        ...produces the shortest code (24 bytes) and is inlined in Hotspot for 1.4.1.   However removing
        the double width = this.width; is likely to be a little faster if most objects are within the
        toroidal region. */
    public double stx(final double x) 
        { if (x >= 0) { if (x < width) return x; return x - width; } return x + width; }
    
    /** Simple [and fast] toroidal y.  Use this if the values you'd pass in never stray
        beyond (-height ... height * 2) not inclusive.  It's a bit faster than the full
        toroidal computation as it uses if statements rather than two modulos.
        The following definition:<br>
        { double height = this.height; if (y >= 0) { if (y < height) return y ; return y - height; } return y + height; } <br>
        ...produces the shortest code (24 bytes) and is inlined in Hotspot for 1.4.1.   However removing
        the double height = this.height; is likely to be a little faster if most objects are within the
        toroidal region. */
    public double sty(final double y) 
        { if (y >= 0) { if (y < height) return y ; return y - height; } return y + height; }
        
    
    // some efficiency to avoid width lookups
    double _stx(final double x, final double width) 
        { if (x >= 0) { if (x < width) return x; return x - width; } return x + width; }

    /** Minimum toroidal difference between two values in the X dimension. */
    public double tdx(final double x1, final double x2)
        {
        double width = this.width;
        if (Math.abs(x1-x2) <= width / 2)
            return x1 - x2;  // no wraparounds  -- quick and dirty check
        
        double dx = _stx(x1,width) - _stx(x2,width);
        if (dx * 2 > width) return dx - width;
        if (dx * 2 < -width) return dx + width;
        return dx;
        }
    
    // some efficiency to avoid height lookups
    double _sty(final double y, final double height) 
        { if (y >= 0) { if (y < height) return y ; return y - height; } return y + height; }

    /** Minimum toroidal difference between two values in the Y dimension. */
    public double tdy(final double y1, final double y2)
        {
        double height = this.height;
        if (Math.abs(y1-y2) <= height / 2)
            return y1 - y2;  // no wraparounds  -- quick and dirty check

        double dy = _sty(y1,height) - _sty(y2,height);
        if (dy * 2 > height) return dy - height;
        if (dy * 2 < -height) return dy + height;
        return dy;
        }
    
    /** Minimum Toroidal Distance Squared between two points. This computes the "shortest" (squared) distance between two points, considering wrap-around possibilities as well. */
    public double tds(final Double2D d1, final Double2D d2)
        {
        double dx = tdx(d1.x,d2.x);
        double dy = tdy(d1.y,d2.y);
        return (dx * dx + dy * dy);
        }

    /** Minimum Toroidal difference vector between two points.  This subtracts the second point from the first and produces the minimum-length such subtractive vector, considering wrap-around possibilities as well*/
    public Double2D tv(final Double2D d1, final Double2D d2)
        {
        return new Double2D(tdx(d1.x,d2.x),tdy(d1.y,d2.y));
        }
    
	
	
}
