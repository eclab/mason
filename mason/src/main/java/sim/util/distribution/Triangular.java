/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

/// Contributions by Vladimir Menkov under AFL 3.0

package sim.util.distribution;
import ec.util.MersenneTwisterFast;

/** This stub class simply calls the equivalent function in Distributions.java */

public class Triangular extends AbstractContinuousDistribution
    {
    /** The min, mode, and max */
    double min;
    double mode;
    double max;

    /** Creates a symmetric triangular distribution on [-1,1] */
    public Triangular(MersenneTwisterFast random)
        {
        setRandomGenerator(random);
        min = -1;
        mode = 0;
        max = 1;
        }
        
    /** Creates a skewed triangular distribution on [min, max] with the given mode in-between. */
    public Triangular(double min, double mode, double max, MersenneTwisterFast random)
        {
        setRandomGenerator(random);
        if (min > mode || mode > max) 
            {
            throw new IllegalArgumentException("Triangular distribution parameters (min,mode,max) not in order");
            }
        this.min = min;
        this.mode = mode;
        this.max = max;
        }
        
    public double nextDouble()
        {
        return Distributions.nextTriangular(min, mode, max, randomGenerator);
        }

    public String toString() 
        {
        return this.getClass().getName() + "(" + min + "," + mode + "," + max + ")";
        }

    /** Unit test */
    public static void main(String[] argv)  
        {
        if (argv.length != 3)
            throw new IllegalArgumentException("Usage: Triangular min mode max");
                
        double min=Double.parseDouble(argv[0]);
        double mode=Double.parseDouble(argv[1]);
        double max=Double.parseDouble(argv[2]);
        MersenneTwisterFast random = new MersenneTwisterFast();
        Triangular t = new Triangular(min, mode, max, random);
        System.out.println(t);
        for(int j = 0; j < 10; j++) 
            {
            System.out.println(t.nextDouble());
            }
        }

    }
