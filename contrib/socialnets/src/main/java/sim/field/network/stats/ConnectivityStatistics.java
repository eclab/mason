/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.network.stats;
import sim.field.network.*;

import sim.util.*;

public class ConnectivityStatistics
    {

    /**
       Checks if a directed graph is strongly connected.
    */
    public static boolean isStronglyConnected( final Network network )
        {
        if( network.isDirected() )
            {
            final Bag bag = getStronglyConnectedComponents(network);
            return( bag.numObjs == 1 );
            }
        else
            return false; // return a false if the network is undirected
        }

    /**
       Computes the strongly connected components of an ORIENTED graph.
       @return Bag containing the connected components (each element in the bag is another bag of nodes).
    */
    public static Bag getStronglyConnectedComponents( final Network network )
        {
        if( !network.isDirected() )
            throw new RuntimeException( "Connect.getStronglyConnectedComponents should be called only with directed graphs" );
        Bag result = new Bag();
        final int N = NetworkStatistics.getNumberNodes(network);
        double[] finishingTime = new double[N];  // double vector to allow the use of heap later on 
        int[] color = new int[N]; // 0=WHITE, 1=GRAY, 2=BLACK
        int time = 0;

        for( int i = 0 ; i < N ; i++ )
            color[i] = 0;

        for( int i = 0 ; i < N ; i++ )
            if( color[i] == 0 )
                {
                IntBag myStack = new IntBag();
                myStack.push(i);
                while( !myStack.isEmpty() )
                    {
                    int j = myStack.pop();
                    if( color[j] == 0 ) // if it's a white node
                        {
                        color[j] = 1;
                        myStack.push(j);
                        time++;
                        final Bag edgesOut = network.getEdgesOut(network.allNodes.objs[j]);
                        for( int k = 0 ; k < edgesOut.numObjs ; k++ )
                            {
                            final Edge edge = (Edge)(edgesOut.objs[k]);
                            final int toNode = network.getNodeIndex(edge.to());
                            if( color[toNode] == 0 )
                                myStack.push(toNode);
                            }
                        }
                    else
                        {
                        color[j] = 2;
                        time++;
                        finishingTime[j] = -time; //negative value, such that heap is in descending order
                        }
                    }
                }

        Integer[] indexes = new Integer[N];
        for( int i = 0 ; i < N ; i++ )
            indexes[i] = new Integer(i);

        DoubleHeap heap = new DoubleHeap( finishingTime, indexes, N );

        for( int i = 0 ; i < N ; i++ )
            color[i] = 0;

        while( !heap.isEmpty() )
            {
            final int i = ((Integer)(heap.extractMin())).intValue();
            if( color[i] == 0 )
                {
                Bag component = new Bag(); // the objects in the current strongly connected component
                IntBag myStack = new IntBag();
                myStack.push(i);
                while( !myStack.isEmpty() )
                    {
                    final int j = myStack.pop();
                    if( color[j] == 0 ) // if it's a white node
                        {
                        color[j] = 1;
                        component.add( network.allNodes.objs[j] );
                        final Bag edgesOut = network.getEdgesIn(network.allNodes.objs[j]); // here we use the in-edges, because the graph should have been transposed
                        for( int k = 0 ; k < edgesOut.numObjs ; k++ )
                            {
                            final Edge edge = (Edge)(edgesOut.objs[k]);
                            final int toNode = network.getNodeIndex(edge.from());  // this is now edge.from() because the graph should have been transposed
                            if( color[toNode] == 0 )
                                myStack.push(toNode);
                            }
                        }
                    }
                        
                result.add( component );
                }
            }

        return result;
        }
                
    /** 
     * Computes the connected components of an undirected OR
     * the weakly connected components of an directed graph
     * graph using DFS.
     * @return A Bag of Bags of nodes. 
     */
    public static Bag getConnectedComponents( final Network network)
        {
        return new ConnectedComponentFactory(network).getComponents(); 
        } 
        
    /** 
     * Determines whether the graph is connected (for undirected graphs) OR
     * weakly connected (for directed graphs) 
     */
    public static boolean isConnected( final Network network)
        {
        return new ConnectedComponentFactory(network).isConnected(); 
        } 

    static class ConnectedComponentFactory
        {
        final Network network;
        final int n;
        final Bag components;
                
        //instead of making a new hashtable, stick all nodes in it
        //and remove them as they get visited,
        //I rely on the hashtable inside the Network and 
        //the nodes' indices in the allNodes bag. 
        final boolean[] visited;
        int countVisited;
                
        public ConnectedComponentFactory( final Network network)
            {
            this.network = network;
            n = network.allNodes.numObjs;
            visited = new boolean[n];
            for(int i=0;i<n;i++)
                visited[i]=false;
            countVisited = 0;
            components = new Bag(n);
            }

        public Bag getComponents()
            {
            boolean directed = network.isDirected();
            for( int i = 0 ; i < n; i++ )
                {
                if(!visited[i])
                    {
                    Bag component = new Bag();  
                    components.add(component);
                    if(directed)
                        exploreWD(network.allNodes.objs[i], i, component);
                    else
                        exploreU(network.allNodes.objs[i], i, component);
                    }
                }
            return components;
            }

        public boolean isConnected()
            {
            if(network.isDirected())
                exploreWD(network.allNodes.objs[0], 0, null);
            else
                exploreU(network.allNodes.objs[0], 0, null);
            return (countVisited ==n);
            }
        /**
         * explore for Weakly connected components in a Directed graph
         * The Bag 'component' can be null if I'm only interested in whether the graph is connected.
         */
        private void exploreWD(Object nodeObj, int nodeIndex, Bag component)
            {
            visited[nodeIndex]=true; countVisited++;
            if(component!=null)
                component.add(nodeObj);
            Bag temp;
            temp = network.getEdgesOut( nodeObj );
            for(int i=0;i<temp.numObjs;i++)
                {
                Object node2 = ((Edge)temp.objs[i]).getOtherNode(nodeObj);
                int node2Index = network.getNodeIndex(node2); 
                if(!visited[node2Index])
                    exploreWD(node2, node2Index, component);
                }
            temp = network.getEdgesIn ( nodeObj );
            for(int i=0;i<temp.numObjs;i++)
                {
                Object node2 = ((Edge)temp.objs[i]).getOtherNode(nodeObj);
                int node2Index = network.getNodeIndex(node2); 
                if(!visited[node2Index])
                    exploreWD(node2, node2Index, component);
                }       
            }

        // The Bag 'component' can be null if I'm only interested in whether the graph is connected.
        private void exploreU(Object nodeObj, int nodeIndex, Bag component)
            {
            visited[nodeIndex]=true;countVisited++;
            if(component!=null) component.add(nodeObj);
            Bag temp = network.getEdgesOut( nodeObj );
            for(int i=0;i<temp.numObjs;i++)
                {
                Object node2 = ((Edge)temp.objs[i]).getOtherNode(nodeObj);
                int node2Index = network.getNodeIndex(node2); 
                if(!visited[node2Index])
                    exploreU(node2, node2Index, component);
                }
            }
                
        }

    static class FlowData
        {
        public FlowData(int flow, int capacity) { this.flow=flow; this.capacity=capacity; }
        public FlowData(FlowData md) { this.flow=md.flow; this.capacity=md.capacity; }
        public int flow;
        public int capacity;
        }

    /*
      Creates the flow network from an original network.
    */
    static Network createFlowNetwork( final Network network )
        {
        // Need to create a new graph with the same nodes, and edges with capacity 1.
        // If the network is undirected, it has to be transformed into a directed network.
        Network flow = new Network(true);
        flow.allNodes = new Bag( network.allNodes );
        final int N = NetworkStatistics.getNumberNodes(network);
        if( network.isDirected() )
            {
            for( int i = 0 ; i < N ; i++ )
                {
                final Object node = network.allNodes.objs[i];
                final Bag edgesIn = network.getEdgesIn(node);
                for( int j = 0 ; j < edgesIn.numObjs ; j++ )
                    flow.addEdge( ((Edge)(edgesIn.objs[j])).from(), node, new FlowData(0,1) );
                }
            }
        else // undirected
            {
            for( int i = 0 ; i < N ; i++ )
                {
                final Object node = network.allNodes.objs[i];
                Bag edges = network.getEdgesIn(node);
                for( int j = 0 ; j < edges.numObjs ; j++ )
                    {
                    flow.addEdge( ((Edge)(edges.objs[j])).getOtherNode(node), node, new FlowData(0,1) );
                    }
                }
            }
        return flow;
        }

    /*
      Computes the maximum flow in the network, starting from startNode and ending in endNode.
      Assumes the network is directed!
    */
    static int maxFlow( final Network network, final Object startNode, final Object endNode )
        {
        final int N = NetworkStatistics.getNumberNodes(network);

        final int startIndex = network.getNodeIndex(startNode);
        final int endIndex = network.getNodeIndex(endNode);

        // reset all flows to 0
        for( int i = 0 ; i < N ; i++ )
            {
            final Object node = network.allNodes.objs[i];
            final Bag edgesOut = network.getEdgesOut( node );
            for( int j = 0 ; j < edgesOut.numObjs ; j++ )
                {
                final Edge edge = (Edge)(edgesOut.objs[j]);
                ((FlowData)(edge.info)).flow = 0;
                }
            }

        // the indexes of parent nodes for paths in residual network
        final Edge[] parent = new Edge[N];

        // the stack
        IntBag stack = new IntBag();

        boolean foundPath = true;

        // the main ford-fulkerson algorithm
        while( foundPath )
            {
            // mark foundPath to false such that if no path is found, the algorithm exits the while loop
            foundPath = false;

            // initialize the data structure
            for( int i = 0 ; i < N ; i++ )
                parent[i] = null;

            // initialize the stack
            stack.clear();
            stack.push( startIndex );

            // the ``recursive'' loop
            while( !stack.isEmpty() )
                {
                // pop a node from the stack, and look at the out-edges
                final int nodeIndex = stack.pop();
                final Object node = network.allNodes.objs[nodeIndex];
                // if we popped the endNode (with index endIndex), we can update the flows in the network
                if( nodeIndex == endIndex )
                    {
                    // compute the maximum amount that the flow can be increased along the path
                    int maxAmount = -1;
                    Edge edge = parent[endIndex];
                    while( edge != null )
                        {
                        final FlowData temp = (FlowData)(edge.info);
                        if( temp.capacity - temp.flow > maxAmount )
                            maxAmount = temp.capacity - temp.flow;
                        edge = parent[network.getNodeIndex(edge.from())];
                        }

                    // now increment the flow along the path by maxAmount
                    edge = parent[endIndex];
                    while( edge != null )
                        {
                        final FlowData temp = (FlowData)(edge.info);
                        temp.flow += maxAmount;
                        edge = parent[network.getNodeIndex(edge.from())];
                        }


                    // mark that we found a path, and break the loop
                    foundPath = true;
                    break;
                    }
                final Bag edgesOut = network.getEdgesOut(node);
                for( int i = 0 ; i < edgesOut.numObjs ; i++ )
                    {
                    final Edge edge = (Edge)(edgesOut.objs[i]);
                    FlowData flowData = (FlowData)(edge.info);
                    // if the edge has same flow as capacity, the edge is useless
                    if( flowData.flow >= flowData.capacity )
                        continue;
                    final Object toNode = edge.to();
                    final int toIndex = network.getNodeIndex(toNode);
                    // if the node has already been visited, the edge is also useless
                    if( parent[toIndex] != null || toIndex == startIndex )
                        continue;
                    // otherwise, we can process the edge
                    parent[toIndex] = edge;
                    stack.push( toIndex );
                    }
                }
            }

        // compute and return the total flow in the network
        int totalFlow = 0;
        final Bag bag = network.getEdgesOut( startNode );
        for( int i = 0 ; i < bag.numObjs ; i++ )
            {
            final Edge e = (Edge)(bag.objs[i]);
            final FlowData data = (FlowData)(e.info);
            totalFlow += data.flow;
            }
        return totalFlow;

        }

    /**
       Computes the edge connectivity of a network
       (i.e. the minimum number of edges that can be removed to make the graph disconnected).
    */
    public static int getEdgeConnectivity( final Network network )
        {
        final int N = NetworkStatistics.getNumberNodes(network);
        if( N == 0 )
            throw new RuntimeException( "The graph has no nodes at all." );
        if( N == 1 ) // if there's a single node
            return NetworkStatistics.getNumberActualEdges(network);
        if( !isConnected(network) )
            return 0;
        if( network.isDirected() )
            return getDigraphEdgeConnectivity( network );
        else
            return getGraphEdgeConnectivity( network );
        }

    /**
       Computes the edge connectivity of a digraph (directed graph)
       (i.e. the minimum number of edges that can be removed to make the graph disconnected).
       Assumes the digraph is weakly connected and non-trivial.
    */
    public static int getDigraphEdgeConnectivity( final Network network )
        {
        final int N = NetworkStatistics.getNumberNodes(network);

        // create the flow network for the computations
        final Network flowNet = createFlowNetwork( network );

        // initialize a min
        int min = Integer.MAX_VALUE;

        for( int i = 0 ; i < N ; i++ )
            {
            // compute the flow through the flow network
            final int flow = maxFlow( flowNet, network.allNodes.objs[i], network.allNodes.objs[(i+1)%N] );
            if( min > flow )
                min = flow;
            }

        return min;

        }

    /**
       Computes the edge connectivity of an undirected graph
       (i.e. the minimum number of edges that can be removed to make the graph disconnected).
    */
    public static int getGraphEdgeConnectivity( final Network network )
        {
        final Bag D = getDominatingSet(network);
        int min = Integer.MAX_VALUE;
        final Network flowNet = createFlowNetwork(network);
        for( int i = 1 ; i < D.numObjs ; i++ )
            {
            final int flow = maxFlow( flowNet, D.objs[0], D.objs[i] );
            if( min > flow )
                min = flow;
            }

        // compute the minimum degree of the graph.
        // (due to representation, it is the same as the minimum in degree, or the minimum out degree)
        final int minDegree = DegreeStatistics.getMinInDegree(network);
        if( min > minDegree )
            min = minDegree;

        return min;
                
        }

    /*
      Computes a dominating set for the network
      (i.e. a set of nodes such that any node in the graph is adjacent to at least one node in the set).
    */
    static Bag getDominatingSet( final Network network )
        {
        final int N = NetworkStatistics.getNumberNodes(network);
        boolean[] adjacent = new boolean[N];
        Bag result = new Bag();
        for( int i = 0 ; i < N ; i++ )
            adjacent[i] = false;
        int numAdjacent = 0;
        for( int i = 0 ; i < N ; i++ )
            {
            if( adjacent[i] )
                continue;
            adjacent[i] = true;
            numAdjacent++;
            final Object node = network.allNodes.objs[i];
            result.add(node);
            Bag edges = network.getEdgesIn(node);
            for( int j = 0 ; j < edges.numObjs ; j++ )
                {
                final Edge edge = (Edge)(edges.objs[j]);
                final int index = network.getNodeIndex(edge.getOtherNode(node));
                if( !adjacent[index] )
                    {
                    adjacent[index] = true;
                    numAdjacent++;
                    }
                }
            if( numAdjacent == N )
                break;
            }
        return result;
        }

    /**
       Computes the node connectivity of a network
       (i.e. the minimum number of nodes that can be removed to make the graph disconnected).
    */
    public static int getNodeConnectivity( final Network network )
        {

        final int N = NetworkStatistics.getNumberNodes(network);
        if( N == 0 )
            throw new RuntimeException( "The graph has no nodes at all." );
        if( N == 1 ) // if there's a single node
            return 0;
        if( !isConnected(network) )
            return 0;

        int result = N-1;

        // Need to create a new graph with the doubled nodes, and edges with capacity 1.
        // If the network is undirected, it has to be transformed into a directed network.
        Network flow = new Network(true);
        final Object[] w1s = new Object[N];
        final Object[] w2s = new Object[N];
        final Edge[] edgeW12 = new Edge[N];
        for( int i = 0 ; i < N ; i++ )
            {
            w1s[i] = new Object();
            flow.addNode(w1s[i]);
            w2s[i] = new Object();
            flow.addNode(w2s[i]);
            edgeW12[i] = new Edge( w1s[i], w2s[i], new FlowData(0,1) );
            flow.addEdge( edgeW12[i] );
            }
        if( network.isDirected() )
            {
            for( int i = 0 ; i < N ; i++ )
                {
                final Object node = network.allNodes.objs[i];
                final Bag edgesIn = network.getEdgesIn(node);
                for( int j = 0 ; j < edgesIn.numObjs ; j++ )
                    flow.addEdge( w2s[network.getNodeIndex(((Edge)(edgesIn.objs[j])).from())], w1s[network.getNodeIndex(node)], new FlowData(0,1) );
                }
            }
        else // undirected
            {
            for( int i = 0 ; i < N ; i++ )
                {
                final Object node = network.allNodes.objs[i];
                Bag edges = network.getEdgesIn(node);
                for( int j = 0 ; j < edges.numObjs ; j++ )
                    {
                    flow.addEdge( w2s[network.getNodeIndex(((Edge)(edges.objs[j])).getOtherNode(node))], w1s[network.getNodeIndex(node)], new FlowData(0,1) );
                    }
                }
            }

        // now the network is created, we need to:
        // 1. select an arbitrary vertex u of minimum degree
        // 2. compute k1 <- min K(u,v) with v in V\{u} and v not adjacent to u
        // 3. compute k2 <- min K(x,y) with x and y adjacent to u and x not adjacent to y
        // 4. the node connectivity equals the minimum of k1 and k2
        // so, let's get hacking

        // 1. select an arbitrary vertex u of minimum degree.
        int uIndex = 0;
        int minDegree = Integer.MAX_VALUE;
        for( int i = 0 ; i < N; i++ )
            {
            final int deg = network.getEdgesOut(network.allNodes.objs[i]).numObjs+network.getEdgesIn(network.allNodes.objs[i]).numObjs;
            if( minDegree > deg )
                {
                uIndex = i;
                minDegree = deg;
                }
            }

        // for easier access
        final Object u = network.allNodes.objs[uIndex];
        final Bag uIn = network.getEdgesIn(u);
        final Bag uOut = network.getEdgesOut(u);

        // 2. compute k1 <- min K(u,v) with v in V\{u} and v not adjacent to u
        // NOTE: if the graph is directed, we use both K(u,v) and K(v,u)
        final boolean[] adjacent = new boolean[N];
        if( network.isDirected() )
            {
            // compute min K(u,v) for all v where (u,v) does not exist in the graph
            for( int i = 0 ; i < N ; i++ )
                adjacent[i] = false;
            adjacent[uIndex] = true;
            for( int i = 0 ; i < uOut.numObjs ; i++ )
                {
                final Object toNode = ((Edge)(uOut.objs[i])).to();
                adjacent[network.getNodeIndex(toNode)] = true;
                }
            for( int i = 0 ; i < N ; i++ )
                if( !adjacent[i] )
                    {
                    final int res = maxFlowNodeConnectivity( flow, u, uIndex, network.allNodes.objs[i], i, w1s, w2s, edgeW12 );
                    if( result > res )
                        result = res;
                    }

            // compute min K(v,u) for all v where (v,u) does not exist in the graph
            for( int i = 0 ; i < N ; i++ )
                adjacent[i] = false;
            adjacent[uIndex] = true;
            for( int i = 0 ; i < uIn.numObjs ; i++ )
                {
                final Object toNode = ((Edge)(uIn.objs[i])).from();
                adjacent[network.getNodeIndex(toNode)] = true;
                }
            for( int i = 0 ; i < N ; i++ )
                if( !adjacent[i] )
                    {
                    final int res = maxFlowNodeConnectivity( flow, network.allNodes.objs[i], i, u, uIndex, w1s, w2s, edgeW12 );
                    if( result > res )
                        result = res;
                    }
            }
        else // the network is not directed
            {
            for( int i = 0 ; i < N ; i++ )
                adjacent[i] = false;
            adjacent[uIndex] = true;
            for( int i = 0 ; i < uOut.numObjs ; i++ )
                {
                final Object toNode = ((Edge)(uOut.objs[i])).getOtherNode(u);
                adjacent[network.getNodeIndex(toNode)] = true;
                }
            for( int i = 0 ; i < N ; i++ )
                if( !adjacent[i] )
                    {
                    final int res = maxFlowNodeConnectivity( flow, u, uIndex, network.allNodes.objs[i], i, w1s, w2s, edgeW12 );
                    if( result > res )
                        result = res;
                    }
            }

        // 3. compute k2 <- min K(x,y) with x and y adjacent to u and x not adjacent to y
        if( network.isDirected() )
            {
            for( int n1 = 0 ; n1 < uIn.numObjs+uOut.numObjs ; n1++ )
                {
                final Object fromNode = n1<uIn.numObjs ? ((Edge)(uIn.objs[n1])).getOtherNode(u) : ((Edge)(uOut.objs[n1-uIn.numObjs])).getOtherNode(u);
                final int fromNodeIndex = network.getNodeIndex(fromNode);
                for( int i = 0 ; i < N ; i++ )
                    adjacent[i] = false;
                adjacent[uIndex] = true;
                adjacent[fromNodeIndex] = true;
                for( int i = 0 ; i < uIn.numObjs+uOut.numObjs ; i++ )
                    {
                    final Object toNode = i<uIn.numObjs ? ((Edge)(uIn.objs[i])).getOtherNode(u) : ((Edge)(uOut.objs[i-uIn.numObjs])).getOtherNode(u);
                    adjacent[network.getNodeIndex(toNode)] = true;
                    }
                for( int i = 0 ; i < N ; i++ )
                    if( !adjacent[i] )
                        {
                        final int res = maxFlowNodeConnectivity( flow, fromNode, fromNodeIndex, network.allNodes.objs[i], i, w1s, w2s, edgeW12 );
                        if( result > res )
                            result = res;
                        }
                }
            }
        else // the network is not directed
            {
            for( int n1 = 0 ; n1 < uIn.numObjs ; n1++ )
                {
                final Object fromNode = ((Edge)(uIn.objs[n1])).getOtherNode(u);
                final int fromNodeIndex = network.getNodeIndex(fromNode);
                for( int i = 0 ; i < N ; i++ )
                    adjacent[i] = false;
                adjacent[uIndex] = true;
                adjacent[fromNodeIndex] = true;
                for( int i = 0 ; i < uOut.numObjs ; i++ )
                    {
                    final Object toNode = ((Edge)(uOut.objs[i])).getOtherNode(u);
                    adjacent[network.getNodeIndex(toNode)] = true;
                    }
                for( int i = 0 ; i < N ; i++ )
                    if( !adjacent[i] )
                        {
                        final int res = maxFlowNodeConnectivity( flow, fromNode, fromNodeIndex, network.allNodes.objs[i], i, w1s, w2s, edgeW12 );
                        if( result > res )
                            result = res;
                        }
                }
            }

        // 4. the node connectivity equals the minimum of k1 and k2
        return result;
        }

    static int maxFlowNodeConnectivity( final Network flow,
        final Object source,
        final int sourceIndex,
        final Object sink,
        final int sinkIndex,
        final Object[] w1s,
        final Object[] w2s,
        final Edge[] edgeW12 )
        {
        flow.removeEdge( edgeW12[sourceIndex] );
        flow.removeEdge( edgeW12[sinkIndex] );
        final int result = maxFlow( flow, w2s[sourceIndex], w1s[sinkIndex] );
        flow.addEdge( edgeW12[sinkIndex] );
        flow.addEdge( edgeW12[sourceIndex] );
        return result;
        }

    }
