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
 * Portrays objects as a cone of the specified color or appearance (flat opaque white by default)
 * which fills the region from (-0.5*scale,-0.5*scale,-0.5*scale) to (0.5*scale,0.5*scale,0.5*scale).
 * The axis of the cone runs along the Y axis, and the point of the cone is pointing towards positive Y.
 * Objects portrayed by this portrayal are selectable.
 */
public class ConePortrayal3D extends SimplePortrayal3D
    {
    public float scale = 1f;
    public Appearance appearance;
    public boolean generateNormals;
    public boolean generateTextureCoordinates;
        
    /** Constructs a ConePortrayal3D with a default (flat opaque white) appearance and a scale of 1.0. */
    public ConePortrayal3D()
        {
        this(1f);
        }
        
    /** Constructs a ConePortrayal3D with a default (flat opaque white) appearance and the given scale. */
    public ConePortrayal3D(float scale)
        {
        this(java.awt.Color.white,scale);
        }
        
    /** Constructs a ConePortrayal3D with a flat opaque appearance of the given color and a scale of 1.0. */
    public ConePortrayal3D(java.awt.Color color)
        {
        this(color,1f);
        }
        
    /** Constructs a ConePortrayal3D with a flat opaque appearance of the given color and the given scale. */
    public ConePortrayal3D(java.awt.Color color, float scale)
        {
        this(appearanceForColor(color),false,false,scale);
        }

    /** Constructs a ConePortrayal3D with the given (opaque) image and a scale of 1.0. */
    public ConePortrayal3D(java.awt.Image image)
        {
        this(image,1f);
        }

    /** Constructs a ConePortrayal3D with the given (opaque) image and scale. */
    public ConePortrayal3D(java.awt.Image image, float scale)
        {
        this(appearanceForImage(image,true),false,true,scale);
        }

    /** Constructs a ConePortrayal3D with the given appearance and scale, plus whether or not to generate normals or texture coordinates.  Without texture coordiantes, a texture will not be displayed. */
    public ConePortrayal3D(Appearance appearance, boolean generateNormals, boolean generateTextureCoordinates, float scale)
        {
        this.generateNormals = generateNormals;
        this.generateTextureCoordinates = generateTextureCoordinates;
        this.appearance = appearance;  this.scale = scale;
        }

    public TransformGroup getModel(Object obj, TransformGroup j3dModel)
        {
        if(j3dModel==null)
            {
            j3dModel = new TransformGroup();
            j3dModel.setCapability(Group.ALLOW_CHILDREN_READ);
            
            // make a Cone
            Cone cone = new Cone(scale/2,scale,
                                 /* Primitive.GEOMETRY_NOT_SHARED | */
                                 (generateNormals ? Primitive.GENERATE_NORMALS : 0) | 
                                 (generateTextureCoordinates ? Primitive.GENERATE_TEXTURE_COORDS : 0), appearance);
            
            // make all of its shapes pickable
            setPickableFlags(cone.getShape(Cone.BODY));
            setPickableFlags(cone.getShape(Cone.CAP));
            
            // build a LocationWrapper for the object
            LocationWrapper pickI = new LocationWrapper(obj, null, parentPortrayal);
            
            // Store the LocationWrapper in the user data of each shape
            cone.getShape(Cone.BODY).setUserData(pickI);
            cone.getShape(Cone.CAP).setUserData(pickI);

            j3dModel.addChild(cone);
            }
        return j3dModel;
        }
    }
