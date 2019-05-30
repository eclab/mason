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
   A 2D portrayal for 3D grids containing objects.  Objects are portrayed in order of their z values: that is,
   objects with higher z values are drawn on top of objects with lower z values.
   
   <p>By default this portrayal describes objects as gray ovals (that's what getDefaultPortrayal() returns)
   and null values as empty regions (that's what getDefaultNullPortrayal() returns).  You may wish to override this
   for your own purposes.

   The 'location' passed
   into the DrawInfo2D handed to the SimplePortrayal2D is a MutableInt3D.
*/

public class ObjectGrid3DPortrayal2D extends ObjectGridPortrayal2D
    {
    public ObjectGrid3DPortrayal2D()
        {
        super();
        }
	
	boolean ignoresEmpty = true;
	
	/** Returns whether null (empty) cells are completely ignored for hit-testing and drawing. By default this is true.*/
	public boolean getIgnoresEmpty()
		{
		return ignoresEmpty;
		}
		
	/** Sets whether null (empty) cells are completely ignored for hit-testing and drawing. By default this is true.*/
	public void setIgnoresEmpty(boolean val)
		{
		ignoresEmpty = val;
		}
	
    public void setField(Object field)
        {
        if (field instanceof ObjectGrid3D ) setFieldBypass(field);
        else throw new RuntimeException("Invalid field for ObjectGrid3DPortrayal2D: " + field);
        }
        
    public Double2D getScale(DrawInfo2D info)
        {
        synchronized(info.gui.state.schedule)
            {
            final Grid3D field = (Grid3D) this.field;
            if (field==null) return null;

            int maxX = field.getWidth(); 
            int maxY = field.getHeight();

            final double xScale = info.draw.width / maxX;
            final double yScale = info.draw.height / maxY;
            return new Double2D(xScale, yScale);
            }
        }
                
    /** Returns the location corresponding with the given position -- and assuming that the
    	location has a z-value of 0. */
    public Object getPositionLocation(Point2D.Double position, DrawInfo2D info)
        {
        Double2D scale = getScale(info);
        double xScale = scale.x;
        double yScale = scale.y;
                
        final int startx = (int)((position.getX() - info.draw.x) / xScale);
        final int starty = (int)((position.getY() - info.draw.y) / yScale); // assume that the X coordinate is proportional -- and yes, it's _width_
        return new Int3D(startx, starty, 0);
        }


    public Object getObjectLocation(Object object, GUIState gui)
        {
        synchronized(gui.state.schedule)
            {
            final ObjectGrid3D field = (ObjectGrid3D)this.field;
            if (field==null) return null;

            final int maxX = field.getWidth(); 
            final int maxY = field.getHeight();

            // find the object.
            for(int x=0; x < maxX; x++)
                {
                Object[][] fieldx = field.field[x];
                for(int y = 0; y < maxY; y++)
                	{
                	Object[] fieldxy = fieldx[y];
                	for(int z = 0; z < fieldxy.length; z++)
                		if (object == fieldxy[z])  // found it
                        	return new Int3D(x,y,z);
                    }
                }
            return null;  // it wasn't there
            }
        }

    public Point2D.Double getLocationPosition(Object location, DrawInfo2D info)
        {
        synchronized(info.gui.state.schedule)
            {
            final Grid3D field = (Grid3D) this.field;
            if (field==null) return null;
        
            final int maxX = field.getWidth(); 
            final int maxY = field.getHeight();
            if (maxX == 0 || maxY == 0) return null;
        
            final double xScale = info.draw.width / maxX;
            final double yScale = info.draw.height / maxY;

            DrawInfo2D newinfo = new DrawInfo2D(info.gui, info.fieldPortrayal, new Rectangle2D.Double(0,0, xScale, yScale), info.clip, info);  // we don't do further clipping 
            newinfo.precise = info.precise;

            Int3D loc = (Int3D) location;
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
    protected final MutableInt3D locationToPass = new MutableInt3D(0,0,0);
        
    protected void hitOrDraw(Graphics2D graphics, DrawInfo2D info, Bag putInHere)
        {
        final ObjectGrid3D field = (ObjectGrid3D)(this.field);

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
        final int maxZ = field.getLength();
        if (maxX == 0 || maxY == 0 || maxZ == 0) return; 
        
        final double xScale = info.draw.width / maxX;
        final double yScale = info.draw.height / maxY;
        int startx = (int)((info.clip.x - info.draw.x) / xScale);
        int starty = (int)((info.clip.y - info.draw.y) / yScale); // assume that the X coordinate is proportional -- and yes, it's _width_
        int endx = /*startx +*/ (int)((info.clip.x - info.draw.x + info.clip.width) / xScale) + /*2*/ 1;  // with rounding, width be as much as 1 off
        int endy = /*starty +*/ (int)((info.clip.y - info.draw.y + info.clip.height) / yScale) + /*2*/ 1;  // with rounding, height be as much as 1 off

        DrawInfo2D newinfo = new DrawInfo2D(info.gui, info.fieldPortrayal, new Rectangle2D.Double(0,0, xScale, yScale), info.clip, info);  // we don't do further clipping 
        newinfo.precise = info.precise;
        newinfo.location = locationToPass;
        newinfo.fieldPortrayal = this;

		boolean ignoresEmpty = this.ignoresEmpty;  // locals are faster
		
        if (endx > maxX) endx = maxX;
        if (endy > maxY) endy = maxY;
        if( startx < 0 ) startx = 0;
        if( starty < 0 ) starty = 0;
        for(int x=startx;x<endx;x++)
            for(int y=starty;y<endy;y++)
            	for(int z = 0; z < maxZ; z++)
                {
                Object obj = field.field[x][y][z];
                if (obj == null && ignoresEmpty) continue;
                
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
                locationToPass.z = z;
                
                if (graphics == null)
                    {
                    if (obj != null && portrayal.hitObject(obj, newinfo))
                        putInHere.add(getWrapper(obj, new Int3D(x,y,z)));
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

	// the others are defined in ObjectGridPortrayal2D
    IntBag zPos = new IntBag(49);
        
    Int3D searchForObject(Object object, Int3D loc)
        {
        ObjectGrid3D field = (ObjectGrid3D)(this.field);
        Object[][][] grid = field.field;
        if (grid[loc.x][loc.y][loc.z] == object)
            return new Int3D(loc.x, loc.y, loc.z);
        field.getMooreLocations(loc.x, loc.y, loc.z, SEARCH_DISTANCE, Grid3D.TOROIDAL, true, xPos, yPos, yPos);  // we include the origin but it doesn't matter
        for(int i=0;i<xPos.numObjs;i++)
            if (grid[xPos.get(i)][yPos.get(i)][zPos.get(i)] == object) return new Int3D(xPos.get(i), yPos.get(i), zPos.get(i));
        return null;
        }
                
                
    public LocationWrapper getWrapper(Object object, Int3D location)
        {
        final ObjectGrid3D field = (ObjectGrid3D)(this.field);
        return new LocationWrapper(object, location, this)
            {
            public Object getLocation()
                { 
                Int3D loc = (Int3D) super.getLocation();
                if (field.field[loc.x][loc.y][loc.z] == getObject())  // it's still there!
                    {
                    return loc;
                    }
                else
                    {
                    Int3D result = searchForObject(object, loc);
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
                if (loc instanceof Int3D)
                    return ((Int3D)this.location).toCoordinates();
                else return "Location Unknown";
                }
            };
        }

    }
