/*
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 *
 * See the file "LICENSE" for more information
 *
 * $Id$
*/
package sim.field.geo;

import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.geom.prep.PreparedPoint;
import com.vividsolutions.jts.geom.prep.PreparedPolygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import sim.engine.Schedule;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.portrayal.DrawInfo2D;
import sim.util.Bag;
import sim.util.geo.AttributeValue;
import sim.util.geo.GeometryUtilities;
import sim.util.geo.MasonGeometry;


/** 
   A GeomVectorField contains one or more MasonGeometry objects.  The field 
   stores the geometries as a quadtree and used the quadtree during various
   queries.  As objects are inserted into the field, the minimum bounding
   rectangle (MBR) is expanded to include the new object.  This allows a
   determination of the area of the field.
   
   <p>Note that the field assumes the geometries use the same coordinate system.
*/
public class GeomVectorField extends GeomField
{
	private static final long serialVersionUID = -754748817928825708L;

	/** A spatial index of all the geometries in the field.

     XXX Consider also storing Bag of MasonGeometry to make rebuilding
     spatial index in updateSpatialIndex() a little more efficient; also would
     make it easier to use alternative JTS spatial index.
     */
	private  Quadtree spatialIndex = new Quadtree();
    
    /** The convex hull of all the geometries in this field */ 
    private PreparedPolygon convexHull;
        
    /** Helper factory for computing the union or convex hull */
    private GeometryFactory geomFactory;

    /** Defines the outer shell of all the geometries within this field */ 
    private PreparedPolygon globalUnion;


    public GeomVectorField()
    {
        this(0, 0);
    }


    /** Default constructor, which resets all internal data structures.  */ 
    public GeomVectorField(int w, int h)
    {
        super(w,h);
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
        {
            return true;
        }
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
        {
            return true;
        }
        return false; 
    }
        
	
	/** @return all the geometries that intersect the provided
     * envelope; will be empty if none intersect  */
	public synchronized Bag queryField(Envelope e)
	{
		List<?> gList = spatialIndex.query(e);
		Bag geometries = new Bag(gList.size());

        // However, the JTS QuadTree query is a little sloppy, which means it
        // may return objects that are still outside the range.  We need to do
        // a second pass to trim out the objects that are further than distance.

        for (int i = 0; i < gList.size(); i++)
        {
            MasonGeometry tempGeometry = (MasonGeometry) gList.get(i);

            if (e.intersects(tempGeometry.getGeometry().getEnvelopeInternal()))
            {
                geometries.add(tempGeometry);
            }
        }

		return geometries; 
	}
    

    /** 
        Returns all the field's geometry objects.  Do not modify the Bag, 
        nor the Geometries inside the bag, as this will have undefined 
        consequences for drawing and inspecting. 
    */
	public Bag getGeometries()
    {
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

        // However, the JTS QuadTree query is a little sloppy, which means it
        // may return objects that are still outside the range.  We need to do
        // a second pass to trim out the objects that are further than distance.

        for (int i = 0; i < gList.size(); i++)
        {
            MasonGeometry tempGeometry = (MasonGeometry) gList.get(i);

            if (g.isWithinDistance(tempGeometry.getGeometry(), dist))
            {
                nearbyObjects.add(tempGeometry);
            }
        }

        return nearbyObjects;
    }
    
    public Bag getObjectsWithinDistance(final MasonGeometry mg, final double dist)
    {
        return getObjectsWithinDistance(mg.getGeometry(), dist);
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
            {
                coveringObjects.add(gm);
            }
		}
        return coveringObjects;
    }

    public final Bag getCoveringObjects(final MasonGeometry mg)
    {
        return getCoveringObjects(mg.getGeometry());
    }

    /** Return geometries that are covered by the given geometry. 
     * Do not modify the returned Bag. */
    public final Bag getCoveredObjects( MasonGeometry g)
    {
		Bag coveringObjects = new Bag(); 
		List<?> gList = spatialIndex.queryAll(); 
		
		if (g.preparedGeometry == null)
        {
            g.preparedGeometry =  PreparedGeometryFactory.prepare(g.getGeometry());
        }
	
        for (int i = 0; i < gList.size(); i++)
		{
			MasonGeometry gm = (MasonGeometry)gList.get(i); 
			Geometry g1 = gm.getGeometry();
			if (g.preparedGeometry.covers(g1))
            {
                coveringObjects.add(gm);
            }
		}
        return coveringObjects;
	}


    /** 
        Returns geometries that contain the given object. Contain is more exclusive than cover
        and doesn't include things on the boundary. Do not modify the returned Bag. 
    */
    public final Bag getContainingObjects(final Geometry g)
    {
		Bag containingObjects = new Bag(); 
		Envelope e = g.getEnvelopeInternal();
		List<?> gList = spatialIndex.query(e);
        for (int i = 0; i < gList.size(); i++)
		{
			MasonGeometry gm = (MasonGeometry)gList.get(i); 
			Geometry g1 = gm.getGeometry();
			if (!g.equals(g1) && g1.contains(g))
            {
                containingObjects.add(gm);
            }
		}
        return containingObjects;
    }

    public final Bag getContainingObjectgs(final MasonGeometry mg)
    {
        return getContainingObjects(mg.getGeometry());
    }

    
    /** Returns geometries that touch the given geometry.
     * Do not modify the returned Bag. */
    public final Bag getTouchingObjects( MasonGeometry g)
    {
		Bag touchingObjects = new Bag(); 
		Envelope e = g.getGeometry().getEnvelopeInternal();
		List<?> gList = spatialIndex.query(e);
		if (g.preparedGeometry == null)
        {
            g.preparedGeometry =  PreparedGeometryFactory.prepare(g.getGeometry());
        }
	
		for (int i = 0; i < gList.size(); i++)
		{
			MasonGeometry gm = (MasonGeometry)gList.get(i); 
			Geometry g1 = gm.getGeometry();
			if (!g.equals(g1) && g.preparedGeometry.touches(g1))
            {
                touchingObjects.add(gm);
            }
		}
        return touchingObjects;
	}
    

    /** 
        Returns true if the given Geometry is covered by any geometry in the field.
        Cover here includes points in the boundaries.           
    */
    public boolean isCovered( MasonGeometry g)
    {
		Envelope e = g.getGeometry().getEnvelopeInternal(); 
		List<?> gList = spatialIndex.query(e);
		if (g.preparedGeometry == null)
        {
            g.preparedGeometry =  PreparedGeometryFactory.prepare(g.getGeometry());
        }
		
		for (int i=0; i < gList.size(); i++) {
			Geometry g1 = ((MasonGeometry)gList.get(i)).getGeometry();
			if (!g.equals(g1) && g.preparedGeometry.covers(g1))
            {
                return true;
            }
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
        for (int i = 0; i < gList.size(); i++)
        {
            Geometry g1 = ((MasonGeometry) gList.get(i)).getGeometry();
            if (p.intersects(g1))
            {
                return true;
            }
        }
        return false;
    }
	
	
	/** Get the centroid of the given Geometry.  Note that the returned location uses the 
	 coordinate system of the underlying GIS data.  */
	public Point getGeometryLocation(MasonGeometry g)
	{
		MasonGeometry g1 = findGeometry(g);
		if (g1.equals(g))
        {
            return g1.geometry.getCentroid();
        }
		return null; 
	}
	
	/** Moves the centroid of the given geometry to the provided point.
     * <p>
     * <em>Note</em> that the spatial index is not notified of the geometry changes.
     * It is strongly recommended that updateSpatialIndex() be invoked after all
     * geometry position changes.
     * 
     * @see GeomVectorField#updateSpatialIndex() 
     */
	 public void setGeometryLocation(MasonGeometry g, CoordinateSequenceFilter p)
	 {
         MasonGeometry g1 = findGeometry(g);
         if (g1 != null)
         {
             // 1/8/2013, spatial index no longer updated; use updateSpatialIndex()
//             spatialIndex.remove(g1.getGeometry().getEnvelopeInternal(), g1);
             g1.geometry.apply(p);
             g1.geometry.geometryChanged();
//             spatialIndex.insert(g1.geometry.getEnvelopeInternal(), g1);
         }
    }

     /** Rebuild the spatial index from the current set of geometry
      * <p>
      * If the objects contained in this field have moved, then the spatial
      * index will have to be updated.  This is done by replacing the current
      * spatial index with an entirely new one built from the same stored geometry.
      */
     public void updateSpatialIndex()
     {
         List objects = spatialIndex.queryAll();

         spatialIndex = new Quadtree();

         for (Object object : objects)
         {
             spatialIndex.insert(((MasonGeometry)object).geometry.getEnvelopeInternal(), object);
         }
     }



     /** Schedules a repeating Steppable that updates spatial index
      * <p>
      * The spatial index for a GeomVectorField containing moving objects
      * will need to be updated after all such objects have moved.  This method
      * returns a Steppable that invokes updateSpatialIndex() that does this.
      * 
      * @return a Steppable that can be used to remove this Steppable from the schedule
      */
     public Steppable scheduleSpatialIndexUpdater()
     {
        return new Steppable()
        {
            public void step(SimState state)
            {
                updateSpatialIndex();
            }

        };
    }
     

     
			
     /** Locate a specific geometry inside the quadtree
      * <p>
      * XXX Is returning what we're looking for when the target geometry is
      * not found the desired behavior?
      * 
      * @param g is geometry for which we're looking
      * @return located geometry; will return g if not found.
      */
    public synchronized MasonGeometry findGeometry(MasonGeometry g)
    {
        List<?> gList = spatialIndex.query(g.getGeometry().getEnvelopeInternal());

        for (int i = 0; i < gList.size(); i++)
        {
            MasonGeometry g1 = ((MasonGeometry) gList.get(i));
            if (g1.equals(g))
            {
                return g1;
            }
        }
        
        return g;
    }
    

	 public void updateTree(Geometry g, com.vividsolutions.jts.geom.util.AffineTransformation at)
	 {
		MasonGeometry mg = new MasonGeometry(g);
		if (spatialIndex.remove(g.getEnvelopeInternal(), mg)) { 
			mg.geometry.apply(at); 
			addGeometry(mg); 
		}
		
		
		/* List<?> gList = spatialIndex.query(g.getEnvelopeInternal());
		 for (int i=0; i < gList.size(); i++) {
			 Geometry g1 = ((MasonGeometry)gList.get(i)).getGeometry(); 
			 if (g1.equals(g)) { 
				 g1.apply(at); 
				 return;
			 }
		 } */
	 }

     

	 /** Searches the field for the first object with attribute <i>name</i> that has value <i>value</i>.
      *
      * @param name of attribute
      * @param value of attribute
      *
      * TODO What if there is more than one such object?
      *
      * @return MasonGeometry with specified attribute otherwise null
      */
    public MasonGeometry getGeometry(String name, Object value)
    {
        AttributeValue key = new AttributeValue(name);
        List<?> gList = spatialIndex.queryAll();

        for (int i = 0; i < gList.size(); i++)
        {
            MasonGeometry mg = (MasonGeometry) gList.get(i);

            if ( mg.hasAttribute(name) && mg.getAttribute(name).equals(value) )
            {
                return  mg;
            }
        }
        
        return null;
    }

    
	 
	 public Envelope clipEnvelope; 
	 DrawInfo2D myInfo; 
	 public AffineTransform worldToScreen; 
	 public com.vividsolutions.jts.geom.util.AffineTransformation jtsTransform; 



    public void updateTransform(DrawInfo2D info)
    {
    	
    	//if (info.draw.width == 4800)
    	//	System.out.println("here"); 
    	
        // need to update the transform
        if (! info.equals(myInfo))
        {
            myInfo = info;
            // compute the transform between world and screen coordinates, and
            // also construct a geom.util.AffineTransform for use in hit-testing
            // later
            Envelope myMBR = getMBR();
            
            worldToScreen = GeometryUtilities.worldToScreenTransform(myMBR, info);
            jtsTransform = GeometryUtilities.getPortrayalTransform(worldToScreen, this, info.draw);

            Point2D p1 = GeometryUtilities.screenToWorldPointTransform(worldToScreen, info.clip.x, info.clip.y);
            Point2D p2 = GeometryUtilities.screenToWorldPointTransform(worldToScreen, info.clip.x + info.clip.width,
                                                                       info.clip.y + info.clip.height);

            clipEnvelope = new Envelope(p1.getX(), p2.getX(), p1.getY(), p2.getY());
        }
    }
	 
}
