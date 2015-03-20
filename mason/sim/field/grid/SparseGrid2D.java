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
   A storage facility for sparse objects in discrete 2D space, using Maps.  SparseGrid2D differs from ObjectGrid2D
   in several respects:
    
   <ul>
   <li>SparseGrid2D can store more than one object at a location.  ObjectGrid2D cannot.
   <li>ObjectGrid2D can store an object at more than one location (though it's bad form!).
   <li>SparseGrid2D can efficiently (O(1)) tell you the location of an object.
   <li>SparseGrid2D can efficiently (O(#objs)) scan through all objects.  The best you can do with ObjectGrid2D is search its array (which might have many empty slots).
   <li>Storing an object, finding its location, or changing its location, in a SparseGrid2D is O(1) but requires several Map lookups and/or removes, which has a significant constant overhead.
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

public class SparseGrid2D extends SparseField implements Grid2D, SparseField2D
    {
    private static final long serialVersionUID = 1;

    protected int width;
    protected int height;
    
    public SparseGrid2D(int width, int height)
        {
        this.width = width;
        this.height = height;
        }
    
    public SparseGrid2D(SparseGrid2D values)
        {
        super(values);
        width = values.width;
        height = values.height;
        }
    
    /** Returns the width of the grid */
    public int getWidth() { return width; }
    
    /** Returns the height of the grid */
    public int getHeight() { return height; }
    
    // slight revision for more efficiency
    public final int tx(int x) 
        { 
        final int width = this.width;
        if (x >= 0 && x < width) return x;  // do clearest case first
        x = x % width;
        if (x < 0) x = x + width;
        return x;
        }
        
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
        





    protected void removeOrigin(int x, int y, IntBag xPos, IntBag yPos)
        {
        int size = xPos.size();
        for(int i = 0; i <size; i++)
            {
            if (xPos.get(i) == x && yPos.get(i) == y)
                {
                xPos.remove(i);
                yPos.remove(i);
                return;
                }
            }
        }
        
    // only removes the first occurence
    protected void removeOriginToroidal(int x, int y, IntBag xPos, IntBag yPos)
        {
        int size = xPos.size();
        x = tx(x, width, width*2, x+width, x-width);
        y = ty(y, height, height*2, y+height, y-height);
        
        for(int i = 0; i <size; i++)
            {
            if (tx(xPos.get(i), width, width*2, x+width, x-width) == x && ty(yPos.get(i), height, height*2, y+height, y-height) == y)
                {
                xPos.remove(i);
                yPos.remove(i);
                return;
                }
            }
        }


    /** Returns the number of objects stored in the grid at the given location. */
    public int numObjectsAtLocation(final int x, final int y)
        {
        return numObjectsAtLocation(new Int2D(x,y));
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
        return getObjectsAtLocation(new Int2D(x,y));
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
        return removeObjectsAtLocation(new Int2D(x,y));
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
        


    /** @deprecated */
    public void getNeighborsMaxDistance( final int x, final int y, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos )
        {
        getMooreLocations(x, y, dist, toroidal ? TOROIDAL : BOUNDED, true, xPos, yPos);
        }

    public void getMooreLocations( final int x, final int y, final int dist, int mode, boolean includeOrigin, IntBag xPos, IntBag yPos )
        {
        boolean toroidal = (mode == TOROIDAL);
        boolean bounded = (mode == BOUNDED);

        if (mode != BOUNDED && mode != UNBOUNDED && mode != TOROIDAL)
            {
            throw new RuntimeException("Mode must be either Grid2D.BOUNDED, Grid2D.UNBOUNDED, or Grid2D.TOROIDAL");
            }
        
        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Distance must be positive" );
            }

        if( xPos == null || yPos == null )
            {
            throw new RuntimeException( "xPos and yPos should not be null" );
            }

        if( ( x < 0 || x >= width || y < 0 || y >= height ) && !bounded)
            throw new RuntimeException( "Invalid initial position" );

        xPos.clear();
        yPos.clear();

        // local variables are faster
        final int height = this.height;
        final int width = this.width;


        // for toroidal environments the code will be different because of wrapping arround
        if( toroidal )
            {
            // compute xmin and xmax for the neighborhood
            int xmin = x - dist;
            int xmax = x + dist;

            // next: is xmax - xmin humongous?  If so, no need to continue wrapping around
            if (xmax - xmin >= width)  // too wide, just use whole neighborhood
                { xmin = 0; xmax = width - 1; }
                
            // compute ymin and ymax for the neighborhood
            int ymin = y - dist;
            int ymax = y + dist;
                
            // next: is ymax - ymin humongous?  If so, no need to continue wrapping around
            if (ymax - ymin >= height)  // too wide, just use whole neighborhood
                { ymin = 0; ymax = width - 1; }
                
            for( int x0 = xmin ; x0 <= xmax ; x0++ )
                {
                final int x_0 = tx(x0, width, width*2, x0+width, x0-width);
                for( int y0 = ymin ; y0 <= ymax ; y0++ )
                    {
                    final int y_0 = ty(y0, height, height*2, y0+height, y0-height);
                    xPos.add( x_0 );
                    yPos.add( y_0 );
                    }
                }
            if (!includeOrigin) removeOriginToroidal(x,y,xPos,yPos); 
            }
        else // not toroidal
            {
            // compute xmin and xmax for the neighborhood such that they are within boundaries
            final int xmin = ((x-dist>=0) || !bounded ?x-dist:0);
            final int xmax =((x+dist<=width-1) || !bounded ?x+dist:width-1);
            // compute ymin and ymax for the neighborhood such that they are within boundaries
            final int ymin = ((y-dist>=0) || !bounded ?y-dist:0);
            final int ymax = ((y+dist<=height-1) || !bounded ?y+dist:height-1);
            for( int x0 = xmin; x0 <= xmax ; x0++ )
                {
                for( int y0 = ymin ; y0 <= ymax ; y0++ )
                    {
                    xPos.add( x0 );
                    yPos.add( y0 );
                    }
                }
            if (!includeOrigin) removeOrigin(x,y,xPos,yPos); 
            }
        }


    /** @deprecated */
    public void getNeighborsHamiltonianDistance( final int x, final int y, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos )
        {
        getVonNeumannLocations(x, y, dist, toroidal ? TOROIDAL : BOUNDED, true, xPos, yPos);
        }


    public void getVonNeumannLocations( final int x, final int y, final int dist, int mode, boolean includeOrigin, IntBag xPos, IntBag yPos )
        {
        boolean toroidal = (mode == TOROIDAL);
        boolean bounded = (mode == BOUNDED);
        
        if (mode != BOUNDED && mode != UNBOUNDED && mode != TOROIDAL)
            {
            throw new RuntimeException("Mode must be either Grid2D.BOUNDED, Grid2D.UNBOUNDED, or Grid2D.TOROIDAL");
            }
        
        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Distance must be positive" );
            }

        if( xPos == null || yPos == null )
            {
            throw new RuntimeException( "xPos and yPos should not be null" );
            }

        if( ( x < 0 || x >= width || y < 0 || y >= height ) && !bounded)
            throw new RuntimeException( "Invalid initial position" );

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
                final int x_0 = tx(x0, width, width*2, x0+width, x0-width);
                // compute ymin and ymax for the neighborhood; they depend on the current x0 value
                final int ymax = y+(dist-((x0-x>=0)?x0-x:x-x0));
                final int ymin = y-(dist-((x0-x>=0)?x0-x:x-x0));
                for( int y0 =  ymin; y0 <= ymax; y0++ )
                    {
                    final int y_0 = ty(y0, height, height*2, y0+height, y0-height);
                    xPos.add( x_0 );
                    yPos.add( y_0 );
                    }
                }
                
            if (dist * 2 >= width || dist * 2 >= height)  // too big, will have to remove duplicates
                {
                int sz = xPos.size();
                Map map = buildMap(sz);
                for(int i = 0 ; i < sz; i++)
                    {
                    Double2D elem = new Double2D(xPos.get(i), yPos.get(i));
                    if (map.containsKey(elem)) // already there
                        {
                        xPos.remove(i);
                        yPos.remove(i);
                        i--;
                        sz--;
                        }
                    else
                        {
                        map.put(elem, elem);
                        }
                    }
                }
            if (!includeOrigin) removeOriginToroidal(x,y,xPos,yPos); 
            }
        else // not toroidal
            {
            // compute xmin and xmax for the neighborhood such that they are within boundaries
            final int xmax = ((x+dist<=width-1) || !bounded ?x+dist:width-1);
            final int xmin = ((x-dist>=0) || !bounded ?x-dist:0);
            for( int x0 = xmin ; x0 <= xmax ; x0++ )
                {
                // compute ymin and ymax for the neighborhood such that they are within boundaries
                // they depend on the curreny x0 value
                final int ymax = ((y+(dist-((x0-x>=0)?x0-x:x-x0))<=height-1) || !bounded ?y+(dist-((x0-x>=0)?x0-x:x-x0)):height-1);
                final int ymin = ((y-(dist-((x0-x>=0)?x0-x:x-x0))>=0) || !bounded ?y-(dist-((x0-x>=0)?x0-x:x-x0)):0);
                for( int y0 =  ymin; y0 <= ymax; y0++ )
                    {
                    xPos.add( x0 );
                    yPos.add( y0 );
                    }
                }
            if (!includeOrigin) removeOrigin(x,y,xPos,yPos);
            }
        }


    /** @deprecated */
    public void getNeighborsHexagonalDistance( final int x, final int y, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos )
        {
        getHexagonalLocations(x, y, dist, toroidal ? TOROIDAL : BOUNDED, true, xPos, yPos);
        }

    public void getHexagonalLocations( final int x, final int y, final int dist, int mode, boolean includeOrigin, IntBag xPos, IntBag yPos )
        {
        boolean toroidal = (mode == TOROIDAL);
        boolean bounded = (mode == BOUNDED);
        
        if (mode != BOUNDED && mode != UNBOUNDED && mode != TOROIDAL)
            {
            throw new RuntimeException("Mode must be either Grid2D.BOUNDED, Grid2D.UNBOUNDED, or Grid2D.TOROIDAL");
            }
        
        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Distance must be positive" );
            }

        if( xPos == null || yPos == null )
            {
            throw new RuntimeException( "xPos and yPos should not be null" );
            }

        if( ( x < 0 || x >= width || y < 0 || y >= height ) && !bounded)
            throw new RuntimeException( "Invalid initial position" );

        xPos.clear();
        yPos.clear();

        // local variables are faster
        final int height = this.height;
        final int width = this.width;

        if( toroidal && height%2==1 )
            throw new RuntimeException( "toroidal hexagonal environment should have even heights" );

        if( toroidal )
            {
            // compute ymin and ymax for the neighborhood
            int ymin = y - dist;
            int ymax = y + dist;
            for( int y0 = ymin ; y0 <= ymax ; y0 = downy(x,y0) )
                {
                xPos.add( tx(x, width, width*2, x+width, x-width) );
                yPos.add( ty(y0, height, height*2, y0+height, y0-height) );
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
                    xPos.add( tx(x0, width, width*2, x0+width, x0-width) );
                    yPos.add( ty(y0, height, height*2, y0+height, y0-height) );
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
                    xPos.add( tx(x0, width, width*2, x0+width, x0-width) );
                    yPos.add( ty(y0, height, height*2, y0+height, y0-height) );
                    }
                }

            if (dist * 2 >= width || dist * 2 >= height)  // too big, will have to remove duplicates
                {
                int sz = xPos.size();
                Map map = buildMap(sz);
                for(int i = 0 ; i < sz; i++)
                    {
                    Double2D elem = new Double2D(xPos.get(i), yPos.get(i));
                    if (map.containsKey(elem)) // already there
                        {
                        xPos.remove(i);
                        yPos.remove(i);
                        i--;
                        sz--;
                        }
                    else
                        {
                        map.put(elem, elem);
                        }
                    }
                }
            if (!includeOrigin) removeOriginToroidal(x,y,xPos,yPos); 
            }
        else // not toroidal
            {
            int ymin = y - dist;
            int ymax = y + dist;
            
            // compute ymin and ymax for the neighborhood
            int ylBound = ((ymin >= 0 || !bounded) ? ymin : 0);
            int yuBound = ((ymax < height || !bounded) ? ymax : height-1);

            // add vertical center line of hexagon
            for( int y0 = ylBound ; y0 <= yuBound ; y0 = downy(x,y0) )
                {
                xPos.add( x );
                yPos.add( y0 );
                }
            
            // add right half of hexagon
            int x0 = x;
            ymin = y - dist;
            ymax = y + dist;
            for( int i = 1 ; i <= dist ; i++ )
                {
                final int temp_ymin = ymin;
                ymin = dly( x0, ymin );
                ymax = uly( x0, ymax );
                x0 = dlx( x0, temp_ymin );
                
                ylBound = ((ymin >= 0 || !bounded) ? ymin : 0);
                yuBound = ((ymax < height || !bounded) ? ymax : height-1);
    
                // yuBound =  (( ymax<height  || !bounded) ? ymax : height-1);

                if( x0 >= 0 )
                    for( int y0 = ylBound ; y0 <= yuBound ; y0 = downy(x0,y0) )
                        {
                        if( y0 >= 0 || !bounded )
                            {
                            xPos.add( x0 );
                            yPos.add( y0 );
                            }
                        }
                }

            x0 = x;
            ymin = y - dist;
            ymax = y + dist;
            for( int i = 1 ; i <= dist ; i++ )
                {
                final int temp_ymin = ymin;
                ymin = dry( x0, ymin );
                ymax = ury( x0, ymax );
                x0 = drx( x0, temp_ymin );

                ylBound = ((ymin >= 0 || !bounded) ? ymin : 0);
                yuBound = ((ymax < height || !bounded) ? ymax : height-1);
                
                // yuBound =  ((ymax<height) || !bounded ?ymax:height);
                
                if( x0 < width )
                    for( int y0 = ymin ; y0 <= yuBound; y0 = downy(x0,y0) )
                        {
                        if( y0 >= 0 || !bounded )
                            {
                            xPos.add( x0 );
                            yPos.add( y0 );
                            }
                        }
                }
            if (!includeOrigin) removeOrigin(x,y,xPos,yPos); 
            }
        }























    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y) ) <= dist, This region forms a
     * square 2*dist+1 cells across, centered at (X,Y).  If dist==1, this
     * is equivalent to the so-called "Moore Neighborhood" (the eight neighbors surrounding (X,Y)), plus (X,Y) itself.
     * Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     *
     * <p>Then places into the result Bag any Objects which fall on one of these <x,y> locations, clearning it first.
     * <b>Note that the order and size of the result Bag may not correspond to the X and Y bags.</b>  If you want
     * all three bags to correspond (x, y, object) then use getNeighborsAndCorrespondingPositionsMaxDistance(...)
     * Returns the result Bag.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p> This function may only run in two modes: toroidal or bounded.  Unbounded lookup is not permitted, and so
     * this function is deprecated: instead you should use the other version of this function which has more functionality.
     * If "bounded",
     * then the neighbors are restricted to be only those which lie within the box ranging from (0,0) to (width, height), 
     * that is, the width and height of the grid.   if "toroidal",
     * then the environment is assumed to be toroidal, that is, wrap-around, and neighbors are computed in this fashion.  Toroidal
     * locations will not appear multiple times: specifically, if the neighborhood distance is so large that it wraps completely around
     * the width or height of the box, neighbors will not be counted multiple times.  Note that to ensure this, subclasses may need to
     * resort to expensive duplicate removal, so it's not suggested you use so unreasonably large distances.
     *
     * <p>The origin -- that is, the (x,y) point at the center of the neighborhood -- is always included in the results.
     *
     * <p>This function is equivalent to: <tt>getNeighborsMaxDistance(x,y,dist,toroidal ? Grid2D.TOROIDAL : Grid2D.BOUNDED, true, result, xPos, yPos);</tt>
     * 
     * @deprecated
     */
    public Bag getNeighborsMaxDistance( final int x, final int y, final int dist, final boolean toroidal, Bag result, IntBag xPos, IntBag yPos )
        {
        return getMooreNeighbors(x, y, dist, toroidal ? TOROIDAL : BOUNDED, true, result, xPos, yPos);
        }


    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y) ) <= dist, This region forms a
     * square 2*dist+1 cells across, centered at (X,Y).  If dist==1, this
     * is equivalent to the so-called "Moore Neighborhood" (the eight neighbors surrounding (X,Y)), plus (X,Y) itself.
     * Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     *
     * <p>Then places into the result Bag any Objects which fall on one of these <x,y> locations, clearning it first.
     * <b>Note that the order and size of the result Bag may not correspond to the X and Y bags.</b>  If you want
     * all three bags to correspond (x, y, object) then use getNeighborsAndCorrespondingPositionsMaxDistance(...)
     * Returns the result Bag.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
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
     *
     * <p>You can also opt to include the origin -- that is, the (x,y) point at the center of the neighborhood -- in the neighborhood results.
     */
    public Bag getMooreNeighbors( final int x, final int y, final int dist, int mode, boolean includeOrigin, Bag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getMooreLocations( x, y, dist, mode, includeOrigin, xPos, yPos );
        return getObjectsAtLocations(xPos,yPos,result);
        }

    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y) ) <= dist.  This region forms a
     * square 2*dist+1 cells across, centered at (X,Y).  If dist==1, this
     * is equivalent to the so-called "Moore Neighborhood" (the eight neighbors surrounding (X,Y)), plus (X,Y) itself.
     *
     * <p>For each Object which falls within this distance, adds the X position, Y position, and Object into the
     * xPos, yPos, and result Bag, clearing them first.  
     * Some <X,Y> positions may not appear
     * and that others may appear multiply if multiple objects share that positions.  Compare this function
     * with getNeighborsMaxDistance(...).
     * Returns the result Bag.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p> This function may only run in two modes: toroidal or bounded.  Unbounded lookup is not permitted, and so
     * this function is deprecated: instead you should use the other version of this function which has more functionality.
     * If "bounded",
     * then the neighbors are restricted to be only those which lie within the box ranging from (0,0) to (width, height), 
     * that is, the width and height of the grid.   if "toroidal",
     * then the environment is assumed to be toroidal, that is, wrap-around, and neighbors are computed in this fashion.  Toroidal
     * locations will not appear multiple times: specifically, if the neighborhood distance is so large that it wraps completely around
     * the width or height of the box, neighbors will not be counted multiple times.  Note that to ensure this, subclasses may need to
     * resort to expensive duplicate removal, so it's not suggested you use so unreasonably large distances.
     *
     * <p>The origin -- that is, the (x,y) point at the center of the neighborhood -- is always included in the results.
     *
     * <p>This function is equivalent to: <tt>getNeighborsAndCorrespondingPositionsMaxDistance(x,y,dist,toroidal ? Grid2D.TOROIDAL : Grid2D.BOUNDED, true, result, xPos, yPos);</tt>
     * 
     * @deprecated
     */
    public Bag getNeighborsAndCorrespondingPositionsMaxDistance( final int x, final int y, final int dist, final boolean toroidal, Bag result, IntBag xPos, IntBag yPos )
        {
        return getMooreNeighborsAndLocations(x, y, dist, toroidal ? TOROIDAL : BOUNDED, result, xPos, yPos);
        }


    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y) ) <= dist.  This region forms a
     * square 2*dist+1 cells across, centered at (X,Y).  If dist==1, this
     * is equivalent to the so-called "Moore Neighborhood" (the eight neighbors surrounding (X,Y)), plus (X,Y) itself.
     *
     * <p>For each Object which falls within this distance, adds the X position, Y position, and Object into the
     * xPos, yPos, and result Bag, clearing them first.  
     * Some <X,Y> positions may not appear
     * and that others may appear multiply if multiple objects share that positions.  Compare this function
     * with getNeighborsMaxDistance(...).
     * Returns the result Bag.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
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
     *
     * <p>You can also opt to include the origin -- that is, the (x,y) point at the center of the neighborhood -- in the neighborhood results.
     */
    public Bag getMooreNeighborsAndLocations(final int x, final int y, final int dist, int mode, Bag result, IntBag xPos, IntBag yPos)
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getMooreLocations( x, y, dist, mode, true, xPos, yPos );
        reduceObjectsAtLocations( xPos,  yPos,  result);
        return result;
        }



    /**
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) <= dist.  This region forms a diamond
     * 2*dist+1 cells from point to opposite point inclusive, centered at (X,Y).  If dist==1 this is
     * equivalent to the so-called "Von-Neumann Neighborhood" (the four neighbors above, below, left, and right of (X,Y)),
     * plus (X,Y) itself.
     *
     * <p>Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     * Then places into the result Bag any Objects which fall on one of these <x,y> locations, clearning it first.
     * Note that the order and size of the result Bag may not correspond to the X and Y bags.  If you want
     * all three bags to correspond (x, y, object) then use getNeighborsAndCorrespondingPositionsHamiltonianDistance(...)
     * Returns the result Bag (constructing one if null had been passed in).
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p> This function may only run in two modes: toroidal or bounded.  Unbounded lookup is not permitted, and so
     * this function is deprecated: instead you should use the other version of this function which has more functionality.
     * If "bounded",
     * then the neighbors are restricted to be only those which lie within the box ranging from (0,0) to (width, height), 
     * that is, the width and height of the grid.   if "toroidal",
     * then the environment is assumed to be toroidal, that is, wrap-around, and neighbors are computed in this fashion.  Toroidal
     * locations will not appear multiple times: specifically, if the neighborhood distance is so large that it wraps completely around
     * the width or height of the box, neighbors will not be counted multiple times.  Note that to ensure this, subclasses may need to
     * resort to expensive duplicate removal, so it's not suggested you use so unreasonably large distances.
     *
     * <p>The origin -- that is, the (x,y) point at the center of the neighborhood -- is always included in the results.
     *
     * <p>This function is equivalent to: <tt>getNeighborsHamiltonianDistance(x,y,dist,toroidal ? Grid2D.TOROIDAL : Grid2D.BOUNDED, true, result, xPos, yPos);</tt>
     * 
     * @deprecated
     */
    public Bag getNeighborsHamiltonianDistance( final int x, final int y, final int dist, final boolean toroidal, Bag result, IntBag xPos, IntBag yPos )
        {
        return getVonNeumannNeighbors(x, y, dist, toroidal ? TOROIDAL : BOUNDED, true, result, xPos, yPos);
        }


    /**
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) <= dist.  This region forms a diamond
     * 2*dist+1 cells from point to opposite point inclusive, centered at (X,Y).  If dist==1 this is
     * equivalent to the so-called "Von-Neumann Neighborhood" (the four neighbors above, below, left, and right of (X,Y)),
     * plus (X,Y) itself.
     *
     * <p>Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     * Then places into the result Bag any Objects which fall on one of these <x,y> locations, clearning it first.
     * Note that the order and size of the result Bag may not correspond to the X and Y bags.  If you want
     * all three bags to correspond (x, y, object) then use getNeighborsAndCorrespondingPositionsHamiltonianDistance(...)
     * Returns the result Bag (constructing one if null had been passed in).
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
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
     *
     * <p>You can also opt to include the origin -- that is, the (x,y) point at the center of the neighborhood -- in the neighborhood results.
     */
    public Bag getVonNeumannNeighbors( final int x, final int y, final int dist, int mode, boolean includeOrigin, Bag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getVonNeumannLocations( x, y, dist, mode, includeOrigin, xPos, yPos );
        return getObjectsAtLocations(xPos,yPos,result);
        }



    /**
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) <= dist.  This region forms a diamond
     * 2*dist+1 cells from point to opposite point inclusive, centered at (X,Y).  If dist==1 this is
     * equivalent to the so-called "Von-Neumann Neighborhood" (the four neighbors above, below, left, and right of (X,Y)),
     * plus (X,Y) itself.
     *
     * <p>For each Object which falls within this distance, adds the X position, Y position, and Object into the
     * xPos, yPos, and result Bag, clearing them first.  
     * Some <X,Y> positions may not appear
     * and that others may appear multiply if multiple objects share that positions.  Compare this function
     * with getNeighborsMaxDistance(...).
     * Returns the result Bag.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p> This function may only run in two modes: toroidal or bounded.  Unbounded lookup is not permitted, and so
     * this function is deprecated: instead you should use the other version of this function which has more functionality.
     * If "bounded",
     * then the neighbors are restricted to be only those which lie within the box ranging from (0,0) to (width, height), 
     * that is, the width and height of the grid.   if "toroidal",
     * then the environment is assumed to be toroidal, that is, wrap-around, and neighbors are computed in this fashion.  Toroidal
     * locations will not appear multiple times: specifically, if the neighborhood distance is so large that it wraps completely around
     * the width or height of the box, neighbors will not be counted multiple times.  Note that to ensure this, subclasses may need to
     * resort to expensive duplicate removal, so it's not suggested you use so unreasonably large distances.
     *
     * <p>The origin -- that is, the (x,y) point at the center of the neighborhood -- is always included in the results.
     *
     * <p>This function is equivalent to: <tt>getNeighborsAndCorrespondingPositionsHamiltonianDistance(x,y,dist,toroidal ? Grid2D.TOROIDAL : Grid2D.BOUNDED, true, result, xPos, yPos);</tt>
     * 
     * @deprecated
     */
    public Bag getNeighborsAndCorrespondingPositionsHamiltonianDistance( final int x, final int y, final int dist, final boolean toroidal, Bag result, IntBag xPos, IntBag yPos )
        {
        return getVonNeumannNeighborsAndLocations(x, y, dist, toroidal ? TOROIDAL : BOUNDED, result, xPos, yPos);
        }


    /**
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) <= dist.  This region forms a diamond
     * 2*dist+1 cells from point to opposite point inclusive, centered at (X,Y).  If dist==1 this is
     * equivalent to the so-called "Von-Neumann Neighborhood" (the four neighbors above, below, left, and right of (X,Y)),
     * plus (X,Y) itself.
     *
     * <p>For each Object which falls within this distance, adds the X position, Y position, and Object into the
     * xPos, yPos, and result Bag, clearing them first.  
     * Some <X,Y> positions may not appear
     * and that others may appear multiply if multiple objects share that positions.  Compare this function
     * with getNeighborsMaxDistance(...).
     * Returns the result Bag.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
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
     *
     * <p>You can also opt to include the origin -- that is, the (x,y) point at the center of the neighborhood -- in the neighborhood results.
     */
    public Bag getVonNeumannNeighborsAndLocations(final int x, final int y, final int dist, int mode, Bag result, IntBag xPos, IntBag yPos)
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getVonNeumannLocations( x, y, dist, mode, true, xPos, yPos );
        reduceObjectsAtLocations( xPos,  yPos,  result);
        return result;
        }




    /**
     * Gets all neighbors located within the hexagon centered at (X,Y) and 2*dist+1 cells from point to opposite point 
     * inclusive.
     * If dist==1, this is equivalent to the six neighbors immediately surrounding (X,Y), 
     * plus (X,Y) itself.
     *
     * <p>Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     * Then places into the result Bag any Objects which fall on one of these <x,y> locations, clearning it first.
     * Note that the order and size of the result Bag may not correspond to the X and Y bags.  If you want
     * all three bags to correspond (x, y, object) then use getNeighborsAndCorrespondingPositionsHamiltonianDistance(...)
     * Returns the result Bag (constructing one if null had been passed in).
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p> This function may only run in two modes: toroidal or bounded.  Unbounded lookup is not permitted, and so
     * this function is deprecated: instead you should use the other version of this function which has more functionality.
     * If "bounded",
     * then the neighbors are restricted to be only those which lie within the box ranging from (0,0) to (width, height), 
     * that is, the width and height of the grid.   if "toroidal",
     * then the environment is assumed to be toroidal, that is, wrap-around, and neighbors are computed in this fashion.  Toroidal
     * locations will not appear multiple times: specifically, if the neighborhood distance is so large that it wraps completely around
     * the width or height of the box, neighbors will not be counted multiple times.  Note that to ensure this, subclasses may need to
     * resort to expensive duplicate removal, so it's not suggested you use so unreasonably large distances.
     *
     * <p>The origin -- that is, the (x,y) point at the center of the neighborhood -- is always included in the results.
     *
     * <p>This function is equivalent to: <tt>getNeighborsHexagonalDistance(x,y,dist,toroidal ? Grid2D.TOROIDAL : Grid2D.BOUNDED, true, result, xPos, yPos);</tt>
     * 
     * @deprecated
     */
    public Bag getNeighborsHexagonalDistance( final int x, final int y, final int dist, final boolean toroidal, Bag result, IntBag xPos, IntBag yPos )
        {
        return getHexagonalNeighbors(x, y, dist, toroidal ? TOROIDAL : BOUNDED, true, result, xPos, yPos);
        }


    /**
     * Gets all neighbors located within the hexagon centered at (X,Y) and 2*dist+1 cells from point to opposite point 
     * inclusive.
     * If dist==1, this is equivalent to the six neighbors immediately surrounding (X,Y), 
     * plus (X,Y) itself.
     *
     * <p>Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     * Then places into the result Bag any Objects which fall on one of these <x,y> locations, clearning it first.
     * Note that the order and size of the result Bag may not correspond to the X and Y bags.  If you want
     * all three bags to correspond (x, y, object) then use getNeighborsAndCorrespondingPositionsHamiltonianDistance(...)
     * Returns the result Bag (constructing one if null had been passed in).
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
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
     *
     * <p>You can also opt to include the origin -- that is, the (x,y) point at the center of the neighborhood -- in the neighborhood results.
     */
    public Bag getHexagonalNeighbors( final int x, final int y, final int dist, int mode, boolean includeOrigin, Bag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getHexagonalLocations( x, y, dist, mode, includeOrigin, xPos, yPos );
        return getObjectsAtLocations(xPos,yPos,result);
        }
                
                
    /**
     * Gets all neighbors located within the hexagon centered at (X,Y) and 2*dist+1 cells from point to opposite point 
     * inclusive.
     * If dist==1, this is equivalent to the six neighbors immediately surrounding (X,Y), 
     * plus (X,Y) itself.
     *
     * <p>For each Object which falls within this distance, adds the X position, Y position, and Object into the
     * xPos, yPos, and result Bag, clearing them first.  
     * Some <X,Y> positions may not appear
     * and that others may appear multiply if multiple objects share that positions.  Compare this function
     * with getNeighborsMaxDistance(...).
     * Returns the result Bag.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p> This function may only run in two modes: toroidal or bounded.  Unbounded lookup is not permitted, and so
     * this function is deprecated: instead you should use the other version of this function which has more functionality.
     * If "bounded",
     * then the neighbors are restricted to be only those which lie within the box ranging from (0,0) to (width, height), 
     * that is, the width and height of the grid.   if "toroidal",
     * then the environment is assumed to be toroidal, that is, wrap-around, and neighbors are computed in this fashion.  Toroidal
     * locations will not appear multiple times: specifically, if the neighborhood distance is so large that it wraps completely around
     * the width or height of the box, neighbors will not be counted multiple times.  Note that to ensure this, subclasses may need to
     * resort to expensive duplicate removal, so it's not suggested you use so unreasonably large distances.
     *
     * <p>The origin -- that is, the (x,y) point at the center of the neighborhood -- is always included in the results.
     *
     * <p>This function is equivalent to: <tt>getNeighborsAndCorrespondingPositionsHexagonalDistance(x,y,dist,toroidal ? Grid2D.TOROIDAL : Grid2D.BOUNDED, true, result, xPos, yPos);</tt>
     * 
     * @deprecated
     */
    public Bag getNeighborsAndCorrespondingPositionsHexagonalDistance( final int x, final int y, final int dist, final boolean toroidal, Bag result, IntBag xPos, IntBag yPos )
        {
        return getHexagonalNeighborsAndLocations(x, y, dist, toroidal ? TOROIDAL : BOUNDED, result, xPos, yPos);
        }


    /**
     * Gets all neighbors located within the hexagon centered at (X,Y) and 2*dist+1 cells from point to opposite point 
     * inclusive.
     * If dist==1, this is equivalent to the six neighbors immediately surrounding (X,Y), 
     * plus (X,Y) itself.
     *
     * <p>For each Object which falls within this distance, adds the X position, Y position, and Object into the
     * xPos, yPos, and result Bag, clearing them first.  
     * Some <X,Y> positions may not appear
     * and that others may appear multiply if multiple objects share that positions.  Compare this function
     * with getNeighborsMaxDistance(...).
     * Returns the result Bag.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
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
     *
     * <p>You can also opt to include the origin -- that is, the (x,y) point at the center of the neighborhood -- in the neighborhood results.
     */
    public Bag getHexagonalNeighborsAndLocations(final int x, final int y, final int dist, int mode, Bag result, IntBag xPos, IntBag yPos)
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getHexagonalLocations( x, y, dist, mode, true, xPos, yPos );
        reduceObjectsAtLocations( xPos,  yPos,  result);
        return result;
        }


    // For each <xPos, yPos> location, puts all such objects into the result bag.  Modifies
    // the xPos and yPos bags so that each position corresponds to the equivalent result in
    // in the result bag.
    void reduceObjectsAtLocations(final IntBag xPos, final IntBag yPos, Bag result)
        {
        if (result==null) result = new Bag();
        else result.clear();

        // build new bags with <x,y> locations one per each result
        IntBag newXPos = new IntBag();
        IntBag newYPos = new IntBag();

        final int len = xPos.numObjs;
        final int[] xs = xPos.objs;
        final int[] ys = yPos.objs;

        // for each location...
        for(int i=0; i < len; i++)
            {
            Bag temp = getObjectsAtLocation(xs[i],ys[i]);
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
                    }
                }
            }
                
        // dump the new IntBags into the old ones
        xPos.clear();
        xPos.addAll(newXPos);
        yPos.clear();
        yPos.addAll(newYPos);
        }
                

    /* For each <xPos,yPos> location, puts all such objects into the result bag.  Returns the result bag.
       If the provided result bag is null, one will be created and returned. */
    Bag getObjectsAtLocations(final IntBag xPos, final IntBag yPos, Bag result)
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





    double ds(double d1x, double d1y, double d2x, double d2y)
        {
        return ((d1x - d2x) * (d1x - d2x) + (d1y - d2y) * (d1y - d2y));
        }
    
    boolean within(double d1x, double d1y, double d2x, double d2y, double distanceSquared, boolean closed)
        {
        double d= ds(d1x, d1y, d2x, d2y);
        return (d < distanceSquared || (d == distanceSquared && closed));
        }
        
    public void getRadialLocations( final int x, final int y, final double dist, int mode, boolean includeOrigin, IntBag xPos, IntBag yPos )
        {
        getRadialLocations(x, y, dist, mode, includeOrigin, Grid2D.ANY, true, xPos, yPos);
        }
        
    public void getRadialLocations( final int x, final int y, final double dist, int mode, boolean includeOrigin, int measurementRule, boolean closed, IntBag xPos, IntBag yPos )
        {
        boolean toroidal = (mode == TOROIDAL);

        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Distance must be positive" );
            }
            
        if (measurementRule != Grid2D.ANY && measurementRule != Grid2D.ALL && measurementRule != Grid2D.CENTER)
            {
            throw new RuntimeException(" Measurement rule must be one of ANY, ALL, or CENTER" );
            }
                
        // grab the rectangle
        if (toroidal)
            getMooreLocations(x,y, (int) Math.ceil(dist + 0.5), UNBOUNDED, includeOrigin, xPos, yPos);
        else
            getMooreLocations(x,y, (int) Math.ceil(dist + 0.5), mode, includeOrigin, xPos, yPos);
        int len = xPos.size();
        double distsq = dist * dist;
        
        int width = this.width;
        int height = this.height;
        int widthtimestwo = width * 2;
        int heighttimestwo = height * 2;
        
        for(int i = 0; i < len; i++)
            {
            int xp = xPos.get(i);
            int yp = yPos.get(i);
            boolean remove = false;
                
            if (measurementRule == Grid2D.ANY)
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
                if (x < xp)
                    {
                    if (y < yp)
                        remove = !within(x,y,xp-0.5,yp-0.5,distsq,closed);
                    else
                        remove = !within(x,y,xp-0.5,yp+0.5,distsq,closed);
                    }
                else
                    {
                    if (y < yp)
                        remove = !within(x,y,xp+0.5,yp-0.5,distsq,closed);
                    else
                        remove = !within(x,y,xp+0.5,yp+0.5,distsq,closed);
                    }
                }
            else if (measurementRule == Grid2D.ALL)
                {
                if (x < xp)
                    {
                    if (y < yp)
                        remove = !within(x,y,xp+0.5,yp+0.5,distsq,closed);
                    else
                        remove = !within(x,y,xp+0.5,yp-0.5,distsq,closed);
                    }
                else
                    {
                    if (y < yp)
                        remove = !within(x,y,xp-0.5,yp+0.5,distsq,closed);
                    else
                        remove = !within(x,y,xp-0.5,yp-0.5,distsq,closed);
                    }
                }
            else // (measurementRule == Grid2D.CENTER)
                {
                remove = !within(x,y,xp,yp,distsq,closed);
                }
                
            if (remove)
                { xPos.remove(i); yPos.remove(i); i--;  len--; }
            else if (toroidal) // need to convert to toroidal position
                { 
                int _x = xPos.get(i);
                int _y = yPos.get(i);
                xPos.set(i, tx(_x, width, widthtimestwo, _x + width, _x - width));
                yPos.set(i, tx(_y, height, heighttimestwo, _y + width, _y - width));
                }
            }
        }


    public Bag getRadialNeighbors( final int x, final int y, final int dist, int mode, boolean includeOrigin,  Bag result, IntBag xPos, IntBag yPos )
        {
        return getRadialNeighbors(x, y, dist, mode, includeOrigin, Grid2D.ANY, true, result, xPos, yPos);
        }


    public Bag getRadialNeighborsAndLocations( final int x, final int y, final int dist, int mode, boolean includeOrigin, Bag result, IntBag xPos, IntBag yPos )
        {
        return getRadialNeighborsAndLocations(x, y, dist, mode, includeOrigin, Grid2D.ANY, true, result, xPos, yPos);
        }



    public Bag getRadialNeighbors( final int x, final int y, final int dist, int mode, boolean includeOrigin,  int measurementRule, boolean closed,  Bag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getRadialLocations( x, y, dist, mode, includeOrigin, measurementRule, closed, xPos, yPos );
        return getObjectsAtLocations(xPos,yPos,result);
        }
                

    public Bag getRadialNeighborsAndLocations( final int x, final int y, final int dist, int mode, boolean includeOrigin,  int measurementRule, boolean closed,  Bag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getRadialLocations( x, y, dist, mode, includeOrigin, measurementRule, closed, xPos, yPos );
        reduceObjectsAtLocations( xPos,  yPos,  result);
        return getObjectsAtLocations(xPos,yPos,result);
        }



    /**
     * Determines all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y) ) <= dist. This region forms a
     * square 2*dist+1 cells across, centered at (X,Y).  If dist==1, this
     * is equivalent to the so-called "Moore Neighborhood" (the eight neighbors surrounding (X,Y)), plus (X,Y) itself.
     * <p>Then returns, as a Bag, any Objects which fall on one of these <x,y> locations.
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
    public Bag getMooreNeighbors( int x, int y, int dist, int mode, boolean includeOrigin )
        {
        return getMooreNeighbors(x, y, dist, mode, includeOrigin, null, null, null);
        }



    /**
     * Determines all neighbors of a location that satisfy abs(x-X) + abs(y-Y) <= dist.  This region forms a diamond
     * 2*dist+1 cells from point to opposite point inclusive, centered at (X,Y).  If dist==1 this is
     * equivalent to the so-called "Von-Neumann Neighborhood" (the four neighbors above, below, left, and right of (X,Y)),
     * plus (X,Y) itself.
     * <p>Then returns, as a Bag, any Objects which fall on one of these <x,y> locations.
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
    public Bag getVonNeumannNeighbors( int x, int y, int dist, int mode, boolean includeOrigin )
        {
        return getVonNeumannNeighbors(x, y, dist, mode, includeOrigin, null, null, null);
        }




    /**
     * Determines all locations located within the hexagon centered at (X,Y) and 2*dist+1 cells from point to opposite point 
     * inclusive.
     * If dist==1, this is equivalent to the six neighboring locations immediately surrounding (X,Y), 
     * plus (X,Y) itself.
     * <p>Then returns, as a Bag, any Objects which fall on one of these <x,y> locations.
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
    public Bag getHexagonalNeighbors( int x, int y, int dist, int mode, boolean includeOrigin )
        {
        return getHexagonalNeighbors(x, y, dist, mode, includeOrigin, null, null, null);
        }


    public Bag getRadialNeighbors( final int x, final int y, final int dist, int mode, boolean includeOrigin)
        {
        return getRadialNeighbors(x, y, dist, mode, includeOrigin, null, null, null);
        }






    public final Double2D getDimensions() { return new Double2D(width, height); }
    }


