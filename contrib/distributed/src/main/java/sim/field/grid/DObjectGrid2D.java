package sim.field.grid;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.rmi.RemoteException;
import sim.engine.*;
import sim.field.*;
import sim.field.partitioning.*;
import sim.field.storage.*;
import sim.util.*;

/**
 * A grid that contains objects of type T. Analogous to Mason's ObjectGrid2D
 *
 * @param <T> Type of object stored in the grid
 */
public class DObjectGrid2D<T extends Serializable> extends DAbstractGrid2D 
	{
	private static final long serialVersionUID = 1L;

	HaloGrid2D<T, ObjectGridStorage<T>> halo;
	ObjectGridStorage<T> storage;

	public DObjectGrid2D(DSimState state) 
		{
		super(state);
		storage = new ObjectGridStorage<T>(state.getPartition().getLocalBounds());
		try 
			{
			halo = new HaloGrid2D<>(storage, state);
			} 
		catch (RemoteException e) 
			{
			throw new RuntimeException(e);
			}
		}

	/** Returns the underlying storage array for the DDoubleGrid2D.  This array
		is a one-dimensional array, in row-major order, of all the cells in
		the halo region. */
	public T[] getStorageArray() { return storage.storage; }

	/** Returns the data associated with the given point.  This point
		must lie within the halo region or an exception will be thrown.  */
	public T getLocal(Int2D p) 
		{
		if (!isHalo(p)) throwNotLocalException(p);
		return storage.storage[storage.getFlatIdx(halo.toLocalPoint(p))];
		}

	/** Returns the data associated with the given point.  This point
		must lie within the (non-halo) local region or an exception will be thrown.  */
	public void setLocal(Int2D p, T t) 
		{
		if (!isLocal(p)) throwNotLocalException(p);
		storage.storage[storage.getFlatIdx(halo.toLocalPoint(p))] = t;
		}

	public HaloGrid2D getHaloGrid() { return halo; }

	/** Returns a Promise which will eventually (immediately or within one timestep)
		hold the data located at the given point.  This point can be outside
		the loal and halo regions. */
	public Promised get(Int2D p) 
		{
		if (isHalo(p))
//			try {
				return new Promise(storage.storage[storage.getFlatIdx(halo.toLocalPoint(p))]);
//			} catch (RemoteException e) {
//				throw new RuntimeException(e);
//			}
		else
			return halo.getFromRemote(p);
		}

	/** Sets the data located at the given point.  This point can be outside
		the local and halo regions; if so, it will be set after the end of this timestep.  */
	public void set(Int2D p, T val) 
		{
		if (isLocal(p))
			storage.storage[storage.getFlatIdx(halo.toLocalPoint(p))] = val;
		else
			halo.addToRemote(p, val);
		}

	/** Sets an agent to be located at the given point and schedules it.  This point can be outside
		the local and halo regions; if so, it will be set after the end of this timestep.  */
	public void setAgent(Int2D p, T agent, double time, int ordering) 
		{
		Stopping a = (Stopping) agent;		// may generate a runtime error
		if (isLocal(p))
			{
			storage.storage[storage.getFlatIdx(halo.toLocalPoint(p))] = agent;
			state.schedule.scheduleOnce(time, ordering, a); 
			}
		else
			{
			halo.addAgent(p, agent, ordering, time);
			}
		}

	/** Sets an agent to be located at the given point and schedules it repeating.  This point can be outside
		the local and halo regions; if so, it will be set after the end of this timestep.  */
	public void setAgent(Int2D p, T agent, double time, int ordering, double interval) 
		{
		Stopping a = (Stopping) agent;		// may generate a runtime error
		if (isLocal(p))
			{
			storage.storage[storage.getFlatIdx(halo.toLocalPoint(p))] = agent;
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
	public void removeAgent(Int2D p, T agent) 
		{
		if (agent == null) return;
		
		// will this work or is Java too smart?
		DObject a = (DObject) agent;	// may generate a runtime error
		Stopping b = (Stopping) a;		// may generate a runtime error
		
		if (isLocal(p))
			{
			if (storage.storage[storage.getFlatIdx(halo.toLocalPoint(p))] == agent)
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
					throw new RuntimeException("Cannot remove agent " + a + " from " + p + " because it is wrapped in a Stoppable other than a DistributedIterativeRepeat or DistributedTenativeStep.  This should not happen.");
					}
				storage.storage[storage.getFlatIdx(halo.toLocalPoint(p))] = null;
				}
			}
		else
			{
			halo.removeAgent(p, a.ID());
			}
		}
		
		 
	/** Moves an agent from one location to another, possibly rescheduling it if the new location is remote.
	  	The [from] location must be local, but the [to] location can be outside
		the local and halo regions; if so, it will be set and rescheduled after the end of this timestep.
		If the agent is not presently AT from, then the from location is undisturbed.  Otherwise the from location
		is nulled out. */
	public void moveAgent(Int2D from, Int2D to, T agent) 
		{
		if (agent == null)
			{
			throw new RuntimeException("Cannot move null agent from " + from + " to " + to);
			}
		else if (isLocal(from))
			{
			int fromidx = storage.getFlatIdx(halo.toLocalPoint(from));
			
			if (from.equals(to))
				{
				// do nothing
				}
			else if (isLocal(to))
				{
				// This situation is easy -- we just move the agent and keep him on our schedule, done and done
				storage.storage[storage.getFlatIdx(halo.toLocalPoint(to))] = agent;
				if (storage.storage[fromidx] == agent) storage.storage[fromidx] = null;
				}
			else
				{
				// Here we have to move the agent remotely and reschedule him
				Stopping a = (Stopping) agent;			// may throw exception if it's not really an agent
				Stoppable stop = a.getStoppable();
				if (stop == null)
					{
					// we're done, just move it but don't bother rescheduling
					halo.addToRemote(to, agent);
					if (storage.storage[fromidx] == agent) storage.storage[fromidx] = null;
					}
				else if (stop instanceof DistributedTentativeStep)
					{
					DistributedTentativeStep _stop = (DistributedTentativeStep)stop;
					double time = _stop.getTime();
					int ordering = _stop.getOrdering();
					_stop.stop();
					if (time > state.schedule.getTime())  // scheduled in the future
						{
						halo.addAgent(to, agent, ordering, time);
						if (storage.storage[fromidx] == agent) storage.storage[fromidx] = null;
						}
					else	// this could theoretically happen because TentativeStep doesn't null out its agent after step()
						{
						// we're done, just move it
						halo.addToRemote(to, agent);
						if (storage.storage[fromidx] == agent) storage.storage[fromidx] = null;
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
					halo.addAgent(to, agent, ordering, time, interval);
					if (storage.storage[fromidx] == agent) storage.storage[fromidx] = null;
					}
				else
					{
					throw new RuntimeException("Cannot move agent " + a + " from " + from + " to " + to + " because it is wrapped in a Stoppable other than a DistributedIterativeRepeat or DistributedTenativeStep.  This should not happen.");
					}
				}
			}
		else
			{
			throw new RuntimeException("Cannot move agent " + agent + " from " + from + " to " + to + " because <from> is not local.");
			}
		}
	}
