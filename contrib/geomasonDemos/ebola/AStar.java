package ebola;


/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

// Haiti project  - searching the nearest road
import java.util.*;

import sim.field.network.Edge;
import sim.util.Heap;
import sim.util.Int2D;

@SuppressWarnings("restriction")
public class AStar {
    // static private HashMap<int[], ArrayList<Location>> cache = new HashMap(1000);

    /**
     * Assumes that both the start and end location are NODES as opposed to LOCATIONS
     * @param start
     * @param goal
     * @return
     */
    static public Route astarPath(EbolaBuilder.Node start, EbolaBuilder.Node goal, double speed) {
//        int[] cacheKey = new int[] {start.location.xLoc, start.location.yLoc, goal.location.xLoc, goal.location.yLoc};
//        if (cache.containsKey(cacheKey))
//            return cache.get(cacheKey);
//        
        // initial check
        long startTime = System.currentTimeMillis();
        if (start == null || goal == null) {
            System.out.println("Error: invalid node provided to AStar");
        }

        // containers for the metainformation about the Nodes relative to the 
        // A* search
        HashMap<EbolaBuilder.Node, AStarNodeWrapper> foundNodes =
                new HashMap<EbolaBuilder.Node, AStarNodeWrapper>();


        AStarNodeWrapper startNode = new AStarNodeWrapper(start);
        AStarNodeWrapper goalNode = new AStarNodeWrapper(goal);
        foundNodes.put(start, startNode);
        foundNodes.put(goal, goalNode);

        startNode.gx = 0;
        startNode.hx = heuristic(start, goal);
        startNode.fx = heuristic(start, goal);

        // A* containers: allRoadNodes to be investigated, allRoadNodes that have been investigated
        HashSet<AStarNodeWrapper> closedSet = new HashSet<>(10000),
                openSet = new HashSet<>(10000);
        PriorityQueue<AStarNodeWrapper> openSetQueue = new PriorityQueue<>(10000);
        openSet.add(startNode);
        openSetQueue.add(startNode);
        while(openSet.size() > 0){ // while there are reachable allRoadNodes to investigate

            //AStarNodeWrapper x = findMin(openSet); // find the shortest path so far
            AStarNodeWrapper x = openSetQueue.peek();
            if(x == null)
            {
                AStarNodeWrapper n = findMin(openSet);
            }
            if(x.node == goal ){ // we have found the shortest possible path to the goal!
                // Reconstruct the path and send it back.
                return reconstructRoute(goalNode, startNode, goalNode, speed);
            }
            openSet.remove(x); // maintain the lists
            openSetQueue.remove();
            closedSet.add(x);

            // check all the neighbors of this location
            for(Edge l: x.node.links){

                EbolaBuilder.Node n = (EbolaBuilder.Node) l.from();
                if( n == x.node )
                    n = (EbolaBuilder.Node) l.to();

                // get the A* meta information about this Node
                AStarNodeWrapper nextNode;
                if( foundNodes.containsKey(n))
                    nextNode = foundNodes.get(n);
                else{
                    nextNode = new AStarNodeWrapper(n);
                    foundNodes.put( n, nextNode );
                }

                if(closedSet.contains(nextNode)) // it has already been considered
                    continue;

                // otherwise evaluate the cost of this node/edge combo
                double tentativeCost = x.gx + (Integer) l.info;
                boolean better = false;

                if(! openSet.contains(nextNode)){
                    openSet.add(nextNode);
                    openSetQueue.add(nextNode);
                    nextNode.hx = heuristic(n, goal);
                    better = true;
                }
                else if(tentativeCost < nextNode.gx){
                    better = true;
                }

                // store A* information about this promising candidate node
                if(better){
                    nextNode.cameFrom = x;
                    nextNode.gx = tentativeCost;
                    nextNode.fx = nextNode.gx + nextNode.hx;
                }
            }

//            if(foundNodes.size()%10000 == 0)
//                System.out.println("Time = " + System.currentTimeMillis());
        }
        //System.out.println("Searched " + foundNodes.size() + " nodes but could not find it");
        return null;
    }

    /**
     * Uses Djikstra to find the closest in the list of endNodes.  Returns the endNode that is closest.
     * @param start
     * @param endNodes
     * @param max_distance the maximum distance you want to search in the road network
     * @param check_capacity determines whether we chceck the capacity of Structure
     * @return
     */
    public static Route getNearestNode(EbolaBuilder.Node start, Map<EbolaBuilder.Node, List<Structure>> endNodes, double max_distance, boolean check_capacity, double speed)
    {
        //        int[] cacheKey = new int[] {start.location.xLoc, start.location.yLoc, goal.location.xLoc, goal.location.yLoc};
//        if (cache.containsKey(cacheKey))
//            return cache.get(cacheKey);
//
        // initial check
        long startTime = System.currentTimeMillis();
        if (start == null || endNodes == null) {
            System.out.println("Error: invalid node provided to AStar");
        }

        // containers for the metainformation about the Nodes relative to the
        // A* search
        HashMap<EbolaBuilder.Node, AStarNodeWrapper> foundNodes =
                new HashMap<EbolaBuilder.Node, AStarNodeWrapper>();


        AStarNodeWrapper startNode = new AStarNodeWrapper(start);
        //AStarNodeWrapper goalNode = new AStarNodeWrapper(goal);
        foundNodes.put(start, startNode);
        //foundNodes.put(goal, goalNode);

        startNode.gx = 0;
        startNode.hx = 0;
        startNode.fx = 0;

        // A* containers: allRoadNodes to be investigated, allRoadNodes that have been investigated
        HashSet<AStarNodeWrapper> closedSet = new HashSet<>(),
                openSet = new HashSet<>();
        PriorityQueue<AStarNodeWrapper> openSetQueue = new PriorityQueue<>(10000);


        openSet.add(startNode);
        openSetQueue.add(startNode);

        while(openSet.size() > 0){ // while there are reachable allRoadNodes to investigate

            //AStarNodeWrapper x = findMin(openSet); // find the shortest path so far
            AStarNodeWrapper x = openSetQueue.peek();
            //check if we have reached maximum route distance
            if(x.hx > max_distance)
                return null;
            if(x == null)
            {
                AStarNodeWrapper n = findMin(openSet);
            }
            if(endNodes.containsKey(x.node)){ // we have found the shortest possible path to the goal!
                if(check_capacity)//check if this structure is already full!
                {
                    for(Structure structure: endNodes.get(x.node))
                        if(!(structure.getMembers().size() >= structure.getCapacity()))//means it is not full
                            return reconstructRoute(x, startNode, x, speed);
                }
                else // Reconstruct the path and send it back.
                    return reconstructRoute(x, startNode, x, speed);
            }
            openSet.remove(x); // maintain the lists
            openSetQueue.remove();
            closedSet.add(x);

            // check all the neighbors of this location
            for(Edge l: x.node.links){

                EbolaBuilder.Node n = (EbolaBuilder.Node) l.from();
                if( n == x.node )
                    n = (EbolaBuilder.Node) l.to();

                // get the A* meta information about this Node
                AStarNodeWrapper nextNode;
                if( foundNodes.containsKey(n))
                    nextNode = foundNodes.get(n);
                else{
                    nextNode = new AStarNodeWrapper(n);
                    foundNodes.put( n, nextNode );
                }

                if(closedSet.contains(nextNode)) // it has already been considered
                    continue;

                // otherwise evaluate the cost of this node/edge combo
                double tentativeCost = x.gx + (Integer) l.info;
                boolean better = false;

                if(! openSet.contains(nextNode)){
                    openSet.add(nextNode);
                    openSetQueue.add(nextNode);
                    nextNode.hx = heuristic(x.node, nextNode.node) + x.hx;
                    better = true;
                }
                else if(tentativeCost < nextNode.gx){
                    better = true;
                }

                // store A* information about this promising candidate node
                if(better){
                    nextNode.cameFrom = x;
                    nextNode.gx = tentativeCost;
                    nextNode.fx = nextNode.gx + nextNode.hx;
                }
            }

//            if(foundNodes.size()%10000 == 0)
//                System.out.println("Time = " + System.currentTimeMillis());
        }
        //System.out.println("Searched " + foundNodes.size() + " nodes but could not find it");
        return null;
    }

    /**
     * Uses Djikstra to find all nodes within the distance that are a part of endNodes.  Returns the list of endNodes within the distance.
     * @param start
     * @param endNodes
     * @param max_distance the maximum distance you want to search in the road network
     * @return A list of nodes within the maximum distance sorted in ascending order by distance to start (index 0 means closest)
     */
    public static List<EbolaBuilder.Node> getNodesWithinDistance(EbolaBuilder.Node start, Map endNodes, double max_distance, double speed)
    {
        //        int[] cacheKey = new int[] {start.location.xLoc, start.location.yLoc, goal.location.xLoc, goal.location.yLoc};
//        if (cache.containsKey(cacheKey))
//            return cache.get(cacheKey);
//
        // initial check
        long startTime = System.currentTimeMillis();
        if (start == null || endNodes == null) {
            System.out.println("Error: invalid node provided to AStar");
        }

        // containers for the metainformation about the Nodes relative to the
        // A* search
        HashMap<EbolaBuilder.Node, AStarNodeWrapper> foundNodes =
                new HashMap<EbolaBuilder.Node, AStarNodeWrapper>();


        AStarNodeWrapper startNode = new AStarNodeWrapper(start);
        //AStarNodeWrapper goalNode = new AStarNodeWrapper(goal);
        foundNodes.put(start, startNode);
        //foundNodes.put(goal, goalNode);

        startNode.gx = 0;
        startNode.hx = 0;
        startNode.fx = 0;

        // A* containers: allRoadNodes to be investigated, allRoadNodes that have been investigated
        //was ArrayList
        HashSet<AStarNodeWrapper> closedSet = new HashSet<>(),
                openSet = new HashSet<>();
        PriorityQueue<AStarNodeWrapper> openSetQueue = new PriorityQueue<>(10000);//added


        openSet.add(startNode);
        openSetQueue.add(startNode);

        List<EbolaBuilder.Node> nodesToReturn = new LinkedList<>();
        ListIterator<EbolaBuilder.Node> listIterator = nodesToReturn.listIterator();//pointer to last position in the nodesToReturn

        while(openSet.size() > 0){ // while there are reachable allRoadNodes to investigate

            //AStarNodeWrapper x = findMin(openSet); // find the shortest path so far
            AStarNodeWrapper x = openSetQueue.peek();
            //check if we have reached maximum route distance
            if(x.hx > max_distance)
            {
                return nodesToReturn;
            }
            if(x == null)
            {
                AStarNodeWrapper n = findMin(openSet);
            }
            if(endNodes.containsKey(x.node)){ // we have found the shortest possible path to the goal!
                listIterator.add(x.node);
            }

            openSet.remove(x); // maintain the lists
            openSetQueue.remove();
            closedSet.add(x);

            // check all the neighbors of this location
            for(Edge l: x.node.links){

                EbolaBuilder.Node n = (EbolaBuilder.Node) l.from();
                if( n == x.node )
                    n = (EbolaBuilder.Node) l.to();

                // get the A* meta information about this Node
                AStarNodeWrapper nextNode;
                if( foundNodes.containsKey(n))
                    nextNode = foundNodes.get(n);
                else{
                    nextNode = new AStarNodeWrapper(n);
                    foundNodes.put( n, nextNode );
                }

                if(closedSet.contains(nextNode)) // it has already been considered
                    continue;

                // otherwise evaluate the cost of this node/edge combo
                double tentativeCost = x.gx + (Integer) l.info;
                boolean better = false;

                if(! openSet.contains(nextNode)){
                    openSet.add(nextNode);
                    openSetQueue.add(nextNode);
                    nextNode.hx = heuristic(x.node, nextNode.node) + x.hx;
                    better = true;
                }
                else if(tentativeCost < nextNode.gx){
                    better = true;
                }

                // store A* information about this promising candidate node
                if(better){
                    nextNode.cameFrom = x;
                    nextNode.gx = tentativeCost;
                    nextNode.fx = nextNode.gx + nextNode.hx;
                }
            }

//            if(foundNodes.size()%10000 == 0)
//                System.out.println("Time = " + System.currentTimeMillis());
        }
        //System.out.println("Searched " + foundNodes.size() + " nodes but could not find it");
        return nodesToReturn;
    }

    /**
     * Uses Djikstra to find the closest in the list of endNodes.  Returns the endNode that is closest.
     * @param start
     * @param distance the target distance to stop
     * @return
     */
    public static Route getNodeAtDistance(EbolaBuilder.Node start, double distance, double speed)
    {
        //        int[] cacheKey = new int[] {start.location.xLoc, start.location.yLoc, goal.location.xLoc, goal.location.yLoc};
//        if (cache.containsKey(cacheKey))
//            return cache.get(cacheKey);
//
        // initial check
        long startTime = System.currentTimeMillis();
        if (start == null) {
            System.out.println("Error: invalid node provided to AStar");
        }

        // containers for the metainformation about the Nodes relative to the
        // A* search
        HashMap<EbolaBuilder.Node, AStarNodeWrapper> foundNodes =
                new HashMap<EbolaBuilder.Node, AStarNodeWrapper>();


        AStarNodeWrapper startNode = new AStarNodeWrapper(start);
        //AStarNodeWrapper goalNode = new AStarNodeWrapper(goal);
        foundNodes.put(start, startNode);
        //foundNodes.put(goal, goalNode);

        startNode.gx = 0;
        startNode.hx = 0;
        startNode.fx = 0;

        // A* containers: allRoadNodes to be investigated, allRoadNodes that have been investigated
        HashSet<AStarNodeWrapper> closedSet = new HashSet<>(),
                openSet = new HashSet<>();
        PriorityQueue<AStarNodeWrapper> openSetQueue = new PriorityQueue<>(10000);

        openSet.add(startNode);
        openSetQueue.add(startNode);

        while(openSet.size() > 0){ // while there are reachable allRoadNodes to investigate

            //AStarNodeWrapper x = findMin(openSet); // find the shortest path so far
            AStarNodeWrapper x = openSetQueue.peek();

            //check if we have reached maximum route distance
            if(x.hx > distance)////we are at the distance!!!
            {
                return reconstructRoute(x, startNode, x, speed);
            }
            if(x == null)
            {
                AStarNodeWrapper n = findMin(openSet);
            }
            openSet.remove(x); // maintain the lists
            openSetQueue.remove();
            closedSet.add(x);

            // check all the neighbors of this location
            for(Edge l: x.node.links){

                EbolaBuilder.Node n = (EbolaBuilder.Node) l.from();
                if( n == x.node )
                    n = (EbolaBuilder.Node) l.to();

                // get the A* meta information about this Node
                AStarNodeWrapper nextNode;
                if( foundNodes.containsKey(n))
                    nextNode = foundNodes.get(n);
                else{
                    nextNode = new AStarNodeWrapper(n);
                    foundNodes.put( n, nextNode );
                }

                if(closedSet.contains(nextNode)) // it has already been considered
                    continue;

                // otherwise evaluate the cost of this node/edge combo
                double tentativeCost = x.gx + (Integer) l.info;
                boolean better = false;

                if(! openSet.contains(nextNode)){
                    openSet.add(nextNode);
                    openSetQueue.add(nextNode);
                    nextNode.hx = heuristic(x.node, nextNode.node) + x.hx;
                    better = true;
                }
                else if(tentativeCost < nextNode.gx){
                    better = true;
                }

                // store A* information about this promising candidate node
                if(better){
                    nextNode.cameFrom = x;
                    nextNode.gx = tentativeCost;
                    nextNode.fx = nextNode.gx + nextNode.hx;
                }
            }

//            if(foundNodes.size()%10000 == 0)
//                System.out.println("Time = " + System.currentTimeMillis());
        }
        //System.out.println("Searched " + foundNodes.size() + " nodes but could not find it");
        return null;
    }

    /**
     * Takes the information about the given node n and returns the path that
     * found it.
     * @param n the end point of the path
     * @return an Route from start to goal
     */
    static Route reconstructRoute(AStarNodeWrapper n, AStarNodeWrapper start, AStarNodeWrapper end, double speed)
    {
        List<Int2D> result = new ArrayList<>(20);

        //adjust speed to temporal resolution
        speed *= Parameters.TEMPORAL_RESOLUTION;//now km per step

        //convert speed to cell block per step
        speed = Parameters.convertFromKilometers(speed);

        double mod_speed = speed;//
        double totalDistance = 0;
        AStarNodeWrapper x = n;

        //start by adding the last one
        result.add(0, x.node.location);

        if(x.cameFrom != null)
        {
            x = x.cameFrom;

            while (x != null)
            {
                double dist = x.node.location.distance(result.get(0));

                while(mod_speed < dist)
                {
                    result.add(0, getPointAlongLine(result.get(0), x.node.location, mod_speed/dist));
                    dist = x.node.location.distance(result.get(0));
                    mod_speed = speed;
                }
                mod_speed -= dist;

                x = x.cameFrom;

                if(x != null && x.cameFrom != null)
                    totalDistance += x.node.location.distance(x.cameFrom.node.location);
            }
        }

        result.add(0, start.node.location);
        return new Route(result, totalDistance, start.node, end.node, Parameters.WALKING_SPEED);
    }

    /**
     * Gets a point a certain percent a long the line
     * @param start
     * @param end
     * @param percent the percent along the line you want to get.  Must be less than 1
     * @return
     */
    public static Int2D getPointAlongLine(Int2D start, Int2D end, double percent)
    {
        return new Int2D((int)Math.round((end.getX()-start.getX())*percent + start.getX()), (int)Math.round((end.getY()-start.getY())*percent + start.getY()));
    }

    /**
     * Measure of the estimated distance between two Nodes.
     * @return notional "distance" between the given allRoadNodes.
     */
    static double heuristic(EbolaBuilder.Node x, EbolaBuilder.Node y) {
        return x.location.distance(y.location);
    }

    /**
     *  Considers the list of Nodes open for consideration and returns the node 
     *  with minimum fx value
     * @param set list of open Nodes
     * @return
     */
    static AStarNodeWrapper findMin(Collection<AStarNodeWrapper> set) {
        double min = Double.MAX_VALUE;
        AStarNodeWrapper minNode = null;
        for (AStarNodeWrapper n : set) {
            if (n.fx < min) {
                min = n.fx;
                minNode = n;
            }
        }
        return minNode;
    }

    /**
     * A wrapper to contain the A* meta information about the Nodes
     *
     */
    static class AStarNodeWrapper implements Comparable<AStarNodeWrapper>{

        // the underlying Node associated with the metainformation
        EbolaBuilder.Node node;
        // the Node from which this Node was most profitably linked
        AStarNodeWrapper cameFrom;
        double gx, hx, fx;

        public AStarNodeWrapper(EbolaBuilder.Node n) {
            node = n;
            gx = 0;
            hx = 0;
            fx = 0;
            cameFrom = null;
        }

        @Override
        public int compareTo(AStarNodeWrapper aStarNodeWrapper) {
            return Double.compare(this.hx, aStarNodeWrapper.hx);
        }
    }
}