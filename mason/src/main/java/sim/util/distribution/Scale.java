/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.distribution;

/**
   Scale is a set of utility functions to transform a distribution into 
   another by multiplying its values and optionally translating them.
**/

public class Scale
    {
    public static AbstractContinuousDistribution scale(AbstractContinuousDistribution dist, double multiply)
        {
        return scale(dist, multiply, 0.0);
        }
                
    public static AbstractContinuousDistribution scale(final AbstractContinuousDistribution dist, final double multiply, final double add)
        {
        return new AbstractContinuousDistribution()
            {
            public double nextDouble()
                {
                return dist.nextDouble() * multiply + add;
                }
            };
        }

    public static AbstractDiscreteDistribution scale(AbstractDiscreteDistribution dist, int multiply)
        {
        return scale(dist, multiply, 0);
        }
                
    public static AbstractDiscreteDistribution scale(final AbstractDiscreteDistribution dist, final int multiply, final int add)
        {
        return new AbstractDiscreteDistribution()
            {
            public int nextInt()
                {
                return dist.nextInt() * multiply + add;
                }
            };
        }


    }
