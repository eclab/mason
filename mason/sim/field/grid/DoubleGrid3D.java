/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.grid;

import sim.util.*;

/**
   A wrapper for 3D arrays of doubles.

   <p>This object expects that the 3D arrays are rectangular.  You are encouraged to access the array
   directly.  The object
   implements all of the Grid3D interface.  See Grid3D for rules on how to properly implement toroidal
   grids.
    
   <p>The width and height and length (z dimension) of the object are provided to avoid having to say field[x].length, etc.  
*/

public /*strictfp*/ class DoubleGrid3D extends AbstractGrid3D
    {
    public double[/**x*/][/**y*/][/**z*/] field;
    
    public DoubleGrid3D (int width, int height, int length)
        {
        this.width = width;
        this.height = height;
        this.length = length;
        field = new double[width][height][length];
        }
    
    public DoubleGrid3D (int width, int height, int length, double initialValue)
        {
        this(width,height,length);
        setTo(initialValue);
        }
    
    public DoubleGrid3D (DoubleGrid3D values)
        {
        super();
        setTo(values);
        }

    /** Sets location (x,y,z) to val */
    public final double set(final int x, final int y, final int z, final double val)
        {
        double returnval = field[x][y][z];
        field[x][y][z] = val;
        return returnval;
        }
    
    /** Returns the element at location (x,y,z) */
    public final double get(final int x, final int y, final int z)
        {
        return field[x][y][z];
        }


    /** Returns the maximum value stored in the grid */
    public final double max()
        {
        double max = Double.NEGATIVE_INFINITY;
        double[][] fieldx = null;
        double[] fieldxy = null;
        final int width = this.width;
        final int height = this.height;
        final int length = this.length;         
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            for(int y=0;y<height;y++)
                {
                fieldxy= fieldx[y];
                for(int z=0;z<length;z++)
                    if (max < fieldxy[z]) max = fieldxy[z];
                }
            }
        return max;
        }

    /** Returns the minimum value stored in the grid */
    public final double min()
        {
        double min = Double.POSITIVE_INFINITY;
        double[][] fieldx = null;
        double[] fieldxy = null;
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
        double[][] fieldx = null;
        double[] fieldxy = null;        
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
    public final DoubleGrid3D setTo(double thisMuch)
        {
        double[][] fieldx = null;
        double[] fieldxy = null;        
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
    public final DoubleGrid3D setTo(DoubleGrid3D values)
        {
        if (width != values.width || height != values.height || length != values.length )
            {
            final int width = this.width = values.width;
            final int height = this.height = values.height;
            /*final int length =*/ this.length = values.length;
            field = new double[width][height][];
            double[][] fieldx = null;
            for(int x = 0 ; x < width; x++)
                {
                fieldx = field[x];
                for( int y = 0 ; y < height ; y++ )
                    fieldx[y] = (double []) (values.field[x][y].clone());
                }
            }
        else
            {
            for(int x =0 ; x < width; x++)
                for( int y = 0 ; y < height ; y++ )
                    System.arraycopy(values.field[x][y],0,field[x][y],0,length);
            }

        return this;
        }

    /** Thresholds the grid so that values greater to <i>toNoMoreThanThisMuch</i> are changed to <i>toNoMoreThanThisMuch</i>.
        Returns the modified grid. 
    */
    public final DoubleGrid3D upperBound(double toNoMoreThanThisMuch)
        {
        double[][] fieldx = null;
        double[] fieldxy = null;     
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
    public final DoubleGrid3D lowerBound(double toNoLowerThanThisMuch)
        {
        double[][] fieldx = null;
        double[] fieldxy = null;                        
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
    public final DoubleGrid3D add(double withThisMuch)
        {
        if (withThisMuch==0.0) return this;
        double[][] fieldx = null;
        double[] fieldxy = null;        
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
    public final DoubleGrid3D add(IntGrid3D withThis)
        {
        int[][][] otherField = withThis.field;
        int[][] ofieldx = null;
        int[] ofieldxy = null;        
        double[][] fieldx = null;
        double[] fieldxy = null;        
        final int width = this.width;
        final int height = this.height;
        final int length = this.length;         
        
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            ofieldx = otherField[x];
            for(int y=0;y<height;y++)
                {
                ofieldxy = ofieldx[y];
                fieldxy = fieldx[y];
                for(int z=0;z<length;z++)
                    fieldxy[z]+=ofieldxy[z];
                }
            }
        return this;
        }

    /** Sets the value at each location in the grid to that value added to the value at the equivalent location in the provided grid.
        Returns the modified grid. 
    */
    public final DoubleGrid3D add(DoubleGrid3D withThis)
        {
        double[][][] otherField = withThis.field;
        double[][] ofieldx = null;
        double[] ofieldxy = null;        
        double[][] fieldx = null;
        double[] fieldxy = null;        
        final int width = this.width;
        final int height = this.height;
        final int length = this.length;         
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            ofieldx = otherField[x];
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
    public final DoubleGrid3D multiply(double byThisMuch)
        {
        if (byThisMuch==1.0) return this;
        double[][] fieldx = null;
        double[] fieldxy = null;                        
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
    public final DoubleGrid3D multiply(IntGrid3D withThis)
        {
        int[][][] otherField = withThis.field;
        int[][] ofieldx = null;
        int[] ofieldxy = null;        
        double[][] fieldx = null;
        double[] fieldxy = null;        
        final int width = this.width;
        final int height = this.height;
        final int length = this.length;         
        
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            ofieldx = otherField[x];
            for(int y=0;y<height;y++)
                {
                ofieldxy = ofieldx[y];
                fieldxy = fieldx[y];
                for(int z=0;z<length;z++)
                    fieldxy[z]*=ofieldxy[z];
                }
            }
        return this;
        }

    /** Sets the value at each location in the grid to that value multiplied by to the value at the equivalent location in the provided grid.
        Returns the modified grid. 
    */
    public final DoubleGrid3D multiply(DoubleGrid3D withThis)
        {
        double[][][] otherField = withThis.field;
        double[][] ofieldx = null;
        double[] ofieldxy = null;        
        double[][] fieldx = null;
        double[] fieldxy = null;        
        final int width = this.width;
        final int height = this.height;
        final int length = this.length;         
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            ofieldx = otherField[x];
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

    /** Sets each value in the grid to floor(value).
        Returns the modified grid. 
    */
    public final DoubleGrid3D floor()
        {
        double[][] fieldx = null;
        double[] fieldxy = null;                        
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
                    fieldxy[z] = /*Strict*/Math.floor(fieldxy[z]);
                }
            }
        return this;
        }

    /** Sets each value in the grid to ceil(value).
        Returns the modified grid. 
    */
    public final DoubleGrid3D ceiling()
        {
        double[][] fieldx = null;
        double[] fieldxy = null;                        
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
                    fieldxy[z] = /*Strict*/Math.ceil(fieldxy[z]);
                }
            }
        return this;
        }
    
    /** Eliminates the decimal portion of each value in the grid (rounds towards zero).
        Returns the modified grid. 
    */
    public final DoubleGrid3D  truncate()
        {
        double[][] fieldx = null;
        double[] fieldxy = null;                        
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
                    if (fieldxy[z] > 0.0) 
                        /*Strict*/Math.floor(fieldxy[z]);
                    else
                        /*Strict*/Math.ceil(fieldxy[z]);
                }
            }
        return this;
        }

    /** Sets each value in the grid to rint(value).  That is, each value
        is rounded to the closest integer value.  If two integers are the same
        distance, the value is rounded to the even integer.
        Returns the modified grid. 
    */
    public final DoubleGrid3D  rint()
        {
        double[][] fieldx = null;
        double[] fieldxy = null;                        
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
                    fieldxy[z] = /*Strict*/Math.rint(fieldxy[z]);
                }
            }
        return this;
        }
    
    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y), abs(z-Z) ) <= dist.  This region forms a
     * cube 2*dist+1 cells across, centered at (X,Y,Z).  If dist==1, this
     * is equivalent to the twenty-six neighbors surrounding (X,Y,Z), plus (X,Y) itself.  
     * Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     * Then places into the result DoubleBag the elements at each of those <x,y,z> locations clearning it first.  
     * Returns the result DoubleBag (constructing one if null had been passed in).
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     */
    public final DoubleBag getNeighborsMaxDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, DoubleBag result, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();
        if( zPos == null )
            zPos = new IntBag();
        if (result == null)
            result = new DoubleBag();

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
     * Then places into the result DoubleBag the elements at each of those <x,y,z> locations clearning it first.  
     * Returns the result DoubleBag (constructing one if null had been passed in).
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     */
    public final DoubleBag getNeighborsHamiltonianDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, DoubleBag result, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();
        if( zPos == null )
            zPos = new IntBag();
        if (result == null)
            result = new DoubleBag();

        getNeighborsHamiltonianDistance( x, y, z, dist, toroidal, xPos, yPos, zPos );

        result.clear();
        for( int i = 0 ; i < xPos.numObjs ; i++ )
            result.add( field[xPos.objs[i]][yPos.objs[i]][zPos.objs[i]] );
        return result;
        }

    }
