/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.grid;
import sim.portrayal.*;
import sim.field.grid.*;
import java.awt.*;
import sim.util.gui.*;
import sim.util.*;

/** A "Fast" version of ObjectGridPortrayal2D, which draws objects as rectangles of specified colors,
    rather than using the provided SimplePortrayal2Ds.  FastObjectGridPortrayal2D contains an underlying
    FastValueGridPortrayal2D.  When the field needs to be drawn, it is first mapped into a DoubleGrid2D,
    using the mapping function <tt>doubleValue(...)</tt> (which you can override to provide specialized
    behavior).  This DoubleGrid2D is then handed to the underlying FastValueGridPortrayal2D to draw with
    the provided ColorMap.
*/


public class FastObjectGridPortrayal2D extends ObjectGridPortrayal2D
    {
    FastValueGridPortrayal2D valueGridPortrayal = new FastValueGridPortrayal2D("", immutableField);
    DoubleGrid2D grid;
    
    /** If immutableField is true, we presume that the grid doesn't change.  This allows us to just
        re-splat the buffer. */
    public FastObjectGridPortrayal2D(boolean immutableField)
        {
        setImmutableField(immutableField);
        }
    
    /** Equivalent to FastObjectGridPortrayal2D(false); */
    public FastObjectGridPortrayal2D() { }

    public void setImmutableField(boolean immutableField)
        {
        super.setImmutableField(immutableField);
        valueGridPortrayal.setImmutableField(immutableField);
        }

    public void setField(Object field)
        {
        super.setField(field);
        ObjectGrid2D og = (ObjectGrid2D)field;
        grid = new DoubleGrid2D(og.getWidth(), og.getHeight());
        valueGridPortrayal.setField(grid);
        }
    
    /** Override this as necessary to map the provided object into a double value.
        Objects selected from the field will be mapped through this function to double values, 
        which in turn will be passed to an underlying FastValueGridPortrayal2D, which will
        map them to appropriate colors using the map provided in setMap.
        
        <p>The default form of this function is:
        <ol>
        <li>If the object is null, return 0.
        <li>Else if the object is a Number or is sim.util.Valuable, return obj.doubleValue();
        <li>Else return 1.
        </ol>
    */
    public double doubleValue(Object obj)
        {
        if (obj==null) return 0.0;
        if (obj instanceof Number) return ((Number)(obj)).doubleValue();
        if (obj instanceof Valuable) return ((Valuable)(obj)).doubleValue();
        return 1.0;
        }
    
    /** Resets the underlying FastValueGridPortrayal2D. */
    public void reset()
        {
        valueGridPortrayal.reset();
        }
        
    public ColorMap getMap() { return valueGridPortrayal.getMap(); }
    public void setMap(ColorMap m) { valueGridPortrayal.setMap(m); }
    
    public int getBuffering() { return valueGridPortrayal.getBuffering(); }
    public void setBuffering(int val) { valueGridPortrayal.setBuffering(val); }
    
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        if (field==null) return;
        
        ObjectGrid2D ogrid = (ObjectGrid2D) field;

        // compute what area to do (as usual)
        
        final int maxX = ogrid.getWidth(); 
        final int maxY = ogrid.getHeight();
        if (maxX == 0 || maxY == 0) return; 
        
        final double xScale = info.draw.width / maxX;
        final double yScale = info.draw.height / maxY;
        int startx = (int)((info.clip.x - info.draw.x) / xScale);
        int starty = (int)((info.clip.y - info.draw.y) / yScale);
        int endx = /*startx +*/ (int)((info.clip.x - info.draw.x + info.clip.width) / xScale) + /*2*/ 1;  // with rounding, width be as much as 1 off
        int endy = /*starty +*/ (int)((info.clip.y - info.draw.y + info.clip.height) / yScale) + /*2*/ 1;  // with rounding, height be as much as 1 off

        if (endx > maxX) endx = maxX;
        if (endy > maxY) endy = maxY;
        if( startx < 0 ) startx = 0;
        if( starty < 0 ) starty = 0;
    
        // convert the object grid into a double grid
        for(int x=startx;x<endx;x++)
            {
            double[] gridx = grid.field[x];
            Object[] ogridx = ogrid.field[x];
            for(int y=starty;y<endy;y++)
                gridx[y] = doubleValue(ogridx[y]);
            }
   
        // now ask the ValueGridPortrayal to draw it!
        valueGridPortrayal.draw(object /* doesn't matter */ , graphics, info);
        }
    }
