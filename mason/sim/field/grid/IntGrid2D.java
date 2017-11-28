/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.grid;
import sim.util.*;

/**
   A wrapper for 2D arrays of ints.

   <p>This object expects that the 2D arrays are rectangular.  You are encouraged to access the array
   directly.  The object
   implements all of the Grid2D interface.  See Grid2D for rules on how to properly implement toroidal
   or hexagonal grids.
    
   <p>The width and height of the object are provided to avoid having to say field[x].length, etc.  
*/

public /*strictfp*/ class IntGrid2D extends AbstractGrid2D
    {
    private static final long serialVersionUID = 1;

    public int[/**x*/][/**y*/] field;
    
    public IntGrid2D (int width, int height)
        {
        reshape(width, height);
        }
    
    public IntGrid2D (int width, int height, int initialValue)
        {
        this(width,height);
        setTo(initialValue);
        }
    
    public IntGrid2D (IntGrid2D values)
        {
        setTo(values);
        }

    public IntGrid2D(int[][] values)
        {
        setTo(values);
        }
    
    /** Replaces the existing array with a new one of the given width and height,
        and with arbitrary values stored. */
    protected void reshape(int width, int height)
        {
        this.width = width;
        this.height = height;
        field = new int[width][height];
        }
    
    /** Sets location (x,y) to val */
    public final void set(final int x, final int y, final int val)
        {
        assert sim.util.LocationLog.it(this, new Int2D(x,y));
        field[x][y] = val;
        }
    
    /** Returns the element at location (x,y) */
    public final int get(final int x, final int y)
        {
        assert sim.util.LocationLog.it(this, new Int2D(x,y));
        return field[x][y];
        }

    /** Sets all the locations in the grid the provided element */
    public final IntGrid2D setTo(int thisMuch)
        {
        if (isDistributed())
            {
            int w = getWidth();
            int h = getHeight();
            reshape(w, h);
            for(int x = 0; x < w; x++)
                for(int y = 0; y < h; y++)
                    {
                    set(x, y, thisMuch);
                    }
            }
        else
            {        
            int[] fieldx = null;
            final int width = getWidth();
            final int height = getHeight();
            for(int x=0;x<width;x++)
                {
                fieldx = field[x];
                for(int y=0;y<height;y++)
                    {
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));
                    fieldx[y]=thisMuch;
                    }
                }
            }
        return this;
        }

    /** Sets the grid to a copy of the provided array, which must be rectangular. */
    public IntGrid2D setTo(int[][] field)
        {
        // check info
        
        if (field == null)
            throw new RuntimeException("IntGrid2D set to null field.");
        int w = field.length;
        int h = 0;
        if (w != 0) h = field[0].length;
        for(int i = 0; i < w; i++)
            if (field[i].length != h) // uh oh
                throw new RuntimeException("IntGrid2D initialized with a non-rectangular field.");

        // load

        reshape(w, h);
        if (isDistributed())
            {
            for(int x = 0; x < w; x++)
                for(int y = 0; y < h; y++)
                    {
                    set(x, y, field[x][y]);
                    }
            }
        else
            {        
            for(int i = 0; i < w; i++)
                this.field[i] = (int[]) field[i].clone();
            }
            return this;
        }

    /** Changes the dimensions of the grid to be the same as the one provided, then
        sets all the locations in the grid to the elements at the equivalent locations in the
        provided grid. */
    public final IntGrid2D setTo(IntGrid2D values)
        {
        if (sim.util.LocationLog.assertsEnabled)
            {
            for(int x=0; x< values.getWidth();x++)
                for(int y =0; y <values.getHeight(); y++)
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));

            }

        if (isDistributed())
            {
            reshape(values.getWidth(), values.getHeight());
            int w = getWidth();
            int h = getHeight();
            for(int x = 0; x < w; x++)
                for(int y = 0; y < h; y++)
                    {
                    set(x, y, values.get(x,y));
                    }
            }
        else if (getWidth() != values.getWidth() || getHeight() != values.getHeight())
            {
            reshape(values.getWidth(), values.getHeight());
            int width = getWidth();
            for(int x =0 ; x < width; x++)
                this.field[x] = (int []) (values.field[x].clone());
            }
        else
            {
            int width = getWidth();
            int height = getHeight();
            for(int x =0 ; x < width; x++)
                {
                System.arraycopy(values.field[x],0,field[x],0,height);
                }
            }

        return this;
        }

    /** Flattens the grid to a one-dimensional array, storing the elements in row-major order,including duplicates and null values. 
        Returns the grid. */
    public final int[] toArray()
        {
        int[] vals = new int[getWidth() * getHeight()];
        int i = 0;

        if (isDistributed())
            {
            int w = getWidth();
            int h = getHeight();
            for(int x = 0; x < w; x++)
                for(int y = 0; y < h; y++)
                    {
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));
                    vals[i++] = get(x,y);
                    }
            }
        else    // more efficient
            {        
            int[][] field = this.field;
            int[] fieldx = null;
            final int width = getWidth();
            final int height = getHeight();
            for(int x=0;x<width;x++)
                {
                fieldx = field[x];
                for(int y = 0; y<height;y++)
                    {
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));
                    vals[i++] = fieldx[y];
                    }
                }
            }
        return vals;
        }
        
    /** Returns the maximum value stored in the grid */
    public final int max()
        {
        int max = Integer.MIN_VALUE;
        if (isDistributed())
            {
            int w = getWidth();
            int h = getHeight();
            for(int x = 0; x < w; x++)
                for(int y = 0; y < h; y++)
                    {
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));
                    int g = get(x,y);
                    if (max < g)
                        max = g;
                    }
            }
        else    // more efficient
            {        
            int[] fieldx = null;
            final int width = getWidth();
            final int height = getHeight();
            for(int x=0;x<width;x++)
                {
                fieldx = field[x];
                for(int y=0;y<height;y++)
                    {
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));
                    if (max < fieldx[y]) max = fieldx[y];
                    }
                }
            }
        return max;
        }

    /** Returns the minimum value stored in the grid */
    public final int min()
        {
        int min = Integer.MAX_VALUE;

        if (isDistributed())
            {
            int w = getWidth();
            int h = getHeight();
            for(int x = 0; x < w; x++)
                for(int y = 0; y < h; y++)
                    {
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));
                    int g = get(x,y);
                    if (min > g)
                        min = g;
                    }
            }
        else    // more efficient
            {        
            int[] fieldx = null;
            final int width = getWidth();
            final int height = getHeight();
            for(int x=0;x<width;x++)
                {
                fieldx = field[x];
                for(int y=0;y<height;y++)
                    {
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));
                    if (min > fieldx[y]) min = fieldx[y];
                    }                
                }
            }
        return min;
        }
    
    /** Returns the mean value stored in the grid */
    public final double mean()
        {
        long count = 0;
        double mean = 0;

        if (isDistributed())
            {
            int w = getWidth();
            int h = getHeight();
            for(int x = 0; x < w; x++)
                for(int y = 0; y < h; y++)
                    {
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));
                    mean += get(x,y);
                    count++;
                    }
            }
        else    // more efficient
            {        
            int[] fieldx = null;
            final int width = getWidth();
            final int height = getHeight();
            for(int x=0;x<width;x++)
                {
                fieldx = field[x];
                for(int y=0;y<height;y++)
                    {
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));
                    mean += fieldx[y]; 
                    count++; 
                    }
                }
            }
        return (count == 0 ? 0 : mean / count);
        }
    
    /** Thresholds the grid so that values greater to <i>toNoMoreThanThisMuch</i> are changed to <i>toNoMoreThanThisMuch</i>.
        Returns the modified grid. 
    */
    public final IntGrid2D upperBound(int toNoMoreThanThisMuch)
        {
        if (isDistributed())
            {
            int w = getWidth();
            int h = getHeight();
            for(int x = 0; x < w; x++)
                for(int y = 0; y < h; y++)
                    {
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));
                    if (get(x,y) > toNoMoreThanThisMuch)
                        set(x,y,toNoMoreThanThisMuch);
                    }
            }
        else    // more efficient
            {        
            int[] fieldx = null;
            final int width = getWidth();
            final int height = getHeight();
            for(int x=0;x<width;x++)
                {
                fieldx = field[x];
                for(int y=0;y<height;y++)
                    {
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));
                    if (fieldx[y] > toNoMoreThanThisMuch)
                        fieldx[y] = toNoMoreThanThisMuch;
                    }
                }
            }
        return this;
        }

    /** Thresholds the grid so that values smaller than <i>toNoLowerThanThisMuch</i> are changed to <i>toNoLowerThanThisMuch</i>
        Returns the modified grid. 
    */

    public final IntGrid2D lowerBound(int toNoLowerThanThisMuch)
        {
        if (isDistributed())
            {
            int w = getWidth();
            int h = getHeight();
            for(int x = 0; x < w; x++)
                for(int y = 0; y < h; y++)
                    {
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));
                    if (get(x,y) < toNoLowerThanThisMuch)
                        set(x,y,toNoLowerThanThisMuch);
                    }
            }
        else    // more efficient
            {        
            int[] fieldx = null;
            final int width = getWidth();
            final int height = getHeight();
            for(int x=0;x<width;x++)
                {
                fieldx = field[x];
                for(int y=0;y<height;y++)
                    {
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));
                    if (fieldx[y] < toNoLowerThanThisMuch)
                        fieldx[y] = toNoLowerThanThisMuch;
                    }
                }
            }
        return this;
        }

    /** Sets each value in the grid to that value added to <i>withThisMuch</i>
        Returns the modified grid. 
    */

    public final IntGrid2D add(int withThisMuch)
        {
        if (withThisMuch==0.0) return this;

        if (isDistributed())
            {
            int w = getWidth();
            int h = getHeight();
            for(int x = 0; x < w; x++)
                for(int y = 0; y < h; y++)
                    {
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));
                    set(x, y, get(x, y) + withThisMuch);
                    }
            }
        else    // more efficient
            {        
            int[] fieldx = null;
            final int width = getWidth();
            final int height = getHeight();
            for(int x=0;x<width;x++)
                {
                fieldx = field[x];
                for(int y=0;y<height;y++)
                    {
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));
                    fieldx[y]+=withThisMuch;
                    }
                }
            }
        return this;
        }
        
    /** Sets the value at each location in the grid to that value added to the value at the equivalent location in the provided grid.
        Returns the modified grid. 
    */

    public final IntGrid2D add(IntGrid2D withThis)
        {
        checkBounds(withThis);

        if (isDistributed())
            {
            int w = getWidth();
            int h = getHeight();
            for(int x = 0; x < w; x++)
                for(int y = 0; y < h; y++)
                    {
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));
                    set(x, y, get(x, y) + withThis.get(x, y));
                    }
            }
        else    // more efficient
            {        
            int[][]ofield = withThis.field;
            int[] ofieldx = null;
            int[] fieldx = null;
            final int width = getWidth();
            final int height = getHeight();
            for(int x=0;x<width;x++)
                {
                fieldx = field[x];
                ofieldx = ofield[x];
                for(int y=0;y<height;y++)
                    {
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));
                    fieldx[y]+=ofieldx[y];
                    }
                }
            }
        return this;
        }

    /** Sets each value in the grid to that value multiplied <i>byThisMuch</i>
        Returns the modified grid. 
    */

    public final IntGrid2D multiply(int byThisMuch)
        {
        if (byThisMuch==1.0) return this;

        if (isDistributed())
            {
            int w = getWidth();
            int h = getHeight();
            for(int x = 0; x < w; x++)
                for(int y = 0; y < h; y++)
                    {
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));
                    set(x, y, get(x, y) * byThisMuch);
                    }
            }
        else    // more efficient
            {        
            int[] fieldx = null;
            final int width = getWidth();
            final int height = getHeight();
            for(int x=0;x<width;x++)
                {
                fieldx = field[x];
                for(int y=0;y<height;y++)
                    {
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));
                    fieldx[y]*=byThisMuch;
                    }
                }
            }
        return this;
        }
    
    /** Sets the value at each location in the grid to that value multiplied by to the value at the equivalent location in the provided grid.
        Returns the modified grid. 
    */

    public final IntGrid2D multiply(IntGrid2D withThis)
        {
        checkBounds(withThis);

        if (isDistributed())
            {
            int w = getWidth();
            int h = getHeight();
            for(int x = 0; x < w; x++)
                for(int y = 0; y < h; y++)
                    {
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));
                    set(x, y, get(x, y) * withThis.get(x, y));
                    }
            }
        else    // more efficient
            {        
            int[][]ofield = withThis.field;
            int[] ofieldx = null;
            int[] fieldx = null;
            final int width = getWidth();
            final int height = getHeight();
            for(int x=0;x<width;x++)
                {
                fieldx = field[x];
                ofieldx = ofield[x];
                for(int y=0;y<height;y++)
                    {
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));
                    fieldx[y]*=ofieldx[y];
                    }
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
        if (isDistributed())
            {
            int w = getWidth();
            int h = getHeight();
            for(int x = 0; x < w; x++)
                for(int y = 0; y < h; y++)
                    {
                    if (get(x, y) == from)
                        set(x, y, to);
                    }
            }
        else    // more efficient
            {        
            final int width = getWidth();
            final int height = getHeight();
            int[] fieldx = null;
            for(int x = 0; x < width; x++)
                {
                fieldx = field[x];
                for(int y = 0;  y < height; y++)
                    {
                    if (fieldx[y] == from)
                        fieldx[y] = to;
                    }
                }
            }
        }



    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y) ) <= dist, This region forms a
     * square 2*dist+1 cells across, centered at (X,Y).  If dist==1, this
     * is equivalent to the so-called "Moore Neighborhood" (the eight neighbors surrounding (X,Y)), plus (X,Y) itself.
     * Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     *
     * <p>Then places into the result IntBag any Objects which fall on one of these <x,y> locations, clearning it first.
     * Returns the result IntBag.
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
    public void getNeighborsMaxDistance( final int x, final int y, final int dist, final boolean toroidal, IntBag result, IntBag xPos, IntBag yPos )
        {
        getMooreNeighbors(x, y, dist, toroidal ? TOROIDAL : BOUNDED, true, result, xPos, yPos);
        }


    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y) ) <= dist, This region forms a
     * square 2*dist+1 cells across, centered at (X,Y).  If dist==1, this
     * is equivalent to the so-called "Moore Neighborhood" (the eight neighbors surrounding (X,Y)), plus (X,Y) itself.
     * Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     *
     * <p>Then places into the result IntBag any Objects which fall on one of these <x,y> locations, clearning it first.
     * Returns the result IntBag.
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
    public IntBag getMooreNeighbors( final int x, final int y, final int dist, int mode, boolean includeOrigin, IntBag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getMooreLocations( x, y, dist, mode, includeOrigin, xPos, yPos );
        return getObjectsAtLocations(xPos,yPos,result);
        }




    /**
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) <= dist.  This region forms a diamond
     * 2*dist+1 cells from point to opposite point inclusive, centered at (X,Y).  If dist==1 this is
     * equivalent to the so-called "Von-Neumann Neighborhood" (the four neighbors above, below, left, and right of (X,Y)),
     * plus (X,Y) itself.
     *
     * <p>Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     * Then places into the result IntBag any Objects which fall on one of these <x,y> locations, clearning it first.
     * Returns the result IntBag (constructing one if null had been passed in).
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
    public void getNeighborsHamiltonianDistance( final int x, final int y, final int dist, final boolean toroidal, IntBag result, IntBag xPos, IntBag yPos )
        {
        getVonNeumannNeighbors(x, y, dist, toroidal ? TOROIDAL : BOUNDED, true,result, xPos, yPos);
        }


    /**
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) <= dist.  This region forms a diamond
     * 2*dist+1 cells from point to opposite point inclusive, centered at (X,Y).  If dist==1 this is
     * equivalent to the so-called "Von-Neumann Neighborhood" (the four neighbors above, below, left, and right of (X,Y)),
     * plus (X,Y) itself.
     *
     * <p>Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     * Then places into the result IntBag any Objects which fall on one of these <x,y> locations, clearning it first.
     * Returns the result IntBag (constructing one if null had been passed in).
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
    public IntBag getVonNeumannNeighbors( final int x, final int y, final int dist, int mode, boolean includeOrigin, IntBag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getVonNeumannLocations( x, y, dist, mode, includeOrigin, xPos, yPos );
        return getObjectsAtLocations(xPos,yPos,result);
        }






    /**
     * Gets all neighbors located within the hexagon centered at (X,Y) and 2*dist+1 cells from point to opposite point 
     * inclusive.
     * If dist==1, this is equivalent to the six neighbors immediately surrounding (X,Y), 
     * plus (X,Y) itself.
     *
     * <p>Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     * Then places into the result IntBag any Objects which fall on one of these <x,y> locations, clearning it first.
     * Returns the result IntBag (constructing one if null had been passed in).
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
    public void getNeighborsHexagonalDistance( final int x, final int y, final int dist, final boolean toroidal, IntBag result, IntBag xPos, IntBag yPos )
        {
        getHexagonalNeighbors(x, y, dist, toroidal ? TOROIDAL : BOUNDED, true, result, xPos, yPos);
        }


    /**
     * Gets all neighbors located within the hexagon centered at (X,Y) and 2*dist+1 cells from point to opposite point 
     * inclusive.
     * If dist==1, this is equivalent to the six neighbors immediately surrounding (X,Y), 
     * plus (X,Y) itself.
     *
     * <p>Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     * Then places into the result IntBag any Objects which fall on one of these <x,y> locations, clearning it first.
     * Returns the result IntBag (constructing one if null had been passed in).
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
    public IntBag getHexagonalNeighbors( final int x, final int y, final int dist, int mode, boolean includeOrigin, IntBag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getHexagonalLocations( x, y, dist, mode, includeOrigin, xPos, yPos );
        return getObjectsAtLocations(xPos,yPos,result);
        }
                
                
    public IntBag getRadialNeighbors( final int x, final int y, final double dist, int mode, boolean includeOrigin, IntBag result, IntBag xPos, IntBag yPos )
        {
        return getRadialNeighbors(x, y, dist, mode, includeOrigin, Grid2D.ANY, true, result, xPos, yPos);
        }

    public IntBag getRadialNeighbors( final int x, final int y, final double dist, int mode, boolean includeOrigin,  int measurementRule, boolean closed,  IntBag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getRadialLocations( x, y, dist, mode, includeOrigin, measurementRule, closed, xPos, yPos );
        return getObjectsAtLocations(xPos,yPos,result);
        }
                


        
    // For each <xPos, yPos> location, puts all such objects into the result IntBag.  Modifies
    // the xPos and yPos bags so that each position corresponds to the equivalent result in
    // in the result IntBag.
    void reduceObjectsAtLocations(final IntBag xPos, final IntBag yPos, IntBag result)
        {
        if (result==null) result = new IntBag();
        else result.clear();

        for( int i = 0 ; i < xPos.numObjs ; i++ )
            {
            assert sim.util.LocationLog.it(this, new Int2D(xPos.objs[i],yPos.objs[i]));
            //int val = field[xPos.objs[i]][yPos.objs[i]] ;
            int val = get(xPos.objs[i], yPos.objs[i]);
            result.add( val );
            }
        }
                

    /* For each <xPos,yPos> location, puts all such objects into the result IntBag.  Returns the result IntBag.
       If the provided result IntBag is null, one will be created and returned. */
    IntBag getObjectsAtLocations(final IntBag xPos, final IntBag yPos, IntBag result)
        {
        if (result==null) result = new IntBag();
        else result.clear();

        for( int i = 0 ; i < xPos.numObjs ; i++ )
            {
            assert sim.util.LocationLog.it(this, new Int2D(xPos.objs[i],yPos.objs[i]));
            //int val = field[xPos.objs[i]][yPos.objs[i]] ;
            int val = get(xPos.objs[i], yPos.objs[i]);
            result.add( val );
            }
        return result;
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
    public IntBag getMooreNeighbors( int x, int y, int dist, int mode, boolean includeOrigin )
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
    public IntBag getVonNeumannNeighbors( int x, int y, int dist, int mode, boolean includeOrigin )
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
    public IntBag getHexagonalNeighbors( int x, int y, int dist, int mode, boolean includeOrigin )
        {
        return getHexagonalNeighbors(x, y, dist, mode, includeOrigin, null, null, null);
        }


    public IntBag getRadialNeighbors( final int x, final int y, final double dist, int mode, boolean includeOrigin)
        {
        return getRadialNeighbors(x, y, dist, mode, includeOrigin, null, null, null);
        }



    }
