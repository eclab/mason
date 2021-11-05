/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.distribution;
import ec.util.MersenneTwisterFast;

/** This stub class simply calls the equivalent function in Distributions.java */

public class Weibull extends AbstractContinuousDistribution
    {
    double alpha;
    double beta;
        
    public Weibull(double alpha, double beta, MersenneTwisterFast random)
        {
        setState(alpha, beta);
        setRandomGenerator(random);
        }
        
    public double nextDouble()
        {
        return Distributions.nextWeibull(alpha, beta, randomGenerator);
        }

    public void setState(double alpha, double beta)
        {
        this.alpha = alpha;
        this.beta = beta;
        }

    public String toString() 
        {
        return this.getClass().getName()+"("+alpha+", " + beta + ")";
        }
    }
