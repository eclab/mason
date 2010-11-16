/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.network.stats.actorcentrality;
import sim.field.network.*;
/**
 * 
 * Actor Centrality Index
 * 
 * <p>Normally this would be an interface containing 
 * <code>double getMeasure(final Network network, Object node)</code>.
 * BUT in some metrics (e.g. <code>InformationCentrality</code> and
 * <code>BetweennessCentrality</code>) all getMeasure calls use some common 
 * precomputed matrix.
 * 
 * <p>The downside of this is that one cannot have a single static instance of a certain centrality mesure, 
 * but as long as one does not have a large number of small Networks processed in a tight loop,
 * the overhead of allocating a new CentralityMeasure object for each graph is small. 
 * 
 * @author Gabriel Catalin Balan
 */
public abstract class NodeIndex 
    {
    public final Network network;
    public NodeIndex(final Network network){this.network=network;}
        
    //I would call this getCentrality, but prestige measures extend this as well
    public abstract double getValue(final Object node);

    //For the measures computed in the constructor, overwriting this saves a hash-table look-up 
    public double getValue(final int nodeIndex)
        {
        return getValue(network.allNodes.objs[nodeIndex]);
        }
    /**
     * The value of the metric might depend on the size of the graph.
     * this function divides these values by the max value.
     * 
     * <br>This is final because this is what I do in <code>CentralizationStatistics</code>
     * but without calling getValue(node) again.
     */
    final public double getStandardizedValue(final Object node)
        {
        return getValue(node)/getMaxValue();
        }
    public abstract double getMaxValue();

    }
