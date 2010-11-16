/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.network.stats;
import sim.field.network.*;
import java.util.*;
import sim.util.*;

public class DegreeStatistics 
    {
    /**
       Returns the sum of degrees of all nodes in the network.
    */
    public static int getSumOfDegrees( final Network network )
        {
        return 2*NetworkStatistics.getNumberActualEdges(network);
        }

    /**
       Returns the minimum in degree of nodes in the graph.
    */
    public static int getMinInDegree( final Network network )
        {
        int N = NetworkStatistics.getNumberNodes(network);
        int min = Integer.MAX_VALUE;
        for( int i = 0 ; i < N; i++ )
            {
            Bag temp = network.getEdgesIn( network.allNodes.objs[i] );
            if( min > temp.numObjs )
                min = temp.numObjs;
            }
        return min;
        }

    /**
       Returns the minimum out degree of nodes in the graph.
    */
    public static int getMinOutDegree( final Network network )
        {
        int N = NetworkStatistics.getNumberNodes(network);
        int min = Integer.MAX_VALUE;
        for( int i = 0 ; i < N; i++ )
            {
            Bag temp = network.getEdgesOut( network.allNodes.objs[i] );
            if( min > temp.numObjs )
                min = temp.numObjs;
            }
        return min;
        }

    /**
       Returns the maximum in degree of nodes in the graph.
    */
    public static int getMaxInDegree( final Network network )
        {
        int N = NetworkStatistics.getNumberNodes(network);
        int max = Integer.MIN_VALUE;
        for( int i = 0 ; i < N; i++ )
            {
            Bag temp = network.getEdgesIn( network.allNodes.objs[i] );
            if( max < temp.numObjs )
                max = temp.numObjs;
            }
        return max;
        }

    /**
       Returns the maximum out degree of nodes in the graph.
    */
    public static int getMaxOutDegree( final Network network )
        {
        int N = NetworkStatistics.getNumberNodes(network);
        int max = Integer.MIN_VALUE;
        for( int i = 0 ; i < N; i++ )
            {
            Bag temp = network.getEdgesOut( network.allNodes.objs[i] );
            if( max < temp.numObjs )
                max = temp.numObjs;
            }
        return max;
        }

    /**
       Returns the mean in degree of nodes in the graph.
    */
    public static double getMeanInDegree( final Network network )
        {
        final int groupSize = NetworkStatistics.getNumberNodes(network);
        if( groupSize == 0 )
            return 0;
        return (double)NetworkStatistics.getNumberActualEdges(network)/(double)groupSize;
        }

    /**
       Returns the mean out degree of nodes in the graph.
    */
    public static double getMeanOutDegree( final Network network )
        {
        final int groupSize = NetworkStatistics.getNumberNodes(network);
        if( groupSize == 0 )
            return 0;
        return (double)NetworkStatistics.getNumberActualEdges(network)/(double)groupSize;
        }

    /**
       Returns the variance of the in degree of nodes in the graph.
    */
    public static double getVarInDegree( final Network network )
        {
        /*
         * m=Sum(x)/n
         * sigma^2 =                    = Sum[(x-m)^2]/(n-1)    
         *                                              = [Sum(x^2)-2mSum(x)+Sum(m^2)]/(n-1)
         *                                              = [Sum(x^2)-2nm^2+n m^2]/(n-1)
         *                                              = [Sum(x^2)-(S^2)/n]/(n-1). 
         *                                              = [Sum(x^2)n-S^2]/[n(n-1)]
         */
        int sumSq=0;
        int sum = NetworkStatistics.getNumberActualEdges(network);
        int N = NetworkStatistics.getNumberNodes(network);
        for( int i = 0 ; i < N; i++ )
            {
            Bag temp = network.getEdgesIn( network.allNodes.objs[i] );
            final int inD = temp.numObjs;
            sumSq += inD*inD;
            }
        return (double)(sumSq*N-sum*sum)/(N*(N-1)); //I hope hotspot will reuse N-1 ;)
        }

    /**
       Returns the variance of the out degree of nodes in the graph.
    */
    public static double getVarOutDegree( final Network network )
        {
        /*
         * m=Sum(x)/n
         * sigma^2 =                    = Sum[(x-m)^2]/(n-1)    
         *                                              = [Sum(x^2)-2mSum(x)+Sum(m^2)]/(n-1)
         *                                              = [Sum(x^2)-2nm^2+n m^2]/(n-1)
         *                                              = [Sum(x^2)-(S^2)/n]/(n-1). 
         *                                              = [Sum(x^2)n-S^2]/[n(n-1)]
         */
        int sumSq=0;
        int sum=NetworkStatistics.getNumberActualEdges(network);
        int N = NetworkStatistics.getNumberNodes(network);
        for( int i = 0 ; i < N; i++ )
            {
            Bag temp = network.getEdgesOut( network.allNodes.objs[i] );
            final int outD = temp.numObjs;
            sumSq += outD*outD;
            }
        return (double)(sumSq*N-sum*sum)/(N*(N-1)); //I hope hotspot will reuse N-1 ;)
        }

    //If one needs both the in and out degree histographs, 
    //I could avoid iterating through indexOutInHash twice
    //by computing them in the same loop. 
    //TODO I could receive 2 IntBags for in and out degrees
    //(one can be null if I should not care about it)
    //Otherwise, the int[] might get extended for multigraphs,
    //so the pointers would be useless.
    static public int[] getDegreeHistogram(final Network network, boolean out)
        {
        int n = network.allNodes.numObjs;
        int[] histogram = new int[n];
        Iterator i = network.indexOutInHash.values().iterator();
        for(int k=0;k<n;k++)
            {
            Network.IndexOutIn ioi= (Network.IndexOutIn)i.next();
            Bag b = out?ioi.out:ioi.in;
            int degree = (b==null)?0:b.numObjs;
            if(degree>=histogram.length)//for multigraphs
                {
                int[]   newhistogram  = new int[degree+1];
                System.arraycopy(histogram, 0, newhistogram, 0, histogram.length);
                histogram = newhistogram;
                }
            histogram[degree]++;
            }
        return histogram;
        }
        
    /**
     * CCDF = complementary cummulative distribution function
     */
    static public double[] getDegreeCCDF(final Network network, boolean out)
        {
        int[] histogram = getDegreeHistogram(network, out);
        int len = histogram.length;
        double[] ccdf = new double[len];
        for(int i=1;i<len;i++)
            histogram[i]+= histogram[i-1];
        int sum = histogram[len-1];
        for(int i=0;i<len;i++)
            ccdf[i]= 1d- ((double)histogram[i])/sum;
        return ccdf;
        }
    }
