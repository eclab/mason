/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.distribution;
import ec.util.MersenneTwisterFast;

/** This stub class simply calls the equivalent function in Distributions.java */

public class Burr8 extends AbstractContinuousDistribution
    {
    double r;
        
    public Burr8(double r, MersenneTwisterFast random)
        {
        setState(r);
        setRandomGenerator(random);
        }
        
    public double nextDouble()
        {
        // yes, *Burr1*
        return Distributions.nextBurr1(r, 8, randomGenerator);
        }

    public void setState(double r)
        {
        this.r = r;
        }

    public String toString() 
        {
        return this.getClass().getName()+"("+r + ")";
        }
    }
