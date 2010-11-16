/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.network.stats;
import sim.field.network.*;
/**
   The UnitEdgeMetric counts each edge as having weight 1.
*/
public class UnitEdgeMetric implements EdgeMetric
    {
    /**
       The weight of an edge is 1.
    */
    public double getWeight( final Edge edge )
        {//TODO maybe we should return 0 is edge is null (for use /w AdjacencyMatrixes)
        return 1.0;
        }

    /**
       A static member to be used by whomever wants to quickly send one of these as a parameter to
       a statistic function.
    */
    public static UnitEdgeMetric defaultInstance = new UnitEdgeMetric();
    }
