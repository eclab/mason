/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.network;

import sim.portrayal3d.*;
import sim.portrayal.*;
import sim.util.*;
import java.awt.*;

import sim.field.network.*;
import javax.vecmath.*;
import javax.media.j3d.*;

/*
 * A simple portrayal for edges in a network field.
 */

public class SimpleEdgePortrayal3D extends SimplePortrayal3D
    {
    Color fromColor;
    Color toColor;
    Color labelColor;
    Font labelFont;
    //Font3D labelFont3D;     // only used if we're doing Text3D
    boolean showLabels;
        
    // A larger font size makes the label bigger but also uses much more memory
    static final int FONT_SIZE = 18;
    // A smaller scaling factor reduces the label size
    static final double SCALING_MODIFIER = 1.0 / 5.0; 
        
    double labelScale = 1.0;
    public double getLabelScale() { return labelScale; }
    public void setLabelScale(double s) { labelScale = Math.abs(s); }
        
    /** @deprecated */
    public void setShowLabels(boolean val) { showLabels = val; }

    /** @deprecated */
    public boolean getShowLabels() { return showLabels; }

    public SimpleEdgePortrayal3D()
        {
        this(Color.gray, Color.gray, Color.white, null);
        }

    public SimpleEdgePortrayal3D(Color edgeColor, Color labelColor)
        {
        this(edgeColor, edgeColor, labelColor, null);
        }

    public SimpleEdgePortrayal3D(Color edgeColor, Color labelColor, Font labelFont)
        {
        this(edgeColor, edgeColor, labelColor, labelFont);
        }

    public SimpleEdgePortrayal3D(Color fromColor, Color toColor, Color labelColor)
        {
        this(fromColor, toColor, labelColor, null);
        }

    /**
     * If fromColor == toColor, one single color line will be drawn, and if
     * labelColor is null, no label is drawn.
     */
    public SimpleEdgePortrayal3D(Color fromColor, Color toColor, Color labelColor, Font labelFont)
        {
        this.fromColor = fromColor;
        this.toColor = toColor;
        this.labelColor = labelColor;
        if (labelFont == null) 
            labelFont = new Font("SansSerif", Font.PLAIN, FONT_SIZE);
        this.labelFont = labelFont;
        //labelFont3D = new Font3D(labelFont, new FontExtrusion());
        showLabels = (labelColor != null);
        if (this.labelColor == null) 
            this.labelColor = Color.white;  // just in case the user turns on labels again
        }

    Transform3D transformForOffset(double x, double y, double z)
        {
        Transform3D offset = new Transform3D();
        offset.setTranslation(new Vector3d(x, y, z));
        return offset;
        }

    /**
     * Returns a name appropriate for the edge. By default, this returns
     * (edge.info == null ? "" : "" + edge.info). Override this to make a more
     * customized label to display for the edge on-screen.
     */

    public String getLabel(Edge edge)
        {
        Object obj = edge.info;
        if (obj == null)
            return "";
        return "" + obj;
        }


    double[] startPoint = new double[3];
    double[] endPoint = new double[3];
    double[] middlePoint = new double[3];

    public TransformGroup getModel(Object object, TransformGroup j3dModel)
        {
        Double3D firstPoint;
        Double3D secondPoint;
        SpatialNetwork3D field;
        LocationWrapper wrapper;
        Transform3D trans = null;
        
        wrapper = (LocationWrapper) object;
        Edge edge = (Edge)(wrapper.getLocation());
        field = (SpatialNetwork3D) wrapper.fieldPortrayal.getField();

        secondPoint = field.getObjectLocation(edge.to());
        firstPoint = field.getObjectLocation(edge.from());
        
        startPoint[0] = firstPoint.x;
        startPoint[1] = firstPoint.y;
        startPoint[2] = firstPoint.z;
        endPoint[0] = secondPoint.x;
        endPoint[1] = secondPoint.y;
        endPoint[2] = secondPoint.z;

        middlePoint[0] = (secondPoint.x + firstPoint.x) / 2;
        middlePoint[1] = (secondPoint.y + firstPoint.y) / 2;
        middlePoint[2] = (secondPoint.z + firstPoint.z) / 2;
        if (showLabels)
            trans = transformForOffset(middlePoint[0], middlePoint[1], middlePoint[2]);

        if (j3dModel == null)
            {
            // build the whole model from scratch
            j3dModel = new TransformGroup();
            j3dModel.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);

            LineArray lineGeometry1 = new LineArray(2, GeometryArray.COORDINATES);
            lineGeometry1.setCoordinate(0, startPoint); 
            lineGeometry1.setCoordinate(1, middlePoint);
            lineGeometry1.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE); 
            Shape3D lineShape1 = new Shape3D(lineGeometry1, SimplePortrayal3D.appearanceForColor(fromColor)); 
            lineShape1.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE); 
            setPickableFlags(lineShape1); 
            lineShape1.setUserData(wrapper); 
            j3dModel.addChild(lineShape1);

            LineArray lineGeometry2 = new LineArray(2, GeometryArray.COORDINATES);
            lineGeometry2.setCoordinate(0, middlePoint); 
            lineGeometry2.setCoordinate(1, endPoint);
            lineGeometry2.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE); 
            Shape3D lineShape2 = new Shape3D(lineGeometry2, SimplePortrayal3D.appearanceForColor(toColor)); 
            lineShape2.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE); 
            setPickableFlags(lineShape2); 
            lineShape2.setUserData(wrapper); 
            j3dModel.addChild(lineShape2);


            // draw the edge labels if the user wants
            if (showLabels)
                {
                String str = getLabel(edge);
                com.sun.j3d.utils.geometry.Text2D text = new com.sun.j3d.utils.geometry.Text2D(
                    str, new Color3f(labelColor), labelFont.getFamily(),
                    labelFont.getSize(), labelFont.getStyle());
                text.setRectangleScaleFactor((float)(labelScale * SCALING_MODIFIER));

                //text = new Shape3D(new Text3D(labelFont3D, ""));
                                
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
                }
            } 
        else
            {
            Shape3D shape = (Shape3D)j3dModel.getChild(0);
            LineArray geo = (LineArray)shape.getGeometry(); 
            geo.setCoordinate(0, startPoint); 
            geo.setCoordinate(1, middlePoint);

            shape = (Shape3D)j3dModel.getChild(1);
            geo = (LineArray)shape.getGeometry(); 
            geo.setCoordinate(0, startPoint); 
            geo.setCoordinate(1, endPoint);

            if (showLabels)
                {
                TransformGroup tg = (TransformGroup) j3dModel.getChild(2);
                String str = getLabel(edge);

                // see if the label has changed?
                if (!tg.getUserData().equals(str))
                    {
                    // make the text again
                    com.sun.j3d.utils.geometry.Text2D text = new com.sun.j3d.utils.geometry.Text2D(
                        str, new Color3f(labelColor),
                        labelFont.getFamily(), labelFont.getSize(),
                        labelFont.getStyle());
                    text.setRectangleScaleFactor((float)(labelScale * SCALING_MODIFIER));

                    //Shape3D text = new Shape3D(new Text3D(labelFont3D, str));
                                        
                    // Grab the OrientedShape3D
                    OrientedShape3D o3d = (OrientedShape3D) (tg.getChild(0));

                    // update its geometry and appearance to reflect the new text.
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

    public String getName(LocationWrapper wrapper)
        {
        // indicate it's an edge
        return "Edge: " + super.getName(wrapper);
        }
    }
