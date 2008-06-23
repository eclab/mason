/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.simple;

import com.sun.j3d.utils.geometry.*;
import sim.portrayal3d.*;
import sim.portrayal.*;
import javax.media.j3d.*;

/**
 * Portrays objects as a cylinder of the specified color or appearance (flat opaque white by default)
 * which fills the region from (-0.5*scale,-0.5*scale,-0.5*scale) to (0.5*scale,0.5*scale,0.5*scale).
 * The axis of the cylinder runs along the Y axis. Objects portrayed by this portrayal are selectable.
 */
public class CylinderPortrayal3D extends SimplePortrayal3D
    {
    public float scale = 1f;
    public Appearance appearance;
    public boolean generateNormals;
    public boolean generateTextureCoordinates;
    public Cylinder cylinder;
    public TransformGroup group;
    
    /** Constructs a CylinderPortrayal3D with a default (flat opaque white) appearance and a scale of 1.0. */
    public CylinderPortrayal3D()
        {
        this(1f);
        }
        
    /** Constructs a CylinderPortrayal3D with a default (flat opaque white) appearance and the given scale. */
    public CylinderPortrayal3D(float scale)
        {
        this(java.awt.Color.white,scale);
        }
        
    /** Constructs a CylinderPortrayal3D with a flat opaque appearance of the given color and a scale of 1.0. */
    public CylinderPortrayal3D(java.awt.Color color)
        {
        this(color,1f);
        }
        
    /** Constructs a CylinderPortrayal3D with a flat opaque appearance of the given color and the given scale. */
    public CylinderPortrayal3D(java.awt.Color color, float scale)
        {
        this(appearanceForColor(color),true,false,scale);
        }

    /** Constructs a CylinderPortrayal3D with the given (opaque) image and a scale of 1.0. */
    public CylinderPortrayal3D(java.awt.Image image)
        {
        this(image,1f);
        }

    /** Constructs a CylinderPortrayal3D with the given (opaque) image and scale. */
    public CylinderPortrayal3D(java.awt.Image image, float scale)
        {
        this(appearanceForImage(image,true),false,true,scale);
        }


    /** Constructs a CylinderPortrayal3D with the given appearance and scale, plus whether or not to generate normals or texture coordinates.  Without texture coordiantes, a texture will not be displayed. */
    public CylinderPortrayal3D(Appearance appearance, boolean generateNormals, boolean generateTextureCoordinates, float scale)
        {
        this.generateNormals = generateNormals;
        this.generateTextureCoordinates = generateTextureCoordinates;
        this.appearance = appearance;  this.scale = scale;

        cylinder = new Cylinder(0.5f,1f,
                                /* Primitive.GEOMETRY_NOT_SHARED | */
                                (generateNormals ? Primitive.GENERATE_NORMALS : 0) | 
                                (generateTextureCoordinates ? Primitive.GENERATE_TEXTURE_COORDS : 0),appearance);
        cylinder.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE); // may need to change the appearance (see below)
        cylinder.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE); // may need to change the geometry (see below)
        cylinder.clearCapabilityIsFrequent(Shape3D.ALLOW_APPEARANCE_WRITE);
        cylinder.clearCapabilityIsFrequent(Shape3D.ALLOW_GEOMETRY_WRITE);
        setPickableFlags(cylinder.getShape(Cylinder.BODY));
        setPickableFlags(cylinder.getShape(Cylinder.TOP));
        setPickableFlags(cylinder.getShape(Cylinder.BOTTOM));
        group = new TransformGroup();
        group.addChild(cylinder);
        }

    public TransformGroup getModel(Object obj, TransformGroup j3dModel)
        {
        if(j3dModel==null)
            {
            j3dModel = new TransformGroup();
            j3dModel.setCapability(Group.ALLOW_CHILDREN_READ);
            
            // build a LocationWrapper for the object
            LocationWrapper pickI = new LocationWrapper(obj, null, parentPortrayal);
            
            TransformGroup g = (TransformGroup) (group.cloneTree());
            Transform3D tr = new Transform3D();
            tr.setScale(scale);
            g.setTransform(tr);
            
            Cylinder cyl = (Cylinder) (g.getChild(0));
            cyl.setAppearance(appearance);
            
            // Store the LocationWrapper in the user data of each shape
            cyl.getShape(Cylinder.BODY).setUserData(pickI);
            cyl.getShape(Cylinder.TOP).setUserData(pickI);
            cyl.getShape(Cylinder.BOTTOM).setUserData(pickI);

            j3dModel.addChild(g);
            }
        return j3dModel;
        }
    }
