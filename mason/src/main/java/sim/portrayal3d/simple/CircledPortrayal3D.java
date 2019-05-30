/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.simple;

import sim.portrayal.*;
import sim.portrayal3d.*;
import sim.display.*;
import javax.media.j3d.*;
import com.sun.j3d.utils.geometry.*;
import sim.display3d.*;
import java.awt.*;

/**
   A wrapper for other Portrayal3Ds which also draws a big translucent sphere around them -- useful for
   distinguishing one object from other similar-looking objects.  When you create this
   CirclePortrayal3D, you will pass in an underlying Portrayal3D which is supposed to draw
   the actual object; CirclePortrayal3D will then add on the sphere.  If the object
   will draw itself (it's its own Portrayal3D), you can signify this by passing in null for the
   underlying Portrayal3D.
      
   <p>There are certain guidelines you can specify for when the sphere is to be drawn.  At construction
   time you can state that the sphere should <i>only</i> be drawn when the object is selected.
   Additionally if you call the setCircleShowing(...) function, you can turn off or on sphere
   drawing entirely for this CirclePortrayal3D.
   
   <p>You may specify a color or appearance for the sphere (the default is 25%-opaque flat white).  If
   you specify a color with some degree of alpha, the sphere will be drawn to that degree of transparency.
   You can also specify a scale -- equivalent to the diameter of the sphere.  
   The default scale (2.0f) draws the sphere in the (-1,-1,-1) to (1,1,1) box.
   
   <p>Why's it not called a "SpheredPortrayal3D?"  One, because there's no such word as "sphered",
   you big goof.  More importantly, we thought it'd be useful to stay consistent with CircledPortrayal2D.
*/
   

public class CircledPortrayal3D extends SimplePortrayal3D
    {
    public final static Appearance DEFAULT_CIRCLED_APPEARANCE = appearanceForColor(new Color(255,255,255,63));
    public final static double DEFAULT_SCALE = 2.0;
        
    double scale;
    Appearance appearance;
    SimplePortrayal3D child;
    
    public CircledPortrayal3D(SimplePortrayal3D child)
        {
        this(child, DEFAULT_SCALE);
        }
    
    public CircledPortrayal3D(SimplePortrayal3D child, double scale)
        {
        this(child,scale, false);
        }
        
    public CircledPortrayal3D(SimplePortrayal3D child, double scale, boolean onlyCircleWhenSelected)
        {
        this(child,DEFAULT_CIRCLED_APPEARANCE,scale, onlyCircleWhenSelected);
        }

    public CircledPortrayal3D(SimplePortrayal3D child, Color color)
        {
        this(child,color, DEFAULT_SCALE ,false);
        }

    public CircledPortrayal3D(SimplePortrayal3D child, Color color, double scale, boolean onlyCircleWhenSelected)
        {
        this(child,appearanceForColor(color),scale,onlyCircleWhenSelected);
        }
    
    public CircledPortrayal3D(SimplePortrayal3D child, Appearance appearance, double scale, boolean onlyCircleWhenSelected)
        {
        this.child = child;
        this.appearance = appearance; 
        this.scale = scale;
        this.onlyCircleWhenSelected = onlyCircleWhenSelected;
        }

    public PolygonAttributes polygonAttributes()
        { 
        return child.polygonAttributes(); 
        }

    public Inspector getInspector(LocationWrapper wrapper, GUIState state)
        {
        return child.getInspector(wrapper,state);
        }
        
    public String getName(LocationWrapper wrapper)
        {
        return child.getName(wrapper);
        }
    
    /** Sets the current display both here and in the child. */
    public void setCurrentDisplay(Display3D display)
        {
        super.setCurrentDisplay(display);
        child.setCurrentDisplay(display);
        }
                
    /** Sets the current field portrayal both here and in the child. */
    public void setCurrentFieldPortrayal(FieldPortrayal3D p)
        {
        super.setCurrentFieldPortrayal(p);
        child.setCurrentFieldPortrayal(p);
        }

    public boolean setSelected(LocationWrapper wrapper, boolean selected)
        {
        if (child.setSelected(wrapper,selected))
            return super.setSelected(wrapper, selected);
        else return false;  // which will bypass the selection procedure entirely.
        }
        
    public SimplePortrayal3D getChild(Object object)
        {
        if (child!=null) return child;
        else
            {
            if (!(object instanceof SimplePortrayal3D))
                throw new RuntimeException("Object provided to CircledPortrayal3D is not a SimplePortrayal3D: " + object);
            return (SimplePortrayal3D) object;
            }
        }
        
    /** Overrides all drawing. */
    boolean showCircle = true;
    
    boolean onlyCircleWhenSelected;
    public void setOnlyCircleWhenSelected(boolean val) { onlyCircleWhenSelected = val;   }
    public boolean getOnlyCircleWhenSelected() { return onlyCircleWhenSelected; }
    
    public boolean isCircleShowing() { return showCircle; }
    public void setCircleShowing(boolean val) { showCircle = val;  }

    void updateSwitch(Switch jswitch, Object object)
        {
        if (showCircle && (isSelected(object) || !onlyCircleWhenSelected))
            jswitch.setWhichChild( Switch.CHILD_ALL );
        else 
            jswitch.setWhichChild( Switch.CHILD_NONE );
        }

    public TransformGroup getModel(Object obj, TransformGroup j3dModel)
        {
        if(j3dModel==null)
            {
            j3dModel = new TransformGroup();
            j3dModel.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
            j3dModel.clearCapabilityIsFrequent(TransformGroup.ALLOW_CHILDREN_READ);
            Switch jswitch = new Switch();
            jswitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
                        
            // make a sphere
            Sphere sphere = new Sphere((float)(scale/2.0),appearance);
            
            // it's not pickable
            clearPickableFlags(sphere);

            // get the child
            Node n = getChild(obj).getModel(obj,null);
            
            j3dModel.addChild(n);  // set at child 0
            jswitch.addChild(sphere);
            j3dModel.addChild(jswitch);  // set at child 1
            updateSwitch(jswitch, obj);
            }
        else
            {
            TransformGroup t = (TransformGroup)(j3dModel.getChild(0));
            getChild(obj).getModel(obj,t);
            updateSwitch((Switch)(j3dModel.getChild(1)), obj);
            }
        return j3dModel;
        }
    }
