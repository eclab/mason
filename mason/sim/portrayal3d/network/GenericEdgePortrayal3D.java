/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/
package sim.portrayal3d.network;

import java.awt.*;

import javax.vecmath.*;
import javax.media.j3d.*;

import sim.portrayal3d.network.SimpleEdgePortrayal3D.EdgeWrapper;
import sim.util.Double3D;


/**
 * An abstract portrayal for edges in a network field:
 * you provide the model (Node), I stretch it between every two vertices:
 * the bottom points to the "from" node, the top points to the
 * "to" node. I don't touch the width of it, so feel free to 
 * set it to your heart's desire.
 * 
 * 
 * The edge model is transformed through whatever transformation 
 * maps the unit vector [0,-1,0]->[0,1,0] into [Fx,Fy,Fz]->[Tx,Ty,Tz]
 * ([Fx,Fy,Fz] is the "from" node, [Tx,Ty,Tz] is the "to" node).
 * V = [Fx,Fy,Fz]-[Tx,Ty,Tz] = [Vx,Vy,Vz].
 * 
 * I chose [0,-1,0]->[0,1,0] cause the java3d primitives come
 * by default in the -1,-1,-1->1,1,1 bounding box, and 
 * cilinder and cone are aligned with the Oy axis.
 * 
 * 
 *  This is abstract so people won't forget that 
 * the locationWrapper stored in the model's userData should
 * be stored in ALL geometries of the model. IT'S A 
 * PICKING THING!!!
 * 
 * At the moment I clone the edgeModel I get in the constructor.
 * Would a factory be better? 
 * 
 * @author Gabriel Balan with help from Alexandru Balan and Zoran Duric.
 */

public abstract class GenericEdgePortrayal3D extends SimpleEdgePortrayal3D
    {
    Node edgeModel;
    public GenericEdgePortrayal3D(Node model)
        {
        super();
        edgeModel = model;
        }

    public GenericEdgePortrayal3D(Node model, Color labelColor)
        {
        super((Color)null, (Color)null, labelColor);
        edgeModel = model;
        }

    public GenericEdgePortrayal3D(Node model, Color labelColor, Font labelFont)
        {
        super(((Color)null), ((Color)null), labelColor, labelFont);
        edgeModel = model;
        }

    public TransformGroup getModel(Object object, TransformGroup j3dModel)
        {
        Double3D firstPoint;
        Double3D secondPoint;
        SpatialNetwork3D field;
        EdgeWrapper drawInfo;
        Transform3D trans = null;
        com.sun.j3d.utils.geometry.Text2D tempText;

        drawInfo = (EdgeWrapper) object;
        field = (SpatialNetwork3D) drawInfo.fieldPortrayal.getField();

        secondPoint = field.getObjectLocation(drawInfo.edge.to());
        firstPoint = field.getObjectLocation(drawInfo.edge.from());

        startPoint[0] = firstPoint.x;
        startPoint[1] = firstPoint.y;
        startPoint[2] = firstPoint.z;

        endPoint[0] = secondPoint.x;
        endPoint[1] = secondPoint.y;
        endPoint[2] = secondPoint.z;

        if (showLabels)
            trans = transformForOffset(
                (float) (firstPoint.x + secondPoint.x) / 2,
                (float) (firstPoint.y + secondPoint.y) / 2,
                (float) (firstPoint.z + secondPoint.z) / 2);

        if (j3dModel == null)
            {
            // build the whole model from scratch
            j3dModel = new TransformGroup();
            j3dModel.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);

            TransformGroup tg = new TransformGroup(getTransform(startPoint, endPoint));
            tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
            tg.addChild(edgeModel.cloneTree());

            passWrapperToGeometries(drawInfo);
                        
            j3dModel.addChild(tg);

            // draw the edge labels if the user wants
            if (showLabels)
                {
                String str = getLabel(drawInfo.edge);
                com.sun.j3d.utils.geometry.Text2D text = new com.sun.j3d.utils.geometry.Text2D(
                    str, new Color3f(labelColor), labelFont.getFamily(),
                    labelFont.getSize(), labelFont.getStyle());

                text.setRectangleScaleFactor(1.0f / 16.0f);
                OrientedShape3D o3d = new OrientedShape3D(text.getGeometry(),
                                                          text.getAppearance(),
                                                          OrientedShape3D.ROTATE_ABOUT_POINT,
                                                          new Point3f(0, 0, 0));
                o3d.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE); // may need to change the appearance (see below)
                o3d.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE); // may need to change the geometry (see below)
                o3d.clearCapabilityIsFrequent(Shape3D.ALLOW_APPEARANCE_WRITE);
                o3d.clearCapabilityIsFrequent(Shape3D.ALLOW_GEOMETRY_WRITE);

                // make the offset TransformGroup
                TransformGroup o = new TransformGroup();
                o.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
                o.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
                o.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
                o.clearCapabilityIsFrequent(TransformGroup.ALLOW_CHILDREN_READ);
                o.setTransform(trans);
                o.setUserData(str);

                // the label shouldn't be pickable -- we'll turn this off in the
                // TransformGroup
                clearPickableFlags(o);
                o.addChild(o3d); // Add label to the offset TransformGroup
                j3dModel.addChild(o);

                tempText = new com.sun.j3d.utils.geometry.Text2D("",
                                                                 new Color3f(labelColor), labelFont.getFamily(),
                                                                 labelFont.getSize(), labelFont.getStyle());

                // tempText = new Text3D(new Font3D(labelFont, new
                // FontExtrusion()), "");

                tempText.setCapability(Appearance.ALLOW_TEXTURE_WRITE);
                tempText.setCapability(Appearance.ALLOW_TEXTURE_READ);
                }
            } 
        else
            {
            TransformGroup tg0 = (TransformGroup) j3dModel.getChild(0);
            tg0.setTransform(getTransform(startPoint, endPoint));
                        

            if (showLabels)
                {
                TransformGroup tg = (TransformGroup) j3dModel.getChild(1);
                String str = getLabel(drawInfo.edge);

                // see if the label has changed?
                if (!tg.getUserData().equals(str))
                    {
                    // ugh. This is really slow. Using the Shape3D results in
                    // huge text, so, the default
                    // value has to be changed in the constructor.

                    // make the text again
                    com.sun.j3d.utils.geometry.Text2D text = new com.sun.j3d.utils.geometry.Text2D(
                        str, new Color3f(labelColor),
                        labelFont.getFamily(), labelFont.getSize(),
                        labelFont.getStyle());
                    text.setRectangleScaleFactor(1.0f / 16.0f);

                    // Shape3D text = new Shape3D(new Text3D(new
                    // Font3D(labelFont, new FontExtrusion()), str));

                    // Grab the OrientedShape3D
                    OrientedShape3D o3d = (OrientedShape3D) (tg.getChild(0));

                    // update its geometry and appearance to reflect the new
                    // text.
                    o3d.setGeometry(text.getGeometry());
                    o3d.setAppearance(text.getAppearance());

                    // update user data to reflect the new text
                    tg.setUserData(str);
                    }

                // update the position of the text
                tg.setTransform(trans);
                }
            }

        return j3dModel;
        }
        
    private double[] transformData = new double[16];
        {       
        transformData[12] = 0;
        transformData[13] = 0;
        transformData[14] = 0;
        transformData[15] = 1;
        }
    private Transform3D transform = new Transform3D();

    /*
     * OVERALL-Transf = Translate(F)*Rot*Scale(1,|V|/2,1) * Translate(0,1,0)
     *  //the last (1st actually) op makes the model start from the origin.
     *  //i.e. (0,0,0)->(0,2,0).
     * 
     * Rot = Rotation around axis norm(0,2,0)Xnorm(V) with an angle alpha
     * cos(alpha) = Vy/V, sin(alpha) = Vxz/V, where Vxz = sqrt(Vx^2+Vz^2).
     * 
     * We're using Rodriguez's formula
     * 
     * R = I3 + W*sin(alpha) + W^2 (1-cos(alpha)).
     * 
     * w = rot-axis / norm(rot-axis)
     * 
     * rot axis = norm([0,2, 0]) X norm(V) 
     *                      = [0,1,0]x[Vx/V,Vy/V,Vz/V]
     *                      = [Vz/V, 0, -Vx/V].
     * w = [Vz/Vxz, 0, -Vx/Vxz].
     * 
     *              |0   -w3  w2|   |0                      Vx/Vxz  0               |
     * W =  |w3  0   -w1| = |-Vx/Vxz        0               -Vz/Vxz |
     *              |-w2 w1  0  |   |0                      Vz/Vxz  0               |
     * 
     * 
     *              |-w3^2-w2^2             w1w2                    w1w3            |
     * W^2= |w1w2                   -w3^2-w1^2              w2w3            |
     *              |w1w3                   w2w3                    -w2^2-w1^2      |
     * 
     *              |-Vx^2/Vxz^2    0       -VxVz/Vxz^2     |
     * W^2= |0                              -1      0                       |
     *              |-VxVz/Vxz^2    0       -Vz^2/Vxz^2     |
     * 
     * <br>
     * =======================================================================
     * <br>
     * 
     *                                      |0              Vx/V    0               |
     * W sin(alpha) =       |-Vx/V  0               -Vz/V   |
     *                                      |0              Vz/V    0               |
     * 
     *                                              |-Vx^2(V-Vy)/VVxz^2     0                       -VxVz(V-Vy)/VVxz^2      |
     * W^2(1- cos(alpha)) = |0                                      -(V-Vy)/V       0                                       |
     *                                              |-VxVz(V-Vy)/VVxz^2     0                       -Vz^2(V-Vy)/VVxz^2      |
     * 
     * 
     *              |1 - Vx^2(V-Vy) /[V Vxz^2]      Vx/V                    -VxVz(V-Vy) / [V Vxz^2]         0|
     * R =  |-Vx/V                                          1-(V-Vy)/V              -Vz/V                                           0|      
     *              |- VxVz(V-Vy)   /[V Vxz^2]      Vz/V                    1-Vz^2(V-Vy) / [V Vxz^2]        0|
     *              |0                              0                       0                               0                                                       1|
     * 
     *              |1 - Vx^2(V-Vy) /[V Vxz^2]      Vx/V                    -VxVz(V-Vy) / [V Vxz^2]         0|
     * R =  |-Vx/V                                          Vy/V            -Vz/V                                           0|      
     *              |- VxVz(V-Vy)   /[V Vxz^2]      Vz/V                    1-Vz^2(V-Vy) / [V Vxz^2]        0|
     *              |0                              0                       0                               0                                                       1|

     * 
     *              |1      0       0       0|              |1      0       0       Fx      |               |1      0       0       0|
     * S =  |0      V/2     0       0|      T = |0  1       0       Fy      | T' =  |0      1       0       1|
     *              |0      0       1       0|              |0      0       1       Fz      |               |0      0       1       0|
     *              |0      0       0       1|              |0      0       0       1       |               |0      0       0       1|
     * 
     * 
     *                              |1 - Vx^2(V-Vy) /[V Vxz^2]      Vx/2            -VxVz(V-Vy) / [V Vxz^2]         Fx+Vx/2 |
     * T*R*S*T' =   |-Vx/V                                          Vy/2            -Vz/V                                           Fy+Vy/2 |       
     *                              |- VxVz(V-Vy)   /[V Vxz^2]      Vz/2            1-Vz^2(V-Vy) / [V Vxz^2]        Fz+Vz/2 |
     *                              |0                              0                       0                       0                                                       1               |
     * 
     * 
     */
    protected Transform3D getTransform(double[] from, double[] to)
        {
        final double fx = from[0];
        final double fy = from[1];
        final double fz = from[2];
                
        final double vx = to[0] - fx;
        final double vy = to[1] - fy;
        final double vz = to[2] - fz;
                
        final double vx2 = vx*vx;
        final double vy2 = vy*vy;
        final double vz2 = vz*vz;

        final double vxz2 = vx2+vz2;
        final double v2   = vxz2+vy2;
                
//              final double vxz = Math.sqrt(vxz2);             
        final double v  = Math.sqrt(v2);
                
        double halfVx = transformData[1]  = vx/2;
        double halfVy = transformData[5]  = vy/2;
        double halfVz = transformData[9]  = vz/2;
                
        transformData[3]  = fx +halfVx;
        transformData[7]  = fy + halfVy;
        transformData[11] = fz + halfVz;                

        transformData[4]  = -vx/v;
        transformData[6]  = -vz/v;
                
        final double vxz2v = v*vxz2;
        final double vxvz = vx*vz;
        final double a = (v-vy)/vxz2v;

        transformData[0]  = 1 - vx2*a;
        transformData[10] = 1 - vz2*a;
        transformData[8] = transformData[2] = -vxvz*a;
                
        transform.set(transformData);
        return transform;
        }
        
    protected void passWrapperToGeometries(Object drawInfo)
        {
        /*
         *One should extend <code>edgeModel</code>'s 
         *<code>setUserData</code> method to set the wrapper
         *as userData for all geometries in it.
         */
        edgeModel.setUserData(drawInfo);        
        }
    }
