/**
 ** StatsTools.java
 **
 ** Copyright 2011 by Joseph Harrison, Mark Coletti, Cristina Metgher, Andrew Crooks
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 ** *$Id$
 **/
package sickStudents;

public class StatsTools
{
	static public double calcLognormalMu(double mean, double stdev) {
		return Math.log(mean) - 0.5 * Math.log(1.0 + (stdev*stdev) / (mean*mean));
	}
	
	static public double calcLognormalSigma(double mean, double stdev) {
		return Math.sqrt(Math.log((stdev*stdev) / (mean*mean) + 1.0));
	}
	
	/**
	 * Convert a random variable drawn from a normal (Gaussian) distribution to
	 * one that is lognormally distributed.
	 * @param mu 
	 * @param sigma
	 * @param normalVal random number drawn from a normal distribution.
	 * @return
	 */
	static public double normalToLognormal(double mu, double sigma, double normalVal) {
		return Math.exp(mu + sigma*normalVal);
	}
}
