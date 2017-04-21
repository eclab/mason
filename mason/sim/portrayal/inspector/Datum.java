/*
  Copyright 2017 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.inspector;


/** 
	A Datum is a simple class which provides both a label and a value.
	It's used as an option in BarChartChartingPropertyInspector and
	in PieChartChartingPropertyInspector. 
*/

public class Datum
	{
	String label;
	double value;
	
	public Datum(String label, double value)
		{
		this.label = label;
		this.value = value;
		}
	
	public String getLabel() { return label; }
	public double getValue() { return value; }
	public String toString() { return "Datum[\"" + label + "\", " + value + "]"; }
	}