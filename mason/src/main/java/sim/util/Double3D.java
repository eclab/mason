/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util;

/**
   Double3D is more or less the same class as javax.vecmath.Point3d, but it is immutable: once the x and y and z values are set, they cannot be changed (they're final).  Why use this immutable class when you could just use Point3d?  Because Point3d is broken with respect to hash tables.  You use Point3d as a key in a hash table at your own peril.  Try this: hash an object by a Point3d as key.  Then change the x value of the original Point3d.  Ta-da!  The object is lost in the hash table.  Additionally, Point3d is in a nonstandard package (javax.vecmath) that we may or may not distribute with.

   <p>One day in the far future, Double3D should also be HIGHLY efficient; since it is immutable, it can be passed by value rather than by pointer by a smart compiler.  Not today, though.  But it's not bad.

   <p>This class has an elaborate hash code generation that is much more random than Sun's standard generator, but takes more time.  For very large numbers of objects, this is a good idea, but we may change it to a simpler version in the future.

   <p>Double3D.equals(...) can compare by value against other Int3Ds and Double3Ds.
*/

public final class Double3D implements java.io.Serializable
    {
    private static final long serialVersionUID = 1;

    public final double x;
    public final double y;
    public final double z;
    
    public Double3D() { x = 0.0; y = 0.0; z = 0.0;}
    /** Explicitly assumes the z value is set to 0 */
    public Double3D(final Int2D p) { x = p.x; y = p.y; z = 0.0; }
    public Double3D(final Int2D p, final double z) { x = p.x; y = p.y; this.z = z; }
    public Double3D(final Int3D p) { x = p.x; y = p.y ; z = p.z; }
    /** Explicitly assumes the z value is set to 0 */
    public Double3D(final MutableInt2D p) { x = p.x; y = p.y; z = 0.0; }
    public Double3D(final MutableInt2D p, final double z) { x = p.x; y = p.y; this.z = z; }
    public Double3D(final MutableInt3D p) { x = p.x; y = p.y ; z = p.z; }
    /** Explicitly assumes the z value is set to 0 */
    public Double3D(final Double2D p) { x = p.x; y = p.y; z = 0.0; }
    public Double3D(final Double2D p, final double z) { x = p.x; y = p.y; this.z = z; }
    public Double3D(final Double3D p) { x=p.x; y=p.y; z=p.z; }
    /** Explicitly assumes the z value is set to 0 */
    public Double3D(final MutableDouble2D p) { x = p.x; y = p.y; z = 0.0; }
    public Double3D(final MutableDouble2D p, final double z) { x = p.x; y = p.y; this.z = z; }
    public Double3D(final MutableDouble3D p) { x=p.x; y=p.y; z=p.z; }
    public Double3D(final double x, final double y, double z) { this.x = x; this.y = y; this.z = z;}
    public final double getX() { return x; }
    public final double getY() { return y; }
    public final double getZ() { return z; }
    public String toString() { return "Double3D["+x+","+y+","+z+"]"; }
    public String toCoordinates() { return "(" + x + ", " + y + ", " + z + ")"; }

    public int hashCode()
        {
        double x = this.x;
        double y = this.y;
        double z = this.z;
                
        // push -0.0 to 0.0 for purposes of hashing.  Note that equals() has also been modified
        // to consider -0.0 to be equal to 0.0.  Hopefully cute Java compilers won't try to optimize this out.
        if (x == -0.0) x = 0.0;
        if (y == -0.0) y = 0.0;
        if (z == -0.0) z = 0.0;
                
        // so we hash to the same value as Int2D does, if we're ints
        if ((((int)x) == x) && (((int)y) == y) && (((int)z) == z))
            //  return Int3D.hashCodeFor((int)x,(int)y,(int)z);
        
            {
            int y_ = (int)y;
            int x_ = (int)x;
            int z_ = (int)z;
            
            // copied from Int3D and inserted here because hashCodeFor can't be
            // inlined and this saves us a fair chunk on some hash-heavy applications
                        
            z_ += ~(z_ << 15);
            z_ ^=  (z_ >>> 10);
            z_ +=  (z_ << 3);
            z_ ^=  (z_ >>> 6);
            z_ += ~(z_ << 11);
            z_ ^=  (z_ >>> 16);
            
            z_ ^= y_;
            z_ += 17;    // a little prime number shifting -- waving a dead chicken?  dunno
            
            z_ += ~(z_ << 15);
            z_ ^=  (z_ >>> 10);
            z_ +=  (z_ << 3);
            z_ ^=  (z_ >>> 6);
            z_ += ~(z_ << 11);
            z_ ^=  (z_ >>> 16);

            // nifty!  Now mix in x
            
            return x_ ^ z_;
            }
            

        // I don't like Sun's simplistic approach to random shuffling.  So...
        // basically we need to randomly disperse <double,double,double> --> int
        // We do this by doing <double,double,double> -> <long,long,long> -> long -> int
        // The first step is done with doubleToLongBits (not RawLongBits;
        // we want all NaN to hash to the same thing).  Then conversion to
        // a single long is done by hashing (shuffling) z, then xoring it with y,
        // then hashing that and xoring with x.
        // I do that as x ^ hash(y ^ hash(z) + 17 [or whatever]). Hash function
        // taken from http://www.cris.com/~Ttwang/tech/inthash.htm

        // Some further discussion.  Sun's moved to a new hash table scheme
        // which has (of all things!) tables with lengths that are powers of two!
        // Normally hash table lengths should be prime numbers, in order to
        // compensate for bad hashcodes.  To fix matters, Sun now is
        // pre-shuffling the hashcodes with the following algorithm (which
        // is short but not too bad -- should we adopt it?  Dunno).  See
        // http://developer.java.sun.com/developer/bugParade/bugs/4669519.html
        //    key += ~(key << 9);
        //    key ^=  (key >>> 14);
        //    key +=  (key << 4);
        //    key ^=  (key >>> 10);
        // This is good for us because Int2D, Int3D, Double2D, and Double3D
        // have hashcodes well distributed with regard to y and z, but when
        // you mix in x, they're just linear in x.  We could do a final
        // shuffle I guess.  In Java 1.3, they DON'T do a pre-shuffle, so
        // it may be suboptimal.  Since we're all moving to 1.4.x, it's not
        // a big deal since 1.4.x is shuffling the final result using the
        // Sun shuffler above.  But I'd appreciate some tests on our method
        // below, and suggestions as to whether or not we should adopt the
        // shorter, likely suboptimal but faster Sun shuffler instead
        // for y and z values.  -- Sean
        
        long key = Double.doubleToLongBits(z);
        key += ~(key << 32);
        key ^= (key >>> 22);
        key += ~(key << 13);
        key ^= (key >>> 8);
        key += (key << 3);
        key ^= (key >>> 15);
        key += ~(key << 27);
        key ^= (key >>> 31);
        
        key ^= Double.doubleToLongBits(y);
        key += 17;    // a little prime number shifting -- waving a dead chicken?  dunno
        
        key += ~(key << 32);
        key ^= (key >>> 22);
        key += ~(key << 13);
        key ^= (key >>> 8);
        key += (key << 3);
        key ^= (key >>> 15);
        key += ~(key << 27);
        key ^= (key >>> 31);

        // nifty!  Now mix in x
        
        key ^= Double.doubleToLongBits(x);
        
        // Last we fold on top of each other
        return (int)(key ^ (key >> 32));
        }
        
    // can't have separate equals(...) methods as the
    // argument isn't virtual
    public boolean equals(final Object obj)
        {
        if (obj==null) return false;
        else if (obj instanceof Double3D)  // do Double3D first
            {
            Double3D other = (Double3D) obj;
            // Note: commented out because it can't handle 0.0 == -0.0, grrr
            return ((x == other.x || (Double.isNaN(x) && Double.isNaN(other.x))) && // they're the same or they're both NaN
                (y == other.y || (Double.isNaN(y) && Double.isNaN(other.y))) && // they're the same or they're both NaN
                (z == other.z || (Double.isNaN(z) && Double.isNaN(other.z)))); // they're the same or they're both NaN

            // can't just do other.x == x && other.y == y && other.z == z because we need to check for NaN
            // return (Double.doubleToLongBits(other.x) == Double.doubleToLongBits(x) &&
            //    Double.doubleToLongBits(other.y) == Double.doubleToLongBits(y) &&
            //    Double.doubleToLongBits(other.z) == Double.doubleToLongBits(z));
            }
        else if (obj instanceof MutableDouble3D)
            {
            MutableDouble3D other = (MutableDouble3D) obj;
            // Note: commented out because it can't handle 0.0 == -0.0, grrr
            return ((x == other.x || (Double.isNaN(x) && Double.isNaN(other.x))) && // they're the same or they're both NaN
                (y == other.y || (Double.isNaN(y) && Double.isNaN(other.y))) && // they're the same or they're both NaN
                (z == other.z || (Double.isNaN(z) && Double.isNaN(other.z)))); // they're the same or they're both NaN

            // can't just do other.x == x && other.y == y && other.z == z because we need to check for NaN
            // return (Double.doubleToLongBits(other.x) == Double.doubleToLongBits(x) &&
            //    Double.doubleToLongBits(other.y) == Double.doubleToLongBits(y) &&
            //    Double.doubleToLongBits(other.z) == Double.doubleToLongBits(z));
            }
        else if (obj instanceof Int3D)
            {
            Int3D other = (Int3D) obj;
            return (other.x == x && other.y == y && other.z == z);
            }
        else if (obj instanceof MutableInt3D)
            {
            MutableInt3D other = (MutableInt3D) obj;
            return (other.x == x && other.y == y && other.z == z);
            }
        else return false;
        }


    /** Returns the distance FROM this Double3D TO the specified point */
    public double distance(final double x, final double y, final double z)
        {
        final double dx = (double)this.x - x;
        final double dy = (double)this.y - y;
        final double dz = (double)this.z - z;
        return Math.sqrt(dx*dx+dy*dy+dz*dz);
        }

    /** Returns the distance FROM this Double3D TO the specified point.   */
    public double distance(final Double3D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        final double dz = (double)this.z - p.z;
        return Math.sqrt(dx*dx+dy*dy+dz*dz);
        }

    /** Returns the distance FROM this Double3D TO the specified point.    */
    public double distance(final Int3D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        final double dz = (double)this.z - p.z;
        return Math.sqrt(dx*dx+dy*dy+dz*dz);
        }

    /** Returns the distance FROM this Double3D TO the specified point.    */
    public double distance(final MutableInt3D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        final double dz = (double)this.z - p.z;
        return Math.sqrt(dx*dx+dy*dy+dz*dz);
        }

    /** Returns the squared distance FROM this Double3D TO the specified point */
    public double distanceSq(final double x, final double y, final double z)
        {
        final double dx = (double)this.x - x;
        final double dy = (double)this.y - y;
        final double dz = (double)this.z - z;
        return (dx*dx+dy*dy+dz*dz);
        }

    /** Returns the squared distance FROM this Double3D TO the specified point.    */
    public double distanceSq(final Double3D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        final double dz = (double)this.z - p.z;
        return (dx*dx+dy*dy+dz*dz);
        }

    /** Returns the squared distance FROM this Double3D TO the specified point.    */
    public double distanceSq(final Int3D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        final double dz = (double)this.z - p.z;
        return (dx*dx+dy*dy+dz*dz);
        }

    /** Returns the squared distance FROM this Double3D TO the specified point.    */
    public double distanceSq(final MutableInt3D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        final double dz = (double)this.z - p.z;
        return (dx*dx+dy*dy+dz*dz);
        }

    /** Returns the manhtattan distance FROM this Double3D TO the specified point */
    public double manhattanDistance(final double x, final double y, final double z)
        {
        final double dx = Math.abs((double)this.x - x);
        final double dy = Math.abs((double)this.y - y);
        final double dz = Math.abs((double)this.z - z);
        return dx + dy + dz;
        }

    /** Returns the manhtattan distance FROM this Double3D TO the specified point */
    public double manhattanDistance(final Double3D p)
        {
        final double dx = Math.abs((double)this.x - p.x);
        final double dy = Math.abs((double)this.y - p.y);
        final double dz = Math.abs((double)this.z - p.z);
        return dx + dy + dz;
        }

    /** Returns the manhtattan distance FROM this Double3D TO the specified point */
    public double manhattanDistance(final Int3D p)
        {
        final double dx = Math.abs((double)this.x - p.x);
        final double dy = Math.abs((double)this.y - p.y);
        final double dz = Math.abs((double)this.z - p.z);
        return dx + dy + dz;
        }

    /** Returns the manhtattan distance FROM this Double3D TO the specified point */
    public double manhattanDistance(final MutableDouble3D p)
        {
        final double dx = Math.abs((double)this.x - p.x);
        final double dy = Math.abs((double)this.y - p.y);
        final double dz = Math.abs((double)this.z - p.z);
        return dx + dy + dz;
        }

    /** Returns the manhtattan distance FROM this Double3D TO the specified point */
    public double manhattanDistance(final MutableInt3D p)
        {
        final double dx = Math.abs((double)this.x - p.x);
        final double dy = Math.abs((double)this.y - p.y);
        final double dz = Math.abs((double)this.z - p.z);
        return dx + dy + dz;
        }

    /** Adds Double3D "other" to current Double3D using 
     * vector addition */
    public final Double3D add(Double3D other)
        {
        return new Double3D(x + other.x, y + other.y, z + other.z);
        }

    /** Subtracts Double3D "other" from current Double3D using 
     * vector subtraction */
    public final Double3D subtract(Double3D other)
        {
        return new Double3D(x - other.x, y - other.y, z - other.z);
        }
        
    /** Returns the vector length of the Double3D */
    public final double length()
        {
        return Math.sqrt(x * x + y * y + z * z);
        }
        
    /** Returns the vector length of the Double3D */
    public final double lengthSq()
        {
        return x*x+y*y+z*z;
        }
        
    /** Multiplies each element by scalar "val" */
    public final Double3D multiply(double val)
        {
        return new Double3D(x * val, y * val, z * val);
        }

    /** Scales the vector to length "dist".  dist must be a finite value.  If the vector has
        NaN, zero, or infinite values, then the vector cannot be resized to any length except for 0:
        other lengths will throw an exception in this case. */
    public final Double3D resize(double dist)
        {
        if (dist == 0)
            return new Double3D(0, 0, 0);
        else if (dist == infinity  || dist == -infinity || dist != dist /* nan */)
            throw new ArithmeticException("Cannot resize to distance " + dist);
        else if (   (x == 0 && y == 0 && z == 0) ||
            x == infinity || x == -infinity || x != x || 
            y == infinity || y == -infinity || y != y || 
            z == infinity || z == -infinity || z != z )
            throw new ArithmeticException("Cannot resize a vector with infinite or NaN values, or of length 0, except to length 0");

        double temp = length();
        return new Double3D(x * dist / temp, y * dist / temp, z * dist / temp);
        }

    static final double infinity = 1.0 / 0.0;

    /** Normalizes the vector (sets its length to 1).  If the vector has NaN or infinite values,
        or has all zero values, then an exception will be thrown.*/
    public final Double3D normalize()
        {
        /*
          final double invertedlen = 1.0 / Math.sqrt(x * x + y * y + z * z);
          if (invertedlen == infinity || invertedlen == -infinity || invertedlen == 0 || invertedlen != invertedlen)  // nan
          throw new ArithmeticException("" + this + " length is " + Math.sqrt(x * x + y * y + z * z) + ", cannot normalize");
          return new Double3D(x * invertedlen, y * invertedlen, z * invertedlen);
        */
        return resize(1.0);
        } 

    /** Takes the dot product this Double3D with another */
    public final double dot(Double3D other)
        {
        return other.x * x + other.y * y + other.z * z;
        }

    /** Returns the negation of this Double3D. */
    public final Double3D negate()
        {
        return new Double3D(-x, -y, -z);
        }
    }
