package sim.field.grid;

import java.rmi.RemoteException;
import sim.engine.*;
import sim.field.*;
import sim.field.partitioning.*;
import sim.field.storage.*;
import sim.util.*;
import java.util.ArrayList;

/**
 * A grid that contains objects of type T. Analogous to Mason's ObjectGrid2D
 *
 * @param <T> Type of object stored in the grid
 */

public class DObjectGrid2D<T extends DObject> extends DAbstractGrid2D 
	{
	private static final long serialVersionUID = 1L;

	HaloGrid2D<T, ObjectGridStorage<T>> halo;
	ObjectGridStorage<T> storage;

	public DObjectGrid2D(DSimState state) 
		{
		super(state);
		storage = new ObjectGridStorage<T>(state.getPartition().getHaloBounds());
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
	public T[] getStorageArray()
		{
		return storage.storage;
		}

	/** Returns the data associated with the given point.  This point
		must lie within the halo region or an exception will be thrown.  */
	public T getLocal(Int2D p) 
		{
		if (!isHalo(p)) throwNotLocalException(p);
		return storage.storage[storage.getFlatIndex(storage.toLocalPoint(p))];
		}

	/** Returns the data associated with the given point.  This point
		must lie within the (non-halo) local region or an exception will be thrown.  */
	public void setLocal(Int2D p, T t) 
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
		the loal and halo regions. */
	public Promised get(Int2D p) 
		{
		if (isHalo(p))
			return new Promise(storage.storage[storage.getFlatIndex(storage.toLocalPoint(p))]);
		else
			return halo.getFromRemote(p);
		}

	/** Sets the data located at the given point.  This point can be outside
		the local and halo regions; if so, it will be set after the end of this timestep.  */
	public void set(Int2D p, T val) 
		{
		if (isLocal(p))
			storage.storage[storage.getFlatIndex(storage.toLocalPoint(p))] = val;
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
			storage.storage[storage.getFlatIndex(storage.toLocalPoint(p))] = agent;
			state.schedule.scheduleOnce(time, ordering, a); 
			}
		else
			{
			halo.addAgentToRemote(p, agent, ordering, time);
			}
		}

	/** Sets an agent to be located at the given point and schedules it repeating.  This point can be outside
		the local and halo regions; if so, it will be set after the end of this timestep.  */
	public void setAgent(Int2D p, T agent, double time, int ordering, double interval) 
		{
		Stopping a = (Stopping) agent;		// may generate a runtime error
		if (isLocal(p))
			{
			storage.storage[storage.getFlatIndex(storage.toLocalPoint(p))] = agent;
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
			if (storage.storage[storage.getFlatIndex(storage.toLocalPoint(p))] == agent)
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
				storage.storage[storage.getFlatIndex(storage.toLocalPoint(p))] = null;
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
			int fromidx = storage.getFlatIndex(storage.toLocalPoint(from));
			
			if (from.equals(to))
				{
				// do nothing
				}
			else if (isLocal(to))
				{
				// This situation is easy -- we just move the agent and keep him on our schedule, done and done
				storage.storage[storage.getFlatIndex(storage.toLocalPoint(to))] = agent;
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
						halo.addAgentToRemote(to, agent, ordering, time);
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
					halo.addAgentToRemote(to, agent, ordering, time, interval);
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









    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y) ) <= dist, This region forms a
     * square 2*dist+1 cells across, centered at (X,Y).  If dist==1, this
     * is equivalent to the so-called "Moore Neighborhood" (the eight neighbors surrounding (X,Y)), plus (X,Y) itself.
     * Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     *
     * <p>Then places into the result ArrayList any Objects which fall on one of these <x,y> locations, clearning it first.
     * <b>Note that the order and size of the result ArrayList may not correspond to the X and Y bags.</b>  If you want
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
        if (!isHaloToroidal(x, y, dist)) throw new RuntimeException("Distance " + dist + " is larger than AOI " + halo.getPartition().getAOI());

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
        if (!isHaloToroidal(x, y, dist)) throw new RuntimeException("Distance " + dist + " is larger than AOI " + halo.getPartition().getAOI());

        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
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
     * Then places into the result ArrayList any Objects which fall on one of these <x,y> locations, clearning it first.
     * Note that the order and size of the result ArrayList may not correspond to the X and Y bags.  If you want
     * all three bags to correspond (x, y, object) then use getNeighborsAndCorrespondingPositionsHamiltonianDistance(...)
     * Returns the result ArrayList (constructing one if null had been passed in).
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
        if (!isHaloToroidal(x, y, dist)) throw new RuntimeException("Distance " + dist + " is larger than AOI " + halo.getPartition().getAOI());

        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
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
        if (!isHaloToroidal(x, y, dist)) throw new RuntimeException("Distance " + dist + " is larger than AOI " + halo.getPartition().getAOI());

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
     * Then places into the result ArrayList any Objects which fall on one of these <x,y> locations, clearning it first.
     * Note that the order and size of the result ArrayList may not correspond to the X and Y bags.  If you want
     * all three bags to correspond (x, y, object) then use getNeighborsAndCorrespondingPositionsHamiltonianDistance(...)
     * Returns the result ArrayList (constructing one if null had been passed in).
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
        if (!isHaloToroidal(x, y, dist)) throw new RuntimeException("Distance " + dist + " is larger than AOI " + halo.getPartition().getAOI());

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
        if (!isHaloToroidal(x, y, dist)) throw new RuntimeException("Distance " + dist + " is larger than AOI " + halo.getPartition().getAOI());

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
        if (!isHaloToroidal(x, y, dist)) throw new RuntimeException("Distance " + dist + " is larger than AOI " + halo.getPartition().getAOI());

        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getRadialLocations( x, y, dist, UNBOUNDED, includeOrigin, measurementRule, closed, xPos, yPos );
        return getObjectsAtLocations(xPos,yPos,result);
        }
                

    public ArrayList<T> getRadialNeighborsAndLocations( final int x, final int y, final double dist, boolean includeOrigin,  int measurementRule, boolean closed,  ArrayList<T> result, IntBag xPos, IntBag yPos )
        {
        if (!isHaloToroidal(x, y, dist)) throw new RuntimeException("Distance " + dist + " is larger than AOI " + halo.getPartition().getAOI());

        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getRadialLocations( x, y, dist, UNBOUNDED, includeOrigin, measurementRule, closed, xPos, yPos );
        reduceObjectsAtLocations( xPos,  yPos,  result);
        return getObjectsAtLocations(xPos,yPos,result);
        }

        
    // For each <xPos, yPos> location, puts all such objects into the resulting ArrayList.  Modifies
    // the xPos and yPos bags so that each position corresponds to the equivalent result in
    // in the resulting ArrayList.
    void reduceObjectsAtLocations(final IntBag xPos, final IntBag yPos, ArrayList<T> result)
        {
        if (result==null) result = new ArrayList<T>();
        else result.clear();

        for( int i = 0 ; i < xPos.numObjs ; i++ )
            {
            T val = storage.storage[storage.getFlatIndex(xPos.objs[i], yPos.objs[i])];
            if (val != null) result.add( val );
            else
                {
                xPos.remove(i);
                yPos.remove(i);
                i--;  // back up and try the object now in the new slot
                }
            }
        }
                

   /* For each <xPos,yPos> location, puts all such objects into the resulting ArrayList.  Returns the resulting ArrayList.
       If the provided resulting ArrayList is null, one will be created and returned. */
    ArrayList getObjectsAtLocations(final IntBag xPos, final IntBag yPos, ArrayList<T> result)
        {
        if (result==null) result = new ArrayList<T>();
        else result.clear();

        for( int i = 0 ; i < xPos.numObjs ; i++ )
            {
            T val = storage.storage[storage.getFlatIndex(xPos.objs[i], yPos.objs[i])];
            if (val != null) result.add( val );
            }
        return result;
        }



    /**
     * Determines all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y) ) <= dist. This region forms a
     * square 2*dist+1 cells across, centered at (X,Y).  If dist==1, this
     * is equivalent to the so-called "Moore Neighborhood" (the eight neighbors surrounding (X,Y)), plus (X,Y) itself.
     * <p>Then returns, as a Bag, any Objects which fall on one of these <x,y> locations.
     *
     * <p>The distance (dist) may be no larger than the Area of Interest (AOI), else an exception will be thrown.
     * The neighbors may leak out into the halo region of your partition.
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
     * <p>Then returns, as a Bag, any Objects which fall on one of these <x,y> locations.
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
     * <p>Then returns, as a Bag, any Objects which fall on one of these <x,y> locations.
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
