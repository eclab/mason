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
	MersenneTwisterFast random;
	
	public Weibull(double alpha, double beta, MersenneTwisterFast random)
		{
		this.alpha = alpha;
		this.beta = beta;
		this.random = random;
		}
	
	public double nextDouble()
		{
		return Distributions.nextWeibull(alpha, beta, random);
		}
	}