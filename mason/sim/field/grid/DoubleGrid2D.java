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
                if (fieldx[y] > 0.0) 
                    /*Strict*/Math.floor(fieldx[y]);
                else
                    /*Strict*/Math.ceil(fieldx[y]);
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
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y) ) <= dist.  This region forms a
     * square 2*dist+1 cells across, centered at (X,Y).  If dist==1, this
     * is equivalent to the so-called "Moore Neighborhood" (the eight neighbors surrounding (X,Y)), plus (X,Y) itself.
     * Places each x and y value of these locations in the provided DoubleBags xPos and yPos, clearing the bags first.
     * Then places into the result DoubleBag the elements at each of those <x,y> locations clearning it first.  
     * Returns the result DoubleBag (constructing one if null had been passed in).
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     */
    public final DoubleBag getNeighborsMaxDistance( final int x, final int y, final int dist, final boolean toroidal, DoubleBag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();
        if (result == null)
            result = new DoubleBag();

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
     * Places each x and y value of these locations in the provided DoubleBags xPos and yPos, clearing the bags first.
     * Then places into the result DoubleBag the elements at each of those <x,y> locations clearning it first.  
     * Returns the result DoubleBag (constructing one if null had been passed in).
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     */
    public final DoubleBag getNeighborsHamiltonianDistance( final int x, final int y, final int dist, final boolean toroidal, DoubleBag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();
        if (result == null)
            result = new DoubleBag();

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
     * Places each x and y value of these locations in the provided DoubleBags xPos and yPos, clearing the bags first.
     * Then places into the result DoubleBag the elements at each of those <x,y> locations clearning it first.  
     * Returns the result DoubleBag (constructing one if null had been passed in).
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     */
    public final DoubleBag getNeighborsHexagonalDistance( final int x, final int y, final int dist, final boolean toroidal, DoubleBag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();
        if (result == null)
            result = new DoubleBag();

        getNeighborsHexagonalDistance( x, y, dist, toroidal, xPos, yPos );

        result.clear();
        for( int i = 0 ; i < xPos.numObjs ; i++ )
            result.add( field[xPos.objs[i]][yPos.objs[i]] );
        return result;
        }

    }
