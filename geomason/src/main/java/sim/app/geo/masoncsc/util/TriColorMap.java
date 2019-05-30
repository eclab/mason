package sim.app.geo.masoncsc.util;

import java.awt.Color;

import sim.util.gui.ColorMap;
import sim.util.gui.SimpleColorMap;

/**
 * TriColorMap is a gradient between three colors. It works the same as 
 * SimpleColorMap, and indeed, makes use of that class. 
 * For example:
 * -1 : red
 *  0 : white
 *  1 : blue
 *  
 *  Values between -1 and 0 get mapped to some shade of pink, while values 
 *  between 0 and 1 would get mapped to some shade of blue
 *  
 * @author Joey Harrison
 *
 */
public class TriColorMap implements ColorMap
{
	SimpleColorMap lowerHalf;
	SimpleColorMap upperHalf;
	double mid;
	
	public TriColorMap(double min, double mid, double max, Color minColor, Color midColor, Color maxColor) {
		this.mid = mid;
		lowerHalf = new SimpleColorMap(min, mid, minColor, midColor);
		upperHalf = new SimpleColorMap(mid, max, midColor, maxColor);
	}
	
	@Override
	public Color getColor(double level) {
		if (level < mid)
			return lowerHalf.getColor(level);
		else 
			return upperHalf.getColor(level);
	}

	@Override
	public int getRGB(double level) {
		if (level < mid)
			return lowerHalf.getRGB(level);
		else 
			return upperHalf.getRGB(level);
	}

	@Override
	public int getAlpha(double level) {
		if (level < mid)
			return lowerHalf.getAlpha(level);
		else 
			return upperHalf.getAlpha(level);
	}

	@Override
	public boolean validLevel(double level) {
		if (level < mid)
			return lowerHalf.validLevel(level);
		else 
			return upperHalf.validLevel(level);
	}

	@Override
	public double defaultValue() {
		return mid;
	}
}
