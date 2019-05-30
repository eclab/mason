/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal;
import java.awt.*;
import sim.display.*;
import java.awt.geom.*;
import java.awt.event.*;

/** The superclass of all 2D Simple Portrayals.  Doesn't draw itself at all.
    Responds to hit testing by intersecting the hit testing rect with a width by
    height rectangle centered at 0,0.  Responds to requests for inspectors by
    providing a basic LabelledList which shows all the portrayed object's 
    object properties (see sim.util.SimpleProperties).  Responds to inspector
    update requests by updating this same LabelledList.
*/

// Note that this class implements java.io.Serializable.  This will cause FindBugs
// and other static analyzers to complain about various non-Serializable objects
// in non-transient instance variables in subclasses of this class.  But it's
// necessary in order to allow user-defined objects to portray themselves without
// forcing this to become an interface.
public class SimplePortrayal2D implements Portrayal2D, java.io.Serializable
    {
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        }
    
    /** Return true if the given object, when drawn, intersects with a provided rectangle, for
        hit testing purposes.  The object is drawn with an origin at (info.draw.x, info.draw.y),
        and with the coordinate system scaled by so that 1 unit is in the x and
        y directions are equal to info.draw.width and info.draw.height respectively
        in pixels.  The rectangle given by info.clip specifies the region to do hit testing in;
        often this region is actually of 0 width or height, which might represent a single point.
        It is possible that object
        is null.  The location of the object in the field may (and may not) be stored in
        info.location.  The form of that location varies depending on the kind of field used. */
                
    public boolean hitObject(Object object, DrawInfo2D range)
        {
        return false;
        }
    
    public boolean setSelected(LocationWrapper wrapper, boolean selected)
        {
        return true;
        }

    public static final int TYPE_SELECTED_OBJECT = 0;
    public static final int TYPE_HIT_OBJECT = 1;
        
    /**
       Optionally handles a mouse event.  At present, events are sent to SimplePortrayal2Ds representing objects which have been either
       selected or are presently hit by the event coordinates.  The wrapper provides the field portrayal, object location, and object.
       Also provided are the display, event, the DrawInfo2D for the field portrayal, and the type of mouse event situation
       (either because the object was SELECTED or because it was HIT).
                
       <p>To indicate that the event was handled, return true.  The default blank implementation of this method simply
       returns false.  Events are first sent to portrayals selected objects, until one of them handles the event.  If none
       handled the event, then events are sent to portrayals of objects hit by the event, until one of *them* handles the event.
       If still no one has handled the event, then the Display2D will route the event to built-in mechanisms such selecting
       the object or inspecting it.
           
       <p>If you're modifying or querying the model as a result of this event, be sure to lock on guistate.state.schedule before
       you do so.
    */
    public boolean handleMouseEvent(GUIState guistate, Manipulating2D manipulating, LocationWrapper wrapper,
        MouseEvent event, DrawInfo2D fieldPortrayalDrawInfo, int type)
        {
        return false;
        }

    public Inspector getInspector(LocationWrapper wrapper, GUIState state)
        {
        if (wrapper == null) return null;
        return Inspector.getInspector(wrapper.getObject(), state, "Properties");
        }
    
    public String getStatus(LocationWrapper wrapper) { return getName(wrapper); }
    
    public String getName(LocationWrapper wrapper)
        {
        if (wrapper == null) return "";
        return "" + wrapper.getObject();
        }
    }
