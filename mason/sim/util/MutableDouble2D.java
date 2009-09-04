/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util;

/** 
    MutableDouble2D is more or less the same class as java.awt.geom.Point2D.Double, except that it is hash-equivalent to Double2D.  
        
    <p>Just as with MutableInt2D: you use MutableDouble2D as a STORED hash key at your peril: it has the same misfeature as 
    java.awt.geom.Point2D.Double, and you should read the warning in Double2D.  However, you can look up Double2D-keyed 
    objects in a hash table by passing in a MutableDouble2D instead.    
*/

public final class MutableDouble2D implements java.io.Serializable
    {
    public double x;
    public double y;
    
    public MutableDouble2D() { x = 0.0; y = 0.0; }
    public MutableDouble2D(final Int2D p) { x = p.x; y = p.y; }
    public MutableDouble2D(final MutableInt2D p) { x = p.x; y = p.y; }
    public MutableDouble2D(final MutableDouble2D p) { x = p.x; y = p.y; }
    public MutableDouble2D(final Double2D p) { x = p.x; y = p.y; }
    public MutableDouble2D(final java.awt.Point p) { x = p.x; y = p.y; }
    public MutableDouble2D(final java.awt.geom.Point2D.Double p) { x = p.x; y = p.y; }
    public MutableDouble2D(final java.awt.geom.Point2D.Float p) { x = p.x; y = p.y; }
    /** Only included for completeness' sakes, in case a new Point2D subclass is created in the future. */
    public MutableDouble2D(final java.awt.geom.Point2D p) { x = p.getX(); y = p.getY(); }
    public MutableDouble2D(final double x, final double y) { this.x = x; this.y = y; }
    public final double getX() { return x; }
    public final double getY() { return y; }
    public final void setX(double val) { x = val; }
    public final void setY(double val) { y = val; }
    public final void setTo(final double bx, final double by) { x = bx; y = by; }
    public final void setTo(final Int2D b) { x = b.x; y = b.y; }
    public final void setTo(final Double2D b) { x = b.x; y = b.y; }
    public final void setTo(final MutableInt2D b) { x = b.x;  y = b.y; }
    public final void setTo(final MutableDouble2D b) { x = b.x;  y = b.y; }
    public String toString() { return "MutableDouble2D["+x+","+y+"]"; }
    public String toCoordinates() { return "(" + x + ", " + y + ")"; }

    // identical to Double2D
    public final int hashCode()
        {
        double x = this.x;
        double y = this.y;
                
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
        // This is good for us because Int2D, Int3D, MutableDouble2D, and Double3D
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
            // can't just do other.x == x && other.y == y because we need to check for NaN
            return (Double.doubleToLongBits(other.x) == Double.doubleToLongBits(x) &&
                Double.doubleToLongBits(other.y) == Double.doubleToLongBits(y));
            }
        if (obj instanceof MutableDouble2D)
            {
            MutableDouble2D other = (MutableDouble2D) obj;
            // can't just do other.x == x && other.y == y because we need to check for NaN
            return (Double.doubleToLongBits(other.x) == Double.doubleToLongBits(x) &&
                Double.doubleToLongBits(other.y) == Double.doubleToLongBits(y));
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


    /** Returns the distance FROM this MutableDouble2D TO the specified point */
    public double distance(final double x, final double y)
        {
        final double dx = (double)this.x - x;
        final double dy = (double)this.y - y;
        return Math.sqrt(dx*dx+dy*dy);
        }

    /** Returns the distance FROM this MutableDouble2D TO the specified point.   */
    public double distance(final MutableDouble2D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        return Math.sqrt(dx*dx+dy*dy);
        }

    /** Returns the distance FROM this MutableDouble2D TO the specified point.   */
    public double distance(final Double2D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        return Math.sqrt(dx*dx+dy*dy);
        }

    /** Returns the distance FROM this MutableDouble2D TO the specified point.    */
    public double distance(final Int2D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        return Math.sqrt(dx*dx+dy*dy);
        }

    /** Returns the distance FROM this MutableDouble2D TO the specified point.    */
    public double distance(final MutableInt2D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        return Math.sqrt(dx*dx+dy*dy);
        }

    /** Returns the distance FROM this MutableDouble2D TO the specified point.    */
    public double distance(final java.awt.geom.Point2D p)
        {
        final double dx = (double)this.x - p.getX();
        final double dy = (double)this.y - p.getY();
        return Math.sqrt(dx*dx+dy*dy);
        }

    /** Returns the distance FROM this MutableDouble2D TO the specified point */
    public double distanceSq(final double x, final double y)
        {
        final double dx = (double)this.x - x;
        final double dy = (double)this.y - y;
        return (dx*dx+dy*dy);
        }

    /** Returns the distance FROM this MutableDouble2D TO the specified point.    */
    public double distanceSq(final Double2D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        return (dx*dx+dy*dy);
        }

    /** Returns the distance FROM this MutableDouble2D TO the specified point.    */
    public double distanceSq(final MutableDouble2D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        return (dx*dx+dy*dy);
        }

    /** Returns the distance FROM this MutableDouble2D TO the specified point.    */
    public double distanceSq(final Int2D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        return (dx*dx+dy*dy);
        }

    /** Returns the distance FROM this MutableDouble2D TO the specified point.    */
    public double distanceSq(final MutableInt2D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        return (dx*dx+dy*dy);
        }

    /** Returns the distance FROM this Point2D TO the specified point */
    public double distanceSq(final java.awt.geom.Point2D p)
        {
        final double dx = (double)this.x - p.getX();
        final double dy = (double)this.y - p.getY();
        return (dx*dx+dy*dy);
        }

    /** Adds other into me, returning me. */
    public final MutableDouble2D addIn(final Double2D other)
        {
        x = other.x + x;
        y = other.y + y;
        return this;
        }
            
    /** Adds other into me, returning me. */
    public final MutableDouble2D addIn(final MutableDouble2D other)
        {
        x = other.x + x;
        y = other.y + y;
        return this;
        }
            
    /** Adds the x and y values into my x and y values, returning me. */
    public final MutableDouble2D addIn(final double x, final double y)
        {
        this.x += x;
        this.y += y;
        return this;
        }

    /** Sets me to the sum of other1 and other2, returning me. */
    public final MutableDouble2D add(final MutableDouble2D other1, final MutableDouble2D other2)
        {
        x = other1.x + other2.x;
        y = other1.y + other2.y;
        return this;
        }
    
    /** Equivalent to <tt>(new MutableDouble2D(d))</tt>, but <tt>(d.dup())</tt> shorter of course, but perhaps not quite as fast. */
    public final MutableDouble2D dup()
        {
        return new MutableDouble2D(this);
        }


    /** Sets me to me minus other, returning me.  */
    public final MutableDouble2D subtractIn(Double2D other)
        {
        x = x - other.x;
        y = y - other.y;
        return this;
        }

    /** Sets me to me minus other, returning me.  */
    public final MutableDouble2D subtractIn(MutableDouble2D other)
        {
        x = x - other.x;
        y = y - other.y;
        return this;
        }

    /** Subtracts other2 from other1, setting me to the result and returning me. */
    public final MutableDouble2D subtract(MutableDouble2D other1, MutableDouble2D other2)
        {
        x = other1.x - other2.x;
        y = other1.y - other2.y;
        return this;
        }
            
    /** Returns the length of the vector. */
    public final double length()
        {
        return Math.sqrt(x * x + y * y);
        }
        
    /** Returns the length of the vector between -Pi and Pi. */
    public final double angle()
        {
        return Math.atan2(y,x);
        }
        
    /** Extends my length so that it is multiplied by val, and returns me. */
    public final MutableDouble2D multiplyIn(final double val)
        {
        x = x * val;
        y = y * val;
        return this;
        }

    /** Multiplies other by val, setting me to the result and returning me. */
    public final MutableDouble2D multiply(MutableDouble2D other, final double val)
        {
        x = other.x * val;
        y = other.y * val;
        return this;
        }

    /** Normalizes me (sets my length to 1.0), returning me.  Throws an error if my previous length was of length 0. */
    static final double infinity = 1.0 / 0.0;
    public final MutableDouble2D normalize()
        {
        final double invertedlen = 1.0 / Math.sqrt(x * x + y * y);
        if (invertedlen == infinity || invertedlen == -infinity || invertedlen == 0 || invertedlen != invertedlen /* nan */)
            throw new ArithmeticException("" + this + " length is " + Math.sqrt(x * x + y * y) + ", cannot normalize");
        x = x * invertedlen;
        y = y * invertedlen;
        return this;
        } 
                
    /** Sets my length, which should be >= 0. */
    public final MutableDouble2D setLength(double val)
        {
        if (val < 0) 
            throw new IllegalArgumentException("The argument to MutableDouble2D.setLength(...) must be zero or positive");
        if (val == 0) x = y = 0;
        else
            {
            final double len = Math.sqrt(x * x + y * y);
            if (len != len || len == infinity || len == -infinity || len == 0)
                throw new ArithmeticException("" + this + " length is "+ len + " cannot change its length");
            final double invertedlen = val / len;
            x = x * invertedlen;
            y = y * invertedlen;
            }
        return this;
        } 

    /** Rotates me by theta radians, returning me. */
    public final MutableDouble2D rotate(double theta)
        {
        // Do the equivalent of multiplying by a 2D rotation
        // matrix without the overhead of converting the Double2D into
        // a matrix
        final double sinTheta = Math.sin(theta);
        final double cosTheta = Math.cos(theta);
        final double x = this.x;
        final double y = this.y;
        this.x = cosTheta * x + -sinTheta * y;
        this.y = sinTheta * x + cosTheta * y;
        return this;
        }

    /** Returns the dot product of myself against other, that is me DOT other. */
    public final double dot(MutableDouble2D other)
        {
        return other.x * x + other.y * y;
        }

    /** 2D version of the cross product: returns the dot product of me rotated 90 degrees dotted
        against the other vector.  Does not modify either vector.  */
    public double perpDot(MutableDouble2D other)
        {
        return (-this.y) * other.x + this.x * other.y;
        }
        
    /** Sets the values to 0. */
    public final void zero()                                                                        
        {
        this.x = 0; 
        this.y = 0;
        }

    /** Sets the values to the negation of the values in the provided MutableDouble2D */
    public final void setToMinus(final MutableDouble2D b)           
        {
        x = -b.x; 
        y = -b.y;
        }
                
    /** Negates the MutableDouble2D's values */
    public final void negate()                                                             
        {
        x = -x; 
        y = -y;
        }
        
    /** Returns the square of the length of the MutableDouble2D. */
    public final double lengthSq()                                                  
        {
        return x*x+y*y;
        }
    }
