/*
 * Weather.java
 *
 * $Id: Weather.java 2029 2013-09-04 19:49:57Z escott8 $
 *
 */
package sim.app.geo.riftland;

import ec.util.MersenneTwisterFast;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import sim.app.geo.riftland.parcel.Parcel;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.grid.ObjectGrid2D;
import sim.util.Int2D;



/**
 * This version of Weather class integrates spatial variation in Rainfall in Rift Land.
 * It reads available dailyRainfall datafile based on the current month and year and assigns appropriate dailyRainfall
 * information to a given parcel based on its coordinates.
 *
 * @author Chenna-Reddy Cotla
 */
public class Weather implements Steppable
{

    private static final long serialVersionUID = 1L;
    private World world;
    public static final int NO_DATA = 9999;
    public static final double EVAPORATION_RATE = 0.2;
    
    // This weather class reads dailyRainfall data from observed dailyRainfall datafiles which are available for 12 years for each month.

    private int rowDivisor;//following varaible will be used to  calculate mapping from parcel to weather patch.
    private int colDivisor;

    /** The current month; note month in [0,11] not [1,12] */
    private int currentMonth;

    /** the current year */
    private int currRainYear;   // Current year in the rainfall cycle
    private int currMultYear;   // Current year in the cluster multiplier cycle

    private int currentDay = 0;
    private int actualYear = 1998;

    
    private double[][][] weatherData;//This array has mean and SD of monthly dailyRainfall for all patches for each of 12 months
    private double[][] hourlyRainfallThisMonth;
    //first index x-cor of patch, second index y-cor and third index for day of month.Each month is 30days fixed.
    private double[] yearlyClusterMultipliers;
    private double avgMonthlyRainfall;
    private double[][] curMonthRainfall;
    private double[][] prevMonthRainfall;
    private double[][][] rainHist;
    private int[][] clusterMap;
    private boolean plantingFlag;
    final private Parameters params;
    private Cluster cluster;
    
    private ArrayList<Integer> weatherSequence = null;
    private int weatherSequenceIndex = 0;
    
    // Some frequently used constants.  This isn't the best place for these,
    // but it's better than just hardcoding them everywhere.
    final private int monthsPerYear = 12; // so we can tell what each 12 means
    final private int W_height = 54;
    final private int W_width = 56;
    final private int W_years = 12;  // Years of weather data
    final private int W_months = W_years * monthsPerYear;
    
    // Has to be set in the constructor.  Can't make it final.
    final private int M_years;  // Years of cluster control multiplier data

    /**
     * @param widthInParcels width of simulation in parcels
     * @param heightInParcels height of simulation in parcels
     * @param dataPath
     * @param rng
     * @param allLand A grid of Parcels which will have their rainfallPatch
     * attribute set according to their location.
     */
    public Weather(Parameters params, int widthInParcels, int heightInParcels, final String dataPath, MersenneTwisterFast rng, Land land)
    {
        this.params = params;
        world = null;
        currentMonth = 0;
        currRainYear = 0;
        currMultYear = 0;
        plantingFlag = false; //set to "false" because initially there is no rainfall hsitory to determine best future planting month

        parseWeatherSequence(params.world.getWeatherSequence());
        if (weatherSequence != null) {
        	currRainYear = weatherSequence.get(weatherSequenceIndex);
        	weatherSequenceIndex = (weatherSequenceIndex + 1) % weatherSequence.size();
        }

        // TODO Refactor this code to adjust size of arrays to actual subarea
        // that the user selected.
        weatherData = new double[W_months][W_height][W_width];//This array has mean and SD of monthly dailyRainfall for all patches for each of 12 months
        hourlyRainfallThisMonth = new double[W_height][W_width];
        //dailyRainfall = new double[1694][1630]; //This variable has dailyRainfall schedule for current month
//        residualRainfall = new double[1694][1630];
        curMonthRainfall = new double[W_height][W_width];
        prevMonthRainfall = new double[W_height][W_width];
        rainHist = new double[W_height][W_width][monthsPerYear];//this is avg rainschedule for each cell
        
        this.cluster = new Cluster(params, true, dataPath, rng);

        M_years = cluster.getNumYears();
        initializeClusterMap(cluster.getClusterMap());
        setYearlyClusterMultipliers(cluster.getYearlyClusterMultipliers(currMultYear));

        readAllRainfallData(widthInParcels, heightInParcels, dataPath);
        updateMonthlyRainfall();
        //readClusterInfo(); //Future implementation based on cluster information
        //updateRainfallInfo(); //next generation
        assignRainfallPatches(land);

    }
    
    private void parseWeatherSequence(String str) {
    	if ((str == null) || str.isEmpty())
    		return;
    	
    	String[] tokens = str.split(",");
    	
    	if (tokens.length == 0)
    		return;
    	
    	weatherSequence = new ArrayList<Integer>();
    	
    	for (String s : tokens) {
    		weatherSequence.add(Integer.parseInt(s));
    	}
    	
    	String temp = "";
    	for (Integer i : weatherSequence) {
    		temp = temp + i + ", ";
    	}
    	
    	System.out.println("weatherSequence: " + temp);
    	
    }
    
    /**
     * Rainfall data is stored at a granularity of 30x30 parcels -- This  method
     * takes a grid of Parcels and sets their rainfallPatch attribute so it
     * knows which patch it belongs to.
     * 
     * @param allLand 
     */
    private void assignRainfallPatches(Land land)
    {
        for (Object p : land.allLand())
        {
            assert(p instanceof Parcel);
            int x = ((Parcel)p).getX();
            int y = ((Parcel)p).getY();
            Int2D patch = getRainfallPatch(x,y);
            ((Parcel)p).setRainfallPatch(patch.x, patch.y);
        }
    }


    /** Reset the weather for another, new invocation of the model. */
    public void reset()
    {
        currentMonth = 0;
        currRainYear = 0;
        currMultYear = 0;
        currentDay = 0;
        actualYear = 1998;
        plantingFlag = false; //set to "false" because initially there is no rainfall hsitory to determine best future planting month

        Arrays.fill(prevMonthRainfall, 0.0);

        // TODO clear rainHist
        Arrays.fill(rainHist, 0.0);
    }


    @Override
    public void step(SimState state)
    {
        world = (World) state;
        updateDate();
        
        // It's not very efficient to do these every step, but relative to
        // other things going on, it probably doesn't take much time.
        updateMonthlyRainfall();
        updateRainHistory();
        setYearlyClusterMultipliers(cluster.getYearlyClusterMultipliers(currMultYear));
    }

    
    private void updateDate() {
    	if ((currentMonth == 0) && (currentDay == 1))
            System.out.println("Start of year: " + currRainYear );
    	
        switch (currentMonth)
        {
            case 1: // February
                if (currentDay == 28 && (actualYear % 4) != 0)
                {
                    currentDay = 0;
                    currentMonth += 1;

                } else if (currentDay == 29 && actualYear % 4 == 0)
                {
                    currentDay = 0;
                    currentMonth += 1;
                }
                break;
            case 3:  // April
            case 5:  // June
            case 8:  // September
            case 10: // November
                if (currentDay == 30)
                {
                    currentDay = 0;
                    currentMonth += 1;
                }
                break;
            default: // All other months have 31 days
                if (currentDay == 31)
                {
                    if (currentMonth == 11) // December
                    {
                        currentMonth = 0;
                        //System.out.println(world.schedule.getTime() % 365); // This diagnostic checks to be sure that the year is the right length.
                        // currRainYear is the current year in the rain data cycle (e.g. year mod 12)
                        if (weatherSequence != null) {
                        	currRainYear = weatherSequence.get(weatherSequenceIndex);
                        	weatherSequenceIndex = (weatherSequenceIndex + 1) % weatherSequence.size();
                        }
                        else {
                            currRainYear = ((currRainYear + 1) % W_years);
                        }
                        currMultYear = (currMultYear + 1) % M_years;
                        actualYear += 1;
                        plantingFlag = true;
                    } else { //This else was missing until June 2013 -- making years be 31 days too short.
                        currentMonth += 1;
                    }

                    currentDay = 0;
                    
                }
        }
        currentDay++;
    }

    private void updateMonthlyRainfall()
    {
        double totalMonthlyRain = 0;

        for (int i = 0; i < W_height; i++)
        {
            for (int j = 0; j < W_width; j++)
            {
                int cellCluster = clusterMap[i][j]; //Cluster to which the given cell belongs

                //deducting 1 because java arrays start from 0 not from 1. 
                //For example, 2nd cluster would map to 1 in the array.
                hourlyRainfallThisMonth[i][j]
                        = weatherData[(currRainYear * monthsPerYear
                                       + currentMonth) % W_months][i][j]
                          * yearlyClusterMultipliers[cellCluster - 1]; 

                prevMonthRainfall[i][j] = curMonthRainfall[i][j];
                curMonthRainfall[i][j] = hourlyRainfallThisMonth[i][j] * 24 * 30;

                totalMonthlyRain += hourlyRainfallThisMonth[i][j] * 24 * 30;
            }
        }

        avgMonthlyRainfall = (totalMonthlyRain) / (W_height * W_width);
//        System.out.format("===== year: %d, month: %d, avgMonthlyRainfall: %f\n", currentRainYear, currentMonth, avgMonthlyRainfall);
    }

    /**
     * Translate from world coordinates (e.g. 0-1600) to patch coordinates (e.g. 0-55) 
     * @param x
     * @param y
     * @return 
     */
    private Int2D getRainfallPatch(int x, int y)
    {
        assert(x >= 0);
        assert(y >= 0);
        int xPatch = Math.min(W_width - 1, x/colDivisor); // Intentional integer division
        int yPatch = Math.min(W_height - 1, y/rowDivisor); // Intentional integer division
        assert(xPatch < hourlyRainfallThisMonth[0].length);
        assert(yPatch < hourlyRainfallThisMonth.length);
        return new Int2D(xPatch, yPatch);
    }
    
    /*
     * Return the amount of daily rainfall for this month at the given patch.
     */
    public double getDailyRainfall(int patchX, int patchY)
    {
        assert(patchX >= 0);
        assert(patchY >= 0);
        assert(patchX < hourlyRainfallThisMonth[0].length);
        assert(patchY < hourlyRainfallThisMonth.length);
        return hourlyRainfallThisMonth[patchY][patchX] * 24;
    }


    //<editor-fold defaultstate="collapsed" desc="Accessors">

//    public double getResidual(int x, int y)
//    {
//        return residualRainfall[x][y];
//    }
//


    public double[] getYearlyClusterMultipliers()
    {
        return yearlyClusterMultipliers;
    }


    /*
     *
     */
    private void setYearlyClusterMultipliers(double[] multipliers)
    {
        yearlyClusterMultipliers = multipliers;
    }


    /*
     * 
     */
    private void initializeClusterMap(int[][] cmap)
    {
        clusterMap = cmap;
    }


    /*
     * This method generates a rain schedule for a month. This depends upon the mean and variance of dailyRainfall at weather patches.
     */
    public double getAvgMonthlyRainfall()
    {
        return avgMonthlyRainfall;
    }

    // </editor-fold>

    /* This function updates rain History as we are moving in time. */
    private void updateRainHistory()
    {
        for (int i = 0; i < W_height; i++)
        {
            for (int j = 0; j < W_width; j++)
            {
                int k = ((currentMonth - 1) % monthsPerYear < 0)
                          ? (currentMonth - 1) + monthsPerYear
                          : currentMonth - 1;
                rainHist[i][j][k] = prevMonthRainfall[i][j];
            }
        }
    }

    /*
     * This overloaded version of computePlantMonth() handles variable season
     * length. A farmer can basically ask for the next best planting data by
     * passing season lengths he is interested in.
     * 
     * @author Chenna Reddy Cotla
     */
    public int computePlantMonth(int x, int y, int seasonLength)
    {
        if (plantingFlag)
        {   
            Int2D loc = getRainfallPatch(x, y);

            // sumRain will contain cumulative rainfall over 3-month windows
            // starting from each each of 12 months based on rainfall in most
            // recent year.
            double[] sumRain = new double[monthsPerYear]; 
            for (int i = 0; i < monthsPerYear; i++)
            {
                double totalRaininSeason = 0;

                for (int j = 0; j < seasonLength; j++)
                {
                    totalRaininSeason += rainHist[loc.y][loc.x][((i + j) % monthsPerYear)];
                }

                sumRain[i] = totalRaininSeason;
            }

            double maxrain = 0;
            int firstSeason = NO_DATA;

            for (int i = 0; i < monthsPerYear; i++)
            {
                if (sumRain[i] >= maxrain)
                {
                    firstSeason = i;
                    maxrain = sumRain[i];
                }
            }

            maxrain = 0;
            int secondSeason = NO_DATA;
            int f = firstSeason;
            
            for (int i = 0; i < monthsPerYear; i++)
            {

                // this flag indicates is that the current month can be a start
                // of secondbest planting season which has cumulative rainfall
                // less than first season.
                boolean secondSeasonFlag = true; 

                // now we check if the current month can actually be eligible as
                // the start of the second season.
                for (int k = -(seasonLength - 1); k <= (seasonLength - 1); k++)
                {

                    int l = ((f + k) % monthsPerYear < 0)
                                ? ((f + k) % monthsPerYear + monthsPerYear)
                                : (f + k) % monthsPerYear;

                    if (i == l)
                    {
                        // current month is not eligible for being the start of
                        // second season
                        secondSeasonFlag = false; 
                    }

                }

                if (secondSeasonFlag == true)
                {
                    if (sumRain[i] > maxrain)
                    {
                        secondSeason = i;
                        maxrain = sumRain[i];
                    }
                }
            }

            if (sumRain[firstSeason] < world.getParams().farming.getPlantingThreshold())
            {
                firstSeason = NO_DATA;
                secondSeason = NO_DATA;
            } else
            {
                if (secondSeason != NO_DATA && sumRain[secondSeason] < world.getParams().farming.getPlantingThreshold())
                {
                    secondSeason = NO_DATA;
                }
            }

            int rvalue = NO_DATA;

            if (firstSeason != NO_DATA && secondSeason != NO_DATA)
            {
                int temp1 = firstSeason, temp2 = secondSeason;

                if (firstSeason < currentMonth)
                {
                    temp1 = 11 + firstSeason;
                }
                if (secondSeason < currentMonth)
                {
                    temp2 = 11 + secondSeason;
                }

                if (temp1 < temp2)
                {
                    rvalue = firstSeason;
                } else
                {
                    rvalue = secondSeason;
                }


            } else if (firstSeason != NO_DATA && secondSeason == NO_DATA)
            {
                rvalue = firstSeason;
            } else if (firstSeason == NO_DATA && secondSeason != NO_DATA)
            {
                rvalue = secondSeason;
            } else
            {
                rvalue = NO_DATA;
            }

            return rvalue;

        } else
        {
            return NO_DATA;
        }
    }

    final public void readAllRainfallData(int widthInParcels, int heightInParcels, String datapath)
    {
        // these are number of patches along rows and columns in weather file.
        double patchHeight = W_height, patchWidth = W_width;

        for (int month = 1; month <= W_months; month++)
        {
            String rainfallfile = datapath + "RainData/" + month + ".txt";

            try
            {
                BufferedReader rainfall;
                if ("".equals(datapath))
                {
                    rainfall = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(params.world.getDatapath() + "RainData/" + month + ".txt")));
                }
                else
                {
                    rainfall = new BufferedReader(new FileReader(rainfallfile));
                }

                String line;
                String[] tokens;

                for (int i = 0; i < 6; ++i)
                {
                    line = rainfall.readLine();  // assign to line so that we can
                    // peep at it in the debugger
                }


                for (int curr_row = 0; curr_row < patchHeight; ++curr_row)
                {
                    line = rainfall.readLine();

                    tokens = line.split("\\s+");

                    for (int curr_col = 0; curr_col < patchWidth; ++curr_col)
                    {
                        Double patchRain = Double.parseDouble(tokens[curr_col]);

                        weatherData[month - 1][curr_row][curr_col] = patchRain;

                    }
                }
            } catch (IOException ex)
            {
                World.getLogger().log(Level.SEVERE, null, ex);
            }
        }

        rowDivisor = (int) Math.floor(heightInParcels / patchHeight);
        colDivisor = (int) Math.floor(widthInParcels / patchWidth);
    }

}
