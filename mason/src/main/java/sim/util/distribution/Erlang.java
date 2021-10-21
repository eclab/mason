/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.distribution;
import ec.util.MersenneTwisterFast;

/** This stub class simply calls the equivalent function in Distributions.java */

public class Erlang extends AbstractContinuousDistribution
	{
	double variance;
	double mean;
	MersenneTwisterFast random;
	
	public Erlang(double variance, double mean, MersenneTwisterFast random)
		{
		this.variance = variance;
		this.mean = mean;
		this.random = random;
		}
	
	public double nextDouble()
		{
		return Distributions.nextErlang(variance, mean, random);
		}
	}