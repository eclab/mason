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

import ucar.ma2.*;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.IntGrid2D;
import sim.util.media.chart.HistogramGenerator;


// This class is a wrapper to the Ucar class, and aggregates some time steps
// the process
public class UcarSeasons extends ClimateIO
{
    // XXX Should make this a parameter
    private final int fileStepsPerModelStep = 3;  // i.e. months per season

    private Ucar pastUcar, futureUcar;
    int pastOffset, futureOffset;
    
    private DoubleGrid2D currentSeasonGrid;
    private DoubleGrid2D currentMonthGrid;  // To reduce re-allocations

    // I can't return multiple values, so I set this instead
    private Ucar temporaryUcar;
    
    private int pastSeasons, futureSeasons;
    
    private int seasonOverrideStart = -1;
    private int seasonOverrideDuration = -1;
    private IntGrid2D nationGrid;


    /**
     * Open a UCAR data file, and keep it open so that we can read different
     * layers out of it while the simulation proceeds.
     *
     * @param filename
     */
    UcarSeasons(String pastFilename, String futureFilename, DoubleGrid2D latGrid, DoubleGrid2D lonGrid, int pastOffset, int futureOffset)
    {
        this.pastOffset = pastOffset;
        this.futureOffset = futureOffset;
        //System.out.println("pastOffset =" + pastOffset);
        
        pastSeasons = 0;
        if (pastFilename != null && pastFilename.length() != 0)
        {
            pastUcar = new Ucar(pastFilename, latGrid, lonGrid);
            pastSeasons = (pastUcar.getLastLayer() - pastOffset)/ fileStepsPerModelStep;
        }
        //System.out.println("pastSeasons = " + pastSeasons);

        // I'll assume the lat and lon grids are the same.
        futureSeasons = 0;
        if (futureFilename != null && futureFilename.length() != 0)
        {
            futureUcar = new Ucar(futureFilename, latGrid, lonGrid);
            futureSeasons = (futureUcar.getLastLayer() - futureOffset) / fileStepsPerModelStep;
        }
        //System.out.println("futureSeasons = " + futureSeasons);

        currentSeasonGrid = new DoubleGrid2D(latGrid);  // Just to get the dimensions right
        currentMonthGrid = new DoubleGrid2D(currentSeasonGrid);
    }


    /**
     * Close the file.  This isn't really necessary.  Destroying the class should
     * be enough.
     * @throws IOException
     */
    void close() throws IOException
    {
        if (pastUcar != null)
            pastUcar.close();

        if (futureUcar != null)
            futureUcar.close();
    }

    
    public int getStepsPerYear()
    {
        if (pastUcar != null)
            return pastUcar.getStepsPerYear() / fileStepsPerModelStep;
        else
            return futureUcar.getStepsPerYear() / fileStepsPerModelStep;
    }
    
    
    public double getFirstTime()
    {
        if (pastUcar != null)
            return pastUcar.getFirstTime();
        else
            return futureUcar.getFirstTime();
    }
    
    
    public void setSeasonOverride(int seasonOverrideStart, int seasonOverrideDuration)
    {
        this.seasonOverrideStart = seasonOverrideStart;
        this.seasonOverrideDuration = seasonOverrideDuration;
    }
    
    
    public int convertSeasonToFileInfo(int seasonIndex)
    {
        // I can't return multiple things, so I return the ucar object by 
        // setting this.temporaryUcar instead.
        int layerIndex;
        
        // Make sure we don't go past the last season that we have data for
        //seasonIndex = Math.min(seasonIndex, pastSeasons + futureSeasons - 1);
        
        if (seasonIndex < pastSeasons && pastUcar != null)
        {
            this.temporaryUcar = pastUcar;
            layerIndex = seasonIndex*fileStepsPerModelStep + pastOffset;
        }
        else if (futureUcar != null)
        {
            this.temporaryUcar = futureUcar;
            layerIndex = (seasonIndex - pastSeasons) * fileStepsPerModelStep;  // Works if past file exists
            // XXX This may actually be wrong.  Still debugging
            //layerIndex = seasonIndex*fileStepsPerModelStep + futureOffset;  // Works if past file doesn't exist?
        }
        else
        {
            layerIndex = -1;
        }
        
        return layerIndex;
    }
    
    
    public double getTimeForLayer(int seasonIndex)
    {
        int fileLayerIndex = convertSeasonToFileInfo(seasonIndex);
        return this.temporaryUcar.getTimeForLayer(fileLayerIndex);
    }
    
    
    public int getLastLayer()
    {
        // This may be off by 1 or 2.  I'm not sure
        return pastSeasons + futureSeasons;
    }
    
    
    /**
     * Reads in the layer of weather data in the next time-step.
     * @throws IOException
     * @throws InvalidRangeException
     */
    public int loadLayer(int seasonIndex)
    {
        if (seasonOverrideStart >= 0)
            seasonIndex = seasonOverrideStart + seasonIndex % seasonOverrideDuration;
        
        int baseFileLayerIndex = convertSeasonToFileInfo(seasonIndex);

        //System.out.println("Loading season " + seasonIndex);
        //System.out.println("Season begins at " + getTimeForLayer(seasonIndex));
        currentSeasonGrid.setTo(0.0);

        for(int i = 0; i < fileStepsPerModelStep; i++)
        {
            int err = temporaryUcar.loadLayer(baseFileLayerIndex + i);
            if (err < 0)
                return err;
            
            temporaryUcar.setNationGrid(this.nationGrid);
            temporaryUcar.populateDoubleGrid2D(currentMonthGrid);
            currentSeasonGrid.add(currentMonthGrid);
            //currentSeason.setTo(currentMonth);
        }
        currentSeasonGrid.multiply(1.0/fileStepsPerModelStep);
        return 0;
    }


    /**
     * Returns a UCAR index object for given (x,y) coordinates.
     */
//    Index getIndex(int x, int y)
//    {
//        return xy2indexMap[x][y];
//    }


    /**
     * Returns weather data at a specific UCAR Index.
     * Of course, this assumes that the index is properly formed.
     */
//    double getDataInCell(Index index)
//    {
//        return currentLayer.getFloat(index);
//    }


    /**
     * Returns the current layer as a DoubleGrid2D, re-projected to our
     * current projection.
     */
    void populateDoubleGrid2D(DoubleGrid2D grid)
    {
        grid.setTo(currentSeasonGrid);
    }
    
    public void setNationGrid(IntGrid2D grid)
    {
    	this.nationGrid = grid;
    }


}

