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

/**
   A wrapper for other Portrayal2Ds which makes it possible to do any or all of the following with the mouse:
   
   <ul>
   <p><li>View the orientation of an object
   <p><li>Rotate the object to a new orientation
   <p><li>Change the "scale" (size perhaps) of the object
   </ul>
        
   <p>This portrayal is used simply by wrapping around another portrayal or around null (if the object portrays itself).
   When the object is selected, an "adjustment ring" will appear overlaid on the object.  The ring shows the current orientation
   of the object.  If you drag the ring, you can change the orientation and scale of the object with your mouse.
        
   <p>To simply view orientation with the ring, the object must implement the Oriented2D interface.
        
   <p>To rotate the object, the object must implement the Orientable2D interface (which extends the Oriented2D interface).
        
   <p>To change the scale of the object, the object must implement the Scalable2D interface.

   <p><b>IMPORTANT NOTE:</b> If using AdjustablePortrayal2D in conjunction with MovablePortrayal2D, 
   always wrap the MovablePortrayal2D inside the AdjustablePortrayal2D, not the other way around.

*/

public class AdjustablePortrayal2D extends SimplePortrayal2D
    {
    public static final double CIRCLE_RADIUS = 30.0;
    public static final double KNOB_RADIUS = 5.0;
    public static final double SLOP = KNOB_RADIUS;
    public static final Paint LOWER_PAINT = new Color(255,255,255,200);
    public static final Stroke LOWER_STROKE = new BasicStroke(3.0f);
    public static final Paint UPPER_PAINT = new Color(0,0,0,255);
    public static final Stroke UPPER_STROKE = new BasicStroke(1.0f);
    public static final Ellipse2D circle = new Ellipse2D.Double();
    public static final Ellipse2D knob = new Ellipse2D.Double();
        
    public SimplePortrayal2D child;
        
    public AdjustablePortrayal2D(SimplePortrayal2D child)
        {
        this.child = child;
        }

    public SimplePortrayal2D getChild(Object object)
        {
        if (child!=null) return child;
        else
            {
            if (!(object instanceof SimplePortrayal2D))
                throw new RuntimeException("Object provided to AdjustablePortrayal2D is not a SimplePortrayal2D: " + object);
            return (SimplePortrayal2D) object;
            }
        }
        
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        SimplePortrayal2D child = getChild(object);
        child.draw(object,graphics,info);

        if (info.selected && object!=null && (object instanceof Oriented2D || object instanceof Scalable2D))
            {
            double orientation = 0.0;
            //double scale = 1.0;
            if (object instanceof Oriented2D)
                { orientation = ((Oriented2D)object).orientation2D(); }
            //if (object instanceof Scalable2D)
            //    scale = ((Scalable2D)object).getScale2D();
            circle.setFrame(info.draw.x - CIRCLE_RADIUS, info.draw.y - CIRCLE_RADIUS, CIRCLE_RADIUS * 2, CIRCLE_RADIUS * 2);
            knob.setFrame(info.draw.x + CIRCLE_RADIUS * Math.cos(orientation) - KNOB_RADIUS, info.draw.y + CIRCLE_RADIUS * Math.sin(orientation) - KNOB_RADIUS, KNOB_RADIUS * 2, KNOB_RADIUS * 2);
            graphics.setPaint(LOWER_PAINT);
            graphics.setStroke(LOWER_STROKE);
            graphics.draw(circle);
            graphics.draw(knob);
            graphics.setPaint(UPPER_PAINT);
            graphics.setStroke(UPPER_STROKE);
            graphics.draw(circle);
            graphics.fill(knob);
            }
        }
                
                
                
    boolean adjusting = false;
    Object adjustingObject = null;
    double adjustingInitialScale = 1.0;
    Point2D.Double adjustingInitialPosition = null;
        
    public boolean handleMouseEvent(GUIState guistate, Manipulating2D manipulating, LocationWrapper wrapper, MouseEvent event, DrawInfo2D range, int type)
        {
        synchronized(guistate.state.schedule)
            {
            Point2D.Double myPosition = ((FieldPortrayal2D)(wrapper.getFieldPortrayal())).getObjectPosition(wrapper.getObject(), range);
            Object object = wrapper.getObject();
            if (!   // if any of this stuff is true, just drop out
                    (myPosition == null || object == null ||                                                   // something is very wrong!
                    (adjusting && adjustingObject != object) || // we're not portraying the relevant object
                    !((object instanceof Scalable2D || object instanceof Orientable2D)))) // it's neither scalable nor orientable
                {
                //double orientation = 0.0;
                //if (object instanceof Oriented2D)
                //    { orientation = ((Oriented2D)object).orientation2D(); }
                //double knobX = myPosition.getX() + CIRCLE_RADIUS * Math.cos(orientation);
                //double knobY = myPosition.getY() + CIRCLE_RADIUS * Math.sin(orientation);
                                                
                int id = event.getID();
                if (id == MouseEvent.MOUSE_PRESSED)
                    {
                    if (adjusting && object == adjustingObject)  // looks spurious.  Better cancel it out just in case
                        {
                        adjusting = false;
                        adjustingObject = null;
                        }
                                                                
                    // nah, let's allow the user to grab anywhere on the ring.
                    double dx = event.getX() - myPosition.getX();
                    double dy = event.getY() - myPosition.getY();
                    double distance = Math.sqrt(dx*dx + dy*dy);
                    if (object instanceof Scalable2D)
                        adjustingInitialScale = ((Scalable2D)object).getScale2D();
                    if (Math.abs(distance - CIRCLE_RADIUS) <= SLOP)
                        {
                        adjusting = true;  // time to start adjusting!
                        adjustingObject = object;
                        adjustingInitialPosition = myPosition;
                        // don't let lower-level portrayals have it -- we handled the event
                        return true;
                        }
                    }
                else if (id == MouseEvent.MOUSE_DRAGGED && adjusting)
                    {
                    double dx = event.getX() - adjustingInitialPosition.getX();
                    double dy = event.getY() - adjustingInitialPosition.getY();
                    double newOrientation = Math.atan2(dy, dx);
                    double newScaleMultiplier = Math.sqrt(dx*dx + dy*dy) / CIRCLE_RADIUS;
                    if (object instanceof Orientable2D)
                        ((Orientable2D)object).setOrientation2D(newOrientation);
                    if (object instanceof Scalable2D)
                        ((Scalable2D)object).setScale2D(adjustingInitialScale * newScaleMultiplier);
                    // don't let lower-level portrayals have it -- we handled the event
                    return true;
                    }
                else if (id == MouseEvent.MOUSE_RELEASED && adjusting)
                    {
                    adjusting = false;
                    adjustingObject = null;
                    adjustingInitialScale = 1.0;
                    adjustingInitialPosition = null;
                    }
                }
            }
        return getChild(wrapper.getObject()).handleMouseEvent(guistate, manipulating, wrapper, event, range, type);  // let any lower portrayals have it, else false
        }
        
    public boolean hitObject(Object object, DrawInfo2D range)
        {
        return getChild(object).hitObject(object,range);
        }

    public boolean setSelected(LocationWrapper wrapper, boolean selected)
        {
        Object object = wrapper.getObject();
        if (!selected && adjusting && object == adjustingObject)
            adjusting = false;  // cancel spurious deselections without a MOUSE_RELEASED
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
    
    
    
