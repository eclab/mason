package sim.util.geo; 

import sim.io.geo.*; 
import com.vividsolutions.jts.geom.*; 
import java.awt.Paint;

/** 
    GeomWrapper is a wrapper class around a JTS geometry object.  In addition to the JTS geometry, GeomWrapper also
    holds a corresponding GeometryInfo object which contains information used by the Inspector.  
 
    @see sim.io.geo.GeometryInfo
*/

public class GeomWrapper implements sim.util.Valuable {

    /** Internal JTS geometry object */ 
    public Geometry geometry; 
        
    /** Holds the underlying metadata information. */ 
    public GeometryInfo geoInfo; 
        
    /** Paint object to draw this GeomWrapper */
    public Paint paint; 
        
    /** Default constructor */ 
    public GeomWrapper() { this(null, null, null); }
    public GeomWrapper(Geometry g) { this(g, null, null); }
    public GeomWrapper(Geometry g, GeometryInfo gi) { this(g, gi, null); }

    /// copy ctor
    public GeomWrapper(GeomWrapper gw)
    {
        // XXX may want to consider assigning via cloning instead
        geometry = gw.geometry;
        geoInfo = gw.geoInfo;
        paint = gw.paint;
    }
        
    /** Sets the internal JTS geometry, GeometryInfo and Paint fields */ 
    public GeomWrapper(Geometry g, GeometryInfo gi, Paint p)
    {
        geometry = g; 
        geoInfo = gi; 
        paint = p; 
    }

    public String toString()
    {
        return geometry.toString();
    }
        
    /** Returns the Paint object.  We use fetch rather than get to avoid problems with the Inspectors. */
    public Paint fetchPaint() { return paint; } 
        
    /** Returns the JTS geometry objects. We use fetch rather than get to avoid problems with the Inspectors. */
    public Geometry fetchGeometry() { return geometry; }
        
    /** Returns the internal GeometryInfo object. We use fetch rather than get to avoid problems with the Inspectors. */
    public GeometryInfo fetchGeometryInfo() { return geoInfo; } 
        
    /** Returns the type of the internal JTS geometry object */ 
    public String getType() { return geometry.getGeometryType(); }
        
    /** Retusn the area of the intenal JTS geometry object.  
        The units are the same as same as the internal JTS geometry object */
    public double getArea() { return geometry.getArea(); } 
        
    /** Returns the length of the perimeter of the internal JTS geometry object. 
        The units are the same as same as the internal JTS geometry object */
    public double getPerimeter() { return geometry.getLength(); } 
        
    /** Returns the centroid of the internal JTS geometry object. The units are the same as same as the internal JTS geometry object */
    public Point getCentroid() { return geometry.getCentroid(); }
        
    /** The returned value is used to handle per region coloring. */ 
    public double doubleValue() { return 1; } 
        
}
