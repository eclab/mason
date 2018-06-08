package CDI.src.migration.parameters;

import java.awt.Color;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import ucar.nc2.dt.point.RecordDatasetHelper;
import CDI.src.migration.util.Distributions;
import ec.util.ParameterDatabase;

/**
 * Centralizes all non-final model parameters.
 * 
 * @author Ermo Wei
 * 
 */

public class Parameters 
{
    public final static String[] DEFAULT_ARGS = {"-file", "src/CDI/dataparameters/default.param"};
    private final static String A_FILE = "-file";
    public final static String PARAMETER_FILE = "src/CDI/dataparameters/default.param";
    public final static double DEFAULT_COEFF = 0.25;

    // desirability coefficients
    public double tempCoeff;
    public double elevCoeff;
    public double portCoeff;
    public double riverCoeff;

    // phase2 parameters
    public double socialWeight = 10;
    public double socialWeightSpread = 0.01;
    public double desExp = 3.0;
    public int popSize = 3000000;
    public int recalculationSkip = 10000;

    // phase4 parameters
    public boolean recordData = true;   //false;

    public double meanTempAdjust = 0.1;
    public double stdevTempAdjust = 0.1;
    public int tempRunnningAvgWindow = 1;

    public double urbanTempCoeff = 0.1;
    public double urbanPortCoeff = 0.1;
    public double urbanRiverCoeff = 0.1;
    public double urbanElevCoeff = 0.1;
    public double urbanOpportunityCoeff = 0.01;
    public double urbanAdjacentSocialDiscount = 0.5;
    public double urbanGrowthRate = 0.00;
    public double urbanMovementWill = 0.02;
    public double urbanSocialWeight = 0.1;
    public double urbanDesExp = 3;

    public double ruralTempCoeff = -1.5;
    public double ruralPortCoeff = -1.5;
    public double ruralRiverCoeff = -1.5;
    public double ruralElevCoeff = -1.5;
    public double ruralOpportunityCoeff = 0.01;
    public double ruralAdjacentSocialDiscount = 0.5;
    public double ruralGrowthRate = 0.00;
    public double ruralMovementWill = 0.02;
    public double ruralSocialWeight = 0.1;
    public double ruralDesExp = 3;

    public double infrastructureDecreaseRate = 0.005;
    public double infrastructureIncreaseRate = 0.01;
    public double infrastructureDeviationRate = 0.1;

    public double initSocialWeight = 10;
    public double initSocialWeightSpread = 0.5;
    public double initDesirabilityExp = 25.0;
    public int initRecalculationSkip = 10;

    public int recalSkip = 10;
    public int householdSize = 4;

    public int initUrbanDensity = 100;
    public int densityIncrement = 5;
    public int densityIncrementInterval = 5;

    public double wealthMu = 1;
    public double wealthSigma = 1;
    public double wealthAdjMu = 0.01;
    public double wealthAdjSigma = 0.01;
    public double moveCost = 1;
    public double wealthLossToBirthMu = 0.9;
    public double wealthLossToBirthSigma = 0.05;
    public double wealthAtBirthMu = 1;
    public double wealthAtBirthSigma = 0.1;

    public boolean favorCloserMoves = true;

    public boolean wealthLimitsMoves = true;
    public boolean urbanitesStayInCities = false;
    public boolean ruralsSplitMoves50_50 = false;
    public boolean permafrostAffectsInfrastructure = false;

    // file path
    public String nationsFilename;
    public String populationFilename;
    public String borealAreaFilename;
    public String latFilename;
    public String lonFilename;
    public String temperatureFilename;
    public String temperatureFilename2;
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
    public String initialzationPopulationFile;
    public String popReigonFile;

    public String filePath;

    public int temperatureFileOffset = 0;
    public int temperatureFile2Offset = 0;

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

    // GUI parameters
    public Color cityColor;
    public double maxCityPortrayalScale;
    public double minCityPortrayalScale;
    public Color resourceColor;
    public double maxResourcePortrayalScale;
    public double minResourcePortrayalScale;
    public Color rebelResourceColor;

	/**
	 * Create a Parameters class for all parameters from the file
	 * 
	 * @param args
	 *            If the file is null, then it create a empty parameter database
	 *            other wise, it use the generate a parameter database use the
	 *            provided parameter file
	 */
	public Parameters(long seed, String[] args) 
	{
		// we now use current system time as seed for the distribution
		// It used the seed from the model before
		System.out.println("Parameters> " + args[0] + ", " + args[1]);
		if (args == null || args.length == 0) { args = DEFAULT_ARGS; }
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

	private static ParameterDatabase openParameterDatabase(String[] args) 
	{
		ParameterDatabase parameters = null;
		System.out.println("openParameterDatabase>parameter file: " + args[0] + ">" + args[1]);
		for (int x = 0; x < args.length - 1; x++) 
		{
			if (args[x].equals(A_FILE)) 
			{
				try 
				{
					File parameterDatabaseFile = new File(args[x + 1]);
					// parameters = new
					// ParameterDatabase(parameterDatabaseFile.getAbsoluteFile());
					parameters = new ParameterDatabase(
							parameterDatabaseFile.getAbsoluteFile(), args);
				} 
				catch (IOException ex) { ex.printStackTrace(); }
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
	private void loadParameters(ParameterDatabase parameterDB) 
	{
		// GUI Parameters
		// It seems we don't currently read any GUI Parameters from the
		// parameter file.

		// Desirability coefficient
		tempCoeff = Utility.returnDoubleParameter(parameterDB, "tempCoeff", DEFAULT_COEFF);
		portCoeff = Utility.returnDoubleParameter(parameterDB, "portCoeff", DEFAULT_COEFF);
		riverCoeff = Utility.returnDoubleParameter(parameterDB, "riverCoeff", DEFAULT_COEFF);
		elevCoeff = Utility.returnDoubleParameter(parameterDB, "elevCoeff", DEFAULT_COEFF);

		// Phase 2 parameters
		socialWeight = Utility.returnDoubleParameter(parameterDB, "socialWeight", socialWeight);
		socialWeightSpread = Utility.returnDoubleParameter(parameterDB, "socialWeightSpread", socialWeightSpread);
		desExp = Utility.returnDoubleParameter(parameterDB, "desExp", desExp);
		popSize = Utility.returnIntParameter(parameterDB, "popSize", popSize);
		recalculationSkip = Utility.returnIntParameter(parameterDB, "recalculationSkip", recalculationSkip);

		// Phase4 parameters
		recordData = Utility.returnBooleanParameter(parameterDB, "recordData", true);  // was false

        temperatureFilename = Utility.returnStringParameter(parameterDB, "temperatureFilename", null);
        temperatureFileOffset = Utility.returnIntParameter(parameterDB, "temperatureFileOffset", temperatureFileOffset);
        temperatureFilename2 = Utility.returnStringParameter(parameterDB, "temperatureFilename2", null);
        temperatureFile2Offset = Utility.returnIntParameter(parameterDB, "temperatureFile2Offset", temperatureFile2Offset);
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
    	nppFile = Utility.returnStringParameter(parameterDB, "nppFile", null);
    	coastalFile = Utility.returnStringParameter(parameterDB,"coastalFile",null);
    	popReigonFile = Utility.returnStringParameter(parameterDB, "popRegionFile", null);
    	initialzationPopulationFile = Utility.returnStringParameter(parameterDB, "initialzationPopulationFile", null);
    	filePath = Utility.returnStringParameter(parameterDB, "filePath", null);  	
    	
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
    	
    	// Other GUI parameter
    	cityColor = Color.decode(Utility.returnStringParameter(parameterDB, "cityColor", null));
    	maxCityPortrayalScale = Utility.returnDoubleParameter(parameterDB, "maxCityPortrayalScale", 0.0);
    	minCityPortrayalScale = Utility.returnDoubleParameter(parameterDB, "minCityPortrayalScale", 0.0);
    	resourceColor = Color.decode(Utility.returnStringParameter(parameterDB, "resourceColor", null));
    	maxResourcePortrayalScale = Utility.returnDoubleParameter(parameterDB, "maxResourcePortrayalScale", 0.0);
    	minResourcePortrayalScale = Utility.returnDoubleParameter(parameterDB, "minResourcePortrayalScale", 0.0);
    	rebelResourceColor = Color.decode(Utility.returnStringParameter(parameterDB, "rebelResourceColor", null));

		meanTempAdjust = Utility.returnDoubleParameter(parameterDB, "meanTempAdjust", 0.1);
		stdevTempAdjust = Utility.returnDoubleParameter(parameterDB, "stdevTempAdjust", 0.1);
		tempRunnningAvgWindow = Utility.returnIntParameter(parameterDB, "tempRunnningAvgWindow", 1);

		urbanTempCoeff = Utility.returnDoubleParameter(parameterDB, "urbanTempCoeff", 0.1);
		urbanPortCoeff = Utility.returnDoubleParameter(parameterDB, "urbanPortCoeff", 0.1);
		urbanRiverCoeff = Utility.returnDoubleParameter(parameterDB, "urbanRiverCoeff", 0.1);
		urbanElevCoeff = Utility.returnDoubleParameter(parameterDB, "urbanElevCoeff", 0.1);
		urbanOpportunityCoeff = Utility.returnDoubleParameter(parameterDB, "urbanOpportunityCoeff", 0.01);
		urbanAdjacentSocialDiscount = Utility.returnDoubleParameter(parameterDB, "urbanAdjacentSocialDiscount", 0.5);
		urbanGrowthRate = Utility.returnDoubleParameter(parameterDB, "urbanGrowthRate", 0.00);
		urbanMovementWill = Utility.returnDoubleParameter(parameterDB, "urbanMovementWill", 0.02);
		urbanSocialWeight = Utility.returnDoubleParameter(parameterDB, "urbanSocialWeight", 0.1);
		urbanDesExp = Utility.returnDoubleParameter(parameterDB, "urbanDesExp", 3);

		ruralTempCoeff = Utility.returnDoubleParameter(parameterDB, "ruralTempCoeff", -1.5);
		ruralPortCoeff = Utility.returnDoubleParameter(parameterDB, "ruralPortCoeff", -1.5);
		ruralRiverCoeff = Utility.returnDoubleParameter(parameterDB, "ruralRiverCoeff", -1.5);
		ruralElevCoeff = Utility.returnDoubleParameter(parameterDB, "ruralElevCoeff", -1.5);
		ruralOpportunityCoeff = Utility.returnDoubleParameter(parameterDB, "ruralOpportunityCoeff", 0.01);
		ruralAdjacentSocialDiscount = Utility.returnDoubleParameter( parameterDB, "ruralAdjacentSocialDiscount", 0.5);
		ruralGrowthRate = Utility.returnDoubleParameter(parameterDB, "ruralGrowthRate", 0.00);
		ruralMovementWill = Utility.returnDoubleParameter(parameterDB, "ruralMovementWill", 0.02);
		ruralSocialWeight = Utility.returnDoubleParameter(parameterDB, "ruralSocialWeight", 0.1);
		ruralDesExp = Utility.returnDoubleParameter(parameterDB, "ruralDesExp",3);

		infrastructureDecreaseRate = Utility.returnDoubleParameter(parameterDB, "infrastructureDecreaseRate", 0.005);
		infrastructureIncreaseRate = Utility.returnDoubleParameter(parameterDB, "infrastructureDecreaseRate", 0.01);
		infrastructureDeviationRate = Utility.returnDoubleParameter(parameterDB, "infrastructureDeviationRate", 0.1);

		initSocialWeight = Utility.returnDoubleParameter(parameterDB, "initSocialWeight", 10);
		initSocialWeightSpread = Utility.returnDoubleParameter(parameterDB, "initSocialWeightSpread", 0.5);
		initDesirabilityExp = Utility.returnDoubleParameter(parameterDB, "initDesirabilityExp", 25.0);
		initRecalculationSkip = Utility.returnIntParameter(parameterDB, "initRecalculationSkip", 10);

		recalSkip = Utility.returnIntParameter(parameterDB, "recalSkip", 10);
		householdSize = Utility.returnIntParameter(parameterDB, "householdSize", 4);

		initUrbanDensity = Utility.returnIntParameter(parameterDB, "initUrbanDensity", 100);
		densityIncrement = Utility.returnIntParameter(parameterDB, "densityIncrement", 5);
		densityIncrementInterval = Utility.returnIntParameter(parameterDB, "densityIncrementInterval", 5);

		wealthMu = Utility.returnDoubleParameter(parameterDB, "wealthMu", 1);
		wealthSigma = Utility.returnDoubleParameter(parameterDB, "wealthSigma", 1);
		wealthAdjMu = Utility.returnDoubleParameter(parameterDB, "wealthAdjMu", 0.01);
		wealthAdjSigma = Utility.returnDoubleParameter(parameterDB, "wealthAdjSigma", 0.01);

		wealthLossToBirthMu = Utility.returnDoubleParameter(parameterDB, "wealthLossToBirthMu", 0.9);
		wealthLossToBirthSigma = Utility.returnDoubleParameter(parameterDB, "wealthLossToBirthSigma", 0.05);
		wealthAtBirthMu = Utility.returnDoubleParameter(parameterDB, "wealthAtBirthMu", 1);
		wealthAtBirthSigma = Utility.returnDoubleParameter(parameterDB, "wealthLossToBirthMu", 0.1);

		moveCost = Utility.returnDoubleParameter(parameterDB, "moveCost", 1);

		// Path Parameters
		nationsFilename = Utility.returnStringParameter(parameterDB, "nationsFilename", null);
		populationFilename = Utility.returnStringParameter(parameterDB, "populationFilename", null);
		borealAreaFilename = Utility.returnStringParameter(parameterDB, "borealAreaFilename", null);
		latFilename = Utility.returnStringParameter(parameterDB, "latFilename", null);
		lonFilename = Utility.returnStringParameter(parameterDB, "lonFilename", null);

		temperatureFilename = Utility.returnStringParameter(parameterDB, "temperatureFilename", null);
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
		nppFile = Utility.returnStringParameter(parameterDB, "nppFile", null);
		coastalFile = Utility.returnStringParameter(parameterDB, "coastalFile", null);
		popReigonFile = Utility.returnStringParameter(parameterDB, "popRegionFile", null);
		initialzationPopulationFile = Utility.returnStringParameter(parameterDB, "initialzationPopulationFile", null);
		filePath = Utility.returnStringParameter(parameterDB, "filePath", null);

		// Color Map Bounds
		popColorMapLowerBound = Utility.returnDoubleParameter(parameterDB, "popColorMapLowerBound", 0.0);
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

		// Other GUI parameter
		cityColor = Color.decode(Utility.returnStringParameter(parameterDB, "cityColor", null));
		maxCityPortrayalScale = Utility.returnDoubleParameter(parameterDB, "maxCityPortrayalScale", 0.0);
		minCityPortrayalScale = Utility.returnDoubleParameter(parameterDB, "minCityPortrayalScale", 0.0);
		resourceColor = Color.decode(Utility.returnStringParameter(parameterDB, "resourceColor", null));
		maxResourcePortrayalScale = Utility.returnDoubleParameter(parameterDB, "maxResourcePortrayalScale", 0.0);
		minResourcePortrayalScale = Utility.returnDoubleParameter(parameterDB, "minResourcePortrayalScale", 0.0);
		rebelResourceColor = Color.decode(Utility.returnStringParameter(parameterDB, "rebelResourceColor", null));

		// System.err.println("-------------------------");
		// parameterDB.listGotten(new PrintWriter(System.err));
	}

	/**
	 * Check that the parameters do not take on invalid values. If they do,
	 * throw an IllegalStateException.
	 */
	private void checkConstraints() 
	{
		// don't know what needs to be check here
		// need to check if the coefficients in the certain range
	}

	public static void main(String[] args) 
	{	
		System.out.println("Parameters>main> " + args[0] + ", " + args[1]);
		Parameters parameters = new Parameters(System.currentTimeMillis(), Parameters.DEFAULT_ARGS); 
	}
	
}
