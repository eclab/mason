package sim.portrayal3d.network;

import java.awt.*;
import javax.media.j3d.*;
import javax.media.j3d.TransformGroup;
import com.sun.j3d.utils.geometry.Primitive;

/**
 * This is an implementation (technically extension) of GenericEdgePortrayal3D
 * for java3D Primitives.  
 * For an example of how to use arbitrary java3D constructurs as edgePortrayals,
 * see ArrowEdgePortrayal3D.
 * 
 * @author Gabriel Balan
 */
public abstract class PrimitiveEdgePortrayal3D extends GenericEdgePortrayal3D
    {
    public PrimitiveEdgePortrayal3D(Primitive model)
        {
        super(model);
        }

    public PrimitiveEdgePortrayal3D(Primitive model, Color labelColor)
        {
        super(model, labelColor);
        }

    public PrimitiveEdgePortrayal3D(Primitive model, Color labelColor, Font labelFont)
        {
        super(model, labelColor, labelFont);
        }

    /** 
     * Returns the shape by the given index.  Cylinder has three shapes
     * (BODY=0, TOP=1, BOTTOM=2), while Cone has two shapes (BODY=0, CAP=1) and
     * Sphere has a single shape (BODY=0).  Useful for use in for-loops
     * in combination with numShapes().  
     * 
     * Here's the structure of the j3dModel in this class:
     * TransformGroup   j3dModel (passed in and out of getModel())
     * TransformGroup   positioning the edge model between the end points.
     * Primitive                clone of edgeModelPrototype
     **/
    protected Shape3D getShape(TransformGroup j3dModel, int shapeIndex)
        {
        TransformGroup g = (TransformGroup)(j3dModel.getChild(0));
        Primitive p = (Primitive)(g.getChild(0));
        return p.getShape(shapeIndex);
        }

    }
