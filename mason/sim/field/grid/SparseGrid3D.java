/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.grid;
import sim.field.*;
import sim.util.*;

/**
   A storage facility for sparse objects in discrete 3D space, using HashMaps.  SparseGrid3D differs from ObjectGrid3D
   in several respects:
    
   <ul>
   <li>SparseGrid3D can store more than one object at a location.  ObjectGrid3D cannot.
   <li>ObjectGrid3D can store an object at more than one location (though it's bad form!).
   <li>SparseGrid3D can efficiently (O(1)) tell you the location of an object.
   <li>SparseGrid3D can efficiently (O(#objs)) scan through all objects.  The best you can do with ObjectGrid3D is search its array (which might have many empty slots).
   <li>Storing an object, finding its location, or changing its location, in a SparseGrid3D is O(1) but requires several HashMap lookups and/or removes, which has a significant constant overhead.
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

public class SparseGrid3D extends SparseField
    {
    protected int width;
    protected int height;
    protected int length;
    
    public SparseGrid3D(int width, int height, int length)
        {
        this.width = width;
        this.height = height;
        this.length = length;
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


    MutableInt3D speedyMutableInt3D = new MutableInt3D();
    /** Returns the number of objects stored in the grid at the given location. */
    public int numObjectsAtLocation(final int x, final int y, final int z)
        {
        MutableInt3D speedyMutableInt3D = this.speedyMutableInt3D;  // a little faster (local)
        speedyMutableInt3D.x = x;
        speedyMutableInt3D.y = y;
        speedyMutableInt3D.z = z;
        return numObjectsAtLocation(speedyMutableInt3D);
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
        MutableInt3D speedyMutableInt3D = this.speedyMutableInt3D;  // a little faster (local)
        speedyMutableInt3D.x = x;
        speedyMutableInt3D.y = y;
        speedyMutableInt3D.z = z;
        return getObjectsAtLocation(speedyMutableInt3D);
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
        MutableInt3D speedyMutableInt3D = this.speedyMutableInt3D;  // a little faster (local)
        speedyMutableInt3D.x = x;
        speedyMutableInt3D.y = y;
        speedyMutableInt3D.z = z;
        return removeObjectsAtLocation(speedyMutableInt3D);
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

    public void getNeighborsMaxDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Runtime exception in method getNeighborsMaxDistance: Distance must be positive" );
            }

        if( xPos == null || yPos == null || zPos == null )
            {
            throw new RuntimeException( "Runtime exception in method getNeighborsMaxDistance: xPos and yPos should not be null" );
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
            final int xmin = x - dist;
            final int xmax = x + dist;
            // compute ymin and ymax for the neighborhood
            final int ymin = y - dist;
            final int ymax = y + dist;
                        
            final int zmin = z - dist;
            final int zmax = z + dist;
                        

            for( int x0 = xmin; x0 <= xmax ; x0++ )
                {
                final int x_0 = stx(x0, width);
                for( int y0 = ymin ; y0 <= ymax ; y0++ )
                    {
                    final int y_0 = sty(y0, height);
                    for( int z0 = zmin ; z0 <= zmax ; z0++ )
                        {
                        final int z_0 = stz(z0, length);
                        if( x_0 != x || y_0 != y || z_0 != z )
                            {
                            xPos.add( x_0 );
                            yPos.add( y_0 );
                            zPos.add( z_0 );
                            }
                        }
                    }
                }
            }
        else // not toroidal
            {
            // compute xmin and xmax for the neighborhood such that they are within boundaries
            final int xmin = ((x-dist>=0)?x-dist:0);
            final int xmax =((x+dist<=width-1)?x+dist:width-1);
            // compute ymin and ymax for the neighborhood such that they are within boundaries
            final int ymin = ((y-dist>=0)?y-dist:0);
            final int ymax = ((y+dist<=height-1)?y+dist:height-1);
                        
            final int zmin = ((z-dist>=0)?z-dist:0);
            final int zmax = ((z+dist<=length-1)?z+dist:length-1);
                        
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
            }
        }


    public void getNeighborsHamiltonianDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Runtime exception in method getNeighborsHamiltonianDistance: Distance must be positive" );
            }

        if( xPos == null || yPos == null || zPos == null )
            {
            throw new RuntimeException( "Runtime exception in method getNeighborsHamiltonianDistance: xPos and yPos should not be null" );
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
                final int x_0 = stx(x0, width);
                // compute ymin and ymax for the neighborhood; they depend on the curreny x0 value
                final int ymax = y+(dist-((x0-x>=0)?x0-x:x-x0));
                final int ymin = y-(dist-((x0-x>=0)?x0-x:x-x0));
                for( int y0 =  ymin; y0 <= ymax; y0++ )
                    {
                    final int y_0 = sty(y0, height);
                    final int zmax = z+(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0));
                    final int zmin = z-(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0));
                    for( int z0 = zmin; z0 <= zmax; z0++ )
                        {
                        final int z_0 = stz(z0, length);
                        if( x_0 != x || y_0 != y || z_0 != z )
                            {
                            xPos.add( x_0 );
                            yPos.add( y_0 );
                            zPos.add( z_0 );
                            }
                        }
                    }
                }
            }
        else // not toroidal
            {
            // compute xmin and xmax for the neighborhood such that they are within boundaries
            final int xmax = ((x+dist<=width-1)?x+dist:width-1);
            final int xmin = ((x-dist>=0)?x-dist:0);
            for( int x0 = xmin ; x0 <= xmax ; x0++ )
                {
                final int x_0 = x0;
                // compute ymin and ymax for the neighborhood such that they are within boundaries
                // they depend on the curreny x0 value
                final int ymax = ((y+(dist-((x0-x>=0)?x0-x:x-x0))<=height-1)?y+(dist-((x0-x>=0)?x0-x:x-x0)):height-1);
                final int ymin = ((y-(dist-((x0-x>=0)?x0-x:x-x0))>=0)?y-(dist-((x0-x>=0)?x0-x:x-x0)):0);
                for( int y0 =  ymin; y0 <= ymax; y0++ )
                    {
                    final int y_0 = y0;
                    final int zmin = ((z-(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0))>=0)?z-(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0)):0);
                    final int zmax = ((z+(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0))<=length-1)?z+(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0)):length-1) ;
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
            }
        }

    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y), abs(z-Z) ) <= dist.  This region forms a
     * cube 2*dist+1 cells across, centered at (X,Y,Z).  If dist==1, this
     * is equivalent to the twenty-six neighbors surrounding (X,Y,Z), plus (X,Y) itself.  
     * Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     * Then places into the result Bag the objects at each of those <x,y,z> locations clearning it first.  
     * Returns the result Bag (constructing one if null had been passed in).
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     */
    public Bag getNeighborsMaxDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, Bag result, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();
        if( zPos == null )
            zPos = new IntBag();

        getNeighborsMaxDistance( x, y, z, dist, toroidal, xPos, yPos, zPos );
        return getObjectsAtLocations(xPos,yPos,zPos,result);
        }
        
        
    /**
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) + abs(z-Z) <= dist.  This region 
     * forms an <a href="http://images.google.com/images?q=octahedron">octohedron</a> 2*dist+1 cells from point
     * to opposite point inclusive, centered at (X,Y,Y).  If dist==1 this is
     * equivalent to the six neighbors  above, below, left, and right, front, and behind (X,Y,Z)),
     * plus (X,Y,Z) itself.
     * Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     * Then places into the result Bag the objects at each of those <x,y,z> locations clearning it first.  
     * Returns the result Bag (constructing one if null had been passed in).
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     */
    public Bag getNeighborsHamiltonianDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, Bag result, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();
        if( zPos == null )
            zPos = new IntBag();

        getNeighborsHamiltonianDistance( x, y, z, dist, toroidal, xPos, yPos, zPos );
        return getObjectsAtLocations(xPos,yPos,zPos,result);
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

    }


