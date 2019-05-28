/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util;

/** 
    Double2D is more or less the same class as java.awt.geom.Point2D.Double, but it is immutable: once the x and y values are set, they cannot be changed (they're final).  Why use this immutable class when you could just use Point2D?  Because Point2D is broken with respect to hash tables.  You use Point2D as a key in a hash table at your own peril.  Try this: hash an object by a Point2D as key.  Then change the x value of the original Point2D.  Ta-da!  The object is lost in the hash table.

    <p>One day in the far future, Double3D should also be HIGHLY efficient; since it is immutable, it can be passed by value rather than by pointer by a smart compiler.  Not today, though.  But it's not bad.

    <p>This class has an elaborate hash code generation that is much more random than Sun's standard generator, but takes more time.  For very large numbers of objects, this is a good idea, but we may change it to a simpler version in the future.

    <p>Double2D.equals(...) can compare by value against other Int2Ds and Double2Ds.
*/
public final class Double2D implements java.io.Serializable
    {
    private static final long serialVersionUID = 1;

    public final double x;
    public final double y;
    
    public Double2D() { x = 0.0; y = 0.0; }
    public Double2D(final Int2D p) { x = p.x; y = p.y; }
    public Double2D(final MutableInt2D p) { x = p.x; y = p.y; }
    public Double2D(final MutableDouble2D p) { x = p.x; y = p.y; }
    public Double2D(final java.awt.Point p) { x = p.x; y = p.y; }
    public Double2D(final java.awt.geom.Point2D.Double p) { x = p.x; y = p.y; }
    public Double2D(final java.awt.geom.Point2D.Float p) { x = p.x; y = p.y; }
    /** Only included for completeness' sakes, in case a new Point2D subclass is created in the future. */
    public Double2D(final java.awt.geom.Point2D p) { x = p.getX(); y = p.getY(); }
    public Double2D(final double x, final double y) { this.x = x; this.y = y; }
    public final double getX() { return x; }
    public final double getY() { return y; }
    public String toString() { return "Double2D["+x+","+y+"]"; }
    public String toCoordinates() { return "(" + x + ", " + y + ")"; }
    
    public java.awt.geom.Point2D.Double toPoint2D() { return new java.awt.geom.Point2D.Double(x,y); }
        
    public final int hashCode()
        {
        double x = this.x;
        double y = this.y;
                
        // push -0.0 to 0.0 for purposes of hashing.  Note that equals() has also been modified
        // to consider -0.0 to be equal to 0.0.  Hopefully cute Java compilers won't try to optimize this out.
        if (x == -0.0) x = 0.0;
        if (y == -0.0) y = 0.0;
                
        // so we hash to the same value as Int2D does, if we're ints.
        if ((((int)x) == x) && ((int)y) == y)
            //return Int2D.hashCodeFor((int)x,(int)y);
            
            {
            int y_ = (int)y;
            int x_ = (int)x;

            // copied from Int2D and inserted here because hashCodeFor can't be
            // inlined and this saves us a fair chunk on some hash-heavy applications

            y_ += ~(y_ << 15);
            y_ ^=  (y_ >>> 10);
            y_ +=  (y_ << 3);
            y_ ^=  (y_ >>> 6);
            y_ += ~(y_ << 11);
            y_ ^=  (y_ >>> 16);

            // nifty!  Now mix in x
            
            return x_ ^ y_;
            }
            
            
        // I don't like Sun's simplistic approach to random shuffling.  So...
        // basically we need to randomly disperse <double,double> --> int
        // We do this by doing <double,double> -> <long,long> -> long -> int
        // The first step is done with doubleToLongBits (not RawLongBits;
        // we want all NaN to hash to the same thing).  Then conversion to
        // a single long is done by hashing (shuffling) y, then xoring it with x.
        // So I need something that will hash y to a nicely random value.
        // this taken from http://www.cris.com/~Ttwang/tech/inthash.htm
        // Last we fold the long onto itself to form the int.

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
        
        long key = Double.doubleToLongBits(y);
            
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
    public final boolean equals(final Object obj)
        {
        if (obj==null) return false;
        else if (obj instanceof Double2D)  // do Double2D first
            {
            Double2D other = (Double2D) obj;
            // Note: commented out because it can't handle 0.0 == -0.0, grrr
            return ((x == other.x || (Double.isNaN(x) && Double.isNaN(other.x))) && // they're the same or they're both NaN
                (y == other.y || (Double.isNaN(y) && Double.isNaN(other.y)))); // they're the same or they're both NaN

            // can't just do other.x == x && other.y == y because we need to check for NaN
            // return (Double.doubleToLongBits(other.x) == Double.doubleToLongBits(x) &&
            //    Double.doubleToLongBits(other.y) == Double.doubleToLongBits(y));
            }
        if (obj instanceof MutableDouble2D)
            {
            MutableDouble2D other = (MutableDouble2D) obj;
            // Note: commented out because it can't handle 0.0 == -0.0, grrr
            return ((x == other.x || (Double.isNaN(x) && Double.isNaN(other.x))) && // they're the same or they're both NaN
                (y == other.y || (Double.isNaN(y) && Double.isNaN(other.y)))); // they're the same or they're both NaN

            // can't just do other.x == x && other.y == y because we need to check for NaN
            // return (Double.doubleToLongBits(other.x) == Double.doubleToLongBits(x) &&
            //     Double.doubleToLongBits(other.y) == Double.doubleToLongBits(y));
            }
        else if (obj instanceof Int2D)
            {
            Int2D other = (Int2D) obj;
            return (other.x == x && other.y == y);
            }
        else if (obj instanceof MutableInt2D)
            {
            MutableInt2D other = (MutableInt2D) obj;
            return (other.x == x && other.y == y);
            }
        else return false;
        }
                
        
    /** Returns the distance FROM this Double2D TO the specified point */
    public double distance(final double x, final double y)
        {
        final double dx = (double)this.x - x;
        final double dy = (double)this.y - y;
        return Math.sqrt(dx*dx+dy*dy);
        }

    /** Returns the distance FROM this Double2D TO the specified point.   */
    public double distance(final Double2D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        return Math.sqrt(dx*dx+dy*dy);
        }

    /** Returns the distance FROM this Double2D TO the specified point.    */
    public double distance(final Int2D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        return Math.sqrt(dx*dx+dy*dy);
        }

    /** Returns the distance FROM this Double2D TO the specified point.    */
    public double distance(final MutableInt2D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        return Math.sqrt(dx*dx+dy*dy);
        }

    /** Returns the distance FROM this Double2D TO the specified point.    */
    public double distance(final java.awt.geom.Point2D p)
        {
        final double dx = (double)this.x - p.getX();
        final double dy = (double)this.y - p.getY();
        return Math.sqrt(dx*dx+dy*dy);
        }

    /** Returns the distance FROM this Double2D TO the specified point */
    public double distanceSq(final double x, final double y)
        {
        final double dx = (double)this.x - x;
        final double dy = (double)this.y - y;
        return (dx*dx+dy*dy);
        }

    /** Returns the distance FROM this Double2D TO the specified point.    */
    public double distanceSq(final Double2D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        return (dx*dx+dy*dy);
        }

    /** Returns the distance FROM this Double2D TO the specified point.    */
    public double distanceSq(final Int2D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        return (dx*dx+dy*dy);
        }

    /** Returns the distance FROM this Double2D TO the specified point.    */
    public double distanceSq(final MutableInt2D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        return (dx*dx+dy*dy);
        }

    /** Returns the distance FROM this Double2D TO the specified point */
    public double distanceSq(final java.awt.geom.Point2D p)
        {
        final double dx = (double)this.x - p.getX();
        final double dy = (double)this.y - p.getY();
        return (dx*dx+dy*dy);
        }

    /** Returns the manhtattan distance FROM this Double2D TO the specified point */
    public double manhattanDistance(final double x, final double y)
        {
        final double dx = Math.abs((double)this.x - x);
        final double dy = Math.abs((double)this.y - y);
        return dx + dy;
        }

    /** Returns the manhtattan distance FROM this Double2D TO the specified point */
    public double manhattanDistance(final Double2D p)
        {
        final double dx = Math.abs((double)this.x - p.x);
        final double dy = Math.abs((double)this.y - p.y);
        return dx + dy;
        }

    /** Returns the manhtattan distance FROM this Double2D TO the specified point */
    public double manhattanDistance(final Int2D p)
        {
        final double dx = Math.abs((double)this.x - p.x);
        final double dy = Math.abs((double)this.y - p.y);
        return dx + dy;
        }

    /** Returns the manhtattan distance FROM this Double2D TO the specified point */
    public double manhattanDistance(final MutableDouble2D p)
        {
        final double dx = Math.abs((double)this.x - p.x);
        final double dy = Math.abs((double)this.y - p.y);
        return dx + dy;
        }

    /** Returns the manhtattan distance FROM this Double2D TO the specified point */
    public double manhattanDistance(final MutableInt2D p)
        {
        final double dx = Math.abs((double)this.x - p.x);
        final double dy = Math.abs((double)this.y - p.y);
        return dx + dy;
        }

    /** Returns the manhtattan distance FROM this Double2D TO the specified point */
    public double manhattanDistance(final java.awt.geom.Point2D p)
        {
        final double dx = Math.abs((double)this.x - p.getX());
        final double dy = Math.abs((double)this.y - p.getY());
        return dx + dy;
        }

    public final Double2D add(Double2D other)
        {
        return new Double2D(x + other.x, y + other.y);
        }

    /** Subtracts Double2D "other" from current Double2D using 
     * vector subtraction */
    public final Double2D subtract(Double2D other)
        {
        return new Double2D(x - other.x, y - other.y);
        }
        
    /** Returns the vector length of the Double2D */
    public final double length()
        {
        return Math.sqrt(x * x + y * y);
        }
        
    /** Returns the length of the vector between -Pi and Pi. */
    public final double angle()
        {
        return Math.atan2(y,x);
        }

    /** Returns the vector length of the Double2D */
    public final double lengthSq()
        {
        return x*x+y*y;
        }
        
    /** Multiplies each element by scalar "val" */
    public final Double2D multiply(double val)
        {
        return new Double2D(x * val, y * val);
        }



    /** Scales the vector to length "dist".  dist must be a finite value.  If the vector has
        NaN, zero, or infinite values, then the vector cannot be resized to any length except for 0:
        other lengths will throw an exception in this case. */
    public final Double2D resize(double dist)
        {
        if (dist == 0)
            return new Double2D(0, 0);
        else if (dist == infinity  || dist == -infinity || dist != dist /* nan */)
            throw new ArithmeticException("Cannot resize to distance " + dist);
        else if (   (x == 0 && y == 0) ||
            x == infinity || x == -infinity || x != x || 
            y == infinity || y == -infinity || y != y )
            throw new ArithmeticException("Cannot resize a vector with infinite or NaN values, or of length 0, except to length 0");

        double temp = length();
        return new Double2D(x * dist / temp, y * dist / temp);
        }

    static final double infinity = 1.0 / 0.0;

    /** Normalizes the vector (sets its length to 1).  If the vector has NaN or infinite values,
        or has all zero values, then an exception will be thrown.*/
    public final Double2D normalize()
        {
        /*
          final double invertedlen = 1.0 / Math.sqrt(x * x + y * y);
          if (invertedlen == infinity || invertedlen == -infinity || invertedlen == 0 || invertedlen != invertedlen) // nan
          throw new ArithmeticException("" + this + " length is " + Math.sqrt(x * x + y * y) + ", cannot normalize");
          return new Double2D(x * invertedlen,  y * invertedlen);
        */
        return resize(1.0);
        } 

    /** Takes the dot product this Double2D with another */
    public final double dot(Double2D other)
        {
        return other.x * x + other.y * y;
        }

    /** 2D version of the cross product. Rotates current
     * Vector2D 90 degrees and takes the dot product of 
     * the result and Double2D "other" */
    public double perpDot(Double2D other)
        {
        // this is the equivalent of multiplying by a 2x2 rotation
        // matrix since cos(90) = 0 and sin(90) = 1
        /*
          Double2D rotated90 = new Double2D(-this.y, this.x);
          return rotated90.dotProduct(other);
        */
        return (-this.y) * other.x + this.x * other.y;          
        }

    /** Returns the negation of this Double2D. */
    public final Double2D negate()
        {
        return new Double2D(-x, -y);
        }

    /** Rotates the Double2D by theta radians */
    public final Double2D rotate(double theta)
        {
        /*
        // Do the equivalent of multiplying by a 2D rotation
        // matrix without the overhead of converting the Double2D into
        // a matrix
        double sinTheta = Math.sin(theta);
        double cosTheta = Math.cos(theta);
                
        return new Double2D(cosTheta * this.x + -sinTheta * this.y, sinTheta * this.x + cosTheta * this.y);
        */

        final double sinTheta = Math.sin(theta);
        final double cosTheta = Math.cos(theta);
        final double x = this.x;
        final double y = this.y;
        return new Double2D(cosTheta * x + -sinTheta * y, sinTheta * x + cosTheta * y);
        }
    }
