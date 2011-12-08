/**
 ** StatsTools.java
 **
 ** Copyright 2011 by Andrew Crooks, Joseph Harrison, Mark Coletti, Cristina Metgher
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 **/
package sim.app.geo.sickStudents;

public class StatsTools
{
	static public double calcLognormalMu(double mean, double stdev) {
		return Math.log(mean) - 0.5 * Math.log(1.0 + (stdev*stdev) / (mean*mean));
	}
	
	static public double calcLognormalSigma(double mean, double stdev) {
		return Math.sqrt(Math.log((stdev*stdev) / (mean*mean) + 1.0));
	}
}
