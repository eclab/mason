/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/
package sim.portrayal3d.network;

import java.awt.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import sim.util.*;
import com.sun.j3d.utils.geometry.*;
import sim.portrayal3d.simple.*;

/**
 * This implementation of GenericEdgePortrayal3D uses Arrow, 
 * which aggregates two primitives.  This class provides 
 * to the super class with uniform/transparent access to all the shapes, 
 * regardless of the primitives they belong to.
 * 
 * @author Gabriel Balan
 * In 2006
 */
 
public class ArrowEdgePortrayal3D extends PrimitiveEdgePortrayal3D
    {   
    static Double3D dummyFrom = new Double3D(0f,-1f,0f);
    static Double3D dummyTo = new Double3D(0f,1f,0f);
        
    public ArrowEdgePortrayal3D()
        {
        this(null, Color.white, null, DEFAULT_RADIUS);
        }

    /** @deprecated */
    public ArrowEdgePortrayal3D(double radius)
        {
        this(null, Color.white, null, radius);
        }
        
    /** @deprecated */
    public ArrowEdgePortrayal3D(double radius, Appearance ap)
        {
        this(ap, Color.white, null, radius);
        }

    /** @deprecated */
    public ArrowEdgePortrayal3D(Color labelColor)
        {
        this(null, labelColor, null, DEFAULT_RADIUS);
        }

    public ArrowEdgePortrayal3D(Appearance appearance, Color labelColor)
        {
        this(appearance, labelColor, null, DEFAULT_RADIUS);
        }       

    public ArrowEdgePortrayal3D(Color color, Color labelColor)
        {
        this(appearanceForColor(color), labelColor, null, DEFAULT_RADIUS);
        }       

    /** Assumes that the image is opaque */
    public ArrowEdgePortrayal3D(Image image, Color labelColor)
        {
        this(appearanceForImage(image, true), labelColor, null, DEFAULT_RADIUS);
        }       

    public ArrowEdgePortrayal3D(Appearance appearance, Color labelColor, Font labelFont, double radius)
        {
        super(new Arrow(radius, dummyFrom, dummyTo, null, null, appearance), appearance, labelColor, labelFont);
        }       

    /** the arrow body has 3 (body, top, bottom), arrow head has 2 (bottom and body) */ 
    protected int numShapes()
        {
        return 5;
        }
        
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
        
        if(shapeIndex < coneOffset) //it's the body
            {       
            TransformGroup arrowBody = (TransformGroup)(edgeModelClone.getChild(0));
            Cylinder c = (Cylinder)arrowBody.getChild(0);
            return c.getShape(shapeIndex);
            }
        TransformGroup arrowHead = (TransformGroup)(edgeModelClone.getChild(1));
        Cone c = (Cone)arrowHead.getChild(0);
        return c.getShape(shapeIndex-coneOffset);        
        }
    
    protected void init(Node edgeModel)
        {
        super.init(edgeModel);
        Arrow arrow = (Arrow)edgeModel; 
        arrow.setCapability(Group.ALLOW_CHILDREN_READ);
        //In the future I will want to read the cylinder out of its TransformGroup
        ((TransformGroup)arrow.getChild(0)).setCapability(Group.ALLOW_CHILDREN_READ);
        //In the future I will want to read the cone out of its TransformGroup
        ((TransformGroup)arrow.getChild(1)).setCapability(Group.ALLOW_CHILDREN_READ);
                
        Cylinder body = arrow.getArrowTail();
        PrimitivePortrayal3D.setShape3DFlags(body.getShape(Cylinder.BODY));
        PrimitivePortrayal3D.setShape3DFlags(body.getShape(Cylinder.TOP));
        PrimitivePortrayal3D.setShape3DFlags(body.getShape(Cylinder.BOTTOM));
        Cone head = arrow.getArrowHead();
        PrimitivePortrayal3D.setShape3DFlags(head.getShape(Cone.BODY));
        PrimitivePortrayal3D.setShape3DFlags(head.getShape(Cone.CAP));
        }

    }
