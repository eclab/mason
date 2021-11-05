/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.distribution;
import ec.util.MersenneTwisterFast;

/** This stub class simply calls the equivalent function in Distributions.java */

public class Burr5 extends AbstractContinuousDistribution
    {
    double r;
    double k;
        
    public Burr5(double r, double k, MersenneTwisterFast random)
        {
        setState(r, k);
        setRandomGenerator(random);
        }
        
    public double nextDouble()
        {
        // yes, *Burr2*
        return Distributions.nextBurr2(r, k, 5, randomGenerator);
        }

    public void setState(double r, double k)
        {
        this.r = r;
        this.k = k;
        }

    public String toString() 
        {
        return this.getClass().getName()+"("+r + ", " + k + ")";
        }
    }
