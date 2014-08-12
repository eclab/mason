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
   into the DrawInfo2D handed to the SimplePortryal2D is an Int2D.
*/

public class SparseGridPortrayal2D extends FieldPortrayal2D
    {
    public DrawPolicy policy;

    public SparseGridPortrayal2D()
        {
        super();
        }

    /** @deprecated Use setDrawPolicy. */
    public SparseGridPortrayal2D (DrawPolicy policy)
        {
        super();
        this.policy = policy;
        }
                
    public void setDrawPolicy(DrawPolicy policy)
        {
        this.policy = policy;
        }

    public DrawPolicy getDrawPolicy()
        {
        return policy;
        }

    // a grey oval.  You should provide your own protrayals...
    SimplePortrayal2D defaultPortrayal = new OvalPortrayal2D();

    public Portrayal getDefaultPortrayal()
        {
        return defaultPortrayal;
        }

    public void setField(Object field)
        {
        if (field instanceof SparseGrid2D ) super.setField(field);
        else throw new RuntimeException("Invalid field for Sparse2DPortrayal: " + field);
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
                
        final int startx = (int)Math.floor((position.getX() - info.draw.x) / xScale);
        final int starty = (int)Math.floor((position.getY() - info.draw.y) / yScale); // assume that the X coordinate is proportional -- and yes, it's _width_
        return new Int2D(startx, starty);
        }

    public void setObjectPosition(Object object, Point2D.Double position, DrawInfo2D fieldPortrayalInfo)
        {
        synchronized(fieldPortrayalInfo.gui.state.schedule)
            {
            final SparseGrid2D field = (SparseGrid2D)this.field;
            if (field==null) return;
            if (field.getObjectLocation(object) == null) return;
            Int2D location = (Int2D)(getPositionLocation(position, fieldPortrayalInfo));
            if (location != null)
                {
                if (object instanceof Fixed2D && (!((Fixed2D)object).maySetLocation(field, location)))
                    return;  // this is deprecated and will be deleted
                //if (object instanceof Constrained)
                //      location = (Int2D)((Constrained)object).constrainLocation(field, location);
                if (location != null)
                    field.setObjectLocation(object, location);
                }
            }
        }

    public Object getObjectLocation(Object object, GUIState gui)
        {
        synchronized(gui.state.schedule)
            {
            final SparseGrid2D field = (SparseGrid2D)this.field;
            if (field==null) return null;
            return field.getObjectLocation(object);
            }
        }

    public Point2D.Double getLocationPosition(Object location, DrawInfo2D info)
        {
        synchronized(info.gui.state.schedule)
            {
            final Grid2D field = (Grid2D) this.field;
            if (field==null) return null;

            int maxX = field.getWidth(); 
            int maxY = field.getHeight();

            final double xScale = info.draw.width / maxX;
            final double yScale = info.draw.height / maxY;

            DrawInfo2D newinfo = new DrawInfo2D(info.gui, info.fieldPortrayal, new Rectangle2D.Double(0,0, xScale, yScale), info.clip);
            newinfo.precise = info.precise;

            Int2D loc = (Int2D)location;
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
        
        
    protected void hitOrDraw(Graphics2D graphics, DrawInfo2D info, Bag putInHere)
        {
        final SparseGrid2D field = (SparseGrid2D) this.field;
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


        //final Rectangle clip = (graphics==null ? null : graphics.getClipBounds());

        DrawInfo2D newinfo = new DrawInfo2D(info.gui, info.fieldPortrayal, new Rectangle2D.Double(0,0, xScale, yScale), info.clip);  // we don't do further clipping 
        newinfo.precise = info.precise;
        newinfo.fieldPortrayal = this;

        // If the person has specified a policy, we have to iterate through the
        // bags.  At present we have to do this by using a hash table iterator
        // (yuck -- possibly expensive, have to search through empty locations).
        //
        // We never use the policy to determine hitting.  hence this only works if graphics != null
        if (policy != null && graphics != null)
            {
            Bag policyBag = new Bag();
            Iterator iterator = field.locationBagIterator();
            while(iterator.hasNext())
                {
                Bag objects = (Bag)(iterator.next());
                
                if (objects == null) continue;
                                
                // restrict the number of objects to draw
                policyBag.clear();  // fast
                if (policy.objectToDraw(objects,policyBag))  // if this function returns FALSE, we should use objects as is, else use the policy bag.
                    objects = policyBag;  // returned TRUE, so we're going to use the modified policyBag instead.
                                        
                // draw 'em
                for(int x=0;x<objects.numObjs;x++)
                    {
                    final Object portrayedObject = objects.objs[x];
                    Int2D loc = field.getObjectLocation(portrayedObject);
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
                        /*
                          {
                          LocationWrapper wrapper = (LocationWrapper)(selectedWrappers.get(portrayedObject));
                          portrayal.setSelected(wrapper,true);
                          portrayal.draw(portrayedObject, graphics, newinfo);
                          portrayal.setSelected(wrapper,false);
                          }
                          else */ 
                        portrayal.draw(portrayedObject, graphics, newinfo);
                        }
                    }
                }
            }
        else            // the easy way -- draw the objects one by one
            {
            Bag objects = field.getAllObjects();
            for(int x=0;x<objects.numObjs;x++)
                {
                final Object portrayedObject = objects.objs[x];
                Int2D loc = field.getObjectLocation(portrayedObject);

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
                            putInHere.add(getWrapper(portrayedObject));
                            }
                        }
                    else
                        {
                        // MacOS X 10.3 Panther has a bug which resets the clip, YUCK
                        //                    graphics.setClip(clip);
                        newinfo.selected = (objectSelected &&  // there's something there
                            selectedWrappers.get(portrayedObject) != null); 
                        /* {
                           LocationWrapper wrapper = (LocationWrapper)(selectedWrappers.get(portrayedObject));
                           portrayal.setSelected(wrapper,true);
                           portrayal.draw(portrayedObject, graphics, newinfo);
                           portrayal.setSelected(wrapper,false);
                           }
                           else */ portrayal.draw(portrayedObject, graphics, newinfo);
                        }
                    }
                }
            }
        }

    // The easiest way to make an inspector which gives the location of my objects
    public LocationWrapper getWrapper(Object object)
        {
        final SparseGrid2D field = (SparseGrid2D) this.field;
        final StableInt2D w = new StableInt2D(field, object);
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
            }
        else
            {
            selectedWrappers.remove(obj);
            }
        return true;
        }
    }
