/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util;

/** 
    Int2D is more or less the same class as java.awt.Point, but it is immutable: once the x and y values are set, they cannot be changed (they're final).  Why use this immutable class when you could just use Point?  Because Point is broken with respect to hash tables.  You use Point as a key in a hash table at your own peril.  Try this: hash an object by a Point as key.  Then change the x value of the original Point.  Ta-da!  The object is lost in the hash table.

    <p>One day in the far future, Int2D should also be HIGHLY efficient; since it is immutable, it can be passed by value rather than by pointer by a smart compiler.  Not today, though.  But it's not bad.

    <p>This class has an elaborate hash code generation that is much more random than Sun's standard generator, but takes more time.  For very large numbers of objects, this is a good idea, but we may change it to a simpler version in the future.

    <p>Int2D.equals(...) can compare by value against other Int2Ds, MutableInt2Ds, Double2Ds, and MutableDouble2Ds.
*/
public final class Int2D implements java.io.Serializable
    {
    public final int x;
    public final int y;
   
    public Int2D() { x = 0; y = 0; }
    public Int2D(final java.awt.Point p) { x = p.x; y = p.y; }
    public Int2D(final MutableInt2D p) { x = p.x; y = p.y; }
    public Int2D(final int x, final int y) { this.x = x; this.y = y; }
    public final int getX() { return x; }
    public final int getY() { return y; }
    /* public final java.awt.Point getPoint() { return new java.awt.Point(x,y); } */
    public final java.awt.Point toPoint() { return new java.awt.Point(x,y); }
    public String toString() { return "Int2D["+x+","+y+"]"; }
    public String toCoordinates() { return "(" + x + ", " + y + ")"; }

    public final int hashCode()
        {
        int y = this.y;
        
        // basically we need to randomly disperse <int,int> --> int
        // I do that by hashing (shuffling) y, then xoring it with x. So I 
        // need something that will hash y to a nicely random value.
        // this taken from http://www.cris.com/~Ttwang/tech/inthash.htm

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
        
        y += ~(y << 15);
        y ^=  (y >>> 10);
        y +=  (y << 3);
        y ^=  (y >>> 6);
        y += ~(y << 11);
        y ^=  (y >>> 16);

        // nifty!  Now mix in x
        
        return x ^ y;
        }
    

    // can't have separate equals(...) methods as the
    // argument isn't virtual
    public final boolean equals(Object obj)
        {
        if (obj==null) return false;
        else if (obj instanceof Int2D)  // do Int2D first
            {
            Int2D other = (Int2D) obj;
            return (other.x == x && other.y == y);
            }
        else if (obj instanceof MutableInt2D)
            {
            MutableInt2D other = (MutableInt2D) obj;
            return (other.x == x && other.y == y);
            }
        else if (obj instanceof Double2D)
            {
            Double2D other = (Double2D) obj;
            return (other.x == x && other.y == y);
            }
        else if (obj instanceof MutableDouble2D)
            {
            MutableDouble2D other = (MutableDouble2D) obj;
            return (other.x == x && other.y == y);
            }
        else return false;
        }


    /** Returns the distance FROM this MutableInt2D TO the specified point */
    public double distance(final double x, final double y)
        {
        final double dx = (double)this.x - x;
        final double dy = (double)this.y - y;
        return Math.sqrt(dx*dx+dy*dy);
        }

    /** Returns the distance FROM this MutableInt2D TO the specified point.   */
    public double distance(final Double2D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        return Math.sqrt(dx*dx+dy*dy);
        }

    /** Returns the distance FROM this MutableInt2D TO the specified point.    */
    public double distance(final MutableInt2D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        return Math.sqrt(dx*dx+dy*dy);
        }

    /** Returns the distance FROM this MutableInt2D TO the specified point.    */
    public double distance(final Int2D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        return Math.sqrt(dx*dx+dy*dy);
        }

    /** Returns the distance FROM this MutableInt2D TO the specified point.    */
    public double distance(final java.awt.geom.Point2D p)
        {
        final double dx = (double)this.x - p.getX();
        final double dy = (double)this.y - p.getY();
        return Math.sqrt(dx*dx+dy*dy);
        }

    /** Returns the squared distance FROM this MutableInt2D TO the specified point */
    public double distanceSq(final double x, final double y)
        {
        final double dx = (double)this.x - x;
        final double dy = (double)this.y - y;
        return (dx*dx+dy*dy);
        }

    /** Returns the squared distance FROM this MutableInt2D TO the specified point.    */
    public double distanceSq(final Double2D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        return (dx*dx+dy*dy);
        }

    /** Returns the squared distance FROM this MutableInt2D TO the specified point */
    public double distanceSq(final java.awt.geom.Point2D p)
        {
        final double dx = (double)this.x - p.getX();
        final double dy = (double)this.y - p.getY();
        return (dx*dx+dy*dy);
        }

    /** Returns the squared distance FROM this MutableInt2D TO the specified point.    */
    public double distanceSq(final MutableInt2D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        return (dx*dx+dy*dy);
        }

    /** Returns the squared distance FROM this MutableInt2D TO the specified point.    */
    public double distanceSq(final Int2D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        return (dx*dx+dy*dy);
        }

    /** Returns the manhattan distance FROM this MutableInt2D TO the specified point.    */
    public long manhattanDistance(final int x, final int y)
        {
        return Math.abs((long)this.x-x) + Math.abs((long)this.y-y);
        }

    /** Returns the manhattan distance FROM this MutableInt2D TO the specified point.    */
    public long manhattanDistance(final MutableInt2D p)
        {
        return Math.abs((long)this.x-p.x) + Math.abs((long)this.y-p.y);
        }
        
    /** Returns the manhattan distance FROM this MutableInt2D TO the specified point.    */
    public long manhattanDistance(final Int2D p)
        {
        return Math.abs((long)this.x-p.x) + Math.abs((long)this.y-p.y);
        }
    }
