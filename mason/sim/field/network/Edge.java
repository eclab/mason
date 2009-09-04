/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.network;
import sim.util.*;

/**
   An Edge stores a relationship between two objects in a Network.  Edges are directed: one
   object is a "from" object and one object is a "to" object.  Edges also hold an "info" object, which
   is an arbitrary object that you specify on your own.  An info object can be used to hold the edge
   weight, an edge label, or whatever you like.  Edges are "semi-mutable": you can change the info
   object at any time, but you may not change the to or from objects once the Edge has been constructed.
    
   <p>An Edge may be added to at most one Network.  You must remove it from that field before you
   can add it to a new Network.  The Edge's field is known as its <i>owner</i>.
   
   <p><b>Explicitly stating weights.</b>  The getWeight() function returns a plausible weight for the
   edge.  If your "info" object is a Number or a MutableDouble, or is sim.util.Valuable, then the
   weight of the edge is the doubleValue() of your object.  Else the weight of the edge is a default
   of 1.0.
*/
public class Edge implements java.io.Serializable
    {
    // to prevent edges from breaking fields by being stored in two different fields.
    // if null, then no owner -- the Edge is free to be added to a field.
    Network owner;

    // purposely package-level protection: you shouldn't play with these.
    /** The node from where the edge leaves */
    Object from;

    // purposely package-level protection: you shouldn't play with these.
    /** The node where the edge enters */
    Object to;

    /** Other information (maybe cost) associated with the edge */
    public Object info;

    // purposely package-level protection: you shouldn't play with these.
    /* The index where the Edge is located in the Out Bag of the edge's From node.
       Used internally for faster remove operations*/
    int indexFrom;

    // purposely package-level protection: you shouldn't play with these.
    /* The index where the Edge is located in the In Bag of the edge's To node.  
       Used internally for faster remove operations*/
    int indexTo;
    
    /** Returns the "from" object. */
    public Object getFrom() { return from; }
    /** Returns the "to" object. */
    public Object getTo() { return to; }
    
    /** Returns the "from" object. */
    public Object from() { return from; }
    /** Returns the "to" object. */
    public Object to() { return to; }
    /** Returns the "owner" field. */    
    public Network owner() { return owner; }
    /* Returns the index where the Edge is located in the Out Bag of the edge's From node.  
       Used internally for faster remove operations*/
    public int indexFrom() { return indexFrom; }

    /* The index where the Edge is located in the In Bag of the edge's To node.  Used internally for faster remove operations. */
    public int indexTo() { return indexTo; }

    public Edge( final Edge e )
        {
        setTo( e.from, e.to, e.info, e.indexFrom, e.indexTo );
        }

    public Edge( final Object from, final Object to, final Object info )
        {
        setTo( from, to, info, -1, -1 );
        }

    // internal package-level function to set various valoues.  Don't play with this.
    void setTo( final Object from, final Object to, final Object info, final int indexFrom, final int indexTo )
        {
        this.from = from;
        this.to = to;
        this.info = info;
        this.indexFrom = indexFrom;
        this.indexFrom = indexTo;
        }

    /**
       Returns the weight of the edge.  The default version of the function returns the value of the
       info object if it is a subclass of Number (including MutableDouble) or is Valuable, else returns 1.0.
    */
    public double getWeight()
        {
        if( info instanceof Number )
            return ((Number)info).doubleValue();
        else if (info instanceof Valuable)
            return ((Valuable)info).doubleValue();
        else
            return 1.0;
        }
                
    /**
       Returns the alternate to the provided node.  Specifically: if node == from(), then to() is
       returned; else from() is returned.  Note that if node != from() AND node != to(), then
       from() is still returned.
                
       <p>This method is useful for various algorithms which operate both on undirected and on
       directed graphs; rather than knowing if you're "to" or "from", you can just "get the node on the
       other side of the edge."
    */
    public Object getOtherNode(Object node)
        {
        if ( node.equals(from) )
            return to;
        return from;
        }

    public String toString()
        {
        return "Edge[" + from + "->" + to + ": " + info + "]";
        }
    }
