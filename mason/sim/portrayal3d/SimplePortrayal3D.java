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
import javax.vecmath.*;
import java.util.*;

/** The superclass of all 3D Simple Portrayals which by default adds nothing to the 3D
    scene.  Since nothing is added to the scene, nothing is shown and you cannot
    select anything.  Subclasses add items to the scene, so they can then be selected
    if they so choose.  By default SimplePortrayal3Ds respond to requests for inspectors by
    providing a basic LabelledList which shows all the portrayed object's 
    object properties (see sim.util.SimpleProperties).  They also respond to inspector
    update requests by updating this same LabelledList.  No polygonAttributes are
    provided by default, and setSelected always true by default.
    
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

    /** Creates an Appearance equivalent to a flat opaque surface of the provided color, needing no lighting.
        Opacity is determined by the opacity of the unlit color.  */
    public static Appearance appearanceForColor(java.awt.Color unlitColor)
        {
		Appearance appearance = new Appearance();
//        return appearanceForColor(unlitColor, null);
//        }
//
//    public static Appearance appearanceForColor(java.awt.Color unlitColor, Appearance setThisAppearance)
//        {
//        Appearance appearance;	
//        if (setThisAppearance == null) appearance = new Appearance();
//        else appearance = setThisAppearance;
//		appearance.setMaterial(null);  // remove material entirely
                
        setAppearanceFlags(appearance);
        float[] c = unlitColor.getRGBComponents(null);
        appearance.setColoringAttributes(
            new ColoringAttributes( c[0], c[1], c[2], ColoringAttributes.SHADE_FLAT));
        if (c[3] < 1.0)  // partially transparent
            appearance.setTransparencyAttributes(
                new TransparencyAttributes(TransparencyAttributes.BLENDED, 1.0f - c[3]));  // duh, alpha's backwards
        return appearance;
        }

	static final Color3f BLACK = new Color3f(Color.black);

    /** Creates an Appearance with the provided lit colors.  Objects will not appear if the scene is unlit.
        shininess and opacity both from 0.0 to 1.0.  If any color is null, it's assumed to be black.
		Note that even jet black ambient color will show up as a charcoal gray under the bright white
		ambient light in MASON.  That's Java3D for you, sorry. */
    public static Appearance appearanceForColors(java.awt.Color ambientColor, 
        java.awt.Color emissiveColor, java.awt.Color diffuseColor, 
        java.awt.Color specularColor, float shininess, float opacity)
        {
		Appearance appearance = new Appearance();

//        return appearanceForColors(ambientColor, emissiveColor, diffuseColor, specularColor, shininess, opacity, null);
//        }
//
//    public static Appearance appearanceForColors(java.awt.Color ambientColor, 
//        java.awt.Color emissiveColor, java.awt.Color diffuseColor, 
//        java.awt.Color specularColor, float shininess, float opacity, Appearance setThisAppearance)
//        {
//        Appearance appearance;
//        if (setThisAppearance == null) appearance = new Appearance();
//        else appearance = setThisAppearance;

        setAppearanceFlags(appearance);
        appearance.setColoringAttributes(new ColoringAttributes(BLACK, ColoringAttributes.SHADE_GOURAUD));

        if (opacity > 1.0f) opacity = 1.0f;
        if (opacity < 0.0f) opacity = 0.0f;
        if (shininess > 1.0f) shininess = 1.0f;
        if (shininess < 0.0f) shininess = 0.0f;
        shininess = shininess * 63.0f + 1.0f;  // to go between 1.0 and 64.0

        Material m = new Material();
        m.setCapability(Material.ALLOW_COMPONENT_READ);
        m.setCapability(Material.ALLOW_COMPONENT_WRITE);
		
        if (ambientColor != null) m.setAmbientColor(new Color3f(ambientColor));
		else m.setAmbientColor(BLACK);
		
        if (emissiveColor != null) m.setEmissiveColor(new Color3f(emissiveColor));
		else m.setEmissiveColor(BLACK);

        if (diffuseColor != null) m.setDiffuseColor(new Color3f(diffuseColor));
		else m.setDiffuseColor(BLACK);

        if (specularColor != null) m.setSpecularColor(new Color3f(specularColor));
		else m.setSpecularColor(BLACK);

        m.setShininess(shininess);
        appearance.setMaterial(m);
        if (opacity < 1.0f)  // partially transparent
            appearance.setTransparencyAttributes(
                new TransparencyAttributes(TransparencyAttributes.BLENDED, 1.0f - opacity));  // duh, alpha's backwards
        return appearance;
        }

    /** Creates an Appearance using the provided image.  If the image should be entirely opaque, you
        should definitely set <tt>opaque</tt> to true.  If you want to use the image's built-in transparency information,
        you should set <tt>opaque</tt> to false.  Beware that there are bugs in Java3D's handling of transparent 
        textures: multiple such objects often will not draw in the correct order; thus objects in the back
        may appear to be in the front. */
    public static Appearance appearanceForImage(java.awt.Image image, boolean opaque)
        {
        Appearance appearance = appearanceForColor(java.awt.Color.black);

//        return appearanceForImage(image, opaque, null);
//        }
//
//    public static Appearance appearanceForImage(java.awt.Image image, boolean opaque, Appearance setThisAppearance)
//        {
//        // build an appearance which is transparent except for the texture.
//        Appearance appearance = appearanceForColor(java.awt.Color.red, setThisAppearance);
//        setAppearanceFlags(appearance);  // already set!
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
		
	public boolean isSelected(Object obj)
		{
		return selectedObjects.containsKey(obj);
		}

    HashMap selectedObjects = new HashMap(1);
    public boolean setSelected(LocationWrapper wrapper, boolean selected)
        {
        if (selected)
            selectedObjects.put(wrapper.getObject(), wrapper);
        else
            selectedObjects.remove(wrapper.getObject());
        return true;
        }
        
    /** Sets a variety of flags on an Appearance so that its features can be modified
        when the scene is live.  This method cannot be called on an Appearance presently
        used in a live scene.  */
    public static void setAppearanceFlags(Appearance a)
        {
        a.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_READ);
        a.setCapabilityIsFrequent(Appearance.ALLOW_COLORING_ATTRIBUTES_READ);
        a.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
        a.setCapabilityIsFrequent(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
        a.setCapability(Appearance.ALLOW_MATERIAL_READ);
        a.setCapabilityIsFrequent(Appearance.ALLOW_MATERIAL_READ);
        a.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
        a.setCapabilityIsFrequent(Appearance.ALLOW_MATERIAL_WRITE);
        a.setCapability(Appearance.ALLOW_POLYGON_ATTRIBUTES_READ);
        a.setCapabilityIsFrequent(Appearance.ALLOW_POLYGON_ATTRIBUTES_READ);
        a.setCapability(Appearance.ALLOW_POLYGON_ATTRIBUTES_WRITE);
        a.setCapabilityIsFrequent(Appearance.ALLOW_POLYGON_ATTRIBUTES_WRITE);
        a.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
        a.setCapabilityIsFrequent(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
        a.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);
        a.setCapabilityIsFrequent(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);
        a.setCapability(Appearance.ALLOW_TEXTURE_ATTRIBUTES_READ);
        a.setCapabilityIsFrequent(Appearance.ALLOW_TEXTURE_ATTRIBUTES_READ);
        a.setCapability(Appearance.ALLOW_TEXTURE_ATTRIBUTES_WRITE);
        a.setCapabilityIsFrequent(Appearance.ALLOW_TEXTURE_ATTRIBUTES_WRITE);
        a.setCapability(Appearance.ALLOW_TEXTURE_READ);
        a.setCapabilityIsFrequent(Appearance.ALLOW_TEXTURE_READ);
        a.setCapability(Appearance.ALLOW_TEXTURE_WRITE);
        a.setCapabilityIsFrequent(Appearance.ALLOW_TEXTURE_WRITE);
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
