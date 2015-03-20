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
    private static final long serialVersionUID = 1;

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

    public IntGrid3D(int[][][] values)
        {
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

    /** Flattens the grid to a one-dimensional array, storing the elements in row-major order,including duplicates and null values. 
        Returns the grid. */
    public final int[] toArray()
        {
        int[][][] field = this.field;
        int[][] fieldx = null;
        int[] fieldxy = null;
        final int width = this.width;
        final int height = this.height;
        final int length = this.length;
        int[] vals = new int[width * height * length];
        int i = 0;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            for(int y = 0; y<height;y++)
                {
                fieldxy = fieldx[y];
                for(int z=0;z<length;z++)
                    {
                    vals[i++] = fieldxy[z];
                    }
                }
            }
        return vals;
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

    /** Sets the grid to a copy of the provided array, which must be rectangular. */
    public IntGrid3D setTo(int[][][] field)
        {
        // check info
        
        if (field == null)
            throw new RuntimeException("IntGrid3D set to null field.");
        int w = field.length;
        int h = 0;
        int l = 0;
        if (w != 0) 
            { 
            h = field[0].length; 
            if (h != 0)
                l = field[0][0].length;
            }
                
        for(int i = 0; i < w; i++)
            {
            if (field[i].length != h) // uh oh
                throw new RuntimeException("IntGrid3D initialized with a non-rectangular field.");
            for(int j = 0; j < h; j++)
                {
                if (field[i][j].length != l) // uh oh
                    throw new RuntimeException("IntGrid3D initialized with a non-rectangular field.");
                }
            }

        // load
        
        width = w;
        height = h;
        length = l;
        this.field = new int[w][h][l];
        for(int i = 0; i < w; i++)
            for(int j=0; j< h; j++)
                {
                this.field[i][j] = (int[]) field[i][j].clone();
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
        checkBounds(withThis);
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
        checkBounds(withThis);
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
     * Replace instances of one value to another.
     * @param from any element that matches this value will be replaced
     * @param to with this value
     */

    public final void replaceAll(int from, int to)
        {
        final int width = this.width;
        final int height = this.height;
        final int length = this.length;
        int[][] fieldx = null;
        int[] fieldxy = null;
        for(int x = 0; x < width; x++)
            {
            fieldx = field[x];
            for(int y = 0;  y < height; y++)
                {
                fieldxy = fieldx[y];
                for(int z = 0; z < length; z++)
                    {
                    if (fieldxy[z] == from)
                        fieldxy[z] = to;
                    }
                }
            }
        }

    
    
    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y), abs(z-Z) ) <= dist.  This region forms a
     * cube 2*dist+1 cells across, centered at (X,Y,Z).  If dist==1, this
     * is equivalent to the twenty-six neighbors surrounding (X,Y,Z), plus (X,Y) itself.  
     * Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p>Then places into the result IntBag any Objects which fall on one of these <x,y,z> locations, clearning it first.
     * Returns the result IntBag.
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
     * <p>This function is equivalent to: <tt>getNeighborsMaxDistance(x,y,z,dist,toroidal ? Grid3D.TOROIDAL : Grid3D.BOUNDED, true, result, xPos, yPos,zPos);</tt>
     * 
     * @deprecated
     */
    public void getNeighborsMaxDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, IntBag result, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        getMooreNeighbors(x, y, z, dist, toroidal ? TOROIDAL : BOUNDED, true, result, xPos, yPos, zPos);
        }


    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y), abs(z-Z) ) <= dist.  This region forms a
     * cube 2*dist+1 cells across, centered at (X,Y,Z).  If dist==1, this
     * is equivalent to the twenty-six neighbors surrounding (X,Y,Z), plus (X,Y) itself.  
     * Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p>Then places into the result IntBag any Objects which fall on one of these <x,y,z> locations, clearning it first.
     * Returns the result IntBag.
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
    public IntBag getMooreNeighbors( final int x, final int y, final int z, final int dist, int mode, boolean includeOrigin, IntBag result, IntBag xPos, IntBag yPos, IntBag zPos )
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
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) + abs(z-Z) <= dist.  This region 
     * forms an <a href="http://images.google.com/images?q=octahedron">octohedron</a> 2*dist+1 cells from point
     * to opposite point inclusive, centered at (X,Y,Y).  If dist==1 this is
     * equivalent to the six neighbors  above, below, left, and right, front, and behind (X,Y,Z)),
     * plus (X,Y,Z) itself.
     * Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p>Then places into the result IntBag any Objects which fall on one of these <x,y,z> locations, clearning it first.
     * Returns the result IntBag.
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
    public void getNeighborsHamiltonianDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, IntBag result, IntBag xPos, IntBag yPos, IntBag zPos)
        {
        getVonNeumannNeighbors(x, y, z, dist, toroidal ? TOROIDAL : BOUNDED, true,result, xPos, yPos, zPos);
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
     * <p>Then places into the result IntBag any Objects which fall on one of these <x,y,z> locations, clearning it first.
     * Returns the result IntBag.
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
    public IntBag getVonNeumannNeighbors( final int x, final int y, int z, final int dist, int mode, boolean includeOrigin, IntBag result, IntBag xPos, IntBag yPos, IntBag zPos )
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



    public IntBag getRadialNeighbors( final int x, final int y, final int z, final int dist, int mode, boolean includeOrigin, IntBag result, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        return getRadialNeighbors(x, y, z, dist, mode, includeOrigin, Grid3D.ANY, true, result, xPos, yPos, zPos);
        }

    public IntBag getRadialNeighbors( final int x, final int y, int z, final int dist, int mode, boolean includeOrigin,  int measurementRule, boolean closed,  IntBag result, IntBag xPos, IntBag yPos, IntBag zPos)
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
                



    // the xPos and yPos bags so that each position corresponds to the equivalent result in
    // in the result IntBag.
    void reduceObjectsAtLocations(final IntBag xPos, final IntBag yPos, final IntBag zPos, IntBag result)
        {
        if (result==null) result = new IntBag();
        else result.clear();

        for( int i = 0 ; i < xPos.numObjs ; i++ )
            {
            assert sim.util.LocationLog.it(this, new Int3D(xPos.objs[i],yPos.objs[i],zPos.objs[i]));
            int val = field[xPos.objs[i]][yPos.objs[i]][zPos.objs[i]] ;
            result.add( val );
            }
        }
                

    /* For each <xPos,yPos> location, puts all such objects into the result IntBag.  Returns the result IntBag.
       If the provided result IntBag is null, one will be created and returned. */
    IntBag getObjectsAtLocations(final IntBag xPos, final IntBag yPos, final IntBag zPos, IntBag result)
        {
        if (result==null) result = new IntBag();
        else result.clear();

        for( int i = 0 ; i < xPos.numObjs ; i++ )
            {
            assert sim.util.LocationLog.it(this, new Int3D(xPos.objs[i],yPos.objs[i],zPos.objs[i]));
            int val = field[xPos.objs[i]][yPos.objs[i]][zPos.objs[i]] ;
            result.add( val );
            }
        return result;
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
    public IntBag getMooreNeighbors( int x, int y, int z, int dist, int mode, boolean includeOrigin )
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
    public IntBag getVonNeumannNeighbors( int x, int y, int z, int dist, int mode, boolean includeOrigin )
        {
        return getVonNeumannNeighbors(x, y, z, dist, mode, includeOrigin, null, null, null, null);
        }





    public IntBag getRadialNeighbors( final int x, final int y, int z, final int dist, int mode, boolean includeOrigin)
        {
        return getRadialNeighbors(x, y, z, dist, mode, includeOrigin, null, null, null, null);
        }



    }
