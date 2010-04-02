package sim.field.geo;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import sim.util.Bag;
import com.vividsolutions.jts.index.strtree.*; 
import com.vividsolutions.jts.index.*;
import com.vividsolutions.jts.algorithm.locate.*; 
import com.vividsolutions.jts.algorithm.*; 
import sim.util.geo.GeomWrapper;

import java.util.*;

/** 
 A GeomField contains multiple 2.5D objects, which can represent either GIS data 
 or generic geometric data.  The geometry objects are represented by sim.util.geo.GeomWrapper
 objects, and should not be modified after constrution as this will have undefined consequences 
 for inspection.  
 */
public class GeomField implements java.io.Serializable {

    /**  Contains the minimum bounding rectangle of all the stored geometries. */
    private Envelope MBR;
    
    /** Contains all the geometries in the field*/
    private Bag geometries;
	
	/** The convex hull of all the geometries in this field */ 
	private Geometry convexHull; 
	
	GeometryFactory geomFactory; 

	/** Defines the outer shell of all the geometries within this field */ 
	Geometry globalUnion; 

	
	/** Default constructor, which resets all internal data structures.  */ 
    public GeomField()
    {
		geometries = new Bag(); 
		MBR = new Envelope(); 
		geomFactory = new GeometryFactory(); 
    }

    /** Adds the GeomWrapper to the field and also expands the MBR */
	public void addGeometry( final GeomWrapper g )
    {
        geometries.add(g);
        MBR.expandToInclude(g.fetchGeometry().getEnvelopeInternal());
    }

    /** Removes all geometry objects and resets the MBR. */
    public void clear()
    {
        geometries.clear();
        MBR = new Envelope();
    }

	/** Computes the convex hull of all the geometries in this field.  Call this 
	 method once.  
	 */ 
	public void computeConvexHull()
	{
		ArrayList pts = new ArrayList(); 
		for (int i=0; i < geometries.size(); i++) { 
			GeomWrapper wrapper = (GeomWrapper)geometries.objs[i]; 
			
			Geometry g = wrapper.fetchGeometry();
			Coordinate c[] = g.getCoordinates(); 
			for (int j=0; j < c.length; j++) 
				pts.add(c[j]); 
		}
		
		Coordinate arr[] = new Coordinate[pts.size()]; 
		for (int j=0; j < pts.size(); j++) 
			arr[j] = (Coordinate)pts.get(j); 
		
		ConvexHull hull = new ConvexHull(arr, geomFactory); 
		convexHull = hull.getConvexHull(); 
	}
	
	/**  Determine if the Coordinate is within the convex hull of the field's geometries.  Call 
	 computeConvexHull first.  */ 
	public boolean isInsideConvexHull(final Coordinate coord)
	{
		if (SimplePointInAreaLocator.locate(coord, convexHull) == Location.INTERIOR)
			return true;
		return false; 
	}
	
	
	/** Compute the union of the field's geometries.  The resulting Geometry is the outside points of 
	 the field's geometries. Call this method only once.  
	 */ 
	public void computeUnion()
	{
		globalUnion = new Polygon(null, null, geomFactory);
		for (int i=0; i < geometries.size(); i++) { 
			GeomWrapper wrapper = (GeomWrapper) geometries.objs[i]; 
			Geometry g = wrapper.fetchGeometry(); 
			globalUnion = globalUnion.union(g); 
		}
	}
	
	/** Determine if the Coordinate is withing the bounding Geometry of the fields 
	 geometries.  Call computeUnion first. 
	 */ 
	public boolean isInsideUnion(final Coordinate point)
	{
		if (SimplePointInAreaLocator.locate(point, globalUnion) == Location.INTERIOR)
			return true; 
		return false; 
	}
	
	
	
    /** Returns the width of the MBR. */
    public double getWidth() { return MBR.getWidth(); }

    /** Returns the height of the MBR. */ 
	public double getHeight() { return MBR.getHeight(); }

    
    /** Returns the minimum bounding rectangle */
	public final Envelope getMBR() { return MBR; }

    /** Set the MBR */
    public void setMBR(Envelope MBR) { this.MBR = MBR; }

    /** 
	 Returns the field's geometry objects.  Do not modify the Bag, 
	 nor the Geometries inside the bag, as this will have undefined 
	 consequences for drawing and inspecting the geometries. 
	 */
    public Bag getGeometry() { return geometries; }

    /** 
	 Returns a bag containing all those objects within distance of the given geometry.  
	 The distance calculation follows the JTS convention, which determines the distance 
	 between the closes points of two geometries.  Do not modify the returned Bag.    
	 */
    public Bag getObjectsWithinDistance(final Geometry g, final double d)
    {
        Bag nearbyObjects = new Bag();

        for (int i = 0; i < geometries.numObjs; i++)
        {
			Geometry g1 = ((GeomWrapper)geometries.objs[i]).fetchGeometry();
            if (!g.equals(g1) && DistanceOp.isWithinDistance(g,g1, d))
                nearbyObjects.add(geometries.objs[i]);
        }
        return nearbyObjects;
    }

    /** 
	 Returns geometries that cover the given object.  Cover here means completely 
	 cover, including points on the bondaries.  Do not modify the returned Bag. 
	 */
    public final Bag getCoveringObjects(final Geometry g)
    {
        Bag coveringObjects = new Bag();

        for (int i = 0; i < geometries.numObjs; i++)
        {
			Geometry g1 = ((GeomWrapper)geometries.objs[i]).fetchGeometry();
			if (!g.equals(g1) && g1.covers(g))
            {
                coveringObjects.add(geometries.objs[i]);
            }
        }

        return coveringObjects;
    }

    /** Return geometries that are covered by the given object
     *
     */
    public final Bag getCoveredObjects(final Geometry g)
    {
        Bag coveredObjects = new Bag();


        for (int i = 0; i < geometries.numObjs; i++)
        {
			Geometry g1 = ((GeomWrapper)geometries.objs[i]).fetchGeometry();
			if (!g.equals(g1) && g.covers(g1))
            {
                coveredObjects.add(geometries.objs[i]);
            }
        }

        return coveredObjects;
    }

    
    /** Returns geometries that touch the given object.
     *
     * @param g Geometry for which we want to find touching objects
     * @returns Bag containing any geometries touching g
     */
    public final Bag getTouchingObjects(final Geometry g)
    {
        Bag touchingObjects = new Bag();
        
  
        for (int i = 0; i < geometries.numObjs; i++)
        {
			Geometry g1 = ((GeomWrapper)geometries.objs[i]).fetchGeometry();
			if (!g.equals(g1) && g1.touches(g))
            {
                touchingObjects.add(geometries.objs[i]);
            }
        }

        return touchingObjects;
    }
    
    

    /** 
	 Returns true if the given object is covered by any geometry in the field.
	 Cover here includes points in the boundaries.  
	 
	 @see #isCovered(com.vividsolutions.jts.geom.Coordinate)     
	 */
    public boolean isCovered(final Geometry g)
    {
        for (int i = 0; i < geometries.numObjs; i++)
        {
			Geometry g1 = ((GeomWrapper)geometries.objs[i]).fetchGeometry();
            if (!g.equals(g1) && g1.covers(g))
				return true;
        }
        return false;
	}
	
	/**
	 Returns true if the coordinate is within any geometry in the field.  However, it offers no 
	 guarentee for points on the boundaries.  Use this version if you want to check if an agent is 
	 within a geometry; its roughly an order of magnitude faster than using the Geometry version. 
	 
	 @see #isCovered(com.vividsolutions.jts.geom.Geometry) 
	 */ 
	public boolean isCovered(final Coordinate point)
    {
		for (int i = 0; i < geometries.numObjs; i++)
        {
			Geometry g1 = ((GeomWrapper)geometries.objs[i]).fetchGeometry();
			if (SimplePointInAreaLocator.locate(point, g1) == Location.INTERIOR)
				return true;
        }
        return false; 
	}
}
