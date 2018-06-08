package CDI.src.movement;

import java.awt.Color;
import java.awt.FileDialog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import sim.portrayal.simple.OrientedPortrayal2D;

import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;

import CDI.src.migration.util.Distributions;
import CDI.src.movement.data.BarChartFactor;
import CDI.src.movement.data.BarChartFactor.Factor;
import CDI.src.movement.data.DataCollector;
import CDI.src.movement.parameters.Parameters;
import sim.engine.MakesSimState;
import sim.engine.SimState;
import sim.field.grid.DenseGrid2D;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.IntGrid2D;
import sim.field.grid.SparseGrid2D;
import sim.util.Interval;
import CDI.src.util.MersenneTwisterFastApache;
import ec.util.MersenneTwisterFast;
import CDI.src.environment.Cell;
import CDI.src.environment.GrowthRateTable;
import CDI.src.environment.Map;
import CDI.src.environment.MegaCellSign;
import CDI.src.government.govAgent;

import java.util.Map.Entry;

public class NorthLandsMovement extends SimState {

	private static final long serialVersionUID = 1L;

	private GrowthRateTable growthTable = null;

	public Parameters parameters;
	public Map map;
	public static Distributions distributions;

	public DoubleGrid2D smoothedTargetPopGrid; // smoothed numHouseholds grid
	public DoubleGrid2D zscoreSmoothedTargetPopGrid; // A z-score version of the
														// smoothed
														// numHouseholds

	public SparseGrid2D householdSelectionGrid = new SparseGrid2D(
			Map.GRID_WIDTH, Map.GRID_HEIGHT);
	public SparseGrid2D householdTrailGrid = new SparseGrid2D(Map.GRID_WIDTH,
			Map.GRID_HEIGHT);

	protected final static double COEFF_LOWER_BOUND = -10.0;
	protected final static double COEFF_UPPER_BOUND = 10.0;

	public DataCollector collector;
	public WorldAgent worldAgent;
	public govAgent federalGovAgent;
	public double[] selectionProb = new double[] { 0.75, 0.25 };
	public double fitness = 0;
	// public World worldAgent; // for doing clean up stuff when all the agents
	// have been scheduled

	
	// This grid is only for the visualization of the numHouseholds in each cell
	public DoubleGrid2D ruralResidenceGrid = new DoubleGrid2D(Map.GRID_WIDTH,
			Map.GRID_HEIGHT, Double.NEGATIVE_INFINITY);
	public DoubleGrid2D urbanResidenceGrid = new DoubleGrid2D(Map.GRID_WIDTH,
			Map.GRID_HEIGHT, Double.NEGATIVE_INFINITY);
	public DoubleGrid2D ruralDesirabilityGrid = new DoubleGrid2D(
			Map.GRID_WIDTH, Map.GRID_HEIGHT, Double.NEGATIVE_INFINITY);
	public DoubleGrid2D urbanDesirabilityGrid = new DoubleGrid2D(
			Map.GRID_WIDTH, Map.GRID_HEIGHT, Double.NEGATIVE_INFINITY);

	public DoubleGrid2D infrastructureGrid = new DoubleGrid2D(Map.GRID_WIDTH,
			Map.GRID_HEIGHT, Double.NEGATIVE_INFINITY);
	public DoubleGrid2D infrastructureAvailabilityGrid = new DoubleGrid2D(
			Map.GRID_WIDTH, Map.GRID_HEIGHT, 0);

	public DoubleGrid2D urbanSocialWeightGrid = new DoubleGrid2D(
			Map.GRID_WIDTH, Map.GRID_HEIGHT);
	public DoubleGrid2D ruralSocialWeightGrid = new DoubleGrid2D(
			Map.GRID_WIDTH, Map.GRID_HEIGHT);
	public DoubleGrid2D urbanAdjacentSocialWeightGrid = new DoubleGrid2D(
			Map.GRID_WIDTH, Map.GRID_HEIGHT);
	public DoubleGrid2D ruralAdjacentSocialWeightGrid = new DoubleGrid2D(
			Map.GRID_WIDTH, Map.GRID_HEIGHT);

	public DoubleGrid2D urbanRouletteWheelGrid = new DoubleGrid2D(
			Map.GRID_WIDTH, Map.GRID_HEIGHT, 0);
	public DoubleGrid2D ruralRouletteWheelGrid = new DoubleGrid2D(
			Map.GRID_WIDTH, Map.GRID_HEIGHT, 0);
    public DoubleGrid2D censusPopChangeGrid = new DoubleGrid2D(
            Map.GRID_WIDTH, Map.GRID_HEIGHT, 0);
    public DoubleGrid2D cumulativePopChangeGrid = new DoubleGrid2D(
            Map.GRID_WIDTH, Map.GRID_HEIGHT, 0);

	public DoubleGrid2D populationChangeGrid = new DoubleGrid2D(Map.GRID_WIDTH,
			Map.GRID_HEIGHT, 0);
	public DoubleGrid2D populationBufferGrid = new DoubleGrid2D(Map.GRID_WIDTH,
			Map.GRID_HEIGHT, 0);

	public double[] infrastructureDataBounds = new double[] {
			Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY };
	public double[] residenceDataBounds = new double[] {
			Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY };
	public double[] desDataBounds = new double[] { Double.POSITIVE_INFINITY,
			Double.NEGATIVE_INFINITY };
	public double[] socialBounds = new double[] { Double.POSITIVE_INFINITY,
			Double.NEGATIVE_INFINITY };
	public double[] rouletteBounds = new double[] { Double.POSITIVE_INFINITY,
			Double.NEGATIVE_INFINITY };
	public double[] infrastructureAvailabilityDataBounds = new double[] {
			Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY };

	public final int numProvinces = 14;
	
	// These match the numbers in the province layer minus 1
    String provinceNames[] = {"Canada", "Alberta", "Nunavut", "Quebec",
            "Nova Scotia", "Yukon",  "New Brunswick", "Northwest Territories",
            "Manitoba", "British Columbia", "Prince Edward Island",
            "Newfoundland & Labrador", "Saskatchewan", "Ontario"};

    public int[] provinceNetMigration = new int[numProvinces];


	// these three properties should not read from parameter file
	public int urbanResidence = 0;
	public int ruralResidence = 0;
	public int urbanDensity = 0;

	public int ruralHouseholdsMovedThisStep = 0;
	public int urbanHouseholdsMovedThisStep = 0;

	public double getInitTempCoeff() {
		return parameters.initTempCoeff;
	}

	public void setInitTempCoeff(double val) {
		if (COEFF_LOWER_BOUND <= val && val <= COEFF_UPPER_BOUND)
			parameters.initTempCoeff = val;
	}

	public Object domInitTempCoeff() {
		return new Interval(COEFF_LOWER_BOUND, COEFF_UPPER_BOUND);
	}

	public double getInitPortCoeff() {
		return parameters.initPortCoeff;
	}

	public void setInitPortCoeff(double val) {
		if (COEFF_LOWER_BOUND <= val && val <= COEFF_UPPER_BOUND)
			parameters.initPortCoeff = val;
	}

	public Object domInitPortCoeff() {
		return new Interval(COEFF_LOWER_BOUND, COEFF_UPPER_BOUND);
	}

	public double getInitRiverCoeff() {
		return parameters.initRiverCoeff;
	}

	public void setInitRiverCoeff(double val) {
		if (COEFF_LOWER_BOUND <= val && val <= COEFF_UPPER_BOUND)
			parameters.initRiverCoeff = val;
	}

	public Object domInitRiverCoeff() {
		return new Interval(COEFF_LOWER_BOUND, COEFF_UPPER_BOUND);
	}

	public double getInitElevCoeff() {
		return parameters.initElevCoeff;
	}

	public void setInitElevCoeff(double val) {
		if (COEFF_LOWER_BOUND <= val && val <= COEFF_UPPER_BOUND)
			parameters.initElevCoeff = val;
	}

	public Object domInitElevCoeff() {
		return new Interval(COEFF_LOWER_BOUND, COEFF_UPPER_BOUND);
	}

	public double getInitSocialWeight() {
		return parameters.initSocialWeight;
	}

	public void setInitSocialWeight(double initSocialWeight) {
		parameters.initSocialWeight = initSocialWeight;
	}

	public double getInitSocialWeightSpread() {
		return parameters.initSocialWeightSpread;
	}

	public void setInitSocialWeightSpread(double initSocialWeightSpread) {
		parameters.initSocialWeightSpread = initSocialWeightSpread;
	}

	public double getInitDesirabilityExp() {
		return parameters.initDesirabilityExp;
	}

	public void setInitDesirabilityExp(double initDesirabilityExp) {
		parameters.initDesirabilityExp = initDesirabilityExp;
	}

	public int getInitRecalculationSkip() {
		return parameters.initRecalculationSkip;
	}

	public void setInitRecalculationSkip(int initRecalculationSkip) {
		parameters.initRecalculationSkip = initRecalculationSkip;
	}

	public double getMeanTempAdjust() {
		return parameters.meanTempAdjust;
	}

	public void setMeanTempAdjust(double meanTempAdjust) {
		parameters.meanTempAdjust = meanTempAdjust;
	}

	public double getStdevTempAdjust() {
		return parameters.stdevTempAdjust;
	}

	public void setStdevTempAdjust(double stdevTempAdjust) {
		parameters.stdevTempAdjust = stdevTempAdjust;
	}

	public int getTempRunnningAvgWindow() {
		return parameters.tempRunnningAvgWindow;
	}

	public void setTempRunnningAvgWindow(int tempRunnningAvgWindow) {
		parameters.tempRunnningAvgWindow = tempRunnningAvgWindow;
	}

	public int getUrbanDensity() {
		return urbanDensity;
	}

	public void setUrbanDensity(int UrbanDensity) {
		this.urbanDensity = UrbanDensity;
	}

	public int getDensityIncrement() {
		return parameters.densityIncrement;
	}

	public void setDensityIncrement(int densityIncrement) {
		parameters.densityIncrement = densityIncrement;
	}

	public int getDensityIncrementInterval() {
		return parameters.densityIncrementInterval;
	}

	public void setDensityIncrementInterval(int densityIncrementInterval) {
		parameters.densityIncrementInterval = densityIncrementInterval;
	}

	public double getUrbanGrowthRate() {
		return growthTable.getUrbanGrowthRateForDate(schedule.getTime(),
				parameters.urbanGrowthRate);
	}

	public double getRuralGrowthRate() {
		return growthTable.getRuralGrowthRateForDate(schedule.getTime(),
				parameters.ruralGrowthRate);
	}

	// Household is directly connect with cell, for a given coordination (x,y),
	// there is only one cell, please see the cellGrid in map
	public ArrayList<Household> households;

	public void addNewHousehold(Household h) {
		this.households.add(h);
	}

	public int getRecalculationSkip() {
		return parameters.recalSkip;
	}

	public void setRecalculationSkip(int recalculationSkip) {
		parameters.recalSkip = recalculationSkip;
	}

	public int getHouseholdSize() {
		return parameters.householdSize;
	}

	public void setHouseholdSize(int val) {
		parameters.householdSize = val;
	}

	public double getInfrastructureDecreaseRate() {
		return parameters.infrastructureDecreaseRate;
	}

	public void setInfrastructureDecreaseRate(double infrastructureDecreaseRate) {
		parameters.infrastructureDecreaseRate = infrastructureDecreaseRate;
	}

	public double getInfrastructureIncreaseRate() {
		return parameters.infrastructureIncreaseRate;
	}

	public void setInfrastructureIncreaseRate(double infrastructureIncreaseRate) {
		parameters.infrastructureIncreaseRate = infrastructureIncreaseRate;
	}

	public boolean isRecordData() {
		return parameters.recordData;
	}

	public void setRecordData(boolean recordData) {
		parameters.recordData = recordData;
	}

	public double getFitness() {
		return fitness;
	}

	public double getInfrastructureDeviationRate() {
		return parameters.infrastructureDeviationRate;
	}

	public void setInfrastructureDeviationRate(double val) {
		parameters.infrastructureDeviationRate = val;
	}

	public boolean getFavorCloserMoves() {
		return parameters.favorCloserMoves;
	}

	public void setFavorCloserMoves(boolean val) {
		parameters.favorCloserMoves = val;
	}

	public boolean getWealthLimitsMoves() {
		return parameters.wealthLimitsMoves;
	}

	public void setWealthLimitsMoves(boolean val) {
		parameters.wealthLimitsMoves = val;
	}

	public boolean getPermafrostAffectsInfrastructure() {
        return parameters.permafrostAffectsInfrastructure;
    }
    public void setPermafrostAffectsInfrastructure(boolean val) {
        parameters.permafrostAffectsInfrastructure = val;
    }
    
    public boolean getPreventMoves() {
        return parameters.preventMoves;
    }
    public void setPreventMoves(boolean val) {
        parameters.preventMoves = val;
    }
    
    public double getMoveCost() {
        return parameters.moveCost;
    }
    public void setMoveCost(double val) {
        parameters.moveCost = val;
    }
    
    public double getMigrationTaxRate() {
        return parameters.migrationTaxRate;
    }
    public void setMigrationTaxRate(double val) {
        parameters.migrationTaxRate = val;
    }
    
    public double getBeginInfrastructureTax() {
        return parameters.beginInfrastructureTax;
    }
    public void setBeginInfrastructureTax(double val) {
        parameters.beginInfrastructureTax = val;
    }
    
    public double getBeginMigrationTax() {
        return parameters.beginMigrationTax;
    }
    public void setBeginMigrationTax(double val) {
        parameters.beginMigrationTax = val;
    }
    
    public double getHouseholdSubsidy() {
        return parameters.householdSubsidy;
    }
    public void setHouseholdSubsidy(double val) {
        parameters.householdSubsidy = val;
    }
    
    public double getHouseholdSubsidyThreshold() {
        return parameters.householdSubsidyThreshold;
    }
    public void setHouseholdSubsidyThreshold(double val) {
        parameters.householdSubsidyThreshold = val;
    }
    
    public boolean getSubsidizeUrbanOnly() {
        return parameters.subsidizeUrbanOnly;
    }
    public void setSubsidizeUrbanOnly(boolean val) {
        parameters.subsidizeUrbanOnly = val;
    }
    
    public boolean getSubsidizeRuralOnly() {
        return parameters.subsidizeRuralOnly;
    }
    public void setSubsidizeRuralOnly(boolean val) {
        parameters.subsidizeRuralOnly = val;
    }
    
    
    public double getIdealTemperature() {
    	return parameters.idealTemperature;
    }
    
    public void setIdealTemperature(double val) {
    	parameters.idealTemperature = val;
    }
    
    public double getWealthMu() {
    	return parameters.wealthMu;
    }
    
    public void setWealthMu(double val) {
    	parameters.wealthMu = val;
    }
    
    public double getWealthSigma() {
    	return parameters.wealthSigma;
    }
    
    public void setWealthSigma(double val) {
    	parameters.wealthSigma = val;
    }
    
    public double getWealthAdjMu() {
    	return parameters.wealthAdjMu;
    }
    
    public void setWealthAdjMu(double val) {
    	parameters.wealthAdjMu = val;
    }
    
    public double getWealthAdjSigma() {
    	return parameters.wealthAdjSigma;
    }
    
    public void setWealthAdjSigma(double val) {
    	parameters.wealthAdjSigma = val;
    }
    
    
    public double getWealthLossToBirthMu() {
    	return parameters.wealthLossToBirthMu;
    }
    
    public void setWealthLossToBirthMu(double val) {
    	parameters.wealthLossToBirthMu = val;
    }
    
    public double getWealthLossToBirthSigma() {
    	return parameters.wealthLossToBirthSigma;
    }
    
    public void setWealthLossToBirthSigma(double val) {
    	parameters.wealthLossToBirthSigma = val;
    }
    
    public double getRecordDistance(){
    	return parameters.recordDistance;
    }
    
    public void setRecordDistance(double val) {
    	parameters.recordDistance=val;
    }
    
    public boolean getTrackHousehold() {
    	return parameters.trackHousehold;
    }
    
    public void setTrackHousehold(boolean val) {
    	parameters.trackHousehold=val;
    }
    
    public double getTrackFromTime() {
    	return parameters.trackFromTime;
    }
    
    public void setTrackFromTime(double val) {
    	parameters.trackFromTime=val;
    }
    
    public int getTrackFromXCoord() {
    	return parameters.trackFromCellX;
    }
    
    public void setTrackFromXCoord(int val) {
    	parameters.trackFromCellX=val;
    }
    
    public int getTrackFromYCoord() {
    	return parameters.trackFromCellY;
    }
    
    public void setTrackFromYCoord(int val) {
    	parameters.trackFromCellY=val;
    }
    
    public boolean getCensusTracking() {
    	return parameters.censusTracking;
    }
    
    public void setCensusTracking(boolean val){
    	parameters.censusTracking=val;
    }
    
    
    
	/**
	 * Constructs a new simulation. Side-effect: sets the random seed of
	 * Parameters to equal seed.
	 * 
	 * @param seed
	 *            Random seed
	 * @param args
	 *            Command-line arguments
	 */
	public NorthLandsMovement(long seed) {
		this(seed, null);
	}

	public NorthLandsMovement(long seed, String[] args) {

		super(seed);

		System.out.println("NorthLandsMovement> " + args[0] + ": " + args[1]);	
		parameters = new Parameters(seed, args);
		map = new Map(parameters, random); // Moved to subclasses
		collector = new DataCollector(parameters.filePath,
				parameters.householdFilePath, this);
		smoothedTargetPopGrid = new DoubleGrid2D(Map.GRID_WIDTH,
				Map.GRID_HEIGHT);
		growthTable = new GrowthRateTable(parameters.growthRateFile);

		prepareGrids();
	}

	/**
	 * This function does the smoothing and other relevant pre-processing before
	 * the actual desirability calculations, everything was used to be merged in
	 * the constructors but now they have been separated for cleaner code.
	 */
	public void prepareGrids() 
	{
		// Create a smoothed version of the target numHouseholds grid
		// smoothedTargetPopGrid = Utility.smoothGridWithMapConstraint(map,
		// map.getPopulationGrid(), 9, 9, 3, 2);
		smoothedTargetPopGrid = Utility.smoothGrid(map,
				map.getPopulationGrid(), 9, 9, 3, 5);
		zscoreSmoothedTargetPopGrid = new DoubleGrid2D(smoothedTargetPopGrid);
		Utility.updateGridWithZscores(zscoreSmoothedTargetPopGrid,
				map.canadaCells);
	}

	/**
	 * Extracts data from a grid and places it in a newly created double array.
	 * The data is massaged in a variety of ways such that it is prepared for
	 * use in with the KS statistic test used in the calcSpikinessFitness
	 * function. Depending on how the data in the grid is stored, several of
	 * these steps may not be necessary, and so parts of this code can be copied
	 * into a new function in order to make a more optimized version for a given
	 * phase. I created this function as a default for people to build from.
	 */
	public double[] extractSpikinessDataFromGrid(DoubleGrid2D theGrid) 
	{
		// Fill the model data from the desirability grid
		double[] theData = new double[map.canadaCells.size()];
		double minDataVal = Double.POSITIVE_INFINITY;
		int index = 0;
		for (Cell c : map.canadaCells) 
		{
			double val = map.popGrid.field[c.x][c.y];
			theData[index++] = val;
			minDataVal = Math.min(minDataVal, val);
		}

		// Make all values positive
		if (minDataVal < 0)
			for (int i = 0; i < theData.length; i++)
				theData[i] -= minDataVal;

		Utility.normalizeValues(theData);

		// Normally we would sort the data here, but the library we're using
		// for the KS statistic calculation seems to do this automatically.

		return theData;
	}

	/**
	 * Extracts data from a grid and places it in a newly created double array.
	 * The data is massaged in a variety of ways such that it is prepared for
	 * use in with the KL divergence test used in the calcSmoothedFitness
	 * function. Depending on how the data in the grid is stored, several of
	 * these steps may not be necessary, and so parts of this code can be copied
	 * into a new function in order to make a more optimized version for a given
	 * phase. I created this function as a default for people to build from.
	 */
	public double[] extractSmoothedDataFromGrid(DoubleGrid2D theGrid) 
	{
		double[] thePop = new double[map.canadaCells.size()];
		double theMin = Double.POSITIVE_INFINITY;
		int i = 0;
		for (Cell cell : map.canadaCells) 
		{
			double val = theGrid.field[cell.x][cell.y];
			thePop[i] = val;
			theMin = Math.min(theMin, val);
			i++;
		}

		// Eliminate negative values
		if (theMin < 0.0) // Maybe we should always do this?
			for (i = 0; i < thePop.length; i++) 
			{	thePop[i] -= theMin;	}

		// Normalize so that the adjustment is consistent.
		Utility.normalizeValues(thePop);

		// Kullback-Leibler doesn't handle 0's very well.
		double KL_adjust = 1.0 / (thePop.length * 1000);
		for (i = 0; i < thePop.length; i++) 
		{	thePop[i] += KL_adjust; 	}

		// Now that I've adjusted the values, the distribution no longer
		// sums to 1, so re-normalize.
		Utility.normalizeValues(thePop);

		return thePop;
	}

	public double calcInvertedKolmogorovSmirnovStatistic(double[] a,
			double[] b, MersenneTwisterFast random) 
	{
		KolmogorovSmirnovTest ksTest = new KolmogorovSmirnovTest(
				new MersenneTwisterFastApache(random));
		double ksStatistic = Double.NaN;

		try 
		{	ksStatistic = ksTest.kolmogorovSmirnovStatistic(a, b);	} 
		catch (Exception e) {	}

		return (1 - ksStatistic);
	}

	/**
	 * Returns the KL divergence, K(p1 || p2).
	 * 
	 * The log is w.r.t. base 2.
	 * <p>
	 * 
	 * *Note*: If any value in <tt>p2</tt> is <tt>0.0</tt> then the
	 * KL-divergence is <tt>infinite</tt>. Limin changes it to zero instead of
	 * infinite.
	 * 
	 */
	public double klDivergence(double[] p1, double[] p2) 
	{
		double klDiv = 0.0;

		for (int i = 0; i < p1.length; ++i) 
		{
			if ((p1[i] == 0.0) || (p2[i] == 0.0))
				continue;

			if (p1[i] < 0.0 || p2[i] < 0.0)
				System.err.println("Error: negative input to klDivergence");

			klDiv += p1[i] * Math.log(p1[i] / p2[i]);
			// System.out.println(p1[i] + ", " + p2[i] + ", " + klDiv);
		}

		return klDiv / Math.log(2);
	}

	public double calcKLDivergenceMetric(double[] a, double[] b) 
	{
		double klDivAB = klDivergence(a, b);
		double klDivBA = klDivergence(b, a);

		double klDiv = (Math.abs(klDivAB) + Math.abs(klDivBA)) / 2; // range
																	// between 0
																	// and ~7,
																	// with 0
																	// being
																	// better
		return klDiv;
	}

	/**
	 * Calculates the similarity between smoothed versions of the sprinkled and
	 * target (i.e. LandScan) populations.
	 * 
	 * @return fitness
	 */
	public double calcSmoothedFitness() 
	{
		double[] modelPop = this.gimmeSmoothedModelPop();
		double[] empiricalPop = this.gimmeSmoothedEmpiricalPop();

		// double smoothFitness =
		// calcInvertedKolmogorovSmirnovStatistic(targetPop, modelPop, random);
		double smoothFitness = 1.0 / (1.0 + calcKLDivergenceMetric(
				empiricalPop, modelPop)); // range 0 to 1
		// double smoothFitness = 1.0/(1.0 +
		// Utility.sumOfSquaredDifferences(targetPop,
		// modelPop)/targetPop.length);
		// double smoothFitness = 1.0/(1.0 +
		// Math.sqrt(Utility.sumOfSquaredDifferences(targetPop,
		// modelPop)/targetPop.length));

		return smoothFitness;
	}

	/**
	 * Measures how similar two numHouseholds distributions are in terms of
	 * "spikiness". In other words, how similar are two numHouseholds
	 * distributions, independent of any spatial component.
	 * 
	 * @return Fitness as a similarity metric between 0 and 1
	 */
	public double calcSpikinessFitness() 
	{
		double[] empiricalPop = this.gimmeEmpiricalPop();
		double[] modelPop = this.gimmeModelPop();

		double invKSStatistic = calcInvertedKolmogorovSmirnovStatistic(
				empiricalPop, modelPop, random);
		return invKSStatistic;
	}

	/**
	 * Calculates the desirability maps coefficients fitness with respect to the
	 * numHouseholds distribution, used as a EC evaluation function, it actually
	 * calculates an RMS pixel-by-pixel difference.
	 */
	public double calcFitness() 
	{
		double smoothedFitness = calcSmoothedFitness();
		double spikinessFitness = calcSpikinessFitness();
		double totalFitness = smoothedFitness * spikinessFitness;
		System.out.format("SmoothedFitness: %f\n", smoothedFitness);
		System.out.format("SpikinessFitness: %f\n", spikinessFitness);
		System.out.format("TotalFitness: %f\n", totalFitness);

		return totalFitness;
	}

	/**
	 * Returns an array containing the empirical numHouseholds distribution, but
	 * only for the grid cells we care about (i.e. Canada). All the values in
	 * the array should sum to 1. Override this in each Phase class.
	 */
	public double[] gimmeEmpiricalPop() 
	{
		// I was considering using this as a parameter: ArrayList<Cell>
		// cellsOfInterest

		// This function ignores 0s, which is fine for calculating spikiness
		// fitness, but
		// we may want to retain all data if this function is used for other
		// purposes.
		double[] empiricalPop = Utility.convertToDoubles(map.getPopGridData(1));
		Utility.normalizeValues(empiricalPop);

		return empiricalPop;
	}

	/**
	 * Returns an array containing the numHouseholds distribution derived by the
	 * model, but for only the grid cells we care about (i.e. Canada). All the
	 * values in the array should sum to 1. Override this in each Phase class.
	 */
	public double[] gimmeModelPop() 
	{
		return null; // Override in subclass
	}

	/**
	 * Returns an array containing the empirical numHouseholds distribution
	 * after being Gaussian smoothed, but for only the grid cells we care about
	 * (i.e. Canada). All the values in the array should sum to 1. Override this
	 * in each Phase class.
	 */
	public double[] gimmeSmoothedEmpiricalPop() 
	{
		return null; // Override in subclass
	}

	/**
	 * Returns an array containing the Gaussian smoothed numHouseholds
	 * distribution derived by running the model, but for only the grid cells we
	 * care about (Canada). All the values in the array should sum to 1.
	 * Override this in each Phase class.
	 */
	public double[] gimmeSmoothedModelPop() 
	{
		return null; // Override in subclass
	}

	public IntGrid2D gimmePopulationGrid() 
	{
		return map.getPopulationGrid();
	}

	public int gimmeRunDuration() 
	{
		return 0;
	}

	protected void inspectPopGrid() 
	{
		double sumSmoothed = 0, sumPop = 0;
		for (int i = 0; i < this.smoothedTargetPopGrid.getWidth(); ++i) 
		{
			for (int j = 0; j < this.smoothedTargetPopGrid.getHeight(); ++j) 
			{
				if (this.smoothedTargetPopGrid.get(i, j) != Map.MISSING
						&& map.getNationGrid().get(i, j) == Map.CANADA_CODE)
					sumSmoothed += this.smoothedTargetPopGrid.get(i, j);
			}
		}

		for (int i = 0; i < map.getPopulationGrid().getWidth(); ++i) 
		{
			for (int j = 0; j < map.getPopulationGrid().getHeight(); ++j) 
			{
				if (map.getPopulationGrid().get(i, j) != Map.MISSING
						&& map.getNationGrid().get(i, j) == Map.CANADA_CODE)
					sumPop += map.getPopulationGrid().get(i, j);
			}
		}

		System.out.println("sumSmoothed is " + sumSmoothed);
		System.out.println("sumPop is " + sumPop);
	}

	@Override
	public void start() 
	{
		super.start();

		distributions = new Distributions(random);

		collector.resetAll(parameters.recordData);

		map.initializeTemperature();

		this.urbanDensity = parameters.initUrbanDensity;
		initializeTotalDesirability(parameters);
		initializeResident();
		initializeSocialWeight();
		initializeInfrastructure();
		initializeInfrastructureAvailability();

		// initialize data for step 0
        collector.initialize(map.weatherIO.getTimeForLayer(0));
        this.cumulativePopChangeGrid.setTo(0.0);
        for(int i = 0; i < numProvinces; i++)
        {
            provinceNetMigration[i] = 0;
            // Clear agent migration information
        }

		worldAgent = new WorldAgent(this);
		// addEvent(); // This was for demo, and isn't needed now. Delete?

		federalGovAgent = new govAgent(this, map.canadaCells);

		scheduleAgents();

	}

	public void finish() 
	{
		super.finish();
		if (parameters.recordData)
			collector.close();
		collector.closeHousehold();
		worldAgent.closeMigrationLogFile();
	}

	private void addEvent() 
	{
		// ArrayList<Point> polygon = new ArrayList<Point>();
		// polygon.add(new Point(733,806));
		// polygon.add(new Point(733,818));
		// polygon.add(new Point(743,806));
		// polygon.add(new Point(743,818));
		// Event e1 = new Event(5, 35, polygon, Type.ADD, 1000,
		// this.urbanDesirabilityGrid);
		// Event e2 = new Event(5, 35, polygon, Type.ADD, 1000,
		// this.ruralDesirabilityGrid);
		// worldAgent.registerEvent(e1);
		// worldAgent.registerEvent(e2);
	}

	public void updateBounds(double[] bounds, double value) 
	{
		if (value < bounds[0]) 
		{
			bounds[0] = value;
		} 
		else if (value > bounds[1]) 
		{
			bounds[1] = value;
		}
	}

	private void initializeInfrastructureAvailability() 
	{
		updateInfrastructureAvailabilityGrid();
	}

	public void updateInfrastructureGrid() 
	{
		// XXX Should we reset the bounds here?
		for (Cell cell : map.canadaCells) 
		{
			this.infrastructureGrid.field[cell.x][cell.y] = cell.infrastructure;
			updateBounds(infrastructureDataBounds, cell.infrastructure);
		}
	}

	// This function updates the infrastructureAvailability in the cells as well
	// as the infrastructureAvailability grid.
	// The cell contains the raw values, but the grid contains zscored raw
	// values.
	public void updateInfrastructureAvailabilityGrid() 
	{
		double value, logi, logh;

		for (Cell cell : map.canadaCells) 
		{
			if (cell.infrastructure == 0.0)
				logi = 0.0;
			else
				logi = Math.log(cell.infrastructure);

			if (cell.numHouseholds == 0.0)
				logh = 0.0;
			else
				logh = Math.log(cell.numHouseholds);

			// value = cell.infrastructure - cell.numHouseholds;
			value = logi - logh;
			this.infrastructureAvailabilityGrid.field[cell.x][cell.y] = value;
		}

		Utility.updateGridWithZscores(this.infrastructureAvailabilityGrid,
				map.canadaCells);

		for (Cell cell : map.canadaCells) 
		{
			value = this.infrastructureAvailabilityGrid.field[cell.x][cell.y];
			updateBounds(infrastructureAvailabilityDataBounds, value);
		}

		// System.out.println("infrastructureAvailabilityDataBounds = (" +
		// infrastructureAvailabilityDataBounds[0] + ", "
		// + infrastructureAvailabilityDataBounds[1] + ")");

		double empty_cell_val = 0.0;
		for (Cell cell : map.canadaCells) 
		{
			if (cell.infrastructure == 0)
				this.infrastructureAvailabilityGrid.field[cell.x][cell.y] = empty_cell_val;
		}
	}

	private void initializeInfrastructure() 
	{
		for (Cell cell : map.canadaCells) 
		{
			cell.infrastructure = 0;
			if (cell.numHouseholds != 0) 
			{
				double sigma = cell.numHouseholds
						* parameters.infrastructureDeviationRate;
				cell.infrastructure = this.distributions.gaussianSample(
						cell.numHouseholds, sigma);
			}
			infrastructureGrid.field[cell.x][cell.y] = cell.infrastructure;
			updateBounds(infrastructureDataBounds, cell.infrastructure);
		}
	}

	private void initializeSocialWeight() 
	{
		this.updateSocialWeight();
	}

	// this initialize the urban and rural socialWeightGrid and
	// adjacentSocialWeightGrid
	public void updateSocialWeight() 
	{
		DoubleGrid2D normalizedPopGrid = new DoubleGrid2D(Map.GRID_WIDTH,
				Map.GRID_HEIGHT, 0);
		double tempPop;
		// first, we reset the grid
		for (Cell cell : map.canadaCells) 
		{
			urbanSocialWeightGrid.field[cell.x][cell.y] = 0;
			urbanAdjacentSocialWeightGrid.field[cell.x][cell.y] = 0;
			ruralSocialWeightGrid.field[cell.x][cell.y] = 0;
			ruralAdjacentSocialWeightGrid.field[cell.x][cell.y] = 0;
			tempPop = urbanResidenceGrid.field[cell.x][cell.y]
					+ ruralResidenceGrid.field[cell.x][cell.y];
			if (tempPop == 0)
				normalizedPopGrid.field[cell.x][cell.y] = 0.0;
			else
				normalizedPopGrid.field[cell.x][cell.y] = Math.log(tempPop);

		}

		// Utility.updateGridWithZscores(normalizedPopGrid, map.canadaCells);

		for (Cell cell : map.canadaCells) 
		{
			urbanSocialWeightGrid.field[cell.x][cell.y] = parameters.urbanSocialWeight
					* normalizedPopGrid.field[cell.x][cell.y];
			ruralSocialWeightGrid.field[cell.x][cell.y] = parameters.ruralSocialWeight
					* normalizedPopGrid.field[cell.x][cell.y];

			// then we start to deal with adjacent social weight
			if (parameters.urbanAdjacentSocialDiscount != 0) 
			{
				double adSocialWeight = urbanSocialWeightGrid.field[cell.x][cell.y]
						* parameters.urbanAdjacentSocialDiscount;
				this.increaseAdjacentSocialWeight(cell,
						urbanAdjacentSocialWeightGrid, adSocialWeight);
			}

			if (parameters.ruralAdjacentSocialDiscount != 0) 
			{
				double adSocialWeight = ruralSocialWeightGrid.field[cell.x][cell.y]
						* parameters.ruralAdjacentSocialDiscount;
				this.increaseAdjacentSocialWeight(cell,
						ruralAdjacentSocialWeightGrid, adSocialWeight);
			}
		}

	}

	private void increaseAdjacentSocialWeight(Cell cell, DoubleGrid2D grid,
			double adSocialWeight) 
	{
		for (int y = cell.y - 1; y <= cell.y + 1; y++)
			for (int x = cell.x - 1; x <= cell.x + 1; x++)
				if (((y != cell.y) || (x != cell.x)) && (x >= 0)
						&& (x < Map.GRID_WIDTH) && (y >= 0)
						&& (y < Map.GRID_HEIGHT)) {
					Cell c = (Cell) map.cellGrid.field[x][y];
					if ((c != null)) 
					{
						grid.field[c.x][c.y] += adSocialWeight;
					}
				}

	}

	/**
	 * initialize the total desirability grid with different kind of agents, the
	 * implementation may change
	 * 
	 * @param parameters
	 */
	public void initializeTotalDesirability(Parameters parameters) 
	{
		// map.updateInitDesirabilityGrid(parameters);
		// map.initializeDesirabilityGrid();
		map.updateDesirabilityGrid();

		updateDesirabilityMap(urbanDesirabilityGrid, parameters.urbanTempCoeff,
				parameters.urbanRiverCoeff, parameters.urbanPortCoeff,
				parameters.urbanElevCoeff);
		updateDesirabilityMap(ruralDesirabilityGrid, parameters.ruralTempCoeff,
				parameters.ruralRiverCoeff, parameters.ruralPortCoeff,
				parameters.ruralElevCoeff);
	}

	public void updateDesirabilityMap(DoubleGrid2D grid, double tempCoeff,
			double riverCoeff, double portCoeff, double elevCoeff) 
	{
		double totDes;
		for (Cell c : map.canadaCells) 
        {
			totDes = map.calculateInitialDesirability(c.x, c.y, tempCoeff,
					portCoeff, riverCoeff, elevCoeff);
			grid.field[c.x][c.y] = totDes;
			updateBounds(desDataBounds, totDes);
		}
	}

	public void scheduleAgents() 
    {
		double seasonLength = 0.25;
		// The first step actually loads the second temperature layer.
		double firstStepTime = map.weatherIO.getTimeForLayer(1);
		int householdOrdering = 2;
		for (Household h : households) 
        {
			schedule.scheduleRepeating(firstStepTime, householdOrdering, h,
					seasonLength);
		}

		schedule.scheduleRepeating(firstStepTime, 6, worldAgent, seasonLength);
		schedule.scheduleRepeating(firstStepTime, 7, federalGovAgent,
				seasonLength);

		Set<Entry<Integer, MegaCellSign>> megaCellSignSet = map.megaCellTable
				.entrySet();
		Iterator<Entry<Integer, MegaCellSign>> iterator = megaCellSignSet
				.iterator();
		while (iterator.hasNext()) 
        {
			MegaCellSign sign = iterator.next().getValue();
			schedule.scheduleRepeating(firstStepTime, 8, sign, seasonLength);
		}
	}

	public void initializeResident() 
    {
		households = new ArrayList<Household>();
		int numHouseholdsToSprinkle = 10; // number of households that we
											// sprinkle in each region at a time

		if (parameters.histTempFilename == null
				|| parameters.histTempFilename.length() == 0) 
        {
			IntGrid2D popGrid = map.getPopulationGrid();
			for (Cell cell : map.canadaCells) 
            {
				int numHouseholds = popGrid.get(cell.x, cell.y)
						/ parameters.householdSize;
				if (numHouseholds > 0)
					cell.addHouseholds(numHouseholds);
			}
		} 
        else 
        {
			new PeopleSprinkler().initializeHouseholds(numHouseholdsToSprinkle,
					map, parameters.initSocialWeight,
					parameters.initSocialWeightSpread,
					parameters.initDesirabilityExp, parameters.householdSize,
					random);
		}
		this.initializeHousehold();
	}

	/**
	 * 
	 */
	public void initializeHousehold() 
    {
		int ruralSum = 0, urbanSum = 0;
		for (Cell cell : map.canadaCells) 
        {
			// reset the grid
			urbanResidenceGrid.field[cell.x][cell.y] = 0;
			ruralResidenceGrid.field[cell.x][cell.y] = 0;

			// this is the urban agent
			if (cell.numHouseholds >= this.urbanDensity) 
            {
				for (int i = 0; i < cell.numHouseholds; ++i) 
                {
					Household h = new Household(cell, parameters, 0,
							this.random, this.distributions.lognormalSample(
									this.parameters.wealthMu,
									this.parameters.wealthSigma));
					this.households.add(h);

					// put the household into the selection grid
					updateHouseholdLoc(h, cell.x, cell.y);
				}
                
				this.urbanResidenceGrid.field[cell.x][cell.y] = cell.numHouseholds;

				urbanSum += cell.numHouseholds;
				cell.cellType = 0;

			} 
            else if (cell.numHouseholds < this.urbanDensity) 
            {
				for (int i = 0; i < cell.numHouseholds; ++i) 
                {
					Household h = new Household(cell, parameters, 1, this.random, 
                                    this.distributions.lognormalSample(
                                            		this.parameters.wealthMu,
                                                    this.parameters.wealthSigma));
					this.households.add(h);

					// put the household into the selection grid
					updateHouseholdLoc(h, cell.x, cell.y);
				}
				this.ruralResidenceGrid.field[cell.x][cell.y] = cell.numHouseholds;
				ruralSum += cell.numHouseholds;
				cell.cellType = 1;
			}

			this.populationBufferGrid.field[cell.x][cell.y] = cell.numHouseholds;

			updateBounds(residenceDataBounds, cell.numHouseholds);
		}

		this.ruralResidence = ruralSum;
		this.urbanResidence = urbanSum;
	}

	
    public boolean censusHeld() 
    {    
        double time = schedule.getTime();
        
        if (time<1951.25 && time%10==1 || time>1951.25 && time%5==1)
            return true;
        else
            return false;
    }


	public void updateHouseholdLoc(Household h, int x, int y) 
    {
		this.householdSelectionGrid.setObjectLocation(h, x, y);
		this.householdTrailGrid.setObjectLocation(h, x, y);
	}

	public void updateResidenceGrid() 
    {
		int ruralSum = 0, urbanSum = 0;
		for (Cell cell : map.canadaCells) 
        {
			// reset the grid
			urbanResidenceGrid.field[cell.x][cell.y] = 0;
			ruralResidenceGrid.field[cell.x][cell.y] = 0;
			// this is the urban agent
			if (cell.numHouseholds >= this.urbanDensity) 
            {
				this.urbanResidenceGrid.field[cell.x][cell.y] = cell.numHouseholds;
				urbanSum += cell.numHouseholds;
				// record Data
				if (cell.cellType == 1) { collector.incrementCellToUrban(); }
				cell.cellType = 0;
			} 
            else if (cell.numHouseholds < this.urbanDensity) 
                {
                    this.ruralResidenceGrid.field[cell.x][cell.y] = cell.numHouseholds;
                    ruralSum += cell.numHouseholds;
                    // record Data
                    if (cell.cellType == 0) { collector.incrementCellToRural(); }
                    cell.cellType = 1;
                }
			updateBounds(residenceDataBounds, cell.numHouseholds);


			double localChange = cell.numHouseholds - this.populationBufferGrid.field[cell.x][cell.y];
            this.populationChangeGrid.field[cell.x][cell.y] = localChange;
			this.populationBufferGrid.field[cell.x][cell.y] = cell.numHouseholds;
			
            this.cumulativePopChangeGrid.field[cell.x][cell.y] += localChange;
			if (censusHeld())
			{
                this.censusPopChangeGrid.field[cell.x][cell.y] = this.cumulativePopChangeGrid.field[cell.x][cell.y];
			    this.cumulativePopChangeGrid.field[cell.x][cell.y] = 0.0;
			}
		}

		this.ruralResidence = ruralSum;
		this.urbanResidence = urbanSum;
	}


    /*
	 * On census years, go through all the households and determine if
	 * they've moved to a new province or not.
	 */
    public void updateMigrationInfo()
    {
        if (censusHeld())
        {
            for(int p = 0; p < numProvinces; p++)
                provinceNetMigration[p] = 0;
            
            for (Household house : households)
            {
                int from = house.previousCensusProvince();
                int to = house.currentCensusProvince();
                //System.out.println("from: " + from + "   to: " + to);
                if (from != to)
                {
                    //System.out.println("Counted a migration");
                    provinceNetMigration[to] += 1;
                    //provinceNetMigration[from] -= 1;
                    provinceNetMigration[0] += 1;  // Total for Canada
                }
            }
        }
    }

    
	public double[] householdDesirabilityArray(int mode, DoubleGrid2D grid, double scale)
	{
		int size = 0;
		ArrayList<Integer> populationList = new ArrayList<Integer>();
		ArrayList<Double> desirabilityList = new ArrayList<Double>();
		for (Cell cell : map.canadaCells) 
		{
			if (cell.cellType == mode) 
            {
				size += cell.numHouseholds;
				populationList.add(cell.numHouseholds);
				desirabilityList.add(grid.field[cell.x][cell.y]);
			}
		}

		double[] array = new double[size];
		int index = 0;
		for (int i = 0; i < populationList.size(); ++i) 
        {
			int pop = populationList.get(i);
			double val = desirabilityList.get(i) * scale;
			int cursor = index;
			while (cursor - index < pop) { array[cursor++] = val; }
			index = cursor;
		}

		return array;
	}

	public double[] residenceArray(int lower, int upper) 
	{
		if ((map == null) || map.canadaCells.isEmpty())
			return null;

		int index = 0;
		for (Cell cell : map.canadaCells) 
        {
			int sum = (int) (ruralResidenceGrid.field[cell.x][cell.y] + urbanResidenceGrid.field[cell.x][cell.y]);
			if (lower <= sum && sum <= upper)
				index++;
		}

		double[] residence = new double[index];
		index = 0;
		for (Cell cell : map.canadaCells) 
		{
			int sum = (int) (ruralResidenceGrid.field[cell.x][cell.y] + urbanResidenceGrid.field[cell.x][cell.y]);
			if (lower <= sum && sum <= upper)
				residence[index++] = sum;
		}
		return residence;
	}

	// mode 0 = all, 1 = urban, 2 = rural
	public double[] desirabilityArray(int mode) 
	{
		if (households == null)
			return null;

		// double[] satis = new double[households.size()];

		ArrayList<Double> satis = new ArrayList<Double>();

		for (int i = 0; i < households.size(); ++i) 
		{
			if (mode == 0) 
			{	satis.add(households.get(i).satisfaction);	}
			if (mode == 1 && households.get(i).typeFlag == 0) 
			{	satis.add(households.get(i).satisfaction);	}
			if (mode == 2 && households.get(i).typeFlag == 1) 
			{	satis.add(households.get(i).satisfaction);	}
		}

		double[] satisArray = new double[satis.size()];

		for (int i = 0; i < satis.size(); ++i) 
		{	satisArray[i] = satis.get(i);	}

		return satisArray;
	}

	public double[] infrastructureHistogram() 
	{
		ArrayList<Double> infrastructurePotential = new ArrayList<Double>();

		int i = 0;
		for (Cell cell : map.canadaCells) 
		{
			int infrastructureUnits = (int) (cell.netAssets / (Math.pow(
					cell.numHouseholds, parameters.infrastructureCostExponent)));
			double diff = cell.infrastructure - cell.numHouseholds
					+ infrastructureUnits;
			if (cell.numHouseholds > 0) 
			{
				double potential = diff / cell.numHouseholds;
				infrastructurePotential.add(potential);
				i++;
			}
		}

		double[] infArray = new double[infrastructurePotential.size()];

		for (int j = 0; j < infrastructurePotential.size(); j++) 
		{	infArray[j] = infrastructurePotential.get(j);	}

		return infArray;
	}

	public double[] assetsHistogram() 
	{
		ArrayList<Double> assets = new ArrayList<Double>();

		for (Cell cell : map.canadaCells) 
		{
			if (cell.numHouseholds > 0) 
			{	assets.add(cell.netAssets);	}
		}

		double[] assetsArray = new double[assets.size()];

		for (int j = 0; j < assets.size(); j++) 
		{	assetsArray[j] = assets.get(j);	}

		return assetsArray;
	}

	public double[] getHouseholdWealthArray(int mode) 
	{
		if (households == null)
			return null;

		// double[] satis = new double[households.size()];

		ArrayList<Double> wealth = new ArrayList<Double>();

		for (int i = 0; i < households.size(); ++i) 
		{
			if (mode == 0) { wealth.add(households.get(i).wealth); }
			if (mode == 1 && households.get(i).typeFlag == 0) 
                { wealth.add(households.get(i).wealth); }
			if (mode == 2 && households.get(i).typeFlag == 1) 
                { wealth.add(households.get(i).wealth); }
		}

		double[] wealthArray = new double[wealth.size()];

		for (int i = 0; i < wealth.size(); ++i) 
		{	wealthArray[i] = wealth.get(i);	}

		return wealthArray;
	}

	public double[] householdWealthAdjustmentArray() 
	{
		if (households == null)
			return null;

		// double[] satis = new double[households.size()];

		ArrayList<Double> wealthAdjustments = new ArrayList<Double>();

		for (int i = 0; i < households.size(); ++i) 
		{	wealthAdjustments.add(households.get(i).wealthAdjustment);	}

		double[] wealthArray = new double[wealthAdjustments.size()];

		for (int i = 0; i < wealthAdjustments.size(); ++i) 
		{	wealthArray[i] = wealthAdjustments.get(i);	}

		return wealthArray;
	}

	
	public double[] infrastructureTaxArray() 
	{	
		ArrayList<Double> taxValues = new ArrayList<Double>();

		for (Cell cell : map.canadaCells) 
		{	taxValues.add(cell.infrastructureTax);	}

		double[] taxArray = new double[taxValues.size()];

		for (int i = 0; i < taxValues.size(); ++i) 
		{	taxArray[i] = taxValues.get(i);	}
		
		return taxArray;
	}
	
	public static void main(String[] args) 
	{
		doLoop(new MakesSimState() 
		{
			@Override
			public SimState newInstance(long seed, String[] args) 
			{	return new NorthLandsMovement(seed, args);	}

			@Override
			public Class simulationClass() 
			{	return NorthLandsMovement.class;	}
		}, args);
	}

	public Object[] getPopulationWithFactor(int mode) 
	{
		int size = 0;
		if (mode == 0) 
		{	size = urbanHouseholdsMovedThisStep;	}
		else if (mode == 1) 
		{	size = ruralHouseholdsMovedThisStep;	}

		BarChartFactor[] array = new BarChartFactor[size];
		int index = 0;

		if (households != null) 
		{
			for (Household h : households) 
			{	if (mode == 0 && h.hasMovedAsUrban) 
				{
					Factor f = h.currentCell.urbanMajorFactor;
					array[index] = new BarChartFactor(f);
					index++;
				} 
			else if (mode == 1 && h.hasMovedAsRural) 
				{
					Factor f = h.currentCell.ruralMajorFactor;
					array[index] = new BarChartFactor(f);
					index++;
				}
			}
		}

		return array;
	}

	public TreeMap<Double, ArrayList<Household>> getSatisfactionToHouseholdMap(int mode) 
	{
		TreeMap<Double, ArrayList<Household>> satisfactionToHouseholdMap = new TreeMap<Double, ArrayList<Household>>();

		for (Household h : households) 
		{
			if (h.typeFlag == mode || mode == -1) 
			{
				if (!satisfactionToHouseholdMap.containsKey(h.satisfaction)) 
				{	
					satisfactionToHouseholdMap.put(h.satisfaction, new ArrayList<Household>()); 
				}
				satisfactionToHouseholdMap.get(h.satisfaction).add(h);				
			}
		}

		return satisfactionToHouseholdMap;
	}

	public TreeMap<Double, ArrayList<Cell>> getAssetsToCellsMap(int mode) 
	{
		TreeMap<Double, ArrayList<Cell>> assetsToCellsMap = new TreeMap<Double, ArrayList<Cell>>();

		for (Cell cell : map.canadaCells) 
		{
			if (cell.cellType == mode || mode == -1) 
			{
				if (!assetsToCellsMap.containsKey(cell.netAssets)) 
				{
					assetsToCellsMap.put(cell.netAssets, new ArrayList<Cell>());
				}
			}
		}
		return assetsToCellsMap;
	}

	public double[] cellDesirabilityArray(DoubleGrid2D desirabilityGrid) 
	{
		double[] desirabilityArray = new double[map.canadaCells.size()];
		int index = 0;
		for (Cell cell : map.canadaCells) 
		{
			desirabilityArray[index] = desirabilityGrid.field[cell.x][cell.y];
			index += 1;
		}

		return desirabilityArray;
	}

	public double[] calculateCohesion() 
	{
		HashMap<Cell, Double> cohesions = new HashMap<Cell, Double>();

		if (households != null) 
		{
			for (Household household : households) 
			{
				if (!cohesions.containsKey(household.currentCell)) 
				{	cohesions.put(household.currentCell, household.stayLength);	} 
				else 
				{
					cohesions.put(household.currentCell,
									cohesions.get(household.currentCell)
									+ household.stayLength);
				}
			}
		}

		double[] cohesionArray = new double[cohesions.size()];

		int i = 0;
		for (Cell cell : cohesions.keySet()) 
		{
			cell.cohesion = cohesions.get(cell) / cell.numHouseholds;
			cohesionArray[i] = cohesions.get(cell) / cell.numHouseholds;
			i++;
		}
		return cohesionArray;
	}
}
