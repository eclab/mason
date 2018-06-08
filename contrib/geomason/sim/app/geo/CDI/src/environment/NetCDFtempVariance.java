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

import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import ucar.ma2.*;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.units.TimeUnit;
import sim.field.grid.DoubleGrid2D;


// This class interfaces the NetCDF files that Akiko created containing
// temperature variance
public class NetCDFtempVariance extends ClimateIO
{

    private NetcdfFile ncFile = null;
    private double latBounds[];
    private double lonBounds[];
    private double timeBounds[];
    private Variable dataVar;
    private Array currentLayer;
    private int[] layerOrigin;
    private int[] layerSize;
    private DoubleGrid2D latGrid;
    private DoubleGrid2D lonGrid;
    private int baseYear = 0;
    public double firstTime = 0;


    // It might make sense to put the re-projection stuff in a different class.
    Index[][] xy2indexMap;  // our (x,y) coordinates to NetCDF indices


    /**
     * Open a UCAR data file, and keep it open so that we can read different
     * layers out of it while the simulation proceeds.
     *
     * XXX I really have to rethink exceptions in this method.
     *
     * @param filename
     */
    NetCDFtempVariance(String filename, DoubleGrid2D latGrid, DoubleGrid2D lonGrid)
    {
        //String filename="../../tas_Amon_CCSM4_piControl_r1i1p1_080001-130012.nc";

        this.latGrid = latGrid;
        this.lonGrid = lonGrid;

        // Open the NetCDF file
        try
        {
            System.out.println("Filename: " + filename);
            ncFile = NetcdfFile.open(filename);
        }
        catch (IOException e)
        {
            // Is this really the right place to be catching exceptions?
            // I think it might make more sense to pass them up.
            System.err.println("Error opening netCDF File");
            System.err.printf("filename = %s%n", filename);
            e.printStackTrace();
            System.exit(1);
        }

        try
        {
            loadMetadata();
        }
        catch (IOException e)
        {
            System.err.println("Error loading NetCDF metadata");
            System.exit(1);
        }

        // In order to create indices for the reprojection map,
        // I need to load a layer.
        if (loadLayer(0) < 0)
            System.err.println("Error loading first temperature layer");

        this.xy2indexMap = generateReprojectionMap();
    }


    /**
     * Close the file.  This isn't really necessary.  Destroying the class should
     * be enough.
     * @throws IOException
     */
    void close() throws IOException
    {
        ncFile.close();
    }


    /**
     * Makes sure the dimensions lat, lon and time are there.  Also records
     * the ranges for each lat and lon cell in the class variables latBounds
     * and lonBounds.  Also checks on the variable that stores the real data.
     * @throws IOException
     */
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
/* ###
        Variable timeBoundsVar = ncFile.findVariable("time_bnds");
        if (timeBoundsVar == null)
            timeBoundsVar = ncFile.findVariable("time_bounds");
*/

        assert(latVar != null);
        assert(lonVar != null);
        assert(timeVar != null);
        // ### assert(timeBoundsVar != null);

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
        // ### Array timeBoundsArray = timeBoundsVar.read();
        Array timeBoundsArray = timeVar.read();
        int timeBoundsShape[] = timeBoundsArray.getShape();
        // ### assert(timeBoundsShape[1] == 2);
        assert(timeBoundsShape[1] == 1);
        timeBounds = new double[timeBoundsShape[0]+1];

        ncIndex = timeBoundsArray.getIndex();
        // ### this.firstTime = baseYear + Math.max(0.0, timeBoundsArray.getDouble(ncIndex.set(0, 0)))/365.0;
        this.firstTime = baseYear + Math.max(0.0, timeBoundsArray.getDouble(ncIndex.set(0)))/365.0;
        timeBounds[0] = firstTime;


        // Read the time metadata
        for (int i=1; i < timeBoundsShape[0]; i++)
        {
            // ### double val = timeBoundsArray.getDouble(ncIndex.set(i,1));
            double val = timeBoundsArray.getDouble(ncIndex.set(i));
            
            //timeBounds[i] = startYear + Math.max(0.0, val) / 365.0;
            timeBounds[i] = firstTime + i / (this.getStepsPerYear() * 1.0);
            //System.out.println(timeBounds[i]);
        }

        // For now we'll assume the data is temperature data.  Actually,
        // a lot of things are assumed here (and above). We should probably
        // generalize this at some point.
        dataVar = ncFile.findVariable("variance");
        assert(dataVar != null);

        int[] shape = dataVar.getShape();
        layerOrigin = new int[] {0, 0, 0};
        layerSize = new int[] {1, shape[1], shape[2]};
    }


    /* (non-Javadoc)
     * @see environment.ClimateIO#getStepsPerYear()
     */
    @Override
    public int getStepsPerYear()
    {
        return 12;  // Should get from metadata.
    }
    

    /* (non-Javadoc)
     * @see environment.ClimateIO#getFirstTime()
     */
    @Override
    public double getFirstTime()
    {
        return (this.firstTime);
    }
    
    
    /* (non-Javadoc)
     * @see environment.ClimateIO#getTimeForLayer(int)
     */
    @Override
    public double getTimeForLayer(int layer)
    {
        return (this.timeBounds[layer]);
    }


    /* (non-Javadoc)
     * @see environment.ClimateIO#getLastLayer()
     */
    @Override
    public int getLastLayer()
    {
        return timeBounds.length - 1;
    }


    /* (non-Javadoc)
     * @see environment.ClimateIO#loadLayer(int)
     */
    @Override
    public int loadLayer(int timeIndex)
    {
        int returnVal = timeIndex;
        int offsetTimeIndex = timeIndex;
        
        layerOrigin[0] = offsetTimeIndex;
        System.out.println("Layer to load = " + offsetTimeIndex);

        // Paul's original comment:
        // The data read in from the file is 3-D [time][lat][lon]
        //  we could get cute and apply .reduce() to it directly,
        //  but for clarity, create a new array, read it in then
        //  reduce it to the 2D temperature Array.

        //System.out.println("timeBounds.length =" + timeBounds.length);
        if (timeIndex >= timeBounds.length || timeIndex < 0)
        {
            //System.err.println("The end of the temperature file has been reached.  No more data will be loaded");
            return -1;
        }
        
        Array t3D = null;
        try
        {
            t3D = dataVar.read(layerOrigin, layerSize);
        }
        catch (Exception e)
        {
            //System.err.println("Error loading temperature Layer.");
            //Thread.dumpStack();
            returnVal = -1;
        }
        currentLayer = t3D.reduce();

        return returnVal;
    }


    /**
     * Returns a UCAR index object for given (x,y) coordinates.
     */
    Index getIndex(int x, int y)
    {
        return xy2indexMap[x][y];
    }


    /**
     * Returns weather data at a specific UCAR Index.
     * Of course, this assumes that the index is properly formed.
     */
    double getDataInCell(Index index)
    {
        return currentLayer.getFloat(index);
    }


    /**
     * Returns the current layer as a DoubleGrid2D, re-projected to our
     * current projection.
     */
    void populateDoubleGrid2D(DoubleGrid2D grid)
    {
        System.out.println("populateDoubleGrid2D with variance data");
        int width = xy2indexMap.length;
        int height = xy2indexMap[0].length;
        assert(width == grid.getWidth());
        assert(height == grid.getHeight());

        double prev = 270;
        for (int xi = 0; xi < width; xi++)
        {
            for (int yi = 0; yi < height; yi++)
            {
                double data = getDataInCell(xy2indexMap[xi][yi]);
                //System.out.printf("%5.3f, ", data);
                if (data <= 0.0)
                {
                    System.out.println(data);
                }
                
                // I believe this was put in to fix a problem with missing data in
                // historical temperature data files.
                if (data > 400 || data <= 0.0)
                {
                    //System.out.println(data);
                    data = prev;
                }
                
                grid.set(xi, yi, data);
                prev = data;
            }
            //System.out.println();
        }
    }


    /* (non-Javadoc)
     * @see environment.ClimateIO#generateReprojectionMap()
     */
    public Index[][] generateReprojectionMap()
    {
        int gridWidth = latGrid.getWidth();
        int gridHeight = latGrid.getHeight();
        Index[][] indexGrid = new Index[gridWidth][gridHeight];

        int latInd, lonInd; // Components of the weather data indices
        for (int yi = 0; yi < gridHeight; yi++)
            for (int xi = 0; xi < gridWidth; xi++)
            {
                double lat = latGrid.get(xi,  yi);
                double lon = lonGrid.get(xi,  yi);

                // Get the associated weather grid cell
                // TODO: This can be optimized a lot, and should be.
                for (latInd = 0; latInd < latBounds.length - 2; latInd++)
                    if (withinAngles(lat, latBounds[latInd],
                                     latBounds[latInd + 1]))
                        break;
                assert (latInd < latBounds.length - 1);

                for (lonInd = 0; lonInd < lonBounds.length - 2; lonInd++)
                    if (withinAngles(lon, lonBounds[lonInd],
                                     lonBounds[lonInd + 1]))
                        break;
                assert (lonInd < lonBounds.length - 1);

                // This assumes a layer has already been loaded!
                Index index = currentLayer.getIndex();
                index.set(latInd, lonInd);
                indexGrid[xi][yi] = index;
            }

        return indexGrid;
    }
}

