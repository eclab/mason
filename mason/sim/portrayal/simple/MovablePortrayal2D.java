/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.simple;
import sim.portrayal.*;
import java.awt.*;
import java.awt.geom.*;
import sim.display.*;
import java.awt.event.*;
import sim.util.*;

/**
   A wrapper for other Portrayal2Ds which makes it possible to drag and move objects with the mouse.
   
   <p>This portrayal is used simply by wrapping around another portrayal or around null (if the object portrays itself).

   <p>If you declare a MovablePortrayal2D for an object, you will be able to move it in any field
   portrayal which supports the basic functions for moving objects.  These at present are the
   various Sparse portrayals and the Continuous2D portrayal.
        
   <p>Moving an object also selects it and deselects other objects.
   
   <p>It's possible that an object wants to control how it's moved.  For example, you may have
   objects which maintain their own internal location and don't like to be moved without being
   informed.  You can still use them with MovablePortrayal2D if they implement the Fixed2D interface,
   implementing its sole method to move themselves in their field, and then returning false.  Similarly
   objects can implement this interface to simply deny moving at all, for example at certain points of time
   in the simulation.

   <p><b>IMPORTANT NOTE:</b> If using AdjustablePortrayal2D in conjunction with MovablePortrayal2D, 
   always wrap the MovablePortrayal2D inside the AdjustablePortrayal2D, not the other way around.
*/

public class MovablePortrayal2D extends SimplePortrayal2D
    {
    public SimplePortrayal2D child;

    public MovablePortrayal2D(SimplePortrayal2D child)
        {
        this.child = child;
        }
    
    public SimplePortrayal2D getChild(Object object)
        {
        if (child!=null) return child;
        else
            {
            if (!(object instanceof SimplePortrayal2D))
                throw new RuntimeException("Object provided to MovablePortrayal2D is not a SimplePortrayal2D: " + object);
            return (SimplePortrayal2D) object;
            }
        }
        
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        getChild(object).draw(object,graphics,info);
        }
        
    boolean selectsWhenMoved = true;

    /** Returns whether the MovablePortrayal2D selects the object when it is being moved. */
    public boolean getSelectsWhenMoved() { return selectsWhenMoved; }
    /** Sets whether the MovablePortrayal2D selects the object when it is being moved. */
    public void setSelectsWhenMoved(boolean val) { selectsWhenMoved = val; }        
        
    Point2D originalMousePosition = null;
    Point2D originalObjectPosition = null;
        
    public boolean handleMouseEvent(GUIState guistate, Manipulating2D manipulating, LocationWrapper wrapper, MouseEvent event, DrawInfo2D range, int type)
        {
        synchronized(guistate.state.schedule)
            {
            int id = event.getID();
            Point2D.Double objPos = ((FieldPortrayal2D)(wrapper.getFieldPortrayal())).getObjectPosition(wrapper.getObject(), range);

            // pressing should be only for hit objects -- selected objects should NOT get priority
            if (id == MouseEvent.MOUSE_PRESSED && objPos != null && type == SimplePortrayal2D.TYPE_HIT_OBJECT)
                {
                originalMousePosition = event.getPoint();     
                originalObjectPosition = objPos;  

                // we need to determine if we were actually hit
                DrawInfo2D hitRange = new DrawInfo2D(range);
                Double2D scale = ((FieldPortrayal2D)(wrapper.getFieldPortrayal())).getScale(range);

                // this magic basically creates a rectangle representing the hittable region of the object
                // and a small pixel where the mouse clicked.
                hitRange.draw.x = originalObjectPosition.getX();
                hitRange.draw.y = originalObjectPosition.getY();
                hitRange.draw.width = scale.x;
                hitRange.draw.height = scale.y;
                hitRange.clip.x = originalMousePosition.getX();
                hitRange.clip.y = originalMousePosition.getY();
                hitRange.clip.width = 1;
                hitRange.clip.height = 1;
                                                        
                if (hitObject(wrapper.getObject(), hitRange))
                    {
                    manipulating.setMovingWrapper(wrapper);
                    if (selectsWhenMoved)
                        manipulating.performSelection(wrapper);  // make sure we're selected, and all others deselected, so we're called again
                    return true;  // will cause a refresh
                    }
                else { originalMousePosition = originalObjectPosition = null; }  // clean up
                }
            // moving should only be for selected objects
            else if (id == MouseEvent.MOUSE_DRAGGED && type == SimplePortrayal2D.TYPE_SELECTED_OBJECT && originalObjectPosition != null)
                {
                Point2D currentMousePosition = event.getPoint();        

                // compute delta
                Point2D.Double d = new Point2D.Double(
                    originalObjectPosition.getX() + (currentMousePosition.getX() - originalMousePosition.getX()),
                    originalObjectPosition.getY() + (currentMousePosition.getY() - originalMousePosition.getY())                            
                    );
                ((FieldPortrayal2D)(wrapper.getFieldPortrayal())).setObjectPosition(wrapper.getObject(), d, range);
                return true;
                }
            else if (id == MouseEvent.MOUSE_RELEASED && type == SimplePortrayal2D.TYPE_SELECTED_OBJECT)
                {
                originalMousePosition = null;
                originalObjectPosition = null;
                manipulating.setMovingWrapper(null);
                }
            }
        return getChild(wrapper.getObject()).handleMouseEvent(guistate, manipulating, wrapper, event, range, type);  // let someone else have it
        }
        
    public boolean hitObject(Object object, DrawInfo2D range)
        {
        return getChild(object).hitObject(object,range);
        }

    public boolean setSelected(LocationWrapper wrapper, boolean selected)
        {
        return getChild(wrapper.getObject()).setSelected(wrapper, selected);
        }

    public Inspector getInspector(LocationWrapper wrapper, GUIState state)
        {
        return getChild(wrapper.getObject()).getInspector(wrapper,state);
        }
    
    public String getName(LocationWrapper wrapper)
        {
        return getChild(wrapper.getObject()).getName(wrapper);
        }
    }
    
    
    
