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
 * An abstract superclass for portrayals involving Shape3D or various Primitive (sphere, cone, etc.) objects.
 * which fills the region from (-0.5*scale,-0.5*scale,-0.5*scale) to (0.5*scale,0.5*scale,0.5*scale).
 * Objects portrayed by this portrayal are selectable.
 */
public abstract class PrimitivePortrayal3D extends SimplePortrayal3D
    {
    // When the coder calls setTransform(...), and the model doesn't exist yet,
    // this is set instead, so that when the model DOES exist, getModel() will use it.
    Transform3D transform;

    // When the coder calls setAppearance(...), and the model doesn't exist yet,
    // this is set instead, so that when the model DOES exist, getModel() will use it.
    Appearance appearance;

    // This is cloned to create the model.  Typically this group holds a single element,
    // either a Shape3D object or a Primitive of some sort.  The model, which is also a
    // TransformGroup, will then hold onto this object (or more properly, a clone).  Note
    // that the outer model TransformGroup (called j3dModel throughout this code) is not owned
    // by us once we create it.  So if we want to rotate or scale the Shape3D or Primitive,
    // we do it by transforming 'group' instead upon creation.
    TransformGroup group;
        
    // This is the shape index by getAppearance to fetch an appearance from.  It's usually
    // the 'body'.
    int DEFAULT_SHAPE = 0;
        
    // indicates whether newly created models should be made pickable or not.
    boolean pickable = true;

    /** Sets common Shape3D flags to make its appearance and geometry easy to modify. */
    public static void setShape3DFlags(Shape3D shape)
        {
        shape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE); // may need to change the appearance (see below)
        shape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
        shape.setCapability(Shape3D.ALLOW_GEOMETRY_READ); // may need to change the geometry (see below)
        shape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE); // may need to change the geometry (see below)
        shape.clearCapabilityIsFrequent(Shape3D.ALLOW_APPEARANCE_READ);
        shape.clearCapabilityIsFrequent(Shape3D.ALLOW_APPEARANCE_WRITE);
        shape.clearCapabilityIsFrequent(Shape3D.ALLOW_GEOMETRY_READ);
        shape.clearCapabilityIsFrequent(Shape3D.ALLOW_GEOMETRY_WRITE);
        }

    /** Returns the shape by the given index.  Cylinder has three shapes
        (BODY=0, TOP=1, BOTTOM=2), while Cone has two shapes (BODY=0, CAP=1) and
        Sphere has a single shape (BODY=0).  Useful for use in for-loops
        in combination with numShapes().  */
    protected Shape3D getShape(TransformGroup j3dModel, int shapeIndex)
        {
        TransformGroup g = (TransformGroup)(j3dModel.getChild(0));
        Primitive p = (Primitive)(g.getChild(0));
        return p.getShape(shapeIndex);
        }

    /** Returns an appearance object suitable to set in setAppearance(...). If the j3DModel 
        is null, a brand new Appearance will be created and returned; otherwise the Appearance
        (not a copy) will be extracted from the j3DModel and provided to you.  It's good
        practice to call setAppearance(...) anyway afterwards.  Only call this method within
        getModel().  */
    protected Appearance getAppearance(TransformGroup j3dModel)
        {
        if (j3dModel == null) 
            {
            Appearance a = new Appearance(); 
            setAppearanceFlags(a);
            return a;
            }
        else return getShape(j3dModel, DEFAULT_SHAPE).getAppearance();
        }       

    /** Sets the Transform3D of the portrayal.  If the j3DModel isn't null, its transform
        is set directly.  If the j3DModel is null (probably because
        the model hasn't been built yet), an underlying transform will be set and then used
        when the model is built.  Only call this method within getModel(). */
    protected void setTransform(TransformGroup j3dModel, Transform3D transform)
        {
        if (j3dModel == null)   // just update the scale variable, it'll get set in getModel
            {
            this.transform = transform;
            }
        else                                    // update manually
            {
            TransformGroup g = (TransformGroup)(j3dModel.getChild(0));
            g.setTransform(transform);
            }
        }
        
    /** Sets the Transform3D of the portrayal to a given scaling value.  If the j3DModel isn't null, its transform
        is set directly.  If the j3DModel is null (probably because
        the model hasn't been built yet), an underlying transform will be set and then used
        when the model is built.  Only call this method within getModel(). */
    protected void setScale(TransformGroup j3dModel, float val)
        {
        Transform3D tr = new Transform3D();
        tr.setScale(val);
        setTransform(j3dModel, tr);
        }
        
        
    /** Sets the Appearance of the portrayal.  If the j3DModel isn't null, its transform
        is set directly.  If the j3DModel is null (probably because
        the model hasn't been built yet), an underlying appearance will be set and then used
        when the model is built.  Only call this method within getModel(). */
    protected void setAppearance(TransformGroup j3dModel, Appearance app)
        {
        if (j3dModel == null) 
            {
            appearance = app;
            }
        else
            {
            int numShapes = numShapes();
            for(int i = 0; i < numShapes; i++)
                getShape(j3dModel, i).setAppearance(app);
            }
        }
        
    /** Returns the number of shapes handled by this primitive or Shape3D.  
        Shape3D objects only have a single shape.  Cylinder has three shapes
        (BODY=0, TOP=1, BOTTOM=2), while Cone has two shapes (BODY=0, CAP=1) and
        Sphere has a single shape (BODY=0).  */
    protected abstract int numShapes();

    /* Sets objects as pickable or not.  If you call setPickable prior to the model
       being built in getModel(), then the model will be pickable or not as you specify. */
    public void setPickable(boolean val) { pickable = val; }

    /** We suggest that if you wish to override this to change the appearance or scale or transform 
        of the underlying model, do the changes first and THEN call super.getModel(obj, j3dModel).
        Be sure to also set the appearance/scale/transform of the model the first time 
        (that is, when j3dModel is null) as well as when something interesting changes necessitating
        an update.
    */
    public TransformGroup getModel(Object obj, TransformGroup j3dModel)
        {
        if (j3dModel==null)
            {
            j3dModel = new TransformGroup();
            j3dModel.setCapability(Group.ALLOW_CHILDREN_READ);
            
            // build a LocationWrapper for the object
            LocationWrapper pickI = new LocationWrapper(obj, null, parentPortrayal);

            TransformGroup g = (TransformGroup) (group.cloneTree());
            if (transform != null) g.setTransform(transform);
            g.setCapability(Group.ALLOW_CHILDREN_READ);
            j3dModel.addChild(g);

            int numShapes = numShapes();
            for(int i = 0; i < numShapes; i++)
                {
                Shape3D shape = getShape(j3dModel, i);
                shape.setAppearance(appearance);
                if (pickable) setPickableFlags(shape);

                // Store the LocationWrapper in the user data of each shape
                shape.setUserData(pickI);
                }
            }
        return j3dModel;
        }
    }
