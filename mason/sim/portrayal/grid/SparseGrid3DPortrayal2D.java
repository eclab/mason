/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.grid;

import sim.portrayal.*;
import sim.portrayal.simple.*;
import sim.field.grid.*;
import sim.util.*;
import java.awt.*;
import java.util.*;
import java.awt.geom.*;
import sim.portrayal.inspector.*;
import sim.display.*;

/**
   Can be used to draw both continuous and discrete sparse fields.

   The 'location' passed
   into the DrawInfo2D handed to the SimplePortryal2D is an Int3D.
*/

public class SparseGrid3DPortrayal2D extends SparseGridPortrayal2D
    {
    public void setField(Object field)
        {
        if (field instanceof SparseGrid3D ) setFieldBypass(field);
        else throw new RuntimeException("Invalid field for SparseGrid3DPortrayal2D: " + field);
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
                
        final int startx = (int)Math.floor((position.getX() - info.draw.x) / xScale);
        final int starty = (int)Math.floor((position.getY() - info.draw.y) / yScale); // assume that the X coordinate is proportional -- and yes, it's _width_
        return new Int3D(startx, starty, 0);
        }

    public void setObjectLocation(Object object, Object location, GUIState gui)
        {
        synchronized(gui.state.schedule)
            {
		if (location != null)
			{
			if (location instanceof Int3D)
				{
				Int3D loc = (Int3D) location;
				if (object instanceof Fixed2D && (!((Fixed2D)object).maySetLocation(field, loc)))
					return;  // this is deprecated and will be deleted
				else if (object instanceof Constrained)
					  loc = (Int3D)((Constrained)object).constrainLocation(field, loc);
				if (loc != null)
					((SparseGrid3D)field).setObjectLocation(object, loc);
				}
			}
			}
        }

    /*
    public void setObjectPosition(Object object, Point2D.Double position, DrawInfo2D fieldPortrayalInfo)
        {
        synchronized(fieldPortrayalInfo.gui.state.schedule)
            {
            final SparseGrid3D field = (SparseGrid3D)this.field;
            if (field==null) return;
            Int3D oldLocation = (Int3D)(field.getObjectLocation(object));
            if (oldLocation == null) return;
            Int3D location = (Int3D)(getPositionLocation(position, fieldPortrayalInfo));
            if (location != null)
                {
                // since getPositionLocation assumes a z-value of 1, we set the location to the proper z
                location = new Int3D(location.x, location.y, oldLocation.z);
                if (object instanceof Fixed2D && (!((Fixed2D)object).maySetLocation(field, location)))
                    return;
                else if (object instanceof Constrained)
                      location = (Int3D)((Constrained)object).constrainLocation(field, location);
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
            final SparseGrid3D field = (SparseGrid3D)this.field;
            if (field==null) return null;
            return field.getObjectLocation(object);
            }
        }

    public Point2D.Double getLocationPosition(Object location, DrawInfo2D info)
        {
        synchronized(info.gui.state.schedule)
            {
            final Grid3D field = (Grid3D) this.field;
            if (field==null) return null;

            int maxX = field.getWidth(); 
            int maxY = field.getHeight();

            final double xScale = info.draw.width / maxX;
            final double yScale = info.draw.height / maxY;

            DrawInfo2D newinfo = new DrawInfo2D(info.gui, info.fieldPortrayal, new Rectangle2D.Double(0,0, xScale, yScale), info.clip, info);
            newinfo.precise = info.precise;

            Int3D loc = (Int3D)location;
            if (loc == null) return null;

            // translate --- the   + newinfo.width/2.0  etc. moves us to the center of the object
            newinfo.draw.x = (int)Math.floor(info.draw.x + (xScale) * loc.x);
            newinfo.draw.y = (int)Math.floor(info.draw.y + (yScale) * loc.y);
            newinfo.draw.width = (int)Math.floor(info.draw.x + (xScale) * (loc.x+1)) - newinfo.draw.x;
            newinfo.draw.height = (int)Math.floor(info.draw.y + (yScale) * (loc.y+1)) - newinfo.draw.y;
        
            // adjust drawX and drawY to center
            newinfo.draw.x += newinfo.draw.width / 2.0;
            newinfo.draw.y += newinfo.draw.height / 2.0;

            return new Point2D.Double(newinfo.draw.x, newinfo.draw.y);
            }
        }
    
    
    
    //// FIXME: The computational complexity of this could be improved.  At present
    //// we are sorting everything by Z and then throwing out the stuff that doesn't
    //// fall within the drawing region.  Instead, we should gather all the elements 
    //// that fall within the region and THEN sort them by Z.
    //// See also Continuous3DPortrayal2D
    	
    protected void hitOrDraw(Graphics2D graphics, DrawInfo2D info, Bag putInHere)
        {
        final SparseGrid3D field = (SparseGrid3D) this.field;
        if (field==null) return;

        boolean objectSelected = !selectedWrappers.isEmpty();

        int maxX = field.getWidth(); 
        int maxY = field.getHeight();

        final double xScale = info.draw.width / maxX;
        final double yScale = info.draw.height / maxY;
        final int startx = (int)Math.floor((info.clip.x - info.draw.x) / xScale);
        final int starty = (int)Math.floor((info.clip.y - info.draw.y) / yScale); // assume that the X coordinate is proportional -- and yes, it's _width_
        int endx = /*startx +*/ (int)Math.floor((info.clip.x - info.draw.x + info.clip.width) / xScale) + /*2*/ 1;  // with rounding, width be as much as 1 off
        int endy = /*starty +*/ (int)Math.floor((info.clip.y - info.draw.y + info.clip.height) / yScale) + /*2*/ 1;  // with rounding, height be as much as 1 off


        DrawInfo2D newinfo = new DrawInfo2D(info.gui, info.fieldPortrayal, new Rectangle2D.Double(0,0, xScale, yScale), info.clip, info);  // we don't do further clipping 
        newinfo.precise = info.precise;
        newinfo.fieldPortrayal = this;

        // If the person has specified a policy, we have to iterate through the
        // bags.  At present we have to do this by using a hash table iterator
        // (yuck -- possibly expensive, have to search through empty locations).
        //
        // We never use the policy to determine hitting.  hence this only works if graphics != null
        if (policy != null && graphics != null)
            {
            Iterator iterator = field.locationBagIterator();
            Bag bagbag = new Bag();
            while(iterator.hasNext())
                {
                Bag objects = (Bag)(iterator.next());
                
                if (objects == null || objects.size() == 0) continue;
                bagbag.add(objects);
                }
            
            // at this point we have a bag of BAGS, where each sub-bag is the objects at
            // a given location.  So we use a comparator which sorts the sub-bags based
            // on (say) the first element in each of them.
            
            // sort here
            bagbag.sort(new Comparator()
            	{
            	public int compare(Object o1, Object o2)
            		{
            		Bag b1 = (Bag)o1;
            		Bag b2 = (Bag)o2;
            		
            		Int3D i1 = (Int3D)(field.getObjectLocation(b1.get(0)));
            		Int3D i2 = (Int3D)(field.getObjectLocation(b2.get(0)));
            		// sort so that smaller objects appear first
            		if (i1.z < i2.z) return -1;
            		if (i2.z < i1.z) return 1;
            		return 0;
            		}
				});
				
			// now we apply the policy
            
	        Bag policyBag = new Bag();
            for(int i = 0; i < bagbag.size(); i++)
            	{
	            Bag objects = (Bag)(bagbag.get(i));
                                
                // restrict the number of objects to draw
                policyBag.clear();  // fast
                if (policy.objectToDraw(objects,policyBag))  // if this function returns FALSE, we should use objects as is, else use the policy bag.
                    objects = policyBag;  // returned TRUE, so we're going to use the modified policyBag instead.
                                        
                // draw 'em
                for(int x=0;x<objects.numObjs;x++)
                    {
                    final Object portrayedObject = objects.objs[x];
                    Int3D loc = field.getObjectLocation(portrayedObject);
                    // here we only draw the object if it's within our range.  However objects
                    // might leak over to other places, so I dunno...  I give them the benefit
                    // of the doubt that they might be three times the size they oughta be, hence the -2 and +2's
                    if (loc.x >= startx -2 && loc.x < endx + 4 &&
                        loc.y >= starty -2 && loc.y < endy + 4)
                        {
                        Portrayal p = getPortrayalForObject(portrayedObject);
                        if (!(p instanceof SimplePortrayal2D))
                            throw new RuntimeException("Unexpected Portrayal " + p + " for object " + 
                                portrayedObject + " -- expected a SimplePortrayal2D");
                        SimplePortrayal2D portrayal = (SimplePortrayal2D) p;
                        
                        // translate --- the   + newinfo.width/2.0  etc. moves us to the center of the object
                        newinfo.draw.x = (int)Math.floor(info.draw.x + (xScale) * loc.x);
                        newinfo.draw.y = (int)Math.floor(info.draw.y + (yScale) * loc.y);
                        newinfo.draw.width = (int)Math.floor(info.draw.x + (xScale) * (loc.x+1)) - newinfo.draw.x;
                        newinfo.draw.height = (int)Math.floor(info.draw.y + (yScale) * (loc.y+1)) - newinfo.draw.y;
                        
                        // adjust drawX and drawY to center
                        newinfo.draw.x += newinfo.draw.width / 2.0;
                        newinfo.draw.y += newinfo.draw.height / 2.0;

                        newinfo.location = loc;

                        newinfo.selected = (objectSelected &&  // there's something there
                            selectedWrappers.get(portrayedObject) != null);
                        portrayal.draw(portrayedObject, graphics, newinfo);
                        }
                    }
                }
            }
        else            // the easy way -- draw the objects one by one
            {
	        Bag objects = new Bag(field.getAllObjects());  // copy the bag
    	    objects.sort(new Comparator()
            	{
            	public int compare(Object o1, Object o2)
            		{
            		Int3D i1 = (Int3D)(field.getObjectLocation(o1));
            		Int3D i2 = (Int3D)(field.getObjectLocation(o2));
            		// sort so that smaller objects appear first
            		if (i1.z < i2.z) return -1;
            		if (i2.z < i1.z) return 1;
            		return 0;
            		}
				});
				        		
            for(int x=0;x<objects.numObjs;x++)
                {
                final Object portrayedObject = objects.objs[x];
                Int3D loc = field.getObjectLocation(portrayedObject);

                // here we only draw the object if it's within our range.  However objects
                // might leak over to other places, so I dunno...  I give them the benefit
                // of the doubt that they might be three times the size they oughta be, hence the -2 and +2's
                if (loc.x >= startx -2 && loc.x < endx + 4 &&
                    loc.y >= starty -2 && loc.y < endy + 4)
                    {
                    Portrayal p = getPortrayalForObject(portrayedObject);
                    if (!(p instanceof SimplePortrayal2D))
                        throw new RuntimeException("Unexpected Portrayal " + p + " for object " + 
                            portrayedObject + " -- expected a SimplePortrayal2D");
                    SimplePortrayal2D portrayal = (SimplePortrayal2D) p;
                
                    // translate --- the   + newinfo.width/2.0  etc. moves us to the center of the object
                    newinfo.draw.x = (int)Math.floor(info.draw.x + (xScale) * loc.x);
                    newinfo.draw.y = (int)Math.floor(info.draw.y + (yScale) * loc.y);
                    newinfo.draw.width = (int)Math.floor(info.draw.x + (xScale) * (loc.x+1)) - newinfo.draw.x;
                    newinfo.draw.height = (int)Math.floor(info.draw.y + (yScale) * (loc.y+1)) - newinfo.draw.y;
                    
                    // adjust drawX and drawY to center
                    newinfo.draw.x += newinfo.draw.width / 2.0;
                    newinfo.draw.y += newinfo.draw.height / 2.0;

                    if (graphics == null)
                        {
                        if (portrayal.hitObject(portrayedObject, newinfo))
                            {
                            putInHere.add(getWrapper(portrayedObject, newinfo.gui));
                            }
                        }
                    else
                        {
                        newinfo.selected = (objectSelected &&  // there's something there
                            selectedWrappers.get(portrayedObject) != null); 
                        portrayal.draw(portrayedObject, graphics, newinfo);
                        }
                    }
                }
            }

        drawGrid(graphics, xScale, yScale, maxX, maxY, info);
        drawBorder(graphics, xScale, info);
        }

    // The easiest way to make an inspector which gives the location of my objects
    public LocationWrapper getWrapper(Object object, GUIState gui)
        {
        final SparseGrid3D field = (SparseGrid3D) this.field;
        final StableInt3D w = new StableInt3D(this, object, gui);
        return new LocationWrapper( object, null, this )  // don't care about location
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
