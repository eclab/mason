/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.simple;
import sim.portrayal.*;
import java.awt.*;
import sim.display.*;

/**
   A wrapper for other Portrayal2Ds which also draws a big circle around them -- useful for
   distinguishing one object from other similar-looking objects.  When you create this
   CirclePortrayal2D, you will pass in an underlying Portrayal2D which is supposed to draw
   the actual object; CirclePortrayal2D will then add on the circle.  If the object
   will draw itself (it's its own Portrayal2D), you can signify this by passing in null for the
   underlying Portrayal2D.
   
   <p>There are certain guidelines you can specify for when the circle is to be drawn.  At construction
   time you can state that the circle should <i>only</i> be drawn when the object is selected.
   Additionally if you call the setCircleShowing(...) function, you can turn off or on circle
   drawing entirely for this CirclePortrayal2D.
   
   <p>You may specify a color or paint for the circle (the default is blue).
   
   <p>The circle is drawn centered at the object's [info.draw.x, info.draw.y]
   origin and with the radius:

   <pre><tt>
   radius:     (int)(or * max(info.draw.width,info.draw.height)) + dr;
   </tt></pre>

   <p>... that is, or is a value which scales when you zoom in, and dr adds 
   additional fixed pixels.  The default is or = 1.0, dr = 0.  This draws the circle 
   at twice the expected width and height of the object.

   <p><b>Note:  </b> One oddity of CircledPortrayal2D is due to the fact that the circle is only
   drawn if the object is being drawn.  While most FieldPortrayals ask objects just off-screen
   to draw themselves just to be careful, if an object is significantly off-screen, it may not
   be asked to draw itself, and so the circle will not be drawn -- even though part of the circle 
   could be on-screen at the time!  C'est la vie.
*/

public class CircledPortrayal2D extends SimplePortrayal2D
    {
    public static final double DEFAULT_OR = 1.0;
    public static final int DEFAULT_DR = 0;
    
    /** The pre-scaling radius */
    public double or;
    
    /** The post-scaling radius offset */   
    public int dr;
    
    /** The Paint or Color of the circle */
    public Paint paint;
    
    public SimplePortrayal2D child;
    
    /** Overrides all drawing. */
    boolean showCircle = true;
    
    boolean onlyCircleWhenSelected;
    
    boolean isSelected = false;
    
    public void setOnlyCircleWhenSelected(boolean val) { onlyCircleWhenSelected = val; }
    public boolean getOnlyCircleWhenSelected() { return onlyCircleWhenSelected; }
    
    public boolean isCircleShowing() { return showCircle; }
    public void setCircleShowing(boolean val) { showCircle = val; }
    
    /** If child is null, then the underlying model object 
        is presumed to be a Portrayal2D and will be used. */
    public CircledPortrayal2D(SimplePortrayal2D child, int dr, double or, Paint paint, boolean onlyCircleWhenSelected)
        {
        this.dr = dr; this.or = or; this.child = child;
        this.paint = paint;  this.onlyCircleWhenSelected = onlyCircleWhenSelected;
        }
    
    /** Draw a circle of radius or = 1.0, dr = 0, in blue.  Draw the circle regardless of selection.
        If child is null, then the underlying model object is presumed to be a Portrayal2D and will be used. */
    public CircledPortrayal2D(SimplePortrayal2D child)
        {
        this(child, Color.blue, false);
        }
        
    /** Draw a circle of radius or = 1.0, dr = 0.
        If child is null, then the underlying model object is presumed to be a Portrayal2D and will be used. */
    public CircledPortrayal2D(SimplePortrayal2D child, Paint paint, boolean onlyCircleWhenSelected)
        {
        this(child, DEFAULT_DR, DEFAULT_OR, paint, onlyCircleWhenSelected);
        }

    public SimplePortrayal2D getChild(Object object)
        {
        if (child!=null) return child;
        else
            {
            if (!(object instanceof SimplePortrayal2D))
                throw new RuntimeException("Object provided to CircledPortrayal2D is not a SimplePortrayal2D: " + object);
            return (SimplePortrayal2D) object;
            }
        }
        
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        getChild(object).draw(object,graphics,info);

        if (showCircle && (isSelected || !onlyCircleWhenSelected))
            {
            final int diameter = 2 * ((int)(or * (info.draw.width > info.draw.height ? 
                        info.draw.width : info.draw.height)) + dr);
            
            final int x = (int)(info.draw.x - diameter / 2.0);
            final int y = (int)(info.draw.y - diameter / 2.0);
            final int d = (int)(diameter);

            graphics.setPaint(paint);
            graphics.drawOval(x,y,d,d);
            }
        }
        
    public boolean hitObject(Object object, DrawInfo2D range)
        {
        return getChild(object).hitObject(object,range);
        }

    public boolean setSelected(LocationWrapper wrapper, boolean selected)
        {
        isSelected = selected;
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
    
    
    
