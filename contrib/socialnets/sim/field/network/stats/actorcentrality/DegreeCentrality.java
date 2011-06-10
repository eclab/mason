/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.network.stats.actorcentrality;
import sim.field.network.*;
//import sim.util.*;
/**
 * Actor Degree Centrality (Wasserman and Faust, page 178)
 * 
 * @author Gabriel Catalin Balan
 */
public class DegreeCentrality extends FreemanNodeIndex 
    {
    final int maxCummulDiff;
    public DegreeCentrality(final Network network)
        {
        super(network);
        int n = network.allNodes.numObjs;
        //see page 199 for (n-1)^2.
        maxCummulDiff = (network.isDirected())? (n-1)*(n-1):(n-1)*(n-2);
        //I computed maxCummulDiff here so getMaxCummulativeDifference would inline     
        }

    public double getValue(final Object node) 
        {
//              Network.IndexOutIn inout = (Network.IndexOutIn)network.indexOutInHash.get(node);
//              Bag b = inout.out;
        return network.getEdgesOut(node).numObjs;
        }
        
    public double getMaxValue()
        {
        return network.allNodes.numObjs-1;
        }
        
    public double getMaxCummulativeDifference()
        {
        return (double)(maxCummulDiff);
        }
    }
