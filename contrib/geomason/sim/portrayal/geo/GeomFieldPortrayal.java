package sim.portrayal.geo;

import com.vividsolutions.jts.geom.*;

import java.awt.Graphics2D;
import java.awt.geom.*;
import sim.field.geo.GeomVectorField;
import sim.portrayal.*;
import sim.util.*; 
import sim.util.geo.*; 
import java.awt.image.*;

/** 
    Portrayal for MasonGeometry objects.  The portrayal handles drawing and hit-testing (for inspectors).    
 
*/
public class GeomFieldPortrayal extends FieldPortrayal2D {
        
    private static final long serialVersionUID = 8409421628913847667L;
	
    /** The underlying portrayal */ 
    GeomPortrayal defaultPortrayal = new GeomPortrayal();
        
    /** Default constructor */     
    public GeomFieldPortrayal()
    {
        super(); 
        setImmutableField(false);
    }

    /** Constructor which sets the field's immutable flag */     
    public GeomFieldPortrayal(boolean immutableField)
    {
        super(); 
        setImmutableField(immutableField);
    }
        
    /** Return the underlying portrayal */ 
    public Portrayal getDefaultPortrayal() { return defaultPortrayal; }

    /** Caches immutable fields.  */
    BufferedImage buffer = null;
        
    /** Handles hit-testing and drawing of the underlying geometry objects.  */ 
    protected void hitOrDraw(Graphics2D graphics, DrawInfo2D info, Bag putInHere)
    {
        if (field == null) return; 
				
        // If we're drawing (and not inspecting), re-fresh the buffer if the
        // associated field is immutable.
        if (graphics != null && immutableField && !info.precise) {
        	
        	GeomVectorField geomField = (GeomVectorField)field;
        	double x = info.clip.x; 
        	double y = info.clip.y; 
        	boolean dirty = false;
        	
        	// make a new buffer? or did the user change the zoom? 
        	if (buffer == null || buffer.getWidth() != info.clip.width || buffer.getHeight() != info.clip.height)
        		{
        		buffer = new BufferedImage((int)info.clip.width,(int)info.clip.height,BufferedImage.TYPE_INT_ARGB);   
        		dirty = true;
        		}
        	
        	// handles the case for scrolling
            if (geomField.drawX != x || geomField.drawY != y)
            	{
            	dirty = true;
            	}

            // save the origin of the drawn region for later
        	geomField.drawX = x; 
        	geomField.drawY = y;        	

        	// re-draw into the buffer 
            if (dirty)
            { 
            	clearBufferedImage(buffer); 
                Graphics2D newGraphics = (Graphics2D)buffer.getGraphics();
                hitOrDraw2(newGraphics, new DrawInfo2D(info, -x, -y), putInHere); 
                newGraphics.dispose(); 
            }
            
            // draw buffer on screen 
            graphics.drawImage(buffer, (int)x, (int)y, null); 
        }
        else  {  // do regular MASON-style drawing
        	buffer = null; 
            hitOrDraw2(graphics, info, putInHere);
        }
    }
   
    /** Clears the BufferedImage by setting all the pixels to RGB(0,0,0,0) */ 
    void clearBufferedImage(BufferedImage image)
    {
    	int len = image.getHeight()*image.getWidth();
    	WritableRaster raster = image.getRaster();
    	int[] data = new int[len];
    	for (int i=0; i < len; i++)
    		data[i] = 0; 
    	raster.setDataElements(0, 0, image.getWidth(), image.getHeight(), data);
    }
    
    /** 
     *  Helper function which performs the actual hit-testing and drawing for both 
     *  immutable fields and non-immutable fields.  
     *  
     *  <p> The objects in the field can either use GeomPortrayal or any SimplePortrayal2D for drawing.  
     * 
     */
     void hitOrDraw2(Graphics2D graphics, DrawInfo2D info, Bag putInHere)
    {
        GeomVectorField geomField = (GeomVectorField)field;
		if (geomField == null) return ;
        AffineTransform savedTransform=null;
                
        // compute the transform between world and screen coordinates, and 
        // also construct a geom.util.AffineTransform for use in hit-testing later
        Envelope MBR = geomField.getMBR();
        AffineTransform worldToScreen = worldToScreenTransform(MBR, info);
        double m[] = new double[6];
        worldToScreen.getMatrix(m); 
        com.vividsolutions.jts.geom.util.AffineTransformation a = new com.vividsolutions.jts.geom.util.AffineTransformation(m[0], m[2], m[4], m[1], m[3], m[5]);
                                
        if (graphics != null) { 
            savedTransform = graphics.getTransform(); // save transform
            graphics.transform(worldToScreen); // using setTransform instead causes problems with SWING!  
        }
               
		// code taken from GeoTools and hacked on
		AffineTransform screenToWorld = null;
		try {
			screenToWorld = worldToScreen.createInverse(); 
		} catch (Exception e) {
			System.out.println(e); 
			System.exit(-1); 
		}

		Point2D p1 = new Point2D.Double();
		Point2D p2 = new Point2D.Double();
		screenToWorld.transform(new Point2D.Double(info.clip.x, info.clip.y), p1);
		screenToWorld.transform(new Point2D.Double(info.clip.x + info.clip.width, info.clip.y + info.clip.height), p2);
		
		Envelope clipEnvelope = new Envelope(p1.getX(), p2.getX(), p1.getY(), p2.getY());
		
		// get all the geometries that *might* be visible in the current clip 
		Bag geometries = geomField.queryField(clipEnvelope);
		
        for (int i = 0; i < geometries.numObjs; i++)
            {
                MasonGeometry gm = (MasonGeometry)geometries.objs[i];
                Geometry geom = gm.getGeometry();
                Portrayal p = getPortrayalForObject(gm);
                if (!(p instanceof SimplePortrayal2D))
                    throw new RuntimeException("Unexpected Portrayal " + p + " for object " + geom + 
                                               " -- expected a SimplePortrayal2D or a GeomPortrayal");
                                        
                SimplePortrayal2D portrayal = (SimplePortrayal2D) p ; 
                        
                if (graphics == null) {
                    Geometry g = (Geometry)(geom.clone()); 
                    g.apply(a); 
                    g.geometryChanged();
                    if (portrayal.hitObject(g, info))
                        putInHere.add(new LocationWrapper(gm, geomField.getGeometryLocation(geom), this));
                }
                else { 
                       if (portrayal instanceof GeomPortrayal) 
							portrayal.draw(gm, graphics, info); 
						else  {         // have a SimplePortrayal2D, so move info.draw to the centroid of the geometry
							Point pt = geom.getCentroid(); 
							info.draw.x = pt.getX(); 
							info.draw.y = pt.getY(); 
							portrayal.draw(geom, graphics, info);
						}
                }  
            }
                
        if (graphics != null) 
            graphics.setTransform(savedTransform); // restore y axis & origin
    }
        
    /** Sets the underlying field, after ensuring its a GeomVectorField. */
    public void setField(Object field)
    {
        dirtyField = true;
        if (field instanceof GeomVectorField)this.field = field;
        else 
            throw new RuntimeException("Invalid field for GeomFieldPortrayal: " + field);
    }
        
        
    /** Determines the affine transform which converts world coordinates into screen 
     * coordinates.  Modified from GeoTools RenderUtilities.java. 
     */
     AffineTransform worldToScreenTransform(Envelope mapExtent, DrawInfo2D info) {
        double scaleX = info.draw.width / mapExtent.getWidth();
        double scaleY = info.draw.height / mapExtent.getHeight();
                
        double tx = -mapExtent.getMinX() * scaleX;
        double ty = (mapExtent.getMinY() * scaleY) + info.draw.height;
                
        AffineTransform at = new AffineTransform(scaleX, 0.0d, 0.0d, -scaleY, tx, ty);
        AffineTransform originTranslation = AffineTransform.getTranslateInstance(info.draw.x, info.draw.y);
        originTranslation.concatenate(at);
                
        return originTranslation != null ? originTranslation : at;
    }       
}
