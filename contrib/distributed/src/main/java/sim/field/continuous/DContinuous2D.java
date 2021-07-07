package sim.field.continuous;

import java.io.Serializable;

import java.rmi.*;
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
import sim.field.HaloGrid2D;
import sim.app.dflockers.DFlocker;
import sim.engine.*;
import sim.field.partitioning.Partition;
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
	
	public DContinuous2D(int discretization, DSimState state) 
		{
		super(state);
		storage = new ContinuousStorage<T>(state.getPartition().getHaloBounds(), discretization);
		
		try 
		{
			halo = new HaloGrid2D<>(storage, state);
		} 
		catch (RemoteException e) 
		{
			throw new RuntimeException(e);
		}
	}


	public ContinuousStorage getStorage()
	{
		return storage;
	}
	
	public HaloGrid2D getHaloGrid()
		{
		return halo;
		}
		
	/** Returns true if the real-valued point is within the local (non-halo) region.  */
	public boolean isLocal(Double2D p)
		{
		return halo.inLocal(p);
		}

	/** Returns true if the real-valued point is within the halo region.  */
	public boolean isHalo(Double2D p)
		{
		return halo.inHalo(p);
		}

	/** Returns the local (including halo region) location object with the given id, if any, else null.*/
	public Double2D getObjectLocationLocal(long id)
		{
		HashMap<Long, Double2D> map = storage.getLocations();
		return map.get(id);
		}

	/** Returns the local (including halo region) location of the given object, if any, else null.*/
	public Double2D getObjectLocationLocal(T t)
		{
		return getObjectLocationLocal(t.ID());
		}

	/** Returns true if the object is located locally, including in the halo region.  */
	public boolean containsLocal(T t) 
		{
		return (getObjectLocationLocal(t.ID()) != null);
		}

	/** Returns true if the object is located locally, including in the halo region.  */
	public boolean containsLocal(long id) 
		{
		return (getObjectLocationLocal(id) != null);
		}

	/** Returns true if the object if the given id is located exactly at the given point locally, including the halo region.  */
	public boolean containsLocal(Double2D p, long id) 
		{
		return (p.equals(getObjectLocationLocal(id)));
		}

	/** Returns true if the object is located exactly at the given point locally, including the halo region. */
	public boolean containsLocal(Double2D p, T t) 
		{
		return containsLocal(p, t.ID());
		}

	/** Returns all the local data located in discretized cell in the <i>vicinity</i> of the given point.  This point
		must lie within the halo region or an exception will be thrown.  */
	public HashMap<Long, T> getCellLocal(Double2D p) 
		{
		if (!isHalo(p)) throwNotLocalException(p);
		return storage.getCell(p);
		}

	/* Returns all the local data located <i>exactly</i> at the given point.  This point
		must lie within the halo region or an exception will be thrown.  */
/*
	public ArrayList<T> getAllLocal(Double2D p) 
		{
		HashMap<Long, T> cell = getCellLocal(p);
		ArrayList<T> reduced = new ArrayList<>();
		if (cell == null) return reduced;
		for(T t : cell.values())
			 {
			 if (p.equals(getObjectLocationLocal(t)))
			 	reduced.add(t);
			 }
		return reduced;
		}
*/
		
	/** Returns the object associated with the given ID if it stored within the halo region, else null. */
	public T getLocal(long id)
		{
		Double2D loc = getObjectLocationLocal(id);
		if (loc != null)
			return storage.getCell(loc).get(id);
		else return null;
		}

	/** Returns the object associated with the given ID only if it stored within the halo region at exactly the given point p, else null. */
	public T getLocal(Double2D p, long id)
		{
		Double2D loc = getObjectLocationLocal(id);
		if (p.equals(loc))
			return storage.getCell(loc).get(id);
		else return null;
		}
			
	/** Sets or moves the given object.  The given point must be local.  If the object
		exists at another local point, it will be removed from that point and moved to the new point. */
	public void addLocal(Double2D p, T t) 
		{
//		System.out.println("agent: " + t);
//		System.out.println("map: " + this.storage.getLocations());
		
		
		if (!isLocal(p)) throwNotLocalException(p);	
		Double2D oldLoc = getObjectLocationLocal(t);
		HashMap<Long, T> newCell = getCellLocal(p);
		

		
		if (oldLoc != null)
			{
			

			HashMap<Long, T> oldCell = getCellLocal(oldLoc);
//			System.out.println("oldcell: " + oldCell);
			if (oldCell == newCell)
				{
				// don't mess with them
				}
			else 
				{
				
				if (oldCell != null)
					{
					
					oldCell.remove(t.ID());
					if (oldCell.isEmpty() && storage.removeEmptyBags)
						{
						//storage.setCell(oldLoc, null);
						storage.setCell(oldLoc, new HashMap<Long, T>());
						}
					
					}
				
				
				if (newCell == null)
					{
					newCell = new HashMap<>();
					storage.setCell(p, newCell);
					}
				
				
				newCell.put(t.ID(), t);
				}
			

			}
			
		else
			{
			
			if (newCell == null)
				{
				newCell = new HashMap<>();
				storage.setCell(p, newCell);
				}
			newCell.put(t.ID(), t);
			
			}

		
		HashMap<Long, Double2D> map = storage.getLocations();
		map.put(t.ID(), p);
		

		}
	
	/** Removes the object of the given id, which must be local.  If it does not exist locally, this method returns FALSE. */
	public boolean removeLocal(long id)
		{
		return removeLocal(storage.getObjectLocation(id), id);
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
				cell.remove(t.ID());
			if (cell.isEmpty() && storage.removeEmptyBags)
			{
				//storage.setCell(loc, null);
				storage.setCell(loc, new HashMap<Long, T>());
			}

			return true;
			}
		}
	
	/** Removes the object of the given id, which must be local and exactly at the given location.  If it doesn't exist, returns FALSE. */
	public boolean removeLocal(Double2D p, long id)
		{
		if (!p.equals(getObjectLocationLocal(id)))
			return false;
		HashMap<Long, T> cell = getCellLocal(p);
		T t = cell.get(id);
		if (t == null) return false;
		else
			{
			if (cell != null)
				cell.remove(t.ID());
			if (cell.isEmpty() && storage.removeEmptyBags)
			{
				//storage.setCell(p, null);
     			storage.setCell(p, new HashMap<Long, T>());
			}

			return true;
			}
		}

	/** Removes the object, which must be local and exactly at the given location.  If it doesn't exist, returns FALSE. */
	public boolean removeLocal(Double2D p, T t)
		{
		return removeLocal(t.ID());
		}
		
	/** Returns a Promise which will eventually (immediately or within one timestep)
		hold the data (which must be a DObject) requested if it is located, else will hold null.  This point can be outside
		the local and halo regions. */
	public Promised get(Double2D p, long id) 
		{
		if (isHalo(p))
			return new Promise(getLocal(p, id));
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
	public void remove(Double2D p, long id) 
		{
		if (isLocal(p))
			removeLocal(id);
		else
			halo.removeFromRemote(p, id);
		}

	/** Removes the data (which must be a DObject) from the given point.  This point can be outside
		the local and halo regions. */
	public void remove(Double2D p, T t) 
		{
		remove(p, t.ID());
		}


	/** Adds an agent to the given point and schedules it.  This point can be outside
		the local and halo regions; if so, it will be set after the end of this timestep.  */
	public void addAgent(Double2D p, T agent, double time, int ordering) 
		{
//		System.out.println("addAgent");
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
			halo.addAgentToRemote(p, agent, ordering, time);
			}
		}
		
	/** Adds an agent to the given point and schedules it repeating.  This point can be outside
		the local and halo regions; if so, it will be set after the end of this timestep.  */
	public void addAgent(Double2D p, T agent, double time, int ordering, double interval) 
		{
//		System.out.println("addAgent");
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
			halo.addAgentToRemote(p, agent, ordering, time, interval);
			}
		}

	/** Removes the given agent from the given point and stops it.  This point can be outside
		the local and halo regions; if so, it will be set after the end of this timestep.  
		The agent must be a DObject and a Stopping: realistically this means it should
		be a DSteppable. */ 
	public void removeAgent(Double2D p, T agent) 
		{
		if (agent == null) return;
		
		// will this work or is Java too smart?
		Stopping b = (Stopping) agent;		// may generate a runtime error
		
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
				throw new RuntimeException("Cannot remove agent " + agent + " from " + p + " because it is wrapped in a Stoppable other than a DistributedIterativeRepeat or DistributedTenativeStep.  This should not happen.");
				}
			removeLocal(agent);
			}
		else
			{
			halo.removeAgent(p, agent.ID());
			}
		}
		
	/** Moves an agent from one location to another, possibly rescheduling it if the new location is remote.
	  	The [from] location must be local, but the [to] location can be outside
		the local and halo regions; if so, it will be set and rescheduled after the end of this timestep.
		If the agent is not presently AT from, then the from location is undisturbed. */
	public void moveAgent(Double2D to, T agent) 
		{
//		System.out.println("moveAgent getHaloGrid: " + getHaloGrid());
//    	System.out.println("moveAgent getHaloBounds: " + getHaloGrid().getHaloBounds());
//    	System.out.println("moveAgent getLocalBounds: " + getHaloGrid().getLocalBounds());
		if (agent == null)
			{
			throw new RuntimeException("Cannot move null agent to " + to);
			}
		else if (containsLocal(agent))
			{
			Double2D p = getObjectLocationLocal(agent);
			if (isLocal(to))
				{
				//removeLocal(agent);
				addLocal(to, agent);
				}
			// otherwise, within the aoi
			else
				{
				
				/*
				System.out.println("moveAgent getHaloGrid: " + getHaloGrid());
		    	System.out.println("moveAgent getHaloBounds: " + getHaloGrid().getHaloBounds());
		    	System.out.println("moveAgent getLocalBounds: " + getHaloGrid().getLocalBounds());
		    	System.out.println("moveAgent m: " + this.storage.m);
		    	System.out.println("moveAgent cells: " + this.storage.storage);
		    	
				System.out.println(agent+" moving remote to "+to);
				*/
				
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
						halo.addAgentToRemote(to, agent, ordering, time);
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
					halo.addAgentToRemote(to, agent, ordering, time, interval);
					
					}
				else
					{
					throw new RuntimeException("Cannot move agent " + a + " from " + p + " to " + to + " because it is wrapped in a Stoppable other than a DistributedIterativeRepeat or DistributedTenativeStep.  This should not happen.");
					}
				}
			}
		else
			{
			/*
	    	System.out.println("agent: " + agent);
            System.out.println("agent loc from"+((DFlocker)agent).loc);
            System.out.println("agent to"+to);

	    	System.out.println("map: " + this.getStorage().getLocations());

			System.out.println("moveAgent getHaloGrid: " + getHaloGrid());
	    	System.out.println("moveAgent getHaloBounds: " + getHaloGrid().getHaloBounds());
	    	System.out.println("moveAgent getLocalBounds: " + getHaloGrid().getLocalBounds());
	    	
	    	System.exit(-1);
	    	*/
			throw new RuntimeException("Cannot move agent " + agent + " to " + to + " because agent is not local.");
			}
		}







    
	/** Toroidal x */
	// slight revision for more efficiency
	public double tx(double x) 
	{
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
	public double ty(double y) 
	{
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
	public double stx(double x) 
	{
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
	public double sty(double y) 
	{
		if (y >= 0) 
		{
			if (y < height)
				return y;
			return y - height;
		}
		return y + height;
	}

	// some efficiency to avoid width lookups
	double _stx(double x, double width) 
	{
		if (x >= 0) 
		{
			if (x < width)
				return x;
			return x - width;
		}
		return x + width;
	}

	/** Minimum toroidal difference between two values in the X dimension. */
	public double tdx(double x1, double x2) 
	{
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
	double _sty(double y, double height) 
	{
		if (y >= 0) 
		{
			if (y < height)
				return y;
			return y - height;
		}
		return y + height;
	}

	/** Minimum toroidal difference between two values in the Y dimension. */
	public double tdy(double y1, double y2) 
	{
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
	public double tds(Double2D d1, Double2D d2) 
	{
		double dx = tdx(d1.x, d2.x);
		double dy = tdy(d1.y, d2.y);
		return (dx * dx + dy * dy);
	}

	/**
	 * Minimum Toroidal difference vector between two points. This subtracts the
	 * second point from the first and produces the minimum-length such subtractive
	 * vector, considering wrap-around possibilities as well
	 */
	public Double2D tv(Double2D d1, Double2D d2) 
	{
		return new Double2D(tdx(d1.x, d2.x), tdy(d1.y, d2.y));
	}




    /** Returns a ArrayList containing EXACTLY those objects within a certain distance of a given position, or equal to that distance, measuring
        using a circle of radius 'distance' around the given position.  Assumes non-toroidal point objects. 
        
        <p> Note: if the field is toroidal, and position is outside the boundaries, it will be wrapped
        to within the boundaries before computation.
    */

    public ArrayList<T> getNeighborsExactlyWithinDistance(Double2D position, double distance)
        {
        return getNeighborsExactlyWithinDistance(position, distance, true, true, null);
        }


    /** Returns a ArrayList containing EXACTLY those objects within a certain distance of a given position.  If 'radial' is true,
        then the distance is measured using a circle around the position, else the distance is measured using a square around
        the position (that is, it's the maximum of the x and y distances).   If 'inclusive' is true, then objects that are
        exactly the given distance away are included as well, else they are discarded.  If 'toroidal' is true, then the
        distance is measured assuming the environment is toroidal.  If the ArrayList 'result' is provided, it will be cleared and objects
        placed in it and it will be returned, else if it is null, then this method will create a new ArrayList and use that instead. 
        Assumes point objects. 
        
        <p> Note: if the field is toroidal, and position is outside the boundaries, it will be wrapped
        to within the boundaries before computation.
    */

    public ArrayList<T> getNeighborsExactlyWithinDistance(Double2D position, double distance, 
    	boolean radial, boolean inclusive, ArrayList<T> result)
        {
        if (distance > halo.getPartition().getAOI()) throw new RuntimeException("Distance " + distance + " is larger than AOI " + halo.getPartition().getAOI());

        int expectedBagSize = 1;  // in the future, pick a smarter bag size?

		ArrayList<T> objs = getNeighborsWithinDistance(position, distance, null);
		if (result == null) result = new ArrayList<T>(expectedBagSize);
		else result.clear();
		
        double distsq = distance*distance;
        if (radial) 
            for(T obj : objs)
                {
                double d = 0;
                Double2D loc = storage.getObjectLocation(obj);
                d = position.distanceSq(loc);
                if (!(d > distsq || (!inclusive && d >= distsq)))
                    result.add(obj);
                }
        else 
            for(T obj : objs)
                {
                Double2D loc = storage.getObjectLocation(obj);
                double minx = 0;
                double miny = 0;
                    minx = loc.x - position.x;
                    miny = loc.y - position.y;
                if (minx < 0) minx = -minx;
                if (miny < 0) miny = -miny;
                if (!((minx > distance || miny > distance) || (!inclusive && (minx >= distance || miny >= distance))))
                    result.add(obj);
                }
        return result;
        }

    /** Returns a bag containing AT LEAST those objects within the bounding box surrounding the
        specified distance of the specified position.  The bag could include other objects than this.
        In this case we include the object if
        any part of the bounding box could overlap into the desired region.  To do this, if nonPointObjects is
        true, we extend the search space by one extra discretization in all directions.  For small distances within
        a single bucket, this returns nine bucket's worth rather than 1, so if you know you only care about the
        actual x/y points stored, rather than possible object overlap into the distance sphere you specified,
        you'd want to set nonPointObjects to FALSE. [assumes non-toroidal, point objects] 
        
        <p> Note: if the field is toroidal, and position is outside the boundaries, it will be wrapped
        to within the boundaries before computation.
    */
    public ArrayList<T> getNeighborsWithinDistance(Double2D position, double distance)
        {
    	return getNeighborsWithinDistance(position,distance, null);
    	}

    /** Puts into the result ArrayList (and returns it) AT LEAST those objects within the bounding box surrounding the
        specified distance of the specified position.  If the result ArrayList is null, then a ArrayList is created.
        
        <p>The bag could include other objects than this.
        If toroidal, then wrap-around possibilities are also considered.
        If nonPointObjects, then it is presumed that
        the object isn't just a point in space, but in fact fills an area in space where the x/y point location
        could be at the extreme corner of a bounding box of the object.  In this case we include the object if
        any part of the bounding box could overlap into the desired region.  To do this, if nonPointObjects is
        true, we extend the search space by one extra discretization in all directions.  For small distances within
        a single bucket, this returns nine bucket's worth rather than 1, so if you know you only care about the
        actual x/y points stored, rather than possible object overlap into the distance sphere you specified,
        you'd want to set nonPointObjects to FALSE. 
        
        <p> Note: if the field is toroidal, and position is outside the boundaries, it will be wrapped
        to within the boundaries before computation.
    */
    
    public ArrayList<T> getNeighborsWithinDistance(Double2D position, double distance, ArrayList<T> result)
        {
        if (distance > halo.getPartition().getAOI()) throw new RuntimeException("Distance " + distance + " is larger than AOI " + halo.getPartition().getAOI());
        
        int expectedBagSize = 1;  // in the future, pick a smarter bag size?
        if (result!=null) result.clear();
        else result = new ArrayList<T>(expectedBagSize);
        ArrayList<T> temp;
    
    	Int2D min = storage.discretize(new Double2D(position.x - distance, position.y - distance));
    	Int2D max = storage.discretize(new Double2D(position.x + distance, position.y + distance));
    	int minX = min.x;
    	int maxX = max.x;
    	int minY = min.y;
    	int maxY = max.y;
    
		// for non-toroidal, it is easier to do the inclusive for-loops
		for(int x = minX; x<= maxX; x++)
			for(int y = minY ; y <= maxY; y++)
				{
				HashMap<Long, T> cell = storage.getDiscretizedCell(x, y);

				for(T t : cell.values())
					{
					result.add(t);
					}
				}

        return result;
        }
    
    public ArrayList<T> getAllAgentsInStorage(){
    	ArrayList<T> allAgents = new ArrayList<T>();
    	for (int i=0; i<this.storage.storage.length; i++) {
    		for (T t :this.storage.storage[i].values()) {
    			allAgents.add(t);
    		}
    	}
    	
		return allAgents;

    }
}
