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
        this.width = width;
        this.height = height;
        field = new int[width][height];
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
        int[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            for(int y=0;y<height;y++)
                {
                assert sim.util.LocationLog.it(this, new Int2D(x,y));
                fieldx[y]=thisMuch;
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
        
        this.field = new int[w][h];
        for(int i = 0; i < w; i++)
            this.field[i] = (int[]) field[i].clone();
        width = w;
        height = h;
        return this;
        }

    /** Changes the dimensions of the grid to be the same as the one provided, then
        sets all the locations in the grid to the elements at the quivalent locations in the
        provided grid. */
    public final IntGrid2D setTo(IntGrid2D values)
        {
        if (sim.util.LocationLog.assertsEnabled)
            {
            for(int x=0; x< values.width;x++)
                for(int y =0; y <values.height; y++)
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));

            }
        if (width != values.width || height != values.height)
            {
            final int width = this.width = values.width;
            /*final int height =*/ this.height = values.height;
            final int[][] field = this.field = new int[width][];
            for(int x =0 ; x < width; x++)
                field[x] = (int []) (values.field[x].clone());
            }
        else
            {
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
        int[][] field = this.field;
        int[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
        int[] vals = new int[width * height];
        int i = 0;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            for(int y = 0; y<height;y++)
                {
                assert sim.util.LocationLog.it(this, new Int2D(x,y));
                vals[i++] = fieldx[y];
                }
            }
        return vals;
        }
        
    /** Returns the maximum value stored in the grid */
    public final int max()
        {
        int max = Integer.MIN_VALUE;
        int[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            for(int y=0;y<height;y++)
                {
                assert sim.util.LocationLog.it(this, new Int2D(x,y));
                if (max < fieldx[y]) max = fieldx[y];
                }
            }
        return max;
        }

    /** Returns the minimum value stored in the grid */
    public final int min()
        {
        int min = Integer.MAX_VALUE;
        int[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            for(int y=0;y<height;y++)
                {
                assert sim.util.LocationLog.it(this, new Int2D(x,y));
                if (min > fieldx[y]) min = fieldx[y];
                }                
            }
        return min;
        }
    
    /** Returns the mean value stored in the grid */
    public final double mean()
        {
        long count = 0;
        double mean = 0;
        int[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
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
        return (count == 0 ? 0 : mean / count);
        }
    
    /** Thresholds the grid so that values greater to <i>toNoMoreThanThisMuch</i> are changed to <i>toNoMoreThanThisMuch</i>.
        Returns the modified grid. 
    */
    public final IntGrid2D upperBound(int toNoMoreThanThisMuch)
        {
        int[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
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
        return this;
        }

    /** Thresholds the grid so that values smaller than <i>toNoLowerThanThisMuch</i> are changed to <i>toNoLowerThanThisMuch</i>
        Returns the modified grid. 
    */

    public final IntGrid2D lowerBound(int toNoLowerThanThisMuch)
        {
        int[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
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
        return this;
        }

    /** Sets each value in the grid to that value added to <i>withThisMuch</i>
        Returns the modified grid. 
    */

    public final IntGrid2D add(int withThisMuch)
        {
        if (withThisMuch==0.0) return this;
        int[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            for(int y=0;y<height;y++)
                {
                assert sim.util.LocationLog.it(this, new Int2D(x,y));
                fieldx[y]+=withThisMuch;
                }
            }
        return this;
        }
        
    /** Sets the value at each location in the grid to that value added to the value at the equivalent location in the provided grid.
        Returns the modified grid. 
    */

    public final IntGrid2D add(IntGrid2D withThis)
        {
        int[][]ofield = withThis.field;
        int[] ofieldx = null;
        int[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
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
        return this;
        }

    /** Sets each value in the grid to that value multiplied <i>byThisMuch</i>
        Returns the modified grid. 
    */

    public final IntGrid2D multiply(int byThisMuch)
        {
        if (byThisMuch==1.0) return this;
        int[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            for(int y=0;y<height;y++)
                {
                assert sim.util.LocationLog.it(this, new Int2D(x,y));
                fieldx[y]*=byThisMuch;
                }
            }
        return this;
        }
    
    /** Sets the value at each location in the grid to that value multiplied by to the value at the equivalent location in the provided grid.
        Returns the modified grid. 
    */

    public final IntGrid2D multiply(IntGrid2D withThis)
        {
        int[][]ofield = withThis.field;
        int[] ofieldx = null;
        int[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
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
        return this;
        }


/*
  final IntBag getImmediateNeighbors(int x, int y, boolean toroidal, IntBag result)
  {
  if (result != null)
  { result.clear();  result.resize(9); }  // not always 9 elements of course but it's the majority case by far
  else
  result = new IntBag(9);  // likwise

  int width = this.width;
  int height = this.height;
        
  int[] fieldx0 = null;
  int[] fieldx = null;
  int[] fieldx1 = null;
        
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
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y) ) <= dist.  This region forms a
     * square 2*dist+1 cells across, centered at (X,Y).  If dist==1, this
     * is equivalent to the so-called "Moore Neighborhood" (the eight neighbors surrounding (X,Y)), plus (X,Y) itself.
     * Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     * Then places into the result IntBag the elements at each of those <x,y> locations clearning it first.  
     * Returns the result IntBag (constructing one if null had been passed in).
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p>There is a faster special case when the distance = 1 and xPos and yPos are both null (you don't care about them).
     * In this case, the neighbors are computed directly and put right into the Bag.  We suggest you use this if it dist=1
     * is what you're interested in.
     */
    public final IntBag getNeighborsMaxDistance( final int x, final int y, final int dist, final boolean toroidal, IntBag result, IntBag xPos, IntBag yPos )
        {
        //if (useNewNeighbors && dist == 1 && xPos == null && yPos == null)  // special case this for speed
        //    return getImmediateNeighbors(x, y, toroidal, result);

        
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getNeighborsMaxDistance( x, y, dist, toroidal, xPos, yPos );

        if (result != null)
            { result.clear();  result.resize(xPos.size()); }
        else
            result = new IntBag(xPos.size());

        for( int i = 0 ; i < xPos.numObjs ; i++ )
            {
            assert sim.util.LocationLog.it(this, new Int2D(xPos.objs[i],yPos.objs[i]));
            result.add( field[xPos.objs[i]][yPos.objs[i]] );
            }
        return result;
        }

    /**
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) <= dist.  This region forms a diamond
     * 2*dist+1 cells from point to opposite point inclusive, centered at (X,Y).  If dist==1 this is
     * equivalent to the so-called "Von-Neumann Neighborhood" (the four neighbors above, below, left, and right of (X,Y)),
     * plus (X,Y) itself.
     * Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     * Then places into the result IntBag the elements at each of those <x,y> locations clearning it first.  
     * Returns the result IntBag (constructing one if null had been passed in).
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     */
    public final IntBag getNeighborsHamiltonianDistance( final int x, final int y, final int dist, final boolean toroidal, IntBag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getNeighborsHamiltonianDistance( x, y, dist, toroidal, xPos, yPos );

        if (result != null)
            { result.clear();  result.resize(xPos.size()); }
        else
            result = new IntBag(xPos.size());

        for( int i = 0 ; i < xPos.numObjs ; i++ )
            {
            assert sim.util.LocationLog.it(this, new Int2D(xPos.objs[i],yPos.objs[i]));
            result.add( field[xPos.objs[i]][yPos.objs[i]] );
            }
        return result;
        }

    /**
     * Gets all neighbors located within the hexagon centered at (X,Y) and 2*dist+1 cells from point to opposite point 
     * inclusive.
     * If dist==1, this is equivalent to the six neighbors immediately surrounding (X,Y), 
     * plus (X,Y) itself.
     * Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     * Then places into the result IntBag the elements at each of those <x,y> locations clearning it first.  
     * Returns the result IntBag (constructing one if null had been passed in).
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     */
    public final IntBag getNeighborsHexagonalDistance( final int x, final int y, final int dist, final boolean toroidal, IntBag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getNeighborsHexagonalDistance( x, y, dist, toroidal, xPos, yPos );

        if (result != null)
            { result.clear();  result.resize(xPos.size()); }
        else
            result = new IntBag(xPos.size());

        for( int i = 0 ; i < xPos.numObjs ; i++ )
            {
            assert sim.util.LocationLog.it(this, new Int2D(xPos.objs[i],yPos.objs[i]));
            result.add( field[xPos.objs[i]][yPos.objs[i]] );
            }
        return result;
        }
    }
