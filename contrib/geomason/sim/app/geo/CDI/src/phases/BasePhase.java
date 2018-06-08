package CDI.src.phases;


import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.spiderland.Psh.intStack;

import CDI.src.migration.util.Distributions;
import CDI.src.movement.parameters.Parameters;
import CDI.src.phases.ECInterface;
import CDI.src.phases.Utility;
import sim.engine.MakesSimState;
import sim.engine.SimState;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.IntGrid2D;
import sim.util.Interval;
import ucar.nc2.ui.geoloc.NewMapAreaEvent;
import CDI.src.util.MersenneTwisterFastApache;
import ec.util.MersenneTwisterFast;
import CDI.src.environment.Cell;
import CDI.src.environment.Map;


public class BasePhase extends SimState implements ECInterface
{
	private static final long serialVersionUID = 1L;

	public Parameters parameters;
	
	private final static double COEFF_LOWER_BOUND = -10.0;
	private final static double COEFF_UPPER_BOUND = 10.0;
	protected Distributions distributions;
	
    public Map map;
    	
	public DoubleGrid2D smoothedTargetPopGrid; // smoothed numHouseholds grid
	public DoubleGrid2D zscoreSmoothedTargetPopGrid; // A z-score version of the smoothed numHouseholds

	public double getTempDesCoefficient() { return parameters.initTempCoeff; }
	public void setTempDesCoefficient(double val) 
	{
		if(COEFF_LOWER_BOUND<=val&&val<=COEFF_UPPER_BOUND)
			parameters.initTempCoeff = val;	
	}

	public Object domTempDesCoefficient() {  return new Interval(COEFF_LOWER_BOUND, COEFF_UPPER_BOUND); }

	public double getPortDesCoefficient() { return parameters.initPortCoeff; }
	public void setPortDesCoefficient(double val) 
	{
		if(COEFF_LOWER_BOUND<=val&&val<=COEFF_UPPER_BOUND)
			parameters.initPortCoeff = val;
	}

	public Object domPortDesCoefficient() { return new Interval(COEFF_LOWER_BOUND, COEFF_UPPER_BOUND); }

	public double getRiverDesCoefficient() { return parameters.initRiverCoeff; }
	public void setRiverDesCoefficient(double val) 
	{
		if(COEFF_LOWER_BOUND<=val&&val<=COEFF_UPPER_BOUND)
			parameters.initRiverCoeff = val;
	}

	public Object domRiverDesCoefficient() { return new Interval(COEFF_LOWER_BOUND, COEFF_UPPER_BOUND); }

	public double getElevDesCoefficient() {return parameters.initElevCoeff; }
	public void setElevDesCoefficient(double val) 
	{
		if(COEFF_LOWER_BOUND<=val&&val<=COEFF_UPPER_BOUND)
			parameters.initElevCoeff = val;
	}

	public Object domElevDesCoefficient() { return new Interval(COEFF_LOWER_BOUND, COEFF_UPPER_BOUND); }
	
	public double fitness = 0;
	public double getFitness() { return fitness; }
	
	/**
     * Constructs a new base phase simulation.
     * 
     */
    public BasePhase(long seed) { this(seed, null); }
    
    public BasePhase(long seed, String[] args)
    {
    	super(seed);
        parameters = new Parameters(seed, args);
        map = new Map(parameters, random);  // Moved to subclasses
        smoothedTargetPopGrid = new DoubleGrid2D(Map.GRID_WIDTH, Map.GRID_HEIGHT);
        prepareGrids();
    }
    
    /**
     * This function does the smoothing and other relevant
     * pre-processing before the actual desirability calculations,
     * everything was used to be merged in the constructors but now
     * they have been separated for cleaner code.
     */
    public void prepareGrids()
    {
        // Create a smoothed version of the target numHouseholds grid
        //smoothedTargetPopGrid = Utility.smoothGridWithMapConstraint(map, map.getPopulationGrid(), 9, 9, 3, 2);
        smoothedTargetPopGrid = Utility.smoothGrid(map, map.getPopulationGrid(), 9, 9, 3, 5);
        zscoreSmoothedTargetPopGrid = new DoubleGrid2D(smoothedTargetPopGrid);
        Utility.updateGridWithZscores(zscoreSmoothedTargetPopGrid, map.canadaCells);
    }
    
    @Override
    public void start()
    {
        super.start();
        distributions = new Distributions(this.random);
    }


    /**
     * Extracts data from a grid and places it in a newly created double array.  The data is
     * massaged in a variety of ways such that it is prepared for use in with the KS statistic
     * test used in the calcSpikinessFitness function.  Depending on how the data in the grid is
     * stored, several of these steps may not be necessary, and so parts of this code can be
     * copied into a new function in order to make a more optimized version for a given phase.
     * I created this function as a default for people to build from.
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
     * Extracts data from a grid and places it in a newly created double array.  The data is
     * massaged in a variety of ways such that it is prepared for use in with the KL divergence
     * test used in the calcSmoothedFitness function.  Depending on how the data in the grid is
     * stored, several of these steps may not be necessary, and so parts of this code can be
     * copied into a new function in order to make a more optimized version for a given phase.
     * I created this function as a default for people to build from.
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
        if (theMin < 0.0)  // Maybe we should always do this?
            for(i = 0; i < thePop.length; i++)
            	{ thePop[i] -= theMin; }

        // Normalize so that the adjustment is consistent.
        Utility.normalizeValues(thePop);

        // Kullback-Leibler doesn't handle 0's very well.
        double KL_adjust = 1.0 / (thePop.length * 1000);
        for(i = 0; i < thePop.length; i++) 
        	{ thePop[i] += KL_adjust; }

        // Now that I've adjusted the values, the distribution no longer
        // sums to 1, so re-normalize.
        Utility.normalizeValues(thePop);

        return thePop;
    }

    
    public double calcInvertedKolmogorovSmirnovStatistic(double[] a, double[] b, MersenneTwisterFast random) 
    {
        KolmogorovSmirnovTest ksTest = new KolmogorovSmirnovTest(new MersenneTwisterFastApache(random));
        double ksStatistic = Double.NaN;
        try { ksStatistic = ksTest.kolmogorovSmirnovStatistic(a, b); }
        catch(Exception e) {}  
        
        return (1 - ksStatistic);
    }
    

    /**
     * Returns the KL divergence, K(p1 || p2).
     *
     * The log is w.r.t. base 2. <p>
     *
     * *Note*: If any value in <tt>p2</tt> is <tt>0.0</tt> then the KL-divergence
     * is <tt>infinite</tt>. Limin changes it to zero instead of infinite. 
     * 
     */
    public double klDivergence(double[] p1, double[] p2) 
    {
        double klDiv = 0.0;
        for (int i = 0; i < p1.length; ++i) 
        {
            if ((p1[i] == 0.0) || (p2[i] == 0.0)) continue;
            if (p1[i] < 0.0 || p2[i] < 0.0) System.err.println("Error: negative input to klDivergence");
            klDiv += p1[i] * Math.log(p1[i] / p2[i]);
            //System.out.println(p1[i] + ", " + p2[i] + ", " + klDiv);
        }
        
        return klDiv / Math.log(2); 
    }


    public double calcKLDivergenceMetric(double[] a, double[] b) 
    {
        double klDivAB = klDivergence(a, b);
        double klDivBA = klDivergence(b, a);
        double klDiv = (Math.abs(klDivAB) + Math.abs(klDivBA))/2;   // range between 0 and ~7, with 0 being better

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
        //double smoothFitness = calcInvertedKolmogorovSmirnovStatistic(targetPop, modelPop, random);
        double smoothFitness = 1.0/(1.0 + calcKLDivergenceMetric(empiricalPop, modelPop));  // range 0 to 1
        //double smoothFitness = 1.0/(1.0 + Utility.sumOfSquaredDifferences(targetPop, modelPop)/targetPop.length);
        //double smoothFitness = 1.0/(1.0 + Math.sqrt(Utility.sumOfSquaredDifferences(targetPop, modelPop)/targetPop.length));       

        return smoothFitness;
    }

    
    /**
     * Measures how similar two numHouseholds distributions are in terms 
     * of "spikiness". In other words, how similar are two numHouseholds 
     * distributions, independent of any spatial component.
     * 
     * @return Fitness as a similarity metric between 0 and 1
     */
    public double calcSpikinessFitness()
    {
        double[] empiricalPop = this.gimmeEmpiricalPop();
        double[] modelPop = this.gimmeModelPop();
        double invKSStatistic = calcInvertedKolmogorovSmirnovStatistic(empiricalPop, modelPop, random);
        return invKSStatistic;
    }


    /**
     * Calculates the desirability maps coefficients fitness 
     * with respect to the numHouseholds distribution, used as a
     * EC evaluation function, it actually calculates an RMS
     * pixel-by-pixel difference.
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
     * Returns an array containing the empirical numHouseholds 
     * distribution, but only for the grid cells we care about 
     * (i.e. Canada). All the values in the array should sum to 1.
     * Override this in each Phase class.
     */
    public double[] gimmeEmpiricalPop()
    {
        // I was considering using this as a parameter: ArrayList<Cell> cellsOfInterest
        
        // This function ignores 0s, which is fine for calculating spikiness fitness, but
        // we may want to retain all data if this function is used for other purposes.
        double[] empiricalPop = Utility.convertToDoubles(map.getPopGridData(1));
        Utility.normalizeValues(empiricalPop);

        return empiricalPop;
    }


    /**
     * Returns an array containing the numHouseholds distribution 
     * derived by the model, but for only the grid cells we care 
     * about (i.e. Canada). All the values in the array should sum to 1.
     * Override this in each Phase class.
     */
    public double[] gimmeModelPop() { return null; }   // Override in subclass


    /**
     * Returns an array containing the empirical numHouseholds distribution 
     * after being Gaussian smoothed, but for only the grid cells we care 
     * about (i.e. Canada). All the values in the array should sum to 1.
     * Override this in each Phase class.
     */
    public double[] gimmeSmoothedEmpiricalPop() { return null; }    // Override in subclass


    /**
     * Returns an array containing the Gaussian smoothed numHouseholds 
     * distribution derived by running the model, but for only the grid 
     * cells we care about (Canada). All the values in the array should 
     * sum to 1. Override this in each Phase class.
     */
    public double[] gimmeSmoothedModelPop() { return null; }   // Override in subclass


	@Override
	public IntGrid2D gimmePopulationGrid() { return map.getPopulationGrid(); }

	
	@Override
	public int gimmeRunDuration() { return 0; }
    
	
	protected void inspectPopGrid()
	{
		double sumSmoothed = 0,sumPop = 0;
		for(int i = 0;i<this.smoothedTargetPopGrid.getWidth();++i)
		{
			for(int j = 0;j<this.smoothedTargetPopGrid.getHeight();++j)
			{
				if(this.smoothedTargetPopGrid.get(i, j)!=Map.MISSING&&map.getNationGrid().get(i, j)==Map.CANADA_CODE)
					sumSmoothed += this.smoothedTargetPopGrid.get(i, j);
			}
		}
		
		for(int i = 0;i<map.getPopulationGrid().getWidth();++i)
		{
			for(int j = 0;j<map.getPopulationGrid().getHeight();++j)
			{
				if(map.getPopulationGrid().get(i, j)!=Map.MISSING&&map.getNationGrid().get(i, j)==Map.CANADA_CODE)
					sumPop += map.getPopulationGrid().get(i, j);
			}
		}
		
		System.out.println("sumSmoothed is "+ sumSmoothed);
		System.out.println("sumPop is "+ sumPop);
	}
	
	
    public static void main(String[] args)
    {
    	// we have a new Migration instance instead of phase2?
      doLoop(new MakesSimState()
        {
            @Override
            public SimState newInstance(long seed, String[] args)
            { return new BasePhase(seed, args); }

            @Override
            public Class simulationClass()
            { return BasePhase.class; }
        }, args);

    }
    
}
