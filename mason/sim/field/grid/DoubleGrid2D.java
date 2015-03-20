/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.grid;
import sim.util.*;
/** 
    A wrapper for 2D arrays of doubles.

    <p>This object expects that the 2D arrays are rectangular.  You are encouraged to access the array
    directly.  The object
    implements all of the Grid2D interface.  See Grid2D for rules on how to properly implement toroidal
    or hexagonal grids.
    
    <p>The width and height of the object are provided to avoid having to say field[x].length, etc.  
*/

public /*strictfp*/ class DoubleGrid2D extends AbstractGrid2D
    {
    private static final long serialVersionUID = 1;

    public double[/**x*/][/**y*/] field;
    
    public double[][] getField() { return field; }
    
    public DoubleGrid2D (int width, int height)
        {
        this.width = width;
        this.height = height;
        field = new double[width][height];
        }
    
    public DoubleGrid2D (int width, int height, double initialValue)
        {
        this(width,height);
        setTo(initialValue);
        }
        
    public DoubleGrid2D (DoubleGrid2D values)
        {
        setTo(values);
        }

    public DoubleGrid2D(double[][] values)
        {
        setTo(values);
        }
        
    /** Sets location (x,y) to val */
    public final void set(final int x, final int y, final double val)
        {
        field[x][y] = val;
        }
    
    /** Returns the element at location (x,y) */
    public final double get(final int x, final int y)
        {
        return field[x][y];
        }

    /** Sets all the locations in the grid the provided element */
    public final DoubleGrid2D setTo(final double thisMuch)
        {
        double[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x]; 
            for(int y=0;y<height;y++)
                fieldx[y]=thisMuch;
            }
        return this;
        }

    /** Sets the grid to a copy of the provided array, which must be rectangular. */
    public DoubleGrid2D setTo(double[][] field)
        {
        // check info
        
        if (field == null)
            throw new RuntimeException("DoubleGrid2D set to null field.");
        int w = field.length;
        int h = 0;
        if (w != 0) h = field[0].length;
        for(int i = 0; i < w; i++)
            if (field[i].length != h) // uh oh
                throw new RuntimeException("DoubleGrid2D initialized with a non-rectangular field.");

        // load

        width = w;
        height = h;
        this.field = new double[w][h];
        for(int i = 0; i < w; i++)
            this.field[i] = (double[]) field[i].clone();
        return this;
        }

    /** Changes the dimensions of the grid to be the same as the one provided, then
        sets all the locations in the grid to the elements at the quivalent locations in the
        provided grid. */
    public final DoubleGrid2D setTo(final DoubleGrid2D values)
        {
        if (width != values.width || height != values.height)
            {
            final int width = this.width = values.width;
            /*final int height =*/ this.height = values.height;
            field = new double[width][];
            for(int x =0 ; x < width; x++)
                field[x] = (double []) (values.field[x].clone());
            }
        else
            {
            for(int x =0 ; x < width; x++)
                System.arraycopy(values.field[x],0,field[x],0,height);
            }
        return this;
        }

    /** Flattens the grid to a one-dimensional array, storing the elements in row-major order,including duplicates and null values. 
        Returns the grid. */
    public final double[] toArray()
        {
        double[][] field = this.field;
        double[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
        double[] vals = new double[width * height];
        int i = 0;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            for(int y = 0; y<height;y++)
                {
                vals[i++] = fieldx[y];
                }
            }
        return vals;
        }
    
    /** Returns the maximum value stored in the grid */
    public final double max()
        {
        double max = Double.NEGATIVE_INFINITY;
        final int width = this.width;
        final int height = this.height;        
        double[] fieldx = null;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x]; 
            for(int y=0;y<height;y++)
                if (max < fieldx[y]) max = fieldx[y];
            }
        return max;
        }

    /** Returns the minimum value stored in the grid */
    public final double min()
        {
        double min = Double.POSITIVE_INFINITY;
        final int width = this.width;
        final int height = this.height;
        double[] fieldx = null;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x]; 
            for(int y=0;y<height;y++)
                if (min > fieldx[y]) min = fieldx[y];
            }
        return min;
        }
        
    /** Returns the mean value stored in the grid */
    public final double mean()
        {
        long count = 0;
        double mean = 0;
        double[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x]; 
            for(int y=0;y<height;y++)
                { mean += fieldx[y]; count++; }
            }
        return (count == 0 ? 0 : mean / count);
        }
        
    /** Thresholds the grid so that values greater to <i>toNoMoreThanThisMuch</i> are changed to <i>toNoMoreThanThisMuch</i>.
        Returns the modified grid. 
    */
    public final DoubleGrid2D upperBound(final double toNoMoreThanThisMuch)
        {
        double[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x]; 
            for(int y=0;y<height;y++)
                if (fieldx[y] > toNoMoreThanThisMuch)
                    fieldx[y] = toNoMoreThanThisMuch;
            }
        return this;
        }

    /** Thresholds the grid so that values smaller than <i>toNoLowerThanThisMuch</i> are changed to <i>toNoLowerThanThisMuch</i>
        Returns the modified grid. 
    */
    public final DoubleGrid2D lowerBound(final double toNoLowerThanThisMuch)
        {
        double[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x]; 
            for(int y=0;y<height;y++)
                if (fieldx[y] < toNoLowerThanThisMuch)
                    fieldx[y] = toNoLowerThanThisMuch;
            }
        return this;
        }
    
    /** Sets each value in the grid to that value added to <i>withThisMuch</i>
        Returns the modified grid. 
    */
    public final DoubleGrid2D add(final double withThisMuch)
        {
        final int width = this.width;
        final int height = this.height;
        if (withThisMuch==0.0) return this;
        double[] fieldx = null;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x]; 
            for(int y=0;y<height;y++)
                fieldx[y]+=withThisMuch;
            }
        return this;
        }
        
    /** Sets the value at each location in the grid to that value added to the value at the equivalent location in the provided grid.
        Returns the modified grid. 
    */
    public final DoubleGrid2D add(final IntGrid2D withThis)
        {
        checkBounds(withThis);
        final int[][] otherField = withThis.field;
        double[] fieldx = null;
        int[] ofieldx = null;
        final int width = this.width;
        final int height = this.height;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            ofieldx = otherField[x];
            for(int y=0;y<height;y++)
                fieldx[y]+=ofieldx[y];
            }
        return this;
        }

    /** Sets the value at each location in the grid to that value added to the value at the equivalent location in the provided grid.
        Returns the modified grid. 
    */
    public final DoubleGrid2D add(final DoubleGrid2D withThis)
        {
        checkBounds(withThis);
        final double[][] otherField = withThis.field;
        double[] fieldx = null;
        double[] ofieldx = null;
        final int width = this.width;
        final int height = this.height;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x]; 
            ofieldx = otherField[x];
            for(int y=0;y<height;y++)
                fieldx[y]+=ofieldx[y];
            }
        return this;
        }

    /** Sets each value in the grid to that value multiplied <i>byThisMuch</i>
        Returns the modified grid. 
    */
    public final DoubleGrid2D multiply(final double byThisMuch)
        {
        if (byThisMuch==1.0) return this;
        double[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x]; 
            for(int y=0;y<height;y++)
                fieldx[y]*=byThisMuch;
            }
        return this;
        }
    
    /** Sets the value at each location in the grid to that value multiplied by to the value at the equivalent location in the provided grid.
        Returns the modified grid. 
    */
    public final DoubleGrid2D multiply(final IntGrid2D withThis)
        {
        checkBounds(withThis);
        final int[][] otherField = withThis.field;
        double[] fieldx = null;
        int[] ofieldx = null;
        final int width = this.width;
        final int height = this.height;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x]; 
            ofieldx = otherField[x];
            for(int y=0;y<height;y++)
                fieldx[y]*=ofieldx[y];
            }
        return this;
        }

    /** Sets the value at each location in the grid to that value multiplied by to the value at the equivalent location in the provided grid.
        Returns the modified grid. 
    */
    public final DoubleGrid2D multiply(final DoubleGrid2D withThis)
        {
        checkBounds(withThis);
        final double[][] otherField = withThis.field;
        double[] fieldx = null;
        double[] ofieldx = null;
        final int width = this.width;
        final int height = this.height;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x]; 
            ofieldx = otherField[x];
            for(int y=0;y<height;y++)
                fieldx[y]*=ofieldx[y];
            }
        return this;
        }

    /** Sets each value in the grid to floor(value).
        Returns the modified grid. 
    */

    public final DoubleGrid2D floor()
        {
        double[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x]; 
            for(int y=0;y<height;y++)
                fieldx[y] = /*Strict*/Math.floor(fieldx[y]);
            }
        return this;
        }

    /** Sets each value in the grid to ceil(value).
        Returns the modified grid. 
    */
    public final DoubleGrid2D ceiling()
        {
        double[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x]; 
            for(int y=0;y<height;y++)
                fieldx[y] = /*Strict*/Math.ceil(fieldx[y]);
            }
        return this;
        }
    
    /** Eliminates the decimal portion of each value in the grid (rounds towards zero).
        Returns the modified grid. 
    */
    public final DoubleGrid2D  truncate()
        {
        double[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x]; 
            for(int y=0;y<height;y++)
                fieldx[y] = (int) fieldx[y];
            //if (fieldx[y] > 0.0) 
            //    fieldx[y] = /*Strict*/Math.floor(fieldx[y]);
            //else
            //    fieldx[y] = /*Strict*/Math.ceil(fieldx[y]);
                
            }
        return this;
        }

    /** Sets each value in the grid to rint(value).  That is, each value
        is rounded to the closest integer value.  If two integers are the same
        distance, the value is rounded to the even integer.
        Returns the modified grid. 
    */
    public final DoubleGrid2D  rint()
        {
        double[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x]; 
            for(int y=0;y<height;y++)
                fieldx[y] = /*Strict*/Math.rint(fieldx[y]);
            }
        return this;
        }

    /**
     * Replace instances of one value to another.
     * @param from any element that matches this value will be replaced
     * @param to with this value
     */

    public final void replaceAll(double from, double to)
        {
        final int width = this.width;
        final int height = this.height;
        double[] fieldx = null;
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


/*

  final DoubleBag getImmediateNeighbors(int x, int y, boolean toroidal, DoubleBag result)
  {
  if (result != null)
  { result.clear();  result.resize(9); }  // not always 9 elements of course but it's the majority case by far
  else
  result = new DoubleBag(9);  // likwise

  int width = this.width;
  int height = this.height;
        
  double[] fieldx0 = null;
  double[] fieldx = null;
  double[] fieldx1 = null;
        
  if (x>0 && y>0 && x<width-1 && y<height-1)  // the majority case
  {
  // toroidal or non-toroidal
  // ---
  // -x-
  // ---

  fieldx0 = field[x-1];
  fieldx = field[x];
  fieldx1 = field[x+1];

  result.add(fieldx[y]);
  result.add(fieldx[y-1]);
  result.add(fieldx[y+1]);
  result.add(fieldx1[y]);
  result.add(fieldx1[y-1]);
  result.add(fieldx1[y+1]);
  result.add(fieldx0[y]);
  result.add(fieldx0[y-1]);
  result.add(fieldx0[y+1]);
  return result;
  }
        
  else if (toroidal)
  {
  if (x==0)
  {
  fieldx0 = field[width-1];
  fieldx = field[0];
  fieldx1 = field[1];
  }
  else if (x==width-1)
  {
  fieldx0 = field[0];
  fieldx = field[width-1];
  fieldx1 = field[width-2];
  }
  else
  {
  fieldx0 = field[x-1];
  fieldx = field[x];
  fieldx1 = field[x+1];
  }
                
  if (y==0)
  {
  result.add(fieldx[y]);
  result.add(fieldx[y+1]);
  result.add(fieldx[height-1]);
  result.add(fieldx1[y]);
  result.add(fieldx1[y+1]);
  result.add(fieldx1[height-1]);
  result.add(fieldx0[y]);
  result.add(fieldx0[y+1]);
  result.add(fieldx0[height-1]);
  }
  else if (y==height-1)
  {
  result.add(fieldx[y]);
  result.add(fieldx[y-1]);
  result.add(fieldx[0]);
  result.add(fieldx1[y]);
  result.add(fieldx1[y-1]);
  result.add(fieldx1[0]);
  result.add(fieldx0[y]);
  result.add(fieldx0[y-1]);
  result.add(fieldx0[0]);
  }
  else  // code never reaches here
  {
  result.add(fieldx[y]);
  result.add(fieldx[y-1]);
  result.add(fieldx[y+1]);
  result.add(fieldx1[y]);
  result.add(fieldx1[y-1]);
  result.add(fieldx1[y+1]);
  result.add(fieldx0[y]);
  result.add(fieldx0[y-1]);
  result.add(fieldx0[y+1]);
  }
  }
        
  else  // non-toroidal
  {
  if (x==0)
  {
  fieldx = field[0];
  fieldx1 = field[1];
  }
  else if (x==width-1)
  {
  fieldx = field[width-1];
  fieldx1 = field[width-2];
  }
  else
  {
  fieldx = field[x];
  fieldx1 = field[x+1];
  }

  if (y==0)
  {
  // x--  --x  -x-
  // ---  ---  ---
  // ---  ---  ---
  result.add(fieldx[y]);
  result.add(fieldx[y+1]);
  result.add(fieldx1[y]);
  result.add(fieldx1[y+1]);
  }
  else if (y==height-1)
  {
  // ---  ---  ---
  // ---  ---  ---
  // x--  --x  -x-
  result.add(fieldx[y]);
  result.add(fieldx[y-1]);
  result.add(fieldx1[y]);
  result.add(fieldx1[y-1]);
  }
  else
  {
  // ---  ---  ---  // the last of these cases will never happen because of the special case at the beginning
  // x--  --x  -x-
  // ---  ---  ---
  result.add(fieldx[y]);
  result.add(fieldx[y-1]);
  result.add(fieldx[y+1]);
  result.add(fieldx1[y]);
  result.add(fieldx1[y-1]);
  result.add(fieldx1[y+1]);
  }
            
  if (x != 0 && x != width-1)
  {
  fieldx0 = field[x-1];
  if (y==0)
  {
  // -x-
  // ---
  // ---
  result.add(fieldx0[y]);
  result.add(fieldx0[y+1]);
  }
  else if (y==height-1)
  {
  // ---
  // ---
  // -x-
  result.add(fieldx0[y]);
  result.add(fieldx0[y-1]);
  }
  else   // this will never happen because of the special case at the beginning
  {
  // ---
  // -x-
  // ---
  result.add(fieldx0[y]);
  result.add(fieldx0[y-1]);
  result.add(fieldx0[y+1]);
  }
  }
  }

  return result;
  }

  public boolean useNewNeighbors = true;
*/





    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y) ) <= dist, This region forms a
     * square 2*dist+1 cells across, centered at (X,Y).  If dist==1, this
     * is equivalent to the so-called "Moore Neighborhood" (the eight neighbors surrounding (X,Y)), plus (X,Y) itself.
     * Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     *
     * <p>Then places into the result DoubleBag any Objects which fall on one of these <x,y> locations, clearning it first.
     * <b>Note that the order and size of the result DoubleBag may not correspond to the X and Y bags.</b>  If you want
     * all three bags to correspond (x, y, object) then use getNeighborsAndCorrespondingPositionsMaxDistance(...)
     * Returns the result DoubleBag.
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
    public void getNeighborsMaxDistance( final int x, final int y, final int dist, final boolean toroidal, DoubleBag result, IntBag xPos, IntBag yPos )
        {
        getMooreNeighbors(x, y, dist, toroidal ? TOROIDAL : BOUNDED, true, result, xPos, yPos);
        }


    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y) ) <= dist, This region forms a
     * square 2*dist+1 cells across, centered at (X,Y).  If dist==1, this
     * is equivalent to the so-called "Moore Neighborhood" (the eight neighbors surrounding (X,Y)), plus (X,Y) itself.
     * Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     *
     * <p>Then places into the result DoubleBag any Objects which fall on one of these <x,y> locations, clearning it first.
     * <b>Note that the order and size of the result DoubleBag may not correspond to the X and Y bags.</b>  If you want
     * all three bags to correspond (x, y, object) then use getNeighborsAndCorrespondingPositionsMaxDistance(...)
     * Returns the result DoubleBag.
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
    public DoubleBag getMooreNeighbors( final int x, final int y, final int dist, int mode, boolean includeOrigin, DoubleBag result, IntBag xPos, IntBag yPos )
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
     * Then places into the result DoubleBag any Objects which fall on one of these <x,y> locations, clearning it first.
     * Note that the order and size of the result DoubleBag may not correspond to the X and Y bags.  If you want
     * all three bags to correspond (x, y, object) then use getNeighborsAndCorrespondingPositionsHamiltonianDistance(...)
     * Returns the result DoubleBag (constructing one if null had been passed in).
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
    public void getNeighborsHamiltonianDistance( final int x, final int y, final int dist, final boolean toroidal, DoubleBag result, IntBag xPos, IntBag yPos )
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
     * Then places into the result DoubleBag any Objects which fall on one of these <x,y> locations, clearning it first.
     * Note that the order and size of the result DoubleBag may not correspond to the X and Y bags.  If you want
     * all three bags to correspond (x, y, object) then use getNeighborsAndCorrespondingPositionsHamiltonianDistance(...)
     * Returns the result DoubleBag (constructing one if null had been passed in).
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
    public DoubleBag getVonNeumannNeighbors( final int x, final int y, final int dist, int mode, boolean includeOrigin, DoubleBag result, IntBag xPos, IntBag yPos )
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
     * Then places into the result DoubleBag any Objects which fall on one of these <x,y> locations, clearning it first.
     * Note that the order and size of the result DoubleBag may not correspond to the X and Y bags.  If you want
     * all three bags to correspond (x, y, object) then use getNeighborsAndCorrespondingPositionsHamiltonianDistance(...)
     * Returns the result DoubleBag (constructing one if null had been passed in).
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
    public void getNeighborsHexagonalDistance( final int x, final int y, final int dist, final boolean toroidal, DoubleBag result, IntBag xPos, IntBag yPos )
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
     * Then places into the result DoubleBag any Objects which fall on one of these <x,y> locations, clearning it first.
     * Note that the order and size of the result DoubleBag may not correspond to the X and Y bags.  If you want
     * all three bags to correspond (x, y, object) then use getNeighborsAndCorrespondingPositionsHamiltonianDistance(...)
     * Returns the result DoubleBag (constructing one if null had been passed in).
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
    public DoubleBag getHexagonalNeighbors( final int x, final int y, final int dist, int mode, boolean includeOrigin, DoubleBag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getHexagonalLocations( x, y, dist, mode, includeOrigin, xPos, yPos );
        return getObjectsAtLocations(xPos,yPos,result);
        }
                

    public DoubleBag getRadialNeighbors( final int x, final int y, final int dist, int mode, boolean includeOrigin,DoubleBag result, IntBag xPos, IntBag yPos )
        {
        return getRadialNeighbors(x, y, dist, mode, includeOrigin, Grid2D.ANY, true, result, xPos, yPos);
        }
                

    public DoubleBag getRadialNeighbors( final int x, final int y, final int dist, int mode, boolean includeOrigin,  int measurementRule, boolean closed,  DoubleBag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getRadialLocations( x, y, dist, mode, includeOrigin, measurementRule, closed, xPos, yPos );
        return getObjectsAtLocations(xPos,yPos,result);
        }
                


        
    // For each <xPos, yPos> location, puts all such objects into the result DoubleBag.  Modifies
    // the xPos and yPos bags so that each position corresponds to the equivalent result in
    // in the result DoubleBag.
    void reduceObjectsAtLocations(final IntBag xPos, final IntBag yPos, DoubleBag result)
        {
        if (result==null) result = new DoubleBag();
        else result.clear();

        for( int i = 0 ; i < xPos.numObjs ; i++ )
            {
            assert sim.util.LocationLog.it(this, new Int2D(xPos.objs[i],yPos.objs[i]));
            double val = field[xPos.objs[i]][yPos.objs[i]] ;
            result.add( val );
            }
        }
                

    /* For each <xPos,yPos> location, puts all such objects into the result DoubleBag.  Returns the result DoubleBag.
       If the provided result DoubleBag is null, one will be created and returned. */
    DoubleBag getObjectsAtLocations(final IntBag xPos, final IntBag yPos, DoubleBag result)
        {
        if (result==null) result = new DoubleBag();
        else result.clear();

        for( int i = 0 ; i < xPos.numObjs ; i++ )
            {
            assert sim.util.LocationLog.it(this, new Int2D(xPos.objs[i],yPos.objs[i]));
            double val = field[xPos.objs[i]][yPos.objs[i]] ;
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
    public DoubleBag getMooreNeighbors( int x, int y, int dist, int mode, boolean includeOrigin )
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
    public DoubleBag getVonNeumannNeighbors( int x, int y, int dist, int mode, boolean includeOrigin )
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
    public DoubleBag getHexagonalNeighbors( int x, int y, int dist, int mode, boolean includeOrigin )
        {
        return getHexagonalNeighbors(x, y, dist, mode, includeOrigin, null, null, null);
        }


    public DoubleBag getRadialNeighbors( final int x, final int y, final int dist, int mode, boolean includeOrigin)
        {
        return getRadialNeighbors(x, y, dist, mode, includeOrigin, null, null, null);
        }



    }
