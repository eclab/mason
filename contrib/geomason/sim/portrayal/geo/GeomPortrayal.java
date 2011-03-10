package sim.portrayal.geo;

// we can't do a mass import of java.awt.* since java.awt.Polygon and 
// com.vividsolutions.jts.geom.Polygon will conflict
import com.vividsolutions.jts.geom.*; 
import java.awt.Color;  
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.*; 
import java.util.ArrayList;

import sim.portrayal.*; 
import sim.display.*; 
import sim.util.geo.*; 
import sim.portrayal.inspector.*;

/** 
    The portrayal for MasonGeometry objects.  The class draws the JTS geometry object (currently, we can draw Point, LineString, 
    Polygon, MultiLineString and MultiPolygon objects), and sets up the inspectors for the MasonGeometry object.  The inspector is 
    TabbedInspector with at most three tabs: the first tab shows various information about the JTS geometry, the second tab
    shows the associated attribute information, and the third tab shows information about the MasonGeometry userData field, which 
    can be any Java object.    
*/
public class GeomPortrayal extends SimplePortrayal2D  {
        
    private static final long serialVersionUID = 472960663330467429L;

    /** How to paint each object*/ 
    public Paint paint;
        
    /** Scale for each object */ 
    public double scale;
        
    /** Should objects be filled when painting? */ 
    public boolean filled;
        
    /** Default constructor creates filled, gray objects with a scale of 1.0 */ 
    public GeomPortrayal() { this(Color.GRAY, 1.0, true); }
    public GeomPortrayal(Paint paint) { this(paint, 1.0, true); }
    public GeomPortrayal(double scale) { this(Color.GRAY, scale, true); }
    public GeomPortrayal(Paint paint, double scale) { this(paint, scale, true); }
    public GeomPortrayal(Paint paint, boolean filled) { this(paint, 1.0, filled); }
    public GeomPortrayal(double scale, boolean filled) { this(Color.GRAY, scale, filled); }
    public GeomPortrayal(boolean filled) { this(Color.GRAY, 1.0, filled); }
        
    public GeomPortrayal(Paint paint, double scale, boolean filled)
    {
        this.paint = paint;
        this.scale = scale;
        this.filled = filled;
    }
        
    /** Use our custom Inspector. We create a TabbedInspector for each object that allows inspection of 
     * the JTS geometry, attribute information, and the MasonGeometry userData field. 
     */ 
    public Inspector getInspector(LocationWrapper wrapper, GUIState state) 
    {
        if (wrapper ==null) return null; 
        TabbedInspector inspector = new TabbedInspector(); 
        
        // for basic geometry information such as area, perimeter, etc. 
        inspector.addInspector(new SimpleInspector(wrapper.getObject(), state, null), "Geometry"); 

        Object o = wrapper.getObject(); 
        if (o instanceof MasonGeometry) { 
	    MasonGeometry gw = (MasonGeometry)o; 
        	
	    if (gw.geometry.getUserData() instanceof ArrayList<?>) { 
		@SuppressWarnings("unchecked")
		    ArrayList<AttributeField> aList = (ArrayList<AttributeField>)gw.geometry.getUserData();
        		
		boolean showAttrs = false; 
		for (int i=0; i < aList.size(); i++)  { 
		    if (!aList.get(i).hidden) {
			showAttrs = true; 
			break; 
		    }
		}
        				
		if (showAttrs) {  // only add attributes tag if JTS geometry has attributes 
		    GeometryProperties properties = new GeometryProperties(aList);
		    inspector.addInspector(new SimpleInspector(properties, state, null), "Attributes"); 
		}
        	
		if (gw.userData != null) // only add userData inspector if there is actually userdata 
		    inspector.addInspector(new SimpleInspector(gw.userData, state, null), "User Data"); 
	    }
        }
        return inspector; 
    } 
        
    /** Draw a JTS geometry object.  The JTS geometries are converted to Java general path 
     * objects, which are then drawn using the native Graphics2D methods.      
     */ 
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
    {
        MasonGeometry gm = (MasonGeometry)object; 
        Geometry geometry = gm.getGeometry(); 
        if (geometry.isEmpty()) return; 

	if (paint != null) 
            graphics.setPaint(paint); 
                    		
        if (geometry instanceof Point)
            {
            	Point point = (Point)geometry;                  
                double offset = 3 * scale / 2.0; // used to center point
                Ellipse2D.Double ellipse = new Ellipse2D.Double(point.getX() - offset, 
								point.getY() - offset,
                                                                3 * scale, 3 * scale);
                    
           	if (info instanceof GeomInfo2D) { 
	    		GeomInfo2D gInfo = (GeomInfo2D)info; 
	    		GeneralPath path = (GeneralPath)(new GeneralPath(ellipse).createTransformedShape(gInfo.transform));
	    		graphics.fill(path);
		}
		else { 
               		if (filled)
                 	   graphics.fill(ellipse);
                	else
                 	   graphics.draw(ellipse); 
                 }
            }
        else if (geometry instanceof LineString)
            drawGeometry(geometry, graphics, info, false); 
        else if (geometry instanceof Polygon)
	    drawPolygon((Polygon) geometry, graphics, info, filled);
        else if (geometry instanceof MultiLineString) 
            {
		// draw each LineString individually 
                MultiLineString multiLine = (MultiLineString)geometry; 
                for (int i=0; i < multiLine.getNumGeometries(); i++) 
                    drawGeometry(multiLine.getGeometryN(i), graphics, info, false); 
            }
        else if (geometry instanceof MultiPolygon)
            {
		// draw each Polygon individually 
                MultiPolygon multiPolygon = (MultiPolygon) geometry;
                for (int i = 0; i < multiPolygon.getNumGeometries(); i++)
                    drawPolygon((Polygon) multiPolygon.getGeometryN(i), graphics, info, filled);
            }
        else 
            throw new UnsupportedOperationException("Unsupported JTS type for draw()" + geometry);
    }


    /** Helper function for drawing a JTS polygon.  
     * 
     * <p> Polygons have two sets of coordinates; one for the outer ring, and
     optionally another for internal ring coordinates.  Draw the outer
     ring first, and then draw each internal ring, if they exist.
     * */
    void drawPolygon(Polygon polygon, Graphics2D graphics, DrawInfo2D info, boolean fill)
    {
    	drawGeometry(polygon.getExteriorRing(), graphics, info, fill);
    	
        for (int i = 0; i < polygon.getNumInteriorRing(); i++)
            {   // fill for internal rings will always be false as they are literally
                // "holes" in the polygon
                drawGeometry(polygon.getInteriorRingN(i), graphics, info, false);                
            }
    }


    /** Helper function to draw a JTS geometry object.  The coordinates of the JTS geometry are converted 
     * to a native Java GeneralPath which is used to draw the object.    */ 
    void drawGeometry(Geometry geom, Graphics2D graphics, DrawInfo2D info, boolean fill)
    {
        GeneralPath path = new GeneralPath(); 
        Coordinate coords[] = geom.getCoordinates(); 
        path.moveTo((float)coords[0].x, (float)coords[0].y);
                
        for (int i=1; i < coords.length; i++) { 
            path.lineTo((float)coords[i].x, (float)coords[i].y); 
        }
            
        if (info instanceof GeomInfo2D) { 
	    GeomInfo2D gInfo = (GeomInfo2D)info; 
	    path = (GeneralPath) path.createTransformedShape(gInfo.transform); 
	}    
                  
        if (fill) 
            graphics.fill(path); 
        else 
            graphics.draw(path); 
    }
             
    /** Determine if the object was hit or not.   */ 
    public boolean hitObject(Object object, DrawInfo2D range)
    {
        double SLOP=2.0; 
        Geometry geom = (Geometry)object; 
        final Rectangle2D.Double rect = range.clip; 
        Envelope e = new Envelope(rect.x-SLOP, rect.x + rect.width + SLOP, 
                                  rect.y - SLOP, rect.y + rect.height + SLOP); 
        return e.intersects(geom.getEnvelopeInternal());
    }
}
