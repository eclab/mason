/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.distribution;
import ec.util.MersenneTwisterFast;

/** This stub class simply calls the equivalent function in Distributions.java */

public class Lambda extends AbstractContinuousDistribution
    {
    double l3;
    double l4;
        
    public Lambda(double l3, double l4, MersenneTwisterFast random)
        {
        setState(l3, l4);
        setRandomGenerator(random);
        }
        
    public double nextDouble()
        {
        return Distributions.nextLambda(l3, l4, randomGenerator);
        }

    public void setState(double l3, double l4)
        {
        this.l3 = l3;
        this.l4 = l4;
        }

    public String toString() 
        {
        return this.getClass().getName()+"("+l3+", " + l4 + ")";
        }
    }
