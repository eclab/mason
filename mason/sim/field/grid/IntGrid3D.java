/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.grid;
import sim.util.*;

/**
   A wrapper for 3D arrays of ints.

   <p>This object expects that the 3D arrays are rectangular.  You are encouraged to access the array
   directly.  The object
   implements all of the Grid3D interface.  See Grid3D for rules on how to properly implement toroidal
   grids.
    
   <p>The width and height and length (z dimension) of the object are provided to avoid having to say field[x].length, etc.  
*/

public /*strictfp*/ class IntGrid3D extends AbstractGrid3D
    {
    public int[/**x*/][/**y*/][/**z*/] field;
    
    public IntGrid3D (int width, int height, int length)
        {
        this.width = width;
        this.height = height;
        this.length = length;
        field = new int[width][height][length];
        }
    
    public IntGrid3D (int width, int height, int length, int initialValue)
        {
        this(width,height,length);
        setTo(initialValue);
        }
    
    public IntGrid3D (IntGrid3D values)
        {
        super();
        setTo(values);
        }

    /** Sets location (x,y) to val */
    public final int set(final int x, final int y, final int z, final int val)
        {
        int returnval = field[x][y][z];
        field[x][y][z] = val;
        return returnval;
        }
    
    /** Returns the element at location (x,y) */
    public final int get(final int x, final int y, final int z)
        {
        return field[x][y][z];
        }

    /** Returns the maximum value stored in the grid */
    public final int max()
        {
        int max = Integer.MIN_VALUE;
        int[][]fieldx = null;
        int[] fieldxy = null;
        final int width = this.width;
        final int height = this.height;
        final int length = this.length;         
        
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            for(int y=0;y<height;y++)
                {
                fieldxy = fieldx[y];
                for(int z=0;z<length;z++)
                    if (max < fieldxy[z]) max = fieldxy[z];
                }
            }
        return max;
        }

    /** Returns the minimum value stored in the grid */
    public final int min()
        {
        int min = Integer.MAX_VALUE;
        int[][]fieldx = null;
        int[] fieldxy = null;
        final int width = this.width;
        final int height = this.height;
        final int length = this.length;         
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            for(int y=0;y<height;y++)
                {
                fieldxy = fieldx[y];
                for(int z=0;z<length;z++)
                    if (min > fieldxy[z]) min = fieldxy[z];
                }
            }
        return min;
        }
        
    /** Returns the mean value stored in the grid */
    public final double mean()
        {
        long count = 0;
        double mean = 0;
        int[][]fieldx = null;
        int[] fieldxy = null;
        final int width = this.width;
        final int height = this.height;
        final int length = this.length;         
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            for(int y=0;y<height;y++)
                {
                fieldxy = fieldx[y];
                for(int z=0;z<length;z++)
                    { mean += fieldxy[z]; count++; }
                }
            }
        return (count == 0 ? 0 : mean / count);
        }
        
    /** Sets all the locations in the grid the provided element */
    public final IntGrid3D setTo(int thisMuch)
        {
        int[][]fieldx = null;
        int[] fieldxy = null;
        final int width = this.width;
        final int height = this.height;
        final int length = this.length;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            for(int y=0;y<height;y++)
                {
                fieldxy = fieldx[y];
                for(int z=0;z<length;z++)
                    fieldxy[z]=thisMuch;
                }
            }
        return this;
        }

    /** Changes the dimensions of the grid to be the same as the one provided, then
        sets all the locations in the grid to the elements at the quivalent locations in the
        provided grid. */
    public final IntGrid3D setTo(IntGrid3D values)
        {
        if (width != values.width || height != values.height || length != values.length )
            {
            final int width = this.width = values.width;
            final int height = this.height = values.height;
            /*final int length = */this.length = values.length;
            field = new int[width][height][];
            int[][]fieldx = null;        
            int[][]ofieldx = null;        
            for(int x = 0 ; x < width; x++)
                {
                fieldx = field[x];
                ofieldx = values.field[x];
                for( int y = 0 ; y < height ; y++ )
                    fieldx[y] = (int []) (ofieldx[y].clone());
                }
            }
        else
            {
            int[][]fieldx = null;        
            int[][]ofieldx = null;
            for(int x =0 ; x < width; x++)
                {
                fieldx = field[x];
                ofieldx = values.field[x];      
                for( int y = 0 ; y < height ; y++ )
                    System.arraycopy(ofieldx[y],0,fieldx[y],0,length);
                }
            }

        return this;
        }

    /** Thresholds the grid so that values greater to <i>toNoMoreThanThisMuch</i> are changed to <i>toNoMoreThanThisMuch</i>.
        Returns the modified grid. 
    */
    public final IntGrid3D upperBound(int toNoMoreThanThisMuch)
        {
        int[][]fieldx = null;
        int[] fieldxy = null;
        final int width = this.width;
        final int height = this.height;
        final int length = this.length;         
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            for(int y=0;y<height;y++)
                {
                fieldxy = fieldx[y];
                for(int z=0;z<length;z++)
                    if (fieldxy[z] > toNoMoreThanThisMuch)
                        fieldxy[z] = toNoMoreThanThisMuch;
                }
            }
        return this;
        }

    /** Thresholds the grid so that values smaller than <i>toNoLowerThanThisMuch</i> are changed to <i>toNoLowerThanThisMuch</i>
        Returns the modified grid. 
    */

    public final IntGrid3D lowerBound(int toNoLowerThanThisMuch)
        {
        int[][]fieldx = null;
        int[] fieldxy = null;
        final int width = this.width;
        final int height = this.height;
        final int length = this.length;         
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            for(int y=0;y<height;y++)
                {
                fieldxy = fieldx[y];
                for(int z=0;z<length;z++)
                    if (fieldxy[z] < toNoLowerThanThisMuch)
                        fieldxy[z] = toNoLowerThanThisMuch;
                }
            }
        return this;
        }

    /** Sets each value in the grid to that value added to <i>withThisMuch</i>
        Returns the modified grid. 
    */
    public final IntGrid3D add(int withThisMuch)
        {
        if (withThisMuch==0.0) return this;
        int[][]fieldx = null;
        int[] fieldxy = null;
        final int width = this.width;
        final int height = this.height;
        final int length = this.length;         
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            for(int y=0;y<height;y++)
                {
                fieldxy = fieldx[y];
                for(int z=0;z<length;z++)
                    fieldxy[z]+=withThisMuch;
                }
            }
        return this;
        }
        
    /** Sets the value at each location in the grid to that value added to the value at the equivalent location in the provided grid.
        Returns the modified grid. 
    */
    public final IntGrid3D add(IntGrid3D withThis)
        {
        int[][]fieldx = null;
        int[] fieldxy = null;
        int[][][] ofield = withThis.field;
        int[][]ofieldx = null;
        int[] ofieldxy = null;
        final int width = this.width;
        final int height = this.height;
        final int length = this.length;         
                                
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            ofieldx = ofield[x];
            for(int y=0;y<height;y++)
                {
                fieldxy = fieldx[y];
                ofieldxy = ofieldx[y];
                for(int z=0;z<length;z++)
                    fieldxy[z]+=ofieldxy[z];
                }
            }
        return this;
        }

    /** Sets each value in the grid to that value multiplied <i>byThisMuch</i>
        Returns the modified grid. 
    */
    public final IntGrid3D multiply(int byThisMuch)
        {
        if (byThisMuch==1.0) return this;
        int[][]fieldx = null;
        int[] fieldxy = null;
        final int width = this.width;
        final int height = this.height;
        final int length = this.length;         
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            for(int y=0;y<height;y++)
                {
                fieldxy = fieldx[y];
                for(int z=0;z<length;z++)
                    fieldxy[z]*=byThisMuch;
                }       
            }
        return this;
        }
    
    /** Sets the value at each location in the grid to that value multiplied by to the value at the equivalent location in the provided grid.
        Returns the modified grid. 
    */
    public final IntGrid3D multiply(IntGrid3D withThis)
        {
        int[][]fieldx = null;
        int[] fieldxy = null;
        int[][][] ofield = withThis.field;
        int[][]ofieldx = null;
        int[] ofieldxy = null;
        final int width = this.width;
        final int height = this.height;
        final int length = this.length;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            ofieldx = ofield[x];
            for(int y=0;y<height;y++)
                {
                fieldxy = fieldx[y];
                ofieldxy = ofieldx[y];
                for(int z=0;z<length;z++)
                    fieldxy[z]*=ofieldxy[z];
                }
            }
        return this;
        }

    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y), abs(z-Z) ) <= dist.  This region forms a
     * cube 2*dist+1 cells across, centered at (X,Y,Z).  If dist==1, this
     * is equivalent to the twenty-six neighbors surrounding (X,Y,Z), plus (X,Y) itself.  
     * Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     * Then places into the result IntBag the elements at each of those <x,y,z> locations clearning it first.  
     * Returns the result IntBag (constructing one if null had been passed in).
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     */
    public final void getNeighborsMaxDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, IntBag result, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();
        if( zPos == null )
            zPos = new IntBag();

        getNeighborsMaxDistance( x, y, z, dist, toroidal, xPos, yPos, zPos );

        result.clear();
        for( int i = 0 ; i < xPos.numObjs ; i++ )
            result.add( field[xPos.objs[i]][yPos.objs[i]][zPos.objs[i]] );
        }

    /**
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) + abs(z-Z) <= dist.  This region 
     * forms an <a href="http://images.google.com/images?q=octahedron">octohedron</a> 2*dist+1 cells from point
     * to opposite point inclusive, centered at (X,Y,Y).  If dist==1 this is
     * equivalent to the six neighbors  above, below, left, and right, front, and behind (X,Y,Z)),
     * plus (X,Y,Z) itself.
     * Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     * Then places into the result IntBag the elements at each of those <x,y,z> locations clearning it first.  
     * Returns the result IntBag (constructing one if null had been passed in).
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     */
    public final void getNeighborsHamiltonianDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, IntBag result, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();
        if( zPos == null )
            zPos = new IntBag();

        getNeighborsHamiltonianDistance( x, y, z, dist, toroidal, xPos, yPos, zPos );

        result.clear();
        for( int i = 0 ; i < xPos.numObjs ; i++ )
            result.add( field[xPos.objs[i]][yPos.objs[i]][zPos.objs[i]] );
        }

    }
