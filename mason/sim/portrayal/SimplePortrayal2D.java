/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal;
import java.awt.*;
import sim.display.*;
import java.awt.geom.*;

/** The superclass of all 2D Simple Portrayals.  Doesn't draw itself at all.
    Responds to hit testing by intersecting the hit testing rect with a width by
    height rectangle centered at 0,0.  Responds to requests for inspectors by
    providing a basic LabelledList which shows all the portrayed object's 
    object properties (see sim.util.SimpleProperties).  Responds to inspector
    update requests by updating this same LabelledList.
*/

public class SimplePortrayal2D implements Portrayal2D
    {
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        }
    
    /** If drawing area intersects selected area, return true.  The default computes
        the intersection with the (-0.5,-0.5) to (0.5,0.5) rectangle. */
    public  boolean hitObject(Object object, DrawInfo2D range)
        {
        return false;
        }
//        {
    // by default we return false on being hit
    //return( range.clip.intersects( range.draw.x-range.draw.width/2, 
    //                               range.draw.y-range.draw.height/2, range.draw.width, range.draw.height ) );
//        }
    
    public boolean setSelected(LocationWrapper wrapper, boolean selected)
        {
        return true;
        }

    public void move(LocationWrapper wrapper, Dimension2D distance)
        {
        }

    public Inspector getInspector(LocationWrapper wrapper, GUIState state)
        {
        if (wrapper == null) return null;
        return new SimpleInspector(wrapper.getObject(), state, "Properties");
        }
    
    public String getStatus(LocationWrapper wrapper) { return getName(wrapper); }
    
    public String getName(LocationWrapper wrapper)
        {
        if (wrapper == null) return "";
        return "" + wrapper.getObject();
        }
    }
