/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.network.stats.actorcentrality;
import sim.field.network.stats.*;
import sim.field.network.*;
/**
 * Sabidussi's index of Actor Closeness Centrality (Wasserman and Faust, page 184)
 *
 * @author Gabriel Catalin Balan
 */
public class ClosenessCentrality extends FreemanNodeIndex {
    final double[][] allDistances;
    public ClosenessCentrality(final Network network)
        {
        super(network);
        allDistances = NetworkStatistics.getShortestPathsMatrix(network, UnitEdgeMetric.defaultInstance);               
        }

    public double getValue(final Object node) {
//              double[] distances = NetworkStatistics.getShortestPaths(network, node, UnitEdgeMetric.defaultInstance);

        double[] distances = allDistances[network.getNodeIndex(node)];
                
        double sum = 0;
        int n= distances.length;
        for(int i=0;i<distances.length; i++)
            sum+=distances[i];
        //I want to return 1/[Sum_{j!=i} disntance(i,j)] 
        //but the hop distance(i, i) is 0, so I don't bother not adding it
        return 1d/sum;
        }

    //TODO double check this, I back engineered it from the standardized value on page 186
    //TODO this will not hold for directed graphs (see page 200)
    public double getMaxCummulativeDifference()
        {
        int n = network.allNodes.numObjs;
        return ((double)(n-2))/(2*n-3);
        }
        
    public double getMaxValue()
        {
        return 1d/(network.allNodes.numObjs-1); 
        }
    }
