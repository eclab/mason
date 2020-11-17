package sim.field.continuous;

import java.io.Serializable;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import sim.engine.DObject;
import sim.engine.DSimState;
import sim.engine.DistributedIterativeRepeat;
import sim.engine.DistributedTentativeStep;
import sim.engine.Stoppable;
import sim.engine.Stopping;
import sim.field.DAbstractGrid2D;
import sim.field.DGrid;
import sim.field.HaloGrid2D;
import sim.field.Promise;
import sim.field.RemoteFulfillable;
import sim.field.partitioning.IntRect2D;
import sim.field.partitioning.PartitionInterface;
import sim.field.storage.ContinuousStorage;
import sim.util.*;

/**
 * A continuous field that contains lists of objects of type T. Analogous to
 * Mason's Continuous2D.
 * 
 * @param <T> Type of object stored in the field
 */
public class DContinuous2D<T extends DObject> extends DAbstractGrid2D 
	{
	private static final long serialVersionUID = 1L;

	private HaloGrid2D<T, ContinuousStorage<T>> halo;
	ContinuousStorage<T> storage;
	boolean removeEmptyBags = true;
	
	public DContinuous2D(PartitionInterface ps, int aoi, double discretization, DSimState state) 
		{
		super(ps, state);
		storage = new ContinuousStorage<T>(ps.getBounds(), discretization);
		
		try 
		{
			halo = new HaloGrid2D<>(ps, aoi, storage, state);
		} 
		catch (RemoteException e) 
		{
			throw new RuntimeException(e);
		}
	}


	/** Returns the local location of the given object, if any, else null.*/
	public Double2D getObjectLocationLocal(T t)
		{
		HashMap<Long, Double2D> map = storage.getStorageMap();
		return map.get(t.getID());
		}
	
	/** Returns the local location of the given object, if any, else null.*/
	public Double2D getObjectLocationLocal(long id)
		{
		HashMap<Long, Double2D> map = storage.getStorageMap();
		return map.get(id);
		}

	/** Returns true if the data is located locally.  */
	public boolean containsLocal(T t) 
		{
		return (getObjectLocationLocal(t) != null);
		}

	/** Returns true if the data is located at the given point.  This point
		must lie within the local region or an exception will be thrown.  */
	public boolean containsLocal(Double2D p, T t) 
		{
		if (!isHalo(p)) throwNotLocalException(p);
		return (p.equals(getObjectLocationLocal(t)));
		}

	/** Returns true if the data is located at the given point.  This point
		must lie within the halo region or an exception will be thrown.  */
	public boolean containsLocal(Double2D p, long id) 
		{
		return (getLocal(id) != null); // uses hashmap
		}

	/** Returns all the local data located in discretized cell in the <i>vicinity</i> of the given point.  This point
		must lie within the local region or an exception will be thrown.  */
	public HashMap<Long, T> getCellLocal(Double2D p) 
		{
		if (!isHalo(p)) throwNotLocalException(p);
		return storage.getCell(p);
		}

	/** Returns all the local data located <i>exactly</i> at the given point.  This point
		must lie within the local region or an exception will be thrown.  */
	public HashMap<Long, T> getLocal(Double2D p) 
		{
		HashMap<Long, T> cell = getCellLocal(p);
		HashMap<Long, T> reduced = new HashMap<>();
		if (cell == null) return reduced;
		for(T t : cell.values())
			 {
			 if (p.equals(getObjectLocationLocal(t)))
			 	reduced.put(t.getID(),t);
			 }
		return reduced;
		}

	/** Returns all the local data located <i>exactly</i> at the given point.  This point
		must lie within the local region or an exception will be thrown.  */
	public DObject getLocal(long id) 
		{
		Double2D p = getObjectLocationLocal(id);
		return getCellLocal(p).get(id);
		}
	
	/** Returns all the local data located <i>exactly</i> at the given point.  This point
	must lie within the local region or an exception will be thrown.  */
	public DObject getLocal(Double2D p, long id) 
		{
		return getCellLocal(p).get(id);
		}

		
	/** Sets or moves the given object, which must be currently local or not set. */
	// this could be done by just calling removeLocal but I'm
	// trying to avoid some unnecessary hash lookups, copies, and hashset deletion
	public void addLocal(Double2D p, T t) 
		{
		if (!isLocal(p)) throwNotLocalException(p);	
		Double2D oldLoc = getObjectLocationLocal(t);
		HashMap<Long, T> newCell = getCellLocal(p);
		if (oldLoc != null)
			{
			HashMap<Long, T> oldCell = getCellLocal(oldLoc);
			
			
			if (oldCell == newCell)
				{
				// don't mess with them
				}
			else 
				{
				if (oldCell != null)
					{
					oldCell.remove(t);
					if (oldCell.isEmpty() && removeEmptyBags)
						storage.setCell(oldLoc, null);
					}
				if (newCell == null)
					{
					newCell = new HashMap<>();
					storage.setCell(p, newCell);
					}
				newCell.put(t.getID(), t);
				}
			}
		else
			{
			if (newCell == null)
				{
				newCell = new HashMap<>();
				storage.setCell(p, newCell);
				}
			newCell.put(t.getID(), t);
			}

		HashMap<Long, Double2D> map = storage.getStorageMap();
		map.put(t.getID(), p);
		}
	
	public boolean removeLocal(long id)
		{
		return removeLocal(storage.getLocation(id), id);
		}
		
	/** Removes the object, which must be local.  If it doesn't exist, returns FALSE. */
	public boolean removeLocal(T t)
		{
		if (t == null) return false;
		Double2D loc = getObjectLocationLocal(t);
		if (loc == null) return false;
		else
			{
			HashMap<Long, T> cell = getCellLocal(loc);
			if (cell != null)
				cell.remove(t);
			if (cell.isEmpty() && removeEmptyBags)
				storage.setCell(loc, null);
			return true;
			}
		}
	
	/** Removes the object, which must be local.  If it doesn't exist, returns FALSE. */
	public boolean removeLocal(Double2D p, long id)
		{
		// TODO: global vs local coordinates?
		HashMap<Long, T> cell = getCellLocal(p);
		T t = cell.get(id);
		if (t == null) return false;
		else
			{
			if (cell != null)
				cell.remove(t);
			if (cell.isEmpty() && removeEmptyBags)
				storage.setCell(p, null);
			return true;
			}
		}
		
	/** Returns the local (non-halo) region.  */
	public IntRect2D localBounds()  { return halo.origPart; }

	/** Returns the halo region.  */
	public IntRect2D haloBounds()  { return halo.haloPart; }

	/** Returns true if the point is within the local (non-halo) region.  */
	public boolean isLocal(Double2D p) { return halo.inLocal(p); }

	/** Returns true if the point is within the halo region.  */
	public boolean isHalo(Double2D p) { return halo.inLocalAndHalo(p); }
		
		
	/** Returns a Promise which will eventually (immediately or within one timestep)
		hold the data (which must be a DObject) requested if it is located, else null.  This point can be outside
		the local and halo regions. */
	public RemoteFulfillable get(Double2D p, long id) 
		{
//		DObject obj = (DObject) t;		// this may throw a runtime exception
		if (isHalo(p))
			{
				try {
					return new Promise(getLocal(p, id));
				} catch (RemoteException e) {
					throw new RuntimeException(e);
				}
			}
		else
			return halo.getFromRemote(p, id);
		}

	/** Adds the data to the given point.  This point can be outside
		the local and halo regions; if so, it will be added after the end of this timestep.  */
	public void add(Double2D p, T t) 
		{
		if (isLocal(p))
			addLocal(p, t);
		else
			halo.addToRemote(p, t);
		}

	/** Removes the data (which must be a DObject) from the given point.  This point can be outside
		the local and halo regions. */
	public void remove(long id) 
		{
		Double2D p = getObjectLocationLocal(id);
		if (isLocal(p))
			removeLocal(id);
		else
			halo.removeFromRemote(p, id);
		}

	/** Removes the data (which must be a DObject) from the given point.  This point can be outside
		the local and halo regions. */
	public void remove(T t) 
		{
		remove(((DObject) t).getID());
		}


	/** Adds an agent to the given point and schedules it.  This point can be outside
		the local and halo regions; if so, it will be set after the end of this timestep.  */
	public void addAndScheduleAgent(Double2D p, T agent, double time, int ordering) 
		{
		if (agent == null)
			{
			throw new RuntimeException("Cannot move null agent to " + p);
			}
		if (isLocal(p))
			{
			Stopping a = (Stopping) agent;		// may generate a runtime error
			addLocal(p, agent);
			state.schedule.scheduleOnce(time, ordering, a); 
			}
		else
			{
			halo.addAgent(p, agent, ordering, time);
			}
		}
		
	/** Adds an agent to the given point and schedules it repeating.  This point can be outside
		the local and halo regions; if so, it will be set after the end of this timestep.  */
	public void addAndScheduleAgent(Double2D p, T agent, double time, int ordering, double interval) 
		{
		if (agent == null)
			{
			throw new RuntimeException("Cannot move null agent to " + p);
			}
		if (isLocal(p))
			{
			Stopping a = (Stopping) agent;		// may generate a runtime error
			addLocal(p, agent);
			state.schedule.scheduleRepeating(time, ordering, a, interval); 
			}
		else
			{
			halo.addAgent(p, agent, ordering, time, interval);
			}
		}

	/** Removes the given agent from the given point and stops it.  This point can be outside
		the local and halo regions; if so, it will be set after the end of this timestep.  
		The agent must be a DObject and a Stopping: realistically this means it should
		be a DSteppable. */ 
	public void removeAgent(T agent) 
		{
		if (agent == null) return;
		
		Double2D p = getObjectLocationLocal(agent);
		
		// will this work or is Java too smart?
		Stopping b = (Stopping) agent;		// may generate a runtime error
		
		if (isLocal(p))
			{
			Stoppable stop = ((Stopping)b).getStoppable();
			if (stop == null)
				{
				// we're done
				}
			else if ((stop instanceof DistributedTentativeStep))
				{
				((DistributedTentativeStep)stop).stop();
				}
			else if ((stop instanceof DistributedIterativeRepeat))
				{
				((DistributedIterativeRepeat)stop).stop();
				}
			else
				{
				throw new RuntimeException("Cannot remove agent " + agent + " from " + p + " because it is wrapped in a Stoppable other than a DistributedIterativeRepeat or DistributedTenativeStep.  This should not happen.");
				}
			removeLocal(agent);
			}
		else
			{
			halo.removeAgent(null, agent.getID());
			}
		}
		

	/** Moves an agent from one location to another, possibly rescheduling it if the new location is remote.
	  	The [from] location must be local, but the [to] location can be outside
		the local and halo regions; if so, it will be set and rescheduled after the end of this timestep.
		If the agent is not presently AT from, then the from location is undisturbed. */
	public void moveAgent(Double2D to, T agent) 
		{
		if (agent == null)
			{
			throw new RuntimeException("Cannot move null agent to " + to);
			}
		else if (containsLocal(agent))
			{
			Double2D p = getObjectLocationLocal(agent);
			if (isLocal(to))
				{
				removeLocal(agent);
				addLocal(to, agent);
				}
			else
				{
				// Here we have to move the agent remotely and reschedule him
				Stopping a = (Stopping) agent;			// may throw exception if it's not really an agent
				Stoppable stop = a.getStoppable();
				if (stop == null)
					{
					// we're done, just move it but don't bother rescheduling
					removeLocal(agent);
					halo.addToRemote(to, agent);
					}
				else if (stop instanceof DistributedTentativeStep)
					{
					DistributedTentativeStep _stop = (DistributedTentativeStep)stop;
					double time = _stop.getTime();
					int ordering = _stop.getOrdering();
					_stop.stop();
					if (time > state.schedule.getTime())  // scheduled in the future
						{
						removeLocal(agent);
						halo.addAgent(to, agent, ordering, time);
						}
					else	// this could theoretically happen because TentativeStep doesn't null out its agent after step()
						{
						// we're done, just move it
						removeLocal(agent);
						halo.addToRemote(to, agent);
						}
					}
				else if (stop instanceof DistributedIterativeRepeat)
					{
					DistributedIterativeRepeat _stop = (DistributedIterativeRepeat)stop;
					double time = _stop.getTime();
					int ordering = _stop.getOrdering();
					double interval = _stop.getInterval();
					_stop.stop();
					
					// now we have to revise the time
					double delta = state.schedule.getTime() - time;
					if (delta > interval)
						{					
						double remainder = delta % interval;	// how much is left?
						time = time + delta + remainder;		// I THINK this is right?  -- Sean
						}
					else
						{
						time = time + interval;					// advance to next
						}
					removeLocal(agent);
					halo.addAgent(to, agent, ordering, time, interval);
					}
				else
					{
					throw new RuntimeException("Cannot move agent " + a + " from " + p + " to " + to + " because it is wrapped in a Stoppable other than a DistributedIterativeRepeat or DistributedTenativeStep.  This should not happen.");
					}
				}
			}
		else
			{
			throw new RuntimeException("Cannot move agent " + agent + " to " + to + " because agent is not local.");
			}
		}








	//// FIXME SEAN:
	//// We can't getNeighborsWithinDistance any more very easily
	//// But we need to figure out how to do something like that (see
	//// Continuous2D.java)

	//// FIXME SEAN:
	//// Width and height are ints :-(

	/** Toroidal x */
	// slight revision for more efficiency
	public double tx(double x) {
		double width = this.width;
		if (x >= 0 && x < width)
			return x; // do clearest case first
		x = x % width;
		if (x < 0)
			x = x + width;
		return x;
	}

	/** Toroidal y */
	// slight revision for more efficiency
	public double ty(double y) {
		double height = this.height;
		if (y >= 0 && y < height)
			return y; // do clearest case first
		y = y % height;
		if (y < 0)
			y = y + height;
		return y;
	}

	/**
	 * Simple [and fast] toroidal x. Use this if the values you'd pass in never
	 * stray beyond (-width ... width * 2) not inclusive. It's a bit faster than the
	 * full toroidal computation as it uses if statements rather than two modulos.
	 * The following definition:<br>
	 * { double width = this.width; if (x >= 0) { if (x < width) return x; return x
	 * - width; } return x + width; } <br>
	 * ...produces the shortest code (24 bytes) and is inlined in Hotspot for 1.4.1.
	 * However removing the double width = this.width; is likely to be a little
	 * faster if most objects are within the toroidal region.
	 */
	public double stx(double x) {
		if (x >= 0) {
			if (x < width)
				return x;
			return x - width;
		}
		return x + width;
	}

	/**
	 * Simple [and fast] toroidal y. Use this if the values you'd pass in never
	 * stray beyond (-height ... height * 2) not inclusive. It's a bit faster than
	 * the full toroidal computation as it uses if statements rather than two
	 * modulos. The following definition:<br>
	 * { double height = this.height; if (y >= 0) { if (y < height) return y ;
	 * return y - height; } return y + height; } <br>
	 * ...produces the shortest code (24 bytes) and is inlined in Hotspot for 1.4.1.
	 * However removing the double height = this.height; is likely to be a little
	 * faster if most objects are within the toroidal region.
	 */
	public double sty(double y) {
		if (y >= 0) {
			if (y < height)
				return y;
			return y - height;
		}
		return y + height;
	}

	// some efficiency to avoid width lookups
	double _stx(double x, double width) {
		if (x >= 0) {
			if (x < width)
				return x;
			return x - width;
		}
		return x + width;
	}

	/** Minimum toroidal difference between two values in the X dimension. */
	public double tdx(double x1, double x2) {
		double width = this.width;
		if (Math.abs(x1 - x2) <= width / 2)
			return x1 - x2; // no wraparounds -- quick and dirty check

		double dx = _stx(x1, width) - _stx(x2, width);
		if (dx * 2 > width)
			return dx - width;
		if (dx * 2 < -width)
			return dx + width;
		return dx;
	}

	// some efficiency to avoid height lookups
	double _sty(double y, double height) {
		if (y >= 0) {
			if (y < height)
				return y;
			return y - height;
		}
		return y + height;
	}

	/** Minimum toroidal difference between two values in the Y dimension. */
	public double tdy(double y1, double y2) {
		double height = this.height;
		if (Math.abs(y1 - y2) <= height / 2)
			return y1 - y2; // no wraparounds -- quick and dirty check

		double dy = _sty(y1, height) - _sty(y2, height);
		if (dy * 2 > height)
			return dy - height;
		if (dy * 2 < -height)
			return dy + height;
		return dy;
	}

	/**
	 * Minimum Toroidal Distance Squared between two points. This computes the
	 * "shortest" (squared) distance between two points, considering wrap-around
	 * possibilities as well.
	 */
	public double tds(Double2D d1, Double2D d2) {
		double dx = tdx(d1.x, d2.x);
		double dy = tdy(d1.y, d2.y);
		return (dx * dx + dy * dy);
	}

	/**
	 * Minimum Toroidal difference vector between two points. This subtracts the
	 * second point from the first and produces the minimum-length such subtractive
	 * vector, considering wrap-around possibilities as well
	 */
	public Double2D tv(Double2D d1, Double2D d2) {
		return new Double2D(tdx(d1.x, d2.x), tdy(d1.y, d2.y));
	}

}
