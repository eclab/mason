/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.grid;
import sim.util.*;

/**
   A wrapper for 2D arrays of Objects.

   <p>This object expects that the 2D arrays are rectangular.  You are encouraged to access the array
   directly.  The object
   implements all of the Grid2D interface.  See Grid2D for rules on how to properly implement toroidal
   or hexagonal grids.
    
   <p>The width and height of the object are provided to avoid having to say field[x].length, etc.
    
   <p>We very strongly encourage you to examine <b>SparseGrid2D</b> first to see if it's more appropriate to your task.  If you need arbitrary numbers of Objects to be able to occupy the same location in the grid, or if you have very few Objects and a very large grid, or if your space is unbounded, you should probably use SparseGrid2D instead.
*/

public class ObjectGrid2D extends AbstractGrid2D
    {
    public Object[/**x*/][/**y*/] field;
    
    public ObjectGrid2D (int width, int height)
        {
        this.width = width;
        this.height = height;
        field = new Object[width][height];
        }
    
    public ObjectGrid2D (int width, int height, Object initialValue)
        {
        this(width,height);
        setTo(initialValue);
        }
        
    public ObjectGrid2D (ObjectGrid2D values)
        {
        setTo(values);
        }
    
    /** Sets location (x,y) to val */
    public final void set(final int x, final int y, final Object val)
        {
        field[x][y] = val;
        }

    /** Returns the element at location (x,y) */
    public final Object get(final int x, final int y)
        {
        return field[x][y];
        }

    /** Sets all the locations in the grid the provided element */
    public final ObjectGrid2D setTo(Object thisObj)
        {
        Object[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            for(int y = 0; y<height;y++)
                fieldx[y]=thisObj;
            }
        return this;
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
                    bag.add(fieldx[y]);
                fieldx[y]=null;
                }
            }
        return bag;
        }

    /** Changes the dimensions of the grid to be the same as the one provided, then
        sets all the locations in the grid to the elements at the quivalent locations in the
        provided grid. */
    public final ObjectGrid2D setTo(final ObjectGrid2D values)
        {
        if (width != values.width || height != values.height)
            {
            final int width = this.width = values.width;
            /*final int height =*/ this.height = values.height;
            Object[][] field = this.field = new Object[width][];
            Object[][] ofield = values.field;
            for(int x =0 ; x < width; x++)
                field[x] = (Object []) (ofield[x].clone());
            }
        else
            {
            Object[][] field = this.field;
            Object[][] ofield = values.field;
            for(int x =0 ; x < width; x++)
                System.arraycopy(ofield[x],0,field[x],0,height);
            }
        return this;
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
        if (result == null)
            result = new Bag();

        getNeighborsMaxDistance( x, y, dist, toroidal, xPos, yPos );

        result.clear();
        for( int i = 0 ; i < xPos.numObjs ; i++ )
            result.add( field[xPos.objs[i]][yPos.objs[i]] );
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
        if (result == null)
            result = new Bag();

        getNeighborsHamiltonianDistance( x, y, dist, toroidal, xPos, yPos );

        result.clear();
        for( int i = 0 ; i < xPos.numObjs ; i++ )
            result.add( field[xPos.objs[i]][yPos.objs[i]] );
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
        if (result == null)
            result = new Bag();

        getNeighborsHexagonalDistance( x, y, dist, toroidal, xPos, yPos );

        result.clear();
        for( int i = 0 ; i < xPos.numObjs ; i++ )
            result.add( field[xPos.objs[i]][yPos.objs[i]] );
        return result;
        }

    }
