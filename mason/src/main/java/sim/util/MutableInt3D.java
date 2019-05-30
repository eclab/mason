/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util;

/**
   MutableInt3D stores three values (x, y, and z) and is hash-equivalent to Int3D; except MutableInt3D's
   values can be modified and Int3D's values cannot.  
   It is used internally in SparseGrid3D and Continuous3D to do neighborhood lookups without having to 
   create an Int3D every time (which causes lots of garbage collection).
        
   <p>You use MutableInt3D as a STORED hash key at your peril: it has the same misfeature as Point, and you 
   should read the warning in Int3D.  However, you can look up Int3D-keyed objects in a hash table by passing in
   a MutableInt3D instead.
*/

public final class MutableInt3D implements java.io.Serializable, Cloneable
    {
    private static final long serialVersionUID = 1;

    public int x;
    public int y;
    public int z;
    
    public MutableInt3D() { x = 0; y = 0; z = 0;}
    public MutableInt3D(final int x, final int y, final int z) { this.x = x; this.y = y; this.z = z;}
    /** Explicitly assumes the z value is set to 0 */
    public MutableInt3D(final Int2D p) { x = p.x; y = p.y; z = 0; }
    public MutableInt3D(final Int2D p, final int z) { x = p.x; y = p.y; this.z = z; }
    public MutableInt3D(final Int3D p) { x = p.x; y = p.y; z = p.z; }
    public MutableInt3D(final MutableInt2D p) { x = p.x; y = p.y; z = 0; }
    public MutableInt3D(final MutableInt2D p, final int z) { x = p.x; y = p.y; this.z = z; }
    public final int getX() { return x; }
    public final int getY() { return y; }
    public final int getZ() { return z; }
    public final void setX(int val) { x = val; }
    public final void setY(int val) { y = val; }
    public final void setZ(int val) { z = val; }
    public void setTo(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
    public void setTo(Int3D p) { x = p.x; y = p.y; z = p.z; }
    public void setTo(MutableInt3D p) { x = p.x; y = p.y; z = p.z; }
    /** @deprecated use setTo */
    public void setLocation(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
    /** @deprecated use setTo */
    public void setLocation(Int3D p) { x = p.x; y = p.y; z = p.z; }
    /** @deprecated use setTo */
    public void setLocation(MutableInt3D p) { x = p.x; y = p.y; z = p.z; }
    public String toString() { return "MutableInt3D["+x+","+y+","+z+"]"; }
    public String toCoordinates() { return "(" + x + ", " + y + ", " + z + ")"; }    
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
    
    public int hashCode()
        {
        int z = this.z;
        
        // basically we need to randomly disperse <int,int,int> --> int
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
        
        z += ~(z << 15);
        z ^=  (z >>> 10);
        z +=  (z << 3);
        z ^=  (z >>> 6);
        z += ~(z << 11);
        z ^=  (z >>> 16);
        
        z ^= y;
        z += 17;    // a little prime number shifting -- waving a dead chicken?  dunno
        
        z += ~(z << 15);
        z ^=  (z >>> 10);
        z +=  (z << 3);
        z ^=  (z >>> 6);
        z += ~(z << 11);
        z ^=  (z >>> 16);

        // nifty!  Now mix in x
        
        return x ^ z;
        }
    
    // can't have separate equals(...) methods as the
    // argument isn't virtual
    public boolean equals(final Object obj)
        {
        if (obj==null) return false;
        else if (obj instanceof Int3D)  // do Int3D first
            {
            Int3D other = (Int3D) obj;
            return (other.x == x && other.y == y && other.z == z);
            }
        else if (obj instanceof MutableInt3D)
            {
            MutableInt3D other = (MutableInt3D) obj;
            return (other.x == x && other.y == y && other.z == z);
            }
        else if (obj instanceof Double3D)
            {
            Double3D other = (Double3D) obj;
            return (other.x == x && other.y == y && other.z == z);
            }
        else if (obj instanceof MutableDouble3D)
            {
            MutableDouble3D other = (MutableDouble3D) obj;
            return (other.x == x && other.y == y && other.z == z);
            }
        else return false;
        }


    /** Returns the distance FROM this Int3D TO the specified point */
    public double distance(final double x, final double y, final double z)
        {
        final double dx = (double)this.x - x;
        final double dy = (double)this.y - y;
        final double dz = (double)this.z - z;
        return Math.sqrt(dx*dx+dy*dy+dz*dz);
        }

    /** Returns the distance FROM this Int3D TO the specified point.   */
    public double distance(final Double3D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        final double dz = (double)this.z - p.z;
        return Math.sqrt(dx*dx+dy*dy+dz*dz);
        }

    /** Returns the distance FROM this Int3D TO the specified point.    */
    public double distance(final MutableInt3D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        final double dz = (double)this.z - p.z;
        return Math.sqrt(dx*dx+dy*dy+dz*dz);
        }

    /** Returns the distance FROM this Int3D TO the specified point.    */
    public double distance(final Int3D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        final double dz = (double)this.z - p.z;
        return Math.sqrt(dx*dx+dy*dy+dz*dz);
        }

    /** Returns the squared distance FROM this Int3D TO the specified point */
    public double distanceSq(final double x, final double y, final double z)
        {
        final double dx = (double)this.x - x;
        final double dy = (double)this.y - y;
        final double dz = (double)this.z - z;
        return (dx*dx+dy*dy+dz*dz);
        }

    /** Returns the squared distance FROM this Int3D TO the specified point.    */
    public double distanceSq(final Double3D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        final double dz = (double)this.z - p.z;
        return (dx*dx+dy*dy+dz*dz);
        }

    /** Returns the squared distance FROM this Int3D TO the specified point.    */
    public double distanceSq(final MutableInt3D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        final double dz = (double)this.z - p.z;
        return (dx*dx+dy*dy+dz*dz);
        }

    /** Returns the squared distance FROM this Int3D TO the specified point.    */
    public double distanceSq(final Int3D p)
        {
        final double dx = (double)this.x - p.x;
        final double dy = (double)this.y - p.y;
        final double dz = (double)this.z - p.z;
        return (dx*dx+dy*dy+dz*dz);
        }

    /** Returns the manhattan distance FROM this Int3D TO the specified point.    */
    public long manhattanDistance(final int x, final int y, final int z)
        {
        return Math.abs((long)this.x-x) + Math.abs((long)this.y-y) + Math.abs((long)this.z-z);
        }

    /** Returns the manhattan distance FROM this Int3D TO the specified point.    */
    public long manhattanDistance(final MutableInt3D p)
        {
        return Math.abs((long)this.x-p.x) + Math.abs((long)this.y-p.y) + Math.abs((long)this.z-p.z);
        }

    /** Returns the manhattan distance FROM this Int3D TO the specified point.    */
    public long manhattanDistance(final Int3D p)
        {
        return Math.abs((long)this.x-p.x) + Math.abs((long)this.y-p.y) + Math.abs((long)this.z-p.z);
        }
    }
