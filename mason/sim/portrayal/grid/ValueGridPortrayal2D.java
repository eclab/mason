/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.grid;
import sim.portrayal.*;
import sim.portrayal.simple.*;
import sim.field.grid.*;
import java.awt.*;
import java.awt.geom.*;
import sim.util.*;
import sim.util.gui.*;

/**
   This class is capable of portraying the DoubleGrid2D and IntGrid2D fields (and <b>only</b> those two fields -- or subclasses).
   It is fairly customizable, and this flexibility
   comes at a cost in drawing speed.  If you just need to draw your field as a grid of squares, you might look into
   the much simpler, and faster (and aptly named) FastValueGridPortrayal2D instead.

   <p>Like other FieldPortrayal2Ds, this class uses an underlying SimplePortrayal2D to draw each separate element
   in the grid.  A default SimplePortrayal2D is provided which draws squares.  In the default, the color for the square is
   determined by looking up the value of the square in a user-provided color-table, or if there is none, by
   interpolating it between two user-provided colors.  See the setColorTable() and setLevels() methods. 

   <p>Here's a trick you might consider in specifying interpolations.  Rather than draw from white to red (for example),
   you might consider setting the backdrop of the display to white, and then instead draw from FULLY TRANSPARENT white to
   FULLY OPAQUE red.  That is, from Color(0,0,0,0) to Color(255,0,0,255).  Fully transparent colors are not drawn; and not drawing
   at all is significantly faster than drawing for no reason!  Plus you can stack multiple ValueGridPortrayal2Ds on top
   of one another and let the transparency bleed through for a nice effect.  The alpha channel is your friend.

   <p>By default the min Level and the max Level are the same (1.0), and the alpha values for both are 0 (totally transparent).
   Thus if you want a range, you must specify it.  This is intentional, because this way if you want to use a color
   table instead (say, to specify three colors for the integers 0, 1, and 2), you can specify them, and ALL other grid values
   will be automatically transparent.

   <p>If you would like more control over the color of your values (perhaps to implement a nonlinear function of the colors),
   you can override the getColor() function to define your own custom color.

   <p>You can also provide your own custom SimplePortrayal2D (use setPortrayalForAll(...) ) to draw elements as you
   see fit rather than as rectangles.  Your SimplePortrayal2D should expect objects passed to its draw method
   to be of type MutableDouble.  Do not hold onto this array -- it will be reused.
*/

public class ValueGridPortrayal2D extends FieldPortrayal2D
    {
    public ColorMap map = new SimpleColorMap();
    public ColorMap getMap() { return map; }
    public void setMap(ColorMap m) { map = m; }
    
    public void setField(Object field)
        {
        dirtyField = true;
        if (field instanceof DoubleGrid2D ||
            field instanceof IntGrid2D ) this.field = field;
        else throw new RuntimeException("Invalid field for ValueGridPortrayal2D: " + field);
        }
        
    SimplePortrayal2D defaultPortrayal = new ValuePortrayal2D(this);

    public String valueName;
    
    public String getValueName() { return valueName; }
    
    public ValueGridPortrayal2D()
        {
        this("Value");
        }

    public ValueGridPortrayal2D(String valueName)
        {
        this.valueName = valueName;
        }
    
    /** This method is called by the default inspector to filter new values set by the user.
        You should return the "corrected" value if the given value is invalid. The default version
        of this method bases values on the values passed into the setLevels() and setColorTable() methods. */
    public double newValue(int x, int y, double value)
        {
        final Grid2D field = (Grid2D)this.field;
        if (field instanceof IntGrid2D) value = (int) value;
                
        if (map.validLevel(value)) return value;
        
        // at this point we need to reset to current value
        java.awt.Toolkit.getDefaultToolkit().beep();
        if (field != null)
            {
            if (field instanceof DoubleGrid2D)
                return ((DoubleGrid2D)field).field[x][y];
            else return ((IntGrid2D)field).field[x][y];
            }
        else return map.defaultValue();
        }

    public Portrayal getDefaultPortrayal()
        {
        return defaultPortrayal;
        }

    // our object to pass to the portrayal
    final MutableDouble valueToPass = new MutableDouble(0);
        
    protected void hitOrDraw(Graphics2D graphics, DrawInfo2D info, Bag putInHere)
        {
        final Grid2D field = (Grid2D)this.field;
        if (field==null) return;

        // Scale graphics to desired shape -- according to p. 90 of Java2D book,
        // this will change the line widths etc. as well.  Maybe that's not what we
        // want.
        
        // first question: determine the range in which we need to draw.
        // We assume that we will fill exactly the info.draw rectangle.
        // We can do the item below because we're an expensive operation ourselves
        
        final int maxX = field.getWidth(); 
        final int maxY = field.getHeight();
        if (maxX == 0 || maxY == 0) return; 
        
        final double xScale = info.draw.width / maxX;
        final double yScale = info.draw.height / maxY;
        int startx = (int)((info.clip.x - info.draw.x) / xScale);
        int starty = (int)((info.clip.y - info.draw.y) / yScale); // assume that the X coordinate is proportional -- and yes, it's _width_
        int endx = /*startx +*/ (int)((info.clip.x - info.draw.x + info.clip.width) / xScale) + /*2*/ 1;  // with rounding, width be as much as 1 off
        int endy = /*starty +*/ (int)((info.clip.y - info.draw.y + info.clip.height) / yScale) + /*2*/ 1;  // with rounding, height be as much as 1 off
        
        // next we determine if this is a DoubleGrid2D or an IntGrid2D
        
        final boolean isDoubleGrid2D = (field instanceof DoubleGrid2D);
        final double[][] doubleField = (isDoubleGrid2D ? ((DoubleGrid2D) field).field : null);
        final int[][] intField = (isDoubleGrid2D ? null : ((IntGrid2D) field).field);
        
//        final Rectangle clip = (graphics==null ? null : graphics.getClipBounds());

        // the drawinfo that the object's portrayal will use -- we fill in the blanks later
        DrawInfo2D newinfo = new DrawInfo2D(new Rectangle2D.Double(0,0, xScale, yScale), info.clip);

        Portrayal p = getPortrayalForObject(valueToPass);
        if (!(p instanceof SimplePortrayal2D))
            throw new RuntimeException("Unexpected Portrayal " + p + " for object " + 
                valueToPass + " -- expected a SimplePortrayal2D");
        SimplePortrayal2D portrayal = (SimplePortrayal2D) p;

        if (endx > maxX) endx = maxX;
        if (endy > maxY) endy = maxY;
        if( startx < 0 ) startx = 0;
        if( starty < 0 ) starty = 0;
        for(int x=startx;x<endx;x++)
            for(int y=starty;y<endy;y++)
                {
                // dunno how much of a hit we get for doing this if/then each and every time...
                valueToPass.val = (isDoubleGrid2D ?  doubleField[x][y] : intField[x][y]);
                
                // translate --- the   + newinfo.width/2.0  etc. moves us to the center of the object
                newinfo.draw.x = (int)(info.draw.x + (xScale) * x);
                newinfo.draw.y = (int)(info.draw.y + (yScale) * y);
                newinfo.draw.width = (int)(info.draw.x + (xScale) * (x+1)) - newinfo.draw.x;
                newinfo.draw.height = (int)(info.draw.y + (yScale) * (y+1)) - newinfo.draw.y;
                
                // adjust drawX and drawY to center
                newinfo.draw.x += newinfo.draw.width / 2.0;
                newinfo.draw.y += newinfo.draw.height / 2.0;
                
                if (graphics == null)
                    {
                    if (portrayal.hitObject(valueToPass, newinfo))
                        putInHere.add(getWrapper(valueToPass.val, x, y));
                    }
                else
                    {
                    // MacOS X 10.3 Panther has a bug which resets the clip, YUCK
                    //                    graphics.setClip(clip);
                    portrayal.draw(valueToPass, graphics, newinfo);
                    }
                }
        }

    // ValueGridPortrayal2D's objects are instances of MutableDouble
    public LocationWrapper getWrapper(double val, int x, int y)
        {
        final Grid2D field = (Grid2D)this.field;
        return new LocationWrapper( new MutableDouble(val),  // something unique to return for getObject()
            new Int2D(x, y), this )  // it's location
            {
            public Object getObject()
                {
                Int2D loc = (Int2D) location;
                MutableDouble val = (MutableDouble) this.object;
                // update the current value
                if (field instanceof DoubleGrid2D) val.val = ((DoubleGrid2D)field).field[loc.x][loc.y];
                else val.val = ((IntGrid2D)field).field[loc.x][loc.y];
                return val;
                }
            
            public String getLocationName()
                {
                return ((Int2D)location).toCoordinates();
                }
            };
        }
    }
