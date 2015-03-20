/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.grid;
import sim.field.*;
import sim.util.*;
import java.util.*;

/**
   A storage facility for sparse objects in discrete 3D space, using Maps.  SparseGrid3D differs from ObjectGrid3D
   in several respects:
    
   <ul>
   <li>SparseGrid3D can store more than one object at a location.  ObjectGrid3D cannot.
   <li>ObjectGrid3D can store an object at more than one location (though it's bad form!).
   <li>SparseGrid3D can efficiently (O(1)) tell you the location of an object.
   <li>SparseGrid3D can efficiently (O(#objs)) scan through all objects.  The best you can do with ObjectGrid3D is search its array (which might have many empty slots).
   <li>Storing an object, finding its location, or changing its location, in a SparseGrid3D is O(1) but requires several Map lookups and/or removes, which has a significant constant overhead.
   <li>SparseGrid3D can associate objects with <i>any</i> 3D integer location.  ObjectGrid3D's locations are restricted to be within its array.
   </ul>

   <p>Generally speaking, if you have a grid of objects, one per location, you should use an ObjectGrid3D.  If you have a large grid occupied by a few objects, or those objects can pile up on the same grid location, you should use a SparseGrid3D.
    
   <p>In either case, you might consider storing the location of an object IN THE OBJECT ITSELF if you need to query for the object location often -- it's faster than the hashtable lookup in SparseGrid3D, and certainly faster than searching the entire array of an ObjectGrid3D.

   <p><b>Boundaries.</b>  SparseGrid3D has no boundaries at all.  <tt>width</tt> and <tt>height</tt> and <tt>length</tt> exist only to allow
   you to define pseudo-boundaries for toroidal computation; and to provide typical bounds for visualization.  But you can
   attach any coordinate as a location for an object with no restrictions.
        
   <b>Setting and getting an object and its Location.</b>  The method <b>setObjectLocation(...)</b> methods set the location of the object
   (to an Int3D or an <x,y,z> location).
   The method <b>getObjectsAtLocation(Object location)</b>, inherited from SparseField, returns a Bag (which you MUST NOT modify)
   containing all objects at a given location (which must be provided in the form of an Int3D or MutableInt3D).  The <b>numObjectsAtLocation(location)</b>
   method returns the number of such objects.  The <b>getObjectsAtLocations(Bag locations, Bag putInHere)</b> gathers objects
   at a variety of locations and puts them in the bag you provide.  The <b>getAllObjects()</b> method returns all objects in a bag you
   must NOT modiify.  The <b>removeObjectsAtLocation(Object location)</b> method removes and returns all objects at a given location
   (defined as an Int3D or MutableDouble3D).  The <b>exists</b> method tells you if the object exists in the field.
        
   <p><b>Neighborhood Lookups.</b>  The method <b>getObjectsAtLocationOfObject</b> returns all Objects at the same location as the provided
   object (in a Bag, which must NOT modify).  The various <b>getNeighbors...Distance(...)</b> methods return all locations defined by certain
   distance bounds, or all the objects stored at those locations.  They are expensive to compute and it may be wiser to compute them by hand
   if there aren't many.

*/

public class SparseGrid3D extends SparseField implements Grid3D, SparseField3D
    {
    private static final long serialVersionUID = 1;

    protected int width;
    protected int height;
    protected int length;
    
    public SparseGrid3D(int width, int height, int length)
        {
        this.width = width;
        this.height = height;
        this.length = length;
        }
        
    public SparseGrid3D(SparseGrid3D values)
        {
        super(values);
        width = values.width;
        height = values.height;
        length = values.length;
        }

    /** Returns the width of the grid */
    public int getWidth() { return width; }
    
    /** Returns the height of the grid */
    public int getHeight() { return height; }
    
    /** Returns the length of the grid */
    public int getLength() { return length; }
    
    /*
      public final int tx(final int x) 
      { 
      final int width = this.width; 
      if (x >= 0) return (x % width); 
      final int width2 = (x % width) + height;
      if (width2 < width) return width2;
      return 0;
      }
    */

    // slight revision for more efficiency
    public final int tx(int x) 
        { 
        final int width = this.width;
        if (x >= 0 && x < width) return x;  // do clearest case first
        x = x % width;
        if (x < 0) x = x + width;
        return x;
        }
        
    /*
      public final int ty(final int y) 
      { 
      final int height = this.height; 
      if (y >= 0) return (y % height); 
      final int height2 = (y % height) + height;
      if (height2 < height) return height2;
      return 0;
      }
    */
        
    // slight revision for more efficiency
    public final int ty(int y) 
        { 
        final int height = this.height;
        if (y >= 0 && y < height) return y;  // do clearest case first
        y = y % height;
        if (y < 0) y = y + height;
        return y;
        }

/*
  public final int tz(final int z) 
  { 
  final int length = this.length; 
  if (z >= 0) return (z % length); 
  final int length2 = (z % length) + length;
  if (length2 < length) return length2;
  return 0;
  }
*/

    // slight revision for more efficiency
    public final int tz(int z) 
        { 
        final int length = this.length;
        if (z >= 0 && z < length) return z;  // do clearest case first
        z = z % length;
        if (z < 0) z = z + height;
        return z;
        }

    public int stx(final int x) 
        { if (x >= 0) { if (x < width) return x; return x - width; } return x + width; }

    public int sty(final int y) 
        { if (y >= 0) { if (y < height) return y ; return y - height; } return y + height; }

    public int stz(final int z) 
        { if (z >= 0) { if (z < length) return z ; return z - length; } return z + length; }

    // faster version
    final int stx(final int x, final int width) 
        { if (x >= 0) { if (x < width) return x; return x - width; } return x + width; }
        
    // faster version
    final int sty(final int y, final int height) 
        { if (y >= 0) { if (y < height) return y ; return y - height; } return y + height; }

    // faster version
    public final int stz(final int z, final int length) 
        { if (z >= 0) { if (z < length) return z ; return z - length; } return z + length; }


    /** Returns the number of objects stored in the grid at the given location. */
    public int numObjectsAtLocation(final int x, final int y, final int z)
        {
        return numObjectsAtLocation(new Int3D(x,y,z));
        }

    /** Returns a bag containing all the objects at a given location -- which MIGHT be empty or MIGHT be null
        (which should also be interpreted as "empty") when there are no objects at the location.
        You should NOT MODIFY THIS BAG. This is the actual container bag, and modifying it will almost certainly break
        the Sparse Field object.   If you want to modify the bag, make a copy and modify the copy instead,
        using something along the lines of <b> new Bag(<i>foo</i>.getObjectsAtLocation(<i>location</i>)) </b>.
        Furthermore, changing values in the Sparse Field may result in a different bag being used -- so you should
        not rely on this bag staying valid.
    */
    public Bag getObjectsAtLocation(final int x, final int y, final int z)
        {
        return getObjectsAtLocation(new Int3D(x,y,z));
        }

    /** Returns the object location as a Double3D, or as null if there is no such object. */
    public Double3D getObjectLocationAsDouble3D(Object obj)
        {
        Int3D loc = (Int3D) super.getRawObjectLocation(obj);
        if (loc == null) return null;
        return new Double3D(loc);
        }

    /** Returns the object location, or null if there is no such object. */
    public Int3D getObjectLocation(Object obj)
        {
        return (Int3D) super.getRawObjectLocation(obj);
        }
    
    /** Removes all the objects stored at the given location and returns them as a Bag (which you are free to modify). */
    public Bag removeObjectsAtLocation(final int x, final int y, final int z)
        {
        return removeObjectsAtLocation(new Int3D(x,y,z));
        }

    /** Changes the location of an object, or adds if it doesn't exist yet.  Returns false
        if the object is null (null objects cannot be put into the grid). */
    public boolean setObjectLocation(final Object obj, final int x, final int y, final int z)
        {
        return super.setObjectLocation(obj,new Int3D(x,y,z));
        }

    /** Changes the location of an object, or adds if it doesn't exist yet.  Returns false
        if the object is null (null objects cannot be put into the grid) or if the location is null. */
    public boolean setObjectLocation(Object obj, final Int3D location)
        {
        return super.setObjectLocation(obj, location);
        }

    // this internal version of tx is arranged to be 34 bytes.  It first tries stx, then tx.
    int tx(int x, int width, int widthtimestwo, int xpluswidth, int xminuswidth) 
        {
        if (x >= -width && x < widthtimestwo)
            {
            if (x < 0) return xpluswidth;
            if (x < width) return x;
            return xminuswidth;
            }
        return tx2(x, width);
        }

    // used internally by the internal version of tx above.  Do not call directly.
    int tx2(int x, int width)
        {
        x = x % width;
        if (x < 0) x = x + width;
        return x;
        }

    // this internal version of ty is arranged to be 34 bytes.  It first tries sty, then ty.
    int ty(int y, int height, int heighttimestwo, int yplusheight, int yminusheight) 
        {
        if (y >= -height && y < heighttimestwo)
            {
            if (y < 0) return yplusheight;
            if (y < height) return y;
            return yminusheight;
            }
        return ty2(y, height);
        }
        
    // used internally by the internal version of ty above.  Do not call directly.
    int ty2(int y, int height)
        {
        y = y % height;
        if (y < 0) y = y + height;
        return y;
        }
        
    // this internal version of tz is arranged to be 34 bytes.  It first tries stz, then tz.
    int tz(int z, int length, int lengthtimestwo, int zpluslength, int zminuslength) 
        {
        if (z >= -length && z < lengthtimestwo)
            {
            if (z < 0) return zpluslength;
            if (z < length) return z;
            return zminuslength;
            }
        return tz2(z, length);
        }
        
    // used internally by the internal version of ty above.  Do not call directly.
    int tz2(int z, int length)
        {
        z = z % length;
        if (z < 0) z = z + length;
        return z;
        }
        

    protected void removeOrigin(int x, int y, int z, IntBag xPos, IntBag yPos, IntBag zPos)
        {
        int size = xPos.size();
        for(int i = 0; i <size; i++)
            {
            if (xPos.get(i) == x && yPos.get(i) == y && zPos.get(i) == z)
                {
                xPos.remove(i);
                yPos.remove(i);
                zPos.remove(i);
                return;
                }
            }
        }
        
    // only removes the first occurence
    protected void removeOriginToroidal(int x, int y, int z, IntBag xPos, IntBag yPos, IntBag zPos)
        {
        int size = xPos.size();
        x = tx(x, width, width*2, x+width, x-width);
        y = ty(y, height, height*2, y+height, y-height);
        z = tz(z, length, length*2, z+length, z-length);
        
        for(int i = 0; i <size; i++)
            {
            if (tx(xPos.get(i), width, width*2, x+width, x-width) == x && 
                ty(yPos.get(i), height, height*2, y+height, y-height) == y &&
                tz(zPos.get(i), length, length*2, z+length, z-length) == z)
                {
                xPos.remove(i);
                yPos.remove(i);
                zPos.remove(i);
                return;
                }
            }
        }




    /** @deprecated */
    public void getNeighborsMaxDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        getMooreLocations(x, y, z, dist, toroidal ? TOROIDAL : BOUNDED, true, xPos, yPos, zPos);
        }

    public void getMooreLocations( final int x, final int y, final int z, final int dist, int mode, boolean includeOrigin, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        boolean toroidal = (mode == TOROIDAL);
        boolean bounded = (mode == BOUNDED);

        if (mode != BOUNDED && mode != UNBOUNDED && mode != TOROIDAL)
            {
            throw new RuntimeException("Mode must be either Grid3D.BOUNDED, Grid3D.UNBOUNDED, or Grid3D.TOROIDAL");
            }
        
        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Distance must be positive" );
            }

        if( xPos == null || yPos == null || zPos == null)
            {
            throw new RuntimeException( "xPos and yPos and zPos should not be null" );
            }

        xPos.clear();
        yPos.clear();
        zPos.clear();

        // local variables are faster
        final int height = this.height;
        final int width = this.width;
        final int length = this.length;

        // for toroidal environments the code will be different because of wrapping arround
        if( toroidal )
            {
            // compute xmin and xmax for the neighborhood
            int xmin = x - dist;
            int xmax = x + dist;

            // next: is xmax - xmin humongous?  If so, no need to continue wrapping around
            if (xmax - xmin >= width)  // too wide
                xmax = xmin + width - 1;
            
            // compute ymin and ymax for the neighborhood
            int ymin = y - dist;
            int ymax = y + dist;
                        
            // next: is ymax - ymin humongous?  If so, no need to continue wrapping around
            if (ymax - ymin >= height)  // too wide
                ymax = ymin + height - 1;

            // compute zmin and zmax for the neighborhood
            int zmin = z - dist;
            int zmax = z + dist;

            // next: is zmax - zmin humongous?  If so, no need to continue wrapping around
            if (zmax - zmin >= length)  // too wide
                zmax = zmin + length - 1;
                        

            for( int x0 = xmin; x0 <= xmax ; x0++ )
                {
                final int x_0 = tx(x0, width, width*2, x0+width, x0-width);
                for( int y0 = ymin ; y0 <= ymax ; y0++ )
                    {
                    final int y_0 = ty(y0, height, height*2, y0+height, y0-height);
                    for( int z0 = zmin ; z0 <= zmax ; z0++ )
                        {
                        final int z_0 = tz(z0, length, length*2, z0+length, z0-length);
                        if( x_0 != x || y_0 != y || z_0 != z )
                            {
                            xPos.add( x_0 );
                            yPos.add( y_0 );
                            zPos.add( z_0 );
                            }
                        }
                    }
                }
            if (!includeOrigin) removeOriginToroidal(x,y,z,xPos,yPos,zPos); 
            }
        else // not toroidal
            {
            // compute xmin and xmax for the neighborhood such that they are within boundaries
            final int xmin = ((x-dist>=0 || !bounded)?x-dist:0);
            final int xmax =((x+dist<=width-1 || !bounded)?x+dist:width-1);
            // compute ymin and ymax for the neighborhood such that they are within boundaries
            final int ymin = ((y-dist>=0 || !bounded)?y-dist:0);
            final int ymax = ((y+dist<=height-1 || !bounded)?y+dist:height-1);
                        
            final int zmin = ((z-dist>=0 || !bounded)?z-dist:0);
            final int zmax = ((z+dist<=length-1 || !bounded)?z+dist:length-1);
                        
            for( int x0 = xmin ; x0 <= xmax ; x0++ )
                {
                for( int y0 = ymin ; y0 <= ymax ; y0++ )
                    {
                    for( int z0 = zmin ; z0 <= zmax ; z0++ )
                        {
                        if( x0 != x || y0 != y || z0 != z )
                            {
                            xPos.add( x0 );
                            yPos.add( y0 );
                            zPos.add( z0 );
                            }
                        }
                    }
                }
            if (!includeOrigin) removeOrigin(x,y,z,xPos,yPos,zPos); 
            }
        }


    /** @deprecated */
    public void getNeighborsHamiltonianDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        getVonNeumannLocations(x, y, z, dist, toroidal ? TOROIDAL : BOUNDED, true, xPos, yPos, zPos);
        }

    public void getVonNeumannLocations( final int x, final int y, final int z, final int dist, int mode, boolean includeOrigin, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        boolean toroidal = (mode == TOROIDAL);
        boolean bounded = (mode == BOUNDED);

        if (mode != BOUNDED && mode != UNBOUNDED && mode != TOROIDAL)
            {
            throw new RuntimeException("Mode must be either Grid3D.BOUNDED, Grid3D.UNBOUNDED, or Grid3D.TOROIDAL");
            }
        
        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Distance must be positive" );
            }

        if( xPos == null || yPos == null || zPos == null)
            {
            throw new RuntimeException( "xPos and yPos and zPos should not be null" );
            }

        xPos.clear();
        yPos.clear();
        zPos.clear();

        // local variables are faster
        final int height = this.height;
        final int width = this.width;
        final int length = this.length;

        // for toroidal environments the code will be different because of wrapping arround
        if( toroidal )
            {
            // compute xmin and xmax for the neighborhood
            final int xmax = x+dist;
            final int xmin = x-dist;
            for( int x0 = xmin; x0 <= xmax ; x0++ )
                {
                final int x_0 = tx(x0, width, width*2, x0+width, x0-width);
                // compute ymin and ymax for the neighborhood; they depend on the curreny x0 value
                final int ymax = y+(dist-((x0-x>=0)?x0-x:x-x0));
                final int ymin = y-(dist-((x0-x>=0)?x0-x:x-x0));
                for( int y0 =  ymin; y0 <= ymax; y0++ )
                    {
                    final int y_0 = ty(y0, height, height*2, y0+height, y0-height);
                    final int zmax = z+(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0));
                    final int zmin = z-(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0));
                    for( int z0 = zmin; z0 <= zmax; z0++ )
                        {
                        final int z_0 = tz(z0, length, length*2, z0+length, z0-length);
                        if( x_0 != x || y_0 != y || z_0 != z )
                            {
                            xPos.add( x_0 );
                            yPos.add( y_0 );
                            zPos.add( z_0 );
                            }
                        }
                    }
                }
            if (dist * 2 >= width || dist * 2 >= height || dist * 2 >= length)  // too big, will have to remove duplicates
                {
                int sz = xPos.size();
                Map map = buildMap(sz);
                for(int i = 0 ; i < sz; i++)
                    {
                    Double3D elem = new Double3D(xPos.get(i), yPos.get(i), zPos.get(i));
                    if (map.containsKey(elem)) // already there
                        {
                        xPos.remove(i);
                        yPos.remove(i);
                        zPos.remove(i);
                        i--;
                        sz--;
                        }
                    else
                        {
                        map.put(elem, elem);
                        }
                    }
                }
            if (!includeOrigin) removeOriginToroidal(x,y,z,xPos,yPos,zPos); 
            }
        else // not toroidal
            {
            // compute xmin and xmax for the neighborhood such that they are within boundaries
            final int xmax = ((x+dist<=width-1 || !bounded)?x+dist:width-1);
            final int xmin = ((x-dist>=0 || !bounded)?x-dist:0);
            for( int x0 = xmin ; x0 <= xmax ; x0++ )
                {
                final int x_0 = x0;
                // compute ymin and ymax for the neighborhood such that they are within boundaries
                // they depend on the curreny x0 value
                final int ymax = ((y+(dist-((x0-x>=0)?x0-x:x-x0))<=height-1 || !bounded)?y+(dist-((x0-x>=0)?x0-x:x-x0)):height-1);
                final int ymin = ((y-(dist-((x0-x>=0)?x0-x:x-x0))>=0 || !bounded)?y-(dist-((x0-x>=0)?x0-x:x-x0)):0);
                for( int y0 =  ymin; y0 <= ymax; y0++ )
                    {
                    final int y_0 = y0;
                    final int zmin = ((z-(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0))>=0 || !bounded)?z-(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0)):0);
                    final int zmax = ((z+(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0))<=length-1 || !bounded)?z+(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0)):length-1) ;
                    for( int z0 = zmin; z0 <= zmax; z0++ )
                        {
                        final int z_0 = z0;
                        if( x_0 != x || y_0 != y || z_0 != z )
                            {
                            xPos.add( x_0 );
                            yPos.add( y_0 );
                            zPos.add( z_0 );
                            }
                        }
                    }
                }
            if (!includeOrigin) removeOrigin(x,y,z,xPos,yPos,zPos); 
            }
        }








    /** @deprecated */
    public Bag getNeighborsMaxDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, Bag result, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        return getMooreNeighbors(x, y, z, dist, toroidal ? TOROIDAL : BOUNDED, true, result, xPos, yPos, zPos);
        }


    public Bag getMooreNeighbors( final int x, final int y, final int z, final int dist, int mode, boolean includeOrigin, Bag result, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();
        if( zPos == null )
            zPos = new IntBag();

        getMooreLocations( x, y, z, dist, mode, includeOrigin, xPos, yPos, zPos );
        return getObjectsAtLocations(xPos,yPos,zPos, result);
        }


    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y), abs(z-Z) ) <= dist.  This region forms a
     * cube 2*dist+1 cells across, centered at (X,Y,Z).  If dist==1, this
     * is equivalent to the twenty-six neighbors surrounding (X,Y,Z), plus (X,Y) itself.  
     * Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p>For each Object which falls within this distance, adds the X position, Y position, Z position, and Object into the
     * xPos, yPos, zPos, and result Bag, clearing them first.  
     * Some <X,Y,Z> positions may not appear
     * and that others may appear multiply if multiple objects share that positions.  Compare this function
     * with getNeighborsMaxDistance(...).
     * Returns the result Bag.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p>This function may be run in one of three modes: Grid3D.BOUNDED, Grid3D.UNBOUNDED, and Grid3D.TOROIDAL.  If "bounded",
     * then the neighbors are restricted to be only those which lie within the box ranging from (0,0,0) to (width, height, length), 
     * that is, the width and height of the grid.  If "unbounded", then the neighbors are not so restricted.  Note that unbounded
     * neighborhood lookup only makes sense if your grid allows locations to actually <i>be</i> outside this box.  For example,
     * SparseGrid3D permits this but ObjectGrid3D and DoubleGrid3D and IntGrid3D and DenseGrid3D do not.  Finally if "toroidal",
     * then the environment is assumed to be toroidal, that is, wrap-around, and neighbors are computed in this fashion.  Toroidal
     * locations will not appear multiple times: specifically, if the neighborhood distance is so large that it wraps completely around
     * the width or height of the box, neighbors will not be counted multiple times.  Note that to ensure this, subclasses may need to
     * resort to expensive duplicate removal, so it's not suggested you use so unreasonably large distances.
     *
     * <p>You can also opt to include the origin -- that is, the (x,y) point at the center of the neighborhood -- in the neighborhood results.
     */
    public Bag getMooreNeighborsAndLocations(final int x, final int y, int z, final int dist, int mode, boolean includeOrigin, Bag result, IntBag xPos, IntBag yPos, IntBag zPos)
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();
        if( zPos == null )
            zPos = new IntBag();

        getMooreLocations( x, y, z, dist, mode, includeOrigin, xPos, yPos, zPos);
        reduceObjectsAtLocations( xPos,  yPos,  zPos, result);
        return result;
        }



    /**
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) + abs(z-Z) <= dist.  This region 
     * forms an <a href="http://images.google.com/images?q=octahedron">octohedron</a> 2*dist+1 cells from point
     * to opposite point inclusive, centered at (X,Y,Y).  If dist==1 this is
     * equivalent to the six neighbors  above, below, left, and right, front, and behind (X,Y,Z)),
     * plus (X,Y,Z) itself.
     * Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p>Then places into the result Bag any Objects which fall on one of these <x,y,z> locations, clearning it first.
     * <b>Note that the order and size of the result Bag may not correspond to the X and Y and Z bags.</b>  If you want
     * all three bags to correspond (x, y, z, object) then use getNeighborsAndCorrespondingPositionsHamiltonianDistance(...)
     * Returns the result Bag.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p> This function may only run in two modes: toroidal or bounded.  Unbounded lookup is not permitted, and so
     * this function is deprecated: instead you should use the other version of this function which has more functionality.
     * If "bounded",
     * then the neighbors are restricted to be only those which lie within the box ranging from (0,0,0) to (width, height, length), 
     * that is, the width and height and length of the grid.   if "toroidal",
     * then the environment is assumed to be toroidal, that is, wrap-around, and neighbors are computed in this fashion.  Toroidal
     * locations will not appear multiple times: specifically, if the neighborhood distance is so large that it wraps completely around
     * the width or height of the box, neighbors will not be counted multiple times.  Note that to ensure this, subclasses may need to
     * resort to expensive duplicate removal, so it's not suggested you use so unreasonably large distances.
     *
     * <p>The origin -- that is, the (x,y,z) point at the center of the neighborhood -- is always included in the results.
     *
     * <p>This function is equivalent to: <tt>getNeighborsHamiltonianDistance(x,y,z,dist,toroidal ? Grid3D.TOROIDAL : Grid3D.BOUNDED, true, result, xPos, yPos,zPos);</tt>
     * 
     * @deprecated
     */
    public Bag getNeighborsHamiltonianDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, Bag result, IntBag xPos, IntBag yPos, IntBag zPos)
        {
        return getVonNeumannNeighbors(x, y, z, dist, toroidal ? TOROIDAL : BOUNDED, true,result, xPos, yPos, zPos);
        }


    /**
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) + abs(z-Z) <= dist.  This region 
     * forms an <a href="http://images.google.com/images?q=octahedron">octohedron</a> 2*dist+1 cells from point
     * to opposite point inclusive, centered at (X,Y,Y).  If dist==1 this is
     * equivalent to the six neighbors  above, below, left, and right, front, and behind (X,Y,Z)),
     * plus (X,Y,Z) itself.
     * Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p>Then places into the result Bag any Objects which fall on one of these <x,y,z> locations, clearning it first.
     * <b>Note that the order and size of the result Bag may not correspond to the X and Y and Z bags.</b>  If you want
     * all three bags to correspond (x, y, z, object) then use getNeighborsAndCorrespondingPositionsMaxDistance(...)
     * Returns the result Bag.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p>This function may be run in one of three modes: Grid3D.BOUNDED, Grid3D.UNBOUNDED, and Grid3D.TOROIDAL.  If "bounded",
     * then the neighbors are restricted to be only those which lie within the box ranging from (0,0,0) to (width, height), 
     * that is, the width and height of the grid.  If "unbounded", then the neighbors are not so restricted.  Note that unbounded
     * neighborhood lookup only makes sense if your grid allows locations to actually <i>be</i> outside this box.  For example,
     * SparseGrid3D permits this but ObjectGrid3D and DoubleGrid3D and IntGrid3D and DenseGrid3D do not.  Finally if "toroidal",
     * then the environment is assumed to be toroidal, that is, wrap-around, and neighbors are computed in this fashion.  Toroidal
     * locations will not appear multiple times: specifically, if the neighborhood distance is so large that it wraps completely around
     * the width or height of the box, neighbors will not be counted multiple times.  Note that to ensure this, subclasses may need to
     * resort to expensive duplicate removal, so it's not suggested you use so unreasonably large distances.
     *
     * <p>You can also opt to include the origin -- that is, the (x,y,z) point at the center of the neighborhood -- in the neighborhood results.
     */
    public Bag getVonNeumannNeighbors( final int x, final int y, int z, final int dist, int mode, boolean includeOrigin, Bag result, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();
        if( zPos == null )
            zPos = new IntBag();

        getVonNeumannLocations( x, y, z, dist, mode, includeOrigin, xPos, yPos, zPos);
        return getObjectsAtLocations(xPos,yPos,zPos, result);
        }



    /**
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) + abs(z-Z) <= dist.  This region 
     * forms an <a href="http://images.google.com/images?q=octahedron">octohedron</a> 2*dist+1 cells from point
     * to opposite point inclusive, centered at (X,Y,Y).  If dist==1 this is
     * equivalent to the six neighbors  above, below, left, and right, front, and behind (X,Y,Z)),
     * plus (X,Y,Z) itself.
     * Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     * <p>For each Object which falls within this distance, adds the X position, Y position, Z position, and Object into the
     * xPos, yPos, zPos, and result Bag, clearing them first.  
     * Some <X,Y,Z> positions may not appear
     * and that others may appear multiply if multiple objects share that positions.  Compare this function
     * with getNeighborsMaxDistance(...).
     * Returns the result Bag.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p>This function may be run in one of three modes: Grid3D.BOUNDED, Grid3D.UNBOUNDED, and Grid3D.TOROIDAL.  If "bounded",
     * then the neighbors are restricted to be only those which lie within the box ranging from (0,0,0) to (width, height, length), 
     * that is, the width and height of the grid.  If "unbounded", then the neighbors are not so restricted.  Note that unbounded
     * neighborhood lookup only makes sense if your grid allows locations to actually <i>be</i> outside this box.  For example,
     * SparseGrid3D permits this but ObjectGrid3D and DoubleGrid3D and IntGrid3D and DenseGrid3D do not.  Finally if "toroidal",
     * then the environment is assumed to be toroidal, that is, wrap-around, and neighbors are computed in this fashion.  Toroidal
     * locations will not appear multiple times: specifically, if the neighborhood distance is so large that it wraps completely around
     * the width or height of the box, neighbors will not be counted multiple times.  Note that to ensure this, subclasses may need to
     * resort to expensive duplicate removal, so it's not suggested you use so unreasonably large distances.
     *
     * <p>You can also opt to include the origin -- that is, the (x,y) point at the center of the neighborhood -- in the neighborhood results.
     */
    public Bag getVonNeumannNeighborsAndLocations(final int x, final int y, final int z, final int dist, int mode, boolean includeOrigin, Bag result, IntBag xPos, IntBag yPos, IntBag zPos)
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();
        if( zPos == null )
            zPos = new IntBag();

        getVonNeumannLocations( x, y, z, dist, mode, includeOrigin, xPos, yPos, zPos );
        reduceObjectsAtLocations( xPos,  yPos, zPos, result);
        return result;
        }


    public Bag getRadialNeighbors( final int x, final int y, int z, final int dist, int mode, boolean includeOrigin,  int measurementRule, boolean closed,  Bag result, IntBag xPos, IntBag yPos, IntBag zPos)
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();
        if( zPos == null )
            zPos = new IntBag();

        getRadialLocations( x, y, z, dist, mode, includeOrigin, measurementRule, closed, xPos, yPos, zPos );
        return getObjectsAtLocations(xPos,yPos,zPos,result);
        }

    public Bag getRadialNeighborsAndLocations( final int x, final int y, int z, final int dist, int mode, boolean includeOrigin,  int measurementRule, boolean closed,  Bag result, IntBag xPos, IntBag yPos, IntBag zPos)
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();
        if( zPos == null )
            zPos = new IntBag();

        getRadialLocations( x, y, z, dist, mode, includeOrigin, measurementRule, closed, xPos, yPos, zPos );
        reduceObjectsAtLocations( xPos,  yPos, zPos, result);
        return result;
        }




    // For each <xPos, yPos> location, puts all such objects into the result bag.  Modifies
    // the xPos and yPos bags so that each position corresponds to the equivalent result in
    // in the result bag.
    void reduceObjectsAtLocations(final IntBag xPos, final IntBag yPos, final IntBag zPos, Bag result)
        {
        if (result==null) result = new Bag();
        else result.clear();

        // build new bags with <x,y> locations one per each result
        IntBag newXPos = new IntBag();
        IntBag newYPos = new IntBag();
        IntBag newZPos = new IntBag();

        final int len = xPos.numObjs;
        final int[] xs = xPos.objs;
        final int[] ys = yPos.objs;
        final int[] zs = zPos.objs;

        // for each location...
        for(int i=0; i < len; i++)
            {
            Bag temp = getObjectsAtLocation(xs[i],ys[i],zs[i]);
            if (temp != null)
                {
                final int size = temp.numObjs;
                final Object[] os = temp.objs;
                // for each object at that location...
                for(int j = 0; j < size; j++)
                    {
                    // add the result, the x, and the y
                    result.add(os[j]);
                    newXPos.add(xs[i]);
                    newYPos.add(ys[i]);
                    newZPos.add(zs[i]);
                    }
                }
            }
                
        // dump the new IntBags into the old ones
        xPos.clear();
        xPos.addAll(newXPos);
        yPos.clear();
        yPos.addAll(newYPos);
        zPos.clear();
        zPos.addAll(newZPos);
        }
                


    /** For each <xPos,yPos,zPos> location, puts all such objects into the result bag.  Returns the result bag.
        If the provided result bag is null, one will be created and returned. */
    public Bag getObjectsAtLocations(final IntBag xPos, final IntBag yPos, final IntBag zPos, Bag result)
        {
        if (result==null) result = new Bag();
        else result.clear();
        
        final int len = xPos.numObjs;
        final int[] xs = xPos.objs;
        final int[] ys = yPos.objs;
        final int[] zs = zPos.objs;
        for(int i=0; i < len; i++)
            {
            // a little efficiency: add if we're 1, addAll if we're > 1, 
            // do nothing if we're 0
            Bag temp = getObjectsAtLocation(xs[i],ys[i],zs[i]);
            if (temp!=null)
                {
                int n = temp.numObjs;
                if (n==1) result.add(temp.objs[0]);
                else if (n > 1) result.addAll(temp);
                }
            }
        return result;
        }




    double ds(double d1x, double d1y, double d1z, double d2x, double d2y, double d2z)
        {
        return ((d1x - d2x) * (d1x - d2x) + (d1y - d2y) * (d1y - d2y) + (d1z - d2z) * (d1z - d2z));
        }
    
    boolean within(double d1x, double d1y, double d1z, double d2x, double d2y, double d2z, double distanceSquared, boolean closed)
        {
        double d= ds(d1x, d1y, d1z, d2x, d2y, d2z);
        return (d < distanceSquared || (d == distanceSquared && closed));
        }

    public void getRadialLocations( final int x, final int y, final int z, final double dist, int mode, boolean includeOrigin, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        getRadialLocations(x, y, z, dist, mode, includeOrigin, Grid3D.ANY, true, xPos, yPos, zPos);
        }
        
    public void getRadialLocations( final int x, final int y, final int z, final double dist, int mode, boolean includeOrigin, int measurementRule, boolean closed, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        boolean toroidal = (mode == TOROIDAL);

        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Distance must be positive" );
            }
            
        if (measurementRule != Grid3D.ANY && measurementRule != Grid3D.ALL && measurementRule != Grid3D.CENTER)
            {
            throw new RuntimeException(" Measurement rule must be one of ANY, ALL, or CENTER" );
            }
                
        // grab the rectangle
        getMooreLocations(x,y, z, (int) Math.ceil(dist + 0.5), mode, includeOrigin, xPos, yPos, zPos);
        int len = xPos.size();
        double distsq = dist * dist;
        

        int width = this.width;
        int height = this.height;
        int length = this.length;
        int widthtimestwo = width * 2;
        int heighttimestwo = height * 2;
        int lengthtimestwo = length * 2;
        

        for(int i = 0; i < len; i++)
            {
            int xp = xPos.get(i);
            int yp = yPos.get(i);
            int zp = zPos.get(i);
            boolean remove = false;
                
            if (measurementRule == Grid3D.ANY)
                {
                if (z == zp)
                    {
                    if (x == xp)
                        {
                        if (y < yp)
                            {
                            double d = (yp - 0.5) -  y;
                            remove = !(d < dist || (d == dist && closed));
                            }
                        else
                            {
                            double d = -((yp - 0.5) - y);
                            remove = !(d < dist || (d == dist && closed));
                            }
                        }
                    else if (y == yp)
                        {
                        if (x < xp)
                            {
                            double d = (xp - 0.5) - x;
                            remove = !(d < dist || (d == dist && closed));
                            }
                        else
                            {
                            double d = -((xp - 0.5) - x);
                            remove = !(d < dist || (d == dist && closed));
                            }
                        }
                    }
                else if (x == xp)
                    {
                    if (y == yp)
                        {
                        if (z  < zp)
                            {
                            double d = (zp - 0.5) -  z;
                            remove = !(d < dist || (d == dist && closed));
                            }
                        else
                            {
                            double d = -((zp - 0.5) - z);
                            remove = !(d < dist || (d == dist && closed));
                            }
                        }
                    }
                else if (z < zp)
                    {
                    if (x < xp)
                        {
                        if (y < yp)
                            remove = !within(x,y,z,xp-0.5,yp-0.5,zp-0.5,distsq,closed);
                        else
                            remove = !within(x,y,z,xp-0.5,yp+0.5,zp-0.5,distsq,closed);
                        }
                    else
                        {
                        if (y < yp)
                            remove = !within(x,y,z,xp+0.5,yp-0.5,zp-0.5,distsq,closed);
                        else
                            remove = !within(x,y,z,xp+0.5,yp+0.5,zp-0.5,distsq,closed);
                        }
                    }
                else
                    {
                    if (x < xp)
                        {
                        if (y < yp)
                            remove = !within(x,y,z,xp-0.5,yp-0.5,zp+0.5,distsq,closed);
                        else
                            remove = !within(x,y,z,xp-0.5,yp+0.5,zp+0.5,distsq,closed);
                        }
                    else
                        {
                        if (y < yp)
                            remove = !within(x,y,z,xp+0.5,yp-0.5,zp+0.5,distsq,closed);
                        else
                            remove = !within(x,y,z,xp+0.5,yp+0.5,zp+0.5,distsq,closed);
                        }
                    }
                }
            else if (measurementRule == Grid3D.ALL)
                {
                if (z < zp)
                    {
                    if (x < xp)
                        {
                        if (y < yp)
                            remove = !within(x,y,z,xp+0.5,yp+0.5,zp+0.5,distsq,closed);
                        else
                            remove = !within(x,y,z,xp+0.5,yp-0.5,zp+0.5,distsq,closed);
                        }
                    else
                        {
                        if (y < yp)
                            remove = !within(x,y,z,xp-0.5,yp+0.5,zp+0.5,distsq,closed);
                        else
                            remove = !within(x,y,z,xp-0.5,yp-0.5,zp+0.5,distsq,closed);
                        }
                    }
                else
                    {
                    if (x < xp)
                        {
                        if (y < yp)
                            remove = !within(x,y,z,xp+0.5,yp+0.5,zp-0.5,distsq,closed);
                        else
                            remove = !within(x,y,z,xp+0.5,yp-0.5,zp-0.5,distsq,closed);
                        }
                    else
                        {
                        if (y < yp)
                            remove = !within(x,y,z,xp-0.5,yp+0.5,zp-0.5,distsq,closed);
                        else
                            remove = !within(x,y,z,xp-0.5,yp-0.5,zp-0.5,distsq,closed);
                        }
                    }

                }
            else // (measurementRule == Grid3D.CENTER)
                {
                remove = !within(x,y,z,xp,yp,zp,distsq,closed);
                }
                
            if (remove)
                { xPos.remove(i); yPos.remove(i); zPos.remove(i); i--; len--; }
            else if (toroidal) // need to convert to toroidal position
                { 
                int _x = xPos.get(i);
                int _y = yPos.get(i);
                int _z = zPos.get(i);
                xPos.set(i, tx(_x, width, widthtimestwo, _x + width, _x - width));
                yPos.set(i, ty(_y, height, heighttimestwo, _y + width, _y - width));
                zPos.set(i, tz(_z, length, lengthtimestwo, _z + length, _z - length));
                }

            }
        }


    public Bag getRadialNeighbors( final int x, final int y, final int z, final int dist, int mode, boolean includeOrigin,  Bag result, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        return getRadialNeighbors(x, y, z, dist, mode, includeOrigin, Grid3D.ANY, true, result, xPos, yPos, zPos);
        }


    public Bag getRadialNeighborsAndLocations( final int x, final int y, final int z, final int dist, int mode, boolean includeOrigin, Bag result, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        return getRadialNeighborsAndLocations(x, y, z, dist, mode, includeOrigin, Grid3D.ANY, true, result, xPos, yPos, zPos);
        }


    /**
     * Determines all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y), abs(z-Z) ) <= dist. This region forms a
     * square 2*dist+1 cells across, centered at (X,Y,Z).  If dist==1, this
     * is equivalent to the so-called "Moore Neighborhood" (the eight neighbors surrounding (X,Y,Z)), plus (X,Y,Z) itself.
     * <p>Then returns, as a Bag, any Objects which fall on one of these <x,y,z> locations.
     *
     * <p>This function may be run in one of three modes: Grid2D.BOUNDED, Grid2D.UNBOUNDED, and Grid2D.TOROIDAL.  If "bounded",
     * then the neighbors are restricted to be only those which lie within the box ranging from (0,0) to (width, height), 
     * that is, the width and height of the grid.  If "unbounded", then the neighbors are not so restricted.  Note that unbounded
     * neighborhood lookup only makes sense if your grid allows locations to actually <i>be</i> outside this box.  For example,
     * SparseGrid2D permits this but ObjectGrid2D and DoubleGrid2D and IntGrid2D and DenseGrid2D do not.  Finally if "toroidal",
     * then the environment is assumed to be toroidal, that is, wrap-around, and neighbors are computed in this fashion.  Toroidal
     * locations will not appear multiple times: specifically, if the neighborhood distance is so large that it wraps completely around
     * the width or height of the box, neighbors will not be counted multiple times.  Note that to ensure this, subclasses may need to
     * resort to expensive duplicate removal, so it's not suggested you use so unreasonably large distances.
     */
    public Bag getMooreNeighbors( int x, int y, int z, int dist, int mode, boolean includeOrigin )
        {
        return getMooreNeighbors(x, y, z, dist, mode, includeOrigin, null, null, null, null);
        }



    /**
     * Determines all neighbors of a location that satisfy abs(x-X) + abs(y-Y) + abs(z-Z) <= dist.  This region forms a diamond
     * 2*dist+1 cells from point to opposite point inclusive, centered at (X,Y,Z).  If dist==1 this is
     * equivalent to the so-called "Von-Neumann Neighborhood" (the four neighbors above, below, left, and right of (X,Y,Z)),
     * plus (X,Y,Z) itself.
     * <p>Then returns, as a Bag, any Objects which fall on one of these <x,y,z> locations.
     *
     * <p>This function may be run in one of three modes: Grid2D.BOUNDED, Grid2D.UNBOUNDED, and Grid2D.TOROIDAL.  If "bounded",
     * then the neighbors are restricted to be only those which lie within the box ranging from (0,0) to (width, height), 
     * that is, the width and height of the grid.  If "unbounded", then the neighbors are not so restricted.  Note that unbounded
     * neighborhood lookup only makes sense if your grid allows locations to actually <i>be</i> outside this box.  For example,
     * SparseGrid2D permits this but ObjectGrid2D and DoubleGrid2D and IntGrid2D and DenseGrid2D do not.  Finally if "toroidal",
     * then the environment is assumed to be toroidal, that is, wrap-around, and neighbors are computed in this fashion.  Toroidal
     * locations will not appear multiple times: specifically, if the neighborhood distance is so large that it wraps completely around
     * the width or height of the box, neighbors will not be counted multiple times.  Note that to ensure this, subclasses may need to
     * resort to expensive duplicate removal, so it's not suggested you use so unreasonably large distances.
     */
    public Bag getVonNeumannNeighbors( int x, int y, int z, int dist, int mode, boolean includeOrigin )
        {
        return getVonNeumannNeighbors(x, y, z, dist, mode, includeOrigin, null, null, null, null);
        }





    public Bag getRadialNeighbors( final int x, final int y, int z, final int dist, int mode, boolean includeOrigin)
        {
        return getRadialNeighbors(x, y, z, dist, mode, includeOrigin, null, null, null, null);
        }





    public final Double3D getDimensions() { return new Double3D(width, height, length); }
    }


