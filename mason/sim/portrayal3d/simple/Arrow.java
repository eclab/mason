/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.simple;
import com.sun.j3d.utils.geometry.*;

import java.awt.Font;
import javax.media.j3d.*;
import javax.vecmath.*;
/**
 * 
 * @author Gabriel Balan
 * 
 * This file was initially taken from
 * http://www.j3d.org/downloads/Arrow.java
 * 
 * and heavily modified
 */
public class Arrow 
    {
    public static final Color3f defaultArrowColor = new Color3f(0.5f,0.5f,0.5f);

    public static final Font3D  f3d = new Font3D(new Font(null, Font.PLAIN, 1), null);

    /**
     * Creates a 3D arrow between points <code>stPt</code> 
     * and <code>endPt</code>
     * if either label is not null, it adds a Text2D
     * obect at the appropriate end. 
     **/
    public static TransformGroup createArrow(   float ArrowDia, 
                                                Vector3f stPt, 
                                                Vector3f endPt, 
                                                String stLabel, 
                                                String endLabel)
        {
                
        Vector3d v = new Vector3d(stPt);
        v.negate();
        v.add(new Vector3d(endPt));
        // v= start -> end
                
        float ArrowLen = (float) v.length();
        float ArrowHeadLen = 5.0f*ArrowDia;
        float ArrowHeadDia = 2.0f*ArrowDia;
        float CylenderLen = ArrowLen - ArrowHeadLen;
        
        //Rotation Matrix for whole arrow (cylinder + two cones)
        
        //Apperance for the arrow
        Appearance caAppearance = new Appearance();
        ColoringAttributes caColor;
        caColor = new ColoringAttributes();
        caColor.setColor(defaultArrowColor);
        caAppearance.setColoringAttributes(caColor);
                
        Transform3D caTransform = new Transform3D();
        caTransform.setTranslation(stPt);

                
        Vector3d oy = new Vector3d(0,1,0);
                
        Vector3d axis = new Vector3d();
        axis.cross(oy, v);
                
        double sin =  axis.length()/ArrowLen;
        if(sin>1)
            sin =1;
        caTransform.setRotation(new AxisAngle4d(axis, Math.asin(axis.length()/v.length())));
        //axis.length() must be v.length() both doubles or floats, otherwise
        // you might get something bigger than 1.
        caTransform.setTranslation(stPt);                                                                               
        TransformGroup caTransformGroup = new TransformGroup(caTransform);
                
                
        Node cArrowCylinder = new Cylinder(ArrowDia, CylenderLen, caAppearance);
        Transform3D arrowCylinderTransform = new Transform3D();
        arrowCylinderTransform.set(new Vector3f(0,CylenderLen/2,0));
        TransformGroup arrowCylinderTransformGroup = new TransformGroup(arrowCylinderTransform);
        arrowCylinderTransformGroup.addChild(cArrowCylinder);
        caTransformGroup.addChild(arrowCylinderTransformGroup);

                                
        Transform3D arrowHeadTransform = new Transform3D();
        arrowHeadTransform.set(new Vector3f(0,CylenderLen,0));
        TransformGroup arrowHeadTransformGroup = new TransformGroup(arrowHeadTransform);
        Node ArrowHeadCone = new Cone(ArrowHeadDia, ArrowHeadLen, 1, caAppearance);
        arrowHeadTransformGroup.addChild(ArrowHeadCone);
        caTransformGroup.addChild(arrowHeadTransformGroup);

        if(stLabel != null)
            {
            Text3D txt = new Text3D(f3d, stLabel);
            OrientedShape3D os3d = new OrientedShape3D(txt, caAppearance, OrientedShape3D.ROTATE_ABOUT_POINT, new Point3f(0,0,0));
                        
            Transform3D t = new Transform3D();
            t.setScale(5*ArrowDia);
            t.setTranslation(new Vector3f(0, -.1f, 0));
            TransformGroup stLabelTG = new TransformGroup(t);
                        
            stLabelTG.addChild(os3d);
            caTransformGroup.addChild(stLabelTG);
            }

        if(endLabel != null)
            {
            Text3D txt = new Text3D(f3d, endLabel);
            OrientedShape3D os3d = new OrientedShape3D(txt, caAppearance, OrientedShape3D.ROTATE_ABOUT_POINT, new Point3f(0,ArrowLen,0));
                        
            Transform3D t = new Transform3D();
            t.setScale(5*ArrowDia);
            t.setTranslation(new Vector3f(0, ArrowLen + .1f, 0));
            TransformGroup endLabelTG = new TransformGroup(t);
                
            endLabelTG.addChild(os3d);
            caTransformGroup.addChild(endLabelTG);
            }
        return caTransformGroup;
        }
    }
