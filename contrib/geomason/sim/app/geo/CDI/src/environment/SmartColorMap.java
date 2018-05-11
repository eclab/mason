package CDI.src.environment;

import java.awt.Color;
import java.util.HashMap;

import CDI.src.movement.parameters.Parameters;
import sim.util.gui.SimpleColorMap;

/**
 * SmartColorMap extends SimpleColorMap and adds functions for setting the bounds
 * based on a given data set. It can use the min and max of the dataset or some
 * given number of standard deviations from the mean.
 * 
 * @author Joey Harrison
 * @see SimpleColorMap
 *
 */
public class SmartColorMap extends SimpleColorMap
{
	private Color minColor;
	private Color maxColor;
	public double scaleFactor = 1.0;
	
	
	
	public SmartColorMap(double scaleFactor, Color minColor, Color maxColor)
	{
		this(minColor, maxColor);
		this.scaleFactor = scaleFactor;
	}
	
	public SmartColorMap(Color minColor, Color maxColor)
	{
		// we just put some default value here for the minLevel and maxLevel
		super(0, 1, minColor, maxColor);
		this.minColor = minColor;
		this.maxColor = maxColor;
	}
	
	
	public SmartColorMap(double minLevel, double maxLevel, Color minColor, Color maxColor) {
		super(minLevel, maxLevel, minColor, maxColor);
		this.minColor = minColor;
		this.maxColor = maxColor;
	}
	
	public SmartColorMap(double[] data, Color minColor, Color maxColor) {
		setBoundsMinMax(data, minColor, maxColor);
		this.minColor = minColor;
		this.maxColor = maxColor;
	}
	
	public SmartColorMap(int[] data, Color minColor, Color maxColor) {
		setBoundsMinMax(data, minColor, maxColor);
		this.minColor = minColor;
		this.maxColor = maxColor;
	}
	
	public SmartColorMap(double[] data, double stdDevs, Color minColor, Color maxColor) {
		setBoundsStdDev(data, stdDevs, minColor, maxColor);
		this.minColor = minColor;
		this.maxColor = maxColor;
	}
	
	public void setBoundsMinMax(double minLevel, double maxLevel)
	{
		this.setLevels(minLevel, maxLevel, minColor, maxColor);
	}
	

	public void setBoundsMinMax(double[] data, Color minColor, Color maxColor) {
		if ((data == null) || (data.length == 0))
			return; 
		
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		
		for (double x : data) {
			if (x < min)
				min = x;
			if (x > max)
				max = x;
		}
		
		setLevels(min, max, minColor, maxColor);
	}

	public void setBoundsMinMax(int[] data, Color minColor, Color maxColor) {
		if ((data == null) || (data.length == 0))
			return; 
		
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		
		for (int x : data) {
			if (x < min)
				min = x;
			if (x > max)
				max = x;
		}
		
		setLevels(min, max, minColor, maxColor);
	}
	
	
	public void setBoundsStdDev(double[] data, double stdDevs, Color minColor, Color maxColor) {
		if ((data == null) || (data.length == 0))
			return;
		
		// calculate mean
		double sum = 0;
		for (double x : data)
			sum += x;
		
		double mean = sum / data.length;
		
		// calculate total variance
		double totalVar = 0;
		for (double x : data)
			totalVar += (mean - x) * (mean - x);
		
		// calculate standard deviation
		double stdDev = Math.sqrt(totalVar / data.length);
		
		double min = mean - stdDev * stdDevs;
		double max = mean + stdDev * stdDevs;

		setLevels(min, max, minColor, maxColor);
	}
	
	public void setBoundsStdDev(int[] data, double stdDevs, Color minColor, Color maxColor) {
		if ((data == null) || (data.length == 0))
			return;
		
		// calculate mean
		double sum = 0;
		for (int x : data)
			sum += x;
		
		double mean = sum / data.length;
		
		// calculate total variance
		double totalVar = 0;
		for (int x : data)
			totalVar += (mean - x) * (mean - x);
		
		// calculate standard deviation
		double stdDev = Math.sqrt(totalVar / data.length);
		
		double min = mean - stdDev * stdDevs;
		double max = mean + stdDev * stdDevs;

		setLevels(min, max, minColor, maxColor);
	}
	
	
	 
	@Override
	public double filterLevel(double level) {
		return Math.pow(level, scaleFactor);
	}
  
	
}
