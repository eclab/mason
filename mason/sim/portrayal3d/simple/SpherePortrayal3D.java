/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.simple;

import sim.portrayal3d.*;
import javax.media.j3d.*;
import sim.portrayal.*;
import com.sun.j3d.utils.geometry.*;

/**
 * Portrays objects as a sphere of the specified color or appearance (flat opaque white by default)
 * which fills the region from (-0.5*scale,-0.5*scale,-0.5*scale) to (0.5*scale,0.5*scale,0.5*scale).
 * Objects portrayed by this portrayal are selectable.
 */
public class SpherePortrayal3D extends SimplePortrayal3D
    {
    public float scale = 1f;
    public Appearance appearance;
    public static final int DEFAULT_DIVISIONS = 15;  // the default number of divisions in Java3D spheres according to the docs
    public int divisions;  
    public boolean generateNormals;
    public boolean generateTextureCoordinates;
        
    /** Constructs a SpherePortrayal3D with a default (flat opaque white) appearance and a scale of 1.0. */
    public SpherePortrayal3D()
        {
        this(1f);
        }
        
    /** Constructs a SpherePortrayal3D with a default (flat opaque white) appearance and the given scale. */
    public SpherePortrayal3D(float scale)
        {
        this(java.awt.Color.white,scale);
        }
        
    /** Constructs a SpherePortrayal3D with a flat opaque appearance of the given color and a scale of 1.0. */
    public SpherePortrayal3D(java.awt.Color color)
        {
        this(color,1f);
        }
        
    /** Constructs a SpherePortrayal3D with a flat opaque appearance of the given color and the given scale. */
    public SpherePortrayal3D(java.awt.Color color, float scale)
        {
        this(color,scale, DEFAULT_DIVISIONS);
        }

    /** Constructs a SpherePortrayal3D with a flat opaque appearance of the given color, scale, and divisions. */
    public SpherePortrayal3D(java.awt.Color color, float scale, int divisions)
        {
        this(appearanceForColor(color),false,false,scale,divisions);
        }

    /** Constructs a SpherePortrayal3D with the given (opaque) image and a scale of 1.0. */
    public SpherePortrayal3D(java.awt.Image image)
        {
        this(image,1f);
        }

    /** Constructs a SpherePortrayal3D with the given (opaque) image and scale. */
    public SpherePortrayal3D(java.awt.Image image, float scale)
        {
        this(image,scale,DEFAULT_DIVISIONS);
        }

    /** Constructs a SpherePortrayal3D with the given (opaque) image, scale, and divisions. */
    public SpherePortrayal3D(java.awt.Image image, float scale, int divisions)
        {
        this(appearanceForImage(image,true),false,true,scale,divisions);
        }

    /** Constructs a SpherePortrayal3D with the given appearance, divisions, and scale, plus whether or not to generate normals or texture coordinates.  Without texture coordiantes, a texture will not be displayed */
    public SpherePortrayal3D(Appearance appearance, boolean generateNormals, boolean generateTextureCoordinates, float scale, int divisions)
        {
        this.generateNormals = generateNormals;
        this.generateTextureCoordinates = generateTextureCoordinates;
        this.appearance = appearance;  this.scale = scale; this.divisions = divisions;
        }


    public TransformGroup getModel(Object obj, TransformGroup j3dModel)
        {
        if (j3dModel==null)
            {
            j3dModel = new TransformGroup();
            j3dModel.setCapability(Group.ALLOW_CHILDREN_READ);
            
            // make a sphere
            Sphere sphere = new Sphere(scale/2,Primitive.GEOMETRY_NOT_SHARED | 
                                       (generateNormals ? Primitive.GENERATE_NORMALS : 0) | 
                                       (generateTextureCoordinates ? Primitive.GENERATE_TEXTURE_COORDS : 0), 
                                       divisions, appearance);
            
            // make all of its shapes pickable -- in this case, spheres
            // only have one "side"
            setPickableFlags(sphere.getShape(Sphere.BODY));
            
            // build a LocationWrapper for the object
            LocationWrapper pickI = new LocationWrapper(obj, null, parentPortrayal);

            // Store the LocationWrapper in the user data of each shape
            sphere.getShape(Sphere.BODY).setUserData(pickI);

            j3dModel.addChild(sphere);
            }
        return j3dModel;
        }
    }
