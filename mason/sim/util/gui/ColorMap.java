/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.gui;
import java.awt.*;
/**
 * ColorMap is a interface for mapping numerical values to colors.
 * The easiest way to implement getRGB(level) is simply with getColor(level).getRGB().
 * validLevel indicates whether the numerical value is within a range that seems "reasonable"
 * for coding into colors -- however ColorMap should provide *some* feasible color for
 * *any* given value, including NaN.  defaultValue() provides a default numerical value
 * within the "reasonable" range -- often the minimum value.  It must be the case that
 * validLevel(defaultValue()) == true.
 *
 * @author Gabriel Catalin Balan
 * 
 */
public interface ColorMap 
    {
    /** Returns a color for the given level */
    public Color getColor(double level);
    /** Returns the RGB values, plus alpha, for a color for the given level.  
        The byte ordering should be in the same fashion that Color.getRGB() is provided. This could
        be simply written as
        
        <p><code return getColor(level).getRGB() </code>
        
        ... however it's likely that this method could be written more efficiently than this.
    */
    public int getRGB(double level);
    /** Returns the alpha value for a color for the given level.  This could be simply written as 
                
        <p><code>return getRGB(level) >>> 24 ; </code>
                
        <p>...or it could be written as:
                
        <p><code>return getColor(level).getAlpha() </code>
                
        <p>...however it's likely that it this method could be written more efficiently than either of these.
    */
    public int getAlpha(double level);
    /** Returns true if a level is "valid" (it provides a meaningful color) */
    public boolean validLevel(double level);
    /** Returns <i>some</i> level which is valid (that is, validLevel(defaultValue()) should
        always return true).  This is commonly provided to give the user a level to replace
        an "invalid" level he's typed in. */
    public double defaultValue();
    }
