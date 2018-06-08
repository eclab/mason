package phases;

import java.util.ArrayList;
import java.util.Arrays;

import optimization.util.Filter;
import optimization.util.GaussianFilter;
import ec.util.MersenneTwisterFast;
import environment.Cell;
import environment.Map;
import sim.field.grid.AbstractGrid2D;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.IntGrid2D;


/**
 * 
 * @author Ermo Wei
 * 
 * This class used singleton pattern, so we can pass some arguments only once
 * instead of pass them very time we call the utility method, this is not thread safe now.
 * FIXME Will change it later if it is necessary
 * The utility will take two grids and compute the "similarity" between the grids 
 * 
 */


public class Utility {

	
	// bounding box of the Canada, start and end for horizontal and vertical dimensions
	private final static int WIDTH_START = 210;
	private final static int WIDTH_END = 771;
	private final static int HEIGHT_START = 546;
	private final static int HEIGHT_END = 980;	
	
	// parameter for the filter
	private final static int KERNEL_WIDTH = 9;
	private final static int KERNEL_HEIGHT = 9;
	private final static int FILTER_SIGMA = 3;
	private final static int FILTER_PASS = 2;
	
	
    /** Calculate the sum of squares of the differences between the first and second grids,
     *  only on the cellsOfInterest.
     *  
     * @param first    The first grid that is being compared
     * @param second   The second grid that is being compared
     * @param cellsOfInterest  A list of cells to examine
     * @return The sum of square differences
     */
    public static double sumOfSquaredDifferences(double[] a, double[] b)
    {
        assert(a.length == b.length);

        double sum = 0.0;
        for (int i = 0; i < a.length; i++)
            sum += (a[i]-b[i]) * (a[i]-b[i]);

        return sum;
    }


	/** Calculate the sum of squares of the differences between the first and second grids,
	 *  only on the cellsOfInterest.
	 *  
	 * @param first    The first grid that is being compared
	 * @param second   The second grid that is being compared
	 * @param cellsOfInterest  A list of cells to examine
	 * @return The sum of square differences
	 */
	public static double sumOfSquaredDifferences(DoubleGrid2D first, DoubleGrid2D second, ArrayList<Cell> cellsOfInterest)
	{
	    double sum = 0.0;
	    for (Cell c : cellsOfInterest)
	    {
	        double v1 = first.field[c.x][c.y];
            double v2 = second.field[c.x][c.y];
	        sum += (v1-v2) * (v1-v2);
	    }

	    return sum;
	}
	
    /** Calculate the absolute differences between the first and second grids,
     *  only on the cellsOfInterest.
     *  
     * @param first    The first grid that is being compared
     * @param second   The second grid that is being compared
     * @param cellsOfInterest  A list of cells to examine
     * @return The average absolute difference
     */
    public static double averageOfAbsoluteDifferences(DoubleGrid2D first, DoubleGrid2D second, ArrayList<Cell> cellsOfInterest)
    {
        double sum = 0.0;
        for (Cell c : cellsOfInterest)
        {
            double v1 = first.field[c.x][c.y];
            double v2 = second.field[c.x][c.y];
            sum += Math.abs(v1 - v2);
        }
        
        return sum / cellsOfInterest.size();
    }
    
    
    /** Take a DoubleGrid2D and modify it to contain z-score values, but
     *  only in the cellsOfInterest.
     *  
     * @param grid    The grid we are modifying
     * @param cellsOfInterest  A list of cells to examine
     * @return The average absolute difference
     */
    public static void updateGridWithZscores(DoubleGrid2D grid, ArrayList<Cell> cellsOfInterest)
    {
        // Get what we need to calculate mean and standard deviation
        double sum = 0.0;
        double sumOfSquares = 0.0;
        int n = cellsOfInterest.size();
        for (Cell c : cellsOfInterest)
        {
            double val = grid.field[c.x][c.y];
            sum += val;
            sumOfSquares += val * val;
        }
        double mean = sum / n;
        double sd = Math.sqrt(sumOfSquares/n - mean * mean);
        
        // Now calculate the z-scores and update the grid
        for (Cell c : cellsOfInterest)
        {
            grid.field[c.x][c.y] = (grid.field[c.x][c.y] - mean) / sd;
        }
    }
    
    /**
     * Convert the given array to z-scores.
     */
    public static void convertToZscores(double[] data) {

        double sum = 0.0;
        double sumOfSquares = 0.0;
        for (int i = 0; i < data.length; i++) {
        	sum += data[i];
        	sumOfSquares += data[i] * data[i];        	
        }

        double mean = sum / data.length;
        double sd = Math.sqrt(sumOfSquares/data.length - mean * mean);
        double invSd = 1.0 / sd;

        for (int i = 0; i < data.length; i++)
        	data[i] = (data[i] - mean) * invSd;
    }

    /**
     * Scale the given array of values so that they sum to 1. May not work
     * with negative values.
     */
    public static void normalizeValues(double[] data) {
        double sum = 0.0;
        for (int i = 0; i < data.length; i++)
        	sum += data[i];

        double invSum = 1.0 / sum;

        for (int i = 0; i < data.length; i++)
        	data[i] = data[i] * invSum;
    }
    
    
	public double utility(Map map, DoubleGrid2D first, DoubleGrid2D second)
	{
		
	    double sum = 0.0;
	    int missingCell = 0;
	        
	                    
	    for(int i = 0 ; i < Map.GRID_WIDTH ; ++i)
	    {
	        for(int j = 0 ; j < Map.GRID_HEIGHT; ++j)
	        {
	            if(map.getNationGrid().field[i][j] == Map.CANADA_CODE)
	            {
	                double tdes = first.field[i][j];
	                double popval = second.field[i][j];
	                if(popval != Map.MISSING)
	                {
	                    sum += euclideanDistance(tdes,popval);
	                }
	                else
	                    ++missingCell ;
	            }
	        }
	    }
	    
	    // FIXME do we need to use Map.GRID_WIDTH * Map.GRID_HEIGHT here?
	    sum = sum/((Map.GRID_WIDTH * Map.GRID_HEIGHT) - missingCell);
	        
	    // bigger rms value means less resemblance
	    // and ecj by default maximizes, so --
	        
	    return 1.0/Math.sqrt(sum);
	}
	
	
	private double euclideanDistance(double x, double y)
	{
		return (x-y)*(x-y);
	}

	

	public static DoubleGrid2D smoothGridWithMapConstraint(Map map, AbstractGrid2D srcGrid)
	{
		return Utility.smoothGridWithMapConstraint(map, srcGrid,
				Utility.KERNEL_WIDTH, Utility.KERNEL_HEIGHT,
				Utility.FILTER_SIGMA, Utility.FILTER_PASS);
	}
	

    public static DoubleGrid2D smoothGridWithMapConstraint(Map map, AbstractGrid2D srcGrid, 
            int kwidth, int kheight, double sigma, int pass)
    {
        DoubleGrid2D destGrid = new DoubleGrid2D(Map.GRID_WIDTH, Map.GRID_HEIGHT);
        // recalculate and save
        GaussianFilter gfilter = new GaussianFilter(kwidth, kheight, sigma);
        if(srcGrid instanceof DoubleGrid2D)
            // destGrid.field = Filter.applyFilterWithMapConstraint(
            destGrid.field = Filter.applyFastFilterWithMapConstraint(
                    ((DoubleGrid2D)srcGrid).field, gfilter, 
                    map.getNationGrid().field, Map.CANADA_CODE, 
                    pass, WIDTH_START, WIDTH_END, HEIGHT_START, HEIGHT_END);
        else if(srcGrid instanceof IntGrid2D)
            // destGrid.field = Filter.applyFilterWithMapConstraint(
            destGrid.field = Filter.applyFastFilterWithMapConstraint(
                    castToDoubleField((IntGrid2D)srcGrid), gfilter, 
                    map.getNationGrid().field, Map.CANADA_CODE, 
                    pass, WIDTH_START, WIDTH_END, HEIGHT_START, HEIGHT_END);
        return destGrid ;
    }
    
    public static DoubleGrid2D smoothGrid(Map map, AbstractGrid2D srcGrid, 
            int kwidth, int kheight, double sigma, int pass)
    {
    	DoubleGrid2D destGrid = new DoubleGrid2D(Map.GRID_WIDTH, Map.GRID_HEIGHT);
    	phases.util.GaussianFilter filter = new phases.util.GaussianFilter(kwidth,kheight,sigma);
    	if(srcGrid instanceof DoubleGrid2D)
            // destGrid.field = Filter.applyFilterWithMapConstraint(
            destGrid.field = filter.applyFilter(((DoubleGrid2D)srcGrid).field, pass, map);
        else if(srcGrid instanceof IntGrid2D)
            // destGrid.field = Filter.applyFilterWithMapConstraint(
            destGrid.field = filter.applyFilter(castToDoubleField((IntGrid2D)srcGrid), pass, map);
        return destGrid; 
    }

    /** 
     * Cast the field of an IntGrid2D to 2D double array.
     */
    public static double[][] castToDoubleField(IntGrid2D grid)
    {
        double[][] dest = new double[grid.field.length][grid.field[0].length] ;
        for (int i = 0 ; i < grid.field.length ; i++) 
        {
            for(int j = 0 ; j < grid.field[0].length ; j++) 
            {
                dest[i][j] = grid.field[i][j] ;
            }
        }
        return dest ;
    }

    /**
     * Convert the given int array to a double array.
     */
    public static double[] convertToDoubles(int[] array)
    {
    	double[] a = new double[array.length];
    	for (int i = 0; i < array.length; i++)
    		a[i] = (double)array[i];
    	
    	return a;    	
    }
    
    
	/**
	 * Randomly returns an index into the array in proportion to the size of values in 
	 * the given array probs. The values do not need to be normalized, but they do need
	 * to all be positive. Some of the values can be zero, but they can't all be zero.
	 * 
	 */
	public static int chooseStochastically(double [] probs, MersenneTwisterFast rand) {
		double [] cumulProbs = new double[probs.length];
		double total = 0;
		
		for (int i = 0; i < probs.length; i++) {
			total += probs[i];
			cumulProbs[i] = total;
		}
		
		int val = Arrays.binarySearch(cumulProbs, rand.nextDouble() * total);
		if (val < 0)
			val = -(val + 1);		// Arrays.binarySearch(...) returns (-(insertion point) - 1) if the key isn't found

		if (val == probs.length)
			System.out.format("Error: val:%d, total:%f\n", val, total);
		
		return val;
	}
	
}
