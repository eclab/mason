package sim.field.grid;

import java.rmi.RemoteException;
import java.util.ArrayList;
import sim.engine.*;
import sim.field.*;
import sim.field.partitioning.*;
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
		storage = new DenseGridStorage<T>(state.getPartition().getHaloBounds());
		try 
		{
			halo = new HaloGrid2D<>(storage, state);
		} 
		catch (RemoteException e) 
		{
			throw new RuntimeException(e);
		}
	}

	/** Returns the underlying storage array for the DDenseGrid2D.  This array
		is a one-dimensional array, in row-major order, of all the cells in
		the halo region.  Each cell is either null or is an arraylist of objects. */
	public ArrayList<T>[] getStorageArray()
		{
		return storage.storage;
		}

	/** Returns true if the data is located at the given point, which must be within the halo region.  */
	public boolean containsLocal(Int2D p, T t) 
		{
		ArrayList<T> list = storage.storage[storage.getFlatIndex(storage.toLocalPoint(p))];
		if (list == null) return false;
		else return (list.contains(t));
		}

	/** Returns true if the data is located at the given point, which must be within the halo region.  */
	public boolean containsLocal(Int2D p, long id) 
		{
		ArrayList<T> list = storage.storage[storage.getFlatIndex(storage.toLocalPoint(p))];
		if (list == null) return false;
		else 
			{
			for(T elt : list)
				{
				if (elt.ID() == id)
					return true;
				}
			return false;
			}
		}


	/** Returns the data associated with the given point.  This point
		must lie within the halo region or an exception will be thrown.  
		The data is in the form of an ArrayList of objects, or null. 
		You do not own this ArrayList: it may be modified later, so if you 
		need data from it, copy the data or the list itself.  */
	public ArrayList<T> getAllLocal(Int2D p) 
		{
		if (!isHalo(p)) throwNotLocalException(p);
		return storage.storage[storage.getFlatIndex(storage.toLocalPoint(p))];
		}

	/** Sets the data associated with the given point.  This point
		must lie within the (non-halo) local region or an exception will be thrown.  
		The data is in the form of an ArrayList of objects, or null. 
		You no longer own this ArrayList once you have passed it in.  */
	public void setAllLocal(Int2D p, ArrayList<T> t) 
		{
		if (!isLocal(p)) throwNotLocalException(p);
		storage.storage[storage.getFlatIndex(storage.toLocalPoint(p))] = t;
		}

	/** Adds an object to the given point. This point
		must lie within the (non-halo) local region or an exception will be thrown.  */
	public void addLocal(Int2D p, T t) 
		{
		if (!isLocal(p)) throwNotLocalException(p);
		ArrayList<T>[] array = storage.storage;
		int idx = storage.getFlatIndex(storage.toLocalPoint(p));

		if (array[idx] == null)
			array[idx] = new ArrayList<T>();

		array[idx].add(t);
		}

	// swaps to the top and removes the top element		
	boolean removeFast(ArrayList<T> list, int pos)
		{
		int top = list.size() - 1;
		if (top != pos)
			list.set(pos, list.get(top));
		return list.remove(top) != null;
		}

	// swaps to the top and removes the top element		
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
		int idx = storage.getFlatIndex(storage.toLocalPoint(p));
		if (array[idx] != null)
			{
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
		int idx = storage.getFlatIndex(storage.toLocalPoint(p));
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
		int idx = storage.getFlatIndex(storage.toLocalPoint(p));
		
		if (array[idx] != null)
			{
			boolean ret = (array[idx].size() == 0);
			if (storage.removeEmptyBags)
				array[idx] = null;
			else
				array[idx].clear();
			return ret;
 			}
 		else
 			return false;
		}

	public HaloGrid2D getHaloGrid()
		{
		return halo;
		}

	/** Returns a Promise which will eventually (immediately or within one timestep)
		hold the data (the ENTIRE ArrayList) located at the given point.  This point can be outside
		the local and halo regions. */
	public Promised getAll(Int2D p) 
		{
		if (isHalo(p))
			return new Promise(storage.storage[storage.getFlatIndex(storage.toLocalPoint(p))]);
		else
			return halo.getFromRemote(p);
		}

	/** Returns a Promise which will eventually (immediately or within one timestep)
		hold the data (which must be a DObject) requested if it is located, else null.  
		This point can be outside the local and halo regions. */
	public Promised get(Int2D p, T t) 
		{
		DObject obj = (DObject) t;		// this may throw a runtime exception
		if (isHalo(p) || isHalo(toHaloToroidal(p)))
			{
			return new Promise(containsLocal(p, t) ? obj : null);
			}
		
		//elif antiwraparound point in halo
		    //return promise for 
		    //toHaloToroidal
		
		else
			return halo.getFromRemote(p, obj.ID());
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
			halo.removeFromRemote(p, obj.ID());
		}
		
//	/** Removes the data (which must be a DObject) from the given point.  This point can be outside
//		the local and halo regions. */
//	public void removeMultiply(Int2D p, T t) 
//		{
//		DObject obj = (DObject) t;		// this may throw a runtime exception
//		if (isLocal(p))
//			removeMultiplyLocal(p, t);
//		else
//			halo.removeFromRemote(p, obj.ID(), true);
//		}

	/** Removes all data from the given point. This point can be outside the local and halo regions. 
		Note that if agents are present at this point, and the point is local, they will not be stopped, but if the point is
		remote, they *will* be stopped.  In general, this method should only be used for non-Agent objects or for local
		Agents that you don't want to stop.  For Agents you want to stop, call removeAllAgentsAndObjects(p). */
	public void removeAll(Int2D p) 
		{
		if (isLocal(p))
			removeAllLocal(p);
		else
			halo.removeAllFromRemote(p);
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
			halo.addAgentToRemote(p, agent, ordering, time);
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
			halo.addAgentToRemote(p, agent, ordering, time, interval);
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
			Stoppable stop = b.getStoppable();
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
			halo.removeAgent(p, a.ID());
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
//			halo.removeAgent(p, a.ID(), true);
//			}
//		}

	/** Removes all agents and objects from the given point repeatedly and stops them if they are agents.  This point can be outside
		the local and halo regions; if so, it will be set after the end of this timestep.  
		To be stopped, agents must be Stopping. */ 
	public void removeAllAgentsAndObjects(Int2D p) 
		{
		if (isLocal(p))
			{
			ArrayList<T> objs = getAllLocal(p);
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
			halo.removeAllFromRemote(p);
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
				//System.out.println(agent+" local_move");
				removeLocal(from, agent);
				addLocal(to, agent);
				}
			else
				{
				
				//System.out.println(agent+" remote_move");

				
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
						halo.addAgentToRemote(to, agent, ordering, time);
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
					halo.addAgentToRemote(to, agent, ordering, time, interval);
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
			{
				if (li != null)
				{
				    for (T my_agent : li)
				    {
					    if (my_agent.equals(agent))
					    {
						    System.out.println("agent"+agent+"is in this storage at point "+from);
						    found = true;
						    //System.exit(-1);
						    //throw new RuntimeException("exit this");

					    }
				    }
				    }
				}
			    if (found == false)
			    {
			    	System.out.println(agent+" not in storage at all");
			    }
				    
			
			
			
			throw new RuntimeException("Cannot move agent " + agent + " from " + from + " to " + to + " because <from> is not local.");
			}
		}








    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y) ) <= dist, This region forms a
     * square 2*dist+1 cells across, centered at (X,Y).  If dist==1, this
     * is equivalent to the so-called "Moore Neighborhood" (the eight neighbors surrounding (X,Y)), plus (X,Y) itself.
     * Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     *
     * <p>Then places into the resulting ArrayList any Objects which fall on one of these <x,y> locations, clearning it first.
     * <b>Note that the order and size of the resulting ArrayList may not correspond to the X and Y bags.</b>  If you want
     * all three bags to correspond (x, y, object) then use getNeighborsAndCorrespondingPositionsMaxDistance(...)
     * Returns the resulting ArrayList.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p>The distance (dist) may be no larger than the Area of Interest (AOI), else an exception will be thrown.
     * The neighbors may leak out into the halo region of your partition.
     *
     * <p>You can also opt to include the origin -- that is, the (x,y) point at the center of the neighborhood -- in the neighborhood results.
     */
    public ArrayList<T> getMooreNeighbors( final int x, final int y, final int dist, boolean includeOrigin, ArrayList<T> result, IntBag xPos, IntBag yPos )
        {
        if (dist > halo.getPartition().getAOI()) throw new RuntimeException("Distance " + dist + " is larger than AOI " + halo.getPartition().getAOI());

        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getMooreLocations( x, y, dist, UNBOUNDED, includeOrigin, xPos, yPos );
        return getObjectsAtLocations(xPos,yPos,result);
        }


    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y) ) <= dist.  This region forms a
     * square 2*dist+1 cells across, centered at (X,Y).  If dist==1, this
     * is equivalent to the so-called "Moore Neighborhood" (the eight neighbors surrounding (X,Y)), plus (X,Y) itself.
     *
     * <p>For each Object which falls within this distance, adds the X position, Y position, and Object into the
     * xPos, yPos, and resulting ArrayList, clearing them first.  
     * Some <X,Y> positions may not appear
     * and that others may appear multiply if multiple objects share that positions.  Compare this function
     * with getNeighborsMaxDistance(...).
     * Returns the resulting ArrayList.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p>The distance (dist) may be no larger than the Area of Interest (AOI), else an exception will be thrown.
     * The neighbors may leak out into the halo region of your partition.
     *
     * <p>You can also opt to include the origin -- that is, the (x,y) point at the center of the neighborhood -- in the neighborhood results.
     */
    public ArrayList<T> getMooreNeighborsAndLocations(final int x, final int y, final int dist, boolean includeOrigin, ArrayList<T> result, IntBag xPos, IntBag yPos)
        {
        if (dist > halo.getPartition().getAOI()) throw new RuntimeException("Distance " + dist + " is larger than AOI " + halo.getPartition().getAOI());

        if (xPos == null)
            xPos = new IntBag();
        if (yPos == null)
            yPos = new IntBag();

        getMooreLocations( x, y, dist, UNBOUNDED, includeOrigin, xPos, yPos );
        reduceObjectsAtLocations( xPos,  yPos,  result);
        return result;
        }



    /**
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) <= dist.  This region forms a diamond
     * 2*dist+1 cells from point to opposite point inclusive, centered at (X,Y).  If dist==1 this is
     * equivalent to the so-called "Von-Neumann Neighborhood" (the four neighbors above, below, left, and right of (X,Y)),
     * plus (X,Y) itself.
     *
     * <p>Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     * Then places into the resulting ArrayList any Objects which fall on one of these <x,y> locations, clearning it first.
     * Note that the order and size of the resulting ArrayList may not correspond to the X and Y bags.  If you want
     * all three bags to correspond (x, y, object) then use getNeighborsAndCorrespondingPositionsHamiltonianDistance(...)
     * Returns the resulting ArrayList (constructing one if null had been passed in).
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p>The distance (dist) may be no larger than the Area of Interest (AOI), else an exception will be thrown.
     * The neighbors may leak out into the halo region of your partition.
     *
     * <p>You can also opt to include the origin -- that is, the (x,y) point at the center of the neighborhood -- in the neighborhood results.
     */
    public ArrayList<T> getVonNeumannNeighbors( final int x, final int y, final int dist, boolean includeOrigin, ArrayList<T> result, IntBag xPos, IntBag yPos )
        {
        if (dist > halo.getPartition().getAOI()) throw new RuntimeException("Distance " + dist + " is larger than AOI " + halo.getPartition().getAOI());

        if (xPos == null)
            xPos = new IntBag();
        if (yPos == null)
            yPos = new IntBag();

        getVonNeumannLocations( x, y, dist, UNBOUNDED, includeOrigin, xPos, yPos );
        return getObjectsAtLocations(xPos,yPos,result);
        }



    /**
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) <= dist.  This region forms a diamond
     * 2*dist+1 cells from point to opposite point inclusive, centered at (X,Y).  If dist==1 this is
     * equivalent to the so-called "Von-Neumann Neighborhood" (the four neighbors above, below, left, and right of (X,Y)),
     * plus (X,Y) itself.
     *
     * <p>For each Object which falls within this distance, adds the X position, Y position, and Object into the
     * xPos, yPos, and resulting ArrayList, clearing them first.  
     * Some <X,Y> positions may not appear
     * and that others may appear multiply if multiple objects share that positions.  Compare this function
     * with getNeighborsMaxDistance(...).
     * Returns the resulting ArrayList.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p>The distance (dist) may be no larger than the Area of Interest (AOI), else an exception will be thrown.
     * The neighbors may leak out into the halo region of your partition.
     *
     * <p>You can also opt to include the origin -- that is, the (x,y) point at the center of the neighborhood -- in the neighborhood results.
     */
    public ArrayList<T> getVonNeumannNeighborsAndLocations(final int x, final int y, final int dist, boolean includeOrigin, ArrayList<T> result, IntBag xPos, IntBag yPos)
        {
        if (dist > halo.getPartition().getAOI()) throw new RuntimeException("Distance " + dist + " is larger than AOI " + halo.getPartition().getAOI());

        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getVonNeumannLocations( x, y, dist, UNBOUNDED, includeOrigin, xPos, yPos );
        reduceObjectsAtLocations( xPos,  yPos,  result);
        return result;
        }



    /**
     * Gets all neighbors located within the hexagon centered at (X,Y) and 2*dist+1 cells from point to opposite point 
     * inclusive.
     * If dist==1, this is equivalent to the six neighbors immediately surrounding (X,Y), 
     * plus (X,Y) itself.
     *
     * <p>Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     * Then places into the resulting ArrayList any Objects which fall on one of these <x,y> locations, clearning it first.
     * Note that the order and size of the resulting ArrayList may not correspond to the X and Y bags.  If you want
     * all three bags to correspond (x, y, object) then use getNeighborsAndCorrespondingPositionsHamiltonianDistance(...)
     * Returns the resulting ArrayList (constructing one if null had been passed in).
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p>The distance (dist) may be no larger than the Area of Interest (AOI), else an exception will be thrown.
     * The neighbors may leak out into the halo region of your partition.
     *
     * <p>You can also opt to include the origin -- that is, the (x,y) point at the center of the neighborhood -- in the neighborhood results.
     */
    public ArrayList<T> getHexagonalNeighbors( final int x, final int y, final int dist, boolean includeOrigin, ArrayList<T> result, IntBag xPos, IntBag yPos )
        {
        if (dist > halo.getPartition().getAOI()) throw new RuntimeException("Distance " + dist + " is larger than AOI " + halo.getPartition().getAOI());

        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getHexagonalLocations( x, y, dist, UNBOUNDED, includeOrigin, xPos, yPos );
        return getObjectsAtLocations(xPos,yPos,result);
        }
                
                
    /**
     * Gets all neighbors located within the hexagon centered at (X,Y) and 2*dist+1 cells from point to opposite point 
     * inclusive.
     * If dist==1, this is equivalent to the six neighbors immediately surrounding (X,Y), 
     * plus (X,Y) itself.
     *
     * <p>For each Object which falls within this distance, adds the X position, Y position, and Object into the
     * xPos, yPos, and resulting ArrayList, clearing them first.  
     * Some <X,Y> positions may not appear
     * and that others may appear multiply if multiple objects share that positions.  Compare this function
     * with getNeighborsMaxDistance(...).
     * Returns the resulting ArrayList.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p>The distance (dist) may be no larger than the Area of Interest (AOI), else an exception will be thrown.
     * The neighbors may leak out into the halo region of your partition.
     *
     * <p>You can also opt to include the origin -- that is, the (x,y) point at the center of the neighborhood -- in the neighborhood results.
     */
    public ArrayList<T> getHexagonalNeighborsAndLocations(final int x, final int y, final int dist, boolean includeOrigin, ArrayList<T> result, IntBag xPos, IntBag yPos)
        {
        if (dist > halo.getPartition().getAOI()) throw new RuntimeException("Distance " + dist + " is larger than AOI " + halo.getPartition().getAOI());

        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getHexagonalLocations( x, y, dist, UNBOUNDED, includeOrigin, xPos, yPos );
        reduceObjectsAtLocations( xPos,  yPos,  result);
        return result;
        }



    public ArrayList<T> getRadialNeighbors( final int x, final int y, final double dist, boolean includeOrigin,  ArrayList<T> result, IntBag xPos, IntBag yPos )
        {
        return getRadialNeighbors(x, y, dist, includeOrigin, Grid2D.ANY, true, result, xPos, yPos);
        }


    public ArrayList<T> getRadialNeighborsAndLocations( final int x, final int y, final double dist, boolean includeOrigin, ArrayList<T> result, IntBag xPos, IntBag yPos )
        {
        return getRadialNeighborsAndLocations(x, y, dist, includeOrigin, Grid2D.ANY, true, result, xPos, yPos);
        }


    public ArrayList<T> getRadialNeighbors( final int x, final int y, final double dist, boolean includeOrigin,  int measurementRule, boolean closed,  ArrayList<T> result, IntBag xPos, IntBag yPos )
        {
        if (dist > halo.getPartition().getAOI()) throw new RuntimeException("Distance " + dist + " is larger than AOI " + halo.getPartition().getAOI());

        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getRadialLocations( x, y, dist, UNBOUNDED, includeOrigin, measurementRule, closed, xPos, yPos );
        return getObjectsAtLocations(xPos,yPos,result);
        }
                

    public ArrayList<T> getRadialNeighborsAndLocations( final int x, final int y, final double dist, boolean includeOrigin,  int measurementRule, boolean closed,  ArrayList<T> result, IntBag xPos, IntBag yPos )
        {
        if (dist > halo.getPartition().getAOI()) throw new RuntimeException("Distance " + dist + " is larger than AOI " + halo.getPartition().getAOI());

        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getRadialLocations( x, y, dist, UNBOUNDED, includeOrigin, measurementRule, closed, xPos, yPos );
        reduceObjectsAtLocations( xPos,  yPos,  result);
        return getObjectsAtLocations(xPos,yPos,result);
        }

        
    // For each <xPos, yPos> location, puts all such objects into the result bag.  Modifies
    // the xPos and yPos bags so that each position corresponds to the equivalent result in
    // in the result bag.
    void reduceObjectsAtLocations(final IntBag xPos, final IntBag yPos, ArrayList<T> result)
        {
        if (result==null) result = new ArrayList<T>();
        else result.clear();

        // build new bags with <x,y> locations one per each result
        IntBag newXPos = new IntBag();
        IntBag newYPos = new IntBag();

        final int len = xPos.numObjs;
        final int[] xs = xPos.objs;
        final int[] ys = yPos.objs;

        // for each location...
        for(int i=0; i < len; i++)
            {
            ArrayList<T> temp = storage.storage[storage.getFlatIndex(xPos.objs[i], yPos.objs[i])];
            int size = temp.size();
            // for each object at that location...
            for(int j = 0; j < size; j++)
                {
                // add the result, the x, and the y
                result.add(temp.get(j));
                newXPos.add(xs[i]);
                newYPos.add(ys[i]);
                }
            }

        // dump the new IntBags into the old ones
        xPos.clear();
        xPos.addAll(newXPos);
        yPos.clear();
        yPos.addAll(newYPos);
        }


    /* For each <xPos,yPos> location, puts all such objects into the result bag.  Returns the result bag.
       If the provided result bag is null, one will be created and returned. */
    ArrayList getObjectsAtLocations(final IntBag xPos, final IntBag yPos, ArrayList<T> result)
        {
        if (result==null) result = new ArrayList<T>();
        else result.clear();

        final int len = xPos.numObjs;
        final int[] xs = xPos.objs;
        final int[] ys = yPos.objs;
        for(int i=0; i < len; i++)
            {
            // a little efficiency: add if we're 1, addAll if we're > 1, 
            // do nothing if we're 0
            ArrayList<T> temp = storage.storage[storage.getFlatIndex(xPos.objs[i], yPos.objs[i])];
            if (temp!=null)
                {
                result.addAll(temp);
                }
            }
        return result;
        }



    /**
     * Determines all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y) ) <= dist. This region forms a
     * square 2*dist+1 cells across, centered at (X,Y).  If dist==1, this
     * is equivalent to the so-called "Moore Neighborhood" (the eight neighbors surrounding (X,Y)), plus (X,Y) itself.
     * <p>Then returns, as a ArrayList, any Objects which fall on one of these <x,y> locations.
     *
     * <p>The distance (dist) may be no larger than the Area of Interest (AOI), else an exception will be thrown.
     * The neighbors may leak out into the halo region of your partition.
     *
     */
    public ArrayList<T> getMooreNeighbors( int x, int y, int dist, boolean includeOrigin )
        {
        return getMooreNeighbors(x, y, dist, includeOrigin, null, null, null);
        }



    /**
     * Determines all neighbors of a location that satisfy abs(x-X) + abs(y-Y) <= dist.  This region forms a diamond
     * 2*dist+1 cells from point to opposite point inclusive, centered at (X,Y).  If dist==1 this is
     * equivalent to the so-called "Von-Neumann Neighborhood" (the four neighbors above, below, left, and right of (X,Y)),
     * plus (X,Y) itself.
     * <p>Then returns, as a ArrayList, any Objects which fall on one of these <x,y> locations.
     *
     * <p>The distance (dist) may be no larger than the Area of Interest (AOI), else an exception will be thrown.
     * The neighbors may leak out into the halo region of your partition.
     *
     */
    public ArrayList<T> getVonNeumannNeighbors( int x, int y, int dist, boolean includeOrigin )
        {
        return getVonNeumannNeighbors(x, y, dist, includeOrigin, null, null, null);
        }




    /**
     * Determines all locations located within the hexagon centered at (X,Y) and 2*dist+1 cells from point to opposite point 
     * inclusive.
     * If dist==1, this is equivalent to the six neighboring locations immediately surrounding (X,Y), 
     * plus (X,Y) itself.
     * <p>Then returns, as a ArrayList, any Objects which fall on one of these <x,y> locations.
     *
     * <p>The distance (dist) may be no larger than the Area of Interest (AOI), else an exception will be thrown.
     * The neighbors may leak out into the halo region of your partition.
     *
     */
    public ArrayList<T> getHexagonalNeighbors( int x, int y, int dist, boolean includeOrigin )
        {
        return getHexagonalNeighbors(x, y, dist, includeOrigin, null, null, null);
        }


    public ArrayList<T> getRadialNeighbors( final int x, final int y, final double dist, boolean includeOrigin)
        {
        return getRadialNeighbors(x, y, dist, includeOrigin, null, null, null);
        }

	}
