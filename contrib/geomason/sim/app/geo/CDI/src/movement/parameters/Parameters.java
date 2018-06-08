package CDI.src.movement.parameters;

//import java.awt.Color;
import java.io.File;
import java.io.IOException;
//import java.io.PrintWriter;

//import org.spiderland.Psh.booleanStack;

//import ucar.nc2.dt.point.RecordDatasetHelper;
import CDI.src.migration.parameters.Utility;
//import CDI.src.migration.util.Distributions;
import ec.util.ParameterDatabase;

/**
 * Centralizes all non-final model parameters.
 * 
 * @author Ermo Wei
 * 
 */

public class Parameters {

	public final static String[] DEFAULT_ARGS = { "-file",
			"src/CDI/dataparameters/default.param" };
	private final static String A_FILE = "-file";
	public final static String PARAMETER_FILE = "src/CDI/dataparameters/default.param";
	public final static double DEFAULT_COEFF = 0.25;

	
	// Model output control flags
    public boolean populationHistrograms;
    public boolean cellHistrograms;
    public boolean recordData = true;   //false;
    public boolean censusTracking = false;

    
	// Desirability coefficients for initialization
	public double initTempCoeff;
    public double initPortCoeff;
    public double initRiverCoeff;
	public double initElevCoeff;
    public double initSocialWeight = 10;
    public double initSocialWeightSpread = 0.5;
    public double initDesirabilityExp = 25.0;
    public int initRecalculationSkip = 10;


    // Urban agent parameters
	public double urbanTempCoeff = 0.1;
	public double urbanPortCoeff = 0.1;
	public double urbanRiverCoeff = 0.1;
	public double urbanElevCoeff = 0.1;
	public double urbanInfrastructureAvailabilityCoeff = 0.01;
	public double urbanSocialWeight = 0.1;
    public double urbanAdjacentSocialDiscount = 0.5;
	public double urbanDesExp = 3;
    public double urbanGrowthRate = 0.00;
    public double urbanMovementWill = 0.02;

    
    // Rural agent parameters
	public double ruralTempCoeff = -1.5;
	public double ruralPortCoeff = -1.5;
	public double ruralRiverCoeff = -1.5;
	public double ruralElevCoeff = -1.5;
	public double ruralInfrastructureAvailabilityCoeff = 0.01;
    public double ruralSocialWeight = 0.1;
	public double ruralAdjacentSocialDiscount = 0.5;
	public double ruralDesExp = 3;
    public double ruralGrowthRate = 0.00;
    public double ruralMovementWill = 0.02;

    
    // General agent parameters
    public int householdSize = 4;
    public double moveCost = 100;
    public double idealTemperature = 293.15;
    public double wealthMu = 10.1;
    public double wealthSigma = 0.33;
    public double wealthAdjMu = 0.05;
    public double wealthAdjSigma = 0.005;
    public double wealthLossToBirthMu = 0.8;
    public double wealthLossToBirthSigma = 0.05;
    public double recordDistance = 0;
    public boolean preventMoves = false;
    
    // are these deprecated?
    public boolean favorCloserMoves = true;
    public boolean wealthLimitsMoves = true;
    public boolean urbanitesStayInCities = false;
    public boolean ruralsSplitMoves50_50 = false;

	

    // World parameters
    public int recalSkip = 10;
    public double infrastructureDecreaseRate = 0.005;
    public double infrastructureIncreaseRate = 0.01;
    public double infrastructureDeviationRate = 0.1;
    public int initUrbanDensity = 100;
    public int densityIncrement = 5;
    public int densityIncrementInterval = 5;

    // is this deprecated?
    public boolean permafrostAffectsInfrastructure = true;      


    // Government parameters
	public double migrationTaxRate = 0.0;
	public double beginInfrastructureTax=1911.0;
	public double beginMigrationTax=2015.0;
	
	public double infrastructureBaseCost = 100.0;
	public double infrastructureCostExponent = 0.2;
	public double infrastructureMaintenanceCoefficient = 0.05;
	
	public double govPctSavings = 0;
	public double householdSubsidyThreshold = 1;
	public double householdSubsidy = moveCost*Math.log(100);
	public boolean subsidizeUrbanOnly = false;
	public boolean subsidizeRuralOnly = false;
	
	public double attachmentTime = 100;
	public double detachmentTime = 50;
	public double attachmentStrength = 0;
	
	//household tracking parameters
	public boolean trackHousehold = false;
	public double trackFromTime;
	public int trackFromCellX;
	public int trackFromCellY;
	


	// Climate data parameters
	public double tempAdjustStart = 2005;
    public double meanTempAdjust = 0.1;
    public double stdevTempAdjust = 0.1;
    public int tempRunnningAvgWindow = 12;

    public String histTempFilename;
    public int histTempFileOffset = 0;
    public String projTempFilename;
    public int projTempFileOffset = 0;

    public int seasonOverrideStart = -1;
    public int seasonOverrideDuration = -1;


	// Filenames for static data
    public String filePath;
    public String householdFilePath;
    public String censusMigrationFilePath;
    public String tempDesFile;
    public String riverDesFile;
    public String portDesFile;
    public String elevDesFile;
    public String tempRawFile; // This is the static map we originally used
    public String riverRawFile;
    public String portRawFile;
    public String elevRawFile;
    public String cultureGroupFile;
    public String landCoverFile;
    public String nppFile; // net primary productivity
    public String coastalFile;
    public String popRegionFile;
    public String initialzationPopulationFile;
    public String growthRateFile;
    public String nationsFilename;
    public String provincesFilename;
	public String populationFilename;
	public String borealAreaFilename;
	public String latFilename;
	public String lonFilename;


	// color map bounds
	public double popColorMapLowerBound;
	public double popColorMapUpperBound;
	public double tempColorMapLowerBound;
	public double tempColorMapUpperBound;
	public double tempDesColorMapLowerBound;
	public double tempDesColorMapUpperBound;
	public double riverDesColorMapLowerBound;
	public double riverDesColorMapUpperBound;
	public double portDesColorMapLowerBound;
	public double portDesColorMapUpperBound;
	public double elevDesColorMapLowerBound;
	public double elevDesColorMapUpperBound;
	public double totalDesColorMapLowerBound;
	public double totalDesColorMapUpperBound;

	

	/**
	 * Create a Parameters class for all parameters from the file
	 * 
	 * @param args
	 *            If the file is null, then it create a empty parameter database
	 *            other wise, it use the generate a parameter database use the
	 *            provided parameter file
	 */
	public Parameters(long seed, String[] args) {
		// we now use current system time as seed for the distribution
		// It used the seed from the model before

		if (args == null || args.length == 0) {
			args = DEFAULT_ARGS;
		}

		loadParameters(openParameterDatabase(args));
		checkConstraints();
	}

	/**
	 * Initialize parameter database from file
	 * 
	 * @param args
	 *            contains path to the parameter file
	 * @return newly created parameter data base
	 * 
	 */

	private static ParameterDatabase openParameterDatabase(String[] args) {
		ParameterDatabase parameters = null;
		for (int x = 0; x < args.length - 1; x++) 
		{
			System.out.println("Parameters>229> x= " + x + " args[x]= " + args[x]);
			if (args[x].equals(A_FILE)) {
				try {
					System.out.println("Parameters>232> args[x+1]= " + args[x + 1]);
					File parameterDatabaseFile = new File(args[x + 1]);
					// parameters = new
					// ParameterDatabase(parameterDatabaseFile.getAbsoluteFile());
					parameters = new ParameterDatabase(
							parameterDatabaseFile.getAbsoluteFile(), args);
				} catch (IOException ex) {
					ex.printStackTrace();
				}
				break;
			}
		}
		if (parameters == null) {
			System.out
					.println("\nNo parameter file was specified;"
							+ "\n-default programmatic settings engaged,"
							+ "\n-command line -p parameters ignored."
							+ "\nConsider using: -file foo.params [-p bar1=val1 [-p bar2=val2 ... ]]\n");
			parameters = new ParameterDatabase();
		}
		return parameters;
	}

	/**
	 * Initialize the model with various run-time parameters.
	 * 
	 * This allows for over-riding the default run-time parameters with values
	 * read from a parameter database that was ultimately initialized from a
	 * parameters file.
	 * 
	 * @param parameterDB
	 *            parameter database containing run-time parameters
	 */
	private void loadParameters(ParameterDatabase parameterDB) {

	    // Model output control flags
        recordData = Utility.returnBooleanParameter(parameterDB, "recordData", true); //false);
        populationHistrograms = Utility.returnBooleanParameter(parameterDB, "populationHistrograms", false);
        cellHistrograms = Utility.returnBooleanParameter(parameterDB, "cellHistrograms", false);

		// Desirability coefficients for initialization
		initTempCoeff = Utility.returnDoubleParameter(parameterDB, "initTempCoeff", DEFAULT_COEFF);
		initPortCoeff = Utility.returnDoubleParameter(parameterDB, "initPortCoeff", DEFAULT_COEFF);
		initRiverCoeff = Utility.returnDoubleParameter(parameterDB, "initRiverCoeff", DEFAULT_COEFF);
		initElevCoeff = Utility.returnDoubleParameter(parameterDB, "initElevCoeff", DEFAULT_COEFF);
	    initSocialWeight = Utility.returnDoubleParameter(parameterDB, "initSocialWeight", 10);
	    initSocialWeightSpread = Utility.returnDoubleParameter(parameterDB, "initSocialWeightSpread", 0.5);
	    initDesirabilityExp = Utility.returnDoubleParameter(parameterDB, "initDesirabilityExp", 25.0);

	    initRecalculationSkip = Utility.returnIntParameter(parameterDB, "initRecalculationSkip", 10);

        // Urban agent parameters
	    urbanTempCoeff = Utility.returnDoubleParameter(parameterDB, "urbanTempCoeff", 0.1);
	    urbanPortCoeff = Utility.returnDoubleParameter(parameterDB, "urbanPortCoeff", 0.1);
	    urbanRiverCoeff = Utility.returnDoubleParameter(parameterDB, "urbanRiverCoeff", 0.1);
	    urbanElevCoeff = Utility.returnDoubleParameter(parameterDB, "urbanElevCoeff", 0.1);
	    urbanInfrastructureAvailabilityCoeff = Utility.returnDoubleParameter(parameterDB, "urbanOpportunityCoeff", 0.01);
        urbanSocialWeight = Utility.returnDoubleParameter(parameterDB, "urbanSocialWeight", 0.1);
	    urbanAdjacentSocialDiscount = Utility.returnDoubleParameter( parameterDB, "urbanAdjacentSocialDiscount", 0.5);
	    urbanDesExp = Utility.returnDoubleParameter(parameterDB, "urbanDesExp", 3);

	    urbanGrowthRate = Utility.returnDoubleParameter(parameterDB, "urbanGrowthRate", 0.00);
        urbanMovementWill = Utility.returnDoubleParameter(parameterDB, "urbanMovementWill", 0.02);

        // Rural agent parameters
	    ruralTempCoeff = Utility.returnDoubleParameter(parameterDB, "ruralTempCoeff", -1.5);
	    ruralPortCoeff = Utility.returnDoubleParameter(parameterDB, "ruralPortCoeff", -1.5);
	    ruralRiverCoeff = Utility.returnDoubleParameter(parameterDB, "ruralRiverCoeff", -1.5);
	    ruralElevCoeff = Utility.returnDoubleParameter(parameterDB, "ruralElevCoeff", -1.5);
	    ruralInfrastructureAvailabilityCoeff = Utility.returnDoubleParameter(parameterDB, "ruralOpportunityCoeff", 0.01);
        ruralSocialWeight = Utility.returnDoubleParameter(parameterDB, "ruralSocialWeight", 0.1);
	    ruralAdjacentSocialDiscount = Utility.returnDoubleParameter(parameterDB, "ruralAdjacentSocialDiscount", 0.5);
	    ruralDesExp = Utility.returnDoubleParameter(parameterDB, "ruralDesExp", 3);

        ruralGrowthRate = Utility.returnDoubleParameter(parameterDB, "ruralGrowthRate", 0.00);
        ruralMovementWill = Utility.returnDoubleParameter(parameterDB, "ruralMovementWill", 0.02);

        // General agent parameters
        householdSize = Utility.returnIntParameter(parameterDB, "householdSize", 4);
        moveCost = Utility.returnDoubleParameter(parameterDB, "moveCost", 100);
        idealTemperature = Utility.returnDoubleParameter(parameterDB, "idealTemperature", 293.15);
        wealthMu = Utility.returnDoubleParameter(parameterDB, "wealthMu", 10.1);
        wealthSigma = Utility.returnDoubleParameter(parameterDB, "wealthSigma", 0.33);
        wealthAdjMu = Utility.returnDoubleParameter(parameterDB, "wealthAdjMu", 0.05);
        wealthAdjSigma = Utility.returnDoubleParameter(parameterDB, "wealthAdjSigma", 0.005);

        wealthLossToBirthMu = Utility.returnDoubleParameter(parameterDB, "wealthLossToBirthMu", 0.8);
        wealthLossToBirthSigma = Utility.returnDoubleParameter(parameterDB, "wealthLossToBirthSigma", 0.05);
        recordDistance = Utility.returnDoubleParameter(parameterDB, "recordDistance", 0);
         
		// World parameters
        recalSkip = Utility.returnIntParameter(parameterDB, "recalSkip", 10);
        infrastructureDecreaseRate = Utility.returnDoubleParameter(parameterDB, "infrastructureDecreaseRate", 0.005);
        infrastructureIncreaseRate = Utility.returnDoubleParameter(parameterDB, "infrastructureDecreaseRate", 0.01);
        infrastructureDeviationRate = Utility.returnDoubleParameter(parameterDB, "infrastructureDeviationRate", 0.1);

        initUrbanDensity = Utility.returnIntParameter(parameterDB, "initUrbanDensity", 100);
        densityIncrement = Utility.returnIntParameter(parameterDB, "densityIncrement", 5);
        densityIncrementInterval = Utility.returnIntParameter(parameterDB, "densityIncrementInterval", 5);
  
    	
    	trackFromTime = Utility.returnDoubleParameter(parameterDB, "trackFromTime", 1911);
    	trackFromCellX = Utility.returnIntParameter(parameterDB, "trackFromCellX", 0);
    	trackFromCellY = Utility.returnIntParameter(parameterDB, "trackFromCellY", 0);

        // Government parameters
        govPctSavings = Utility.returnDoubleParameter(parameterDB, "govPctSavings", 0);
        householdSubsidy = Utility.returnDoubleParameter(parameterDB, "householdSubsidy", moveCost*Math.log(100));
        householdSubsidyThreshold = Utility.returnDoubleParameter(parameterDB, "householdSubsidyThreshold", 1);
        migrationTaxRate = Utility.returnDoubleParameter(parameterDB, "migrationTaxRate", 0.0);
        beginInfrastructureTax=Utility.returnDoubleParameter(parameterDB, "beginInfrastructureTax", 1911.0);
        beginMigrationTax=Utility.returnDoubleParameter(parameterDB, "beginMigrationTax", 2015.0);
        infrastructureBaseCost = Utility.returnDoubleParameter(parameterDB, "infrastructureBaseCost", 100.0);
        infrastructureCostExponent = Utility.returnDoubleParameter(parameterDB, "infrastructureCostExponent", 0.15);
        infrastructureMaintenanceCoefficient = Utility.returnDoubleParameter(parameterDB, "infrastructureMaintenanceCoefficient", 0.05);


		// Climate data parameters
        histTempFilename = Utility.returnStringParameter(parameterDB, "histTempFilename", null);
        histTempFileOffset = Utility.returnIntParameter(parameterDB, "histTempFileOffset", histTempFileOffset);
        projTempFilename = Utility.returnStringParameter(parameterDB, "projTempFilename", null);
        projTempFileOffset = Utility.returnIntParameter(parameterDB, "projTempFileOffset", projTempFileOffset);

        seasonOverrideStart = Utility.returnIntParameter(parameterDB, "seasonOverrideStart", seasonOverrideStart);
        seasonOverrideDuration = Utility.returnIntParameter(parameterDB, "seasonOverrideDuration", seasonOverrideDuration);

        tempAdjustStart = Utility.returnDoubleParameter(parameterDB, "tempAdjustStart", 2005);
        meanTempAdjust = Utility.returnDoubleParameter(parameterDB, "meanTempAdjust", 0.1);
        stdevTempAdjust = Utility.returnDoubleParameter(parameterDB, "stdevTempAdjust", 0.1);
        tempRunnningAvgWindow = Utility.returnIntParameter(parameterDB, "tempRunnningAvgWindow", 1);


        // Filenames for static data
        filePath = Utility.returnStringParameter(parameterDB, "filePath", null);
        householdFilePath = Utility.returnStringParameter(parameterDB, "householdFilePath", null);
        censusMigrationFilePath = Utility.returnStringParameter(parameterDB, "censusMigrationFilePath", "migration");
        tempDesFile = Utility.returnStringParameter(parameterDB, "tempDesFile", null);
    	riverDesFile = Utility.returnStringParameter(parameterDB, "riverDesFile", null);
    	portDesFile = Utility.returnStringParameter(parameterDB, "portDesFile", null);
    	elevDesFile = Utility.returnStringParameter(parameterDB, "elevDesFile", null);
    	riverRawFile = Utility.returnStringParameter(parameterDB, "riverRawFile", null);
    	portRawFile = Utility.returnStringParameter(parameterDB, "portRawFile", null);
    	elevRawFile = Utility.returnStringParameter(parameterDB, "elevRawFile", null);
        tempRawFile = Utility.returnStringParameter(parameterDB, "tempRawFile", null);
    	cultureGroupFile = Utility.returnStringParameter(parameterDB, "cultureGroupFile", null);
    	landCoverFile = Utility.returnStringParameter(parameterDB, "landCoverFile", null);
    	nppFile = Utility.returnStringParameter(parameterDB, "nppFile", null); // Net primary productivity, a bit like NDVI
    	coastalFile = Utility.returnStringParameter(parameterDB,"coastalFile",null);
    	popRegionFile = Utility.returnStringParameter(parameterDB, "popRegionFile", null);
    	initialzationPopulationFile = Utility.returnStringParameter(parameterDB, "initialzationPopulationFile", null);
        growthRateFile = Utility.returnStringParameter(parameterDB, "growthRateFile", null);
        nationsFilename = Utility.returnStringParameter(parameterDB, "nationsFilename", null);
        provincesFilename = Utility.returnStringParameter(parameterDB, "provincesFilename", null);
        populationFilename = Utility.returnStringParameter(parameterDB, "populationFilename", null);
        borealAreaFilename = Utility.returnStringParameter(parameterDB, "borealAreaFilename", null);
        latFilename = Utility.returnStringParameter(parameterDB, "latFilename", null);
        lonFilename = Utility.returnStringParameter(parameterDB, "lonFilename", null);
    	
    	// Color Map Bounds
    	popColorMapLowerBound =  Utility.returnDoubleParameter(parameterDB, "popColorMapLowerBound", 0.0);
    	popColorMapUpperBound = Utility.returnDoubleParameter(parameterDB, "popColorMapUpperBound", 0.0);
    	
    	tempColorMapLowerBound = Utility.returnDoubleParameter(parameterDB, "tempColorMapLowerBound", 0.0);
    	tempColorMapUpperBound = Utility.returnDoubleParameter(parameterDB, "tempColorMapUpperBound", 0.0);
    	
    	tempDesColorMapLowerBound = Utility.returnDoubleParameter(parameterDB, "tempDesColorMapLowerBound", 0.0);
    	tempDesColorMapUpperBound = Utility.returnDoubleParameter(parameterDB, "tempDesColorMapUpperBound", 0.0);
    	
    	riverDesColorMapLowerBound = Utility.returnDoubleParameter(parameterDB, "riverDesColorMapLowerBound", 0.0);
    	riverDesColorMapUpperBound = Utility.returnDoubleParameter(parameterDB, "riverDesColorMapUpperBound", 0.0);
    	
    	portDesColorMapLowerBound = Utility.returnDoubleParameter(parameterDB, "portDesColorMapLowerBound", 0.0);
    	portDesColorMapUpperBound = Utility.returnDoubleParameter(parameterDB, "portDesColorMapUpperBound", 0.0);
    	
    	elevDesColorMapLowerBound = Utility.returnDoubleParameter(parameterDB, "elevDesColorMapLowerBound", 0.0);
    	elevDesColorMapUpperBound = Utility.returnDoubleParameter(parameterDB, "elevDesColorMapUpperBound", 0.0);
    	
    	totalDesColorMapLowerBound = Utility.returnDoubleParameter(parameterDB, "totalDesColorMapLowerBound", 0.0);
    	totalDesColorMapUpperBound = Utility.returnDoubleParameter(parameterDB, "totalDesColorMapUpperBound", 0.0);
    	
		attachmentTime = Utility.returnDoubleParameter(parameterDB, "attachmentTime", 100);
		detachmentTime = Utility.returnDoubleParameter(parameterDB, "detachmentTime", 50);
		attachmentStrength = Utility.returnDoubleParameter(parameterDB, "attachmentStrength", 0);



		// System.err.println("-------------------------");
		// parameterDB.listGotten(new PrintWriter(System.err));
	}

	/**
	 * Check that the parameters do not take on invalid values. If they do,
	 * throw an IllegalStateException.
	 */
	private void checkConstraints() {
		// don't know what needs to be check here

		// need to check if the coefficients in the certain range
	}

	public static void main(String[] args) {
		Parameters parameters = new Parameters(System.currentTimeMillis(),
				Parameters.DEFAULT_ARGS);

	}

}
