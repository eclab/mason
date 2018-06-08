package CDI.src.environment;

import java.awt.Color;
import java.util.HashMap;

import sim.util.gui.SimpleColorMap;
/**
 * This color will map the level of 0 to transparent
 * @author Ermo Wei
 *
 */
public class DoubleColorMap extends SimpleColorMap{
	private SimpleColorMap upperColorMap;
	private SimpleColorMap lowerColorMap;
	private double lowerBound;
	private double upperBound;	
	private double midLevel;
	private Color midColor;
	private HashMap<Double, Color> extraColorsHashMap = new HashMap<Double, Color>();
	
	public DoubleColorMap(double minLevel, double midLevel, double maxLevel, Color minColor, Color midColor, Color maxColor)
	{
		super(0, 1, Color.WHITE, Color.BLACK); // the color of this doesn't matter
		upperColorMap = new SimpleColorMap(midLevel, maxLevel, midColor, maxColor);
		lowerColorMap = new SimpleColorMap(minLevel, midLevel, minColor, midColor);
		this.midLevel = midLevel;
		this.midColor = midColor;
	}
	

	public void setLevels(double minLevel, double midLevel, double maxLevel, Color minColor, Color midColor, Color maxColor) {
		upperColorMap.setLevels(midLevel, maxLevel, midColor, maxColor);
		lowerColorMap.setLevels(minLevel, midLevel, minColor, midColor);
		this.midLevel = midLevel;
		this.midColor = midColor;
	}
	
	public void addExtraColor(double value, Color c) {
		extraColorsHashMap.put(value, c);
	}

	
	public Color getColor(double level)
	{
		if(extraColorsHashMap.containsKey(level))
			return extraColorsHashMap.get(level);
		
		if(level>=this.midLevel)
			return upperColorMap.getColor(level);
		else
			return lowerColorMap.getColor(level);
	}

	public int getRGB(double level)
	{
		if(extraColorsHashMap.containsKey(level))
			return extraColorsHashMap.get(level).getRGB();
		
		if(level>=this.midLevel)
			return upperColorMap.getRGB(level);
		else
			return lowerColorMap.getRGB(level);
	}

	public int getAlpha(double level)
	{
		if(extraColorsHashMap.containsKey(level))
			return extraColorsHashMap.get(level).getAlpha();
		
		if(level>=this.midLevel)
			return upperColorMap.getAlpha(level);
		else
			return lowerColorMap.getAlpha(level);
	}

	public boolean validLevel(double level)
	{
		if(upperColorMap.validLevel(level)||lowerColorMap.validLevel(level))
			return true;
		return false;
	}	
}

