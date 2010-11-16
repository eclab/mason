/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.network.stats;
import sim.field.network.*;

/**
   The WeightedEdgeMetric is a simple class that implements the <code>WeightComputer</code>
   interface for computing the weight of an edge.
*/
public class WeightedEdgeMetric implements EdgeMetric
    {
    /**
       Call the getWeight method of the edge to compute the weight of the edge.
    */
    public double getWeight( final Edge edge )
        {
        return edge.getWeight();
        }

    /**
       A static member to be used by whomever wants to quickly send one of these as a parameter to
       a statistic function.
    */
    public static WeightedEdgeMetric defaultInstance = new WeightedEdgeMetric();
    }
