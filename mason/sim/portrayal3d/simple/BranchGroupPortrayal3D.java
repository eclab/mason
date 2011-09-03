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

import java.net.*;
import java.io.*;
import com.sun.j3d.loaders.*;
import com.sun.j3d.loaders.lw3d.*;
import com.sun.j3d.loaders.objectfile.*;
import com.sun.j3d.utils.picking.*;

/**
 * Loads a Lightwave 3D scene file (.lwo or .lws extension) or a Wavefront object file (.obj extension) into a BranchGroup, 
 * then attaches this as a portrayal.  All scene elements and attributes are loaded (fog, sound, etc. as well).  Generally
 * all geometry in the scene is loaded as a Shape3D.  If an Appearance is specified in the constructor, it is applied to all 
 * Shape3D objects in the scene.  Else they are left with the appearance specified in the file.
 *
 * <p>You can also use other loaders for other scene file formats.  NCSA Portfolio contains a large number of them, but 
 * it's not quite open source (free for academic, research, or internal business use).  See the MASON website for a link
 * to download and use the NCSA Portfolio library.
 */
 
public class BranchGroupPortrayal3D extends PrimitivePortrayal3D
    {
    public static BranchGroup getBranchGroupForResource(Class c, String resourceName) throws IllegalArgumentException, FileNotFoundException
        {
        return getBranchGroupForURL(c.getResource(resourceName));
        }
                
    public static BranchGroup getBranchGroupForURL(URL url) throws IllegalArgumentException, FileNotFoundException
        {
        String s = url.getPath().trim();
        s = s.substring(s.length() - 4);
        if (s.equalsIgnoreCase(".obj")) return new ObjectFile().load(url).getSceneGroup();
        if (s.equalsIgnoreCase(".lws") ||
            s.equalsIgnoreCase(".lwo")) return new Lw3dLoader().load(url).getSceneGroup();
        throw new IllegalArgumentException("Invalid extension to file name in url (" + url + "), MASON requires '.obj' or '.lws' at present time.");
        }
        
    public static BranchGroup getBranchGroupForFile(String filename) throws IllegalArgumentException, FileNotFoundException
        {
        String s = filename.trim();
        s = s.substring(s.length() - 4);
        if (s.equalsIgnoreCase(".obj")) return new ObjectFile().load(filename).getSceneGroup();
        if (s.equalsIgnoreCase(".lws")) return new Lw3dLoader().load(filename).getSceneGroup();
        throw new IllegalArgumentException("Invalid extension to file name (" + filename + "), MASON requires '.obj' or '.lws' at present time.");
        }

    /** Constructs a BranchGroupPortrayal3D with the given scene file loader without changing its appearance, scale, or transform. */
    public BranchGroupPortrayal3D(BranchGroup scene) 
        {
        this(scene, 1.0f, null);
        }
        
    /** Constructs a BranchGroupPortrayal3D with the given scene file loader without changing its appearance, but scaling it. */
    public BranchGroupPortrayal3D(BranchGroup scene, double scale) 
        {
        this(scene, scale, null);
        }

    /** Constructs a BranchGroupPortrayal3D with the given scene file loader without changing its appearance, but transforming it. */
    public BranchGroupPortrayal3D(BranchGroup scene, Transform3D transform) 
        {
        this(scene, transform, null);
        }

    /** Constructs a BranchGroupPortrayal3D with the given scene file loader without transforming it or scaling it, but changing its appearance (unless the appearance is null). */
    public BranchGroupPortrayal3D(BranchGroup scene, Appearance a) 
        {
        this(scene, 1.0f, a);
        }
        
    /** Constructs a BranchGroupPortrayal3D with the given scene file loader by scaling it and changing its appearance (unless the appearance is null). */
    public BranchGroupPortrayal3D(BranchGroup scene, double scale, Appearance a) 
        {
        setScale(null, scale);
        traverseForAttributes(scene);
        group = scene;
        appearance = a;
        }

    /** Constructs a BranchGroupPortrayal3D with the given scene file loader by transforming it and changing its appearance (unless the appearance is null). */
    public BranchGroupPortrayal3D(BranchGroup scene, Transform3D transform, Appearance a) 
        {
        setTransform(null, transform);
        traverseForAttributes(scene);
        group = scene;
        appearance = a;
        }


    // traverses the BranchGroup and sets all Shape3D geometries and other attributes
    // sufficient to allow proper picking.  This is done during construction.
    void traverseForAttributes(Node n)
        {
        if (n instanceof Shape3D)
            {
            Shape3D s = (Shape3D) n;
            setShape3DFlags(s);
            setPickableFlags(s);
            PickTool.setCapabilities(s, PickTool.INTERSECT_FULL);
            Geometry g = s.getGeometry();
            if (g instanceof CompressedGeometry)
                ((CompressedGeometry)g).setCapability(CompressedGeometry.ALLOW_GEOMETRY_READ);
            else if (g instanceof GeometryArray)
                {
                ((GeometryArray)g).setCapability(GeometryArray.ALLOW_COUNT_READ);
                ((GeometryArray)g).setCapability(GeometryArray.ALLOW_FORMAT_READ);
                }
            }
        else if (n instanceof Group)
            {
            Group g = (Group) n;
            for(int i = 0; i < g.numChildren(); i++)
                traverseForAttributes(g.getChild(i));
            }
        }

    // traverses the BranchGroup and sets all Shape3D userdata and appearance (if non-null).
    // This is done during getModel()
    void traverseForUserDataAndAppearance(Node n, LocationWrapper wrapper)
        {
        if (n instanceof Shape3D)
            {
            Shape3D s = (Shape3D) n;
            s.setUserData(wrapper);
            if (appearance != null)
                s.setAppearance(appearance);
            }
        else if (n instanceof Group)
            {
            Group g = (Group) n;
            for(int i = 0; i < g.numChildren(); i++)
                traverseForUserDataAndAppearance(g.getChild(i), wrapper);
            }
        }
                
    // basically a copy of the same code in PrimitivePortrayal3D but with
    // appearance setting removed and a change from O(n^2) to O(n) updates of userdata
    public TransformGroup getModel(Object obj, TransformGroup j3dModel)
        {
        if (j3dModel==null)
            {
            j3dModel = new TransformGroup();
            j3dModel.setCapability(Group.ALLOW_CHILDREN_READ);
            
            // build a LocationWrapper for the object
            LocationWrapper pickI = new LocationWrapper(obj, null, getCurrentFieldPortrayal());

            Node g = (Node) (group.cloneTree(true));

            if (transform != null)
                {
                TransformGroup tg = new TransformGroup();
                tg.setTransform(transform);
                tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
                tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
                tg.setCapability(Group.ALLOW_CHILDREN_READ);
                tg.addChild(g);
                g = tg;
                }
            j3dModel.addChild(g);

            traverseForUserDataAndAppearance(j3dModel, pickI);
            }
        return j3dModel;
        }

    /** Unused: returns 0 always. */
    protected int numShapes() { return 0; } 
    }
