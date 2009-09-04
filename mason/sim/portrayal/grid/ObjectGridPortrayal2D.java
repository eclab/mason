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

/**
   A portrayal for grids containing objects, such as maybe agents or agent bodies.
   
   <p>By default this portrayal describes objects as gray ovals (that's what getDefaultPortrayal() returns)
   and null values as empty regions (that's what getDefaultNullPortrayal() returns).  You may wish to override this
   for your own purposes.
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
        dirtyField = true;
        if (field instanceof ObjectGrid2D ) this.field = field;
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


    public Point2D.Double getPositionInFieldPortrayal(Object object, DrawInfo2D info)
        {
        final ObjectGrid2D field = (ObjectGrid2D)(this.field);

        if (field==null) return null;
        
        final int maxX = field.getWidth(); 
        final int maxY = field.getHeight();
        if (maxX == 0 || maxY == 0) return null;
        
        final double xScale = info.draw.width / maxX;
        final double yScale = info.draw.height / maxY;

        DrawInfo2D newinfo = new DrawInfo2D(new Rectangle2D.Double(0,0, xScale, yScale),
            info.clip);  // we don't do further clipping 

        // find the object.
        for(int x=0; x < maxX; x++)
            {
            Object[] fieldx = field.field[x];
            for(int y = 0; y < maxY; y++)
                if (object == fieldx[y])  // found it
                    {
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
        return null;  // it wasn't there
        }



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

        DrawInfo2D newinfo = new DrawInfo2D(new Rectangle2D.Double(0,0, xScale, yScale),
            info.clip);  // we don't do further clipping 

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
                
                if (graphics == null)
                    {
                    if (obj != null && portrayal.hitObject(obj, newinfo))
                        putInHere.add(getWrapper(obj, new Int2D(x,y)));
                    }
                else
                    {
                    if (objectSelected &&  // there's something there
                        (selectedObject==obj || selectedWrappers.get(obj) != null))
                        {
                        LocationWrapper wrapper = null;
                        if (selectedObject == obj) 
                            wrapper = selectedWrapper;
                        else wrapper = (LocationWrapper)(selectedWrappers.get(obj));
                        portrayal.setSelected(wrapper,true);
                        portrayal.draw(obj, graphics, newinfo);
                        portrayal.setSelected(wrapper,false);
                        }
                    else portrayal.draw(obj, graphics, newinfo);
                    }
                }
        }

/*
  public LocationWrapper getWrapper(Int2D location)
  {
  final ObjectGrid2D field = (ObjectGrid2D)(this.field);
  return new LocationWrapper(null, location, this)
  {
  Int2D loc = (Int2D) this.location;
  public Object getObject()
  { 
  return field.field[loc.x][loc.y];
  }
            
  public String getLocationName()
  {
  return ((Int2D)this.location).toCoordinates();
  }
  };
  }
*/

    // searches for an object within a short distance of a location
    final int SEARCH_DISTANCE = 3;
    IntBag xPos = new IntBag(49);
    IntBag yPos = new IntBag(49);
        
    Int2D searchForObject(Object object, Int2D loc)
        {
        ObjectGrid2D field = (ObjectGrid2D)(this.field);
        Object[][] grid = field.field;
        if (grid[loc.x][loc.y] == object)
            return new Int2D(loc.x, loc.y);
        field.getNeighborsMaxDistance(loc.x, loc.y, SEARCH_DISTANCE, true, xPos, yPos);
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
        if (selected)
            {
            // first let's determine if the object WANTs to be selected
            boolean b = getPortrayalForObject(obj).setSelected(wrapper,selected);
                        
            // now we turn the selection back to regular
            getPortrayalForObject(obj).setSelected(wrapper,!selected);
                        
            // Okay, now we can tell whether or not to add to the wrapper collection
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
    }
