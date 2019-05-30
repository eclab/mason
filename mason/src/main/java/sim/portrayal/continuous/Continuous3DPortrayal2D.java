/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.continuous;
import sim.portrayal.*;
import sim.portrayal.simple.*;
import sim.field.continuous.*;
import sim.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import sim.portrayal.inspector.*;
import sim.display.*;

/**
   Portrays Continuous3D fields projected into a 2D space.  When asked to portray objects, this field computes the buckets
   covered by the requested region, then includes an additional boundary of two buckets in each
   direction just in case objects leak over the boundary region.  Objects are portrayed in order of their z values: that is,
   objects with higher z values are drawn on top of objects with lower z values.
   
   The 'location' passed
   into the DrawInfo2D handed to the SimplePortryal2D is a Double3D.
*/

public class Continuous3DPortrayal2D extends ContinuousPortrayal2D
    {
    public void setField(Object field)
        {
        if (field instanceof Continuous3D) setFieldBypass(field);
        else throw new RuntimeException("Invalid field for ContinuousPortrayal3D: " + field);
        }
        
    public Point2D.Double getRelativeObjectPosition(Object location, Object otherObjectLocation, DrawInfo2D otherObjectInfo)
        {
        final Continuous3D field = (Continuous3D)this.field;
        if (field==null) return null;

        Double3D loc = (Double3D) location;
        Double3D oloc = (Double3D) otherObjectLocation;
        double dx = loc.x - oloc.x;
        double dy = loc.y - oloc.y;
        double xScale = otherObjectInfo.draw.width;
        double yScale = otherObjectInfo.draw.height;
        return new Point2D.Double(dx * xScale + otherObjectInfo.draw.x, dy * yScale + otherObjectInfo.draw.y);
        }
        
    public Double2D getScale(DrawInfo2D info)
        {
        synchronized(info.gui.state.schedule)
            {
            final Continuous3D field = (Continuous3D)this.field;
            if (field==null) return null;
                
            final double xScale = info.draw.width / field.width;
            final double yScale = info.draw.height / field.height;
            
            return new Double2D(xScale, yScale);
            }
        }
    
    /** Returns the location corresponding with the given position -- and assuming that the
    	location has a z-value of 0. */
    public Object getPositionLocation(Point2D.Double position, DrawInfo2D fieldPortrayalInfo)
        {
        Double2D scale = getScale(fieldPortrayalInfo);
        double xScale = scale.x;
        double yScale = scale.y;
                
        final double x = (position.getX() - fieldPortrayalInfo.draw.x) / xScale;  // notice not (int) like elsewhere.
        final double y = (position.getY() - fieldPortrayalInfo.draw.y) / yScale;
        return new Double3D(x,y, 0);
        }

    public void setObjectLocation(Object object, Object location, GUIState gui)
        {
        synchronized(gui.state.schedule)
            {
		if (location != null)
			{
			if (location instanceof Double2D)
				{
				Double3D loc = (Double3D) location;
				if (object instanceof Fixed2D && (!((Fixed2D)object).maySetLocation(field, loc)))
					return;  // this is deprecated and will be deleted
				else if (object instanceof Constrained)
					  loc = (Double3D)((Constrained)object).constrainLocation(field, loc);
				if (loc != null)
					((Continuous3D)field).setObjectLocation(object, loc);
				}
			}
			}
        }

/*
    public void setObjectPosition(Object object, Point2D.Double position, DrawInfo2D fieldPortrayalInfo)
        {
        synchronized(fieldPortrayalInfo.gui.state.schedule)
            {
            final Continuous3D field = (Continuous3D)this.field;
            if (field==null) return;
            Double3D oldLocation = (Double3D)(field.getObjectLocation(object));
            if (oldLocation == null) return;
            Double3D location = (Double3D)(getPositionLocation(position, fieldPortrayalInfo));
            if (location != null)
                {
                // since getPositionLocation assumes a z-value of 1, we set the location to the proper z
                location = new Double3D(location.x, location.y, oldLocation.z);
                if (object instanceof Fixed2D && (!((Fixed2D)object).maySetLocation(field, location)))
                    return;  
            	else if (object instanceof Constrained)
                      location = (Double3D)((Constrained)object).constrainLocation(field, location);
				if (location != null)
                    field.setObjectLocation(object, location);
                }
            }
        }
*/
    public Object getObjectLocation(Object object, GUIState gui)
        {
        synchronized(gui.state.schedule)
            {
            final Continuous3D field = (Continuous3D)this.field;
            if (field==null) return null;
            return field.getObjectLocation(object);
            }
        }

    public Point2D.Double getLocationPosition(Object location, DrawInfo2D fieldPortrayalInfo)
        {
        synchronized(fieldPortrayalInfo.gui.state.schedule)
            {
            final Continuous3D field = (Continuous3D)this.field;
            if (field==null) return null;
                
            final double xScale = fieldPortrayalInfo.draw.width / field.width;
            final double yScale = fieldPortrayalInfo.draw.height / field.height;
            DrawInfo2D newinfo = new DrawInfo2D(fieldPortrayalInfo.gui, fieldPortrayalInfo.fieldPortrayal, new Rectangle2D.Double(0,0, xScale, yScale), fieldPortrayalInfo.clip, fieldPortrayalInfo);  // we don't do further clipping 
            newinfo.precise = fieldPortrayalInfo.precise;

            Double3D loc = (Double3D) location;
            if (loc == null) return null;

            newinfo.draw.x = (fieldPortrayalInfo.draw.x + (xScale) * loc.x);
            newinfo.draw.y = (fieldPortrayalInfo.draw.y + (yScale) * loc.y);

            return new Point2D.Double(newinfo.draw.x, newinfo.draw.y);
            }
        }

    //// FIXME: The computational complexity of this could be improved.  At present
    //// we are sorting everything by Z and then throwing out the stuff that doesn't
    //// fall within the drawing region.  Instead, we should gather all the elements 
    //// that fall within the region and THEN sort them by Z.
    //// See also SparseGrid3DPortrayal2D
    	
    protected void hitOrDraw(Graphics2D graphics, DrawInfo2D info, Bag putInHere)
        {
        final Continuous3D field = (Continuous3D)this.field;
        if (field==null) return;
                
        boolean objectSelected = !selectedWrappers.isEmpty();

        final double xScale = info.draw.width / field.width;
        final double yScale = info.draw.height / field.height;
        final int startx = (int)Math.floor((info.clip.x - info.draw.x) / xScale);
        final int starty = (int)Math.floor((info.clip.y - info.draw.y) / yScale);
        int endx = /*startx +*/ (int)Math.floor((info.clip.x - info.draw.x + info.clip.width) / xScale) + /*2*/ 1;  // with rounding, width be as much as 1 off
        int endy = /*starty +*/ (int)Math.floor((info.clip.y - info.draw.y + info.clip.height) / yScale) + /*2*/ 1;  // with rounding, height be as much as 1 off

        DrawInfo2D newinfo = new DrawInfo2D(info.gui, info.fieldPortrayal, new Rectangle2D.Double(0,0, xScale, yScale), info.clip, info);  // we don't do further clipping 
        newinfo.precise = info.precise;
        newinfo.fieldPortrayal = this;

        // hit/draw the objects one by one -- perhaps for large numbers of objects it would
        // be smarter to grab the objects out of the buckets that specifically are inside
        // our range...

		Bag objects = new Bag(field.getAllObjects());  // copy the bag
		objects.sort(new Comparator()
			{
			public int compare(Object o1, Object o2)
				{
				Double3D i1 = (Double3D)(field.getObjectLocation(o1));
				Double3D i2 = (Double3D)(field.getObjectLocation(o2));
				// sort so that smaller objects appear first
				if (i1.z < i2.z) return -1;
				if (i2.z < i1.z) return 1;
				return 0;
				}
			});
							
        final double discretizationOverlap = field.discretization;
        for(int x=0;x<objects.numObjs;x++)
            {
            Object object = (objects.objs[x]);
            Double3D objectLoc = field.getObjectLocation(object);
                        
            if (displayingToroidally)
                objectLoc = new Double3D(field.tx(objectLoc.x), field.tx(objectLoc.y), objectLoc.z);
                                                
            for(int i = 0; i < toroidalX.length; i++) 
                {
                Double3D loc = null;
                if (i == 0)
                    loc = objectLoc;
                else if (displayingToroidally)  // and i > 0
                    loc = new Double3D(objectLoc.x + field.width * toroidalX[i],
                        objectLoc.y + field.height * toroidalY[i], objectLoc.z);
                else
                    break; // no toroidal function
                                
                // here we only hit/draw the object if it's within our range.  However objects
                // might leak over to other places, so I dunno...  I give them the benefit
                // of the doubt that they might be three times the size they oughta be, hence the -2 and +2's
                                
                if (loc.x >= startx - discretizationOverlap && loc.x < endx + discretizationOverlap &&
                    loc.y >= starty - discretizationOverlap && loc.y < endy + discretizationOverlap)
                    {
                    Portrayal p = getPortrayalForObject(object);
                    if (!(p instanceof SimplePortrayal2D))
                        throw new RuntimeException("Unexpected Portrayal " + p + " for object " + 
                            objects.objs[x] + " -- expected a SimplePortrayal2D");
                    SimplePortrayal2D portrayal = (SimplePortrayal2D) p;
                                        
                    newinfo.draw.x = (info.draw.x + (xScale) * loc.x);
                    newinfo.draw.y = (info.draw.y + (yScale) * loc.y);

                    newinfo.location = loc;

                    final Object portrayedObject = object;
                    if (graphics == null)
                        {
                        if (portrayal.hitObject(portrayedObject, newinfo))
                            putInHere.add(getWrapper(portrayedObject, newinfo.gui));
                        }
                    else
                        {
                        // MacOS X 10.3 Panther has a bug which resets the clip, YUCK
                        //                    graphics.setClip(clip);
                        newinfo.selected = (objectSelected &&  // there's something there
                            selectedWrappers.get(portrayedObject) != null); 
                        portrayal.draw(portrayedObject, graphics, newinfo);
                        }
                    }
                }
            }
            
        drawAxes(graphics, xScale, yScale, info);
        drawBorder(graphics, xScale, info);
        }


    public LocationWrapper getWrapper(final Object obj, GUIState gui)
        {
        final Continuous3D field = (Continuous3D)this.field;
        final StableDouble3D w = new StableDouble3D(this, obj, gui);
        return new LocationWrapper( obj, null , this)  // don't care about location
            {
            public Object getLocation()
                {
                return w;
                }
                
            public String getLocationName()
                {
                return w.toString();
                }
            };
        }
    }
    
    
