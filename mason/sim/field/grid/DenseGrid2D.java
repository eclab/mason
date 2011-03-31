/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.grid;
import sim.util.*;
import java.util.*;

/**
   A wrapper for 2D arrays of Objects.

   <p>This object expects that the 2D arrays are rectangular.  You are encouraged to access the array
   directly.  The object
   implements all of the Grid2D interface.  See Grid2D for rules on how to properly implement toroidal
   or hexagonal grids.
    
   <p>The width and height of the object are provided to avoid having to say field[x].length, etc.
    
   <p>We very strongly encourage you to examine <b>SparseGrid2D</b> first to see if it's more appropriate to your task.  If you need arbitrary numbers of Objects to be able to occupy the same location in the grid, or if you have very few Objects and a very large grid, or if your space is unbounded, you should probably use SparseGrid2D instead.
*/

public class DenseGrid2D extends AbstractGrid2D
    {
    /** Should we remove bags in the field if they have been emptied, and let them GC, or should
        we keep them around? */
    public boolean removeEmptyBags = true;
    
    /** When a bag drops to one quarter capacity, should we replace it with a new bag? */
    public boolean replaceLargeBags = true;

    /** The size of an initial bag */
    public static final int INITIAL_BAG_SIZE = 16;

    /** No bags smaller than this size will be replaced regardless of the setting of <tt>replaceLargeBags</tt> */
    public static final int MIN_BAG_SIZE = 32;

    /** A bag must be larger than its contents by this ratio to be replaced <tt>replaceLargeBags</tt> is true*/
    public static final int LARGE_BAG_RATIO = 4;
    
    /** A bag to be replaced will be shrunk to this ratio if <tt>replaceLargeBags</tt> is true*/
    public static final int REPLACEMENT_BAG_RATIO = 2;

    public Bag[/**x*/][/**y*/] field;
    
    public DenseGrid2D (int width, int height)
        {
        this.width = width;
        this.height = height;
        field = new Bag[width][height];
        }
    
    /** Returns a bag containing all the objects at a given location, or null when there are no objects at the location.
        You should NOT MODIFY THIS BAG. This is the actual container bag, and modifying it will almost certainly break
        the Dense Field object.   If you want to modify the bag, make a copy and modify the copy instead,
        using something along the lines of <b> new Bag(<i>foo</i>.getObjectsAtLocation(<i>location</i>)) </b>.
        Furthermore, changing values in the Dense Field may result in a different bag being used -- so you should
        not rely on this bag staying valid.
    */
    public Bag getObjectsAtLocation(final int x, final int y)
        {
        return field[x][y];
        }
                
    /** Returns a bag containing all the objects at a given location, or null when there are no objects at the location.
        You should NOT MODIFY THIS BAG. This is the actual container bag, and modifying it will almost certainly break
        the Dense Field object.   If you want to modify the bag, make a copy and modify the copy instead,
        using something along the lines of <b> new Bag(<i>foo</i>.getObjectsAtLocation(<i>location</i>)) </b>.
        Furthermore, changing values in the Dense Field may result in a different bag being used -- so you should
        not rely on this bag staying valid.
    */
    public Bag getObjectsAtLocation(Int2D location) { return getObjectsAtLocation(location.x, location.y); }
        
    /** Removes all the objects stored at the given location and returns them as a Bag (which you are free to modify). 
        The location is set to null (the bag is removed) regardless of the setting of removeEmptyBags.  */
    public Bag removeObjectsAtLocation(final int x, final int y)
        {
        Bag b = field[x][y];
        field[x][y] = null;
        return b;
        }
    
    /** Removes all the objects stored at the given location and returns them as a Bag (which you are free to modify). 
        The location is set to null (the bag is removed) regardless of the setting of removeEmptyBags.  */
    public Bag removeObjectsAtLocation(Int2D location) { return removeObjectsAtLocation(location.x, location.y); }


    public boolean removeObjectAtLocation(final Object obj, final int x, final int y)
        {
        Bag b = field[x][y];
        if (b==null) return false;
        boolean result = b.remove(obj);
        int objsNumObjs = b.numObjs;
        if (removeEmptyBags && objsNumObjs==0) b = null;
        else if (replaceLargeBags && objsNumObjs >= MIN_BAG_SIZE && objsNumObjs * LARGE_BAG_RATIO <= b.objs.length)
            b.shrink(objsNumObjs * REPLACEMENT_BAG_RATIO);
        return result;
        }

    public boolean removeObjectAtLocation(final Object obj, Int2D location) { return removeObjectAtLocation(obj, location.x, location.y); }
    
    public boolean removeObjectMultiplyAtLocation(final Object obj, final int x, final int y)
        {
        Bag b = field[x][y];
        if (b==null) return false;
        boolean result = b.removeMultiply(obj);
        int objsNumObjs = b.numObjs;
        if (removeEmptyBags && objsNumObjs==0) b = null;
        else if (replaceLargeBags && objsNumObjs >= MIN_BAG_SIZE && objsNumObjs * LARGE_BAG_RATIO <= b.objs.length)
            b.shrink(objsNumObjs * REPLACEMENT_BAG_RATIO);
        return result;
        }

    public boolean removeObjectMultiplyAtLocation(final Object obj, Int2D location) { return removeObjectMultiplyAtLocation(obj, location.x, location.y); }

    /** If the object is not at [fromX, fromY], then it's simply inserted into [toX, toY], and FALSE is returned.  
        Else it is removed ONCE from [fromX, fromY] and inserted into [toX, toY] and TRUE is returned.
        If the object exists multiply at [fromX, fromY], only one instance of the object is moved.*/
    public boolean moveObject(final Object obj, final int fromX, final int fromY, final int toX, final int toY)
        {
        boolean result = removeObjectAtLocation(obj, fromX, fromY);
        addObjectToLocation(obj, toX, toY);
        return result;
        }
                
    /** If the object is not at FROM, then it's simply inserted into TO, and FALSE is returned.  
        Else it is removed ONCE from FROM and inserted into TO and TRUE is returned.
        If the object exists multiply at FROM, only one instance of the object is moved.*/
    public boolean moveObject(final Object obj, Int2D from, Int2D to) { return moveObject(obj, from.x, from.y, to.x, to.y); }
        

    public void moveObjects(final int fromX, final int fromY, final int toX, final int toY)
        {
        addObjectsToLocation(removeObjectsAtLocation(fromX, fromY), toX, toY);
        }

    public void moveObjects(Int2D from, Int2D to) { moveObjects(from.x, from.y, to.x, to.y); }
        
    public int numObjectsAtLocation(final int x, final int y)
        {
        Bag b = field[x][y];
        if (b == null) return 0;
        return b.numObjs;
        }

    public int numObjectsAtLocation(Int2D location) { return numObjectsAtLocation(location.x, location.y); }
    
    void buildBag(final Bag[] fieldx, final int y)
        {
        fieldx[y] = new Bag(INITIAL_BAG_SIZE);
        }
    
    /** Adds an object to a given location. */
    // this odd construction allows us to get under 32 bytes
    public void addObjectToLocation(final Object obj, final int x, final int y)
        {
        Bag[] fieldx = field[x];
        if (fieldx[y] == null) buildBag(fieldx, y);
        fieldx[y].add(obj);
        }
                
    public void addObjectToLocation(final Object obj, Int2D location) { addObjectToLocation(obj, location.x, location.y); }

    /** Adds an object to a given location. */
    public void addObjectsToLocation(final Bag objs, final int x, final int y)
        {
        if (objs==null) return;
        Bag[] fieldx = field[x];
        if (fieldx[y] == null) buildBag(fieldx, y);
        fieldx[y].addAll(objs);
        }

    public void addObjectsToLocation(final Bag objs, Int2D location) { addObjectsToLocation( objs, location.x, location.y ); }

    /** Adds an object to a given location. */
    public void addObjectsToLocation(final Object[] objs, final int x, final int y)
        {
        if (objs==null) return;
        Bag[] fieldx = field[x];
        if (fieldx[y] == null) buildBag(fieldx, y);
        fieldx[y].addAll(0, objs);
        }

    public void addObjectsToLocation(final Object[] objs, Int2D location) { addObjectsToLocation( objs, location.x, location.y ); }

    /** Adds an object to a given location. */
    public void addObjectsToLocation(final Collection objs, final int x, final int y)
        {
        if (objs==null) return;
        Bag[] fieldx = field[x];
        if (fieldx[y] == null) buildBag(fieldx, y);
        fieldx[y].addAll(objs);
        }

    /** Sets all the locations in the grid to null, and returns in a Bag all stored objects 
        (including duplicates but not null values).  You are free to modify the Bag. */
    public final Bag clear()
        {
        Bag bag = new Bag();
        Object[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            for(int y = 0; y<height;y++)
                {
                if (fieldx[y]!=null) 
                    bag.addAll((Bag)(fieldx[y]));
                fieldx[y]=null;
                }
            }
        return bag;
        }

    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y) ) <= dist.  This region forms a
     * square 2*dist+1 cells across, centered at (X,Y).  If dist==1, this
     * is equivalent to the so-called "Moore Neighborhood" (the eight neighbors surrounding (X,Y)), plus (X,Y) itself.
     * Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     * Then places into the result Bag the objects at each of those <x,y> locations clearning it first.  
     * Returns the result Bag (constructing one if null had been passed in).
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     */
    public final Bag getNeighborsMaxDistance( final int x, final int y, final int dist, final boolean toroidal, Bag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getNeighborsMaxDistance( x, y, dist, toroidal, xPos, yPos );

        if (result != null)
            { result.clear();  result.resize(xPos.size()); }
        else
            result = new Bag(xPos.size());

        for( int i = 0 ; i < xPos.numObjs ; i++ )
            result.addAll( field[xPos.objs[i]][yPos.objs[i]] );
        return result;
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
    public final Bag getNeighborsHamiltonianDistance( final int x, final int y, final int dist, final boolean toroidal, Bag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getNeighborsHamiltonianDistance( x, y, dist, toroidal, xPos, yPos );

        if (result != null)
            { result.clear();  result.resize(xPos.size()); }
        else
            result = new Bag(xPos.size());

        for( int i = 0 ; i < xPos.numObjs ; i++ )
            result.addAll( field[xPos.objs[i]][yPos.objs[i]] );
        return result;
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
    public final Bag getNeighborsHexagonalDistance( final int x, final int y, final int dist, final boolean toroidal, Bag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getNeighborsHexagonalDistance( x, y, dist, toroidal, xPos, yPos );

        if (result != null)
            { result.clear();  result.resize(xPos.size()); }
        else
            result = new Bag(xPos.size());

        for( int i = 0 ; i < xPos.numObjs ; i++ )
            result.addAll( field[xPos.objs[i]][yPos.objs[i]] );
        return result;
        }

    }
