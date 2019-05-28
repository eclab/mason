/*
  Copyright 2015 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.gui;
import java.awt.*;

/**
 * AbstractColorMap is a ColorMap with all methods implemented except for getColor(...).
 * The validLevel(...) method is implemented to always return true, and thus the defaultValue()
 * method is implemented to return an arbitrary default level (set to 0).
 * 
 * <P>This class is intended to make it easy to implement a ColorMap which responds to any 
 * numerical value to provide a Color: you only need to implement the obvious method, getColor(...).
 * However it's not particularly efficient, since the other methods call getColor(...), then extract
 * the RGBA values out of the 
 * 
 */
 
public abstract class AbstractColorMap implements ColorMap
    {
    public abstract Color getColor(double level);
    
    public int getRGB(double level) { return getColor(level).getRGB(); }
    
    public int getAlpha(double level) { return getColor(level).getAlpha(); }

    public boolean validLevel(double level) { return true; }

    public double defaultValue() { return 0; }
    }
