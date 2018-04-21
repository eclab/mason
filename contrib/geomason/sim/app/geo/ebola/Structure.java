package ebola;


import sim.util.Bag;
import sim.util.Int2D;

import java.util.*;

/**
 * Created by rohansuri on 7/24/15.
 */
public class Structure
{
    protected Int2D location;
    protected EbolaBuilder.Node nearestNode;
    protected HashSet<Resident> members;//all people that go to this structure on the daily.  Could be students, household members, hospital staff, etc
    protected HashMap<Structure, Route> cachedPaths;
    int currentMembers;
    protected int capacity;
    private int admin_id;//unique within each country but not unique between countries

    public Structure(Int2D location)
    {
        this.location = location;
        members = new HashSet<>();
        cachedPaths = new HashMap<>();
    }

    public Int2D getLocation()
    {
        return location;
    }

    public void setLocation(Int2D location)
    {
        this.location = location;
    }

    public void setNearestNode(EbolaBuilder.Node node)
    {
        nearestNode = node;
    }

    public EbolaBuilder.Node getNearestNode()
    {
        return nearestNode;
    }

    public void addMembers(Bag people)
    {
        members.addAll(people);
    }

    public void addMember(Resident r)
    {
        members.add(r);
    }

    public HashSet<Resident> getMembers()
    {
        return members;
    }

    /** Uses Astar to find shortest path and keeps caches of all previously found paths.
     * @param destination Destination Structure
     * @return null if no path exist, otherwise uses AStar to find shortest path to destination
     */
    public Route getRoute(Structure destination, double speed)
    {
        if(cachedPaths.containsKey(destination))//means we have this path cached
        {
            Route route = cachedPaths.get(destination);
            return route;
        }
        else
        {
            //check if the route has already been cached for the other way (destination -> here)
            if(destination.getCachedRoutes().containsKey(this))
            {
                Route route = destination.getRoute(this, speed).reverse();//be sure to reverse the route
                cachedPaths.put(destination, route);
                return route;
            }
            else
            {
                Route route = AStar.astarPath(this.getNearestNode(), destination.getNearestNode(), speed);
                cachedPaths.put(destination, route);
                return route;
            }
        }
    }

    public void cacheRoute(Route route, Structure destination)
    {
        cachedPaths.put(destination, route);
    }

    public Map<Structure, Route> getCachedRoutes()
    {
        return cachedPaths;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getCurrentMembers() {
        return members.size();
    }


    public int getAdmin_id() {
        return admin_id;
    }

    public void setAdmin_id(int admin_id) {
        this.admin_id = admin_id;
    }
}
