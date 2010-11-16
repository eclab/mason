/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.network.stats;
import sim.field.network.*;
import sim.field.network.stats.actorcentrality.*;
import sim.util.*;
/**
 * @author Gabriel Catalin Balan
 * 
 */
public class CentralizationStatistics 
    {
    final double[] centralityIndices;
    final double[] stdCentralityIndices;
    final double   avgCentrality;
    final double   stdAvgCentrality;
    final double   maxCentrality;
    final double   stdMaxCentrality;
        
    final int n;
    //TODO I could offer both normalized and non normalized variance and group centraolization
    // by keeping maxValue
    public CentralizationStatistics(final Network network, final NodeIndex metric)
        {
        n = network.allNodes.numObjs;
        centralityIndices = new double[n];
        stdCentralityIndices = new double[n];
        double maxPossibleValue = metric.getMaxValue();         
        double max = Double.NEGATIVE_INFINITY;
        double sum = 0;
        double oneOverMax = 1d/maxPossibleValue;//multiplication is faster than division.
        for(int i = 0; i< network.allNodes.numObjs;i++)
            {
            double value = metric.getValue(network.allNodes.objs[i]);
            centralityIndices[i] = value;
            stdCentralityIndices[i] = value*oneOverMax;
            sum+=value;
            if(max<value)
                max=value;
            }
        avgCentrality = sum/n;
        stdAvgCentrality = avgCentrality*oneOverMax;
        maxCentrality = max;
        stdMaxCentrality = max*oneOverMax;
        }
        
    /**
     * The average node centrality is Group Centralization for metrics like
     * <ul>
     * <li>Information Centralization (Wasserman and Faust, page 197),</li>
     * <li>Proximity Prestige (Wasserman and Faust, page 204),</li>
     * <li>etc.</li>
     * </ul>
     */
    public double getCentralizationIndexMean(boolean useNormalization)
        {
        return useNormalization? stdAvgCentrality: avgCentrality;
        }       
        
    /**
     * 
     * @param unbiased according to (Wasserman and Faust, page 180), the sum of (avg - val_i)^2 
     * should be diveded by n. I also offer the unbiased variance (i.e. dividing by n-1).
     * 
     */
    public double getCentralizationIndexVariance(boolean useNormalization, boolean unbiased)
        {
        double sum = 0;
        double[] values;
        double avg;
        if(useNormalization)
            {
            values =  stdCentralityIndices;
            avg = stdAvgCentrality;
            }
        else
            {
            values = centralityIndices;
            avg = avgCentrality;
            }
        for(int i = 0; i< n;i++)
            {
            double dif = avg - values[i];
            sum+= dif*dif;
            }
        return sum/(unbiased? n-1:n);
        }
        
    /**
     * Identifies the points with the smallest maximal distance to all other points.
     */
// TODO I should use a Centrality measure: 1/max geodesic.
    public static Bag getGraphTheoreticCenter(final Network network)
        {
        double[][] distances = NetworkStatistics.getShortestPathsMatrix(network, 
            UnitEdgeMetric.defaultInstance);
        int n = network.allNodes.numObjs;
        int[] maxGeodesics = new int[n];
        int count=0;
        int min = n;
        for(int i=0;i<n;i++)
            {
            double[] distances_i = distances[i];
            int maxG_i = 0;
            for(int j=0; j<n;j++)
                {
                int dist_ij = (int)distances_i[j];
                if(dist_ij>maxG_i)
                    maxG_i = dist_ij;
                //I could also compare with min to skip the rest of the column
                }
            maxGeodesics[i]=maxG_i;
            if(maxG_i>0 && maxG_i < min)//I do min>0 to avoid giving the price to an isolated node
                {
                min = maxG_i;                           
                count=1;
                }
            else if (maxG_i == min)
                count++;
            }
                

        if(min==n)
            //all nodes are isolated, so we'll return'em all.
            return new Bag(network.allNodes);
        Bag b = new Bag(count);
        for(int i=0;i<n;i++)
            if(maxGeodesics[i]==min)
                b.add(network.allNodes.objs[i]);
        return b;
        //I could have done it all in a single pass, by 
        //-using a linked list, but then there would be too many function calls          OR
        //-using a BAG, but then there might be resizes (arraycopy ops)                          OR 
        //-using a prealocated Bag (i.e. new Bag(n)), but then there would be waisted space     
        //
        //BUT asymptotically speaking, another O(n) is irrelevant
        }
        
    double freemanDenominator;
    boolean isFreeman = false;
    public CentralizationStatistics(final Network network, final FreemanNodeIndex metric)
        {
        this(network, (NodeIndex)metric);
        freemanDenominator = metric.getMaxCummulativeDifference();
        isFreeman = true;
        //while useNormalization is irrelevant for getGeneralCentralizationIndex, its not for variance
        }
        
    /**
     * Computes Freeman's General Centralization Index (Wasserman and Faust, page 177)
     */
    public double getGeneralCentralizationIndex()
        {
        if(!isFreeman)
            throw new RuntimeException("Computable only for FreemanCentralityMeasures");
        // return Sum(max - val_i) / max_sum = [n*max-Sum(val)]/max_sum
        return (maxCentrality-avgCentrality)*n /freemanDenominator;
        }
        
    }
