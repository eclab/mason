/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.simple; 
import sim.display.*;
import sim.field.grid.*;
import sim.portrayal3d.*;
import sim.portrayal3d.grid.*;
import javax.media.j3d.*;
import sim.portrayal.*;
import sim.util.*;

public class ValuePortrayal3D extends SimplePortrayal3D
    {
    public static int SHAPE_CUBE = 0;
    public static int SHAPE_SQUARE = 1;
    
    public int shape;
    public ValuePortrayal3D()
        {
        super();
        shape = SHAPE_CUBE;
        }

    public ValuePortrayal3D(int shape)
        {
        super();
        this.shape = shape;
        }
        
    static final float[] verts_cube = {
        // front face
        0.5f, -0.5f,  0.5f,                             0.5f,  0.5f,  0.5f,
        -0.5f,  0.5f,  0.5f,                            -0.5f, -0.5f,  0.5f,
        // back face
        -0.5f, -0.5f, -0.5f,                            -0.5f,  0.5f, -0.5f,
        0.5f,  0.5f, -0.5f,                             0.5f, -0.5f, -0.5f,
        // right face
        0.5f, -0.5f, -0.5f,                             0.5f,  0.5f, -0.5f,
        0.5f,  0.5f,  0.5f,                             0.5f, -0.5f,  0.5f,
        // left face
        -0.5f, -0.5f,  0.5f,                            -0.5f,  0.5f,  0.5f,
        -0.5f,  0.5f, -0.5f,                            -0.5f, -0.5f, -0.5f,
        // top face
        0.5f,  0.5f,  0.5f,                             0.5f,  0.5f, -0.5f,
        -0.5f,  0.5f, -0.5f,                            -0.5f,  0.5f,  0.5f,
        // bottom face
        -0.5f, -0.5f,  0.5f,                            -0.5f, -0.5f, -0.5f,
        0.5f, -0.5f, -0.5f,                             0.5f, -0.5f,  0.5f,
        };
   
    static final float[] verts_square = {
        0.5f, -0.5f,  0.0f,                             0.5f,  0.5f,  0.0f,
        -0.5f,  0.5f,  0.0f,                            -0.5f, -0.5f,  0.0f
        };

    public boolean usesTriangles=false; 
    public boolean getUsesTriangles() { return usesTriangles; } 
    public void setUsesTriangles(boolean val) { usesTriangles = val; }

    public sim.util.gui.ColorMap map;
    float[] tempColorComponents = new float[4];
        
    public void setParentPortrayal(FieldPortrayal3D p)
        {
        super.setParentPortrayal(p);
        map = ((ValueGridPortrayal3D)p).map;
        }

    final PolygonAttributes mPolyAttributes = new PolygonAttributes();
        {
        mPolyAttributes.setCapability(PolygonAttributes.ALLOW_MODE_WRITE);
        mPolyAttributes.clearCapabilityIsFrequent(PolygonAttributes.ALLOW_MODE_WRITE);
        if (shape == SHAPE_SQUARE) 
            {
            mPolyAttributes.setCapability(PolygonAttributes.ALLOW_CULL_FACE_WRITE);
            mPolyAttributes.clearCapabilityIsFrequent(PolygonAttributes.ALLOW_CULL_FACE_WRITE);
            }
        else 
            mPolyAttributes.setCullFace(PolygonAttributes.CULL_BACK);
        }

    public PolygonAttributes polygonAttributes()
        {
        return mPolyAttributes;
        }
        
    // set up the geometry array
    GeometryArray ga; 

    /** Builds a model, but obj is expected to be a ValuePortrayal3D.ValueWrapper. */
    public TransformGroup getModel(Object obj, TransformGroup j3dModel) 
        {
        // extract color to use
        double val = ((ValueWrapper)obj).lastVal; 
        int color = map.getRGB(val);
        float[] c = tempColorComponents;
        c[0] = ((float) (color & 255)) / 255;
        color >>>= 8;
        c[1] = ((float) (color & 255)) / 255;
        color >>>= 8;
        c[2] = ((float) (color & 255)) / 255;
        color >>>= 8;
        c[3] = ((float) (color & 255)) / 255;

        // build model if necessary
        if(j3dModel==null) 
            {
            j3dModel = new TransformGroup();
            j3dModel.setCapability(Group.ALLOW_CHILDREN_READ);

            if (ga==null)
                {
                float[] verts = null;
                if (shape == SHAPE_CUBE)
                    verts = verts_cube;
                else verts = verts_square;
                
                if (usesTriangles) 
                    { 
                    int[] lengths = new int[verts.length/12];
                    for(int i=0; i<lengths.length;i++)
                        lengths[i]=4;
                    ga = new TriangleFanArray(4*lengths.length, TriangleFanArray.COORDINATES, lengths);
                    }
                else 
                    {
                    ga = new QuadArray(verts.length/3, QuadArray.COORDINATES ); 
                    }
                ga.setCoordinates(0, verts);
                }

            // set up an appearance that can be modified
            Appearance appearance = new Appearance();
            appearance.setCapability(Appearance.ALLOW_POLYGON_ATTRIBUTES_WRITE);  
            appearance.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_READ); 
            appearance.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
            appearance.setPolygonAttributes(polygonAttributes());
            ColoringAttributes ca = new ColoringAttributes(c[0], c[1], c[2], ColoringAttributes.SHADE_FLAT);
            ca.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
            appearance.setColoringAttributes(ca);
            TransparencyAttributes ta = new TransparencyAttributes(TransparencyAttributes.BLENDED, 1.0f - c[3]);  // duh, alpha's backwards
            ta.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
            appearance.setTransparencyAttributes(ta);
                
            // construct the shape
            Shape3D localShape = new Shape3D(ga, appearance);
            localShape.setCapability(Shape3D.ALLOW_APPEARANCE_READ); 
            setPickableFlags(localShape); 
                        
            // obj is our ValueWrapper, which is a LocationWrapper already
            localShape.setUserData(obj);
                        
            j3dModel.addChild(localShape); 
            }
        else            // just update color and transparency
            {
            Shape3D shape = (Shape3D)(j3dModel.getChild(0));
            Appearance appearance = shape.getAppearance();
            appearance.getColoringAttributes().setColor(c[0],c[1],c[2]);
            appearance.getTransparencyAttributes().setTransparency(1.0f - c[3]);  // duh, alpha's backwards
            }
        return j3dModel;
        }


    /* This special LocationWrapper contains a public double holding the last value used
       to display the object. */
    public static class ValueWrapper extends LocationWrapper
        {
        // we keep this around so we don't keep allocating MutableDoubles
        // every time getObject is called -- that's wasteful, but more importantly,
        // it causes the inspector to load its property inspector entirely again,
        // which will cause some flashing...
        MutableDouble val = null;  
                        
        public ValueWrapper(double lastVal, int x, int y, int z, FieldPortrayal fieldPortrayal)
            {
            super((Object)null, new Int3D(x,y,z), fieldPortrayal);
            this.lastVal = lastVal;
            }

        public String getLocationName()
            {
            Int3D loc = (Int3D) location;
            Object field = fieldPortrayal.getField();
            if (field instanceof DoubleGrid3D || field instanceof IntGrid3D)
                return loc.toCoordinates();
            else return (new Int2D(loc.x,loc.y)).toCoordinates();
            }

        public Object getObject()
            {
            Object field = fieldPortrayal.getField();
            Int3D loc = (Int3D)location;
            if (val==null) val = new MutableDouble(0);

            if (field instanceof DoubleGrid3D)
                val.val = ((DoubleGrid3D)field).field[loc.x][loc.y][loc.z];
            else if (field instanceof IntGrid3D)
                val.val = ((IntGrid3D)field).field[loc.x][loc.y][loc.z];
            else if (field instanceof DoubleGrid2D)
                val.val = ((DoubleGrid2D)field).field[loc.x][loc.y];
            else // if (field instanceof IntGrid2D)
                val.val = ((IntGrid2D)field).field[loc.x][loc.y];
            
            return val;
            }

        public double lastVal;
        }

    public static abstract class Filter
        {
        int x;
        int y;
        int z; 
            
        ValueGridPortrayal3D fieldPortrayal;
        Grid3D grid; 
        public Filter(LocationWrapper wrapper)
            {
            fieldPortrayal = (ValueGridPortrayal3D)(wrapper.getFieldPortrayal());
            grid = (Grid3D)fieldPortrayal.getField(); 
            Int3D loc = (Int3D)(wrapper.getLocation());
            x = loc.x;
            y = loc.y;
            z = loc.z; 
            }
        }

    // the only reason for these two subclasses is that they differ in the data
    // type of their property (double vs int).  This allows us to guarantee that
    // ints are displayed or set as opposed to doubles in the Inspector.  No
    // big whoop -- it's more a formatting thing than anything else.
    
    public static class DoubleFilter extends Filter
        {
        public DoubleFilter(LocationWrapper wrapper) { super(wrapper); }
        
        public double getValue() { 
            if (grid instanceof DoubleGrid3D)
                return ((DoubleGrid3D)grid).field[x][y][z];
            else // if (field instanceof DoubleGrid2D)
                return ((DoubleGrid2D)grid).field[x][y];
        }

        public void setValue(double val) { 
            if (grid instanceof DoubleGrid3D)
                ((DoubleGrid3D)grid).field[x][y][z] = val;
            else //if (field instanceof DoubleGrid2D)
                ((DoubleGrid2D)grid).field[x][y] = val;
        }
        // static inner classes don't need serialVersionUIDs
        }
        

    public static class IntFilter extends Filter
        {
        public IntFilter(LocationWrapper wrapper) { super(wrapper); }
        
        public int getValue() { 
            if (grid instanceof IntGrid3D)
                return ((IntGrid3D)grid).field[x][y][z];
            else //if (field instanceof IntGrid2D)
                return ((IntGrid2D)grid).field[x][y];
        }

        public void setValue(int val) { 
            if (grid instanceof IntGrid3D)
                ((IntGrid3D)grid).field[x][y][z] = val;
            else //if (field instanceof IntGrid2D)
                ((IntGrid2D)grid).field[x][y] = val;
        }
        }

    public Inspector getInspector(LocationWrapper wrapper, GUIState state)
        {
        if (((ValueGridPortrayal3D)(wrapper.getFieldPortrayal())).getField() instanceof DoubleGrid3D)
            return new SimpleInspector(new DoubleFilter(wrapper), state, "Properties");
        else
            return new SimpleInspector(new IntFilter(wrapper) ,state, "Properties");
        // static inner classes don't need serialVersionUIDs
        }
    
    public String getName(LocationWrapper wrapper)
        {
        ValueGridPortrayal3D portrayal = (ValueGridPortrayal3D)(wrapper.getFieldPortrayal());
        return portrayal.getValueName() + " at " + wrapper.getLocationName();
        }
    }
