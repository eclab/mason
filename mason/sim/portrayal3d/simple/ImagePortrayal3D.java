/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.simple;

import sim.portrayal3d.*;
import sim.portrayal.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.awt.*;

/**
 * Portrays objects as a thin flat rectangle displaying a provided Image.  The rectangle can be
 * oriented (always facing the user) or nonoriented (rotateable).  If you don't understand this,
 * you probably want oriented.
 *
 * <p>The image scaling is handled similar to how it's done in ImagePortrayal2D: the rectangle is
 * scaled so that the smaller of the two dimensions (width, height) is equal to 1.0.  The rectangle
 * is then centered on its origin.
 *
 * <p>You can also specify whether or not the image is intended to be (semi-)transparent or
 * fully opaque.  If your image is fully opaque, you should DEFINITELY
 * state this, because semitransparent images have some serious Java3D bugs.  
 * Specifically, semitransparent images may not be drawn in the correct order if they're piled up
 * on top of one another.  Thus a portrayal in the background may incorrectly appear in front of a portrayal
 * in the foreground.
 *
 * <p>Objects portrayed by this portrayal are selectable.
 */

public class ImagePortrayal3D extends SimplePortrayal3D 
    {
    Shape3D shape;
     
    /** Constructs a (semi-)transparent, oriented ImagePortrayal3D */
    public ImagePortrayal3D(Image image)
        {
        this(image,true,false);
        }
        
    /** Constructs an ImagePortrayal3D */
    public ImagePortrayal3D(Image image, boolean oriented, boolean opaque)
        {
        float width = image.getWidth(null);
        float height = image.getHeight(null);
        
        // figure out the width and height so that the smaller of 
        // the two is of dimension 1, and the other one is
        // scaled proportionally.
        if (width > height)
            { width = width / height; height = 1.0f; }
        else
            { height = height / width; width = 1.0f; }
            
        // build the vertices using these measurements
        float[] vertices = new float[]
            {
            width/2, -height/2, 0f,
            width/2,  height/2,  0f,
            -width/2,  height/2,  0f,
            -width/2, -height/2,  0f,
            };
        
        // create an array out of the four vertices
        QuadArray geometry = new QuadArray(4,QuadArray.COORDINATES | QuadArray.TEXTURE_COORDINATE_2);
        geometry.setCoordinates(0,vertices);
        
        // specify the four corners of the image are the four vertices (Java3D pretends that the
        // image runs from (0,0) to (1,1) in pixels).  Thus the image is stretched to fit the
        // quad, but of course the quad has already been set up to be the right dimensions for the
        // image, so it's all good.
        geometry.setTextureCoordinate(0,0,new TexCoord2f(1,1));
        geometry.setTextureCoordinate(0,1,new TexCoord2f(1,0));
        geometry.setTextureCoordinate(0,2,new TexCoord2f(0,0));
        geometry.setTextureCoordinate(0,3,new TexCoord2f(0,1));
        
        Appearance appearance = appearanceForImage(image, opaque);
        PolygonAttributes pa = new PolygonAttributes();
        pa.setCullFace(oriented ? PolygonAttributes.CULL_BACK: PolygonAttributes.CULL_NONE);        
        appearance.setPolygonAttributes(pa);
        
        // make the shape
        if (oriented) shape = new OrientedShape3D(geometry, appearance,
            OrientedShape3D.ROTATE_ABOUT_POINT, new Point3f(0,0,0));
        else shape = new Shape3D(geometry, appearance);
        }

    public TransformGroup getModel(Object obj, TransformGroup j3dModel)
        {
        if(j3dModel==null)
            {
            j3dModel = new TransformGroup();
            j3dModel.setCapability(Group.ALLOW_CHILDREN_READ);
            
            // copy the shape
            Shape3D s = (Shape3D)(shape.cloneTree(false));  // I think we can share geometries

            // make it pickable
            setPickableFlags(s);
            
            // build a LocationWrapper for the object
            LocationWrapper pickI = new LocationWrapper(obj, null, parentPortrayal);

            // Store the LocationWrapper in the user data
            s.setUserData(pickI);

            j3dModel.addChild(s);
            }
        return j3dModel;
        }
    }
