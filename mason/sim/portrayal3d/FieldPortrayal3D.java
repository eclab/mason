/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d;
import sim.portrayal.*;
import sim.portrayal3d.simple.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.picking.*;

/** 
 * Superclass of all Field Portrayals in J3D.
 *
 * <p>This class implements the getModel() function by breaking it down into
 * two subsidiary functions which Field Portrayals need to implement: the
 * createModel(...) and updateModel(...) functions.  
 * 
 * <p>FieldPortrayal3D maintains
 * a user-modifiable transform called <i>internalTransform</i>, provides an
 * additional layer of transform indirection goodness.  internalTransform should
 * be used primarily to adjust the location of the FieldPortrayal3D relative to
 * other FieldPortrayal3Ds.  Note that the internalTransform is not the root
 * TransformGroup in the scenegraph handed to getModel(...) -- it's child of the root.
 * However, the createModel() and updateModel() functions will see it as the root
 * of their respective scenegraphs.  
 *
 * <p> Many FieldPortrayals need to further transform themselves: for example, grids
 * might need to shift themselves one-half a grid position.  These operations should
 * be done privately and should not fool around with internalTransform to get the job
 * done.
 *
 * <p> Most FieldPortrayal3Ds benefit from knowing if their underlying fields are immutable.
 *
 * <p> FieldPortrayal3Ds must also implement the <i>completedWrapper()</i> method,
 * which provides them with a PickIntersection and a LocationWrapper found stored in
 * the object at that intersection.
 *
 **/

 
public abstract class FieldPortrayal3D extends FieldPortrayal implements Portrayal3D
    {
    public PolygonAttributes polygonAttributes() { return null; } // default

    Transform3D internalTransform;
    boolean updateInternalTransform = false;
    
    /** Sets the FieldPortrayal3D's internal Transform.  This is a user-modifiable
        transform which should be used primarily to adjust the location of the FieldPortrayal3D
        relative to <i>other FieldPortrayal3D</i> objects.  If null is provided, then
        the value of getDefaultTransform() is used.  */
    public void setTransform(Transform3D transform)
        {
        if (transform == null) internalTransform = getDefaultTransform();
        else internalTransform = new Transform3D(transform);
        updateInternalTransform = true;
        }
    
    /** Returns the default internal transform suggested for this FieldPortrayal3D. */
    public Transform3D getDefaultTransform()
        {
        return new Transform3D();
        }
    
    /** Returns a copy of the current internal transform for the FieldPortrayal3D. */
    public Transform3D getTransform()
        {
        return new Transform3D(internalTransform);
        }
    
    /** Changes the internal transform of the FieldPortrayal3D by 
        appending to it the provided transform operation. */
    public void transform(Transform3D transform)
        {
        Transform3D current = getTransform();
        current.mul(transform, current);
        setTransform(current);
        }
    
    /** Resets the internal transform to the value of getDefaultTransform() */
    public void resetTransform()
        {
        setTransform(getDefaultTransform());
        }
    
    /** Modifies the internal transform by rotating along the current X axis the provided number of degrees. */
    public void rotateX(double degrees)
        {
        Transform3D other = new Transform3D();
        other.rotX(degrees * Math.PI / 180);
        transform(other);
        }
        
    /** Modifies the internal transform by rotating along the current Y axis the provided number of degrees. */
    public void rotateY(double degrees)
        {
        Transform3D other = new Transform3D();
        other.rotY(degrees * Math.PI / 180);
        transform(other);
        }

    /** Modifies the internal transform by rotating along the current Z axis the provided number of degrees. */    
    public void rotateZ(double degrees)
        {
        Transform3D other = new Transform3D();
        other.rotZ(degrees * Math.PI / 180);
        transform(other);
        }
        
    /** Modifies the internal transform by translating in the provided x, y, and z amounts. */    
    public void translate(double dx, double dy, double dz)
        {
        Transform3D other = new Transform3D();
        other.setTranslation(new Vector3d(dx,dy,dz));
        transform(other);
        }
        
    /** Modifies the internal transform by uniformly scaling it in all directions by the provided amount. */    
    public void scale(double value)
        {
        Transform3D other = new Transform3D();
        other.setScale(value);
        transform(other);
        }

    /** Modifies the internal transform by scaling it in a nonuniform fashion. Note that this is less efficient than a uniform scale*/    
    public void scale(double sx, double sy, double sz)
        {
        Transform3D other = new Transform3D();
        other.setScale(new Vector3d(sx,sy,sz));
        transform(other);
        }
                
    public FieldPortrayal3D() { this(null); }
    
    public FieldPortrayal3D(Transform3D transform)
        {
        defaultPortrayal = new SpherePortrayal3D();
        setTransform(transform);
        }

    /** 
     * White sphere as default portrayal
     * for objects that do not have any other specified to them
     * 
     * Note that it is not final, so it can be replaced. It
     * was chosen for its low triangle-count.
     */
    protected SimplePortrayal3D defaultPortrayal;
        
    public Portrayal getDefaultPortrayal()
        {
        return defaultPortrayal;
        }
        
    /** Produces the requested model.  You usually do not want to override this:
        instead you should implement the createModel and updateModel methods. 
    
        <p>The TransformGroup tree structure should be of the form 
        ModelTransform[InternalTransform[<i>model info</i>]].
        <p>InternalTransform is a hook for transforming the FieldPortrayal3D.  ModelTransform
        should not be played with. */
    
    public TransformGroup getModel(Object obj, TransformGroup previousTransformGroup)
        {
        TransformGroup internalTransformGroup;
        if (previousTransformGroup == null)
            {
            internalTransformGroup = createModel();
            internalTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
            updateInternalTransform = true;
            previousTransformGroup = new TransformGroup();
            previousTransformGroup.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
            previousTransformGroup.clearCapabilityIsFrequent(TransformGroup.ALLOW_CHILDREN_READ);
            previousTransformGroup.addChild(internalTransformGroup);
            }
        else 
            {
            internalTransformGroup = (TransformGroup)(previousTransformGroup.getChild(0));
            if (!immutableField || dirtyField) updateModel(internalTransformGroup);
            }
        if (updateInternalTransform)
            internalTransformGroup.setTransform(internalTransform);
        dirtyField = false;
        return previousTransformGroup;
        }

    /** Returns a tree structure of the form
        InternalTransform[<i>model info</i>].
        <p>...where InternalTransform is an identity transformgroup whose transform will be
        modified elsewhere (create it but don't play with it). */
    protected abstract TransformGroup createModel();
    
    /** Returns a tree structure of the form
        InternalTransform[<i>model info</i>].
        <p>...where InternalTransform is an identity transformgroup whose transform will be
        modified elsewhere (don't play with it).  By default, this function does nothing.
        Override it to update the model when it's called. */
    protected void updateModel(TransformGroup previousTransformGroup) { }
    
    /** Given the provided PickIntersection and the PickResult it came from, fill in w the location of 
        the picked object, and return it. Alternatively, return a new LocationWrapper with all 
        information filled in.  The PickResult is provided you in case the object is in a shared
        group and you need to identify what link had connected to it. */
    public abstract LocationWrapper completedWrapper(LocationWrapper w, PickIntersection pi, PickResult pr);
    }
