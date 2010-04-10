/*
 * GeomPortryal.java
 *
 * $Id: GeomPortrayal.java,v 1.3 2010-04-10 18:27:33 kemsulli Exp $
 */

package sim.portrayal.geo;

// we can't do a mass import of java.awt.* since java.awt.Polygon and 
// com.vividsolutions.jts.geom.Polygon will conflict
import com.vividsolutions.jts.geom.*; 
import java.awt.Color;  
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.*; 
import sim.portrayal.*; 
import sim.display.*; 
import sim.util.geo.*; 

/** 
    A simple portrayal for visualizing 2D GeomField geometries.  Currently, we can draw Point, LineString, 
    Polygon, MultiLineString and MultiPolygon objects.  
*/
public class GeomPortrayal extends SimplePortrayal2D {
        
    /** How to paint each object*/ 
    public Paint paint;
        
    /** Scale for each object */ 
    public double scale;
        
    /** Should objects be filled when painting? */ 
    public boolean filled;
        
    /** Default constructor creates filled, gray circles with a scale of 1.0 */ 
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
        
    /** Use our custom Inspector. If wrapper is null, then return null.  Otherwise, return a GeometryInspector.  
        @see GeometryInspector 
    */ 
    public Inspector getInspector(LocationWrapper wrapper, GUIState state) 
    {
        if (wrapper ==null) return null; 
        return new GeometryInspector(wrapper.getObject(), state, "Geometry Properties"); 
    } 
        
    /** Draw a JTS geometry object.    
     */ 
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
    {
        GeomWrapper gm = (GeomWrapper)object; 
        Geometry geometry = gm.fetchGeometry(); 
        if (geometry.isEmpty()) return; 

        if (gm.paint != null)
            graphics.setPaint(gm.paint); 
        else if (paint != null) 
            graphics.setPaint(paint); 
                                                                
        if (geometry instanceof Point)
            {
                Point point = (Point)geometry;                  
                double offset = 3 * scale / 2.0; // used to center point
                Ellipse2D.Double ellipse = new Ellipse2D.Double(point.getX() - offset, point.getY() - offset,
                                                                3 * scale, 3 * scale);
                        
                if (filled)
                    graphics.fill(ellipse);
                else
                    graphics.draw(ellipse);
            }
        else if (geometry instanceof LineString)
            drawGeometry(geometry, graphics, false); 
        else if (geometry instanceof Polygon)
            {
                drawPolygon((Polygon) geometry, graphics, filled);
            }
        else if (geometry instanceof MultiLineString) 
            {
                MultiLineString multiLine = (MultiLineString)geometry; 
                for (int i=0; i < multiLine.getNumGeometries(); i++) 
                    drawGeometry(multiLine.getGeometryN(i), graphics, false); 
            }
        else if (geometry instanceof MultiPolygon)
            {
                MultiPolygon multiPolygon = (MultiPolygon) geometry;
                        
                for (int i = 0; i < multiPolygon.getNumGeometries(); i++)
                    drawPolygon((Polygon) multiPolygon.getGeometryN(i), graphics, filled);
            }
        else { 
            System.out.println("Geometry is unsupported: " + geometry); 
            throw new UnsupportedOperationException("Unsupported JTS type for draw()");
        }
    }


    /** Helper function for drawing a JTS polygon
     *
     * @param polygon
     * @param graphics
     * @param fill
     */
    private void drawPolygon(Polygon polygon, Graphics2D graphics, boolean fill)
    {
        // Polygons have two sets of coordinates; one for the outer ring, and
        // optionally another for internal ring coordinates.  Draw the outer
        // ring first, and then draw each internal ring, if they exist.

        drawGeometry(polygon.getExteriorRing(), graphics, fill);

        for (int i = 0; i < polygon.getNumInteriorRing(); i++)
            {   // fill for internal rings will always be false as they are literally
                // "holes" in the polygon
                drawGeometry(polygon.getInteriorRingN(i), graphics, false);
            }
    }


    /** Helper function to draw a JTS geometry object.  Uses the native Java GeneralPath to 
        draw the object.  */ 
    private void drawGeometry(Geometry geom, Graphics2D graphics, boolean fill)
    {
        GeneralPath path = new GeneralPath(); 
        Coordinate coords[] = geom.getCoordinates(); 
        path.moveTo((float)coords[0].x, (float)coords[0].y);
                
                
        for (int i=1; i < coords.length; i++) { 
            path.lineTo((float)coords[i].x, (float)coords[i].y); 
        }
                                
        if (fill) 
            graphics.fill(path); 
        else 
            graphics.draw(path); 
    }
        
    /** Used to create new geometries for hit testing.  */ 
    GeometryFactory geomFactory = new GeometryFactory(); 
        
    /** Determine if the object was hit or not.   */ 
    public boolean hitObject(Object object, DrawInfo2D range)
    {
        double SLOP=2.0; 
        Geometry geom = (Geometry)object; 
        final Rectangle2D.Double rect = range.clip; 
        Envelope e = new Envelope(rect.x-SLOP, rect.x + rect.width + SLOP, 
                                  rect.y - SLOP, rect.y + rect.height + SLOP); 
        Geometry g = geomFactory.toGeometry(e);
        return geom.intersects(g); 
    }
}
