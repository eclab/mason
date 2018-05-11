package CDI.src.migration.util;

import java.util.HashMap;

import org.apache.commons.math3.distribution.WeibullDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import ec.util.MersenneTwisterFast;
import sim.util.Double2D;

/**
 * Simulation from various distributions with the Apache commons math library.
 * Creates and maintains a distribution object with the given seed for each
 * set of parameters requested.
 * 
 * @author Eric 'Siggy' Scott
 * @author Ahmed Elmolla
 */
public class Distributions
{
    RandomGenerator generator;
    HashMap<Double2D, LogNormalDistribution> logNormalInstances;
    HashMap<Double2D, WeibullDistribution> weibullInstances;
    HashMap<Double, ExponentialDistribution> exponentialInstances;
    HashMap<Double2D, NormalDistribution> normalInstances;
    
    
    public Distributions(final MersenneTwisterFast mtf)
    {
    	generator = new RandomGenerator()
		{
    		public void setSeed(int seed) { mtf.setSeed(seed); }
    		public void setSeed(int[] seed) { mtf.setSeed(seed); }
    		public void setSeed(long seed) { mtf.setSeed(seed); }
    		public void nextBytes(byte[] bytes) { mtf.nextBytes(bytes); }
    		public int nextInt() { return mtf.nextInt(); }
    		public int nextInt(int n) { return mtf.nextInt(n); }
    		public long nextLong() { return mtf.nextLong(); } 
    		public boolean nextBoolean() { return mtf.nextBoolean(); }
    		public float nextFloat() { return mtf.nextFloat(); }
    		public double nextDouble() { return mtf.nextDouble(); }
    		public double nextGaussian() { return mtf.nextGaussian(); }
		};
		
        logNormalInstances = new HashMap<Double2D, LogNormalDistribution>();
        weibullInstances = new HashMap<Double2D, WeibullDistribution>();
        exponentialInstances = new HashMap<Double, ExponentialDistribution>();
        normalInstances = new HashMap<Double2D, NormalDistribution>();
    }
    
    public double lognormalSample(double mu, double sigma)
    {
        Double2D parameters = new Double2D(mu, sigma);
        LogNormalDistribution dist;
        if (logNormalInstances.containsKey(parameters))
            dist = logNormalInstances.get(parameters);
        else
        {
            dist = new LogNormalDistribution(generator, mu, sigma);
            logNormalInstances.put(parameters, dist);
        }
        
        return dist.sample();
    }
    
    public double weibullSample(double shape, double scale)
    {
        Double2D parameters = new Double2D(shape, scale);
        WeibullDistribution dist;
        if (weibullInstances.containsKey(parameters))
            dist = weibullInstances.get(parameters);
        else
        {
            dist = new WeibullDistribution(generator, shape, scale);
            weibullInstances.put(parameters, dist);
        }
        
        return dist.sample();
    }
    
    public double poissonIntervalSample(double interval)
    {
        ExponentialDistribution dist;
        if (exponentialInstances.containsKey(interval))
            dist = exponentialInstances.get(interval);
        else
        {
            dist = new ExponentialDistribution(generator, interval);
            exponentialInstances.put(interval, dist);
        }
        
        return dist.sample();
    }
    
    public double gaussianSample(double mean, double sd)
    {
        Double2D parameters = new Double2D(mean, sd);
        NormalDistribution dist;
        if (normalInstances.containsKey(parameters))
            dist = normalInstances.get(parameters);
        else
        {
            dist = new NormalDistribution(generator, mean, sd);
            normalInstances.put(parameters, dist);
        }
        
        return dist.sample();
    }
}
