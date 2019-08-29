/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.network.stats.actorcentrality;
import sim.field.network.stats.*;
import sim.field.network.*;
import sim.util.mantissa.linalg.*;

/**
 * Bonacich's Power Centrality
 * 
 * <p>C(alpha, beta)=alpha (I - beta R)^-1 R 1
 * <br>where<ul>
 *              <li>R = adjacency matrix (can be valued);</li>
 *              <li>I = identity matrix;</li>
 *              <li>1 = all 1s;</li>
 *              <li>beta = attenuation parameter (a.k.a. decay rate);</li>
 *              <li>alpha = scaling vector, set to normalize the score;</li>
 * </ul>
 * 
 *  <p>"The coefficient alpha acts as a scaling parameter, and is set here (following 
 *   Bonacich (1987)) such that the sum of squared scores is equal to the number of 
 *   vertices. This allows 1 to be used as a reference value for the ``middle'' of
 *   the centrality range" 
 * (<a href="http://pbil.univ-lyon1.fr/library/sna/html/bonpow.html">R documentation</a>).
 * 
 * @author Gabriel Catalin Balan
 */
public class PowerPrestige extends NodeIndex 
    {
    final double[] prestige;
    final double scalingDenominator;
        
    public PowerPrestige(final Network network, EdgeMetric metric){this(network, 1, metric);}
    public PowerPrestige(final Network network,  double beta, EdgeMetric metric)
        {
        super(network);
        int n = network.allNodes.numObjs;
        prestige = new double[n];
        Edge[][] adjM = network.getAdjacencyMatrix();
        //I-betaR
        GeneralSquareMatrix I_bR = new GeneralSquareMatrix(n);
                
        //R1 = is a square matrix with val_ij = Sum_k(R_ik) for all j. 
        //A simple column vector is enough, the rest is redundancy
        double[] Rsum = new double[n];// a column of R1
        for(int i=0;i<n;i++)
            {
            double sum=0;
            Edge[] adjMi = adjM[i];
            for(int j=0;j<n;j++)
                {
                Edge e = adjMi[j];
                double val = (e==null? 0: metric.getWeight(e));
                sum+=val;
                I_bR.setElement(i, j, (i==j?1:0)-beta* val);
                }
            Rsum[i]=sum;
            }
        SquareMatrix I_bR_1;
        try
            {
            I_bR_1 = I_bR.getInverse(0.0);
            //prestige = multiplyByR1(I_bR_1, Rsum)
            double sumsq = 0;//for scaling purposes
            for(int i=0;i<n;i++)
                {
//                              double[] matrix_i = I_bR_1[i];
                double sum = 0;
                for(int k=0;k<n;k++)
                    sum+= I_bR_1.getElement(i,k)*Rsum[k];   
                prestige[i]=sum;
                sumsq += sum*sum;
                }
            scalingDenominator = Math.sqrt(sumsq/n);//remember, this will be used to divide the prestige values
            }catch(SingularMatrixException ex)
            {
            throw new RuntimeException("Singular Matrix");
            }
        }

    public double getValue(final Object node) {
        Network.IndexOutIn inout = (Network.IndexOutIn)network.indexOutInHash.get(node);
        return prestige[inout.index];
        }
        
    public double getValue(final int nodeIndex)
        {
        return prestige[nodeIndex];
        }
        
    public double getMaxValue()
        {
        return scalingDenominator;
        }
    }
