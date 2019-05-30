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
 *
 * <p>You can customize the interpolation by overriding the transformLevel(...) method.  This method receives
 * a level value and the minimum and maximum values before it's clamped to a minimum or maximum color;
 * you can modify this level value and return a new value (ideally between the minimum and maximum).
 * For example, you could move the values to a log of their previous values.  
 *
 * <p>The default of transformLevel(...) calls another optional function you can override: the filterLevel(...)
 * method.  This method is passed a level value which has been pre-transformed such that 0.0 is the "minimum"
 * and 1.0 is the "maximum".  You can override this method to return a new value between 0.0 and 1.0
 * which will be "de-transformed" and used instead.  The default simply returns the value itself.
 *
 * <p>You should only override transformLevel(...), of filterLevel(...), or none, but not both.
 * </ol>
 *
 * <p>The user can provide both a color table <i>and</i> an interpolation; in this case, the color table takes
 * precedence over the interpolation in that region where the color table is relevant.  You specify a color
 * table with setColorTable(), and you specify an interpolation range with setLevels().  It's important to
 * note that transformLevel(...) and filterLevel(...) are <i>not</i> applied to the color table, only to the
 * interpolation.  So if you provide 2.7 as a level, and have some fancy-shmancy transformation for that
 * in transformLevel(...), but you've made a color table 5 elements long, color number 2 will be used
 * directly without every checking your transformLevel(...) method.
 *
 * <p>validLevel() is set to return true if the level range is between min-level and max-level, or
 * if it's inside the color table range.  Neither transformLevel(...) nor filterLevel(...) are called.
 *
 * <p>defaultValue() is set to return 0 if the color table exists, else min-level is provided.
 *
 * <p>NaN is assumed to be the same color as negative infinity.
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
    final Color clearColor = new Color(0,0,0,0);
    static final int COLOR_DISCRETIZATION = 257;

    int minRed = 0;
    int minBlue = 0;
    int minGreen = 0;
    int minAlpha = 0;
    int maxRed = 0;
    int maxBlue = 0;
    int maxGreen = 0;
    int maxAlpha = 0;
    double maxLevel = 0;
    double minLevel = 0;
    Color minColor = clearColor;
    Color maxColor = clearColor;
    
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
        For all other values, clear is returned. 
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

    /** Specifies that if a value (cast into an int) in the IntGrid2D or DoubleGrid2D falls in the range 0 ... colors.length,
        then that index in the colors table should be used to represent that value.  Otherwise, values in
        setLevels(...) are used.  You can remove the color table by passing in null here.  Returns the old color table. */
    public Color[] setColorTable(Color[] colorTable)
        {
        Color[] retval = colors;
        colors = colorTable;
        return retval;
        }
                
    /** Override this to convert level values to new values using some appropriate mathematical transformation.
        The provided level value will be scaled to between 0 and 1 inclusive, which represent the minimum and 
        maximum possible colors. The value you return must also be between 0 and 1 inclusive.  The default
        version of this function just returns the level. 
        
        <p><b>Do not override both this function and transformLevel(...).  transformLevel simply calls this
        function.</b>
    */
    public double filterLevel(double level) { return level; }
    
    /** Override this to convert level values to new values using some appropriate mathematical transformation.
        The values you return ought to be >= minLevel and <= maxLevel; values outside these bounds will be
        trimmed to minLevel or maxLevel respectively prior to conversion to a color.  The default implementation
        simply returns the passed-in level.  The default version scales level to a range between 0 and 1 in such a
        way that minLevel is 0 and maxLevel is 1; it then calls filterLevel, then un-scales the result and returns it.
        
        <p><b>Do not override both this function and filterLevel(...).  filterLevel is just used by this function.</b>
    */
    public double transformLevel(double level, double minLevel, double maxLevel)
        {
        if (level <= minLevel) return minLevel;
        if (level >= maxLevel) return maxLevel;
        double interval = maxLevel - minLevel;
        return filterLevel((level - minLevel) / interval) * interval + minLevel;
        }
    
    /** Sets the color levels for the ValueGridPortrayal2D values for use by the default getColor(...)
        method.  These are overridden by any array provided in setColorTable().  If the value in the IntGrid2D or DoubleGrid2D
        is less than or equal to minLevel, then minColor is used.  If the value is greater than or equal to maxColor, then
        maxColor is used.  Otherwise a linear interpolation from minColor to maxColor is used. */
    public void setLevels(double minLevel, double maxLevel, Color minColor, Color maxColor)
        {
        if (maxLevel != maxLevel || minLevel != minLevel) throw new RuntimeException("maxLevel or minLevel cannot be NaN");
        if (Double.isInfinite(maxLevel) || Double.isInfinite(minLevel)) throw new RuntimeException("maxLevel or minLevel cannot be infinite");
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
        
    public Color getColor(double level)
        {
        if (colors != null && level >= 0 && level < colors.length)
            {
            return colors[(int)level];
            }
        else
            {
            // preprocess the level
            level = transformLevel(level, minLevel, maxLevel);
        
            if (level != level) level = Double.NEGATIVE_INFINITY;  // NaN handling
            
            if (level == minLevel) return minColor;  // so we don't divide by zero (maxLevel - minLevel)
            else if (level == maxLevel) return maxColor;  // so we don't overflow
            
            final double interpolation = (level - minLevel) / (maxLevel - minLevel);
            
            // the +1's below are because the only way you can get the maxColor is if you have EXACTLY the maxLevel --
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
        if (colors != null && level >= 0 && level < colors.length)
            {
            return colors[(int)level].getAlpha();
            }
        else
            {
            // preprocess the level
            level = transformLevel(level, minLevel, maxLevel);
                
            if (level != level) level = Double.NEGATIVE_INFINITY;  // NaN handling

            // else...
                        
            // these next two also handle the possibility that maxLevel = minLevel
            if (level >= maxLevel) return maxColor.getAlpha();
            if (level <= minLevel) return minColor.getAlpha();

            final double interpolation = (level - minLevel) / (maxLevel - minLevel);

            final int maxAlpha = this.maxAlpha;
            final int minAlpha = this.minAlpha;
            return (maxAlpha == minAlpha ? minAlpha : (int)(interpolation * (maxAlpha - minAlpha) + minAlpha));
            }
        }
                
         
    public int getRGB(double level)
        {
        if (colors != null && level >= 0 && level < colors.length)
            {
            return colors[(int)level].getRGB();
            }
        else
            {
            // preprocess the level
            level = transformLevel(level, minLevel, maxLevel);
        
            if (level != level) level = Double.NEGATIVE_INFINITY;  // NaN handling

            // these next two also handle the possibility that maxLevel = minLevel
            if (level >= maxLevel) return maxColor.getRGB();
            if (level <= minLevel) return minColor.getRGB();

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
        }
    /*

      public int getRGB(double level)
      {
      return getColor(level).getRGB();
      }
    */

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
