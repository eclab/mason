/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.network.stats.actorcentrality;
import sim.field.network.stats.*;
import sim.field.network.*;
/**
 *
 * Lin's Proximity Prestige (Wasserman and Faust, page 203)
 * 
 * <p> PPi=Ii^2/[(n-1)*Sum_{j in Ii}(dji)], where Ii = number 
 * of nodes in the influence domain of node i (i.e. number of nodes that can reach i).
 *
 * <p>Note that this is not a <code>FreemanCentralityMeasure</code>.
 * 
 * @author Gabriel Catalin Balan
 **/
public class ProximityPrestige extends NodeIndex {
    final double[][] allDistances;
    public ProximityPrestige(final Network network)
        {
        super(network);
        allDistances = NetworkStatistics.getShortestPathsMatrix(network, UnitEdgeMetric.defaultInstance);               
        }

    public double getValue(final Object node) {

        int i = network.getNodeIndex(node);
        int Ii = 0;
        double sum = 0;
        int n= network.allNodes.numObjs;
        for(int j=0;j<n; j++)
            {
            double dji = allDistances[j][i];
            if(dji!=Double.POSITIVE_INFINITY)
                Ii++;
            sum+=dji;
            }
        //I want to return 1/[Sum_{j!=i} dintance(i,j)] 
        //but the hop distance(i, i) is 0, so I don't bother
        return Ii*Ii/sum/(n-1);
        }
        
    public double  getMaxValue()
        {
        return 1d;
        }
    }
