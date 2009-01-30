/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.grid;

import sim.field.grid.*;
import sim.portrayal.*;
import sim.portrayal3d.*;
import sim.portrayal3d.grid.quad.*;
import sim.util.*;

import java.awt.*;
import javax.vecmath.*;
import javax.media.j3d.*;
import com.sun.j3d.utils.picking.*;

/**
 * Displays ValueGrid2Ds values along the XY grid using a surface.  The kind of surface is defined by
 * the underlying Portrayal used for the objects, which must be a subclass of QuadPortrayal.
 * Presently there are two kinds of surfaces: TilePortrayal and MeshPortrayal.  TilePortrayal draws
 * values as square regions on the surface.  MeshPortrayal draws values as the intersections of
 * square regions.  This distinction is important because QuadPortrayals may additionally specify
 * a <b>scale</b> which determines how "high" on the Z dimension the QuadPortrayal is drawn to
 * reflect its underlying value.  Thus MeshPortrayals look like landscapes, and TilePortrayals look
 * like space-age stairs.  QuadPortrayals can also change color to reflect their underlying values,
 * and scale is often 0 by default.
 *
 * <p>ValueGrid2DPortrayal3Ds can also be wrapped with an opaque image which obscures any colors, 
 * and can be set to have some degree of overall transparency (but not the two in combination).
 * Images will be squished to fit just within the field space.
 *
 * <p>ValueGrid2DPortrayal3Ds do <i>not</i> allow you to specify transparency on a per-gridpoint
 * basis unfortunately.  This is due to a lack of this feature in Java3D.  Sorry abut that.  You
 * can change the colors from gridpoint to gridpoint, but you can't change the transparency
 * from gridpoint to gridpoint.
 *
 * <p><b><font color=red>Bug in MeshPortrayal.</font></b>  It appears that if values in MeshPortrayals
 * cause "bends" in the angle of the underlying squares that are too severe (we've seen over 45 degrees), 
 * then when Java3D tries to pick the square you've double-clicked on, the "bent" squares will insist on
 * being included in the pick collection. We believe this to be a bug in Sun's Java3D code.  You'll
 * see this happen when you double-click on a MeshPortrayal and the wrong-coordinate object pops up.
 * You can get around this by calling <tt>setUsingTriangles(true)</tt>, which tells the ValueGrid2DPortrayal3D
 * to draw grids using two triangles per cell rather than a rectangle per cell.  There are only minor
 * disadvantages to using triangles rather than squares: (1) it's very very slightly slower and (2)
 * if the user displays the grids as a wireframe mesh, he'll see the diagonal line dividing the triangles.
 *
 * @author Gabriel Balan
 * 
 */

public class ValueGrid2DPortrayal3D extends FieldPortrayal3D
    {
    public Grid2D field;
    public Image image;
    /** Non-image transparency: 1.0f is fully opaque, 0.0f is fully transparent. */
    public float transparency = 1.0f;
    
    boolean useTriangles = false;
        
    public boolean isUsingTriangles() { return useTriangles; }
    public void setUsingTriangles(boolean val) { useTriangles = val; }
    
    public Object getField()
        {
        return field;
        }

    public String valueName;
    
    public String getValueName() { return valueName; }
    
    /** Sets non-image transparency: 1.0f is fully opaque, 0.0f is fully transparent. */
    public void setTransparency(float transparency)
        {
        this.transparency = transparency;
        }
    
    /** Set the appearance to a fully opaque image.  If image is null, then removes any image. */
    public void setImage(Image image)
        {
        this.image = image;
        }
    
    /** Use a fully opaque image as the appearance. */
    public ValueGrid2DPortrayal3D(String valueName, Image image)
        {
        this(valueName, 1.0f);
        this.image = image;
        }

    /** Be somewhat transparent (1.0 is fully opaque, 0.0f is fully transparent). */
    public ValueGrid2DPortrayal3D(String valueName, float transparency)
        {
        this.valueName = valueName;
        // we make a default portrayal that goes from blue to red when going from 0 to 1,
        // no change in height
        sim.util.gui.SimpleColorMap cm = new sim.util.gui.SimpleColorMap();
        cm.setLevels(0.0,1.0,java.awt.Color.blue,java.awt.Color.red);
        _def = new TilePortrayal(cm);
        this.transparency = transparency;
        }

    /** Be completely opaque */
    public ValueGrid2DPortrayal3D(String valueName)
        {
        this(valueName,1.0f);
        }
        
    public ValueGrid2DPortrayal3D()
        {
        this("Value");
        }

    final PolygonAttributes mPolyAttributes = new PolygonAttributes();
        {
        mPolyAttributes.setCapability(PolygonAttributes.ALLOW_CULL_FACE_WRITE);
        mPolyAttributes.setCapability(PolygonAttributes.ALLOW_MODE_WRITE);
        mPolyAttributes.clearCapabilityIsFrequent(PolygonAttributes.ALLOW_CULL_FACE_WRITE);
        mPolyAttributes.clearCapabilityIsFrequent(PolygonAttributes.ALLOW_MODE_WRITE);
        }  

    public PolygonAttributes polygonAttributes()
        {
        return mPolyAttributes;
        }

    QuadPortrayal _def;
    
    public Portrayal getDefaultPortrayal()
        {
        return _def;
        }

    float[] coords;
    float[] colors;

    boolean resetField = true;
    public void setField(Object grid)
        {
        if(this.field == grid)
            return;
        if (grid instanceof Grid2D) 
            this.field = (Grid2D) grid;
        else throw new RuntimeException("ValueGridPortrayal2D3D cannot portray the object: " + grid);
        tmpGCI = new ValueGridCellInfo(this.field);
        coords = new float[field.getWidth()* field.getHeight()*4*3];    // 3 coordinates: x, y, z
        colors = new float[field.getWidth()* field.getHeight()*4*3];    // 3 color values -- alpha transparency doesn't work here :-(
        resetField = true;
        }
        
    /** tmp Vector3d */ 
    protected Vector3d tmpVect = new Vector3d();
    /** tmp Transform3D 
     * it is reused, since the TGs are copying it internally*/
    protected Transform3D tmpLocalT = new Transform3D();

    /** allocated in portray, and heavily reused in create/update model
     * to avoid "new"s
     */
    private ValueGridCellInfo tmpGCI;


    /**
     * Format is: 
     **/
    public TransformGroup createModel()
        {
        TransformGroup globalTG = new TransformGroup(); 
        globalTG.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        globalTG.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
        globalTG.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);

        if (field == null) return globalTG;
        
        QuadPortrayal quadPortrayal = (QuadPortrayal)getPortrayalForObject(tmpGCI);
                                
        GeometryArray ga;
        if(!useTriangles)
            ga = new QuadArray(4*field.getWidth()*field.getHeight(), 
                QuadArray.COORDINATES | QuadArray.COLOR_3 | // 3 color values -- alpha transparency doesn't work here :-(
                (image != null ? QuadArray.TEXTURE_COORDINATE_2 : 0));
        else
            {
            int[] lengths = new int[field.getWidth()*field.getHeight()];                       
            for(int i=0; i<lengths.length;i++)
                lengths[i]=4;
            ga = new TriangleFanArray(      4*lengths.length, 
                TriangleFanArray.COORDINATES | TriangleFanArray.COLOR_3 | // 3 color values -- alpha transparency doesn't work here :-(
                (image != null ? QuadArray.TEXTURE_COORDINATE_2 : 0),
                lengths);
            }

        ga.setCapability(QuadArray.ALLOW_COLOR_WRITE);
        ga.setCapability(QuadArray.ALLOW_COORDINATE_WRITE);
        SimplePortrayal3D.setPickableFlags(ga);
                
        tmpVect.z = 0;
        int quadIndex = 0;
        final int width = field.getWidth();
        final int height = field.getHeight();
        
        for(int i=0; i<width;i++)
            {           
            tmpGCI.x = i;
                        
            // cell<i,j> is i units to far on x and j unit too far on y.
            //
            tmpVect.x = i;
            for(int j=0; j<height;j++)
                {
                tmpGCI.y = j;
                tmpVect.y = j;
                //                              quadPortrayal.setQuad(tmpGCI, qa,quadIndex);
                quadPortrayal.setData(tmpGCI, coords, colors, quadIndex, width, height);
                quadIndex++;
                }
            }
        ga.setCoordinates(0, coords);
        ga.setColors(0,colors);
                
        Shape3D shape = new Shape3D(ga);
        shape.setCapability(Shape3D.ALLOW_GEOMETRY_READ);

        Appearance appearance;
        if (image!=null)
            {
            appearance = SimplePortrayal3D.appearanceForImage(image,true);
            TexCoordGeneration tex = new TexCoordGeneration();
            Vector4f s = new Vector4f(1f/width,0,0,0);
            tex.setPlaneS(s);
            Vector4f t = new Vector4f(0,1f/height,0,0);
            tex.setPlaneT(t);
            appearance.setTexCoordGeneration(tex);
            }
        else 
            {
            appearance = new Appearance();
            if (transparency < 1.0f )
                {
                appearance.setTransparencyAttributes(
                    new TransparencyAttributes(TransparencyAttributes.BLENDED, 1.0f - transparency));  // duh, alpha's backwards  
                }
            }
        
        appearance.setCapability(Appearance.ALLOW_POLYGON_ATTRIBUTES_WRITE);
        appearance.setPolygonAttributes(mPolyAttributes);
        appearance.setColoringAttributes( new ColoringAttributes(1.0f,1.0f,1.0f,ColoringAttributes.SHADE_GOURAUD));

        shape.setAppearance(appearance);

        LocationWrapper pi = new LocationWrapper(null, null, this);
        shape.setUserData(pi);
        
        BranchGroup bg = new BranchGroup();
        bg.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
        bg.setCapability(BranchGroup.ALLOW_DETACH);
        bg.addChild(shape);
        globalTG.addChild(bg);
        return globalTG;
        }

    public void updateModel(TransformGroup modelTG)
        {
        if (resetField || modelTG.numChildren()==0)  // won't even be considered if we're immutable though
            {
            // need to rebuild the model entirely :-(
            TransformGroup g = createModel();
            if (g.numChildren() > 0)
                {
                Node model = g.getChild(0);
                g.removeChild(0);
                // I've noticed on MacOS X an occasional spurious index error in the
                // following method.  It checks to see how many kids there
                // are, then removes them, but in-between the kids disappear
                // and the underlying ArrayList generates an index out of bounds
                // error. Might be an internal race condition in Apple's code.
                // I'll keep an eye on it -- Sean
                modelTG.removeAllChildren();
                modelTG.addChild(model);
                }
            resetField = false;
            }
        else
            {
            QuadPortrayal quadPortrayal = (QuadPortrayal)getPortrayalForObject(tmpGCI);         
            BranchGroup bg = (BranchGroup)modelTG.getChild(0);  
            Shape3D shape = (Shape3D)bg.getChild(0);
            GeometryArray ga = (GeometryArray)shape.getGeometry();
            int quadIndex = 0;
            final int width = field.getWidth();
            final int height = field.getHeight();
            
            for(int i=0; i< width;i++)
                {               
                tmpGCI.x = i;
                for(int j=0; j< height;j++)
                    {
                    tmpGCI.y = j;
                    //                          quadPortrayal.setQuad(tmpGCI, qa,quadIndex);
                    quadPortrayal.setData(tmpGCI, coords, colors, quadIndex, width, height);
                    quadIndex++;
                    }
                }
            ga.setCoordinates(0, coords);
            ga.setColors(0,colors);
            }
        }
        
    /** This method is called by the default inspector to filter new values set by the user.
        You should return the "corrected" value if the given value is invalid. The default version
        of this method bases values on the values passed into the setLevels() and setColorTable() methods. */
    public double newValue(int x, int y, double value)
        {
        if (field instanceof IntGrid2D) value = (int) value;
        tmpGCI.x = x; tmpGCI.y = y;
        QuadPortrayal quadPortrayal = (QuadPortrayal)getPortrayalForObject(tmpGCI);
        if(quadPortrayal.colorDispenser.validLevel(value))
            return value;

        // at this point we need to reset to current value
        java.awt.Toolkit.getDefaultToolkit().beep();
        if (field != null)
            {
            if (field instanceof DoubleGrid2D)
                return ((DoubleGrid2D)field).field[x][y];
            else return ((IntGrid2D)field).field[x][y];
            }
        else return quadPortrayal.colorDispenser.defaultValue(); // return *something*
        }

    public LocationWrapper completedWrapper(LocationWrapper w, PickIntersection pi, PickResult pr)
        {
        return new LocationWrapper( new ValueGridCellInfo(field), 
            ((QuadPortrayal)getPortrayalForObject(tmpGCI)).getCellForIntersection(pi,field),
            this ) 
            {
            // we keep this around so we don't keep allocating MutableDoubles
            // every time getObject is called -- that's wasteful, but more importantly,
            // it causes the inspector to load its property inspector entirely again,
            // which will cause some flashing...
            MutableDouble val = null;  
                        
            public Object getObject()
                {
                if (val == null) val = new MutableDouble(0);  // create the very first time only
                val.val = ((ValueGridCellInfo)object).value();
                return val;
                }
            public String getLocationName()
                {
                if (location!=null) return ((Int2D)location).toCoordinates();
                return null;
                }
            };
        }
    }
