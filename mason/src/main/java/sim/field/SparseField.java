/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field;
import sim.util.*;
import java.util.*;

/** While it has no abstract members, SparseField is explicitly an abstract superclass of various sparse
    field objects.  It specifies a many-to-one relationship between objects and locations.
    It contains two hash tables and a bag, resulting in fairly speedy, O(1) searches for
    all objects at a location, all objects in the entire field, changes in location for
    an object, and addition or removal of an object.
    
    <p>There are two flags you can set to trade memory for speed.  These flags are here because
    often in a Sparse Field, you'd load a lot of things into one location, then delete all
    of them, but the large Bag which held them is still hanging there.  Thus:
    <tt>removeEmptyBags</tt> will garbage-collect any bags in the field which have been completely
    emptied (except for the allObjects bag).  And <tt>replaceLargeBags</tt> will replace large
    bags (over 32 in size) with smaller ones when the bag's contents drop below 1/4 the bag
    size.  The smaller bags will be twice the size of the contents (rather than 4 times or more).
    
    <p>
    To create a SparseField, you need to specify exactly what type a "location" object is.
    You do this by overriding the setObjectLocation() method and creating a method called
    getObjectLocation(), which returns objects of that class (by calling getRawObjectLocation()
    and casting the result into the proper type).  See the Example Usage below.    
    
    <p><b>Warning About Hashing.</b>  Java's hashing method is broken in an important way.  
    One can override the hashCode() and equals()
    methods of an object so that they hash by the value of an object rather than just the 
    pointer to it.  But if this is done, then if
    you use this object as a key in a hash table, then <i>change</i> those values in the 
    object, it will break the hash table -- the key
    and the object hashed by it will both be lost in the hashtable, unable to be accessed 
    or removed from it.  The moral of the story is:
    do not override hashCode() and equals() to hash by value unless your object is 
    <i>immutable</i> -- its values cannot be changed.  This
    is the case, for example, with Strings, which hash by value but cannot be modified.  
    It's also the case with Int2D, Int3D, Double2D,
    and Double3D, as well as Double, Integer, etc.  Some of Sun's own objects are broken 
    in this respect: Point, Point2D, etc. are both mutable <i>and</i> hashed by value.
    
    <p>This affects you in the following way.  While SparseField can specify any Object as a 
    "location", generally speaking, subclasses should only permit immutable objects as locations 
    (Int2D, Integer, String, etc.), or warn the user to never modify the location object he
    provides as a key.
    
    <p><b>Computational Complexity.</b>  Adding a new object to a location is O(1).  Changing
    an object's location, or removing the object, is O(M), where M is the number of objects
    currently located at the object's old location.  Scanning through all objects is O(N) and fast,
    where N is the number of objects total -- just scan through the allObjects Bag.  
    Scanning through all stored locations is potentially slow, probably O(H), where H is the 
    number of hash table buckets in the objectHash hash table -- you'd have to get a hash table 
    iterator and iterate through it.  Removing all objects at a given location is O(O), where O
    is the number of objects at that location.  Clearing the hash table is O(1) discounting GC.

    <p><b>Example Usage.</b>  
    Here is an example of a simple subclass which allows locations to be positive, non-zero integers:
    
    *    <pre><tt>
    *    public class PositiveIntegerSparseField extends SparseField
    *        {
    *        // note we return an Integer, not an Object.  If the user wants
    *        // an abstract supermethod for all SparseFields, he can use
    *        // getRawObjectLocation instead.  Java's a pain sometimes
    *            
    *        public Integer getObjectLocation(Object obj)
    *            {
    *            return (Integer) super.getRawObjectLocation(obj);
    *            }
    *
    *        // note we explicitly state that the location has to be an integer
    *
    *        public boolean setObjectLocation(Object obj, final Integer location)
    *            {
    *            if (location.intValue() > 0)  // it's a valid location
    *            return super.setObjectLocation(obj, location);
    *            else return false;
    *            }
    *        }
    *    </tt>
    *    </pre>
    */

public abstract class SparseField implements java.io.Serializable
    {
    private static final long serialVersionUID = 1;

    /** Should we remove bags in the field if they have been emptied, and let them GC, or should
        we keep them around?   This doesn't include the allObjects bag. */
    public boolean removeEmptyBags = true;
    
    /** When a bag drops to one quarter capacity, should we replace it with a new bag?  This doesn't include the allObjects bag. */
    public boolean replaceLargeBags = true;

    /** The size of an initial bag */
    public static final int INITIAL_BAG_SIZE = 16;

    /** No bags smaller than this size will be replaced regardless of the setting of <tt>replaceLargeBags</tt> */
    public static final int MIN_BAG_SIZE = 32;

    /** A bag must be larger than its contents by this ratio to be replaced <tt>replaceLargeBags</tt> is true*/
    public static final int LARGE_BAG_RATIO = 4;
    
    /** A bag to be replaced will be shrunk to this ratio if <tt>replaceLargeBags</tt> is true*/
    public static final int REPLACEMENT_BAG_RATIO = 2;

    /** LocationAndIndex objects (locations and indexes into the allObjects array) hashed by Object.  Ideally you would
        store only immutable or hash-by-pointer objects, el se they'll get lost in the HashMap. */
    public Map locationAndIndexHash = buildMap(ANY_SIZE);

    /** Bags of objects hashed by location.  Do not rely on these bags always being the same objects. */
    public Map objectHash = buildMap(ANY_SIZE);

    /** All the objects in the sparse field.  For fast scans.  Do not rely on this bag always being the same object. */
    public Bag allObjects = new Bag();
    
    /** Pass this into buildMap to indicate that it should make a map of any size it likes. */
    public static final int ANY_SIZE = 0;
    /** Creates a Map which is a copy of another. By default, HashMap is used. */
    public Map buildMap(Map other) { return new HashMap(other); }
    /** Creates a map of the provided size (or any size it likes if ANY_SIZE is passed in).  By default, HashMap is used. */
    public Map buildMap(int size) 
        {
        if (size <= ANY_SIZE) return new HashMap();
        else return new HashMap(size);
        }

    protected SparseField() { }
        
    protected SparseField(SparseField other)
        {
        removeEmptyBags = other.removeEmptyBags;
        replaceLargeBags = other.replaceLargeBags;
        locationAndIndexHash = buildMap(other.locationAndIndexHash);
        objectHash = buildMap(other.objectHash);
        allObjects = new Bag(other.allObjects);
        }
        
    /** Returns the index of the object in the allObjects Bag, if the object exists, else returns -1. */
    public int getObjectIndex(final Object obj)
        {
        LocationAndIndex lai = ((LocationAndIndex)(locationAndIndexHash.get(obj)));
        if (lai == null) return -1;
        return lai.index;
        }

    /** Returns true if the object is in the field. */
    public boolean exists(final Object obj)
        {
        return (getRawObjectLocation(obj) != null);
        }
    
    /** Returns the number of elements in the field */
    public int size() { return allObjects.size(); }
        
    /** Get the location of the provided object, or null if the object does not exist.  
        Subclasses should create a <b> getObjectLocation(</b><i>Object obj</i><b>) </b> method which 
        returns a location type appropriate for your kind of Sparse Field. */

    protected final Object getRawObjectLocation(final Object obj)
        {
        LocationAndIndex lai = ((LocationAndIndex)(locationAndIndexHash.get(obj)));
        if (lai == null) return null;
        assert sim.util.LocationLog.it(this, lai.location);
        return lai.location;
        }
    
    /** Returns the number of objects at a given location. */
    public final int numObjectsAtLocation(final Object location)
        {
        final Bag b = (Bag)(objectHash.get(location));
        if (b==null) return 0;
        assert sim.util.LocationLog.it(this, location);
        return b.numObjs;
        }
    
    /** Returns a bag containing all the objects at a given location, or null when there are no objects at the location.
        You should NOT MODIFY THIS BAG. This is the actual container bag, and modifying it will almost certainly break
        the Sparse Field object.   If you want to modify the bag, make a copy and modify the copy instead,
        using something along the lines of <b> new Bag(<i>foo</i>.getObjectsAtLocation(<i>location</i>)) </b>.
        Furthermore, changing values in the Sparse Field may result in a different bag being used -- so you should
        not rely on this bag staying valid.  The default implementation of this method simply calls getRawObjectsAtLocation(),
        but you may need to override it for more custom functionality (which is rare).
    */
    public Bag getObjectsAtLocation(final Object location)
        {
        return getRawObjectsAtLocation(location);
        }

    /** This method is called by getObjectsAtLocation(location) so you can override getObjectsAtLocation() to
        customize it in certain ways (which is rare).  All internal methods in SparseField instead call getRawObjectsAtLocation().
        Returns a bag containing all the objects at a given location, or null when there are no objects at the location.
        You should NOT MODIFY THIS BAG. This is the actual container bag, and modifying it will almost certainly break
        the Sparse Field object.   If you want to modify the bag, make a copy and modify the copy instead,
        using something along the lines of <b> new Bag(<i>foo</i>.getObjectsAtLocation(<i>location</i>)) </b>.
        Furthermore, changing values in the Sparse Field may result in a different bag being used -- so you should
        not rely on this bag staying valid.
    */
    protected final Bag getRawObjectsAtLocation(final Object location)
        {
        Bag b = (Bag)(objectHash.get(location));
        if (b==null) return null;
        if (b.numObjs == 0) return null;
        assert sim.util.LocationLog.it(this, location);
        return b;
        }
                
    /** Returns a bag containing all the objects at the same location as a given object, including the object itself, 
        or null if the object is not in the Field.
        You should NOT MODIFY THIS BAG. This is the actual container bag, and modifying it will almost certainly break
        the Sparse Field object.   If you want to modify the bag, make a copy and modify the copy instead,
        using something along the lines of <b> new Bag(<i>foo</i>.getObjectsAtLocation(<i>location</i>)) </b>.
        Furthermore, changing values in the Sparse Field may result in a different bag being used -- so you should
        not rely on this bag staying valid.
    */
    public Bag getObjectsAtLocationOfObject(final Object obj)
        {
        LocationAndIndex lai = ((LocationAndIndex)(locationAndIndexHash.get(obj)));
        if (lai == null) return null;
        assert sim.util.LocationLog.it(this, lai.location);
        return lai.otherObjectsAtLocation;  // should be non-null
        }
                        
    /** Returns the number of objects at the same location as a given object, including the object itself, or 0 if the object
        is not in the SparseField.
    */
    public int numObjectsAtLocationOfObject(final Object obj)
        {
        LocationAndIndex lai = ((LocationAndIndex)(locationAndIndexHash.get(obj)));
        if (lai == null) return 0;
        assert sim.util.LocationLog.it(this, lai.location);
        return lai.otherObjectsAtLocation.numObjs;
        }

    /** Removes objects at the given location, and returns a bag of them, or null of no objects are at that location.
        The Bag may be empty, or null, if there were no objects at that location.  You can freely modify this bag. */
    public Bag removeObjectsAtLocation(final Object location)
        {
        Bag objs = (Bag)objectHash.remove(location);
        if (objs!=null)
            for(int j=0;j<objs.numObjs;j++)
                {
                // remove location
                LocationAndIndex lai = (LocationAndIndex)(locationAndIndexHash.remove(objs.objs[j]));
                // remove object from allobjects bag
                assert sim.util.LocationLog.it(this, lai.location);
                allObjects.remove(lai.index);
                if (allObjects.numObjs > lai.index)    // update the index of the guy who just got moved
                    ((LocationAndIndex)(locationAndIndexHash.get(allObjects.objs[lai.index]))).index = lai.index;
                }
        return objs;
        }
    
    /** Deletes everything, returning all the objects as a Bag (which you can freely use and modify).
        If you need the Bag, then this is a useful method -- otherwise it might in fact be faster to
        just make a brand new Sparse Field and let the garbage collector do its magic. */
    public Bag clear()
        {
        locationAndIndexHash = buildMap(ANY_SIZE);
        objectHash = buildMap(ANY_SIZE);
        Bag retval = allObjects;
        allObjects = new Bag();
        return retval;
        }
    
    /** Removes an object if it exists.  Returns its location, or null if the object didn't exist. */
    public Object remove(final Object obj)
        {
        // remove from locationAndIndexHash
        LocationAndIndex lai = (LocationAndIndex)(locationAndIndexHash.remove(obj));
        if (lai!=null)
            {
            // remove from objectHash
            Bag objs = (Bag)(objectHash.get(lai.location));
            objs.remove(obj);
            
            ///// You can comment this out for speed -- but it doesn't look like it has much effect.
            ///// Commenting it would mean that big empty bags can be sitting around in the hashtable
            int objsNumObjs = objs.numObjs;  // a little efficiency
            if (removeEmptyBags && objsNumObjs==0)
                objectHash.remove(lai.location);  // let Bag GC
            else if (replaceLargeBags && objsNumObjs >= MIN_BAG_SIZE && objsNumObjs * LARGE_BAG_RATIO <= objs.objs.length)  // bag not full enough
                objs.shrink(objsNumObjs * REPLACEMENT_BAG_RATIO);               // OPTIONAL O(N) SCAN
            ///// End commentable out
            
            // remove object from bag
            allObjects.remove(lai.index);
            if (allObjects.numObjs > lai.index)    // update the index of the guy who just got moved
                ((LocationAndIndex)(locationAndIndexHash.get(allObjects.objs[lai.index]))).index = lai.index;
            
            assert sim.util.LocationLog.it(this, lai.location);
            return lai.location;
            }
        else return null;
        }
    
    /** Changes the location of an object, or adds if it doesn't exist yet.  Returns false
        if the object could not be set to that location.  For example: you cannot put null
        objects or null locations into a Sparse Field.  Subclasses may have further restrictions
        on locations. */
    // a long method but actually it runs quite speedily -- lots of if/then stuff
    protected boolean setObjectLocation(final Object obj, final Object location)
        {
        if (obj==null) return false;  // RuntimeException("Cannot add null as an object to a SparseField");
        if (location==null) return false;  // RuntimeException("Cannot use null as a location in a SparseField");
                
        Bag canUse = null;  // reusable bag perhaps
            
        // check if previously somewhere
        LocationAndIndex lai = (LocationAndIndex)(locationAndIndexHash.get(obj));  // HASH
        if (lai!=null)
            {
            // first check to see if we need to bother putting it back in
            if (lai.location.equals(location)) return true;  // it's already there!
            
            // remove from old objectHash
            Bag objs = lai.otherObjectsAtLocation;
            objs.remove(obj);                                                   // O(N) SCAN
            
            ///// You can comment this out for speed -- but it doesn't look like it has much effect.
            ///// Commenting it would mean that big empty bags can be sitting around in the hashtable
            final int objsNumObjs = objs.numObjs;  // a little efficiency
            if (removeEmptyBags && objsNumObjs==0) // remove from objectHash, let GC
                {
                objectHash.remove(lai.location);                                // OPTIONAL MAYBE HASH
                // reuse the bag maybe
                canUse = objs;
                }
            else if (replaceLargeBags && objsNumObjs >= MIN_BAG_SIZE && objsNumObjs * LARGE_BAG_RATIO <= objs.objs.length)  // bag not full enough
                objs.shrink(objsNumObjs * REPLACEMENT_BAG_RATIO);               // OPTIONAL O(N) SCAN
            ///// End commentable out
            
            // write in new location  -- we're reusing the LocationAndIndex
            assert sim.util.LocationLog.it(this, lai.location);
            lai.location = location;
            }
        else   // add new object
            {
            // put object into bag
            allObjects.add(obj);
            
            // put object into locationAndIndexHash, with bag index and location
            locationAndIndexHash.put(obj, lai = new LocationAndIndex(location, allObjects.numObjs - 1));    // HASH
            }

        // put into objectHash
        assert sim.util.LocationLog.it(this, location);
        Bag objs = (Bag)(objectHash.get(location));                         // HASH
        if (objs==null)
            {
            // add a bag.  Possibly reuse
            if (canUse != null) canUse.clear();  // clean out and get rid of the cockroaches
            else canUse = new Bag(INITIAL_BAG_SIZE);
            canUse.add(obj);
            objectHash.put(location, objs = canUse);                        // MAYBE HASH
            }
        else objs.add(obj);
        lai.otherObjectsAtLocation = objs;

        return true; // yay, done
        }
        
    /** Returns all the objects in the Sparse Field.  Do NOT modify the bag that you receive from this method -- it
        is used internally.  If you wish to modify the Bag you receive, make a copy of the Bag first, 
        using something like <b>new Bag(<i>foo</i>.getAllObjects())</b>. */
    public final Bag getAllObjects()
        {
        return allObjects;
        }

    /** For each location, puts all object at that location into the result bag.  Returns the result bag.
        If the provided result bag is null, one will be created and returned. */
    public Bag getObjectsAtLocations(final Bag locations, Bag result)
        {
        if (result==null) result = new Bag();
        final Object[] objs = locations.objs;
        final int len = locations.numObjs;
        for(int i=0; i < len; i++)
            {
            // a little efficiency: add if we're 1, addAll if we're > 1, 
            // do nothing if we're 0  -- NOTE this can no longer ever happen (we return null always if we're 0)
            Bag temp = getObjectsAtLocation(objs[i]);
            if (temp!=null)
                {
                int n = temp.numObjs;
                if (n==1) result.add(temp.objs[0]);
                else result.addAll(temp);
                }
            }
        return result;
        }
    

    /** Iterates over all objects.  
        NOT fail-fast, and remove() not supported.  Use this method only if you're
        woozy about accessing allObject.numObjs and allObject.objs directly. 
        
        For the fastest scan, you can do:
        <p><tt>
        
        for(int x=0;x<field.allObjects.numObjs;x++) ... field.allObjects.objs[x] ... </tt>
        
        <p>... but do NOT modify the allObjects.objs array.
        
    */
    public Iterator iterator() 
        {
        final Iterator i = allObjects.iterator();
        return new Iterator()
            {
            public boolean hasNext() { return i.hasNext(); }
            public Object next() { return i.next(); }
            public void remove() { throw new IllegalStateException("Remove not supported in SparseField.iterator()"); }
            };
        }
    
    /** Iterates [somewhat inefficiently] over all bags of objects grouped by location.
        Only used by SparseFieldPortrayal -- generally this should not be interesting to you. */
    public Iterator locationBagIterator()
        {
        final Iterator i = objectHash.values().iterator();
        return new Iterator()
            {
            public boolean hasNext() { return i.hasNext(); }
            public Object next() { return i.next(); }
            public void remove() { throw new IllegalStateException("Remove not supported in SparseField.iterator()"); }
            };
        }

    /** Objects stored in SparseField's locationAndIndexHash table.  This class contains
        an Object <i>location</i> and an int <i>index</i>.  index is the position of
        the objects in the allObjects bag. */
    public static class LocationAndIndex implements java.io.Serializable
        {
        Object location;
        int index;
        Bag otherObjectsAtLocation;
        
        public Object getLocation() { return location; }
        public int getIndex() { return index; }
        
        public LocationAndIndex(final Object location, final int index)
            {
            this.location = location;
            this.index = index;
            }
        // static inner classes don't need serialVersionUIDs
        }
    }


