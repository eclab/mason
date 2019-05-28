/*
  Copyright 2015 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.gui;
import java.awt.*;

/**
 * CompositeColorMap is a ColorMap which consists of an array of ColorMaps.
 * In response to a color request, it queries each of the maps in turn until
 * one of them indicates that the value is a valid level, at which point it
 * gets the color from that map.  If none of them think the value is valid, then
 * the default value (and color) is used from the final map.
 *
 * <p>This is a simple way of defining multiple gradients
 * from different colors in the same space (such as red->green->blue->white->black)
 */

public class CompositeColorMap implements ColorMap
    {
    ColorMap[] maps;
        
    /** Builds a CompositeColorMap with two subsidiary maps */
    public CompositeColorMap(ColorMap map1, ColorMap map2) { this(new ColorMap[] { map1, map2 }); }

    /** Builds a CompositeColorMap with three subsidiary maps */
    public CompositeColorMap(ColorMap map1, ColorMap map2, ColorMap map3) { this(new ColorMap[] { map1, map2, map3}); }

    /** Builds a CompositeColorMap with four subsidiary maps */
    public CompositeColorMap(ColorMap map1, ColorMap map2, ColorMap map3, ColorMap map4) { this(new ColorMap[] { map1, map2, map3, map4}); }

    /** Builds a CompositeColorMap with an arbitrary number (> 0) of subsidiary maps */
    public CompositeColorMap(ColorMap[] maps)
        {
        if (maps.length == 0)
            throw new RuntimeException("CompositeColorMap requires at least one ColorMap");
        this.maps = maps; 
        }
     
    public Color getColor(double level)
        {
        for(int i = 0; i < maps.length -1; i++)
            {
            if (maps[i].validLevel(level))
                return maps[i].getColor(level);
            }
        return maps[maps.length - 1].getColor(level);  // regardless
        }
    
    public int getRGB(double level)
        {
        for(int i = 0; i < maps.length -1; i++)
            {
            if (maps[i].validLevel(level))
                return maps[i].getRGB(level);
            }
        return maps[maps.length - 1].getRGB(level);  // regardless
        }
    
    public int getAlpha(double level)
        {
        for(int i = 0; i < maps.length -1; i++)
            {
            if (maps[i].validLevel(level))
                return maps[i].getAlpha(level);
            }
        return maps[maps.length - 1].getAlpha(level);  // regardless
        }

    public boolean validLevel(double level)
        {
        for(int i = 0; i < maps.length -1; i++)
            {
            if (maps[i].validLevel(level))
                return true;
            }
        return maps[maps.length - 1].validLevel(level);  // regardless
        }

    public double defaultValue()
        {
        return maps[maps.length - 1].defaultValue();  // regardless
        }
    }
