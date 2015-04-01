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
   
   The 'location' passed
   into the DrawInfo2D handed to the SimplePortryal2D is a MutableInt2D.

*/

public class ValueGridPortrayal2D extends FieldPortrayal2D
    {
    ColorMap map = new SimpleColorMap();
    public ColorMap getMap() { return map; }
    public void setMap(ColorMap m) { map = m; }
    
    public void setField(Object field)
        {
        if (field instanceof DoubleGrid2D ||
            field instanceof IntGrid2D ) super.setField(field);
        else throw new RuntimeException("Invalid field for ValueGridPortrayal2D: " + field);
        }
        
    SimplePortrayal2D defaultPortrayal = new ValuePortrayal2D();
    String valueName;
    
    public String getValueName() { return valueName; }
    public void setValueName(String name) { valueName = name; }
        
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
        if (field != null)
            {
            if (field instanceof DoubleGrid2D)
                return ((DoubleGrid2D)field).field[x][y];
            else return ((IntGrid2D)field).field[x][y];
            }
        else return map.defaultValue();
        }

    public Double2D getScale(DrawInfo2D info)
        {
        synchronized(info.gui.state.schedule)
            {
            final Grid2D field = (Grid2D) this.field;
            if (field==null) return null;

            int maxX = field.getWidth(); 
            int maxY = field.getHeight();

            final double xScale = info.draw.width / maxX;
            final double yScale = info.draw.height / maxY;
            return new Double2D(xScale, yScale);
            }
        }
                
    public Object getPositionLocation(Point2D.Double position, DrawInfo2D info)
        {
        Double2D scale = getScale(info);
        double xScale = scale.x;
        double yScale = scale.y;
                
        final int startx = (int)((position.getX() - info.draw.x) / xScale);
        final int starty = (int)((position.getY() - info.draw.y) / yScale); // assume that the X coordinate is proportional -- and yes, it's _width_
        return new Int2D(startx, starty);
        }

// there is no getObjectLocation.

    public Point2D.Double getLocationPosition(Object location, DrawInfo2D info)
        {
        synchronized(info.gui.state.schedule)
            {
            final Grid2D field = (Grid2D) this.field;
            if (field==null) return null;
        
            final int maxX = field.getWidth(); 
            final int maxY = field.getHeight();
            if (maxX == 0 || maxY == 0) return null;
        
            final double xScale = info.draw.width / maxX;
            final double yScale = info.draw.height / maxY;

            DrawInfo2D newinfo = new DrawInfo2D(info.gui, info.fieldPortrayal, new Rectangle2D.Double(0,0, xScale, yScale), info.clip);  // we don't do further clipping 
            newinfo.precise = info.precise;
            
            Int2D loc = (Int2D) location;
            if (location == null) return null;
                
            int x = loc.x;
            int y = loc.y;

            // translate --- the   + newinfo.width/2.0  etc. moves us to the center of the object
            newinfo.draw.x = (int)(info.draw.x + (xScale) * x);
            newinfo.draw.y = (int)(info.draw.y + (yScale) * y);
            newinfo.draw.width = (int)(info.draw.x + (xScale) * (x+1)) - newinfo.draw.x;
            newinfo.draw.height = (int)(info.draw.y + (yScale) * (y+1)) - newinfo.draw.y;
        
            // adjust drawX and drawY to center
            newinfo.draw.x += newinfo.draw.width / 2.0;
            newinfo.draw.y += newinfo.draw.height / 2.0;

            return new Point2D.Double(newinfo.draw.x, newinfo.draw.y);
            }
        }


    public Portrayal getDefaultPortrayal()
        {
        return defaultPortrayal;
        }

    // our object to pass to the portrayal
    protected final MutableDouble valueToPass = new MutableDouble(0);

    // our location to pass to the portrayal
    protected final MutableInt2D locationToPass = new MutableInt2D(0,0);
        
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
        DrawInfo2D newinfo = new DrawInfo2D(info.gui, info.fieldPortrayal, new Rectangle2D.Double(0,0, xScale, yScale), info.clip);
        newinfo.precise = info.precise;
        newinfo.location = locationToPass;
        newinfo.fieldPortrayal = this;  // crucial for ValuePortrayal2D to get the parent out

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

                locationToPass.x = x;
                locationToPass.y = y;
                
                if (graphics == null)
                    {
                    if (portrayal.hitObject(valueToPass, newinfo))
                        putInHere.add(getWrapper(valueToPass.val, new Int2D(x, y)));
                    }
                else
                    {
                    // MacOS X 10.3 Panther has a bug which resets the clip, YUCK
                    //                    graphics.setClip(clip);
                    portrayal.draw(valueToPass, graphics, newinfo);
                    }
                }
                
        drawGrid(graphics, xScale, yScale, maxX, maxY, info);
        drawBorder(graphics, xScale, info);
        }

    // ValueGridPortrayal2D's objects are instances of MutableDouble
    public LocationWrapper getWrapper(double val, Int2D loc)
        {
        final Grid2D field = (Grid2D)this.field;
        return new LocationWrapper( new MutableDouble(val),  // something unique to return for getObject()
            loc, this )  // its location
            {
            public Object getObject()
                {
                Int2D loc = (Int2D) location;
                MutableDouble val = (MutableDouble) this.object;
                // update the current value
                if (field instanceof DoubleGrid2D) 
                    val.val = ((DoubleGrid2D)field).field[loc.x][loc.y];
                else 
                    val.val = ((IntGrid2D)field).field[loc.x][loc.y];
                return val;
                }
            
            public String getLocationName()
                {
                return ((Int2D)location).toCoordinates();
                }
            };
        }

    // Indicates the fraction of a cell width or height that will be filled by the stroked line.
    //    The line is actually centered on the border between the two cells: so the fraction is the
    //    total amount filled by the portions of the stroked lines on both sides of the cells.
    double gridLineFraction = 1/8.0;
    Color gridColor = Color.blue;
    int gridModulus = 10;
    double gridMinSpacing = 2.0;
    double gridLineMinWidth = 1.0;
    double gridLineMaxWidth = Double.POSITIVE_INFINITY;
    boolean gridLines = false;
    
    /** Turns grid lines on or off.  By default the grid is off.  */
    public void setGridLines(boolean on) { gridLines = on; }

    /** Sets the grid color.   By default the grid is blue.  */
    public void setGridColor(Color val)
        {
        if (val == null) throw new RuntimeException("color must be non-null");
        gridColor = val;
        }
    
    /** Sets the grid modulus. This is the minimum number of grid cells skipped before another grid line is drawn. 
        By default the modulus is 10.  */
    public void setGridModulus(int val)
        {
        if (val <= 0) throw new RuntimeException("modulus must be > 0");
        gridModulus = val;
        }

    /** Sets the grid min spacing.  This is the minimum number of pixels skipped before another grid line is drawn.
        The grid modulus is doubled until the grid spacing equals or exceeds the minimum spacing.  
        By default the min spacing is 2.0.  */
    public void setGridMinSpacing(double val)
        {
        if (val < 0 || val > 1) throw new RuntimeException("grid min spacing must be > 0");
        gridMinSpacing = val;
        }

    /** Sets the grid line fraction.  This is the width of a stroked line as a fraction of the width (or height) 
        of a grid cell.  Grid lines are drawn centered on the borders between cells.  
        By default the fraction is 1/8.0.  */
    public void setGridLineFraction(double val)
        {
        if (val <= 0) throw new RuntimeException("gridLineFraction must be between 0 and 1");
        gridLineFraction = val;
        }
        
    /** Sets the minimum and maximum width of a grid line in pixels. 
        By default, the minimum is 1.0 and the maximum is positive infinity. */
    public void setGridLineMinMaxWidth(double min, double max)
        {
        if (min <= 0) throw new RuntimeException("minimum width must be between >= 0");
        if (min > max) throw new RuntimeException("maximum width must be >= minimum width");
        gridLineMinWidth = min;
        gridLineMaxWidth = max;
        }
        
    // Indicates the fraction of a cell width or height that will be filled by the stroked line.
    //    The line is actually centered on the border between the two cells: so the fraction is the
    //    total amount filled by the portions of the stroked lines on both sides of the cells.
    double borderLineFraction = 1/8.0;
    Color borderColor = Color.red;
    double borderLineMinWidth = 1.0;
    double borderLineMaxWidth = Double.POSITIVE_INFINITY;
    boolean border = false;
    
    /** Turns border lines on or off.    By default the border is off.  */
    public void setBorder(boolean on) { border = on; }

    /** Sets the border color.  By default the border is red.  */
    public void setBorderColor(Color val)
        {
        if (val == null) throw new RuntimeException("color must be non-null");
        borderColor = val;
        }
    
    /** Sets the border line fraction. This is the width of a stroked line as a fraction of the width (or height) 
        of a grid cell.  Grid lines are drawn centered on the borders around the grid.  Note that if the grid
        is being drawn clipped (see Display2D.setClipping(...)), then only HALF of the width of this line will
        be visible (the half that lies within the grid region).  
        By default the fraction is 1/8.0..  */
    public void setBorderLineFraction(double val)
        {
        if (val <= 0) throw new RuntimeException("borderLineFraction must be between 0 and 1");
        borderLineFraction = val;
        }

    /** Sets the minimum and maximum width of a border line in pixels. 
        By default, the minimum is 1.0 and the maximum is positive infinity. */
    public void setBorderLineMinMaxWidth(double min, double max)
        {
        if (min <= 0) throw new RuntimeException("minimum width must be between >= 0");
        if (min > max) throw new RuntimeException("maximum width must be >= minimum width");
        borderLineMinWidth = min;
        borderLineMaxWidth = max;
        }
        
    void drawBorder(Graphics2D graphics, double xScale, DrawInfo2D info)
        {
        /** Draw a border if any */
        if (border && graphics != null)
            {
            Stroke oldStroke = graphics.getStroke();
            Paint oldPaint = graphics.getPaint();
            java.awt.geom.Rectangle2D.Double d = new java.awt.geom.Rectangle2D.Double();
            graphics.setColor(borderColor);
            graphics.setStroke(new BasicStroke((float)Math.min(borderLineMaxWidth, Math.max(borderLineMinWidth, (xScale * borderLineFraction)))));
            d.setRect(info.draw.x, info.draw.y, info.draw.x + info.draw.width, info.draw.y + info.draw.height);
            graphics.draw(d);
            graphics.setStroke(oldStroke);
            graphics.setPaint(oldPaint);
            }
        }
    
    void drawGrid(Graphics2D graphics, double xScale, double yScale, int maxX, int maxY, DrawInfo2D info)
        {
        /** Draw the grid if any */
        if (gridLines && graphics != null)
            {
            // determine the skip
            int skipX = gridModulus;
            while(skipX * xScale < gridMinSpacing) skipX *= 2;
            int skipY = gridModulus;
            while(skipY * yScale < gridMinSpacing) skipY *= 2;
            
            Stroke oldStroke = graphics.getStroke();
            Paint oldPaint = graphics.getPaint();
            java.awt.geom.Line2D.Double d = new java.awt.geom.Line2D.Double();
            graphics.setColor(gridColor);
            graphics.setStroke(new BasicStroke((float)Math.min(gridLineMaxWidth, Math.max(gridLineMinWidth, (xScale * gridLineFraction)))));
            for(int i = gridModulus; i < maxX; i+= skipX)
                {
                d.setLine(info.draw.x + xScale * i , info.draw.y, info.draw.x + xScale * i , info.draw.y + info.draw.height);
                graphics.draw(d);
                }

            graphics.setStroke(new BasicStroke((float)Math.min(gridLineMaxWidth, Math.max(gridLineMinWidth, (yScale * gridLineFraction)))));
            for(int i = gridModulus; i < maxY; i+= skipY)
                {
                d.setLine(info.draw.x, info.draw.y + yScale * i , info.draw.x + info.draw.width, info.draw.y + yScale * i );
                graphics.draw(d);
                }
            graphics.setStroke(oldStroke);
            graphics.setPaint(oldPaint);
            }
        }

    }
