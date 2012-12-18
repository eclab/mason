/*
 * $Id$
 */
package tests.sim.field.geo;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import sim.field.geo.GeomVectorField;
import sim.portrayal.DrawInfo2D;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;



/**
 */
public class GeomVectorFieldTest
{

    private GeometryFactory factory = new GeometryFactory();

    private Point createPoint(double x, double y)
    {
        return factory.createPoint(new Coordinate(x,y));
    }

    public GeomVectorFieldTest()
    {
    }



    @BeforeClass
    public static void setUpClass()
    {
    }



    @AfterClass
    public static void tearDownClass()
    {
    }



    @Before
    public void setUp()
    {
    }



    @After
    public void tearDown()
    {
    }



    /**
     * Test of addGeometry method, of class GeomVectorField.
     */
    @Test
    public void testAddGeometry()
    {
        System.out.println("addGeometry");
        MasonGeometry g = new MasonGeometry(createPoint(1,1));
        GeomVectorField instance = new GeomVectorField();
        instance.addGeometry(g);
        
        Bag geometries = instance.getGeometries();

        assertTrue(geometries.size() == 1);
        assertTrue(geometries.objs[0].equals(g));
    }



    /**
     * Test of clear method, of class GeomVectorField.
     */
    @Test
    public void testClear()
    {
        System.out.println("clear");

        MasonGeometry g = new MasonGeometry(createPoint(1,1));
        GeomVectorField instance = new GeomVectorField();
        instance.addGeometry(g);

        assertTrue(instance.getGeometries().size() == 1);
        
        instance.clear();
        Bag geometries = instance.getGeometries();

        assertTrue(geometries.isEmpty());
    }

//
//
//    /**
//     * Test of computeConvexHull method, of class GeomVectorField.
//     */
//    @Test
//    public void testComputeConvexHull()
//    {
//        System.out.println("computeConvexHull");
//        GeomVectorField instance = new GeomVectorField();
//        instance.computeConvexHull();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//
//
//    /**
//     * Test of isInsideConvexHull method, of class GeomVectorField.
//     */
//    @Test
//    public void testIsInsideConvexHull()
//    {
//        System.out.println("isInsideConvexHull");
//        Coordinate coord = null;
//        GeomVectorField instance = new GeomVectorField();
//        boolean expResult = false;
//        boolean result = instance.isInsideConvexHull(coord);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//
//
//    /**
//     * Test of computeUnion method, of class GeomVectorField.
//     */
//    @Test
//    public void testComputeUnion()
//    {
//        System.out.println("computeUnion");
//        GeomVectorField instance = new GeomVectorField();
//        instance.computeUnion();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//
//
//    /**
//     * Test of isInsideUnion method, of class GeomVectorField.
//     */
//    @Test
//    public void testIsInsideUnion()
//    {
//        System.out.println("isInsideUnion");
//        Coordinate point = null;
//        GeomVectorField instance = new GeomVectorField();
//        boolean expResult = false;
//        boolean result = instance.isInsideUnion(point);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//

    /**
     * Test of queryField method, of class GeomVectorField.
     */
    @Test
    public void testQueryField()
    {
        System.out.println("queryField");

        Envelope e = new Envelope(40, 60, 40, 60);
        GeomVectorField instance = new GeomVectorField();

        addEvenlySpacedPoints(100, 100, 10, instance);

        Bag result = instance.queryField(e);

        // We *should* get nine points
        // TODO ensure that besides the correct # of points, that we also
        // have *the correct* points.
        assertTrue( result.size() == 9 );
    }

//    /**
//     * Test of getGeometries method, of class GeomVectorField.
//     */
//    @Test
//    public void testGetGeometries()
//    {
//        System.out.println("getGeometries");
//        GeomVectorField instance = new GeomVectorField();
//        Bag expResult = null;
//        Bag result = instance.getGeometries();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//

    /**
     * Test of getObjectsWithinDistance method, of class GeomVectorField.
     */
    @Test
    public void testGetObjectsWithinDistance()
    {
        System.out.println("getObjectsWithinDistance");

        GeomVectorField instance = new GeomVectorField();

        addEvenlySpacedPoints(100, 100, 10, instance);

        MasonGeometry testPoint = new MasonGeometry(createPoint(50,50));

        Bag result = instance.getObjectsWithinDistance(testPoint, 10.0);

        // Essentially the points immediately above, below, to either side, and
        // the point centered at (50,50).
        assertTrue( result.size() == 5 );
    }

//
//
//    /**
//     * Test of getCoveringObjects method, of class GeomVectorField.
//     */
//    @Test
//    public void testGetCoveringObjects()
//    {
//        System.out.println("getCoveringObjects");
//        Geometry g = null;
//        GeomVectorField instance = new GeomVectorField();
//        Bag expResult = null;
//        Bag result = instance.getCoveringObjects(g);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//
//
//    /**
//     * Test of getCoveredObjects method, of class GeomVectorField.
//     */
//    @Test
//    public void testGetCoveredObjects()
//    {
//        System.out.println("getCoveredObjects");
//        MasonGeometry g = null;
//        GeomVectorField instance = new GeomVectorField();
//        Bag expResult = null;
//        Bag result = instance.getCoveredObjects(g);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//
//
//    /**
//     * Test of getContainingObjects method, of class GeomVectorField.
//     */
//    @Test
//    public void testGetContainingObjects()
//    {
//        System.out.println("getContainingObjects");
//        Geometry g = null;
//        GeomVectorField instance = new GeomVectorField();
//        Bag expResult = null;
//        Bag result = instance.getContainingObjects(g);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//
//
//    /**
//     * Test of getTouchingObjects method, of class GeomVectorField.
//     */
//    @Test
//    public void testGetTouchingObjects()
//    {
//        System.out.println("getTouchingObjects");
//        MasonGeometry g = null;
//        GeomVectorField instance = new GeomVectorField();
//        Bag expResult = null;
//        Bag result = instance.getTouchingObjects(g);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//
//
//    /**
//     * Test of isCovered method, of class GeomVectorField.
//     */
//    @Test
//    public void testIsCovered_MasonGeometry()
//    {
//        System.out.println("isCovered");
//        MasonGeometry g = null;
//        GeomVectorField instance = new GeomVectorField();
//        boolean expResult = false;
//        boolean result = instance.isCovered(g);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//
//
//    /**
//     * Test of isCovered method, of class GeomVectorField.
//     */
//    @Test
//    public void testIsCovered_Coordinate()
//    {
//        System.out.println("isCovered");
//        Coordinate point = null;
//        GeomVectorField instance = new GeomVectorField();
//        boolean expResult = false;
//        boolean result = instance.isCovered(point);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//
//
//    /**
//     * Test of getGeometryLocation method, of class GeomVectorField.
//     */
//    @Test
//    public void testGetGeometryLocation()
//    {
//        System.out.println("getGeometryLocation");
//        Geometry g = null;
//        GeomVectorField instance = new GeomVectorField();
//        Point expResult = null;
//        Point result = instance.getGeometryLocation(g);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//
//
//    /**
//     * Test of setGeometryLocation method, of class GeomVectorField.
//     */
//    @Test
//    public void testSetGeometryLocation()
//    {
//        System.out.println("setGeometryLocation");
//        Geometry g = null;
//        CoordinateSequenceFilter p = null;
//        GeomVectorField instance = new GeomVectorField();
//        instance.setGeometryLocation(g, p);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//
//
//    /**
//     * Test of findGeometry method, of class GeomVectorField.
//     */
//    @Test
//    public void testFindGeometry()
//    {
//        System.out.println("findGeometry");
//        Geometry g = null;
//        GeomVectorField instance = new GeomVectorField();
//        Geometry expResult = null;
//        Geometry result = instance.findGeometry(g);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//
//
//    /**
//     * Test of updateTree method, of class GeomVectorField.
//     */
//    @Test
//    public void testUpdateTree()
//    {
//        System.out.println("updateTree");
//        Geometry g = null;
//        AffineTransformation at = null;
//        GeomVectorField instance = new GeomVectorField();
//        instance.updateTree(g, at);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//
//
//    /**
//     * Test of getGeometry method, of class GeomVectorField.
//     */
//    @Test
//    public void testGetGeometry()
//    {
//        System.out.println("getGeometry");
//        String name = "";
//        Object value = null;
//        GeomVectorField instance = new GeomVectorField();
//        MasonGeometry expResult = null;
//        MasonGeometry result = instance.getGeometry(name, value);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//
//
//    /**
//     * Test of updateTransform method, of class GeomVectorField.
//     */
//    @Test
//    public void testUpdateTransform()
//    {
//        System.out.println("updateTransform");
//        DrawInfo2D info = null;
//        GeomVectorField instance = new GeomVectorField();
//        instance.updateTransform(info);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }



    /** Adds evenly spaced points to the given vector field */
    private void addEvenlySpacedPoints(int numX, int numY, int spacing, GeomVectorField field)
    {
        for (int x = 0; x < numX; x++)
        {
            for (int y = 0; y < numY; y++)
            {
                field.addGeometry(new MasonGeometry(createPoint(x * spacing, y * spacing)));
            }
        }
    }

}
