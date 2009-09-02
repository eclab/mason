/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.gui;
import java.awt.Color;
import sim.util.*;

/**
 * Maps numerical levels to colors using either a lookup table, color interpolation, or both.
 * A simple implementation of the ColorMap interface.
 *
 * <ol>
 * <li> Method 1: a color table.  The user can provide an array of Colors; if the numerical value,
 * cast into an integer, is between 0 and (the length of this array - 1), then the appropriate Color is returned.
 *
 * <li> Method 2: color interpolation.  The user can provide a min-level, min-Color, max-level, and max-Color.
 * If the numerical value is below min-level, then minColor is provided.  If it's above max-level, then max-Color
 * is provided.  If it's between min-level and max-level, then a linear interpolation between min-Color and
 * max-Color is provided.
 * </ol>
 *
 * <p>The user can provide both a color table <i>and</i> an interpolation; in this case, the color table takes
 * precedence over the interpolation in that region where the color table is relevant.  You specify a color
 * table with setColorTable(), and you specify an interpolation range with setLevels().
 *
 * <p>validLevel() is set to return true if the level range is between min-level and max-level, or
 * if it's inside the color table range.
 *
 * <p>defaultValue() is set to return 0 if the color table exists, else min-level is provided.
 *
 * @author Sean Luke
 * 
 */

// Color interpolation relies on a cache so we don't make billions of colors.  The slightly slower cache,
// turned on by default, lets us search to a bucket, then wander through the bucket to find the right Color.
// The faster cache creates a hard-set Color array.  This has lower resolution in colors (probably 1/4 the
// true Color values) than the slower cache, but it's faster in lookup by a bit.  I've commented out the
// faster cache, but you can see how it roughly works.  


public class SimpleColorMap implements ColorMap 
    {
    public int minRed = 0;
    public int minBlue = 0;
    public int minGreen = 0;
    public int minAlpha = 0;
    public int maxRed = 0;
    public int maxBlue = 0;
    public int maxGreen = 0;
    public int maxAlpha = 0;
    public double maxLevel = 0;
    public double minLevel = 0;
    public final Color clearColor = new Color(0,0,0,0);
    public Color minColor = clearColor;  // used when minLevel = maxLevel
    public Color maxColor = clearColor;
    
    public static final int COLOR_DISCRETIZATION = 257;
    
    /** User-provided color table */
    public Color[] colors;
    
    //      (The slower cache for color interpolation)
    Bag[] colorCache = new Bag[COLOR_DISCRETIZATION];

    //      (The faster cache)
    //      Color[] colorCache2 = new Color[COLOR_DISCRETIZATION];  
    
    /** Constructs a ColorMap that gradiates from 0.0 -> black to 1.0 -> white.  
        Values higher than 1.0 are mapped to white.  
        Values less than 0.0 are mapped to black. 
        Values from 0.0 through 1.0 are considered valid levels by validLevel(...). 
        The default value is 0.0 (for defaultValue() ).
    */
    public SimpleColorMap()
        {
        setLevels(0,1,Color.black,Color.white);
        }
    
    /** Constructs a ColorMap that gradiates from minLevel -> minColor to maxLevel -> maxColor.  
        Values higher than maxLevel are mapped to maxColor.  
        Values less than minLevel are mapped to minColor. 
        Values from minLevel through maxLevel are considered valid levels by validLevel(...). 
        The default value is minLevel (for defaultValue() ).
    */
    public SimpleColorMap(double minLevel, double maxLevel, Color minColor, Color maxColor)
        {
        setLevels(minLevel,maxLevel,minColor,maxColor);
        }
    
    /** Given an array of size n, constructs a ColorMap that maps integers from 0 to n-1 to the colors in the array.
        Any real-valued number x, for 0 <= x < n, is converted into an integer (with floor()) and then mapped to an array color.
        For all other values, black is returned. 
        Values from x through n, not including n, are considered valid levels by validLevel(...).
        The default value is 0 (for defaultValue() ).
    */
    public SimpleColorMap(Color[] colorTable)
        {
        setColorTable(colorTable);
        }
        
    /** Given an array of size n, constructs a ColorMap that maps integers from 0 to n-1 to the colors in the array,
        and gradiates from minLevel -> minColor to maxLevel -> maxColor for certain other values.
        Any real-valued number x, for 0 <= x < n, is converted into an integer (with floor()) and then mapped to an array color.
        Outside this range, gradiation occurs for minLevel <= x <= maxLevel.
        For any other value of x, values higher than maxLevel are mapped to maxColor,
        and values less than minLevel are mapped to minColor.
        Values from x through n, not including n, and additionally values from minLevel through maxLevel,
        are considered valid levels by validLevel(...)
        The default value is 0 (for defaultValue() ).
    */
    public SimpleColorMap(Color[] colorTable, double minLevel, double maxLevel, Color minColor, Color maxColor)
        {
        setColorTable(colorTable);
        setLevels(minLevel,maxLevel,minColor,maxColor);
        }

    /** Sets the color levels for the ValueGridPortrayal2D values for use by the default getColor(...)
        method.  These are overridden by any array provided in setColorTable().  If the value in the IntGrid2D or DoubleGrid2D
        is less than or equal to minLevel, then minColor is used.  If the value is greater than or equal to maxColor, then
        maxColor is used.  Otherwise a linear interpolation from minColor to maxColor is used. */
    public void setLevels(double minLevel, double maxLevel, Color minColor, Color maxColor)
        {
        if (maxLevel < minLevel) throw new RuntimeException("maxLevel cannot be less than minLevel");
        minRed = minColor.getRed(); minGreen = minColor.getGreen(); minBlue = minColor.getBlue(); minAlpha = minColor.getAlpha();
        maxRed = maxColor.getRed(); maxGreen = maxColor.getGreen(); maxBlue = maxColor.getBlue(); maxAlpha = maxColor.getAlpha();
        this.maxLevel = maxLevel; this.minLevel = minLevel;
        this.minColor = minColor;
        this.maxColor = maxColor;

        // reset cache
        // (the slower cache)
        for(int x=0;x<COLOR_DISCRETIZATION;x++) colorCache[x] = new Bag();
                
        // (the faster cache)
        //              for(int x=0;x<COLOR_DISCRETIZATION;x++) 
        //                      {
        //                      colorCache2[x] =
        //                      new Color( x * (maxColor.getRed()-minColor.getRed()) / (COLOR_DISCRETIZATION-1) + minColor.getRed(), 
        //                              x * (maxColor.getGreen()-minColor.getGreen()) / (COLOR_DISCRETIZATION-1) + minColor.getGreen(), 
        //                              x * (maxColor.getBlue()-minColor.getBlue()) / (COLOR_DISCRETIZATION-1) + minColor.getBlue(), 
        //                              x * (maxColor.getAlpha()-minColor.getAlpha()) / (COLOR_DISCRETIZATION-1) + minColor.getAlpha());
        //                      }
        }
        
    /** Specifies that if a value (cast into an int) in the IntGrid2D or DoubleGrid2D falls in the range 0 ... colors.length,
        then that index in the colors table should be used to represent that value.  Otherwise, values in
        setLevels(...) are used.  You can remove the color table by passing in null here.  Returns the old color table. */
    public Color[] setColorTable(Color[] colorTable)
        {
        Color[] retval = colors;
        colors = colorTable;
        return retval;
        }
        
    /** Override this if you'd like to customize the color for values in the portrayal.  The default version
        looks up the value in the colors[] table, else computes the interpolated color and grabs it out of
        a predefined color cache (there can't be more than about 1024 or so interpolated colors, max). 
    */
    
    public Color getColor(double level)
        {
        if (colors != null && level >= 0 && level < colors.length)
            {
            return colors[(int)level];
            }
        else
            {
            if (level > maxLevel) level = maxLevel;
            else if (level < minLevel) level = minLevel;
            if (level == minLevel) return minColor;  // so we don't divide by zero (maxLevel - minLevel)
            else if (level == maxLevel) return maxColor;  // so we don't overflow
            
            final double interpolation = (level - minLevel) / (maxLevel - minLevel);
            
            // the +1's beow are because the only way you can get the maxColor is if you have EXACTLY the maxLevel --
            // that's an incorrect discretization distribution.  Instead we return the maxColor if you have the maxLevel,
            // and otherwise we'd like to round it.
            // ... hope that's right!  -- Sean
            
            // look up color in cache
            // (the slower cache)
            final int alpha = (maxAlpha == minAlpha ? minAlpha : (int)(interpolation * (maxAlpha - minAlpha + 1) + minAlpha));
            if (alpha==0) return clearColor;
            final int red = (maxRed == minRed ? minRed : (int)(interpolation * (maxRed - minRed + 1) + minRed));
            final int green = (maxGreen == minGreen ? minGreen : (int)(interpolation * (maxGreen - minGreen + 1) + minGreen));
            final int blue = (maxBlue == minBlue ? minBlue : (int)(interpolation * (maxBlue - minBlue + 1) + minBlue));
            final int rgb = (alpha << 24) | (red << 16) | (green << 8) | blue;
            Bag colors = colorCache[(int)(interpolation * (COLOR_DISCRETIZATION-1))];
            for(int x=0;x<colors.numObjs;x++)
                {
                Color c = (Color)(colors.objs[x]);
                if (c.getRGB()==rgb)  // it's the right color
                    return c;
                }
            Color c = new Color(rgb,(alpha!=0));
            colors.add(c);
            return c;
                        
            // (the faster cache)
            //                      return colorColorCache2[(int)(interpolation * (COLOR_DISCRETIZATION-1))];
            }
        }

    public int getAlpha(double level)
        {
        if (colors != null)
            {
            if (level >= 0 && level < colors.length)
                return colors[(int)level].getAlpha();
            }

        // else...
            
        if (level > maxLevel) level = maxLevel;
        else if (level < minLevel) level = minLevel;
        if (level == minLevel) return minColor.getAlpha();
            
        final double interpolation = (level - minLevel) / (maxLevel - minLevel);

        final int maxAlpha = this.maxAlpha;
        final int minAlpha = this.minAlpha;
        return (maxAlpha == minAlpha ? minAlpha : (int)(interpolation * (maxAlpha - minAlpha) + minAlpha));
        }
                
    public int getRGB(double level)
        {
        if (colors != null)
            {
            if (level >= 0 && level < colors.length)
                return colors[(int)level].getRGB();
            }
            
        // else...
            
        if (level > maxLevel) level = maxLevel;
        else if (level < minLevel) level = minLevel;
        if (level == minLevel) return minColor.getRGB();
            
        final double interpolation = (level - minLevel) / (maxLevel - minLevel);

        final int maxAlpha = this.maxAlpha;
        final int minAlpha = this.minAlpha;
        final int alpha = (maxAlpha == minAlpha ? minAlpha : (int)(interpolation * (maxAlpha - minAlpha) + minAlpha));
        if (alpha==0) return 0;

        final int maxRed = this.maxRed;
        final int minRed = this.minRed;
        final int maxGreen = this.maxGreen;
        final int minGreen = this.minGreen;
        final int maxBlue = this.maxBlue;
        final int minBlue = this.minBlue;
        final int red = (maxRed == minRed ? minRed : (int)(interpolation * (maxRed - minRed) + minRed));
        final int green = (maxGreen == minGreen ? minGreen : (int)(interpolation * (maxGreen - minGreen) + minGreen));
        final int blue = (maxBlue == minBlue ? minBlue : (int)(interpolation * (maxBlue - minBlue) + minBlue));
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
        }

    public boolean validLevel(double value)
        {
        if (colors!=null && value >= 0 && value < colors.length)
            return true;
        if (value <= maxLevel && value >= minLevel)
            return true;
        return false;
        }
        
    public double defaultValue()
        {
        if (colors != null) return 0;
        return minLevel;
        }
    }
