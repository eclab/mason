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
import javax.vecmath.*;
import java.awt.*;
import java.util.*;

/**
   A wrapper for other Portrayal3Ds which also draws a textual label.  When you create this
   LabelledPortrayal3D, you will pass in an underlying Portrayal3D which is supposed to represent
   the actual object; LabelledPortrayal3D will then add on an underlying label.  If the object
   will draw itself (it's its own Portrayal3D), you can signify this by passing in null for the
   underlying Portrayal3D.

   <p>You provide a string label at construction time.  The LabelledPortrayal2D will retrieve the label
   by calling getLabel(Object obj, DrawInfo2D info) at draw time to get the expected label.
   By default the getLabel function will return the label
   you had provided at construction; if your provided label was null, then getLabel will by default
   return the string name of the object.  You are free to override this function to provide more
   specialized label information.  Beware that every time you change a label, the object has to generate
   a new label object and insert it into the system.  This is expensive.
   
   <p>There are certain guidelines you can specify for when the label is to be drawn.  At construction
   time you can state that the label should <i>only</i> be drawn when the object is selected.
   Additionally if you call the setLabelShowing(...) function, you can turn off or on label
   drawing entirely for this LabelledPortrayal2D.
   
   <p>You may specify a color, or font for the label.  You can also provide a scale for the text relative
   to the labelled object: a scale of 1.0f (the default) is fairly reasonable for small objects. 
   The label is drawn at the (0.5,0.5,0.5) corner of the object by default: you can change this offset
   in the constructor if you like.  
   
   <P>The label will always be drawn directly facing the viewer regardless of the rotation of the model.

   <p>Labelled
*/

public class LabelledPortrayal3D extends SimplePortrayal3D
    {
    public static final Transform3D DEFAULT_LABEL_OFFSET;
    static
        {
        DEFAULT_LABEL_OFFSET = transformForOffset(0.5f,0.5f,0.5f);
        }
    
    static Transform3D transformForOffset(float x, float y, float z)
        {
        Transform3D offset = new Transform3D();
        offset.setTranslation(new Vector3f(x,y,z));
        return offset;
        }
        
    public float scale = 1.0f;
    public Color color;
    public Transform3D offset;
    public Font font;
    protected SimplePortrayal3D child;
    public String label;
    
    public LabelledPortrayal3D(SimplePortrayal3D child, String label)
        {
        this(child,label,Color.white, false);
        }

    public LabelledPortrayal3D(SimplePortrayal3D child, String label, Color color, boolean onlyLabelWhenSelected)
        {
        this(child,DEFAULT_LABEL_OFFSET,new Font("SansSerif",Font.PLAIN, 24),
            label,color,1.0f,onlyLabelWhenSelected);
        }
    
    public LabelledPortrayal3D(SimplePortrayal3D child, float offsetx, float offsety, float offsetz, 
        Font font, String label, Color color, float scale, boolean onlyLabelWhenSelected)
        {
        this(child,transformForOffset(offsetx,offsety,offsetz),font,label,color,scale,onlyLabelWhenSelected);
        }        
        
    public LabelledPortrayal3D(SimplePortrayal3D child, Transform3D offset, Font font, String label, Color color,
        float scale, boolean onlyLabelWhenSelected)
        {
        this.child = child;
        this.color = color; this.offset = offset;
        this.onlyLabelWhenSelected = onlyLabelWhenSelected;
        this.label = label;
        this.font = font;
        this.scale = scale;
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
    
    public void setParentPortrayal(FieldPortrayal3D p)
        {
        child.setParentPortrayal(p);
        }
            
    public boolean setSelected(LocationWrapper wrapper, boolean selected)
        {
        if (child.setSelected(wrapper,selected))
            return super.setSelected(wrapper, selected);
        else return false;  // which will bypass the selection procedure entirely.
        }
        
    /** Returns a name appropriate for the object.  By default, this returns
        the label, or if label is null, then returns ("" + object).
        Override this to make a more customized label to display for the object
        on-screen.  The TransformGroup is the one for the underlying portrayal3d. */
    public String getLabel(Object object, TransformGroup j3dModel)
        {
        if (label==null) return ("" + object);
        else return label;
        }
    
    /** Overrides all drawing. */
    boolean showLabel = true;
    
    boolean onlyLabelWhenSelected;
    public void setOnlyLabelWhenSelected(boolean val) { onlyLabelWhenSelected = val; }
    public boolean getOnlyLabelWhenSelected() { return onlyLabelWhenSelected; }
    
    public boolean isLabelShowing() { return showLabel; }
    public void setLabelShowing(boolean val) { showLabel = val; }

    public SimplePortrayal3D getChild(Object object)
        {
        if (child!=null) return child;
        else
            {
            if (!(object instanceof SimplePortrayal3D))
                throw new RuntimeException("Object provided to LabelledPortrayal3D is not a SimplePortrayal3D: " + object);
            return (SimplePortrayal3D) object;
            }
        }
        
    public void updateSwitch(Switch jswitch, Object object)
        {
        // we do it this way rather than the obvious if/else
        // statement because it gets inlined this way (32 bytes vs. 36 bytes).
        // no biggie.
        jswitch.setWhichChild(
            (showLabel && (isSelected(object) || !onlyLabelWhenSelected)) ?
            Switch.CHILD_ALL : Switch.CHILD_NONE );
        }

    public TransformGroup getModel(Object obj, TransformGroup j3dModel)
        {
        if(j3dModel==null)
            {
            // The general idea here is: we put a Text2D label in TransformGroup
            // which is used to offset it relative to the underlying portrayal model.
            // Then we put the TransformGroupo in a Switch so we can turn the 
            // label on or off.  Then we put the Switch, plus the underlying
            // portrayal model, together in another TransformGroup.  The switch
            // will hold the label string so we can see if we need to re-write the
            // label text later if the string has changed.
            
            // get the child
            TransformGroup n = (TransformGroup)(getChild(obj).getModel(obj,null));
            
            // figure the label
            String l = getLabel(obj, n);
            
            // Make the outer (top-level) TransformGroup
            j3dModel = new TransformGroup();
            j3dModel.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
            j3dModel.clearCapabilityIsFrequent(TransformGroup.ALLOW_CHILDREN_READ);
            
            // Make the switch
            Switch jswitch = new Switch();
            jswitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
            jswitch.setCapability(Switch.ALLOW_CHILDREN_READ);
            jswitch.clearCapabilityIsFrequent(Switch.ALLOW_SWITCH_WRITE);
            jswitch.clearCapabilityIsFrequent(Switch.ALLOW_CHILDREN_READ);

            // Store the current label in the switch
            jswitch.setUserData(l);
                        
            // make the text
            Text2D text = new Text2D(l, new Color3f(color), font.getFamily(), font.getSize(), font.getStyle());
            text.setRectangleScaleFactor(scale/16.0f);
            
            // Windows is acting weird with regard to Text2D.  The text is way
            // too small.  Or is it that MacOSX is weird with the font way too big?
            // dunno yet.  Anyway, an alternative to Text2D is to do Text3D, but it's
            // significantly more expensive in terms of polygons, so I'd prefer not to
            // do it if I can.
            //Shape3D text = new Shape3D(new Text3D(new Font3D(font,new FontExtrusion()), l));
            
            // We want the Text2D to always be facing forwards.  So we dump its
            // geometry and appearance into an OrientedShape3D and use that instead.
            OrientedShape3D o3d = new OrientedShape3D(text.getGeometry(), text.getAppearance(),
                OrientedShape3D.ROTATE_ABOUT_POINT, new Point3f(0,0,0));
            o3d.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);  // may need to change the appearance (see below)
            o3d.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);  // may need to change the geometry (see below)
            o3d.clearCapabilityIsFrequent(Shape3D.ALLOW_APPEARANCE_WRITE);
            o3d.clearCapabilityIsFrequent(Shape3D.ALLOW_GEOMETRY_WRITE);

            // make the offset TransformGroup
            TransformGroup o = new TransformGroup();
            o.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
            o.clearCapabilityIsFrequent(TransformGroup.ALLOW_CHILDREN_READ);
            o.setTransform(offset);

            // the label shouldn't be pickable -- we'll turn this off in the TransformGroup
            clearPickableFlags(o);

            o.addChild(o3d);         // Add label to the offset TransformGroup
            jswitch.addChild(o);    // Add offset TransformGroup to the Switch
            j3dModel.addChild(n);   // Add the underlying model as child 0
            j3dModel.addChild(jswitch);  // Add the switch as child 1
            updateSwitch(jswitch, obj);       // turn the switch on/off
            }
        else
            {
            TransformGroup t = (TransformGroup)(j3dModel.getChild(0));
            Switch s = (Switch)(j3dModel.getChild(1));

            // do we need to make a new label?  Only if the label's changed
            String l = getLabel(obj, t);
            if (!s.getUserData().equals(l)                    // the label text has changed.  Time to rebuild.  Expensive.
                && showLabel)  // only rebuild if we're displaying.  If we're not selected, we still need to build.
                {  
                // make the text again
                Shape3D text = new Shape3D(new Text3D(new Font3D(font,new FontExtrusion()), l));

                //Text2D text = new Text2D(l, new Color3f(color), font.getFamily(), font.getSize(), font.getStyle());
                //text.setRectangleScaleFactor(scale/16.0f);
                
                // Grab the OrientedShape3D
                TransformGroup t2 = (TransformGroup)(s.getChild(0));
                OrientedShape3D o3d = (OrientedShape3D)(t2.getChild(0));
                
                // update its geometry and appearance to reflect the new text.
                o3d.setGeometry(text.getGeometry());
                o3d.setAppearance(text.getAppearance());
                }
            
            // at any rate... update the child and turn the switch on/off
            getChild(obj).getModel(obj,t);
            updateSwitch(s, obj);
            }
        return j3dModel;
        }
    }
