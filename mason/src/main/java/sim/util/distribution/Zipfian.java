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
	MersenneTwisterFast random;
	
	public Zipfian(double z, MersenneTwisterFast random)
		{
		this.z = z;
		this.random = random;
		}
	
	public int nextInt()
		{
		return Distributions.nextZipfInt(z, random);
		}
	}