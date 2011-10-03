/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.network.stats;
import sim.field.network.*;
import sim.util.*;
import java.util.*;

/**
   Contributor:  Martin Pokropp <mapokropp@googlemail.com> 
*/

public class NetworkStatistics
    {
    /**
       Returns the number of nodes in the network.
    */
    public static int getNumberNodes( final Network network )
        {
        return network.allNodes.numObjs;
        }

    /**
       Returns the maximum number of edges in the network.  Assumes the network is simple,
       in the sense that there can be at most an edge from node i to node j (for any i and j).
       With this assumption, there are N*(N-1) potential edges, not including self-loops.
    */
    public static int getNumberPotentialEdges( final Network network )
        {
        int N = getNumberNodes( network );
        if( network.isDirected() )
            return N * (N-1);
        else
            return N * (N-1) / 2;
        }

    /**
       Returns the number of edges in the network.  Does not pay attention whether the edges
       are self-loops or not.
    */
    public static int getNumberActualEdges( final Network network )
        {
        int actualTies= 0;
        int N = getNumberNodes( network );
        for( int i = 0 ; i < N; i++ )
            {
            Bag temp = network.getEdgesOut( network.allNodes.objs[i] );
            actualTies += temp.numObjs;
            }
        if( network.isDirected() )
            return actualTies;
        else
            return actualTies / 2;
        }

    /**
       Returns the density of a matrix (the ratio of number of actual edges in the network to
       the maximum number of edges that can exist in the network).
    */
    public static double getDensity( final Network network )
        {
        // TODO: should we worry about self loops?
        if( getNumberNodes(network) == 0 )
            return 0;
        return (double)getNumberActualEdges(network)/(double)getNumberPotentialEdges(network);
        }

        
    /**
       Returns a list containing the isolated nodes in the network (nodes with zero in-degree and zero out-degree).
    */
    public static Bag getIsolatedNodes( final Network network )
        {
        Bag result = new Bag();
        int N = getNumberNodes(network);
        Iterator i = network.indexOutInHash.values().iterator();
        for( int k = 0 ; k < N ; k++ )
            {
            Network.IndexOutIn ioi = (Network.IndexOutIn)i.next();
            if(( ioi.in == null || ioi.in.numObjs == 0 ) && ( ioi.out == null || ioi.out.numObjs == 0 )) 
                result.add( network.allNodes.objs[ioi.index]);
            }
        return result;
        }

    /**
       Returns the inclusiveness of the network (the ratio of non-isolated nodes to total number of nodes in the network).
    */
    public static double getInclusiveness( final Network network )
        {       
        int N = getNumberNodes(network);
        int count = 0;
        Iterator i = network.indexOutInHash.values().iterator();
        for( int k = 0 ; k < N ; k++ )//this way I avoid doing N procedure calls.
            {
            Network.IndexOutIn ioi = (Network.IndexOutIn)i.next();
            if(( ioi.in == null || ioi.in.numObjs == 0 ) && ( ioi.out == null || ioi.out.numObjs == 0 )) 
                count++;
            }
        return ((double)(N-count))/N;
        }

    /**
       Returns a vector with distances to the nodes in the graph.  Double.POSITIVE_INFINITY marks nodes
       that are not accessible from the startNode.
       The vector is indexed based on the indexes of the nodes in the allNodes Bag in the Network.
    */
    public static double[] getShortestPaths( final Network network, final Object startNode, final EdgeMetric computer )
        {
        double[] result = new double[network.allNodes.numObjs];
        for( int i = 0 ; i < result.length ; i++ )
            result[i] = Double.POSITIVE_INFINITY;
        DoubleHeap heap = new DoubleHeap();
//              System.out.println("***" + startNode);
        heap.add( startNode, 0.0 );

        // TODO: we need to check this thing again!
        // extract nodes from the heap, until the heap becomes empty.  to make the code easier, we do not
        // modify the information on the heap, but we rather make sure the information is not too old.  that is,
        // once we pop a node from the heap, we verify that it has not already been visited with a lower cost
        // (in which case, we just discard the current information).  this incurs some extra costs (i am not sure
        // exactly how slower this can make things go), but i think the costs are not that significant.  once we
        // check this, the same check/modifications will need to be performed to the getShortestPath and
        // johnsonShortestPathsMatrix methods, where I basically just copied/pasted the code.
        while( !heap.isEmpty() )
            {
            final double dist = heap.getMinKey();
            final Object node = heap.extractMin();
            int nodeIndex = network.getNodeIndex(node);
            if( result[nodeIndex] <= dist ) // if node was already visited and it was closer than it is now, skip it
                continue;
            result[nodeIndex] = dist;
            final Bag edgesOut = network.getEdgesOut(node);
            for( int i = 0 ; i < edgesOut.numObjs ; i++ )
                {
                final Edge edge = (Edge)(edgesOut.objs[i]);
                int toNode = network.getNodeIndex(((Edge)(edgesOut.objs[i])).getOtherNode(node));
//System.out.println("%%%");                            
//System.out.println("" + edge + " " + nodeIndex + " " + ((Edge)(edgesOut.objs[i])).getOtherNode(node) + " " + toNode + " " + network.allNodes.numObjs);
//System.out.println("!!!" + java.lang.Thread.currentThread());
                final double newDist = dist + computer.getWeight(edge);
                if( result[toNode] > newDist )
                    {
//                                      System.out.println("+++" + network.allNodes.objs[toNode]);
                    heap.add( network.allNodes.objs[toNode], newDist );
                    }
                }
            }

        return result;
        }

    /**
       Returns the shortest path (number of edges) between two nodes indicated by their indexes in the allNodes Bag.
       Returns Double.POSITIVE_INFINITY if no path is found.
    */
    public static double getShortestPath( final Network network, final Object startNode, final Object endNode, final EdgeMetric computer )
        {
        if( startNode.equals(endNode) )
            return 0;
        double[] result = new double[network.allNodes.numObjs];
        for( int i = 0 ; i < result.length ; i++ )
            result[i] = Double.POSITIVE_INFINITY;
        DoubleHeap heap = new DoubleHeap();
        heap.add( startNode, 0.0 );

        while( !heap.isEmpty() )
            {
            final double dist = heap.getMinKey();
            final Object node = heap.extractMin();
            int nodeIndex = network.getNodeIndex(node);
            if( result[nodeIndex] <= dist ) // if node was already visited and it was closer than it is now, skip it
                continue;
            result[nodeIndex] = dist;
            if( node.equals(endNode) )
                return dist;
            final Bag edgesOut = network.getEdgesOut(node);
            for( int i = 0 ; i < edgesOut.numObjs ; i++ )
                {
                final Edge edge = (Edge)(edgesOut.objs[i]);
                int toNode = network.getNodeIndex(((Edge)(edgesOut.objs[i])).getOtherNode(node));
                final double newDist = dist + computer.getWeight(edge);
                if( result[toNode] > newDist )
                    heap.add( network.allNodes.objs[toNode], newDist );
                }
            }

        return Double.POSITIVE_INFINITY;
        }

    /**
       Computes the clustering coefficient, i.e. the average ratio of neighbors of a node that are connected by direct edges.
    */
    public static double getClusteringCoefficient( final Network network )
        {
        double clusteringCoefficient = 0;
        int N = getNumberNodes(network);
        boolean[] allFalse = new boolean[N];
        HashSet neighbors = new HashSet();
        for( int i = 0 ; i < N ; i++ )
            {
            // collect all the neighbors of a node into a single hash set
            neighbors.clear();
            final Bag edgesOut = network.getEdgesOut(network.allNodes.objs[i]);
            for( int j = 0 ; j < edgesOut.numObjs ; j++ )
                {
                Object toNode = ((Edge)(edgesOut.objs[j])).getOtherNode(network.allNodes.objs[i]);
                if( !toNode.equals(network.allNodes.objs[i]) )
                    neighbors.add(toNode);
                }

            // go through the neighbors, and compute how many pairs of them have direct edges connecting them
            int pairs = 0;
            Iterator iter = neighbors.iterator();
            while( iter.hasNext() )
                {
                Object neigh = iter.next();
                final Bag edgesOutOfNeigh = network.getEdgesOut(neigh);
                for( int j = 0 ; j < edgesOutOfNeigh.numObjs ; j++ )
                    {
                    Object toNode = ((Edge)(edgesOutOfNeigh.objs[j])).getOtherNode(neigh);
                    if( (!toNode.equals(network.allNodes.objs[i])) && (!toNode.equals(neigh)) && neighbors.contains(toNode) )
                        pairs++;
                    }
                }
            if( edgesOut.numObjs >= 2 )
                //Gabriel: "An object must have at least two neighbors to calculate the clustering coefficient"
                clusteringCoefficient += (double)pairs/(double)(edgesOut.numObjs*(edgesOut.numObjs-1));
            }
        return clusteringCoefficient/N;
        }

    /**
       Checks whether the network is a multigraph or not (a multigraph may contain several edges from a node i to a node j).
    */
    public static boolean isMultigraphNetwork( final Network network )
        {
        HashSet hashSet = new HashSet();
        for( int i = 0 ; i < network.allNodes.numObjs ; i++ )
            {
            hashSet.clear();
            Bag edgesOut = network.getEdgesOut(network.allNodes.objs[i]);
            for( int j = 0 ; j < edgesOut.numObjs ; j++ )
                {
                final Object toNode = ((Edge)(edgesOut.objs[j])).getOtherNode(network.allNodes.objs[i]);
                if( hashSet.contains(toNode) )
                    return true;
                hashSet.add(toNode);
                }
            }
        return false;
        }

    /**
       Checks whether the network has self loops or not (a self-loop is a link from a node to itself).
    */
    public static boolean getHasSelfLoops( final Network network )
        {
        for( int i = 0 ; i < network.allNodes.numObjs ; i++ )
            {
            final Bag edgesOut = network.getEdgesOut(network.allNodes.objs[i]);
            for( int j = 0 ; j < edgesOut.numObjs ; j++ )
                {
                final Object toNode = ((Edge)(edgesOut.objs[j])).getOtherNode(network.allNodes.objs[i]);
                if( network.allNodes.objs[i].equals(((Edge)(edgesOut.objs[j])).getOtherNode(network.allNodes.objs[i])) )
                    return true;
                }
            }
        return false;
        }

    /**
       Computes the amount of symmetry in the network, ie the ratio of edges i->j and j->i over all edges.
       If there are three nodes (1,2, and 3), and three edges (1->2, 2->1 and 1->3), the symmetry coefficient is 2/3.
    */
    public static double getSymmetryCoefficient( final Network network )
        {
        if( !network.isDirected() )
            return 1.0; // if the graph is undirected, it is *very* symmetric
        int totalNumberEdges = 0;
        int symmetricEdges = 0;
        HashSet hashSet = new HashSet();

        for( int i = 0 ; i < network.allNodes.numObjs ; i++ )
            {
            hashSet.clear();
            final Bag edgesOut = network.getEdgesOut(network.allNodes.objs[i]);
            for( int j = 0 ; j < edgesOut.numObjs ; j++ )
                hashSet.add( ((Edge)(edgesOut.objs[j])).to() );

            final Bag edgesIn = network.getEdgesIn(network.allNodes.objs[i]);
            for( int j = 0 ; j < edgesIn.numObjs ; j++ )
                if( hashSet.contains( ((Edge)(edgesIn.objs[j])).to() ) )
                    symmetricEdges++;

            totalNumberEdges += edgesIn.numObjs;
            }

        if( totalNumberEdges > 0 )
            return (double)symmetricEdges / (double)totalNumberEdges;
        else
            return 1.0;
        }

    /**
       Get the shortest paths matrix (from any node to any other node)
    */
    public static double[][] getShortestPathsMatrix( final Network network, final EdgeMetric computer )
        {
        final int numNodes = getNumberNodes(network);
        final int numEdges = getNumberActualEdges(network);

        if( numEdges > numNodes*Math.sqrt(numNodes) ) // a quick-and-dirty way of deciding whether the matrix is sparse or not
            return floydWarshallShortestPathsMatrix(network,computer); // if too many edges, use the O(V^3) algorithm
        else
            return johnsonShortestPathsMatrix(network,computer); // use the O(V^2*log(V)+VE) algorithm
        }

    /**
       Get the shortest paths matrix (from any node to any other node) using the Floyd-Warshall algorithm.
       The time complexity is O(V^3), where V is the number of nodes.
    */
    public static double[][] floydWarshallShortestPathsMatrix( final Network network, final EdgeMetric computer )
        {
        final int N = getNumberNodes(network);
        double[][] result = new double[N][N];

        // initialize the distance matrix to all infinity
        for( int i = 0 ; i < N ; i++ )
            for( int j = 0 ; j < N ; j++ )
                result[i][j] = Double.POSITIVE_INFINITY;

        // initialize the distance matrix with the weights of the existing edges
        for( int i = 0 ; i < N ; i++ )
            {
            Bag bag = network.getEdgesOut(network.allNodes.objs[i]);
            for( int j = 0 ; j < bag.numObjs ; j++ )
                {
                Edge edge = (Edge)(bag.objs[j]);
                result[i][network.getNodeIndex(edge.getOtherNode(network.allNodes.objs[i]))] = computer.getWeight(edge);
                }
            }

        // initialize the distance matrix with 0s on the main diagonal
        for( int i = 0 ; i < N ; i++ )
            result[i][i] = 0;

        // perform the Floyd-Warshall algorithm (three-for-loops)
        for( int k = 0 ; k < N ; k++ )
            for( int i = 0 ; i < N ; i++ )
                for( int j = 0 ; j< N ; j++ )
                    if( result[i][j] > result[i][k] + result[k][j] )
                        result[i][j] = result[i][k] + result[k][j];

        // return the computed shortest path matrix
        return result;
        }

    /**
       Get the shortest paths matrix (from any node to any other node) using the Johnson algorithm for sparse graphs.
       The time complexity is O(V^2*log(V)+VE), where V is the number of nodes.  In fact, as we know the edges have
       only positive costs, we basically call Dijkstra's algorithm from each in the graph (Dijkstra's algorithm computes
       single-node-shortest-paths to all other nodes in the graph).
    */
    public static double[][] johnsonShortestPathsMatrix( final Network network, final EdgeMetric computer )
        {
        final int N = getNumberNodes(network);
        double[][] result = new double[N][N];
        DoubleHeap heap = new DoubleHeap();
        for( int i = 0 ; i < N ; i++ )
            {
            for( int j = 0 ; j < result.length ; j++ )
                result[i][j] = Double.POSITIVE_INFINITY;
            heap.clear();
            heap.add( network.allNodes.objs[i], 0.0 );

            while( !heap.isEmpty() )
                {
                final double dist = heap.getMinKey();
                final Object node = heap.extractMin();
                int nodeIndex = network.getNodeIndex(node);
                if( result[i][nodeIndex] > -1 && result[i][nodeIndex] <= dist ) // if node was already visited and it was closer than it is now, skip it
                    continue;
                result[i][nodeIndex] = dist;
                final Bag edgesOut = network.getEdgesOut(node);
                for( int j = 0 ; j < edgesOut.numObjs ; j++ )
                    {
                    final Edge edge = (Edge)(edgesOut.objs[j]);
                    int toNode = network.getNodeIndex(((Edge)(edgesOut.objs[j])).getOtherNode(node));
                    final double newDist = dist + computer.getWeight(edge);
                    if( result[i][toNode] > newDist )
                        heap.add( network.allNodes.objs[toNode], newDist );
                    }
                }

            result[i][i] = 0;
            }
        return result;
        }

    /**
       Returns the average length of the shortest path between nodes in the network.  Ignores self-loops.
    */
    public static double getMeanShortestPath( final Network network, final EdgeMetric computer )
        {
        try
            {
            double result = 0;
            double[][] paths = getShortestPathsMatrix(network, computer);
            int N = paths.length;
            for( int i = 0 ; i < N ; i++ )  
                for( int j = 0 ; j < N ; j++ )
                    if( i != j )
                        result += paths[i][j];
            return result / (double)(N*(N-1));
            }
        catch (OutOfMemoryError e)
            {
            throw new RuntimeException("You ran out of memory!  getMeanShortestPath(...) has large memory requirements and may " +
                "not be appropriate for big networks.  You should try getLargeNetworkMeanShortestPath(...) instead.", e);
            }
        }


    /**
       Returns the average length of the shortest path between nodes in the network: memory-constrained version. Ignores self-loops. 
       This method can be used if your machine does not provide enough heap memory to store the shortest 
       paths matrix required in NetworkStatistics.getMeanShortestPath above (which is much faster if enough memory 
       is provided). The problem is likely to occur with very large networks (some > 20k nodes??).
           
       @author  Martin Pokropp <mapokropp@googlemail.com>
    */
    public static double getLargeNetworkMeanShortestPath( final Network network, final EdgeMetric computer )
        {
        double result = 0;
        Bag nodes = network.getAllNodes();
        int N = nodes.numObjs;
        if(!network.isDirected())
            {
            for( int i = 0; i < N - 1; i++ )
                {
                Object node = (Object) nodes.get(i);
                double[] paths = getShortestPaths( network, node, computer );
                for( int j = i + 1 ; j < N ; j++ )
                    result += paths[j];
                }
            return result / ( N * (N - 1) / 2d );
            }
        else
            {
            for( int i = 0; i < N; i++ )
                {
                Object node = (Object) nodes.get(i);
                double[] paths = getShortestPaths( network, node, computer );
                for( int j = 0 ; j < N ; j++ )
                    if( i != j )
                        result += paths[j];
                }
            return result / (double) ( N * (N - 1) );
            }
        }




    /**
       Returns the eccentricity of a node.  The eccentricity is defined as the longest shortest distance to another node.
       If there's a node that's not accessible from the start node, the eccentricity is Double.POSITIVE_INFINITY;
    */
    public static double getNodeEccentricity( final Network network, final Object node, final EdgeMetric computer )
        {
        final int N = getNumberNodes(network);
        if( N == 0 )
            return 0;
        double[] dist = getShortestPaths( network, node, computer );
        double max = dist[0];
        for( int i = 1 ; i < dist.length ; i++ )
            if( max < dist[i] )
                max = dist[i];
        return max;
        }

    /**
       Compute the radius of a network (the minimum node eccentricity).
    */
    public static double getRadius( final Network network, final EdgeMetric computer )
        {
        final int N = getNumberNodes(network);
        if( N == 0 || N == 1 )
            return 0;
        double min = Double.POSITIVE_INFINITY;
        for( int nn = 0 ; nn < N ; nn++ )
            {
            double dist = getNodeEccentricity( network, network.allNodes.objs[nn], computer );
            if( min > dist )
                min = dist;
            }
        return min;
        }

    /**
       Computes the diameter of a network (the maximum node eccentricity).
    */
    public static double getDiameter( final Network network, final EdgeMetric computer )
        {
        final int N = getNumberNodes(network);
        if( N == 0 || N == 1 )
            return 0;
        double max = -1;
        for( int nn = 0 ; nn < N ; nn++ )
            {
            double dist = getNodeEccentricity( network, network.allNodes.objs[nn], computer );
            if( max < dist )
                max = dist;
            }
        return max;
        }
        
    /**
       Get the shortest paths matrix (from any node to any other node) using the Johnson algorithm for sparse graphs.
       The time complexity is O(V^2*log(V)+VE), where V is the number of nodes.  In fact, as we know the edges have
       only positive costs, we basically call Dijkstra's algorithm from each in the graph (Dijkstra's algorithm computes
       single-node-shortest-paths to all other nodes in the graph).
    */
    public static long[][] johnsonNumberShortestPathsMatrix( final Network network, final EdgeMetric computer, final double precision )
        {
        final int N = getNumberNodes(network);
        double[][] result = new double[N][N];
        long[][] number = new long[N][N];
        DoubleHeap heap = new DoubleHeap();
        for( int i = 0 ; i < N ; i++ )
            {
            for( int j = 0 ; j < N ; j++ )
                {
                result[i][j] = Double.POSITIVE_INFINITY;
                number[i][j] = 0;
                }
            heap.clear();
            heap.add( new Pair(network.allNodes.objs[i],-1), 0d );

            while( !heap.isEmpty() )
                {
                boolean shouldExpand = true;
                final double dist = heap.getMinKey();
                final Pair node = (Pair)(heap.extractMin());
                int nodeIndex = network.getNodeIndex(node.object);
                if( result[i][nodeIndex] < dist - precision ) // if node was already visited and it was closer than it is now, skip it
                    continue;
                if( result[i][nodeIndex] > dist + precision )
                    {
                    result[i][nodeIndex] = dist;
                    if( node.index < 0 )
                        number[i][nodeIndex] = 1;
                    else
                        number[i][nodeIndex] = number[i][node.index];
                    }
                else
                    {
                    if( node.index < 0 )
                        number[i][nodeIndex] += 1;
                    else
                        number[i][nodeIndex] += number[i][node.index];
                    shouldExpand = false;
                    }
                if( !shouldExpand )
                    continue;
                final Bag edgesOut = network.getEdgesOut(node.object);
                for( int j = 0 ; j < edgesOut.numObjs ; j++ )
                    {
                    final Edge edge = (Edge)(edgesOut.objs[j]);
                    int toNode = network.getNodeIndex(((Edge)(edgesOut.objs[j])).getOtherNode(node.object));
                    final double newDist = dist + computer.getWeight(edge);
                    if(result[i][toNode] >= newDist + precision )
                        {
                        heap.add( new Pair(network.allNodes.objs[toNode],nodeIndex), newDist );
                        }
                    }
                }

            result[i][i] = 0;
            }
        return number;
        }
        
    static class Pair
        {
        Object object;
        int index;
        public Pair( Object o, int i ) { object=o; index=i; }
        }

    /**
       Get the numer of shortest paths (from any node to any other node) using the Floyd-Warshall algorithm.
       Precision is used to deal with equality of real-valued paths (two real-valued numbers are different with their
       absolute difference is greater than 'precision').
       The time complexity is O(V^3), where V is the number of nodes.
    */
    public static long[][] floydWarshallNumberShortestPathsMatrix( final Network network, final EdgeMetric computer, final double precision )
        {
        final int N = getNumberNodes(network);
        double[][] result = new double[N][N];
        long[][] number = new long[N][N];

        // initialize the distance matrix to all infinity
        for( int i = 0 ; i < N ; i++ )
            for( int j = 0 ; j < N ; j++ )
                {
                result[i][j] = Double.POSITIVE_INFINITY;
                number[i][j] = 0;
                }

        // initialize the distance matrix with the weights of the existing edges
        for( int i = 0 ; i < N ; i++ )
            {
            Bag bag = network.getEdgesOut(network.allNodes.objs[i]);
            for( int j = 0 ; j < bag.numObjs ; j++ )
                {
                Edge edge = (Edge)(bag.objs[j]);
                final int k = network.getNodeIndex(edge.getOtherNode(network.allNodes.objs[i]));
                result[i][k] = computer.getWeight(edge);
                number[i][k]++;
                }
            }

        // initialize the distance matrix with 0s on the main diagonal
        for( int i = 0 ; i < N ; i++ )
            result[i][i] = 0;

        // perform the Floyd-Warshall algorithm (three-for-loops)
        for( int k = 0 ; k < N ; k++ )
            for( int i = 0 ; i < N ; i++ )
                for( int j = 0 ; j< N ; j++ )
                    if( result[i][j] > ( result[i][k] + result[k][j] + precision ) )
                        {
                        result[i][j] = result[i][k] + result[k][j];
                        number[i][j] = number[i][k] * number[k][j];
                        }
                    else if( abs( result[i][j] - (result[i][k] + result[k][j]) ) <= precision )
                        {
                        number[i][j] += number[i][k] * number[k][j];
                        }

        // return the computed shortest path matrix
        return number;
        }

    /**
       Get the numer of shortest paths (from any node through a second node to a third node) using the Floyd-Warshall algorithm.
       Precision is used to deal with equality of real-valued paths (two real-valued numbers are different with their
       absolute difference is greater than 'precision').
       The time complexity is O(V^3), where V is the number of nodes.
    */
    public static long[][][] floydWarshallNumberShortestPathsWithIntermediatesMatrix( final Network network, final EdgeMetric computer, final double precision )
        {
        final int N = getNumberNodes(network);
        double[][] result = new double[N][N];
        long[][] number = new long[N][N];

        // initialize the distance matrix to all infinity
        for( int i = 0 ; i < N ; i++ )
            for( int j = 0 ; j < N ; j++ )
                {
                result[i][j] = Double.POSITIVE_INFINITY;
                number[i][j] = 0;
                }

        // initialize the distance matrix with the weights of the existing edges
        for( int i = 0 ; i < N ; i++ )
            {
            Bag bag = network.getEdgesOut(network.allNodes.objs[i]);
            for( int j = 0 ; j < bag.numObjs ; j++ )
                {
                Edge edge = (Edge)(bag.objs[j]);
                final int k = network.getNodeIndex(edge.getOtherNode(network.allNodes.objs[i]));
                result[i][k] = computer.getWeight(edge);
                number[i][k]++;
                }
            }

        // initialize the distance matrix with 0s on the main diagonal
        for( int i = 0 ; i < N ; i++ )
            result[i][i] = 0;

        // perform the Floyd-Warshall algorithm (three-for-loops)
        for( int k = 0 ; k < N ; k++ )
            for( int i = 0 ; i < N ; i++ )
                for( int j = 0 ; j< N ; j++ )
                    if( result[i][j] > ( result[i][k] + result[k][j] + precision ) )
                        {
                        result[i][j] = result[i][k] + result[k][j];
                        number[i][j] = number[i][k] * number[k][j];
                        }
                    else if( abs( result[i][j] - (result[i][k] + result[k][j]) ) <= precision )
                        {
                        number[i][j] += number[i][k] * number[k][j];
                        }

        // now we compute the answer: the shortest path from i to j going through k is composed of a shortest path
        // from i to k, and a shortest path from k to j.  Therefore, we can simply compare the lengths of the shortest
        // paths between pairs of nodes, and decide whether the desired number is 0, or how it can be computed.
        long[][][] theResult = new long[N][N][N];

        for( int i = 0 ; i < N ; i++ )
            for( int j = 0 ; j < N ; j++ )
                for( int k = 0 ; k < N ; k++ )
                    if( abs(result[i][j]+result[j][k]-result[i][k]) <= precision )
                        theResult[i][j][k] = number[i][j]*number[j][k];
                    else
                        theResult[i][j][k] = 0;

        // return the computed shortest path matrix
        return theResult;
        }

    /**
       Get the matrix with number of shortest paths (from any node to any other node)
    */
    public static long[][] getNumberShortestPathsMatrix( final Network network, final EdgeMetric computer, final double precision )
        {
        final int numNodes = getNumberNodes(network);
        final int numEdges = getNumberActualEdges(network);

        if( numEdges > numNodes*Math.sqrt(numNodes) ) // a quick-and-dirty way of deciding whether the matrix is sparse or not
            return floydWarshallNumberShortestPathsMatrix(network,computer,precision); // if too many edges, use the O(V^3) algorithm
        else
            return johnsonNumberShortestPathsMatrix(network,computer,precision); // use the O(V^2*log(V)+VE) algorithm
        }

    /**
       Get the numer of shortest paths (from any node through a second node to a third node).
       Precision is used to deal with equality of real-valued paths (two real-valued numbers are different with their
       absolute difference is greater than 'precision').
    */
    public static long[][][] getNumberShortestPathsWithIntermediatesMatrix( final Network network, final EdgeMetric computer, final double precision )
        {
        // As the output is a vector with V^3 elements, we need O(V^3) to update the vector anyway.
        // Therefore, we may simply use the Floyd-Warshall algorithm directly, without worrying about Johnson's algorithm for sparse graphs.
        return floydWarshallNumberShortestPathsWithIntermediatesMatrix(network,computer,precision);
        }

    final static double abs( double x ) { return (x>0)?x:(-x); }

    }
