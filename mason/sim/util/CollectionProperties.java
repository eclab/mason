/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util;
import java.util.*;
import java.lang.reflect.*;

/**
   A simple class for examining the slots of Maps, Collections, Indexed, and arrays as if the slots were Java Bean Properties.  Beware that Maps and non-List Collections perform O(n) scans just to give you the property you were looking for.  Lists, Indexed, and arrays are always O(1).  

   <p>You can easily create a CollectionProperties object by passing in the appropriate object to examine.  From then on, you can get the number of "Properties" (in this case, objects in the set), and retrieve the "Name" and the value of each Property, plus other information.

   <p>
   This class allows you to set and get properties on the object via boxing the property (java.lang.Integer
   for int, for example).  You can also pass in a String, and SimpleProperties will parse the appropriate
   value out of the string automatically without you having to bother checking the type.  arrays, Maps, Indexed, and Lists can have property values set.  Other Collections cannot have values set.

   <p>If any errors arise from generating the properties, setting them, or getting their values, then typically null is returned.
*/

public class CollectionProperties extends Properties
    {
    Collection collection;
    Map map;
    Indexed indexed;
    boolean isVolatile;
    
    
    public boolean isVolatile()
        {
        return isVolatile;
        }

    /** Object can be a Collection, a List, a Map, an Indexed, or an array. */
    public CollectionProperties(Object o)
        {
        // we have to have one unified constructor rather than separate ones because
        // constructor polymorphism doesn't consider interfaces.  I didn't know that!
        // hence the big if/then statement here.
        if ( o == null ) throw new NullPointerException();
        else if (o instanceof Indexed) buildCollectionProperties((Indexed)o);
        else if (o instanceof List) buildCollectionProperties((List)o);
        else if (o instanceof Map) buildCollectionProperties((Map)o);
        else if (o instanceof Collection) buildCollectionProperties((Collection)o);
        else if (o.getClass().isArray()) buildCollectionPropertiesForArray(o);
        else throw new IllegalArgumentException("Invalid type for collection properties: " + o);
        object = o;
        }

    void buildCollectionProperties(final Collection c)
        {
        isVolatile = true;
        collection = c;
        object = c;
        }
    
    void buildCollectionProperties(final List list)
        {
        isVolatile = true;
        indexed = new Indexed()
            {
            public Class componentType() { return null; }
            public int size() { return list.size(); }
            public Object setValue(int index, Object value) { return list.set(index,value); }
            public Object getValue(int index) { return list.get(index); }
            };
        object = list;
        }

    void buildCollectionProperties(Map m)
        {
        isVolatile = true;
        map = m;
        object = m;
        }
    
    void buildCollectionProperties(Indexed i)
        {
        isVolatile = true;
        indexed = i;
        object = i;
        }

    void buildCollectionPropertiesForArray(final Object o) // for arrays
        {
        isVolatile = false;  // fixed in size and order
        
        indexed = new Indexed()
            {
            public Class componentType() { return o.getClass().getComponentType(); }
            public int size() { return Array.getLength(o); }
            public Object setValue(int index, Object value)
                {
                Object oldVal = getValue(index);
                Array.set(o,index,value);
                return oldVal;
                }
            public Object getValue(int index) { return Array.get(o,index); }
            };
        
        object = o;
        }
    
    public int numProperties()
        {
        if (indexed!=null) return indexed.size();
        else if (collection!=null) return collection.size();
        else return map.size();
        }

    // only for maps and collections
    Iterator valueIterator()
        {
        if (collection !=null) return collection.iterator();
        else
            {
            Set s = map.entrySet();
            final Iterator i = s.iterator();
            return new Iterator()
                {
                public boolean hasNext() { return i.hasNext(); }
                public Object next() { return ((Map.Entry)(i.next())).getValue(); }
                public void remove() { throw new UnsupportedOperationException(
                        "Cannot remove from a CollectionProperties Iterator"); }
                };
            }
        }

    public Object getValue(int index)
        {
        if (index < 0 || index > numProperties()) return null;

        if (indexed!=null) return indexed.getValue(index);  // O(1)
        else  // O(n)
            {
            Iterator i = valueIterator();
            Object obj = null;
            for(int x=0;x<=index;x++)  // yes, it's <=
                {
                if (!i.hasNext()) return null;
                obj = i.next();
                }
            return obj;
            }
        }

    public boolean isReadWrite(int index)
        {
        if (index < 0 || index > numProperties()) return false;

        if (collection!=null) return false;  // collections are not modifiable -- they can't be referenced

        Class type = getTypeConversion(getType(index));
        Object obj = getValue(index);
        if (obj!=null)
            {
            if (!type.isAssignableFrom(obj.getClass())) // uh oh, violated base type
                return false;
            }
        return !isComposite(index);
        }

    public String getName(int index)
        {
        if (index < 0 || index > numProperties()) return null;
        
        if (map!=null)
            {
            Iterator i = map.entrySet().iterator();
            Object obj = null;
            for(int x=0;x<=index;x++)  // yes, it's <=
                {
                if (!i.hasNext()) return null;
                obj = i.next();
                }
            return "" + ((Map.Entry)(obj)).getKey();
            }
        else if (collection!= null)
            {
            return "Member";
            }
        else
            {
            return "" + index;
            }
        }

    public Class getType(int index)
        {
        if (index < 0 || index > numProperties()) return null;
        if (indexed!=null)
            {
            Class type = indexed.componentType();
            if (type!=null) return type;
            }
        Object obj = getValue(index);
        if (obj==null) return Object.class;
        return obj.getClass();
        }

    protected Object _setValue(int index, Object value)
        {
        if (index < 0 || index > numProperties()) return null;

        if (indexed!=null) indexed.setValue(index,value);  // O(1)
        else if (map!=null)
            {
            Iterator i = map.entrySet().iterator();
            Object obj = null;
            for(int x=0;x<=index;x++)  // yes, it's <=
                {
                if (!i.hasNext()) return null;
                obj = i.next();
                }
            map.put(((Map.Entry)(obj)).getKey(), value);
            }
        // can't set collections
        return getValue(index);
        }

    }
