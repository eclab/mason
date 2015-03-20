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
    private static final long serialVersionUID = 1;

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
        
    public ObjectGrid3D(Object[][][] values)
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

        
    /**
     * Replace instances of one value to another.  Equality is measured using equals(...).
     * null is considered equal to null.  This is equivalent to calling replaceAll(from, to, false)
     * @param from any element that matches this value will be replaced
     * @param to with this value
     */

    public final void replaceAll(Object from, Object to)
        {
        replaceAll(from, to, false);
        }

    /**
     * Replace instances of one value to another.  Equality is measured
     * as follows.  (1) if onlyIfSameObject is true, then objects must be "== from"
     * to one another to be considered equal.  (2) if onlyIfSameObject is false,
     * then objects in the field must be "equals(from)".  In either case, null
     * is considered equal to null.
     * @param from any element that matches this value will be replaced
     * @param to with this value
     */

    public final void replaceAll(Object from, Object to, boolean onlyIfSameObject)
        {
        final int width = this.width;
        final int height = this.height;
        final int length = this.length;
        Object[][] fieldx = null;
        Object[] fieldxy = null;
        for(int x = 0; x < width; x++)
            {
            fieldx = field[x];
            for(int y = 0;  y < height; y++)
                {
                fieldxy = fieldx[y];
                for(int z = 0; z < length; z++)
                    {
                    Object obj = fieldxy[z];
                    if ((obj == null && from == null) ||
                        (onlyIfSameObject && obj == from) ||
                        (!onlyIfSameObject && obj.equals(from)))
                        fieldxy[z] = to;
                    }
                }
            }
        }

    /** Flattens the grid to a one-dimensional array, storing the elements in row-major order,including duplicates and null values. 
        Returns the grid. */
    public final Object[] toArray()
        {
        Object[][][] field = this.field;
        Object[][] fieldx = null;
        Object[] fieldxy = null;
        final int width = this.width;
        final int height = this.height;
        final int length = this.length;
        Object[] vals = new Object[width * height * length];
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
        

    /** Returns in a Bag all stored objects 
        (including duplicates but not null values).  You are free to modify the Bag. */
    public final Bag elements()
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
                    }
                }
            }
        return bag;
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



    /** Sets the grid to a copy of the provided array, which must be rectangular. */
    public ObjectGrid3D setTo(Object[][][] field)
        {
        // check info
        
        if (field == null)
            throw new RuntimeException("ObjectGrid3D set to null field.");
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
                throw new RuntimeException("ObjectGrid3D initialized with a non-rectangular field.");
            for(int j = 0; j < h; j++)
                {
                if (field[i][j].length != l) // uh oh
                    throw new RuntimeException("ObjectGrid3D initialized with a non-rectangular field.");
                }
            }

        // load
        
        width = w;
        height = h;
        length = l;
        this.field = new Object[w][h][l];
        for(int i = 0; i < w; i++)
            for(int j=0; j< h; j++)
                {
                this.field[i][j] = (Object[]) field[i][j].clone();
                }
        return this;
        }




    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y), abs(z-Z) ) <= dist.  This region forms a
     * cube 2*dist+1 cells across, centered at (X,Y,Z).  If dist==1, this
     * is equivalent to the twenty-six neighbors surrounding (X,Y,Z), plus (X,Y) itself.  
     * Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p>Then places into the result Bag any Objects which fall on one of these <x,y,z> locations, clearning it first.
     * <b>Note that the order and size of the result Bag may not correspond to the X and Y and Z bags.</b>  If you want
     * all three bags to correspond (x, y, z, object) then use getNeighborsAndCorrespondingPositionsMaxDistance(...)
     * Returns the result Bag.
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
    public Bag getNeighborsMaxDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, Bag result, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        return getMooreNeighbors(x, y, z, dist, toroidal ? TOROIDAL : BOUNDED, true, result, xPos, yPos, zPos);
        }


    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y), abs(z-Z) ) <= dist.  This region forms a
     * cube 2*dist+1 cells across, centered at (X,Y,Z).  If dist==1, this
     * is equivalent to the twenty-six neighbors surrounding (X,Y,Z), plus (X,Y) itself.  
     * Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p>Then places into the result Bag any Objects which fall on one of these <x,y,z> locations, clearning it first.
     * <b>Note that the order and size of the result Bag may not correspond to the X and Y and Z bags.</b>  If you want
     * all three bags to correspond (x, y, z, object) then use getNeighborsAndCorrespondingPositionsMaxDistance(...)
     * Returns the result Bag.
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
    public Bag getMooreNeighbors( final int x, final int y, final int z, final int dist, int mode, boolean includeOrigin, Bag result, IntBag xPos, IntBag yPos, IntBag zPos )
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
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y), abs(z-Z) ) <= dist.  This region forms a
     * cube 2*dist+1 cells across, centered at (X,Y,Z).  If dist==1, this
     * is equivalent to the twenty-six neighbors surrounding (X,Y,Z), plus (X,Y) itself.  
     * Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p>For each Object which falls within this distance, adds the X position, Y position, Z position, and Object into the
     * xPos, yPos, zPos, and result Bag, clearing them first.  
     * Some <X,Y,Z> positions may not appear
     * and that others may appear multiply if multiple objects share that positions.  Compare this function
     * with getNeighborsMaxDistance(...).
     * Returns the result Bag.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p>This function may be run in one of three modes: Grid3D.BOUNDED, Grid3D.UNBOUNDED, and Grid3D.TOROIDAL.  If "bounded",
     * then the neighbors are restricted to be only those which lie within the box ranging from (0,0,0) to (width, height, length), 
     * that is, the width and height of the grid.  If "unbounded", then the neighbors are not so restricted.  Note that unbounded
     * neighborhood lookup only makes sense if your grid allows locations to actually <i>be</i> outside this box.  For example,
     * SparseGrid3D permits this but ObjectGrid3D and DoubleGrid3D and IntGrid3D and DenseGrid3D do not.  Finally if "toroidal",
     * then the environment is assumed to be toroidal, that is, wrap-around, and neighbors are computed in this fashion.  Toroidal
     * locations will not appear multiple times: specifically, if the neighborhood distance is so large that it wraps completely around
     * the width or height of the box, neighbors will not be counted multiple times.  Note that to ensure this, subclasses may need to
     * resort to expensive duplicate removal, so it's not suggested you use so unreasonably large distances.
     *
     * <p>You can also opt to include the origin -- that is, the (x,y) point at the center of the neighborhood -- in the neighborhood results.
     */
    public Bag getMooreNeighborsAndLocations(final int x, final int y, int z, final int dist, int mode, boolean includeOrigin, Bag result, IntBag xPos, IntBag yPos, IntBag zPos)
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();
        if( zPos == null )
            zPos = new IntBag();

        getMooreLocations( x, y, z, dist, mode, includeOrigin, xPos, yPos, zPos);
        reduceObjectsAtLocations( xPos,  yPos,  zPos, result);
        return result;
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
     * <p>Then places into the result Bag any Objects which fall on one of these <x,y,z> locations, clearning it first.
     * <b>Note that the order and size of the result Bag may not correspond to the X and Y and Z bags.</b>  If you want
     * all three bags to correspond (x, y, z, object) then use getNeighborsAndCorrespondingPositionsHamiltonianDistance(...)
     * Returns the result Bag.
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
    public Bag getNeighborsHamiltonianDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, Bag result, IntBag xPos, IntBag yPos, IntBag zPos)
        {
        return getVonNeumannNeighbors(x, y, z, dist, toroidal ? TOROIDAL : BOUNDED, true,result, xPos, yPos, zPos);
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
     * <p>Then places into the result Bag any Objects which fall on one of these <x,y,z> locations, clearning it first.
     * <b>Note that the order and size of the result Bag may not correspond to the X and Y and Z bags.</b>  If you want
     * all three bags to correspond (x, y, z, object) then use getNeighborsAndCorrespondingPositionsMaxDistance(...)
     * Returns the result Bag.
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
    public Bag getVonNeumannNeighbors( final int x, final int y, int z, final int dist, int mode, boolean includeOrigin, Bag result, IntBag xPos, IntBag yPos, IntBag zPos )
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



    /**
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) + abs(z-Z) <= dist.  This region 
     * forms an <a href="http://images.google.com/images?q=octahedron">octohedron</a> 2*dist+1 cells from point
     * to opposite point inclusive, centered at (X,Y,Y).  If dist==1 this is
     * equivalent to the six neighbors  above, below, left, and right, front, and behind (X,Y,Z)),
     * plus (X,Y,Z) itself.
     * Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     * <p>For each Object which falls within this distance, adds the X position, Y position, Z position, and Object into the
     * xPos, yPos, zPos, and result Bag, clearing them first.  
     * Some <X,Y,Z> positions may not appear
     * and that others may appear multiply if multiple objects share that positions.  Compare this function
     * with getNeighborsMaxDistance(...).
     * Returns the result Bag.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p>This function may be run in one of three modes: Grid3D.BOUNDED, Grid3D.UNBOUNDED, and Grid3D.TOROIDAL.  If "bounded",
     * then the neighbors are restricted to be only those which lie within the box ranging from (0,0,0) to (width, height, length), 
     * that is, the width and height of the grid.  If "unbounded", then the neighbors are not so restricted.  Note that unbounded
     * neighborhood lookup only makes sense if your grid allows locations to actually <i>be</i> outside this box.  For example,
     * SparseGrid3D permits this but ObjectGrid3D and DoubleGrid3D and IntGrid3D and DenseGrid3D do not.  Finally if "toroidal",
     * then the environment is assumed to be toroidal, that is, wrap-around, and neighbors are computed in this fashion.  Toroidal
     * locations will not appear multiple times: specifically, if the neighborhood distance is so large that it wraps completely around
     * the width or height of the box, neighbors will not be counted multiple times.  Note that to ensure this, subclasses may need to
     * resort to expensive duplicate removal, so it's not suggested you use so unreasonably large distances.
     *
     * <p>You can also opt to include the origin -- that is, the (x,y) point at the center of the neighborhood -- in the neighborhood results.
     */
    public Bag getVonNeumannNeighborsAndLocations(final int x, final int y, final int z, final int dist, int mode, boolean includeOrigin, Bag result, IntBag xPos, IntBag yPos, IntBag zPos)
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();
        if( zPos == null )
            zPos = new IntBag();

        getVonNeumannLocations( x, y, z, dist, mode, includeOrigin, xPos, yPos, zPos );
        reduceObjectsAtLocations( xPos,  yPos, zPos, result);
        return result;
        }

    public Bag getRadialNeighbors( final int x, final int y, final int z, final int dist, int mode, boolean includeOrigin,  Bag result, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        return getRadialNeighbors(x, y, z, dist, mode, includeOrigin, Grid3D.ANY, true, result, xPos, yPos, zPos);
        }


    public Bag getRadialNeighborsAndLocations( final int x, final int y, final int z, final int dist, int mode, boolean includeOrigin, Bag result, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        return getRadialNeighborsAndLocations(x, y, z, dist, mode, includeOrigin, Grid3D.ANY, true, result, xPos, yPos, zPos);
        }


    public Bag getRadialNeighbors( final int x, final int y, int z, final int dist, int mode, boolean includeOrigin,  int measurementRule, boolean closed,  Bag result, IntBag xPos, IntBag yPos, IntBag zPos)
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

    public Bag getRadialNeighborsAndLocations( final int x, final int y, int z, final int dist, int mode, boolean includeOrigin,  int measurementRule, boolean closed,  Bag result, IntBag xPos, IntBag yPos, IntBag zPos)
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();
        if( zPos == null )
            zPos = new IntBag();

        getRadialLocations( x, y, z, dist, mode, includeOrigin, measurementRule, closed, xPos, yPos, zPos );
        reduceObjectsAtLocations( xPos,  yPos, zPos, result);
        return result;
        }

    // the xPos and yPos bags so that each position corresponds to the equivalent result in
    // in the result bag.
    void reduceObjectsAtLocations(final IntBag xPos, final IntBag yPos, final IntBag zPos, Bag result)
        {
        if (result==null) result = new Bag();
        else result.clear();

        for( int i = 0 ; i < xPos.numObjs ; i++ )
            {
            assert sim.util.LocationLog.it(this, new Int3D(xPos.objs[i],yPos.objs[i],zPos.objs[i]));
            Object val = field[xPos.objs[i]][yPos.objs[i]][zPos.objs[i]] ;
            if (val != null) result.add( val );
            else
                {
                xPos.remove(i);
                yPos.remove(i);
                i--;  // back up and try the object now in the new slot
                }
            }
        }
                

    /* For each <xPos,yPos> location, puts all such objects into the result bag.  Returns the result bag.
       If the provided result bag is null, one will be created and returned. */
    Bag getObjectsAtLocations(final IntBag xPos, final IntBag yPos, final IntBag zPos, Bag result)
        {
        if (result==null) result = new Bag();
        else result.clear();

        for( int i = 0 ; i < xPos.numObjs ; i++ )
            {
            assert sim.util.LocationLog.it(this, new Int3D(xPos.objs[i],yPos.objs[i],zPos.objs[i]));
            Object val = field[xPos.objs[i]][yPos.objs[i]][zPos.objs[i]] ;
            if (val != null) result.add( val );
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
    public Bag getMooreNeighbors( int x, int y, int z, int dist, int mode, boolean includeOrigin )
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
    public Bag getVonNeumannNeighbors( int x, int y, int z, int dist, int mode, boolean includeOrigin )
        {
        return getVonNeumannNeighbors(x, y, z, dist, mode, includeOrigin, null, null, null, null);
        }





    public Bag getRadialNeighbors( final int x, final int y, int z, final int dist, int mode, boolean includeOrigin)
        {
        return getRadialNeighbors(x, y, z, dist, mode, includeOrigin, null, null, null, null);
        }




    }
