/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.distribution;
import ec.util.MersenneTwisterFast;

/** This stub class simply calls the equivalent function in Distributions.java */

public class Zipfian extends AbstractDiscreteDistribution
    {
    double z;
        
    public Zipfian(double z, MersenneTwisterFast random)
        {
        setState(z);
        setRandomGenerator(random);
        }
        
    public int nextInt()
        {
        return Distributions.nextZipfInt(z, randomGenerator);
        }

    public void setState(double z)
        {
        this.z = z;
        }

    public String toString() 
        {
        return this.getClass().getName()+"("+z+")";
        }
    }
