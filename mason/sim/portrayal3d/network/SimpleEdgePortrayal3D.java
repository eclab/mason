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
  A simple portrayal for edges in a network field.
*/

public class SimpleEdgePortrayal3D extends SimplePortrayal3D
    {
    public Color labelColor; 
    public Font labelFont; 
    public String label; 
    public boolean showLabels; 
    
    public void setShowLabels(boolean val) { showLabels = val; } 
    public boolean getShowLabels() { return showLabels; } 

    //java.text.DecimalFormat strengthFormat = new java.text.DecimalFormat("#0.##");
  
    /** Draws a single-color, undirected black line with no label. */
    public SimpleEdgePortrayal3D()
        {
        this(Color.white, null, true);
        }
    
    /** If fromPaint == toPaint, one single color line will be drawn, and if labelPaint is null, no label is drawn. */
    public SimpleEdgePortrayal3D(Color labelColor, String label, boolean showLabels)
        {
        this(labelColor, new Font("SansSerif", Font.PLAIN, 24), label, showLabels);
        }

    /** If fromPaint == toPaint, one single color line will be drawn, and if labelPaint is null, no label is drawn. */
    public SimpleEdgePortrayal3D(Color labelColor, Font labelFont, String label, boolean showLabels)
        {
        this.labelColor = labelColor;
        this.labelFont = labelFont;
        this.label = label; 
        this.showLabels = showLabels; 
        }
    
    static Transform3D transformForOffset(float x, float y, float z)
        {
        Transform3D offset = new Transform3D();
        offset.setTranslation(new Vector3f(x,y,z));
        return offset;
        }
    
    /** Returns a name appropriate for the edge.  By default, this returns 
        (edge.info == null ? "" : "" + edge.info).
        Override this to make a more customized label to display for the edge on-screen. */
        
    public String getLabel(Edge edge)
        {
        Object obj = edge.info;
        if (obj == null) return "";
        return "" + obj;
        }
    

    static Double3D firstPoint = new Double3D(); 
    static Double3D secondPoint = new Double3D(); 
    static SpatialNetwork3D field; 
    static EdgeWrapper drawInfo = new EdgeWrapper(); 
   
    Transform3D trans = new Transform3D(); 

    static Shape3D shape; 
    static LineArray geo; 

    static double[] startPoint = new double[3]; 
    static double[] endPoint = new double[3]; 
        
    com.sun.j3d.utils.geometry.Text2D tempText; 
    //Text3D tempText; 

    public TransformGroup getModel (Object object, TransformGroup j3dModel) 
        { 

        drawInfo = (EdgeWrapper)object; 
        field = (SpatialNetwork3D)drawInfo.fieldPortrayal.getField();

        secondPoint = field.getObjectLocation(drawInfo.edge.to()); 
        firstPoint = field.getObjectLocation(drawInfo.edge.from()); 
        
        startPoint[0] = firstPoint.x; 
        startPoint[1] = firstPoint.y; 
        startPoint[2] = firstPoint.z; 
        endPoint[0] = secondPoint.x; 
        endPoint[1] = secondPoint.y; 
        endPoint[2] = secondPoint.z; 
        
        if (showLabels) 
            trans = transformForOffset((float)(firstPoint.x + secondPoint.x)/2, 
                                       (float)(firstPoint.y + secondPoint.y)/2, 
                                       (float)(firstPoint.z + secondPoint.z)/2); 
                
        if (j3dModel == null) 
            { 
            // build the whole model from scratch 
            j3dModel = new TransformGroup(); 
            j3dModel.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND); 
        
            LineArray lineGeometry = new LineArray(2, GeometryArray.COORDINATES);
            lineGeometry.setCoordinate(0, startPoint); 
            lineGeometry.setCoordinate(1, endPoint); 
            lineGeometry.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE); 
            
            Shape3D lineShape = new Shape3D(lineGeometry, SimplePortrayal3D.appearanceForColor(Color.red)); 
            lineShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE); 
            setPickableFlags(lineShape); 

            lineShape.setUserData(drawInfo); 

            j3dModel.addChild(lineShape); 

            // draw the edge labels if the user wants
            if (showLabels)  
                { 
                String str = getLabel(drawInfo.edge);
                com.sun.j3d.utils.geometry.Text2D text = new com.sun.j3d.utils.geometry.Text2D(str, new Color3f(labelColor), labelFont.getFamily(), 
                                                                                               labelFont.getSize(), labelFont.getStyle());
                
                text.setRectangleScaleFactor(1.0f/16.0f);
                OrientedShape3D o3d = new OrientedShape3D(text.getGeometry(), text.getAppearance(),
                                                          OrientedShape3D.ROTATE_ABOUT_POINT, new Point3f(0,0,0));
                o3d.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);  // may need to change the appearance (see below)
                o3d.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);  // may need to change the geometry (see below)
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
                
                // the label shouldn't be pickable -- we'll turn this off in the TransformGroup
                clearPickableFlags(o);
                o.addChild(o3d);         // Add label to the offset TransformGroup
                j3dModel.addChild(o);
                                
                                
                tempText = new com.sun.j3d.utils.geometry.Text2D("", new Color3f(labelColor), labelFont.getFamily(), 
                                                                 labelFont.getSize(), labelFont.getStyle());
                                
                //tempText = new Text3D(new Font3D(labelFont, new FontExtrusion()), ""); 
                                
                tempText.setCapability(Appearance.ALLOW_TEXTURE_WRITE); 
                tempText.setCapability(Appearance.ALLOW_TEXTURE_READ); 
                }
            }
        else 
            { 
            shape = (Shape3D)j3dModel.getChild(0);
            geo = (LineArray)shape.getGeometry(); 
            geo.setCoordinate(0, startPoint); 
            geo.setCoordinate(1, endPoint); 
                    
            if (showLabels)
                {
                TransformGroup tg = (TransformGroup)j3dModel.getChild(1); 
                String str = getLabel(drawInfo.edge);
                
                // see if the label has changed? 
                if (!tg.getUserData().equals(str))
                    {  
                    //ugh.  This is really slow.  Using the Shape3D results in huge text, so, the default
                    // value has to be changed in the constructor.  
                                        
                    // make the text again
                    com.sun.j3d.utils.geometry.Text2D text = new com.sun.j3d.utils.geometry.Text2D(str, new Color3f(labelColor), labelFont.getFamily(), 
                                                                                                   labelFont.getSize(), labelFont.getStyle());
                    text.setRectangleScaleFactor(1.0f/16.0f);
                
                    //Shape3D text = new Shape3D(new Text3D(new Font3D(labelFont, new FontExtrusion()), str)); 
                                        
                    // Grab the OrientedShape3D
                    OrientedShape3D o3d = (OrientedShape3D)(tg.getChild(0));
                    
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


    public static class EdgeWrapper extends LocationWrapper
        {
        // we keep this around so we don't keep allocating MutableDoubles
        // every time getObject is called -- that's wasteful, but more importantly,
        // it causes the inspector to load its property inspector entirely again,
        // which will cause some flashing...
        MutableDouble val = null;
                 
        public EdgeWrapper() 
            { 
            this(0,0,0,null);
            }
        
        public EdgeWrapper(int x, int y, int z, FieldPortrayal fieldPortrayal)
            {
            super((Object)null, new Int3D(x,y,z), fieldPortrayal);
            }

        public String toString() 
            { 
            return "" + edge.info;
            }
                
        public String getLocationName()
            {
            SpatialNetwork3D field = (SpatialNetwork3D)fieldPortrayal.getField(); 
            if (field != null && field.network != null)
                {
                // do I still exist in the field?  Check the from() value
                Bag b = field.network.getEdgesOut(edge.from());
                for(int x=0;x<b.numObjs;x++)
                    if (b.objs[x] == edge)
                        return "" + edge.from() + " --> " + edge.to();
                }
            return "Gone.  Was: " + edge.from() + " --> " + edge.to();
            } 
 
        public Object getObject()
            {
            return edge; 
            }
        public Edge edge; 
        }
    }
