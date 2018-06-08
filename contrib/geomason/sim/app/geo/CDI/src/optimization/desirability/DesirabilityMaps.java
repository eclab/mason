package CDI.src.optimization.desirability ;

/**
 * This is the merged version of Map.java and DesirabilityMaps.java, the legacy
 * codes are put at the end of the class, this class encapsulates everything related
 * to Map. -- khaled
 */

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.net.URL;

import sim.field.grid.AbstractGrid2D;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.IntGrid2D;
import CDI.src.movement.parameters.*;
import CDI.src.environment.Map;
import CDI.src.optimization.util.Filter ;
import CDI.src.optimization.util.GaussianFilter ;

public class DesirabilityMaps
{
    public static boolean isDebug =  true ;
    
    // data file preamble's minimum size
    private static final int MIN_PREAMBLE_SIZE = 78 ;
    // canada bounding box
    public static int START_I = 546 ;
    public static int END_I = 980 ;
    public static int START_J = 210 ;
    public static int END_J = 771 ;

    /**
     * For example the min population in canada is 1 and max is like 513984 
     * so to make a better visualization, the minLevel and maxLevel is clamped 
     * within 100 to 10000 respectively, and this was not done by me. -- khaled
     */
    public static final int POPVISIBILITY_MIN = 100 ;
    public static final int POPVISIBILITY_MAX = 10000 ;
    // best visibility ranges for normalized population data
    // and this will be updated later.
    public static double NORM_POP_VIS_MIN = 0.0 ;
    public static double NORM_POP_VIS_MAX = 1.0 ;

    // other relevant stuffs
    private BufferedReader reader = null ;
    private BufferedWriter writer = null ;
    private StringTokenizer tokenizer = null ;

    // This is the data-files path
    private String path = "files/data-files/";

    private IntGrid2D nationGrid ;
    private IntGrid2D popGrid ;
    private DoubleGrid2D smoothedPopGrid ; // smoothed population grid
    private DoubleGrid2D normalizedPopGrid ; // normalized smoothed population grid
    private DoubleGrid2D zscoreSmoothedPopGrid ; // normalized smoothed population grid
    
    private DoubleGrid2D tempDes ; // temperature desirability, "as is" from Map.java 
    private    DoubleGrid2D elevDes ; // elevation data desirability, "as is" from Map.java
    private    DoubleGrid2D riverDes ; // distance from water source desirability, "as is" from Map.java
    private    DoubleGrid2D portDes ; // distance from port desirability, "as is" from Map.java
    
    // total desirability = w1 * tempDes + w2 * elevDes + w3 * riverDes + w4 * portDes
    // assuming that the wi's are already set. Now we have 2 different kind of totalDes, 
    // namely median and mean coefficient values, these two give a slightly different 
    // results.
    private DoubleGrid2D totalDesMedian ;  
    private DoubleGrid2D totalDesMean ; 

    public DesirabilityMapsPortrayals portrayals ;    

    /**
     * A no-argument constructor, used during
     * the EC desirability experiments, not sure
     * will be used in future.
     */
    public DesirabilityMaps(Map origMap)
    {
        //Thread.dumpStack();
        //System.exit(0);
        if(origMap != null)
        {
            this.popGrid = origMap.getPopulationGrid() ;
            this.nationGrid = origMap.getNationGrid();
            this.tempDes = origMap.getTempDes() ;
            this.elevDes = origMap.getElevDes() ; 
            this.riverDes = origMap.getRiverDes() ;
            this.portDes = origMap.getPortDes() ;
            this.allocateGrids();
            // this.findCanadaBoundingBox(); // now hard coded 
            portrayals = new DesirabilityMapsPortrayals(this);
        }
        else
            System.err.println("DesirabilityMaps.DesirabilityMaps() :" 
                    + " The original Map object is null, something went wrong !!");
        Filter.isDebug = true ;
    }

    /**
     * Allocate memory for the grids
     */
    private void allocateGrids()
    {
        //Thread.dumpStack();
        //System.exit(0);
        smoothedPopGrid = new DoubleGrid2D(Map.GRID_WIDTH, Map.GRID_HEIGHT);
        normalizedPopGrid = new DoubleGrid2D(Map.GRID_WIDTH, Map.GRID_HEIGHT);
        zscoreSmoothedPopGrid = new DoubleGrid2D(Map.GRID_WIDTH, Map.GRID_HEIGHT);
        // now we have 2 different levels of totalDes
        totalDesMedian = new DoubleGrid2D(Map.GRID_WIDTH, Map.GRID_HEIGHT);
        totalDesMean = new DoubleGrid2D(Map.GRID_WIDTH, Map.GRID_HEIGHT);
    }

    private void findCanadaBoundingBox()
    {
        //Thread.dumpStack();
        //System.exit(0);
        int starti = 0 , endi = Map.GRID_HEIGHT; 
        int startj = 0 , endj = Map.GRID_WIDTH;
        for(int i = 0 ; i < Map.GRID_HEIGHT ; i++)
            for(int j = 0 ; j < Map.GRID_WIDTH; j++)
            {
                if(this.nationGrid.field[j][i] == Map.CANADA_CODE)
                {
                    if(i >= starti)
                        starti = i ;
                    if(i < endi)
                        endi = i ;
                    if(j >= startj)
                        startj = j ;
                    if(j < endj)
                        endj = j ;
                }
            }
        
        START_I = endi; END_I = starti; START_J = endj; END_J = startj ;        
        System.err.println("DesirabilityMaps.findCanadaBoundingBox() : ");
        System.err.println("\tSTART_I = " + START_I + " END_I = " + END_I);
        System.err.println("\tSTART_J = " + START_J + " END_J = " + END_J);        
    }

    // Getters
    public IntGrid2D getNationGrid() { return nationGrid ;}
    public IntGrid2D getPopGrid() { return popGrid ;}
    public DoubleGrid2D getNormalizedPopGrid() { return normalizedPopGrid; }
    public DoubleGrid2D getZscoreSmoothedPopGrid() { return zscoreSmoothedPopGrid; }
    public DoubleGrid2D getSmoothedPopGrid() { return smoothedPopGrid ;}
    public DoubleGrid2D getTempDes() { return tempDes ; }
    public DoubleGrid2D getElevDes() { return elevDes ; }
    public DoubleGrid2D getRiverDes() { return riverDes ; }
    public DoubleGrid2D getPortDes() { return portDes ; }
    public DoubleGrid2D getTotalDesMedian() { return totalDesMedian ; }
    public DoubleGrid2D getTotalDesMean() { return totalDesMean ; }

    /**
     * This function does the smoothing and other relevant
     * pre-processing before the actual desirability calculations,
     * everything was used to be merged in the constructors but now
     * they have been separated for cleaner code.
     */
    public void prepareMapsForDesirabilityCalculations(boolean useFile)
    {
        //Thread.dumpStack();
        //System.exit(0);
        if(isDebug)
            System.err.println("DesirabilityMaps.prepareMapsForDesirabilityCalculations()");
        // We need to test with different kernel parameters, ignores Map.MISSING values --
        if(useFile) // if you want to load from an already saved data
            smoothedPopGrid = this.smoothGridWithMapConstraint(popGrid, path, 9, 9, 3, 2);
        else // if you recompute every time
            smoothedPopGrid = this.smoothGridWithMapConstraint(popGrid, 9, 9, 3, 2);
        // Get the normalized population grid, 
        // normalization is done only in canda, ignores Map.MISSING values --
        normalizedPopGrid.field = Filter.normalizeWithMapConstraint(smoothedPopGrid.field, 
                nationGrid.field, Map.CANADA_CODE, 0.0, 1.0, 
                START_J, END_J, START_I, END_I);
        
        // calculate and store zscores on smoothed population
        zscoreSmoothedPopGrid.field = Filter.zscoreWithMapConstraint(smoothedPopGrid.field, 
                nationGrid.field, Map.CANADA_CODE, START_J, END_J, START_I, END_I);
        
        // now convert the POPVISIBILITY_MIN and POPVISIBILITY_MAX to 
        // corresponding [0.0,1.0] range, ignores Map.MISSING values -- 
        double[] origRange = Filter.getValueRangeWithMapConstraint(castToDoubleField(popGrid), 
                                    nationGrid.field,
                                    Map.CANADA_CODE, 
                                    START_J, END_J, START_I, END_I);
        double[] normRange = Filter.getValueRangeWithMapConstraint(normalizedPopGrid.field, 
                                    nationGrid.field, 
                                    Map.CANADA_CODE,
                                    START_J, END_J, START_I, END_I);
        NORM_POP_VIS_MIN = normRange[0] + (POPVISIBILITY_MIN / (origRange[1] - origRange[0])) * 
                                    (normRange[1] - normRange[0]);
        NORM_POP_VIS_MAX = normRange[0] + (POPVISIBILITY_MAX / (origRange[1] - origRange[0])) * 
                                    (normRange[1] - normRange[0]);    
        if(isDebug)
        {
            System.err.println("DesirabilityMaps.prepareMapsForDesirabilityCalculations() : ");
            System.err.println("\tOriginal pop-range: " + origRange[0] + " ~ " + origRange[1]);
            System.err.println("\tNormalized pop-range: " + normRange[0] + " ~ " + normRange[1]);
            System.err.println("\tNORM_POP_VIS_MIN: " + NORM_POP_VIS_MIN 
                    + "\n\tNORM_POP_VIS_MAX: " + NORM_POP_VIS_MAX);
        }
    }

    /** 
     * Cast the field of an IntGrid2D to 2D double array.
     */
    private double[][] castToDoubleField(IntGrid2D grid)
    {
        //Thread.dumpStack();
        //System.exit(0);
        double[][] dest = new double[grid.field.length][grid.field[0].length] ;
        for (int i = 0 ; i < grid.field.length ; i++) 
            for(int j = 0 ; j < grid.field[0].length ; j++)
                dest[i][j] = grid.field[i][j] ;
        return dest ;
    }

    /**
     * Apply smoothing on the grid if the smoothed population grid is already in a file,
     * load from it directly, otherwise recalculate.
     */
    private DoubleGrid2D smoothGridWithMapConstraint(AbstractGrid2D srcGrid, String path, 
            int kwidth, int kheight, double sigma, int pass)
    {
        //Thread.dumpStack();
        //System.exit(0);
        if(isDebug)System.err.println("DesirabilityMaps.smoothGridWithMapConstraint()");
        DoubleGrid2D destGrid = new DoubleGrid2D(Map.GRID_WIDTH, Map.GRID_HEIGHT);
        String smoothedPopFileName = "pop-smooth-" + kwidth + "-" + kheight + "-" 
                        + sigma + "-" + pass + ".txt" ;
        System.out.println("Pop Filename = " + smoothedPopFileName);
        try {
            URL url = getClass().getClassLoader().getResource(path);
            String dirPath = url.toURI().getPath();
            File smoothedPopFile = new File(dirPath + smoothedPopFileName);
            // if the smoothed data already exists
            if(smoothedPopFile.exists() && smoothedPopFile.length() > MIN_PREAMBLE_SIZE)
            {
                // load it
                if(isDebug)System.err.println(
                        "\tDesirabilityMaps.smoothGridWithMapConstraint() :" 
                        + " file found \'" 
                        + smoothedPopFile.toString() + "\', loading ...");
                this.loadGridValuesFromStream(getClass().getClassLoader().
                        getResourceAsStream(path + smoothedPopFileName), 
                        destGrid);
            }
            else
            {
                if(isDebug)System.err.println(
                        "\tDesirabilityMaps.smoothGridWithMapConstraint() :" 
                        + " file not found at \'" 
                        + dirPath + "\', recalculating ...");
                smoothedPopFile.createNewFile();
                // recalculate and save
                GaussianFilter gfilter = new GaussianFilter(kwidth, kheight, sigma);
                if(srcGrid instanceof DoubleGrid2D)
                    // destGrid.field = Filter.applyFilterWithMapConstraint(
                    destGrid.field = Filter.applyFastFilterWithMapConstraint(
                            ((DoubleGrid2D)srcGrid).field, gfilter, 
                            nationGrid.field, Map.CANADA_CODE, 
                            pass, START_J, END_J, START_I, END_I);
                else if(srcGrid instanceof IntGrid2D)
                    // destGrid.field = Filter.applyFilterWithMapConstraint(
                    destGrid.field = Filter.applyFastFilterWithMapConstraint(
                            castToDoubleField((IntGrid2D)srcGrid), gfilter, 
                            nationGrid.field, Map.CANADA_CODE, 
                            pass, START_J, END_J, START_I, END_I);
                if(isDebug)System.err.println(
                        "\tDesirabilityMaps.smoothGridWithMapConstraint() :" 
                        + " saving data at \'" 
                        + smoothedPopFile.toString() + "\'.");
                saveGridValuesToFile(smoothedPopFile, destGrid);
            }
        } catch(Exception e) 
            { e.printStackTrace(); }

        return destGrid ;
    }
    
    /**
     * Same as above but it does not involve any data in file
     * rather it computes the smoothed population everytime as invoked.
     */
    private DoubleGrid2D smoothGridWithMapConstraint(AbstractGrid2D srcGrid, 
            int kwidth, int kheight, double sigma, int pass)
    {
        //Thread.dumpStack();
        //System.exit(0);
        if(isDebug)
            System.err.println("DesirabilityMaps.smoothGridWithMapConstraint() : " 
                    + "recomputing ... ");
        DoubleGrid2D destGrid = new DoubleGrid2D(Map.GRID_WIDTH, Map.GRID_HEIGHT);
        // recalculate and save
        GaussianFilter gfilter = new GaussianFilter(kwidth, kheight, sigma);
        if(srcGrid instanceof DoubleGrid2D)
            // destGrid.field = Filter.applyFilterWithMapConstraint(
            destGrid.field = Filter.applyFastFilterWithMapConstraint(
                    ((DoubleGrid2D)srcGrid).field, gfilter, 
                    nationGrid.field, Map.CANADA_CODE, 
                    pass, START_J, END_J, START_I, END_I);
        else if(srcGrid instanceof IntGrid2D)
            // destGrid.field = Filter.applyFilterWithMapConstraint(
            destGrid.field = Filter.applyFastFilterWithMapConstraint(
                    castToDoubleField((IntGrid2D)srcGrid), gfilter, 
                    nationGrid.field, Map.CANADA_CODE, 
                    pass, START_J, END_J, START_I, END_I);
        return destGrid ;
    }
    
    /**
     * Load grid values from an input stream.
     */
    private void loadGridValuesFromStream(InputStream is, 
                        AbstractGrid2D grid) throws Exception
    {
        //Thread.dumpStack();
        //System.exit(0);
        InputStreamReader isr = new InputStreamReader(is);
        reader = new BufferedReader(isr);

        // skip the preamble
        for(int i = 0 ; i < 6 ; i++)
            reader.readLine();

        if(grid instanceof DoubleGrid2D) // if the grid is double
        {
            DoubleGrid2D terrain = (DoubleGrid2D)grid ;
            for(int i = 0; i < terrain.field[0].length; i++)
            {
                tokenizer = new StringTokenizer(reader.readLine());
                for(int j = 0; j < terrain.field.length; j++)
                    terrain.field[j][i] = 
                        Double.parseDouble(tokenizer.nextToken());
            }
        }
        else if(grid instanceof IntGrid2D) // if integer
        {
            IntGrid2D terrain = (IntGrid2D)grid ;
            for(int i = 0; i < terrain.field[0].length; i++)
            {
                tokenizer = new StringTokenizer(reader.readLine());
                for(int j = 0; j < terrain.field.length; j++)
                    terrain.field[j][i] = 
                        (int)Double.parseDouble(tokenizer.nextToken());
            }
        }
        isr.close();
        reader.close();
    }
    
    /** 
     * Save the grid values to file.
     */
    private void saveGridValuesToFile(File file, AbstractGrid2D grid) throws Exception
    {
        //Thread.dumpStack();
        //System.exit(0);
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        writer = new BufferedWriter(fw);
        for(int i = 0 ; i < 6 ; i++)
            writer.write("# some preamble " + i + " that we do not care for now.\n");
        if(grid instanceof IntGrid2D)
        {
            IntGrid2D terrain = (IntGrid2D)grid ;
            for(int i = 0 ; i < terrain.field[0].length; i++)
            {
                for(int j = 0 ; j < terrain.field.length ; j++)
                    writer.write(terrain.field[j][i] + " ");
                writer.write("\n");
                writer.flush();
            }
        }
        else if(grid instanceof DoubleGrid2D)
        {
            DoubleGrid2D terrain = (DoubleGrid2D)grid ;
            for(int i = 0 ; i < terrain.field[0].length; i++)
            {
                for(int j = 0 ; j < terrain.field.length ; j++)
                    writer.write(terrain.field[j][i] + " ");
                writer.write("\n");
                writer.flush();
            }
        }
        writer.close();
        fw.close();
    }
    
    /**
     * Calculates the total desirability, given that 
     * all the cofficients are known/pre-computed.
     */
    private void calculateTotalDesirability(DoubleGrid2D totalDes, double[] coeff)
    {
        //Thread.dumpStack();
        //System.exit(0);
        for(int i = START_I ; i < END_I ; i++)
            for(int j = START_J ; j < END_J ;j++)
            { 
                if(nationGrid.field[j][i] == Map.CANADA_CODE)
                {
                    double temp = tempDes.field[j][i] ;
                    double elev = elevDes.field[j][i] ;
                    double river = riverDes.field[j][i] ;
                    double port = portDes.field[j][i] ;
                    if(temp != Map.MISSING && elev != Map.MISSING 
                        && river != Map.MISSING && port != Map.MISSING)
                        totalDes.field[j][i] = coeff[0] * temp 
                                    + coeff[1] * elev 
                                    + coeff[2] * river 
                                    + coeff[3] * port ;
                }
            }
    }

    /**
     * Calculates the desirability maps coeeficients fitness 
     * with respect to the population distribution, used as a
     * EC evaluation function, it actually calculates an RMS
     * pixel-by-pixel difference.
     */
    public double getCoefficientFitness(Parameters param)
    {
        //Thread.dumpStack();
        //System.exit(0);
        DoubleGrid2D totalDes = new DoubleGrid2D(Map.GRID_WIDTH, Map.GRID_HEIGHT);
        
        double[] coeff = new double[4];
        coeff[0] = param.initTempCoeff;
		coeff[1] = param.initElevCoeff;
		coeff[2] = param.initRiverCoeff;
		coeff[3] = param.initPortCoeff;
        
        this.calculateTotalDesirability(totalDes, coeff);
        // Now this gives better total desirability coefficient maps
        totalDes.field = Filter.normalizeWithMapConstraint(totalDes.field, 
                    nationGrid.field, Map.CANADA_CODE, 0.0, 1.0,
                    START_J, END_J, START_I, END_I);
        double sum = 0.0, diff = 0.0 ;
        int missingTotal = 0;
        for(int i = START_I ; i < END_I ; i++)
            for(int j = START_J ; j < END_J ;j++)
            {
                if(nationGrid.field[j][i] == Map.CANADA_CODE)
                {
                    double tdes = totalDes.field[j][i];
                    //double popval = normalizedPopGrid.field[j][i];
                    double popval = zscoreSmoothedPopGrid.field[j][i];
                    if(popval != Map.MISSING)
                    {
                        diff = popval - tdes ;
                        diff = diff * diff ;
                        sum += diff ;
                    }
                    else
                        ++missingTotal ;
                }
            }
        sum = sum/((Map.GRID_WIDTH * Map.GRID_HEIGHT) - missingTotal);
        // bigger rms value means less resemblance
        // and ecj by default maximizes, so --
        return 1.0/Math.sqrt(sum);
    }
    
    /**
     * Calculates the desirability maps coeeficients fitness 
     * with respect to the population distribution, used as a
     * EC evaluation function, it actually calculates an RMS
     * pixel-by-pixel difference.
     */
    public double getCoefficientFitness(double[] coeff)
    {
        //Thread.dumpStack();
        //System.exit(0);
        double sum = 0.0, diff = 0.0 ;
        int missingTotal = 0;
        DoubleGrid2D totalDes = new DoubleGrid2D(Map.GRID_WIDTH, Map.GRID_HEIGHT);
        this.calculateTotalDesirability(totalDes, coeff);
        // Now this gives better total desirability coefficient maps
        totalDes.field = Filter.normalizeWithMapConstraint(totalDes.field, 
                    nationGrid.field, Map.CANADA_CODE, 0.0, 1.0,
                    START_J, END_J, START_I, END_I);
        for(int i = START_I ; i < END_I ; i++)
            for(int j = START_J ; j < END_J ;j++)
            {
                if(nationGrid.field[j][i] == Map.CANADA_CODE)
                {
                    double tdes = totalDes.field[j][i];
                    //double popval = normalizedPopGrid.field[j][i];
                    double popval = zscoreSmoothedPopGrid.field[j][i];
                    if(popval != Map.MISSING)
                    {
                        diff = popval - tdes ;
                        diff = diff * diff ;
                        sum += diff ;
                    }
                    else
                        ++missingTotal ;
                }
            }
        sum = sum/((Map.GRID_WIDTH * Map.GRID_HEIGHT) - missingTotal);
        // bigger rms value means less resemblance
        // and ecj by default maximizes, so --
        return 1.0/Math.sqrt(sum);
    }

        
    /** All sorts of portrayal stuffs for the MASON gui */

    /**
     * This function is added to separate the map rendering
     * preparation with the actual desirability calculations,
     * such that we can have more speed-ups during thr EC runs;
     * as this function is not called.
     */
    public void prepareMapsForRendering()
    {
        //Thread.dumpStack();
        //System.exit(0);
        if(isDebug)System.err.println("DesirabilityMaps.prepareMapsForRendering()");
        // calculate the total desirability
        // the values are found from a EC run
        
        // median values [-1.0,1.0]
        double[] coeff = new double[] {
            .4059447374108467000, -.657623119894535450,
            .1091409621812686750, -.240725064095216325}; // normalization */
            /*.3345704704884404650, -.054216940319366633,
            -.082690233223019940, .0451374825432452330}; // no-normalization */
        calculateTotalDesirability(totalDesMedian, coeff);
        totalDesMedian.field = Filter.normalizeWithMapConstraint(totalDesMedian.field, 
                                nationGrid.field, Map.CANADA_CODE, 
                                0.0, 1.0,
                                START_J, END_J, START_I, END_I);
        // mean values [-1.0,1.0]
        coeff = new double[] {
            0.393528, -0.481401,
            0.099045, -0.0954752}; // normalization */
            /*0.313709, -0.098199,
            -0.127221, 0.0773088}; // no-normalization */
        calculateTotalDesirability(totalDesMean, coeff);
        totalDesMean.field = Filter.normalizeWithMapConstraint(totalDesMean.field, 
                                nationGrid.field, Map.CANADA_CODE, 
                                0.0, 1.0,
                                START_J, END_J, START_I, END_I);
    }    
}
