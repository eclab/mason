package sim.app.geo.masoncsc.util;

import java.util.Arrays;

import ec.util.MersenneTwisterFast;

/**
 * This class provides some useful functions for certain statistical operations, including:
 * - Drawing from a lognormal distribution.
 * 
 * @author Joey Harrison
 * 
 */
public class Stats
{
	static public double calcLognormalMu(double mean, double stdev) {
		return Math.log(mean) - 0.5 * Math.log(1.0 + (stdev*stdev) / (mean*mean));
	}
	
	static public double calcLognormalSigma(double mean, double stdev) {
		return Math.sqrt(Math.log((stdev*stdev) / (mean*mean) + 1.0));
	}
	
	/**
	 * Convert a random variable drawn from a normal (Gaussian) distribution to
	 * one that is lognormally distributed.
	 * @param mu 
	 * @param sigma
	 * @param normalVal random number drawn from a normal distribution.
	 * @return
	 */
	static public double normalToLognormal(double mu, double sigma, double normalVal) {
		return Math.exp(mu + sigma*normalVal);
	}
  

	/**
	 * Calculate the Pearson product-moment correlation coefficient.
	 * Code adapted from here: http://stackoverflow.com/a/17448499
	 */
	static public double calcCorrelation(double[] x, double[] y) {
		double sumX = 0;
		double sumX2 = 0;
		double sumY = 0;
		double sumY2 = 0;
		double sumXY = 0;

		int n = x.length < y.length ? x.length : y.length;

		for (int i = 0; i < n; ++i) {
			double _x = x[i];
			double _y = y[i];

			sumX += _x;
			sumX2 += _x * _x;
			sumY += _y;
			sumY2 += _y * _y;
			sumXY += _x * _y;
		}

		double stdX = Math.sqrt(sumX2 / n - sumX * sumX / n / n);
		double stdY = Math.sqrt(sumY2 / n - sumY * sumY / n / n);
		double covariance = (sumXY / n - sumX * sumY / n / n);

		return covariance / stdX / stdY;
	}
	
    /**
     * Calculate Kullback-Liebler divergence. Returns the KL divergence, K(p1 || p2).
     *
     * The log is w.r.t. base 2. <p>
     *
     * *Note*: If any value in <tt>p2</tt> is <tt>0.0</tt> then the KL-divergence
     * is <tt>infinite</tt>. Limin changes it to zero instead of infinite. 
     * 
     * @return KL Divergence. Lower values reflect more similarity.
     * 
     */
    static public double klDivergence(double[] p1, double[] p2) {
        double klDiv = 0.0;

        for (int i = 0; i < p1.length; ++i) {
            if ((p1[i] == 0.0) || (p2[i] == 0.0)) 
                continue;

            if (p1[i] < 0.0 || p2[i] < 0.0) {
                System.err.println("Error: negative input to klDivergence");
                return Double.NaN;
            }
            
            klDiv += p1[i] * Math.log(p1[i] / p2[i]);
        }

        return klDiv / Math.log(2); 
    }
    
    /**
     * Calculate the Empirical Cumulative Distribution Function (ECDF) of the 
     * given distribution.
     * @param a Array of doubles, assumed to be sorted.
     * @return Array of doubles containing cumulative probabilities
     */
    static public double[][] calcECDF(double[] a) {
    	final int n = a.length;
    	final double increment = 1 / (double)n;
    	
    	// remove duplicates
    	int numNonDupes = 1;
    	for (int i = 1; i < n; i++) {
    		if (a[i] != a[i-1])
    			numNonDupes++;
    	}

    	double[] x = new double[numNonDupes];
    	double[] y = new double[numNonDupes];
    	
    	int index = 0;
    	for (int i = 1; i < n; i++) {
    		if (a[i] != a[i-1]) {
    			x[index] = a[i-1];
    			y[index] = i * increment;
    			index++;
    		}
    	}
    	
    	// add last value
    	x[index] = a[n-1];
    	y[index] = 1.0;
    	    	
    	return new double[][] { x, y };
    }
    
    /**
     * Return the value that 
     * @param x
     * @param y
     * @param val
     * @return
     */
    static private double binarySearchECDF(double[] x, double[] y, double val) {
		int index = Arrays.binarySearch(x, val);
		if (index < 0) {	// if exact value wasn't found
			if (index == -1)	// value is below min x
				return 0.0;
		
			index = -(index + 1);		// Arrays.binarySearch(...) returns (-(insertion point) - 1) if the key isn't found
			
			if (index >= y.length)		// value is above max x
				return 1.0;
			
			return y[index-1];	// return the nearest value smaller than val
		}
		
		return y[index];
    }
    
    /**
     * Calculate the area between the ECDF of the two given distributions.
     * @param a 
     * @param b
     * @param useAbs Use the absolute value of the area. If false, criss-crossing sections
     * of the ECDF will have different signs and cancel each other out. This is exact same 
     * the difference between the means. If true, all area between the curves is considered 
     * positive and added up.
     * @return The area. 
     */
    static public double calcAreaBetweenECDFs(double a[], double b[], boolean useAbs) {
    	Arrays.sort(a);
    	Arrays.sort(b);
    	
    	double[][] ecdfA = calcECDF(a);
    	double[][] ecdfB = calcECDF(b);

    	double[] ax = ecdfA[0];
    	double[] bx = ecdfB[0];
    	
    	// Combine and sort the x values
    	// note: this could be sped up by looping through both arrays at the same time instead of calling sort
    	// note: there may be duplicates, but not more than one per value 
    	double[] combinedX = new double[ax.length + bx.length];
    	for (int i = 0; i < ax.length; i++)
    		combinedX[i] = ax[i];
    	for (int i = 0; i < bx.length; i++)
    		combinedX[ax.length + i] = bx[i];
    	Arrays.sort(combinedX);
    	
    	double total = 0;
    	double lastX = combinedX[0];
    	double x, width, height, yA, yB;
    	
    	for (int i = 0; i < combinedX.length; i++) {
    		x = combinedX[i];
    		width = x - lastX;

    		// note: this can be optimized by not doing binary searches and by instead keeping track of each one's last x val
    		yA = binarySearchECDF(ax, ecdfA[1], lastX);
    		yB = binarySearchECDF(bx, ecdfB[1], lastX);

    		height = yA - yB;
    		if (useAbs)
    			height = Math.abs(height);

    		total += width * height;
    		
//    		System.out.format("lastX: %f, x: %f, ya: %f, yb: %f, w: %f, h: %f, current: %f, total: %f\n", lastX, x, yA, yB, width, height, (width*height),total);
    		
    		lastX = x;
    	}
    	
    	return total;    	
    }
    

    /**
     * Calculate the largest vertical distance between the ECDFs of the given distributions.
     * This should be the same as the Kolmogorov-Smirnov statistic.
     * @param a
     * @param b
     * @return
     */
    static public double calcMaxVerticalDistanceBetweenECDFs(double a[], double b[]) {
    	Arrays.sort(a);
    	Arrays.sort(b);
    	
    	double[][] ecdfA = calcECDF(a);
    	double[][] ecdfB = calcECDF(b);

    	double[] ax = ecdfA[0];
    	double[] bx = ecdfB[0];
    	
    	double maxHeight = Double.NEGATIVE_INFINITY;
    	
    	// Combine and sort the x values
    	// note: this could be sped up by looping through both arrays at the same time instead of calling sort
    	// note: there may be duplicates, but not more than one per value 
    	double[] combinedX = new double[ax.length + bx.length];
    	for (int i = 0; i < ax.length; i++)
    		combinedX[i] = ax[i];
    	for (int i = 0; i < bx.length; i++)
    		combinedX[ax.length + i] = bx[i];
    	Arrays.sort(combinedX);
    	
    	double lastX = combinedX[0];
    	double x, height, yA, yB;
    	
    	for (int i = 0; i < combinedX.length; i++) {
    		x = combinedX[i];

    		// note: this can be optimized by not doing binary searches and by instead keeping track of each one's last x val
    		yA = binarySearchECDF(ax, ecdfA[1], lastX);
    		yB = binarySearchECDF(bx, ecdfB[1], lastX);

   			height = Math.abs(yA - yB);
   			if (height > maxHeight)
   				maxHeight = height;

   			lastX = x;
    	}
    	
    	return maxHeight;    	
    }

    
    /**
     * Calculate the area between the ECDF of the two given distributions.
     * @param a 
     * @param b
     * @return The area. 
     */
    static public double calcAreaBetweenECDFs(double a[], double b[]) {
    	return calcAreaBetweenECDFs(a, b, true);
    }
    
    static public double mean(double[] a) {
    	double total = 0;
    	for (int i = 0; i < a.length; i++)
    		total += a[i];
    	
    	return total / a.length;
    }
    
    private static void testLogNormal() {
		MersenneTwisterFast random = new MersenneTwisterFast();		// when using MASON, this is already in SimState
		double mean = 1.45;
		double stdev = 0.5;
		double mu = Stats.calcLognormalMu(mean, stdev);
		double sigma = Stats.calcLognormalSigma(mean, stdev);
		
		for (int i = 0; i < 1000; i++) {
			double x = Stats.normalToLognormal(mu, sigma, random.nextGaussian());		// This is how to use this with MASON
			System.out.format("%.6f\n", x);
		}
    }
    
    private static void testECDF() {
    	double[] a = { 1, 2, 1, 2 };
    	double[] b = { 0, 1, 5, 6 };
    	
    	Arrays.sort(b);    	
    	double[][] ecdf = calcECDF(b);
    	
    	int n = ecdf[0].length;
    	for (int i = 0; i < n; i++) {
    		System.out.format("%6f, %6f\n", ecdf[0][i], ecdf[1][i]);
    	}
    	
    	System.out.format("Area between ECDFs: %f, difference between means: %f\n", calcAreaBetweenECDFs(a, b, true), (mean(b)-mean(a)));
    }

	
	public static void main(String[] args) {

		testECDF();
	}
}
