/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.network.stats.actorcentrality;
import sim.field.network.stats.*;
import sim.field.network.*;
import sim.util.mantissa.linalg.*;
import java.util.*;
/**
 * Actor Information Centrality (Wasserman and Faust, page 195 for undirected and page 201 for
 * directed graphs).
 * 
 * <p>Note that this works for both dichotomous and valued relations (i.e. hops and weights).
 * 
 * <p>It requires a matrix inversion that should be reused by all <code>getMeasure()</code> calls.
 * 
 * @author Gabriel Catalin Balan
 */
public class InformationCentrality extends NodeIndex 
    {
    final double normalizationDenominator;
    final double[] ci;
    public InformationCentrality(final Network network, final EdgeMetric weightFn)
        {
        super(network);
        int n = network.allNodes.numObjs;

        //imagine I move all isolated nodes at the end, 
        //so the non isolated nodes are compact, so I can go ahead with the matrix inversion.
        //but when I compute the final stats I want the old order. 
        int[] indirect = new int[n];
        for(int i=0;i<n;i++)
            indirect[i]=i;
        int isolated = 0;
        Iterator nodeIO = network.indexOutInHash.values().iterator();
        for(int k=0;k<n;k++)
            {
            Network.IndexOutIn ioi = (Network.IndexOutIn)nodeIO.next();
            int index= ioi.index;
            if(ioi.out == null)
                {       
                isolated++;
                indirect[n-isolated] = indirect[index];
                indirect[index] = n-isolated;
                }
            }
                
        int nonIsolatedN = n -isolated;
        GeneralSquareMatrix a = new GeneralSquareMatrix(nonIsolatedN);
        //pseudo-adjacency matrix formed by replacing the diagonal of 1-X
        // with one plus each actor's degree
        for(int i=0;i<nonIsolatedN;i++)
            for(int j=0;j<nonIsolatedN;j++)
                a.setElement(i, j, 1);
        //TODO too many functioncalls
                                
        nodeIO = network.indexOutInHash.values().iterator();
        for(int k=0;k<n;k++)
            {
            Network.IndexOutIn ioi = (Network.IndexOutIn)nodeIO.next();
            int index= ioi.index;
            if(ioi.out == null) continue;
            int outDegree = ioi.out.numObjs;
            int indirectIndex = indirect[index];
            Object nodeObj =  network.allNodes.objs[ioi.index];
            for(int i=0;i<outDegree;i++)
                {
                Edge e = (Edge)ioi.out.objs[i];
                // this is getNodeIndex without the function call
                int j = ((Network.IndexOutIn)network.indexOutInHash.get(e.getOtherNode(nodeObj))).index;
                a.setElement(indirectIndex, indirect[j], 1-weightFn.getWeight(e));
                // multigraphs will have edges ignored here
                }
            //there better not be self loops
            a.setElement(indirectIndex, indirectIndex, 1+outDegree);
            }
        Matrix c;
        double R=0, T=0;
        ci = new double[n];
        //TODO maybe we could find a in-place matrix multiplication procedure
        try
            {
            c = a.getInverse(0.0);
            for(int i=0;i<nonIsolatedN;i++)
                {
                T+=c.getElement(i, i);
                R+=c.getElement(0, i);
                }
            for(int i=0;i<n;i++)
                {
                int indirectI = indirect[i];
                ci[i]= (indirectI>=nonIsolatedN)? Double.POSITIVE_INFINITY : c.getElement(indirectI, indirectI);
                //I want the ci values of the isolated nodes to be 0, so 1 /positive infinity does the trick
                }
                        
            }catch(SingularMatrixException ex)
            {
            throw new RuntimeException("Singular Matrix");
            }
        double k = (T-2*R)/nonIsolatedN;
        double sum = 0;
        for(int i=0;i<n;i++)
            {
            ci[i]=1d/(ci[i]+k);
            sum+=ci[i];
            }       
        normalizationDenominator = sum;
        }

        
    public double getValue(Object node) {
        return ci[network.getNodeIndex(node)];
        }
        
    public double getValue(int nodeIndex) {
        return ci[nodeIndex];
        }
        
    /**
     * Sum_{i} getMeasure(i)
     */
    public double getMaxValue(){return normalizationDenominator;}
    }
