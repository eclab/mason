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
public class CylinderPortrayal3D extends PrimitivePortrayal3D
    {    
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
        this.appearance = appearance;
        setScale(null, scale);
                
        Cylinder cylinder = new Cylinder(0.5f,1f,
            /* Primitive.GEOMETRY_NOT_SHARED | */
            (generateNormals ? Primitive.GENERATE_NORMALS : 0) | 
            (generateTextureCoordinates ? Primitive.GENERATE_TEXTURE_COORDS : 0),appearance);
                        
        setShape3DFlags(cylinder.getShape(Cylinder.BODY));
        setShape3DFlags(cylinder.getShape(Cylinder.TOP));
        setShape3DFlags(cylinder.getShape(Cylinder.BOTTOM));
/*
  group = new TransformGroup();
  group.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
  group.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
  group.addChild(cylinder);
*/
        group = cylinder;
        }

    protected int numShapes() { return 3; }
    }
