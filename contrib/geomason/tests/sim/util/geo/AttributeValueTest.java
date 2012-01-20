/*
 * $Id$
 */
package tests.sim.util.geo;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import sim.util.geo.AttributeValue;



/**
 *
 * @author mcoletti
 */
public class AttributeValueTest
{

    public AttributeValueTest()
    {
    }



    @BeforeClass
    public static void setUpClass() throws Exception
    {
    }



    @AfterClass
    public static void tearDownClass() throws Exception
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
     * Test of toString method, of class AttributeValue.
     */
    @Test
    public void testToString()
    {
        System.out.println("toString");
        AttributeValue instance = new AttributeValue();
        
        instance.setInteger(42);
        instance.setHidden(true);
        instance.setFieldSize(99);

        String expResult = "Value: 42 Field size: 99 Hidden: true";
        String result = instance.toString();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }



    /**
     * Test of clone method, of class AttributeValue.
     */
    @Test
    public void testClone()
    {
        System.out.println("clone");
        AttributeValue instance = new AttributeValue();
        
        instance.setInteger(42);
        instance.setHidden(true);
        instance.setFieldSize(99);

        Object expResult = instance;
        Object result = instance.clone();
        
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }



    /**
     * Test of getValue method, of class AttributeValue.
     */
    @Test
    public void testGetValue()
    {
        System.out.println("getValue");
        AttributeValue instance = new AttributeValue(new Integer(42));

        Object expResult = new Integer(42);

        Object result = instance.getValue();
        
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }



    /**
     * Test of setValue method, of class AttributeValue.
     */
    @Test
    public void testSetValue()
    {
        System.out.println("setValue");
        Object value = new Double(123.45);
        AttributeValue instance = new AttributeValue();
        instance.setValue(value);
        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
        Object result = instance.getDouble();

        assertEquals(result, value);
    }



    /**
     * Test of setInteger method, of class AttributeValue.
     */
    @Test
    public void testSetInteger()
    {
        System.out.println("setInteger");
        Integer value = new Integer(42);
        AttributeValue instance = new AttributeValue();
        instance.setInteger(value);
        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
        Integer returned = instance.getInteger();
        assertEquals(value,returned);
    }



    /**
     * Test of getInteger method, of class AttributeValue.
     */
    @Test
    public void testGetInteger()
    {
        System.out.println("getInteger");
        AttributeValue instance = new AttributeValue(new Integer(42));
        Integer expResult = new Integer(42);
        Integer result = instance.getInteger();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }



    /**
     * Test of setDouble method, of class AttributeValue.
     */
    @Test
    public void testSetDouble()
    {
        System.out.println("setDouble");
        Double value = new Double(123.4);
        AttributeValue instance = new AttributeValue();
        instance.setDouble(value);
        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
        assertEquals(instance.getDouble(),value);
    }



    /**
     * Test of getDouble method, of class AttributeValue.
     */
    @Test
    public void testGetDouble()
    {
        System.out.println("getDouble");
        AttributeValue instance = new AttributeValue(new Double(123.4));
        Double expResult = new Double(123.4);
        Double result = instance.getDouble();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }



    /**
     * Test of setString method, of class AttributeValue.
     */
    @Test
    public void testSetString()
    {
        System.out.println("setString");
        String value = "foo";
        AttributeValue instance = new AttributeValue();
        instance.setString(value);
        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
        assertEquals("foo",instance.getString());
    }



    /**
     * Test of getString method, of class AttributeValue.
     */
    @Test
    public void testGetString()
    {
        System.out.println("getString");
        AttributeValue instance = new AttributeValue("foo");
        String expResult = "foo";
        String result = instance.getString();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }



    /**
     * Test of isHidden method, of class AttributeValue.
     */
    @Test
    public void testIsHidden()
    {
        System.out.println("isHidden");
        AttributeValue instance = new AttributeValue();
        boolean expResult = false;
        boolean result = instance.isHidden();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }



    /**
     * Test of setHidden method, of class AttributeValue.
     */
    @Test
    public void testSetHidden()
    {
        System.out.println("setHidden");
        boolean hidden = true;
        AttributeValue instance = new AttributeValue();
        instance.setHidden(hidden);
        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
        assertEquals(hidden,instance.isHidden());
    }



    /**
     * Test of getFieldSize method, of class AttributeValue.
     */
    @Test
    public void testGetFieldSize()
    {
        System.out.println("getFieldSize");
        AttributeValue instance = new AttributeValue();
        int expResult = 0;
        int result = instance.getFieldSize();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }



    /**
     * Test of setFieldSize method, of class AttributeValue.
     */
    @Test
    public void testSetFieldSize()
    {
        System.out.println("setFieldSize");
        int fieldSize = 99;
        AttributeValue instance = new AttributeValue();
        instance.setFieldSize(fieldSize);
        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
        assertEquals(fieldSize,instance.getFieldSize());
    }

}
