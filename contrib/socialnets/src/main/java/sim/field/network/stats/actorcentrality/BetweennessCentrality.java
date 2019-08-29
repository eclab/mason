/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.network.stats.actorcentrality;
import sim.field.network.*;
import sim.field.network.stats.*;

/**
 * 
 * Actor Betweenness Centrality:
 * <ul>
 * <li>For undirected graphs (Wasserman and Faust, page 190) I count the geodesics between k and j just once 
 * (look for <code>j &lt k</code> in <code>getValue()</code>)</li>
 * <li>For directed graphs (Wasserman and Faust, page 201) the geodesics from k to j are different from those from j to k, hence the
 * x2 in <code>getMaxValue()</code>. I am not 100% sure about the x2 in 
 * <code>getMaxCummulativeDifference()</code></li>
 * </ul>
 * 
 * @author Gabriel Catalin Balan
 */
//TODO I believe that they say "transform the directed graph into and undirected one -by 
//ignoring edges ij when there's no ji-, perform the standard computation and x2 the results."

public class BetweennessCentrality extends FreemanNodeIndex {
    final long[][][]g3;//g_{ikj}= # shortest paths between i and j that go through k
    final long[][]g2;  //g_{ij} = # shortest paths between i and j 
        
    public BetweennessCentrality(final Network network)
        {
        super(network);
        g3 = NetworkStatistics.getNumberShortestPathsWithIntermediatesMatrix(   network,
            UnitEdgeMetric.defaultInstance,
            0d);
        g2 = NetworkStatistics.getNumberShortestPathsMatrix(    network, 
            UnitEdgeMetric.defaultInstance,
            0d);
        //(since the weights are integer values (hops), precisison is 0)
        }
        
    //note that g3[][0][0] is zero, so we are consistent with the idea that i must be distinct from k and j in g3[k][i][j];
    public double getValue(final Object node) {
        int index = network.getNodeIndex(node);
        int n = network.allNodes.numObjs;
        double sum = 0;


        if(network.isDirected())
            for(int k=0;k<n;k++)
                {
                if(k==index) continue;
                long[] g2_k = g2[k];
                long[] g3_ki = g3[k][index];
                for(int j=0;j<n;j++)
                    sum  += ((double)g3_ki[j])/g2_k[j];
                }               
        else
            for(int k=1;k<n;k++)//I start from 1 since no j can be <0.
                {
                if(k==index) continue;
                long[] g2_k = g2[k];
                long[] g3_ki = g3[k][index];
                for(int j=0;j<k;j++)
                    sum  += ((double)g3_ki[j])/g2_k[j];
                }               
        return sum;
        }


    public double getMaxCummulativeDifference()
        {
        int n = network.allNodes.numObjs;
        int value = (n-1)*(n-1)*(n-2);
        if(network.isDirected())
            return value;
        return .5*value;
        }
        
    public double getMaxValue()
        {
        int n = network.allNodes.numObjs;
        int value = (n-1)*(n-2);
        if (network.isDirected())
            return value;
        return 0.5*value;
        }
    }
