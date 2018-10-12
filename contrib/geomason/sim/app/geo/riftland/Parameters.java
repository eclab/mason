package sim.app.geo.riftland;

import ec.util.Parameter;
import ec.util.ParameterDatabase;
import sim.app.geo.riftland.riftlandData.RiftLandData;

import java.io.*;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Level;

/**
 * Centralizes all non-final model parameters.
 *
 * @author Eric 'Siggy' Scott
 */
public class Parameters
{
    public final SystemParams system = new SystemParams();
    public final GUIParams gui = new GUIParams();
    public final WorldParams world = new WorldParams();
    public final VegetationParams vegetation = new VegetationParams();
    public final HouseholdsParams households = new HouseholdsParams();
    public final FarmingParams farming = new FarmingParams();
    public final HerdingParams herding = new HerdingParams();

    /** The default parameters file CLI argument */
    private final static String A_FILE = "-file";

    public Parameters(String[] args)
    {
        if (args != null)
            loadParameters(openParameterDatabase(args));
        checkConstraints();
    }

    /** Check that the parameters do not take on invalid values.  If they do,
     * throw an IllegalStateException.
     */
    private void checkConstraints()
    {
        if (households.meanHouseholdSize < 1)
            throw new IllegalStateException("Parameters: meanHouseHoldSize < 1, but must be >= 1.");
        if (households.laborProductionRate <= 0)
            throw new IllegalStateException("Parameters: laborProductionRate must be greater than 0.");
        if (herding.visionAndMovementRange <= 0)
            throw new IllegalStateException("Parameters: visionAndMovementRange must be >= 1.");
        if (world.numInitialWateringHoles < 0)
            throw new IllegalStateException("Parameters: numInitialWateringHoles must be >= 0.");
        if (vegetation.minVegetationKgPerKm2 <= 5)
            throw new IllegalStateException("Parameters: vegetation.minVegetationKgPerKm2 must be > 5.");
    }

    //<editor-fold defaultstate="collapsed" desc="ECJ ParameterDatabase methods">
    /**
     * Initialize parameter database from file
     *
     * If there exists an command line argument '-file', create a parameter
     * database from the file specified. Otherwise create an empty parameter
     * database.
     *
     * @param args contains command line arguments
     * @return newly created parameter data base
     *
     */
    private static ParameterDatabase openParameterDatabase(String[] args)
    {
        ParameterDatabase parameters = null;
        for (int x = 0; x < args.length - 1; x++)
        {
            if (args[x].equals(A_FILE))
            {
                try
                {

                    File parameterDatabaseFile = getFile(args[x + 1]);
//                    parameters = new ParameterDatabase(parameterDatabaseFile.getAbsoluteFile());
                    System.err.println(parameterDatabaseFile.getAbsoluteFile() + " that b path " + Arrays.toString(args));
                    parameters = new ParameterDatabase(parameterDatabaseFile.getAbsoluteFile(), args);
                } catch (IOException ex)
                {
                    ex.printStackTrace();
                }
                break;
            }
        }
        if (parameters == null)
        {
            System.out.println("\nNo parameter file was specified;"
                    + "\n-default programmatic settings engaged,"
                    + "\n-command line -p parameters ignored."
                    + "\nConsider using: -file foo.params [-p bar1=val1 [-p bar2=val2 ... ]]\n");
            parameters = new ParameterDatabase();
        }
        return parameters;
    }
    private static File getFile(String nodesFilename) throws IOException {
        InputStream nodeStream = RiftLandData.class.getResourceAsStream(nodesFilename);
        try {
            if (!new File("./shapeFiles/").exists()) {
                new File("./shapeFiles/").mkdir();
            }
            File targetFile = new File("./shapeFiles/" + nodesFilename.split("/")[nodesFilename.split("/").length - 1]);
            OutputStream outStream = new FileOutputStream(targetFile);
            //outStream.write(buffer);
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = nodeStream.read(bytes)) != -1) {
                outStream.write(bytes, 0, read);
            }
            outStream.close();
            nodeStream.close();
            return targetFile;
        } catch (Exception e) {
            if (nodesFilename.endsWith("shp")) {
                e.printStackTrace();
                return null;
            } else {
                e.printStackTrace();
                return null;
            }
        }
    }
    /**
     * Initialize the model with various run-time parameters.
     *
     * This allows for over-riding the default run-time parameters with values
     * read from a parameter database that was ultimately initialized from a
     * parameters file.
     *
     * @param parameterDB parameter database containing run-time parameters
     */
    private void loadParameters(ParameterDatabase parameterDB)
    {
        // System Parameters
        String loggerLevel = returnStringParameter(parameterDB, "LoggerLevel", null);
        if (loggerLevel != null)
        {
            system.setLoggerLevel(Level.parse(loggerLevel));
        }

//        system.setWriteStats(returnBooleanParameter(parameterDB, "OutputStats", false));
        system.setOutputFilename(returnStringParameter(parameterDB, "OutputFilename", null));
        system.setEthnicityOutputFilename(returnStringParameter(parameterDB, "EthnicityFilename", null));
        system.setDisplacedOutputFilename(returnStringParameter(parameterDB, "DisplacementFilename", null));
        system.setDisplacedDetailsOutputFilename(returnStringParameter(parameterDB, "DisplacementDetailsFilename", null));
        system.setNumthreads(returnIntParameter(parameterDB, "Numthreads", Runtime.getRuntime().availableProcessors()));


        // GUI Parameters
        //   It seems we don't currently read any GUI Parameters from the
        //   parameter file.


        // World Parameters
        world.setSubArea(returnStringParameter(parameterDB, "SubArea", null));
        world.setNumInitialWateringHoles(returnIntParameter(parameterDB, "NumWaterHoles",
                world.getNumInitialWateringHoles()));

        world.setInitTruePopulationPortion(returnDoubleParameter(parameterDB, "InitTruePopulationPortion", 1.0));

        world.setOnlyOneHousehold(returnBooleanParameter(parameterDB, "OnlyOneHousehold", false));

        world.setOneHouseholdX(returnIntParameter(parameterDB, "OneHouseholdX", 0));
        world.setOneHouseholdY(returnIntParameter(parameterDB, "OneHouseholdY", 0));

        world.setWeatherSequence(returnStringParameter(parameterDB, "WeatherSequence", null));

        world.setWeatherClusterControlFile(returnStringParameter(parameterDB,
                "WeatherClusterControlFile", "ClusterData/ClusterControl.txt"));
        world.setScheduleMediator(returnBooleanParameter(parameterDB, "ScheduleMediator", world.isScheduleMediator()));

        world.setPermanentWaterThreshold(returnDoubleParameter(parameterDB, "PermanentWaterThreshold",
                world.getPermanentWaterThreshold()));

        world.setWaterHoleCounterFlag(returnBooleanParameter(parameterDB, "WaterHoleCounterFlag", false));


        // Vegetation Parameters
        //   It seems we don't currently read any vegetation parameters from the
        //   parameter file.


        // Household parameters
        households.setAnnualTaxRate(returnDoubleParameter(parameterDB, "AnnualTaxRate",
                households.getAnnualTaxRate()));

        households.setInitialEndowmentInYears(returnDoubleParameter(parameterDB, "InitialEndowmentInYears",
                households.getInitialEndowmentInYears()));

        households.setLaborProductionRate(returnDoubleParameter(parameterDB, "LaborProductionRate",
                households.getLaborProductionRate()));

        households.setCanFarm(returnBooleanParameter(parameterDB, "CanFarm", households.canFarm()));
        households.setCanHerd(returnBooleanParameter(parameterDB, "CanHerd", households.canHerd()));


        // Farming parameters
        farming.setPlantingThreshold(returnDoubleParameter(parameterDB, "PlantingThreshold",
                farming.getPlantingThreshold()));

        farming.setKgOfGrainPerPersonDay(returnDoubleParameter(parameterDB, "KgOfGrainPerPersonDay",
                farming.getKgOfGrainPerPersonDay()));

        farming.setMaxWorkableLandPerFarmer(returnDoubleParameter(parameterDB, "MaxWorkableLandPerFarmer",
                farming.getMaxWorkableLandPerFarmer()));

        farming.setFarmIntensificationExponent(returnDoubleParameter(parameterDB, "FarmIntensificationExponent",
                farming.getFarmIntensificationExponent()));

        farming.setMaxMoveDistanceForNewHousehold(returnIntParameter(parameterDB, "MaxMoveDistanceForNewHousehold",
                farming.getMaxMoveDistanceForNewHousehold()));

        farming.setFarmingRestartProbabilityExponent(returnDoubleParameter(parameterDB, "FarmingRestartProbabilityExponent",
                farming.getFarmingRestartProbabilityExponent()));


        // Herding: Physiology parameters
        herding.setTLUFoodConsumptionRate(returnDoubleParameter(parameterDB, "TLUFoodConsumptionRate",
                herding.getTLUFoodConsumptionRate()));

        herding.setTLUFoodMetabolismRate(returnDoubleParameter(parameterDB, "TLUFoodMetabolismRate",
                herding.getTLUFoodMetabolismRate()));

        herding.setTLUWaterMetabolismRate(returnIntParameter(parameterDB, "TLUWaterMetabolismRate",
                herding.getTLUWaterMetabolismRate()));

        herding.setTLUMaxDaysWithoutFood(returnIntParameter(parameterDB, "TLUMaxDaysWithoutFood",
                herding.getTLUMaxDaysWithoutFood()));

        herding.setTLUMaxDaysWithoutWater(returnIntParameter(parameterDB, "TLUMaxDaysWithoutWater",
                herding.getTLUMaxDaysWithoutWater()));

        herding.setTLUFoodMax(returnDoubleParameter(parameterDB, "TLUFoodMax",
                herding.getTLUFoodMax()));

        herding.setTLUFoodStress(returnDoubleParameter(parameterDB, "TLUFoodStress",
                herding.getTLUFoodStress()));

        herding.setTLUWaterMax(returnDoubleParameter(parameterDB, "TLUWaterMax",
                herding.getTLUWaterMax()));

        herding.setTLUWaterStress(returnDoubleParameter(parameterDB, "TLUWaterStress",
                herding.getTLUWaterStress()));

        herding.setHerdIdealBirthProbability(returnDoubleParameter(parameterDB, "HerdIdealBirthProbability",
                herding.getHerdIdealBirthProbability()));

        herding.setFoodCostOfMovement(returnDoubleParameter(parameterDB, "FoodCostOfMovement",
                herding.getFoodCostOfMovement()));

        herding.setWaterCostOfMovement(returnDoubleParameter(parameterDB, "WaterCostOfMovement",
                herding.getWaterCostOfMovement()));

        // Herding: Behavior parameters
        herding.setHerdingFoodWeight(returnDoubleParameter(parameterDB, "HerdingFoodWeight",
                herding.getHerdingFoodWeight()));

        herding.setHerdingWaterWeight(returnDoubleParameter(parameterDB, "HerdingWaterWeight",
                herding.getHerdingWaterWeight()));

        herding.setHerdingDistanceWeight(returnDoubleParameter(parameterDB, "HerdingDistanceWeight",
                herding.getHerdingDistanceWeight()));

        herding.setHerdingHomeWeight(returnDoubleParameter(parameterDB, "HerdingHomeWeight",
                herding.getHerdingHomeWeight()));

        herding.setReturnHomeCheckInterval(returnIntParameter(parameterDB, "ReturnHomeCheckInterval",
                herding.getReturnHomeCheckInterval()));


        // Herding: Conversions parameters
        herding.setTLUPerPFD(returnIntParameter(parameterDB, "TLUPerPFD",
                herding.getTLUPerPFD()));

        herding.setPFDPerConsumedTLU(returnDoubleParameter(parameterDB, "PFDPerConsumedTLU",
                herding.getPFDPerConsumedTLU()));

        herding.setMaxTLUsPerHerder(returnIntParameter(parameterDB, "MaxTLUsPerHerder",
                herding.getMaxTLUsPerHerder()));

        herding.setHerdVegetationThreshold(returnDoubleParameter(parameterDB, "HerdVegetationThreshold",
                herding.getHerdVegetationThreshold()));

        herding.setVisionAndMovementRange(returnIntParameter(parameterDB, "VisionAndMovementRange",
                herding.getVisionAndMovementRange()));

        herding.setMaxNearbyWaterHoles(returnIntParameter(parameterDB, "MaxNearbyWaterHoles",
                herding.getMaxNearbyWaterHoles()));

        herding.setHerdSplitThreshold(returnIntParameter(parameterDB, "HerdSplitThreshold",
                herding.getHerdSplitThreshold()));

        herding.setHerdingRestartProbabilityExponent(returnDoubleParameter(parameterDB, "HerdingRestartProbabilityExponent",
                herding.getHerdingRestartProbabilityExponent()));

        herding.setPlentyOfWaterInTLUDays(returnIntParameter(parameterDB, "PlentyOfWaterInTLUDays",
                herding.getPlentyOfWaterInTLUDays()));


        // Herding: other parameters
        herding.setMaxTrailLength(returnIntParameter(parameterDB, "MaxTrailLength",
                herding.getMaxTrailLength()));

        herding.setUseHerdingRuleBased(returnBooleanParameter(parameterDB, "HerdUseRuleBased",
                herding.isUseHerdingRuleBased()));
    }



    /**
     * Convenience function for getting an integer value from the parameter
     * database
     *
     * @param parameterName name of parameter
     * @param defaultValue value to return if database doesn't know about that
     * parameter
     *
     * @return the value stored for that parameter, or 'default' if database
     * doesn't have that value
     *
     * XXX consider moving this to a separate utility class; or changing MASON
     * to accept mere strings
     *
     */
    public int returnIntParameter(ParameterDatabase paramDB, String parameterName, int defaultValue)
    {
        return paramDB.getIntWithDefault(new Parameter(parameterName), null, defaultValue);
    }

    /**
     * Convenience function for getting a boolean value from the parameter
     * database
     *
     * @param parameterName
     * @param defaultValue
     *
     * @return the value stored for that parameter, or 'default' if database
     * doesn't have that value
     *
     * XXX consider moving this to a separate utility class; or changing MASON
     * to accept mere strings
     *
     */
    public boolean returnBooleanParameter(ParameterDatabase paramDB, String parameterName, boolean defaultValue)
    {
        return paramDB.getBoolean(new Parameter(parameterName), null, defaultValue);
    }

    /**
     * Convenience function for getting a double value from the parameter
     * database
     *
     * @param parameterName name of parameter
     * @param defaultValue value to return if database doesn't know about that
     * parameter
     *
     * @return the value stored for that parameter, or 'default' if database
     * doesn't have that value
     *
     * XXX consider moving this to a separate utility class; or changing MASON
     * to accept mere strings
     *
     */
    double returnDoubleParameter(ParameterDatabase paramDB, String parameterName, double defaultValue)
    {
        return paramDB.getDoubleWithDefault(new Parameter(parameterName), null, defaultValue);
    }

    /**
     * Convenience function for getting a String value from the parameter
     * database
     *
     * @param parameterName name of parameter
     * @param defaultValue value to return if database doesn't know about that
     * parameter
     *
     * @return the value stored for that parameter, or 'default' if database
     * doesn't have that value
     *
     * XXX consider moving this to a separate utility class; or changing MASON
     * to accept mere strings
     *
     */
    String returnStringParameter(ParameterDatabase paramDB, String parameterName, String defaultValue)
    {
        return paramDB.getStringWithDefault(new Parameter(parameterName), null, defaultValue);
    }
    //</editor-fold>


    public class SystemParams
    {
        private static final long serialVersionUID = 1L;
        private Level loggerLevel = Level.INFO;
        private int numthreads = Runtime.getRuntime().availableProcessors(); // Number of threads to spawn for running assigneParcel & populateParcel in parallel
        private boolean runExpensiveAsserts = false; // Asserts that are known to be expensive will only be checked if this is true and asserts are enabled.
        private String outputFilename = "";
        private String ethnicityOutputFilename = "";
        private String displacedOutputFilename = "";
        private String displacedDetailsOutputFilename = "";

        //<editor-fold defaultstate="collapsed" desc="Accessors">



        public boolean isRunExpensiveAsserts()
        {
            return runExpensiveAsserts;
        }

        public void setRunExpensiveAsserts(boolean runExpensiveAsserts) {
            this.runExpensiveAsserts = runExpensiveAsserts;
        }

        public int getNumthreads()
        {
            return numthreads;
        }

        public void setNumthreads(int numthreads)
        {
            this.numthreads = numthreads;
        }

        public Level getLoggerLevel()
        {
            return loggerLevel;
        }

        public void setLoggerLevel(Level loggerLevel)
        {
            this.loggerLevel = loggerLevel;
        }

        /** XXX Would be great if MASON handled enums. =\
         *
         * @param levelString corresponding to valid Level
         */
        public void setLoggerLevel(final String levelString)
        {
            // At startup, the world logger won't exist yet.
            if ( World.getLogger() == null )
            {
                return;
            }

            Level level;

            try
            {
                // upcase as a convenience
                String upperizedLevelString = levelString.toUpperCase();
                level = Level.parse(upperizedLevelString);
            } catch (IllegalArgumentException illegalArgumentException)
            {
                System.err.println("Bogus logging string: " + levelString + ", logging level not set");
                return;
            }

            World.setLoggerLevel(level);

            System.err.println("Logger level now " + World.getLoggerLevel());
        }

        public String getOutputFilename() {
            return outputFilename;
        }

        public void setOutputFilename(String val) {
            outputFilename = val;
        }
        public String getEthnicityOutputFilename() {
            return ethnicityOutputFilename;
        }

        public void setEthnicityOutputFilename(String val) {
            ethnicityOutputFilename = val;
        }

        public String getDisplacedOutputFilename()
        {
            return displacedOutputFilename;
        }

        public void setDisplacedOutputFilename(String val)
        {
            displacedOutputFilename = val;
        }

        public String getDisplacedDetailsOutputFilename() {
            return displacedDetailsOutputFilename;
        }
        public void setDisplacedDetailsOutputFilename(String val)
        {
            displacedDetailsOutputFilename = val;
        }


        //</editor-fold>
    }


    public enum HerdingJitter { NONE, FIXED, RANDOM };
    public class GUIParams
    {
        // NOTE: Currently GUI parameters are not read from the parameter file,
        //       so these values are the hard-coded defaults.

        /** The scale of Herding portrayals will not fall below this value. */
        private double minHerdingScale = 0.3;
        /** The scale of Herding portrayals will not rise above this value. */
        private double maxHerdingScale = 1.0;
        /** Whether to jitter the Herding portrayals, and how. */
        private HerdingJitter herdingJitter = HerdingJitter.FIXED;

        //<editor-fold defaultstate="collapsed" desc="Accessors">
        public HerdingJitter getHerdingJitter()
        {
            return herdingJitter;
        }

        public void setHerdingJitter(HerdingJitter herdingJitter) {
            this.herdingJitter = herdingJitter;
        }

        public double getMinHerdingScale() {
            return minHerdingScale;
        }

        public void setMinHerdingScale(double minHerdingSize) {
            this.minHerdingScale = minHerdingSize;
        }

        public double getMaxHerdingScale() {
            return maxHerdingScale;
        }

        public void setMaxHerdingScale(double maxHerdingSize) {
            this.maxHerdingScale = maxHerdingSize;
        }
        //</editor-fold>
    }


    public class WorldParams
    {
        private String datapath = "/riftland/riftlandData/";
        private String populationFile = "PopData/riftpopulation.asc";
        private String politicalBoundariesFile = "political/RiftLand_Boundary.shp";
        private String landuseFile = "LandData/landquality.txt";
        private String farmerFile = "LandData/landtenure.txt";
        private String urbanuseFile = "RLUData/rlurbareas.txt";
        private String parklandFile = "LandData/landreserved.txt";
        private String forestFile = "LandData/landforest.txt";
        private String ndviResidualFile = "NDVIData/NDVIRes.txt";
        private String ndviDataFile = "NDVIData/ndvi2001001.asc";
        private String ethnicRegionsFile = "ethnicities/MurdockMOA.shp";
        private String riverDataFile = "RiverData/riverdataallaccum.txt";
        private String weatherClusterControlFile = "ClusterData/ClusterControl.txt";

        private String subArea = "";

        /**
         * If true, then a Mediator object is scheduled to run. <p> Can be set by
         * "ScheduleMediator" in params file.
         */
        private boolean scheduleMediator = false;
        /** number of initial watering holes in the simulation */
        private int numInitialWateringHoles = 20000;
        /** starting number of herding activities in simulation */
        private int numInitialHerdingActivities = 1000;
        /**
         * The percentage of the true population to place in a parcel <p> In range
         * (0,1].
         *
         * @see riftland.Population; //@placeHouseholdsUsingPopulationData
         */
        private double initTruePopulationPortion = 1.0;

        /** Overrides initTruePopulationPortion and places exactly one
         * household in the simulation. */
        private boolean onlyOneHousehold = false;

        /** Coordinates to place the household if onlyOneHousehold is true. */
        private int oneHouseholdX = 0;
        /** Coordinates to place the household if onlyOneHousehold is true. */
        private int oneHouseholdY = 0;

        /** Comma-seperated sequence of weather years to run, after which, the years loop as normal. */
        private String weatherSequence = null;

        /** Water holes with at least this much flow will never go dry. */
        private double permanentWaterThreshold = 50000;

        private boolean waterHoleCounterFlag = false;

        //<editor-fold defaultstate="collapsed" desc="Accessors">

        public String getDatapath()
        {
            return datapath;
        }

        public boolean isWaterHoleCounterFlag() {
            return waterHoleCounterFlag;
        }

        public boolean isOnlyOneHousehold()
        {
            return onlyOneHousehold;
        }

        public void setOnlyOneHousehold(boolean onlyOneHousehold) {
            this.onlyOneHousehold = onlyOneHousehold;
        }

        public int getOneHouseholdX() {
            return oneHouseholdX;
        }

        public void setOneHouseholdX(int oneHouseholdX) {
            this.oneHouseholdX = oneHouseholdX;
        }

        public int getOneHouseholdY() {
            return oneHouseholdY;
        }

        public void setOneHouseholdY(int oneHouseholdY) {
            this.oneHouseholdY = oneHouseholdY;
        }

        public void setPopulationFile(String populationFile)
        {
            this.populationFile = populationFile;
        }

        public void setPoliticalBoundariesFile(String politicalBoundariesFile)
        {
            this.politicalBoundariesFile = politicalBoundariesFile;
        }

        public void setLanduseFile(String landuseFile)
        {
            this.landuseFile = landuseFile;
        }

        public void setFarmerFile(String farmerFile)
        {
            this.farmerFile = farmerFile;
        }

        public void setUrbanuseFile(String urbanuseFile)
        {
            this.urbanuseFile = urbanuseFile;
        }

        public void setParklandFile(String parklandFile)
        {
            this.parklandFile = parklandFile;
        }

        public void setForestFile(String forestFile)
        {
            this.forestFile = forestFile;
        }

        public void setNdviResidualFile(String ndviResidualFile)
        {
            this.ndviResidualFile = ndviResidualFile;
        }

        public void setNdviDataFile(String ndviDataFile)
        {
            this.ndviDataFile = ndviDataFile;
        }

        public void setEthnicRegionsFile(String ethnicRegionsFile)
        {
            this.ethnicRegionsFile = ethnicRegionsFile;
        }

        public void setRiverDataFile(String riverDataFile)
        {
            this.riverDataFile = riverDataFile;
        }

        public String getRiverDataFile()
        {
            return riverDataFile;
        }

        public void setWeatherClusterControlFile(String weatherClusterControlFile)
        {
            this.weatherClusterControlFile = weatherClusterControlFile;
        }

        public String getWeatherClusterControlFile()
        {
            return weatherClusterControlFile;
        }

        public String getEthnicRegionsFile()
        {
            return ethnicRegionsFile;
        }

        public String getNdviDataFile()
        {
            return ndviDataFile;
        }

        public String getPopulationFile()
        {
            return populationFile;
        }

        public String getPoliticalBoundariesFile()
        {
            return politicalBoundariesFile;
        }

        public String getLanduseFile()
        {
            return landuseFile;
        }

        public String getFarmerFile()
        {
            return farmerFile;
        }

        public String getUrbanuseFile()
        {
            return urbanuseFile;
        }

        public String getParklandFile()
        {
            return parklandFile;
        }

        public String getForestFile()
        {
            return forestFile;
        }

        public String getNdviResidualFile()
        {
            return ndviResidualFile;
        }

        public String getSubArea()
        {
            return subArea;
        }

        public void setSubArea(String subArea)
        {
            this.subArea = subArea;
        }

        /**
         * Sets the number of water holes
         *
         * Will not set to val if val < 0
         *
         * @param val new amount of watering holes
         */
        public void setNumInitialWateringHoles(int val)
        {
            if (val >= 0)
            {
                numInitialWateringHoles = val;
            }
        }

        public boolean isScheduleMediator()
        {
            return scheduleMediator;
        }

        public void setScheduleMediator(boolean scheduleMediator)
        {
            this.scheduleMediator = scheduleMediator;
        }

        public int getNumInitialWateringHoles()
        {
            return numInitialWateringHoles;
        }

        public int getNumInitialHerdingActivities()
        {
            return numInitialHerdingActivities;
        }

        public void setNumInitialHerdingActivities(int numInitialHerdingActivities)
        {
            this.numInitialHerdingActivities = numInitialHerdingActivities;
        }

        public double getInitTruePopulationPortion()
        {
            return initTruePopulationPortion;
        }

        public void setInitTruePopulationPortion(double initTruePopulationPortion)
        {
            this.initTruePopulationPortion = initTruePopulationPortion;
        }

//        public int getNumInitialRuralHouseholds()
//        {
//            return numInitialRuralHouseholds;
//        }
//
//        public void setNumInitialRuralHouseholds(int numInitialRuralHouseholds)
//        {
//            this.numInitialRuralHouseholds = numInitialRuralHouseholds;
//        }

//        public int getNumInitialRuralPopulation()
//        {
//            return numInitialRuralPopulation;
//        }
//
//        public void setNumInitialRuralPopulation(int numInitialRuralPopulation)
//        {
//            this.numInitialRuralPopulation = numInitialRuralPopulation;
//        }
//
//        public double getWaterHoleFertilityBias()
//        {
//            return waterHoleFertilityBias;
//        }
//
//        public void setWaterHoleFertilityBias(double waterHoleFertilityBias)
//        {
//            this.waterHoleFertilityBias = waterHoleFertilityBias;
//        }


        public String getWeatherSequence() {
            return weatherSequence;
        }

        public void setWeatherSequence(String val) {
            weatherSequence = val;
        }

        public double getPermanentWaterThreshold() {
            return permanentWaterThreshold;
        }

        public void setPermanentWaterThreshold(double val) {
            permanentWaterThreshold = val;
        }

        public boolean getWaterHoleCounterFlag() {
            return waterHoleCounterFlag;
        }

        public void setWaterHoleCounterFlag(boolean val) {
            waterHoleCounterFlag = val;
        }

        //</editor-fold>
    }


    public class VegetationParams
    {
        /**
         * What is this?  Where is it used? < explains magic number for theoreticalMaxVegetationKgPerKm2
         *
         * ref: De Leeuw, et al. 1993 table 1
         *
         * production for 200mm rainfall in Kenya
         * Pallas 1986 (ref for water) has a table
         * with value for 250 mm annual rainfall:
         *
         * 25 tons of DM/km2 which is 25 x 1000
         * = 2.5x10^4, or .25x10^5 vice 1.1x10^5
         * ~1/4 the above figure, i.e., in ballpark
         */
        private double theoreticalMaxVegetationKgPerHectare = 1.1 * 1000; // kgDM/ha= tonsDM/ha * kg/ton
        private double theoreticalMaxVegetationKgPerKm2 = theoreticalMaxVegetationKgPerHectare * 100; // kg/km2= tons DM/ha * kg/ton * ha/km2

        /** Minimum rainfall needed for growth (per day) (alpha in the MatLab GA code).
         *  Set by the GA. */
        //private double growthThreshold = 4.237; // value produced by GA with no herders
        private double growthThreshold = 3;// recalibration to account for herders
        /** Weight of the parcel quality's contribution to the growth rate.
         * (beta in the MatLab GA code).  Set by the GA. */
        private double qualityMultiplier = 2.33;
        /** Fraction of the theoretical max vegetation that the
         * parcel's vegetation is not permitted to fall below.
         * Set by the GA. */
        private double minVegRatio = 0.15;
        /** Fraction of the theoretical max vegetation that the
         * parcel's vegetation is not permitted to rise above.
         * Set by the GA. */
        private double maxVegRatio = 0.908;

        /** The amount of vegetation does not fall below this value.
         *  (If we let it get too close to the theoretical minimum,
         *  the growth rate would get too close to zero and couldn't
         *  grow fast enough). */
        private double minVegetationKgPerKm2 = minVegRatio * theoreticalMaxVegetationKgPerKm2;

        /** The amount of vegetation does not rise above this value. 
         *  (If we let it get too close to the theoretical maximum,
         *  the growth rate would get too close to zero and couldn't
         *  shrink fast enough). */
        private double maxVegetationKgPerKm2 = maxVegRatio * theoreticalMaxVegetationKgPerKm2;


        /** VegetationParams base growth rate
         *
         * TODO Reference?  Is this a reasonable default value?
         * TODO needs to be moved to world and integrated with parameter database
         */
        private double baseGrowthRate = 0.011; // -- this is the Sacred, calibrated value
        //private double baseGrowthRate = 0.11; // test turnup

        // <editor-fold defaultstate="collapsed" desc="Accessors">
        public double getTheoreticalMaxVegetationKgPerHectare()
        {
            return theoreticalMaxVegetationKgPerHectare;
        }

        public void setTheoreticalMaxVegetationKgPerHectare(double theoreticalMaxVegetationKgPerHectare)
        {
            this.theoreticalMaxVegetationKgPerHectare = theoreticalMaxVegetationKgPerHectare;
        }

        public double getTheoreticalMaxVegetationKgPerKm2()
        {
            return theoreticalMaxVegetationKgPerKm2;
        }

        public void setTheoreticalMaxVegetationKgPerKm2(double theoreticalMaxVegetationKgPerKm2)
        {
            this.theoreticalMaxVegetationKgPerKm2 = theoreticalMaxVegetationKgPerKm2;
        }

        public double getGrowthThreshold()
        {
            return growthThreshold;
        }

        public void setGrowthThreshold(double growthThreshold)
        {
            this.growthThreshold = growthThreshold;
        }

        public double getQualityMultiplier()
        {
            return qualityMultiplier;
        }

        public void setQualityMultiplier(double qualityMultiplier)
        {
            this.qualityMultiplier = qualityMultiplier;
        }

        public double getMinVegRatio()
        {
            return minVegRatio;
        }

        public void setMinVegRatio(double minVegRatio)
        {
            this.minVegRatio = minVegRatio;
        }

        public double getMaxVegRatio()
        {
            return maxVegRatio;
        }

        public void setMaxVegRatio(double maxVegRatio)
        {
            this.maxVegRatio = maxVegRatio;
        }

        public double getMinVegetationKgPerKm2()
        {
            return minVegetationKgPerKm2;
        }

        public void setMinVegetationKgPerKm2(double minVegetationKgPerKm2)
        {
            assert (minVegetationKgPerKm2 > 5);		// this is necessary for visualization purposes
            this.minVegetationKgPerKm2 = minVegetationKgPerKm2;
        }

        public double getMaxVegetationKgPerKm2()
        {
            return maxVegetationKgPerKm2;
        }

        public void setMaxVegetationKgPerKm2(double maxVegetationKgPerKm2)
        {
            this.maxVegetationKgPerKm2 = maxVegetationKgPerKm2;
        }

        public double getBaseGrowthRate()
        {
            return baseGrowthRate;
        }

        public void setBaseGrowthRate(double baseGrowthRate)
        {
            this.baseGrowthRate = baseGrowthRate;
        }
        // </editor-fold>
    }


    public class HouseholdsParams
    {
        /** Enable or disable herding across the board. */
        private boolean canHerd = true;

        /** Enable or disable farming across the board. */
        private boolean canFarm = true;

        /** Average number of persons in each household at initialization. */
        private int meanHouseholdSize = 9;

        // minimum household population to split
        private int householdSplitPopulation = 12;  // min pop level to split

        /** Households will be initialized with enough wealth to survie for 
         * this many years, based on initial population and no production.
         */
        private double initialEndowmentInYears = 3.0;

        /** This is how many person-food-days of wealth produced by one day's labor. */
        private double laborProductionRate = 0.8;

        // tax on all assets
        private double annualTaxRate = 0.02;

        //<editor-fold defaultstate="collapsed" desc="Accessors">

        public boolean canHerd() {	return canHerd; }

        public boolean canFarm() {
            return canFarm;
        }

        public void setCanHerd(boolean canHerd) {
            this.canHerd = canHerd;
        }

        public void setCanFarm(boolean canFarm) {
            this.canFarm = canFarm;
        }

        public int getMeanHouseholdSize()
        {
            return meanHouseholdSize;
        }

        public void setMeanHouseholdSize(int meanHouseholdSize)
        {
            this.meanHouseholdSize = meanHouseholdSize;
        }

        public int getHouseholdSplitPopulation()
        {
            return householdSplitPopulation;
        }

        public void setHouseholdSplitPopulation(int householdSplitPopulation)
        {
            this.householdSplitPopulation = householdSplitPopulation;
        }

        public double getInitialEndowmentInYears() {
            return initialEndowmentInYears;
        }

        public void setInitialEndowmentInYears(double val) {
            initialEndowmentInYears = val;
        }

        public double getLaborProductionRate()
        {
            return laborProductionRate;
        }

        public void setLaborProductionRate(double laborProductionRate)
        {
            this.laborProductionRate = laborProductionRate;
        }

        public double getAnnualTaxRate()
        {
            return annualTaxRate;
        }

        public void setAnnualTaxRate(double tax)
        {
            this.annualTaxRate = tax;
        }
        //</editor-fold>
    }


    public class FarmingParams
    {
        /** threshold of rain per month to start planting */
        private double plantingThreshold = 300;
        private int initialPlantingDate = 85;
        private int initialPlantingSeasonLength = 90;
        private double kgOfGrainPerPersonDay = 1.5;

        /** amount of farm land per farmer at which productivity peaks (currently 1ha/farmer) */
        private double maxWorkableLandPerFarmer = 1.0;

        /**
         *
         * how much additional production comes from an additional farmer.
         * Exponent used by computeYieldRate() to determine the increase in
         * productivity that comes from adding workers to a farm.
         *
         * */
        private double farmIntensificationExponent = 0.2;

        /**
         * This is the radius of maximum Moore neighborhood that we would like to search for finding a suitable parcel to locate new farm.
         */
        private int maxMoveDistanceForNewHousehold = 10;

        /**
         * needs description.
         */
        private double farmingRestartProbabilityExponent = 1.0;

        //<editor-fold defaultstate="collapsed" desc="Accessors">
        public double getFarmingRestartProbabilityExponent()
        {
            return farmingRestartProbabilityExponent;
        }

        public void setFarmingRestartProbabilityExponent(double farmingRestartProbabilityExponent)
        {
            this.farmingRestartProbabilityExponent = farmingRestartProbabilityExponent;
        }

        public int getMaxMoveDistanceForNewHousehold()
        {
            return maxMoveDistanceForNewHousehold;
        }

        public void setMaxMoveDistanceForNewHousehold(int maxMoveDistanceForNewHousehold)
        {
            this.maxMoveDistanceForNewHousehold = maxMoveDistanceForNewHousehold;
        }

        public double getFarmIntensificationExponent()
        {
            return farmIntensificationExponent;
        }

        public void setFarmIntensificationExponent(double farmIntensificationExponent)
        {
            this.farmIntensificationExponent = farmIntensificationExponent;
        }
        public double getMaxWorkableLandPerFarmer()
        {
            return maxWorkableLandPerFarmer;
        }

        public void setMaxWorkableLandPerFarmer(double maxWorkableLandPerFarmer)
        {
            this.maxWorkableLandPerFarmer = maxWorkableLandPerFarmer;
        }

        public double getPlantingThreshold()
        {
            return plantingThreshold;
        }

        public void setPlantingThreshold(double plantingThreshold)
        {
            this.plantingThreshold = plantingThreshold;
        }

        public int getInitialPlantingDate()
        {
            return initialPlantingDate;
        }

        public void setInitialPlantingDate(int initialPlantingDate)
        {
            this.initialPlantingDate = initialPlantingDate;
        }

        public int getInitialPlantingSeasonLength()
        {
            return initialPlantingSeasonLength;
        }

        public void setInitialPlantingSeasonLength(int initialPlantingSeasonLength)
        {
            this.initialPlantingSeasonLength = initialPlantingSeasonLength;
        }

        public double getKgOfGrainPerPersonDay() {
            return kgOfGrainPerPersonDay;
        }

        public void setKgOfGrainPerPersonDay(double val) {
            kgOfGrainPerPersonDay = val;
        }
        //</editor-fold>
    }


    public class HerdingParams
    {
        /** Average number of cows used to initialize a herd (at household creation) */
        private int meanInitialNumberHerdAssets = 19;
        /**
         * This determines which HerdingParams subclass is used for herder behavior. <p>
         * If this is true, a "fast and frugal"-based herding behavior is used.
         * Otherwise a weighted sum-based approach is used.
         */
        private boolean useHerdingRuleBased = false;
        /**
         * We model herding's contribution to the household as how much food a TLU
         * will provide. For all TLUs, we presume a constant ration of 1 TLU provides
         * food for 1 person for X days based on 3kg per person per day (with 1/2
         * losses)
         *
         * change units to days person fed by sold TLU units are days
         *
         *
         */
        private double PFDPerConsumedTLU = 250.0 / 3.0;
        /**
         * How many TLU's a person can herd
         */
        private int maxTLUsPerHerder = 15;

        /**
         * TLUs to support herder: how many TLUs needed to support 1 person
         * continuously
         */
        private int TLUPerPFD = 3;

        /** How much each animal can consume in a day */
        private double TLUFoodConsumptionRate = 1500.00;

        /** Amount of vegetation a single starving animal needs to be full again. */
        //Not sure about above comment, I think it is the base amount a TLU needs to eat each day -- TIM
        //private double TLUFoodMetabolismRate = 10.25;   // kg/day
        private double TLUFoodMetabolismRate = 5.0;   // kg/day
        /** Number of days an animal can go without food before dying */
        private int TLUMaxDaysWithoutFood = 60; // multiple references
        /**
         * The maximum amount of food an animal can "store" (in its belly) Assume
         * minFood is 0.
         */
        private double TLUFoodMax = 1.25 * TLUFoodMetabolismRate * TLUMaxDaysWithoutFood;
        /**
         * The amount of food (in it's belly) below which an animal becomes stressed
         * (risks death?)
         */
        private double TLUFoodStress = 0.5 * TLUFoodMetabolismRate * TLUMaxDaysWithoutFood;
        /** Number of days an animal can go without water before dying */
        private int TLUMaxDaysWithoutWater = 6; // 2-3 to "less than a week"

        /**
         * The amount of water (in liters) metabolized each day per TLU. FAO study has cattle 
         * drinking 5 liters/day in the wet season (due to water in vegetation, and 27 liters/day
         * in the dry season.
         * So, 30 is high. We'll try 20.
         *
         */
        private int TLUWaterMetabolismRate = 20;
        /**
         * The maximum amount of water an animal can "store" (in its belly) Assume
         * minWater is 0.
         */
        private double TLUWaterMax = 1.25 * TLUWaterMetabolismRate * TLUMaxDaysWithoutWater;
        /**
         * The amount of water (in its belly) below which an animal becomes
         * stressed (risks death?)
         *
         */
        private double TLUWaterStress = 0.5 * TLUWaterMetabolismRate * TLUMaxDaysWithoutWater;

        /** Food burned by moving 1km as a fraction of TLUFoodMetabolismRate */
        private double foodCostOfMovement = 0.1;

        /** Water burned by moving 1km as a fraction of TLUWaterMetabolismRate */
        private double waterCostOfMovement = 0.1;

        /**
         * this is the amount of vegetation that 100 TLUs would eat in 1 day based
         * on 10.25 kgDM/day consumption <p> XXX what is the reference for this?
         */
        private double herdVegetationThreshold = 1025;

        /**
         * XXX How far a unit can "see"? <p> XXX NOTE THIS <B>ALSO</B> DENOTES
         * MAXIMUM MOVEMENT PER DAY!!
         */
        private int visionAndMovementRange = 10;
        /** Max number of waterholes stored in each WaterHole's nearbyWateringHoles cache. */
        private int maxNearbyWaterHoles = 10;
        /** Threshold for when a herd divides into two herds */
        private int herdSplitThreshold = 100;  // split herd

        /** Probability that a TLU gives birth on a given day.
         * Fertile female cows have, on average, one calf per year.
         * Assume that 30% of all TLUs are fertile females.
         */
        private double herdIdealBirthProbability = (1 / 365.0) * 0.3;

        /** how much weight we should give to food when deciding where to go */
        private double herdingFoodWeight = 1.0;
        /** how much weight we should give to water when deciding where to go */
        private double herdingWaterWeight = 1.0;
        /** how much weight we should give to distance when deciding where to go */
        private double herdingDistanceWeight = 0.05;
        /** how much weight we should give to returning home to plant when deciding where to go */
        private double herdingHomeWeight = 1.0;

        /** The maximum visual history for herders */
        private int maxTrailLength = 5;

        /** How often to check whether it's time for a herd to return home. */
        private int returnHomeCheckInterval = 14; // Days

        /** Amount of water in TLU days that a waterhole must have to be considered "plenty".
         * Above "plenty", all waterholes are considered equal for herd movement purposes.
         */
        private int plentyOfWaterInTLUDays = 10000;	// 10 herds of 100 cows drinking for 10 days


        /**
         * Controls how quickly a household will give up on herding (larger is more persistent)
         */
        private double herdingRestartProbabilityExponent = 1.0;

        //<editor-fold defaultstate="collapsed" desc="Accessors">

        public int getPlentyOfWaterInTLUDays()
        {
            return plentyOfWaterInTLUDays;
        }

        public void setPlentyOfWaterInTLUDays(int val)
        {
            plentyOfWaterInTLUDays = val;
        }

        public double getHerdingRestartProbabilityExponent()
        {
            return herdingRestartProbabilityExponent;
        }

        public void setHerdingRestartProbabilityExponent(double herdingRestartProbabilityExponent)
        {
            this.herdingRestartProbabilityExponent = herdingRestartProbabilityExponent;
        }

        public double getWaterCostOfMovement()
        {
            return waterCostOfMovement;
        }

        public void setWaterCostOfMovement(double val)
        {
            waterCostOfMovement = val;
        }

        public double getFoodCostOfMovement()
        {
            return foodCostOfMovement;
        }

        public void setFoodCostOfMovement(double val)
        {
            foodCostOfMovement = val;
        }

        /** Amount of water a WaterHole ought to have (in liters) to be considered to have
         *  "plenty of water" -- i.e. if it has more than this, we don't care.
         */
        public double getPlentyOfWater()
        {
            return plentyOfWaterInTLUDays * TLUWaterMetabolismRate; // (herds)(cows/herd)(liters/(cow*day))(days)
        }

        public int getReturnHomeCheckInterval()
        {
            return returnHomeCheckInterval;
        }

        public void setReturnHomeCheckInterval(int val) {
            returnHomeCheckInterval = val;
        }

        public int getMaxNearbyWaterHoles()
        {
            return maxNearbyWaterHoles;
        }

        public void setMaxNearbyWaterHoles(int val)
        {
            maxNearbyWaterHoles = val;
        }

        public int getMigrationRange()
        {
            return visionAndMovementRange * TLUMaxDaysWithoutWater;
        }

        public int getMeanInitialNumberHerdAssets()
        {
            return meanInitialNumberHerdAssets;
        }

        public void setMeanInitialNumberHerdAssets(int meanInitialNumberHerdAssets)
        {
            this.meanInitialNumberHerdAssets = meanInitialNumberHerdAssets;
        }
        public double getPFDPerConsumedTLU()
        {
            return PFDPerConsumedTLU;
        }

        public void setPFDPerConsumedTLU(double PFDPerConsumedTLU)
        {
            this.PFDPerConsumedTLU = PFDPerConsumedTLU;
        }

        public int getMaxTLUsPerHerder()
        {
            return maxTLUsPerHerder;
        }

        public void setMaxTLUsPerHerder(int maxTLUsPerHerder)
        {
            this.maxTLUsPerHerder = maxTLUsPerHerder;
        }

        public int getTLUPerPFD()
        {
            return TLUPerPFD;
        }

        public void setTLUPerPFD(int val)
        {
            TLUPerPFD = val;
        }

        /**
         * Set the amount of vegetation a single starving animal needs to be full
         * again
         *
         * Note that maximum amount that an animal stores and when the animal is
         * stressed due to starvation depends on the consumption rate, so are
         * recalibrated accordingly.
         *
         */
        void setTLUFoodMetabolismRate(double rate)
        {
            TLUFoodMetabolismRate = rate;

            TLUFoodMax = 1.25 * TLUFoodMetabolismRate * TLUMaxDaysWithoutFood;
            TLUFoodStress = 0.5 * TLUFoodMetabolismRate * TLUMaxDaysWithoutFood;
        }

        /**
         * Set the most amount of time in days an animal can go without food before
         * dying.
         *
         * Not that the maximum amount that an animal stores and when the animal is
         * stressed due to starvation depends on the maximum days before death due
         * to starvation, so these are recalibrated accordingly.
         *
         * @param days
         */
        void setTLUMaxDaysWithoutFood(int days)
        {
            TLUMaxDaysWithoutFood = days;

            TLUFoodMax = 1.25 * TLUFoodMetabolismRate * TLUMaxDaysWithoutFood;
            TLUFoodStress = 0.5 * TLUFoodMetabolismRate * TLUMaxDaysWithoutFood;
        }

        /**
         * Assign the number of days an animal can go before dying of thirst.
         *
         * Note that the maximum amount of water an animal can hold and the number
         * of days an animal can go without water before being stressed are
         * dependent on this value, so they're recalibrated accordingly.
         *
         * @param days
         */
        void setTLUMaxDaysWithoutWater(int days)
        {
            TLUMaxDaysWithoutWater = days;

            TLUWaterMax = 1.25 * TLUWaterMetabolismRate * TLUMaxDaysWithoutWater;
            TLUWaterStress = 0.5 * TLUWaterMetabolismRate * TLUMaxDaysWithoutWater;
        }

        void setVisionAndMovementRange(int val)
        {
            if (val > 0)
            {
                visionAndMovementRange = val;
            }
        }

        void setHerdSplitThreshold(int val)
        {
            if (val > 0)
            {
                herdSplitThreshold = val;
            }
        }

        void setHerdIdealBirthProbability(double val)
        {
            if (val > 0.0 && val <= 1.0)
            {
                herdIdealBirthProbability = val;
            }
        }

        public boolean isUseHerdingRuleBased()
        {
            return useHerdingRuleBased;
        }

        public void setUseHerdingRuleBased(boolean useHerdingRuleBased)
        {
            this.useHerdingRuleBased = useHerdingRuleBased;
        }

        public double getTLUFoodMax()
        {
            return TLUFoodMax;
        }

        public void setTLUFoodMax(double TLUFoodMax)
        {
            this.TLUFoodMax = TLUFoodMax;
        }

        public double getTLUFoodStress()
        {
            return TLUFoodStress;
        }

        public void setTLUFoodStress(double TLUFoodStress)
        {
            this.TLUFoodStress = TLUFoodStress;
        }

        public double getTLUWaterMax()
        {
            return TLUWaterMax;
        }

        public void setTLUWaterMax(double TLUWaterMax)
        {
            this.TLUWaterMax = TLUWaterMax;
        }

        public double getTLUWaterStress()
        {
            return TLUWaterStress;
        }

        public void setTLUWaterStress(double TLUWaterStress)
        {
            this.TLUWaterStress = TLUWaterStress;
        }

        public int getTLUWaterMetabolismRate()
        {
            return TLUWaterMetabolismRate;
        }

        /**
         * How much an animal consumes per day.
         *
         * Note that the maximum amount of water an animal can hold and the number
         * of days an animal can go without water before being stressed are
         * dependent on this value, so they're recalibrated accordingly.
         *
         * @param TLUWaterMetabolismRate
         */
        public void setTLUWaterMetabolismRate(int TLUWaterMetabolismRate)
        {
            this.TLUWaterMetabolismRate = TLUWaterMetabolismRate;

            TLUWaterMax = 1.25 * TLUWaterMetabolismRate * TLUMaxDaysWithoutWater;
            TLUWaterStress = 0.5 * TLUWaterMetabolismRate * TLUMaxDaysWithoutWater;
        }

        public double getHerdVegetationThreshold()
        {
            return herdVegetationThreshold;
        }

        public void setHerdVegetationThreshold(double herdVegetationThreshold)
        {
            this.herdVegetationThreshold = herdVegetationThreshold;
        }

        public double getTLUFoodConsumptionRate()
        {
            return TLUFoodConsumptionRate;
        }

        public void setTLUFoodConsumptionRate(double TLUFoodConsumptionRate)
        {
            this.TLUFoodConsumptionRate = TLUFoodConsumptionRate;
        }

        public double getHerdingFoodWeight()
        {
            return herdingFoodWeight;
        }

        public void setHerdingFoodWeight(double herdingFoodWeight)
        {
            this.herdingFoodWeight = herdingFoodWeight;
        }

        public double getHerdingWaterWeight()
        {
            return herdingWaterWeight;
        }

        public void setHerdingWaterWeight(double herdingWaterWeight)
        {
            this.herdingWaterWeight = herdingWaterWeight;
        }

        public double getHerdingDistanceWeight()
        {
            return herdingDistanceWeight;
        }

        public void setHerdingDistanceWeight(double herdingDistanceWeight)
        {
            this.herdingDistanceWeight = herdingDistanceWeight;
        }

        public double getHerdingHomeWeight()
        {
            return herdingHomeWeight;
        }

        public void setHerdingHomeWeight(double herdingHomeWeight)
        {
            this.herdingHomeWeight = herdingHomeWeight;
        }

        public int getMaxTrailLength()
        {
            return maxTrailLength;
        }

        public void setMaxTrailLength(int maxTrailLength)
        {
            this.maxTrailLength = maxTrailLength;
        }

        public double getTLUFoodMetabolismRate()
        {
            return TLUFoodMetabolismRate;
        }

        public int getTLUMaxDaysWithoutFood()
        {
            return TLUMaxDaysWithoutFood;
        }

        public int getTLUMaxDaysWithoutWater()
        {
            return TLUMaxDaysWithoutWater;
        }

        public int getVisionAndMovementRange()
        {
            return visionAndMovementRange;
        }

        public int getHerdSplitThreshold()
        {
            return herdSplitThreshold;
        }

        public double getHerdIdealBirthProbability()
        {
            return herdIdealBirthProbability;
        }
        //</editor-fold>
    }

}
