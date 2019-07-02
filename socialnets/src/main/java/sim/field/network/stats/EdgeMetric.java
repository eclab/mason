/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.network.stats;
import sim.field.network.*;
/**
   This interface provides a unified way of dealing with the weight of an edge.  The only method, getWeight,
   is supposed to compute and return the weight of the edge.

   Most statistics use this function call to compute different metrics related to the network.  One may set the
   weight of all edges to 1 in order to compute distances between nodes in terms of the number of edges that
   need to be traversed.  Alternatively, using real weights for the edges results in algorithms computing real
   distances between the nodes.
*/
public interface EdgeMetric
    {
    /**
       Returns the weight of the edge.
    */
    public double getWeight( final Edge edge );
    }
