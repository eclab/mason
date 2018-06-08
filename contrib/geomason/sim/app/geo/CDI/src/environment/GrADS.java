/**
 * This class is designed to allow a relatively simple interface to the UCAR
 * tools jar library for reading weather data.  Where possible, this class will
 * return data in a model-friendly form (i.e. DoubleGrid2D, or whatever).
 */
package CDI.src.environment;

//import java.awt.Color;
//import java.io.FileInputStream;
//import java.io.ObjectInputStream;
//import java.io.BufferedReader;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.StringTokenizer;
//import ec.util.MersenneTwisterFast;

//import java.io.*;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import sim.field.grid.DoubleGrid2D;

// This class interfaces reads data from the GrADS gridded data format
public class GrADS extends ClimateIO
{
    private InputStream fileStream;
    private BufferedInputStream bufStream;
    private DataInputStream dataStream;

    private double latBounds[];
    private double lonBounds[];
    private double timeBounds[];
    private DoubleGrid2D latGrid;
    private DoubleGrid2D lonGrid;
    private int baseYear = 0;
    public double firstTime = 0;
    
    private DoubleGrid2D currentLayer;

    public class GeoCoord
    {
        int lat;
        int lon;
    }


    /**
     * Open a GrADS data file, and keep it open so that we can read different
     * layers out of it while the simulation proceeds.
     *
     * @param filename
     */
    GrADS(String filename, DoubleGrid2D latGrid, DoubleGrid2D lonGrid)
    {
        //String filename="../../tas_Amon_CCSM4_piControl_r1i1p1_080001-130012.nc";

        this.latGrid = latGrid;
        this.lonGrid = lonGrid;
        this.currentLayer = new DoubleGrid2D(latGrid.getWidth(), latGrid.getHeight(), 0.0);

        // Open the GrADS file
        try
        {
            //System.out.println("Filename: " + filename);
            fileStream = new FileInputStream(filename);
            bufStream = new BufferedInputStream(fileStream);
            dataStream = new DataInputStream(bufStream);

        }
        catch (IOException e)
        {
            // Is this really the right place to be catching exceptions?
            // I think it might make more sense to pass them up.
            System.err.println("Error opening GrADS File");
            System.err.printf("filename = %s%n", filename);
            e.printStackTrace();
            System.exit(1);
        }

        initHardcodedMetadata();

/*
        // In order to create indices for the reprojection map,
        // I need to load a layer.
        if (loadLayer(0) < 0)
            System.err.println("Error loading first temperature layer");

        this.xy2indexMap = generateReprojectionMap();
*/
    }


    /**
     * Close the file.  This isn't really necessary.  Destroying the class should
     * be enough.
     * @throws IOException
     */
    void close() throws IOException
    {
        fileStream.close();
        bufStream.close();
    }


    /**
     * Makes sure the dimensions lat, lon and time are there.  Also records
     * the ranges for each lat and lon cell in the class variables latBounds
     * and lonBounds.  Also checks on the variable that stores the real data.
     * @throws IOException
     */
/*
    private void loadMetadata() throws IOException
    {
        Dimension latDim = ncFile.findDimension("lat");
        Dimension lonDim = ncFile.findDimension("lon");
        Dimension timeDim = ncFile.findDimension("time");
        assert(latDim != null);
        assert(lonDim != null);
        assert(timeDim != null);

        Variable latVar = ncFile.findVariable(latDim.getShortName());
        Variable lonVar = ncFile.findVariable(lonDim.getShortName());
        Variable timeVar = ncFile.findVariable(timeDim.getShortName());
        Variable timeBoundsVar = ncFile.findVariable("time_bnds");
        if (timeBoundsVar == null)
            timeBoundsVar = ncFile.findVariable("time_bounds");

        assert(latVar != null);
        assert(lonVar != null);
        assert(timeVar != null);
        assert(timeBoundsVar != null);

        //Variable lonBoundsVar = ncFile.findVariable("lon_bnds");
        //assert(lonBoundsVar != null);

        Index  ncIndex;

        // Get the latitude bounds
        Array latArray = latVar.read();
        int latShape[] = latArray.getShape();
        assert(latShape[1] == 1);
        latBounds = new double[latShape[0]+1];

        ncIndex = latArray.getIndex();
        double firstLat = latArray.getDouble(ncIndex.set(0));
        for (int i=0; i < latShape[0]; i++)
        {
            double val = latArray.getDouble(ncIndex.set(i));
            latBounds[i] = val;
            //System.out.println("Val = " + val);
        }
        // XXX I may need to wrap around here if we circle the earth.

        // Get the longitude bounds
        Array lonArray = lonVar.read();
        int lonShape[] = lonArray.getShape();
        assert(lonShape[1] == 1);
        lonBounds = new double[lonShape[0]+1];

        ncIndex = lonArray.getIndex();
        double firstLon = lonArray.getDouble(ncIndex.set(0));
        for (int i=0; i < lonShape[0]; i++)
        {
            double val = lonArray.getDouble(ncIndex.set(i));
            lonBounds[i] = val;
        }
        // XXX I may need to wrap around here if we circle the earth.

        // Figure out the year to offset from
        Pattern pattern = Pattern.compile("\\d{4}");  // 4 digits is the year
        Matcher matcher = pattern.matcher(timeVar.getUnitsString());
        if (matcher.find())
            this.baseYear = Integer.parseInt(matcher.group(0));

        // Get the time bounds
        Array timeBoundsArray = timeBoundsVar.read();
        int timeBoundsShape[] = timeBoundsArray.getShape();
        assert(timeBoundsShape[1] == 2);
        timeBounds = new double[timeBoundsShape[0]+1];

        ncIndex = timeBoundsArray.getIndex();
        this.firstTime = baseYear + Math.max(0.0, timeBoundsArray.getDouble(ncIndex.set(0, 0)))/365.0;
        timeBounds[0] = firstTime;


        // Read the time metadata
        for (int i=1; i < timeBoundsShape[0]; i++)
        {
            double val = timeBoundsArray.getDouble(ncIndex.set(i,1));
            
            //timeBounds[i] = startYear + Math.max(0.0, val) / 365.0;
            timeBounds[i] = firstTime + i / (this.getStepsPerYear() * 1.0);
            //System.out.println(timeBounds[i]);
        }

        // For now we'll assume the data is temperature data.  Actually,
        // a lot of things are assumed here (and above). We should probably
        // generalize this at some point.
        dataVar = ncFile.findVariable("tas");
        if (dataVar == null)
            dataVar = ncFile.findVariable("TSA");
        if (dataVar == null)
            dataVar = ncFile.findVariable("TSOI");
        assert(dataVar != null);

        int[] shape = dataVar.getShape();
        layerOrigin = new int[] {0, 0, 0};
        layerSize = new int[] {1, shape[1], shape[2]};
    }
*/
    
    // Setup info about how many layer there are, etc.
    // GrADS use an additional descriptor file to describe metadata.
    // In theory, we should get this info from there.
    private void initHardcodedMetadata()
    {
        int numLatBins = 192;
        int numLonBins = 288;
        int startYear = 1885;
        int numMonths = 1452;
        
        timeBounds = new double[numMonths+1];
        latBounds = new double[numLatBins+1];
        lonBounds = new double[numLonBins+1];
        
        int i;
        for(i=0; i < timeBounds.length; i++)
            timeBounds[i] = startYear + i/12.0;
        
        for(i=0; i < latBounds.length; i++)
        {
            latBounds[i] = -90.0 + (i*180.0)/numLatBins;
            //latBounds[i] = adjustAngle(-90.0 + (i*180.0)/numLatBins);  // XXX This could cause some real problems
            //System.out.println("latBounds["+i+"] = " + latBounds[i]);
        }
        
        for(i=0; i < lonBounds.length; i++)
            lonBounds[i] = (i*360.0)/numLonBins;
    }


    public int getStepsPerYear()
    {
        return 12;  // Should get from metadata.
    }
    

    public double getFirstTime()
    {
        return (this.firstTime);
    }
    
    
    public double getTimeForLayer(int layer)
    {
        return (this.timeBounds[layer]);
    }


    public int getLastLayer()
    {
        return timeBounds.length - 1;
    }


    /**
     * Reads in the layer of weather data in the next time-step.
     * @throws IOException
     * @throws InvalidRangeException
     */
    public int loadLayer(int timeIndex)
    {
        int returnVal = timeIndex;
        int offsetTimeIndex = timeIndex;

        // For now I'm just going to load the next layer, whatever it is
        // XXX I should implement some sort of seek to load the right layer
        
        //System.out.println("timeBounds.length =" + timeBounds.length);
        if (timeIndex >= timeBounds.length || timeIndex < 0)
        {
            //System.err.println("The end of the temperature file has been reached.  No more data will be loaded");
            return -1;
        }
        
        int dataWidth = 288;
        int dataHeight = 192;
        int numVars = 4;
        int loadVar = 0;
        
        // There are 4 variables.  For now, just read in 4 grids
        // each time, and throw out the first 3.
        for(int var = 0; var < numVars; var++)
        {
            for(int latInd = 0; latInd < dataHeight; latInd++)
            {
                for(int lonInd = 0; lonInd < dataWidth; lonInd++)
                {
                    double val = 0.0;
                    try
                    {
                        val = dataStream.readDouble();
                        //currentLayer = null;
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                        System.exit(1);
                    }
                    
                    if(var == loadVar)
                    {
                        currentLayer.set(lonInd, latInd, val);
                        //System.out.printf("%5.3f, ", val);
                    }
                }
                if(var == loadVar)
                {
                    //System.out.println("");
                    //System.out.print(">>>");
                }
            }
        }

        return returnVal;
    }


    /**
     * Returns the current layer as a DoubleGrid2D, re-projected to our
     * current projection.
     */
    void populateDoubleGrid2D(DoubleGrid2D grid, double minVal, double maxVal, double defaultVal)
    {
        reprojectDoubleGrid2D(currentLayer, grid, latGrid, lonGrid,
                latBounds, lonBounds, minVal, maxVal, defaultVal);
    }

}

