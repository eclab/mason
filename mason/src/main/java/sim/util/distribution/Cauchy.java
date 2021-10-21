/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.distribution;
import ec.util.MersenneTwisterFast;

/** This stub class simply calls the equivalent function in Distributions.java */

public class Cauchy extends AbstractContinuousDistribution
	{
	MersenneTwisterFast random;
	
	public Cauchy(MersenneTwisterFast random)
		{
		this.random = random;
		}
	
	public double nextDouble()
		{
		return Distributions.nextCauchy(random);
		}
	}