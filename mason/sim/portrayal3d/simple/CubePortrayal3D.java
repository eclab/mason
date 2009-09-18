/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.simple;

import sim.portrayal3d.*;
import javax.vecmath.*;
import sim.portrayal.*;
import javax.media.j3d.*;

/**
 * Portrays objects as a cube of the specified color or appearance (flat opaque white by default)
 * which fills the region from (-0.5*scale,-0.5*scale,-0.5*scale) to (0.5*scale,0.5*scale,0.5*scale).
 * Objects portrayed by this portrayal are selectable.
 */
public class CubePortrayal3D extends SimplePortrayal3D
    {
    public float scale = 1f;
    public Appearance appearance;
    public boolean generateNormals;
    public boolean generateTextureCoordinates;
    
    /** Constructs a CubePortrayal3D with a default (flat opaque white) appearance and a scale of 1.0. */
    public CubePortrayal3D()
        {
        this(1f);
        }
        
    /** Constructs a CubePortrayal3D with a default (flat opaque white) appearance and the given scale. */
    public CubePortrayal3D(float scale)
        {
        this(java.awt.Color.white,scale);
        }
        
    /** Constructs a CubePortrayal3D with a flat opaque appearance of the given color and a scale of 1.0. */
    public CubePortrayal3D(java.awt.Color color)
        {
        this(color,1f);
        }
        
    /** Constructs a CubePortrayal3D with a flat opaque appearance of the given color and the given scale. */
    public CubePortrayal3D(java.awt.Color color, float scale)
        {
        this(appearanceForColor(color),scale);
        }
    public CubePortrayal3D(Appearance appearence)
        {
        this(appearence,1f);
        }

    public CubePortrayal3D(Appearance appearence, float scale)
        {
        this(appearence,false,false,scale);
        }


    /** Constructs a CubePortrayal3D with the given (opaque) image and a scale of 1.0. */
    public CubePortrayal3D(java.awt.Image image)
        {
        this(image,1f);
        }

    /** Constructs a CubePortrayal3D with the given (opaque) image and scale. */
    public CubePortrayal3D(java.awt.Image image, float scale)
        {
        this(appearanceForImage(image,true),false,true,scale);
        }

    /** Constructs a CubePortrayal3D with the given appearance and scale, plus whether or not to generate normals or texture coordinates.  Without texture coordiantes, a texture will not be displayed */
    public CubePortrayal3D(Appearance appearance, boolean generateNormals, boolean generateTextureCoordinates, float scale)
        {
        this.generateNormals = generateNormals;
        this.generateTextureCoordinates = generateTextureCoordinates;
        this.appearance = appearance;  
        this.scale = scale;
        for(int i=0;i<scaledVerts.length;i++)
            scaledVerts[i] = verts[i]*scale;
        }

    final float[] scaledVerts = new float[verts.length];

    static final float[] verts = 
        {
        // front face
        0.5f, -0.5f,  0.5f,                             0.5f,  0.5f,  0.5f,
        -0.5f,  0.5f,  0.5f,                            -0.5f, -0.5f,  0.5f,
        // back face
        -0.5f, -0.5f, -0.5f,                            -0.5f,  0.5f, -0.5f,
        0.5f,  0.5f, -0.5f,                             0.5f, -0.5f, -0.5f,
        // right face
        0.5f, -0.5f, -0.5f,                             0.5f,  0.5f, -0.5f,
        0.5f,  0.5f,  0.5f,                             0.5f, -0.5f,  0.5f,
        // left face
        -0.5f, -0.5f,  0.5f,                            -0.5f,  0.5f,  0.5f,
        -0.5f,  0.5f, -0.5f,                            -0.5f, -0.5f, -0.5f,
        // top face
        0.5f,  0.5f,  0.5f,                             0.5f,  0.5f, -0.5f,
        -0.5f,  0.5f, -0.5f,                            -0.5f,  0.5f,  0.5f,
        // bottom face
        -0.5f, -0.5f,  0.5f,                            -0.5f, -0.5f, -0.5f,
        0.5f, -0.5f, -0.5f,                             0.5f, -0.5f,  0.5f,
        };

    public TransformGroup getModel(Object obj, TransformGroup j3dModel)
        {
        if(j3dModel==null)
            {
            j3dModel = new TransformGroup();
            j3dModel.setCapability(Group.ALLOW_CHILDREN_READ);
            
            QuadArray quadArray = new QuadArray(24, QuadArray.COORDINATES |
                (generateTextureCoordinates ? QuadArray.TEXTURE_COORDINATE_2 : 0) ); 
            quadArray.setCoordinates(0, scaledVerts);

            // specify the four corners of the image are four vertices (Java3D pretends that the
            // image runs from (0,0) to (1,1) in pixels).  Thus the image is stretched to fit the
            // quad, but of course the quad has already been set up to be the right dimensions for the
            // image, so it's all good.  -- dunno if this works, it works right for ImagePortrayal3D
            // -- maybe we'll have to go back to using a Box, ugh.
            if (generateTextureCoordinates)
                {
                quadArray.setTextureCoordinate(0,0,new TexCoord2f(1,1));
                quadArray.setTextureCoordinate(0,1,new TexCoord2f(1,0));
                quadArray.setTextureCoordinate(0,2,new TexCoord2f(0,0));
                quadArray.setTextureCoordinate(0,3,new TexCoord2f(0,1));
                }

            PolygonAttributes pa = new PolygonAttributes();
            pa.setCullFace(PolygonAttributes.CULL_BACK);
            if(!appearance.isLive())
                appearance.setCapability(Appearance.ALLOW_POLYGON_ATTRIBUTES_WRITE);
            appearance.setPolygonAttributes(pa);


            Shape3D localShape = new Shape3D(quadArray,appearance); 
            localShape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE); 
            setPickableFlags(localShape);
                        
            // build a LocationWrapper for the object
            LocationWrapper pickI = new LocationWrapper(obj, null, parentPortrayal);
            localShape.setUserData(pickI); 
                        
            j3dModel.addChild(localShape); 
            }
        return j3dModel;
        }
    }
