/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util;
import java.text.*;
import java.util.*;

/**
   The abstract superclass of Property inspectors.  Such a beast inspects an object and returns a list of variables for which there are get and possibly set methods.
    
   <P>There are two such inspectors: SimpleProperties (inspects objects for their slots) and CollectionProperties (inspects Collections, Maps, and arrays for their contents).  The easiest way to get an appropriate Properties subclass instance is to simply call the static method <b>Properties.getProperties(<i>object to inspect</i>, .... )</b>.  See the SimpleProperties and CollectionProperties classes for specifics about how they define what a Property is.  SimpleProperties in particular will beinteresting..
   
   <p>Property inspectors enumerate the Properties in their provided object.  SimpleProperties will enumerate each of the slots; and CollectionProperties will enumerate the elements in the corresponding array, Map, Collection, etc.  You get the number of Properties with numProperties().  Properties have the following features:
   
   <ul>
   <li><b>Property Name</b> (Readable).  A string, usually the name of the instance variable in the object, a stringified number ("1", "203", etc.) for Collections or Arrays, or a stringified version of the key in a Map.
   <li><b>Property Value</b> (Readable and possibly Writable).  An object representing the value of the property.  Numbers etc. are wrapped in their corresponding wrapper classes.
   <li><b>Writability</b> (Readable).
   <li><b>Type</b> (Readable).  The type of the object as a Class.  ints return Integer.TYPE, etc.
   <li><b>Composite Nature</b> (Readable).  Whether or not the Property is composite or atomic.  Atomic types are int, float, etc., plus String.
   <li><b>Domain</b> (Readable).  Whether or not the object has defined a Domain (a set or range of legal values) for the Property.  Domains allow a GUI to set up sliders, pull-down menus and combo-boxes, etc.
   <li><b>Description</b> (Readable).  A string describing an extended description of the property.  Returns null (the default) if there is no extended description. 
   <li><b>Hiddenness</b> (Readable).  A boolean indicating whether the property should be shown to the user. 
   </ul>
*/

public abstract class Properties implements java.io.Serializable
    {
    private static final long serialVersionUID = 1;

    /** Returns a Properties object for the given object.  If the object is an array, Map, Indexed, or Collection,
        then it will be treated using CollectionProperties.  Otherwise it will be
        treated using SimpleProperties.  The returned SimpleProperties will include superclasses properties.
        The Class property will NOT be included.
        The domFoo() and hideFoo() property extension methods are respected.
        Domains will always be produced according to the rules in the comments for getDomain(index) below.
    */
    public static Properties getProperties(Object object)
        {
        return getProperties(object, true, true, false ,true);
        }

    /** Returns a Properties object for the given object.  
        If expandCollections is true, then if object is a Map, Indexed, or Collection,
        then it will be treated using CollectionProperties.  Otherwise it will be
        treated using SimpleProperties.   Arrays are always treated using CollectionProperties. 
        If includeSuperclasses is true, then any SimpleProperties will include superclasses.
        If includeGetClass is true, then the Class property will be included.
        The domFoo() and hideFoo() property extension methods are respected.
    
        @deprecated use the full version
    */
    public static Properties getProperties(Object object, boolean expandCollections, boolean includeSuperclasses, boolean includeGetClass)
        {
        return getProperties(object,expandCollections,includeSuperclasses,includeGetClass,true);
        }
        
    /** Returns a Properties object for the given object.  
        If expandCollections is true, then if object is a Map, Indexed, or Collection,
        then it will be treated using CollectionProperties.  Otherwise it will be
        treated using SimpleProperties.   Arrays are always treated using CollectionProperties. 
        If includeSuperclasses is true, then any SimpleProperties will include superclasses.
        If includeGetClass is true, then the Class property will be included.
        The domFoo() and hideFoo() property extension methods are respected if <tt>includeExtensions</tt> is true.
    */
    public static Properties getProperties(Object object, boolean expandCollections, boolean includeSuperclasses, boolean includeGetClass, boolean includeExtensions)
        {
        if (object == null) return new SimpleProperties(object, includeSuperclasses, includeGetClass, includeExtensions);
        Class c = object.getClass();
        if (c.isArray()) return new CollectionProperties(object);
        else if (expandCollections && (Collection.class.isAssignableFrom(c) ||
                Indexed.class.isAssignableFrom(c) ||
                Map.class.isAssignableFrom(c)))
            return new CollectionProperties(object);
        else return new SimpleProperties(object, includeSuperclasses, includeGetClass, includeExtensions);
        }

    protected Object object;
    /** Returns the original object from which the properties are extracted */
    public Object getObject() { return object; }
    
    /** Returns true if the number or order of properties could change at any time */
    public abstract boolean isVolatile();
    
    /** Returns the number of properties discovered in the object. */
    public abstract int numProperties();

    /** Returns the value of the property at the given index. */
    public abstract Object getValue(int index);
    
    /** Returns the domain of the property at the given index. 
        Domains are defined by methods of the form <tt>public Object dom<i>Property</i>()</tt>
        and should generally take one of three forms:
        <dl>
        <dt><tt>null</tt>
        <dd>no domain (domain is infinite).
        <dt>An array of elements
        <dd>the domain consists solely of those elements.
        <dt>A <tt>sim.util.Interval</tt>
        <dd>the domain is an inclusive (closed) numerical range defined by the Interval.
        If the Interval returns Longs, then the domain is considered to be integral; else
        it is considered to be real-valued.
        </dl>
    */
    public Object getDomain(int index) { return null; }

    /** Returns the description of the property at the given index. 
        Descriptions are defined by methods of the form <tt>des<i>Property</i>()</tt>
        and are either <tt>null</tt> (no description), or are Strings, possibly
        including HTML data.  The primary function of description methods is to provide
        tooltip information for widgets describing the Property.
    */
    public String getDescription(int index) { return null; }

    /** Returns true if the property at the given index is both readable and writable (as opposed to read-only). */
    public abstract boolean isReadWrite(int index);

    /** Returns true if the property at the given index is a "Composite" object, meaning it's not a primitive type (double, int, etc.) nor a String. */
    public boolean isComposite(int index)
        {
        if (index < 0 || index > numProperties()) return false;
        Class type = getTypeConversion(getType(index));
        return !(type.isPrimitive() || type == String.class);
        }

    /** Returns true if the class requested that this property be hidden from the user.  By default, false. */
    public boolean isHidden(int index) { return false; }
    
    /** Returns the name of the property at the given index. */
    public abstract String getName(int index);

    /** Returns the Class (or for primitive objects, the primitive TYPE) of the property at the given index. */
    public abstract Class getType(int index);

    protected abstract Object _setValue(int index, Object value);

    /** Sets the current value of the property.  Simple values (byte, int, etc.)
        must be boxed (into Byte, Integer, etc.).  Then returns the current (hopefully changed) value of the property.
        Returns null if an error occurs or if the index is out of the range [0 ... numProperties() - 1 ]*/
    public Object setValue(int index, Object value)
        {
        // so we can also have a setValue (int, String) which calls US
        return _setValue(index, value);
        }
        
    /** Sets the current value of the property to the value parsed from the given string.
        Then returns the current (hopefully changed) value of the property.
        Returns null if an error occurs or if the index is out of the range [0 ... numProperties() - 1 ]*/
    public Object setValue(int index, String value)
        {
        try
            {
            Class type = getType(index);
            if ( type == Boolean.TYPE ) return _setValue(index,Boolean.valueOf(value));
            else if ( type == Byte.TYPE ) 
                {
                try { return _setValue(index,Byte.valueOf(value)); }
                catch (NumberFormatException e) // try again for x.0 stuff
                    { 
                    double d = Double.parseDouble(value); 
                    byte b = (byte) d; 
                    if (b==d) return _setValue(index,Byte.valueOf(b)); 
                    else throw e; 
                    }
                }
            else if ( type == Short.TYPE )
                {
                try { return _setValue(index,Short.valueOf(value)); }
                catch (NumberFormatException e) // try again for x.0 stuff
                    { 
                    double d = Double.parseDouble(value); 
                    short b = (short) d; 
                    if (b==d) return _setValue(index,Short.valueOf(b)); 
                    else throw e; 
                    }
                }
            else if ( type == Integer.TYPE )
                {
                try { return _setValue(index,Integer.valueOf(value)); }
                catch (NumberFormatException e) // try again for x.0 stuff
                    { 
                    double d = Double.parseDouble(value); 
                    int b = (int) d; 
                    if (b==d) return _setValue(index,Integer.valueOf(b)); 
                    else throw e; 
                    }
                }
            else if ( type == Long.TYPE )
                {
                try { return _setValue(index,Long.valueOf(value)); }
                catch (NumberFormatException e) // try again for x.0 stuff
                    { 
                    double d = Double.parseDouble(value); 
                    long b = (long) d; 
                    if (b==d) return _setValue(index,Long.valueOf(b)); 
                    else throw e; 
                    }
                }
            else if ( type == Float.TYPE ) return _setValue(index,Float.valueOf(value));
            else if ( type == Double.TYPE ) return _setValue(index,betterDoubleValueOf(value));
            else if ( type == Character.TYPE ) return _setValue(index,new Character(value.charAt(0)));
            else if ( type == String.class ) return _setValue(index,value);
            else return null;
            }
        catch (Exception e)
            {
            e.printStackTrace();
            return null;
            }
        }
    
    NumberFormat format = NumberFormat.getInstance();
    /**
     * Replaces <code>Double.valueOf()</code> for the GUI.
     * 
     * <p>
     * For specific locals, the separator char may be other than <code>'.'</code> (like <code>','</code> for instance),
     * and in these cases a simple <code>Double.valueOf()</code> code results in a <code>NumberFormatException</code>.
     * </p>
     * 
     * <p>
     * This cause an error when there is a double parameter with a <code>domFoo()</code> function returning an
     * <code>Interval</code> object and the systems current local setting is non-US.
     * </p>
     * 
     * @param s the string object to transform into a <code>Double</code>
     * @return a <code>Double</code> instance representing <code>s</code>.
     * @throws ParseException if the specified <code>s</code> string cannot be parsed as a <code>Double</code>
     */
    Double betterDoubleValueOf(String s) throws ParseException
        {
        return Double.valueOf( format.parse(s).doubleValue() );
        }
    
    /*
      f = new JFrame();
      h = new sim.app.heatbugs.HeatBugsWithUI();
      c = new sim.display.Console(h);
      i = new sim.portrayal.SimpleInspector(new Foo(),h,"Yo, Mama!");
      f.getContentPane().setLayout(new BorderLayout());
      f.getContentPane().add(i,BorderLayout.CENTER);
      f.pack();
      f.show();
    */
    
    // converts boxed type classes into simple types
    protected Class getTypeConversion(Class type)
        {
        if (type==Boolean.class || type==Boolean.TYPE)
            return Boolean.TYPE;
        else if (type==Byte.class || type==Byte.TYPE)
            return Byte.TYPE;
        else if (type==Short.class || type==Short.TYPE)
            return Short.TYPE;
        else if (type==Integer.class || type==Integer.TYPE)
            return Integer.TYPE;
        else if (type==Long.class || type==Long.TYPE)
            return Long.TYPE;
        else if (type==Float.class || type==Float.TYPE)
            return Float.TYPE;
        else if (type==Double.class || type==Double.TYPE)
            return Double.TYPE;
        else if (type==Character.class || type==Character.TYPE)
            return Character.TYPE;
        else return type;
        }

    /** Call this to get a prettier print-name for an object -- converting arrays to a nicer format, for example. */
    public String betterToString(Object obj)
        {
        if (obj == null) return "null";
        Class c = obj.getClass();
        if (c.isArray()) return typeToName(c) + "@" + Integer.toHexString(obj.hashCode());
        else return "" + obj;
        }
        
    protected String typeToName( Class type )
        {
        if ( type == null ) return null;

        if ( type.isPrimitive() )
            {
            return type.toString();
            }
        else if ( type == String.class )
            {
            return "String";
            }
        else if ( type.isArray() )
            {
            Class componentType = type.getComponentType();
            
            Class convertedComponentType = getTypeConversion(componentType);
            return typeToName(convertedComponentType) + "[]";
            }
        else
            return null;
        }
              
    }
