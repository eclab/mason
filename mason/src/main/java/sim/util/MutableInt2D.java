/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util;

/** 
    MutableInt2D is largely a class identical to java.awt.Point, except that it is hash-equivalent to Int2D.
    It is used internally in SparseGrid2D and Continuous2D to do neighborhood lookups without having to 
    create an Int2D every time (which causes lots of garbage collection).
        
    <p>You use MutableInt2D as a STORED hash key at your peril: it has the same misfeature as Point, and you 
    should read the warning in Int2D.  However, you can look up Int2D-keyed objects in a hash table by passing in
    a MutableInt2D instead.
*/

public class MutableInt2D implements java.io.Serializable, Cloneable
    {
    private static final long serialVersionUID = 1;

    public int x;
    public int y;
   
    public MutableInt2D() { x = 0; y = 0; }
    public MutableInt2D(final java.awt.Point p) { x = p.x; y = p.y; }
    public MutableInt2D(final Int2D p) { x = p.x; y = p.y; }
    public MutableInt2D(final int x, final int y) { this.x = x; this.y = y; }
    public final int getX() { return x; }
    public final int getY() { return y; }
    public final void setX(int val) { x = val; }
    public final void setY(int val) { y = val; }
    public void setTo(int x, int y) { this.x = x; this.y = y; }
    public void setTo(java.awt.Point p) { x = p.x; y = p.y; }
    public void setTo(Int2D p) { x = p.x; y = p.y; }
    public void setTo(MutableInt2D p) { x = p.x; y = p.y; }
    /** @deprecated use setTo */
    public void setLocation(int x, int y) { this.x = x; this.y = y; }
    /** @deprecated use setTo */
    public void setLocation(java.awt.Point p) { x = p.x; y = p.y; }
    /** @deprecated use setTo */
    public void setLocation(Int2D p) { x = p.x; y = p.y; }
    /** @deprecated use setTo */
    public void setLocation(MutableInt2D p) { x = p.x; y = p.y; }
    public java.awt.geom.Point2D.Double toPoint2D() { return new java.awt.geom.Point2D.Double(x,y); }
    public java.awt.Point toPoint() { return new java.awt.Point(x,y); }
    public String toString() { return "MutableInt2D["+x+","+y+"]"; }
    public String toCoordinates() { return "(" + x + ", " + y + ")"; }
    public Object clone()
        { 
        try 
            { 
            return super.clone(); 
            }
        catch(CloneNotSupportedException e)
            { 
            return null; // never happens
            } 
        }

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
        // This is good for us because MutableInt2D, Int3D, Double2D, and Double3D
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
                
    /** Returns the manhattan distance FROM this Int2D TO the specified point.    */
    public long manhattanDistance(final java.awt.Point p)
        {
        return Math.abs((long)this.x-p.x) + Math.abs((long)this.y-p.y);
        }
    }
