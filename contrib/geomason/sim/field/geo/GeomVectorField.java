package sim.field.geo;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.prep.*; 

import com.vividsolutions.jts.algorithm.*; 

import sim.util.geo.AttributeField;
import sim.util.geo.GeometryUtilities;
import sim.util.geo.MasonGeometry;

import com.vividsolutions.jts.index.quadtree.*; 

import sim.util.*; 
import java.util.*;

/** 
   A GeomVectorField contains one or more MasonGeometry objects.  The field stores the geometries as a quadtree
   and used the quadtree during various queries.  As objects are inserted into the field, the minimum 
   bounding rectangle (MBR) is expanded to include the new object.  This allows a determination of the area 
   of the field.    
   
   <p>Note that the field assumes the geometries use the same coordinate system.  
*/
public class GeomVectorField extends GeomField
{
	private static final long serialVersionUID = -754748817928825708L;

	/** A spatial index of all the geometries in the field. */ 
	public Quadtree spatialIndex;
    
    /** The convex hull of all the geometries in this field */ 
    public PreparedPolygon convexHull; 
        
    /** Helper factory for computing the union or convex hull */
    public GeometryFactory geomFactory; 

    /** Defines the outer shell of all the geometries within this field */ 
    public PreparedPolygon globalUnion; 
    
    /** Default constructor, which resets all internal data structures.  */ 
    public GeomVectorField()
    {
        super();
        
		spatialIndex = new Quadtree();
        geomFactory = new GeometryFactory(); 
    }

    /** Adds the MasonGeometry to the field and also expands the MBR */
    public void addGeometry( final MasonGeometry g )
    {
		Envelope e = g.getGeometry().getEnvelopeInternal();
        MBR.expandToInclude(e);
		spatialIndex.insert(e, g);
    }

    /** Removes all geometry objects and resets the MBR. */
    @Override
    public void clear()
    {
        super.clear();
		spatialIndex = new Quadtree();
    }


    /** Computes the convex hull of all the geometries in this field.  Call this 
        method once.  
    */ 
    public void computeConvexHull()
    {
        ArrayList<Coordinate> pts = new ArrayList<Coordinate>();
        List<?> gList = spatialIndex.queryAll();

        if (gList.isEmpty())
        {
            return;
        }

        // Accumulate all the Coordinates in all the geometry into 'pts'
        for (int i = 0; i < gList.size(); i++)
        {
            Geometry g = ((MasonGeometry) gList.get(i)).getGeometry();
            Coordinate c[] = g.getCoordinates();
            pts.addAll(Arrays.asList(c));
        }

        // ConvexHull expects a vector of Coordinates, so now convert
        // the array list of Coordinates into a vector
        Coordinate[] coords = pts.toArray(new Coordinate[pts.size()]);
        
        ConvexHull hull = new ConvexHull(coords, geomFactory);
        this.convexHull = new PreparedPolygon((Polygon) hull.getConvexHull());
    }
        
    /**  Determine if the Coordinate is within the convex hull of the field's geometries.  Call 
         computeConvexHull first.  */ 
    public boolean isInsideConvexHull(final Coordinate coord)
    {
    	Point p = geomFactory.createPoint(coord);

        // XXX is intersects() correct? Would covers be appropriate?
        if (convexHull.intersects(p))
            return true;
        return false; 
    }
        
        
    /** Compute the union of the field's geometries.  The resulting Geometry is the outside points of 
        the field's geometries. Call this method only once.  
    */ 
        
    public void computeUnion()
    {
        Geometry p = new Polygon(null, null, geomFactory);	
		List<?> gList = spatialIndex.queryAll();

        if (gList.isEmpty())
        {
            return;
        }
        
        for (int i=0; i < gList.size(); i++) {
            Geometry g = ((MasonGeometry)gList.get(i)).getGeometry(); 
            p = p.union(g); 
        }
        
        p = p.union();
        globalUnion = new PreparedPolygon((Polygon)p);
    }
        
    /** Determine if the Coordinate is within the bounding Geometry of the field's 
        geometries.  Call computeUnion first. 
    */ 
    public boolean isInsideUnion(final Coordinate point)
    {
    	Point p = geomFactory.createPoint(point);
    	if (globalUnion.intersects(p)) 
    		return true;
        return false; 
    }
        
	
	/** Returns all the geometries that <i>might</i> intersect the provided envelope.  If the intersection is 
	 * empty, then return all the objects in the field.  */
	public Bag queryField(Envelope e)
	{
		Bag geometries = new Bag(); 
		List<?> gList = spatialIndex.query(e);
		
		if (gList.isEmpty()) 
			gList = spatialIndex.queryAll();
		
		geometries.addAll(gList); 
		return geometries; 
	}

    /** 
        Returns all the field's geometry objects.  Do not modify the Bag, 
        nor the Geometries inside the bag, as this will have undefined 
        consequences for drawing and inspecting. 
    */
	public Bag getGeometries() { 
		Bag geometries = new Bag(); 
		List<?> gList = spatialIndex.queryAll();
		geometries.addAll(gList); 
		return geometries; 
	}

    /** 
        Returns a bag containing all those objects within distance of the given geometry.  
        The distance calculation follows the JTS convention, which determines the distance 
        between the closest points of two geometries.  Do not modify the returned Bag.    
    */
    public Bag getObjectsWithinDistance(final Geometry g, final double dist)
    {
        Bag nearbyObjects = new Bag();
		Envelope e = g.getEnvelopeInternal();
		e.expandBy(dist); 
		
		List<?> gList = spatialIndex.query(e);
		nearbyObjects.addAll(gList); 
        return nearbyObjects;
    }

    /** 
        Returns geometries that cover the given object.  Cover here means completely 
        cover, including points on the boundaries.  Do not modify the returned Bag. 
    */
    public final Bag getCoveringObjects(final Geometry g)
    {
		Bag coveringObjects = new Bag(); 
		Envelope e = g.getEnvelopeInternal();
		List<?> gList = spatialIndex.query(e);
        for (int i = 0; i < gList.size(); i++)
		{
			MasonGeometry gm = (MasonGeometry)gList.get(i); 
			Geometry g1 = gm.getGeometry();
			if (!g.equals(g1) && g1.covers(g))
				coveringObjects.add(gm);
		}
        return coveringObjects;
    }

    /** Return geometries that are covered by the given geometry. 
     * Do not modify the returned Bag. */
    public final Bag getCoveredObjects(final Geometry g)
    {
		Bag coveringObjects = new Bag(); 
		//Envelope e = g.getEnvelopeInternal();
		//List<?> gList = spatialIndex.query(e);
		List<?> gList = spatialIndex.queryAll(); 
		
		PreparedGeometry p = PreparedGeometryFactory.prepare(g); 
	
        for (int i = 0; i < gList.size(); i++)
		{
			MasonGeometry gm = (MasonGeometry)gList.get(i); 
			Geometry g1 = gm.getGeometry();
			if (p.covers(g1)) 
				coveringObjects.add(gm); 
		}
        return coveringObjects;
	}

    
    /** Returns geometries that touch the given geometry.
     * Do not modify the returned Bag. */
    public final Bag getTouchingObjects(final Geometry g)
    {
		Bag touchingObjects = new Bag(); 
		Envelope e = g.getEnvelopeInternal();
		List<?> gList = spatialIndex.query(e);
		PreparedGeometry p = PreparedGeometryFactory.prepare(g); 
        for (int i = 0; i < gList.size(); i++)
		{
			MasonGeometry gm = (MasonGeometry)gList.get(i); 
			Geometry g1 = gm.getGeometry();
			if (!g.equals(g1) && p.touches(g))
				touchingObjects.add(gm);
		}
        return touchingObjects;
	}
    

    /** 
        Returns true if the given Geometry is covered by any geometry in the field.
        Cover here includes points in the boundaries.           
    */
    public boolean isCovered(final Geometry g)
    {
		Envelope e = g.getEnvelopeInternal(); 
		List<?> gList = spatialIndex.query(e);
		PreparedGeometry p = PreparedGeometryFactory.prepare(g); 
        for (int i=0; i < gList.size(); i++) {
			Geometry g1 = ((MasonGeometry)gList.get(i)).getGeometry();
			if (!g.equals(g1) && p.covers(g))
				return true; 
		}
		return false; 
	}
        
    /**
       Returns true if the coordinate is within any geometry in the field.  However, it offers no 
       guarentee for points on the boundaries.  Use this version if you want to check if an agent is 
       within a geometry; its roughly an order of magnitude faster than using the Geometry version. 
    */ 
    public boolean isCovered(final Coordinate point)
    {
		Envelope e = new Envelope(point);
		List<?> gList = spatialIndex.query(e);
		PreparedPoint p = new PreparedPoint(geomFactory.createPoint(point));
        for (int i=0; i < gList.size(); i++) {
			Geometry g1 = ((MasonGeometry)gList.get(i)).getGeometry();
			if (p.intersects(g1))
				return true;
            }
        return false; 
    }
	
	
	/** Get the centroid of the given Geometry.  Note that the returned location uses the 
	 coordinate system of the underlying GIS data.  */
	public Point getGeometryLocation(Geometry g)
	{
		Geometry g1 = findGeometry(g);
		if (g1.equals(g)) 
			return g1.getCentroid(); 
		return null; 
	}
	
	/** Moves the centriod of the given geometry to the provided point.  Note that the provided point 
	 * must be in the same coordinate system as the geometry.  */
	 public void setGeometryLocation(Geometry g, CoordinateSequenceFilter p)
	 {
		 Geometry g1 = findGeometry(g); 
		 if (g1 != null) { 
			 g1.apply(p); 
			 g1.geometryChanged();
		 }
	 }
			
	 /** Helper function to locate a specific geometry inside the quadtree */ 
	 Geometry findGeometry(Geometry g)
	 {
		List<?> gList = spatialIndex.query(g.getEnvelopeInternal());
		for (int i=0; i < gList.size(); i++) {
			Geometry g1 = ((MasonGeometry)gList.get(i)).getGeometry(); 
			if (g1.equals(g)) 
				return g1;
		}
		return g; 	
	 }
	 
	 /**
	  *  Searches the field for the object with attribute <i>name</i> that has value <i>value</i>. 
	  *  Returns null if no such object exists 
	  */
	 @SuppressWarnings("unchecked")
	public MasonGeometry getGeometry(String name, Object value)
	 {
		 AttributeField key = new AttributeField(name); 
		 List<?> gList = spatialIndex.queryAll(); 
		 for (int i=0; i < gList.size(); i++) { 
			 MasonGeometry mg = (MasonGeometry)gList.get(i); 
			 Geometry g = mg.getGeometry(); 
			 ArrayList<AttributeField> attrs = (ArrayList<AttributeField>)g.getUserData(); 
			 int index = Collections.binarySearch(attrs, key, GeometryUtilities.attrFieldCompartor); 
			 if (index >= 0) { 
				 if (attrs.get(index).value.equals(value))
					 return mg; 
			 }
		 }
		 return null; 
	 }
}