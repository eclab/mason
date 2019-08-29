/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.network.stats;
import sim.field.network.*;
import java.util.*;
import sim.util.*;
/**
 * @author Gabriel Catalin Balan
 *
 */
public class DyadTriadStatistics {
    public static final int TRIAD_003 = 0;
    public static final int TRIAD_012 = 1;  
    public static final int TRIAD_102 = 2;
    public static final int TRIAD_021D = 3; 
    public static final int TRIAD_021U = 4;
    public static final int TRIAD_021C = 5;         
    public static final int TRIAD_111D = 6;
    public static final int TRIAD_111U = 7; 
    public static final int TRIAD_030T = 8;
    public static final int TRIAD_030C = 9;
    public static final int TRIAD_201 = 10;
    public static final int TRIAD_120D = 11;
    public static final int TRIAD_120U = 12;
    public static final int TRIAD_120C = 13;
    public static final int TRIAD_210 = 14;
    public static final int TRIAD_300 = 15; 
        
    public static final String[] MAN_TRIAD_CLASSES = 
        {
        "003", "012", "102", "021D", "021U", "021C",
        "111D", "111U", "030T", "030C", "201", "120D",
        "120U", "120C", "210", "300"
        };
    /**
     * Computes the triad census (Wasserman and Faust, pages 564-567). 
     * It assumes a single asymmetric relation where weights are irrelevant.
     * @return 16-length int array
     * 
     */     
    public static int[] triadCensus( final Network network)
        {                       
        int[] census = new int[16];
        for(int i=0;i<16;i++) census[i]=0;
        int n = network.allNodes.numObjs;
        if(n<3)
            return census;// should I through an exception instead? 
        Edge[][] adjMatrix = network.getAdjacencyMatrix();

        //md, ad, nd= mutual, asymmetric and null dyads in the triad (MAN notation)
        //c = edge count in the triad
        //c_ij = edge count in dyad ij
        //m_ij, a_ij, n_ij = {1,0} depending on whether the ij dyad is M, A and N
        //in_i, out_i = in and out degree of node i 
        //e_uv = {1,0} dependinf on whether there is an edge between u and v 
        for(int i=0; i<n;i++)
            {
            Edge[] adj_i = adjMatrix[i]; 
            for(int j=i+1;j<n;j++)
                {
                Edge[] adj_j = adjMatrix[j]; 
                int e_ij=(adj_i[j]==null)?0:1; 
                int e_ji=(adj_j[i]==null)?0:1;
                int c_ij;
                int m_ij=0;
////                            ad and nd are never used. Thy are not completelly deleted 
////                            cause I might need them for debugging                           
////                            int a_ij=0, n_ij=0;
////                            switch (c_ij=e_ij+e_ji)
////                            {
////                                    case 0: n_ij++;break;
////                                    case 1: a_ij++;break;
////                                    case 2: m_ij++;break;
////                            }
                if((c_ij=e_ij+e_ji)==2)
                    m_ij++;

                for(int k=j+1;k<n;k++)
                    {
                    int md=m_ij;
////                                    ad and nd are never used. Thy are not completelly deleted 
////                                    cause I might need them for debugging  
////                                    int ad=a_ij, nd=n_ij,
                    int c=c_ij=e_ij+e_ji;
                    Edge[] adj_k = adjMatrix[k]; 
                    int c_ik, c_jk;
                    int e_ik = (adj_i[k]==null)?0:1;
                    int e_jk = (adj_j[k]==null)?0:1;
                    int e_ki = (adj_k[i]==null)?0:1;
                    int e_kj = (adj_k[j]==null)?0:1;
                                        
////                                    switch (c_ik=e_ik+e_ki)
////                                    {
////                                            case 0: nd++;break;
////                                            case 1: ad++;break;
////                                            case 2: md++;break;
////                                    }
                    if((c_ik=e_ik+e_ki)==2)
                        md++;
                                        
////                                    switch (c_jk=e_jk+e_kj)
////                                    {
////                                            case 0: nd++;break;
////                                            case 1: ad++;break;
////                                            case 2: md++;break;
////                                    }
                    if((c_jk=e_jk+e_kj)==2)
                        md++;
                                                
                    c+=c_ik+c_jk;
                    switch (c)
                        {
                        case 0: census[TRIAD_003]++;break;
                        case 1: census[TRIAD_012]++;break;
                        case 2: if(md==1)
                            {
                            census[TRIAD_102]++;
                            break;
                            }
                            // now I must decide between the three 021s (D, U and C)
                            // f=sum out degree for the unconnected nodes 
                            //f(D) =0, f(U)=2, f(C)=1 
                            //
                            int f = (c_ij==0)?//i and j are "down" in the 021 MAN diagram 
                                e_jk+e_ik:
                                (c_ik==0)?//i and k are "down"
                                e_ij+e_kj:
                                e_ji+e_ki;
                            switch(f)                               
                                {
                                case 0: census[TRIAD_021D]++;break;
                                case 1: census[TRIAD_021C]++;break;
                                case 2: census[TRIAD_021U]++;break;
                                }
                            break;
                        case 3: if(md==1)
                                {
                                //here's how I can tell the 111s apart D has <1,0> and U has <0,1>
                                //(the other nodes are <1,1> and <1,2> in both cases.
                                //
                                //I use the <o,i> notation to say out degree and in-degree of a node. 

                                if(c_ij==2)//k is the discriminating node (either <0,1> or <1,0>)
                                    census[(e_ki+e_ki==0)?TRIAD_111U:TRIAD_111D]++;
                                else if(c_ik==2)//look at j
                                    census[(e_ji+e_jk==0)?TRIAD_111U:TRIAD_111D]++;
                                else//look and i
                                    census[(e_ij+e_ik==0)?TRIAD_111U:TRIAD_111D]++;                                                                 
                                }
                            else                                                            
                                // I must decide between 030T and 030C
                                // 030C is all <1,1>, while 030T has a <0,2> and a <2, 0>
                                // so it;s enough to look at atmost 2 nodes for <1,1>
                                census[(e_ij+e_ik==1 && e_ji+e_ki==1 && e_ji+e_jk==1 && e_ij+e_kj==1)? TRIAD_030C: TRIAD_030T]++;
                            break;
                        case 4: if(md==2)
                                census[TRIAD_201]++;
                            else
                                {
                                //120: D, C or U?
                                // <1,1> => C
                                // <2,0> => D
                                // <0,2> => U
                                // they all got <1,2> and <2,1>
                                int in, out; 
                                                                        
                                out =  e_ij+e_ik; //in of i.
                                in = e_ji+e_ki; //out of i
                                if(in+out==2)
                                    {//i is the node
                                    switch(out)
                                        {
                                        case 0:  census[TRIAD_120U]++; break;
                                        case 1:  census[TRIAD_120C]++; break;
                                        case 2:  census[TRIAD_120D]++; break;
                                        }
                                    break;
                                    }
                                                                        
                                in =  e_ij+e_kj; //in of j.
                                out = e_ji+e_jk; //out of j
                                if(in+out==2)
                                    {//j is the node
                                    switch(out)
                                        {
                                        case 0:  census[TRIAD_120U]++; break;
                                        case 1:  census[TRIAD_120C]++; break;
                                        case 2:  census[TRIAD_120D]++; break;
                                        }
                                    break;
                                    }
                                                                        
                                in =  e_ik+e_jk; //in of k.
                                out = e_ki+e_kj; //out of k
                                if(in+out==2)
                                    {//k is the node
                                    switch(out)
                                        {
                                        case 0:  census[TRIAD_120U]++; break;
                                        case 1:  census[TRIAD_120C]++; break;
                                        case 2:  census[TRIAD_120D]++; break;
                                        }
                                    break;
                                    }
                                }
                            break;
                        case 5: census[TRIAD_210]++;break;
                        case 6: census[TRIAD_300]++;break;
                        }                                                
                    }
                }
            }
        return census;
        }
        
    /**
     * Returns the number of direct triads 
     * (number of parent nodes that point to both nodes sent as parameters).
     * @author Liviu Panait
     */
    public static int getNumberDirectTriads( final Network network, final Object node1, final Object node2 )
        {
        if( !network.isDirected() )
            throw new RuntimeException( "NetworkStatistics.getNumberDirectTriads should be called only with directed graphs" );
        int result = 0;
        HashSet hash = new HashSet();
        final Bag out1 = network.getEdgesIn(node1);
        for( int i = 0 ; i < out1.numObjs ; i++ )
            hash.add( ((Edge)(out1.objs[i])).from() );
        final Bag out2 = network.getEdgesIn(node2);
        for( int i = 0 ; i < out2.numObjs ; i++ )
            if( hash.contains( ((Edge)(out2.objs[i])).from() ) )
                result++;
        return result;
        }
        
    public static final int DYAD_MUTUAL = 0;
    public static final int DYAD_ASYMMETRIC = 1;
    public static final int DYAD_NULL = 2;  
    public static final String[] DYAD_CLASSES = {   "M", "A", "N"   };
        
    /**
     * Computes thw dyad census (Wasserman and Faust, pages 512).
     * It assumes a single asymmetric relation where weights are irrelevant.
     * O(n^2) space and time.
     * I could do O(1) space and n x outdegree^2 time, but that's O(n^3) worst time
     * @return 3-length int array
     */     
    public static int[] dyadCensus( final Network network)
        {                       
        int[] census = new int[3];
        int n = network.allNodes.numObjs;
        Edge[][] adjacencyMatrix = network.getAdjacencyMatrix();
        for(int i=1;i<n;i++)
            {
            Edge[] adj_i = adjacencyMatrix[i];
            for(int j=0;j<i;j++)
                {
                int k=(adjacencyMatrix[j][i]==null? 0:1)+(adj_i[j]==null?0:1);
                census[2-k]++;
                }
            }
        return census;
        }
    }
