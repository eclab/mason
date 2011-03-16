package sim.util.geo; 

import java.awt.geom.*;

import com.vividsolutions.jts.geom.*; 
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

/** 
   A MasonGeometry is a wrapper for a JTS geometry and an associated userData field.  The userData field
   can be any MASON object, or general Java object, which will be included in the inspector by default.  
   
   <p> MasonGeometry implements sim.util.Proxiable to allow the hiding of various getXXX and setXXX methods 
   from the inspectors.  
*/

public class MasonGeometry implements sim.util.Proxiable, java.io.Serializable  {

    private static final long serialVersionUID = 6290810736517692387L;

	/** Internal JTS geometry object */ 
    public Geometry geometry; 
        
   /** Arbitrary object set by the user */ 
    public Object userData; 
        
    
    /** Java2D shape corresponding to this Geometry. Used to 
     * speed up drawing.
     */
    public Path2D shape; 
    
    public AffineTransform transform; 
    
    public boolean equals(Object o) 
    {
    	if (!(o instanceof MasonGeometry)) { 
    		return false; 
    	}
    	MasonGeometry mg = (MasonGeometry)o; 
    	
    	if (userData != null && mg.userData != null) 
    		return geometry.equals(mg.geometry) && userData.equals(mg.userData); 
    	return geometry.equals(mg.geometry); 
    }
    
    
	public PreparedGeometry preparedGeometry; 

    
    /** Default constructors */ 
    public MasonGeometry() { this(null, null); }
    public MasonGeometry(Geometry g) { this(g, null); }
    public MasonGeometry(Geometry g, Object o) { 
    	geometry  = g; 
    	userData = o; 
    	shape = null; 
    	transform = new AffineTransform(); 
    	preparedGeometry = null;
    	if (geometry != null) 
    		preparedGeometry = PreparedGeometryFactory.prepare(geometry); 

    }

    /** Get and set the userData field */ 
    public void setUserData(Object o) { userData = o; } 
    public Object getUserData() { return userData; } 
    
    /** Returns the type of the internal JTS geometry object (Point, Polygon, Linestring, etc) */ 
    public String toString() { return geometry.getGeometryType();  } 
             
    /** Returns the JTS geometry object.  */
    public Geometry getGeometry() { return geometry; }
    
    /** Inner class allows us to prevent certain getXXX and setXXX methods from 
     * appearing in the Inspector
     */
    public class GeomWrapperProxy
    {
    	/** Returns the area of the internal JTS geometry object.  
        The units are the same as same as the internal JTS geometry object */
    	public double getArea() { return geometry.getArea(); }
    	
    	/** Returns the length of the perimeter of the internal JTS geometry object. 
        The units are the same as same as the internal JTS geometry object */
    	public double getPerimeter() { return geometry.getLength(); }
    	
    	/** The number of vertices which make up the geometry */ 
    	public int getNumVertices() { return geometry.getNumPoints(); }
    }
    
	public Object propertiesProxy() { return new GeomWrapperProxy(); }
     
}
