package migration.util;

import java.util.HashMap;
import sim.util.Double2D;

/**
 * A speedier logistic function.  We discretize the function by pre-computing
 * its value for 1000 inputs between 0.0 and 1.0.  Doing this is roughly five
 * times faster than computing the function directly each time it is called.
 * Since this is used by 10^5 to 10^7 Household agents, it's worth the added
 * complexity.
 * 
 * @author Eric 'Siggy' Scott
 */
public class CachedSigmoid
{
    private static HashMap<Double2D, CachedSigmoid> hashOfCaches;
    
    private double[] sigmoidCache;
    private double beta;
    private double offset;
    
    private CachedSigmoid(Double2D parameters)
    {
        assert(parameters != null);
        
        this.beta = parameters.x;
        this.offset = parameters.y;
        initializeSigmoidCache();
        hashOfCaches.put(parameters, this);
    }
    
    public synchronized static CachedSigmoid getInstance(double beta, double offset)
    {
        if (hashOfCaches == null)
            hashOfCaches = new HashMap<Double2D, CachedSigmoid>();
        
        Double2D key = new Double2D(beta, offset);
        if (hashOfCaches.containsKey(key))
            return hashOfCaches.get(key);
        else
            return new CachedSigmoid(key);
    }
    public double get(double x)
    {
        return sigmoidCache[((int) (1000 * x))];
    }
    
    private void initializeSigmoidCache()
    {
        sigmoidCache = new double[1001];
        for (int i = 0; i <= 1000; i += 1)
            sigmoidCache[i] = Misc.sigmoid(i/1000.0, beta, offset);
    }
}
