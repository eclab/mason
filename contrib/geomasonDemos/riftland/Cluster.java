/*
 * Cluster.java
 *
 * $Id: Cluster.java 2029 2013-09-04 19:49:57Z escott8 $
 */
package riftland;

import ec.util.MersenneTwisterFast;
import riftland.riftlandData.RiftLandData;

import java.io.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * XXX What does this class do and why? Docs, docs. -- Siggy
 * 
 * The landscape is divided into a number of weather areas called clusters. A
 * cluster is defined as a group of weather grid elements that have similar
 * (i.e. correlated) weather effects, and the elements in a cluster tend to be
 * contiguous. The weather in these clusters can be affected on an annual basis
 * by changing the weather multiplier for that cluster and year. A multiplier of
 * 1.0 provides historical data (or whatever is stored in the weather data
 * files). Values less than 1 will reduce the annual rainfall in that cluster,
 * and greater than 1 will increase it.
 * 
 * @author chennacotla
 */
public class Cluster //implements Steppable
{
    /** Do we want to control weather at cluster level or if we want automatic
     * precipitation generation?
     */
    private boolean clusterFlag = true;  // true=read ClusterControl file, false=generate randomly
    private double[][] clusterMultipliers; //Multipliers at cluster level
    private int numClusters;
    private int numYears;
    private double[] yearlyClusterMultipliers;
    private int[][] clusterMap;
    final private Parameters params;



    public Cluster(Parameters params, boolean x, final String dataPath, MersenneTwisterFast rng)
    {
        this.params = params;
        
        // set numClusters and numYears from parameter file
        readParams(dataPath);

        clusterMultipliers = new double[numClusters][numYears];
        yearlyClusterMultipliers = new double[numClusters];
        clusterMap = new int[54][56];

        readClusterInfo(dataPath);

        setClusterFlag(x);
        setClusterMultipliers(dataPath, rng);
    }


    

    public double[] getYearlyClusterMultipliers(int year)
    {
        for (int i = 0; i < numClusters; i++)
        {

            yearlyClusterMultipliers[i] = clusterMultipliers[i][year];
        }

        return yearlyClusterMultipliers;
    }



    private void setClusterMultipliers(final String datapath, MersenneTwisterFast randgen)
    {
        if (clusterFlag == false)
        {
            for (int k = 0; k < numClusters; k++)
            {
                for (int j = 0; j < numYears; j++)
                {
                    clusterMultipliers[k][j] = 1 + randgen.nextGaussian() * 0.1;
                }

            }

        } else
        {
            String clusterControlFile = datapath + params.world.getWeatherClusterControlFile();

            try
            {

                BufferedReader clusterControl;
                if (datapath.equals(""))
                {
                    clusterControl = new BufferedReader(new InputStreamReader(RiftLandData.class.getResourceAsStream(params.world.getDatapath() + params.world.getWeatherClusterControlFile())));
                }
                else
                {
                    clusterControl = new BufferedReader(new InputStreamReader(RiftLandData.class.getResourceAsStream(clusterControlFile)));
                }

                String line;
                String[] tokens;

                line = clusterControl.readLine();
                tokens = line.split("\\s+");
                numYears = (int) Double.parseDouble(tokens[1]);
                //System.out.println(numYears);
                line = clusterControl.readLine();
                line = clusterControl.readLine();


                for (int year = 0; year < numYears; year++)
                {
                    line = clusterControl.readLine();
                    //System.out.println(line);
                    tokens = line.split("\\s+");

                    for (int cluster = 0; cluster < numClusters; cluster++)
                    {
                        clusterMultipliers[cluster][year] = Double.parseDouble(tokens[cluster+1]);
                    }
                }
                clusterControl.close();

            } catch (IOException ex)
            {
                Logger.getLogger(Weather.class.getName()).log(Level.SEVERE, null, ex);
            }


            //controlled weather according to user's specification
        }

    }



    private void readClusterInfo(final String datapath)
    {
        int weatherGridHeight = 54;
        int weatherGridWidth = 56;

        String clusterFile = datapath + "ClusterData/ClusterInfo.txt";

        try
        {
            BufferedReader clusterInfo;
            if (datapath.equals(""))
            {
                clusterInfo = new BufferedReader(new InputStreamReader(RiftLandData.class.getResourceAsStream(params.world.getDatapath() + "ClusterData/ClusterInfo.txt")));
            }
            else
            {
                clusterInfo = new BufferedReader(new InputStreamReader(RiftLandData.class.getResourceAsStream(clusterFile)));
            }

            String line;
            String[] tokens;

            line = clusterInfo.readLine();
            line = clusterInfo.readLine();
            tokens = line.split("\\s+");
            numClusters = Integer.parseInt(tokens[1]);


            for (int curr_row = 0; curr_row < weatherGridHeight; curr_row++)
            {
                line = clusterInfo.readLine();

                tokens = line.split("\\s+");
                for (int curr_col = 0; curr_col < weatherGridWidth; curr_col++)
                {
                    clusterMap[curr_row][curr_col] = Integer.parseInt(tokens[curr_col]);
                }
            }
            clusterInfo.close();
        } catch (IOException ex)
        {
            Logger.getLogger(Weather.class.getName()).log(Level.SEVERE, null, ex);
        }
    }



    private void readParams(final String datapath)
    {
        String clusterControlFile = datapath + params.world.getWeatherClusterControlFile();

        try
        {
            BufferedReader clusterControl;
            if (datapath.equals(""))
            {
                clusterControl = new BufferedReader(new InputStreamReader(RiftLandData.class.getResourceAsStream(params.world.getDatapath() + params.world.getWeatherClusterControlFile())));
            }
            else
            {
                clusterControl = new BufferedReader(new InputStreamReader(RiftLandData.class.getResourceAsStream(clusterControlFile)));
            }

            String line;
            String[] tokens;

            line = clusterControl.readLine();
            tokens = line.split("\\s+");
            numYears = (int) Double.parseDouble(tokens[1]);
            //System.out.println(numYears);
            line = clusterControl.readLine();
            tokens = line.split("\\s+");
            numClusters = (int) Double.parseDouble(tokens[1]);
            //System.out.println(numClusters);

            clusterControl.close();

        } catch (IOException ex)
        {
            Logger.getLogger(Weather.class.getName()).log(Level.SEVERE, null, ex);
        }

    }



    public int[][] getClusterMap()
    {
        return clusterMap;
    }



    private void setClusterFlag(boolean x)
    {
        // If cluster flag is true, read cluster control info from file,
        // otherwise generate it randomly.
        clusterFlag = x;
    }



    public int getNumYears()
    {
        return numYears;
    }

}
