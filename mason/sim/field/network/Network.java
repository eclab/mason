/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.network;
import sim.util.*;
import java.util.*;

/** The Network is a field which stores binary graph and multigraph structures of all kinds, using hash tables to allow
    reasonably rapid dynamic modification.
    
    <p>The nodes of a Network's graph can be any arbitrary, properly hashable object.  The edges of the
    graph are members of the Edge class.  This class is little more than a wrapper around an arbitrary object as well (the Edge's 'info'
    object).  Thus your graph's nodes and edges can essentially be objects entirely of your choosing.
    
    <p>Edge objects also contain pointers to the Nodes that they are to and from (plus some auxillary index information
    for speed).
    
    <p>Nodes and Edges are stored in the Network using two data structures: a Bag containing all the nodes in the Field;
    and a HashMap which maps each Node to a container holding the Node's index in the Bag, plus a Bag of the Node's outgoing
    Edges and a Bag of the Node's incoming Edges.  Ordinarily you won't fool with these structures other than to scan through
    them (in particular, to scan rapidly through the allNodes bag rather than use an iterator).
    
    <p>To add a node to the Network, simply use addNode(node).  To remove a node, use removeNode(node).
    To add an edge to the Network, use addEdge(fromNode,toNode,edgeInfoObject), where edgeInfoObject is your
    arbitrary edge object. Alternatively, you can make an Edge object from scratch and add it with addEdge(new Edge(fromNode, toNode, edgeInfoObject)).
    You remove edges with removeEdge(edge).  If you add an edge, and its nodes have not been added yet, they will
    automatically be added as well.
    
    <p>Traversing a Network is easy.  
    To get a Bag of all the incoming (or outgoing) Edges to a node, use getEdgesIn(node) or getEdgesOut(node).
    Do <b>not</b> add or remove Edges from this Bag -- it's used internally and we trust you here.  Also don't expect the
    Bag to not change its values mysteriously later on.  Make a copy of the Bag if you want to keep it and/or modify it.
    Once you have an Edge, you can call its to() method and from() methods to get the nodes it's from and to, and you can
    at any time get and modify its info object.  The to() and from() are fast and inlined.
    
    <p>However, the getEdgesIn(node) and getEdgesOut(node) methods are not super fast: they require a hash lookup.  If you
    are planning on applying an algorithm on the Network which doesn't change the topology at all but traverses it a lot
    and changes just the <b>contents</b> of the edge info objects and the node object contents, you might consider first 
    getting an adjacency list for the Network with getAdjacencyList(...), or an adjacency matrix with getAdjacencyMatrix(...)
    or getMultigraphAdjacencyMatrix(...).  But remember that as soon as the topology changes (adding/deleting a node or edge),
    the adjacency list is invalid, and you need to request another one.
    
    <p><b>Computational Complexity.</b>  Adding a node or an edge is O(1).  Removing an edge is O(1).  Removing a node is O(m), where
    m is the total number of edges in and out of the node.  Removing all nodes is O(1) and fast.  Getting the in-edges or out-edges for a node
    is O(1).  Getting the to or from node for an edge is O(1) and fast.
    
    <p><b>Warning About Hashing.</b>  Java's hashing method is broken in an important way.  One can override the hashCode() and equals()
    methods of an object so that they hash by the value of an object rather than just the pointer to it.  But if this is done, then if
    you use this object as a key in a hash table, then <i>change</i> those values in the object, it will break the hash table -- the key
    and the object hashed by it will both be lost in the hashtable, unable to be accessed or removed from it.  The moral of the story is:
    do not override hashCode() and equals() to hash by value unless your object is <i>immutable</i> -- its values cannot be changed.  This
    is the case, for example, with Strings, which hash by value but cannot be modified.  It's also the case with Int2D, Int3D, Double2D,
    and Double3D, as well as Double, Integer, etc.  Some of Sun's own objects are broken in this respect: Point, Point2D, etc. are both
    mutable <i>and</i> hashed by value.
    
    <p>This affects you in only one way in a Network: edges are hashed by nodes.  The Network permits you to use any object
    as a node -- but you have been suitably warned: if you use a mutable but hashed-by-value node object, do NOT modify its values while
    it's being used as a key in the Network.
    
    <p><b>Directed vs. Undirected Graphs.</b>  Networks are constructed to be either directed or undirected, and they cannot be changed
    afterwards.  If the network is directed, then an Edge's to() and from() nodes have explicit meaning: the Edge goes from() one node to()
    another.  If the network is undirected, then to() and from() are simply the two nodes at each end of the Edge with no special meaning,
    though they're always consistent.  The convenience method <i>edge</i>.getOtherNode(<i>node</i>) will provide "other" node (if node is to(),
    then from() is returned, and vice versa).  This is particularly useful in undirected graphs where you could be entering an edge as to()
    or as from() and you just want to know what the node on the other end of the edge is.
        
    <p>There are three methods for getting all the edges attached to a node: getEdgesIn(), getEdgesOut(), and the less efficient getEdges().  These methods
    work differently depending on whether or not the network is directed:
        
    <p><table width="100%" border=0>
    <tr><td><td><b>Directed</b><td><b>Undirected</b>
    <tr><td><b>getEdgesIn()</b><td>Bag&nbsp;of&nbsp;incoming&nbsp;edges<td>Bag&nbsp;of&nbsp;all&nbsp;edges
    <tr><td><b>getEdgesOut()</b><td>Bag&nbsp;of&nbsp;outgoing&nbsp;edges<td>Bag&nbsp;of&nbsp;all&nbsp;edges
    <tr><td><b>getEdges()</b><td><i>Modifiable</i>&nbsp;Bag&nbsp;of&nbsp;all&nbsp;edges<td><i>Modifiable</i>&nbsp;Bag&nbsp;of&nbsp;all&nbsp;edges
    </table>
        
    <p><b>Hypergraphs.</b> Network is binary.  In the future we may provide a Hypergraph facility if it's needed, but for now you'll
    need to make "multi-edge nodes" and store them in the field, then hook them to your nodes via Edges.  For example, to store the
    relationship foo(node1, node2, node3), here's one way to do it:
    <ol>
    <li>Make a special foo object.
    <li>field.addEdge(foo,node1,new Double(0));
    <li>field.addEdge(foo,node2,new Double(1));
    <li>field.addEdge(foo,node3,new Double(2));
    </ol>
*/

public class Network implements java.io.Serializable
    {
    final public boolean directed;
    
    /** Constructs a directed or undirected graph. */
    public Network(boolean directed){this.directed = directed;  }

    /** Constructs a directed graph */
    public Network(){this(true); }
    
        
    /** Hashes Network.IndexOutIn structures by Node.  These structures
        contain the incoming edges of the Node, its outgoing edges, and the index of
        the Node in the allNodes bag. */
    public HashMap indexOutInHash = new HashMap();

    // perhaps rather than using a bag we should use an edge array... it'd be faster...
    /** All the objects in the sparse field.  For fast scans.  Do not rely on this bag always being the same object. */
    public Bag allNodes = new Bag();
        
    // returned instead of null for those methods which require a guarantee that the returned Bag should never be touched.
    final Bag emptyBag = new Bag();

    /** Creates and returns an adjacency list.  If you're doing lots of operations (especially network traversals)
        which won't effect the topology of the network, an adjacency list structure might be more efficient for you to access rather than lots of
        calls to getEdgesIn() and getEdgesOut() etc.  Building the list is an O(#edges) operation.
        
        <p>The adjacency list is an array of Edge arrays.  Each edge array holds all outgoing edges from a node
        (if outEdges is true -- otherwise it's the incoming edges to the node).  The edge arrays are ordered in
        their parent array in the same order that the corresponding nodes are ordered in the allNodes bag.
           
        <p>As soon as you modify any part of the Network's topology (through addEdge(), addNode(), removeEdge(),
        removeNode(), removeAllNodes(), etc.), the adjacency list data is invalid and should not be used.  Instead, request
        a new adjacency list. 
        
        <p>You can modify these edge arrays any way you like, though the Edge objects are the actual Edges.
    */
    public Edge[][] getAdjacencyList(boolean outEdges)
        {
        final Edge[][] list = new Edge[allNodes.numObjs][];
        for(int x=0;x<allNodes.numObjs;x++)
            {
            // load each list slot with an array consisting of all in or out edges for the node
            final Bag edges = 
                (outEdges ? getEdgesOut(allNodes.objs[x]) : getEdgesIn(allNodes.objs[x]));
            list[x] = new Edge[edges.numObjs];
            final Edge[] l = list[x];  // a little faster, one less level of indirection
            final int n = edges.numObjs;  // likewiswe
            final Object[] objs = edges.objs; // likewise
            System.arraycopy(objs,0,l,0,n); // I don't know if this is faster or not, given the type mismatch -- Sean
            /*            for(int y=0;y<n;y++)
                          l[y] = (Edge)(objs[y]);  // hmmm, can we do an array copy across array types?
            */
            }
        return list;
        }
        
    /** Creates and returns a simple adjacency matrix, where only one edge between any two nodes is considered -- if you're
        using a multigraph, use getMultigraphAdjacencyMatrix() instead.  If you're doing lots of operations (especially network traversals)
        which won't effect the topology of the network, an adjacency matrix structure might be more efficient for you to access rather than lots of
        calls to getEdgesIn() and getEdgesOut() etc.  Building the matrix is an O(#edges + #nodes^2) operation.
        
        <p>The adjacency matrix is a two-dimensional array of Edges, each dimension as long as the number of nodes in the graph.
        Each entry in the array is either an Edge FROM a node TO another, or it is null (if there is no such edge).  If there are multiple
        edges between any two nodes, an arbitrary one is chosen.  The Edge array returned is organized as Edge[FROM][TO].
        The indices are ordered in the same order that the corresponding nodes are ordered in the allNodes bag.
           
        <p>As soon as you modify any part of the Network's topology (through addEdge(), addNode(), removeEdge(),
        removeNode(), removeAllNodes(), etc.), the adjacency matrix data is invalid and should not be used.  Instead, request
        a new adjacency matrix. 
        
        <p>You can modify the array returned any way you like, though the Edge objects are the actual Edges.
    */
    public Edge[][] getAdjacencyMatrix()
        {
        final int n = allNodes.numObjs;
        final Edge[][] matrix = new Edge[n][n];   // I assume it filled with nulls?

        Iterator nodeIO = indexOutInHash.values().iterator();
        while(nodeIO.hasNext()) // this replaces n hash lookups with n class casts
            {
            IndexOutIn ioi = (IndexOutIn)nodeIO.next();
            if(ioi.out==null) continue;
            int outDegree = ioi.out.numObjs;
            Edge[] outEdges = matrix[ioi.index];
            Object sourceNode =  allNodes.objs[ioi.index];
                        
            for(int i=0;i<outDegree;i++)
                {
                Edge e = (Edge)ioi.out.objs[i];
                // this is getNodeIndex without the function call
                outEdges[((IndexOutIn)indexOutInHash.get(e.getOtherNode(sourceNode))).index] = e;
                }
            }
        return matrix;
        }
        
    /** Creates and returns a multigraph adjacency matrix, which includes all edges from a given node to another -- if you know for sure
        that you have a simple graph (no multiple edges between two nodes), use getAdjacencyMatrix instead.  
        If you're doing lots of operations (especially network traversals) which won't effect the topology of the network, an 
        adjacency matrix structure might be more efficient for you to access rather than lots of calls to getEdgesIn() and 
        getEdgesOut() etc.  Building the matrix is expensive: it's an O(#edges + #nodes^2) operation.
        
        <p>The adjacency matrix is a two-dimensional array of Edge arrays, both of the dimensions as long as the number of nodes in the graph.
        Each entry in this two-dimensional array is an <b>array</b> of all edges FROM a node TO another.  Thus the
        returned array structure is organized as Edge[FROM][TO][EDGES].
        The FROM and TO indices are ordered in the same order that the corresponding nodes are ordered in the allNodes bag.
    
        <p>Important note: if there are <i>no</i> edges FROM a given node TO another, an empty array is placed in that entry.
        For efficiency's sake, the <i>same</i> empty array is used.  Thus you should not assume that you can compare edge arrays
        for equality (an unlikely event anyway). 
                                         
        <p>As soon as you modify any part of the Network's topology (through addEdge(), addNode(), removeEdge(),
        removeNode(), removeAllNodes(), etc.), the adjacency matrix data is invalid and should not be used.  Instead, request
        a new adjacency matrix. 
        
        <p>You can modify the array returned any way you like, though the Edge objects are the actual Edges.
    */
    public Edge[][][] getMultigraphAdjacencyMatrix()
        {
        final int n = allNodes.numObjs;
        final Edge[][][] matrix = new Edge[n][n][]; //I assume it filled with nulls?

        Iterator nodeIO = indexOutInHash.values().iterator();
        Bag[] tmp  = new Bag[n];
        for(int i=0; i<n;i++)
            tmp[i]=new Bag(n);
                        
        while(nodeIO.hasNext()) // this replaces n hash lookups with n class casts
            {
            IndexOutIn ioi = (IndexOutIn)nodeIO.next();
            if(ioi.out==null) continue;
            int outDegree = ioi.out.numObjs;
            Object sourceNode =  allNodes.objs[ioi.index];

            for(int i=0;i<outDegree;i++)
                {
                Edge e = (Edge)ioi.out.objs[i];
                //this is getNodeIndex without the function call
                int j = ((IndexOutIn)indexOutInHash.get(e.getOtherNode(sourceNode))).index;
                tmp[j].add(e);
                }       
                        
            Edge[][] outEdges = matrix[ioi.index];
            for(int i=0;i<n;i++)
                {
                Bag b = tmp[i];
                int ne = b.numObjs;
                outEdges[i]=new Edge[ne];
                if(ne>0)
                    {
                    System.arraycopy(b.objs,0,outEdges[i], 0, ne );
                    b.clear();
                    }
                }       
            }
        //now the nodes with 0 out-degree have a row full of nulls in the matrix
        for(int i=0;i<n;i++)
            {
            Edge[][] e2 = matrix[i];
            if(e2[0]==null)
                for(int j=0;j<n;j++)
                    e2[j]=emptyEdgeArray;
            }
                 
        return matrix;
        }
        
    static Edge[] emptyEdgeArray = new Edge[0];


    /** Get all edges that leave a node.  Do NOT modify this Bag -- it is used internally. */
    // this bizarre construction puts us just at 32 bytes so we can be inlined
    public Bag getEdgesOut( final Object node )
        {
        IndexOutIn ioi = (IndexOutIn)(indexOutInHash.get(node));
        Bag b;
        if (ioi==null || (b=ioi.out)==null) return emptyBag;
        return b;
        }

    /** Get all edges that enter a node.  Do NOT modify this Bag -- it is used internally. */
    // this bizarre construction puts us just at 32 bytes so we can be inlined
    public Bag getEdgesIn( final Object node )
        {
        IndexOutIn ioi = (IndexOutIn)(indexOutInHash.get(node));
        Bag b;
        if (ioi==null || (b=ioi.in)==null) return emptyBag;
        return b;
        }
    
    /** Get all the edges that enter or leave a node.  If a Bag is provided, it will be cleared, then filled and returned.
        Else a Bag will be constructed and returned.  If the graph is undirected, then edgesIn and edgesOut should
        be the same thing, and so this is roughly equivalent to bag.addAll(getEdgesIn(node));  If the graph is
        directed, then both the edgesIn AND the edgesOut are added to the Bag.  Generally speaking you should
        try to use the more efficient getEdgesIn(...) and getEdgesOut(...) methods instead if you can.
    */
    public Bag getEdges( final Object node, Bag bag )
        {
        if( !directed )
            {
            if ( bag == null) bag = new Bag();
            else bag.clear();
                        
            IndexOutIn ioi = (IndexOutIn)(indexOutInHash.get(node));
            if (ioi==null) return bag;
            if (ioi.in!=null && ioi.in.numObjs>0) bag.addAll(ioi.in);
            //if (ioi.out!=null && ioi.in!=ioi.out && ioi.out.numObjs>0) bag.addAll(ioi.out);
            return bag;
            }
        else
            {
            if ( bag == null) bag = new Bag();
            else bag.clear();
                        
            IndexOutIn ioi = (IndexOutIn)(indexOutInHash.get(node));
            if (ioi==null) return bag;
            if (ioi.in!=null && ioi.in.numObjs>0) bag.addAll(ioi.in);
            if (ioi.out!=null && ioi.out.numObjs>0) bag.addAll(ioi.out);
            return bag;
            }
        }
        
    /** Add a node */
    public void addNode( final Object node )
        {
        if( indexOutInHash.get( node ) != null ) // if the object already exists
            return;
        allNodes.add( node );
        IndexOutIn ioih = new IndexOutIn( allNodes.numObjs-1, null, null );
        indexOutInHash.put( node, ioih );
        }

    /** Add an edge, storing info as the edge's associated information object. 
        If you add an edge, and its nodes have not been added yet, they will
        automatically be added as well. */
    public void addEdge( final Object from, final Object to, final Object info )
        {
        addEdge( new Edge( from, to, info ) );
        }

    /** Add an edge. If you add an edge, and its nodes have not been added yet, they will
        automatically be added as well. Throws an exception if the edge is null or if it's
        already added to a Field (including this one). 
    */
    public void addEdge( final Edge edge )
        {
        if (edge==null) 
            throw new RuntimeException("Attempted to add a null Edge.");
            
        // assert ownership
        if (edge.owner!=null)
            throw new RuntimeException("Attempted to add an Edge already added elsewhere");
        edge.owner = this;
    
        edge.indexFrom = 0;
        edge.indexTo = 0;
        IndexOutIn outNode = (IndexOutIn)(indexOutInHash.get(edge.from));
        if( outNode==null)
            {
            addNode( edge.from );
            outNode = (IndexOutIn)(indexOutInHash.get(edge.from));
            }
        if( outNode.out == null )
            {
            if(directed)
                outNode.out = new Bag();
            else
                {
                if(outNode.in!=null)
                    outNode.out = outNode.in;
                else
                    outNode.out = outNode.in = new Bag();
                }
            }
        outNode.out.add( edge );
        edge.indexFrom = outNode.out.numObjs-1;

        IndexOutIn inNode = (IndexOutIn)(indexOutInHash.get(edge.to));
        if( inNode==null)
            {
            addNode( edge.to );
            inNode = (IndexOutIn)(indexOutInHash.get(edge.to));
            }
        if( inNode.in == null )
            {
            if(directed)
                inNode.in = new Bag();
            else
                {
                if(inNode.out!=null)
                    inNode.in = inNode.out;
                else
                    inNode.in = inNode.out = new Bag();
                }
            }
        inNode.in.add( edge );
        edge.indexTo = inNode.in.numObjs-1;
        }

    /** Removes the given edge, then changes its from, to, and info values to the provided ones,
        then adds the edge to the network again.  Ordinarily you wouldn't need to do this -- you can
        just remove an edge and add a new one.  But in the case that you want to reuse an edge (to track
        it in an inspector, for example), this function might be helpful given that Edge specifically
        denies you the ability to change its to and from values.  */
    public Edge updateEdge( Edge edge, final Object from, final Object to, final Object info)
        {
        edge = removeEdge(edge);
        if (edge != null)
            {
            edge.setTo(from, to, info, -1, -1);
            addEdge(edge);
            }
        return edge;
        }

    /** Removes an edge and returns it.  The edge will still retain its info, to, and from fields, so you can
        add it again with addEdge.   Returns null if the edge is null or if there is no such 
        edge added to the field.  
    */
    public Edge removeEdge( final Edge edge )
        {
        if( edge == null )
            return null;
        
        // remove ownership
        if (edge.owner != this)
            return null;
        edge.owner = null;
        // we'll do an extraneous hash if this is being called from removeNode...
        
        // remove the edge from the "out" node's "out" bag
        final Bag outNodeBag = ((IndexOutIn)(indexOutInHash.get(edge.from))).out;
        outNodeBag.remove( edge.indexFrom );
        if( outNodeBag.numObjs > edge.indexFrom )
            {
            Edge shiftedEdge = (Edge)(outNodeBag.objs[edge.indexFrom]);
            int shiftedIndex = outNodeBag.numObjs;  // the old location of the shifted edge
            if (directed)
                {
                // it's clear that it's an indexFrom
                shiftedEdge.indexFrom = edge.indexFrom;
                }
            else
                {
                // we don't know if the edge shifted down needs to have its indexFrom or indexTo changed.
                if (shiftedEdge.indexFrom == shiftedIndex &&
                    shiftedEdge.from.equals(edge.from))
                    shiftedEdge.indexFrom = edge.indexFrom;
                // this second 'if' can be eliminated if we don't have bugs reported in the final 'else'
                else if (shiftedEdge.indexTo == shiftedIndex &&
                    shiftedEdge.to.equals(edge.from))
                    shiftedEdge.indexTo = edge.indexFrom;
                else throw new InternalError("This shouldn't ever happen: #1");
                }
            }

        // remove the edge from the "in" node's "in" bag
        final Bag inNodeBag = ((IndexOutIn)(indexOutInHash.get(edge.to))).in;
        inNodeBag.remove( edge.indexTo );
        if( inNodeBag.numObjs > edge.indexTo )
            {
            Edge shiftedEdge = (Edge)(inNodeBag.objs[edge.indexTo]);
            int shiftedIndex = inNodeBag.numObjs;  // the old location of the shifted edge
            if (directed)
                {
                // it's clear that it's an indexTo
                shiftedEdge.indexTo = edge.indexTo;
                }
            else
                {
                // we don't know if the edge shifted down needs to have its indexFrom or indexTo changed.
                if (shiftedEdge.indexTo == shiftedIndex &&
                    shiftedEdge.to.equals(edge.to))
                    shiftedEdge.indexTo = edge.indexTo;
                // this second 'if' can be eliminated if we don't have bugs reported in the final 'else'
                else if (shiftedEdge.indexFrom == shiftedIndex &&
                    shiftedEdge.from.equals(edge.to))
                    shiftedEdge.indexFrom = edge.indexTo;
                else throw new InternalError("This shouldn't ever happen: #2");
                }
            }
        // return the edge
        return edge;
        }

    /** Removes a node, deleting all incoming and outgoing edges from the Field as well.  Returns the node,
        or null if there is no such node in the field. 
    */
    public Object removeNode( final Object node )
        {
        IndexOutIn ioi = (IndexOutIn)(indexOutInHash.get(node));

        if (ioi == null) { return null; }
                
        // remove all edges coming in the node
        while( ioi.out != null && ioi.out.numObjs > 0 )
            {
            removeEdge( (Edge)(ioi.out.objs[0]) );
            }

        // remove all edges leaving the node
        while( ioi.in != null && ioi.in.numObjs > 0 )
            {
            removeEdge( (Edge)(ioi.in.objs[0]) );
            }

        // remove the node from the allNodes bag
        allNodes.remove(ioi.index);
        if (allNodes.numObjs > ioi.index)    // update the index of the guy who just got moved
            {
            ((IndexOutIn)(indexOutInHash.get(allNodes.objs[ioi.index]))).index = ioi.index;
            }
                
        // finally, delete the ioi
        indexOutInHash.remove(node);

        // return the node
        return node;
        }

    /** Removes all nodes, deleting all edges from the Field as well.  Returns the nodes as a Bag, which you
        are free to modify as it's no longer used internally by the Network. */
    public Bag clear()
        {
        indexOutInHash = new HashMap();
        Bag retval = allNodes;
        allNodes = new Bag();
        return retval;
        }
    
    /** Synonym for clear(), here only for backward-compatibility.
        Removes all nodes, deleting all edges from the Field as well.  Returns the nodes as a Bag, which you
        are free to modify as it's no longer used internally by the Network. */
    public Bag removeAllNodes()
        {
        return clear();
        }

    /** Returns all the objects in the Sparse Field.  Do NOT modify the bag that you receive out this method -- it
        is used internally.  If you wish in modify the Bag you receive, make a copy of the Bag first, 
        using something like <b>new Bag(<i>foo</i>.getallNodes())</b>. */
    public Bag getAllNodes()
        {
        return allNodes;
        }
    
    /** Iterates over all objects.  
        NOT fail-fast, and remove() not supported.  Use this method only if you're
        woozy about accessing allObject.numObjs and allObject.objs directly. 
        
        For the fastest scan, you can do:
        <p><tt>
        
        for(int x=0;x&lt;field.allNodes.numObjs;x++) ... field.allNodes.objs[x] ... </tt>
        
        <p>... but do NOT modify the allNodes.objs array.
    */
    public Iterator iterator() { return allNodes.iterator(); }

    /** The structure stored in the indexOutInHash hash table.  Holds the index of
        a node in the allNodes bag, a Bag containing the node's outgoing edges,
        and a Bag containing the node's incoming edges.  */
    public static class IndexOutIn implements java.io.Serializable
        {
        /** Index of the node in the allNodes bag */
        public int index;
        /** Bag containing outgoing edges of (leaving) the node */
        public Bag out;
        /** Bag containing incoming edges of (entering) the node */
        public Bag in;
               
        public IndexOutIn(final int index, final Bag out, final Bag in)
            {
            this.index = index;
            this.out = out;
            this.in = in;
            }
        // static inner classes don't need serialVersionUIDs
        }

    /*
      Returns the index of a node.  If the node does not exist in the Network, a runtime exception is thrown. 
    */
    public int getNodeIndex( final Object node )
        {
        IndexOutIn ioi = (IndexOutIn)(indexOutInHash.get(node));
        if( ioi == null )
            throw new RuntimeException( "Object parameter is not a node in the network." );
        return ioi.index;
        }



    /**
     * This reverse the direction of all edges in the graph.
     * It is more expensive to clone the graph than to reverse the edges in place.
     * It is more than twice as fast to reverse the edges than to 
     * create the dual graph. As a matter of fact getDualNetwork() took 240 time units
     * while  two reverseAllEdges() calls took only 40 time units on a directed
     * graph (1time unit = 1 millisecond / 10000 calls).
     * 
     * In that case it is more advantageous to reverse the edges, 
     * compute whatever stats on the dual and revert it than to allocate memory.
     * 
     */
    public void reverseAllEdges()
        {
        if(!directed) return;//that was quick
        int n = allNodes.numObjs;
        Iterator i = indexOutInHash.values().iterator();
        for(int k=0;k<n;k++)
            {
            IndexOutIn ioi= (IndexOutIn)i.next();
            Bag tmpB = ioi.out;
            ioi.out = ioi.in;
            ioi.in = tmpB;
            if(ioi.in!=null)
                for(int j=0;j<ioi.in.numObjs;j++)
                    {
                    Edge e = (Edge)ioi.in.objs[j];
                    //reverse e
                    Object tmpO = e.from;
                    e.from = e.to;
                    e.to= tmpO;
                    
                    int tmpI = e.indexFrom;
                    e.indexFrom = e.indexTo;
                    e.indexTo = tmpI;
                    }
            }
        }
    
    
    /**
     * An advantage over calling addNode and addEdge n and m times, 
     * is to allocate the Bags the right size the first time.
     * @return a clone of this graph
     * 
     * I cannot use clone() cause it's too shallow.
     * I don't need the deep clone cause I want to reuse the nodes 
     * I need a special custom clone
     */
    public Network cloneGraph()
        {
        Network clone = new Network(directed);
        clone.allNodes.addAll(allNodes);
        int n = allNodes.numObjs;
        Iterator ioiIterator = indexOutInHash.values().iterator();
        Network.IndexOutIn[] ioiArray = new                Network.IndexOutIn[n];

        for(int k=0;k<n;k++)
            {
            IndexOutIn oldIOI = (IndexOutIn)ioiIterator.next();
            int index = oldIOI.index;
            Bag copyOutBag = oldIOI.out==null?new Bag():new Bag(oldIOI.out.numObjs);
            Bag copyInBag = directed? (oldIOI.in==null?new Bag():new Bag(oldIOI.in.numObjs)):copyOutBag;
            IndexOutIn clonedIOI = new IndexOutIn( oldIOI.index, copyOutBag, copyInBag);
            clone.indexOutInHash.put( allNodes.objs[index], clonedIOI);
            ioiArray[k]=oldIOI;
            }
        //so far I avoided any resizing.
        //TODO now I could work around addEdge, too.
        //for instance I already now "IndexOutIn oldIOI", no need to hash-table look-up it. 
        for(int k=0;k<n;k++)
            {
            IndexOutIn oldIOI = ioiArray[k];
            Object node_k = allNodes.objs[oldIOI.index];
            if(oldIOI.out!=null)
                for(int i=0;i<oldIOI.out.numObjs;i++)
                    {
                    Edge e = (Edge) oldIOI.out.objs[i];
                    if(directed || e.from==node_k)
                        clone.addEdge(new Edge(e.from, e.to, e.info));
                    }           
            }
        return clone;
        }



    /**
     * Complements the graph: same nodes, no edges where they were, edges where they were not.
     * 
     * An advantage over calling addNode and addEdge n and m times, 
     * is to allocate the Bags the right size the first time.
     */
    public Network getGraphComplement(boolean allowSelfLoops)
        {
        Network complement = new Network(directed);
        complement.allNodes.addAll(allNodes);
        int n = allNodes.numObjs;

        Iterator ioiIterator = indexOutInHash.values().iterator();
        Network.IndexOutIn[] ioiArray = new Network.IndexOutIn[n];
        int maxDegree = n-1+(allowSelfLoops?1:0);
        
        for(int k=0;k<n;k++)
            {
            Network.IndexOutIn oldIOI = (Network.IndexOutIn)ioiIterator.next();
            int index = oldIOI.index;
            //TODO if i am multigraph, oldIOI.out.numObjs might be bigger than n, 
            //so that's how you get it to bomb on your hands.
            Bag newOutBag = oldIOI.out==null?
                new Bag(maxDegree):
                new Bag(maxDegree-oldIOI.out.numObjs);
            Bag newInBag = (!directed)? 
                newOutBag:
                (oldIOI.in==null?
                new Bag(maxDegree):
                new Bag(maxDegree-oldIOI.in.numObjs));
            Network.IndexOutIn clonedIOI = new Network.IndexOutIn( oldIOI.index, newOutBag, newInBag);
            complement.indexOutInHash.put( allNodes.objs[index], clonedIOI);
            //so far I avoided any resizing.
            ioiArray[k]=oldIOI;
            }
        //TODO now I could work around addEdge, too.
        //for instance I already now "IndexOutIn oldIOI", no need to hash-table look-up it. 
        boolean[] edgeArray = new boolean[n];
        for(int k=0;k<n;k++)
            {
            Network.IndexOutIn oldIOI = ioiArray[k];
            int nodeIndex = oldIOI.index;
            Object nodeObj = allNodes.objs[nodeIndex];
            for(int i=0;i<n;i++) edgeArray[i]=true;
            if(!allowSelfLoops) edgeArray[nodeIndex]= false;
                
            if(oldIOI.out!=null)
                for(int i=0;i<oldIOI.out.numObjs;i++)
                    {
                    Edge e = (Edge) oldIOI.out.objs[i];
                    Object otherNode = e.getOtherNode(nodeObj);
                    int otherIndex = getNodeIndex(otherNode);
                    edgeArray[otherIndex]=false;
                    }
                
            for(int i=0;i<n;i++) 
                if(edgeArray[i] && (directed || nodeIndex<=i))
                    complement.addEdge(nodeObj,allNodes.objs[i], null);
            }
        return complement;
        }       

    }
