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

/**
   A wrapper for other Portrayal2Ds which transforms the graphics space before drawing them. */

public class TransformedPortrayal2D extends SimplePortrayal2D
    {
    public SimplePortrayal2D child;
    public AffineTransform transform;

    public TransformedPortrayal2D(SimplePortrayal2D child, AffineTransform transform)
        {
        this.child = child; this.transform = transform;
        }
        
    public SimplePortrayal2D getChild(Object object)
        {
        if (child!=null) return child;
        else
            {
            if (!(object instanceof SimplePortrayal2D))
                throw new RuntimeException("Object provided to TransformedPortrayal2D is not a SimplePortrayal2D: " + object);
            return (SimplePortrayal2D) object;
            }
        }
    
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        final double theta = ((Oriented2D)object).orientation2D();
        transform.setToRotation(theta);
                
        AffineTransform old = graphics.getTransform();
        AffineTransform translationTransform = new AffineTransform();
        translationTransform.setToTranslation(info.draw.x,info.draw.y);
        graphics.transform(translationTransform);
        graphics.transform(transform);
        getChild(object).draw(object,graphics,new DrawInfo2D(info,-info.draw.x,-info.draw.y));
        // restore
        graphics.setTransform(old);
        }
        
    public boolean hitObject(Object object, DrawInfo2D range)
        {
        /* To-do */
        return false;
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
    
    
    
