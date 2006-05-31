/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d;

import sim.portrayal.*;
import sim.display.*;
import javax.media.j3d.*;
import com.sun.j3d.utils.image.*;
import java.awt.*;

/** The superclass of all 3D Simple Portrayals which by default adds nothing to the 3D
    scene.  Since nothing is added to the scene, nothing is shown and you cannot
    select anything.  Subclasses add items to the scene, so they can then be selected
    if they so choose.  By default SimplePortrayal3Ds respond to requests for inspectors by
    providing a basic LabelledList which shows all the portrayed object's 
    object properties (see sim.util.SimpleProperties).  They also respond to inspector
    update requests by updating this same LabelledList.  No polygonAttributes are
    provided by default, and setSelected always returns false.
    
    <p>SimplePortrayal3Ds have a <i>parentPortrayal</i>, which is the FieldPortrayal3D
    which houses them.  This value can be null if the SimplePortrayal3D was added directly
    into the Display3D's collection of portrayals rather than being used inside
    a field portrayal.  The contract SimplePortrayal3Ds may assume is that the parentPortrayal,
    if it exists, will have been set prior to getModel(...) being called.
    
    <P>Various utility functions are provided.  setPickableFlags makes a Java3D object
    pickable (necessary for inspectability and selection).  clearPickableFlags does the opposite. 
    appearanceForColor creates a Java3D appearance that's a flat version of the color
    which requires no lighting -- a very basic default appearance function.  If the color
    has a degree of transparency, the appearance will as well.
    
    <p>A default appearance is provided for subclasses which wish to draw themselves
    using a default: flat white opaque.
*/

public class SimplePortrayal3D implements Portrayal3D
    {
    /** Flat white opaque. */
    public static final Appearance DEFAULT_APPEARANCE = appearanceForColor(Color.white);

    /** Creates an Appearance equivalent to a flat opaque surface of the provided color, needing no lighting. */
    public static Appearance appearanceForColor(java.awt.Color color)
        {
        Appearance appearance = new Appearance();
        float[] c = color.getRGBComponents(null);
        appearance.setColoringAttributes(
            new ColoringAttributes( c[0], c[1], c[2], ColoringAttributes.SHADE_FLAT));
        if (c[3] < 1.0)  // partially transparent
            appearance.setTransparencyAttributes(
                new TransparencyAttributes(TransparencyAttributes.BLENDED, 1.0f - c[3]));  // duh, alpha's backwards
        return appearance;
        }

    /** Creates an Appearance using the provided image.  If the image should be entirely opaque, you
        should definitely set <tt>opaque</tt> to true.  If you want to use the image's built-in transparency information,
        you should set <tt>opaque</tt> to false.  Beware that there are bugs in Java3D's handling of transparent 
        textures: multiple such objects often will not draw in the correct order; thus objects in the back
        may appear to be in the front. */
    public static Appearance appearanceForImage(java.awt.Image image, boolean opaque)
        {
        // build an appearance which is transparent except for the texture.
        Appearance appearance = appearanceForColor(java.awt.Color.red);
        if (!opaque)
            appearance.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.BLENDED,1.0f));
        appearance.setTexture(new TextureLoader(image, TextureLoader.BY_REFERENCE, null).getTexture());
        TextureAttributes ta = new TextureAttributes();
        ta.setTextureMode(TextureAttributes.REPLACE);
        PolygonAttributes pa = new PolygonAttributes();
        pa.setCullFace(PolygonAttributes.CULL_NONE);
        appearance.setPolygonAttributes(pa);
        appearance.setTextureAttributes(ta);
        return appearance;
        }

    /** Used by the SimplePortrayal3D to add its parent to its pickInfo object
        when the user picks the SimplePortrayal3D. */
    protected FieldPortrayal3D parentPortrayal = null;
        
    public PolygonAttributes polygonAttributes() { return null; } // default

    public TransformGroup getModel(Object object, TransformGroup prev)
        {
        // By default we'll not put anything in here.  But we still have to
        // create a TransformGroup if necessary.
        if (prev==null) prev = new TransformGroup();
        return prev;
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
    
    /** Sets the parent portrayal (a FieldPortrayal3D). */
    public void setParentPortrayal(FieldPortrayal3D p)
        {
        parentPortrayal = p;
        }

    // COPIED from SimplePortrayal2D
    public boolean setSelected(LocationWrapper wrapper, boolean selected)
        {
        // by default, we don't want to be selected
        if (selected) return false;  // don't want to be selected
        else return true;            // we'll always be deselected -- doesn't matter
        }
        
    /** Utility method which prepares the given Shape3D to be pickable (for selection and inspection). */
    public static void setPickableFlags(Shape3D s3d)
        {
        s3d.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
        setPickableFlags(s3d.getGeometry());
        // these are not going to be common, so we should state that they are infrequent
        s3d.clearCapabilityIsFrequent(Shape3D.ALLOW_GEOMETRY_READ);
        }
    
    /** Utility method which prepares the given Geometry to be pickable (for selection and inspection). */
    public static void setPickableFlags(Geometry geom)
        {
        geom.setCapability(GeometryArray.ALLOW_COUNT_READ);
        geom.setCapability(GeometryArray.ALLOW_FORMAT_READ);
        geom.setCapability(GeometryArray.ALLOW_COORDINATE_READ);
        // these are not going to be common, so we should state that they are infrequent
        geom.clearCapabilityIsFrequent(GeometryArray.ALLOW_COUNT_READ);
        geom.clearCapabilityIsFrequent(GeometryArray.ALLOW_FORMAT_READ);
        geom.clearCapabilityIsFrequent(GeometryArray.ALLOW_COORDINATE_READ);
        }
    
    /** Utility method which makes the given Node unpickable. */
    static public void clearPickableFlags(Node n)
        {
        n.setPickable(false);
        n.clearCapability(Group.ENABLE_PICK_REPORTING);
        }
    }
