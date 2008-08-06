/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.grid;
import sim.field.*;
import sim.util.*;

/**
   A storage facility for sparse objects in discrete 2D space, using HashMaps.  SparseGrid2D differs from ObjectGrid2D
   in several respects:
    
   <ul>
   <li>SparseGrid2D can store more than one object at a location.  ObjectGrid2D cannot.
   <li>ObjectGrid2D can store an object at more than one location (though it's bad form!).
   <li>SparseGrid2D can efficiently (O(1)) tell you the location of an object.
   <li>SparseGrid2D can efficiently (O(#objs)) scan through all objects.  The best you can do with ObjectGrid2D is search its array (which might have many empty slots).
   <li>Storing an object, finding its location, or changing its location, in a SparseGrid2D is O(1) but requires several HashMap lookups and/or removes, which has a significant constant overhead.
   <li>SparseGrid2D can associate objects with <i>any</i> 2D integer location.  ObjectGrid2D's locations are restricted to be within its array.
   </ul>

   <p>Generally speaking, if you have a grid of objects, one per location, you should use an ObjectGrid2D.  If you have a large grid occupied by a few objects, or those objects can pile up on the same grid location, you should use a SparseGrid2D.
    
   <p>In either case, you might consider storing the location of an object IN THE OBJECT ITSELF if you need to query for the object location often -- it's faster than the hashtable lookup in SparseGrid2D, and certainly faster than searching the entire array of an ObjectGrid2D.

   <p><b>Boundaries.</b>  SparseGrid2D has no boundaries at all.  <tt>width</tt> and <tt>height</tt> exist only to allow
   you to define pseudo-boundaries for toroidal computation; and to provide typical bounds for visualization.  But you can
   attach any coordinate as a location for an object with no restrictions.
        
   <b>Setting and getting an object and its Location.</b>  The method <b>setObjectLocation(...)</b> methods set the location of the object
   (to an Int2D or an <x,y> location).
   The method <b>getObjectsAtLocation(Object location)</b>, inherited from SparseField, returns a Bag (which you MUST NOT modify)
   containing all objects at a given location (which must be provided in the form of an Int2D or MutableInt2D).  The <b>numObjectsAtLocation(location)</b>
   method returns the number of such objects.  The <b>getObjectsAtLocations(Bag locations, Bag putInHere)</b> gathers objects
   at a variety of locations and puts them in the bag you provide.  The <b>getAllObjects()</b> method returns all objects in a bag you
   must NOT modiify.  The <b>removeObjectsAtLocation(Object location)</b> method removes and returns all objects at a given location
   (defined as an Int2D or MutableDouble2D).  The <b>exists</b> method tells you if the object exists in the field.
        
   <p><b>Neighborhood Lookups.</b>  The method <b>getObjectsAtLocationOfObject</b> returns all Objects at the same location as the provided
   object (in a Bag, which must NOT modify).  The various <b>getNeighbors...Distance(...)</b> methods return all locations defined by certain
   distance bounds, or all the objects stored at those locations.  They are expensive to compute and it may be wiser to compute them by hand
   if there aren't many.

*/

public class SparseGrid2D extends SparseField implements Grid2D
    {
    protected int width;
    protected int height;
    
    public SparseGrid2D(int width, int height)
        {
        this.width = width;
        this.height = height;
        }
    
    /** Returns the width of the grid */
    public int getWidth() { return width; }
    
    /** Returns the height of the grid */
    public int getHeight() { return height; }
    
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

    public int stx(final int x) 
        { if (x >= 0) { if (x < width) return x; return x - width; } return x + width; }

    public int sty(final int y) 
        { if (y >= 0) { if (y < height) return y ; return y - height; } return y + height; }

    // faster version
    final int stx(final int x, final int width) 
        { if (x >= 0) { if (x < width) return x; return x - width; } return x + width; }
        
    // faster version
    final int sty(final int y, final int height) 
        { if (y >= 0) { if (y < height) return y ; return y - height; } return y + height; }

    public int ulx(final int x, final int y) { return x - 1; }

    public int uly(final int x, final int y) { if ((x & 1) == 0) return y - 1; return y; }

    public int urx(final int x, final int y) { return x + 1; }

    public int ury(final int x, final int y) { if ((x & 1) == 0) return y - 1; return y; }
        
    public int dlx(final int x, final int y) { return x - 1; }

    public int dly(final int x, final int y) { if ((x & 1) == 0) return y ; return y + 1; }
    
    public int drx(final int x, final int y) { return x + 1; }

    public int dry(final int x, final int y) { if ((x & 1) == 0) return y ; return y + 1; }

    public int upx(final int x, final int y) { return x; }

    public int upy(final int x, final int y) { return y - 1; }

    public int downx(final int x, final int y) { return x; }

    public int downy(final int x, final int y) { return y + 1; }
    
    public boolean trb(final int x, final int y) { return ((x + y) & 1) == 1; }
    
    public boolean trt(final int x, final int y) { return ((x + y) & 1) == 0; }


    MutableInt2D speedyMutableInt2D = new MutableInt2D();
    /** Returns the number of objects stored in the grid at the given location. */
    public int numObjectsAtLocation(final int x, final int y)
        {
        MutableInt2D speedyMutableInt2D = this.speedyMutableInt2D;  // a little faster (local)
        speedyMutableInt2D.x = x;
        speedyMutableInt2D.y = y;
        return numObjectsAtLocation(speedyMutableInt2D);
        }

    /** Returns a bag containing all the objects at a given location, or null when there are no objects at the location.
        You should NOT MODIFY THIS BAG. This is the actual container bag, and modifying it will almost certainly break
        the Sparse Field object.   If you want to modify the bag, make a copy and modify the copy instead,
        using something along the lines of <b> new Bag(<i>foo</i>.getObjectsAtLocation(<i>location</i>)) </b>.
        Furthermore, changing values in the Sparse Field may result in a different bag being used -- so you should
        not rely on this bag staying valid.
    */
    public Bag getObjectsAtLocation(final int x, final int y)
        {
        MutableInt2D speedyMutableInt2D = this.speedyMutableInt2D;  // a little faster (local)
        speedyMutableInt2D.x = x;
        speedyMutableInt2D.y = y;
        return getObjectsAtLocation(speedyMutableInt2D);
        }

    /** Returns the object location as a Double2D, or as null if there is no such object. */
    public Double2D getObjectLocationAsDouble2D(Object obj)
        {
        Int2D loc = (Int2D) super.getRawObjectLocation(obj);
        if (loc == null) return null;
        return new Double2D(loc);
        }

    /** Returns the object location, or null if there is no such object. */
    public Int2D getObjectLocation(Object obj)
        {
        return (Int2D) super.getRawObjectLocation(obj);
        }
    
    /** Removes all the objects stored at the given location and returns them as a Bag (which you are free to modify). */
    public Bag removeObjectsAtLocation(final int x, final int y)
        {
        MutableInt2D speedyMutableInt2D = this.speedyMutableInt2D;  // a little faster (local)
        speedyMutableInt2D.x = x;
        speedyMutableInt2D.y = y;
        return removeObjectsAtLocation(speedyMutableInt2D);
        }

    /** Changes the location of an object, or adds if it doesn't exist yet.  Returns false
        if the object is null (null objects cannot be put into the grid). */
    public boolean setObjectLocation(final Object obj, final int x, final int y)
        {
        return super.setObjectLocation(obj,new Int2D(x,y));
        }
    
    /** Changes the location of an object, or adds if it doesn't exist yet.  Returns false
        if the object is null (null objects cannot be put into the grid) or if the location is null. */
    public boolean setObjectLocation(Object obj, final Int2D location)
        {
        return super.setObjectLocation(obj, location);
        }
        
    public void getNeighborsMaxDistance( final int x, final int y, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos )
        {
        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Runtime exception in method getNeighborsMaxDistance: Distance must be positive" );
            }

        if( xPos == null || yPos == null )
            {
            throw new RuntimeException( "Runtime exception in method getNeighborsMaxDistance: xPos and yPos should not be null" );
            }

        xPos.clear();
        yPos.clear();

        // local variables are faster
        final int height = this.height;
        final int width = this.width;


        // for toroidal environments the code will be different because of wrapping arround
        if( toroidal )
            {
            // compute xmin and xmax for the neighborhood
            final int xmin = x - dist;
            final int xmax = x + dist;
            // compute ymin and ymax for the neighborhood
            final int ymin = y - dist;
            final int ymax = y + dist;
                
            for( int x0 = xmin ; x0 <= xmax ; x0++ )
                {
                final int x_0 = stx(x0, width);
                for( int y0 = ymin ; y0 <= ymax ; y0++ )
                    {
                    final int y_0 = sty(y0, height);
                    xPos.add( x_0 );
                    yPos.add( y_0 );
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
            for( int x0 = xmin; x0 <= xmax ; x0++ )
                {
                for( int y0 = ymin ; y0 <= ymax ; y0++ )
                    {
                    xPos.add( x0 );
                    yPos.add( y0 );
                    }
                }
            }
        }


    public void getNeighborsHamiltonianDistance( final int x, final int y, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos )
        {
        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Runtime exception in method getNeighborsHamiltonianDistance: Distance must be positive" );
            }

        if( xPos == null || yPos == null )
            {
            throw new RuntimeException( "Runtime exception in method getNeighborsHamiltonianDistance: xPos and yPos should not be null" );
            }

        xPos.clear();
        yPos.clear();

        // local variables are faster
        final int height = this.height;
        final int width = this.width;

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
                    xPos.add( x_0 );
                    yPos.add( y_0 );
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
                // compute ymin and ymax for the neighborhood such that they are within boundaries
                // they depend on the curreny x0 value
                final int ymax = ((y+(dist-((x0-x>=0)?x0-x:x-x0))<=height-1)?y+(dist-((x0-x>=0)?x0-x:x-x0)):height-1);
                final int ymin = ((y-(dist-((x0-x>=0)?x0-x:x-x0))>=0)?y-(dist-((x0-x>=0)?x0-x:x-x0)):0);
                for( int y0 =  ymin; y0 <= ymax; y0++ )
                    {
                    xPos.add( x0 );
                    yPos.add( y0 );
                    }
                }
            }
        }


    public void getNeighborsHexagonalDistance( final int x, final int y, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos )
        {
        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Runtime exception in method getNeighborsHexagonalDistance: Distance must be positive" );
            }

        if( xPos == null || yPos == null )
            {
            throw new RuntimeException( "Runtime exception in method getNeighborsHamiltonianDistance: xPos and yPos should not be null" );
            }

        xPos.clear();
        yPos.clear();

        // local variables are faster
        final int height = this.height;
        final int width = this.width;

        if( toroidal && height%2==1 )
            throw new RuntimeException( "Runtime exception in getNeighborsHexagonalDistance: toroidal hexagonal environment should have even heights" );

        if( toroidal )
            {
            // compute ymin and ymax for the neighborhood
            int ymin = y - dist;
            int ymax = y + dist;
            for( int y0 = ymin ; y0 <= ymax ; y0 = downy(x,y0) )
                {
                xPos.add( stx(x, width) );
                yPos.add( sty(y0, height) );
                }
            int x0 = x;
            for( int i = 1 ; i <= dist ; i++ )
                {
                final int temp_ymin = ymin;
                ymin = dly( x0, ymin );
                ymax = uly( x0, ymax );
                x0 = dlx( x0, temp_ymin );
                for( int y0 = ymin ; y0 <= ymax ; y0 = downy(x0,y0) )
                    {
                    xPos.add( stx(x0, width) );
                    yPos.add( sty(y0, height) );
                    }
                }
            x0 = x;
            ymin = y-dist;
            ymax = y+dist;
            for( int i = 1 ; i <= dist ; i++ )
                {
                final int temp_ymin = ymin;
                ymin = dry( x0, ymin );
                ymax = ury( x0, ymax );
                x0 = drx( x0, temp_ymin );
                for( int y0 = ymin ; y0 <= ymax ; y0 = downy(x0,y0) )
                    {
                    xPos.add( stx(x0, width) );
                    yPos.add( sty(y0, height) );
                    }
                }
            }
        else // not toroidal
            {
            if( x < 0 || x >= width || y < 0 || y >= height )
                throw new RuntimeException( "Runtime exception in method getNeighborsHexagonalDistance: invalid initial position" );

            // compute ymin and ymax for the neighborhood
            int ylBound = y - dist;
            int yuBound = ((y+dist<height)?y+dist:height-1);

            for( int y0 = ylBound ; y0 <= yuBound ; y0 = downy(x,y0) )
                {
                xPos.add( x );
                yPos.add( y0 );

                }
            int x0 = x;
            int ymin = y-dist;
            int ymax = y+dist;
            for( int i = 1 ; i <= dist ; i++ )
                {
                final int temp_ymin = ymin;
                ymin = dly( x0, ymin );
                ymax = uly( x0, ymax );
                x0 = dlx( x0, temp_ymin );
                yuBound =  ((ymax<height)?ymax:height-1);

                if( x0 >= 0 )
                    for( int y0 = ylBound ; y0 <= yuBound ; y0 = downy(x0,y0) )
                        {
                        if( y0 >= 0 )
                            {
                            xPos.add( x0 );
                            yPos.add( y0 );
                            }
                        }
                }

            x0 = x;
            ymin = y-dist;
            ymax = y+dist;
            for( int i = 1 ; i <= dist ; i++ )
                {
                final int temp_ymin = ymin;
                ymin = dry( x0, ymin );
                ymax = ury( x0, ymax );
                x0 = drx( x0, temp_ymin );
                yuBound =  ((ymax<height)?ymax:height);
                if( x0 < width )
                    for( int y0 = ymin ; y0 <= yuBound; y0 = downy(x0,y0) )
                        {
                        if( y0 >= 0 )
                            {
                            xPos.add( x0 );
                            yPos.add( y0 );
                            }
                        }
                }
            }
        }

    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y) ) <= dist.  This region forms a
     * square 2*dist+1 cells across, centered at (X,Y).  If dist==1, this
     * is equivalent to the so-called "Moore Neighborhood" (the eight neighbors surrounding (X,Y)), plus (X,Y) itself.
     * Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     * Then places into the result Bag the objects at each of those <x,y> locations clearning it first.  
     * Returns the result Bag.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     */
    public Bag getNeighborsMaxDistance( final int x, final int y, final int dist, final boolean toroidal, Bag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getNeighborsMaxDistance( x, y, dist, toroidal, xPos, yPos );
        return getObjectsAtLocations(xPos,yPos,result);
        }

    /**
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) <= dist.  This region forms a diamond
     * 2*dist+1 cells from point to opposite point inclusive, centered at (X,Y).  If dist==1 this is
     * equivalent to the so-called "Von-Neumann Neighborhood" (the four neighbors above, below, left, and right of (X,Y)),
     * plus (X,Y) itself.
     * Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     * Then places into the result Bag the objects at each of those <x,y> locations, clearning it first.  
     * Returns the result Bag (constructing one if null had been passed in).
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     */
    public Bag getNeighborsHamiltonianDistance( final int x, final int y, final int dist, final boolean toroidal, Bag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getNeighborsHamiltonianDistance( x, y, dist, toroidal, xPos, yPos );
        return getObjectsAtLocations(xPos,yPos,result);
        }

    /**
     * Gets all neighbors located within the hexagon centered at (X,Y) and 2*dist+1 cells from point to opposite point 
     * inclusive.
     * If dist==1, this is equivalent to the six neighbors immediately surrounding (X,Y), 
     * plus (X,Y) itself.
     * Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     * Then places into the result Bag the objects at each of those <x,y> locations clearning it first.  
     * Returns the result Bag (constructing one if null had been passed in).
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     */
    public Bag getNeighborsHexagonalDistance( final int x, final int y, final int dist, final boolean toroidal, Bag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getNeighborsHexagonalDistance( x, y, dist, toroidal, xPos, yPos );
        return getObjectsAtLocations(xPos,yPos,result);
        }
    
    /** For each <xPos,yPos> location, puts all such objects into the result bag.  Returns the result bag.
        If the provided result bag is null, one will be created and returned. */
    public Bag getObjectsAtLocations(final IntBag xPos, final IntBag yPos, Bag result)
        {
        if (result==null) result = new Bag();
        else result.clear();

        final int len = xPos.numObjs;
        final int[] xs = xPos.objs;
        final int[] ys = yPos.objs;
        for(int i=0; i < len; i++)
            {
            // a little efficiency: add if we're 1, addAll if we're > 1, 
            // do nothing if we're 0
            Bag temp = getObjectsAtLocation(xs[i],ys[i]);
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


