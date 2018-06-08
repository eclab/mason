package CDI.src.environment;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.spiderland.Psh.booleanStack;
import org.spiderland.Psh.intStack;

import CDI.src.movement.NorthLandsMovement;
import CDI.src.movement.parameters.*;
import sim.engine.Stoppable;
import sim.engine.SimState;
import sim.field.continuous.Continuous2D;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.IntGrid2D;
import sim.field.grid.ObjectGrid2D;
import sim.field.grid.SparseGrid2D;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.Int2D;
import ec.util.MersenneTwisterFast;

//import java.net.URL;
//import sim.util.geo.MasonGeometry;
//import com.vividsolutions.jts.geom.*;
//import sim.field.geo.GeomVectorField;
//import sim.io.geo.ShapeFileImporter;



public class Map
{
    public static final int GRID_CELL_SIZE = 10;  // i.e. 10km or 100km square
    public static final int REGION_WIDTH = 10000;
    public static final int REGION_HEIGHT = REGION_WIDTH;
    public static final int GRID_WIDTH = 772;
    public static final int GRID_HEIGHT = 981;
    public static final int CANADA_CODE = 1;

    // TODO: I should get this from the file instead of hard-coding it.
    //       Same for GRID_WIDTH and GRID_HEIGHT.
    public static final int MISSING = -9999;
    private static final int PEOPLE_PER_BUILDING = 100; // 4;
    
    public static final double ARTIC_CIRCLE_LATITUDE = 66.5622;
    public static final double ARCTIC_CIRCLE_DIAMETER = 259.138959 * 2; // in grid space
    public static final Double2D NORTH_POLE = new Double2D(427, 481);    // determined by finding the cell with latitude closest to 90
    
    BufferedReader reader;
    StringTokenizer tokenizer;
    
    public IntGrid2D nationGrid;
    public IntGrid2D megaCellGrid;  // this host the number for portrayal
    public Continuous2D megaCellSignGrid;
    public HashMap<Integer, MegaCellSign> megaCellTable = new HashMap<Integer,MegaCellSign>();
   

    public IntGrid2D popGrid;
    public ObjectGrid2D cellGrid;
    public ArrayList<Cell> canadaCells;
    IntGrid2D cultureGrid;
    IntGrid2D landCoverGrid;
    public DoubleGrid2D nppGrid;   // Net primary productivity, a bit like NDVI
    public DoubleGrid2D latGrid;   // Maybe these should move to a
    public DoubleGrid2D lonGrid;   // reprojection class
    public DoubleGrid2D coastalGrid;
    public IntGrid2D initPopRegionGrid;  // map the canada cell to region for 1911 numHouseholds initialization
    //Parameters params;
    public Parameters parameters;
    MersenneTwisterFast random;  // This is just a pointer to the same random
                                 // number generator in the SimState class.
    
    //Ucar weatherIO;
    public UcarSeasons weatherIO;
    public NetCDFtempVariance tempVarIO;  // Reads temperature variance data
    public HashMap<Cell, Integer> indexMap;
    
    //migration portrayals
    public DoubleGrid2D tempDes,portDes,riverDes,elevDes;
    DoubleGrid2D tempRawToAdd, tempRawToSubtract, tempRawMovingAverage, tempRawMovingAverageC, tempTemp, tempRaw;
    public DoubleGrid2D tempVariance;
    DoubleGrid2D portRaw, riverRaw, elevRaw;
    public DoubleGrid2D initDesGrid;
    SparseGrid2D cities;
    
    //GeomVectorField provinceBoundaries;
    IntGrid2D provinceGrid;

    
    private double[] tempParams;
    
    public MapPortrayals portrayals;
    

    public Map(Parameters params, MersenneTwisterFast random_)
    {
        nationGrid = new IntGrid2D(GRID_WIDTH, GRID_HEIGHT);
        provinceGrid = new IntGrid2D(GRID_WIDTH, GRID_HEIGHT);
        megaCellGrid = new IntGrid2D(GRID_WIDTH, GRID_HEIGHT);
        megaCellSignGrid = new Continuous2D(1.0, GRID_WIDTH, GRID_HEIGHT);
        popGrid = new IntGrid2D(GRID_WIDTH, GRID_HEIGHT);
        cultureGrid = new IntGrid2D(GRID_WIDTH, GRID_HEIGHT);
        landCoverGrid = new IntGrid2D(GRID_WIDTH, GRID_HEIGHT);
        nppGrid = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT);
        tempRawToAdd = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT);  // The most recent temperatures
        tempRaw = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT);  // The most recent temperatures
        tempRawToSubtract = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT);  // Remove from moving average
        tempRawMovingAverage = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT);  // Averaged temps over some period
        tempRawMovingAverageC = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT);  // Averaged temps over some period in Celsius
        tempTemp = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT);  // Used to temporarily store temperature data
        tempDes = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT);  // derived from tempRawMovingAverage
        tempVariance = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT);
        latGrid = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT);
        lonGrid = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT);
        portRaw = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT);
        portDes = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT);
        riverRaw = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT);
        riverDes = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT);
        elevRaw = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT);
        elevDes = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT);
        initDesGrid = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT);
        cellGrid = new ObjectGrid2D(GRID_WIDTH, GRID_HEIGHT);
        coastalGrid = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT);
        initPopRegionGrid = new IntGrid2D(GRID_WIDTH, GRID_HEIGHT);
        canadaCells = new ArrayList<Cell>(91777);    // how many Canadian cells there are
        
        cities = new SparseGrid2D(GRID_WIDTH, GRID_HEIGHT);
        
        indexMap = new HashMap<Cell, Integer>();
        
        random = random_;
        this.parameters = params;
        
        portrayals = new MapPortrayals(this);

        try {
            populateGrid(parameters.nationsFilename, nationGrid);
            populateGrid(parameters.provincesFilename, provinceGrid);
            populateGrid(parameters.populationFilename, popGrid);  // The 2005 "emperical" numHouseholds
            populateGrid(parameters.latFilename, latGrid);
            populateGrid(parameters.lonFilename, lonGrid);
            
            nationGrid.replaceAll(MISSING, 0);
            nationGrid.replaceAll(124, CANADA_CODE);  // Canada
            
            coastalGrid.replaceAll(MISSING, 0);
            
            remapCountryCodes(nationGrid);
            
            
            //weatherIO = new Ucar(parameters.temperatureFilename, latGrid, lonGrid);
            weatherIO = new UcarSeasons(parameters.histTempFilename, parameters.projTempFilename, latGrid, lonGrid, parameters.histTempFileOffset, parameters.projTempFileOffset);
            weatherIO.setNationGrid(this.nationGrid);
            if(parameters.seasonOverrideStart >= 0 && parameters.seasonOverrideDuration >= 0)
                weatherIO.setSeasonOverride(parameters.seasonOverrideStart, parameters.seasonOverrideDuration);
            calcInitialTempMovingAverage(parameters);

/*
            //tempVarIO = new NetCDFtempVariance("src/CDI/datatempData/historical_temp.nc", latGrid, lonGrid);
            tempVarIO = new NetCDFtempVariance("src/CDI/datatempData/rcp85_temp.nc", latGrid, lonGrid);
            tempVarIO.loadLayer(0);
            tempVarIO.populateDoubleGrid2D(tempVariance);
            for (int i = 0; i < 15; i++)
            {
                double data = tempVariance.get(i, 0);
                System.out.printf("%5.3f, ", data);
            }
            System.out.println();
*/

            populateGrid(parameters.portRawFile, portRaw);
            populateGrid(parameters.portDesFile, portDes);
            populateGrid(parameters.riverRawFile, riverRaw);
            populateGrid(parameters.riverDesFile, riverDes);
            populateGrid(parameters.elevRawFile, elevRaw);
            populateGrid(parameters.elevDesFile, elevDes);
            populateGrid(parameters.cultureGroupFile, cultureGrid);
            populateGrid(parameters.landCoverFile, landCoverGrid);
            populateGrid(parameters.nppFile, nppGrid);
            populateGrid(parameters.coastalFile, coastalGrid);
            populateGrid(parameters.popRegionFile, initPopRegionGrid);
            
        } catch (Exception e) {
            e.printStackTrace();
            //return -1;
        }
        
        
        
        //compute the zscore for raw data
        zscoreValue(riverDes, riverRaw);
        zscoreValue(elevDes, elevRaw);
        zscoreValue(portDes, portRaw);
        calcDistanceFromIdeal(tempRawMovingAverage, tempTemp, parameters.idealTemperature);
        tempParams = zscoreValue(tempDes, tempTemp);  // XXX Mean will be lost, so need to record the mean and sigma here
        
        tempDes.replaceAll(MISSING, Double.NEGATIVE_INFINITY);    // TODO: We should find a better way
        portDes.replaceAll(MISSING, Double.NEGATIVE_INFINITY);
        riverDes.replaceAll(MISSING, Double.NEGATIVE_INFINITY);
        elevDes.replaceAll(MISSING, Double.NEGATIVE_INFINITY);
        
        int waterCount = initializeDesirabilityGrid();
        
        initializeMegaCellGrid();

        System.out.format("CanadaCells: %d, water cells: %d\n", canadaCells.size(), waterCount);
//      findNearestPointToLatitude(90.0); // north pole
//      findNearestPointToLatitude(ARTIC_CIRCLE_LATITUDE);
        
        //generateProvinces();

    }

/*
    void generateProvinces()
    {
        provinceBoundaries = new GeomVectorField(GRID_WIDTH, GRID_HEIGHT);

        try {
            // Code for generating the provinceGrid using a shapefile
            System.out.println("Loading shape file");
            File shapefile = new File("src/CDI/datagpr_000a11a_e.shp");
            URL shapefileURL = shapefile.toURL();
            ShapeFileImporter.read(shapefileURL, provinceBoundaries);
            PrecisionModel pm = new PrecisionModel();
            GeometryFactory fac = new GeometryFactory(pm);
            Double2D lonLat;
            Coordinate coord;
            Point p;
            Bag allProvinces = provinceBoundaries.getGeometries();
            Bag inProvince;
            Object theProvince;
            int prov;

            // Loop through all the cells
            System.out.println(canadaCells.size());
            for (Cell c : canadaCells) 
            {
                // Convert to lat, lon
                lonLat = getLonLat(c.x, c.y);
                coord = new Coordinate(lonLat.x, lonLat.y);
                p = fac.createPoint(coord);

                // Check which province(s) the cell is in
                inProvince = provinceBoundaries.getCoveringObjects(p);
                if ( inProvince.isEmpty() )
                {
                    System.out.println("Not in a province.  Finding nearest.");
                    double distance = 0.05;
                    Bag nearbyProvinces = provinceBoundaries.getObjectsWithinDistance(p,distance);
                    while (nearbyProvinces.isEmpty())
                    {
                        distance = distance + 0.05;
                        System.out.println("Setting to " + distance);
                        nearbyProvinces = provinceBoundaries.getObjectsWithinDistance(p,distance);
                    }
                    if (nearbyProvinces.size() > 1)
                        System.out.println("Multiple nearby provinces.  Picking one.");
 
                    theProvince = nearbyProvinces.get(0);
                }
                else
                {
                    if ( inProvince.size() > 1 )
                        System.out.println("Multiple provinces found.  Picking one");

                    theProvince = inProvince.get(0);
                }

                // Find a number for the province
                if(theProvince == null)
                    prov = 0;
                else
                {
                    prov = 1;
                    MasonGeometry geom;
                    for(Object o: allProvinces)
                    {
                        if (o == theProvince)
                            break;
                        prov += 1;
                    }
                }

                provinceGrid.set(c.x, c.y, prov);
                //System.out.println(prov);
            }

            PrintWriter writer = new PrintWriter("Provinces.txt", "UTF-8");
            writer.println("ncols         772");
            writer.println("nrows         981");
            writer.println("xllcorner     -4275739.6953");
            writer.println("yllcorner     -4991292.3216");
            writer.println("cellsize      10000");
            writer.println("NODATA_value  -9999");
            for(int y = 0; y < provinceGrid.getHeight(); y++)
            {
                for(int x = 0; x < provinceGrid.getWidth(); x++)
                {
                    writer.print(provinceGrid.get(x,y) + " ");
                }
                writer.println("");
            }
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
            //return -1;
        }
    }
*/


    public int initializeDesirabilityGrid()
    {
        double tWeight = parameters.initTempCoeff;
        double pWeight = parameters.initPortCoeff;
        double rWeight = parameters.initRiverCoeff;
        double eWeight = parameters.initElevCoeff;

        int waterCount = 0;

        for(int y = 0; y < GRID_HEIGHT; y++) {
            for(int x = 0; x < GRID_WIDTH; x++) {
                if (nationGrid.get(x, y)==CANADA_CODE) {
                    // exclude cells with land cover of 0 (water), unless people really live there
                    if ((nppGrid.field[x][y] < 0) && (popGrid.field[x][y] <= 0)) {
                        waterCount++;
                        continue;
                    }
                    double totDes = calculateInitialDesirability(x, y, tWeight, pWeight, rWeight, eWeight);
                    if (Double.isInfinite(totDes) || Double.isNaN(totDes))
                        continue;
                    initDesGrid.set(x, y, totDes);
                    Cell c = new Cell(x, y, nationGrid.get(x, y), provinceGrid.get(x,y), tempDes.get(x, y), portDes.get(x, y), elevDes.get(x, y), riverDes.get(x, y), totDes, popGrid.get(x, y));
                    cellGrid.set(x, y, c);
                    canadaCells.add(c);
                    this.indexMap.put(c,canadaCells.size()-1);
                }
                else {
                    initDesGrid.set(x, y, Double.NEGATIVE_INFINITY);
                }
            }
        }
        return(waterCount);
    }


    public void updateDesirabilityGrid()
    {
        double tWeight = parameters.initTempCoeff;
        double pWeight = parameters.initPortCoeff;
        double rWeight = parameters.initRiverCoeff;
        double eWeight = parameters.initElevCoeff;

        for(Cell c : canadaCells)
        {
            double totDes = calculateInitialDesirability(c.x, c.y, tWeight, pWeight, rWeight, eWeight);
            c.initDes = totDes;
            //c.tempDes = 0;
            //c.portDes = 0;
            //c.elevDes = 0;
            //c.riverDes = 0;
            initDesGrid.set(c.x, c.y, totDes);
        }
    }

    public void putMegaCellSign() {
        
    }
    
    
    private void initializeMegaCellGrid() {

    	int megaSize = 30;
    	int height = this.megaCellGrid.getHeight();
    	int width = this.megaCellGrid.getWidth();
    	int counter = 0;
    	
    	
    	int row = 0;
    	
    	// iterate through the cells, with the jump of the megacell
    	for(int i = 0;i<height;i+=megaSize) {
    		for(int j = 0;j<width;j+=megaSize) {
    			
    			boolean canadaCellInvolved = false;
    			
    			// start to fill one block
    			for(int ii = 0;ii<megaSize;++ii) {
    				for(int jj = 0;jj<megaSize;++jj) {
    					if(ii+i<height&&jj+j<width) {
    						
    						//System.out.println("height is "+(ii+i)+", width is "+(jj+j));
    						if(row%2==0)
    							megaCellGrid.field[jj+j][ii+i] = counter%2;
    						else
    							megaCellGrid.field[jj+j][ii+i] = (counter - 1)%2;
    						
    						
    						// cell only in canada area
    						Cell cell = (Cell) cellGrid.get(jj+j, ii+i);
    						if(cell!=null)
    						{
    							cell.megaCellId = counter;
    							canadaCellInvolved = true;
    						}
    					}
    				}
    			}
    			
    			if(canadaCellInvolved) 
    			{
    				MegaCellSign sign = new MegaCellSign(counter, new Double2D(j+megaSize/2,i+megaSize/2));
    				this.megaCellTable.put(counter, sign);
    				this.megaCellSignGrid.setObjectLocation(sign, new Double2D(j+megaSize/2,i+megaSize/2));
    				
    			}
    			// increase the counter after fill the block
    			counter++;
    		}
    		
    		row++;
    	}
		
	}



    public void calcDistanceFromIdeal(DoubleGrid2D srcGrid, DoubleGrid2D destGrid, double ideal)
    {
        //System.out.println("calcDistanceFromIdeal");
        if(destGrid != srcGrid)
            destGrid.setTo(srcGrid);
        
        int width = srcGrid.getWidth();
        int height = srcGrid.getHeight();
        double val = 0.0;
        
        for(int x = 0; x < width; x++)
            for(int y = 0; y < height; y++)
            {
                val = srcGrid.get(x, y);
                //System.out.println(val);
                val = -Math.abs(ideal - val);
                destGrid.set(x, y, val);
            }
    }
    
    
    public void initializeTemperature()
    {
        try {
            calcInitialTempMovingAverage(parameters);
            calcDistanceFromIdeal(tempRawMovingAverage, tempTemp, parameters.idealTemperature);
            tempParams = zscoreValue(tempDes, tempTemp);  // XXX Mean will be lost, so need to record the mean and sigma here
            tempDes.replaceAll(MISSING, Double.NEGATIVE_INFINITY);    // TODO: We should find a better way

/*
            int err = tempVarIO.loadLayer(0);
            tempVarIO.populateDoubleGrid2D(tempVariance);
            for (int i = 0; i < 15; i++)
            {
                double data = tempVariance.get(i, 0);
                System.out.printf("%5.3f, ", data);
            }
            System.out.println();
*/
        }
        catch (Exception e) {
            e.printStackTrace();
        }  
    }
    
    
    private double[] zscoreValue(DoubleGrid2D desGrid, DoubleGrid2D srcGrid) {

        double sum = 0.0;
        double sumOfSquares = 0.0;
        int counter = 0; 
        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                if (nationGrid.get(x, y) == CANADA_CODE) {
                    double val = srcGrid.field[x][y];
                    sum += val;
                    sumOfSquares += val * val;
                    counter++;
                }
            }
        }

        double mean = sum / counter;
        double sd = Math.sqrt(sumOfSquares / counter - mean * mean);

        
        
        
        double[] ret = new double[2];
        ret[0] = mean;
        ret[1] = sd;

        for (int y = 0; y < GRID_HEIGHT; y++) 
        {
            for (int x = 0; x < GRID_WIDTH; x++) 
            {
                if (nationGrid.get(x, y) == CANADA_CODE) 
                    desGrid.field[x][y] = (srcGrid.field[x][y] - mean) / sd;
                else 
                    desGrid.field[x][y] = Double.NEGATIVE_INFINITY;
            }
        }
        return ret;
    }

    private Int2D findNearestPointToLatitude(double latitude) 
    {
        double minDistance = Double.MAX_VALUE;
        int nearestX = -1, nearestY = -1;

        for (int y = 0; y < GRID_HEIGHT; y++) 
        {
            for (int x = 0; x < GRID_WIDTH; x++) 
            {
                double distance = Math.abs(latGrid.field[x][y] - latitude);
                if (distance < minDistance) 
                {
                    minDistance = distance;
                    nearestX = x;
                    nearestY = y;
                }
            }
        }

        double centerX = 427;
        double centerY = 481;
        Int2D point = new Int2D(nearestX, nearestY);
        double gridDistance = point.distance(centerX, centerY);

        System.out.format("Nearest point to latitude %f is %f at (%d,%d), at a grid distance of %f from the center (%.0f,%.0f)\n",
                latitude, latGrid.field[nearestX][nearestY], nearestX, nearestY,
                gridDistance, centerX, centerY);

        return new Int2D(nearestX, nearestY);
    }
    
    public int countMatchingCells(IntGrid2D grid, int value) 
    {
        int count = 0;
        int h = grid.getHeight();
        int w = grid.getWidth();
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (grid.field[x][y] == value)
                    count++;
        return count;
    }
    
    public void updateTotalDesirability(Parameters params) 
    {
        double totDes;
        double maxTotal = Double.NEGATIVE_INFINITY;
        double minTotal = Double.POSITIVE_INFINITY;
        for (Cell c : canadaCells) 
        {
            totDes = calculateInitialDesirability(c.x, c.y, params.initTempCoeff, params.initPortCoeff, params.initRiverCoeff, params.initElevCoeff);
            //initDesGrid.set(c.x, c.y, totDes);
            c.initDes = totDes;
            
            if(totDes > maxTotal)
                maxTotal = totDes;
            if(totDes < minTotal)
                minTotal = totDes;
        }
        
        System.out.println("the max total is "+maxTotal);
        System.out.println("the min total is "+minTotal);
    }
    
    private double[] getCanadianSubset(DoubleGrid2D grid) 
    {
        double[] data = new double[canadaCells.size()];
        int index = 0;
        for (Cell c : canadaCells)
            data[index++] = grid.field[c.x][c.y];
        
        return data;
    }
    
    public double[] getTotalDesData() 
    {
        return getCanadianSubset(initDesGrid);
    }
    
    public double[] getTempDesData() 
    {
        return getCanadianSubset(tempDes);
    }
    
    public double[] getTempVarData() 
    {
/*
        int i;
        // Print out some of the tempVariance grid
        for (i = 0; i < 15; i++)
        {
            double data = tempVariance.get(i, 0);
            System.out.printf("%5.3f, ", data);
        }
        System.out.println();
*/
        
        double[] varData = getCanadianSubset(tempVariance);

/*
        // Print out the varData
        //for(double data : varData)
        for (i = 0; i < 15; i++)
        {
            double data = varData[i];
            System.out.printf("%5.3f, ", data);
        }
        System.out.println();
*/
        return(varData);
    }

    public double[] getTemperatureData() 
    {
//        tempRawMovingAverageC.setTo(0.0);
        tempRawMovingAverageC.setTo(tempRawMovingAverage);  // get copy
        return getCanadianSubset(tempRawMovingAverageC.add( -273.15));  // adjust this copy   vs tempTemp
    }
    
    public double[] getRiverDesData() 
    {
        return getCanadianSubset(riverDes);
    }
    
    public double[] getPortDesData() 
    {
        return getCanadianSubset(portDes);
    }
    
    public double[] getElevDesData() 
    {
        return getCanadianSubset(elevDes);
    }
    
    public int[] getPopGridData() 
    {
        int[] data = new int[canadaCells.size()];
        int index = 0;
        for (Cell c : canadaCells)
            data[index++] = popGrid.field[c.x][c.y];
        
        return data;
    }
    
    public int[] getPopGridData(int minVal) 
    {
        ArrayList<Integer> sizes = new ArrayList<Integer>();
        
        for (Cell cell : canadaCells) 
                {
            if (cell.empPop > 0)
                if (cell.empPop >= minVal)
                    sizes.add(popGrid.field[cell.x][cell.y]);
        }
        
        int[] a = new int[sizes.size()];
        for (int i = 0; i < a.length; i++)
            a[i] = sizes.get(i);
        
        return a;
    }
    
    public double[] getDoublePopGridData() 
    {
        double[] data = new double[canadaCells.size()];
        int index = 0;
        for (Cell c : canadaCells)
            data[index++] = popGrid.field[c.x][c.y];
        
        return data;
    }
    
    public double[] getDoublePopGridData(double minVal) 
    {
        ArrayList<Integer> sizes = new ArrayList<Integer>();
        
        for (Cell cell : canadaCells) 
                {
            if (cell.empPop > 0)
                if (cell.empPop >= minVal)
                    sizes.add(popGrid.field[cell.x][cell.y]);
        }
        
        double[] a = new double[sizes.size()];
        for (int i = 0; i < a.length; i++)
            a[i] = sizes.get(i);
        
        return a;
    }
    
        
    public IntGrid2D getPopulationGrid() { return popGrid; }
    public DoubleGrid2D getTempDes() { return tempDes ; }
    public DoubleGrid2D getElevDes() { return elevDes ; }
    public DoubleGrid2D getRiverDes() { return riverDes ; }
    public DoubleGrid2D getPortDes() { return portDes ; }
    
    /**
     * Function to calculate the total desirability of a cell based on the individual desirability 
     * scores for temperature, port-distance, river-distance, and elevation. This is currently 
     * calculated using a linear combination but could be calculated in a different functional form.
     * 
     * @param x x-coordinate
     * @param y y-coordinate
     * @param tWeight temperature weight
     * @param pWeight port-distance weight
     * @param rWeight river-distance weight
     * @param eWeight elevation weight
     * @return The total desirability at the given coordinates
     */
    public double calculateInitialDesirability(int x, int y, double tWeight, double pWeight, double rWeight, double eWeight) {
        return  tWeight * tempDes.field[x][y] +
                pWeight * portDes.field[x][y] +
                rWeight * riverDes.field[x][y] +
                eWeight * elevDes.field[x][y];
    }




    public void calcInitialTempMovingAverage(Parameters parameters)
    { 
        final int movingAverageSize = parameters.tempRunnningAvgWindow;
        
        tempRawMovingAverage.setTo(0.0);

        int err;
        for (int i = 1 - movingAverageSize; i < 1; i++)
        {
            // Try to load negative index layers.  These may or may not exist.
            // If not, we'll load a positive layer and then not update the average
            // until we pass all the positive layers we loaded.
            err = weatherIO.loadLayer(i);
            if (err < 0)
            {
                err = weatherIO.loadLayer(i + movingAverageSize);  // I'll just assume that this works
                if (err < 0)
                {
                    
                }
            }
            
            weatherIO.populateDoubleGrid2D(tempRawToAdd);
            
            if(i==0)
                tempRaw.setTo(tempRawToAdd);
            
            tempRawMovingAverage = tempRawMovingAverage.add(tempRawToAdd);
        }
        tempRawMovingAverage = tempRawMovingAverage.multiply(1.0/movingAverageSize);            
    }


    public int updateTemperatures(NorthLandsMovement model, int seasonIndex)
    {
        //System.out.println(seasonIndex);
        final int movingAverageSize = model.getTempRunnningAvgWindow();
        final int seasonsPerYear = weatherIO.getStepsPerYear();
        final int seasonToRemove = seasonIndex - movingAverageSize;
        int err;

        // If there's no previous data, use the initial moving average until we
        // actually move beyond the initial window.
        //System.out.println("Updating average");
        //System.out.println("seasonToRemove: " + seasonToRemove);

        // Load and prepare data from a previous timestep (current - movingAverageSize)
        err = weatherIO.loadLayer(seasonToRemove);
        //System.out.println("err = " + err);
        if (err < 0)
            weatherIO.loadLayer(seasonIndex);   // I'll assume this worked
        weatherIO.populateDoubleGrid2D(tempRawToSubtract);
        
        tempRawToSubtract = adjustVariance(tempRawToSubtract, model.getStdevTempAdjust() * Math.max(seasonToRemove, 0.0) / seasonsPerYear);
        if (this.parameters.tempAdjustStart<=model.schedule.getTime()) {tempRawToSubtract = tempRawToSubtract.add(model.getMeanTempAdjust() * Math.max(seasonToRemove, 0.0) / seasonsPerYear);}
        tempRawToSubtract = tempRawToSubtract.multiply(-1.0/movingAverageSize);  // x/N to calculate mean, - to subtract

        //System.out.println("seasonToAdd: " + seasonIndex);
        // Load and prepare data from the current timestep
        err = weatherIO.loadLayer(seasonIndex);
        if (err < 0)
        {
            System.err.println("No more temperature data to load.");
            model.schedule.clear();

            return -1;
        }
        weatherIO.populateDoubleGrid2D(tempRawToAdd);
        tempRaw.setTo(tempRawToAdd);
        tempRawToAdd = adjustVariance(tempRawToAdd, model.getStdevTempAdjust() * seasonIndex / seasonsPerYear);
        if (this.parameters.tempAdjustStart<=model.schedule.getTime()) {tempRawToAdd = tempRawToAdd.add(model.getMeanTempAdjust() * seasonIndex / seasonsPerYear);}
        tempRawToAdd = tempRawToAdd.multiply(1.0/movingAverageSize);  // x/N to calculate mean


        // Add current data and subtract previous data (i.e. moving average)
        tempRawMovingAverage = tempRawMovingAverage.add(tempRawToAdd);
        tempRawMovingAverage = tempRawMovingAverage.add(tempRawToSubtract);

        // Standardize the data so that it can be combined with other desirability parameters
        calcDistanceFromIdeal(tempRawMovingAverage, tempTemp, parameters.idealTemperature);
        updateWithMiuAndSigma(tempDes, tempTemp, tempParams);     
//System.out.println("map> raw temp @365,900: "+ tempRaw.get(365,900));
        // Load and prepare variance data from file
/*
        err = tempVarIO.loadLayer(seasonIndex);
        
        //System.out.println("err = " + err);
        if (err < 0)
        {
            tempVarIO.loadLayer(varLayer);   // I'll assume this worked
            System.err.println("Error loading temperature variance data");
            System.exit(-1);
        }
        double minVar = 0.0;  // We'd better not see any values below this
        double maxVar = 1e+6; // We can set this higher if we need to
        double defaultVar = 1.0;  // We only use this if we see missing data before real data
        //tempVarIO.populateDoubleGrid2D(tempVariance, minVar, maxVar, defaultVar);
        tempVarIO.populateDoubleGrid2D(tempVariance);
*/
/*
        for (int i = 0; i < 15; i++)
        {
            double data = tempVariance.get(i, 0);
            System.out.printf("%5.3f, ", data);
        }
        System.out.println();
*/
/*
        try
        {
            //Thread.sleep(1000);
            TimeUnit.SECONDS.sleep(5);
        }
        catch(Exception exp)
        {
        }
        System.out.println();
        System.out.println();
        for (int x = 0; x < tempVariance.getWidth(); x++)
        {
            for (int y = 0; y < tempVariance.getWidth(); y++)
            {
                System.out.printf("%5.3f, ", tempVariance.get(x, y));
            }
            System.out.println();
        }
*/
        return 0;
    }


    
    private DoubleGrid2D adjustVariance(DoubleGrid2D srcGrid,
            double stdevTempAdjust) {
        double sum = 0.0;
        int counter = 0;
        for(int y = 0; y < GRID_HEIGHT; y++) {
            for(int x = 0; x < GRID_WIDTH; x++) {
                if (nationGrid.get(x, y)==CANADA_CODE) {
                    double val = srcGrid.field[x][y];
                    sum += val;
                    counter++;
                }
            }
        }
        
        double mean = sum / counter;
    
  
        for(int y = 0; y < GRID_HEIGHT; y++) {
            for(int x = 0; x < GRID_WIDTH; x++) {
                if (nationGrid.get(x, y)==CANADA_CODE) {
                    srcGrid.field[x][y] = (srcGrid.field[x][y] - mean) * stdevTempAdjust + mean;
                }
            }
        }
        return srcGrid;
    }

    private void updateWithMiuAndSigma(DoubleGrid2D desGrid,
            DoubleGrid2D srcGrid, double[] params) {
        //System.out.println("update temperature grid");
        //System.out.println(params[0]);
        //System.out.println(params[1]);
        
        double mean = params[0];
        double sd = params[1];
        
        for(int y = 0; y < GRID_HEIGHT; y++) {
            for(int x = 0; x < GRID_WIDTH; x++) {
                if (nationGrid.get(x, y)==CANADA_CODE) {
                    desGrid.field[x][y] = (srcGrid.field[x][y] - mean) / sd;
                }
                else {
                    desGrid.field[x][y] = Double.NEGATIVE_INFINITY;
                }
            }
        }        
    }

    public DoubleGrid2D loadDoubleGrid2D(String filename)
    {
        DoubleGrid2D grid = null;

        try
        {
            //FileInputStream fileIn = new FileInputStream(filename);
            InputStream fileIn = getClass().getClassLoader().getResourceAsStream(filename);
            ObjectInputStream objIn = new ObjectInputStream(fileIn);
            grid = (DoubleGrid2D) objIn.readObject();
            objIn.close();
            fileIn.close();
        }
        catch(IOException i)
        {
            System.out.println("Error reading file: " + filename);
            //i.printStackTrace();
        }
        catch(ClassNotFoundException c)
        {
            System.out.println("Grid2D class not found");
            c.printStackTrace();
        }
      
        return grid;
    }

    /**
     * Reads and ArcGIS grid file full of integers
     */
    public void populateGrid(String filename, IntGrid2D grid) throws Exception {
//        InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        System.err.println("trying to find file " + filename);
        FileInputStream is = new FileInputStream(filename);
        if (is == null)
        {
            System.out.println("can't find filename = >" + filename + "<");
            System.out.println("with root = >" + System.getProperty("user.dir") + "<");
            System.out.println("and classpath = >" + System.getProperty("java.class.path") + "<");
        }
        else // continue
        {
            InputStreamReader isr = new InputStreamReader(is);
            reader = new BufferedReader(isr);

            for(int i=0;i<6;i++)
                reader.readLine();

            for(int i=0;i<grid.getHeight();i++) 
            {
                tokenizer = new StringTokenizer(reader.readLine());
                for(int j=0;j<grid.getWidth();j++) 
                {
                    grid.set(j, i, (int)Double.parseDouble(tokenizer.nextToken()));
                }
            }
        }
    }

    /**
     * Reads and ArcGIS grid file full of doubles
     */
    public void populateGrid(String filename, DoubleGrid2D grid) throws Exception {
//        InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        FileInputStream is = new FileInputStream(filename);
        
        if (is == null)
        {
            System.out.println("can't find filename = >" + filename + "<");
            System.out.println("with root = >" + System.getProperty("user.dir") + "<");
        }
        else // continue
        {
            InputStreamReader isr = new InputStreamReader(is);
            reader = new BufferedReader(isr);

            for(int i=0;i<6;i++)
                reader.readLine();
            
            for(int i=0;i<grid.getHeight();i++) 
            {
                tokenizer = new StringTokenizer(reader.readLine());
                for(int j=0;j<grid.getWidth();j++) 
                {
                    grid.set(j, i, Double.parseDouble(tokenizer.nextToken()));
                }
            }
        }
    }
    
    public void remapCountryCodes(IntGrid2D grid) {
        boolean [] bools = new boolean [900];   // Country codes go into the 800s
        for(int i=0;i<grid.getHeight();i++) {
            for(int j=0;j<grid.getWidth();j++) {
//                bools[grid.get(j, i)]= true;
                int val = grid.get(j, i);
                if (val < 0)
                {
                    System.out.println("j = " + j);
                    System.out.println("i = " + i);
                    System.out.println("val = " + val);
                }
                bools[val] = true;
            }
        }
        int count = 0;
        int [] numbers = new int[bools.length];
        for(int i=0;i<bools.length;i++) 
            if(bools[i]) {
                numbers[i] = count;
                count++;
            }
        
        for(int i=0;i<grid.getHeight();i++) {
            for(int j=0;j<grid.getWidth();j++) {
                grid.set(j,i,numbers[grid.get(j, i)]);
            }
        }
    }
      
    public IntGrid2D getNationGrid() {
        return nationGrid;
    }

    public Double2D getLonLat(int x, int y)
    {
        return new Double2D(lonGrid.get(x,y), latGrid.get(x,y));
    }
    
    public double getTemperature(int x, int y)
    {
        return tempRawMovingAverage.get(x,y);
    }
    
    public Bag getCities() {
        return cities.getAllObjects();
    }

    
}


