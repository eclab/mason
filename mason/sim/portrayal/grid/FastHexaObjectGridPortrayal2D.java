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

/** A "Fast" version of HexaObjectGridPortrayal2D, which draws objects as rectangles of specified colors,
    rather than using the provided SimplePortrayal2Ds.  FastHexaObjectGridPortrayal2D contains an underlying
    FastHexaValueGridPortrayal2D.  When the field needs to be drawn, it is first mapped into a DoubleGrid2D,
    using the mapping function <tt>doubleValue(...)</tt> (which you can override to provide specialized
    behavior).  This DoubleGrid2D is then handed to the underlying FastHexaValueGridPortrayal2D to draw with
    the provided ColorMap.
*/


public class FastHexaObjectGridPortrayal2D extends HexaObjectGridPortrayal2D
    {
    FastHexaValueGridPortrayal2D valueGridPortrayal = new FastHexaValueGridPortrayal2D("", immutableField);
    DoubleGrid2D grid;
    
    /** If immutableField is true, we presume that the grid doesn't change.  This allows us to just
        re-splat the buffer. */
    public FastHexaObjectGridPortrayal2D(boolean immutableField)
        {
        setImmutableField(immutableField);
        }
    
    /** Equivalent to FastHexaObjectGridPortrayal2D(false); */
    public FastHexaObjectGridPortrayal2D() { }

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
        which in turn will be passed to an underlying FastHexaValueGridPortrayal2D, which will
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
    
    /** Resets the underlying FastHexaValueGridPortrayal2D. */
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
        
        final double divideByX = ((maxX%2==0)?(3.0*maxX/2.0+0.5):(3.0*maxX/2.0+2.0));
        final double divideByY = (1.0+2.0*maxY);

        final double xScale = info.draw.width / divideByX;
        final double yScale = info.draw.height / divideByY;
        int startx = (int)(((info.clip.x - info.draw.x)/xScale-0.5)/1.5)-2;
        int starty = (int)((info.clip.y - info.draw.y)/(yScale*2.0))-2;
        int endx = /*startx +*/ (int)(((info.clip.x - info.draw.x + info.clip.width)/xScale-0.5)/1.5) + 4;  // with rounding, width be as much as 1 off
        int endy = /*starty +*/ (int)((info.clip.y - info.draw.y + info.clip.height)/(yScale*2.0)) + 4;  // with rounding, height be as much as 1 off

//        double precomputedWidth = -1;  // see discussion further below
//        double precomputedHeight = -1;  // see discussion further below
        
        //
        //
        // CAUTION!
        //
        // At some point we should triple check the math for rounding such
        // that the margins are drawn properly
        //
        //

        // Horizontal hexagons are staggered.  This complicates computations.  Thus
        // if  you have a M x N grid scaled to SCALE, then
        // your height is (N + 0.5) * SCALE
        // and your width is ((M - 1) * (3/4) + 1) * HEXAGONAL_RATIO * SCALE
        // we invert these calculations here to compute the rough width and height
        // for the newinfo here.  Additionally, because the original screen sizes were likely
        // converted from floats to ints, there's a round down there, so we round up to
        // compensate.  This usually results in nice circles.


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
