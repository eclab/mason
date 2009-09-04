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

    }
