package environment;

import java.awt.Color;

import sim.util.gui.SimpleColorMap;
/**
 * This color will map the level of 0 to transparent
 * @author Ermo Wei
 *
 */
public class SmartDoubleColorMap extends SmartColorMap{
	private SmartColorMap maxColorMap;
	private SmartColorMap minColorMap;
	private double lowerBound;
	private double upperBound;

	public SmartDoubleColorMap(double scaleFactor, Color minColor, Color maxColor)
	{
		this(minColor, maxColor);
		this.scaleFactor = scaleFactor;
	}
	
	
	public SmartDoubleColorMap(Color minColor, Color maxColor)
	{
		super(0, 1, Color.WHITE, Color.BLACK);
		maxColorMap = new SmartColorMap(new Color(0,0,0,0), maxColor);
		minColorMap = new SmartColorMap(minColor, new Color(0,0,0,0));
	}
	
	/** 
	 * the two bounds can not be overlapped
	 * @param firstBounds
	 * @param secondBounds
	 * @return 
	 */
	public void setLevel(double[] bounds)
	{
		maxColorMap.setBoundsMinMax(0, bounds[1]);
		minColorMap.setBoundsMinMax(bounds[0], 0);
		lowerBound = bounds[0];
		upperBound = bounds[1];
	}
	
	public void setBoundsMinMax(double[] bounds)
	{
		this.setBoundsMinMax(bounds[0], bounds[1]);
	}
	
	public void setBoundsMinMax(double minLevel, double maxLevel)
	{
		lowerBound = minLevel;
		upperBound = maxLevel;
		maxColorMap.setBoundsMinMax(0, maxLevel);
		minColorMap.setBoundsMinMax(minLevel, 0);
	}
	
	public Color getColor(double level)
	{
		if(level>=0)
			return maxColorMap.getColor(level);
		else
			return minColorMap.getColor(level);
	}

	public int getRGB(double level)
	{
		if(level>=0)
			return maxColorMap.getRGB(level);
		else
			return minColorMap.getRGB(level);
	}

	public int getAlpha(double level)
	{
		if(level>=0)
			return maxColorMap.getAlpha(level);
		else
			return minColorMap.getAlpha(level);
	}

	public boolean validLevel(double level)
	{
		if(maxColorMap.validLevel(level)||minColorMap.validLevel(level))
			return true;
		return false;
	}

	@Override
	public double filterLevel(double level) {
		System.out.println("filterlevel");
		if(level>=0)
			return Math.pow(level, scaleFactor);
		else {
			return -Math.pow(-level, scaleFactor);
		}
	};
	
	

	
	
}
