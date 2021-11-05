/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.distribution;
import ec.util.MersenneTwisterFast;

/** This stub class simply calls the equivalent function in Distributions.java */

public class PowLaw extends AbstractContinuousDistribution
    {
    double alpha;
    double cut;
        
    public PowLaw(double alpha, double cut, MersenneTwisterFast random)
        {
        setState(alpha, cut);
        setRandomGenerator(random);
        }
        
    public double nextDouble()
        {
        return Distributions.nextPowLaw(alpha, cut, randomGenerator);
        }

    public void setState(double alpha, double cut)
        {
        this.alpha = alpha;
        this.cut = cut;
        }

    public String toString() 
        {
        return this.getClass().getName()+"("+alpha+", " + cut + ")";
        }
    }
