/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util;

/**
   Int3D stores three values (x, y, and z) but it is immutable: once the x and y and z values are set, they cannot be changed (they're final).  Like the others, Int3D is immutable primarily to prevent hash tables from breaking.

   <p>One day in the far future, Int3D should also be HIGHLY efficient; since it is immutable, it can be passed by value rather than by pointer by a smart compiler.  Not today, though.  But it's not bad.

   <p>This class has an elaborate hash code generation that is much more random than Sun's standard generator, but takes more time.  For very large numbers of objects, this is a good idea, but we may change it to a simpler version in the future.

   <p>Int3D.equals(...) can compare by value against other Int3Ds, MutableInt2Ds, and Double3Ds.
*/
public final class Int3D implements java.io.Serializable
    {
    public final int x;
    public final int y;
    public final int z;
    
    public Int3D() { x = 0; y = 0; z = 0;}
    public Int3D(final int x, final int y, final int z) { this.x = x; this.y = y; this.z = z;}
    /** Explicitly assumes the z value is set to 0 */
    public Int3D(final Int2D p) { x = p.x; y = p.y; z = 0; }
    public Int3D(final Int2D p, final int z) { x = p.x; y = p.y; this.z = z; }
    public Int3D(final MutableInt2D p) { x = p.x; y = p.y; z = 0; }
    public Int3D(final MutableInt2D p, final int z) { x = p.x; y = p.y; this.z = z; }
    public final int getX() { return x; }
    public final int getY() { return y; }
    public final int getZ() { return z; }
    public String toString() { return "Int3D["+x+","+y+","+z+"]"; }
    public String toCoordinates() { return "(" + x + ", " + y + ", " + z + ")"; }    
    
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
