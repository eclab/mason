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
import java.util.*;
import sim.display.*;

/**
   A portrayal for grids containing objects, such as maybe agents or agent bodies.
   
   <p>By default this portrayal describes objects as gray ovals (that's what getDefaultPortrayal() returns)
   and null values as empty regions (that's what getDefaultNullPortrayal() returns).  You may wish to override this
   for your own purposes.

   The 'location' passed
   into the DrawInfo2D handed to the SimplePortryal2D is a MutableInt2D.
*/

public class ObjectGridPortrayal2D extends FieldPortrayal2D
    {
    // a grey oval.  You should provide your own protrayals...
    SimplePortrayal2D defaultPortrayal = new OvalPortrayal2D();
    SimplePortrayal2D defaultNullPortrayal = new SimplePortrayal2D();
        
    public ObjectGridPortrayal2D()
        {
        super();
        }
        
    public void setField(Object field)
        {
        if (field instanceof ObjectGrid2D ) super.setField(field);
        else throw new RuntimeException("Invalid field for ObjectGridPortrayal2D: " + field);
        }
        
    public Portrayal getDefaultPortrayal()
        {
        return defaultPortrayal;
        }
                
    public Portrayal getDefaultNullPortrayal()
        {
        return defaultNullPortrayal;
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


    public Object getObjectLocation(Object object, GUIState gui)
        {
        synchronized(gui.state.schedule)
            {
            final ObjectGrid2D field = (ObjectGrid2D)this.field;
            if (field==null) return null;

            final int maxX = field.getWidth(); 
            final int maxY = field.getHeight();

            // find the object.
            for(int x=0; x < maxX; x++)
                {
                Object[] fieldx = field.field[x];
                for(int y = 0; y < maxY; y++)
                    if (object == fieldx[y])  // found it
                        return new Int2D(x,y);
                }
            return null;  // it wasn't there
            }
        }

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


    // our location to pass to the portrayal
    protected final MutableInt2D locationToPass = new MutableInt2D(0,0);
        
    protected void hitOrDraw(Graphics2D graphics, DrawInfo2D info, Bag putInHere)
        {
        final ObjectGrid2D field = (ObjectGrid2D)(this.field);

        if (field==null) return;
        
        boolean objectSelected = !selectedWrappers.isEmpty();
        Object selectedObject = (selectedWrapper == null ? null : selectedWrapper.getObject());

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

        DrawInfo2D newinfo = new DrawInfo2D(info.gui, info.fieldPortrayal, new Rectangle2D.Double(0,0, xScale, yScale), info.clip);  // we don't do further clipping 
        newinfo.precise = info.precise;
        newinfo.location = locationToPass;
        newinfo.fieldPortrayal = this;

        if (endx > maxX) endx = maxX;
        if (endy > maxY) endy = maxY;
        if( startx < 0 ) startx = 0;
        if( starty < 0 ) starty = 0;
        for(int x=startx;x<endx;x++)
            for(int y=starty;y<endy;y++)
                {
                Object obj = field.field[x][y];
                Portrayal p = getPortrayalForObject(obj);
                if (!(p instanceof SimplePortrayal2D))
                    throw new RuntimeException("Unexpected Portrayal " + p + " for object " + 
                        obj + " -- expected a SimplePortrayal2D");
                SimplePortrayal2D portrayal = (SimplePortrayal2D)p;
                
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
                    if (obj != null && portrayal.hitObject(obj, newinfo))
                        putInHere.add(getWrapper(obj, new Int2D(x,y)));
                    }
                else
                    {
                    newinfo.selected = (objectSelected &&  // there's something there
                        (selectedObject==obj || selectedWrappers.get(obj) != null));
                    portrayal.draw(obj, graphics, newinfo);
                    }
                }

        drawGrid(graphics, xScale, yScale, maxX, maxY, info);
        drawBorder(graphics, xScale, info);
        }

    // searches for an object within a short distance of a location
    final static int SEARCH_DISTANCE = 3;
    IntBag xPos = new IntBag(49);
    IntBag yPos = new IntBag(49);
        
    Int2D searchForObject(Object object, Int2D loc)
        {
        ObjectGrid2D field = (ObjectGrid2D)(this.field);
        Object[][] grid = field.field;
        if (grid[loc.x][loc.y] == object)
            return new Int2D(loc.x, loc.y);
        //field.getNeighborsMaxDistance(loc.x, loc.y, SEARCH_DISTANCE, true, xPos, yPos);
        field.getMooreLocations(loc.x, loc.y, SEARCH_DISTANCE, Grid2D.TOROIDAL, true, xPos, yPos);  // we include the origin but it doesn't matter
        for(int i=0;i<xPos.numObjs;i++)
            if (grid[xPos.get(i)][yPos.get(i)] == object) return new Int2D(xPos.get(i), yPos.get(i));
        return null;
        }
                
                
    public static class Message
        {
        String message;
        public Message(String message) { this.message = message; }
        public String getSorry()
            {
            return message;
            }
        }

    final Message unknown = new Message("It's too costly to figure out where the object went.");
    public LocationWrapper getWrapper(Object object, Int2D location)
        {
        final ObjectGrid2D field = (ObjectGrid2D)(this.field);
        return new LocationWrapper(object, location, this)
            {
            public Object getLocation()
                { 
                Int2D loc = (Int2D) super.getLocation();
                if (field.field[loc.x][loc.y] == getObject())  // it's still there!
                    {
                    return loc;
                    }
                else
                    {
                    Int2D result = searchForObject(object, loc);
                    if (result != null)  // found it nearby
                        {
                        location = result;
                        return result;
                        }
                    else    // it's moved on!
                        {
                        return unknown;
                        }
                    }
                }
            
            public String getLocationName()
                {
                Object loc = getLocation();
                if (loc instanceof Int2D)
                    return ((Int2D)this.location).toCoordinates();
                else return "Location Unknown";
                }
            };
        }

    LocationWrapper selectedWrapper = null;  // some efficiency: if there's only one non-null object selected, it will be here
    HashMap selectedWrappers = new HashMap();
    public boolean setSelected(LocationWrapper wrapper, boolean selected)
        {
        if (wrapper == null) return true;
        if (wrapper.getFieldPortrayal() != this) return true;

        Object obj = wrapper.getObject();
        boolean b = getPortrayalForObject(obj).setSelected(wrapper, selected);
        if (selected)
            {
            if (b==false) return false;
            selectedWrappers.put(obj, wrapper);
            selectedWrapper = wrapper;
            }
        else
            {
            selectedWrappers.remove(obj);
            selectedWrapper = null;
            }
        return true;
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
