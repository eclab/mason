/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.grid;

import sim.util.*;

/**
   A wrapper for 3D arrays of Objects.

   <p>This object expects that the 3D arrays are rectangular.  You are encouraged to access the array
   directly.  The object
   implements all of the Grid3D interface.  See Grid3D for rules on how to properly implement toroidal
   grids.
    
   <p>The width and height and length (z dimension) of the object are provided to avoid having to say field[x].length, etc. 
    
   <p>We very strongly encourage you to examine <b>SparseGrid3D</b> first to see if it's more appropriate to your task.  If you need arbitrary numbers of Objects to be able to occupy the same location in the grid, or if you have very few Objects and a very large grid, or if your space is unbounded, you should probably use SparseGrid3D instead.

*/

public class ObjectGrid3D extends AbstractGrid3D
    {
    public Object[/**x*/][/**y*/][/**z*/] field;
    
    public ObjectGrid3D (int width, int height, int length)
        {
        this.width = width;
        this.height = height;
        this.length = length;
        field = new Object[width][height][length];
        }
    
    public ObjectGrid3D (int width, int height, int length, Object initialValue)
        {
        this(width,height,length);
        setTo(initialValue);
        }
    
    public ObjectGrid3D (ObjectGrid3D values)
        {
        setTo(values);
        }
        
    public final void set(final int x, final int y, final int z, final Object val)
        {
        field[x][y][z] = val;
        }

    public final Object get(final int x, final int y, final int z)
        {
        return field[x][y][z];
        }


    public final ObjectGrid3D setTo(Object thisObj)
        {
        Object[][][] field = this.field;
        Object[][] fieldx = null;
        Object[] fieldxy = null;
        final int width = this.width;
        final int height = this.height;
        final int length = this.length;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            for(int y = 0; y<height;y++)
                {
                fieldxy = fieldx[y];
                for(int z=0;z<length;z++)
                    fieldxy[z]=thisObj;
                }
            }
        return this;
        }

        
    /** Sets all the locations in the grid to null, and returns in a Bag all stored objects 
        (including duplicates but not null values).  You are free to modify the Bag. */
    public final Bag clear()
        {
        Bag bag = new Bag();
        Object[][][] field = this.field;
        Object[][] fieldx = null;
        Object[] fieldxy = null;
        final int width = this.width;
        final int height = this.height;
        final int length = this.length;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            for(int y = 0; y<height;y++)
                {
                fieldxy = fieldx[y];
                for(int z=0;z<length;z++)
                    {
                    if (fieldxy[z]!=null) 
                        bag.add(fieldxy[z]);
                    fieldxy[z]=null;
                    }
                }
            }
        return bag;
        }


    public final ObjectGrid3D setTo(final ObjectGrid3D values)
        {
        if (width != values.width || height != values.height)
            {
            final int width = this.width = values.width;
            final int height = this.height = values.height;
            /*final int length =*/ this.length = values.length;
            Object[][][] field = this.field = new Object[width][height][];
            Object[][][] ofield = values.field;
            Object[][] fieldx = null;
            Object[][] ofieldx = null;
            for(int x =0 ; x < width; x++)
                {
                fieldx = field[x];
                ofieldx = ofield[x];
                for(int y=0 ; y < height ; y++)
                    fieldx[y] = (Object[]) (ofieldx[y].clone());
                }
            }
        else
            {
            Object[][][] field = this.field;
            Object[][][] ofield = values.field;
            Object[][] fieldx = null;
            Object[][] ofieldx = null;
            for(int x =0 ; x < width; x++)
                {
                fieldx = field[x];
                ofieldx = ofield[x];
                for(int y=0;y<height;y++)
                    System.arraycopy(ofieldx[y],0,fieldx[y],0,length);
                }
            }
        return this;
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
    public final Bag getNeighborsMaxDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, Bag result, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();
        if( zPos == null )
            zPos = new IntBag();
        if (result == null)
            result = new Bag();

        getNeighborsMaxDistance( x, y, z, dist, toroidal, xPos, yPos, zPos );

        result.clear();
        for( int i = 0 ; i < xPos.numObjs ; i++ )
            result.add( field[xPos.objs[i]][yPos.objs[i]][zPos.objs[i]] );
        return result;
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
    public final Bag getNeighborsHamiltonianDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, Bag result, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();
        if( zPos == null )
            zPos = new IntBag();
        if (result == null)
            result = new Bag();

        getNeighborsHamiltonianDistance( x, y, z, dist, toroidal, xPos, yPos, zPos );

        result.clear();
        for( int i = 0 ; i < xPos.numObjs ; i++ )
            result.add( field[xPos.objs[i]][yPos.objs[i]][zPos.objs[i]] );
        return result;
        }

    }
