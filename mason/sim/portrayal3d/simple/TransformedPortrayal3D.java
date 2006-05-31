/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.simple;

import sim.portrayal.*;
import sim.portrayal3d.*;
import sim.display.*;
import javax.media.j3d.*;
import javax.vecmath.*;

/**
   A wrapper for other Portrayal3Ds which transforms them with an underlying Transform3D: meaning
   that you can rotate them, translate them, scale them, etc.    When you create this
   TransformedPortrayal3D, you will pass in an underlying Portrayal3D which is supposed to represent
   the actual object; TransformedPortrayal3D will then transform it as requested.  If the object
   will draw itself (it's its own Portrayal3D), you can signify this by passing in null for the
   underlying Portrayal3D.
    
   <p>You can provide a Transform3D with the child if you like.  Otherwise a default Transform3D
   (which does nothing) will be provided.  From there you can further rotate, translate, or scale
   the underlying Transform3D using the provided functions.  This is essentially equivalent to the
   similar features provided in FieldPortrayal3Ds and in Display3D for transforming Fields or the
   entire scene.
   
   <p>If you change the transform at any time through the provided functions,
   the underlying models will be updated appropriately.
*/

public class TransformedPortrayal3D extends SimplePortrayal3D
    {
    protected SimplePortrayal3D child;
    
    public TransformedPortrayal3D(SimplePortrayal3D child, Transform3D transform)
        {
        this.child = child;
        setTransform(transform);
        }
    
    public TransformedPortrayal3D(SimplePortrayal3D child)
        {
        this(child,null);
        }

    public PolygonAttributes polygonAttributes()
        { 
        return child.polygonAttributes(); 
        }

    public Inspector getInspector(LocationWrapper wrapper, GUIState state)
        {
        return child.getInspector(wrapper,state);
        }
        
    public String getName(LocationWrapper wrapper)
        {
        return child.getName(wrapper);
        }
    
    public void setParentPortrayal(FieldPortrayal3D p)
        {
        child.setParentPortrayal(p);
        }

    public boolean setSelected(LocationWrapper wrapper, boolean selected)
        {
        return child.setSelected(wrapper,selected);
        }
        
    public SimplePortrayal3D getChild(Object object)
        {
        if (child!=null) return child;
        else
            {
            if (!(object instanceof SimplePortrayal3D))
                throw new RuntimeException("Object provided to TransformedPortrayal3D is not a SimplePortrayal3D: " + object);
            return (SimplePortrayal3D) object;
            }
        }
        
    public TransformGroup getModel(Object obj, TransformGroup previousTransformGroup)
        {
        TransformGroup internalTransformGroup;
        if (previousTransformGroup == null)
            {
            internalTransformGroup = getChild(obj).getModel(obj,null);
            internalTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
            internalTransformGroup.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
            internalTransformGroup.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
            internalTransformGroup.clearCapabilityIsFrequent(TransformGroup.ALLOW_CHILDREN_READ);
            internalTransformGroup.clearCapabilityIsFrequent(TransformGroup.ALLOW_CHILDREN_WRITE);
            updateInternalTransform = true;
            previousTransformGroup = new TransformGroup();
            previousTransformGroup.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
            previousTransformGroup.clearCapabilityIsFrequent(TransformGroup.ALLOW_CHILDREN_READ);
            previousTransformGroup.addChild(internalTransformGroup);
            }
        else 
            {
            internalTransformGroup = (TransformGroup)(previousTransformGroup.getChild(0));
            getChild(obj).getModel(obj,internalTransformGroup);
            }
        if (updateInternalTransform)
            internalTransformGroup.setTransform(internalTransform);
        return previousTransformGroup;
        }

    Transform3D internalTransform;
    boolean updateInternalTransform = false;
    
    /** Sets the TransformedPortrayal3D's internal Transform.  This is a user-modifiable
        transform which should be used primarily to adjust the location of the TransformedPortrayal3D
        relative to <i>other TransformedPortrayal3D</i> objects.  If null is provided, then
        the value of getDefaultTransform() is used.  */
    public void setTransform(Transform3D transform)
        {
        if (transform == null) internalTransform = getDefaultTransform();
        else internalTransform = new Transform3D(transform);
        updateInternalTransform = true;
        }
    
    /** Returns the default internal transform suggested for this TransformedPortrayal3D. */
    public Transform3D getDefaultTransform()
        {
        return new Transform3D();
        }
    
    /** Returns a copy of the current internal transform for the TransformedPortrayal3D. */
    public Transform3D getTransform()
        {
        return new Transform3D(internalTransform);
        }
    
    /** Changes the internal transform of the TransformedPortrayal3D by 
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
                

    }
