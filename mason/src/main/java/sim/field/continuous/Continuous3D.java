/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.continuous;
import sim.field.*;
import sim.util.*;
import java.util.*;

/**
   A storage facility for objects located in a continuous 3D environment.  This facility relates
   objects with 3D double tuples (in the form of Double3D).  The facility extends SparseField, and like
   other objects which extend SparseField (such as SparseGrid3D), the facility can only relate any given
   object with a single location at a time -- that is, an object cannot hold two locations at once in 
   the Continuous3D field.

   <p>Because hashtable lookups are more expensive than just storing the object, we suggest that you ALSO
   store the location of an object in the object itself, so you can read from the object rather than having
   to call getObjectLocation(object).  

   <p>The Continuous3D has been arranged to make neighborhood lookup information efficient.
   It discretizes the space into grid buckets.  The discretization size of the buckets is
   provided in the constructor and cannot be changed thereafter.  If the discretization was 0.7,
   for example, then one bucket would be (0,0,0) to (under 0.7, under 0.7, under 0.7), 
   another bucket would be (0,0,0.0,0.7) to (under 0.7, under 0.7, under 1.4), etc.

   <p>You can use Continuous3D to look up objects in a given region by asking for objects within the
   enclosing buckets, then rummaging through the buckets to find the individuals actually in the desired
   region.  The trick here is to come up with a good bucket size.  If the bucket size is much larger than
   the typical size of a neighborhood lookup, then a typical lookup will include large numbers of objects
   you don't care about; in the worst case, this is an O(n) lookup for something that could have been much
   smaller.  On the other hand, if the bucket size is much smaller than the typical size of a neighborhood
   lookup, then you have to do lots of bucket lookups to cover your range; many if not most of these buckets
   could be empty.  This can also be highly inefficient.

   <p>If your objects are point objects, you have no minimum bound on the discretization size.  But if
   the object are non-point location objects (that is, they have dimensions of width, height, etc.), and
   you care about this overlap when you do distance lookups, then you have a minimum bound on your
   discretization.  In this case, you want to make certain that your discretization is at LEAST larger than
   the LARGEST dimension of any object you plan on putting in the Continuous3D.  The idea here is that if an
   any part of an object fell within the bounding box for your distance lookup task 
   (see getNeighborsWithinDistance(...)), you're guaranteed that the stored location of the object must be within
   a bounding box 1 discretization larger in each direction.

   <p>Okay, so that gives you the minimum discretization you should use.  What about the maximum discretization?
   It depends largely on the number of objects expected to occupy a given discretized bucket region, and on what
   kind of lookups you need to do for objects within a given distance.  Searching through one bucket is a hash
   table lookup.  A smaller discretization returns a more accurate sample of objects within the requested
   bounding box, but requires more hash table lookups.  If you have <b>point location</b> objects, and 
   your field is very dense (LOTS of objects in a bucket on average), then we recommend a
   discretization equal to the maximum range distance you are likely to look up; but if your field is very sparse,
   then we recommend a discretization equal to twice the maximum range distance.  You have to tune it.  If you
   have <b>non-point-location</b> objects, then you have two choices.  One approach is to assume a discretization 
   equal to the maximum range distance, but when doing lookups with getNeighborsWithinDistance(...), you need to
   state that you're using non-point-location objects.  If you're fairly sparse and your objects aren't big, you
   can set the discretization to twice the maximum range distance, and you should be safe calling getNeighborsWithinDistance()
   pretending that your objects are point-location; this saves you a lot of hash table lookups.

   <p>At any rate, do NOT go below the minimum discretization rules. 

   <p>But wait, you say, I have objects of widely varying sizes.  Or I have many different neighborhood lookup
   range needs.  Never fear.  Just use multiple Continuous3Ds of different discretizations.  Depending on your
   needs, you can put all the objects in all of the Continuous3Ds (making different range lookups efficient)
   or various-sized classes of objects in their own Continuous3Ds perhaps.  You have to think this through based
   on your needs.  If all the objects were in all of the Continuous3Ds, you'd think that'd be inefficient in
   moving objects around.  Not really: if the discretizations doubled (or more) each time, you're looking at 
   typically an O(ln n) number of Continuous3Ds, and a corresponding number of lookups.

   <p>Continuous3D objects have a width and a height, but this is <b>only used</b> in computing toroidal
   (wrap-around) situations.  If you don't care about toroidal features, then you can completely disregard
   the width and height.
*/

public /*strictfp*/ class Continuous3D extends SparseField implements SparseField3D
    {
    private static final long serialVersionUID = 1;

    /** Where we store the Double3D values hashed by object */
    public Map doubleLocationHash = buildMap(ANY_SIZE);
    
    public double width;
    public double height;
    public double length;

    public final double discretization;
    
    /** Provide expected bounds on the SparseContinuous3D */
    public Continuous3D(double discretization, double width, double height, double length)
        {
        this.discretization = discretization;
        this.width = width; this.height = height; this.length = length;
        }

    public Continuous3D(Continuous3D other)
        {
        super(other);
        discretization = other.discretization;
        width = other.width;
        height = other.height;
        length = other.length;
        }

    public final Double3D getObjectLocation(Object obj)
        {
        return (Double3D) doubleLocationHash.get(obj);
        }
    
    /** Discretizes the location according to the internal discretization of the Continuous3D.  You can use this to determine what
        internal grid slot the continuous point would fall in.  */
    public final Int3D discretize(final Double3D location)
        {
        final double discretization = this.discretization;  // gets us below 35 bytes so we can be inlined
        return new Int3D((int)(location.x / discretization), 
            (int)(location.y / discretization), 
            (int)(location.z / discretization));
        }
    
    /** Discretizes the location according to the provided discretization, which may or may not be the discretization used internally by the Continuous3D.
        If you're trying to determine what grid slot a continuous point would fall in, you probably want discretize(location) instead. */
    public final Int3D discretize(final Double3D location, int discretization)
        {
        return new Int3D((int)(location.x / discretization), 
            (int)(location.y / discretization), 
            (int)(location.z / discretization));
        }
    
    public final boolean setObjectLocation(Object obj, final Double3D location)
        {
        boolean result = super.setObjectLocation(obj, discretize(location));
        if (result) doubleLocationHash.put(obj,location);
        return result;
        }
        
    public final Bag clear()
        {
        doubleLocationHash = buildMap(ANY_SIZE);
        return super.clear();
        }
        
    public final Object remove(final Object obj)
        {
        Object result = super.remove(obj);
        doubleLocationHash.remove(obj);
        return result;
        }
 

    /** Get the width */
    public double getWidth() { return width; }
    
    /** Get the height */
    public double getHeight() { return height; }
    
    /** Get the height */
    public double getLength() { return length; }


    /** Toroidal x */
    // slight revision for more efficiency
    public final double tx(double x) 
        { 
        final double width = this.width;
        if (x >= 0 && x < width) return x;  // do clearest case first
        x = x % width;
        if (x < 0) x = x + width;
        return x;
        }
        
    /** Toroidal y */
    // slight revision for more efficiency
    public final double ty(double y) 
        { 
        final double height = this.height;
        if (y >= 0 && y < height) return y;  // do clearest case first
        y = y % height;
        if (y < 0) y = y + height;
        return y;
        }

    /** Toroidal z */
    // slight revision for more efficiency
    public final double tz(double z) 
        { 
        final double length = this.length;
        if (z >= 0 && z < length) return z;  // do clearest case first
        z = z % length;
        if (z < 0) z = z + length;
        return z;
        }

    /*
      public final double tx(final double x) 
      { 
      final double width = this.width; 
      if (x >= 0) return (x % width); 
      final double width2 = (x % width) + width; 
      if (width2 < width) return width2;
      return 0;
      }
    */
        
    /*
      public final double ty(final double y) 
      { 
      final double height = this.height; 
      if (y >= 0) return (y % height); 
      final double height2 = (y % height) + height;
      if (height2 < height) return height2;
      return 0;
      }
    */
        
    /*
      public final double tz(final double z) 
      { 
      final double length = this.length; 
      if (z >= 0) return (z % length); 
      final double length2 = (z % length) + length;
      if (length2 < length) return length2;
      return 0;
      }
    */
        
    /** Simple [and fast] toroidal x.  Use this if the values you'd pass in never stray
        beyond (-width ... width * 2) not inclusive.  It's a bit faster than the full
        toroidal computation as it uses if statements rather than two modulos.
        The following definition:<br>
        { double width = this.width; if (x >= 0) { if (x < width) return x; return x - width; } return x + width; } <br>
        ...produces the shortest code (24 bytes) and is inlined in Hotspot for 1.4.1.   However removing
        the double width = this.width; is likely to be a little faster if most objects are within the
        toroidal region. */
    public double stx(final double x) 
        { if (x >= 0) { if (x < width) return x; return x - width; } return x + width; }
    
    /** Simple [and fast] toroidal y.  Use this if the values you'd pass in never stray
        beyond (-height ... height * 2) not inclusive.  It's a bit faster than the full
        toroidal computation as it uses if statements rather than two modulos.
        The following definition:<br>
        { double height = this.height; if (y >= 0) { if (y < height) return y ; return y - height; } return y + height; } <br>
        ...produces the shortest code (24 bytes) and is inlined in Hotspot for 1.4.1.   However removing
        the double height = this.height; is likely to be a little faster if most objects are within the
        toroidal region. */
    public double sty(final double y) 
        { if (y >= 0) { if (y < height) return y ; return y - height; } return y + height; }

    /** Simple [and fast] toroidal z.  Use this if the values you'd pass in never stray
        beyond (-length ... length * 2) not inclusive.  It's a bit faster than the full
        toroidal computation as it uses if statements rather than two modulos.
        The following definition:<br>
        { double length = this.length; if (z >= 0) { if (z < length) return z ; return z - length; } return z + length; } <br>
        ...produces the shortest code (24 bytes) and is inlined in Hotspot for 1.4.1.   However removing
        the double length = this.length; is likely to be a little faster if most objects are within the
        toroidal region. */
    public double stz(final double z) 
        { if (z >= 0) { if (z < length) return z ; return z - length; } return z + length; }
        
    // some efficiency to avoid width lookups
    double stx(final double x, final double width) 
        { if (x >= 0) { if (x < width) return x; return x - width; } return x + width; }

    /** Minimum toroidal difference between two values in the X dimension. */
    public double tdx(final double x1, final double x2)
        {
        double width = this.width;
        if (Math.abs(x1-x2) <= width / 2)
            return x1 - x2;  // no wraparounds  -- quick and dirty check
        
        double dx = stx(x1,width) - stx(x2,width);
        if (dx * 2 > width) return dx - width;
        if (dx * 2 < -width) return dx + width;
        return dx;
        }
    
    // some efficiency to avoid height lookups
    double sty(final double y, final double height) 
        { if (y >= 0) { if (y < height) return y ; return y - height; } return y + height; }

    /** Minimum toroidal difference between two values in the Y dimension. */
    public double tdy(final double y1, final double y2)
        {
        double height = this.height;
        if (Math.abs(y1-y2) <= height / 2)
            return y1 - y2;  // no wraparounds  -- quick and dirty check

        double dy = sty(y1,height) - sty(y2,height);
        if (dy * 2 > height) return dy - height;
        if (dy * 2 < -height) return dy + height;
        return dy;
        }
    
    double stz(final double z, final double length) 
        { if (z >= 0) { if (z < length) return z ; return z - length; } return z + length; }

    /** Minimum toroidal difference between two values in the Z dimension. */
    public double tdz(final double z1, final double z2)
        {
        double length = this.length;
        if (Math.abs(z1-z2) <= length / 2)
            return z1 - z2;  // no wraparounds  -- quick and dirty check

        double dz = stz(z1,length) - stz(z2,length);
        if (dz * 2 > length) return dz - length;
        if (dz * 2 < -length) return dz + length;
        return dz;
        }

    /** Minimum Toroidal Distance Squared between two points. This computes the "shortest" (squared) distance between two points, considering wrap-around possibilities as well. */
    public double tds(final Double3D d1, final Double3D d2)
        {
        double dx = tdx(d1.x,d2.x);
        double dy = tdy(d1.y,d2.y);
        double dz = tdz(d1.z,d2.z);
        return (dx * dx + dy * dy + dz * dz);
        }

    /** Minimum Toroidal difference vector between two points.  This subtracts the second point from the first and produces the minimum-length such subtractive vector, considering wrap-around possibilities as well*/
    public Double3D tv(final Double3D d1, final Double3D d2)
        {
        return new Double3D(tdx(d1.x,d2.x),tdy(d1.y,d2.y),tdz(d1.z,d2.z));
        }


    /** Returns a Bag containing EXACTLY those objects within a certain distance of a given position, or equal to that distance, measuring
        using a circle of radius 'distance' around the given position.  Assumes non-toroidal point objects.   
        
        <p> Note: if the field is toroidal, and position is outside the boundaries, it will be wrapped
        to within the boundaries before computation.
        @deprecated
    */

    public Bag getObjectsExactlyWithinDistance(final Double3D position, final double distance)
        {
        return getObjectsExactlyWithinDistance(position, distance, false, true, true, null);
        }

    /** Returns a Bag containing EXACTLY those objects within a certain distance of a given position, or equal to that distance, measuring
        using a circle of radius 'distance' around the given position.  If 'toroidal' is true, then the
        distance is measured assuming the environment is toroidal.  Assumes point objects.   
        
        <p> Note: if the field is toroidal, and position is outside the boundaries, it will be wrapped
        to within the boundaries before computation.
        @deprecated
    */

    public Bag getObjectsExactlyWithinDistance(final Double3D position, final double distance, final boolean toroidal)
        {
        return getObjectsExactlyWithinDistance(position, distance, toroidal, true, true, null);
        }

    /** Returns a Bag containing EXACTLY those objects within a certain distance of a given position.  If 'radial' is true,
        then the distance is measured using a circle around the position, else the distance is meaured using a square around
        the position (that is, it's the maximum of the x and y distances).   If 'inclusive' is true, then objects that are
        exactly the given distance away are included as well, else they are discarded.  If 'toroidal' is true, then the
        distance is measured assuming the environment is toroidal.  If the Bag 'result' is provided, it will be cleared and objects
        placed in it and it will be returned, else if it is null, then this method will create a new Bag and use that instead. 
        Assumes point objects.  
        
        <p> Note: if the field is toroidal, and position is outside the boundaries, it will be wrapped
        to within the boundaries before computation.
        @deprecated
    */

    public Bag getObjectsExactlyWithinDistance(final Double3D position, final double distance, final boolean toroidal, 
        final boolean radial, final boolean inclusive, Bag result)
        {
        return getNeighborsExactlyWithinDistance(position, distance, toroidal, radial, inclusive, result);
        }



    /** Returns a Bag containing EXACTLY those objects within a certain distance of a given position, or equal to that distance, measuring
        using a circle of radius 'distance' around the given position.  Assumes non-toroidal point objects.   
        
        <p> Note: if the field is toroidal, and position is outside the boundaries, it will be wrapped
        to within the boundaries before computation.
    */

    public Bag getNeighborsExactlyWithinDistance(final Double3D position, final double distance)
        {
        return getObjectsExactlyWithinDistance(position, distance, false, true, true, null);
        }

    /** Returns a Bag containing EXACTLY those objects within a certain distance of a given position, or equal to that distance, measuring
        using a circle of radius 'distance' around the given position.  If 'toroidal' is true, then the
        distance is measured assuming the environment is toroidal.  Assumes point objects.   
        
        <p> Note: if the field is toroidal, and position is outside the boundaries, it will be wrapped
        to within the boundaries before computation.
    */

    public Bag getNeighborsExactlyWithinDistance(final Double3D position, final double distance, final boolean toroidal)
        {
        return getNeighborsExactlyWithinDistance(position, distance, toroidal, true, true, null);
        }

    /** Returns a Bag containing EXACTLY those objects within a certain distance of a given position.  If 'radial' is true,
        then the distance is measured using a circle around the position, else the distance is meaured using a square around
        the position (that is, it's the maximum of the x and y distances).   If 'inclusive' is true, then objects that are
        exactly the given distance away are included as well, else they are discarded.  If 'toroidal' is true, then the
        distance is measured assuming the environment is toroidal.  If the Bag 'result' is provided, it will be cleared and objects
        placed in it and it will be returned, else if it is null, then this method will create a new Bag and use that instead. 
        Assumes point objects.  
        
        <p> Note: if the field is toroidal, and position is outside the boundaries, it will be wrapped
        to within the boundaries before computation.
    */

    public Bag getNeighborsExactlyWithinDistance(final Double3D position, final double distance, final boolean toroidal, 
        final boolean radial, final boolean inclusive, Bag result)
        {
        result = getNeighborsWithinDistance(position, distance, toroidal, false, result);
        int numObjs = result.numObjs;
        Object[] objs = result.objs;
        double distsq = distance*distance;
        if (radial) 
            for(int i=0;i<numObjs;i++)
                {
                double d = 0;
                Double3D loc = getObjectLocation(objs[i]);
                if (toroidal) d = tds(position, loc);
                else d = position.distanceSq(loc);
                if (d > distsq || (!inclusive && d >= distsq)) 
                    { result.remove(i); i--; numObjs--; }
                }
        else 
            for(int i=0;i<numObjs;i++)
                {
                Double3D loc = getObjectLocation(objs[i]);
                double minx = 0;
                double miny = 0;
                double minz = 0;
                if (toroidal)
                    {
                    minx = tdx(loc.x, position.x);
                    miny = tdy(loc.y, position.y);
                    minz = tdz(loc.z, position.z);
                    }
                else
                    {
                    minx = loc.x - position.x;
                    miny = loc.y - position.y;
                    minz = loc.z - position.z;
                    }
                if (minx < 0) minx = -minx;
                if (miny < 0) miny = -miny;
                if (minz < 0) minz = -minz;
                if ((minx > distance || miny > distance || minz > distance) ||
                    (!inclusive && (minx >= distance || miny >= distance || minz >= distance)))
                    { result.remove(i); i--;  numObjs--; }
                }
        return result;
        }




    /** Returns a bag containing AT LEAST those objects within the bounding box surrounding the
        specified distance of the specified position.  The bag could include other objects than this.
        In this case we include the object if
        any part of the bounding box could overlap into the desired region.  To do this, if nonPointObjects is
        true, we extend the search space by one extra discretization in all directions.  For small distances within
        a single bucket, this returns 27 bucket's worth rather than 1 (!!!), so if you know you only care about the
        actual x/y/z points stored, rather than possible object overlap into the distance sphere you specified,
        you'd want to set nonPointObjects to FALSE.   That's a lot of extra hash table lookups. [assumes non-toroidal, point objects]    
        
        <p> Note: if the field is toroidal, and position is outside the boundaries, it will be wrapped
        to within the boundaries before computation.
        @deprecated
    */
    public Bag getObjectsWithinDistance( final Double3D position, final double distance)
        { return getObjectsWithinDistance(position,distance,false,false, new Bag()); }

    /** Returns a bag containing AT LEAST those objects within the bounding box surrounding the
        specified distance of the specified position.  The bag could include other objects than this.
        If toroidal, then wrap-around possibilities are also considered.  In this case we include the object if
        any part of the bounding box could overlap into the desired region.  To do this, if nonPointObjects is
        true, we extend the search space by one extra discretization in all directions.  For small distances within
        a single bucket, this returns 27 bucket's worth rather than 1 (!!!), so if you know you only care about the
        actual x/y/z points stored, rather than possible object overlap into the distance sphere you specified,
        you'd want to set nonPointObjects to FALSE.   That's a lot of extra hash table lookups. [assumes point objects]    
        
        <p> Note: if the field is toroidal, and position is outside the boundaries, it will be wrapped
        to within the boundaries before computation.
        @deprecated
    */
    public Bag getObjectsWithinDistance( final Double3D position, final double distance, final boolean toroidal)
        { return getObjectsWithinDistance(position,distance,toroidal,false, new Bag()); }

    /**  Returns a bag containing AT LEAST those objects within the bounding box surrounding the
         specified distance of the specified position.  The bag could include other objects than this.
         If toroidal, then wrap-around possibilities are also considered.
         If nonPointObjects, then it is presumed that
         the object isn't just a point in space, but in fact fills an area in space where the x/y point location
         could be at the extreme corner of a bounding box of the object.  In this case we include the object if
         any part of the bounding box could overlap into the desired region.  To do this, if nonPointObjects is
         true, we extend the search space by one extra discretization in all directions.  For small distances within
         a single bucket, this returns 27 bucket's worth rather than 1 (!!!), so if you know you only care about the
         actual x/y/z points stored, rather than possible object overlap into the distance sphere you specified,
         you'd want to set nonPointObjects to FALSE.   That's a lot of extra hash table lookups.   
        
         <p> Note: if the field is toroidal, and position is outside the boundaries, it will be wrapped
         to within the boundaries before computation.
         @deprecated
    */
        
    public Bag getObjectsWithinDistance( final Double3D position, final double distance, final boolean toroidal,
        final boolean nonPointObjects)
        { return getObjectsWithinDistance(position, distance, toroidal, nonPointObjects, new Bag()); }


    /** Puts into the result Bag (and returns it) AT LEAST those objects within the bounding box surrounding the
        specified distance of the specified position.  The bag could include other objects than this.
        If toroidal, then wrap-around possibilities are also considered.
        If nonPointObjects, then it is presumed that
        the object isn't just a point in space, but in fact fills an area in space where the x/y point location
        could be at the extreme corner of a bounding box of the object.  In this case we include the object if
        any part of the bounding box could overlap into the desired region.  To do this, if nonPointObjects is
        true, we extend the search space by one extra discretization in all directions.  For small distances within
        a single bucket, this returns 27 bucket's worth rather than 1 (!!!), so if you know you only care about the
        actual x/y/z points stored, rather than possible object overlap into the distance sphere you specified,
        you'd want to set nonPointObjects to FALSE.   That's a lot of extra hash table lookups.   
        
        <p> Note: if the field is toroidal, and position is outside the boundaries, it will be wrapped
        to within the boundaries before computation.
        @deprecated
    */
        
    public Bag getObjectsWithinDistance( Double3D position, final double distance, final boolean toroidal,
        final boolean nonPointObjects, Bag result)
        {
        return getNeighborsWithinDistance(position, distance, toroidal, nonPointObjects, result);
        }



    /** Returns a bag containing AT LEAST those objects within the bounding box surrounding the
        specified distance of the specified position.  The bag could include other objects than this.
        In this case we include the object if
        any part of the bounding box could overlap into the desired region.  To do this, if nonPointObjects is
        true, we extend the search space by one extra discretization in all directions.  For small distances within
        a single bucket, this returns 27 bucket's worth rather than 1 (!!!), so if you know you only care about the
        actual x/y/z points stored, rather than possible object overlap into the distance sphere you specified,
        you'd want to set nonPointObjects to FALSE.   That's a lot of extra hash table lookups. [assumes non-toroidal, point objects]    
        
        <p> Note: if the field is toroidal, and position is outside the boundaries, it will be wrapped
        to within the boundaries before computation.
    */
    public Bag getNeighborsWithinDistance( final Double3D position, final double distance)
        { return getNeighborsWithinDistance(position,distance,false,false, new Bag()); }

    /** Returns a bag containing AT LEAST those objects within the bounding box surrounding the
        specified distance of the specified position.  The bag could include other objects than this.
        If toroidal, then wrap-around possibilities are also considered.  In this case we include the object if
        any part of the bounding box could overlap into the desired region.  To do this, if nonPointObjects is
        true, we extend the search space by one extra discretization in all directions.  For small distances within
        a single bucket, this returns 27 bucket's worth rather than 1 (!!!), so if you know you only care about the
        actual x/y/z points stored, rather than possible object overlap into the distance sphere you specified,
        you'd want to set nonPointObjects to FALSE.   That's a lot of extra hash table lookups. [assumes point objects]    
        
        <p> Note: if the field is toroidal, and position is outside the boundaries, it will be wrapped
        to within the boundaries before computation.
    */
    public Bag getNeighborsWithinDistance( final Double3D position, final double distance, final boolean toroidal)
        { return getNeighborsWithinDistance(position,distance,toroidal,false, new Bag()); }

    /**  Returns a bag containing AT LEAST those objects within the bounding box surrounding the
         specified distance of the specified position.  The bag could include other objects than this.
         If toroidal, then wrap-around possibilities are also considered.
         If nonPointObjects, then it is presumed that
         the object isn't just a point in space, but in fact fills an area in space where the x/y point location
         could be at the extreme corner of a bounding box of the object.  In this case we include the object if
         any part of the bounding box could overlap into the desired region.  To do this, if nonPointObjects is
         true, we extend the search space by one extra discretization in all directions.  For small distances within
         a single bucket, this returns 27 bucket's worth rather than 1 (!!!), so if you know you only care about the
         actual x/y/z points stored, rather than possible object overlap into the distance sphere you specified,
         you'd want to set nonPointObjects to FALSE.   That's a lot of extra hash table lookups.   
        
         <p> Note: if the field is toroidal, and position is outside the boundaries, it will be wrapped
         to within the boundaries before computation.
    */
        
    public Bag getNeighborsWithinDistance( final Double3D position, final double distance, final boolean toroidal,
        final boolean nonPointObjects)
        { return getNeighborsWithinDistance(position, distance, toroidal, nonPointObjects, new Bag()); }


    /** Puts into the result Bag (and returns it) AT LEAST those objects within the bounding box surrounding the
        specified distance of the specified position.  The bag could include other objects than this.
        If toroidal, then wrap-around possibilities are also considered.
        If nonPointObjects, then it is presumed that
        the object isn't just a point in space, but in fact fills an area in space where the x/y point location
        could be at the extreme corner of a bounding box of the object.  In this case we include the object if
        any part of the bounding box could overlap into the desired region.  To do this, if nonPointObjects is
        true, we extend the search space by one extra discretization in all directions.  For small distances within
        a single bucket, this returns 27 bucket's worth rather than 1 (!!!), so if you know you only care about the
        actual x/y/z points stored, rather than possible object overlap into the distance sphere you specified,
        you'd want to set nonPointObjects to FALSE.   That's a lot of extra hash table lookups.   
        
        <p> Note: if the field is toroidal, and position is outside the boundaries, it will be wrapped
        to within the boundaries before computation.
    */
        
    public Bag getNeighborsWithinDistance( Double3D position, final double distance, final boolean toroidal,
        final boolean nonPointObjects, Bag result)
        {
        // push location to within legal boundaries
        if (toroidal && (position.x >= width || position.y >= height || position.z >= length || position.x < 0 || position.y < 0 || position.z < 0))
            position = new Double3D(tx(position.x), ty(position.y), tz(position.z));

        double discDistance = distance / discretization;
        double discX = position.x / discretization;
        double discY = position.y / discretization;
        double discZ = position.z / discretization;
        
        if (nonPointObjects)
            {
            // We assume that the discretization is larger than the bounding
            // box width or height for the object in question.  In this case, then
            // we can just increase the range by 1 in each direction and we are
            // guaranteed to have the location of the object in our collection.
            discDistance++;
            }
        
        final int expectedBagSize = 1;  // in the future, pick a smarter bag size?
        if (result!=null) result.clear();
        else result = new Bag(expectedBagSize);
        Bag temp;
    
        MutableInt3D speedyMutableInt3D = new MutableInt3D();  // a little faster (local)


        // do the loop
        if( toroidal )
            {
            final int iWidth = (int)(StrictMath.ceil(width / discretization));
            final int iHeight = (int)(StrictMath.ceil(height / discretization));
            final int iLength = (int)(StrictMath.ceil(length / discretization));

            // we're using StrictMath.floor instead of Math.floor because
            // Math.floor just calls StrictMath.floor, and so using the
            // StrictMath version may help in the inlining (one function
            // to inline, not two).  They should be identical in function anyway.
            
            int minX = (int) StrictMath.floor(discX - discDistance);
            int maxX = (int) StrictMath.floor(discX + discDistance);
            int minY = (int) StrictMath.floor(discY - discDistance);
            int maxY = (int) StrictMath.floor(discY + discDistance);
            int minZ = (int) StrictMath.floor(discZ - discDistance);
            int maxZ = (int) StrictMath.floor(discZ + discDistance);

            if (position.x + distance >= width && maxX == iWidth - 1)  // oops, need to recompute wrap-around if width is not a multiple of discretization
                maxX = 0;

            if (position.y + distance >= height && maxY == iHeight - 1)  // oops, need to recompute wrap-around if height is not a multiple of discretization
                maxY = 0;

            if (position.z + distance >= length && maxZ == iLength - 1)  // oops, need to recompute wrap-around if length is not a multiple of discretization
                maxZ = 0;



            // we promote to longs so that maxX - minX can't totally wrap around by accident
            if ((long)maxX - (long)minX >= iWidth)  // total wrap-around.
                { minX = 0; maxX = iWidth-1; }
            if ((long)maxY - (long)minY >= iHeight) // similar
                { minY = 0; maxY = iHeight-1; }
            if ((long)maxZ - (long)minZ >= iLength) // similar
                { minZ = 0; maxZ = iLength-1; }

            // okay, now tx 'em.
            final int tmaxX = toroidal(maxX,iWidth);
            final int tmaxY = toroidal(maxY,iHeight);
            final int tmaxZ = toroidal(maxZ,iLength);
            final int tminX = toroidal(minX,iWidth);
            final int tminY = toroidal(minY,iHeight);
            final int tminZ = toroidal(minZ,iLength);

            int x = tminX ;
            do
                {
                int y = tminY;
                do
                    {
                    int z = tminZ;
                    do
                        {
                        // grab location
                        speedyMutableInt3D.x=x; speedyMutableInt3D.y=y; speedyMutableInt3D.z=z;
                        temp =  getRawObjectsAtLocation(speedyMutableInt3D);
                        if( temp != null && !temp.isEmpty())
                            {
                            // a little efficiency: add if we're 1, addAll if we're > 1, 
                            // do nothing if we're <= 0 (we're empty)
                            int n = temp.numObjs;
                            if (n==1) result.add(temp.objs[0]);
                            else result.addAll(temp);
                            }

                        // update z
                        if( z == tmaxZ )
                            break;
                        if( z == iLength-1 )
                            z = 0;
                        else
                            z++;
                        }
                    while(true);

                    // update y
                    if( y == tmaxY )
                        break;
                    if( y == iHeight-1 )
                        y = 0;
                    else
                        y++;
                    }
                while(true);

                // update x
                if( x == tmaxX )
                    break;
                if( x == iWidth-1 )
                    x = 0;
                else
                    x++;
                }
            while(true);
            }
        else
            {
            // we're using StrictMath.floor instead of Math.floor because
            // Math.floor just calls StrictMath.floor, and so using the
            // StrictMath version may help in the inlining (one function
            // to inline, not two).  They should be identical in function anyway.
            
            int minX = (int) StrictMath.floor(discX - discDistance);
            int maxX = (int) StrictMath.floor(discX + discDistance);
            int minY = (int) StrictMath.floor(discY - discDistance);
            int maxY = (int) StrictMath.floor(discY + discDistance);
            int minZ = (int) StrictMath.floor(discZ - discDistance);
            int maxZ = (int) StrictMath.floor(discZ + discDistance);

            // for non-toroidal, it is easier to do the inclusive for-loops
            for(int x = minX; x<= maxX; x++)
                for(int y = minY ; y <= maxY; y++)
                    for (int z = minZ ; z <= maxZ; z++)
                        {
                        // grab location
                        speedyMutableInt3D.x=x; speedyMutableInt3D.y=y; speedyMutableInt3D.z=z;
                        temp =  getRawObjectsAtLocation(speedyMutableInt3D);
                        if( temp != null && !temp.isEmpty())
                            {
                            // a little efficiency: add if we're 1, addAll if we're > 1, 
                            // do nothing if we're <= 0 (we're empty)
                            final int n = temp.numObjs;
                            if (n==1) result.add(temp.objs[0]);
                            else result.addAll(temp);
                            }
                        }
            }
            
        return result;
        }

    // used internally in getNeighborsWithinDistance.  Note similarity to
    // AbstractGrid3D's tx method
    final int toroidal(final int x, final int width) 
        { 
        if (x >= 0) return (x % width); 
        final int width2 = (x % width) + width; 
        if (width2 < width) return width2;
        return 0;
        }


    /** Returns a bag containing all the objects at a given discretized location, or null when there are no objects at the location.
        You should NOT MODIFY THIS BAG. This is the actual container bag, and modifying it will almost certainly break
        the Sparse Field object.   If you want to modify the bag, make a copy and modify the copy instead,
        using something along the lines of <b> new Bag(<i>foo</i>.getObjectsAtLocation(<i>location</i>)) </b>.
        Furthermore, changing values in the Sparse Field may result in a different bag being used -- so you should
        not rely on this bag staying valid.  The default implementation of this method simply calls getRawObjectsAtLocation(),
        but you may need to override it for more custom functionality (which is rare).
    */
    public Bag getObjectsAtDiscretizedLocation(final Int3D location)
        {
        return getRawObjectsAtLocation(location);
        }

    /** Returns a bag containing all the objects at a given location, 
        or null if there are no such objects or if location is null.  Unlike other SparseField versions, you may modify this bag.
    */
    public Bag getObjectsAtLocation(Double3D location)
        {
        if (location == null) return null;
        Bag cell = getRawObjectsAtLocation(discretize(location));
        if (cell == null) return null;
        Bag result = new Bag();
        Object[] objs = cell.objs;
        int numObjs = cell.numObjs;
        // whittle down
        for(int i = 0; i < numObjs; i++)
            {
            Object loc = getObjectLocation(objs[i]);
            if (loc.equals(location))
                result.add(objs[i]);
            }
        return result;
        }
                
    /** Returns the number of the objects at a given location, 
        or 0 if there are no such objects or if location is null.
    */
    public int numObjectsAtLocation(Double3D location)
        {
        if (location == null) return 0;
        Bag cell = getRawObjectsAtLocation(discretize(location));
        if (cell == null) return 0;
        int count = 0;
        Object[] objs = cell.objs;
        int numObjs = cell.numObjs;
        // whittle down
        for(int i = 0; i < numObjs; i++)
            {
            Object loc = getObjectLocation(objs[i]);
            if (loc.equals(location))
                count++;
            }
        return count;
        }

    /** Returns a bag containing all the objects at the exact same location as a given object, including the object itself, 
        or null if the object is not in the Field.  Unlike other SparseField versions, you may modify this bag.
    */
    public Bag getObjectsAtLocationOfObject(Object obj)
        {
        Object location = getObjectLocation(obj);
        if (location == null) return null;
        else return getObjectsAtLocation(location);
        }
                
    /** Returns the number of objects at the exact same location as a given object, including the object itself, 
        or 0 if the object is not in the Field.
    */
    public int numObjectsAtLocationOfObject(Object obj)
        {
        Object location = getObjectLocation(obj);
        if (location == null) return 0;
        else return numObjectsAtLocation(location);
        }

    /** Removes objects at exactly the given location, and returns a bag of them, or null of no objects are at that location.
        The Bag may be empty, or null, if there were no objects at that location.  You can freely modify this bag. */
    public Bag removeObjectsAtLocation(final Double3D location)
        {
        Bag bag = getObjectsAtLocation(location);               // this bag is a copy so it won't be reduced as I remove objects
        if (bag != null)
            {
            Object[] objs = bag.objs;
            int numObjs = bag.numObjs;
            for(int i = 0; i < bag.numObjs; i++)
                remove(objs[i]);
            }
        return bag;
        }

    public final Double3D getDimensions() { return new Double3D(width, height, length); }

    /** Returns the object location as a Double3D, or as null if there is no such object. */
    public Double3D getObjectLocationAsDouble3D(Object obj)
        {
        return (Double3D) doubleLocationHash.get(obj);
        }
    }


