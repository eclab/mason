/*
 * Created on Nov 18, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package sim.portrayal3d.network;

import java.awt.Color;
import javax.media.j3d.*;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.geometry.*;

import sim.portrayal3d.simple.*;
/**
 * This implementation of GenericEDgePortrayal3D uses Arrow, 
 * which aggregates two primitives.  This class provides 
 * to the super class with uniform/transparent access to all the shapes, 
 * regardless of the primitives they belong to.
 * 
 * @author Gabriel Balan
 */
public class ArrowEdgePortrayal3D extends GenericEdgePortrayal3D
    {   
    private static Vector3f dummyFrom = new Vector3f(0f,-1f,0f);
    private static Vector3f dummyTo = new Vector3f(0f,1f,0f);
    
    public ArrowEdgePortrayal3D()
        {
        this(0.5f);
        }
        
    public ArrowEdgePortrayal3D(float radius)
        {
        this(radius, null);
        }
        
    public ArrowEdgePortrayal3D(float radius, Appearance ap)
        {
        super(new Arrow(radius, dummyFrom, dummyTo, null, null, ap));
        }       


    public ArrowEdgePortrayal3D(Color labelColor)
        {
        super(new Arrow(1, dummyFrom, dummyTo, null, null, null), labelColor);
        }


    /** the arrow body has 3 (body, top, bottom), arrow head has 2 (bottom and body) */ 
    protected int numShapes(){return 5;}
    /** 
     * Returns the shape by the given index.  Cylinder shapes come first
     * (BODY=0, TOP=1, BOTTOM=2), Cone chape come last (BODY=3, CAP=4) 
     * 
     * Here's the structure of the j3dModel in this class:
     * TransformGroup                   j3dModel (passed in and out of getModel())
     * ->TransformGroup                 positioning the edge model between the end points.
     *  ->TransformGroup                a clone of the prototypical arrow you pass in the constructor)
     *          ->TransformGroup        arrowCylinderTransformGroup
     *                  ->Cylinder
     *          ->TransformGroup        arrowHeadTransformGroup
     *                  ->Cone
     *          ->TansformGroup         startLable, endLabel TransformGroups
     *                  
     **/
    protected Shape3D getShape(TransformGroup j3dModel, int shapeIndex)
        {
        TransformGroup endPointTG = (TransformGroup)(j3dModel.getChild(0));
        TransformGroup edgeModelClone = (TransformGroup)(endPointTG.getChild(0));
        int coneOffset = 3;
        
        if(shapeIndex<coneOffset)//it's the body
            {       
            TransformGroup arrowBody = (TransformGroup)(edgeModelClone.getChild(0));
            Cylinder c = (Cylinder)arrowBody.getChild(0);
            return c.getShape(shapeIndex);
            }
        TransformGroup arrowHead = (TransformGroup)(edgeModelClone.getChild(1));
        Cone c = (Cone)arrowHead.getChild(0);
        return c.getShape(shapeIndex-coneOffset);        
        }
    
//    public TransformGroup getModel(Object object, TransformGroup j3dModel)
//    {
//      boolean j3dModelWasNull = j3dModel == null;
//      j3dModel = super.getModel(object, j3dModel);
//      if(j3dModelWasNull)
//              j3dModel.setCapability(Group.ALLOW_CHILDREN_READ);
//      return j3dModel;
//    }
    
    protected void init(Node edgeModel)
        {
        super.init(edgeModel);
        Arrow arrow = (Arrow)edgeModel; 
        arrow.setCapability(Group.ALLOW_CHILDREN_READ);
        //In the future I will want to read the cylinder out of its TransformGroup
        ((TransformGroup)arrow.getChild(0)).setCapability(Group.ALLOW_CHILDREN_READ);
        //In the future I will want to read the cone out of its TransformGroup
        ((TransformGroup)arrow.getChild(1)).setCapability(Group.ALLOW_CHILDREN_READ);
                
        Cylinder body = arrow.arrowTail;
        PrimitivePortrayal3D.setShape3DFlags(body.getShape(Cylinder.BODY));
        PrimitivePortrayal3D.setShape3DFlags(body.getShape(Cylinder.TOP));
        PrimitivePortrayal3D.setShape3DFlags(body.getShape(Cylinder.BOTTOM));
        Cone head = arrow.arrowHead;
        PrimitivePortrayal3D.setShape3DFlags(head.getShape(Cone.BODY));
        PrimitivePortrayal3D.setShape3DFlags(head.getShape(Cone.CAP));
        }

    }
