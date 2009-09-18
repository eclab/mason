/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.simple;

import javax.media.j3d.*;

import com.sun.j3d.utils.geometry.*;

/**
 * Portrays objects as a cube of the specified color or appearance (flat opaque white by default)
 * which fills the region from (-0.5*scale,-0.5*scale,-0.5*scale) to (0.5*scale,0.5*scale,0.5*scale).
 * Objects portrayed by this portrayal are selectable.
 */
public class BoxPortrayal3D extends PrimitivePortrayal3D
    {
//    public float scale = 1f;
//    public boolean generateNormals;
//    public boolean generateTextureCoordinates;
    
    /** Constructs a BoxPortrayal3D with a default (flat opaque white) appearance and a scale of 1.0. */
    public BoxPortrayal3D()
        {
        this(1f);
        }
        
    /** Constructs a BoxPortrayal3D with a default (flat opaque white) appearance and the given scale. */
    public BoxPortrayal3D(float scale)
        {
        this(java.awt.Color.white,scale);
        }
        
    /** Constructs a BoxPortrayal3D with a flat opaque appearance of the given color and a scale of 1.0. */
    public BoxPortrayal3D(java.awt.Color color)
        {
        this(color,1f);
        }
        
    /** Constructs a BoxPortrayal3D with a flat opaque appearance of the given color and the given scale. */
    public BoxPortrayal3D(java.awt.Color color, float scale)
        {
        this(appearanceForColor(color),scale);
        }
    public BoxPortrayal3D(Appearance appearance, float scale)
        {
        this(appearance,true,false,scale);
        }
    /** Constructs a BoxPortrayal3D with the given (opaque) image and a scale of 1.0. */
    public BoxPortrayal3D(java.awt.Image image)
        {
        this(image,1f);
        }

    /** Constructs a BoxPortrayal3D with the given (opaque) image and scale. */
    public BoxPortrayal3D(java.awt.Image image, float scale)
        {
        this(appearanceForImage(image,true),false,true,scale);
        }

    /** Constructs a BoxPortrayal3D with the given appearance and scale, plus whether or not to generate normals or texture coordinates.  Without texture coordiantes, a texture will not be displayed */
    public BoxPortrayal3D(Appearance appearance, boolean generateNormals, boolean generateTextureCoordinates, float scale)
        {
        this.appearance = appearance;
        setScale(null, scale);

        Box box = new Box(.5f,.5f,.5f,
            /* Primitive.GEOMETRY_NOT_SHARED | */
            (generateNormals ? Primitive.GENERATE_NORMALS : 0) | 
            (generateTextureCoordinates ? Primitive.GENERATE_TEXTURE_COORDS : 0),appearance);
                        
        setShape3DFlags(box.getShape(Box.BACK));
        setShape3DFlags(box.getShape(Box.FRONT));
        setShape3DFlags(box.getShape(Box.BOTTOM));
        setShape3DFlags(box.getShape(Box.TOP));
        setShape3DFlags(box.getShape(Box.LEFT));
        setShape3DFlags(box.getShape(Box.RIGHT));
/*
  group = new TransformGroup();
  group.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
  group.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
  group.addChild(box);
*/
        group = box;
        }


    
    /** Returns the number of shapes handled by this primitive or Shape3D.  
     * Box has 6: TOP, BOTTOM, BACK, FRONT, LEFT, RIGHT.
     * */
    protected int numShapes(){return 6;}

    }
