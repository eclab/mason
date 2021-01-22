package sim.field.grid;

import java.rmi.RemoteException;
import java.util.ArrayList;
import sim.engine.DObject;
import sim.engine.DSimState;
import sim.engine.DistributedIterativeRepeat;
import sim.engine.DistributedTentativeStep;
import sim.engine.Stoppable;
import sim.engine.Stopping;
import sim.field.DAbstractGrid2D;
import sim.field.HaloGrid2D;
import sim.field.Promise;
import sim.field.RemoteFulfillable;
import sim.field.partitioning.Partition;
import sim.field.storage.DenseGridStorage;
import sim.util.*;

/**
 * A grid that contains lists of objects of type T. Analogous to Mason's
 * DenseGrid2D
 *
 * @param <T> Type of object stored in the grid
 */
public class DDenseGrid2D<T extends DObject> extends DAbstractGrid2D 
	{
	private static final long serialVersionUID = 1L;

	private HaloGrid2D<T, DenseGridStorage<T>> halo;
	DenseGridStorage<T> storage;
		
	public DDenseGrid2D(DSimState state) 
		{
		super(state);
		storage = new DenseGridStorage<T>(state.getPartition().getBounds());
		
		try 
		{
			halo = new HaloGrid2D<>(storage, state);
		} catch (RemoteException e) 
		{
			throw new RuntimeException(e);
		}
	}

	/** Returns the underlying storage array for the DDenseGrid2D.  This array
		is a one-dimensional array, in row-major order, of all the cells in
		the halo region.  Each cell is either null or is an arraylist of objects. */
	public ArrayList<T>[] getStorageArray() { return storage.storage; }

	/** Returns true if the data is located at the given point.  This point
		must lie within the halo region or an exception will be thrown.  */
	public boolean containsLocal(Int2D p, T t) 
		{
		if (!isHalo(p)) throwNotLocalException(p);
		ArrayList<T> list = storage.storage[storage.getFlatIdx(halo.toLocalPoint(p))];
		if (list == null) return false;
		else return (list.contains(t));
		}

	/** Returns the data associated with the given point.  This point
		must lie within the halo region or an exception will be thrown.  
		The data is in the form of an ArrayList of objects, or null. */
	public ArrayList<T> getLocal(Int2D p) 
		{
		if (!isHalo(p)) throwNotLocalException(p);
		return storage.storage[storage.getFlatIdx(halo.toLocalPoint(p))];
		}

	/** Sets the data associated with the given point.  This point
		must lie within the (non-halo) local region or an exception will be thrown.  
		The data is in the form of an ArrayList of objects, or null. */
	public void setLocal(Int2D p, ArrayList<T> t) 
		{
		if (!isLocal(p)) throwNotLocalException(p);
		storage.storage[storage.getFlatIdx(halo.toLocalPoint(p))] = t;
		}

	/** Adds an object to the given point. This point
		must lie within the (non-halo) local region or an exception will be thrown.  */
	public void addLocal(Int2D p, T t) 
		{
		if (!isLocal(p)) throwNotLocalException(p);
		ArrayList<T>[] array = storage.storage;
		int idx = storage.getFlatIdx(halo.toLocalPoint(p));

		if (array[idx] == null)
			array[idx] = new ArrayList<T>();

		array[idx].add(t);
		//System.out.println(t+" added to point "+p);
		}
		
		
	boolean removeFast(ArrayList<T> list, int pos)
		{
		int top = list.size() - 1;
		if (top != pos)
			list.set(pos, list.get(top));
		return list.remove(top) != null;
		}

	boolean removeFast(ArrayList<T> list, T t)
		{
		int pos = list.indexOf(t);
		if (pos >= 0)
			return removeFast(list, pos);
		else return (pos >= 0);
		}

	/** Removes an object from the given point. This point
		must lie within the (non-halo) local region or an exception will be thrown.  
		If the object does not exist here, FALSE is returned. */
	public boolean removeLocal(Int2D p, T t) 
	{
		if (!isLocal(p)) throwNotLocalException(p);
		ArrayList<T>[] array = storage.storage;
		int idx = storage.getFlatIdx(halo.toLocalPoint(p));
		if (array[idx] != null)
			{
//			return array[idx].remove(t);
			if (removeFast(array[idx], t))
				{
				if (array[idx].size() == 0 && storage.removeEmptyBags)
					array[idx] = null;
				return true;
				}
			else
				{
				return false;
				}
			}
		return false;
	}

	/** Removes all instances of an object from the given point. This point
		must lie within the (non-halo) local region or an exception will be thrown.  
		If the object does not exist here, FALSE is returned. */
	public boolean removeMultiplyLocal(Int2D p, T t) 
		{
		ArrayList<T>[] array = storage.storage;
		int idx = storage.getFlatIdx(halo.toLocalPoint(p));
		boolean found = false;
		
		ArrayList<T> list = array[idx];

		if (array[idx] != null)
			{
			for(int i = 0; i < list.size(); i++)
				{
				if (list.get(i) == t)
					{
					found = true;
					removeFast(list, t);
					i--;		// do it again
					}
				}

			if (found && array[idx].size() == 0 && storage.removeEmptyBags)
				array[idx] = null;
			}
		return found;
	}
	
	/** Removes all objects from the given point. This point
		must lie within the (non-halo) local region or an exception will be thrown.  
		If no objects exist here, FALSE is returned.   */
	public boolean removeAllLocal(Int2D p) 
		{
		ArrayList<T>[] array = storage.storage;
		int idx = storage.getFlatIdx(halo.toLocalPoint(p));
		
		if (array[idx] != null)
			{
			boolean ret = (array[idx].size() == 0);
			if (storage.removeEmptyBags)
				array[idx] = null;
			else
				array[idx].clear();
			return ret;
 			}
 		else return false;
		}

	public HaloGrid2D getHaloGrid() { return halo; }

	/** Returns a Promise which will eventually (immediately or within one timestep)
		hold the data (the ENTIRE ArrayList) located at the given point.  This point can be outside
		the local and halo regions. */
	public RemoteFulfillable get(Int2D p) 
		{
		if (isHalo(p))
			try {
				return new Promise(storage.storage[storage.getFlatIdx(halo.toLocalPoint(p))]);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		else
			return halo.getFromRemote(p);
		}

	/** Returns a Promise which will eventually (immediately or within one timestep)
		hold the data (which must be a DObject) requested if it is located, else null.  This point can be outside
		the local and halo regions. */
	public RemoteFulfillable get(Int2D p, T t) 
		{
		DObject obj = (DObject) t;		// this may throw a runtime exception
		if (isHalo(p))
			{
			try {
				if (containsLocal(p, t)) 
					return new Promise(obj);
				else
					return new Promise(null);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
				}
			}
		else
			return halo.getFromRemote(p, obj.getID());
		}

	/** Adds the data to the given point.  This point can be outside
		the local and halo regions; if so, it will be added after the end of this timestep.  */
	public void add(Int2D p, T t) 
		{
		if (isLocal(p))
			addLocal(p, t);
		else
			halo.addToRemote(p, t);
		}

	/** Removes the data (which must be a DObject) from the given point.  This point can be outside
		the local and halo regions. */
	public void remove(Int2D p, T t) 
		{
		DObject obj = (DObject) t;		// this may throw a runtime exception
		if (isLocal(p))
			removeLocal(p, t);
		else
			halo.removeFromRemote(p, obj.getID());
		}
		
//	/** Removes the data (which must be a DObject) from the given point.  This point can be outside
//		the local and halo regions. */
//	public void removeMultiply(Int2D p, T t) 
//		{
//		DObject obj = (DObject) t;		// this may throw a runtime exception
//		if (isLocal(p))
//			removeMultiplyLocal(p, t);
//		else
//			halo.removeFromRemote(p, obj.getID(), true);
//		}

	/** Removes all data from the given point. This point can be outside the local and halo regions. */
	public void removeAll(Int2D p) 
		{
		if (isLocal(p))
			removeAllLocal(p);
		else
			halo.removeFromRemote(p);			// FIXME: maybe rename to removeAllFromRemote?
		}

	/** Adds an agent to the given point and schedules it.  This point can be outside
		the local and halo regions; if so, it will be set after the end of this timestep.  */
	public void addAgent(Int2D p, T agent, double time, int ordering) 
		{
		if (agent == null)
			{
			throw new RuntimeException("Cannot add null agent to " + p);
			}
		Stopping a = (Stopping) agent;		// may generate a runtime error
		if (isLocal(p))
			{
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
	public void addAgent(Int2D p, T agent, double time, int ordering, double interval) 
		{
		if (agent == null)
			{
			throw new RuntimeException("Cannot add null agent to " + p);
			}
		Stopping a = (Stopping) agent;		// may generate a runtime error
		if (isLocal(p))
			{
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
	public void removeAgent(Int2D p, T agent) 
		{
		if (agent == null) return;
		
		// will this work or is Java too smart?
		DObject a = (DObject) agent;	// may generate a runtime error
		Stopping b = (Stopping) a;		// may generate a runtime error
		
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
				throw new RuntimeException("Cannot remove agent " + a + " from " + p + " because it is wrapped in a Stoppable other than a DistributedIterativeRepeat or DistributedTenativeStep.  This should not happen.");
				}
			removeLocal(p, agent);
			}
		else
			{
			halo.removeAgent(p, a.getID());
			}
		}
		
//	/** Removes the given agent from the given point repeatedly and stops it.  This point can be outside
//		the local and halo regions; if so, it will be set after the end of this timestep.  
//		The agent must be a DObject and a Stopping: realistically this means it should
//		be a DSteppable. */ 
//	public void removeAgentMultiply(Int2D p, T agent) 
//		{
//		if (agent == null) return;
//		
//		// will this work or is Java too smart?
//		DObject a = (DObject) agent;	// may generate a runtime error
//		Stopping b = (Stopping) a;		// may generate a runtime error
//		
//		if (isLocal(p))
//			{
//			Stoppable stop = ((Stopping)b).getStoppable();
//			if (stop == null)
//				{
//				// we're done
//				}
//			else if ((stop instanceof DistributedTentativeStep))
//				{
//				((DistributedTentativeStep)stop).stop();
//				}
//			else if ((stop instanceof DistributedIterativeRepeat))
//				{
//				((DistributedIterativeRepeat)stop).stop();
//				}
//			else
//				{
//				throw new RuntimeException("Cannot remove agent " + a + " from " + p + " because it is wrapped in a Stoppable other than a DistributedIterativeRepeat or DistributedTenativeStep.  This should not happen.");
//				}
//			removeMultiplyLocal(p, agent);
//			}
//		else
//			{
//			halo.removeAgent(p, a.getID(), true);
//			}
//		}

	/** Removes all agents and objects from the given point repeatedly and stops them if they are agents.  This point can be outside
		the local and halo regions; if so, it will be set after the end of this timestep.  
		To be stopped, agents must be Stopping. */ 
	public void removeAllAgentsAndObjects(Int2D p) 
		{
		if (isLocal(p))
			{
			ArrayList<T> objs = getLocal(p);
			if (objs != null)
				{
				for(T obj : objs)
					{
					if (obj instanceof Stopping)
						{
						Stoppable stop = ((Stopping)obj).getStoppable();
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
							throw new RuntimeException("Cannot remove agent " + stop + " from " + p + " because it is wrapped in a Stoppable other than a DistributedIterativeRepeat or DistributedTenativeStep.  This should not happen.");
							}
						}
					}
				}
			removeAllLocal(p);
			}
		else
			halo.removeAllAgentsAndObjects(p);
		}

	/** Moves an agent from one location to another, possibly rescheduling it if the new location is remote.
	  	The [from] location must be local, but the [to] location can be outside
		the local and halo regions; if so, it will be set and rescheduled after the end of this timestep.
		If the agent is not presently AT from, then the from location is undisturbed. */
	public void moveAgent(Int2D from, Int2D to, T agent) 
		{
		if (agent == null)
			{
			throw new RuntimeException("Cannot move null agent from " + from + " to " + to);
			}
		else if (isLocal(from))
			{
			if (from.equals(to))
				{
				// do nothing
				}
			else if (isLocal(to))
				{
				removeLocal(from, agent);
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
					removeLocal(from, agent);
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
						removeLocal(from, agent);
						halo.addAgent(to, agent, ordering, time);
						}
					else	// this could theoretically happen because TentativeStep doesn't null out its agent after step()
						{
						// we're done, just move it
						removeLocal(from, agent);
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
					removeLocal(from, agent);
					//System.out.println(a+" addedAgent at"+ to+" (4)");
					halo.addAgent(to, agent, ordering, time, interval);
					}
				else
					{
					throw new RuntimeException("Cannot move agent " + a + " from " + from + " to " + to + " because it is wrapped in a Stoppable other than a DistributedIterativeRepeat or DistributedTenativeStep.  This should not happen.");
					}
				}
			}
		else
			{
			System.out.println("storage shape : "+ this.storage.getShape());
			System.out.println("halo origPart : "+ getHaloBounds());
			
			//is this agent in local storage?
			Boolean found = false;
			for (ArrayList<T> li : this.storage.storage)
			{   if (li != null) {
				    for (T my_agent : li) {
					    if (my_agent.equals(agent)){
						    System.out.println("agent"+agent+"is in this storage at point "+from);
						    found = true;
						    //System.exit(-1);
						    //throw new RuntimeException("exit this");

					    }
				    }}}
			    if (found == false) {
			    	System.out.println(agent+" not in storage at all");
			    }
				    
			
			
			
			throw new RuntimeException("Cannot move agent " + agent + " from " + from + " to " + to + " because <from> is not local.");
			}
		}
	}
