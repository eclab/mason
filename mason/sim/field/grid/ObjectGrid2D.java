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
    private static final long serialVersionUID = 1;

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
    
    public ObjectGrid2D(Object[][] values)
        {
        setTo(values);
        }
        
    /** Sets location (x,y) to val */
    public final void set(final int x, final int y, final Object val)
        {
        assert sim.util.LocationLog.it(this, new Int2D(x,y));
        field[x][y] = val;
        }

    /** Returns the element at location (x,y) */
    public final Object get(final int x, final int y)
        {
        assert sim.util.LocationLog.it(this, new Int2D(x,y));
        return field[x][y];
        }

    /** Sets all the locations in the grid the provided element. <b>WARNING:
        this may conflict with setTo(Object[][]) -- make sure you have casted properly.   */
    public final ObjectGrid2D setTo(Object thisObj)
        {
        Object[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
        for(int x=0;x<width;x++)
            {
            fieldx = field[x];
            for(int y = 0; y<height;y++)
                {
                assert sim.util.LocationLog.it(this, new Int2D(x,y));
                fieldx[y]=thisObj;
                }
            }
        return this;
        }

    /** Sets the grid to a copy of the provided array, which must be rectangular.  <b>WARNING:
        this may conflict with setTo(Object) -- make sure you have casted properly.  */
    public ObjectGrid2D setTo(Object[][] field)
        {
        // check info
        
        if (field == null)
            throw new RuntimeException("ObjectGrid2D set to null field.");
        int w = field.length;
        int h = 0;
        if (w != 0) h = field[0].length;
        for(int i = 0; i < w; i++)
            if (field[i].length != h) // uh oh
                throw new RuntimeException("ObjectGrid2D initialized with a non-rectangular field.");

        // load
        
        width = w;
        height = h;
        this.field = new Object[w][h];
        for(int i = 0; i < w; i++)
            this.field[i] = (Object[]) field[i].clone();
        return this;
        }

    /** Flattens the grid to a one-dimensional array, storing the elements in row-major order,including duplicates and null values. 
        Returns the grid. */
    public final Object[] toArray()
        {
        Object[][] field = this.field;
        Object[] fieldx = null;
        final int width = this.width;
        final int height = this.height;
        Object[] vals = new Object[width * height];
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
        
    /** Returns in a Bag all stored objects (including duplicates but not null values).  
        You are free to modify the Bag. */
    public final Bag elements()
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
                    {
                    assert sim.util.LocationLog.it(this, new Int2D(x,y));
                    bag.add(fieldx[y]);
                    }
                }
            }
        return bag;
        }


    /** Sets all the locations in the grid to null, and returns in a Bag all previously stored objects 
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
                assert sim.util.LocationLog.it(this, new Int2D(x,y));
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
        Object[] fieldx = null;
        for(int x = 0; x < width; x++)
            {
            fieldx = field[x];
            for(int y = 0;  y < height; y++)
                {
                Object obj = fieldx[y];
                if ((obj == null && from == null) ||
                    (onlyIfSameObject && obj == from) ||
                    (!onlyIfSameObject && obj.equals(from)))
                    fieldx[y] = to;
                }
            }
        }


/*
  final Bag getImmediateNeighbors(int x, int y, boolean toroidal, Bag result)
  {
  if (result != null)
  { result.clear();  result.resize(9); }  // not always 9 elements of course but it's the majority case by far
  else
  result = new Bag(9);  // likwise

  int width = this.width;
  int height = this.height;
        
  Object[] fieldx0 = null;
  Object[] fieldx = null;
  Object[] fieldx1 = null;
        
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
*/
       



    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y) ) <= dist, This region forms a
     * square 2*dist+1 cells across, centered at (X,Y).  If dist==1, this
     * is equivalent to the so-called "Moore Neighborhood" (the eight neighbors surrounding (X,Y)), plus (X,Y) itself.
     * Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     *
     * <p>Then places into the result Bag any Objects which fall on one of these <x,y> locations, clearning it first.
     * <b>Note that the order and size of the result Bag may not correspond to the X and Y bags.</b>  If you want
     * all three bags to correspond (x, y, object) then use getNeighborsAndCorrespondingPositionsMaxDistance(...)
     * Returns the result Bag.
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
    public Bag getNeighborsMaxDistance( final int x, final int y, final int dist, final boolean toroidal, Bag result, IntBag xPos, IntBag yPos )
        {
        return getMooreNeighbors(x, y, dist, toroidal ? TOROIDAL : BOUNDED, true, result, xPos, yPos);
        }


    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y) ) <= dist, This region forms a
     * square 2*dist+1 cells across, centered at (X,Y).  If dist==1, this
     * is equivalent to the so-called "Moore Neighborhood" (the eight neighbors surrounding (X,Y)), plus (X,Y) itself.
     * Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     *
     * <p>Then places into the result Bag any Objects which fall on one of these <x,y> locations, clearning it first.
     * <b>Note that the order and size of the result Bag may not correspond to the X and Y bags.</b>  If you want
     * all three bags to correspond (x, y, object) then use getNeighborsAndCorrespondingPositionsMaxDistance(...)
     * Returns the result Bag.
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
    public Bag getMooreNeighbors( final int x, final int y, final int dist, int mode, boolean includeOrigin, Bag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getMooreLocations( x, y, dist, mode, includeOrigin, xPos, yPos );
        return getObjectsAtLocations(xPos,yPos,result);
        }


    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y) ) <= dist.  This region forms a
     * square 2*dist+1 cells across, centered at (X,Y).  If dist==1, this
     * is equivalent to the so-called "Moore Neighborhood" (the eight neighbors surrounding (X,Y)), plus (X,Y) itself.
     *
     * <p>For each Object which falls within this distance, adds the X position, Y position, and Object into the
     * xPos, yPos, and result Bag, clearing them first.  
     * Some <X,Y> positions may not appear
     * and that others may appear multiply if multiple objects share that positions.  Compare this function
     * with getNeighborsMaxDistance(...).
     * Returns the result Bag.
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
    public Bag getMooreNeighborsAndLocations(final int x, final int y, final int dist, int mode, boolean includeOrigin, Bag result, IntBag xPos, IntBag yPos)
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getMooreLocations( x, y, dist, mode, includeOrigin, xPos, yPos );
        reduceObjectsAtLocations( xPos,  yPos,  result);
        return result;
        }



    /**
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) <= dist.  This region forms a diamond
     * 2*dist+1 cells from point to opposite point inclusive, centered at (X,Y).  If dist==1 this is
     * equivalent to the so-called "Von-Neumann Neighborhood" (the four neighbors above, below, left, and right of (X,Y)),
     * plus (X,Y) itself.
     *
     * <p>Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     * Then places into the result Bag any Objects which fall on one of these <x,y> locations, clearning it first.
     * Note that the order and size of the result Bag may not correspond to the X and Y bags.  If you want
     * all three bags to correspond (x, y, object) then use getNeighborsAndCorrespondingPositionsHamiltonianDistance(...)
     * Returns the result Bag (constructing one if null had been passed in).
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
    public Bag getNeighborsHamiltonianDistance( final int x, final int y, final int dist, final boolean toroidal, Bag result, IntBag xPos, IntBag yPos )
        {
        return getVonNeumannNeighbors(x, y, dist, toroidal ? TOROIDAL : BOUNDED, true,result, xPos, yPos);
        }


    /**
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) <= dist.  This region forms a diamond
     * 2*dist+1 cells from point to opposite point inclusive, centered at (X,Y).  If dist==1 this is
     * equivalent to the so-called "Von-Neumann Neighborhood" (the four neighbors above, below, left, and right of (X,Y)),
     * plus (X,Y) itself.
     *
     * <p>Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     * Then places into the result Bag any Objects which fall on one of these <x,y> locations, clearning it first.
     * Note that the order and size of the result Bag may not correspond to the X and Y bags.  If you want
     * all three bags to correspond (x, y, object) then use getNeighborsAndCorrespondingPositionsHamiltonianDistance(...)
     * Returns the result Bag (constructing one if null had been passed in).
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
    public Bag getVonNeumannNeighbors( final int x, final int y, final int dist, int mode, boolean includeOrigin, Bag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getVonNeumannLocations( x, y, dist, mode, includeOrigin, xPos, yPos );
        return getObjectsAtLocations(xPos,yPos,result);
        }



    /**
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) <= dist.  This region forms a diamond
     * 2*dist+1 cells from point to opposite point inclusive, centered at (X,Y).  If dist==1 this is
     * equivalent to the so-called "Von-Neumann Neighborhood" (the four neighbors above, below, left, and right of (X,Y)),
     * plus (X,Y) itself.
     *
     * <p>For each Object which falls within this distance, adds the X position, Y position, and Object into the
     * xPos, yPos, and result Bag, clearing them first.  
     * Some <X,Y> positions may not appear
     * and that others may appear multiply if multiple objects share that positions.  Compare this function
     * with getNeighborsMaxDistance(...).
     * Returns the result Bag.
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
    public Bag getVonNeumannNeighborsAndLocations(final int x, final int y, final int dist, int mode, boolean includeOrigin, Bag result, IntBag xPos, IntBag yPos)
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getVonNeumannLocations( x, y, dist, mode, includeOrigin, xPos, yPos );
        reduceObjectsAtLocations( xPos,  yPos,  result);
        return result;
        }




    /**
     * Gets all neighbors located within the hexagon centered at (X,Y) and 2*dist+1 cells from point to opposite point 
     * inclusive.
     * If dist==1, this is equivalent to the six neighbors immediately surrounding (X,Y), 
     * plus (X,Y) itself.
     *
     * <p>Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     * Then places into the result Bag any Objects which fall on one of these <x,y> locations, clearning it first.
     * Note that the order and size of the result Bag may not correspond to the X and Y bags.  If you want
     * all three bags to correspond (x, y, object) then use getNeighborsAndCorrespondingPositionsHamiltonianDistance(...)
     * Returns the result Bag (constructing one if null had been passed in).
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
    public Bag getNeighborsHexagonalDistance( final int x, final int y, final int dist, final boolean toroidal, Bag result, IntBag xPos, IntBag yPos )
        {
        return getHexagonalNeighbors(x, y, dist, toroidal ? TOROIDAL : BOUNDED, true, result, xPos, yPos);
        }


    /**
     * Gets all neighbors located within the hexagon centered at (X,Y) and 2*dist+1 cells from point to opposite point 
     * inclusive.
     * If dist==1, this is equivalent to the six neighbors immediately surrounding (X,Y), 
     * plus (X,Y) itself.
     *
     * <p>Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     * Then places into the result Bag any Objects which fall on one of these <x,y> locations, clearning it first.
     * Note that the order and size of the result Bag may not correspond to the X and Y bags.  If you want
     * all three bags to correspond (x, y, object) then use getNeighborsAndCorrespondingPositionsHamiltonianDistance(...)
     * Returns the result Bag (constructing one if null had been passed in).
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
    public Bag getHexagonalNeighbors( final int x, final int y, final int dist, int mode, boolean includeOrigin, Bag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getHexagonalLocations( x, y, dist, mode, includeOrigin, xPos, yPos );
        return getObjectsAtLocations(xPos,yPos,result);
        }
                
                
    /**
     * Gets all neighbors located within the hexagon centered at (X,Y) and 2*dist+1 cells from point to opposite point 
     * inclusive.
     * If dist==1, this is equivalent to the six neighbors immediately surrounding (X,Y), 
     * plus (X,Y) itself.
     *
     * <p>For each Object which falls within this distance, adds the X position, Y position, and Object into the
     * xPos, yPos, and result Bag, clearing them first.  
     * Some <X,Y> positions may not appear
     * and that others may appear multiply if multiple objects share that positions.  Compare this function
     * with getNeighborsMaxDistance(...).
     * Returns the result Bag.
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
    public Bag getHexagonalNeighborsAndLocations(final int x, final int y, final int dist, int mode, boolean includeOrigin, Bag result, IntBag xPos, IntBag yPos)
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getHexagonalLocations( x, y, dist, mode, includeOrigin, xPos, yPos );
        reduceObjectsAtLocations( xPos,  yPos,  result);
        return result;
        }



    public Bag getRadialNeighbors( final int x, final int y, final int dist, int mode, boolean includeOrigin,  Bag result, IntBag xPos, IntBag yPos )
        {
        return getRadialNeighbors(x, y, dist, mode, includeOrigin, Grid2D.ANY, true, result, xPos, yPos);
        }


    public Bag getRadialNeighborsAndLocations( final int x, final int y, final int dist, int mode, boolean includeOrigin, Bag result, IntBag xPos, IntBag yPos )
        {
        return getRadialNeighborsAndLocations(x, y, dist, mode, includeOrigin, Grid2D.ANY, true, result, xPos, yPos);
        }


    public Bag getRadialNeighbors( final int x, final int y, final int dist, int mode, boolean includeOrigin,  int measurementRule, boolean closed,  Bag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getRadialLocations( x, y, dist, mode, includeOrigin, measurementRule, closed, xPos, yPos );
        return getObjectsAtLocations(xPos,yPos,result);
        }
                

    public Bag getRadialNeighborsAndLocations( final int x, final int y, final int dist, int mode, boolean includeOrigin,  int measurementRule, boolean closed,  Bag result, IntBag xPos, IntBag yPos )
        {
        if( xPos == null )
            xPos = new IntBag();
        if( yPos == null )
            yPos = new IntBag();

        getRadialLocations( x, y, dist, mode, includeOrigin, measurementRule, closed, xPos, yPos );
        reduceObjectsAtLocations( xPos,  yPos,  result);
        return getObjectsAtLocations(xPos,yPos,result);
        }

        
    // For each <xPos, yPos> location, puts all such objects into the result bag.  Modifies
    // the xPos and yPos bags so that each position corresponds to the equivalent result in
    // in the result bag.
    void reduceObjectsAtLocations(final IntBag xPos, final IntBag yPos, Bag result)
        {
        if (result==null) result = new Bag();
        else result.clear();

        for( int i = 0 ; i < xPos.numObjs ; i++ )
            {
            assert sim.util.LocationLog.it(this, new Int2D(xPos.objs[i],yPos.objs[i]));
            Object val = field[xPos.objs[i]][yPos.objs[i]] ;
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
    Bag getObjectsAtLocations(final IntBag xPos, final IntBag yPos, Bag result)
        {
        if (result==null) result = new Bag();
        else result.clear();

        for( int i = 0 ; i < xPos.numObjs ; i++ )
            {
            assert sim.util.LocationLog.it(this, new Int2D(xPos.objs[i],yPos.objs[i]));
            Object val = field[xPos.objs[i]][yPos.objs[i]] ;
            if (val != null) result.add( val );
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
    public Bag getMooreNeighbors( int x, int y, int dist, int mode, boolean includeOrigin )
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
    public Bag getVonNeumannNeighbors( int x, int y, int dist, int mode, boolean includeOrigin )
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
    public Bag getHexagonalNeighbors( int x, int y, int dist, int mode, boolean includeOrigin )
        {
        return getHexagonalNeighbors(x, y, dist, mode, includeOrigin, null, null, null);
        }


    public Bag getRadialNeighbors( final int x, final int y, final int dist, int mode, boolean includeOrigin)
        {
        return getRadialNeighbors(x, y, dist, mode, includeOrigin, null, null, null);
        }





    }
