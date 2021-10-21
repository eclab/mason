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
	MersenneTwisterFast random;
	
	public Lambda(double l3, double l4, MersenneTwisterFast random)
		{
		this.l3 = l3;
		this.l4 = l4;
		this.random = random;
		}
	
	public double nextDouble()
		{
		return Distributions.nextLambda(l3, l4, random);
		}
	}