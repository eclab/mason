/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util;
import java.util.*;
import java.lang.reflect.*;

// stars down the side maintain formatting
/**
 *  A very simple class for getting and setting object properties.  You create this class by passing in the 
 *  object you'd like to modify.  The only properties that are considered are ones which are simple (or boxed)
 *  booleans, bytes, shorts, ints, longs, floats, doubles, characters, or strings.  
 *  Alternatively, you can get a class like this by calling Properties.getProperties(...), which will return 
 *  either a SimpleProperties or a CollectionProperties, depending on which is appropriate and the flags you 
 *  have passed in.
 *
 *  <p>A property Foo exists in a class if there is a getFoo() or isFoo() method.  ReadWrite properties
 *  are ones for which there is ALSO a setFoo(prop) method.  If the property is a numerical one, you can
 *  also provide a <i>domain</i> in the form of a function called domFoo(), which returns either an array
 *  of Objects or a <tt>sim.util.Interval</tt>.  If no domain function exists, or if the domain function
 *  returns null, then it is assumed the property has no domain (it can take on any value).
 *  You can also hide a property by creating a boolean method called hideFoo() which returns true.
 *
 *  <p>A few classes have special hard-coded properties because they lack get() and set() methods.  Notably:
 *  CharSequences (Strings, StringBuffers, StringBuilders, etc.) have toString() considered a property, integer Numbers have longValue() considered
 *  a property, other Numbers have doubleValue() considered a property, and Booleans have booleanValue() considered
 *  a property.  In all cases the name of the property is simply "Value" and it is read-only.
 *
 *  <p>The idea behind domains is to make it easy to create graphical interfaces (sliders, pop-up menus)
 *  for the user to set properties, where it's often convenient to know beforehand what values the property
 *  can be set to in order to construct the GUI widget appropriately.  Here are the domain rules (other than null).
 *
 *  <ul>
 *  <li>If your property type is a float or double, you can return a domain in the form of an Interval, with a 
 *      double-valued minimum maximum value.  This will typically result in the GUI creating a slider.
 *  <li>If your property type is an integer type (byte/short/int/long), you can return a domain in the form
 *      of an Interval with a long-valued minimum and maximum value.  This will typically result in the GUI creating a slider.
 *  <li>If your property type is an integer type (byte/short/int/long), and you want your domain to be just the
 *      integers 0 ... n, you can return an array (n long) of Strings, each representing a name for the corresponding
 *      integer value.  This will typically result in the GUI creating a pop-up list or menu, displaying those
 *      string labels, and then setting the property to the equivalent number.  For example, a property
 *      called Format might allow the number values 0, 1, and 2, and might have "names" in the array called
 *      "Left Justified", "Right Justified", and "Centered" respectively.

 *  <!--  NOT YET IMPLEMENTED
 *  <li>If your property type is a String, and you want your domain to be any string but typically chosen from
 *      some set of common Strings, you can return an array of Strings, each representing one of those options.
 *      This will typically result in the GUI creating a ComboBox for the text which the user can type in or
 *      choose an option from a drop-down menu.
 *  <li>If your property type is an Object of some for (including a String or an array), and you want your domain
 *      to be restricted to be only one of a set of possible Objects, return an array of arrays.  The
 *      first item of each inner array will be the Object, and the second item in each inner array will be a
 *      String labelling the object.  If there is no second item, then the label will be "" + object for the
 *      first object.  For example, to restrict a Double return type to be one of three kinds of Doubles, you might 
 *      return an array of the form {{new Double(4), "Fourth"}, {new Double(7), "Seventh"}, {new Double(8), "Fred"}}.
 *      If you want a String property type to be one of three Strings, serving as their own labels, you might say:
 *      {{"Hello"}, {"What's"}, {"Going On?"}} (or if you wish to be pedantic,
 *      {{"Hello","Hello"}, {"What's","What's"}, {"Going on?","Going on?"}}).
 *      Note that this is different from saying {"Hello", "What's", "Going on?"},
 *      which more or less states that the user is allowed to enter any other String he wishes as well.
 *  -->
 
 *  </ul>
 *  
 *  <p>This class allows you to set and get properties on the object via boxing the property (java.lang.Integer
 *  for int, for example).  You can also pass in a String, and SimpleProperties will parse the appropriate
 *  value out of the string automatically without you having to bother checking the type.
 *
 *  <p>If any errors arise from generating the properties, setting them, or getting their values, then
 *  the error is printed to the console, but is not thrown: instead typically null is returned.
 *
 *  <p>If the object provided to SimpleProperties is sim.util.Proxiable, then SimpleProperties will call
 *  propertiesProxy() on that object to get the "true" object whose properties are to be inspected.  This
 *  makes it possible for objects to create filter objects which only permit access to certain properties.
 *  Generally speaking, such proxies (and indeed any object whose properties will be inspected) should be
 *  public, non-anonymous classes.  For example, imagine that you've got get/set methods on some property
 *  Foo, but you only want SimpleProperties to recognize the get method.  Furthermore you have another property
 *  called Bar that you want hidden entirely.  You can easily do this by making
 *  your class Proxiable and providing an inner-class proxy like this:
 *  
 * <pre><tt>
 * import sim.util.Proxiable;
 *
 * public class MyClass extends Proxiable
 *     {        
 *     int foo;
 *     float bar;
 *    
 *     public int getFoo() { return foo; }
 *     public void setFoo(int val) { foo = val; }
 *     public float getBar() { return bar; }
 *     public void setBar(float val) { bar = val; }
 *    
 *     public class MyProxy
 *         {
 *         public int getFoo() { return foo; }
 *         }
 *   
 *     public Object propertiesProxy() { return new MyProxy(); }
 *     }
 * </tt></pre>
 *
 *  <p>If the object provided to SimpleProperties is sim.util.Propertied, then SimpleProperties will not
 *  scan the object, but instead query the object for a Properties of its own, using the object's properties()
 *  method.  All accesses to the SimpleProperties will simply get routed to that Properties object instead.
 *  This is another filter approach which enables dynamically changing properties, or properties based on
 *  features other than get... and set... methods.
 */

public class SimpleProperties extends Properties implements java.io.Serializable
    {
    private static final long serialVersionUID = 1;

    ArrayList getMethods = new ArrayList();
    ArrayList setMethods = new ArrayList(); // if no setters, that corresponding spot will be null
    ArrayList domMethods = new ArrayList(); // if no domain, that corresponding spot will be null
    ArrayList desMethods = new ArrayList(); // if no description, that corresponding spot will be null
    ArrayList hideMethods = new ArrayList(); // if not hidden (or explicitly shown), that corresponding spot will be null
    ArrayList nameMethods = new ArrayList(); // if not hidden (or explicitly shown), that corresponding spot will be null
    Properties auxillary = null;  // if non-null, we use this properties instead
    
    /** Gathers all properties for the object, including ones defined in superclasses. 
        SimpleProperties will search the object for methods of the form <tt>public Object dom<i>Property</i>()</tt>
        which define the domain of the property.  The domFoo() and hideFoo() property extension methods are respected.
    */
    public SimpleProperties(Object o) { this(o,true,false,true); }
    
    /** Gathers all properties for the object, possibly including ones defined in superclasses. 
        If includeGetClass is true, then the Class property will be included. 
        SimpleProperties will search the object for methods of the form <tt>public Object dom<i>Property</i>()</tt>
        which define the domain of the property.  The domFoo() and hideFoo() property extension methods are respected.
                
        @deprecated Use the full form
    */
    public SimpleProperties(Object o, boolean includeSuperclasses, boolean includeGetClass)
        {
        this(o,includeSuperclasses,includeGetClass,true);
        }
    
    /** Gathers all properties for the object, possibly including ones defined in superclasses. 
        If includeGetClass is true, then the Class property will be included. If includeDomains is true, then
        SimpleProperties will search the object for methods of the form <tt>public Object dom<i>Property</i>()</tt>
        which define the domain of the property.  The domFoo() and hideFoo() property extension methods are respected
        if <tt>includeExtensions</tt> is true.
    */
    public SimpleProperties(Object o, boolean includeSuperclasses, boolean includeGetClass, boolean includeExtensions)
        {
        object = o;
        if (o!=null && o instanceof sim.util.Proxiable)
            object = ((sim.util.Proxiable)(o)).propertiesProxy();
        else if (o!=null && o instanceof sim.util.Propertied)
            auxillary = ((sim.util.Propertied)(o)).properties();
        generateProperties(includeSuperclasses,includeGetClass,includeExtensions);
        }
    
    void generateProperties(boolean includeSuperclasses, boolean includeGetClass, boolean includeExtensions)
        {
        if (object != null && auxillary == null) 
            {
            // generate the properties
            Class c = object.getClass();

            try
                {
                // handle integers
                if (object instanceof Long || object instanceof Integer || object instanceof Short || object instanceof Byte)
                    {
                    Method meth = c.getMethod("longValue", new Class[0]);
                    getMethods.add(meth);
                    setMethods.add(null);
                    domMethods.add(null);
                    hideMethods.add(null);
                    desMethods.add(null);
                    nameMethods.add(null);
                    }
                                                                                                                
                // handle other kinds of numbers
                else if (object instanceof Number)
                    {
                    Method meth = c.getMethod("doubleValue", new Class[0]);
                    getMethods.add(meth);
                    setMethods.add(null);
                    domMethods.add(null);
                    hideMethods.add(null);
                    desMethods.add(null);
                    nameMethods.add(null);
                    }
                                                                                                                
                // handle Booleans
                if (object instanceof Boolean)
                    {
                    Method meth = c.getMethod("booleanValue", new Class[0]);
                    getMethods.add(meth);
                    setMethods.add(null);
                    domMethods.add(null);
                    hideMethods.add(null);
                    desMethods.add(null);
                    nameMethods.add(null);
                    }
                                                                                                                
                // handle Strings
                if (object instanceof CharSequence)
                    {
                    Method meth = c.getMethod("toString", new Class[0]);
                    getMethods.add(meth);
                    setMethods.add(null);
                    domMethods.add(null);
                    hideMethods.add(null);
                    desMethods.add(null);
                    nameMethods.add(null);
                    }
                }
            catch (Exception e)         // just in case of RuntimeExceptions
                {
                e.printStackTrace();
                }

            // handle general properties
            Method[] m = (includeSuperclasses ? c.getMethods() : c.getDeclaredMethods());
            for(int x = 0 ; x < m.length; x++)
                {
                try  // we handle exceptions here by going to the next method and trying that one.
                    {
                    if (!("get".equals(m[x].getName())) && !("is".equals(m[x].getName())) &&  // "get()" and "is()" aren't properties
                        (m[x].getName().startsWith("get") || m[x].getName().startsWith("is"))) // corrrect syntax?
                        {
                        int modifier = m[x].getModifiers();
                        if ((includeGetClass || !m[x].getName().equals("getClass")) &&
                            m[x].getParameterTypes().length == 0 &&
                            Modifier.isPublic(modifier)) // no arguments, and public, non-abstract?
                            {
                            //// Add all properties...
                            Class returnType = m[x].getReturnType();
                            if (returnType!= Void.TYPE)
                                {
                                getMethods.add(m[x]);
                                setMethods.add(getWriteProperty(m[x],c));
                                domMethods.add(getDomain(m[x],c,includeExtensions));
                                hideMethods.add(getHidden(m[x], c, includeExtensions));
                                desMethods.add(getDescription(m[x],c,includeExtensions));
                                nameMethods.add(getName(m[x],c,includeExtensions));
                                                                                                                                         
                                // simple check for invalid Interval domains
                                int lastIndex = domMethods.size() - 1;
                                Object domain = getDomain(lastIndex);
                                if (returnType == Float.TYPE || returnType == Double.TYPE)
                                    {
                                    if (domain != null && domain instanceof Interval)
                                        {
                                        Interval interval = (Interval) domain;
                                        if (!interval.isDouble())
                                            {
                                            System.err.println("WARNING: Property is double or float valued, but the Interval provided for the property's domain is byte/short/integer/long valued: " + 
                                                getName(lastIndex) + " on Object " + object);
                                            // get rid of the domain
                                            domMethods.set(lastIndex, null);
                                            }
                                        }
                                    }
                                else if (returnType == Byte.TYPE || returnType == Short.TYPE || returnType == Integer.TYPE || returnType == Long.TYPE)
                                    {
                                    if (domain != null && domain instanceof Interval)
                                        {
                                        Interval interval = (Interval) domain;
                                        if (interval.isDouble())
                                            {
                                            System.err.println("WARNING: Property is byte/short/integer/long valued, but the Interval provided for the property's domain is double or float valued: " + 
                                                getName(lastIndex) + " on Object " + object);
                                            // get rid of the domain
                                            domMethods.set(lastIndex, null);
                                            }
                                        }
                                    }
                                else if (domain != null && domain instanceof Interval)
                                    {
                                    System.err.println("WARNING: Property is not a basic number type, but an Interval was provided for the property's domain: " + 
                                        getName(lastIndex) + " on Object " + object);
                                    // get rid of the domain
                                    domMethods.set(lastIndex, null);
                                    }
                                }
                            }
                        }
                    }
                catch(Exception e1)
                    {
                    e1.printStackTrace();  // try again though
                    }
                }
            }
        }
    
    /* If it exists, returns a method of the form 'public boolean hideFoo() { ...}'.  In this method the developer can declare
       whether or not he wants to hide this property.  If there is no such method, we must assume that the property is to be
       shown. */
    Method getHidden(Method m, Class c, boolean includeExtensions)
        {
        if (!includeExtensions) return null;
        try
            {
            if (m.getName().startsWith("get"))
                {
                Method m2 = c.getMethod("hide" + (m.getName().substring(3)), new Class[] { });
                if (m2.getReturnType() == Boolean.TYPE) return m2;
                }
            else if (m.getName().startsWith("is"))
                {
                Method m2 = c.getMethod("hide" + (m.getName().substring(2)), new Class[] { });
                if (m2.getReturnType() == Boolean.TYPE) return m2;
                }
            }
        catch (Exception e)
            {
            // couldn't find a domain
            }
        return null;
        }
    
    Method getWriteProperty(Method m, Class c)
        {
        try
            {
            if (m.getName().startsWith("get"))
                {
                return c.getMethod("set" + (m.getName().substring(3)), new Class[] { m.getReturnType() });
                }
            else if (m.getName().startsWith("is"))
                {
                return c.getMethod("set" + (m.getName().substring(2)), new Class[] { m.getReturnType() });
                }
            else return null;
            }
        catch (Exception e)             // just in case of RuntimeExceptions
            {
            // couldn't find a setter
            return null;
            }
        }
    
    Method getDomain(Method m, Class c, boolean includeExtensions)
        {
        if (!includeExtensions) return null;
        try
            {
            if (m.getName().startsWith("get"))
                {
                return c.getMethod("dom" + (m.getName().substring(3)), new Class[] {});
                }
            else if (m.getName().startsWith("is"))
                {
                return c.getMethod("dom" + (m.getName().substring(2)), new Class[] { });
                }
            else return null;
            }
        catch (Exception e)             // just in case of RuntimeExceptions
            {
            // couldn't find a domain
            return null;
            }
        }

    Method getDescription(Method m, Class c, boolean includeExtensions)
        {
        if (!includeExtensions) return null;
        try
            {
            if (m.getName().startsWith("get"))
                {
                return c.getMethod("des" + (m.getName().substring(3)), new Class[] {});
                }
            else if (m.getName().startsWith("is"))
                {
                return c.getMethod("des" + (m.getName().substring(2)), new Class[] { });
                }
            else return null;
            }
        catch (Exception e)             // just in case of RuntimeExceptions
            {
            // couldn't find a domain
            return null;
            }
        }
    
    Method getName(Method m, Class c, boolean includeExtensions)
        {
        if (!includeExtensions) return null;
        try
            {
            if (m.getName().startsWith("get"))
                {
                return c.getMethod("name" + (m.getName().substring(3)), new Class[] {});
                }
            else if (m.getName().startsWith("is"))
                {
                return c.getMethod("name" + (m.getName().substring(2)), new Class[] { });
                }
            else return null;
            }
        catch (Exception e)             // just in case of RuntimeExceptions
            {
            // couldn't find a domain
            return null;
            }
        }
    
    public boolean isVolatile() { if (auxillary!=null) return auxillary.isVolatile(); return false; }

    /** Returns the number of properties discovered */
    public int numProperties()
        {
        if (auxillary!=null) return auxillary.numProperties();
        return getMethods.size();
        }

    /** Returns the name of the given property.
        Returns null if the index is out of the range [0 ... numProperties() - 1 ]*/
    public String getName(int index)
        {
        if (auxillary!=null) return auxillary.getName(index);
        if (index < 0 || index >= numProperties()) return null;

        try
            {
            if (nameMethods.get(index) != null) 
                return (String)(((Method)(nameMethods.get(index))).invoke(object, new Object[0]));
            }
        catch (Exception e)
            {
            e.printStackTrace();
            return null;
            }

        String name = ((Method)(getMethods.get(index))).getName();
        if (name.startsWith("is"))
            return name.substring(2);
        else if (name.equals("longValue"))   // Integers of various kinds
            return "Value";
        else if (name.equals("doubleValue"))   // Other Numbers
            return "Value";
        else if (name.equals("booleanValue"))   // Booleans
            return "Value";
        else if (name.equals("toString"))   // Strings, StringBuffers
            return "Value";
        else return name.substring(3);  // "get", "set"
        }
        
    /** Returns whether or not the property can be written as well as read
        Returns false if the index is out of the range [0 ... numProperties() - 1 ]*/
    public boolean isReadWrite(int index)
        {
        if (auxillary!=null) return auxillary.isReadWrite(index);
        if (index < 0 || index >= numProperties()) return false;
        if (isComposite(index)) return false;
        return (setMethods.get(index)!=null);
        }

    /** Returns the return type of the property (see the TYPE_... values)
        Returns -1 if the index is out of the range [0 ... numProperties() - 1 ]*/
    public Class getType(int index)
        {
        if (auxillary!=null) return auxillary.getType(index);
        if (index < 0 || index >= numProperties()) return null;
        Class returnType = ((Method)(getMethods.get(index))).getReturnType();

        return getTypeConversion(returnType);
        }

    /** Returns the current value of the property.  Simple values (byte, int, etc.)
        are boxed (into Byte, Integer, etc.).
        Returns null if an error occurs or if the index is out of the range [0 ... numProperties() - 1 ]*/
    public Object getValue(int index)
        {
        if (auxillary!=null) return auxillary.getValue(index);
        if (index < 0 || index >= numProperties()) return null;
        try
            {
            return ((Method)(getMethods.get(index))).invoke(object, new Object[0]);
            }
        catch (Exception e)
            {
            e.printStackTrace();
            return null;
            }
        }
    
    protected Object _setValue(int index, Object value)
        {
        if (auxillary!=null) return auxillary.setValue(index,value);  // I think this is right
        try
            {
            if (setMethods.get(index) == null) return null;
            ((Method)(setMethods.get(index))).invoke(object, new Object[] { value });
            return getValue(index);
            }
        catch (Exception e)
            {
            e.printStackTrace();
            return null;
            }
        }

    public String getDescription(int index)
        {
        if (auxillary!=null) return auxillary.getDescription(index);
        if (index < 0 || index >= numProperties()) return null;
        try
            {
            if (desMethods.get(index) == null) return null;
            return (String)(((Method)(desMethods.get(index))).invoke(object, new Object[0]));
            }
        catch (Exception e)
            {
            e.printStackTrace();
            return null;
            }
        }

    public Object getDomain(int index)
        {
        if (auxillary!=null) return auxillary.getDomain(index);
        if (index < 0 || index >= numProperties()) return null;
        try
            {
            if (domMethods.get(index) == null) return null;
            return ((Method)(domMethods.get(index))).invoke(object, new Object[0]);
            }
        catch (Exception e)
            {
            e.printStackTrace();
            return null;
            }
        }

    public boolean isHidden(int index)
        {
        if (auxillary!=null) return auxillary.isHidden(index);
        if (index < 0 || index >= numProperties()) return false;
        try
            {
            if (hideMethods.get(index) == null) return false;
            return ((Boolean)((Method)(hideMethods.get(index))).invoke(object, new Object[0])).booleanValue();
            }
        catch (Exception e)
            {
            e.printStackTrace();
            return false;
            }
        }





    // can only be called internally 
    SimpleProperties() { }
        
    /** Produces a new SimpleProperties which is a strict subset of the
        existing SimpleProperties where the properties found in propertyNamesToRetain 
        are retained and other properties are removed.  The existing SimpleProperties
        must not use an auxillary.  The new SimpleProperties has the same volatility
        and object as the original.
    */
    public SimpleProperties getPropertiesSubset(String[] propertyNames, boolean retain)
        {
        if (auxillary != null)
            throw new RuntimeException("Properties may not be reduced if the SimpleProperties has an auxillary.");
                
        SimpleProperties props = new SimpleProperties()
            {
            public boolean isVolatile() { return SimpleProperties.this.isVolatile(); }
            };
        props.object = object;
        
        // normally it'd make more sense to put propertyNames in the outer loop
        // but this allows us to search for both retention and removal.
        
        // This is a little ugly
        boolean[] found = new boolean[propertyNames.length]; 
        for(int index = 0; index < numProperties(); index++)
            {
            int i;
            for(i = 0; i < propertyNames.length; i++)
                {
                if (getName(index).equals(propertyNames[i]))
                    { found[i] = true; break; }
                }
                
            if ((i < propertyNames.length && retain) ||
                (i >= propertyNames.length && !retain))  // add this one
                {
                props.getMethods.add(getMethods.get(index));
                props.setMethods.add(setMethods.get(index));
                props.domMethods.add(domMethods.get(index));
                props.desMethods.add(desMethods.get(index));
                props.hideMethods.add(hideMethods.get(index));
                props.nameMethods.add(nameMethods.get(index));
                }
            }
                
        if (retain)
            {
            for(int i = 0; i < found.length; i++)
                {
                if (!found[i])
                    throw new RuntimeException("Unknown property name " + propertyNames[i] + " in object " + object);
                }
            }

        return props;
        }
    
    public String toString()
        {
        String s = "{ ";
        for(int i = 0; i < numProperties(); i++)
            {
            if (i > 0) s += ", ";
            s += getName(i);
            }
        s += "}";
        return s;
        }
    
    }
