/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.network.stats;
import sim.field.network.*;
import sim.field.network.stats.*;
import java.util.*;
import sim.util.gui.*;
import sim.util.*;

// Provides the statistics used by the SocialNetworkInspector in the form
// of Java Bean Properties or other methods.
public class DisplayableNetworkStatistics 
    {
    public final Network net;
    public DisplayableNetworkStatistics(final Network net){this.net = net;}
    public double getClusteringCoefficient()
        {
        return NetworkStatistics.getClusteringCoefficient(net);
        }
        
    public double getDensity()
        {
        return NetworkStatistics.getDensity(net);
        }
        
    public double getDiameterByEdge()
        {
        return NetworkStatistics.getDiameter(net, new WeightedEdgeMetric());
        }

    public double getDiameterByWeight()
        {
        return NetworkStatistics.getDiameter(net, new UnitEdgeMetric());
        }

    public double getSymmetryCoefficient()
        {
        return NetworkStatistics.getSymmetryCoefficient(net);
        }

    // must be callled BEFORE nodeEccentricityLabels
    public double[] nodeEccentricityDistribution(EdgeMetric metric)
        {
        final int n = net.allNodes.numObjs;
        double [] eccentricities = new double[n];
        for( int i = 0 ; i < n ; i++ )
            eccentricities[i] = NetworkStatistics.getNodeEccentricity( net, net.allNodes.objs[i], metric);
        return eccentricities;
        }

    public String[] nodeEccentricityLabels()
        {
        final int n = net.allNodes.numObjs;
        String [] labels = new String[n];
        for( int i = 0 ; i < n ; i++ )
            labels[i]=net.allNodes.objs[i].toString();
        return labels;
        }

    // must be callled BEFORE degreeDistributionLabels
    int maxDegree;
    public double[] degreeDistribution(boolean out)
        {
        final int n = net.allNodes.numObjs;
        double[] data = new double[n];
        Iterator i = net.indexOutInHash.values().iterator();
        maxDegree = 0;
        for(int k=0;k<n;k++)
            {
            Network.IndexOutIn ioi= (Network.IndexOutIn)i.next();   
            Bag b = (out ? ioi.out : ioi.in);
            int d = (b==null)? 0: b.numObjs;
            data[k]= d;
            if(d>maxDegree) maxDegree = d;
            }               
        return MiniHistogram.makeBuckets(data, maxDegree, 0, maxDegree, false);
        }
    public String[] degreeDistributionLabels()
        {
        String[] labels = new String[maxDegree+1];
        for(int i=0;i<labels.length;i++) labels[i] = ""+i;
        return labels;
        }

    // must be callled BEFORE loglogScaleDegreeCCDFLabels
    public double[] loglogScaleDegreeCCDF(boolean out)
        {
        double[] data = DegreeStatistics.getDegreeCCDF(net, out);
        //now I have to trimm off the ZEROs at the end before scaling into log scale
        int maxSize = data.length-1;
        while(maxSize >= 0 && data[maxSize]==0) maxSize--;
        double[] histogram  = new double[maxSize+1];
        System.arraycopy(data, 0, histogram, 0, maxSize+1);
        maxDegree = maxSize;
        return histogram;
        }
        
    public String[] loglogScaleDegreeCCDFLabels()
        {
        return MiniHistogram.makeBucketLabels(maxDegree+1, 0, maxDegree, false);
        }
    }
